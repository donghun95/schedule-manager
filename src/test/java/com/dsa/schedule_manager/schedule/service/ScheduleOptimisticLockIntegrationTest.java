package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ScheduleOptimisticLockIntegrationTest {

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void 같은_version을_읽은_두_트랜잭션_중_하나는_충돌한다() throws Exception {
        Schedule saved = scheduleRepository.saveAndFlush(Schedule.create(
                1L,
                "원본",
                null,
                LocalDateTime.of(2099, 1, 1, 10, 0)
        ));

        CountDownLatch loaded = new CountDownLatch(2);
        CountDownLatch updateTogether = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<String> first = executor.submit(
                    () -> updateInTransaction(saved.getId(), "첫 번째", loaded, updateTogether));
            Future<String> second = executor.submit(
                    () -> updateInTransaction(saved.getId(), "두 번째", loaded, updateTogether));

            boolean bothLoaded = loaded.await(5, TimeUnit.SECONDS);
            updateTogether.countDown();
            assertThat(bothLoaded).isTrue();

            assertThat(List.of(
                    first.get(5, TimeUnit.SECONDS),
                    second.get(5, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("SUCCESS", "CONFLICT");
        }
    }

    private String updateInTransaction(
            Long scheduleId,
            String title,
            CountDownLatch loaded,
            CountDownLatch updateTogether) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        try {
            transaction.executeWithoutResult(status -> {
                Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow();
                loaded.countDown();
                await(updateTogether);
                schedule.update(title, null, null);
                scheduleRepository.flush();
            });
            return "SUCCESS";
        } catch (ObjectOptimisticLockingFailureException ex) {
            return "CONFLICT";
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 수정 대기 시간이 초과됐습니다.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
