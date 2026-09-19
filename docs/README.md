# 문서 인덱스

> 이 폴더는 DDU-RU Backend의 설계, 아키텍처, 운영, 협업 문서를 모아둔 곳입니다.
> Delicious_food_delivery 문서 양식처럼 주제별 폴더와 각 폴더 인덱스로 정리합니다.

---

## 빠르게 찾기

| 상황 | 읽을 문서 |
|---|---|
| 처음 봐요 | [프로젝트 개요](./overview.md) -> [정책 문서](./001-policy/) -> [도메인 명세](./002-design/001-domain.md) |
| 제품 정책 확인 | [정책 문서](./001-policy/) |
| 기능 구현 참고 | [정책 문서](./001-policy/) -> [구현 문서](./004-implementation/) |
| 도메인/권한/상태 흐름 | [도메인 명세](./002-design/001-domain.md) |
| 테이블/마이그레이션 기준 | [데이터 명세](./002-design/002-data.md) -> [Flyway 가이드](./006-operations/002-flyway.md) |
| "파일 어디 두지?" | [패키지 구조 가이드](./005-architecture/001-package-structure.md) |
| 공통 응답/예외/JWT | [공통 기반 사용 가이드](./005-architecture/002-common-foundation.md) |
| 로컬 실행/Docker | [Docker 운영 가이드](./006-operations/001-docker.md) |
| 배포/Blue-Green | [배포 파이프라인](./006-operations/004-deployment.md) |
| PR/커밋/브랜치 | [팀 협업 컨벤션](./007-conventions/001-team.md) |
| Swagger API 명세 작성 | [Swagger API 명세 작성 가이드](./007-conventions/005-swagger-api-docs.md) |
| 날짜/시간 기준 | [날짜/시간 사용 기준](./007-conventions/006-time-provider.md) |
| API 계약/클라이언트 연동 | [API 설계 문서](./003-api/) |
| 의사결정 기록 | [ADR](./008-adr/) |
| ADR 작성 | [ADR 템플릿](./008-adr/000-template.md) |
| 트러블슈팅 작성 | [트러블슈팅 템플릿](./009-troubleshooting/000-template.md) |

---

## 문서 구조

```text
docs/
├── overview.md
├── 001-policy/
│   ├── 001-home-mate-recommendation-policy.md
│   ├── 002-home-recommendation-eligibility.md
│   └── flows/
├── 002-design/
│   ├── 001-domain.md
│   └── 002-data.md
├── 003-api/
│   ├── 001-chat-api-client-guide.md
│   ├── 002-chat-room-list-realtime.md
│   ├── 003-chat-message-retrieve.md
│   ├── 004-chat-read-receipt.md
│   └── 005-fcm-app-integration.md
├── 004-implementation/
│   ├── 001-my-journey.md
│   ├── 002-home-mate-recommendation.md
│   ├── 003-post.md
│   ├── 004-participation.md
│   ├── 005-notification.md
│   ├── 006-chat.md
│   ├── 007-travel-preferences-and-home-destination-trips.md
│   └── 008-home-same-age-trips.md
├── 005-architecture/
│   ├── 001-package-structure.md
│   ├── 002-common-foundation.md
│   └── 003-jpa.md
├── 006-operations/
│   ├── 001-docker.md
│   ├── 002-flyway.md
│   ├── 003-infrastructure.md
│   └── 004-deployment.md
├── 007-conventions/
│   ├── 001-team.md
│   ├── 002-faq.md
│   ├── 003-exception.md
│   ├── 004-validation.md
│   ├── 005-swagger-api-docs.md
│   └── 006-time-provider.md
├── 008-adr/
│   ├── 000-template.md
│   ├── 001-package-by-feature-controller-service-repository.md
│   ├── 002-central-error-code-with-individual-exceptions.md
│   ├── 003-oauth-jwt-refresh-token-redis.md
│   ├── 004-participation-journey-membership-separation.md
│   ├── 005-blue-green-deployment-with-nginx-ecr.md
│   └── 006-home-api-section-based-loading.md
└── 009-troubleshooting/
    └── 000-template.md
```

### 001-policy - 정책/제품 흐름

제품 정책과 기능 흐름을 정리합니다. 개발자가 구현 판단에 참고할 수 있도록 상태 전이, 권한, 저장 기준의 의미, 주요 처리 원칙까지 포함할 수 있습니다.

단, 엔드포인트, 요청/응답 필드, HTTP status, 에러 코드, 페이지네이션 응답처럼 클라이언트와 맞춰야 하는 API 계약은 [API 설계 문서](./003-api/)에 둡니다.
락 순서, 트랜잭션, SQL, repository/service 책임, Redis/STOMP/FCM 처리처럼 내부 구현 세부는 [구현 문서](./004-implementation/)에 둡니다.

