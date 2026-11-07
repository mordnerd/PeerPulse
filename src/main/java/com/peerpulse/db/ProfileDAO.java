package com.peerpulse.db;

import com.peerpulse.model.Profile;
import java.sql.*;
import java.util.Optional;

public class ProfileDAO {

    // Finding users by their ID
    public Optional<Profile> findByUserId(int userId) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM profiles WHERE user_id = ?")) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) return Optional.empty();
                return Optional.of(new Profile(

                        rs.getInt("user_id"),
                        nz(rs.getString("academic_year")),
                        nz(rs.getString("major")),
                        nz(rs.getString("skills")),
                        nz(rs.getString("certifications")),
                        nz(rs.getString("transcript"))));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Update the editable fields, the transcript is set at the registration and not here
    public void save(Profile p) {
        try (Connection c = Database.get();

             PreparedStatement ps = c.prepareStatement(
                     "UPDATE profiles SET academic_year = ?, major = ?, skills = ?, certifications =? WHERE user_id = ?")) {

            ps.setString(1, p.academicYear());
            ps.setString(2, p.major());
            ps.setString(3, p.skills());
            ps.setString(4, p.certifications());
            ps.setInt(5, p.userId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}