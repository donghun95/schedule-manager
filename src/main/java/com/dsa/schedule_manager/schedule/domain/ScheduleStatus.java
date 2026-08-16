package com.dsa.schedule_manager.schedule.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum ScheduleStatus {
    PLANNED,
    IN_PROGRESS,
    DONE,
    CANCELED;
    private static final Map<ScheduleStatus, Set<ScheduleStatus>>
            ALLOWED_TRANSITIONS = Map.of(

            PLANNED, EnumSet.of(IN_PROGRESS, CANCELED),
            IN_PROGRESS, EnumSet.of(DONE, CANCELED),
            DONE, EnumSet.noneOf(ScheduleStatus.class),
            CANCELED, EnumSet.noneOf(ScheduleStatus.class)
    );
    public boolean canTransitionTo(ScheduleStatus target) {
        return target != null && ALLOWED_TRANSITIONS.get(this).contains(target);
    }
    public Set<ScheduleStatus> allowedTransitions() {
        return Set.copyOf(ALLOWED_TRANSITIONS.get(this));
    }

    public boolean isTerminal() {
        return ALLOWED_TRANSITIONS.get(this).isEmpty();
    }
}
