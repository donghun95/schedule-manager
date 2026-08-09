package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;
import com.dsa.schedule_manager.schedule.dto.ScheduleParticipantResponse;
import com.dsa.schedule_manager.schedule.repository.ScheduleParticipantRepository;
import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
import com.dsa.schedule_manager.user.domain.User;
import com.dsa.schedule_manager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleParticipantService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleParticipantRepository participantRepository;
    private final UserRepository userRepository;

    @Transactional
    public ScheduleParticipantResponse addParticipant(Long requesterId, Long scheduleId, Long targetUserId) {
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
                ScheduleParticipant.of(schedule, target)
        );

        return ScheduleParticipantResponse.from(saved);
    }

    private Schedule getOwnedSchedule(Long requesterId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (!schedule.isOwnedBy(requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return schedule;
    }
    // 여기에 일단 내 권한을 한번 체크하고 참여자 조회하고 만약 참여자가 없으면 404 나오고 있다면 삭제하는 참여자 제게 API
    @Transactional
    public void removeParticipant(Long scheduleId, Long targetUserId, Long currentUserId){
        // 1 일정이 존재 확인
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        // 2 작성자 권한 확인 (요청자가 일정 작성자가 아니면 예외)
        if(!schedule.isOwnedBy(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        // 3. 참여자 조회
        ScheduleParticipant participant = participantRepository.findByScheduleIdAndUserId(scheduleId,targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND)); // 404 예외 처리

        // 4. 삭제
        participantRepository.delete(participant);
    }

}