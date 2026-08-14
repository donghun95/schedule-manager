package com.dsa.schedule_manager.schedule.dto;

import com.dsa.schedule_manager.schedule.domain.ParticipantStatus;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;

import java.time.LocalDateTime;

public record ScheduleInvitationResponse(
        Long scheduleId,
        String title,
        LocalDateTime scheduledAt,
        String ownerNickname,
        ParticipantStatus status
) {

}