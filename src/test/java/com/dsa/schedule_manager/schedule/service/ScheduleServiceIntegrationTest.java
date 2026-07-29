package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import com.dsa.schedule_manager.schedule.dto.ScheduleCreateRequest;
import com.dsa.schedule_manager.schedule.dto.ScheduleResponse;
import com.dsa.schedule_manager.schedule.dto.ScheduleUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ScheduleServiceIntegrationTest {

    @Autowired
    ScheduleService scheduleService;

    @Test
    void update는_증가한_version을_응답하고_오래된_version은_거절한다() {
        ScheduleResponse created = scheduleService.create(
                1L,
                new ScheduleCreateRequest(
                        "원본",
                        null,
                        LocalDateTime.of(2099, 1, 1, 10, 0)
                )
        );

        ScheduleResponse updated = scheduleService.update(
                1L,
                created.id(),
                new ScheduleUpdateRequest("변경", null, null, created.version())
        );

        assertThat(updated.version()).isEqualTo(created.version() + 1);
        assertThatThrownBy(() -> scheduleService.update(
                1L,
                created.id(),
                new ScheduleUpdateRequest("오래된 요청", null, null, created.version())
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SCHEDULE_CONFLICT);
    }
}
