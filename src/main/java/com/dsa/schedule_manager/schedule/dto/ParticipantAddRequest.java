package com.dsa.schedule_manager.schedule.dto;

import jakarta.validation.constraints.NotNull;

public record ParticipantAddRequest(
        @NotNull Long userId
) {
}
