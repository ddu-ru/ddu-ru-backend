package com.dduru.gildongmu.journey.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.journey.domain.*;
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberStatus;
import com.dduru.gildongmu.journey.dto.query.*;
import com.dduru.gildongmu.journey.dto.request.JourneyPollListRequest;
import com.dduru.gildongmu.journey.dto.response.JourneyPollResponse;
import com.dduru.gildongmu.journey.dto.response.JourneyPollSummaryResponse;
import com.dduru.gildongmu.journey.exception.JourneyAccessDeniedException;
import com.dduru.gildongmu.journey.repository.*;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JourneyPollQueryServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 12, 0);
    @Mock private JourneyPollRepository pollRepository;
    @Mock private JourneyPollOptionRepository optionRepository;
    @Mock private JourneyPollVoteRepository voteRepository;
    @Mock private JourneyMemberRepository journeyMemberRepository;
    @Mock private ProfileImageResolver profileImageResolver;
    @Mock private TimeProvider timeProvider;
    @InjectMocks private JourneyPollQueryService service;

    @Test
    void anonymousPollDoesNotQueryOrExposeVoterIdentities() {
        JourneyPoll poll = poll(true, 5L);
        allowDetail(poll);
        when(voteRepository.findSelectionsByPollIdsAndUserId(List.of(5L), 10L))
                .thenReturn(List.of(new JourneyPollSelectionQueryResult(5L, 11L)));
        JourneyPollResponse response = service.retrievePoll(1L, 5L, 10L);
        assertThat(response.voters()).isEmpty();
        assertThat(response.poll().selectedOptionIds()).containsExactly(11L);
        verify(voteRepository, never()).findByPollIdWithVoter(anyLong());
    }

    @Test
    void nonAnonymousPollExposesVotersOnlyInDetail() {
        JourneyPoll poll = poll(false, 5L);
        allowDetail(poll);
        JourneyPollOption option = JourneyPollOption.create(poll, "A", 0);
        ReflectionTestUtils.setField(option, "id", 11L);
        when(voteRepository.findByPollIdWithVoter(5L)).thenReturn(List.of(JourneyPollVote.create(option, poll.getAuthor())));
        JourneyPollResponse response = service.retrievePoll(1L, 5L, 10L);
        assertThat(response.voters()).hasSize(1);
        assertThat(response.voters().get(0).voter().userId()).isEqualTo(10L);
    }

    @Test
    void publicCardsExcludeSelectionsAndCountPeopleSeparatelyFromVotes() {
        JourneyPoll poll = poll(true, 5L);
        when(timeProvider.now()).thenReturn(NOW);
        when(optionRepository.countVotesByPollIds(List.of(5L))).thenReturn(List.of(
                new JourneyPollOptionCountQueryResult(5L, 11L, "A", 1),
                new JourneyPollOptionCountQueryResult(5L, 12L, "B", 1)));
        when(voteRepository.countVotersByPollIds(List.of(5L))).thenReturn(List.of(new JourneyPollVoterCountQueryResult(5L, 1)));
        JourneyPollSummaryResponse summary = service.buildSummaries(List.of(poll), null).get(5L);
        assertThat(summary.selectedOptionIds()).isNull();
        assertThat(summary.voterCount()).isEqualTo(1);
        assertThat(summary.options()).extracting(option -> option.voteCount()).containsExactly(1L, 1L);
        verify(voteRepository, never()).findSelectionsByPollIdsAndUserId(any(), any());
    }

    @Test
    void listUsesLookAheadAndLastReturnedPollAsCursor() {
        allowAccess();
        when(timeProvider.now()).thenReturn(NOW);
        List<JourneyPoll> polls = List.of(poll(true, 9L), poll(true, 8L));
        when(pollRepository.findByJourneyIdWithCursor(1L, null, PageRequest.of(0, 2)))
                .thenReturn(polls);
        var response = service.retrievePolls(1L, 10L, new JourneyPollListRequest(null, 1));
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(9L);
        assertThat(response.polls()).hasSize(1);
        verify(optionRepository).countVotesByPollIds(List.of(9L));
    }

    @Test
    void emptyListHasNoCursorOrAggregateQueries() {
        allowAccess();
        var response = service.retrievePolls(1L, 10L, new JourneyPollListRequest(null, null));
        assertThat(response.polls()).isEmpty();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.hasNext()).isFalse();
        verifyNoInteractions(optionRepository, voteRepository);
    }

    @Test
    void nonMemberCannotReadPoll() {
        assertThatThrownBy(() -> service.retrievePoll(1L, 5L, 10L)).isInstanceOf(JourneyAccessDeniedException.class);
        verifyNoInteractions(pollRepository, voteRepository);
    }

    private void allowDetail(JourneyPoll poll) {
        allowAccess();
        when(pollRepository.getByIdAndJourneyIdOrThrow(5L, 1L)).thenReturn(poll);
        when(timeProvider.now()).thenReturn(NOW);
    }

    private void allowAccess() {
        when(journeyMemberRepository.existsByJourneyIdAndUserIdAndStatus(1L, 10L, JourneyMemberStatus.ACTIVE)).thenReturn(true);
    }

    private JourneyPoll poll(boolean anonymous, Long id) {
        Journey journey = Journey.create(mock(com.dduru.gildongmu.post.domain.Post.class));
        ReflectionTestUtils.setField(journey, "id", 1L);
        User user = User.builder().name("author").build();
        ReflectionTestUtils.setField(user, "id", 10L);
        JourneyPoll poll = JourneyPoll.create(journey, user, "Dinner", true, anonymous, true, NOW.plusDays(1), NOW);
        ReflectionTestUtils.setField(poll, "id", id);
        return poll;
    }
}
