package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleStatus;
import com.dsa.schedule_manager.schedule.domain.ScheduleStatusHistory;
import com.dsa.schedule_manager.schedule.dto.ScheduleCreateRequest;
import com.dsa.schedule_manager.schedule.dto.ScheduleResponse;
import com.dsa.schedule_manager.schedule.dto.ScheduleStatusChangeRequest;
import com.dsa.schedule_manager.schedule.dto.ScheduleUpdateRequest;
import com.dsa.schedule_manager.schedule.repository.ScheduleParticipantRepository;
import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
import com.dsa.schedule_manager.schedule.repository.ScheduleStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleStatusHistoryRepository historyRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;

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

        // 1. 일정 존재 여부 확인
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        // 2. 작성자 권한 검증
        if (!schedule.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 3. PLANNED 상태 검증 (PLANNED가 아니면 삭제 불가)
        if (schedule.getStatus() != ScheduleStatus.PLANNED) {
            throw new BusinessException(ErrorCode.SCHEDULE_DELETE_NOT_ALLOWED);
        }

        // 4. FK 제약 조건 해결: 연관된 참여자(ScheduleParticipant) 먼저 삭제
        scheduleParticipantRepository.deleteAllByScheduleId(scheduleId);

        // 5. 일정 삭제
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

    @Transactional // ★ 핵심: 상태 변경과 이력 저장을 하나의 트랜잭션으로 묶음
    public ScheduleResponse changeStatus(
            Long userId, Long scheduleId, ScheduleStatusChangeRequest request) {

        Schedule schedule = findSchedule(scheduleId);
        requireOwner(schedule, userId); // 작성자 권한 검증

        // 요청 version 검증
        if (!schedule.getVersion().equals(request.version())) {
            throw new BusinessException(ErrorCode.SCHEDULE_CONFLICT);
        }

        ScheduleStatus fromStatus = schedule.getStatus();

        // 1. 엔티티 상태 변경 (전이 규칙 검증 포함)
        schedule.changeStatus(request.toStatus());

        // 2. DB에 UPDATE를 먼저 보내 version 충돌을 감지 (flush)
        scheduleRepository.flush();

        // 3. 상태 변경 이력 저장
        historyRepository.save(ScheduleStatusHistory.record(
                schedule, fromStatus, request.toStatus(), userId
        ));

        return ScheduleResponse.from(schedule);
    }

    // 1. scheduleId로 일정을 조회하고, 없으면 404 예외 발생
    private Schedule findSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    // 2. 작성자 권한 검증 (일정 작성자가 아니면 403 예외 발생)
    private void requireOwner(Schedule schedule, Long userId) {
        if (!schedule.isOwnedBy(userId)) { // 또는 !schedule.getOwnerId().equals(userId)
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}