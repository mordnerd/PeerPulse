package com.peerpulse.db;

import com.peerpulse.model.SessionBooking;
import com.peerpulse.model.SessionSlot;
import java.time.LocalDateTime;
import java.util.List;

/** Interface keeps business logic testable without a live database. */
public interface SchedulingRepository {
    int create(int mentorId, LocalDateTime start, LocalDateTime end);
    void update(int mentorId, int sessionId, LocalDateTime start, LocalDateTime end, LocalDateTime now);
    void remove(int mentorId, int sessionId);
    void markCompleted(int mentorId, int sessionId, LocalDateTime now);
    List<SessionSlot> forMentor(int mentorId);
    List<SessionSlot> available(LocalDateTime now);
    int book(int menteeId, int sessionId, LocalDateTime now);
    void cancel(int menteeId, int bookingId, LocalDateTime now);
    List<SessionBooking> forMentee(int menteeId);
}
