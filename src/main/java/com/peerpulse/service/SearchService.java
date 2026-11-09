package com.peerpulse.service;

import com.peerpulse.db.CourseDAO;
import com.peerpulse.db.SearchDAO;
import com.peerpulse.model.Course;
import com.peerpulse.model.PeerMatch;
import com.peerpulse.model.Role;
import com.peerpulse.model.Status;
import com.peerpulse.model.User;
import java.util.List;

public class SearchService {
    public static final String EMPTY_RESULTS = "No results - change search";

    private final SearchDAO search = new SearchDAO();
    private final CourseDAO courses = new CourseDAO();

    public List<PeerMatch> search(User caller, String query, Integer courseId, String skill) {
        requireActiveMember(caller);
        String cleanedQuery = query == null ? "" : query.trim();
        String cleanedSkill = skill == null ? "" : skill.trim();
        if (courseId != null) {
            courses.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course filter is not valid."));
        }
        if (caller.role() == Role.MENTEE) {
            return search.searchMentors(cleanedQuery, courseId, cleanedSkill);
        }
        return search.searchMentees(cleanedQuery, courseId, cleanedSkill);
    }

    public List<Course> courseFilters(User caller) {
        requireActiveMember(caller);
        return courses.findAll();
    }

    public List<String> skillFilters(User caller) {
        requireActiveMember(caller);
        Role target = caller.role() == Role.MENTEE ? Role.MENTOR : Role.MENTEE;
        return search.distinctSkills(target);
    }

    public static String resultSummary(int count) {
        if (count <= 0) {
            return EMPTY_RESULTS;
        }
        if (count == 1) {
            return "1 match";
        }
        return count + " matches";
    }

    private void requireActiveMember(User caller) {
        if (caller == null || caller.status() != Status.ACTIVE
                || (caller.role() != Role.MENTOR && caller.role() != Role.MENTEE)) {
            throw new SecurityException("Only active mentors and mentees can search for peers.");
        }
    }
}
