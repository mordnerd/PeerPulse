package com.peerpulse.db;

import java.nio.charset.StandardCharsets;
import java.sql.*;

public final class Database {
    private static String url = "jdbc:sqlite:peerpulse.db";

    private Database() {}

    public static void setUrl(String newUrl)  { url = newUrl; }

    public static Connection get() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    // Creates all the tables from schema.sql if they don't already exist
    public static void init() {
        try (Connection c = get(); Statement s = c.createStatement()) {

            String sql = new String(
                    Database.class.getResourceAsStream("/schema.sql").readAllBytes(),
                    StandardCharsets.UTF_8);

            for (String stmt : sql.split(";")) {
                if (!stmt.isBlank()) s.execute(stmt);

            }
        } catch (Exception e) {
            throw new RuntimeException("Database initialisation failed", e);
        }
    }
}