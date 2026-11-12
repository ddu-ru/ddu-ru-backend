# 002. 데이터 명세

> 이 문서를 보면 DDU-RU Backend의 주요 테이블, 마이그레이션 방식, 공통 시간 필드, 삭제 정책을 파악할 수 있습니다.

---

## 한 줄 요약

운영 DB는 MySQL이며, 운영 프로필에서 Flyway SQL을 적용합니다. 공통 생성/수정 시간은 `BaseTimeEntity`로 관리하지만, 삭제 정책은 도메인별로 다르게 구현되어 있습니다.

---

## 1. 데이터 저장소

| 환경 | 저장소 | 설정 |
|---|---|---|
| local/dev | MySQL 8 Docker, Redis Docker | `docker-compose.yml`, `application-dev.yml` |
| test | H2 in-memory(MySQL mode), Redis localhost 설정 | `application-test.yml` |
| prod | AWS RDS MySQL, Redis container | `application-prod.yml`, `docker-compose.prod.yml` |

---

## 2. 마이그레이션

| 항목 | 내용 |
|---|---|
| 도구 | Flyway |
| 위치 | `src/main/resources/db/migration` |
| 파일명 | `V{버전}__설명.sql` |
| dev | `spring.flyway.enabled=false`, JPA `ddl-auto=update` |
| test | `spring.flyway.enabled=false`, JPA `ddl-auto=create-drop` |
| prod | `spring.flyway.enabled=true`, `validate-on-migrate=true` |

자세한 사용법은 [Flyway 가이드](../006-operations/002-flyway.md)를 확인합니다.

---

## 3. 주요 테이블

| 테이블 | 도메인 | 설명 |
|---|---|---|
| `users` | user/auth | OAuth 사용자 계정, role |
| `profiles` | profile | 닉네임, 성별, 전화번호, 생년월일, 이미지, bio |
| `bg_colors` | profile | 프로필 배경색 |
| `user_onboardings` | onboarding | 온보딩/설문 진행 상태 |
| `surveys` | survey | 설문 답변 |
| `survey_questions` | survey | 설문 문항 |
| `survey_question_options` | survey | 설문 선택지 |
| `survey_activity_tags` | survey | 설문 활동 태그 |
| `travel_tendencies` | survey | 성향 점수와 아바타 타입 |
| `avatar_profiles` | survey | 아바타 프로필 문구/이미지 |
| `destinations` | destination | 여행지 |
| `posts` | post | 공개 여행 모집글 |
| `post_likes` | like | 게시글 좋아요 |
| `participations` | participation | 참여 신청 이력 |
| `user_recommendation_destination_preferences` | recommendation | 홈 추천용 관심 여행지 |
| `user_recommendation_available_dates` | recommendation | 홈 추천용 가능한 여행 기간 |
| `mate_recommendation_batches` | recommendation | 사용자별 KST 일자 추천 묶음 |
| `mate_recommendations` | recommendation | 추천 묶음 내 여행방 추천 결과 |
| `mate_recommendation_passes` | recommendation | 사용자가 패스한 추천 여행방 |
| `journeys` | journey | 승인 이후 여정 워크스페이스 |
| `journey_members` | journey | 여정 멤버십 |
| `journey_posts` | journey | 여정 내부 게시글/공지 |
| `chat_rooms` | chat | 1:1/그룹 채팅방 |
| `chat_room_members` | chat | 채팅방 멤버와 읽음 위치 |
| `chat_messages` | chat | 채팅 메시지 |
| `reports` | report | 게시글 신고 |
| `faqs` | support | FAQ 카테고리, 질문/답변, 노출 여부, 정렬 순서 |
| `inquiries` | support | 문의 작성자, 유형/본문/상태, 단일 답변과 마지막 답변 관리자 |
| `inquiry_images` | support | 문의 첨부 사진 URL과 표시 순서 |
| `super_host_tickets` | superhost | 슈퍼호스트 티켓 |
| `super_host_exposures` | superhost | 슈퍼호스트 노출 |
| `notifications` | notification | 인앱 알림 (이벤트 기반 생성, 커서 페이지네이션 조회) |
| `user_fcm_tokens` | fcm | FCM 푸시 토큰 (사용자·디바이스별 upsert, 만료 시 자동 삭제) |

