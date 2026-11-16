package com.dduru.gildongmu.block.policy;

import com.dduru.gildongmu.block.repository.UserBlockRepository;
import com.dduru.gildongmu.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBlockPolicy {

    private final UserBlockRepository userBlockRepository;

    public boolean isBlockedBetween(Long firstUserId, Long secondUserId) {
        if (firstUserId == null || secondUserId == null || firstUserId.equals(secondUserId)) {
            return false;
        }
        return userBlockRepository.existsBetween(firstUserId, secondUserId);
    }

    public void validateInteraction(
            Long firstUserId,
            Long secondUserId,
            Supplier<? extends BusinessException> notFoundExceptionSupplier
    ) {
        if (isBlockedBetween(firstUserId, secondUserId)) {
            throw notFoundExceptionSupplier.get();
        }
    }
}
