package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.dto.ScheduleParticipantResponse;
import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
import com.dsa.schedule_manager.user.domain.User; // User 패키지 경로 확인 필요
import com.dsa.schedule_manager.user.repository.UserRepository; // UserRepository 패키지 경로 확인 필요
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ScheduleParticipantServiceTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ScheduleParticipantService participantService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    private Long ownerId;
    private Long notOwnerId;
    private Long scheduleId;
    private Long targetUserId;

    @BeforeEach
    void setUp() {
        // 1. 작성자 User DB 저장
        User owner = userRepository.save(User.createNewUser("owner@test.com", "password", "작성자"));
        ownerId = owner.getId();

        // 2. 비작성자 User DB 저장
        User notOwner = userRepository.save(User.createNewUser("notowner@test.com", "password", "비작성자"));
        notOwnerId = notOwner.getId();

        // 3. 대상 참여자 User DB 저장
        User targetUser = userRepository.save(User.createNewUser("target@test.com", "password", "참여자"));
        targetUserId = targetUser.getId();

        // 4. 테스트용 일정 DB 저장
        Schedule schedule = Schedule.create(
                ownerId,
                "테스트 일정",
                "설명",
                LocalDateTime.now().plusDays(1)
        );
        Schedule savedSchedule = scheduleRepository.save(schedule);
        scheduleId = savedSchedule.getId();
    }

    // 1. 작성자만 참여자를 추가할 수 있음 (비작성자 요청 시 403)
    @Test
    void addParticipant_NotOwner_ThrowsForbidden() {
        assertThatThrownBy(() -> participantService.addParticipant(notOwnerId, scheduleId, targetUserId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    // 2. 중복 참여자 추가는 409
    @Test
    void addParticipant_AlreadyExists_ThrowsConflict() {
        // given: 참여자 이미 1회 정상 추가
        participantService.addParticipant(ownerId, scheduleId, targetUserId);

        // when & then: 동일한 참여자 재추가 시도
        assertThatThrownBy(() -> participantService.addParticipant(ownerId, scheduleId, targetUserId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPANT_ALREADY_EXISTS);
    }

    // 3. 없는 참여자 제거는 404
    @Test
    void removeParticipant_NotFound_ThrowsNotFound() {
        Long nonExistUserId = 888L;

        assertThatThrownBy(() -> participantService.removeParticipant(scheduleId, nonExistUserId, ownerId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }

    // 4. 작성자가 성공적으로 참여자를 추가함 (성공 케이스)
    @Test
    void addParticipant_Success() {
        ScheduleParticipantResponse response = participantService.addParticipant(ownerId, scheduleId, targetUserId);

        assertThat(response.userId()).isEqualTo(targetUserId);
    }
}