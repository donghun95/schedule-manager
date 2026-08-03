package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ScheduleParticipantServiceTest {

    @Autowired
    private ScheduleParticipantService participantService;

    // 1. 작성자만 참여자를 추가/제거할 수 있음 (비작성자 요청 시 403)
    @Test
    void addParticipant_NotOwner_ThrowsForbidden() {
        Long notOwnerId = 999L;
        Long scheduleId = 1L;
        Long targetUserId = 2L;

        assertThatThrownBy(() -> participantService.addParticipant(notOwnerId, scheduleId, targetUserId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
    }

    // 2. 중복 참여자 추가는 409
    @Test
    void addParticipant_AlreadyExists_ThrowsConflict() {

        Long ownerId = 999L;
        Long scheduleId = 1L;
        Long targetUserId = 2L;
        // given: 참여자 이미 1회 추가
        participantService.addParticipant(ownerId, scheduleId, targetUserId);

        // when & then
        assertThatThrownBy(() -> participantService.addParticipant(ownerId, scheduleId, targetUserId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PARTICIPANT_ALREADY_EXISTS);
    }

    // 3. 없는 참여자 제거는 404
    @Test
    void removeParticipant_NotFound_ThrowsNotFound() {
        Long nonExistUserId = 888L;
        Long ownerId = 999L;
        Long scheduleId = 1L;
        assertThatThrownBy(() -> participantService.removeParticipant(scheduleId, nonExistUserId, ownerId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }
}