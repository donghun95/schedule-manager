package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import com.dsa.schedule_manager.schedule.authorization.ScheduleAccessPolicy;
import com.dsa.schedule_manager.schedule.authorization.ScheduleRelation;
import com.dsa.schedule_manager.schedule.authorization.ScheduleRelationResolver;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleStatus;
import com.dsa.schedule_manager.schedule.domain.ScheduleStatusHistory;
import com.dsa.schedule_manager.schedule.dto.ScheduleResponse;
import com.dsa.schedule_manager.schedule.dto.ScheduleStatusChangeRequest;
import com.dsa.schedule_manager.schedule.dto.ScheduleStatusHistoryResponse;
import com.dsa.schedule_manager.schedule.repository.ScheduleParticipantRepository;
import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
import com.dsa.schedule_manager.schedule.repository.ScheduleStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleStatusService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleParticipantRepository participantRepository;
    private final ScheduleStatusHistoryRepository historyRepository;
    private final ScheduleRelationResolver scheduleRelationResolver;
    private final ScheduleAccessPolicy scheduleAccessPolicy;

    @Transactional
    public ScheduleResponse changeStatus(
            Long userId,
            Long scheduleId,
            ScheduleStatusChangeRequest request) {
        Schedule schedule = findSchedule(scheduleId);
        requireOwner(schedule, userId);
        if (!schedule.getVersion().equals(request.version())) {
            throw new BusinessException(ErrorCode.SCHEDULE_CONFLICT);
        }

        ScheduleStatus fromStatus = schedule.getStatus();
        schedule.changeStatus(request.toStatus());
        scheduleRepository.flush();
        historyRepository.save(ScheduleStatusHistory.record(
                schedule,
                fromStatus,
                request.toStatus(),
                userId));
        return ScheduleResponse.from(schedule);
    }

    @Transactional(readOnly = true)
    public List<ScheduleStatusHistoryResponse> getHistory(
            Long userId,
            Long scheduleId) {
        Schedule schedule = findSchedule(scheduleId);
        ScheduleRelation relation =
                scheduleRelationResolver.resolve(userId, schedule);

        if(!scheduleAccessPolicy.canViewStatusHistory(relation)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return historyRepository.findAllByScheduleIdOrderByIdAsc(scheduleId)
                .stream()
                .map(ScheduleStatusHistoryResponse::from)
                .toList();
    }

    private Schedule findSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private void requireOwner(Schedule schedule, Long userId) {
        if (!schedule.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void requireAccessible(Schedule schedule, Long userId) {
        if (!schedule.isOwnedBy(userId)
                && !participantRepository.existsByScheduleIdAndUserId(schedule.getId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
