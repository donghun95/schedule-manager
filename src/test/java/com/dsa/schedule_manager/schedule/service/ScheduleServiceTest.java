package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleStatus;
import com.dsa.schedule_manager.schedule.dto.ScheduleResponse;
import com.dsa.schedule_manager.schedule.dto.ScheduleStatusChangeRequest;
import com.dsa.schedule_manager.schedule.dto.ScheduleUpdateRequest;
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
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    ScheduleRepository scheduleRepository;

    @Mock
    ScheduleStatusHistoryRepository scheduleStatusHistoryRepository;

    @InjectMocks
    ScheduleService sut;

    @Test
    void update_작성자와_version이_맞으면_수정한다() {
        Schedule schedule = schedule(1L, 0L);
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule));
        ScheduleUpdateRequest request = new ScheduleUpdateRequest("변경", null, null, 0L);

        ScheduleResponse response = sut.update(1L, 10L, request);

        assertThat(response.title()).isEqualTo("변경");
        assertThat(response.version()).isZero();
        then(scheduleRepository).should().flush();
    }

    @Test
    void update_일정이_없으면_NOT_FOUND() {
        given(scheduleRepository.findById(10L)).willReturn(Optional.empty());
        ScheduleUpdateRequest request = new ScheduleUpdateRequest("변경", null, null, 0L);

        assertThatThrownBy(() -> sut.update(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    void update_변경할_필드가_없으면_INVALID_INPUT() {
        ScheduleUpdateRequest request = new ScheduleUpdateRequest(null, null, null, 0L);

        assertThatThrownBy(() -> sut.update(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void update_작성자가_아니면_FORBIDDEN() {
        Schedule schedule = schedule(1L, 0L);
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule));
        ScheduleUpdateRequest request = new ScheduleUpdateRequest("변경", null, null, 0L);

        assertThatThrownBy(() -> sut.update(2L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void update_version이_다르면_CONFLICT() {
        Schedule schedule = schedule(1L, 0L);
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule));
        ScheduleUpdateRequest request = new ScheduleUpdateRequest("변경", null, null, 99L);

        assertThatThrownBy(() -> sut.update(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SCHEDULE_CONFLICT);
    }

    // 4. 허용된 상태 전이는 성공
    @Test
    void changeStatus_Success() {
        // given
        Long ownerId = 1L;
        Long scheduleId = 10L;
        Schedule schedule = schedule(ownerId, 0L);
        given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

        // 생성자 순서: (toStatus, version)
        ScheduleStatusChangeRequest request = new ScheduleStatusChangeRequest(ScheduleStatus.IN_PROGRESS, 0L);

        // when (scheduleService 대신 sut 사용)
        ScheduleResponse response = sut.changeStatus(ownerId, scheduleId, request);

        // then
        assertThat(response.status()).isEqualTo(ScheduleStatus.IN_PROGRESS.name());
    }

    // 5. 같은 상태 또는 허용되지 않은 전이는 409
    @Test
    void changeStatus_InvalidTransition_ThrowsConflict() {
        // given
        Long ownerId = 1L;
        Long scheduleId = 10L;
        Schedule schedule = schedule(ownerId, 0L);
        given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

        ScheduleStatusChangeRequest request = new ScheduleStatusChangeRequest(ScheduleStatus.DONE, 0L);

        // when & then
        assertThatThrownBy(() -> sut.changeStatus(ownerId, scheduleId, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void changeStatus_상태변경_성공시_이력을_저장하고_실패시_저장하지_않는다() {
        // given
        Long ownerId = 1L;
        Long scheduleId = 10L;
        Schedule schedule = schedule(ownerId, 0L);
        given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

        ScheduleStatusChangeRequest validRequest = new ScheduleStatusChangeRequest(ScheduleStatus.IN_PROGRESS, 0L);
        ScheduleStatusChangeRequest invalidRequest = new ScheduleStatusChangeRequest(ScheduleStatus.PLANNED, 0L);

        // 1. 성공 케이스 실행
        sut.changeStatus(ownerId, scheduleId, validRequest);

        // then: historyRepository.save(...)가 정확히 1번 호출되었는지 검증
        then(scheduleStatusHistoryRepository).should(times(1)).save(any());

        // 2. 실패 케이스 실행 (잘못된 전이 예외 발생)
        assertThatThrownBy(() -> sut.changeStatus(ownerId, scheduleId, invalidRequest))
                .isInstanceOf(BusinessException.class);

        // then: 예외가 발생했으므로 여전히 save(...)의 누적 호출 횟수는 1번이어야 함
        then(scheduleStatusHistoryRepository).shouldHaveNoMoreInteractions();
        // 또는 then(scheduleHistoryRepository).should(times(1)).save(any());
    }



    private Schedule schedule(Long ownerId, Long version) {
        Schedule schedule = Schedule.create(
                ownerId,
                "제목",
                null,
                LocalDateTime.now().plusDays(1)
        );
        ReflectionTestUtils.setField(schedule, "version", version);
        return schedule;
    }
}
