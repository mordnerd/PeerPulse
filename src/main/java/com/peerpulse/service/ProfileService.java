package com.peerpulse.service;

import com.peerpulse.db.ProfileDAO;
import com.peerpulse.model.*;

public class ProfileService {
    private final ProfileDAO profiles = new ProfileDAO();

    // Gets the users own profile so they can only edit their own
    public Profile get(User caller) {
        requireMemberRole(caller);
        return profiles.findByUserId(caller.id())
                .orElse(new Profile(caller.id(), "", "", "", "", ""));
    }

    public void save(User caller, String academicYear, String major,
                     String skills, String certifications) {
        requireMemberRole(caller);
        if (major == null || major.isBlank())
            throw new IllegalArgumentException("Major is required.");

        Profile current = get(caller);
        profiles.save(new Profile(caller.id(),
                clean(academicYear), major.trim(), clean(skills), clean(certifications),
                current.transcript()));
    }

    private void requireMemberRole(User caller) {
        if (caller == null || (caller.role() != Role.MENTOR && caller.role() != Role.MENTEE))
            throw new SecurityException("Only mentors and mentees have profiles.");
    }

    private String clean(String s) {
        return s == null ? "" : s.trim();
    }
}