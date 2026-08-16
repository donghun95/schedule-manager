package com.dsa.schedule_manager.schedule.dto;

import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;
import java.time.LocalDateTime;

public record ScheduleParticipantResponse(
        Long id,
        Long scheduleId,
        Long userId,
        LocalDateTime createdAt
) {
    public static ScheduleParticipantResponse from(ScheduleParticipant participant) {
        return new ScheduleParticipantResponse(
                participant.getId(),
                participant.getSchedule().getId(),
                participant.getUser().getId(),
                participant.getCreatedAt()
        );
    }
}