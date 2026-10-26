package com.dduru.gildongmu.journey.service;

import com.dduru.gildongmu.chat.service.ChatMessageSendService;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.journey.domain.JourneyPoll;
import com.dduru.gildongmu.journey.domain.JourneyPollOption;
import com.dduru.gildongmu.journey.domain.JourneyPollVote;
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberStatus;
import com.dduru.gildongmu.journey.dto.request.JourneyPollCreateRequest;
import com.dduru.gildongmu.journey.dto.request.JourneyPollOptionCreateRequest;
import com.dduru.gildongmu.journey.dto.request.JourneyPollVoteRequest;
import com.dduru.gildongmu.journey.dto.response.JourneyPollResponse;
import com.dduru.gildongmu.journey.dto.response.JourneyPollSummaryResponse;
import com.dduru.gildongmu.journey.event.JourneyPollUpdatedEvent;
import com.dduru.gildongmu.journey.exception.InvalidJourneyPollException;
import com.dduru.gildongmu.journey.exception.JourneyAccessDeniedException;
import com.dduru.gildongmu.journey.exception.JourneyPollOptionAdditionDeniedException;
import com.dduru.gildongmu.journey.repository.JourneyRepository;
import com.dduru.gildongmu.journey.repository.JourneyMemberRepository;
import com.dduru.gildongmu.journey.repository.JourneyPollRepository;
import com.dduru.gildongmu.journey.repository.JourneyPollOptionRepository;
import com.dduru.gildongmu.journey.repository.JourneyPollVoteRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
// Lock waiters must see the preceding voter's committed selections, not an older membership-query snapshot.
@Transactional(isolation = Isolation.READ_COMMITTED)
public class JourneyPollService {
    private final JourneyRepository journeyRepository;
    private final JourneyMemberRepository journeyMemberRepository;
    private final JourneyPollRepository pollRepository;
    private final JourneyPollOptionRepository optionRepository;
    private final JourneyPollVoteRepository voteRepository;
    private final UserRepository userRepository;
    private final JourneyPollQueryService pollQueryService;
    private final ChatMessageSendService chatMessageSendService;
    private final TimeProvider timeProvider;
    private final ApplicationEventPublisher eventPublisher;

    public JourneyPollResponse createPoll(Long journeyId, Long userId, JourneyPollCreateRequest request) {
        validateJourneyAccess(journeyId, userId);
        List<String> options = normalizeOptions(request.options());
        Journey journey = journeyRepository.getByIdOrThrow(journeyId);
        User author = userRepository.getWithProfileByIdOrThrow(userId);
        JourneyPoll poll = pollRepository.save(JourneyPoll.create(journey, author, request.title(),
                request.allowMultipleChoices(), request.anonymous(), request.allowOptionAddition(),
                request.closesAt(), timeProvider.now()));
        optionRepository.saveAll(IntStream.range(0, options.size())
                .mapToObj(index -> JourneyPollOption.create(poll, options.get(index), index)).toList());
        JourneyPollSummaryResponse publicSummary = pollQueryService.buildSummaries(List.of(poll), null).get(poll.getId());
        chatMessageSendService.publishPoll(poll, publicSummary);
        return pollQueryService.retrievePoll(journeyId, poll.getId(), userId);
    }

    public JourneyPollResponse vote(Long journeyId, Long pollId, Long userId, JourneyPollVoteRequest request) {
        validateJourneyAccess(journeyId, userId);
        // Serialize replacements and option additions on the parent, including a user's first vote.
        JourneyPoll poll = pollRepository.getByIdAndJourneyIdWithLockOrThrow(pollId, journeyId);
        poll.validateOpen(timeProvider.now());
        Set<Long> selectedIds = validateSelections(poll, request.optionIds());
        List<JourneyPollOption> options = optionRepository.findByPollIdOrderByDisplayOrderAsc(pollId);
        List<JourneyPollOption> selected = options.stream().filter(option -> selectedIds.contains(option.getId())).toList();
        if (selected.size() != selectedIds.size()) {
            throw InvalidJourneyPollException.invalidSelection();
        }
        Set<Long> previousIds = new HashSet<>(voteRepository.findSelectionsByPollIdsAndUserId(List.of(pollId), userId)
                .stream().map(selection -> selection.optionId()).toList());
        if (!previousIds.equals(selectedIds)) {
            User user = userRepository.getByIdOrThrow(userId);
            voteRepository.deleteByOptionIdsAndUserId(options.stream().map(JourneyPollOption::getId).toList(), userId);
            voteRepository.saveAll(selected.stream().map(option -> JourneyPollVote.create(option, user)).toList());
            eventPublisher.publishEvent(new JourneyPollUpdatedEvent(journeyId, pollId));
        }
        return pollQueryService.retrievePoll(journeyId, pollId, userId);
    }

    public JourneyPollResponse addOption(Long journeyId, Long pollId, Long userId, JourneyPollOptionCreateRequest request) {
        validateJourneyAccess(journeyId, userId);
        JourneyPoll poll = pollRepository.getByIdAndJourneyIdWithLockOrThrow(pollId, journeyId);
        poll.validateOpen(timeProvider.now());
        if (!poll.isAllowOptionAddition()) {
            throw new JourneyPollOptionAdditionDeniedException();
        }
        String content = JourneyPoll.normalizeText(request.content());
        List<JourneyPollOption> options = optionRepository.findByPollIdOrderByDisplayOrderAsc(pollId);
        if (options.size() >= JourneyPoll.MAX_OPTIONS || options.stream().anyMatch(option -> option.getContent().equals(content))) {
            throw InvalidJourneyPollException.invalidOptions();
        }
        optionRepository.save(JourneyPollOption.create(poll, content, options.size()));
        eventPublisher.publishEvent(new JourneyPollUpdatedEvent(journeyId, pollId));
        return pollQueryService.retrievePoll(journeyId, pollId, userId);
    }

    private static List<String> normalizeOptions(List<String> options) {
        if (options == null || options.size() < JourneyPoll.MIN_OPTIONS || options.size() > JourneyPoll.MAX_OPTIONS) {
            throw InvalidJourneyPollException.invalidOptions();
        }
        List<String> normalized = options.stream().map(JourneyPoll::normalizeText).toList();
        if (new HashSet<>(normalized).size() != normalized.size()) {
            throw InvalidJourneyPollException.invalidOptions();
        }
        return normalized;
    }

    private static Set<Long> validateSelections(JourneyPoll poll, List<Long> optionIds) {
        if (optionIds == null || optionIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw InvalidJourneyPollException.invalidSelection();
        }
        poll.validateSelectionCount(optionIds.size());
        Set<Long> selectedIds = new HashSet<>(optionIds);
        if (selectedIds.size() != optionIds.size()) {
            throw InvalidJourneyPollException.invalidSelection();
        }
        return selectedIds;
    }

    private void validateJourneyAccess(Long journeyId, Long userId) {
        if (!journeyMemberRepository.existsByJourneyIdAndUserIdAndStatus(journeyId, userId, JourneyMemberStatus.ACTIVE)) {
            throw new JourneyAccessDeniedException();
        }
    }
}
