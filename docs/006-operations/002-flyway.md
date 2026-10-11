# 002. DB 마이그레이션 (Flyway)

> 이 문서를 보면 Flyway SQL을 어디에 어떻게 추가하고, dev/test/prod에서 어떻게 동작하는지 파악할 수 있습니다.

---

## 한 줄 요약

운영 프로필에서만 Flyway가 활성화되어 `src/main/resources/db/migration`의 SQL을 버전 순서대로 MySQL에 적용합니다. dev/test는 Flyway를 끄고 JPA ddl-auto를 사용합니다.

---

## 1. 스크립트 위치와 이름

| 항목 | 값 |
|---|---|
| 위치 | `src/main/resources/db/migration/` |
| 이름 규칙 | `V{버전}__설명.sql` |
| 예시 | `V18__journey_posts.sql` |

버전은 한 번만 적용되며, 적용 이력은 `flyway_schema_history`에 기록됩니다.

---

## 2. 환경별 동작

| 환경 | Flyway | JPA ddl-auto | 비고 |
|---|---:|---|---|
| dev | disabled | `update` | 로컬 개발 편의 우선 |
| test | disabled | `create-drop` | H2 MySQL mode |
| prod | enabled | `${DDL_AUTO}` | `validate-on-migrate=true`, 배포 환경변수로 지정 |

운영에서는 Hibernate보다 Flyway가 먼저 실행됩니다.

---

## 3. 새 마이그레이션 추가 절차

1. `src/main/resources/db/migration`에서 현재 가장 큰 버전 번호를 확인합니다.
2. 다음 번호로 `V{next}__{description}.sql` 파일을 추가합니다.
3. MySQL 문법으로 작성합니다.
4. 운영에 적용될 수 있는 DDL/DML인지 검토합니다.
5. 필요한 경우 로컬 MySQL에 수동 적용하거나 prod 유사 환경에서 검증합니다.
6. PR에 스키마 변경 영향과 rollback 또는 복구 방안을 적습니다.

---

## 4. 주의사항

- 이미 운영 데이터가 있는 테이블에 `NOT NULL` 컬럼을 추가할 때는 기본값, backfill, 단계적 배포를 검토합니다.
- `ALTER TABLE ... DROP COLUMN`은 되돌리기 어렵기 때문에 백업/영향 범위를 확인합니다.
- **MySQL은 `DROP COLUMN IF EXISTS` 문법을 지원하지 않습니다** (MariaDB 전용). MySQL에서는 `DROP COLUMN col_name`만 사용해야 합니다. 선행 마이그레이션(V21 등)이 success=1이면 컬럼 존재가 보장되므로 `IF EXISTS` 없이도 안전합니다.
- seed DML은 중복 실행을 고려해 `INSERT IGNORE` 또는 중복 조건을 검토합니다.
- 운영 `DDL_AUTO`는 가능하면 `validate` 또는 `none`을 사용합니다.
- Flyway checksum이 바뀌므로 이미 적용된 migration 파일은 수정하지 않습니다. 새 버전으로 보정합니다.
- 현재 `V1__profile_and_bg_color.sql` 등 일부 migration은 ddl-auto로 먼저 생성된 운영 스키마를 전제로 합니다. clean DB 재현용 baseline/squash migration을 확정하기 전까지 기존 파일을 직접 수정하지 않습니다.

---

## 5. Flyway 끄기

임시로 끄는 설정:

```yaml
spring:
  flyway:
    enabled: false
```

운영에서 임시로 끌 때는 마이그레이션 미적용 상태와 앱 코드의 스키마 기대값이 어긋날 수 있으므로 신중하게 결정합니다.

---

## 6. 의존성

`build.gradle`:

```gradle
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-mysql'
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:mysql'
```

---

## 7. Clean DB 검증

`src/test/java/com/dduru/gildongmu/migration/FlywayMigrationTest.java`는 Testcontainers MySQL에 `classpath:db/migration` 전체를 적용하는 guard test입니다.

현재는 기존 migration이 운영 스키마를 전제로 하므로 `@Disabled` 상태입니다. baseline 또는 squash migration을 결정한 뒤 아래 순서로 활성화합니다.

1. 현재 운영 스키마를 기준으로 baseline/squash SQL을 작성합니다.
2. 이미 운영에 적용된 migration checksum을 바꾸지 않는 방식인지 검토합니다.
3. 기존 운영 마이그레이션 파일을 수정하지 않는 baseline/squash 적용 방식을 결정합니다.
4. `FlywayMigrationTest`의 `@Disabled`를 제거합니다.
5. CI에서 Testcontainers MySQL migration test를 필수로 실행합니다.


## 8. 버전 중복 검증

`MigrationVersionUniquenessTest`는 DB 연결 없이 classpath의 versioned SQL을 검사합니다.
Flyway 버전 비교 규칙으로 중복을 판별하며 일반 테스트에 포함되므로, test 프로필에서 Flyway를 꺼도 번호 충돌을 검출합니다.
SQL 실행 및 운영 checksum 검증을 대신하지는 않습니다.

V30 충돌은 선호 여행지 순위 migration을 V30으로 유지하고 알림 소프트 삭제 migration을 V32로 이동해 해결했습니다.
#331 기능 브랜치에서 사용하는 V31 인기 여행지 인덱스와 충돌하지 않도록 V32를 사용하며, 알림 SQL의 내용은 변경하지 않았습니다.

운영 적용 이력은 RDS 호스트 DNS 조회 실패로 확인하지 못했습니다. 저장소 번호만 변경했으며 DB와 Flyway 이력은 수정하지 않았습니다.
알림 migration이 이미 V30으로 적용된 환경에는 단순 재배포하면 안 됩니다. 배포 전 `flyway_schema_history`의
version·script·success와 실제 `notifications.deleted_at` 및 인덱스 존재 여부를 확인하고 환경에 맞는 복구안을 정해야 합니다.
미적용 환경에서는 V32로 적용합니다. 이력 삭제나 자동 repair로 우회하지 않습니다.
