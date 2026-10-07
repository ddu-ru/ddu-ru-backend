# ADR-006: 홈 API 섹션 단위 로딩 구조

- 상태: Proposed
- 날짜: 2026-06-20
- 결정자: [GitHub @shinheekim](https://github.com/shinheekim)

> TODO: 백엔드/프론트 합의 후 상태를 Accepted로 변경하고, 실제 API PR 링크를 추가합니다.

## 맥락

기존 홈 화면은 `GET /api/v1/home` 한 번으로 사용자 상태, 예정 여행, 인기 여행지, 메이트 추천, 슈퍼호스트와 개인화 여행 목록을 함께 조회했습니다.

홈 데이터가 늘어나면서 다음 문제가 생겼습니다.

- 한 섹션의 실패가 홈 전체 실패로 이어질 수 있습니다.
- 섹션마다 인증 조건, 갱신 주기와 조회 비용이 다릅니다.
- 실시간 데이터와 집계·추천 결과가 하나의 응답에 묶입니다.
- 클라이언트가 섹션별 로딩, 빈 상태와 재시도를 독립적으로 처리하기 어렵습니다.

## 결정

`GET /api/v1/home`은 홈 전체 데이터를 조립하지 않고 초기 구성 정보만 반환합니다.

- 현재 사용자 상태
- 노출 가능한 홈 섹션
- 섹션별 활성 여부와 비활성 사유
- 섹션별 endpoint

실제 데이터는 홈 전용 섹션 API에서 독립적으로 조회합니다.

```http
GET /api/v1/home
GET /api/v1/home/upcoming-trip
GET /api/v1/home/popular-destinations
GET /api/v1/home/mate-recommendations
GET /api/v1/home/super-hosts
GET /api/v1/home/same-destination-trips
GET /api/v1/home/same-age-trips
```

홈 카드의 목적, 필드와 노출 개수는 범용 목록 API와 다르므로 외부 응답 계약은 홈 전용 DTO로 분리합니다.
필요한 경우 내부 도메인 repository와 service는 재사용합니다.

클라이언트는 홈 개요를 먼저 조회한 뒤 활성화된 섹션 API를 병렬로 호출합니다.

```text
GET /api/v1/home
  → 사용자 상태와 활성 섹션 확인
  → 활성 섹션 API 병렬 호출
  → 섹션별 success / empty / failure 처리
```

섹션별 후보 조건, 정렬, 빈 상태, 캐시와 재검증 기준은 ADR에서 관리하지 않고 [홈 섹션 조회 정책](../004-implementation/009-home-section-query-policy.md)에 기록합니다.

## 대안

| 대안 | 제외한 이유 |
|---|---|
| 기존 단일 홈 응답 유지 | 특정 섹션의 지연이나 실패가 홈 전체에 영향을 주고 섹션별 갱신 주기를 적용하기 어려움 |
| 단일 홈 API 내부에서 섹션별 실패만 감싸기 | API 호출 수는 줄지만 홈 서비스가 여러 도메인의 조회와 실패 처리를 계속 조정해야 함 |
| 초기 홈 API도 제거하고 모든 섹션을 완전히 독립 제공 | 사용자 상태에 따른 홈 구성 판단이 클라이언트에 분산됨 |

## 결과

좋은 점:

- 한 섹션의 실패가 다른 섹션을 막지 않습니다.
- 섹션별 skeleton, empty와 retry UI를 독립적으로 제공할 수 있습니다.
- 조회 비용이 큰 추천·집계 API를 별도로 개선하고 캐시할 수 있습니다.
- 서버가 섹션 단위로 쿼리와 성능을 관측할 수 있습니다.

감수할 점:

- 클라이언트의 API 호출 수가 늘어납니다.
- 클라이언트가 섹션별 로딩과 실패 상태를 관리해야 합니다.
- 홈 개요와 실제 섹션 데이터 사이에 짧은 시점 차이가 발생할 수 있습니다.

## 관련 문서

- [홈 섹션 조회 정책](../004-implementation/009-home-section-query-policy.md)
- [여행지 선호와 홈 같은 여행지 동행](../004-implementation/007-travel-preferences-and-home-destination-trips.md)
- [홈 또래 여행 조회](../004-implementation/008-home-same-age-trips.md)
- [홈 메이트 추천 정책](../001-policy/001-home-mate-recommendation-policy.md)
- [홈 메이트 추천 구현 문서](../004-implementation/002-home-mate-recommendation.md)
- [#282 홈 API 섹션 단위 분리](https://github.com/ddu-ru/ddu-ru-backend/issues/282)
- [#283 홈 API 섹션 단위 응답 구조 분리](https://github.com/ddu-ru/ddu-ru-backend/issues/283)
- [#284 홈 섹션 실제 조회 로직 및 캐시 고도화](https://github.com/ddu-ru/ddu-ru-backend/issues/284)
- [#285 홈 API 섹션 분리 ADR 문서화](https://github.com/ddu-ru/ddu-ru-backend/issues/285)
