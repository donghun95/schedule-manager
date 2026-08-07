package com.dsa.schedule_manager.schedule.repository;

import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findAllByOwnerIdOrderByScheduledAtDesc(Long ownerId);

    @Query("""
            select s
            from Schedule s
            where (
                s.ownerId = :userId
                or exists (
                    select sp.id
                    from ScheduleParticipant sp
                    where sp.schedule = s
                      and sp.user.id = :userId
                      and sp.status =
                          com.dsa.schedule_manager.schedule.domain.ParticipantStatus.ACCEPTED
                )
            )
              and (:status is null or s.status = :status)
              and (:fromAt is null or s.scheduledAt >= :fromAt)
              and (:toAt is null or s.scheduledAt <= :toAt)
              and (
                  :cursorScheduledAt is null
                  or s.scheduledAt < :cursorScheduledAt
                  or (s.scheduledAt = :cursorScheduledAt and s.id < :cursorId)
              )
            order by s.scheduledAt desc, s.id desc
            """)
    List<Schedule> findAccessibleCursorPage(
            @Param("userId") Long userId,
            @Param("status") ScheduleStatus status,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt,
            @Param("cursorScheduledAt") LocalDateTime cursorScheduledAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);
}
