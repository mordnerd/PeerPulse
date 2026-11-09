package com.peerpulse.service;

import com.peerpulse.db.ReportDAO;
import com.peerpulse.db.UserDAO;
import com.peerpulse.model.*;
import java.util.List;
// Admin perm's to approve or deny applications and to see pending mentor applications
public class AdminService {
    private final UserDAO users = new UserDAO();
    private final ReportDAO reports = new ReportDAO();

    private void requireAdmin(User caller) {
        if (caller == null || caller.role() != Role.ADMIN)
            throw new SecurityException("Only administrators can do this.");
    }

    public AdminCounts counts(User caller) {
        requireAdmin(caller);
        return new AdminCounts(users.countPendingMentors(),
                users.countActiveUsers(),
                reports.countOpen());
    }

    public List<MentorApplication> pendingMentors(User caller) {
        requireAdmin(caller);
        return users.findPendingMentors();
    }

    public void approve(User caller, int mentorId) {
        decide(caller, mentorId, Status.ACTIVE);
    }

    public void reject(User caller, int mentorId) {
        decide(caller, mentorId, Status.REJECTED);
    }

    private void decide(User caller, int mentorId, Status newStatus) {
        requireAdmin(caller);
        User target = users.findById(mentorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        if (target.role() != Role.MENTOR || target.status() != Status.PENDING)
            throw new IllegalArgumentException("This application is no longer pending.");
        users.updateStatus(mentorId, newStatus);
    }
}