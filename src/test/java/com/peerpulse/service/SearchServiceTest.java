package com.peerpulse.service;

import static org.junit.jupiter.api.Assertions.*;

import com.peerpulse.model.Course;
import com.peerpulse.model.PeerMatch;
import com.peerpulse.model.Role;
import com.peerpulse.model.User;
import com.peerpulse.support.TestDb;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchServiceTest extends TestDb {
    private final CourseService courses = new CourseService();
    private final SearchService search = new SearchService();

    @Test
    void tc04_searchByCodeTitleAndSkillReturnsProfiles() {
        Course design = courses.add(admin, "EECS3311", "Software Design", "");
        Course algo = courses.add(admin, "EECS3101", "Algorithms", "");
        User ada = user("ada@yorku.ca", "Ada Mentor", Role.MENTOR, "Java");
        User grace = user("grace@yorku.ca", "Grace Mentor", Role.MENTOR, "Python");
        courses.signUp(ada, design.id());
        courses.signUp(grace, algo.id());
        User ann = user("ann@yorku.ca", "Ann Mentee", Role.MENTEE, "Java, SQL");

        PeerMatch byCode = search.search(ann, "EECS3311", null, "").get(0);
        assertEquals("Ada Mentor", byCode.fullName());
        assertEquals("Year 2", byCode.academicYear());
        assertEquals("Computer Science", byCode.major());
        assertTrue(byCode.courses().contains("EECS3311"));
        assertFalse(byCode.toString().contains("ada@yorku.ca"));
        assertFalse(byCode.toString().toLowerCase().contains("password"));

        assertEquals("Ada Mentor", search.search(ann, "software design", null, "").get(0).fullName());
        assertEquals("Grace Mentor", search.search(ann, "python", null, "").get(0).fullName());
        assertEquals("Ann Mentee", search.search(ada, "sql", null, "").get(0).fullName());
    }

    @Test
    void tc05_noMatchesShowEmptyState() {
        User mentee = user("ann@yorku.ca", "Ann Mentee", Role.MENTEE, "Java");
        List<PeerMatch> matches = search.search(mentee, "NoSuchCourse", null, "");
        assertTrue(matches.isEmpty());
        assertEquals("No results - change search", SearchService.resultSummary(0));
    }

    @Test
    void tc06_searchRespectsRolesAndCompletesQuickly() {
        Course design = courses.add(admin, "EECS3311", "Software Design", "");
        User ada = user("ada@yorku.ca", "Ada Mentor", Role.MENTOR, "Java");
        courses.signUp(ada, design.id());
        User ann = user("ann@yorku.ca", "Ann Mentee", Role.MENTEE, "Java");

        assertThrows(SecurityException.class, () -> search.search(admin, "Java", null, ""));

        long start = System.nanoTime();
        List<PeerMatch> matches = search.search(ann, "EECS3311", null, "");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertEquals(1, matches.size());
        assertTrue(elapsedMs < 3000, "Search took " + elapsedMs + " ms");
    }
}
