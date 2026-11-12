# 001. 도메인 명세

> 이 문서를 보면 DDU-RU Backend의 주요 도메인, 책임, 관계, 상태 흐름을 파악할 수 있습니다.

---

## 한 줄 요약

여행 모집글(`post`)에서 참여 신청(`participation`)이 승인되면 나의 여정(`journey`) 멤버십과 그룹 채팅(`chat`)으로 이어지는 구조입니다.

---

## 1. 도메인 목록

| 도메인 | 주요 책임 | 주요 엔티티/데이터 |
|---|---|---|
| `auth` | OAuth 로그인, JWT/refresh token 발급, 로그아웃 | Redis refresh token |
| `user` | 사용자 계정, OAuth 식별자, role | `User`, `OauthType`, `Role` |
| `profile` | 닉네임, 성별, 생년월일, 이미지, bio, 배경색 | `Profile`, `BgColor` |
| `onboarding` | 설문 진행 상태 조회 | `UserOnboarding`, `SurveyStatus` |
| `survey` | 여행 성향 설문, 점수 계산, 아바타 매칭 | `Survey`, `TravelTendency`, `AvatarProfile` |
| `destination` | 여행지 조회, 인기 여행지 | `Destination` |
| `post` | 여행 모집글 CRUD, 모집 상태, 정원, 좋아요/조회수 | `Post`, `PostStatus`, `CompanionType` |
| `tag` | 게시글 태그 검증, 인기 태그 | 태그 문자열 |
| `participation` | 참여 신청, 연락중, 승인, 거절, 취소 | `Participation`, `ParticipationStatus` |
| `journey` | 승인 후 워크스페이스, 멤버십, 여정 게시판/공지 | `Journey`, `JourneyMember`, `JourneyPost` |
| `chat` | 1:1/그룹 채팅방, 메시지, 읽음 처리 | `ChatRoom`, `ChatRoomMember`, `ChatMessage` |
| `like` | 게시글 좋아요 토글 | `PostLike` |
| `report` | 게시글 신고 | `Report`, `ReportReason`, `ReportStatus` |
| `support` | FAQ 검색, 본인 문의 등록/전체 기간 조회/답변 전 수정 | `Faq`, `Inquiry`, `InquiryImage`, `InquiryStatus` |
| `superhost` | 슈퍼호스트 티켓, 노출 상태, 스케줄링 | `SuperHostTicket`, `SuperHostExposure` |
| `s3` | 이미지 업로드용 presigned URL 발급 | `S3ImageDirectory` |
| `verification` | 휴대폰 인증번호 발송/검증 | Redis 또는 인증 데이터 |
| `admin` | 관리자 사용자/게시글/신고 조회 및 처리, 문의 조회 및 답변 등록/수정 | admin controller/service |
| `home` | 홈 화면 데이터 조합 | `HomeResponse` |
| `notification` | 도메인 이벤트 기반 인앱 알림 생성·저장, 알림함 조회/읽음 처리 | `Notification`, `NotificationType`, `ResourceType` |
| `fcm` | FCM 디바이스 토큰 등록/삭제, 이벤트 기반 FCM 푸시 발송 | `UserFcmToken`, `DeviceType` |
| `common` | 공통 응답, 예외, 설정, JWT, WebSocket, validation | `ApiResult`, `ErrorCode`, `BaseTimeEntity` |

---

## 2. 핵심 관계

| 관계 | 설명 |
|---|---|
| `User` - `Profile` | 사용자 1명은 프로필 1개를 가집니다. |
| `Profile` - `AvatarProfile`/`BgColor` | 프로필은 설문 결과 아바타와 배경색을 참조합니다. |
| `User` - `Post` | 모집글 작성자가 여행 호스트의 출발점입니다. |
| `Post` - `Destination` | 모집글은 여행지 1개를 참조합니다. |
| `Post` - `Participation` | 사용자는 모집글에 참여 신청을 남깁니다. |
| `Post` - `Journey` | 나의 여정은 공개 모집글과 1:1로 연결됩니다. |
| `Journey` - `JourneyMember` | 승인 후 실제 협업 멤버십은 `journey_members`에서 관리합니다. |
| `Journey` - `JourneyPost` | 나의 여정 내부 게시글과 공지입니다. |
| `ChatRoom` - `Post` | 1:1 채팅방은 모집글 컨텍스트를 참조합니다. |
| `ChatRoom` - `Journey` | 그룹 채팅방은 나의 여정 컨텍스트를 참조합니다. |
| `ChatMessage` - `ChatRoom` | 메시지는 채팅방에 속합니다. |
| `User` - `Inquiry` | 문의는 작성자와 마지막 답변 관리자를 각각 참조합니다. |
| `Inquiry` - `InquiryImage` | 문의는 표시 순서가 있는 첨부 사진을 최대 4장 가집니다. |

