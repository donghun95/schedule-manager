package com.dsa.schedule_manager.schedule.repository;

import com.dsa.schedule_manager.config.JpaAuditingConfig;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class ScheduleRepositoryTest {

    @Autowired
    ScheduleRepository scheduleRepository;

    @Test
    void 내_일정만_예정일_역순으로_조회한다() {
        LocalDateTime base = LocalDateTime.of(2099, 1, 1, 10, 0);
        scheduleRepository.save(Schedule.create(1L, "내 일정 1", null, base.plusDays(1)));
        scheduleRepository.save(Schedule.create(1L, "내 일정 2", null, base.plusDays(2)));
        scheduleRepository.save(Schedule.create(2L, "남의 일정", null, base.plusDays(3)));

        List<Schedule> result =
                scheduleRepository.findAllByOwnerIdOrderByScheduledAtDesc(1L);

        assertThat(result).extracting(Schedule::getTitle)
                .containsExactly("내 일정 2", "내 일정 1");
    }
}
