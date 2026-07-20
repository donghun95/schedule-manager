package com.dsa.schedule_manager.schedule.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ScheduleUpdateRequest(
        @Size(max = 200)
        @Pattern(regexp = "(?s).*\\S.*",message = "제목은 공백으로만 구성할 수 없습니다.")
        String title,
        @Size(max = 2000) String description,
        @Future LocalDateTime scheduledAt,
        @NotNull Long version
        )
{
    public boolean hasChanges() {
        return title != null || description != null || scheduledAt != null;
    }
}
