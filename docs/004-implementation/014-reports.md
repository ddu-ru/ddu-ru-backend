# 신고 접수 및 검토

## API

모든 API는 JWT 인증이 필요합니다. `/admin/` 경로는 ADMIN 역할만 접근합니다.

| Method | URL | 용도 |
|---|---|---|
| POST | `/api/v1/posts/{postId}/reports` | 기존 게시글 신고 |
| POST | `/api/v1/users/{targetUserId}/reports` | 사용자 신고 |
| POST | `/api/v1/chat/rooms/{roomId}/messages/{messageId}/reports` | 메시지 신고 |
| GET | `/api/v1/reports/{reportId}` | 신고자 본인의 처리 상태 |
| POST | `/api/v1/reports/evidence/presigned-url` | 증빙 사진 1장 업로드 URL |
| GET | `/api/v1/admin/reports` | 관리자 목록, 기존 상태/페이지 필터 유지 |
| PATCH | `/api/v1/admin/reports/{reportId}` | 관리자 처리 |
| GET | `/api/v1/admin/reports/{reportId}/evidence` | 관리자 증빙 다운로드 URL |

접수 요청은 공통입니다. 사유는 필수이며 설명과 첨부는 선택입니다.

```json
{
  "reason": "INAPPROPRIATE",
  "description": "부적절한 메시지를 반복해서 보내고 있습니다.",
  "evidenceKey": "reports/1/12345678-1234-1234-1234-123456789abc.png"
}
```

성공 시 HTTP 201과 `{"status":201,"data":{"reportId":10,"status":"RECEIVED"}}`를 반환합니다.
접수 확인 팝업은 프론트에서 처리하며, API 성공 응답 이후에만 완료 화면으로 이동합니다.

## 대상 및 입력 정책

- `POST`, `USER`, `CHAT_MESSAGE`를 별도 대상으로 저장합니다. 채팅방 자체 신고는 제공하지 않습니다.
- 본인, 본인 게시글, 본인 메시지는 신고할 수 없습니다.
- 사용자는 프로필 진입에서도 신고할 수 있어 같은 방 소속을 요구하지 않습니다.
- 메시지는 현재 채팅방 참여자만 신고할 수 있습니다. 그룹방은 ACTIVE 여정 참여자도 확인합니다.
- CLOSED 방은 접근 가능한 참여자에게 신고를 허용하고 DELETED 방은 제외합니다.
- 메시지의 방 소속 및 참여 시작 시각을 확인합니다. 타인의 TEXT/IMAGE/POLL 메시지가 대상이며 SYSTEM은 제외합니다.
- 메시지 작성자와 대상 스냅샷은 서버가 저장합니다. 클라이언트가 전달한 작성자/원문을 사용하지 않습니다.
- 신규 신고 사유는 `SPAM_AD`, `INAPPROPRIATE`, `SCAM`, `PRIVACY_EXPOSURE`, `OTHER`입니다.
- 기존 게시글 신고의 `FALSE_INFO`, `DUPLICATE`는 하위 호환을 위해 유지합니다. 새 화면에서는 공통 5개 사유만 노출할 수 있습니다.
- 설명은 선택, 최대 200자입니다. 기존 1,000자 데이터는 유지하지만 새 접수의 한도는 변경되므로 프론트 공지가 필요합니다.
- 첨부는 우선 선택 1장입니다. JPG/JPEG/PNG, 최대 5,242,880바이트이며 GIF는 허용하지 않습니다.
- 신고는 자동 차단/탈퇴/강퇴를 수행하지 않습니다. 접수 후 사용자용 수정/취소 API는 없습니다.

## 중복 및 동시성

- 게시글/메시지: 신고자별 같은 대상 1회. DB 유니크 제약을 함께 사용합니다.
- 사용자: 같은 신고자·대상의 RECEIVED/IN_REVIEW 신고가 있으면 409 `DUPLICATE_REPORT`입니다.
- 사용자 신고는 RESOLVED/REJECTED 이후 새 사건을 다시 접수할 수 있습니다.
- 사용자/메시지 접수는 신고자 User 행을 잠그고 READ_COMMITTED에서 중복을 조회합니다. 동시에 접수되어도 미처리 신고가 중복 생성되지 않도록 합니다.
- 관리자 처리는 Report 행 잠금 후 상태를 변경합니다. 동일 최종 상태 요청은 결과 알림을 재발행하지 않습니다.
- 반복 접수 속도 제한, 증거 보존 기간, 처리 SLA는 운영 정책으로 별도 확정해야 합니다. UI의 1~3일 안내는 시스템이 보장하는 시간이 아닙니다.

## 비공개 증빙 업로드

