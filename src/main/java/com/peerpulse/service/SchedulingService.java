package com.peerpulse.service;

import com.peerpulse.db.SchedulingDAO;
import com.peerpulse.db.SchedulingRepository;
import com.peerpulse.model.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Enforces permissions and validation for mentor and mentee scheduling. */
public final class SchedulingService {
    private final SchedulingRepository repository;
    private final Clock clock;

    public SchedulingService() {
        this(new SchedulingDAO(), Clock.systemDefaultZone());
    }

    public SchedulingService(SchedulingRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    public int createSession(User caller, LocalDateTime start, LocalDateTime end) {
        requireRole(caller, Role.MENTOR);
        SchedulingRules.requireValidFutureSlot(start, end, clock);
        return repository.create(caller.id(), start, end);
    }

    public void editSession(User caller, int sessionId, LocalDateTime start, LocalDateTime end) {
        requireRole(caller, Role.MENTOR);
        SchedulingRules.requireValidFutureSlot(start, end, clock);
        repository.update(caller.id(), sessionId, start, end, now());
    }

    public void removeSession(User caller, int sessionId) {
        requireRole(caller, Role.MENTOR);
        repository.remove(caller.id(), sessionId);
    }

    public void completeSession(User caller, int sessionId) {
        requireRole(caller, Role.MENTOR);
        repository.markCompleted(caller.id(), sessionId, now());
    }

    public List<SessionSlot> mentorSessions(User caller) {
        requireRole(caller, Role.MENTOR);
        return repository.forMentor(caller.id());
    }

    public List<SessionSlot> availableSessions(User caller) {
        requireRole(caller, Role.MENTEE);
        return repository.available(now());
    }

    public int book(User caller, int sessionId) {
        requireRole(caller, Role.MENTEE);
        return repository.book(caller.id(), sessionId, now());
    }

    public void cancel(User caller, int bookingId) {
        requireRole(caller, Role.MENTEE);
        repository.cancel(caller.id(), bookingId, now());
    }

    public List<SessionBooking> myBookings(User caller) {
        requireRole(caller, Role.MENTEE);
        return repository.forMentee(caller.id());
    }

    private LocalDateTime now() { return LocalDateTime.now(clock); }

    private static void requireRole(User caller, Role required) {
        if (caller == null || caller.role() != required || caller.status() != Status.ACTIVE) {
            throw new SecurityException("Only active " + required.name().toLowerCase() + " accounts may perform this action.");
        }
    }
}
