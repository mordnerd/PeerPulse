package com.peerpulse.db;

import com.peerpulse.model.SessionBooking;
import com.peerpulse.model.SessionSlot;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** SQLite scheduling repository. Writes are transactional and bookings are claim-first. */
public final class SchedulingDAO implements SchedulingRepository {
    private static final DateTimeFormatter DB_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    private static String timestamp(LocalDateTime time) { return time.format(DB_TIME); }

    private static Connection connect() throws SQLException {
        Connection c = Database.get();
        try (Statement statement = c.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return c;
    }

    @Override
    public int create(int mentorId, LocalDateTime start, LocalDateTime end) {
        String insert = "INSERT INTO sessions(mentor_id, course_id, start_time, end_time, status) "
                + "VALUES (?, NULL, ?, ?, 'AVAILABLE')";
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                int id;
                // Insert before checking conflicts: SQLite serializes writers and prevents races.
                try (PreparedStatement s = c.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                    s.setInt(1, mentorId);
                    s.setString(2, timestamp(start));
                    s.setString(3, timestamp(end));
                    s.executeUpdate();
                    try (ResultSet keys = s.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No ID returned for session.");
                        id = keys.getInt(1);
                    }
                }
                if (mentorHasConflict(c, mentorId, start, end, id)) {
                    throw new IllegalArgumentException("You already have a session during this time.");
                }
                c.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                rollback(c, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to create the session.", e);
        }
    }

    @Override
    public void update(int mentorId, int sessionId, LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        String update = "UPDATE sessions SET start_time=?, end_time=? "
                + "WHERE id=? AND mentor_id=? AND status='AVAILABLE' AND start_time > ?";
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement s = c.prepareStatement(update)) {
                    s.setString(1, timestamp(start));
                    s.setString(2, timestamp(end));
                    s.setInt(3, sessionId);
                    s.setInt(4, mentorId);
                    s.setString(5, timestamp(now));
                    if (s.executeUpdate() != 1) {
                        throw new IllegalArgumentException("Only your future available sessions may be edited.");
                    }
                }
                if (mentorHasConflict(c, mentorId, start, end, sessionId)) {
                    throw new IllegalArgumentException("You already have a session during this time.");
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                rollback(c, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to update the session.", e);
        }
    }

    @Override
    public void remove(int mentorId, int sessionId) {
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement(
                "UPDATE sessions SET status='CANCELLED' WHERE id=? AND mentor_id=? AND status='AVAILABLE'")) {
            s.setInt(1, sessionId);
            s.setInt(2, mentorId);
            if (s.executeUpdate() != 1) {
                throw new IllegalArgumentException("Only your available sessions may be removed.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to remove the session.", e);
        }
    }

    @Override
    public void markCompleted(int mentorId, int sessionId, LocalDateTime now) {
        // Keeps the booking's status BOOKED, as expected by the existing FeedbackDAO.
        String sql = "UPDATE sessions SET status='COMPLETED' "
                + "WHERE id=? AND mentor_id=? AND status='BOOKED' AND end_time <= ?";
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, sessionId);
            s.setInt(2, mentorId);
            s.setString(3, timestamp(now));
            if (s.executeUpdate() != 1) {
                throw new IllegalArgumentException("A booked session may be completed only after its end time.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to complete the session.", e);
        }
    }

