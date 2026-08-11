package com.dsa.schedule_manager.schedule.authorization;

import com.dsa.schedule_manager.schedule.domain.ParticipantStatus;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;
import com.dsa.schedule_manager.schedule.repository.ScheduleParticipantRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ScheduleRelationResolverTest {

    private final ScheduleParticipantRepository participantRepository =
            mock(ScheduleParticipantRepository.class);

    private final ScheduleRelationResolver resolver =
            new ScheduleRelationResolver(participantRepository);

    @Test
    void 작성자는_참여정보를_조회하지_않고_owner를_반환한다() {
        Long ownerId = 1L;
        Schedule schedule = mock(Schedule.class);

        // 작성자는 참여 상태를 확인하기 전에 OWNER로 결정한다.
        when(schedule.isOwnedBy(ownerId))
                .thenReturn(true);

        ScheduleRelation result =
                resolver.resolve(ownerId, schedule);

        assertThat(result)
                .isEqualTo(ScheduleRelation.OWNER);

        // 작성자라면 참여자 Repository를 조회하지 않는다.
        verifyNoInteractions(participantRepository);
    }

    @Test
    void pending_참여자는_pending을_반환한다() {
        Long userId = 2L;
        Long scheduleId = 10L;

        Schedule schedule = mock(Schedule.class);
        ScheduleParticipant participant = mock(ScheduleParticipant.class);

        // 작성자는 아님
        when(schedule.isOwnedBy(userId))
                .thenReturn(false);

        // 조회할 일정 ID
        when(schedule.getId())
                .thenReturn(scheduleId);

        // 해당 일정에 참여 정보가 존재함
        when(participantRepository.findByScheduleIdAndUserId(scheduleId, userId))
                .thenReturn(Optional.of(participant));

        // 현재 참여 상태는 PENDING
        when(participant.getStatus())
                .thenReturn(ParticipantStatus.PENDING);

        ScheduleRelation result =
                resolver.resolve(userId, schedule);

        assertThat(result)
                .isEqualTo(ScheduleRelation.PENDING);
    }

    @Test
    void accepted_참여자는_accepted를_반환한다() {
        Long userId = 2L;
        Long scheduleId = 10L;

        Schedule schedule = mock(Schedule.class);
        ScheduleParticipant participant = mock(ScheduleParticipant.class);

        // 작성자는 아님
        when(schedule.isOwnedBy(userId))
                .thenReturn(false);

        // 조회할 일정 ID
        when(schedule.getId())
                .thenReturn(scheduleId);

        // 해당 일정에 참여 정보가 존재함
        when(participantRepository.findByScheduleIdAndUserId(scheduleId, userId))
                .thenReturn(Optional.of(participant));

        // 현재 참여 상태는 ACCEPTED
        when(participant.getStatus())
                .thenReturn(ParticipantStatus.ACCEPTED);

        ScheduleRelation result =
                resolver.resolve(userId, schedule);

        assertThat(result)
                .isEqualTo(ScheduleRelation.ACCEPTED);
    }

    @Test
    void rejected_참여자는_rejected를_반환한다() {
        Long userId = 2L;
        Long scheduleId = 10L;

        Schedule schedule = mock(Schedule.class);
        ScheduleParticipant participant = mock(ScheduleParticipant.class);

        // 작성자는 아님
        when(schedule.isOwnedBy(userId))
                .thenReturn(false);

        // 조회할 일정 ID
        when(schedule.getId())
                .thenReturn(scheduleId);

        // 해당 일정에 참여 정보가 존재함
        when(participantRepository.findByScheduleIdAndUserId(scheduleId, userId))
                .thenReturn(Optional.of(participant));

        // 현재 참여 상태는 REJECTED
        when(participant.getStatus())
                .thenReturn(ParticipantStatus.REJECTED);

        ScheduleRelation result =
                resolver.resolve(userId, schedule);

        assertThat(result)
                .isEqualTo(ScheduleRelation.REJECTED);
    }

    @Test
    void 참여정보가_없으면_none을_반환한다() {
        Long userId = 2L;
        Long scheduleId = 10L;

        Schedule schedule = mock(Schedule.class);

        // 작성자는 아님
        when(schedule.isOwnedBy(userId))
                .thenReturn(false);

        // 조회할 일정 ID
        when(schedule.getId())
                .thenReturn(scheduleId);

        // 해당 사용자에 대한 참여 정보가 없음
        when(participantRepository.findByScheduleIdAndUserId(scheduleId, userId))
                .thenReturn(Optional.empty());

        ScheduleRelation result =
                resolver.resolve(userId, schedule);

        assertThat(result)
                .isEqualTo(ScheduleRelation.NONE);
    }
}