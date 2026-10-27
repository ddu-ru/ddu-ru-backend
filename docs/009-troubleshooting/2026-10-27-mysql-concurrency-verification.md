# 2026-10-27 MySQL 동시성 정합성 검증

## 목적과 범위

운영 RDS와 같은 MySQL 8.4.9에서 핵심 서비스의 행 잠금, UNIQUE 충돌, 롤백 결과를 확인한다. 기존 H2 테스트는 빠른 회귀 테스트로 유지하고, 이 문서의 자동화 테스트는 서비스 프록시부터 실제 MySQL까지 검증한다.

- 관련 작업: [#362](https://github.com/ddu-ru/ddu-ru-backend/issues/362)
- 후속 반복/HTTP 실험: [#363](https://github.com/ddu-ru/ddu-ru-backend/issues/363)
- 발견한 슈퍼호스트 deadlock: [#365](https://github.com/ddu-ru/ddu-ru-backend/issues/365)
- 제외: 운영 RDS 부하, 실제 FCM의 정확히 한 번 발송, 운영 코드의 선제 수정

## 실행 환경

| 항목 | 값 |
|---|---|
| DB | Testcontainers `mysql:8.4.9`, InnoDB |
| 격리 수준 | `REPEATABLE-READ` — 테스트에서 서버 변수 확인 |
| 스키마 | 빈 DB에 최신 Flyway 적용 후 Hibernate `ddl-auto=validate` |
| Hikari | 최대 10 connections, connection timeout 30초 |
| 작업 스레드 | 경계 조건당 2개 |
| 시작 동기화 | 모든 작업이 준비 latch에 도달한 뒤 start latch 개방 |
| 작업 timeout | 준비 10초, 요청 완료 30초 |
| fixture | 테스트 스레드 시작 전에 별도 트랜잭션으로 커밋 |
| 최종 검증 | 서비스 트랜잭션 종료 후 새 트랜잭션에서 조회 |

로컬 실행:

```bash
./gradlew test
./gradlew mysqlIntegrationTest
```

첫 명령은 H2 회귀 테스트만 실행한다. 두 번째 명령은 Docker가 필요하며, Docker를 사용할 수 없으면 skip하지 않고 실패한다. PR CI도 두 단계를 별도로 실행한다.

## 결과 분류

공통 실행기는 각 요청의 전체 서비스 호출 시간을 기록하고 결과를 다음처럼 분리한다.

- `SUCCESS`
- `EXPECTED_BUSINESS_CONFLICT`
- `DEADLOCK`: MySQL error code `1213`
- `LOCK_TIMEOUT`: MySQL error code `1205`
- `UNEXPECTED_FAILURE`

경과 시간은 서비스 호출 전체 시간이며 DB lock wait만을 의미하지 않는다. #363에서는 이 값을 평균/p95/최대로 집계하되, 별도 DB 계측이 없으면 lock 대기 시간으로 표기하지 않는다.

## 자동화 시나리오와 불변조건

| 시나리오 | 초기 상태 | 요청/역할 | 허용 결과 | 최종 DB 불변조건 |
|---|---|---|---|---|
| 같은 사용자 중복 신청 | 열린 게시글, 신청 없음 | 동일 신청자 2개 요청 | 성공 1, 중복 신청 1 | 신청 1, `MATCH_APPLIED` 알림 1 |
| 마지막 한 자리 승인 | 정원 2, 호스트만 참여, 서로 다른 PENDING 신청 2 | 호스트가 두 신청 동시 승인 | 성공 1, 모집 정원 충돌 1 | `recruitCount=2`, 승인/여정 멤버/그룹방 게스트/알림 각 1 |
| 신청·승인 재호출 | 최초 요청 커밋 완료 | 동일 신청 또는 승인 재호출 | 중복 신청 또는 잘못된 상태 | 모집 인원과 후속 행 추가 없음 |
| 승인 후속 저장 실패 | 정원 2, PENDING 신청 | 그룹방 초대 단계에서 실패 주입 | 트랜잭션 실패 | 신청 PENDING, `recruitCount=1`, 멤버/그룹방/알림 0 |
| 티켓 1장 동시 적용 | UNUSED 티켓 1, 서로 다른 게시글 2 | 동일 사용자 2개 요청 | 성공 1, 티켓 없음 또는 deadlock 1 | USED 티켓 1, UNUSED 0, 노출 1 |

DB 알림은 원 트랜잭션의 `AFTER_COMMIT` 이벤트와 별도 `REQUIRES_NEW` 저장이 끝난 상태를 검증한다. FCM 서비스는 mock 처리하므로 외부 전송의 정확히 한 번 보장은 이 결과에 포함하지 않는다.

## 잠금 순서와 관찰 결과

참여 흐름의 잠금 순서는 다음과 같다.

```text
신청 생성: Post FOR UPDATE -> Participation INSERT
승인: Participation FOR UPDATE -> Post FOR UPDATE -> JourneyMember -> ChatRoom FOR UPDATE -> ChatRoomMember
티켓 적용: 만료 Exposure UPDATE(post별) -> SuperHostTicket FOR UPDATE -> Ticket UPDATE -> Exposure INSERT
```

MySQL 8.4.9 최초 실행에서 참여 시나리오는 기대한 비즈니스 충돌로 종료되고 최종 불변조건을 유지했다. 티켓 1장의 서로 다른 게시글 동시 적용에서는 한 요청이 성공하고 다른 요청이 error code 1213 deadlock으로 롤백되는 경우가 관찰됐다. 롤백 뒤 티켓과 노출은 각각 1건으로 정합성을 유지했지만, 이를 의도한 오류 응답으로 간주하지 않고 #365에서 원인과 재시도/잠금 순서 대안을 추적한다.

deadlock이 한 실행에서 없다는 사실은 deadlock 발생 시 안전하다는 증거가 아니다. 반대로 이번 테스트는 실제 deadlock을 분류하고 실패 트랜잭션 롤백 뒤 불변조건을 검증한다.

## #363 확장 계약

### Testcontainers 반복 실험

- 동일한 fixture 생성 규칙과 `ConcurrentRequestRunner`를 사용한다.
- 요청 수를 20/50/100으로 바꾸고 각 조건을 최소 5회 반복한다.
- 매 회차 별도 fixture를 커밋하고 성공, 비즈니스 충돌, deadlock, lock timeout, 예상 밖 실패 및 요청 시간을 원시 결과로 저장한다.
- 스레드 수, Hikari 최대 connections, 실제 동시 DB 트랜잭션 수를 구분한다.
- #365 분석 시 MySQL deadlock 로그와 `SHOW ENGINE INNODB STATUS`의 latest deadlock을 함께 보존한다.

### 테스트 서버 HTTP 준비표

| 항목 | 준비/검증 내용 |
|---|---|
| 격리 확인 | 운영 RDS 및 실제 사용자 데이터와 분리된 MySQL 8.4 DB인지 확인 |
| 코드 | 애플리케이션 commit SHA 기록 |
| 사용자 | 호스트 1명과 서로 다른 신청자 100명, 토큰은 결과물에서 제외 |
| 게시글 | 회차마다 정원 2, `recruitCount=1`인 새 게시글 또는 완전 초기화된 fixture |
| 신청 | 신청자별 PENDING 행 사전 커밋 |
| 요청 | 실제 승인 HTTP API, 20/50/100개, 각 최소 5회 |
| 응답 | HTTP 상태/비즈니스 오류별 수, 5xx, p50/p95/p99, 처리량 |
| 서버/DB | Hikari 활성·대기 connections, CPU/메모리, DB lock/deadlock/timeout |
| 사후 검증 | 승인 1, `recruitCount=2`, 활성 여정 멤버와 그룹방 게스트 1, 승인 알림 1 |

토큰, DB 비밀번호, 서버 접속 비밀값은 부하 스크립트와 결과 문서에 저장하지 않는다.
