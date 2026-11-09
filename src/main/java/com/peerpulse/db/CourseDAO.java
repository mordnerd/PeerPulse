package com.peerpulse.db;

import com.peerpulse.model.Course;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CourseDAO {

    public List<Course> findAll() {
        List<Course> result = new ArrayList<>();
        try (Connection c = Database.get();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT id, code, title, description FROM courses ORDER BY code")) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Course> findById(int id) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, code, title, description FROM courses WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Course> findByCode(String code) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, code, title, description FROM courses WHERE lower(code) = lower(?)")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int count() {
        try (Connection c = Database.get();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM courses")) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Course insert(String code, String title, String description) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO courses(code, title, description) VALUES(?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return new Course(rs.getInt(1), code, title, description);
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique")) {
                throw new IllegalArgumentException("A course with that code already exists.");
            }
            throw new RuntimeException(e);
        }
    }

    public boolean deleteById(int id) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM courses WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Course> findByMentor(int mentorId) {
        List<Course> result = new ArrayList<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("""
                     SELECT c.id, c.code, c.title, c.description
                     FROM courses c
                     JOIN mentor_courses mc ON mc.course_id = c.id
                     WHERE mc.mentor_id = ?
                     ORDER BY c.code
                     """)) {
            ps.setInt(1, mentorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isMentorFor(int mentorId, int courseId) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM mentor_courses WHERE mentor_id = ? AND course_id = ?")) {
            ps.setInt(1, mentorId);
            ps.setInt(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addMentorCourse(int mentorId, int courseId) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO mentor_courses(mentor_id, course_id) VALUES(?,?)")) {
            ps.setInt(1, mentorId);
            ps.setInt(2, courseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique")) {
                throw new IllegalArgumentException("You are already signed up to mentor this course.");
            }
            throw new RuntimeException(e);
        }
    }

    public boolean removeMentorCourse(int mentorId, int courseId) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM mentor_courses WHERE mentor_id = ? AND course_id = ?")) {
            ps.setInt(1, mentorId);
            ps.setInt(2, courseId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Course map(ResultSet rs) throws SQLException {
        return new Course(
                rs.getInt("id"),
                rs.getString("code"),
                rs.getString("title"),
                nz(rs.getString("description")));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
