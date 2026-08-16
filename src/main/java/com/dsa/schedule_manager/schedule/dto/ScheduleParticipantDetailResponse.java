package com.dsa.schedule_manager.schedule.dto;

import com.dsa.schedule_manager.schedule.domain.ParticipantStatus;
import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;

import java.time.LocalDateTime;

public record ScheduleParticipantDetailResponse(
        Long userId,
        String nickname,
        LocalDateTime invitedAt,
        ParticipantStatus status
) {

    public static ScheduleParticipantDetailResponse from(
            ScheduleParticipant participant
    ) {
        return new ScheduleParticipantDetailResponse(
                participant.getUser().getId(),
                participant.getUser().getNickname(),
                participant.getCreatedAt(),
                participant.getStatus()
        );
    }
}