- [홈 메이트 추천 정책](./001-policy/001-home-mate-recommendation-policy.md)
- [홈 여행방 추천 공통 노출 조건](./001-policy/002-home-recommendation-eligibility.md)
- [제품/기능 흐름](./001-policy/flows/)

### 002-design - 설계

서비스가 어떤 도메인과 데이터를 다루는지 정의합니다.

- [도메인 명세](./002-design/001-domain.md)
- [데이터 명세](./002-design/002-data.md)

### 003-api - API 설계

Swagger보다 상세한 API 계약과 클라이언트 연동 기준을 정리합니다.
엔드포인트, 요청/응답 필드, HTTP status, 에러 코드, 페이지네이션 응답, 앱/웹 클라이언트가 따라야 하는 호출 규칙을 이곳에 둡니다.

- [API 설계 문서](./003-api/)

### 004-implementation - 구현

AI와 개발자가 기능 구현 시 참고할 내부 구현 세부를 정리합니다.
락 순서, 트랜잭션, SQL, repository/service 책임, 저장 모델, Redis/STOMP/FCM 처리, 테스트 방향처럼 제품 정책이나 API 계약보다 코드 작성에 가까운 내용을 이곳에 둡니다.

- [나의 여정 구현 현황](./004-implementation/001-my-journey.md)
- [홈 메이트 추천 구현 문서](./004-implementation/002-home-mate-recommendation.md)
- [모집글 구현 문서](./004-implementation/003-post.md)
- [참여 신청/그룹 채팅 구현 문서](./004-implementation/004-participation.md)
- [알림 구현 문서](./004-implementation/005-notification.md)
- [채팅 구현 문서](./004-implementation/006-chat.md)
- [여행지 선호와 홈 같은 여행지 동행](./004-implementation/007-travel-preferences-and-home-destination-trips.md)
- [홈 또래 동행 구현 문서](./004-implementation/008-home-same-age-trips.md)

### 005-architecture - 코드 구조

실제 코드가 어떤 패키지 규칙과 공통 기반 위에서 작성되는지 설명합니다.

- [패키지 구조 가이드](./005-architecture/001-package-structure.md)
- [공통 기반 사용 가이드](./005-architecture/002-common-foundation.md)
- [JPA 엔티티 가이드](./005-architecture/003-jpa.md)

### 006-operations - 운영/배포

로컬 실행, 운영 인프라, 배포 파이프라인, DB 마이그레이션을 설명합니다.

- [Docker 운영 가이드](./006-operations/001-docker.md)
- [Flyway 가이드](./006-operations/002-flyway.md)
- [인프라 명세](./006-operations/003-infrastructure.md)
- [배포 파이프라인](./006-operations/004-deployment.md)

### 007-conventions - 협업 규칙

팀이 함께 지키는 브랜치, 커밋, PR, 예외 처리, 자주 묻는 질문을 정리합니다.

- [팀 협업 컨벤션](./007-conventions/001-team.md)
- [팀 FAQ](./007-conventions/002-faq.md)
- [예외 처리 전략](./007-conventions/003-exception.md)
- [데이터 검증 및 문자열 정제 전략](./007-conventions/004-validation.md)
- [Swagger API 명세 작성 가이드](./007-conventions/005-swagger-api-docs.md)
- [날짜/시간 사용 기준](./007-conventions/006-time-provider.md)

### 008-adr - 의사결정 기록

- [ADR](./008-adr/) - 주요 기술/설계 의사결정 기록

### 009-troubleshooting - 트러블슈팅

- [트러블슈팅](./009-troubleshooting/) - 이슈 해결 기록

---

## 업데이트 규칙

- 설계나 운영 방식이 바뀌면 관련 문서를 같은 PR에서 수정합니다.
- 새 문서를 추가하면 해당 폴더 `README.md`와 이 인덱스에 링크를 추가합니다.
- 번호 기반 카테고리의 문서 파일명과 문서 제목에는 읽는 순서대로 숫자를 붙입니다. 예: `001-domain.md`, `# 001. 도메인 명세`
- 카테고리 폴더와 문서 파일명은 외부 온보딩 중요도와 읽는 순서에 맞춰 번호를 붙입니다.
- 새 문서는 같은 폴더의 기존 문서 스타일을 따릅니다.
- 코드에서 확인할 수 없는 결정 배경은 추정하지 않고 `TODO`로 남깁니다.

---

## 템플릿

- [ADR 템플릿](./008-adr/000-template.md)
- [트러블슈팅 템플릿](./009-troubleshooting/000-template.md)
