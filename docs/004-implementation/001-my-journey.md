# 001. 나의 여정 구현 현황

나의 여정 기능의 개발 진행 상태, 도메인 구조, 설계 포인트를 정리한 문서다.
기획/제품 흐름은 [나의 여정 제품 흐름](../001-policy/flows/004-my-journey-product-flow.md)을 참고한다.

---

## 1. 도메인 구분

백엔드 관점에서 나의 여정은 아래 테이블 구조로 표현된다.

| 테이블 | 역할 |
|--------|------|
| `posts` | 여행/모집글 루트 |
| `participations` | 신청 과정 관리 (`PENDING / CONTACTING / APPROVED / REJECTED`) |
| `journey_members` | 실제 협업 멤버 관리 (`HOST / MEMBER`, `ACTIVE / REMOVED / LEFT`) |
| `journey_member_role_labels` | 멤버별 역할 라벨 (멤버당 최대 5개, 기본 역할 5종 + CUSTOM) |
| `journey_posts` | 여행 게시판 게시글 |
| `journey_post_images` | 게시글 이미지 (게시글당 최대 4장, `sort_order` 순서 유지) |
| `journey_post_comments` | 게시글 댓글 |
| `journey_schedules` | 여행 일정 아이템 |

나의 여정 관련 권한과 목록 조회는 `journey_members`를 중심으로 판단한다.

---

## 2. 개발 순서 및 진행 상태

### 1단계. 정책 확정 ✅ 완료

- 호스트 전용 권한과 공통 협업 권한 경계 확정
- `participations`와 `journey_members`의 역할 경계 확정

### 2단계. 공통 상세 응답 ✅ 완료

상세 화면 렌더링에 필요한 핵심값 확정

- `isOwner` — 내가 호스트인지
- `myParticipationStatus` — 내 참여 상태
- `canEditPost` — 모집글 원본 수정 가능 여부
- `groupRoomId` — 그룹 채팅방 ID

### 3단계. 메인/상세 진입 ✅ 완료

- 나의 여정 메인 목록 (`journey_members` 기준 조회)
- 나의 여정 상세 조회

### 4단계. 협업 도메인 순차 개발 🔄 진행 중

- 여행 게시판 → ✅ 완료
- 댓글 → ✅ 완료
- 공지 지정/해제 → ✅ 완료
- 일정 → ✅ 완료
- 역할 지정 → ✅ 완료
- 갤러리 통합조회 → ✅ 완료
- 할 일 → ⏸️ 보류
- 여행 정보 → ⏸️ 보류
- 투표 → 생성/목록/상세/투표/재투표/선택지 추가 구현

### 5단계. 호스트 전용 관리 기능 🔄 진행 중

- 게시글 공지 설정/고정 → ✅ 완료
- 참여자 내보내기 → ✅ 완료
- 역할 지정/해제 → ✅ 완료
- 투표 확정 → ⏸️ 보류
- 여행 종료 → 🔜 구현예정

---

## 3. API 구현 현황

### 나의 여정 (워크스페이스 진입)

| 기능 | 메서드 | 엔드포인트 | 상태 |
|------|--------|-----------|------|
| 메인 목록 조회 | `GET` | `/api/v1/users/me/journeys` | ✅ 구현 |
| 멤버 목록 조회 | `GET` | `/api/v1/journeys/{journeyId}/members` | ✅ 구현 |
| 상세 조회 | `GET` | `/api/v1/journeys/{journeyId}` | ✅ 구현 |
| 기본 정보 수정 (제목·대표 사진·여행 날짜) | `PATCH` | `/api/v1/journeys/{journeyId}` | ✅ 구현 |
| 멤버 역할 설정 (PUT replace-all, 호스트 전용) | `PUT` | `/api/v1/journeys/{journeyId}/members/{memberUserId}/roles` | ✅ 구현 |
| 참여자 내보내기 | `DELETE` | `/api/v1/journeys/{journeyId}/members/{memberUserId}` | ✅ 구현 |
| 여행 종료 | - | - | 🔜 구현예정 |

### 여행 게시판