    @Override
    public List<SessionSlot> forMentor(int mentorId) {
        String sql = """
                SELECT s.id, s.mentor_id, u.full_name AS mentor_name,
                       s.start_time, s.end_time, s.status,
                       COALESCE(c.title, 'Mentorship session') AS course_title
                FROM sessions s
                JOIN users u ON u.id = s.mentor_id
                LEFT JOIN courses c ON c.id = s.course_id
                WHERE s.mentor_id = ?
                ORDER BY s.start_time DESC, s.id DESC
                """;
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, mentorId);
            try (ResultSet rs = s.executeQuery()) { return readSessions(rs); }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load mentor sessions.", e);
        }
    }

    @Override
    public List<SessionSlot> available(LocalDateTime now) {
        String sql = """
                SELECT s.id, s.mentor_id, u.full_name AS mentor_name,
                       s.start_time, s.end_time, s.status,
                       COALESCE(c.title, 'Mentorship session') AS course_title
                FROM sessions s
                JOIN users u ON u.id = s.mentor_id
                LEFT JOIN courses c ON c.id = s.course_id
                WHERE s.status='AVAILABLE' AND s.start_time > ?
                  AND u.role='MENTOR' AND u.status='ACTIVE'
                ORDER BY s.start_time ASC, s.id ASC
                """;
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, timestamp(now));
            try (ResultSet rs = s.executeQuery()) { return readSessions(rs); }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load available sessions.", e);
        }
    }

    @Override
    public int book(int menteeId, int sessionId, LocalDateTime now) {
        String claim = """
                UPDATE sessions SET status='BOOKED'
                WHERE id=? AND status='AVAILABLE' AND start_time > ?
                  AND EXISTS (SELECT 1 FROM users u WHERE u.id=sessions.mentor_id
                              AND u.role='MENTOR' AND u.status='ACTIVE')
                """;
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                // Claim first: only one concurrent request can transition AVAILABLE -> BOOKED.
                try (PreparedStatement s = c.prepareStatement(claim)) {
                    s.setInt(1, sessionId);
                    s.setString(2, timestamp(now));
                    if (s.executeUpdate() != 1) {
                        throw new IllegalArgumentException("This session is unavailable or has already been booked.");
                    }
                }
                LocalDateTime start, end;
                try (PreparedStatement s = c.prepareStatement(
                        "SELECT start_time, end_time FROM sessions WHERE id=?")) {
                    s.setInt(1, sessionId);
                    try (ResultSet rs = s.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Session disappeared while booking.");
                        start = LocalDateTime.parse(rs.getString(1));
                        end = LocalDateTime.parse(rs.getString(2));
                    }
                }
                // For a given mentee, forbid any overlap with their other active bookings.
                if (menteeHasConflict(c, menteeId, start, end)) {
                    throw new IllegalArgumentException("You already have a booking that overlaps this session.");
                }
                int bookingId;
                try (PreparedStatement s = c.prepareStatement(
                        "INSERT INTO bookings(session_id, mentee_id, status) VALUES (?, ?, 'BOOKED')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    s.setInt(1, sessionId);
                    s.setInt(2, menteeId);
                    s.executeUpdate();
                    try (ResultSet keys = s.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No ID returned for booking.");
                        bookingId = keys.getInt(1);
                    }
                }
                c.commit();
                return bookingId;
            } catch (SQLException | RuntimeException e) {
                rollback(c, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to book the session.", e);
        }
    }

    @Override
    public void cancel(int menteeId, int bookingId, LocalDateTime now) {
        String cancelBooking = """
                UPDATE bookings SET status='CANCELLED'
                WHERE id=? AND mentee_id=? AND status='BOOKED'
                  AND EXISTS (SELECT 1 FROM sessions s
                              WHERE s.id=bookings.session_id AND s.status='BOOKED'
                                AND s.start_time > ?)
                """;
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement s = c.prepareStatement(cancelBooking)) {
                    s.setInt(1, bookingId);
                    s.setInt(2, menteeId);
                    s.setString(3, timestamp(now));
                    if (s.executeUpdate() != 1) {
                        throw new IllegalArgumentException("Only your upcoming bookings may be cancelled.");
                    }
                }
                try (PreparedStatement s = c.prepareStatement(
                        "UPDATE sessions SET status='AVAILABLE' "
                        + "WHERE status='BOOKED' AND id=(SELECT session_id FROM bookings WHERE id=?)")) {
                    s.setInt(1, bookingId);
                    if (s.executeUpdate() != 1) throw new SQLException("Could not release the session.");
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                rollback(c, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to cancel the booking.", e);
        }
    }

    @Override
    public List<SessionBooking> forMentee(int menteeId) {
        String sql = """
                SELECT b.id, b.session_id, b.mentee_id, u.full_name AS mentor_name,
                       s.start_time, s.end_time, b.status
                FROM bookings b
                JOIN sessions s ON s.id = b.session_id
                JOIN users u ON u.id = s.mentor_id
                WHERE b.mentee_id=? AND b.status='BOOKED'
                ORDER BY s.start_time DESC, b.id DESC
                """;
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, menteeId);
            try (ResultSet rs = s.executeQuery()) {
                List<SessionBooking> bookings = new ArrayList<>();
                while (rs.next()) {
                    bookings.add(new SessionBooking(rs.getInt("id"), rs.getInt("session_id"),
                            rs.getInt("mentee_id"), rs.getString("mentor_name"),
                            LocalDateTime.parse(rs.getString("start_time")),
                            LocalDateTime.parse(rs.getString("end_time")), rs.getString("status")));
                }
                return bookings;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load your bookings.", e);
        }
    }

    private static List<SessionSlot> readSessions(ResultSet rs) throws SQLException {
        List<SessionSlot> result = new ArrayList<>();
        while (rs.next()) {
            result.add(new SessionSlot(rs.getInt("id"), rs.getInt("mentor_id"),
                    rs.getString("mentor_name"), LocalDateTime.parse(rs.getString("start_time")),
                    LocalDateTime.parse(rs.getString("end_time")), rs.getString("status"),
                    rs.getString("course_title")));
        }
        return result;
    }

    private static boolean mentorHasConflict(Connection c, int mentorId, LocalDateTime start,
                                             LocalDateTime end, int excludedId) throws SQLException {
        String sql = """
                SELECT 1 FROM sessions
                WHERE mentor_id=? AND id<>? AND status IN ('AVAILABLE','BOOKED')
                  AND start_time < ? AND end_time > ? LIMIT 1
                """;
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, mentorId);
            s.setInt(2, excludedId);
            s.setString(3, timestamp(end));
            s.setString(4, timestamp(start));
            try (ResultSet rs = s.executeQuery()) { return rs.next(); }
        }
    }

    private static boolean menteeHasConflict(Connection c, int menteeId, LocalDateTime start,
                                             LocalDateTime end) throws SQLException {
        String sql = """
                SELECT 1 FROM bookings b JOIN sessions s ON s.id=b.session_id
                WHERE b.mentee_id=? AND b.status='BOOKED'
                  AND s.status IN ('BOOKED','COMPLETED')
                  AND s.start_time < ? AND s.end_time > ? LIMIT 1
                """;
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, menteeId);
            s.setString(2, timestamp(end));
            s.setString(3, timestamp(start));
            try (ResultSet rs = s.executeQuery()) { return rs.next(); }
        }
    }

    private static void rollback(Connection connection, Exception original) {
        try { connection.rollback(); }
        catch (SQLException rollbackError) { original.addSuppressed(rollbackError); }
    }
}
