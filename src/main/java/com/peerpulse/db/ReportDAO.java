package com.peerpulse.db;

import java.sql.*;

public class ReportDAO {
    //This is meant to read count reports for the admin (Still not fully set up yet)
    public int countOpen() {
        try (Connection c = Database.get();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM reports WHERE resolved = 0")) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}