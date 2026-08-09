package com.dsa.schedule_manager.schedule.repository;

import com.dsa.schedule_manager.schedule.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findAllByOwnerIdOrderByScheduledAtDesc(Long ownerId);
}