package com.dduru.gildongmu.home.repository;

import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.report.domain.Report;
import com.dduru.gildongmu.report.domain.enums.ReportReason;
import com.dduru.gildongmu.superhost.domain.SuperHostExposure;
import com.dduru.gildongmu.superhost.domain.SuperHostTicket;
import com.dduru.gildongmu.superhost.domain.enums.SuperHostTicketSource;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QueryDslConfig.class, HomeSuperHostQueryRepository.class})
@DisplayName("홈 슈퍼호스트 조회 저장소 테스트")
class HomeSuperHostQueryRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 12, 0);
    private static final LocalDate TODAY = NOW.toLocalDate();

    @Autowired
    private EntityManager em;

    @Autowired
    private HomeSuperHostQueryRepository repository;

    private Destination destination;
    private User viewer;
    private int sequence;

    @BeforeEach
    void setUp() {
        destination = Destination.builder()
                .countryCode("KR")
                .countryName("대한민국")
                .city("제주")
                .image("https://example.com/jeju.jpg")
                .build();
        em.persist(destination);
        viewer = user("viewer");
    }

    @Test
    @DisplayName("참여 가능한 활성 노출만 조회하고 본인 글은 포함하며 신고한 글은 제외한다")
    void filtersVisibleSuperHosts() {
        Post ownPost = exposedPost(viewer, NOW.minusHours(1), NOW.plusDays(2));

        Post reported = exposedPost(user("reported"), NOW.minusHours(1), NOW.plusDays(2));
        em.persist(Report.createReport(viewer, reported, ReportReason.values()[0], "신고 사유"));

        exposedPost(user("expired"), NOW.minusDays(2), NOW);
        exposedPost(user("not-started"), NOW.plusMinutes(1), NOW.plusDays(2));
        change(exposedPost(user("closed"), NOW.minusHours(1), NOW.plusDays(2)), "status = 'CLOSED'");
        change(exposedPost(user("full"), NOW.minusHours(1), NOW.plusDays(2)),
                "recruit_count = recruit_capacity");
        change(exposedPost(user("deadline"), NOW.minusHours(1), NOW.plusDays(2)),
                "recruit_deadline = '2026-10-02'");
        change(exposedPost(user("ended-trip"), NOW.minusHours(1), NOW.plusDays(2)),
                "end_date = '2026-10-02'");
        change(exposedPost(user("deleted"), NOW.minusHours(1), NOW.plusDays(2)), "is_deleted = true");

        List<Long> candidateIds = repository.findVisibleCandidatePostIds(NOW, TODAY);

        assertThat(candidateIds).containsExactlyInAnyOrder(ownPost.getId(), reported.getId());
        assertThat(repository.findVisibleSuperHosts(viewer.getId(), NOW, TODAY, candidateIds))
                .extracting("postId")
                .containsExactly(ownPost.getId());
    }

    @Test
    @DisplayName("후보 ID와 선택된 ID의 상세 정보를 분리해 조회한다")
    void retrievesCandidateIdsAndSelectedDetails() {
        List<Long> candidateIds = IntStream.range(0, 6)
                .mapToObj(index -> exposedPost(
                        user("host-" + index),
                        NOW.minusHours(index + 1L),
                        NOW.plusDays(2)
                ).getId())
                .toList();

        assertThat(repository.findVisibleCandidatePostIds(NOW, TODAY))
                .containsExactlyInAnyOrderElementsOf(candidateIds);

        var result = repository.findVisibleSuperHosts(null, NOW, TODAY, candidateIds.subList(0, 5));

        assertThat(result).hasSize(5);
        assertThat(result).extracting("postId")
                .containsExactlyInAnyOrderElementsOf(candidateIds.subList(0, 5));
    }

    @Test
    @DisplayName("노출 가능한 후보가 없으면 빈 목록을 반환한다")
    void returnsEmptyList() {
        assertThat(repository.findVisibleCandidatePostIds(NOW, TODAY)).isEmpty();
        assertThat(repository.findVisibleSuperHosts(viewer.getId(), NOW, TODAY, List.of())).isEmpty();
    }

    private Post exposedPost(User host, LocalDateTime startedAt, LocalDateTime endedAt) {
        Post post = Post.createPost(
                host,
                destination,
                "여행 동행을 모집합니다 " + sequence,
                "함께 여행할 동행을 모집하는 테스트 게시글입니다.",
                TODAY.plusDays(1),
                TODAY.plusDays(3),
                4,
                null,
                Gender.U,
                true,
                null,
                null,
                "https://example.com/trip.jpg",
                "[\"힐링\"]",
                CompanionType.FULL
        );
        sequence++;
        em.persist(post);

        SuperHostTicket ticket = SuperHostTicket.create(host, SuperHostTicketSource.ONBOARDING_SURVEY, 3);
        em.persist(ticket);
        em.persist(SuperHostExposure.create(ticket, host, post, startedAt, endedAt));
        return post;
    }

    private void change(Post post, String assignments) {
        em.flush();
        em.createNativeQuery("UPDATE posts SET " + assignments + " WHERE id = :id")
                .setParameter("id", post.getId())
                .executeUpdate();
        em.clear();
    }

    private User user(String name) {
        User user = User.builder()
                .email(name + "@example.com")
                .name(name)
                .oauthId(name)
                .oauthType(OauthType.KAKAO)
                .build();
        em.persist(user);
        return user;
    }
}
