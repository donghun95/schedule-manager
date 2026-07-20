package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.dto.ScheduleCreateRequest;
import com.dsa.schedule_manager.schedule.dto.ScheduleResponse;
import com.dsa.schedule_manager.schedule.dto.ScheduleUpdateRequest;
import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    @Transactional
    public ScheduleResponse create(Long userId, ScheduleCreateRequest request) {
        Schedule schedule = Schedule.create(
                userId, request.title(), request.description(),

                request.scheduledAt());
        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }
    @Transactional(readOnly = true)
    public ScheduleResponse findById(Long userId, Long scheduleId) {
        Schedule schedule = getOwnedSchedule(userId, scheduleId);
        return ScheduleResponse.from(schedule);
    }
    @Transactional(readOnly = true)
    public List<ScheduleResponse> findMySchedules(Long userId) {
        return scheduleRepository.findAllByOwnerIdOrderByScheduledAtDesc(userId)
                .stream()
                .map(ScheduleResponse::from)
                .toList();
    }
    @Transactional
    public ScheduleResponse update(Long userId, Long scheduleId,
                                   ScheduleUpdateRequest request) {
        if (!request.hasChanges()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        Schedule schedule = getOwnedSchedule(userId, scheduleId);
        if (!schedule.getVersion().equals(request.version())) {
            throw new BusinessException(ErrorCode.SCHEDULE_CONFLICT);
        }
        schedule.update(request.title(), request.description(),
                request.scheduledAt());
        scheduleRepository.flush();
        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(Long userId, Long scheduleId) {
        Schedule schedule = getOwnedSchedule(userId, scheduleId);
        scheduleRepository.delete(schedule);
    }
    private Schedule getOwnedSchedule(Long userId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new

                        BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        if (!schedule.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return schedule;
    }
}