1. `{ "fileName": "evidence.png", "fileSize": 102400 }`로 업로드 URL을 요청합니다.
2. 응답의 `presignedUrl`로 정확히 해당 바이트 수의 파일을 PUT 업로드합니다. URL은 10분간 유효합니다.
3. 업로드 성공 후 `evidenceKey`를 신고 요청에 포함합니다. 공개 이미지 URL은 받지 않습니다.
4. 서버는 로그인 사용자 ID가 포함된 정규 키 형식만 허용합니다.
5. 관리자 증빙 API는 신고에 저장된 파일에 한해 5분짜리 GET URL을 반환합니다.

기존 `AWS_S3_BUCKET` 버킷의 `reports/{userId}/{UUID}.{확장자}` 경로를 사용합니다. 별도 버킷이나 증빙 전용 환경변수는 필요하지 않습니다.
버킷 설정/서명기가 없으면 증빙 기능은 503 `REPORT_EVIDENCE_UNAVAILABLE`로 실패합니다. 첨부 없는 신고는 가능합니다.

- 기존 S3와 같은 리전을 사용합니다. 서버 자격증명에 해당 버킷 `reports/*`의 PutObject/GetObject 권한이 필요합니다.
- 공개 읽기 버킷 정책은 기존 이미지 경로에만 적용하고 `reports/*`를 포함하지 않습니다. 전체 버킷(`버킷명/*`)에 대한 공개 읽기도 허용하면 안 됩니다.
- ACL을 비활성화하고, 다른 정책이나 공개 CDN을 통해 `reports/*`에 접근할 수 없는지 확인합니다. 경로를 나누는 것만으로 비공개가 보장되지는 않습니다.
- 기존 공개 이미지가 영향을 받으므로 버킷 전체의 공개 접근 설정을 일괄 변경하지 않습니다. 배포 전 서명 없는 증빙 URL은 접근 불가하고 관리자용 서명 URL만 정상 조회되는지 확인합니다.
- 클라이언트에 맞는 PUT CORS, 암호화와 증빙 보존/폐기 정책을 설정합니다.
- 키 형식 검증은 파일 실재 여부나 실제 이미지 내용 검사가 아닙니다. 악성 파일 검사, 내용 기반 MIME 확인, 이미지 바이너리의 불변 보존은 별도 작업입니다.
- 공개 이미지 URL로 우회하거나 기존 게시판/채팅 갤러리에 증빙을 노출하지 않습니다.

## 관리자 및 알림

- 기존 `reports` 테이블을 확장합니다. `post_id`는 게시글 신고에만 존재하고 각 대상 FK와 `target_type`을 검증하는 CHECK를 추가합니다.
- 신고 당시 게시글 제목/내용, 사용자 표시명, 메시지 타입/내용을 `target_snapshot`에 저장합니다. IMAGE는 URL 문자열 스냅샷이며 바이너리 복사본이 아닙니다.
- 관리자 목록은 `targetType`, `reportedUserId`, `chatRoomId`, `messageId`, `targetSnapshot`, `hasEvidence`를 추가합니다. 기존 게시글 필드는 유지합니다.
- ‘익명 신고’는 상대방에게 신고자를 공개하지 않는다는 의미입니다. 관리자는 신고자 ID를 조회할 수 있습니다.
- 처리 완료/거부 상태로 변경되면 커밋 이후 신고자에게 `REPORT_REVIEWED` 인앱 알림과 설정에 따른 FCM을 발송합니다. 관리자 메모/증거/상대방 정보는 알림에 넣지 않습니다.
- 이 알림은 SYSTEM 분류이며 기존 ALL/UNREAD 탭에만 표시합니다. 매칭/여정 카운트에는 포함하지 않으므로 전체 카운트와 두 탭의 합이 달라질 수 있습니다.
- 푸시 data는 `resourceType=REPORT`, `resourceId=신고 ID`입니다. 프론트는 본인 신고 상태 API로 연결합니다. 타인의 신고 ID는 404입니다.
- 기존 알림 방식과 동일하게 커밋 이후 전송 실패는 로깅하며 자동 재시도/outbox는 없습니다.

## 마이그레이션 및 검증

- `V40__extend_report_targets.sql`: 기존 게시글 신고 보존, nullable post FK, 신규 대상 FK/인덱스/스냅샷/증빙 키, 알림 ENUM 확장.
- 새 버전은 배포 시점의 dev 최신 마이그레이션 번호와 중복이 없는지 다시 확인합니다.
- 신고 서비스, 채팅 접근 검증, 증빙 서명, 관리자 처리, 결과 알림, 본인 상태 조회 및 H2 동시성 테스트를 제공합니다.
- `./gradlew mysqlIntegrationTest --tests '*MySqlSchemaCompatibilityIntegrationTest'`로 격리된 MySQL 8.4.9의 Flyway 적용 및 JPA 매핑을 검증합니다.
- 운영 DB 데이터가 있는 상태의 업그레이드, 비공개 버킷 정책, 실기기 FCM은 배포 환경에서 별도 확인합니다.
