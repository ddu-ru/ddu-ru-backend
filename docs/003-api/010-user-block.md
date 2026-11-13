# 010. 사용자 차단 API

차단은 사용자가 직접 설정하는 단방향 관계입니다. 상대가 나를 차단한 경우도 서비스 이용 제한은 동일하게 적용되지만, 내 차단 목록에는 포함되지 않습니다.

## 차단 및 해제

```text
PUT    /api/v1/users/me/blocks/{targetUserId}
DELETE /api/v1/users/me/blocks/{targetUserId}
```

두 요청은 멱등입니다. 이미 차단한 사용자를 다시 차단하거나 이미 해제한 관계를 해제해도 `204`를 반환합니다. 자기 자신을 차단하면 `SELF_BLOCK_NOT_ALLOWED`를 반환합니다.

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
