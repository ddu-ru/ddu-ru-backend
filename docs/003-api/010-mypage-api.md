# 010. 마이페이지 API 연동 가이드

마이페이지 메인, 작성글 관리, 찜한 여행 화면은 아래 API를 사용합니다. 모든 API는 JWT 인증이 필요합니다.

## 마이페이지 메인

`GET /api/v1/users/me/overview`

마이페이지 홈에 필요한 프로필 표시 정보와 함께 다음 정보를 반환합니다.

- `travelPreferences.destinationPreferences`: 선호 여행지. 도시 항목은 `imageUrl`을 포함하며 설정이 없으면 빈 배열
- `travelPreferences.availableDates`: 최대 3개의 여행 가능 날짜. 시작일·종료일 오름차순이며 설정이 없으면 빈 배열
- `travelStyleSummary`: 현재 프로필에 연결된 아바타 성향의 이름, 한줄 설명, 태그
- 설문을 완료하지 않아 아바타 성향이 없으면 `travelStyleSummary`는 `null`

여행 선호를 수정할 때는 기존 `PATCH /api/v1/users/me/travel-preferences`를 사용합니다.

성별, 생년월일, 전화번호 등 개인 상세 정보는 `GET /api/v1/users/me/profile`에서 별도로 조회합니다.

## 작성글 관리

`GET /api/v1/users/me/posts?cursor={postId}&size=10&status=ALL&sort=LATEST`

- `sort=LATEST`는 최신순, `sort=OLDEST`는 오래된순이며 생략 시 최신순입니다.
- 다음 페이지를 조회할 때는 첫 요청과 같은 `sort`를 전달합니다.
- `unusedSuperHostTicketCount`로 화면 상단의 보유 티켓 수를 표시합니다.
- 각 게시글은 모집 인원, 대표 이미지, 조회 수, 좋아요 수와 `displayStatus`를 반환합니다.
- 슈퍼호스트 적용 화면에서는 `GET /api/v1/posts/super-hosts/me`를 사용합니다.

## 찜한 여행

`GET /api/v1/users/me/liked-posts?cursor={postLikeId}&size=10`

- `cursor`는 게시글 ID가 아니라 이전 응답의 `nextCursor`인 PostLike ID입니다.
- 각 게시글은 모집 조건, 대표 이미지, 작성자 닉네임과 현재 슈퍼호스트 여부를 반환합니다.
- 하트 해제는 기존 `POST /api/v1/posts/{postId}/likes` 토글 API를 사용합니다.

## 빈 상태

- 목록이 없으면 `posts`는 빈 배열이고 `hasNext=false`, `nextCursor=null`입니다.
- 선호 여행지가 없거나 여행 날짜가 없으면 해당 목록만 빈 배열입니다.
- 설문 미완료는 오류가 아니며 `travelStyleSummary=null`입니다.
