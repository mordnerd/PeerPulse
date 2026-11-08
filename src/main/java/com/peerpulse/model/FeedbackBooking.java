package com.peerpulse.model;

public record FeedbackBooking(int id, String startTime, String courseTitle, String menteeName) {
    @Override
    public String toString() {
        return startTime + " · " + courseTitle + " · " + menteeName;
    }
}