| 기능 | 메서드 | 엔드포인트 | 상태 |
|------|--------|-----------|------|
| 게시글 목록 조회 (커서 기반, 공지 우선) | `GET` | `/api/v1/journeys/{journeyId}/posts` | ✅ 구현 |
| 게시글 단건 조회 | `GET` | `/api/v1/journeys/{journeyId}/posts/{journeyPostId}` | ✅ 구현 |
| 게시글 작성 (이미지 최대 4장) | `POST` | `/api/v1/journeys/{journeyId}/posts` | ✅ 구현 |
| 게시글 수정 (이미지 최대 4장) | `PATCH` | `/api/v1/journeys/{journeyId}/posts/{journeyPostId}` | ✅ 구현 |
| 게시글 삭제 (작성자 또는 호스트) | `DELETE` | `/api/v1/journeys/{journeyId}/posts/{journeyPostId}` | ✅ 구현 |
| 공지 지정/해제 (호스트 전용, 최대 5개) | `PATCH` | `/api/v1/journeys/{journeyId}/posts/{journeyPostId}/notice` | ✅ 구현 |

### 갤러리

| 기능 | 메서드 | 엔드포인트 | 상태 |
|------|--------|-----------|------|
| 갤러리 통합조회 (이미지 단위 커서 페이지네이션) | `GET` | `/api/v1/journeys/{journeyId}/gallery` | ✅ 구현 |

### 댓글

| 기능 | 메서드 | 엔드포인트 | 상태 |
|------|--------|-----------|------|
| 댓글 목록 조회 (전체) | `GET` | `/api/v1/journeys/{journeyId}/posts/{journeyPostId}/comments` | ✅ 구현 |
| 댓글 작성 | `POST` | `/api/v1/journeys/{journeyId}/posts/{journeyPostId}/comments` | ✅ 구현 |
| 댓글 수정 | `PATCH` | `/api/v1/journeys/{journeyId}/posts/{journeyPostId}/comments/{commentId}` | ✅ 구현 |
| 댓글 삭제 (작성자 또는 호스트) | `DELETE` | `/api/v1/journeys/{journeyId}/posts/{journeyPostId}/comments/{commentId}` | ✅ 구현 |

### 일정

| 기능 | 메서드 | 엔드포인트 | 상태 |
|------|--------|-----------|------|
| 일정 목록 조회 (Day 그룹) | `GET` | `/api/v1/journeys/{journeyId}/schedules` | ✅ 구현 |
| 일정 생성 | `POST` | `/api/v1/journeys/{journeyId}/schedules` | ✅ 구현 |
| 일정 수정 | `PATCH` | `/api/v1/journeys/{journeyId}/schedules/{scheduleId}` | ✅ 구현 |
| 일정 삭제 | `DELETE` | `/api/v1/journeys/{journeyId}/schedules/{scheduleId}` | ✅ 구현 |

### 할 일

> ⏸️ 스코프 축소로 보류 (8월 출시 이후 검토)

| 기능 | 메서드 | 엔드포인트 | 상태 |
|------|--------|-----------|------|
| 할 일 목록 조회 | `GET` | `/api/v1/journeys/{journeyId}/todos` | ⏸️ 보류 |
| 할 일 추가 | `POST` | `/api/v1/journeys/{journeyId}/todos` | ⏸️ 보류 |
| 할 일 상태 변경/수정 | `PATCH` | `/api/v1/journeys/{journeyId}/todos/{todoId}` | ⏸️ 보류 |
| 할 일 삭제 | `DELETE` | `/api/v1/journeys/{journeyId}/todos/{todoId}` | ⏸️ 보류 |

### 여행 정보

> ⏸️ 스코프 축소로 보류 (8월 출시 이후 검토)

| 기능 | 메서드 | 엔드포인트 | 상태 |
|------|--------|-----------|------|
| 여행 정보 조회 | `GET` | `/api/v1/journeys/{journeyId}/info` | ⏸️ 보류 |
| 여행 정보 추가/수정 | `PATCH` | `/api/v1/journeys/{journeyId}/info` | ⏸️ 보류 |

### 투표

> ⏸️ 스코프 축소로 보류 (8월 출시 이후 검토)

| 기능 | 메서드 | 엔드포인트 | 상태 |
|------|--------|-----------|------|
| 투표 목록 조회 | `GET` | `/api/v1/journeys/{journeyId}/polls` | 구현 |
| 투표 생성 | `POST` | `/api/v1/journeys/{journeyId}/polls` | 구현 |
| 투표 상세 조회 | `GET` | `/api/v1/journeys/{journeyId}/polls/{pollId}` | 구현 |
| 투표 및 재투표 | `PUT` | `/api/v1/journeys/{journeyId}/polls/{pollId}/votes/me` | 구현 |
| 선택지 추가 | `POST` | `/api/v1/journeys/{journeyId}/polls/{pollId}/options` | 구현 |
| 투표 확정 (호스트 전용) | `PATCH` | `/api/v1/journeys/{journeyId}/polls/{pollId}/finalize` | ⏸️ 보류 |

