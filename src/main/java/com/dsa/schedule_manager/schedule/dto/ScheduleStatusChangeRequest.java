package com.dsa.schedule_manager.schedule.dto;

import com.dsa.schedule_manager.schedule.domain.ScheduleStatus;
import jakarta.validation.constraints.NotNull;

public record ScheduleStatusChangeRequest(
        @NotNull ScheduleStatus toStatus,
        @NotNull Long version
) {
}
