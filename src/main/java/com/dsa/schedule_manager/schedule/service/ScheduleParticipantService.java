package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import com.dsa.schedule_manager.schedule.authorization.ParticipantVisibility;
import com.dsa.schedule_manager.schedule.authorization.ScheduleAccessPolicy;
import com.dsa.schedule_manager.schedule.authorization.ScheduleRelation;
import com.dsa.schedule_manager.schedule.authorization.ScheduleRelationResolver;
import com.dsa.schedule_manager.schedule.domain.ParticipantStatus;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;
import com.dsa.schedule_manager.schedule.dto.ScheduleParticipantDetailResponse;
import com.dsa.schedule_manager.schedule.dto.ScheduleParticipantResponse;
import com.dsa.schedule_manager.schedule.repository.ScheduleParticipantRepository;
import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
import com.dsa.schedule_manager.user.domain.User;
import com.dsa.schedule_manager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleParticipantService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final ScheduleRelationResolver scheduleRelationResolver;
    private final ScheduleAccessPolicy scheduleAccessPolicy;

    @Transactional
    public ScheduleParticipantResponse addParticipant(
            Long requesterId,
            Long scheduleId,
            Long targetUserId) {
        Schedule schedule = getOwnedSchedule(requesterId, scheduleId);
        if (schedule.isOwnedBy(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_ADD_OWNER_AS_PARTICIPANT);
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (participantRepository.existsByScheduleIdAndUserId(scheduleId, targetUserId)) {
            throw new BusinessException(ErrorCode.PARTICIPANT_ALREADY_EXISTS);
        }

        ScheduleParticipant saved = participantRepository.saveAndFlush(
                ScheduleParticipant.of(schedule, target));
        return ScheduleParticipantResponse.from(saved);
    }

    @Transactional
    public void removeParticipant(
            Long requesterId,
            Long scheduleId,
            Long targetUserId) {
        getOwnedSchedule(requesterId, scheduleId);
        ScheduleParticipant participant = participantRepository
                .findByScheduleIdAndUserId(scheduleId, targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));
        participantRepository.delete(participant);
    }

    @Transactional(readOnly = true)
    public List<ScheduleParticipantDetailResponse> getParticipants(
            Long requesterId,
            Long scheduleId) {

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        ScheduleRelation relation =
                scheduleRelationResolver.resolve(requesterId, schedule);

        ParticipantVisibility visibility =
                scheduleAccessPolicy.participantVisibility(relation);

        List<ScheduleParticipant> participants = switch (visibility) {
            case ALL ->
                    participantRepository.findAllWithUserByScheduleId(scheduleId);

            case ACCEPTED_ONLY ->
                    participantRepository.findAllByScheduleIdAndStatus(
                            scheduleId,
                            ParticipantStatus.ACCEPTED
                    );

            case NONE ->
                    throw new BusinessException(ErrorCode.FORBIDDEN);
        };

        return participants.stream()
                .map(ScheduleParticipantDetailResponse::from)
                .toList();
    }

    private Schedule getOwnedSchedule(Long requesterId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        if (!schedule.isOwnedBy(requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return schedule;
    }
    // requestId - 누가 수락하는가? scheduleId - 어떤 일정의 초대를 수락하는가?
    @Transactional
    public void acceptInvitation(
            Long requesterId,
            Long scheduleId) {

        ScheduleParticipant participant =
                participantRepository.findByScheduleIdAndUserId(
                                scheduleId,
                                requesterId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.PARTICIPANT_NOT_FOUND
                                ));
        participant.accept();
    }

    @Transactional
    public void rejectInvitation(
            Long requesterId,
            Long scheduleId) {

        ScheduleParticipant participant =
                participantRepository.findByScheduleIdAndUserId(
                                scheduleId,
                                requesterId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.PARTICIPANT_NOT_FOUND
                                ));
        participant.reject();
    }
}
