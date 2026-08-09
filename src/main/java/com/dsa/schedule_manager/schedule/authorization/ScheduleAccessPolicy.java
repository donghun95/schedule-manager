package com.dsa.schedule_manager.schedule.authorization;

import com.dsa.schedule_manager.schedule.domain.ParticipantStatus;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.repository.ScheduleParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleAccessPolicy {

    private final ScheduleParticipantRepository participantRepository;

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

    public boolean canManageSchedule(Long userId, Schedule schedule) {
        return schedule.isOwnedBy(userId);
    }
}