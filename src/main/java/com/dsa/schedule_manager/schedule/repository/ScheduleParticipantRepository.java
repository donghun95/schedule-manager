package com.dsa.schedule_manager.schedule.repository;

import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ScheduleParticipantRepository
        extends JpaRepository<ScheduleParticipant, Long> {
    boolean existsByScheduleIdAndUserId(Long scheduleId, Long userId);
    Optional<ScheduleParticipant> findByScheduleIdAndUserId(Long scheduleId,
                                                            Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ScheduleParticipant p WHERE p.schedule.id = :scheduleId")
    void deleteAllByScheduleId(Long scheduleId);
}