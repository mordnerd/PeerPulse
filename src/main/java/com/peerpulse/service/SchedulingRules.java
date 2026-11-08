package com.peerpulse.service;

import java.time.Clock;
import java.time.LocalDateTime;

/** Pure domain rules: independent of JavaFX and SQLite, suitable for unit testing. */
public final class SchedulingRules {
    private SchedulingRules() {}

    public static void requireValidFutureSlot(LocalDateTime start, LocalDateTime end, Clock clock) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end time are required.");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        if (!start.isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("The session must start in the future.");
        }
    }

    /** Endpoints touching (10:00-11:00 and 11:00-12:00) are not a conflict. */
    public static boolean overlaps(LocalDateTime startA, LocalDateTime endA,
                                   LocalDateTime startB, LocalDateTime endB) {
        if (startA == null || endA == null || startB == null || endB == null) {
            throw new IllegalArgumentException("Time intervals cannot contain null values.");
        }
        if (!startA.isBefore(endA) || !startB.isBefore(endB)) {
            throw new IllegalArgumentException("Time intervals must have positive duration.");
        }
        return startA.isBefore(endB) && startB.isBefore(endA);
    }
}
