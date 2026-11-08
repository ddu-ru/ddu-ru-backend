package com.dduru.gildongmu.participation.service;

import com.dduru.gildongmu.chat.service.GroupChatRoomService;
import com.dduru.gildongmu.participation.domain.enums.ParticipationStatus;
import com.dduru.gildongmu.participation.dto.request.ParticipationRequest;
import com.dduru.gildongmu.participation.exception.DuplicateParticipationException;
import com.dduru.gildongmu.participation.exception.InvalidParticipationStatusException;
import com.dduru.gildongmu.participation.exception.RecruitmentClosedException;
import com.dduru.gildongmu.support.concurrency.ConcurrentRequestResult;
import com.dduru.gildongmu.support.concurrency.ConcurrentRequestRunner;
import com.dduru.gildongmu.support.concurrency.ConcurrentRequestStatus;
import com.dduru.gildongmu.support.mysql.MySqlIntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static com.dduru.gildongmu.support.concurrency.ConcurrentRequestRunner.hasCause;

@DisplayName("참여 신청 MySQL 동시성 통합 테스트")
@Import(ParticipationMySqlFixtureFactory.class)
class ParticipationMySqlConcurrencyIntegrationTest extends MySqlIntegrationTestSupport {

    @Autowired private ParticipationApplicantService applicantService;
    @Autowired private ParticipationCommandService commandService;
    @Autowired private ParticipationMySqlFixtureFactory fixtures;

    @MockitoSpyBean
    private GroupChatRoomService groupChatRoomService;

    @Test
    @DisplayName("같은 사용자와 게시글의 동시 신청은 한 건만 커밋한다")
    void concurrentDuplicateApplicationsCommitOnlyOnce() {
        var fixture = fixtures.application("duplicate-application");
        ParticipationRequest request = new ParticipationRequest("MySQL 동시 신청 검증");

        var results = ConcurrentRequestRunner.run(
                List.of(
                        () -> applicantService.participate(fixture.applicantId(), fixture.postId(), request),
                        () -> applicantService.participate(fixture.applicantId(), fixture.postId(), request)
                ),
                failure -> hasCause(failure, DuplicateParticipationException.class)
        );

        assertThat(results).extracting(ConcurrentRequestResult::status)
                .containsExactlyInAnyOrder(
                        ConcurrentRequestStatus.SUCCESS,
                        ConcurrentRequestStatus.EXPECTED_BUSINESS_CONFLICT
                );

        queryCommitted(() -> {
            assertThat(count("SELECT COUNT(*) FROM participations WHERE post_id = ? AND user_id = ?",
                    fixture.postId(), fixture.applicantId())).isEqualTo(1);
            assertThat(count("""
                    SELECT COUNT(*) FROM notifications n
                    JOIN participations p ON p.id = n.resource_id
                    WHERE n.type = 'MATCH_APPLIED' AND p.post_id = ? AND p.user_id = ?
                    """, fixture.postId(), fixture.applicantId())).isEqualTo(1);
            return null;
        });
    }

