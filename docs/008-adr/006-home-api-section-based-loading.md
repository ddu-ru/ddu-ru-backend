# ADR-006: 홈 API 섹션 단위 로딩 구조

- 상태: Proposed
- 날짜: 2026-06-20
- 결정자: [GitHub @shinheekim](https://github.com/shinheekim)

> TODO: 백엔드/프론트 합의 후 상태를 Accepted로 변경하고, 실제 API PR 링크를 추가합니다.

## 맥락

현재 홈 화면은 `GET /api/v1/home` 한 번으로 필요한 데이터를 모두 내려받는 구조입니다. 이 응답에는 사용자 상태, 예정 여행, 인기 여행지, 메이트 추천, 슈퍼호스트, 같은 여행지 여행, 또래 여행이 함께 포함됩니다.

이 방식은 첫 구현이 단순하지만, 홈에 들어가는 데이터가 늘어날수록 다음 문제가 생깁니다.

- 한 섹션 조회가 실패하면 홈 전체 조회가 실패할 수 있습니다.
- 섹션마다 갱신 주기와 조회 비용이 다릅니다.
- 사용자별 추천, 모집글 상태, 참여 인원, pending task count처럼 실시간성이 있는 값이 정적인 값과 같은 응답에 묶입니다.
- 프론트가 섹션별 로딩, 빈 상태, 실패 상태를 독립적으로 보여주기 어렵습니다.

예를 들어 메이트 추천 조회가 일시적으로 실패해도 인기 여행지나 슈퍼호스트 섹션은 보여줄 수 있어야 합니다. 하지만 모든 데이터를 하나의 홈 API에서 조립하면, 서버와 클라이언트 모두 부분 실패를 다루기 어려워집니다.

## 결정

홈 API를 "홈 전체 데이터 조회 API"가 아니라 "홈 초기 구성 API"로 축소합니다.

`GET /api/v1/home`은 홈을 그리기 위해 가장 먼저 필요한 최소 정보만 반환합니다.

- 현재 사용자 상태
- 노출 가능한 홈 섹션 목록
- 섹션별 호출 가능 여부
- 필요한 경우 섹션별 endpoint 정보

실제 섹션 데이터는 섹션별 API에서 독립적으로 조회합니다.

```http
GET /api/v1/home
GET /api/v1/home/upcoming-trip
GET /api/v1/home/popular-destinations
GET /api/v1/home/mate-recommendations
GET /api/v1/home/super-hosts
GET /api/v1/home/same-destination-trips
GET /api/v1/home/same-age-trips
```

인기 여행지와 슈퍼호스트도 기존 범용 API를 재사용하지 않고 홈 전용 API로 제공합니다.
두 섹션은 홈 화면의 카드 노출 목적, 고정 노출 개수, 필요한 필드가 기존 검색/목록 API와 다르기 때문입니다.
내부 조회 로직은 기존 도메인 repository/service를 재사용할 수 있지만 외부 응답 계약은 홈 전용 DTO로 분리합니다.

초기 응답 예시는 다음과 같습니다. 실제 필드명과 endpoint는 구현 PR에서 확정합니다.

```json
{
  "userAccessStatus": "MEMBER_SURVEY_COMPLETED",
  "sections": [
    {
      "key": "UPCOMING_TRIP",
      "enabled": true,
      "endpoint": "/api/v1/home/upcoming-trip"
    },
    {
      "key": "POPULAR_DESTINATIONS",
      "enabled": true,
      "endpoint": "/api/v1/home/popular-destinations"
    },
    {
      "key": "MATE_RECOMMENDATIONS",
      "enabled": true,
      "endpoint": "/api/v1/home/mate-recommendations"
    },
    {
      "key": "SUPER_HOSTS",
      "enabled": true,
      "endpoint": "/api/v1/home/super-hosts"
    },
    {
      "key": "SAME_DESTINATION_TRIPS",
      "enabled": true,
      "endpoint": "/api/v1/home/same-destination-trips"
    },
    {
      "key": "SAME_AGE_TRIPS",
      "enabled": true,
      "endpoint": "/api/v1/home/same-age-trips"
    }
  ]
}
```

프론트는 홈 진입 시 `GET /api/v1/home`을 먼저 호출하고, 응답에 포함된 섹션을 기준으로 각 섹션 API를 병렬 호출합니다.

```text
Home Screen
  -> GET /api/v1/home
      -> userAccessStatus 확인
      -> 노출 가능한 section 확인
      -> section API 병렬 호출
      -> upcomingTrip
      -> popularDestinations
      -> mateRecommendations
      -> superHosts
      -> sameDestinationTrips
      -> sameAgeTrips
```

인기 여행지와 슈퍼호스트 홈 preview 섹션은 화면 고정 노출 정책에 맞춰 항상 5개를 반환하고 별도의 `size` 요청 파라미터를 받지 않습니다.
인기 여행지는 현재 mock 응답으로 제공합니다.
실제 데이터 전환 시에는 `destination_id`별 `OPEN` 상태 여행동행방(`posts`) 개수를 기준으로 정렬합니다.
인기 여행지 응답의 `availableTripCount`는 해당 destination의 `OPEN` 상태 여행동행방 개수입니다.
인기 여행지의 `tags`는 destination별 여행방 생성 시 많이 사용된 태그 상위 3개를 의미하지만, 태그 집계가 아직 구현되지 않았으므로 현재 mock 응답에서는 빈 배열과 일부 태그가 있는 케이스를 함께 제공합니다.
슈퍼호스트도 현재 mock 응답으로 제공합니다.
실제 데이터 전환 시 `tags`는 인기 여행지 태그와 다르게 해당 게시글의 `posts.tags` 값을 반환합니다.
실제 데이터 전환 시 `hasLiked`는 회원 전용 개인화 값으로 계산하고, 비회원이면 `false`를 반환합니다.

### 인기 여행지 구현 메모

현재 인기 여행지 API는 화면 연동과 섹션 분리 계약을 먼저 검증하기 위해 mock 데이터로 구현합니다.
이 단계에서는 repository 집계 쿼리나 Redis cache를 붙이지 않습니다.
아직 태그 집계 기준과 검색 기반 인기 산정 방식이 확정되지 않았고, 실제 집계 쿼리를 먼저 붙이면 임시 기준이 API 운영 정책처럼 굳어질 수 있기 때문입니다.

실제 데이터 기반 구현 시 우선안은 다음과 같습니다.

- `posts.status = OPEN`이고 삭제되지 않은 여행동행방만 집계합니다.
- `destination_id`별 게시글 수를 `availableTripCount`로 계산합니다.
- `availableTripCount DESC`, 동률이면 `destination_id ASC`로 정렬해 5개를 반환합니다.
- `Destination.city`를 `destinationName`, `Destination.image`를 `imageUrl`로 매핑합니다.
- destination별 게시글 tags를 파싱해 등장 빈도 상위 3개를 `tags`로 반환합니다.
- 집계 비용이 커지는 시점에는 Redis cache 또는 배치성 집계 테이블을 도입합니다.

검토했던 대안은 다음과 같습니다.

| 대안 | 판단 |
|---|---|
| 기존 `/api/v1/destinations/popular` 재사용 | 여행지 검색/선택용 응답과 홈 랭킹 카드 응답의 목적과 필드가 달라 제외 |
| 바로 repository 집계 쿼리 구현 | 태그 집계와 향후 검색 기반 인기 기준이 미정이라 임시 정책이 굳어질 수 있어 보류 |
| Redis cache 먼저 적용 | mock 단계에서는 캐시할 실제 데이터가 없고 캐시 무효화 기준도 아직 없어 보류 |
| 별도 집계 테이블 선도입 | 현재 데이터 규모와 요구사항 대비 과하고, 검색 기반 인기 산정 도입 시 설계가 바뀔 수 있어 보류 |

추가 개발 방향은 다음과 같습니다.

- 인기 기준을 여행방 개수에서 검색량/클릭/참여 가능 수/최근 생성 가중치 조합으로 확장할 수 있습니다.
- 태그 집계는 JSON tags 직접 파싱보다 태그 정규화 테이블 또는 집계 테이블을 두는 방향을 우선 검토합니다.
- 집계 응답에는 `updatedAt`을 유지해 프론트가 "방금 전" 같은 갱신 시각 UI를 표현할 수 있게 합니다.
- 인기 여행지 전체보기 화면이 필요해지면 홈 프리뷰 API와 별도 목록 API를 분리합니다.

### 슈퍼호스트 구현 메모

현재 슈퍼호스트 API도 화면 연동과 섹션 분리 계약을 먼저 검증하기 위해 mock 데이터로 구현합니다.
이 단계에서는 슈퍼호스트 노출 repository 조회나 사용자별 좋아요 조회를 붙이지 않습니다.
슈퍼호스트 홈 카드의 최종 필드, 정렬 기준, 개인화 범위가 확정되기 전에는 실제 노출 로직을 홈 API에 결합하지 않는 편이 낫기 때문입니다.

실제 데이터 기반 구현 시 우선안은 다음과 같습니다.

- `SuperHostExposure` 중 `ACTIVE`이고 `endedAt > now`인 노출만 조회합니다.
- 연결된 게시글은 삭제되지 않았고 `PostStatus.OPEN`인 것만 노출합니다.
- 홈 프리뷰는 5개 고정으로 반환합니다.
- 정렬은 `startedAt DESC`, 동률이면 게시글 인기 지표를 보조 기준으로 둡니다.
- `tags`는 해당 게시글의 `posts.tags` JSON 값을 파싱해 반환합니다.
- 로그인 회원이면 노출 게시글 id 목록으로 좋아요 여부를 한 번에 조회해 `hasLiked`를 계산하고, 비회원이면 항상 `false`를 반환합니다.

검토했던 대안은 다음과 같습니다.

| 대안 | 판단 |
|---|---|
| 기존 `/api/v1/posts/super-hosts` 재사용 | 기존 API는 슈퍼호스트 목록용이고 홈 카드의 고정 개수, 필드, 개인화 정책과 다를 수 있어 제외 |
| 바로 `SuperHostExposureRepository` 조회 구현 | 홈 카드 최종 정책 확정 전 실데이터 정렬/필드가 굳어질 수 있어 보류 |
| `hasLiked`를 mock에서도 계산 | mock 데이터는 실제 게시글 id와 좋아요 데이터가 보장되지 않으므로 의미가 없어 제외 |
| 슈퍼호스트 응답 cache 적용 | `hasLiked`가 사용자별 값이고 노출 상태가 동적이라 v1 실제 구현에서도 기본적으로 cache하지 않는 방향 |

추가 개발 방향은 다음과 같습니다.

- 홈용 슈퍼호스트 카드와 전체보기 목록 API를 분리합니다.
- 노출 우선순위는 슈퍼호스트 시작 시각, 마감 임박도, 조회수/좋아요 수 등을 조합할 수 있습니다.
- `hasLiked` 외에도 로그인 회원에게만 필요한 상태가 늘면 공개 응답과 개인화 overlay를 분리하는 방식을 검토합니다.

## 프론트 영향

프론트 변경의 핵심은 "홈 전체 로딩 상태 하나"에서 "섹션별 로딩 상태"로 바꾸는 것입니다.

기존 구조는 대략 다음과 같습니다.

```text
home loading
  -> success: 전체 홈 렌더링
  -> failure: 전체 홈 실패 화면
```

변경 후 구조는 다음과 같습니다.

```text
home shell loading
  -> success: 홈 기본 구조 렌더링

section loading
  -> upcomingTrip success/failure/empty
  -> popularDestinations success/failure/empty
  -> mateRecommendations success/failure/empty
  -> superHosts success/failure/empty
  -> sameDestinationTrips success/failure/empty
  -> sameAgeTrips success/failure/empty
```

이렇게 바꾸면 특정 섹션이 실패해도 다른 섹션은 정상적으로 보여줄 수 있습니다.

예를 들어 추천 섹션만 실패한 경우:

- 홈 화면 자체는 정상 진입합니다.
- 인기 여행지, 슈퍼호스트, 예정 여행은 그대로 보여줍니다.
- 메이트 추천 섹션만 재시도 버튼, 빈 상태, 또는 fallback UI를 노출합니다.

사용자 상태별 섹션 노출도 명확해집니다.

| userAccessStatus | 홈 초기 응답 | 섹션 호출 |
|---|---|---|
| `GUEST` | 비회원에게 노출 가능한 섹션만 반환 | 회원 전용 섹션 호출하지 않음 |
| `MEMBER_SURVEY_REQUIRED` | 설문 전 회원용 섹션 반환 | 추천 섹션은 비활성 또는 호출하지 않음 |
| `MEMBER_SURVEY_COMPLETED` | 개인화 섹션 포함 | 추천, 또래 여행 등 개인화 섹션 호출 |

## 전환 순서

클라이언트 영향을 줄이기 위해 두 단계로 진행합니다.

1. 홈 API 구조 분리
   - `GET /api/v1/home`의 책임을 홈 초기 구성 정보 중심으로 축소합니다.
   - 섹션별 API 계약을 정의합니다.
   - Swagger 문서와 테스트를 먼저 맞춥니다.

2. 섹션별 실제 조회 로직 고도화
   - 예정 여행, 추천, 같은 여행지/또래 여행을 실제 데이터 기반으로 조회합니다.
   - 같은 여행지 여행은 회원의 선호 여행지를 기준으로 하는 개인화 섹션이므로 회원 전용 API로 유지합니다.
   - 인기 여행지는 현재 mock API로 제공하고, 실제 데이터 전환 시 destination별 `OPEN` 여행동행방 개수 기준 조회와 캐시 적용을 검토합니다.
   - 슈퍼호스트도 현재 mock API로 제공하고, 실제 데이터 전환 시 홈 카드용 조회와 로그인 회원의 `hasLiked` 개인화 계산을 적용합니다.
   - 섹션별 성능과 인덱스 필요 여부를 확인합니다.

## 대안

| 대안 | 기각/보류 이유 |
|---|---|
| 기존처럼 `GET /api/v1/home`에서 모든 데이터를 계속 반환 | 특정 섹션 실패가 홈 전체 실패로 이어질 수 있고, 섹션별 갱신 주기와 로딩 상태를 분리하기 어려움 |
| `GET /api/v1/home`은 유지하고 내부에서 섹션별 실패를 감싸서 내려줌 | 프론트 호출 수는 줄지만 홈 API가 계속 여러 도메인의 orchestration 책임을 갖게 됨 |
| 모든 섹션을 완전히 독립 API로만 제공하고 초기 홈 API 제거 | 사용자 상태에 따른 홈 구성 판단이 프론트에 흩어질 수 있음 |

## 결과

좋은 점:

- 홈 화면의 부분 실패 대응이 쉬워집니다.
- 섹션별 skeleton, empty, retry UI를 자연스럽게 만들 수 있습니다.
- 조회 비용이 큰 추천/집계 API를 다른 섹션과 분리해서 운영할 수 있습니다.
- 백엔드는 섹션 단위로 쿼리, 캐시, 성능 개선을 진행할 수 있습니다.

감수할 점:

- 프론트에서 API 호출 수가 늘어납니다.
- 프론트 상태 관리가 홈 전체 단일 상태에서 섹션별 상태로 바뀝니다.
- 초기 전환 시 기존 `GET /api/v1/home` 응답에 의존하던 화면 코드를 조정해야 합니다.

이 트레이드오프는 홈 화면 안정성을 위해 감수할 만합니다.
홈은 여러 성격의 데이터를 모아 보여주는 화면이므로, 하나의 API 성공 여부에 전체 화면을 묶는 것보다 섹션 단위로 점진적으로 렌더링하는 편이 사용자 경험과 장애 격리에 유리합니다.

## 관련 문서

- [#282 [TASK] 홈 API 섹션 단위 분리](https://github.com/ddu-ru/ddu-ru-backend/issues/282)
- [#283 [REFACTOR] 홈 API 섹션 단위 응답 구조 분리](https://github.com/ddu-ru/ddu-ru-backend/issues/283)
- [#284 [FEAT] 홈 섹션 실제 조회 로직 및 캐시 고도화](https://github.com/ddu-ru/ddu-ru-backend/issues/284)
- [#285 [TASK] 홈 API 섹션 분리 ADR 문서화](https://github.com/ddu-ru/ddu-ru-backend/issues/285)


## #329 메이트 추천 실제 데이터 응답 정책

`GET /api/v1/home/mate-recommendations`는 로그인 회원의 KST 당일 추천 묶음을 생성하거나 재사용합니다. 전날 추천으로 대체하지 않으며 현재 노출 조건을 다시 확인해 저장 순위대로 반환합니다. 게시글·호스트 정보는 현재 값, 적합도·추천 이유는 저장값입니다.

- 온보딩·설문 미완료: `SURVEY_REQUIRED`와 빈 목록.
- 당일 묶음 생성 중: `GENERATING`과 빈 목록.
- 생성 완료: `AVAILABLE`과 현재 노출 가능한 카드. 후보 없음 또는 모든 카드 숨김도 `AVAILABLE`과 빈 목록.
- `remainingFreeCount`는 항상 0이며 당일 숨겨진 카드를 대체 생성하지 않습니다.
- 정합성 오류 및 생성·조회 오류는 기존 오류 응답으로 반환합니다. 생성 실패 배치는 다음 추천 호출에서 재시도합니다.

추천 실패를 홈 전체 실패로 확대하거나 빈 성공 응답으로 감추지 않습니다. 클라이언트는 추천 섹션의 실패·재시도를 처리하고 홈 초기 구성 및 다른 섹션을 독립적으로 조회합니다. 서버가 다른 섹션을 대신 호출하거나 이전 추천을 반환하는 fallback은 추가하지 않습니다.
