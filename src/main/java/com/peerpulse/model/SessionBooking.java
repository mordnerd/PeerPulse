package com.peerpulse.model;

import java.time.LocalDateTime;

/** A mentee's booking of a mentor session. */
public record SessionBooking(int id, int sessionId, int menteeId,
                             String mentorName, LocalDateTime startTime,
                             LocalDateTime endTime, String status) {}
