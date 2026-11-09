package com.peerpulse.model;

public record PeerMatch(int userId, String fullName, Role role,
                        String academicYear, String major, String skills, String courses) {}
