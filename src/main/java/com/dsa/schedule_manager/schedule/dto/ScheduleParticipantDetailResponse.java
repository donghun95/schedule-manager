package com.dsa.schedule_manager.schedule.dto;

import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;
import java.time.LocalDateTime;

public record ScheduleParticipantDetailResponse(
        Long id,
        Long userId,
        String nickname,
        LocalDateTime joinedAt
) {
    public static ScheduleParticipantDetailResponse from(ScheduleParticipant participant) {
        return new ScheduleParticipantDetailResponse(
                participant.getId(),
                participant.getUser().getId(),
                participant.getUser().getNickname(),
                participant.getCreatedAt());
    }
}
