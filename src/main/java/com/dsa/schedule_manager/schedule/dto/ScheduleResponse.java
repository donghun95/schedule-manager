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
    public static ScheduleResponse from(Schedule s) {
        return new ScheduleResponse(
                s.getId(), s.getOwnerId(), s.getTitle(), s.getDescription(),
                s.getStatus().name(), s.getScheduledAt(), s.getVersion(),
                s.getCreatedAt(), s.getUpdatedAt()
        );
    }
}