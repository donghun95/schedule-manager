package com.dsa.schedule_manager.schedule.service;

import com.dsa.schedule_manager.common.error.BusinessException;
import com.dsa.schedule_manager.common.error.ErrorCode;
import com.dsa.schedule_manager.schedule.domain.Schedule;
import com.dsa.schedule_manager.schedule.domain.ScheduleParticipant;
import com.dsa.schedule_manager.schedule.domain.ScheduleStatus;
import com.dsa.schedule_manager.schedule.dto.ScheduleSearchRequest;
import com.dsa.schedule_manager.schedule.repository.ScheduleParticipantRepository;
import com.dsa.schedule_manager.schedule.repository.ScheduleRepository;
import com.dsa.schedule_manager.user.domain.User;
import com.dsa.schedule_manager.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        properties = "spring.jpa.properties.hibernate.generate_statistics=true"
)
@ActiveProfiles("test")
@Transactional
class ScheduleQueryIntegrationTest {

    @Autowired
    ScheduleService scheduleService;

    @Autowired
    ScheduleParticipantService participantService;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    ScheduleParticipantRepository participantRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();
    }

    @Test
    void compositeCursorReturnsOwnedAndParticipatingSchedulesWithoutDuplicates() {
        User me = saveUser(
                "me@example.com",
                "me"
        );

        User owner = saveUser(
                "owner@example.com",
                "owner"
        );

        LocalDateTime sameTime =
                LocalDateTime.of(2099, 8, 10, 10, 0);

        Schedule firstOwned = scheduleRepository.save(
                Schedule.create(
                        me.getId(),
                        "owned-1",
                        null,
                        sameTime
                )
        );

        Schedule secondOwned = scheduleRepository.save(
                Schedule.create(
                        me.getId(),
                        "owned-2",
                        null,
                        sameTime
                )
        );

        Schedule participating = scheduleRepository.save(
                Schedule.create(
                        owner.getId(),
                        "participating",
                        null,
                        sameTime.minusDays(1)
                )
        );

        scheduleRepository.save(
                Schedule.create(
                        owner.getId(),
                        "inaccessible",
                        null,
                        sameTime.plusDays(1)
                )
        );

        /*
         * 일반 일정 목록에는 ACCEPTED 참여자만 노출된다.
         *
         * ScheduleParticipant.of(...)의 기본 상태는 PENDING이므로
         * 이 테스트에서는 실제 참여 중인 일정을 만들기 위해
         * accept() 처리 후 저장한다.
         */
        ScheduleParticipant participant =
                ScheduleParticipant.of(participating, me);

        participant.accept();

        participantRepository.save(participant);

        entityManager.flush();
        entityManager.clear();

        var firstPage = scheduleService.findMySchedules(
                me.getId(),
                request(
                        null,
                        null,
                        2
                )
        );

        assertThat(firstPage.items())
                .extracting("id")
                .containsExactly(
                        secondOwned.getId(),
                        firstOwned.getId()
                );

        assertThat(firstPage.hasNext()).isTrue();

        assertThat(firstPage.nextCursorScheduledAt())
                .isEqualTo(sameTime);

        assertThat(firstPage.nextCursorId())
                .isEqualTo(firstOwned.getId());

        var secondPage = scheduleService.findMySchedules(
                me.getId(),
                request(
                        firstPage.nextCursorScheduledAt(),
                        firstPage.nextCursorId(),
                        2
                )
        );

        assertThat(secondPage.items())
                .extracting("id")
                .containsExactly(participating.getId());

        assertThat(secondPage.items().getFirst().accessType())
                .isEqualTo("PARTICIPANT");

        assertThat(secondPage.hasNext()).isFalse();

        HashSet<Long> allIds = new HashSet<>();

        firstPage.items()
                .forEach(item -> allIds.add(item.id()));

        secondPage.items()
                .forEach(item -> allIds.add(item.id()));

        assertThat(allIds).hasSize(3);
    }

    @Test
    void incompleteCursorIsRejectedWith400ErrorCode() {
        User me = saveUser(
                "cursor@example.com",
                "cursor"
        );

        assertThatThrownBy(() ->
                scheduleService.findMySchedules(
                        me.getId(),
                        request(
                                LocalDateTime.of(
                                        2099,
                                        8,
                                        1,
                                        0,
                                        0
                                ),
                                null,
                                20
                        )
                )
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_CURSOR)
                );
    }

    @Test
    void statusAndDateFiltersIncludeTheirBoundaryValues() {
        User me = saveUser(
                "filter@example.com",
                "filter"
        );

        LocalDateTime fromAt =
                LocalDateTime.of(
                        2099,
                        8,
                        1,
                        10,
                        0
                );

        LocalDateTime toAt =
                LocalDateTime.of(
                        2099,
                        8,
                        3,
                        10,
                        0
                );

        Schedule before = scheduleRepository.save(
                Schedule.create(
                        me.getId(),
                        "before",
                        null,
                        fromAt.minusSeconds(1)
                )
        );

        Schedule fromBoundary = scheduleRepository.save(
                Schedule.create(
                        me.getId(),
                        "from",
                        null,
                        fromAt
                )
        );

        Schedule toBoundary = scheduleRepository.save(
                Schedule.create(
                        me.getId(),
                        "to",
                        null,
                        toAt
                )
        );

        Schedule done = scheduleRepository.save(
                Schedule.create(
                        me.getId(),
                        "done",
                        null,
                        fromAt.plusDays(1)
                )
        );

        done.changeStatus(ScheduleStatus.IN_PROGRESS);
        done.changeStatus(ScheduleStatus.DONE);

        scheduleRepository.save(
                Schedule.create(
                        me.getId(),
                        "after",
                        null,
                        toAt.plusSeconds(1)
                )
        );

        entityManager.flush();
        entityManager.clear();

        var response = scheduleService.findMySchedules(
                me.getId(),
                new ScheduleSearchRequest(
                        ScheduleStatus.PLANNED,
                        fromAt,
                        toAt,
                        null,
                        null,
                        20
                )
        );

        assertThat(response.items())
                .extracting("id")
                .containsExactly(
                        toBoundary.getId(),
                        fromBoundary.getId()
                )
                .doesNotContain(
                        before.getId(),
                        done.getId()
                );
    }

    @Test
    void lazyParticipantMappingCausesOnePlusNQueries() {
        Long scheduleId =
                saveScheduleWithThreeParticipants();

        statistics.clear();

        List<ScheduleParticipant> participants =
                participantRepository
                        .findAllByScheduleIdOrderByIdAsc(
                                scheduleId
                        );

        participants.forEach(
                participant ->
                        participant
                                .getUser()
                                .getNickname()
        );

        long queryCount =
                statistics.getPrepareStatementCount();

        System.out.println(
                "[N+1 Before] prepared statements = "
                        + queryCount
        );

        assertThat(queryCount).isEqualTo(4);
    }

    @Test
    void entityGraphLoadsParticipantsAndUsersInOneQuery() {
        Long scheduleId =
                saveScheduleWithThreeParticipants();

        statistics.clear();

        List<ScheduleParticipant> participants =
                participantRepository
                        .findAllWithUserByScheduleId(
                                scheduleId
                        );

        participants.forEach(
                participant ->
                        participant
                                .getUser()
                                .getNickname()
        );

        long queryCount =
                statistics.getPrepareStatementCount();

        System.out.println(
                "[N+1 After] prepared statements = "
                        + queryCount
        );

        assertThat(queryCount).isEqualTo(1);
    }

    @Test
    void participantServiceUsesEntityGraphOnTheActualServicePath() {
        Long scheduleId =
                saveScheduleWithThreeParticipants();

        Long ownerId =
                scheduleRepository
                        .findById(scheduleId)
                        .orElseThrow()
                        .getOwnerId();

        entityManager.clear();

        statistics.clear();

        var participants =
                participantService.getParticipants(
                        ownerId,
                        scheduleId
                );

        assertThat(participants)
                .hasSize(3);

        assertThat(participants)
                .extracting("nickname")
                .containsExactly(
                        "user-1",
                        "user-2",
                        "user-3"
                );

        assertThat(
                statistics.getPrepareStatementCount()
        ).isEqualTo(2);
    }

    private Long saveScheduleWithThreeParticipants() {
        User owner = saveUser(
                "query-owner@example.com",
                "owner"
        );

        Schedule schedule = scheduleRepository.save(
                Schedule.create(
                        owner.getId(),
                        "N+1 query check",
                        null,
                        LocalDateTime.of(
                                2099,
                                9,
                                1,
                                10,
                                0
                        )
                )
        );

        for (int i = 1; i <= 3; i++) {
            User participant = saveUser(
                    "query-user-%d@example.com".formatted(i),
                    "user-%d".formatted(i)
            );

            participantRepository.save(
                    ScheduleParticipant.of(
                            schedule,
                            participant
                    )
            );
        }

        entityManager.flush();
        entityManager.clear();

        return schedule.getId();
    }

    private User saveUser(
            String email,
            String nickname
    ) {
        return userRepository.save(
                User.createNewUser(
                        email,
                        "encoded",
                        nickname
                )
        );
    }

    private ScheduleSearchRequest request(
            LocalDateTime cursorScheduledAt,
            Long cursorId,
            int size
    ) {
        return new ScheduleSearchRequest(
                null,
                null,
                null,
                cursorScheduledAt,
                cursorId,
                size
        );
    }
}