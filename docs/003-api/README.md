# API 설계 문서

Swagger보다 상세한 설계 배경, 처리 흐름, 테스트 케이스가 필요한 API 문서를 둡니다.

| 문서 | 한 줄 요약 |
|---|---|
| [채팅 API 클라이언트 구현 가이드](./001-chat-api-client-guide.md) | 모바일 채팅 화면 구현을 위한 API 호출 순서, 렌더링 기준, 미지원 API 정리 |
| [채팅방 목록 실시간 갱신](./002-chat-room-list-realtime.md) | 개인 WebSocket queue 기반 채팅방 목록 UPSERT/REMOVE 이벤트 설계 |
| [채팅 메시지 조회 API](./003-chat-message-retrieve.md) | 커서 기반 메시지 조회, roomInfo, unreadCount 설계 |
| [채팅 읽음 처리 API](./004-chat-read-receipt.md) | lastReadMessageId 갱신, READ 이벤트, unreadCount 연동 설계 |
| [FCM 앱 연동 가이드](./005-fcm-app-integration.md) | FCM 토큰 등록, 갱신, 삭제 클라이언트 연동 기준 |
| [휴대폰 인증 앱 연동 가이드](./006-phone-verification.md) | 인증 만료, 재발송 시각·대기 시간과 앱 카운트다운 |

## 작성 기준

- 구현 전 설계 문서는 "권장/계획"과 "현재 구현"을 구분해서 씁니다.
- 구현이 완료되면 DTO, 예외, 테스트 목록을 실제 코드 기준으로 갱신합니다.
- Swagger에 노출되는 계약과 다르면 이 문서를 우선 수정합니다.
- 같은 폴더 안에서는 파일명과 문서 제목에 읽는 순서 숫자를 붙입니다. 예: `003-chat-message-retrieve.md`, `# 003. Chat Message Retrieve API Design`
