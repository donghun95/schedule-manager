package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleStatus;
import com.dsa.schedule_manager.schedule.dto.ScheduleStatusChangeRequest;
import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:rollbackdb;MODE=MariaDB;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class ScheduleStatusTransactionRollbackIntegrationTest {

    @Autowired
    ScheduleStatusService scheduleStatusService;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void 이력_저장이_실패하면_상태와_version도_롤백된다() {
        Schedule created = scheduleRepository.saveAndFlush(Schedule.create(
                1L,
                "롤백 검증 일정",
                null,
                LocalDateTime.of(2099, 8, 16, 10, 0)));
        jdbcTemplate.execute("""
                alter table schedule_status_history
                add constraint ck_history_changed_by_negative
                check (changed_by < 0)
                """);

        assertThatThrownBy(() -> scheduleStatusService.changeStatus(
                1L,
                created.getId(),
                new ScheduleStatusChangeRequest(
                        ScheduleStatus.IN_PROGRESS,
                        created.getVersion())))
                .isInstanceOf(DataIntegrityViolationException.class);

        Schedule rolledBack = scheduleRepository.findById(created.getId()).orElseThrow();
        assertThat(rolledBack.getStatus()).isEqualTo(ScheduleStatus.PLANNED);
        assertThat(rolledBack.getVersion()).isEqualTo(0L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from schedule_status_history",
                Long.class)).isZero();
    }
}
