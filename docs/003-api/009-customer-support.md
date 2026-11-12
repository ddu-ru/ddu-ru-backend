# 009. 고객센터 FAQ 및 문의 API

모든 고객센터 API는 로그인(JWT)이 필요합니다. 관리자 경로는 `ADMIN` 권한이 필요합니다.
공통 응답은 `{ "status": 200, "data": ... }`이며 문의 생성은 HTTP 및 status 모두 201입니다.

## 화면별 호출

| 화면 | API | 주요 동작 |
|---|---|---|
| FAQ | `GET /api/v1/faqs?category=SERVICE&keyword=사진` | 노출 FAQ, 카테고리 및 질문·답변 부분 검색 |
| 내 문의 목록 | `GET /api/v1/inquiries?cursor=42&size=20` | **기간 제한 없이 본인의 전체 문의**, ID 내림차순 |
| 문의 작성 | `POST /api/v1/inquiries` | 유형·본문·사진 등록 |
| 문의 상세 | `GET /api/v1/inquiries/{id}` | 내용·사진·답변·수정 가능 여부 |
| 문의 수정 | `PUT /api/v1/inquiries/{id}` | 답변 대기인 본인 문의 전체 교체 |
| 사진 업로드 | `POST /api/v1/images/inquiries/presigned-url` | 문의 전용 이미지 업로드 URL 발급 |
| 관리자 목록 | `GET /api/v1/admin/inquiries?status=WAITING&page=0&size=20` | 모든 작성자의 문의, 상태 필터 |
| 관리자 상세 | `GET /api/v1/admin/inquiries/{id}` | 문의·사진·답변 조회 |
| 관리자 답변 | `PUT /api/v1/admin/inquiries/{id}/answer` | 최초 답변 등록 또는 기존 답변 수정 |

## FAQ

`category`는 `SERVICE`(서비스), `ACCOUNT`(계정), `MATCHING`(매칭), `SECURITY`(보안)입니다.
전체 탭은 category를 생략합니다. keyword는 최대 100자이며 앞뒤 공백을 제거하고 공백만 있으면 검색 조건을 적용하지 않습니다.
질문 또는 답변의 대소문자를 구분하지 않는 부분 검색입니다. `%`, `_`는 와일드카드가 아닌 입력 문자로 검색합니다.
노출 중인 FAQ만 `sortOrder`, `id` 오름차순으로 반환합니다. 카테고리와 검색 조건은 동시에 적용합니다.

```json
{
  "status": 200,
  "data": [{"id": 1, "category": "SERVICE", "question": "질문", "answer": "답변"}]
}
```

빈 결과는 `data: []`입니다. 아코디언 열림·닫힘은 앱에서 처리합니다.
FAQ는 DB에서 관리하며 관리자 편집 API와 운영 FAQ seed는 포함하지 않습니다. 초기 DB는 빈 목록입니다.
운영자는 확정된 문구와 카테고리, `visible`, `sort_order`를 입력해야 합니다.

## 문의 등록·수정

```json
{
  "category": "BUG",
  "content": "채팅방에서 사진을 보내면 앱이 종료돼요.",
  "imageUrls": ["https://<bucket>.s3.<region>.amazonaws.com/inquiries/<uuid>.png"]
}
```

- 유형: `SERVICE`(서비스 이용), `ACCOUNT`(계정), `MATCHING`(매칭), `SECURITY`(보안), `BUG`(오류 및 버그), `OTHER`(기타).
- 제목은 별도 입력받지 않습니다. 목록은 `content`를 사용하고 앱에서 말줄임합니다.
- 본문은 앞뒤 유니코드 공백 제거 후 1~500자입니다. 길이는 Java 문자열 길이(UTF-16 단위) 기준입니다.
- `imageUrls`는 최대 4장으로 입력 순서를 보존합니다. 생략·null·빈 배열은 첨부 없음입니다. 배열 내 null·빈 URL은 거절합니다.
- PUT은 유형·본문·사진 전체 교체입니다. 사진 생략이나 빈 배열도 기존 첨부를 제거하므로 유지할 URL을 모두 보내야 합니다.
- 사진은 기존 [업로드 정책](./002-image-upload-guide.md)을 사용합니다. URL 발급은 요청당 최대 10개, 실제 문의 첨부는 최대 4장입니다.
- 각 사진은 JPG/JPEG/PNG, 최대 5MB이며 문의 전용 `inquiries/` 경로의 HTTPS S3 URL만 허용합니다. URL 최대 길이는 2,048자입니다.
- 첨부 제거는 문의와의 연결만 삭제합니다. S3 객체 자체 삭제와 이미지 바이너리 검사는 수행하지 않습니다.