---

## 3. 역할과 권한 축

### 서비스 role

| Role | 설명 |
|---|---|
| `USER` | 일반 사용자 |
| `ADMIN` | `/api/v1/admin/**` 접근 가능 관리자 |

### 여정 멤버 role

| Role | 설명 |
|---|---|
| `HOST` | 모집글 작성자이자 여정 운영자 |
| `MEMBER` | 승인되어 여정에 참여한 멤버 |

`Role.ADMIN`은 서비스 관리자 권한이고, `JourneyMemberRole.HOST`는 특정 여정 내부 권한입니다. 두 권한 축은 분리해서 봅니다.

---

## 4. 주요 상태 흐름

### 모집글

```text
OPEN -> CLOSED
```

- `OPEN`: 참여 신청 가능 상태입니다.
- `CLOSED`: 모집이 마감된 상태입니다.
- 모집 정원이 가득 차거나 명시적으로 마감되면 참여 신청이 제한됩니다.

### 참여 신청

```text
PENDING -> CONTACTING -> APPROVED
        -> REJECTED
```

- `PENDING`: 신청자가 신청한 최초 상태입니다.
- `CONTACTING`: 호스트가 연락 단계로 전환한 상태입니다.
- `APPROVED`: 참여가 승인된 상태입니다.
- `REJECTED`: 참여가 거절된 상태입니다.
- 승인되면 `journey_members`의 `MEMBER` 멤버십과 그룹 채팅 참여 흐름으로 이어집니다.

### 나의 여정 멤버십

```text
ACTIVE -> REMOVED
ACTIVE -> LEFT
```

- `ACTIVE`: 여정에 접근 가능한 멤버입니다.
- `REMOVED`: 호스트 또는 운영 정책에 의해 제거된 상태입니다.
- `LEFT`: 사용자가 직접 나간 상태입니다.

### 채팅방

```text
ACTIVE -> CLOSED -> DELETED
```

- `ACTIVE`: 메시지 전송과 조회가 가능합니다.
- `CLOSED`: 조회/읽음 처리는 가능하지만 메시지 전송은 제한됩니다.
- `DELETED`: 존재하지 않는 방처럼 처리합니다.

### 고객센터 문의

```text
WAITING -> ANSWERED
```

- `WAITING`: 답변 대기 상태로, 작성자가 문의 내용을 수정할 수 있습니다.
- `ANSWERED`: 관리자 답변이 등록된 상태로, 작성자의 문의 수정은 제한됩니다.
- 관리자는 기존 답변을 수정할 수 있으며 상태는 `ANSWERED`로 유지됩니다.

---

## 5. 도메인별 주의점

- `posts`는 공개 모집글, `journeys`는 승인 이후 협업 워크스페이스입니다.
- `participations`는 신청 이력이고, 실제 여정 접근 권한은 `journey_members`를 기준으로 판단합니다.
- 1:1 채팅방은 `post_id`, 그룹 채팅방은 `journey_id`만 가져야 합니다.
- 설문 v2는 4개 성향 축과 활동 태그, 기록 스타일을 사용합니다.
- 관리자 API는 JWT claim만 신뢰하지 않고 DB role을 다시 조회합니다.
- 고객센터 API는 FAQ를 포함해 인증이 필요하며, 일반 사용자는 본인의 문의만 조회/수정할 수 있습니다.
- FAQ는 DB에서 관리하며 노출 중인 항목만 조회합니다. 문의 수정과 답변 저장은 동일 문의의 비관적 쓰기 잠금으로 순차 처리합니다.
- 고객센터 답변 등록 시 인앱 알림과 FCM 푸시는 아직 발송하지 않습니다.

---

## 관련 문서 위치

도메인별 세부 정책, 흐름, API, 구현 참고는 아래 문서 카테고리에서 확인합니다.

- `docs/001-policy/`
- `docs/003-api/`
- `docs/004-implementation/`
- `docs/005-architecture/`
