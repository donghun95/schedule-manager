package com.dsa.schedule_manager.schedule.dto;

import com.dsa.schedule_manager.schedule.domain.ScheduleStatus;
import com.dsa.schedule_manager.schedule.domain.ScheduleStatusHistory;
import java.time.LocalDateTime;

public record ScheduleStatusHistoryResponse(
        Long id,
        ScheduleStatus fromStatus,
        ScheduleStatus toStatus,
        Long changedBy,
        LocalDateTime changedAt
) {
    public static ScheduleStatusHistoryResponse from(ScheduleStatusHistory history) {
        return new ScheduleStatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy(),
                history.getChangedAt()
        );
    }
}
