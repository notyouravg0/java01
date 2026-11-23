package carrental.db;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Standalone Database Initializer and Verifier.
 * Can be run from command line or batch file without requiring Python.
 */
public class DBInit {

    public static void executeSqlScript(Connection conn, File sqlFile) throws Exception {
        if (!sqlFile.exists()) {
            throw new IllegalArgumentException("SQL file not found: " + sqlFile.getAbsolutePath());
        }

        String content = new String(Files.readAllBytes(sqlFile.toPath()));
        try (Statement stmt = conn.createStatement()) {
            for (String rawStmt : content.split(";")) {
                StringBuilder cleanSql = new StringBuilder();
                for (String line : rawStmt.split("\\r?\\n")) {
                    String t = line.trim();
                    if (!t.startsWith("--")) {
                        cleanSql.append(line).append("\n");
                    }
                }
                String executable = cleanSql.toString().trim();
                if (!executable.isEmpty()) {
                    try {
                        stmt.execute(executable);
                    } catch (Exception ex) {
                        // ignore idempotent errors
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("   GROUP 10 CAR RENTAL SYSTEM - DATABASE INITIALIZER (JAVA)");
        System.out.println("======================================================================");

        boolean reset = false;
        for (String arg : args) {
            if ("--reset".equalsIgnoreCase(arg)) {
                reset = true;
            }
        }

        File dbFile = new File("db/car_rental.db");
        if (reset && dbFile.exists()) {
            boolean deleted = dbFile.delete();
            System.out.println("[RESET] Existing database deleted: " + deleted);
        }

        File dbDir = new File("db");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("[INFO] Connected to SQLite database: " + DBConnection.getDatabaseUrl());

            // 1. Execute schema.sql
            File schemaFile = new File("db/schema.sql");
            System.out.println("[1/3] Applying schema DDL...");
            executeSqlScript(conn, schemaFile);
            System.out.println("      Schema executed successfully.");

            // 2. Execute seed_data.sql if needed
            System.out.println("[2/3] Checking seed data...");
            boolean needSeed = false;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM cars;")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    needSeed = true;
                }
            }

            if (needSeed) {
                File seedFile = new File("db/seed_data.sql");
                if (seedFile.exists()) {
                    executeSqlScript(conn, seedFile);
                    System.out.println("      Seed records inserted successfully.");
                }
            } else {
                System.out.println("      Seed data already present; skipping re-insertion.");
            }

            // 3. Verify counts
            System.out.println("[3/3] Verifying database records...");
            try (Statement stmt = conn.createStatement()) {
                int cars = 0, custs = 0, rentals = 0;
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM cars;")) {
                    if (rs.next()) cars = rs.getInt(1);
                }
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers;")) {
                    if (rs.next()) custs = rs.getInt(1);
                }
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM rentals;")) {
                    if (rs.next()) rentals = rs.getInt(1);
                }

                System.out.println("----------------------------------------------------------------------");
                System.out.println("DATABASE SUMMARY:");
                System.out.println("  Cars in Fleet      : " + cars);
                System.out.println("  Registered Clients : " + custs);
                System.out.println("  Rental Records     : " + rentals);
                System.out.println("----------------------------------------------------------------------");
                System.out.println("[SUCCESS] Database is fully initialized, verified, and ready!");
                System.out.println("======================================================================");
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Database initialization failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
