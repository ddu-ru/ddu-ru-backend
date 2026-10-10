# 005. 알림 구현 문서

> 알림 저장, FCM 푸시, 읽음 처리, 여행 임박 스케줄러의 구현 기준을 정리합니다.
> 제품 정책은 [알림 흐름](../001-policy/flows/005-notification-flow.md)을 참고합니다.

---

## 1. 현재 구현 범위

| 영역 | 구현 |
|---|---|
| 인앱 알림 저장 | `notifications` 테이블, `NotificationEventListener`, `NotificationPersistService` |
| 알림 목록 조회 | `GET /api/v1/notifications`, 최근 한 달, 탭 필터, 커서 기반 조회 |
| 안 읽은 수 조회 | `GET /api/v1/notifications/unread-count`, 전체 및 탭별 집계 |
| 읽음 처리 | 단건 읽음, 전체 읽음 |
| 전체 삭제 | `DELETE /api/v1/notifications`, `deleted_at` 소프트 삭제 |
| 수신 설정 | `users.notification_enabled` 전체 on/off |
| FCM 토큰 | `user_fcm_tokens`, 사용자+디바이스 타입 기준 upsert |
| FCM 푸시 | `FcmPushService` 비동기 발송 |
| 여행 임박 알림 | `TripUpcomingScheduler`, `TripUpcomingNotificationService` |

채팅 메시지는 인앱 알림으로 저장하지 않고 `ChatPushNotificationService`에서 FCM 푸시만 발송합니다.

---

## 2. 이벤트 기반 알림

`NotificationEventListener`는 도메인 이벤트를 `AFTER_COMMIT`에서 처리합니다.
원래 도메인 트랜잭션이 롤백되면 알림도 발송하지 않습니다.

| 이벤트 | 알림 타입 | 리소스 타입 | 리소스 ID |
|---|---|---|---|
| `MatchAppliedEvent` | `MATCH_APPLIED` | `MATCH` | `participationId` |
| `MatchApprovedEvent` | `MATCH_APPROVED` | `JOURNEY` | `journeyId` |
| `JourneyNoticeCreatedEvent` | `JOURNEY_NOTICE` | `JOURNEY_POST` | `journeyPostId` |
| `ScheduleCreatedEvent` | `SCHEDULE_CREATED` | `SCHEDULE` | `scheduleId` |
| `ScheduleUpdatedEvent` | `SCHEDULE_UPDATED` | `SCHEDULE` | `scheduleId` |
| `ScheduleCanceledEvent` | `SCHEDULE_CANCELED` | `SCHEDULE` | `scheduleId` |
| `PostUpdatedEvent` | `POST_UPDATED` | `POST` | `postId` |

수신자 결정:

- 매칭 신청: 모집글 작성자 1명
- 매칭 승인: 신청자 1명
- 여정 공지/일정: 여정 ACTIVE 멤버 중 행위자 제외
- 모집글 수정: 해당 모집글을 좋아요한 사용자

---

## 3. FCM 발송

FCM 데이터 payload는 아래 키를 사용합니다.

```json
{
  "resourceType": "JOURNEY",
  "resourceId": "1"
}
```

`resourceType` 값은 `ResourceType.payloadValue`를 사용합니다.
현재 값은 `JOURNEY`, `JOURNEY_POST`, `SCHEDULE`, `MATCH`, `POST`, `CHAT_ROOM`입니다.

사용자가 `notification_enabled=false`이면 인앱 알림은 저장하지만 FCM 푸시는 보내지 않습니다.
만료되었거나 영구적으로 유효하지 않은 FCM 토큰은 발송 실패 응답을 기준으로 삭제합니다.

---

## 4. 여행 임박 알림

`TripUpcomingScheduler`는 여행 시작 3일 전인 여정을 찾아 `TRIP_UPCOMING` 알림을 생성합니다.
같은 날 같은 여정/수신자에게 이미 발송된 알림은 재발송하지 않습니다.

중복 판정은 `NotificationType.TRIP_UPCOMING`, `ResourceType.JOURNEY`, `journeyId`, 당일 시작 시각을 기준으로 합니다.
사용자가 삭제한 알림도 발송 이력으로 유지하므로 중복 판정에서 제외하지 않습니다.

---

## 5. 알림함 API

JWT 인증이 필요하며 모든 조회 및 변경은 로그인 사용자 기준입니다.
조회 범위는 서버의 현재 시각(KST)에서 `minusMonths(1)`한 시각 이상입니다.
예를 들어 3월 31일 10시에 조회하면 2월 28일 10시부터 표시합니다(평년 기준).
고정 30일 기준이 아니며, 기간이 지난 데이터는 숨길 뿐 물리 삭제하지 않습니다.

