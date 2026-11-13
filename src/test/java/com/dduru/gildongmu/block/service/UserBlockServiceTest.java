package com.dduru.gildongmu.block.service;

import com.dduru.gildongmu.block.domain.UserBlock;
import com.dduru.gildongmu.block.dto.request.BlockedUserListRequest;
import com.dduru.gildongmu.block.dto.response.BlockedUserListResponse;
import com.dduru.gildongmu.block.dto.response.BlockedUserResponse;
import com.dduru.gildongmu.block.exception.BlockedUserException;
import com.dduru.gildongmu.block.exception.SelfBlockNotAllowedException;
import com.dduru.gildongmu.block.repository.UserBlockRepository;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserBlockService 테스트")
class UserBlockServiceTest {

    @Mock private UserBlockRepository userBlockRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProfileImageResolver profileImageResolver;

    @InjectMocks private UserBlockService userBlockService;

    @Test
    @DisplayName("자기 자신은 차단할 수 없다")
    void rejectsSelfBlock() {
        assertThatThrownBy(() -> userBlockService.block(1L, 1L))
                .isInstanceOf(SelfBlockNotAllowedException.class);

        verify(userBlockRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 차단한 사용자는 다시 저장하지 않는다")
    void blockIsIdempotent() {
        when(userBlockRepository.existsByBlocker_IdAndBlockedUser_Id(1L, 2L)).thenReturn(true);

        userBlockService.block(1L, 2L);

        verify(userBlockRepository, never()).save(any());
        verify(userRepository).getByIdWithLockOrThrow(1L);
        verify(userRepository).getByIdWithLockOrThrow(2L);
    }

    @Test
    @DisplayName("없는 차단 관계를 해제해도 성공으로 처리한다")
    void unblockIsIdempotent() {
        userBlockService.unblock(1L, 2L);

        verify(userBlockRepository).deleteByBlockerUserIdAndBlockedUserId(1L, 2L);
    }

    @Test
    @DisplayName("새 차단 관계를 저장한다")
    void savesBlockRelation() {
        User blocker = user(1L, "blocker");
        User blocked = user(2L, "blocked");
        when(userBlockRepository.existsByBlocker_IdAndBlockedUser_Id(1L, 2L)).thenReturn(false);
        when(userRepository.getByIdWithLockOrThrow(1L)).thenReturn(blocker);
        when(userRepository.getByIdWithLockOrThrow(2L)).thenReturn(blocked);

        userBlockService.block(1L, 2L);

        ArgumentCaptor<UserBlock> captor = ArgumentCaptor.forClass(UserBlock.class);
        verify(userBlockRepository).save(captor.capture());
        assertThat(captor.getValue().getBlocker()).isSameAs(blocker);
        assertThat(captor.getValue().getBlockedUser()).isSameAs(blocked);
    }

    @Test
    @DisplayName("상대방이 나를 차단한 경우도 이용 제한 대상이다")
    void blocksBothDirections() {
        when(userBlockRepository.existsBetween(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> userBlockService.validateNotBlocked(1L, 2L))
                .isInstanceOf(BlockedUserException.class);
    }

    @Test
    @DisplayName("목록은 최근 차단순으로 다음 커서를 반환한다")
    void returnsBlockedUsersWithCursor() {
        UserBlock newest = block(30L, user(3L, "three"));
        UserBlock older = block(20L, user(2L, "two"));
        UserBlock extra = block(10L, user(1L, "one"));
        when(userBlockRepository.findBlockedUsers(anyLong(), any(), any(Pageable.class)))
                .thenReturn(List.of(newest, older, extra));

        BlockedUserListResponse response = userBlockService.getBlockedUsers(100L, new BlockedUserListRequest(null, 2));

        assertThat(response.users()).extracting(BlockedUserResponse::userId).containsExactly(3L, 2L);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(20L);
    }

    private UserBlock block(Long id, User blockedUser) {
        UserBlock userBlock = UserBlock.create(user(100L, "blocker"), blockedUser);
        ReflectionTestUtils.setField(userBlock, "id", id);
        return userBlock;
    }

    private User user(Long id, String name) {
        User user = User.builder()
                .email(name + "@example.com")
                .name(name)
                .oauthId(name)
                .oauthType(OauthType.GOOGLE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
