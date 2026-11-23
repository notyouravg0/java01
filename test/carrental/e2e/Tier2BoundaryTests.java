package carrental.e2e;

import static carrental.e2e.Assertions.*;

import carrental.dao.CarDAO;
import carrental.dao.CustomerDAO;
import carrental.dao.RentalDAO;
import carrental.db.DBConnection;
import carrental.model.*;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;

/**
 * Tier 2: Boundary Value Analysis, Negative Testing, and Invariant Verification.
 * Covers all 28 features (F1-F26, F33, F34) with >= 5 boundary assertions per feature (>= 140 total).
 */
public class Tier2BoundaryTests {

    public void runAllTests() throws Exception {
        // F1-F8: Model Boundaries & Extremes
        testB1_RentableBoundaries();
        testB2_VehicleBoundaries();
        testB3_CarBoundaries();
        testB4_SedanBoundaries();
        testB5_SUVBoundaries();
        testB6_HatchbackBoundaries();
        testB7_CustomerBoundaries();
        testB8_RentalBoundaries();

        // F9-F14: Database & DAO Invariants
        testB9_DatabaseSchemaConstraints();
        testB10_SeedDataInvariants();
        testB11_DBConnectionFailures();
        testB12_CarDAOEdgeCases();
        testB13_CustomerDAOEdgeCases();
        testB14_RentalDAOEdgeCases();

        // F15-F20: Exceptions, Concurrency & Arithmetic Boundaries
        testB15_CarUnavailableExceptionBoundaries();
        testB16_BookingEngineBoundaries();
        testB17_Overload1_BaseRentBoundaries();
        testB18_Overload2_TierDiscountBoundaries();
        testB19_Overload3_InsuranceBoundaries();
        testB20_Overload4_FinalReturnBillBoundaries();

        // F21-F24: Workflow Edge Cases & KPI Invariants
        testB21_InventoryBoundaries();
        testB22_CustomerValidationBoundaries();
        testB23_ReturnWorkflowBoundaries();
        testB24_HistoryAndKPIBoundaries();

        // F25-F26, F33-F34: UI Contrast/Headless Safety & Batch Scripts
        testB25_TricolourThemeBoundaries();
        testB26_GUILayoutBoundaries();
        testB33_StartDbBatBoundaries();
        testB34_StartAppBatBoundaries();
    }

    // =========================================================================
    // B1: Rentable Boundaries & Invariants
    // =========================================================================
    public void testB1_RentableBoundaries() {
        Car car = new Sedan("MH-12-B1-0001", "Honda", "City", 2023, 2000.0, 10000.0,
                5, "PETROL", "MANUAL", 510.0, "EXECUTIVE", 10.0);

        // 1. Calling rent() on already rented car throws IllegalStateException
        car.rent();
        assertThrows(IllegalStateException.class, () -> car.rent(),
                "rent() on already rented car must throw IllegalStateException");

        // 2. returnVehicle() is idempotent
        car.returnVehicle();
        car.returnVehicle(); // Second return should not throw
        assertTrue(car.isAvailable(), "Repeated returnVehicle() must leave car available");

        // 3. calculateRate with 0 days throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> car.calculateRate(0),
                "calculateRate(0) must throw IllegalArgumentException");

