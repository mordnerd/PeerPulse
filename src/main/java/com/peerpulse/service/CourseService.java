package com.peerpulse.service;

import com.peerpulse.db.CourseDAO;
import com.peerpulse.model.Course;
import com.peerpulse.model.Role;
import com.peerpulse.model.Status;
import com.peerpulse.model.User;
import java.util.List;

public class CourseService {
    private final CourseDAO courses = new CourseDAO();

    public List<Course> listAll(User caller) {
        requireLoggedIn(caller);
        return courses.findAll();
    }

    public Course add(User caller, String code, String title, String description) {
        requireAdmin(caller);
        String cleanCode = required(code, "Course code is required.");
        String cleanTitle = required(title, "Course title is required.");
        if (cleanCode.length() > 20) {
            throw new IllegalArgumentException("Course code must be 20 characters or fewer.");
        }
        if (!cleanCode.matches("[A-Za-z0-9][A-Za-z0-9 \\-]*")) {
            throw new IllegalArgumentException("Course code may contain letters, numbers, spaces, and hyphens.");
        }
        return courses.insert(cleanCode.toUpperCase(), cleanTitle, clean(description));
    }

    public void delete(User caller, int courseId) {
        requireAdmin(caller);
        if (!courses.deleteById(courseId)) {
            throw new IllegalArgumentException("Course not found.");
        }
    }

    public List<Course> mentoringSubjects(User caller) {
        requireActiveRole(caller, Role.MENTOR);
        return courses.findByMentor(caller.id());
    }

    public boolean isSignedUp(User caller, int courseId) {
        requireActiveRole(caller, Role.MENTOR);
        return courses.isMentorFor(caller.id(), courseId);
    }

    public void signUp(User caller, int courseId) {
        requireActiveRole(caller, Role.MENTOR);
        courses.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found."));
        courses.addMentorCourse(caller.id(), courseId);
    }

    public void quit(User caller, int courseId) {
        requireActiveRole(caller, Role.MENTOR);
        if (!courses.removeMentorCourse(caller.id(), courseId)) {
            throw new IllegalArgumentException("You are not signed up to mentor this course.");
        }
    }

    public void seedDefaults() {
        if (courses.count() > 0) {
            return;
        }
        courses.insert("EECS3311", "Software Design", "Object-oriented design, testing, and architecture.");
        courses.insert("EECS3101", "Design and Analysis of Algorithms", "Algorithm design techniques and complexity.");
        courses.insert("EECS2030", "Advanced Object Oriented Programming", "Java programming and data structures.");
        courses.insert("MATH1013", "Applied Calculus", "Differential and integral calculus for science students.");
    }

    private void requireLoggedIn(User caller) {
        if (caller == null) {
            throw new SecurityException("You must be logged in to view courses.");
        }
    }

    private void requireAdmin(User caller) {
        if (caller == null || caller.role() != Role.ADMIN) {
            throw new SecurityException("Only administrators can manage the course repository.");
        }
    }

    private void requireActiveRole(User caller, Role role) {
        if (caller == null || caller.role() != role || caller.status() != Status.ACTIVE) {
            throw new SecurityException("Only active mentors can manage mentoring subjects.");
        }
    }

    private static String required(String value, String message) {
        String cleaned = clean(value);
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return cleaned;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
