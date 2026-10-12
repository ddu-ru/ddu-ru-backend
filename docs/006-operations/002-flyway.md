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
| 일반 migration | `V{버전}__설명.sql` |
| clean DB baseline | `B32__baseline.sql` |
| 일반 migration 예시 | `V33__add_example_column.sql` |

`B32__baseline.sql`은 V32까지의 최종 상태를 한 번에 만드는 기준점입니다. 이후 스키마 변경은 기존과 동일하게 `V33`, `V34` 순서로 추가합니다. 적용된 파일은 수정하지 않으며, 적용 이력은 `flyway_schema_history`에 기록됩니다.

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

1. `src/main/resources/db/migration`에서 가장 큰 `V` 버전을 확인합니다. 병렬 작업 중이라면 열린 PR에서 사용 중인 버전도 함께 확인합니다.
2. 다음 버전으로 `V{next}__{description}.sql` 파일을 추가합니다. 예를 들어 현재 가장 큰 버전이 V32라면 V33을 추가합니다.
3. MySQL 문법으로 작성합니다.
4. 운영 데이터와 애플리케이션에 미치는 영향을 검토합니다.
5. `FlywayBaselineMigrationTest`와 `MigrationVersionUniquenessTest`로 clean DB 적용 및 버전 충돌 여부를 확인합니다.
6. PR에 스키마 변경 영향과 복구 방안을 작성합니다.

적용 순서는 다음과 같습니다.

- 빈 DB: `B32 → V33 → V34 → ...`
- 기존 DB: `flyway_schema_history`에 기록된 마지막 버전 이후의 V migration부터 적용

`B32__baseline.sql`은 수정하거나 새로 만들지 않습니다. 이후 변경은 모두 V migration으로 추가합니다.

---

## 4. 주의사항

- 이미 운영 데이터가 있는 테이블에 `NOT NULL` 컬럼을 추가할 때는 기본값, backfill, 단계적 배포를 검토합니다.
- `ALTER TABLE ... DROP COLUMN`은 되돌리기 어렵기 때문에 백업/영향 범위를 확인합니다.
- **MySQL은 `DROP COLUMN IF EXISTS` 문법을 지원하지 않습니다** (MariaDB 전용). MySQL에서는 `DROP COLUMN col_name`만 사용해야 합니다. 선행 마이그레이션(V21 등)이 success=1이면 컬럼 존재가 보장되므로 `IF EXISTS` 없이도 안전합니다.
- seed DML은 중복 실행을 고려해 `INSERT IGNORE` 또는 중복 조건을 검토합니다.
- 운영 `DDL_AUTO`는 가능하면 `validate` 또는 `none`을 사용합니다.
- Flyway checksum이 바뀌므로 이미 적용된 migration 파일은 수정하지 않습니다. 새 버전으로 보정합니다.
- `B32__baseline.sql`은 V32 기준 최종 스키마와 기준 데이터를 포함한 clean DB 전용 baseline입니다.
- 기존 DB는 B32를 새로 실행하지 않고 기존 이력을 유지하며, 아직 적용되지 않은 다음 `V` migration부터 실행합니다.

---

## 5. Flyway 활성화 및 비활성화

배포 환경에서는 설정 파일을 수정하지 않고 `FLYWAY_ENABLED` 환경변수로 제어합니다.

```bash
# Flyway 비활성화
export FLYWAY_ENABLED=false

# Flyway 활성화
export FLYWAY_ENABLED=true
```

CD 배포에서는 GitHub Actions의 Repository Variable `FLYWAY_ENABLED` 값을 사용합니다. 변수가 없으면 CD 워크플로는 안전을 위해 `false`를 전달하므로, Flyway를 적용할 배포에서는 해당 변수를 `true`로 설정해야 합니다.

변경된 값은 다음 컨테이너 배포부터 적용됩니다. Flyway를 끈 상태에서는 migration이 실행되지 않으므로, 애플리케이션 코드가 요구하는 스키마가 DB에 이미 존재하는지 확인합니다.

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

## 7. 테스트

- `FlywayBaselineMigrationTest`: MySQL 8 컨테이너의 빈 DB에 B32와 이후 V migration을 순서대로 적용하고, 미적용 migration과 기준 데이터를 확인합니다.
- `MigrationVersionUniquenessTest`: DB 연결 없이 migration 버전 중복을 검사합니다.

새 migration을 추가하면 두 테스트를 실행해 clean DB 적용과 버전 충돌 여부를 확인합니다.
