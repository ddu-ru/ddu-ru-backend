package com.dduru.gildongmu.home.service;

import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.common.util.JsonConverter;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.home.dto.response.HomePopularDestinationResponse;
import com.dduru.gildongmu.home.repository.HomePopularDestinationRepository;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({QueryDslConfig.class, HomePopularDestinationRepository.class,
        HomePopularDestinationAggregationService.class, HomePopularDestinationIntegrationTest.JsonConfig.class})
class HomePopularDestinationIntegrationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 19, 12, 0);
    @Autowired EntityManager em;
    @Autowired HomePopularDestinationAggregationService service;
    @MockitoBean TimeProvider timeProvider;
    private User host;

    @TestConfiguration
    static class JsonConfig {
        @Bean JsonConverter jsonConverter() { return new JsonConverter(new ObjectMapper()); }
    }

    @BeforeEach
    void setUp() {
        when(timeProvider.now()).thenReturn(NOW);
        host = User.builder().email("popular@example.com").name("host")
                .oauthId("popular").oauthType(OauthType.KAKAO).build();
        em.persist(host);
    }

    @Test
    void countsAllHistoryIncludingClosedButDisplaysOpenSeparately() {
        var tokyo = destination("JP", "도쿄");
        var busan = destination("KR", "부산");
        post(busan, NOW.minusDays(30), "CLOSED", false, "[]");
        post(busan, NOW.minusDays(10), "CLOSED", false, "[]");
        post(busan, NOW.minusDays(1), "OPEN", false, "[]");
        post(busan, NOW.minusYears(2), "OPEN", false, "[]");
        post(busan, NOW.minusDays(2), "OPEN", true, "[]");
        post(tokyo, NOW.minusDays(1), "OPEN", false, "[]");
        post(tokyo, NOW, "CLOSED", false, "[]");
        var response = service.aggregate();
        assertThat(response.updatedAt()).isEqualTo(NOW);
        assertThat(response.items()).extracting(HomePopularDestinationResponse.Item::destinationId)
                .containsExactly(busan.getId(), tokyo.getId());
        assertThat(response.items().get(0).totalTripCount()).isEqualTo(4);
        assertThat(response.items().get(0).availableTripCount()).isEqualTo(2);
        assertThat(response.items().get(1).totalTripCount()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 4, 5, 6})
    void fillsUpToFiveWithoutRepeatingRankedDestinations(int popularCount) {
        var destinations = defaults();
        for (int i = 0; i < popularCount; i++) {
            post(destinations.get(i), NOW.minusDays(1), "CLOSED", false, "[]");
        }
        var items = service.aggregate().items();
        assertThat(items).hasSize(5);
        assertThat(items).extracting(HomePopularDestinationResponse.Item::destinationId).doesNotHaveDuplicates();
        assertThat(items).extracting(HomePopularDestinationResponse.Item::rank).containsExactly(1, 2, 3, 4, 5);
        assertThat(items.stream().filter(i -> i.source() == HomePopularDestinationResponse.Source.POPULAR).count())
                .isEqualTo(Math.min(popularCount, 5));
        for (int i = 0; i < Math.min(popularCount, 5); i++) {
            assertThat(items.get(i).destinationId()).isEqualTo(destinations.get(i).getId());
        }
        if (popularCount == 0) {
            assertThat(items).extracting(HomePopularDestinationResponse.Item::destinationName)
                    .containsExactly("제주", "부산", "강릉", "후쿠오카", "오사카");
        }
        assertThat(items).filteredOn(i -> i.source() == HomePopularDestinationResponse.Source.DEFAULT)
                .allSatisfy(i -> {
                    assertThat(i.totalTripCount()).isZero();
                    assertThat(i.tags()).isEmpty();
                });
    }

    @Test
    void oldPostsRemainPopularAndFallbackUsesCountryCityThenRemainingIds() {
        var wrongCountry = destination("US", "제주");
        var other = destination("KR", "기타도시");
        var busan = destination("KR", "부산");
        var jeju = destination("KR", "제주");
        post(jeju, NOW.minusDays(31), "OPEN", false, "[\"오래된태그\"]");
        var items = service.aggregate().items();
        assertThat(items).extracting(HomePopularDestinationResponse.Item::destinationId)
                .containsExactly(jeju.getId(), busan.getId(), wrongCountry.getId(), other.getId());
        assertThat(items.get(0).availableTripCount()).isEqualTo(1);
        assertThat(items.get(0).totalTripCount()).isEqualTo(1);
        assertThat(items.get(0).source()).isEqualTo(HomePopularDestinationResponse.Source.POPULAR);
        assertThat(items.get(0).tags()).containsExactly("오래된태그");
        assertThat(items.get(0).imageUrl()).isNull();
    }

    @Test
    void tagsUseAllHistoryCountEachPostOnceAndBreakTiesByString() {
        var busan = destination("KR", "부산");
        post(busan, NOW.minusDays(1), "OPEN", false, """
                [" A ","A","B","C","D"," ",null]
                """);
        post(busan, NOW.minusDays(2), "CLOSED", false, "[\"B\",\"D\"]");
        post(busan, NOW.minusDays(3), "OPEN", false, "[]");
        post(busan, NOW.minusDays(4), "OPEN", false, null);
        post(busan, NOW.minusYears(1), "OPEN", false, "[\"X\"]");
        post(busan, NOW.minusYears(2), "CLOSED", false, "[\"X\"]");
        post(busan, NOW.minusYears(3), "CLOSED", false, "[\"X\"]");
        post(busan, NOW.minusDays(1), "OPEN", true, "[\"X\"]");
        assertThat(service.aggregate().items().get(0).tags()).containsExactly("X", "B", "D");
    }

    @Test
    void emptyMasterDataReturnsEmptyAndDestinationImageIsMapped() {
        assertThat(service.aggregate().items()).isEmpty();
        var destination = Destination.builder().countryCode("KR").countryName("대한민국")
                .city("제주").image("https://example.com/jeju.jpg").build();
        em.persist(destination);
        var item = service.aggregate().items().get(0);
        assertThat(item.imageUrl()).isEqualTo(destination.getImage());
    }

    private List<Destination> defaults() {
        var result = new ArrayList<Destination>();
        // Insert in reverse priority to ensure the fallback is not just ID order.
        String[][] names = {{"VN", "다낭"}, {"TH", "방콕"}, {"JP", "교토"}, {"JP", "도쿄"},
                {"KR", "서울"}, {"JP", "오사카"}, {"JP", "후쿠오카"}, {"KR", "강릉"}, {"KR", "부산"}, {"KR", "제주"}};
        for (var name : names) result.add(destination(name[0], name[1]));
        return result;
    }

    private Destination destination(String code, String city) {
        var destination = Destination.builder().countryCode(code).countryName(code).city(city).build();
        em.persist(destination);
        return destination;
    }

    private void post(Destination destination, LocalDateTime createdAt, String status, boolean deleted, String tags) {
        var post = Post.createPost(host, destination, "여행 동행을 모집합니다", "함께 여행할 동행을 모집하는 테스트 게시글입니다.",
                NOW.toLocalDate().plusDays(1), NOW.toLocalDate().plusDays(3), 4, null,
                Gender.U, true, null, null, null, "[]", CompanionType.FULL);
        em.persist(post);
        em.flush();
        em.createNativeQuery("UPDATE posts SET created_at = :createdAt, status = :status, is_deleted = :deleted, "
                        + "tags = :tags FORMAT JSON WHERE id = :id")
                .setParameter("createdAt", createdAt).setParameter("status", status)
                .setParameter("deleted", deleted).setParameter("tags", tags).setParameter("id", post.getId())
                .executeUpdate();
        em.clear();
    }
}
