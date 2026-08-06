package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleStatus;
import com.dsa.schedule_manager.schedule.domain.ScheduleStatusHistory;
import com.dsa.schedule_manager.schedule.dto.ScheduleStatusChangeRequest;
import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
import com.dsa.schedule_manager.schedule.repository.ScheduleStatusHistoryRepository;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:statuslockdb;MODE=MariaDB;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
@Import(ScheduleStatusOptimisticLockIntegrationTest.RepositoryBarrierConfig.class)
class ScheduleStatusOptimisticLockIntegrationTest {

    @Autowired
    ScheduleStatusService scheduleStatusService;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    ScheduleStatusHistoryRepository historyRepository;

    @Autowired
    RepositoryLoadBarrier repositoryLoadBarrier;

    @Test
    void 같은_version의_상태_변경은_하나만_성공하고_이력도_한_건만_남는다() throws Exception {
        Schedule saved = scheduleRepository.saveAndFlush(Schedule.create(
                1L,
                "상태 동시성 검증",
                null,
                LocalDateTime.of(2099, 8, 17, 10, 0)));

        repositoryLoadBarrier.arm(saved.getId());

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<String> first = executor.submit(() -> changeStatus(
                    saved.getId(),
                    ScheduleStatus.IN_PROGRESS));
            Future<String> second = executor.submit(() -> changeStatus(
                    saved.getId(),
                    ScheduleStatus.CANCELED));

            boolean bothLoaded = repositoryLoadBarrier.awaitBothLoaded();
            repositoryLoadBarrier.continueTogether();

            assertThat(bothLoaded).isTrue();
            assertThat(List.of(
                    first.get(5, TimeUnit.SECONDS),
                    second.get(5, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("SUCCESS", "CONFLICT");
        }

        Schedule result = scheduleRepository.findById(saved.getId()).orElseThrow();
        List<ScheduleStatusHistory> histories =
                historyRepository.findAllByScheduleIdOrderByIdAsc(saved.getId());

        assertThat(result.getVersion()).isEqualTo(1L);
        assertThat(histories).hasSize(1);
        assertThat(histories.getFirst().getFromStatus()).isEqualTo(ScheduleStatus.PLANNED);
        assertThat(histories.getFirst().getToStatus()).isEqualTo(result.getStatus());
    }

    private String changeStatus(Long scheduleId, ScheduleStatus toStatus) {
        try {
            scheduleStatusService.changeStatus(
                    1L,
                    scheduleId,
                    new ScheduleStatusChangeRequest(toStatus, 0L));
            return "SUCCESS";
        } catch (ObjectOptimisticLockingFailureException ex) {
            return "CONFLICT";
        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.SCHEDULE_CONFLICT) {
                return "CONFLICT";
            }
            throw ex;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class RepositoryBarrierConfig {

        @Bean
        RepositoryLoadBarrier repositoryLoadBarrier() {
            return new RepositoryLoadBarrier();
        }

        @Bean
        Advisor scheduleRepositoryFindByIdAdvisor(RepositoryLoadBarrier barrier) {
            AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
            pointcut.setExpression(
                    "execution(* com.dsa.schedule_manager.schedule.repository.ScheduleRepository.findById(..))");
            return new DefaultPointcutAdvisor(pointcut, barrier);
        }
    }

    static class RepositoryLoadBarrier implements MethodInterceptor {

        private final AtomicInteger interceptedLoads = new AtomicInteger();
        private CountDownLatch loaded;
        private CountDownLatch continueTogether;
        private Long scheduleId;

        synchronized void arm(Long scheduleId) {
            this.scheduleId = scheduleId;
            this.interceptedLoads.set(0);
            this.loaded = new CountDownLatch(2);
            this.continueTogether = new CountDownLatch(1);
        }

        boolean awaitBothLoaded() throws InterruptedException {
            return loaded.await(5, TimeUnit.SECONDS);
        }

        void continueTogether() {
            continueTogether.countDown();
        }

        @Override
        public Object invoke(org.aopalliance.intercept.MethodInvocation invocation) throws Throwable {
            Object result = invocation.proceed();
            Object requestedId = invocation.getArguments()[0];
            if (scheduleId != null
                    && scheduleId.equals(requestedId)
                    && interceptedLoads.incrementAndGet() <= 2) {
                loaded.countDown();
                try {
                    if (!continueTogether.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("동시 상태 변경 대기 시간이 초과됐습니다.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
            }
            return result;
        }
    }
}
