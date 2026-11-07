package com.peerpulse.model;
//Meant to get the count info for admins and the dashboard
public record AdminCounts(int pendingMentors, int activeUsers, int openReports) {}