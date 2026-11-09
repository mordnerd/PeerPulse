package com.peerpulse.model;

// Meant for the mentors applications
public record MentorApplication(int id, String fullName, String email,
                                String academicYear, String major, String skills,
                                String certifications, String transcript) {}