package com.dsa.schedule_manager.schedule.authorization;

import com.dsa.schedule_manager.schedule.domain.ParticipantStatus;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.repository.ScheduleParticipantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ScheduleAccessPolicyTest {

    @Mock
    ScheduleParticipantRepository participantRepository;

    @InjectMocks
    ScheduleAccessPolicy sut;

    @Test
    void 작성자는_일정을_조회할_수_있다() {
        // given
        Schedule schedule = Schedule.create(
                1L,
                "제목",
                null,
                LocalDateTime.now().plusDays(1)
        );

        // when
        boolean result = sut.canViewSchedule(1L, schedule);

        // then
        assertThat(result).isTrue();

        then(participantRepository)
                .shouldHaveNoInteractions();
    }
    @Test
    void ACCEPTED_참여자는_일정을_조회할_수_있다() {
        // given
        Schedule schedule = Schedule.create(
                1L,
                "제목",
                null,
                LocalDateTime.now().plusDays(1)
        );

        ReflectionTestUtils.setField(schedule, "id", 10L);

        given(participantRepository.existsByScheduleIdAndUserIdAndStatus(
                10L,
                2L,
                ParticipantStatus.ACCEPTED
        )).willReturn(true);

        // when
        boolean result = sut.canViewSchedule(2L, schedule);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void PENDING_초대자는_일정을_조회할_수_없다() {
        // given
        Schedule schedule = Schedule.create(
                1L,
                "제목",
                null,
                LocalDateTime.now().plusDays(1)
        );

        ReflectionTestUtils.setField(schedule, "id", 10L);

        given(participantRepository.existsByScheduleIdAndUserIdAndStatus(
                10L,
                2L,
                ParticipantStatus.ACCEPTED
        )).willReturn(false);

        // when
        boolean result = sut.canViewSchedule(2L, schedule);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void REJECTED_참여자는_일정을_조회할_수_없다() {
        // given
        Schedule schedule = Schedule.create(
                1L,
                "제목",
                null,
                LocalDateTime.now().plusDays(1)
        );

        ReflectionTestUtils.setField(schedule, "id", 10L);

        given(participantRepository.existsByScheduleIdAndUserIdAndStatus(
                10L,
                2L,
                ParticipantStatus.ACCEPTED
        )).willReturn(false);

        // when
        boolean result = sut.canViewSchedule(2L, schedule);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void 관계없는_사용자는_일정을_조회할_수_없다() {
        // given
        Schedule schedule = Schedule.create(
                1L,
                "제목",
                null,
                LocalDateTime.now().plusDays(1)
        );

        ReflectionTestUtils.setField(schedule, "id", 10L);

        given(participantRepository.existsByScheduleIdAndUserIdAndStatus(
                10L,
                3L,
                ParticipantStatus.ACCEPTED
        )).willReturn(false);

        // when
        boolean result = sut.canViewSchedule(3L, schedule);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void 다른_일정의_ACCEPTED_참여자는_현재_일정을_조회할_수_없다() {
        // given
        Schedule targetSchedule = Schedule.create(
                1L,
                "제목",
                null,
                LocalDateTime.now().plusDays(1)
        );

        ReflectionTestUtils.setField(targetSchedule, "id", 20L);

        given(participantRepository.existsByScheduleIdAndUserIdAndStatus(
                20L,
                2L,
                ParticipantStatus.ACCEPTED
        )).willReturn(false);

        // when
        boolean result = sut.canViewSchedule(2L, targetSchedule);

        // then
        assertThat(result).isFalse();
    }




}