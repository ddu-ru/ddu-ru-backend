# 011. 사용자 차단 API

차단은 사용자가 직접 설정하는 단방향 관계입니다. 상대가 나를 차단한 경우도 서비스 이용 제한은 동일하게 적용되지만, 내 차단 목록에는 포함되지 않습니다.

## 차단 및 해제

```text
PUT    /api/v1/users/me/blocks/{targetUserId}
DELETE /api/v1/users/me/blocks/{targetUserId}
```

두 요청은 멱등입니다. 이미 차단한 사용자를 다시 차단하거나 이미 해제한 관계를 해제해도 `204`를 반환합니다. 자기 자신을 차단하면 `SELF_BLOCK_NOT_ALLOWED`를 반환합니다.

## 차단 관계의 응답 코드

| 상황 | 응답 | 기준 |
|---|---|---|
| 요청한 사용자·게시글·채팅방 등 리소스가 없음 | `404 Not Found` | 실제 리소스 부재를 의미한다. |
| 존재하는 상대와 차단 관계여서 게시글 상세·1:1 채팅·동행 상호작용을 제한 | 해당 도메인의 `404 Not Found` | 차단 관계와 리소스 존재 여부를 노출하지 않는다. |
| 차단 사용자가 작성한 게시글을 목록·추천에서 조회 | 정상 응답에서 제외 | 목록에서는 개별 오류를 반환하지 않는다. |

차단 관계의 `404`는 실제 리소스 부재와 동일한 도메인 오류 코드·메시지를 사용한다. 게시글은 `POST_NOT_FOUND`, 채팅방은 `CHAT_ROOM_NOT_FOUND`, 참여 신청은 `PARTICIPATION_NOT_FOUND`로 응답하므로 클라이언트는 응답만으로 차단 여부를 구분할 수 없다. 현재 공개 상대 프로필 상세 API는 제공하지 않으며, 도입할 경우에도 동일한 비노출 원칙을 적용한다.

## 목록 조회

```text
GET /api/v1/users/me/blocks?cursor={blockId}&size={1..50}
```

최근 차단순으로 반환하며 기본 `size`는 20입니다.

```json
{
  "status": 200,
  "data": {
    "users": [
      {
        "userId": 12,
        "nickname": "여행자",
        "bio": "맛집 탐방을 좋아해요",
        "profileImage": {
          "type": "AVATAR",
          "url": "https://cdn.example.com/avatar.png",
          "bgColorId": 3
        }
      }
    ],
    "nextCursor": 31,
    "hasNext": true
  }
}
```

`nextCursor`는 차단 관계 ID이며, 사용자 ID가 아닙니다. `hasNext=false`이면 `nextCursor`는 `null`입니다.
