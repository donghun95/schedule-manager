package com.dsa.schedule_manager.schedule.repository;

import com.dsa.schedule_manager.schedule.domain.ScheduleStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleStatusHistoryRepository
        extends JpaRepository<ScheduleStatusHistory, Long> {

    List<ScheduleStatusHistory> findAllByScheduleIdOrderByIdAsc(Long scheduleId);
}
