package com.dduru.gildongmu.block.policy;

import com.dduru.gildongmu.block.exception.BlockedUserException;
import com.dduru.gildongmu.block.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    public void validateInteraction(Long firstUserId, Long secondUserId) {
        if (isBlockedBetween(firstUserId, secondUserId)) {
            throw new BlockedUserException();
        }
    }
}
