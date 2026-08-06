package com.dsa.schedule_manager.schedule.repository;

import com.dsa.schedule_manager.config.JpaAuditingConfig;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;
import com.dsa.schedule_manager.user.domain.User;
import com.dsa.schedule_manager.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class ScheduleParticipantRepositoryTest {

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    ScheduleParticipantRepository participantRepository;

    @Autowired
    UserRepository userRepository;

    @Test
    void 같은_일정과_사용자_조합은_DB_유니크_제약으로_막는다() {
        Schedule schedule = scheduleRepository.saveAndFlush(Schedule.create(
                1L,
                "유니크 제약 일정",
                null,
                LocalDateTime.of(2099, 8, 15, 10, 0)));
        User participant = userRepository.saveAndFlush(
                User.createNewUser("unique@example.com", "encoded-password", "참여자"));
        participantRepository.saveAndFlush(ScheduleParticipant.of(schedule, participant));

        assertThatThrownBy(() -> participantRepository.saveAndFlush(
                ScheduleParticipant.of(schedule, participant)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
