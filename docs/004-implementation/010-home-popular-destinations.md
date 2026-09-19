# 홈 인기 여행지 실제 집계

관련 이슈: [#331](https://github.com/ddu-ru/ddu-ru-backend/issues/331)
제품 기준: [홈 인기 여행지 정책](../001-policy/003-home-popular-destinations.md)

## API 계약

`GET /api/v1/home/popular-destinations`는 비회원에게도 동일한 결과를 제공합니다. size 파라미터는 없습니다.

| 필드 | 의미 |
|---|---|
| updatedAt | 집계 기준 시각, KST LocalDateTime. 캐시 조회 시 유지 |
| items[].rank | 반환 순서 1~5. DEFAULT 카드의 순위 배지는 표시하지 않음 |
| destinationId / destinationName | 여행지 ID / Destination.city |
| imageUrl | Destination.image. 없으면 null |
| totalTripCount | 전체 기간 누적 생성 수. OPEN/CLOSED 포함, 삭제 제외 |
| availableTripCount | 기간 제한 없이 삭제되지 않은 OPEN 방 수. 개인별 신청 가능 수는 아님 |
| tags | 전체 기간 게시글에서 태그 상위 3개. 없거나 DEFAULT이면 [] |
| source | POPULAR: 실제 인기, DEFAULT: 부족한 자리를 채운 추천 |

기본 제목은 “인기 여행지”이며 updatedAt으로 “14:00 기준” 또는 “20분 전 갱신”을 표시합니다.
모두 DEFAULT이면 프런트는 추천 제목을 사용합니다. items=[]는 여행지 기본 데이터가 없는 경우입니다.
가로 스크롤·배지·제목·이미지 대체 및 동행 목록 연결은 프런트에서 이 계약에 따라 구현합니다.
이미지는 imageUrl 하나로 제공합니다. 중복 thumbnailUrl은 제거하며 source와 totalTripCount는 유지합니다.
기존 여행지 선택 API는 변경하지 않습니다.

## 조회 흐름

1. QueryService에서 전용 Redis 캐시 조회.
2. 미스/Redis 조회 실패/손상된 JSON이면 별도 AggregationService에서 REPEATABLE_READ 읽기 전용 트랜잭션 시작.
3. TimeProvider.now()를 집계 기준 시각으로 한 번만 읽습니다. 생성일 조건 없이 삭제되지 않은 전체 게시글을 지역별로 집계합니다.
4. findRankedDestinationCounts에서 누적 생성 수 DESC, destinationId ASC로 상위 5개의 ID와 누적 방 수를 반환한 뒤, 해당 순서를 유지하며 Destination을 일괄 로딩.
5. 부족한 자리는 국가 코드·도시명 CASE 우선순위와 destinationId ASC로 조회. 이미 선택한 ID 제외.
6. 인기 지역의 누적 방 수는 순위 조회 결과를 재사용하며, 보충한 지역의 누적 방 수는 0으로 처리합니다. 현재 누적 방 수 기준에서는 유효한 방이 있는 모든 지역이 순위에 포함된 뒤에만 보충하므로 별도 집계가 필요하지 않습니다. 최종 지역들의 OPEN 방 수와 실제 인기 지역의 전체 기간 tags는 각각 일괄 조회합니다.
7. 태그 JSON을 파싱해 null·공백·빈 값 및 게시글 내 중복을 제거. 빈도 DESC, Java 문자열 자연 순서 ASC로 3개 선택.
8. 응답 조립 후 DB 트랜잭션 종료. Redis에 전체 응답을 JSON으로 저장.

정상 JSON이더라도 태그 배열로 변환할 수 없는 저장 데이터는 기존 JsonConverter 정책대로 집계 실패 처리합니다.
DB 오류를 잡아 기본 추천으로 바꾸지 않습니다. 모든 지역은 현재 게시글 destination_id 기준이며 편집 전 이력은 추적하지 않습니다.

## 캐시와 운영

- 키: `home:popular-destinations:v1`, TTL: 3600초. 사용자별 키 없음.
- 캐시 히트 시 DB 조회나 TTL 연장 없이 저장된 응답을 그대로 반환.
- TTL 만료 후 첫 요청에서 다시 집계하며 배치, 푸시, 쓰기 시 캐시 무효화는 없음.
- Redis 읽기 실패 또는 손상 데이터: 경고 로그 후 DB 조회.
- Redis 쓰기 실패: 경고 로그 후 정상 집계 결과 반환.
- DB 집계 실패: 기존 전역 예외 처리로 섹션 오류 응답. 다른 섹션은 별도 API 유지.
- 동시 캐시 미스는 중복 집계를 허용합니다. 트래픽 증가 시 분산 갱신 잠금을 검토합니다.
- 별도 집계 테이블은 도입하지 않으며 기존 공통 캐시의 TTL도 변경하지 않습니다.

V31에서 posts에 `(is_deleted, created_at, destination_id)`와 `(destination_id, is_deleted, status)` 인덱스를 추가합니다.
첫 인덱스는 이전 기간 제한 집계를 위해 추가한 인덱스이며, 두 번째는 선택 지역의 열린 방 수와 지역별 조회를 지원합니다.
누적 집계 전환에서는 기존 migration을 변경하지 않습니다.
개발 프로필은 Flyway가 꺼져 있으므로 배포와 별개로 로컬 DB의 V31 적용 여부를 확인해야 합니다.
실행 계획의 실제 인덱스 선택은 데이터 분포에 따라 달라질 수 있습니다.

이전 30일 집계 구현 당시인 2026-09-19 로컬 MySQL 8에서 임시 테이블 1만 건으로 V31을 실행해 검증했습니다.
당시 기간 제한 생성 수 집계는 `idx_posts_deleted_created_destination`, OPEN 방 수 집계는
`idx_posts_destination_deleted_status`를 사용하는 range 조회로 확인했습니다.
실제 개발·운영 테이블에 migration을 적용한 결과는 아닙니다.

## 검증

- JPA 통합 테스트: 수년 전 게시글 포함, CLOSED 포함·삭제 제외, 누적 수와 OPEN 수 분리, 동률 정렬.
- 인기 지역 0/1/4/5/6개, 보충 우선순위, 국가 코드 구분, 누락 기본 지역, ID 보충, 중복 제외, 기본 데이터 부족.
- 태그 반복·공백·null·동률·과거 게시글 포함과 이미지 매핑.
- 서비스 테스트: 순위 집계 수 재사용, 보충 지역의 누적 수 0 처리, 인기 지역 5개일 때 보충 생략.
- 서비스/캐시 테스트: 스냅샷 재사용, 만료 후 재집계, 1시간 TTL, JSON 왕복, 손상 캐시, Redis 읽기·쓰기 실패, DB 실패.
- 컨트롤러 테스트: 비회원 접근 및 추가 필드 직렬화. 프런트 화면 테스트는 해당 클라이언트 저장소에서 수행.

## 검색량 기준으로 전환할 때

현재 순위 선정 쿼리는 여행지 ID와 누적 방 수를 함께 반환해 카드의 totalTripCount에 재사용합니다.
보충 지역의 누적 방 수는 0으로 처리하고, 최종 선택 지역의 availableTripCount는 별도로 조회합니다. source는 방 수가 아니라 순위 선정 결과 포함 여부로 정합니다.
검색량 기준을 도입해도 방 수로 재정렬하거나 방이 없는 인기 지역을 DEFAULT로 바꾸지 않습니다.
현재 누적 방 수 기준에서는 DEFAULT의 방 수가 0이지만, 검색량으로 전환하면 실제 방 수가 존재할 수 있습니다.

검색량 도입 시 순위 선정 쿼리를 교체하고 totalTripCount를 최종 선택된 전체 지역에 대해 별도 조회하도록 변경합니다.
응답 구조는 유지하며, 해당 집계 조건·동률 기준과 캐시 전환 정책을 함께 수정합니다.
기본 지역 보충, 카드 응답 구조, 이미지·방 수·태그 조회는 재사용합니다.
검색 이벤트 수집과 집계 테이블은 이번 구현에 포함하지 않습니다. 도입 전 여행지 선택 후 검색 결과 조회를
검색 1회로 볼지, 반복 검색 제외 기준과 집계 기간을 확정해야 합니다. 자동완성 호출 수는 검색량으로 사용하지 않습니다.
