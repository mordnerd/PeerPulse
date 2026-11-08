package com.peerpulse.db;

import com.peerpulse.model.FeedbackBooking;
import com.peerpulse.model.SessionFeedback;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FeedbackDAO {
    public List<FeedbackBooking> findAwaitingFeedback(int mentorId) {
        String sql = """
                SELECT booking.id, session.start_time,
                       COALESCE(course.title, 'Mentorship session') AS course_title,
                       mentee.full_name AS mentee_name
                FROM bookings booking
                JOIN sessions session ON session.id = booking.session_id
                JOIN users mentee ON mentee.id = booking.mentee_id
                LEFT JOIN courses course ON course.id = session.course_id
                LEFT JOIN feedback ON feedback.booking_id = booking.id
                WHERE session.mentor_id = ?
                  AND session.status = 'COMPLETED'
                  AND booking.status = 'BOOKED'
                  AND feedback.id IS NULL
                ORDER BY session.start_time DESC, booking.id
                """;
        try (Connection connection = Database.get();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, mentorId);
            ResultSet rows = statement.executeQuery();
            List<FeedbackBooking> bookings = new ArrayList<>();
            while (rows.next()) {
                bookings.add(new FeedbackBooking(rows.getInt("id"), rows.getString("start_time"),
                        rows.getString("course_title"), rows.getString("mentee_name")));
            }
            return bookings;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load completed bookings.", e);
        }
    }

    public boolean save(int mentorId, int bookingId, String comment) {
        // Check the booking and save the comment in one database operation.
        String sql = """
                INSERT INTO feedback (booking_id, comment, created_at)
                SELECT booking.id, ?, strftime('%Y-%m-%dT%H:%M:%fZ', 'now')
                FROM bookings booking
                JOIN sessions session ON session.id = booking.session_id
                WHERE booking.id = ?
                  AND session.mentor_id = ?
                  AND session.status = 'COMPLETED'
                  AND booking.status = 'BOOKED'
                ON CONFLICT(booking_id) DO NOTHING
                """;
        try (Connection connection = Database.get();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, comment);
            statement.setInt(2, bookingId);
            statement.setInt(3, mentorId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save feedback.", e);
        }
    }

    public List<SessionFeedback> findForMentee(int menteeId) {
        String sql = """
                SELECT session.start_time, COALESCE(course.title, 'Mentorship session') AS course_title,
                       mentor.full_name AS mentor_name, feedback.comment
                FROM feedback
                JOIN bookings booking ON booking.id = feedback.booking_id
                JOIN sessions session ON session.id = booking.session_id
                JOIN users mentor ON mentor.id = session.mentor_id
                LEFT JOIN courses course ON course.id = session.course_id
                WHERE booking.mentee_id = ?
                ORDER BY feedback.created_at DESC, feedback.id DESC
                """;
        try (Connection connection = Database.get();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, menteeId);
            ResultSet rows = statement.executeQuery();
            List<SessionFeedback> feedback = new ArrayList<>();
            while (rows.next()) {
                feedback.add(new SessionFeedback(rows.getString("start_time"),
                        rows.getString("course_title"), rows.getString("mentor_name"),
                        rows.getString("comment")));
            }
            return feedback;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load feedback.", e);
        }
    }
}
