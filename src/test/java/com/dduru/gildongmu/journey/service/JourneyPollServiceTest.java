package com.dduru.gildongmu.journey.service;

import com.dduru.gildongmu.chat.service.ChatMessageSendService;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.journey.domain.*;
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberStatus;
import com.dduru.gildongmu.journey.dto.query.JourneyPollSelectionQueryResult;
import com.dduru.gildongmu.journey.dto.request.*;
import com.dduru.gildongmu.journey.dto.response.JourneyPollSummaryResponse;
import com.dduru.gildongmu.journey.event.JourneyPollUpdatedEvent;
import com.dduru.gildongmu.journey.exception.*;
import com.dduru.gildongmu.journey.repository.*;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JourneyPollServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 12, 0);
    @Mock private JourneyRepository journeyRepository;
    @Mock private JourneyMemberRepository journeyMemberRepository;
    @Mock private JourneyPollRepository pollRepository;
    @Mock private JourneyPollOptionRepository optionRepository;
    @Mock private JourneyPollVoteRepository voteRepository;
    @Mock private UserRepository userRepository;
    @Mock private JourneyPollQueryService pollQueryService;
    @Mock private ChatMessageSendService chatMessageSendService;
    @Mock private TimeProvider timeProvider;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private JourneyPollService service;
    private Journey journey;
    private User user;

    @BeforeEach
    void setUp() {
        journey = mock(Journey.class);
        user = User.builder().name("voter").build();
        ReflectionTestUtils.setField(user, "id", 10L);
    }

    @Test
    void createsOptionsAndOnePublicChatCard() {
        allowAccess();
        when(journeyRepository.getByIdOrThrow(1L)).thenReturn(journey);
        when(userRepository.getWithProfileByIdOrThrow(10L)).thenReturn(user);
        when(timeProvider.now()).thenReturn(NOW);
        when(pollRepository.save(any())).thenAnswer(invocation -> {
            JourneyPoll poll = invocation.getArgument(0);
            ReflectionTestUtils.setField(poll, "id", 5L);
            return poll;
        });
        JourneyPollSummaryResponse summary = mock(JourneyPollSummaryResponse.class);
        when(pollQueryService.buildSummaries(anyList(), isNull())).thenReturn(Map.of(5L, summary));

        service.createPoll(1L, 10L, new JourneyPollCreateRequest(" Dinner ", List.of(" A ", "B"),
                true, true, true, NOW.plusHours(1)));

        ArgumentCaptor<JourneyPoll> poll = ArgumentCaptor.forClass(JourneyPoll.class);
        verify(chatMessageSendService).publishPoll(poll.capture(), same(summary));
        assertThat(poll.getValue().getTitle()).isEqualTo("Dinner");
        assertThat(poll.getValue().isAnonymous()).isTrue();
        ArgumentCaptor<List<JourneyPollOption>> options = ArgumentCaptor.forClass(List.class);
        verify(optionRepository).saveAll(options.capture());
        assertThat(options.getValue()).extracting(JourneyPollOption::getContent).containsExactly("A", "B");
    }

    @Test
    void rejectsDuplicateNormalizedOptions() {
        allowAccess();
        assertThatThrownBy(() -> service.createPoll(1L, 10L,
                new JourneyPollCreateRequest("Title", List.of(" A ", "A"), false, false, false, NOW.plusDays(1))))
                .isInstanceOf(InvalidJourneyPollException.class);
        verifyNoInteractions(pollRepository, chatMessageSendService);
    }

    @Test
    void deniesNonMemberBeforeReadingPoll() {
        assertThatThrownBy(() -> service.vote(1L, 5L, 10L, new JourneyPollVoteRequest(List.of(11L))))
                .isInstanceOf(JourneyAccessDeniedException.class);
        verifyNoInteractions(pollRepository, voteRepository);
    }

    @Test
    void rejectsVoteAtExactDeadline() {
        lockedPoll(false, false, NOW.plusHours(1));
        when(timeProvider.now()).thenReturn(NOW.plusHours(1));
        assertThatThrownBy(() -> service.vote(1L, 5L, 10L, new JourneyPollVoteRequest(List.of(11L))))
                .isInstanceOf(JourneyPollClosedException.class);
        verifyNoInteractions(optionRepository, voteRepository);
    }

    @Test
    void singleChoiceRejectsMultipleSelections() {
        lockedPoll(false, false, NOW.plusHours(1));
        assertThatThrownBy(() -> service.vote(1L, 5L, 10L, new JourneyPollVoteRequest(List.of(11L, 12L))))
                .isInstanceOf(InvalidJourneyPollException.class);
        verifyNoInteractions(voteRepository);
    }

    @Test
    void rejectsDuplicateSelectionIds() {
        lockedPoll(true, false, NOW.plusHours(1));
        assertThatThrownBy(() -> service.vote(1L, 5L, 10L, new JourneyPollVoteRequest(List.of(11L, 11L))))
                .isInstanceOf(InvalidJourneyPollException.class);
    }

    @Test
    void rejectsOptionsOfAnotherPollBeforeDeletingExistingVotes() {
        JourneyPoll poll = lockedPoll(true, false, NOW.plusHours(1));
        when(optionRepository.findByPollIdOrderByDisplayOrderAsc(5L)).thenReturn(options(poll));
        assertThatThrownBy(() -> service.vote(1L, 5L, 10L, new JourneyPollVoteRequest(List.of(999L))))
                .isInstanceOf(InvalidJourneyPollException.class);
        verifyNoInteractions(voteRepository);
    }

    @Test
    void replacesSelectionsAndPublishesInvalidationWithoutAnotherChatMessage() {
        JourneyPoll poll = lockedPoll(true, false, NOW.plusHours(1));
        when(optionRepository.findByPollIdOrderByDisplayOrderAsc(5L)).thenReturn(options(poll));
        when(userRepository.getByIdOrThrow(10L)).thenReturn(user);
        when(voteRepository.findSelectionsByPollIdsAndUserId(List.of(5L), 10L))
                .thenReturn(List.of(new JourneyPollSelectionQueryResult(5L, 11L)));

        service.vote(1L, 5L, 10L, new JourneyPollVoteRequest(List.of(12L)));

        verify(voteRepository).deleteByOptionIdsAndUserId(List.of(11L, 12L), 10L);
        ArgumentCaptor<List<JourneyPollVote>> votes = ArgumentCaptor.forClass(List.class);
        verify(voteRepository).saveAll(votes.capture());
        assertThat(votes.getValue()).extracting(vote -> vote.getOption().getId()).containsExactly(12L);
        verify(eventPublisher).publishEvent(new JourneyPollUpdatedEvent(1L, 5L));
        verifyNoInteractions(chatMessageSendService);
    }

    @Test
    void sameSelectionsInDifferentOrderAreIdempotent() {
        JourneyPoll poll = lockedPoll(true, false, NOW.plusHours(1));
        when(optionRepository.findByPollIdOrderByDisplayOrderAsc(5L)).thenReturn(options(poll));
        when(voteRepository.findSelectionsByPollIdsAndUserId(List.of(5L), 10L)).thenReturn(List.of(
                new JourneyPollSelectionQueryResult(5L, 11L), new JourneyPollSelectionQueryResult(5L, 12L)));
        service.vote(1L, 5L, 10L, new JourneyPollVoteRequest(List.of(12L, 11L)));
        verify(voteRepository, never()).deleteByOptionIdsAndUserId(any(), any());
        verify(voteRepository, never()).saveAll(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void rejectsOptionAdditionWhenDisabled() {
        lockedPoll(false, false, NOW.plusHours(1));
        assertThatThrownBy(() -> service.addOption(1L, 5L, 10L, new JourneyPollOptionCreateRequest("C")))
                .isInstanceOf(JourneyPollOptionAdditionDeniedException.class);
    }

    @Test
    void rejectsDuplicateAdditionalOption() {
        JourneyPoll poll = lockedPoll(false, true, NOW.plusHours(1));
        when(optionRepository.findByPollIdOrderByDisplayOrderAsc(5L)).thenReturn(options(poll));
        assertThatThrownBy(() -> service.addOption(1L, 5L, 10L, new JourneyPollOptionCreateRequest(" A ")))
                .isInstanceOf(InvalidJourneyPollException.class);
        verify(optionRepository, never()).save(any());
    }

    @Test
    void rejectsEleventhOption() {
        JourneyPoll poll = lockedPoll(false, true, NOW.plusHours(1));
        when(optionRepository.findByPollIdOrderByDisplayOrderAsc(5L)).thenReturn(IntStream.range(0, 10)
                .mapToObj(i -> JourneyPollOption.create(poll, "Option " + i, i)).toList());
        assertThatThrownBy(() -> service.addOption(1L, 5L, 10L, new JourneyPollOptionCreateRequest("New")))
                .isInstanceOf(InvalidJourneyPollException.class);
    }

    @Test
    void addsOptionAtEndAndPublishesEvent() {
        JourneyPoll poll = lockedPoll(false, true, NOW.plusHours(1));
        when(optionRepository.findByPollIdOrderByDisplayOrderAsc(5L)).thenReturn(options(poll));
        service.addOption(1L, 5L, 10L, new JourneyPollOptionCreateRequest(" C "));
        ArgumentCaptor<JourneyPollOption> option = ArgumentCaptor.forClass(JourneyPollOption.class);
        verify(optionRepository).save(option.capture());
        assertThat(option.getValue().getDisplayOrder()).isEqualTo(2);
        assertThat(option.getValue().getContent()).isEqualTo("C");
        verify(eventPublisher).publishEvent(new JourneyPollUpdatedEvent(1L, 5L));
    }

    private JourneyPoll lockedPoll(boolean multiple, boolean addition, LocalDateTime closesAt) {
        allowAccess();
        JourneyPoll poll = JourneyPoll.create(journey, user, "Dinner", multiple, true, addition, closesAt, NOW);
        ReflectionTestUtils.setField(poll, "id", 5L);
        when(pollRepository.getByIdAndJourneyIdWithLockOrThrow(5L, 1L)).thenReturn(poll);
        when(timeProvider.now()).thenReturn(NOW);
        return poll;
    }

    private void allowAccess() {
        when(journeyMemberRepository.existsByJourneyIdAndUserIdAndStatus(1L, 10L, JourneyMemberStatus.ACTIVE)).thenReturn(true);
    }

    private List<JourneyPollOption> options(JourneyPoll poll) {
        JourneyPollOption a = JourneyPollOption.create(poll, "A", 0);
        JourneyPollOption b = JourneyPollOption.create(poll, "B", 1);
        ReflectionTestUtils.setField(a, "id", 11L);
        ReflectionTestUtils.setField(b, "id", 12L);
        return List.of(a, b);
    }
}
