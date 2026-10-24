# 슈퍼호스트 구현

제품 기준은 [슈퍼호스트 정책](../001-policy/004-super-host-policy.md)을 따릅니다.

## API

| API | 인증 | 용도 |
|---|---|---|
| `GET /api/v1/posts/super-hosts/me` | 필수 | 미사용 티켓 수와 적용 중·미적용 게시글 조회 |
| `POST /api/v1/posts/{postId}/super-host` | 필수 | 게시글에 티켓 1장 적용 또는 현재 종료 시각부터 기간 연장 |
| `GET /api/v1/home/super-hosts` | 선택 | 홈에 노출할 참여 가능한 슈퍼호스트 게시글을 최대 5개 조회 |
| `POST /api/v1/surveys` | 필수 | 최초 설문 완료 과정에서 3일권 티켓 3장 지급 |

기존 `GET /api/v1/posts/super-hosts`는 홈 전용 API와 역할이 중복되어 제거했습니다. 홈 노출에는 `GET /api/v1/home/super-hosts`를 사용합니다.

## 처리 기준

- 온보딩 티켓은 사용자 행을 잠근 뒤 기존 지급 여부를 확인하고 3장을 한 번만 생성합니다.
- 적용 시 게시글과 사용할 미사용 티켓을 잠가 동시 요청의 중복 소비를 방지합니다.
- 미적용 게시글은 현재 시각부터 3일, 적용 중인 게시글은 현재 최종 종료 시각부터 3일 연장합니다.
- 관리 조회는 적용 가능한 내 게시글을 `appliedPosts`와 `availablePosts`로 나눠 반환합니다.
- 홈 조회는 활성 노출 중에서도 삭제·마감·정원·여행 종료·신고 조건을 다시 확인합니다.
- 동일 게시글의 연장 이력이 여러 건이어도 홈에는 한 번만 노출합니다.

## 주요 응답

`GET /api/v1/posts/super-hosts/me`는 `unusedTicketCount`, `appliedPosts`, `availablePosts`를 반환합니다. 적용 중 게시글에는 `superHostEndsAt`이 있고 미적용 게시글에는 `null`이 들어갑니다.

`POST /api/v1/posts/{postId}/super-host`는 실제 적용 시각과 최종 종료 시각, 적용 후 남은 티켓 수를 반환합니다. 확인 화면의 남은 시간과 예상 종료 시각은 클라이언트가 계산하되, 적용 완료 후에는 응답의 `endsAt`과 `unusedTicketCount`를 최종값으로 사용합니다.

