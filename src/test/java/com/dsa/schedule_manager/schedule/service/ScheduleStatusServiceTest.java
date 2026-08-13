package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import com.dsa.schedule_manager.schedule.authorization.ScheduleRelation;
import com.dsa.schedule_manager.schedule.authorization.ScheduleRelationResolver;
import com.dsa.schedule_manager.schedule.authorization.ScheduleAccessPolicy;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleStatus;
import com.dsa.schedule_manager.schedule.domain.ScheduleStatusHistory;
import com.dsa.schedule_manager.schedule.dto.ScheduleStatusChangeRequest;
import com.dsa.schedule_manager.schedule.repository.ScheduleParticipantRepository;
import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
import com.dsa.schedule_manager.schedule.repository.ScheduleStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ScheduleStatusServiceTest {

    @Mock
    ScheduleRepository scheduleRepository;

    @Mock
    ScheduleParticipantRepository participantRepository;

    @Mock
    ScheduleStatusHistoryRepository historyRepository;

    @Mock
    ScheduleRelationResolver scheduleRelationResolver;

    @Mock
    ScheduleAccessPolicy scheduleAccessPolicy;

    @InjectMocks
    ScheduleStatusService sut;

    @Test
    void 작성자가_상태를_바꾸면_같은_트랜잭션에서_이력을_저장한다() {
        Schedule schedule = schedule(1L, 0L);
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule));

        var response = sut.changeStatus(
                1L,
                10L,
                new ScheduleStatusChangeRequest(ScheduleStatus.IN_PROGRESS, 0L));

        assertThat(response.status()).isEqualTo("IN_PROGRESS");
        then(scheduleRepository).should().flush();
        then(historyRepository).should().save(any(ScheduleStatusHistory.class));
    }

    @Test
    void 참여자는_DONE으로_직접_변경할_수_없다() {
        Schedule schedule = schedule(1L, 0L);
        schedule.changeStatus(ScheduleStatus.IN_PROGRESS);
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule));

        assertThatThrownBy(() -> sut.changeStatus(
                2L,
                10L,
                new ScheduleStatusChangeRequest(ScheduleStatus.DONE, 0L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        then(historyRepository).should(never()).save(any());
    }

    @Test
    void 허용되지_않는_전이는_409이고_이력을_저장하지_않는다() {
        Schedule schedule = schedule(1L, 0L);
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule));

        assertThatThrownBy(() -> sut.changeStatus(
                1L,
                10L,
                new ScheduleStatusChangeRequest(ScheduleStatus.DONE, 0L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);

        then(scheduleRepository).should(never()).flush();
        then(historyRepository).should(never()).save(any());
    }

    @Test
    void 같은_상태로_변경하면_409이고_이력을_저장하지_않는다() {
        Schedule schedule = schedule(1L, 0L);
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule));

        assertThatThrownBy(() -> sut.changeStatus(
                1L,
                10L,
                new ScheduleStatusChangeRequest(ScheduleStatus.PLANNED, 0L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SAME_STATUS_TRANSITION);

        then(scheduleRepository).should(never()).flush();
        then(historyRepository).should(never()).save(any());
    }

    @Test
    void 오래된_version이면_409이고_상태와_이력을_변경하지_않는다() {
        Schedule schedule = schedule(1L, 1L);
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule));

        assertThatThrownBy(() -> sut.changeStatus(
                1L,
                10L,
                new ScheduleStatusChangeRequest(ScheduleStatus.IN_PROGRESS, 0L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SCHEDULE_CONFLICT);

        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.PLANNED);
        then(scheduleRepository).should(never()).flush();
        then(historyRepository).should(never()).save(any());
    }

    @Test
    void 참여자는_상태_이력을_조회할_수_있다() {
        Schedule schedule = schedule(1L, 0L);

        given(scheduleRepository.findById(10L))
                .willReturn(Optional.of(schedule));

        given(scheduleRelationResolver.resolve(2L, schedule))
                .willReturn(ScheduleRelation.ACCEPTED);

        given(scheduleAccessPolicy.canViewStatusHistory(ScheduleRelation.ACCEPTED))
                .willReturn(true);

        given(historyRepository.findAllByScheduleIdOrderByIdAsc(10L))
                .willReturn(java.util.List.of());

        assertThat(sut.getHistory(2L, 10L)).isEmpty();
    }

    private Schedule schedule(Long ownerId, Long version) {
        Schedule schedule = Schedule.create(
                ownerId,
                "일정",
                null,
                LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(schedule, "id", 10L);
        ReflectionTestUtils.setField(schedule, "version", version);
        return schedule;
    }
}
