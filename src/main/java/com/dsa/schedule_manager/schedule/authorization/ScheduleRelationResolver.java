package com.dsa.schedule_manager.schedule.authorization;

import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;
import com.dsa.schedule_manager.schedule.repository.ScheduleParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleRelationResolver {

    private final ScheduleParticipantRepository participantRepository;

    /**
     * 사용자와 일정의 관계를 판단한다.
     *
     * OWNER    : 일정 작성자
     * PENDING  : 초대를 받고 아직 응답하지 않은 참여자
     * ACCEPTED : 초대를 수락한 참여자
     * REJECTED : 초대를 거절한 참여자
     * NONE     : 일정과 아무 관계가 없는 사용자
     */
    public ScheduleRelation resolve(Long userId, Schedule schedule) {

        // 작성자 여부를 가장 먼저 확인한다.
        // 작성자는 참여 정보와 관계없이 항상 OWNER가 우선한다.
        if (schedule.isOwnedBy(userId)) {
            return ScheduleRelation.OWNER;
        }

        // 작성자가 아니라면 참여자 정보를 조회한다.
        // 참여 정보가 있으면 현재 참여 상태를 ScheduleRelation으로 변환하고,
        // 참여 정보 자체가 없으면 NONE을 반환한다.
        return participantRepository
                .findByScheduleIdAndUserId(schedule.getId(), userId)
                .map(this::toRelation)
                .orElse(ScheduleRelation.NONE);
    }

    /**
     * 참여자의 현재 상태를 일정과의 관계로 변환한다.
     */
    private ScheduleRelation toRelation(ScheduleParticipant participant) {
        return switch (participant.getStatus()) {
            case PENDING -> ScheduleRelation.PENDING;
            case ACCEPTED -> ScheduleRelation.ACCEPTED;
            case REJECTED -> ScheduleRelation.REJECTED;
        };
    }
}