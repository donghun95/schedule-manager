package com.dsa.schedule_manager.schedule.authorization;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ScheduleAccessPolicyTest {

    private final ScheduleAccessPolicy policy =
            new ScheduleAccessPolicy();

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

    @Test
    void owner는_모든_상태의_참여자를_조회할_수_있다() {
        ParticipantVisibility result =
                policy.participantVisibility(ScheduleRelation.OWNER);

        assertThat(result)
                .isEqualTo(ParticipantVisibility.ALL);
    }

    @Test
    void accepted는_accepted_상태의_참여자만_조회할_수_있다() {
        ParticipantVisibility result =
                policy.participantVisibility(ScheduleRelation.ACCEPTED);

        assertThat(result)
                .isEqualTo(ParticipantVisibility.ACCEPTED_ONLY);
    }

    @Test
    void pending은_참여자_목록을_조회할_수_없다() {
        ParticipantVisibility result =
                policy.participantVisibility(ScheduleRelation.PENDING);

        assertThat(result)
                .isEqualTo(ParticipantVisibility.NONE);
    }

    @Test
    void rejected는_참여자_목록을_조회할_수_없다() {
        ParticipantVisibility result =
                policy.participantVisibility(ScheduleRelation.REJECTED);

        assertThat(result)
                .isEqualTo(ParticipantVisibility.NONE);
    }

    @Test
    void 관계없는_사용자는_참여자_목록을_조회할_수_없다() {
        ParticipantVisibility result =
                policy.participantVisibility(ScheduleRelation.NONE);

        assertThat(result)
                .isEqualTo(ParticipantVisibility.NONE);
    }
}