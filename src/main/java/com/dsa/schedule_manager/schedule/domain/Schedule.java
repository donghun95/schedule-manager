package com.dsa.schedule_manager.schedule.domain;

import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import com.dsa.schedule_manager.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "schedules",
        indexes = {
                @Index(
                        name = "idx_schedules_owner_scheduled_id",
                        columnList = "owner_id, scheduled_at, id"),
                @Index(
                        name = "idx_schedules_scheduled_id",
                        columnList = "scheduled_at, id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Version
    @Column(nullable = false)
    private Long version;

    private Schedule(Long ownerId, String title, String description, LocalDateTime scheduledAt) {
        this.ownerId = ownerId;
        this.title = title;
        this.description = description;
        this.status = ScheduleStatus.PLANNED;
        this.scheduledAt = scheduledAt;
    }

    public static Schedule create(
            Long ownerId,
            String title,
            String description,
            LocalDateTime scheduledAt) {
        return new Schedule(ownerId, title, description, scheduledAt);
    }

    public void update(String title, String description, LocalDateTime scheduledAt) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (scheduledAt != null) {
            this.scheduledAt = scheduledAt;
        }
    }

    public boolean isOwnedBy(Long userId) {
        return ownerId.equals(userId);
    }

    public void changeStatus(ScheduleStatus target) {
        if (status == target) {
            throw new BusinessException(ErrorCode.SAME_STATUS_TRANSITION);
        }
        if (!status.canTransitionTo(target)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        status = target;
    }
}
