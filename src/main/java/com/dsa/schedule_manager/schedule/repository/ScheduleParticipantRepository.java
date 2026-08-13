package com.dsa.schedule_manager.schedule.repository;

import com.dsa.schedule_manager.schedule.domain.ParticipantStatus;
import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleParticipantRepository
        extends JpaRepository<ScheduleParticipant, Long> {

    boolean existsByScheduleIdAndUserId(Long scheduleId, Long userId);

    boolean existsByScheduleIdAndUserIdAndStatus(
            Long scheduleId,
            Long userId,
            ParticipantStatus status
    );

    Optional<ScheduleParticipant> findByScheduleIdAndUserId(
            Long scheduleId,
            Long userId);


    List<ScheduleParticipant> findAllByScheduleIdOrderByIdAsc(Long scheduleId);

    @EntityGraph(attributePaths = "user")
    List<ScheduleParticipant> findAllByScheduleIdAndStatus(
            Long scheduleId,
            ParticipantStatus status
    );

    @EntityGraph(attributePaths = "user")
    @Query("""
            select sp
            from ScheduleParticipant sp
            where sp.schedule.id = :scheduleId
            order by sp.id asc
            """)
    List<ScheduleParticipant> findAllWithUserByScheduleId(
            @Param("scheduleId") Long scheduleId);

    void deleteAllByScheduleId(Long scheduleId);
}
