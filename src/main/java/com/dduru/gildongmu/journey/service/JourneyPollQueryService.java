package com.dduru.gildongmu.journey.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.journey.domain.JourneyPoll;
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberStatus;
import com.dduru.gildongmu.journey.dto.query.JourneyPollOptionCountQueryResult;
import com.dduru.gildongmu.journey.dto.query.JourneyPollSelectionQueryResult;
import com.dduru.gildongmu.journey.dto.query.JourneyPollVoterCountQueryResult;
import com.dduru.gildongmu.journey.dto.request.JourneyPollListRequest;
import com.dduru.gildongmu.journey.dto.response.JourneyPollListResponse;
import com.dduru.gildongmu.journey.dto.response.JourneyPollOptionResponse;
import com.dduru.gildongmu.journey.dto.response.JourneyPollResponse;
import com.dduru.gildongmu.journey.dto.response.JourneyPollSummaryResponse;
import com.dduru.gildongmu.journey.dto.response.JourneyPollUserResponse;
import com.dduru.gildongmu.journey.dto.response.JourneyPollVoterResponse;
import com.dduru.gildongmu.journey.exception.JourneyAccessDeniedException;
import com.dduru.gildongmu.journey.repository.JourneyMemberRepository;
import com.dduru.gildongmu.journey.repository.JourneyPollOptionRepository;
import com.dduru.gildongmu.journey.repository.JourneyPollRepository;
import com.dduru.gildongmu.journey.repository.JourneyPollVoteRepository;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JourneyPollQueryService {
    private final JourneyPollRepository pollRepository;
    private final JourneyPollOptionRepository optionRepository;
    private final JourneyPollVoteRepository voteRepository;
    private final JourneyMemberRepository journeyMemberRepository;
    private final ProfileImageResolver profileImageResolver;
    private final TimeProvider timeProvider;

    public JourneyPollListResponse retrievePolls(Long journeyId, Long userId, JourneyPollListRequest request) {
        validateJourneyAccess(journeyId, userId);
        List<JourneyPoll> polls = pollRepository.findByJourneyIdWithCursor(
                journeyId, request.cursor(), PageRequest.of(0, request.size() + 1));
        boolean hasNext = polls.size() > request.size();
        if (hasNext) polls = polls.subList(0, request.size());
        Map<Long, JourneyPollSummaryResponse> summaries = buildSummaries(polls, userId);
        return JourneyPollListResponse.of(polls.stream().map(poll -> summaries.get(poll.getId())).toList(), hasNext);
    }

    public JourneyPollResponse retrievePoll(Long journeyId, Long pollId, Long userId) {
        validateJourneyAccess(journeyId, userId);
        JourneyPoll poll = pollRepository.getByIdAndJourneyIdOrThrow(pollId, journeyId);
        JourneyPollSummaryResponse summary = buildSummaries(List.of(poll), userId).get(pollId);
        List<JourneyPollVoterResponse> voters = poll.isAnonymous() ? List.of()
                : voteRepository.findByPollIdWithVoter(pollId).stream()
                    .map(vote -> JourneyPollVoterResponse.from(vote, profileImageResolver)).toList();
        return new JourneyPollResponse(summary, JourneyPollUserResponse.from(poll.getAuthor(), profileImageResolver),
                poll.getCreatedAt(), voters);
    }

    // Called only after the owning journey/chat service has checked membership.
    // A null userId builds a public payload without any user's selections.
    public Map<Long, JourneyPollSummaryResponse> buildSummaries(List<JourneyPoll> polls, Long userId) {
        if (polls.isEmpty()) return Map.of();
        List<Long> pollIds = polls.stream().map(JourneyPoll::getId).distinct().toList();
        Map<Long, List<JourneyPollOptionResponse>> optionsByPollId = optionRepository.countVotesByPollIds(pollIds).stream()
                .collect(Collectors.groupingBy(JourneyPollOptionCountQueryResult::pollId,
                        Collectors.mapping(JourneyPollOptionResponse::from, Collectors.toList())));
        Map<Long, Long> voterCounts = voteRepository.countVotersByPollIds(pollIds).stream()
                .collect(Collectors.toMap(JourneyPollVoterCountQueryResult::pollId, JourneyPollVoterCountQueryResult::voterCount));
        Map<Long, List<Long>> selections = userId == null ? Map.of()
                : voteRepository.findSelectionsByPollIdsAndUserId(pollIds, userId).stream()
                    .collect(Collectors.groupingBy(JourneyPollSelectionQueryResult::pollId,
                            Collectors.mapping(JourneyPollSelectionQueryResult::optionId, Collectors.toList())));
        LocalDateTime now = timeProvider.now();
        return polls.stream().collect(Collectors.toMap(JourneyPoll::getId,
                poll -> JourneyPollSummaryResponse.from(poll, now, voterCounts.getOrDefault(poll.getId(), 0L),
                        optionsByPollId.getOrDefault(poll.getId(), List.of()),
                        userId == null ? null : selections.getOrDefault(poll.getId(), List.of())),
                (first, duplicate) -> first));
    }

    private void validateJourneyAccess(Long journeyId, Long userId) {
        if (!journeyMemberRepository.existsByJourneyIdAndUserIdAndStatus(journeyId, userId, JourneyMemberStatus.ACTIVE)) {
            throw new JourneyAccessDeniedException();
        }
    }
}
