package com.dsa.schedule_manager.schedule.authorization;

import com.dsa.schedule_manager.schedule.domain.ParticipantStatus;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import org.springframework.stereotype.Component;
import com.dsa.schedule_manager.schedule.repository.ScheduleParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleAccessPolicy {


    private final ScheduleParticipantRepository participantRepository;

    // 기존 서비스 호환용 - 일단 유지
    public boolean canViewSchedule(Long userId, Schedule schedule) {
        if (schedule.isOwnedBy(userId)) {
            return true;
        }

        return participantRepository.existsByScheduleIdAndUserIdAndStatus(
                schedule.getId(),
                userId,
                ParticipantStatus.ACCEPTED
        );
    }

    // 새 정책 테스트용

    public boolean canViewSchedule(ScheduleRelation relation) {
        return switch (relation) {
            case OWNER, ACCEPTED -> true;
            case PENDING, REJECTED, NONE -> false;
        };
    }

    public boolean canViewStatusHistory(ScheduleRelation relation) {
        return switch (relation) {
            case OWNER, ACCEPTED -> true;
            case PENDING, REJECTED, NONE -> false;
        };
    }

    public ParticipantVisibility participantVisibility(ScheduleRelation relation) {
        return switch (relation) {
            case OWNER -> ParticipantVisibility.ALL;
            case ACCEPTED -> ParticipantVisibility.ACCEPTED_ONLY;
            case PENDING, REJECTED, NONE -> ParticipantVisibility.NONE;
        };
    }

    public boolean canManageSchedule(ScheduleRelation relation) {
        return switch (relation) {
            case OWNER -> true;
            case PENDING, ACCEPTED, REJECTED, NONE -> false;
        };
    }
}