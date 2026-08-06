package com.dsa.schedule_manager.schedule.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleStatusTest {

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @MethodSource("transitionCases")
    void 상태_전이_규칙을_모두_검증한다(
            ScheduleStatus from,
            ScheduleStatus to,
            boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    private static Stream<Arguments> transitionCases() {
        return Stream.of(ScheduleStatus.values())
                .flatMap(from -> Stream.of(ScheduleStatus.values())
                        .map(to -> Arguments.of(from, to, expected(from, to))));
    }

    private static boolean expected(ScheduleStatus from, ScheduleStatus to) {
        return switch (from) {
            case PLANNED -> to == ScheduleStatus.IN_PROGRESS
                    || to == ScheduleStatus.CANCELED;
            case IN_PROGRESS -> to == ScheduleStatus.DONE
                    || to == ScheduleStatus.CANCELED;
            case DONE, CANCELED -> false;
        };
    }
}
