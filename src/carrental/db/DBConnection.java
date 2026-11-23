package carrental.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Unit 4 Database Connection Manager for SQLite.
 * Provides singleton connection management, foreign key pragma enforcement,
 * busy timeout configuration, and configurable connection URLs for testing.
 */
public class DBConnection {
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:db/car_rental.db";
    private static String customDbUrl = null;
    private static boolean driverLoaded = false;

    private static synchronized void loadDriver() throws SQLException {
        if (!driverLoaded) {
            try {
                Class.forName("org.sqlite.JDBC");
                driverLoaded = true;
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC Driver (org.sqlite.JDBC) not found. " +
                        "Ensure sqlite-jdbc jar is located in the lib/ folder and added to classpath.", e);
            }
        }
    }

    public static synchronized void setDatabaseUrl(String url) {
        customDbUrl = url;
    }

    public static synchronized void resetDatabaseUrl() {
        customDbUrl = null;
    }

    public static synchronized String getDatabaseUrl() {
        return customDbUrl != null ? customDbUrl : DEFAULT_DB_URL;
    }

    /**
     * Obtains a new JDBC Connection configured for SQLite with PRAGMA foreign_keys = ON.
     */
    public static Connection getConnection() throws SQLException {
        return getConnection(getDatabaseUrl());
    }

    /**
     * Obtains a JDBC Connection to a specific database URL.
     */
    public static Connection getConnection(String dbUrl) throws SQLException {
        loadDriver();

        if (dbUrl != null && dbUrl.startsWith("jdbc:sqlite:") && !dbUrl.contains(":memory:")) {
            String filePath = dbUrl.substring("jdbc:sqlite:".length());
            File dbFile = new File(filePath);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
        }

        Connection conn = DriverManager.getConnection(dbUrl);

        // Unit 4 PRAGMA enforcement for SQLite
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA busy_timeout = 5000;");
        }

        return conn;
    }

    /**
     * Validates connection health using Unit 4 explicit executeQuery.
     */
    public static boolean isValidConnection(Connection conn) {
        if (conn == null) return false;
        try {
            if (conn.isClosed()) return false;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1;")) {
                return rs.next() && rs.getInt(1) == 1;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Safe connection closer utility.
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }
}
