package com.peerpulse.model;

// Meant to help set up the profile
public record Profile(int userId, String academicYear, String major,
                      String skills, String certifications, String transcript) {}