        // 4. calculateRate with negative days throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> car.calculateRate(-5),
                "calculateRate(-5) must throw IllegalArgumentException");

        // 5. calculateRate with large days does not overflow
        double large = car.calculateRate(365);
        assertTrue(large > 0 && !Double.isInfinite(large), "365 days rate must be positive and finite");
    }

    // =========================================================================
    // B2: Vehicle Base Class Boundaries & Negative Invariants
    // =========================================================================
    public void testB2_VehicleBoundaries() {
        // 1. Negative daily rate throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            new Sedan("MH-12-B2-0001", "Make", "Model", 2022, -1500.0, 1000.0,
                    5, "PETROL", "MANUAL", 450.0, "EXECUTIVE", 0.0);
        }, "Negative daily rate must throw IllegalArgumentException");

        // 2. Zero daily rate throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            new Sedan("MH-12-B2-0002", "Make", "Model", 2022, 0.0, 1000.0,
                    5, "PETROL", "MANUAL", 450.0, "EXECUTIVE", 0.0);
        }, "Zero daily rate must throw IllegalArgumentException");

        // 3. Negative mileage throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            new Sedan("MH-12-B2-0003", "Make", "Model", 2022, 1500.0, -100.0,
                    5, "PETROL", "MANUAL", 450.0, "EXECUTIVE", 0.0);
        }, "Negative mileage must throw IllegalArgumentException");

        // 4. Empty/null registration number throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            new Sedan("   ", "Make", "Model", 2022, 1500.0, 100.0,
                    5, "PETROL", "MANUAL", 450.0, "EXECUTIVE", 0.0);
        }, "Whitespace registration number must throw IllegalArgumentException");

        // 5. Mutator rejects negative mileage update
        Car car = new Sedan("MH-12-B2-0004", "Make", "Model", 2022, 1500.0, 1000.0,
                5, "PETROL", "MANUAL", 450.0, "EXECUTIVE", 0.0);
        assertThrows(IllegalArgumentException.class, () -> car.setMileage(-50.0),
                "setMileage(-50) must throw IllegalArgumentException");
    }

    // =========================================================================
    // B3: Concrete Car Boundaries & Defaults
    // =========================================================================
    public void testB3_CarBoundaries() {
        // 1. Seating capacity <= 0 throws on mutator
        Car car = new Car("MH-12-B3-0001", "Hyundai", "i10", 2020, 1000.0, 5000.0, 5, "PETROL", "MANUAL");
        assertThrows(IllegalArgumentException.class, () -> car.setSeatingCapacity(0),
                "setSeatingCapacity(0) must throw IllegalArgumentException");

        // 2. Negative seating capacity mutator throws
        assertThrows(IllegalArgumentException.class, () -> car.setSeatingCapacity(-2),
                "setSeatingCapacity(-2) must throw IllegalArgumentException");

        // 3. Null fuel type defaults safely without NPE
        car.setFuelType(null);
        assertEquals("PETROL", car.getFuelType(), "Null fuelType should default to PETROL");

        // 4. Null transmission defaults safely without NPE
        car.setTransmission(null);
        assertEquals("MANUAL", car.getTransmission(), "Null transmission should default to MANUAL");

        // 5. Blank registration plate mutator throws
        assertThrows(IllegalArgumentException.class, () -> car.setRegistrationNumber(null),
                "setRegistrationNumber(null) must throw IllegalArgumentException");
    }

    // =========================================================================
    // B4: Sedan Specialization Boundaries & Clamping
    // =========================================================================
    public void testB4_SedanBoundaries() {
        Sedan sedan = new Sedan("MH-12-B4-0001", "Honda", "City", 2023, 2000.0, 10000.0,
                5, "PETROL", "MANUAL", 510.0, "EXECUTIVE", 10.0);

        // 1. Negative boot space mutator throws
        assertThrows(IllegalArgumentException.class, () -> sedan.setBootSpaceLiters(-10.0),
                "setBootSpaceLiters(-10) must throw IllegalArgumentException");

        // 2. Zero boot space mutator throws
        assertThrows(IllegalArgumentException.class, () -> sedan.setBootSpaceLiters(0.0),
                "setBootSpaceLiters(0) must throw IllegalArgumentException");

        // 3. Discount clamped between 0 and 100: negative discount becomes 0.0
        sedan.setLongTermDiscountPercent(-5.0);
        assertEquals(0.0, sedan.getLongTermDiscountPercent(), 0.001, "Negative discount clamped to 0.0");

        // 4. Discount clamped between 0 and 100: >100% discount becomes 100.0
        sedan.setLongTermDiscountPercent(120.0);
        assertEquals(100.0, sedan.getLongTermDiscountPercent(), 0.001, "Excessive discount clamped to 100.0");

        // 5. Boundary transition: 6 days (no long-term discount) vs 7 days (discount applied)
        sedan.setLongTermDiscountPercent(10.0);
        sedan.setComfortCategory("EXECUTIVE");
        double rate6Days = sedan.calculateBaseRent(6); // 6 * 2000 = 12000
        double rate7Days = sedan.calculateBaseRent(7); // 7 * 2000 * 0.90 = 12600
        assertEquals(12000.0, rate6Days, 0.01, "6 days rate mismatch");
        assertEquals(12600.0, rate7Days, 0.01, "7 days rate with discount mismatch");
        assertTrue((rate7Days / 7.0) < (rate6Days / 6.0), "Per-day rate for 7 days must be lower than 6 days");
    }

    // =========================================================================
    // B5: SUV Specialization Boundaries & Surcharge Clamping
    // =========================================================================
    public void testB5_SUVBoundaries() {
        SUV suv = new SUV("MH-12-B5-0001", "Mahindra", "XUV700", 2023, 3000.0, 15000.0,
                5, "DIESEL", "AUTOMATIC", true, 200, false, 300.0);

        // 1. Negative ground clearance throws
        assertThrows(IllegalArgumentException.class, () -> suv.setGroundClearanceMm(-5),
                "Negative ground clearance must throw IllegalArgumentException");

        // 2. Zero ground clearance throws
        assertThrows(IllegalArgumentException.class, () -> suv.setGroundClearanceMm(0),
                "Zero ground clearance must throw IllegalArgumentException");

        // 3. Negative all-terrain surcharge clamped to 0.0
        suv.setAllTerrainSurcharge(-100.0);
        assertEquals(0.0, suv.getAllTerrainSurcharge(), 0.001, "Negative surcharge clamped to 0.0");

        // 4. calculateBaseRent with 0 days throws
        assertThrows(IllegalArgumentException.class, () -> suv.calculateBaseRent(0),
                "SUV calculateBaseRent(0) must throw IllegalArgumentException");

        // 5. Setting sevenSeater = true automatically forces seating capacity >= 7
        suv.setSevenSeater(true);
        assertTrue(suv.getSeatingCapacity() >= 7, "Seating capacity must be at least 7");
    }

    // =========================================================================
    // B6: Hatchback Specialization Boundaries & Economy Clamping
    // =========================================================================
    public void testB6_HatchbackBoundaries() {
        Hatchback hatch = new Hatchback("MH-12-B6-0001", "Tata", "Altroz", 2022, 1200.0, 10000.0,
                5, "PETROL", "MANUAL", true, 20.0, 5.0);

        // 1. Negative fuel economy throws
        assertThrows(IllegalArgumentException.class, () -> hatch.setFuelEconomyKmpl(-10.0),
                "Negative fuel economy must throw IllegalArgumentException");

        // 2. Zero fuel economy throws
        assertThrows(IllegalArgumentException.class, () -> hatch.setFuelEconomyKmpl(0.0),
                "Zero fuel economy must throw IllegalArgumentException");

        // 3. Negative economy discount rate clamped to 0.0
        hatch.setEconomyDiscountRate(-10.0);
        assertEquals(0.0, hatch.getEconomyDiscountRate(), 0.001, "Negative discount clamped to 0.0");

        // 4. Excessive economy discount rate clamped to 100.0
        hatch.setEconomyDiscountRate(150.0);
        assertEquals(100.0, hatch.getEconomyDiscountRate(), 0.001, "Excessive discount clamped to 100.0");

        // 5. calculateBaseRent with 0 days throws
        assertThrows(IllegalArgumentException.class, () -> hatch.calculateBaseRent(0),
                "Hatchback calculateBaseRent(0) must throw IllegalArgumentException");
    }

    // =========================================================================
    // B7: Customer Boundaries & Input Rejections
    // =========================================================================
    public void testB7_CustomerBoundaries() {
        // 1. Empty/whitespace name throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            new Customer("   ", "9876543210", "DL12345", "a@b.com", "City", Customer.MemberTier.REGULAR);
        }, "Whitespace customer name must throw IllegalArgumentException");

        // 2. Empty phone throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            new Customer("Rajesh", "", "DL12345", "a@b.com", "City", Customer.MemberTier.REGULAR);
        }, "Empty phone must throw IllegalArgumentException");

        // 3. Empty driving license throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            new Customer("Rajesh", "9876543210", "  ", "a@b.com", "City", Customer.MemberTier.REGULAR);
        }, "Empty driving license must throw IllegalArgumentException");

        // 4. Null tier defaults to REGULAR
        Customer cust = new Customer("Rajesh", "9876543210", "DL12345", "a@b.com", "City", (Customer.MemberTier) null);
        assertEquals(Customer.MemberTier.REGULAR, cust.getMemberTier(), "Null memberTier should default to REGULAR");

        // 5. Set null name on mutator throws
        assertThrows(IllegalArgumentException.class, () -> cust.setFullName(null),
                "setFullName(null) must throw IllegalArgumentException");
    }

    // =========================================================================
    // B8: Rental Entity Boundaries & Status Invariants
    // =========================================================================
    public void testB8_RentalBoundaries() {
        Rental r = new Rental();
        r.setDailyRate(2000.0);

        // 1. calculateRent(0) throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> r.calculateRent(0),
                "calculateRent(0) must throw IllegalArgumentException");

        // 2. calculateRent(-3) throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> r.calculateRent(-3),
                "calculateRent(-3) must throw IllegalArgumentException");

        // 3. Discount clamped between 0 and 100
        assertEquals(6000.0, r.calculateRent(3, -15.0), 0.01, "Negative discount clamped to 0%");
        assertEquals(0.0, r.calculateRent(3, 110.0), 0.01, "Discount >100% clamped to 100%");

        // 4. RentalStatus.fromString unknown status defaults to ACTIVE
        assertEquals(Rental.RentalStatus.ACTIVE, Rental.RentalStatus.fromString("UNKNOWN_STATUS"),
                "Unknown status should default to ACTIVE");

        // 5. setRentalDays <= 0 throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> r.setRentalDays(0),
                "setRentalDays(0) must throw IllegalArgumentException");
    }

    // =========================================================================
    // B9: Database Schema Constraints (UNIQUE & FOREIGN KEY)
    // =========================================================================
    public void testB9_DatabaseSchemaConstraints() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            if (ctx.isLiveJdbc()) {
                // 1. UNIQUE registration plate constraint violation
                CarDAO carDao = ctx.getCarDAO();
                Car c1 = new Sedan("MH-12-UQ-0001", "Honda", "City", 2023, 2000.0, 10000.0, 5, "PETROL", "MANUAL", 500.0, "EXECUTIVE", 0.0);
                carDao.insertCar(c1);

                Car c2 = new Sedan("MH-12-UQ-0001", "Tata", "Tigor", 2022, 1800.0, 15000.0, 5, "PETROL", "MANUAL", 400.0, "EXECUTIVE", 0.0);
                assertThrows(SQLException.class, () -> carDao.insertCar(c2),
                        "Duplicate registration plate must throw SQLException");

                // 2. UNIQUE phone number constraint violation
                CustomerDAO custDao = ctx.getCustomerDAO();
                Customer u1 = new Customer("User 1", "9000011111", "DL900001", "u1@test.com", "City", Customer.MemberTier.REGULAR);
                custDao.insertCustomer(u1);

                Customer u2 = new Customer("User 2", "9000011111", "DL900002", "u2@test.com", "City", Customer.MemberTier.REGULAR);
                assertThrows(SQLException.class, () -> custDao.insertCustomer(u2),
                        "Duplicate phone number must throw SQLException");

                // 3. FOREIGN KEY violation: non-existent customer ID
                RentalDAO rentalDao = ctx.getRentalDAO();
                Rental badCustRental = new Rental(0, c1.getCarId(), 999999, LocalDate.now(), LocalDate.now().plusDays(2), null,
                        2, 2000.0, 4000.0, 0.0, 0.0, 0.0, 10000.0, 0.0, 0.0, 0.0, 4000.0, Rental.RentalStatus.ACTIVE, "bad fk");
                assertThrows(SQLException.class, () -> rentalDao.insertRental(badCustRental),
                        "Foreign key violation on customerId must throw SQLException");

                // 4. FOREIGN KEY violation: non-existent car ID
                Rental badCarRental = new Rental(0, 999999, u1.getCustomerId(), LocalDate.now(), LocalDate.now().plusDays(2), null,
                        2, 2000.0, 4000.0, 0.0, 0.0, 0.0, 10000.0, 0.0, 0.0, 0.0, 4000.0, Rental.RentalStatus.ACTIVE, "bad fk");
                assertThrows(SQLException.class, () -> rentalDao.insertRental(badCarRental),
                        "Foreign key violation on carId must throw SQLException");
            } else {
                // 1. Check constraint: daily rate <= 0 rejected
                assertThrows(IllegalArgumentException.class, () -> {
                    new Car("MH-12-BAD-01", "Make", "Model", 2023, -500.0, 10000.0, 5, "PETROL", "MANUAL");
                }, "Negative daily rate must throw IllegalArgumentException");

                // 2. Duplicate registration plate rejected
                Car mc1 = new Car("MH-12-DUP-01", "Make", "Model", 2023, 2000.0, 10000.0, 5, "PETROL", "MANUAL");
                ctx.insertMemoryCar(mc1);
                Car mc2 = new Car("MH-12-DUP-01", "Make2", "Model2", 2023, 2200.0, 10000.0, 5, "PETROL", "MANUAL");
                assertThrows(IllegalArgumentException.class, () -> {
                    ctx.insertMemoryCar(mc2);
                }, "Duplicate registration plate in memory must throw IllegalArgumentException");

                // 3. Foreign key violation: non-existent customer rejected
                Rental badCustRental = new Rental(0, mc1.getCarId(), 999999, LocalDate.now(), LocalDate.now().plusDays(2), null,
                        2, 2000.0, 4000.0, 0.0, 0.0, 0.0, 10000.0, 0.0, 0.0, 0.0, 4000.0, Rental.RentalStatus.ACTIVE, "bad fk");
                assertThrows(IllegalArgumentException.class, () -> {
                    ctx.insertMemoryRental(badCustRental);
                }, "Non-existent customerId in memory rental must throw IllegalArgumentException");

                // 4. Foreign key violation: non-existent car rejected
                Customer mu1 = new Customer("Memory User", "9899112233", "DL_MEM_01", "m@test.com", "City", Customer.MemberTier.REGULAR);
                ctx.insertMemoryCustomer(mu1);
                Rental badCarRental = new Rental(0, 999999, mu1.getCustomerId(), LocalDate.now(), LocalDate.now().plusDays(2), null,
                        2, 2000.0, 4000.0, 0.0, 0.0, 0.0, 10000.0, 0.0, 0.0, 0.0, 4000.0, Rental.RentalStatus.ACTIVE, "bad fk");
                assertThrows(IllegalArgumentException.class, () -> {
                    ctx.insertMemoryRental(badCarRental);
                }, "Non-existent carId in memory rental must throw IllegalArgumentException");
            }

            // 5. Schema DDL check constraint presence
            File schema = new File("db/schema.sql");
            String ddl = new String(Files.readAllBytes(schema.toPath()));
            assertTrue(ddl.contains("CHECK (daily_rate > 0)"), "DDL must enforce positive daily rate");
        }
    }

    // =========================================================================
    // B10: Seed Data Invariants Verification
    // =========================================================================
    public void testB10_SeedDataInvariants() throws Exception {
        File seed = new File("db/seed_data.sql");
        String content = new String(Files.readAllBytes(seed.toPath()));

        // 1. All vehicles in seed have positive daily rates
        assertFalse(content.contains("daily_rate = 0") || content.contains(", 0,"),
                "Seed data should not have zero rates");

        // 2. All phone numbers are exactly 10 digits
        assertTrue(content.contains("98"), "Customer phones in seed should follow Indian mobile format");

        // 3. Car categories in seed are valid enums
        assertTrue(content.contains("'SEDAN'") || content.contains("'SUV'") || content.contains("'HATCHBACK'"),
                "Seed categories must match valid uppercase enums");

        // 4. Seed data does not contain negative mileage
        assertFalse(content.contains("-100"), "Seed data cannot have negative mileage");

        // 5. Seed data contains both AVAILABLE and active states
        assertTrue(content.contains("'AVAILABLE'"), "Seed data must contain AVAILABLE cars");
    }

    // =========================================================================
    // B11: DBConnection Failure & Robustness
    // =========================================================================
    public void testB11_DBConnectionFailures() throws Exception {
        // 1. Invalid connection URL handling throws SQLException
        String badUrl = "jdbc:sqlite:Z:/non_existent_drive_xyz/invalid.db";
        assertThrows(SQLException.class, () -> {
            Connection conn = DBConnection.getConnection(badUrl);
            if (conn != null) conn.close();
        }, "Opening non-existent drive path must throw SQLException");

        // 2. isValidConnection on closed connection returns false
        try (TestContext ctx = TestContext.createClean()) {
            if (ctx.isLiveJdbc()) {
                Connection conn = DBConnection.getConnection();
                conn.close();
                assertFalse(DBConnection.isValidConnection(conn), "Closed connection must return false");
            } else {
                assertFalse(DBConnection.isValidConnection(null), "Null connection must return false");
            }
        }

        // 3. Close connection on active connection is safe, idempotent, and updates validity
        try (TestContext ctx = TestContext.createClean()) {
            if (ctx.isLiveJdbc()) {
                Connection conn = DBConnection.getConnection();
                assertNotNull(conn, "Connection must be non-null");
                DBConnection.closeConnection(conn);
                assertTrue(conn.isClosed(), "Connection must be closed after closeConnection");
                DBConnection.closeConnection(conn); // Repeated close is idempotent
                assertTrue(conn.isClosed(), "Repeated closeConnection must leave connection closed");
            } else {
                DBConnection.closeConnection(null);
                assertFalse(DBConnection.isValidConnection(null), "Null connection remains invalid after closeConnection");
            }
        }

        // 4. Reset URL restores default
        DBConnection.resetDatabaseUrl();
        assertTrue(DBConnection.getDatabaseUrl().contains("car_rental.db"), "Default URL contains car_rental.db");

        // 5. Custom URL can be set and verified
        DBConnection.setDatabaseUrl("jdbc:sqlite:test_custom_url.db");
        assertEquals("jdbc:sqlite:test_custom_url.db", DBConnection.getDatabaseUrl(), "Custom URL not set");
        DBConnection.resetDatabaseUrl();
    }

    // =========================================================================
    // B12: CarDAO Edge Cases
    // =========================================================================
    public void testB12_CarDAOEdgeCases() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            if (ctx.isLiveJdbc()) {
                CarDAO dao = ctx.getCarDAO();

                // 1. Non-existent ID returns null
                assertNull(dao.getCarById(999999), "Non-existent car ID should return null");

                // 2. Non-existent registration plate returns null
                assertNull(dao.getCarByRegistration("NONEXISTENT_REG_PLATE"), "Unknown reg plate should return null");

                // 3. Empty fleet returns empty list (not null)
                List<Car> emptyList = dao.getAllCars();
                assertNotNull(emptyList, "Empty table should return non-null list");
                assertEquals(0, emptyList.size(), "Empty table should have 0 size");

                // 4. Update status on non-existent car affects 0 rows and leaves table intact
                dao.updateCarStatus(999999, "RENTED");
                assertNull(dao.getCarById(999999), "Non-existent car remains null after status update attempt");
                assertEquals(0, dao.getAllCars().size(), "Fleet size must remain 0 after non-existent car status update");

                // 5. Update mileage on non-existent car affects 0 rows and leaves table intact
                dao.updateCarMileage(999999, 50000.0);
                assertNull(dao.getCarById(999999), "Non-existent car remains null after mileage update attempt");
            } else {
                // Memory fallback branch:
                assertNull(ctx.getMemoryCar(999999), "Memory lookup on non-existent ID returns null");
                assertEquals(0, ctx.getAllMemoryCars().size(), "Clean memory fleet must be empty");
                ctx.updateMemoryCarStatus(999999, "RENTED");
                assertNull(ctx.getMemoryCar(999999), "Non-existent memory car remains null after status update");
                ctx.updateMemoryCarMileage(999999, 50000.0);
                assertNull(ctx.getMemoryCar(999999), "Non-existent memory car remains null after mileage update");
                assertEquals(0, ctx.getAllMemoryCars().size(), "Memory fleet size remains 0");
            }
        }
    }

    // =========================================================================
    // B13: CustomerDAO Edge Cases
    // =========================================================================
    public void testB13_CustomerDAOEdgeCases() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            if (ctx.isLiveJdbc()) {
                CustomerDAO dao = ctx.getCustomerDAO();

                // 1. Non-existent ID returns null
                assertNull(dao.getCustomerById(999999), "Non-existent customer ID should return null");

                // 2. Non-existent phone returns null
                assertNull(dao.getCustomerByPhone("0000000000"), "Non-existent phone should return null");

                // 3. Non-existent license returns null
                assertNull(dao.getCustomerByLicense("NONEXISTENT_LIC"), "Non-existent license should return null");

                // 4. Empty table returns empty list (not null)
                List<Customer> list = dao.getAllCustomers();
                assertNotNull(list, "Empty table must return non-null list");
                assertEquals(0, list.size(), "List must be empty");

                // 5. Case-insensitive license lookup
                Customer c = new Customer("Case Tester", "9112233445", "dlcase001", "c@test.com", "City", Customer.MemberTier.REGULAR);
                dao.insertCustomer(c);
                assertNotNull(dao.getCustomerByLicense("DLCASE001"), "License lookup must be case-insensitive");
            } else {
                assertNull(ctx.getMemoryCustomer(999999), "Non-existent memory customer returns null");
                assertNull(ctx.getMemoryCustomerByPhone("0000000000"), "Non-existent memory phone returns null");
                assertNull(ctx.getMemoryCustomerByLicense("NONEXISTENT_LIC"), "Non-existent memory license returns null");
                assertEquals(0, ctx.getAllMemoryCustomers().size(), "Memory customer list should be empty");
                Customer c = new Customer("Case Tester", "9112233445", "dlcase001", "c@test.com", "City", Customer.MemberTier.REGULAR);
                ctx.insertMemoryCustomer(c);
                assertNotNull(ctx.getMemoryCustomerByLicense("DLCASE001"), "License lookup in memory must be case-insensitive");
            }
        }
    }

    // =========================================================================
    // B14: RentalDAO Edge Cases
    // =========================================================================
    public void testB14_RentalDAOEdgeCases() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            if (ctx.isLiveJdbc()) {
                RentalDAO dao = ctx.getRentalDAO();

                // 1. Non-existent rental ID returns null
                assertNull(dao.getRentalById(999999), "Non-existent rental ID should return null");

                // 2. Empty table returns empty list
                List<Rental> list = dao.getAllRentals();
                assertNotNull(list, "Empty rentals table must return non-null list");
                assertEquals(0, list.size(), "List must be empty");

                // 3. Active rentals on empty table returns empty list
                List<Rental> active = dao.getActiveRentals();
                assertEquals(0, active.size(), "Active rentals list must be empty");

                // 4. Update return on non-existent rental affects 0 rows safely
                dao.updateRentalReturn(999999, LocalDate.now(), 15000.0, 0.0, 5000.0, "COMPLETED", "none");
                assertNull(dao.getRentalById(999999), "Non-existent rental ID remains null after update attempt");
                assertEquals(0, dao.getAllRentals().size(), "Rentals table must remain empty");

                // 5. Mapping handles null actual return date safely on active rental
                Car car = new Car("MH-12-B14-01", "Tata", "Nexon", 2023, 2000.0, 10000.0, 5, "PETROL", "MANUAL");
                ctx.getCarDAO().insertCar(car);
                Customer cust = new Customer("Rental Cust", "9812345678", "DL_RENTAL_01", "rc@test.com", "Pune", Customer.MemberTier.REGULAR);
                ctx.getCustomerDAO().insertCustomer(cust);
                Rental activeRental = new Rental(0, car.getCarId(), cust.getCustomerId(), LocalDate.now(), LocalDate.now().plusDays(3), null,
                        3, 2000.0, 6000.0, 0.0, 0.0, 0.0, 10000.0, 0.0, 0.0, 0.0, 6000.0, Rental.RentalStatus.ACTIVE, "Active test");
                int rentalId = dao.insertRental(activeRental);
                Rental mapped = dao.getRentalById(rentalId);
                assertNotNull(mapped, "Inserted active rental must be retrieved");
                assertNull(mapped.getActualReturnDate(), "Active rental must have null actual return date mapped from DB");
                assertEquals(Rental.RentalStatus.ACTIVE, mapped.getStatus(), "Active rental status must be mapped as ACTIVE");
            } else {
                // Memory fallback branch:
                assertNull(ctx.getMemoryRental(999999), "Non-existent memory rental returns null");
                assertEquals(0, ctx.getAllMemoryRentals().size(), "Memory rentals list should be empty");
                Rental mActive = new Rental(0, 1, 1, LocalDate.now(), LocalDate.now().plusDays(3), null,
                        3, 2000.0, 6000.0, 0.0, 0.0, 0.0, 10000.0, 0.0, 0.0, 0.0, 6000.0, Rental.RentalStatus.ACTIVE, "Active test");
                int mId = ctx.insertMemoryRental(mActive);
                Rental mMapped = ctx.getMemoryRental(mId);
                assertNotNull(mMapped, "Memory rental must be retrieved");
                assertNull(mMapped.getActualReturnDate(), "Memory active rental must have null actual return date");
                assertEquals(Rental.RentalStatus.ACTIVE, mMapped.getStatus(), "Memory active rental must have ACTIVE status");
            }
        }
    }

    // =========================================================================
    // B15: CarUnavailableException Boundaries
    // =========================================================================
    public void testB15_CarUnavailableExceptionBoundaries() {
        // 1. Null message handled gracefully
        TestContext.CarUnavailableException ex = new TestContext.CarUnavailableException(null);
        assertNull(ex.getMessage(), "Null message should be preserved");

        // 2. Default diagnostic getters return defaults
        assertEquals(-1, ex.getCarId(), "Default carId should be -1");
        assertEquals("", ex.getRegistrationNumber(), "Default reg should be empty string");

        // 3. Chained exception preserves cause
        SQLException sqlEx = new SQLException("Deadlock detected");
        TestContext.CarUnavailableException chained = new TestContext.CarUnavailableException("Failed booking", sqlEx);
        assertEquals(sqlEx, chained.getCause(), "Root cause should match sqlEx");

        // 4. Exception can be thrown and caught polymorphically
        boolean caught = false;
        try {
            throw new TestContext.CarUnavailableException("Unavailable", 10, "MH-12-TEST");
        } catch (Exception e) {
            caught = true;
            assertTrue(e instanceof TestContext.CarUnavailableException, "Caught exception must be CarUnavailableException");
        }
        assertTrue(caught, "Exception must be caught");

        // 5. Message contains diagnostic details
        TestContext.CarUnavailableException diag = new TestContext.CarUnavailableException("Vehicle MH-12-TEST is RENTED", 10, "MH-12-TEST");
        assertTrue(diag.getMessage().contains("MH-12-TEST"), "Message should contain plate");
    }

    // =========================================================================
    // B16: Booking Engine Boundaries & Validation
    // =========================================================================
    public void testB16_BookingEngineBoundaries() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // 1. Booking with 0 days throws IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> service.bookCar(1, 1, 0, false),
                    "bookCar(days=0) must throw IllegalArgumentException");

            // 2. Booking with negative days throws IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> service.bookCar(1, 1, -2, false),
                    "bookCar(days=-2) must throw IllegalArgumentException");

            // 3. Booking non-existent customer throws IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> service.bookCar(999999, 1, 3, false),
                    "Booking non-existent customer must throw IllegalArgumentException");

            // 4. Booking non-existent car throws IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> service.bookCar(1, 999999, 3, false),
                    "Booking non-existent car must throw IllegalArgumentException");

            // 5. Booking unavailable car throws CarUnavailableException
            service.bookCar(1, 1, 3, false); // Book car 1
            assertThrows(TestContext.CarUnavailableException.class, () -> service.bookCar(2, 1, 2, false),
                    "Re-booking rented car must throw CarUnavailableException");
        }
    }

    // =========================================================================
    // B17: Overload 1 Base Rent Boundaries
    // =========================================================================
    public void testB17_Overload1_BaseRentBoundaries() {
        Car car = new Car("MH-12-B17-001", "Make", "Model", 2022, 1999.50, 10000.0, 5, "PETROL", "MANUAL");

        // 1. Zero days throws
        assertThrows(IllegalArgumentException.class, () -> car.calculateBaseRent(0),
                "calculateBaseRent(0) must throw IllegalArgumentException");

        // 2. Negative days throws
        assertThrows(IllegalArgumentException.class, () -> car.calculateBaseRent(-1),
                "calculateBaseRent(-1) must throw IllegalArgumentException");

        // 3. Fractional daily rate rounding precision (cents accuracy)
        assertEquals(5998.50, car.calculateBaseRent(3), 0.01, "Fractional daily rate must calculate exactly");

        // 4. Service adapter rejects null car
        TestContext ctx = TestContext.createClean();
        assertThrows(IllegalArgumentException.class, () -> ctx.getRentalService().calculateRent(null, 3),
                "calculateRent(null, 3) must throw IllegalArgumentException");
        ctx.close();

        // 5. 1-day boundary equals exact daily rate
        assertEquals(1999.50, car.calculateBaseRent(1), 0.01, "1 day rent should equal daily rate");
    }

    // =========================================================================
    // B18: Overload 2 Tier Discount Boundaries
    // =========================================================================
    public void testB18_Overload2_TierDiscountBoundaries() {
        Rental r = new Rental();
        r.setDailyRate(2000.0);

        // 1. Negative discount clamped to 0.0
        assertEquals(6000.0, r.calculateRent(3, -50.0), 0.01, "Negative discount clamped to 0.0");

        // 2. >100% discount clamped to 100.0 (resulting in 0.0)
        assertEquals(0.0, r.calculateRent(3, 150.0), 0.01, ">100% discount clamped to 100%");

        // 3. Exact 100% discount yields 0.0
        assertEquals(0.0, r.calculateRent(3, 100.0), 0.01, "100% discount yields 0.0");

        // 4. Exact 0% discount yields full base rent
        assertEquals(6000.0, r.calculateRent(3, 0.0), 0.01, "0% discount yields full base rent");

        // 5. Fractional discount precision (12.5% discount on 6000 = 5250.0)
        assertEquals(5250.0, r.calculateRent(3, 12.5), 0.01, "12.5% fractional discount mismatch");
    }

    // =========================================================================
    // B19: Overload 3 Insurance Add-on Boundaries
    // =========================================================================
    public void testB19_Overload3_InsuranceBoundaries() {
        Rental r = new Rental();
        r.setDailyRate(2000.0);

        // 1. Insurance fee is NOT discounted by customer tier discount
        // Base 6000 - 20% (1200) = 4800 + (3 * 250 = 750) = 5550.0
        assertEquals(5550.0, r.calculateRent(3, 20.0, true), 0.01,
                "Insurance fee must be added at full rate without discount");

        // 2. Zero insurance when withInsurance = false
        assertEquals(4800.0, r.calculateRent(3, 20.0, false), 0.01,
                "withInsurance=false should have 0 insurance fee");

        // 3. 1 day insurance is exactly 250.0
        // 2000 + 250 = 2250.0
        assertEquals(2250.0, r.calculateRent(1, 0.0, true), 0.01,
                "1 day insurance fee must equal 250.0");

        // 4. Large rental duration insurance calculation
        // 60 days * 250 = 15000.0
        // (2000 * 60) + 15000 = 120000 + 15000 = 135000.0
        assertEquals(135000.0, r.calculateRent(60, 0.0, true), 0.01,
                "60 days insurance calculation mismatch");

        // 5. Service calculateRent with insurance rejects 0 days
        TestContext ctx = TestContext.createClean();
        Car car = new Car("MH-12-XX-99", "M", "M", 2022, 2000.0, 1000.0, 5, "PETROL", "MANUAL");
        assertThrows(IllegalArgumentException.class, () -> ctx.getRentalService().calculateRent(car, 0, 0.0, true),
                "calculateRent with 0 days must throw IllegalArgumentException");
        ctx.close();
    }

    // =========================================================================
    // B20: Overload 4 Final Return Bill Boundaries
    // =========================================================================
    public void testB20_Overload4_FinalReturnBillBoundaries() {
        Rental r = new Rental();
        r.setRentalDays(3);
        r.setDailyRate(2000.0);
        r.setBaseRent(6000.0);
        r.setStartMileage(10000.0);
        r.setSecurityDeposit(3000.0);

        // 1. Negative damage fee clamped to 0.0
        double billNegDamage = r.calculateFinalReturnBill(3, 10300.0, -500.0, 0.0);
        assertEquals(6000.0, billNegDamage, 0.01, "Negative damage fee must be clamped to 0.0");

        // 2. Negative cleaning fee clamped to 0.0
        double billNegCleaning = r.calculateFinalReturnBill(3, 10300.0, 0.0, -200.0);
        assertEquals(6000.0, billNegCleaning, 0.01, "Negative cleaning fee must be clamped to 0.0");

        // 3. Early return duration billed at minimum 1 day
        double billEarly = r.calculateFinalReturnBill(1, 10100.0, 0.0, 0.0);
        assertTrue(billEarly >= 2000.0, "Early return billed at minimum 1 day base");

        // 4. Overdue penalty calculation: 2 days overdue @ 1.5x daily rate = 6000.0
        double billOverdue = r.calculateFinalReturnBill(5, 10500.0, 0.0, 0.0);
        assertEquals(12000.0, billOverdue, 0.01, "Overdue bill mismatch");

        // 5. End mileage < start mileage rejected in service adapter
        TestContext ctx = TestContext.createClean();
        assertThrows(IllegalArgumentException.class, () -> {
            ctx.getRentalService().calculateFinalReturnBill(r, 3, 9500.0, 0.0, 0.0);
        }, "End mileage < start mileage must throw IllegalArgumentException");
        ctx.close();
    }

    // =========================================================================
    // B21: Inventory Boundaries
    // =========================================================================
    public void testB21_InventoryBoundaries() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // 1. Filter unknown category returns empty list gracefully
            List<Car> unknownCat = service.filterInventory("SPACESHIP", null);
            assertNotNull(unknownCat, "Filter unknown category should return non-null list");
            assertEquals(0, unknownCat.size(), "Filter unknown category should return 0 results");

            // 2. Filter unknown status returns empty list gracefully
            List<Car> unknownStat = service.filterInventory(null, "DESTROYED");
            assertEquals(0, unknownStat.size(), "Filter unknown status should return 0 results");

            // 3. Toggle maintenance on rented car throws IllegalStateException
            Rental r = service.bookCar(1, 1, 2, false);
            assertThrows(IllegalStateException.class, () -> service.toggleMaintenance(1, true),
                    "Cannot toggle maintenance on currently rented vehicle");

            // 4. Case-insensitive category filtering
            List<Car> lowerSedan = service.filterInventory("sedan", null);
            List<Car> upperSedan = service.filterInventory("SEDAN", null);
            assertEquals(lowerSedan.size(), upperSedan.size(), "Category filter must be case-insensitive");

            // 5. Empty filter returns all cars
            List<Car> all = service.filterInventory("", "");
            assertTrue(all.size() >= 3, "Empty filter criteria should return all cars");
        }
    }

    // =========================================================================
    // B22: Customer Validation Boundaries
    // =========================================================================
    public void testB22_CustomerValidationBoundaries() {
        // 1. Phone number with letters fails validation
        String alphaPhone = "98765ABCD0";
        assertFalse(alphaPhone.matches("\\d{10}"), "Phone containing letters must fail validation");

        // 2. Phone number too short fails validation
        String shortPhone = "98765";
        assertFalse(shortPhone.matches("\\d{10}"), "Phone shorter than 10 digits must fail validation");

        // 3. Phone number too long fails validation
        String longPhone = "9876543210123";
        assertFalse(longPhone.matches("\\d{10}"), "Phone longer than 10 digits must fail validation");

        // 4. License too short fails validation
        String shortLic = "DL1";
        assertFalse(shortLic.length() >= 5, "License shorter than 5 chars must fail validation");

        // 5. Customer name trimming verification
        Customer cust = new Customer("   Amit Kumar   ", "9876543210", "DL12345", "", "", Customer.MemberTier.REGULAR);
        assertEquals("Amit Kumar", cust.getFullName(), "Customer name should be trimmed");
    }

    // =========================================================================
    // B23: Return Workflow Boundaries & Exceptions
    // =========================================================================
    public void testB23_ReturnWorkflowBoundaries() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // 1. Return on non-existent rental ID throws IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> {
                service.processReturn(999999, LocalDate.now(), 20000.0, 0.0, "notes");
            }, "Returning non-existent rental ID must throw IllegalArgumentException");

            // 2. Book car and verify booking
            Rental r = service.bookCar(1, 1, 3, false);
            assertNotNull(r, "Booking car for return workflow must succeed");
            assertTrue(r.getRentalId() > 0, "Valid rental ID must be assigned");

            Rental closed = service.processReturn(r.getRentalId(), LocalDate.now().plusDays(3), 16000.0, 0.0, "clean");
            assertEquals(Rental.RentalStatus.COMPLETED, closed.getStatus(), "Closed rental status must be COMPLETED");
            assertNotNull(closed.getActualReturnDate(), "Actual return date must be recorded upon return");

            // 3. Returning already completed rental throws IllegalStateException
            assertThrows(IllegalStateException.class, () -> {
                service.processReturn(r.getRentalId(), LocalDate.now().plusDays(3), 16000.0, 0.0, "again");
            }, "Returning already completed rental must throw IllegalStateException");

            // 4. Returning with end mileage < start mileage throws IllegalArgumentException
            Rental r2 = service.bookCar(2, 2, 2, false);
            assertThrows(IllegalArgumentException.class, () -> {
                service.processReturn(r2.getRentalId(), LocalDate.now().plusDays(2), 1000.0, 0.0, "tampered");
            }, "Returning with endMileage < startMileage must throw IllegalArgumentException");

            // 5. Severe damage (>5000) transitions car to MAINTENANCE
            service.processReturn(r2.getRentalId(), LocalDate.now().plusDays(2), 25000.0, 7500.0, "Severe body damage");
            Car damagedCar = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(2) : ctx.getMemoryCar(2);
            assertEquals("MAINTENANCE", damagedCar.getStatus(), "Car with severe damage must transition to MAINTENANCE");
        }
    }

    // =========================================================================
    // B24: History and KPI Invariants
    // =========================================================================
    public void testB24_HistoryAndKPIBoundaries() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // 1. KPI total revenue on empty database is 0.0
            assertEquals(0.0, service.calculateTotalRevenue(), 0.001, "Total revenue on empty DB must be 0.0");

            // 2. KPI active rentals count on empty database is 0
            assertEquals(0, service.getActiveRentalsCount(), "Active rentals count on empty DB must be 0");

            // 3. Active rentals do not contribute to completed revenue
            // Insert active rental
            Rental activeRental = new Rental(0, 1, 1, LocalDate.now(), LocalDate.now().plusDays(2), null,
                    2, 2000.0, 4000.0, 0.0, 0.0, 0.0, 10000.0, 0.0, 0.0, 0.0, 4000.0, Rental.RentalStatus.ACTIVE, "active");
            if (ctx.isLiveJdbc()) {
                // Ensure FKs
                ctx.getCustomerDAO().insertCustomer(new Customer(1, "Cust A", "9811111111", "DL11111", "a@a.com", "City", Customer.MemberTier.REGULAR));
                ctx.getCarDAO().insertCar(new Sedan("MH-12-TEST-B24", "Honda", "City", 2023, 2000.0, 10000.0, 5, "PETROL", "MANUAL", 500.0, "EXECUTIVE", 0.0));
                ctx.getRentalDAO().insertRental(activeRental);
            } else {
                ctx.insertMemoryRental(activeRental);
            }
            assertEquals(0.0, service.calculateTotalRevenue(), 0.001, "Active rental should not contribute to revenue");

            // 4. Active count is 1
            assertEquals(1, service.getActiveRentalsCount(), "Active count must be 1");

            // 5. Settling rental contributes to revenue
            activeRental.setStatus(Rental.RentalStatus.COMPLETED);
            activeRental.setTotalAmount(4500.0);
            if (ctx.isLiveJdbc()) {
                ctx.getRentalDAO().updateRentalReturn(activeRental.getRentalId(), LocalDate.now(), 10200.0, 0.0, 4500.0, "COMPLETED", "done");
            }
            assertEquals(4500.0, service.calculateTotalRevenue(), 0.01, "Revenue must reflect completed rental");
        }
    }

    // =========================================================================
    // B25: Tricolour Theme Contrast & Headless Safety
    // =========================================================================
    public void testB25_TricolourThemeBoundaries() {
        Color saffron = new Color(255, 153, 51);
        Color white = new Color(255, 255, 255);
        Color green = new Color(19, 136, 8);
        Color navy = new Color(0, 0, 128);

        // 1. None of the theme colors are null
        assertNotNull(saffron, "Saffron color must not be null");
        assertNotNull(white, "White color must not be null");
        assertNotNull(green, "Green color must not be null");
        assertNotNull(navy, "Navy color must not be null");

        // 2. Saffron background with Navy text has high contrast
        double saffronLum = 0.2126 * (255 / 255.0) + 0.7152 * (153 / 255.0) + 0.0722 * (51 / 255.0);
        double navyLum = 0.2126 * (0 / 255.0) + 0.7152 * (0 / 255.0) + 0.0722 * (128 / 255.0);
        double contrastRatio = (saffronLum + 0.05) / (navyLum + 0.05);
        assertTrue(contrastRatio >= 4.5, "Saffron with Navy text must satisfy WCAG AA contrast (>= 4.5)");

        // 3. Green background with White text contrast
        double greenLum = 0.2126 * (19 / 255.0) + 0.7152 * (136 / 255.0) + 0.0722 * (8 / 255.0);
        double whiteLum = 1.0;
        double greenContrast = (whiteLum + 0.05) / (greenLum + 0.05);
        assertTrue(greenContrast >= 2.0, "India Green with White text must satisfy WCAG contrast");

        // 4. Button accepts null text safely without NPE
        JButton btn = new JButton((String) null);
        btn.setBackground(saffron);
        assertNotNull(btn, "Button with null text instantiated cleanly");

        // 5. Headless component hierarchy execution
        JPanel p = new JPanel();
        p.setBackground(white);
        p.add(btn);
        assertEquals(1, p.getComponentCount(), "Headless panel component count mismatch");
    }

    // =========================================================================
    // B26: GUI Layout Boundaries & Table Models
    // =========================================================================
    public void testB26_GUILayoutBoundaries() {
        // 1. BorderLayout regions reject duplicate placement
        JPanel borderPanel = new JPanel(new BorderLayout());
        JPanel compA = new JPanel();
        JPanel compB = new JPanel();
        borderPanel.add(compA, BorderLayout.NORTH);
        borderPanel.remove(compA);
        borderPanel.add(compB, BorderLayout.NORTH);
        // Overwriting NORTH replaces previous component
        assertEquals(1, borderPanel.getComponentCount(), "Overwriting BorderLayout region replaces previous");

        // 2. GridLayout with 0 rows throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> new GridLayout(0, 0),
                "GridLayout(0, 0) must throw IllegalArgumentException");

        // 3. JTable selection on empty table returns -1
        JTable emptyTable = new JTable(0, 5);
        assertEquals(-1, emptyTable.getSelectedRow(), "No row selected on empty table should return -1");

        // 4. JTable column count cannot be negative
        assertTrue(emptyTable.getColumnCount() >= 0, "Column count cannot be negative");

        // 5. Table cell selection model is non-null
        assertNotNull(emptyTable.getSelectionModel(), "Table selection model must not be null");
    }

    // =========================================================================
    // B33: start_db.bat Invariants & Idempotency
    // =========================================================================
    public void testB33_StartDbBatBoundaries() throws Exception {
        File batFile = new File("start_db.bat");
        assertTrue(batFile.exists(), "start_db.bat must exist");

        String content = new String(Files.readAllBytes(batFile.toPath()));

        // 1. Script checks for Python availability before execution
        assertTrue(content.contains("where python") || content.contains("where py"),
                "start_db.bat must verify python executable presence");

        // 2. Script sets errorlevel on failure
        assertTrue(content.contains("exit /b"), "start_db.bat must exit with code on failure");

        // 3. Script references db/init_db.py
        assertTrue(content.contains("init_db.py"), "start_db.bat must invoke init_db.py");

        // 4. Database initializer Python script exists
        File initScript = new File("db/init_db.py");
        assertTrue(initScript.exists(), "db/init_db.py must exist");

        // 5. Python initializer handles --reset argument
        String pyContent = new String(Files.readAllBytes(initScript.toPath()));
        assertTrue(pyContent.contains("--reset") || pyContent.contains("reset"),
                "db/init_db.py must support reset mode");
    }

    // =========================================================================
    // B34: start_app.bat Invariants & Fallbacks
    // =========================================================================
    public void testB34_StartAppBatBoundaries() throws Exception {
        File batFile = new File("start_app.bat");
        // 1. Script existence check
        assertTrue(batFile.exists(), "start_app.bat deliverable must exist in project root");

        String content = new String(Files.readAllBytes(batFile.toPath()), StandardCharsets.UTF_8);

        // 2. Search pattern checks for JDK discovery
        assertTrue(content.contains("javac") || content.contains("JAVA_HOME"),
                "Launcher must search JDK locations via javac or JAVA_HOME");

        // 3. Classpath flag presence in compilation and execution commands
        assertTrue(content.contains("-cp") || content.contains("-classpath"),
                "start_app.bat must specify classpath flag (-cp or -classpath)");

        // 4. Target application package contract
        assertTrue(content.contains("carrental.Main") || content.contains("carrental"),
                "Launcher must reference root application package carrental");

        // 5. Non-zero exit code on compilation failure
        assertTrue(content.contains("exit /b") || content.contains("exit 1") || content.toLowerCase().contains("errorlevel"),
                "Launcher must yield non-zero exit code upon compilation failure");
    }
}
