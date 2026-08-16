package com.dsa.schedule_manager.schedule.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "schedule_status_history",
        indexes = @Index(
                name = "idx_schedule_status_history_schedule_id_id",
                columnList = "schedule_id, id"
        )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 20)
    private ScheduleStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private ScheduleStatus toStatus;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @CreatedDate
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    // 정적 팩토리 메서드 추가
    public static ScheduleStatusHistory record(
            Schedule schedule,
            ScheduleStatus fromStatus,
            ScheduleStatus toStatus,
            Long changedBy) {

        return new ScheduleStatusHistory(schedule, fromStatus, toStatus, changedBy);
    }

    // 생성자 (private으로 외부 직접 생성 제한)
    private ScheduleStatusHistory(
            Schedule schedule,
            ScheduleStatus fromStatus,
            ScheduleStatus toStatus,
            Long changedBy) {

        this.schedule = schedule;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
    }
}