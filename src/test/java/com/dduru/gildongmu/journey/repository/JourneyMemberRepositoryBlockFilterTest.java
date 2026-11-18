package com.dduru.gildongmu.journey.repository;

import com.dduru.gildongmu.block.domain.UserBlock;
import com.dduru.gildongmu.block.repository.UserBlockRepository;
import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.destination.repository.DestinationRepository;
import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.journey.domain.JourneyMember;
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberStatus;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.profile.repository.ProfileRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@DisplayName("공개 게시글 참여자 차단 필터 테스트")
class JourneyMemberRepositoryBlockFilterTest {

    @Autowired private JourneyMemberRepository journeyMemberRepository;
    @Autowired private JourneyRepository journeyRepository;
    @Autowired private UserBlockRepository userBlockRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private DestinationRepository destinationRepository;
    @Autowired private PostRepository postRepository;

    private int sequence;

    @Test
    @DisplayName("조회자와 양방향 차단 관계인 참여자는 공개 목록에서 제외한다")
    void excludesBlockedMembersFromPublicPostParticipants() {
        User viewer = saveUserWithProfile("viewer");
        User author = saveUserWithProfile("author");
        User blockedByViewer = saveUserWithProfile("blocked-by-viewer");
        User blocksViewer = saveUserWithProfile("blocks-viewer");
        User visibleMember = saveUserWithProfile("visible");
        Post post = savePost(author);
        Journey journey = journeyRepository.save(Journey.create(post));
        LocalDateTime joinedAt = LocalDateTime.of(2026, 11, 16, 12, 0);

        journeyMemberRepository.saveAll(List.of(
                JourneyMember.createHost(journey, author, joinedAt),
                JourneyMember.createMember(journey, blockedByViewer, joinedAt.plusMinutes(1)),
                JourneyMember.createMember(journey, blocksViewer, joinedAt.plusMinutes(2)),
                JourneyMember.createMember(journey, visibleMember, joinedAt.plusMinutes(3))
        ));
        userBlockRepository.save(UserBlock.create(viewer, blockedByViewer));
        userBlockRepository.save(UserBlock.create(blocksViewer, viewer));

        List<JourneyMember> visibleMembers = journeyMemberRepository
                .findVisibleByPostIdAndStatusWithMemberProfiles(
                        post.getId(),
                        JourneyMemberStatus.ACTIVE,
                        viewer.getId()
                );

        assertThat(visibleMembers)
                .extracting(member -> member.getUser().getId())
                .containsExactly(author.getId(), visibleMember.getId());
    }

    @Test
    @DisplayName("비로그인 조회에서는 모든 활성 참여자를 반환한다")
    void returnsAllActiveMembersForAnonymousViewer() {
        User author = saveUserWithProfile("anonymous-author");
        User member = saveUserWithProfile("anonymous-member");
        Post post = savePost(author);
        Journey journey = journeyRepository.save(Journey.create(post));
        LocalDateTime joinedAt = LocalDateTime.of(2026, 11, 16, 12, 0);
        journeyMemberRepository.saveAll(List.of(
                JourneyMember.createHost(journey, author, joinedAt),
                JourneyMember.createMember(journey, member, joinedAt.plusMinutes(1))
        ));

        List<JourneyMember> visibleMembers = journeyMemberRepository
                .findVisibleByPostIdAndStatusWithMemberProfiles(
                        post.getId(),
                        JourneyMemberStatus.ACTIVE,
                        null
                );

        assertThat(visibleMembers)
                .extracting(journeyMember -> journeyMember.getUser().getId())
                .containsExactly(author.getId(), member.getId());
    }

    private User saveUserWithProfile(String name) {
        sequence++;
        User user = userRepository.save(User.builder()
                .email(name + sequence + "@example.com")
                .name(name)
                .oauthId("oauth-" + name + sequence)
                .oauthType(OauthType.KAKAO)
                .build());
        Profile profile = new Profile(user);
        profile.setupInitialProfile(Gender.U, null, LocalDate.of(1995, 1, 1));
        profile.updateNickname("사용자" + sequence);
        profileRepository.save(profile);
        return user;
    }

    private Post savePost(User author) {
        Destination destination = destinationRepository.save(Destination.builder()
                .countryCode("KR")
                .countryName("대한민국")
                .city("서울")
                .build());
        return postRepository.save(Post.createPost(
                author,
                destination,
                "공개 참여자 목록 테스트 게시글",
                "공개 게시글 참여자 차단 필터를 검증하기 위한 충분히 긴 본문입니다.",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5),
                5,
                null,
                Gender.U,
                true,
                null,
                null,
                "https://example.com/trip.jpg",
                "[\"도시\"]",
                CompanionType.FULL
        ));
    }
}
