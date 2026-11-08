package com.peerpulse.service;

import com.peerpulse.db.FeedbackDAO;
import com.peerpulse.model.FeedbackBooking;
import com.peerpulse.model.Role;
import com.peerpulse.model.SessionFeedback;
import com.peerpulse.model.Status;
import com.peerpulse.model.User;
import java.util.List;

public class FeedbackService {
    private final FeedbackDAO feedback = new FeedbackDAO();

    public List<FeedbackBooking> bookingsForMentor(User caller) {
        requireActiveRole(caller, Role.MENTOR);
        return feedback.findAwaitingFeedback(caller.id());
    }

    public void submit(User caller, int bookingId, String comment) {
        requireActiveRole(caller, Role.MENTOR);
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Enter feedback before saving.");
        }
        boolean saved = feedback.save(caller.id(), bookingId, comment.strip());
        if (!saved) {
            throw new IllegalArgumentException("This booking is no longer available for feedback.");
        }
    }

    public List<SessionFeedback> forMentee(User caller) {
        requireActiveRole(caller, Role.MENTEE);
        return feedback.findForMentee(caller.id());
    }

    private void requireActiveRole(User caller, Role role) {
        if (caller == null || caller.role() != role || caller.status() != Status.ACTIVE) {
            throw new SecurityException("You do not have permission to access this feedback.");
        }
    }
}
