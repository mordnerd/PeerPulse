package com.peerpulse.support;

import com.peerpulse.db.Database;
import com.peerpulse.db.UserDAO;
import com.peerpulse.model.Profile;
import com.peerpulse.model.Role;
import com.peerpulse.model.Status;
import com.peerpulse.model.User;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

/** Test users are inserted directly so these tests do not go through login or registration. */
public abstract class TestDb {
    protected final User admin = new User(0, "admin@test", "x", "Admin", Role.ADMIN, Status.ACTIVE);

    @BeforeEach
    void setUpDatabase(@TempDir Path dir) {
        Database.setUrl("jdbc:sqlite:" + dir.resolve("test.db").toAbsolutePath());
        Database.init();
    }

    protected User user(String email, String name, Role role, String skills) {
        int id = new UserDAO().insert(email, "x", name, role, Status.ACTIVE,
                new Profile(0, "Year 2", "Computer Science", skills, "", ""));
        return new UserDAO().findById(id).orElseThrow();
    }
}
