package com.dsa.schedule_manager.schedule.repository;

import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScheduleParticipantRepository
        extends JpaRepository<ScheduleParticipant, Long> {
    boolean existsByScheduleIdAndUserId(Long scheduleId, Long userId);
    Optional<ScheduleParticipant> findByScheduleIdAndUserId(Long scheduleId,
                                                            Long userId);
    void deleteAllByScheduleId(Long scheduleId);
}