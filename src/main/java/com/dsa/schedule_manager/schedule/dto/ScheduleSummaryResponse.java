package com.dsa.schedule_manager.schedule.dto;

import com.dsa.schedule_manager.schedule.domain.Schedule;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ScheduleSummaryResponse(
        Long id,
        String title,
        @Schema(allowableValues = {"PLANNED", "IN_PROGRESS", "DONE", "CANCELED"})
        String status,
        LocalDateTime scheduledAt,
        @Schema(allowableValues = {"OWNER", "PARTICIPANT"})
        String accessType,
        Long version
) {
    public static ScheduleSummaryResponse from(Schedule schedule, Long userId) {
        return new ScheduleSummaryResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getStatus().name(),
                schedule.getScheduledAt(),
                schedule.isOwnedBy(userId) ? "OWNER" : "PARTICIPANT",
                schedule.getVersion());
    }
}
