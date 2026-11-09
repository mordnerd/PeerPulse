package com.peerpulse.db;

import com.peerpulse.model.PeerMatch;
import com.peerpulse.model.Role;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SearchDAO {

    public List<PeerMatch> searchMentors(String query, Integer courseId, String skill) {
        String sql = """
                SELECT u.id, u.full_name, u.role,
                       ifnull(p.academic_year, '') AS academic_year,
                       ifnull(p.major, '') AS major,
                       ifnull(p.skills, '') AS skills,
                       ifnull(GROUP_CONCAT(c.code, ', '), '') AS courses
                FROM users u
                LEFT JOIN profiles p ON p.user_id = u.id
                LEFT JOIN mentor_courses mc ON mc.mentor_id = u.id
                LEFT JOIN courses c ON c.id = mc.course_id
                WHERE u.role = 'MENTOR' AND u.status = 'ACTIVE'
                  AND (
                        ? = ''
                        OR lower(u.full_name) LIKE ?
                        OR lower(ifnull(p.skills, '')) LIKE ?
                        OR EXISTS (
                            SELECT 1 FROM mentor_courses mc2
                            JOIN courses c2 ON c2.id = mc2.course_id
                            WHERE mc2.mentor_id = u.id
                              AND (lower(c2.code) LIKE ? OR lower(c2.title) LIKE ?)
                        )
                  )
                  AND (? = 0 OR EXISTS (
                        SELECT 1 FROM mentor_courses mc3
                        WHERE mc3.mentor_id = u.id AND mc3.course_id = ?
                  ))
                  AND (? = '' OR lower(ifnull(p.skills, '')) LIKE ?)
                GROUP BY u.id
                ORDER BY u.full_name
                """;
        return query(sql, query, courseId, skill, Role.MENTOR);
    }

    public List<PeerMatch> searchMentees(String query, Integer courseId, String skill) {
        String sql = """
                SELECT u.id, u.full_name, u.role,
                       ifnull(p.academic_year, '') AS academic_year,
                       ifnull(p.major, '') AS major,
                       ifnull(p.skills, '') AS skills,
                       '' AS courses
                FROM users u
                LEFT JOIN profiles p ON p.user_id = u.id
                WHERE u.role = 'MENTEE' AND u.status = 'ACTIVE'
                  AND (
                        ? = ''
                        OR lower(u.full_name) LIKE ?
                        OR lower(ifnull(p.skills, '')) LIKE ?
                        OR lower(ifnull(p.major, '')) LIKE ?
                        OR lower(ifnull(p.certifications, '')) LIKE ?
                  )
                  AND (? = 0 OR lower(ifnull(p.skills, '') || ' ' || ifnull(p.major, '')) LIKE ?
                              OR lower(ifnull(p.skills, '') || ' ' || ifnull(p.major, '')) LIKE ?)
                  AND (? = '' OR lower(ifnull(p.skills, '')) LIKE ?)
                ORDER BY u.full_name
                """;
        return queryMentees(sql, query, courseId, skill);
    }

    public List<String> distinctSkills(Role role) {
        List<String> skills = new ArrayList<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT skills FROM profiles p JOIN users u ON u.id = p.user_id "
                             + "WHERE u.role = ? AND u.status = 'ACTIVE' AND ifnull(p.skills, '') <> ''")) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    for (String part : rs.getString(1).split(",")) {
                        String skill = part.trim();
                        if (!skill.isEmpty() && skills.stream().noneMatch(s -> s.equalsIgnoreCase(skill))) {
                            skills.add(skill);
                        }
                    }
                }
            }
            skills.sort(String.CASE_INSENSITIVE_ORDER);
            return skills;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<PeerMatch> query(String sql, String query, Integer courseId, String skill, Role role) {
        String like = like(query);
        String skillLike = like(skill);
        int course = courseId == null ? 0 : courseId;
        List<PeerMatch> result = new ArrayList<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, query);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);
            ps.setInt(6, course);
            ps.setInt(7, course);
            ps.setString(8, skill == null ? "" : skill.trim());
            ps.setString(9, skillLike);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs, role));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<PeerMatch> queryMentees(String sql, String query, Integer courseId, String skill) {
        String like = like(query);
        String skillLike = like(skill);
        String codeLike = "%__no_match__%";
        String titleLike = "%__no_match__%";
        int courseFlag = 0;
        if (courseId != null) {
            courseFlag = 1;
            var course = new CourseDAO().findById(courseId);
            if (course.isPresent()) {
                codeLike = like(course.get().code());
                titleLike = like(course.get().title());
            }
        }
        List<PeerMatch> result = new ArrayList<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, query);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);
            ps.setInt(6, courseFlag);
            ps.setString(7, codeLike);
            ps.setString(8, titleLike);
            ps.setString(9, skill == null ? "" : skill.trim());
            ps.setString(10, skillLike);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs, Role.MENTEE));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private PeerMatch map(ResultSet rs, Role role) throws SQLException {
        return new PeerMatch(
                rs.getInt("id"),
                rs.getString("full_name"),
                role,
                rs.getString("academic_year"),
                rs.getString("major"),
                rs.getString("skills"),
                nz(rs.getString("courses")));
    }

    private static String like(String value) {
        if (value == null || value.isBlank()) {
            return "%";
        }
        return "%" + value.trim().toLowerCase() + "%";
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
