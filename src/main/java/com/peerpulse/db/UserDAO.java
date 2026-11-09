package com.peerpulse.db;

import com.peerpulse.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {
    // Saves a new user and the profile
    // It also returns the new user's id if the insert failes
    public int insert(String email, String hash, String name,
                      Role role, Status status, Profile profile) {

        //Auto commit is turned off here so that it only commits both inserts or none
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            int id;

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users(email,password_hash,full_name,role,status) VALUES(?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, email);
                ps.setString(2, hash);
                ps.setString(3, name);
                ps.setString(4, role.name());
                ps.setString(5, status.name());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    id = rs.getInt(1);

                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO profiles(user_id, academic_year, major, skills, certifications, transcript) "
                            + "VALUES(?,?,?,?,?,?)")) {

                ps.setInt(1, id);
                ps.setString(2, profile.academicYear());
                ps.setString(3, profile.major());
                ps.setString(4, profile.skills());
                ps.setString(5, profile.certifications());
                ps.setString(6, profile.transcript());
                ps.executeUpdate();

            }
            c.commit();
            return id;
        } catch (SQLException e) {

            throw new RuntimeException(e);
        }
    }

    public Optional<User> findByEmail(String email) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<User> findById(int id) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Mentors have to wait until their applications are accepted but can still fill out the information for admins to view
    public List<MentorApplication> findPendingMentors() {
        String sql = """
            SELECT u.id, u.full_name, u.email,
                   p.academic_year, p.major, p.skills, p.certifications, p.transcript
            FROM users u LEFT JOIN profiles p ON p.user_id = u.id
            WHERE u.role = 'MENTOR' AND u.status = 'PENDING'
            ORDER BY u.id
            """;
        List<MentorApplication> result = new ArrayList<>();
        try (Connection c = Database.get();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new MentorApplication(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        nz(rs.getString("academic_year")),
                        nz(rs.getString("major")),
                        nz(rs.getString("skills")),
                        nz(rs.getString("certifications")),
                        nz(rs.getString("transcript"))));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int countPendingMentors() {
        return count("SELECT COUNT(*) FROM users WHERE role = 'MENTOR' AND status = 'PENDING'");
    }

    // This count only takes the active mentor and mentee's, not admins
    public int countActiveUsers() {
        return count("SELECT COUNT(*) FROM users WHERE role <> 'ADMIN' AND status = 'ACTIVE'");
    }

    public void updateStatus(int id, Status status) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // This resets the mentors accoutn so if they want to apply again they can (Basically so the email is not locked)
    public void reapplyMentor(int id, String hash, String name, Profile profile) {
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE users SET password_hash = ?, full_name = ?, status = 'PENDING' WHERE id = ?")) {
                ps.setString(1, hash);
                ps.setString(2, name);
                ps.setInt(3, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE profiles SET academic_year = ?, major = ?, skills = ?, "
                            + "certifications = ?, transcript = ? WHERE user_id = ?")) {
                ps.setString(1, profile.academicYear());
                ps.setString(2, profile.major());
                ps.setString(3, profile.skills());
                ps.setString(4, profile.certifications());
                ps.setString(5, profile.transcript());
                ps.setInt(6, id);
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean adminExists() {
        return count("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'") > 0;
    }

    private int count(String sql) {
        try (Connection c = Database.get();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("email"),
                rs.getString("password_hash"), rs.getString("full_name"),
                Role.valueOf(rs.getString("role")),
                Status.valueOf(rs.getString("status")));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}