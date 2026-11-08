package com.peerpulse.service;

import static org.junit.jupiter.api.Assertions.*;

import com.peerpulse.model.Course;
import com.peerpulse.model.Role;
import com.peerpulse.model.User;
import com.peerpulse.support.TestDb;
import org.junit.jupiter.api.Test;

class CourseServiceTest extends TestDb {
    private final CourseService courses = new CourseService();

    @Test
    void tc01_adminAddsDeletesAndUsersCanBrowse() {
        Course course = courses.add(admin, "eecs3311", "Software Design", "OOP");
        assertEquals("EECS3311", course.code());
        assertEquals("Software Design", course.title());

        User mentee = user("ann@yorku.ca", "Ann Mentee", Role.MENTEE, "Java");
        assertEquals(1, courses.listAll(mentee).size());

        for (int i = 2; i <= 10; i++) {
            courses.add(admin, "C" + i, "Course " + i, "");
        }
        assertEquals(10, courses.listAll(mentee).size());

        courses.delete(admin, course.id());
        assertEquals(9, courses.listAll(admin).size());
    }

    @Test
    void tc02_mentorSignsUpAndWithdraws() {
        Course course = courses.add(admin, "EECS3311", "Software Design", "");
        User mentor = user("mentor@yorku.ca", "Ada Mentor", Role.MENTOR, "Java");

        courses.signUp(mentor, course.id());
        assertEquals("EECS3311", courses.mentoringSubjects(mentor).get(0).code());

        courses.quit(mentor, course.id());
        assertTrue(courses.mentoringSubjects(mentor).isEmpty());
    }

    @Test
    void tc03_courseActionsFollowRoles() {
        Course course = courses.add(admin, "EECS3311", "Software Design", "");
        User mentor = user("mentor@yorku.ca", "Ada Mentor", Role.MENTOR, "Java");
        User mentee = user("ann@yorku.ca", "Ann Mentee", Role.MENTEE, "Java");

        assertThrows(SecurityException.class, () -> courses.add(mentee, "EECS3101", "Algorithms", ""));
        assertThrows(SecurityException.class, () -> courses.delete(mentor, course.id()));
        assertThrows(SecurityException.class, () -> courses.signUp(mentee, course.id()));
        assertThrows(SecurityException.class, () -> courses.quit(admin, course.id()));
    }
}
