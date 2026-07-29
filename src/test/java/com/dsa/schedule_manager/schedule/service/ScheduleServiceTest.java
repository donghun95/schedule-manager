package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.dto.ScheduleResponse;
import com.dsa.schedule_manager.schedule.dto.ScheduleUpdateRequest;
import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    ScheduleRepository scheduleRepository;

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
