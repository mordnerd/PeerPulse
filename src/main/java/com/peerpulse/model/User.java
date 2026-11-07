package com.peerpulse.model;
public record User(int id, String email, String passwordHash,
                   String fullName, Role role, Status status) {}