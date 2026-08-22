package com.dsa.schedule_manager.schedule.authorization;

import org.springframework.stereotype.Component;

@Component
public class ScheduleAccessPolicy {
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

}