package com.dsa.schedule_manager.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record CursorResponse<T>(
        List<T> items,
        @Schema(nullable = true, description = "다음 페이지가 있을 때 마지막 일정 시각")
        LocalDateTime nextCursorScheduledAt,
        @Schema(nullable = true, description = "다음 페이지가 있을 때 마지막 일정 ID")
        Long nextCursorId,
        boolean hasNext
) {
}
