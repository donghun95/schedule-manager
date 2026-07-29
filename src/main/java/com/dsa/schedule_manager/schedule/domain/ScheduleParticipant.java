package com.dsa.schedule_manager.schedule.domain;

import com.dsa.schedule_manager.user.domain.BaseEntity;
import com.dsa.schedule_manager.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "schedule_participants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_schedule_participant",
                columnNames = {"schedule_id", "user_id"}
        ),
        indexes = @Index(
                name = "idx_schedule_participants_user_schedule",
                columnList = "user_id, schedule_id"
        )
)
@Access(AccessType.FIELD)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private ScheduleParticipant(Schedule schedule, User user) {
        this.schedule = schedule;
        this.user = user;
    }

    public static ScheduleParticipant of(Schedule schedule, User user) {
        return new ScheduleParticipant(schedule, user);
    }
}