### 목록 및 탭

`GET /api/v1/notifications?filter=ALL&size=20`

| filter | 화면 탭 | 포함 타입 |
|---|---|---|
| `ALL` (기본값) | 전체 | 모든 인앱 알림 |
| `UNREAD` | 읽지 않음 | 모든 타입 중 미읽음 |
| `MATCH` | 모집·매칭 | `MATCH_APPLIED`, `MATCH_APPROVED`, `POST_UPDATED` |
| `JOURNEY` | 여정 | `JOURNEY_NOTICE`, `SCHEDULE_CREATED`, `SCHEDULE_UPDATED`, `SCHEDULE_CANCELED`, `TRIP_UPCOMING` |

- 모든 탭에서 삭제된 알림과 한 달보다 오래된 알림은 제외합니다.
- `cursor`는 마지막 알림 ID이며 첫 요청에는 생략합니다. 탭을 바꾸면 커서를 초기화합니다.
- ID 내림차순 정렬이며 `size`는 기본 20, 최대 50입니다.
- 응답은 기존 `notifications`, `nextCursor`, `hasNext`, `size` 구조를 유지합니다.
- 목록 항목에 `title`을 추가합니다. 프론트는 `title`과 `body`를 그대로 표시하고 `createdAt`으로 상대 시간을 계산합니다.
- `resourceType`은 화면 이동용이며 탭 분류 기준이 아닙니다. 예를 들어 `MATCH_APPROVED`의 리소스는 `JOURNEY`이지만 탭은 `MATCH`입니다.
- 채팅 푸시는 기존 동작을 유지하며 메시지 탭과 인앱 채팅 알림 저장은 추가하지 않습니다.

### 미읽음 배지

`GET /api/v1/notifications/unread-count`의 `data` 예시:

```json
{
  "unreadCount": 3,
  "matchUnreadCount": 2,
  "journeyUnreadCount": 1
}
```

선택한 탭이나 페이지와 관계없이 최근 한 달의 미삭제 알림을 집계합니다.
읽음 처리 및 전체 삭제 성공 후 목록과 배지를 다시 조회합니다.

### 읽음 및 삭제

| API | 동작 |
|---|---|
| `PATCH /api/v1/notifications/{notificationId}/read` | 조회 가능한 본인 알림 단건 읽음, 이미 읽었다면 멱등 성공 |
| `PATCH /api/v1/notifications/read-all` | 최근 한 달의 미삭제 알림 전체 읽음, 선택한 탭과 무관 |
| `DELETE /api/v1/notifications` | 최근 한 달의 알림 전체 소프트 삭제, 선택한 탭과 무관, 204 본문 없음 |

삭제되었거나 조회 기간이 지난 알림의 단건 읽음은 `NOTIFICATION_NOT_FOUND`로 처리합니다.
다른 사용자 알림의 단건 읽음은 `NOTIFICATION_ACCESS_DENIED`입니다.
전체 읽음/삭제 시 대상이 없어도 성공하며, 요청 처리 기준 시각 이후에 생성된 알림은 처리 대상에서 제외합니다.

단건 읽음은 조회·소유권·기간 검증 후 조건부 UPDATE로 처리합니다.
UPDATE 시에도 본인·미읽음·미삭제·조회 기간 조건을 확인하고 `read`, `readAt`만 변경합니다.
검증 이후 다른 요청이 먼저 읽음 또는 삭제 처리한 경우에는 변경 없이 성공하며, 최초 읽음 시각과 삭제 상태를 덮어쓰지 않습니다.

### 배포

`V30__notification_soft_delete.sql`로 `notifications.deleted_at`과 조회용 인덱스를 추가합니다.
기존 알림은 `deleted_at = NULL`이므로 삭제되지 않은 상태로 유지됩니다.
기존 푸시 payload 및 알림 생성 로직은 변경하지 않습니다.

## 6. 테스트 기준

- 도메인 이벤트별 알림 타입, 본문, 리소스 타입, 리소스 ID가 맞는지 검증합니다.
- 행위자 제외와 수신자 0명일 때 저장 생략을 검증합니다.
- FCM 수신 설정이 꺼진 사용자는 푸시 대상에서 제외되는지 검증합니다.
- 여행 임박 알림은 같은 날 중복 생성되지 않는지 검증합니다.
- 한 달 경계 포함, 삭제/기간 만료/타인 알림 제외, 타입별 필터 및 미읽음 집계를 검증합니다.
- 커서 알림이 삭제되어도 다음 페이지 조회가 가능한지 검증합니다.
- 전체 읽음/삭제의 사용자·기간 범위 및 멱등성을 검증합니다.
- 삭제한 여행 임박 알림도 중복 발송 검사에 포함되는지 검증합니다.
