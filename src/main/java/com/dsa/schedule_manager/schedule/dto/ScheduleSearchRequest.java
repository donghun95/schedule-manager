package com.dsa.schedule_manager.schedule.dto;

import com.dsa.schedule_manager.schedule.domain.ScheduleStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record ScheduleSearchRequest(
        ScheduleStatus status,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime fromAt,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime toAt,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime cursorScheduledAt,
        Long cursorId,
        @Min(1) @Max(100) Integer size
) {
    public int effectiveSize() {
        return size == null ? 20 : size;
    }

    public boolean hasOnlyOneCursorValue() {
        return (cursorScheduledAt == null) != (cursorId == null);
    }

    public boolean hasInvalidDateRange() {
        return fromAt != null && toAt != null && fromAt.isAfter(toAt);
    }
}