---

## 4. 공통 시간 필드

대부분의 엔티티는 `BaseTimeEntity`를 상속합니다.

| Java 필드 | DB 컬럼 | 설명 |
|---|---|---|
| `createdAt` | `created_at` | 생성 시각 |
| `modifiedAt` | `modified_at` | 수정 시각 |

`BaseTimeEntity`는 `@MappedSuperclass`와 JPA Auditing을 사용합니다.
문의는 별도로 `answered_at`과 `answer_modified_at`을 저장하며, 답변 수정 시 최초 답변 시각은 유지합니다.
`inquiry_images`는 `BaseTimeEntity`를 상속하지 않습니다.

---

## 5. 삭제 정책

삭제 정책은 도메인별로 다릅니다.

| 도메인 | 방식 | 설명 |
|---|---|---|
| `post` | Soft delete | `is_deleted`, `deleted_at`, `deleted_by` 직접 관리 |
| `journey_posts` | Soft delete | 게시글 삭제 여부와 삭제자 기록 |
| `chat_rooms` | 상태 기반 | `ACTIVE`, `CLOSED`, `DELETED` |
| `inquiries` | 삭제 API 없음 | 문의와 단일 답변을 유지하며 답변 변경 이력은 별도로 저장하지 않음 |
| `inquiry_images` | 물리 삭제 | 문의 수정 시 제거한 첨부 연결을 삭제하며 S3 객체는 삭제하지 않음 |
| 그 외 | 도메인별 상이 | 물리 삭제, 상태 변경, unique row 유지 등 코드 확인 필요 |

---

## 6. 주요 제약/인덱스

- `participations`: `(post_id, user_id)` unique.
- `user_recommendation_destination_preferences`: `(user_id, preference_type, country_code)`, `(user_id, preference_type, destination_id)` unique.
- `user_recommendation_available_dates`: `(user_id, start_date, end_date)` unique.
- `mate_recommendation_batches`: `(user_id, recommendation_date)` unique.
- `mate_recommendations`: `(batch_id, recommendation_rank)`, `(batch_id, post_id)` unique.
- `mate_recommendation_passes`: `(user_id, post_id)` unique.
- `journey_members`: `(journey_id, user_id)` unique.
- `journeys`: `post_id` unique.
- `chat_rooms`: 그룹 채팅방은 `journey_id` unique.
- `chat_messages`: `(room_id, id)` 조회 인덱스가 메시지 커서 조회에 사용됩니다.
- `journey_posts`: `(journey_id, is_deleted, is_notice, created_at, id)` 인덱스가 여정 게시판 조회에 사용됩니다.
- `notifications`: `(recipient_user_id, id)` 인덱스가 커서 페이지네이션 조회에 사용됩니다.
- `user_fcm_tokens`: `uk_token (token)` — 동일 토큰 중복 등록 방지. `uk_user_device (user_id, device_type)` — 사용자+디바이스 타입 기준 upsert 키.
- `faqs`: `(visible, sort_order, id)` 인덱스가 노출 FAQ 정렬 조회에 사용됩니다.
- `inquiries`: `user_id`, `answered_by`는 `users(id)` FK. 본문은 최대 500자, 답변은 최대 5,000자이며 상태는 `WAITING`/`ANSWERED`입니다. `(user_id, id)`, `(status, id)` 인덱스가 본인 문의와 관리자 상태별 조회에 사용됩니다.
- `inquiry_images`: `inquiry_id`는 `inquiries(id)` FK. URL은 최대 2,048자이며 첨부 최대 4장은 도메인에서 보장합니다. `(inquiry_id, sort_order)` 인덱스가 문의별 첨부 순서 조회에 사용됩니다.
