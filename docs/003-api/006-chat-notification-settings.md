# 006. 채팅방 알림 설정 API

## 1. API 개요

현재 사용자가 참여 중인 1:1/그룹 채팅방의 메시지 푸시 수신 설정을 조회·변경한다.
JWT 인증이 필요하며 사용자 ID는 인증 정보에서 가져온다. 다른 멤버의 설정은 조회하거나 변경할 수 없다.

- 기존·신규 멤버의 기본값은 ON이다. 퇴장·제외 후 재참여하면 ON으로 초기화한다.
- ACTIVE와 CLOSED 방 모두 현재 멤버는 설정을 조회·변경할 수 있다.
- 그룹방은 채팅 멤버이면서 ACTIVE 여정 멤버여야 한다.
- OFF는 TEXT·IMAGE·POLL 메시지 푸시에만 적용한다. SYSTEM 메시지는 기존처럼 푸시하지 않는다.
- 메시지 저장·WebSocket 수신·미읽음 수·읽음 위치·여정 공지/일정 알림은 그대로 유지한다.
- 전체 알림 설정이 OFF이면 방별 ON이어도 푸시하지 않는다. 전체 설정 변경은 방별 저장값을 바꾸지 않는다.

## 2. 조회

```http
GET /api/v1/chat-rooms/{chatRoomId}/notification-settings
Authorization: Bearer {accessToken}
```

성공 응답은 HTTP 200이다.

```json
{
  "status": 200,
  "data": {
    "chatRoomId": 1,
    "enabled": true
  }
}
```

`enabled`는 현재 사용자의 **방별 저장값**이다. 전체 설정, 방 접속 여부, 쿨다운까지 계산한 실제 푸시 가능 여부가 아니다.

## 3. 변경

```http
PATCH /api/v1/chat-rooms/{chatRoomId}/notification-settings
Authorization: Bearer {accessToken}
Content-Type: application/json

{"enabled": false}
```

| 필드 | 타입 | 필수 | 의미 |
|---|---|---|---|
| `enabled` | Boolean | Y | `true`: ON, `false`: OFF. 누락·null 불가 |

성공 응답은 조회와 같은 구조의 HTTP 200이며, 변경 후 저장값을 반환한다.
같은 값을 반복 요청해도 성공한다. 토글 명령이 아니라 최종 상태를 지정하는 API다.

설정 변경은 이후 푸시 대상 조회부터 반영한다. 이미 FCM에 전달된 푸시는 취소하지 않는다.

## 4. 예외

| HTTP | errorCode | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT_VALUE` | 변경 요청의 enabled 누락·null, 잘못된 JSON/값 |
| 401 | `UNAUTHORIZED` | 인증 실패 |
| 403 | `CHAT_ACCESS_DENIED` | 채팅 비멤버 또는 그룹방의 비활성 여정 멤버 |
| 404 | `CHAT_ROOM_NOT_FOUND` | 존재하지 않거나 DELETED인 방 |

## 5. 클라이언트 연동

1. 채팅방 설정 화면 진입 시 조회 API를 호출한다.
2. 사용자가 스위치를 변경하면 원하는 `enabled` 값을 PATCH한다.
3. 성공 응답의 `enabled`를 화면에 반영하고, 실패하면 이전 상태를 유지하거나 재조회한다.
4. 여러 기기에서 변경할 수 있으므로 화면 재진입 시 다시 조회한다. 별도 설정 변경 WebSocket 이벤트는 없다.
5. 전체 알림이 OFF인 경우 방별 ON도 푸시를 받지 못함을 안내한다.

기존 채팅방 목록·메시지 조회 응답 구조는 유지하며, 설정값은 이 API에서 조회한다.

## 관련 문서

- [채팅 구현 문서](../004-implementation/006-chat.md)
- [알림 정책](../001-policy/flows/005-notification-flow.md)
