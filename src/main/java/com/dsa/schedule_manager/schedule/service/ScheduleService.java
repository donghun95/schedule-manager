package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleStatus;
import com.dsa.schedule_manager.schedule.dto.*;
import com.dsa.schedule_manager.schedule.repository.ScheduleParticipantRepository;
import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleParticipantRepository participantRepository;

    @Transactional
    public ScheduleResponse create(Long userId, ScheduleCreateRequest request) {
        Schedule schedule = Schedule.create(
                userId,
                request.title(),
                request.description(),
                request.scheduledAt()
        );
        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    @Transactional(readOnly = true)
    public ScheduleResponse findById(Long userId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        if (!schedule.isOwnedBy(userId)
                && !participantRepository.existsByScheduleIdAndUserId(scheduleId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return ScheduleResponse.from(schedule);
    }

    @Transactional(readOnly = true)
    public CursorResponse<ScheduleSummaryResponse> findMySchedules(
            Long userId,
            ScheduleSearchRequest request) {
        validateSearchRequest(request);

        int size = request.effectiveSize();
        List<Schedule> rows = scheduleRepository.findAccessibleCursorPage(
                userId,
                request.status(),
                request.fromAt(),
                request.toAt(),
                request.cursorScheduledAt(),
                request.cursorId(),
                PageRequest.of(0, size + 1));

        boolean hasNext = rows.size() > size;
        List<Schedule> page = hasNext ? rows.subList(0, size) : rows;
        List<ScheduleSummaryResponse> items = page.stream()
                .map(schedule -> ScheduleSummaryResponse.from(schedule, userId))
                .toList();

        Schedule nextCursorSource = hasNext ? page.get(page.size() - 1) : null;
        return new CursorResponse<>(
                items,
                nextCursorSource == null ? null : nextCursorSource.getScheduledAt(),
                nextCursorSource == null ? null : nextCursorSource.getId(),
                hasNext);
    }

    @Transactional
    public ScheduleResponse update(
            Long userId,
            Long scheduleId,
            ScheduleUpdateRequest request) {
        if (!request.hasChanges()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        Schedule schedule = getOwnedSchedule(userId, scheduleId);

        if (!schedule.getVersion().equals(request.version())) {
            throw new BusinessException(ErrorCode.SCHEDULE_CONFLICT);
        }

        schedule.update(request.title(), request.description(), request.scheduledAt());

        // Flush before mapping so the response contains the incremented version and
        // an optimistic-lock conflict is translated while this request is active.
        scheduleRepository.flush();
        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(Long userId, Long scheduleId) {
        Schedule schedule = getOwnedSchedule(userId, scheduleId);
        if (schedule.getStatus() != ScheduleStatus.PLANNED) {
            throw new BusinessException(ErrorCode.SCHEDULE_DELETE_NOT_ALLOWED);
        }
        participantRepository.deleteAllByScheduleId(scheduleId);
        scheduleRepository.delete(schedule);
    }

    private Schedule getOwnedSchedule(Long userId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (!schedule.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return schedule;
    }

    private void validateSearchRequest(ScheduleSearchRequest request) {
        if (request.hasOnlyOneCursorValue()) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
        if (request.hasInvalidDateRange()) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
    }
}
