package com.dduru.gildongmu.participation.service;

import com.dduru.gildongmu.block.service.UserBlockService;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomStatus;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomType;
import com.dduru.gildongmu.chat.dto.query.ChatRoomIdByPostIdQueryResult;
import com.dduru.gildongmu.chat.repository.ChatRoomRepository;
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberStatus;
import com.dduru.gildongmu.journey.dto.query.JourneyMemberStatusQueryResult;
import com.dduru.gildongmu.journey.repository.JourneyMemberRepository;
import com.dduru.gildongmu.participation.domain.Participation;
import com.dduru.gildongmu.participation.dto.request.ParticipationRequest;
import com.dduru.gildongmu.participation.dto.response.ChatRoomIds;
import com.dduru.gildongmu.participation.dto.response.MyParticipationResponse;
import com.dduru.gildongmu.participation.dto.response.ParticipationCreateResponse;
import com.dduru.gildongmu.participation.event.MatchAppliedEvent;
import com.dduru.gildongmu.participation.exception.DuplicateParticipationException;
import com.dduru.gildongmu.participation.exception.ParticipationApplicantAccessDeniedException;
import com.dduru.gildongmu.participation.exception.SelfParticipationNotAllowedException;
import com.dduru.gildongmu.participation.repository.ParticipationRepository;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.dto.response.MyParticipationStatus;
import com.dduru.gildongmu.post.dto.response.ParticipantInfo;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.profile.repository.ProfileRepository;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParticipationApplicantService {

    private final ParticipationRepository participationRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ProfileImageResolver profileImageResolver;
    private final ProfileRepository profileRepository;
    private final JourneyMemberRepository journeyMemberRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserBlockService userBlockService;

    public ParticipationCreateResponse participate(Long userId, Long postId, ParticipationRequest request) {
        Post post = postRepository.getActiveByIdWithLockOrThrow(postId);
        User applicant = userRepository.getByIdOrThrow(userId);

        userBlockService.validateNotBlocked(userId, post.getUser().getId());
        validateParticipationAllowed(post, applicant);

        Participation participation = Participation.createParticipation(post, applicant, request.message());
        Participation saved = saveParticipationOrThrowDuplicate(participation, postId, userId);

        String actorNickname = profileRepository.findByUser_Id(userId)
                .map(p -> p.getNickname())
                .orElse("알 수 없음");
        eventPublisher.publishEvent(new MatchAppliedEvent(
                saved.getId(), userId, post.getUser().getId(), post.getTitle(), actorNickname
        ));

        log.info("참여신청 완료 - participationId: {}, postId: {}, userId: {}", saved.getId(), postId, userId);
        return new ParticipationCreateResponse(saved.getId(), saved.getStatus());
    }

    @Transactional(readOnly = true)
    public List<MyParticipationResponse> retrieveMyApplications(Long userId) {
        List<Participation> participations = participationRepository.findMyApplicationsForVisiblePosts(userId);
        Map<Long, JourneyMemberStatus> journeyMemberStatusesByPostId = findJourneyMemberStatusesByPostId(userId, participations);
        Map<Long, Long> groupRoomIdsByPostId = findGroupRoomIdsByPostId(journeyMemberStatusesByPostId);

        return participations.stream()
                .map(participation -> toApplicationResponse(participation, journeyMemberStatusesByPostId, groupRoomIdsByPostId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParticipantInfo> getParticipantsForPostDetail(Post post, LocalDate today) {
        return journeyMemberRepository
                .findByPostIdAndStatusWithMemberProfiles(post.getId(), JourneyMemberStatus.ACTIVE).stream()
                .map(journeyMember -> ParticipantInfo.from(journeyMember.getUser(), journeyMember.isHost(), profileImageResolver, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public MyParticipationStatus getMyParticipationStatus(Long postId, Long currentUserId, boolean isOwner) {
        if (isOwner || currentUserId == null) {
            return MyParticipationStatus.NONE;
        }
        return participationRepository.findByPostIdAndUserId(postId, currentUserId)
                .map(participation -> toMyParticipationStatus(
                        participation,
                        findJourneyMemberStatusIfApproved(postId, currentUserId, participation)
                ))
                .orElse(MyParticipationStatus.NONE);
    }

    public void cancelMyParticipation(Long userId, Long participationId) {
        Participation participation = participationRepository.getByIdWithLockOrThrow(participationId);
        validateApplicantOwnership(userId, participation);
        participation.validateCancellableByApplicant();

        participationRepository.delete(participation);
        log.info("참여신청 취소(삭제) - participationId: {}, postId: {}, userId: {}", participationId, participation.getPost().getId(), userId);
    }

    private MyParticipationStatus toMyParticipationStatus(
            Participation participation,
            Optional<JourneyMemberStatus> journeyMemberStatus
    ) {
        // 내보내기는 신청 상태가 아니라 현재 여정 멤버십 상태로 응답에만 표현한다.
        if (participation.isApproved() && hasJourneyMemberStatus(journeyMemberStatus, JourneyMemberStatus.REMOVED)) {
            return MyParticipationStatus.REMOVED_BY_HOST;
        }

        return switch (participation.getStatus()) {
            case PENDING -> MyParticipationStatus.PENDING;
            case CONTACTING -> MyParticipationStatus.CONTACTING;
            case APPROVED -> MyParticipationStatus.APPROVED;
            case REJECTED -> MyParticipationStatus.REJECTED;
        };
    }

    private Optional<JourneyMemberStatus> findJourneyMemberStatus(Long postId, Long userId) {
        return journeyMemberRepository.findStatusByPostIdAndUserId(postId, userId);
    }

    private Optional<JourneyMemberStatus> findJourneyMemberStatusIfApproved(
            Long postId,
            Long userId,
            Participation participation
    ) {
        if (!participation.isApproved()) {
            return Optional.empty();
        }
        return findJourneyMemberStatus(postId, userId);
    }

    private static boolean hasJourneyMemberStatus(
            Optional<JourneyMemberStatus> journeyMemberStatus,
            JourneyMemberStatus status
    ) {
        return journeyMemberStatus.filter(status::equals)
                .isPresent();
    }

    private void validateParticipationAllowed(Post post, User applicant) {
        validateNotSelfParticipation(post.getUser().getId(), applicant.getId());
        post.validateIsOpen();
        ensureNoDuplicateApplication(post.getId(), applicant.getId());
    }

    private Map<Long, JourneyMemberStatus> findJourneyMemberStatusesByPostId(Long userId, List<Participation> participations) {
        // 실제 여정 멤버십은 승인 이후에만 생성되므로 APPROVED 신청만 조회한다.
        Set<Long> approvedPostIds = participations.stream()
                .filter(Participation::isApproved)
                .map(participation -> participation.getPost().getId())
                .collect(Collectors.toSet());
        if (approvedPostIds.isEmpty()) {
            return Map.of();
        }

        return journeyMemberRepository.findStatusesByPostIdsAndUserId(approvedPostIds, userId).stream()
                .collect(Collectors.toMap(
                        JourneyMemberStatusQueryResult::postId,
                        JourneyMemberStatusQueryResult::status,
                        (first, second) -> first
                ));
    }

    private Map<Long, Long> findGroupRoomIdsByPostId(Map<Long, JourneyMemberStatus> journeyMemberStatusesByPostId) {
        Set<Long> activePostIds = journeyMemberStatusesByPostId.entrySet().stream()
                .filter(entry -> entry.getValue() == JourneyMemberStatus.ACTIVE)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (activePostIds.isEmpty()) {
            return Map.of();
        }

        return chatRoomRepository.findRoomIdsByJourneyPostIdsAndRoomType(activePostIds, ChatRoomType.GROUP).stream()
                .collect(Collectors.toMap(
                        ChatRoomIdByPostIdQueryResult::postId,
                        ChatRoomIdByPostIdQueryResult::roomId,
                        (first, second) -> first
                ));
    }

    private MyParticipationResponse toApplicationResponse(
            Participation participation,
            Map<Long, JourneyMemberStatus> journeyMemberStatusesByPostId,
            Map<Long, Long> groupRoomIdsByPostId
    ) {
        Long postId = participation.getPost().getId();
        Optional<JourneyMemberStatus> journeyMemberStatus = getJourneyMemberStatusIfApproved(
                participation,
                journeyMemberStatusesByPostId
        );
        ChatRoomIds roomIds = resolveChatRoomIds(participation, journeyMemberStatus, groupRoomIdsByPostId);
        MyParticipationStatus status = toMyParticipationStatus(participation, journeyMemberStatus);
        return MyParticipationResponse.from(participation, status, roomIds.privateRoomId(), roomIds.groupRoomId());
    }

    private static Optional<JourneyMemberStatus> getJourneyMemberStatusIfApproved(
            Participation participation,
            Map<Long, JourneyMemberStatus> journeyMemberStatusesByPostId
    ) {
        if (!participation.isApproved()) {
            return Optional.empty();
        }
        return Optional.ofNullable(journeyMemberStatusesByPostId.get(participation.getPost().getId()));
    }

    private ChatRoomIds resolveChatRoomIds(
            Participation participation,
            Optional<JourneyMemberStatus> journeyMemberStatus,
            Map<Long, Long> groupRoomIdsByPostId
    ) {
        Post post = participation.getPost();
        return switch (participation.getStatus()) {
            case CONTACTING -> new ChatRoomIds(
                    findActivePrivateRoomId(participation.getUser().getId(), post),
                    null
            );
            case APPROVED -> new ChatRoomIds(
                    null,
                    hasJourneyMemberStatus(journeyMemberStatus, JourneyMemberStatus.ACTIVE)
                            ? groupRoomIdsByPostId.get(post.getId())
                            : null
            );
            case PENDING, REJECTED -> new ChatRoomIds(null, null);
        };
    }

    private Long findActivePrivateRoomId(Long applicantUserId, Post post) {
        return chatRoomRepository.findPrivateRoomIdByPostAndUserIds(
                post.getId(),
                ChatRoomType.PRIVATE,
                ChatRoomStatus.ACTIVE,
                applicantUserId,
                post.getUser().getId()
        ).orElse(null);
    }

    private static void validateApplicantOwnership(Long userId, Participation participation) {
        if (!participation.getUser().getId().equals(userId)) {
            throw new ParticipationApplicantAccessDeniedException();
        }
    }

    private static void validateNotSelfParticipation(Long authorId, Long applicantId) {
        if (authorId.equals(applicantId)) {
            throw new SelfParticipationNotAllowedException();
        }
    }

    private void ensureNoDuplicateApplication(Long postId, Long applicantId) {
        if (participationRepository.existsByPostIdAndUserId(postId, applicantId)) {
            throw new DuplicateParticipationException();
        }
    }

    private Participation saveParticipationOrThrowDuplicate(Participation participation, Long postId, Long userId) {
        try {
            return participationRepository.save(participation);
        } catch (DataIntegrityViolationException e) {
            if (participationRepository.existsByPostIdAndUserId(postId, userId)) {
                log.warn("중복 참여신청 동시성 충돌 - postId: {}, userId: {}", postId, userId);
                throw new DuplicateParticipationException();
            }
            throw e;
        }
    }
}