등록·수정·상세는 동일한 상세 응답을 반환합니다.

```json
{
  "status": 200,
  "data": {
    "id": 42,
    "category": "BUG",
    "content": "문의 내용",
    "status": "WAITING",
    "createdAt": "2026-11-12T12:00:00",
    "modifiedAt": "2026-11-12T12:00:00",
    "imageUrls": [],
    "editable": true,
    "answer": null
  }
}
```

시간은 서버 기준 KST입니다. `WAITING`은 답변 대기, `ANSWERED`는 답변 완료입니다.
`editable`을 수정 버튼 활성화에 사용합니다. 답변 완료 시 사용자 수정은 서버에서도 409로 차단합니다.
답변 카드의 표시 이름·이미지는 앱의 고정 고객센터 리소스를 사용합니다.

## 내 문의 목록

```json
{
  "status": 200,
  "data": {
    "inquiries": [{"id": 42, "category": "BUG", "content": "문의 내용", "status": "WAITING", "createdAt": "2026-11-12T12:00:00"}],
    "nextCursor": 42,
    "hasNext": true,
    "size": 1
  }
}
```

최초 요청은 cursor를 생략하고 후속 요청은 nextCursor를 전달합니다. size 기본 20, 허용 범위 1~50, cursor는 양수입니다.
마지막 페이지는 nextCursor가 null, hasNext가 false입니다. 빈 목록은 inquiries가 빈 배열, size가 0입니다.
등록 ID 내림차순으로 조회하므로 수정·답변으로 순서가 바뀌지 않습니다. 한 달보다 오래된 문의도 포함합니다.

## 관리자 답변

본문 `{"content":"고객센터 답변"}`을 전송합니다. 앞뒤 공백 제거 후 1~5,000자이며 문의당 답변 하나를 저장합니다.
최초 등록 시 `ANSWERED`로 전환하고 작성자 수정을 막습니다. 관리자만 기존 답변을 수정할 수 있습니다.

상세의 answer는 다음 형태입니다.

```json
{"content":"고객센터 답변", "answeredAt":"2026-11-12T12:00:00", "modifiedAt":"2026-11-12T13:00:00"}
```

최초 답변 시각은 유지하며 수정 시각과 마지막 담당 관리자를 갱신합니다. 답변 이력은 저장하지 않습니다.
문의 수정과 관리자 답변은 동일 문의 행을 잠가 순차 처리합니다.
관리자 목록은 기간 제한이 없으며 상태 생략 시 전체, page는 0부터, size 기본 20·최대 50입니다.
최신순(ID 내림차순) 고정이며 sort는 적용하지 않습니다. 응답은 `content`, `totalElements`, `totalPages`, `page`, `size`, `last`입니다.

**이번 범위에서는 앱 내 알림·FCM을 발송하지 않습니다.** UI의 “답변이 등록되면 알림” 문구는 후속 알림 연동 전까지 실제 동작과 다릅니다.

## 오류

| HTTP | errorCode | 조건 |
|---|---|---|
| 400 | `INVALID_INPUT_VALUE` | 누락, 길이·개수·enum·페이지 형식 오류 |
| 400 | `INVALID_INQUIRY`, `INVALID_INQUIRY_ANSWER` | 도메인의 문의·답변 규칙 위반 |
| 400 | `IMAGE_URL_INVALID_FORMAT`, `IMAGE_URL_NOT_ABSOLUTE`, `IMAGE_URL_INVALID_SCHEME`, `IMAGE_URL_NOT_ALLOWED` | 첨부 URL 오류 |
| 401 | `UNAUTHORIZED` | 인증 필요 |
| 403 | `INQUIRY_ACCESS_DENIED` | 타인 문의 조회·수정 |
| 403 | `ACCESS_DENIED` | 일반 사용자의 관리자 API 접근 |
| 404 | `INQUIRY_NOT_FOUND` | 존재하지 않는 문의 |
| 409 | `INQUIRY_ALREADY_ANSWERED` | 답변 완료 문의 수정 |

업로드 오류 및 사용자 조회 오류를 포함한 상세 명세는 Swagger `public`/`admin` 그룹을 참고합니다.
