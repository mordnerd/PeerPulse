package com.peerpulse.service;

import com.peerpulse.db.UserDAO;
import com.peerpulse.model.*;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Optional;

public class AuthService {
    private final UserDAO users = new UserDAO();
    public void register(String email, String password, String name,
                         Role role, String transcript) {
        register(email, password, name, role, transcript, "", "", "", "");
    }

    // Full registration form where academic details are optional at sign-up (May change this later to uploading PDF's)
    public void register(String email, String password, String name, Role role, String transcript,
                         String academicYear, String major, String skills, String certifications) {
        email = email == null ? "" : email.trim().toLowerCase();
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name is required.");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Enter a valid email address.");
        if (password == null || password.length() < 8)
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        if (role == null || role == Role.ADMIN)
            throw new IllegalArgumentException("Choose mentor or mentee.");

        Profile profile = new Profile(0, clean(academicYear), clean(major),
                clean(skills), clean(certifications), clean(transcript));

        Optional<User> existing = users.findByEmail(email);
        if (existing.isPresent()) {
            User u = existing.get();
            // A rejected mentor may apply again with the same email
            if (role == Role.MENTOR && u.role() == Role.MENTOR && u.status() == Status.REJECTED) {
                users.reapplyMentor(u.id(), BCrypt.hashpw(password, BCrypt.gensalt()),
                        name.trim(), profile);
                return;
            }
            if (u.role() == Role.MENTOR && u.status() == Status.PENDING)
                throw new IllegalArgumentException("Your mentor application is already awaiting approval.");
            throw new IllegalArgumentException("An account with that email already exists.");
        }

        // Mentors need admin approval, mentees are active immediately
        Status status = (role == Role.MENTOR) ? Status.PENDING : Status.ACTIVE;
        users.insert(email, BCrypt.hashpw(password, BCrypt.gensalt()),
                name.trim(), role, status, profile);
    }

    public User login(String email, String password) {
        email = email == null ? "" : email.trim().toLowerCase();
        User u = users.findByEmail(email).orElse(null);
        if (u == null || password == null || !BCrypt.checkpw(password, u.passwordHash()))
            throw new IllegalArgumentException("Incorrect email or password.");
        if (u.status() == Status.PENDING)
            throw new IllegalArgumentException("Your mentor application is awaiting administrator approval.");
        if (u.status() == Status.REJECTED)
            throw new IllegalArgumentException(
                    "Your mentor application was not approved. You can apply again by registering with the same email.");
        return u;
    }

    // Pre-made admin account (May add another way to add admin accounts but for now this it enough)
    public void seedAdmin() {
        if (!users.adminExists()) {
            users.insert("admin@peerpulse.local", BCrypt.hashpw("Admin123!", BCrypt.gensalt()),
                    "Administrator", Role.ADMIN, Status.ACTIVE,
                    new Profile(0, "", "", "", "", ""));
        }
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }
}