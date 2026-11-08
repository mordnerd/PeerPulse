package com.peerpulse.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** A mentor-owned session; status is AVAILABLE, BOOKED, COMPLETED, or CANCELLED. */
public record SessionSlot(int id, int mentorId, String mentorName,
                          LocalDateTime startTime, LocalDateTime endTime,
                          String status, String courseTitle) {
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    public String timeLabel() {
        return startTime.format(LABEL) + " - " + endTime.toLocalTime();
    }
}
