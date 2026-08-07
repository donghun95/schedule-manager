package com.dsa.schedule_manager.schedule.domain;


import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.dsa.schedule_manager.user.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ScheduleParticipantTest {

    @Test
    void 참여자는_처음_초대되면_PENDING_상태다() {
        // given
        Schedule schedule = Schedule.create(
                1L,
                "테스트 일정",
                null,
                LocalDateTime.now().plusDays(1)
        );

        User user = mock(User.class);

        // when
        ScheduleParticipant participant =
                ScheduleParticipant.of(schedule, user);

        // then
        assertThat(participant.getStatus())
                .isEqualTo(ParticipantStatus.PENDING);




    }

    @Test
    void PENDING_참여자는_초대를_수락하면_ACCEPTED가_된다() {
        // given
        Schedule schedule = Schedule.create(
                1L,
                "테스트 일정",
                null,
                LocalDateTime.now().plusDays(1)
        );

        User user = mock(User.class);

        ScheduleParticipant participant =
                ScheduleParticipant.of(schedule, user);

        // when
        participant.accept();

        // then
        assertThat(participant.getStatus())
                .isEqualTo(ParticipantStatus.ACCEPTED);
    }

    @Test
    void 이미_ACCEPTED인_참여자는_다시_수락할_수_없다() {
        // given
        Schedule schedule = Schedule.create(
                1L,
                "테스트 일정",
                null,
                LocalDateTime.now().plusDays(1)
        );

        User user = mock(User.class);

        ScheduleParticipant participant =
                ScheduleParticipant.of(schedule, user);

        participant.accept();

        // when & then
        assertThatThrownBy(participant::accept)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARTICIPANT_STATUS_TRANSITION);
    }

    @Test
    void PENDING_참여자는_초대를_거절하면_REJECTED가_된다() {
        // given
        Schedule schedule = Schedule.create(
                1L,
                "테스트 일정",
                null,
                LocalDateTime.now().plusDays(1)
        );

        User user = mock(User.class);

        ScheduleParticipant participant =
                ScheduleParticipant.of(schedule, user);

        // when
        participant.reject();

        // then
        assertThat(participant.getStatus())
                .isEqualTo(ParticipantStatus.REJECTED);
    }

    @Test
    void 이미_REJECTED인_참여자는_다시_거절할_수_없다() {
        // given
        Schedule schedule = Schedule.create(
                1L,
                "테스트 일정",
                null,
                LocalDateTime.now().plusDays(1)
        );

        User user = mock(User.class);

        ScheduleParticipant participant =
                ScheduleParticipant.of(schedule, user);

        participant.reject();

        // when & then
        assertThatThrownBy(participant::reject)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARTICIPANT_STATUS_TRANSITION);
    }

    @Test
    void ACCEPTED_참여자는_거절할_수_없다() {
        // given
        Schedule schedule = Schedule.create(
                1L,
                "테스트 일정",
                null,
                LocalDateTime.now().plusDays(1)
        );

        User user = mock(User.class);

        ScheduleParticipant participant =
                ScheduleParticipant.of(schedule, user);

        participant.accept();

        // when & then
        assertThatThrownBy(participant::reject)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARTICIPANT_STATUS_TRANSITION);
    }

    @Test
    void REJECTED_참여자는_수락할_수_없다() {
        // given
        Schedule schedule = Schedule.create(
                1L,
                "테스트 일정",
                null,
                LocalDateTime.now().plusDays(1)
        );

        User user = mock(User.class);

        ScheduleParticipant participant =
                ScheduleParticipant.of(schedule, user);

        participant.reject();

        // when & then
        assertThatThrownBy(participant::accept)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARTICIPANT_STATUS_TRANSITION);
    }
}