package com.dduru.gildongmu.like.repository;

import com.dduru.gildongmu.block.domain.UserBlock;
import com.dduru.gildongmu.block.repository.UserBlockRepository;
import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.destination.repository.DestinationRepository;
import com.dduru.gildongmu.like.domain.PostLike;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@DisplayName("PostLikeRepository 테스트")
class PostLikeRepositoryTest {

    @Autowired private PostLikeRepository postLikeRepository;
    @Autowired private UserBlockRepository userBlockRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DestinationRepository destinationRepository;
    @Autowired private PostRepository postRepository;

    private int sequence;

    @Test
    @DisplayName("게시글 업데이트 알림 수신자에서 양방향 차단 사용자를 제외한다")
    void excludesBlockedUsersFromPostUpdateRecipients() {
        User author = saveUser("author");
        User blocksAuthor = saveUser("blocks-author");
        User blockedByAuthor = saveUser("blocked-by-author");
        User visibleUser = saveUser("visible");
        Post post = savePost(author);

        postLikeRepository.saveAll(List.of(
                PostLike.createPostLike(blocksAuthor, post),
                PostLike.createPostLike(blockedByAuthor, post),
                PostLike.createPostLike(visibleUser, post)
        ));
        userBlockRepository.save(UserBlock.create(blocksAuthor, author));
        userBlockRepository.save(UserBlock.create(author, blockedByAuthor));

        List<Long> recipientIds = postLikeRepository.findPostUpdateNotificationRecipientIds(post.getId());

        assertThat(recipientIds).containsExactly(visibleUser.getId());
    }

    private User saveUser(String name) {
        sequence++;
        return userRepository.save(User.builder()
                .email(name + sequence + "@example.com")
                .name(name)
                .oauthId("oauth-" + name + sequence)
                .oauthType(OauthType.KAKAO)
                .build());
    }

    private Post savePost(User author) {
        Destination destination = destinationRepository.save(Destination.builder()
                .countryCode("KR")
                .countryName("대한민국")
                .city("제주")
                .build());
        return postRepository.save(Post.createPost(
                author,
                destination,
                "함께 여행할 동행을 구합니다",
                "게시글 업데이트 알림 수신자 필터를 검증하기 위한 테스트 본문입니다.",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5),
                4,
                null,
                Gender.U,
                true,
                null,
                null,
                "https://example.com/trip.jpg",
                "[\"힐링\"]",
                CompanionType.FULL
        ));
    }
}
