package com.dduru.gildongmu.block.policy;

import com.dduru.gildongmu.block.exception.BlockedUserException;
import com.dduru.gildongmu.block.repository.UserBlockRepository;
import com.dduru.gildongmu.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserBlockPolicy 테스트")
class UserBlockPolicyTest {

    @Mock private UserBlockRepository userBlockRepository;

    @InjectMocks private UserBlockPolicy userBlockPolicy;

    @Test
    @DisplayName("어느 한쪽이 상대를 차단하면 상호작용을 거절한다")
    void rejectsBlockedInteractionInEitherDirection() {
        when(userBlockRepository.existsBetween(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> userBlockPolicy.validateInteraction(1L, 2L))
                .isInstanceOfSatisfying(BlockedUserException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    @DisplayName("차단 관계가 없으면 상호작용을 허용한다")
    void allowsInteractionWithoutBlock() {
        when(userBlockRepository.existsBetween(1L, 2L)).thenReturn(false);

        userBlockPolicy.validateInteraction(1L, 2L);

        verify(userBlockRepository).existsBetween(1L, 2L);
    }

    @Test
    @DisplayName("비로그인 조회와 자기 자신은 차단 조회를 생략한다")
    void skipsLookupWhenRelationCannotExist() {
        assertThat(userBlockPolicy.isBlockedBetween(null, 2L)).isFalse();
        assertThat(userBlockPolicy.isBlockedBetween(1L, 1L)).isFalse();

        verify(userBlockRepository, never()).existsBetween(1L, 2L);
    }
}
