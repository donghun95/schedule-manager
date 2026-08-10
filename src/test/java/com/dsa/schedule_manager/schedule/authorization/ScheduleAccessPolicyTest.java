package com.dsa.schedule_manager.schedule.authorization;

import com.dsa.schedule_manager.schedule.repository.ScheduleParticipantRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ScheduleAccessPolicyTest {

    private final ScheduleParticipantRepository participantRepository =
            mock(ScheduleParticipantRepository.class);

    private final ScheduleAccessPolicy policy =
            new ScheduleAccessPolicy(participantRepository);

    @Test
    void owner는_일정_단건과_이력을_조회할_수_있다() {
        assertThat(policy.canViewSchedule(ScheduleRelation.OWNER))
                .isTrue();

        assertThat(policy.canViewStatusHistory(ScheduleRelation.OWNER))
                .isTrue();
    }

    @Test
    void accepted는_일정_단건과_이력을_조회할_수_있다() {
        assertThat(policy.canViewSchedule(ScheduleRelation.ACCEPTED))
                .isTrue();

        assertThat(policy.canViewStatusHistory(ScheduleRelation.ACCEPTED))
                .isTrue();
    }

    @Test
    void pending은_일정_단건과_이력을_조회할_수_없다() {
        assertThat(policy.canViewSchedule(ScheduleRelation.PENDING))
                .isFalse();

        assertThat(policy.canViewStatusHistory(ScheduleRelation.PENDING))
                .isFalse();
    }

    @Test
    void rejected는_일정_단건과_이력을_조회할_수_없다() {
        assertThat(policy.canViewSchedule(ScheduleRelation.REJECTED))
                .isFalse();

        assertThat(policy.canViewStatusHistory(ScheduleRelation.REJECTED))
                .isFalse();
    }

    @Test
    void 관계없는_사용자는_일정_단건과_이력을_조회할_수_없다() {
        assertThat(policy.canViewSchedule(ScheduleRelation.NONE))
                .isFalse();

        assertThat(policy.canViewStatusHistory(ScheduleRelation.NONE))
                .isFalse();
    }
}