---

## 4. 도메인별 구현 상태

| 도메인 | 엔티티 | DB 마이그레이션 | 서비스 | 상태 |
|--------|--------|----------------|--------|------|
| 여행 워크스페이스 | `Journey` | V14 | `JourneyQueryService`, `JourneyService` | ✅ 구현 |
| 여행 멤버십 | `JourneyMember` | V13, V14 | `JourneyService` | ✅ 구현 |
| 멤버 역할 라벨 | `JourneyMemberRoleLabel` | V22 | `JourneyService` | ✅ 구현 |
| 여행 게시판 | `JourneyPost` | V18 | `JourneyPostService` | ✅ 구현 |
| 게시글 이미지 | `JourneyPostImage` | V23 | `JourneyPostService`, `JourneyGalleryService` | ✅ 구현 |
| 댓글 | `JourneyPostComment` | V19 | `JourneyPostCommentService` | ✅ 구현 |
| 일정 | `JourneySchedule` | V20 | `JourneyScheduleService` | ✅ 구현 |
| 할 일 | - | - | - | ⏸️ 보류 |
| 여행 정보 | - | - | - | ⏸️ 보류 |
| 투표 | `JourneyPoll`, `JourneyPollOption`, `JourneyPollVote` | V33 | `JourneyPollService`, `JourneyPollQueryService` | 구현 |

---

### 투표 API 계약

투표의 생성/조회/투표/선택지 추가는 활성 여정 멤버가 사용할 수 있습니다. 생성에는 활성 그룹 채팅방과 생성자의 채팅 멤버십도 필요합니다.
투표 목록은 `cursor`(마지막 투표 ID), `size`(기본 20, 최대 50)를 받으며 ID 내림차순으로 종료된 투표까지 반환합니다.

생성 요청:

```json
{
  "title": "저녁 뭐 먹을까요?",
  "options": ["한식", "일식", "중식"],
  "allowMultipleChoices": true,
  "anonymous": true,
  "allowOptionAddition": true,
  "closesAt": "2026-12-01T18:00:00"
}
```

- 제목과 선택지는 앞뒤 공백을 제거한 뒤 1~100자로 검증합니다. 선택지는 2~10개이며 같은 내용은 허용하지 않습니다.
- 종료 시각은 서비스 시간대(Asia/Seoul) 기준으로 미래여야 합니다. `now >= closesAt`이면 `CLOSED`이며 투표/선택지 추가를 거절합니다.
- 생성 뒤 제목, 설정, 종료 시각 변경, 조기 종료/확정, 투표 삭제는 이번 범위에 포함하지 않습니다.
- `PUT /{pollId}/votes/me` 요청은 `{"optionIds":[11,12]}`입니다. 단일 선택은 정확히 1개만 허용하고 다른 투표의 선택지, 중복 ID, 빈 배열은 거절합니다.
- 다시 투표하기는 같은 PUT API를 사용합니다. 화면에서 다시 선택하는 동안 기존 표를 유지하고 제출할 때 전체 교체합니다. 동일한 선택 재요청은 변경/이벤트 없이 성공합니다.
- `POST /{pollId}/options` 요청은 `{"content":"양식"}`입니다. 추가 허용 설정이 켜진 경우에만 활성 멤버 누구나 추가할 수 있고, 추가 후에도 총 10개 제한을 유지합니다.

상세/생성/투표/선택지 추가 응답은 공통 `ApiResult`의 `data`에 아래 구조를 사용합니다.

```json
{
  "poll": {
    "pollId": 5,
    "journeyId": 1,
    "title": "저녁 뭐 먹을까요?",
    "status": "OPEN",
    "closesAt": "2026-12-01T18:00:00",
    "allowMultipleChoices": true,
    "anonymous": true,
    "allowOptionAddition": true,
    "voterCount": 1,
    "options": [
      {"optionId": 11, "content": "한식", "voteCount": 1},
      {"optionId": 12, "content": "일식", "voteCount": 1},
      {"optionId": 13, "content": "중식", "voteCount": 0}
    ],
    "selectedOptionIds": [11, 12]
  },
  "author": {"userId": 10, "nickname": "여행자", "profileImageUrl": null},
  "createdAt": "2026-11-30T12:00:00",
  "voters": []
}
```

