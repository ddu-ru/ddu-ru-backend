package com.dduru.gildongmu.block.service;

import com.dduru.gildongmu.block.domain.UserBlock;
import com.dduru.gildongmu.block.dto.request.BlockedUserListRequest;
import com.dduru.gildongmu.block.dto.response.BlockedUserListResponse;
import com.dduru.gildongmu.block.dto.response.BlockedUserResponse;
import com.dduru.gildongmu.block.exception.BlockedUserException;
import com.dduru.gildongmu.block.exception.SelfBlockNotAllowedException;
import com.dduru.gildongmu.block.repository.UserBlockRepository;
import com.dduru.gildongmu.profile.dto.response.ProfileImageInfo;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final ProfileImageResolver profileImageResolver;

    public void block(Long blockerUserId, Long blockedUserId) {
        validateNotSelf(blockerUserId, blockedUserId);
        User firstLockedUser = userRepository.getByIdWithLockOrThrow(Math.min(blockerUserId, blockedUserId));
        User secondLockedUser = userRepository.getByIdWithLockOrThrow(Math.max(blockerUserId, blockedUserId));
        if (userBlockRepository.existsByBlocker_IdAndBlockedUser_Id(blockerUserId, blockedUserId)) {
            return;
        }

        User blocker = blockerUserId.equals(firstLockedUser.getId()) ? firstLockedUser : secondLockedUser;
        User blockedUser = blockedUserId.equals(firstLockedUser.getId()) ? firstLockedUser : secondLockedUser;
        userBlockRepository.save(UserBlock.create(blocker, blockedUser));
    }

    public void unblock(Long blockerUserId, Long blockedUserId) {
        userBlockRepository.deleteByBlockerUserIdAndBlockedUserId(blockerUserId, blockedUserId);
    }

    @Transactional(readOnly = true)
    public BlockedUserListResponse getBlockedUsers(Long blockerUserId, BlockedUserListRequest request) {
        int pageSize = request.pageSize();
        List<UserBlock> blocks = userBlockRepository.findBlockedUsers(
                blockerUserId,
                request.cursor(),
                PageRequest.of(0, pageSize + 1)
        );
        boolean hasNext = blocks.size() > pageSize;
        List<UserBlock> pageBlocks = hasNext ? blocks.subList(0, pageSize) : blocks;
        Long nextCursor = hasNext ? pageBlocks.get(pageBlocks.size() - 1).getId() : null;

        List<BlockedUserResponse> users = pageBlocks.stream()
                .map(block -> toResponse(block.getBlockedUser()))
                .toList();
        return BlockedUserListResponse.of(users, hasNext, nextCursor);
    }

    @Transactional(readOnly = true)
    public boolean isBlockedBetween(Long firstUserId, Long secondUserId) {
        if (firstUserId == null || secondUserId == null || firstUserId.equals(secondUserId)) {
            return false;
        }
        return userBlockRepository.existsBetween(firstUserId, secondUserId);
    }

    @Transactional(readOnly = true)
    public void validateNotBlocked(Long firstUserId, Long secondUserId) {
        if (isBlockedBetween(firstUserId, secondUserId)) {
            throw new BlockedUserException();
        }
    }

    private void validateNotSelf(Long blockerUserId, Long blockedUserId) {
        if (blockerUserId.equals(blockedUserId)) {
            throw new SelfBlockNotAllowedException();
        }
    }

    private BlockedUserResponse toResponse(User user) {
        return new BlockedUserResponse(
                user.getId(),
                user.getProfile() != null ? user.getProfile().getNickname() : null,
                user.getProfile() != null ? user.getProfile().getBio() : null,
                ProfileImageInfo.from(user.getProfile(), profileImageResolver)
        );
    }
}
