package com.dduru.gildongmu.superhost.service;

import com.dduru.gildongmu.auth.exception.UserNotFoundException;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.PostStatus;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.superhost.domain.SuperHostExposure;
import com.dduru.gildongmu.superhost.domain.SuperHostTicket;
import com.dduru.gildongmu.superhost.domain.enums.SuperHostExposureStatus;
import com.dduru.gildongmu.superhost.domain.enums.SuperHostTicketSource;
import com.dduru.gildongmu.superhost.domain.enums.SuperHostTicketStatus;
import com.dduru.gildongmu.superhost.dto.response.MySuperHostStatusResponse;
import com.dduru.gildongmu.superhost.dto.response.SuperHostApplyResponse;
import com.dduru.gildongmu.superhost.dto.response.SuperHostManagePostResponse;
import com.dduru.gildongmu.superhost.exception.SuperHostPostNotApplicableException;
import com.dduru.gildongmu.superhost.exception.SuperHostTicketNotFoundException;
import com.dduru.gildongmu.superhost.repository.SuperHostExposureRepository;
import com.dduru.gildongmu.superhost.repository.SuperHostTicketRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SuperHostService {
    private static final int ONBOARDING_TICKET_COUNT = 3;
    private static final int ONBOARDING_TICKET_BOOST_DAYS = 3;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final SuperHostTicketRepository superHostTicketRepository;
    private final SuperHostExposureRepository superHostExposureRepository;
    private final TimeProvider timeProvider;

    public void grantOnboardingRewardTicket(Long userId) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(UserNotFoundException::new);
        if (superHostTicketRepository.existsByUser_IdAndSource(userId, SuperHostTicketSource.ONBOARDING_SURVEY)) {
            log.info("온보딩 보상 슈퍼호스트 티켓이 이미 지급됨 - userId={}", userId);
            return;
        }

        List<SuperHostTicket> tickets = IntStream.range(0, ONBOARDING_TICKET_COUNT)
                .mapToObj(ignored -> SuperHostTicket.create(
                        user,
                        SuperHostTicketSource.ONBOARDING_SURVEY,
                        ONBOARDING_TICKET_BOOST_DAYS
                ))
                .toList();
        superHostTicketRepository.saveAll(tickets);
        log.info("온보딩 보상 슈퍼호스트 티켓 지급 - userId={}, count={}", userId, tickets.size());
    }

    public SuperHostApplyResponse applyTicketToPost(Long userId, Long postId) {
        LocalDateTime now = timeProvider.now();
        Post post = getApplicablePostOrThrow(postId, userId, now);
        endExpiredExposureByPostId(postId, now);
        SuperHostTicket ticket = getUnusedTicketOrThrow(userId);
        LocalDateTime extensionBase = superHostExposureRepository
                .findLatestActiveEndedAtByPostId(postId, SuperHostExposureStatus.ACTIVE, now)
                .orElse(now);

        ticket.markUsed(now);
        LocalDateTime endsAt = extensionBase.plusDays(ticket.getBoostDurationDays());

        saveExposure(ticket, post, now, endsAt);
        log.info("슈퍼호스트 티켓 사용 완료 - userId={}, postId={}, ticketId={}", userId, postId, ticket.getId());

        int unusedTicketCount = getUnusedTicketCount(userId);
        return SuperHostApplyResponse.of(postId, ticket.getId(), now, endsAt, unusedTicketCount);
    }

    public int endExpiredExposures() {
        return superHostExposureRepository.endExpiredExposures(
                SuperHostExposureStatus.ACTIVE,
                SuperHostExposureStatus.ENDED,
                timeProvider.now()
        );
    }

    @Transactional(readOnly = true)
    public MySuperHostStatusResponse getMyStatus(Long userId) {
        LocalDateTime now = timeProvider.now();
        int unusedCount = getUnusedTicketCount(userId);
        List<Post> manageablePosts = postRepository.findSuperHostManageablePostsByUserId(
                userId,
                PostStatus.OPEN,
                now.toLocalDate()
        );
        Map<Long, LocalDateTime> activeEndsAtByPostId = findActiveSuperHostExposures(
                manageablePosts.stream().map(Post::getId).toList()
        );

        List<SuperHostManagePostResponse> appliedPosts = manageablePosts.stream()
                .filter(post -> activeEndsAtByPostId.containsKey(post.getId()))
                .map(post -> SuperHostManagePostResponse.from(post, activeEndsAtByPostId.get(post.getId())))
                .sorted(Comparator.comparing(SuperHostManagePostResponse::superHostEndsAt))
                .toList();
        List<SuperHostManagePostResponse> availablePosts = manageablePosts.stream()
                .filter(post -> !activeEndsAtByPostId.containsKey(post.getId()))
                .map(post -> SuperHostManagePostResponse.from(post, null))
                .toList();

        return MySuperHostStatusResponse.of(unusedCount, appliedPosts, availablePosts);
    }

    @Transactional(readOnly = true)
    public boolean isAuthorSuperHostForPost(Long postId) {
        return superHostExposureRepository.existsByPost_IdAndStatusAndEndedAtAfter(
                postId, SuperHostExposureStatus.ACTIVE, timeProvider.now()
        );
    }

    @Transactional(readOnly = true)
    public Map<Long, LocalDateTime> findActiveSuperHostExposures(Collection<Long> postIds) {
        if (postIds.isEmpty()) return Map.of();
        return superHostExposureRepository
                .findActivePostIdsWithEndedAt(postIds, SuperHostExposureStatus.ACTIVE, timeProvider.now())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (LocalDateTime) row[1],
                        (current, candidate) -> current.isAfter(candidate) ? current : candidate
                ));
    }

    public void cancelActiveExposureByPostId(Long postId) {
        int cancelledCount = superHostExposureRepository.cancelActiveExposureByPostId(
                postId,
                SuperHostExposureStatus.ACTIVE,
                SuperHostExposureStatus.CANCELLED,
                timeProvider.now()
        );
        if (cancelledCount > 0) {
            log.info("게시글 상태 변경으로 슈퍼호스트 노출 취소 - postId={}, count={}", postId, cancelledCount);
        }
    }

    public void cancelActiveExposureByClosedPosts() {
        int cancelledCount = superHostExposureRepository.cancelActiveExposureByClosedPosts(
                SuperHostExposureStatus.ACTIVE,
                SuperHostExposureStatus.CANCELLED,
                timeProvider.now()
        );
        if (cancelledCount > 0) {
            log.info("자동 마감된 게시글의 슈퍼호스트 노출 취소 - count={}", cancelledCount);
        }
    }

    private Post getApplicablePostOrThrow(Long postId, Long userId, LocalDateTime now) {
        return postRepository.findSuperHostApplicableByIdAndUserIdWithLock(
                        postId,
                        userId,
                        PostStatus.OPEN,
                        now.toLocalDate()
                )
                .orElseThrow(SuperHostPostNotApplicableException::new);
    }

    private void endExpiredExposureByPostId(Long postId, LocalDateTime now) {
        superHostExposureRepository.endExpiredExposureByPostId(
                postId,
                SuperHostExposureStatus.ACTIVE,
                SuperHostExposureStatus.ENDED,
                now
        );
    }

    private SuperHostTicket getUnusedTicketOrThrow(Long userId) {
        return superHostTicketRepository
                .findFirstByUser_IdAndStatusOrderByIdAsc(userId, SuperHostTicketStatus.UNUSED)
                .orElseThrow(SuperHostTicketNotFoundException::new);
    }

    private void saveExposure(SuperHostTicket ticket, Post post, LocalDateTime startedAt, LocalDateTime endedAt) {
        SuperHostExposure exposure = SuperHostExposure.create(ticket, post.getUser(), post, startedAt, endedAt);
        superHostExposureRepository.save(exposure);
    }

    private int getUnusedTicketCount(Long userId) {
        return Math.toIntExact(
                superHostTicketRepository.countByUser_IdAndStatus(userId, SuperHostTicketStatus.UNUSED)
        );
    }

}
