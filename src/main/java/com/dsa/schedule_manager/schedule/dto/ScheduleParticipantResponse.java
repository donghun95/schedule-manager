package com.dsa.schedule_manager.schedule.dto;

import com.dsa.schedule_manager.schedule.domain.ParticipantStatus;
import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;

import java.time.LocalDateTime;

public record ScheduleParticipantResponse(
        Long userId,
        String email,
        String nickname,
        LocalDateTime joinedAt,
        ParticipantStatus status

) {
    public static ScheduleParticipantResponse from(ScheduleParticipant participant) {
        return new ScheduleParticipantResponse(
                participant.getUser().getId(),
                participant.getUser().getEmail(),
                participant.getUser().getNickname(),
                participant.getCreatedAt(),
                participant.getStatus()
        );
    }
}