- 목록의 `polls`와 채팅 카드 `poll`은 위 응답의 `poll` 요약 구조를 재사용합니다. 목록에는 `nextCursor`, `hasNext`, 실제 반환 개수인 `size`도 포함합니다.
- 득표수는 투표 전에도 공개합니다. `voterCount`는 선택지별 표의 합이 아니라 중복을 제거한 참여자 수입니다.
- 익명 투표는 `voters: []`이며 본인 선택만 `selectedOptionIds`로 반환합니다. 비익명 상세 응답은 `voters`에 `{optionId, voter: {userId, nickname, profileImageUrl}}`을 반환합니다. 생성자 정보는 익명 여부와 관계없이 표시합니다.
- 여정에서 나가거나 강퇴된 멤버의 기존 표는 보존하지만 그 멤버는 조회/재투표할 수 없습니다. 나중에 합류한 활성 멤버는 여정 투표 목록에서 기존 투표에 접근할 수 있습니다.
- 재투표/선택지 추가는 부모 투표 행의 비관적 잠금으로 직렬화합니다. 명령 트랜잭션은 `READ_COMMITTED`를 사용해 잠금 대기 후 이전 요청의 최신 선택/집계를 읽습니다.
- 각 선택지별 `(option_id, user_id)` 유니크 제약으로 중복 저장을 막고, 참여자 수와 옵션별 득표수는 DB에서 집계합니다.
- 채팅 연동과 `POLL_UPDATED` 이벤트는 [채팅 구현 문서](006-chat.md#6-투표-카드-연동)를 참고합니다.
- V33은 투표 테이블 3개와 `chat_messages.poll_id`를 추가하고 `message_type`의 MySQL ENUM에 `POLL`을 추가합니다.
- 투표 테스트는 서비스 단위 테스트와 H2 기반 서비스 트랜잭션/동시성 테스트로 구성합니다. 상세 조회자 수와 실기기 FCM/채팅 UI 확인은 별도 항목이며, 기존 MySQL Flyway 테스트는 Docker가 있어야 실행됩니다.

## 5. 주요 설계 포인트

- **접근 제어**: 모든 나의 여정 API는 `journey_members.status = ACTIVE` 여부를 확인해 비멤버 접근을 차단한다.
- **공지 최대 5개 제한**: 공지 지정 시 비관적 락(`SELECT FOR UPDATE`)으로 직렬화해 동시성을 제어한다.
- **커서 기반 페이지네이션**: 게시글 목록은 공지 우선 → 최신순 정렬을 DB `ORDER BY`로 보장하며, look-ahead 방식으로 `hasNext`를 판단한다.
- **댓글 전체 로드**: 댓글 목록은 게시글 상세 진입 시 전체를 한 번에 반환한다. 여정 멤버는 소규모 그룹으로 댓글 수가 구조적으로 제한되므로 페이지네이션 없이 전체 조회한다.
- **댓글 정렬**: 오래된 댓글부터(ASC) 표시한다.
- **댓글 수 N+1 방지**: 게시글 목록 조회 시 `GROUP BY` 벌크 쿼리로 댓글 수를 한 번에 집계한다.
- **소프트 삭제**: 게시글(`JourneyPost`), 댓글(`JourneyPostComment`), 일정(`JourneySchedule`) 모두 `isDeleted / deletedAt / deletedBy` 패턴을 사용한다.
- **게시글 삭제 권한**: 게시글은 작성자 본인 또는 호스트가 삭제할 수 있다. 수정은 작성자 본인만 가능하다.
- **댓글 삭제 권한**: 댓글은 작성자 본인 또는 호스트가 삭제할 수 있다. 수정은 작성자 본인만 가능하다.
- **게시글 이미지**: 게시글당 최대 4장. `journey_post_images` 테이블에 `sort_order`로 순서를 유지하며, 수정 시 PUT replace-all 방식으로 전체 교체한다(`imageUrls: null`이면 변경 없음, `imageUrls: []`이면 전체 삭제).
- **content 검증 위치**: 댓글 content의 유효성(빈 값, 300자 초과)은 `JourneyPostComment` 도메인 내부에서 검증한다. 서비스는 trim만 수행한다.
- **호스트 표시**: 게시글·댓글 응답에서 작성자가 호스트인지 여부를 `isHost` 필드로 함께 내려준다.
- **그룹 채팅 연동**: 나의 여정 상세 응답에 `groupRoomId`를 포함해 프론트가 바로 채팅방으로 이동할 수 있게 한다.
- **일정 상대 일차(dayOffset)**: 일정은 절대 날짜 대신 여행 시작일 기준 상대 일차(`dayOffset: 0, 1, 2...`)로 저장한다. 여행 날짜가 변경되어도 일정 데이터가 그대로 보존된다. 응답에서 실제 날짜는 `startDate.plusDays(dayOffset)`으로 계산해 내려준다.
- **일정 Day 그룹핑**: 일정 목록은 `dayOffset` 기준으로 그룹핑해 Day 단위로 반환한다. 일정이 없는 날도 빈 Day 구조를 유지한다. `day` 번호는 `dayOffset + 1`이며 클라이언트는 보내지 않는다.
- **일정 시간 제약**: 시작 시간(`startTime`)과 종료 시간(`endTime`) 모두 선택값이다. 종료 시간은 시작 시간이 있을 때만 허용하며, 시작 시간이 종료 시간보다 같거나 늦으면 검증 오류다. 이 제약은 엔티티 생성/수정 시 도메인 내부에서 검증한다.
- **일정 시간 클리어**: 수정 요청에서 `clearStartTime: true` 또는 `clearEndTime: true`로 시간 필드를 명시적으로 null로 초기화할 수 있다. `clearStartTime`을 쓰면서 기존 `endTime`이 남아 있으면 시간 제약 위반으로 오류. `clear` 플래그와 값이 동시에 오면 `clear`가 우선한다.
- **일정 카테고리**: `MEAL / SIGHTSEEING / CAFE / REST / SHOPPING / ACTIVITY` 6종이다.
- **일정 수정·삭제 권한**: 일정은 작성자 여부와 관계없이 `ACTIVE` 멤버 전체가 수정·삭제할 수 있다. 협업 일정 관리 특성상 소유권 제한을 두지 않는다.
- **여행 날짜 수정**: 기본 정보 수정 API에서 `startDate` / `endDate`를 함께 입력하면 `posts` 테이블의 여행 날짜가 업데이트된다. 날짜는 항상 쌍으로 입력해야 하며(`startDate`만 또는 `endDate`만 입력 불가), `startDate`는 `endDate`보다 늦으면 안 된다 (당일치기 허용). 기간이 줄어드는 경우 새 기간 밖(`dayOffset >= 새 총 일수`)의 일정은 자동으로 소프트 삭제된다. 프론트에서 삭제 예정 일정이 있을 때 사전 안내 모달을 노출한다.
- **멤버 역할 라벨**: `journey_member_role_labels` 테이블로 분리 관리한다. 기본 역할(총무·일정 담당 등) 5종과 직접 입력(`CUSTOM`) 중 선택하며 멤버당 최대 5개까지 복수 지정 가능하다. 지정·수정·해제는 호스트만 가능하며, 본인 포함 전 멤버에게 지정할 수 있다. `PUT replace-all` 방식으로 전체 교체하며 빈 배열 전달 시 전체 해제된다.
- **멤버 자기 강퇴 차단**: 호스트가 자기 자신을 내보내려 하면 전용 에러코드(`JOURNEY_MEMBER_CANNOT_REMOVE_SELF`)를 반환하며 DB 조회 없이 즉시 차단한다.
- **갤러리 통합조회**: 여정 내 게시글 이미지를 최신 게시글 순(`createdAt DESC, id DESC`), 같은 게시글 내에서는 `sort_order` 오름차순으로 통합 조회한다. 3열 격자 UI에 맞게 이미지 단위로 평탄화해서 반환하며, size는 이미지 수 기준이다. 커서는 `(createdAt, journeyPostId, sortOrder)` 복합 커서를 사용한다. `post.createdAt < cursorCreatedAt OR (post.createdAt = cursorCreatedAt AND post.id < cursorPostId) OR (post.createdAt = cursorCreatedAt AND post.id = cursorPostId AND img.sortOrder > cursorSortOrder)` 조건으로 게시글 중간에서 잘려도 다음 페이지에서 정확히 이어받는다. 삭제된 게시글 이미지와 이미지 없는 게시글은 결과에서 제외된다.
