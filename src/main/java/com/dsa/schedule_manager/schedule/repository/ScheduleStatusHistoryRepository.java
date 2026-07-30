package com.dsa.schedule_manager.schedule.repository;

import com.dsa.schedule_manager.schedule.domain.ScheduleStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleStatusHistoryRepository extends JpaRepository<ScheduleStatusHistory, Long> {
}