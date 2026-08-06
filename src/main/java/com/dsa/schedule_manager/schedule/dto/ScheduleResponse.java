package com.dsa.schedule_manager.schedule.dto;

import com.dsa.schedule_manager.schedule.domain.Schedule;
import java.time.LocalDateTime;

public record ScheduleResponse(
        Long id,
        Long ownerId,
        String title,
        String description,
        String status,
        LocalDateTime scheduledAt,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getOwnerId(),
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getStatus().name(),
                schedule.getScheduledAt(),
                schedule.getVersion(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