    @Test
    @DisplayName("마지막 한 자리의 동시 승인은 정원과 후속 저장을 초과하지 않는다")
    void concurrentApprovalsForLastSeatPreserveCapacityAndDependentRows() {
        var fixture = fixtures.approval("last-seat");

        var results = ConcurrentRequestRunner.run(
                List.of(
                        () -> commandService.approveParticipation(fixture.hostId(), fixture.firstParticipationId()),
                        () -> commandService.approveParticipation(fixture.hostId(), fixture.secondParticipationId())
                ),
                failure -> hasCause(failure, RecruitmentClosedException.class)
        );

        assertThat(results).extracting(ConcurrentRequestResult::status)
                .containsExactlyInAnyOrder(
                        ConcurrentRequestStatus.SUCCESS,
                        ConcurrentRequestStatus.EXPECTED_BUSINESS_CONFLICT
                );

        queryCommitted(() -> {
            assertThat(integer("SELECT recruit_count FROM posts WHERE id = ?", fixture.postId())).isEqualTo(2);
            assertThat(count("SELECT COUNT(*) FROM participations WHERE post_id = ? AND status = 'APPROVED'",
                    fixture.postId())).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM journey_members WHERE journey_id = ? AND status = 'ACTIVE'",
                    fixture.journeyId())).isEqualTo(2);
            assertThat(count("SELECT COUNT(*) FROM journey_members WHERE journey_id = ? AND role = 'MEMBER'",
                    fixture.journeyId())).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM chat_room_members WHERE room_id = ? AND role = 'GUEST'",
                    fixture.roomId())).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM notifications WHERE type = 'MATCH_APPROVED' AND resource_id = ?",
                    fixture.journeyId())).isEqualTo(1);
            assertThat(count("""
                    SELECT COUNT(*) FROM (
                        SELECT journey_id, user_id FROM journey_members
                        WHERE journey_id = ? GROUP BY journey_id, user_id HAVING COUNT(*) > 1
                    ) duplicates
                    """, fixture.journeyId())).isZero();
            assertThat(count("""
                    SELECT COUNT(*) FROM (
                        SELECT room_id, user_id FROM chat_room_members
                        WHERE room_id = ? GROUP BY room_id, user_id HAVING COUNT(*) > 1
                    ) duplicates
                    """, fixture.roomId())).isZero();
            return null;
        });
    }

    @Test
    @DisplayName("성공한 신청과 승인을 재호출해도 후속 행은 한 번만 생성한다")
    void repeatedRequestsDoNotDuplicateDependentRows() {
        var application = fixtures.application("repeat-application");
        applicantService.participate(application.applicantId(), application.postId(), new ParticipationRequest("첫 신청"));

        assertThatThrownBy(() -> applicantService.participate(
                application.applicantId(), application.postId(), new ParticipationRequest("재신청")
        )).isInstanceOf(DuplicateParticipationException.class);

        var approval = fixtures.approval("repeat-approval", 3);
        commandService.approveParticipation(approval.hostId(), approval.firstParticipationId());

        assertThatThrownBy(() -> commandService.approveParticipation(
                approval.hostId(), approval.firstParticipationId()
        )).isInstanceOf(InvalidParticipationStatusException.class);

        queryCommitted(() -> {
            assertThat(count("SELECT COUNT(*) FROM participations WHERE post_id = ? AND user_id = ?",
                    application.postId(), application.applicantId())).isEqualTo(1);
            assertThat(count("""
                    SELECT COUNT(*) FROM notifications n
                    JOIN participations p ON p.id = n.resource_id
                    WHERE n.type = 'MATCH_APPLIED' AND p.post_id = ? AND p.user_id = ?
                    """, application.postId(), application.applicantId())).isEqualTo(1);
            assertThat(integer("SELECT recruit_count FROM posts WHERE id = ?", approval.postId())).isEqualTo(2);
            assertThat(count("SELECT COUNT(*) FROM journey_members WHERE journey_id = ? AND user_id = ?",
                    approval.journeyId(), approval.firstApplicantId())).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM chat_room_members WHERE room_id = ? AND user_id = ?",
                    approval.roomId(), approval.firstApplicantId())).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM notifications WHERE type = 'MATCH_APPROVED' AND resource_id = ?",
                    approval.journeyId())).isEqualTo(1);
            return null;
        });
    }

    @Test
    @DisplayName("승인 후속 저장 실패는 모집 인원과 모든 후속 행을 롤백한다")
    void approvalDependentSaveFailureRollsBackWholeTransaction() {
        var fixture = fixtures.approval("approval-rollback");
        doThrow(new IllegalStateException("후속 그룹방 저장 실패 재현"))
                .when(groupChatRoomService)
                .inviteMemberOrGetRoom(anyLong(), anyLong(), anyLong());

        assertThatThrownBy(() -> commandService.approveParticipation(
                fixture.hostId(), fixture.firstParticipationId()
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("후속 그룹방 저장 실패");

        queryCommitted(() -> {
            assertThat(integer("SELECT recruit_count FROM posts WHERE id = ?", fixture.postId())).isEqualTo(1);
            assertThat(string("SELECT status FROM participations WHERE id = ?", fixture.firstParticipationId()))
                    .isEqualTo(ParticipationStatus.PENDING.name());
            assertThat(count("SELECT COUNT(*) FROM journey_members WHERE journey_id = ? AND user_id = ?",
                    fixture.journeyId(), fixture.firstApplicantId())).isZero();
            assertThat(count("SELECT COUNT(*) FROM chat_room_members WHERE room_id = ? AND user_id = ?",
                    fixture.roomId(), fixture.firstApplicantId())).isZero();
            assertThat(count("SELECT COUNT(*) FROM notifications WHERE type = 'MATCH_APPROVED' AND resource_id = ?",
                    fixture.journeyId())).isZero();
            return null;
        });
    }

}
