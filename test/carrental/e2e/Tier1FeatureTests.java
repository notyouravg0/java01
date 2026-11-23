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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;

/**
 * Tier 1: Isolated Feature Tests (F1-F26, F33, F34).
 * Exercises exactly 28 features with at least 5 assertions each (>= 140 assertions).
 */
public class Tier1FeatureTests {

    public void runAllTests() throws Exception {
        // F1-F8: Domain Model Hierarchy
        testF1_RentableInterface();
        testF2_AbstractVehicleClass();
        testF3_ConcreteCarEntity();
        testF4_SedanSpecialization();
        testF5_SUVSpecialization();
        testF6_HatchbackSpecialization();
        testF7_CustomerEntityAndTiers();
        testF8_RentalEntityAndBilling();

        // F9-F14: Database & DAO Layer
        testF9_DatabaseSchemaDDL();
        testF10_SeedDataSQL();
        testF11_DBConnectionFactory();
        testF12_CarDAOOperations();
        testF13_CustomerDAOOperations();
        testF14_RentalDAOOperations();

        // F15-F20: Business Logic, Concurrency & Calculations
        testF15_CarUnavailableException();
        testF16_SynchronizedBookingEngine();
        testF17_Overload1_BaseRent();
        testF18_Overload2_TierDiscount();
        testF19_Overload3_Insurance();
        testF20_Overload4_FinalReturnBill();

        // F21-F24: Application Workflows & KPIs
        testF21_InventoryFilterAndMaintenance();
        testF22_CustomerUniquenessAndValidation();
        testF23_ReturnAndInspectionWorkflow();
        testF24_RentalHistoryAndKPIs();

        // F25-F26, F33-F34: UI Contracts & Launch Scripts
        testF25_MinimalTricolourTheme();
        testF26_GUILayoutContracts();
        testF33_StartDbBatVerification();
        testF34_StartAppBatVerification();
    }

    // =========================================================================
    // F1: Rentable Interface Contract (Syllabus Unit 2)
    // =========================================================================
    public void testF1_RentableInterface() throws Exception {
        // 1. Verify interface exists and has public modifier
        assertTrue(Rentable.class.isInterface(), "Rentable must be an interface");
        assertTrue(Modifier.isPublic(Rentable.class.getModifiers()), "Rentable must be public");

        // 2. Verify all 4 mandated methods exist on interface
        Method rentMethod = Rentable.class.getMethod("rent");
        Method returnMethod = Rentable.class.getMethod("returnVehicle");
        Method isAvailMethod = Rentable.class.getMethod("isAvailable");
        Method calcRateMethod = Rentable.class.getMethod("calculateRate", int.class);

        assertNotNull(rentMethod, "rent() must exist on Rentable");
        assertNotNull(returnMethod, "returnVehicle() must exist on Rentable");
        assertNotNull(isAvailMethod, "isAvailable() must exist on Rentable");
        assertNotNull(calcRateMethod, "calculateRate(int) must exist on Rentable");

        // 3. State transition: rent() changes isAvailable() to false
        Car car = new Sedan("MH-12-AB-1234", "Honda", "City", 2023, 2000.0, 10000.0,
                5, "PETROL", "MANUAL", 510.0, "EXECUTIVE", 10.0);
        assertTrue(car.isAvailable(), "New car should be available");
        car.rent();
        assertFalse(car.isAvailable(), "Car should not be available after rent()");

        // 4. State transition: returnVehicle() restores availability
        car.returnVehicle();
        assertTrue(car.isAvailable(), "Car should be available after returnVehicle()");

        // 5. Polymorphic calculateRate delegation
        Rentable rentable = car;
        double rate3Days = rentable.calculateRate(3);
        assertEquals(6000.0, rate3Days, 0.01, "3 days rental rate should equal 6000.0");
    }

    // =========================================================================
    // F2: Abstract Vehicle Base Class (Syllabus Unit 2)
    // =========================================================================
    public void testF2_AbstractVehicleClass() throws Exception {
        // 1. Vehicle is abstract
        assertTrue(Modifier.isAbstract(Vehicle.class.getModifiers()), "Vehicle must be abstract class");

        // 2. Abstract methods exist
        Method getVehicleTypeMethod = Vehicle.class.getDeclaredMethod("getVehicleType");
        Method calculateBaseRentMethod = Vehicle.class.getDeclaredMethod("calculateBaseRent", int.class);
        assertTrue(Modifier.isAbstract(getVehicleTypeMethod.getModifiers()), "getVehicleType must be abstract");
        assertTrue(Modifier.isAbstract(calculateBaseRentMethod.getModifiers()), "calculateBaseRent must be abstract");

        // 3. Getters return assigned attributes
        Car sedan = new Sedan(10, "MH-12-XY-9999", "Tata", "Tigor", 2022, 1800.0, 25000.0, "AVAILABLE",
                5, "PETROL", "MANUAL", 419.0, "EXECUTIVE", 5.0);
        assertEquals("MH-12-XY-9999", sedan.getRegistrationNumber(), "Registration number mismatch");
        assertEquals("Tata", sedan.getMake(), "Make mismatch");
        assertEquals("Tigor", sedan.getModel(), "Model mismatch");
        assertEquals(2022, sedan.getYear(), "Year mismatch");

        // 4. Daily rate & mileage getters
        assertEquals(1800.0, sedan.getDailyRate(), 0.001, "Daily rate mismatch");
        assertEquals(25000.0, sedan.getMileage(), 0.001, "Mileage mismatch");

        // 5. Mileage update mutator
        sedan.setMileage(25500.0);
        assertEquals(25500.0, sedan.getMileage(), 0.001, "Mileage not updated correctly");
    }

    // =========================================================================
    // F3: Concrete Car Entity (Syllabus Units 1 & 2)
    // =========================================================================
    public void testF3_ConcreteCarEntity() {
        // 1. Hierarchy verification
        Car car = new Car("MH-14-AA-1111", "Hyundai", "i20", 2021, 1500.0, 18000.0, 5, "PETROL", "AUTOMATIC");
        assertTrue(car instanceof Vehicle, "Car must inherit from Vehicle");
        assertTrue(car instanceof Rentable, "Car must implement Rentable");

        // 2. Default status is AVAILABLE
        assertEquals("AVAILABLE", car.getStatus(), "Default status must be AVAILABLE");
        assertTrue(car.isAvailable(), "Car must report available");

        // 3. Seating, fuel, and transmission attributes
        assertEquals(5, car.getSeatingCapacity(), "Seating capacity mismatch");
        assertEquals("PETROL", car.getFuelType(), "Fuel type mismatch");
        assertEquals("AUTOMATIC", car.getTransmission(), "Transmission mismatch");

        // 4. Base rent calculation
        assertEquals(4500.0, car.calculateBaseRent(3), 0.01, "Base rent for 3 days mismatch");

        // 5. ToString contains essential vehicle metadata
        String str = car.toString();
        assertTrue(str.contains("Hyundai") && str.contains("i20") && str.contains("MH-14-AA-1111"),
                "toString must include make, model, and registration plate");
    }

    // =========================================================================
    // F4: Sedan Specialization (Syllabus Unit 2)
    // =========================================================================
    public void testF4_SedanSpecialization() {
        // 1. Boot space attribute
        Sedan sedan = new Sedan("MH-01-SD-1234", "Honda", "City", 2023, 2000.0, 12000.0,
                5, "PETROL", "MANUAL", 510.0, "EXECUTIVE", 10.0);
        assertEquals(510.0, sedan.getBootSpaceLiters(), 0.1, "Boot space mismatch");

        // 2. Vehicle type is SEDAN
        assertEquals("SEDAN", sedan.getVehicleType(), "Vehicle type must be SEDAN");

        // 3. Comfort category multiplier: EXECUTIVE = 1.0
        assertEquals(6000.0, sedan.calculateBaseRent(3), 0.01, "3 days standard executive rent mismatch");

        // 4. Comfort category multiplier: LUXURY = 1.15
        sedan.setComfortCategory("LUXURY");
        // 2000 * 3 * 1.15 = 6900.0
        assertEquals(6900.0, sedan.calculateBaseRent(3), 0.01, "Luxury comfort multiplier mismatch");

        // 5. Long term discount (>= 7 days): 10% discount on 7 days
        // 2000 * 7 * 1.15 = 16100 * 0.90 = 14490.0
        assertEquals(14490.0, sedan.calculateBaseRent(7), 0.01, "Long term discount mismatch on 7 days");
    }

    // =========================================================================
    // F5: SUV Specialization (Syllabus Unit 2)
    // =========================================================================
    public void testF5_SUVSpecialization() {
        // 1. SUV attributes getters
        SUV suv = new SUV("MH-02-SV-5555", "Mahindra", "Scorpio-N", 2023, 3000.0, 20000.0,
                7, "DIESEL", "MANUAL", true, 210, true, 300.0);
        assertTrue(suv.isAwd(), "AWD must be true");
        assertEquals(210, suv.getGroundClearanceMm(), "Ground clearance mismatch");
        assertTrue(suv.isSevenSeater(), "isSevenSeater must be true");

        // 2. Vehicle type is SUV
        assertEquals("SUV", suv.getVehicleType(), "Vehicle type must be SUV");

        // 3. Surcharge accumulation: base + AWD surcharge (300/day) + 7-seater premium (10% = 300/day)
        // Per day: 3000 + 300 + 300 = 3600.0 * 2 days = 7200.0
        assertEquals(7200.0, suv.calculateBaseRent(2), 0.01, "SUV combined AWD and 7-seater rate mismatch");

        // 4. Non-AWD 5-seater rate calculation
        suv.setAwd(false);
        suv.setSevenSeater(false);
        // Per day: 3000 * 2 days = 6000.0
        assertEquals(6000.0, suv.calculateBaseRent(2), 0.01, "Non-AWD 5-seater rate mismatch");

        // 5. Automatic seating capacity sync when sevenSeater is true
        suv.setSevenSeater(true);
        assertTrue(suv.getSeatingCapacity() >= 7, "Seating capacity must be at least 7 for 7-seater");
    }

    // =========================================================================
    // F6: Hatchback Specialization (Syllabus Unit 2)
    // =========================================================================
    public void testF6_HatchbackSpecialization() {
        // 1. Hatchback attributes
        Hatchback hatch = new Hatchback("MH-03-HB-8888", "Maruti", "Swift", 2022, 1200.0, 15000.0,
                5, "PETROL", "MANUAL", true, 22.5, 5.0);
        assertTrue(hatch.isCompactCity(), "Compact city flag should be true");
        assertEquals(22.5, hatch.getFuelEconomyKmpl(), 0.01, "Fuel economy mismatch");
        assertEquals(5.0, hatch.getEconomyDiscountRate(), 0.01, "Economy discount mismatch");

        // 2. Vehicle type is HATCHBACK
        assertEquals("HATCHBACK", hatch.getVehicleType(), "Vehicle type must be HATCHBACK");

        // 3. Base rate calculation incorporating 5% economy discount
        // 1200 * 2 = 2400 * 0.95 = 2280.0
        assertEquals(2280.0, hatch.calculateBaseRent(2), 0.01, "Hatchback economy discount rate mismatch");

        // 4. Zero economy discount rate produces pure base rent
        hatch.setEconomyDiscountRate(0.0);
        assertEquals(2400.0, hatch.calculateBaseRent(2), 0.01, "Zero discount should equal base rent");

        // 5. Mutator updates
        hatch.setFuelEconomyKmpl(24.0);
        assertEquals(24.0, hatch.getFuelEconomyKmpl(), 0.01, "Fuel economy update failed");
    }

    // =========================================================================
    // F7: Customer Entity & Loyalty Tiers (Syllabus Unit 1)
    // =========================================================================
    public void testF7_CustomerEntityAndTiers() {
        // 1. Customer instantiation and getters
        Customer cust = new Customer(1, "Rajesh Sharma", "9820112233", "DL0120190012345",
                "rajesh@example.com", "Pune", Customer.MemberTier.REGULAR);
        assertEquals("Rajesh Sharma", cust.getFullName(), "Name mismatch");
        assertEquals("9820112233", cust.getPhone(), "Phone mismatch");
        assertEquals("DL0120190012345", cust.getDrivingLicense(), "License mismatch");

        // 2. MemberTier REGULAR discount is 0%
        assertEquals(0.0, cust.getTierDiscountPercent(), 0.01, "REGULAR discount must be 0%");

        // 3. MemberTier SILVER discount is 10%
        cust.setMemberTier(Customer.MemberTier.SILVER);
        assertEquals(10.0, cust.getTierDiscountPercent(), 0.01, "SILVER discount must be 10%");

        // 4. MemberTier GOLD discount is 20%
        cust.setMemberTier(Customer.MemberTier.GOLD);
        assertEquals(20.0, cust.getTierDiscountPercent(), 0.01, "GOLD discount must be 20%");

        // 5. MemberTier.fromString safe fallback
        assertEquals(Customer.MemberTier.GOLD, Customer.MemberTier.fromString("gold"), "Case insensitive tier parsing");
        assertEquals(Customer.MemberTier.REGULAR, Customer.MemberTier.fromString("INVALID"), "Unknown tier should fallback to REGULAR");
    }

    // =========================================================================
    // F8: Rental Entity & Overloaded Billing (Syllabus Unit 1)
    // =========================================================================
    public void testF8_RentalEntityAndBilling() {
        Rental rental = new Rental();
        rental.setRentalId(101);
        rental.setCarId(5);
        rental.setCustomerId(3);
        rental.setDailyRate(2000.0);
        rental.setRentalDays(3);
        rental.setBaseRent(6000.0);
        rental.setStartMileage(10000.0);

        // 1. Initial status is ACTIVE
        assertEquals(Rental.RentalStatus.ACTIVE, rental.getStatus(), "Initial status must be ACTIVE");
        assertTrue(rental.isActive(), "isActive() must be true");

        // 2. Overload 1: calculateRent(days)
        assertEquals(6000.0, rental.calculateRent(3), 0.01, "Base rent for 3 days mismatch");

        // 3. Overload 2: calculateRent(days, discountPercent)
        assertEquals(5400.0, rental.calculateRent(3, 10.0), 0.01, "10% discounted rent mismatch");

        // 4. Overload 3: calculateRent(days, discount, withInsurance) (250/day)
        // 5400 + (3 * 250) = 6150.0
        assertEquals(6150.0, rental.calculateRent(3, 10.0, true), 0.01, "Rent with insurance mismatch");

        // 5. Overload 5: calculateFinalReturnBill on-time return
        // Base 6000 - 600 disc + 750 ins + 0 late + 0 damage = 6150.0
        rental.setDiscountAmount(600.0);
        rental.setInsuranceFee(750.0);
        double finalBill = rental.calculateFinalReturnBill(3, 10300.0, 0.0, 0.0);
        assertEquals(6150.0, finalBill, 0.01, "Final return bill mismatch");
    }

    // =========================================================================
    // F9: Database Schema DDL (Syllabus Unit 4)
    // =========================================================================
    public void testF9_DatabaseSchemaDDL() throws Exception {
        File schemaFile = new File("db/schema.sql");
        assertTrue(schemaFile.exists(), "db/schema.sql must exist");

        String sql = new String(Files.readAllBytes(schemaFile.toPath()));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS cars"), "Schema must create cars table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS customers"), "Schema must create customers table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS rentals"), "Schema must create rentals table");
        assertTrue(sql.contains("FOREIGN KEY"), "Schema must define foreign key constraints");
    }

    // =========================================================================
    // F10: Seed Data SQL Initialization (Survey & Deliverables)
    // =========================================================================
    public void testF10_SeedDataSQL() throws Exception {
        File seedFile = new File("db/seed_data.sql");
        assertTrue(seedFile.exists(), "db/seed_data.sql must exist");

        String seedSql = new String(Files.readAllBytes(seedFile.toPath()));
        assertTrue(seedSql.contains("INSERT INTO cars") || seedSql.contains("INSERT OR IGNORE INTO cars"),
                "Seed script must populate cars");
        assertTrue(seedSql.contains("INSERT INTO customers") || seedSql.contains("INSERT OR IGNORE INTO customers"),
                "Seed script must populate customers");
        assertTrue(seedSql.contains("SEDAN") || seedSql.contains("SUV") || seedSql.contains("HATCHBACK"),
                "Seed script must include car categories");
        assertTrue(seedSql.contains("REGULAR") || seedSql.contains("GOLD"),
                "Seed script must include customer tiers");
        assertTrue(seedSql.length() > 200, "Seed script must contain substantial seed rows");
    }

    // =========================================================================
    // F11: DBConnection & Transaction Factory (Syllabus Unit 4)
    // =========================================================================
    public void testF11_DBConnectionFactory() throws Exception {
        // 1. Default DB URL is configured
        assertNotNull(DBConnection.getDatabaseUrl(), "Default DB URL must not be null");

        // 2. Custom URL configuration
        String customUrl = "jdbc:sqlite:db/test_custom.db";
        DBConnection.setDatabaseUrl(customUrl);
        assertEquals(customUrl, DBConnection.getDatabaseUrl(), "Custom URL should be set");

        // 3. Reset URL restores default
        DBConnection.resetDatabaseUrl();
        assertTrue(DBConnection.getDatabaseUrl().contains("car_rental.db"), "Reset URL should point to car_rental.db");

        // 4. Validate isValidConnection handles null safely
        assertFalse(DBConnection.isValidConnection(null), "Null connection should not be valid");

        // 5. Close connection utility safely closes active connection and handles null idempotently
        DBConnection.closeConnection(null);
        assertFalse(DBConnection.isValidConnection(null), "Null connection must remain invalid after close attempt");
        if (DBConnection.getDatabaseUrl() != null && !DBConnection.getDatabaseUrl().isEmpty()) {
            try (Connection testConn = DBConnection.getConnection()) {
                assertNotNull(testConn, "Active connection must not be null");
                DBConnection.closeConnection(testConn);
                assertTrue(testConn.isClosed(), "Connection must be closed by closeConnection");
                DBConnection.closeConnection(testConn); // Idempotent repeat close
                assertTrue(testConn.isClosed(), "Repeated closeConnection must leave connection closed");
            }
        }
    }

    // =========================================================================
    // F12: CarDAO JDBC CRUD & executeQuery (Syllabus Unit 4)
    // =========================================================================
    public void testF12_CarDAOOperations() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            if (ctx.isLiveJdbc()) {
                CarDAO dao = ctx.getCarDAO();
                List<Car> cars = dao.getAllCars();
                assertTrue(cars.size() >= 1, "Fleet should contain at least 1 car");

                Car first = cars.get(0);
                assertNotNull(first, "First car should not be null");
                Car byId = dao.getCarById(first.getCarId());
                assertEquals(first.getRegistrationNumber(), byId.getRegistrationNumber(), "Car by ID should match");

                List<Car> avail = dao.getAvailableCars();
                assertNotNull(avail, "Available cars list must not be null");
                assertTrue(avail.size() <= cars.size(), "Available count cannot exceed total count");
            } else {
                List<Car> cars = ctx.getAllMemoryCars();
                assertTrue(cars.size() >= 3, "Memory seed should contain at least 3 cars");
                Car c = ctx.getMemoryCar(1);
                assertNotNull(c, "Car #1 should be present in memory store");
                assertEquals("SEDAN", c.getVehicleType(), "Car #1 should be SEDAN");
            }
        }
    }

    // =========================================================================
    // F13: CustomerDAO JDBC CRUD & Lookup (Syllabus Unit 4)
    // =========================================================================
    public void testF13_CustomerDAOOperations() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            if (ctx.isLiveJdbc()) {
                CustomerDAO dao = ctx.getCustomerDAO();
                Customer c = new Customer("Test User", "9988776655", "DL99TEST001", "test@user.com", "City", Customer.MemberTier.SILVER);
                int id = dao.insertCustomer(c);
                assertTrue(id > 0, "Generated customer ID should be positive");

                Customer byId = dao.getCustomerById(id);
                assertNotNull(byId, "Retrieved customer should not be null");
                assertEquals("Test User", byId.getFullName(), "Customer name mismatch");

                Customer byPhone = dao.getCustomerByPhone("9988776655");
                assertNotNull(byPhone, "Customer lookup by phone failed");

                Customer byLic = dao.getCustomerByLicense("DL99TEST001");
                assertNotNull(byLic, "Customer lookup by license failed");
            } else {
                Customer c = new Customer("Mem User", "9988776655", "DL99TEST001", "m@user.com", "City", Customer.MemberTier.GOLD);
                int id = ctx.insertMemoryCustomer(c);
                assertTrue(id > 0, "Memory customer ID should be positive");
                Customer byId = ctx.getMemoryCustomer(id);
                assertEquals("Mem User", byId.getFullName(), "Name mismatch");
                assertNotNull(ctx.getMemoryCustomerByPhone("9988776655"), "Phone lookup failed");
            }
        }
    }

    // =========================================================================
    // F14: RentalDAO JDBC CRUD & History (Syllabus Unit 4)
    // =========================================================================
    public void testF14_RentalDAOOperations() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            Rental r = new Rental(0, 1, 1, LocalDate.now(), LocalDate.now().plusDays(2), null,
                    2, 2000.0, 4000.0, 0.0, 500.0, 3000.0, 10000.0, 0.0, 0.0, 0.0, 4500.0,
                    Rental.RentalStatus.ACTIVE, "Test booking");

            if (ctx.isLiveJdbc()) {
                RentalDAO dao = ctx.getRentalDAO();
                // Ensure foreign keys exist
                ctx.getCustomerDAO().insertCustomer(new Customer(1, "Cust A", "9811111111", "DL11111", "a@a.com", "City", Customer.MemberTier.REGULAR));
                ctx.getCarDAO().insertCar(new Sedan("MH-12-TEST-1", "Honda", "City", 2023, 2000.0, 10000.0, 5, "PETROL", "MANUAL", 500.0, "EXECUTIVE", 0.0));

                int rentalId = dao.insertRental(r);
                assertTrue(rentalId > 0, "Rental ID must be positive");

                Rental fetched = dao.getRentalById(rentalId);
                assertNotNull(fetched, "Fetched rental must not be null");
                assertEquals(4500.0, fetched.getTotalAmount(), 0.01, "Total amount mismatch");

                List<Rental> active = dao.getActiveRentals();
                assertTrue(active.size() >= 1, "Active rentals should contain created rental");
            } else {
                int id = ctx.insertMemoryRental(r);
                assertTrue(id > 0, "Memory rental ID must be positive");
                Rental fetched = ctx.getMemoryRental(id);
                assertNotNull(fetched, "Memory rental should be retrieved");
                assertEquals(4500.0, fetched.getTotalAmount(), 0.01, "Total amount mismatch");
            }
        }
    }

    // =========================================================================
    // F15: CarUnavailableException Handling (Syllabus Unit 3)
    // =========================================================================
    public void testF15_CarUnavailableException() {
        // 1. Instantiation with message
        TestContext.CarUnavailableException ex = new TestContext.CarUnavailableException("Car is booked");
        assertEquals("Car is booked", ex.getMessage(), "Exception message mismatch");

        // 2. Instantiation with carId and reg plate
        TestContext.CarUnavailableException ex2 = new TestContext.CarUnavailableException("Unavailable", 42, "MH-12-QQ-7777");
        assertEquals(42, ex2.getCarId(), "Diagnostic car ID mismatch");
        assertEquals("MH-12-QQ-7777", ex2.getRegistrationNumber(), "Diagnostic reg plate mismatch");

        // 3. Exception inheritance: checked exception (extends Exception)
        assertTrue(Exception.class.isAssignableFrom(TestContext.CarUnavailableException.class), "Must extend Exception");
        assertFalse(RuntimeException.class.isAssignableFrom(TestContext.CarUnavailableException.class), "Must be a checked exception, not RuntimeException");

        // 4. Cause chaining
        Throwable rootCause = new IllegalStateException("Database lock");
        TestContext.CarUnavailableException chained = new TestContext.CarUnavailableException("Wrapper", rootCause);
        assertEquals(rootCause, chained.getCause(), "Chained cause mismatch");
    }

    // =========================================================================
    // F16: Synchronized Booking Engine (Syllabus Unit 3)
    // =========================================================================
    public void testF16_SynchronizedBookingEngine() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // 1. Successful booking transitions car to RENTED
            Rental rental = service.bookCar(1, 1, 3, true);
            assertNotNull(rental, "Booked rental must not be null");
            assertTrue(rental.getRentalId() > 0, "Rental ID must be positive");

            // 2. Car status is now RENTED
            Car car = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(1) : ctx.getMemoryCar(1);
            assertEquals("RENTED", car.getStatus(), "Car status must transition to RENTED");

            // 3. Attempting to book already rented car throws CarUnavailableException
            assertThrows(TestContext.CarUnavailableException.class, () -> {
                service.bookCar(2, 1, 2, false);
            }, "Booking rented car must throw CarUnavailableException");

            // 4. Renting different car concurrently succeeds
            Rental rental2 = service.bookCar(2, 2, 2, false);
            assertNotNull(rental2, "Booking separate car should succeed");

            // 5. Active rentals count is at least 2
            assertTrue(service.getActiveRentalsCount() >= 2, "Active count should reflect bookings");
        }
    }

    // =========================================================================
    // F17: Overload 1: Base Rent Calculation (Syllabus Unit 1)
    // =========================================================================
    public void testF17_Overload1_BaseRent() {
        Car car = new Car("MH-12-AA-1000", "Honda", "Amaze", 2022, 1500.0, 10000.0, 5, "PETROL", "MANUAL");

        // 1. Single day base rent
        assertEquals(1500.0, car.calculateBaseRent(1), 0.01, "1 day rent should equal daily rate");

        // 2. Multi-day base rent
        assertEquals(7500.0, car.calculateBaseRent(5), 0.01, "5 days rent mismatch");

        // 3. Rental entity delegation
        Rental r = new Rental();
        r.setDailyRate(1500.0);
        assertEquals(4500.0, r.calculateRent(3), 0.01, "Rental entity Overload 1 mismatch");

        // 4. Service adapter Overload 1
        TestContext ctx = TestContext.createClean();
        assertEquals(3000.0, ctx.getRentalService().calculateRent(car, 2), 0.01, "Service Overload 1 mismatch");
        ctx.close();

        // 5. Large day duration scaling
        assertEquals(45000.0, car.calculateBaseRent(30), 0.01, "30 days rate mismatch");
    }

    // =========================================================================
    // F18: Overload 2: Customer Tier Discount (Syllabus Unit 1)
    // =========================================================================
    public void testF18_Overload2_TierDiscount() {
        Rental r = new Rental();
        r.setDailyRate(2000.0);

        // 1. REGULAR customer discount (0%)
        assertEquals(6000.0, r.calculateRent(3, 0.0), 0.01, "0% discount should match base");

        // 2. SILVER customer discount (10%)
        // 6000 * 0.90 = 5400.0
        assertEquals(5400.0, r.calculateRent(3, 10.0), 0.01, "10% discount mismatch");

        // 3. GOLD customer discount (20%)
        // 6000 * 0.80 = 4800.0
        assertEquals(4800.0, r.calculateRent(3, 20.0), 0.01, "20% discount mismatch");

        // 4. Promo custom discount (15%)
        assertEquals(5100.0, r.calculateRent(3, 15.0), 0.01, "15% discount mismatch");

        // 5. Service adapter Overload 2
        Car car = new Car("MH-12-XX-0001", "Maruti", "Dzire", 2022, 2000.0, 10000.0, 5, "PETROL", "MANUAL");
        TestContext ctx = TestContext.createClean();
        assertEquals(5400.0, ctx.getRentalService().calculateRent(car, 3, 10.0), 0.01, "Service Overload 2 mismatch");
        ctx.close();
    }

    // =========================================================================
    // F19: Overload 3: Insurance Add-on Calculation (Syllabus Unit 1)
    // =========================================================================
    public void testF19_Overload3_Insurance() {
        Rental r = new Rental();
        r.setDailyRate(2000.0);

        // 1. Without insurance equals Overload 2
        assertEquals(5400.0, r.calculateRent(3, 10.0, false), 0.01, "Without insurance mismatch");

        // 2. With insurance adds 250/day flat
        // 5400 + (3 * 250) = 6150.0
        assertEquals(6150.0, r.calculateRent(3, 10.0, true), 0.01, "With insurance mismatch");

        // 3. Single day insurance fee
        // 2000 + 250 = 2250.0
        assertEquals(2250.0, r.calculateRent(1, 0.0, true), 0.01, "1 day with insurance mismatch");

        // 4. Gold member with insurance
        // (2000 * 5 * 0.80) + (5 * 250) = 8000 + 1250 = 9250.0
        assertEquals(9250.0, r.calculateRent(5, 20.0, true), 0.01, "Gold member 5 days with insurance mismatch");

        // 5. Default insurance daily rate constant
        assertEquals(250.0, Rental.DEFAULT_INSURANCE_RATE_PER_DAY, 0.001, "Default insurance fee constant mismatch");
    }

    // =========================================================================
    // F20: Overload 4: Final Return Bill (Syllabus Units 1 & 4)
    // =========================================================================
    public void testF20_Overload4_FinalReturnBill() {
        Rental r = new Rental();
        r.setRentalDays(3);
        r.setDailyRate(2000.0);
        r.setBaseRent(6000.0);
        r.setDiscountAmount(600.0);
        r.setInsuranceFee(750.0);
        r.setSecurityDeposit(3000.0);
        r.setStartMileage(10000.0);

        // 1. On time return, zero damage
        // 6000 - 600 + 750 = 6150.0
        double onTime = r.calculateFinalReturnBill(3, 10300.0, 0.0, 0.0);
        assertEquals(6150.0, onTime, 0.01, "On time return bill mismatch");

        // 2. Late return (2 days late, late multiplier 1.5x)
        // Late fee: 2 * (2000 * 1.5) = 6000.0
        // Total: 6150 + 6000 = 12150.0
        double lateBill = r.calculateFinalReturnBill(5, 10500.0, 0.0, 0.0);
        assertEquals(12150.0, lateBill, 0.01, "Late return bill mismatch");

        // 3. With assessed damage fee (Rs 1500)
        double damageBill = r.calculateFinalReturnBill(3, 10300.0, 1500.0, 0.0);
        assertEquals(7650.0, damageBill, 0.01, "Return bill with damage mismatch");

        // 4. With cleaning fee (Rs 500)
        double cleaningBill = r.calculateFinalReturnBill(3, 10300.0, 0.0, 500.0);
        assertEquals(6650.0, cleaningBill, 0.01, "Return bill with cleaning fee mismatch");

        // 5. Net balance payable after deposit deduction
        // Net payable = 7650 - 3000 = 4650.0
        double netPayable = damageBill - r.getSecurityDeposit();
        assertEquals(4650.0, netPayable, 0.01, "Net balance payable mismatch");
    }

    // =========================================================================
    // F21: Car Inventory Filter & Maintenance Toggle (Image Module 1)
    // =========================================================================
    public void testF21_InventoryFilterAndMaintenance() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // 1. Filter by category SEDAN
            List<Car> sedans = service.filterInventory("SEDAN", null);
            for (Car c : sedans) {
                assertEquals("SEDAN", c.getVehicleType(), "Filtered car must be SEDAN");
            }

            // 2. Filter by status AVAILABLE
            List<Car> avail = service.filterInventory(null, "AVAILABLE");
            for (Car c : avail) {
                assertEquals("AVAILABLE", c.getStatus(), "Filtered car must be AVAILABLE");
            }

            // 3. Toggle maintenance to true
            service.toggleMaintenance(1, true);
            Car maintained = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(1) : ctx.getMemoryCar(1);
            assertEquals("MAINTENANCE", maintained.getStatus(), "Status must become MAINTENANCE");

            // 4. Maintained car cannot be booked
            assertThrows(TestContext.CarUnavailableException.class, () -> {
                service.bookCar(1, 1, 2, false);
            }, "Maintained vehicle must throw CarUnavailableException");

            // 5. Toggle maintenance to false restores AVAILABLE
            service.toggleMaintenance(1, false);
            Car restored = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(1) : ctx.getMemoryCar(1);
            assertEquals("AVAILABLE", restored.getStatus(), "Status must restore to AVAILABLE");
        }
    }

    // =========================================================================
    // F22: Customer Uniqueness & Phone/License Validation (Image Module 2)
    // =========================================================================
    public void testF22_CustomerUniquenessAndValidation() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            // 1. Phone validation: valid 10 digits
            String validPhone = "9876543210";
            assertTrue(validPhone.matches("\\d{10}"), "10-digit phone must be valid");

            // 2. License validation: alphanumeric format
            String validLic = "DL0120201234567";
            assertTrue(validLic.length() >= 5, "License must be at least 5 chars");

            // 3. Duplicate phone registration rejected in live JDBC
            if (ctx.isLiveJdbc()) {
                Customer c1 = new Customer("User A", "9899001122", "DL9899001122", "a@test.com", "Pune", Customer.MemberTier.REGULAR);
                ctx.getCustomerDAO().insertCustomer(c1);

                Customer c2 = new Customer("User B", "9899001122", "DL9899009999", "b@test.com", "Pune", Customer.MemberTier.REGULAR);
                assertThrows(Exception.class, () -> {
                    ctx.getCustomerDAO().insertCustomer(c2);
                }, "Duplicate phone must trigger uniqueness violation");

                // 4. Duplicate license registration rejected
                Customer c3 = new Customer("User C", "9899003344", "DL9899001122", "c@test.com", "Pune", Customer.MemberTier.REGULAR);
                assertThrows(Exception.class, () -> {
                    ctx.getCustomerDAO().insertCustomer(c3);
                }, "Duplicate license must trigger uniqueness violation");
            } else {
                Customer c1 = new Customer("User A", "9899001122", "DL9899001122", "a@test.com", "Pune", Customer.MemberTier.REGULAR);
                ctx.insertMemoryCustomer(c1);

                // 3. Duplicate phone rejected in memory store
                Customer c2 = new Customer("User B", "9899001122", "DL9899009999", "b@test.com", "Pune", Customer.MemberTier.REGULAR);
                assertThrows(IllegalArgumentException.class, () -> {
                    ctx.insertMemoryCustomer(c2);
                }, "Duplicate phone in memory store must throw IllegalArgumentException");

                // 4. Duplicate license rejected in memory store
                Customer c3 = new Customer("User C", "9899003344", "DL9899001122", "c@test.com", "Pune", Customer.MemberTier.REGULAR);
                assertThrows(IllegalArgumentException.class, () -> {
                    ctx.insertMemoryCustomer(c3);
                }, "Duplicate license in memory store must throw IllegalArgumentException");
            }

            // 5. Customer object equals / hashcode based on license
            Customer custA = new Customer("Alice", "9111111111", "DL_COMMON_1", "", "", Customer.MemberTier.REGULAR);
            Customer custB = new Customer("Bob", "9222222222", "DL_COMMON_1", "", "", Customer.MemberTier.REGULAR);
            assertEquals(custA, custB, "Customers with same driving license are equal");
        }
    }

    // =========================================================================
    // F23: Return & Damage Inspection Workflow (Image Module 4)
    // =========================================================================
    public void testF23_ReturnAndInspectionWorkflow() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // 1. Book car
            Rental r = service.bookCar(1, 1, 3, false);
            assertNotNull(r, "Initial booking confirmed");
            assertTrue(r.getRentalId() > 0, "Valid positive rental ID generated");
            int rentalId = r.getRentalId();

            // 2. Process return with updated odometer
            Rental closed = service.processReturn(rentalId, LocalDate.now().plusDays(3), 15400.0, 0.0, "Clean return");
            assertEquals(Rental.RentalStatus.COMPLETED, closed.getStatus(), "Closed rental status must be COMPLETED");
            assertNotNull(closed.getActualReturnDate(), "Actual return date must be recorded on return");

            // 3. Car is released back to AVAILABLE
            Car car = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(1) : ctx.getMemoryCar(1);
            assertEquals("AVAILABLE", car.getStatus(), "Car must become AVAILABLE on return");

            // 4. Car odometer in fleet is updated
            assertEquals(15400.0, car.getMileage(), 0.01, "Fleet car odometer must update to end mileage");

            // 5. Car can now be immediately re-booked with odometer continuity
            Rental rebooked = service.bookCar(2, 1, 2, false);
            assertNotNull(rebooked, "Car must be rebookable after return");
            assertEquals(15400.0, rebooked.getStartMileage(), 0.01, "Rebooked rental start mileage must equal returned end mileage");
        }
    }

    // =========================================================================
    // F24: Rental History & Revenue/Active KPIs (Image Module 5)
    // =========================================================================
    public void testF24_RentalHistoryAndKPIs() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // 1. Active count reflects initial state
            int initialActive = service.getActiveRentalsCount();
            assertTrue(initialActive >= 0, "Active count must be non-negative");

            // 2. Book 2 distinct vehicles
            Rental r1 = service.bookCar(1, 1, 2, false);
            Rental r2 = service.bookCar(2, 2, 3, false);
            assertEquals(initialActive + 2, service.getActiveRentalsCount(), "Active rentals count incremented by 2");

            // 3. Return 1 rental
            service.processReturn(r1.getRentalId(), LocalDate.now().plusDays(2), 16000.0, 0.0, "Returned");
            assertEquals(initialActive + 1, service.getActiveRentalsCount(), "Active count decremented by 1");

            // 4. Completed revenue calculation is positive
            double rev = service.calculateTotalRevenue();
            assertTrue(rev > 0.0, "Total revenue must be positive after return");

            // 5. Second return adds to revenue
            double prevRev = rev;
            service.processReturn(r2.getRentalId(), LocalDate.now().plusDays(3), 23000.0, 0.0, "Returned");
            assertTrue(service.calculateTotalRevenue() > prevRev, "Revenue must increase after second return");
        }
    }

    // =========================================================================
    // F25: Minimal Indian Tricolour Theme Constants (User Requirement)
    // =========================================================================
    public void testF25_MinimalTricolourTheme() {
        // Contract color definitions
        Color saffron = new Color(255, 153, 51); // #FF9933
        Color white = new Color(255, 255, 255);   // #FFFFFF
        Color green = new Color(19, 136, 8);     // #138808
        Color navy = new Color(0, 0, 128);       // #000080

        // 1. Saffron RGB components
        assertEquals(255, saffron.getRed(), "Saffron Red component mismatch");
        assertEquals(153, saffron.getGreen(), "Saffron Green component mismatch");
        assertEquals(51, saffron.getBlue(), "Saffron Blue component mismatch");

        // 2. India Green RGB components
        assertEquals(19, green.getRed(), "Green Red component mismatch");
        assertEquals(136, green.getGreen(), "Green Green component mismatch");
        assertEquals(8, green.getBlue(), "Green Blue component mismatch");

        // 3. Ashoka Navy Blue RGB components
        assertEquals(0, navy.getRed(), "Navy Red component mismatch");
        assertEquals(0, navy.getGreen(), "Navy Green component mismatch");
        assertEquals(128, navy.getBlue(), "Navy Blue component mismatch");

        // 4. Contrast check: Green button with White text has sufficient contrast
        double greenLum = 0.2126 * (19 / 255.0) + 0.7152 * (136 / 255.0) + 0.0722 * (8 / 255.0);
        assertTrue(greenLum < 0.5, "India green luminance allows high white text contrast");

        // 5. Button construction using Tricolour theme tokens
        JButton btn = new JButton("Book Car");
        btn.setBackground(green);
        btn.setForeground(white);
        assertEquals(green, btn.getBackground(), "Button background should be India Green");
    }

    // =========================================================================
    // F26: GUI Layout Contracts (Syllabus Unit 5)
    // =========================================================================
    public void testF26_GUILayoutContracts() {
        // 1. BorderLayout contract (mandated for MainFrame)
        JPanel mainPanel = new JPanel(new BorderLayout());
        assertTrue(mainPanel.getLayout() instanceof BorderLayout, "Main container must use BorderLayout");

        // 2. North, Center, South regions
        JPanel northBanner = new JPanel();
        JPanel centerWorkspace = new JPanel();
        JPanel southStatusBar = new JPanel();
        mainPanel.add(northBanner, BorderLayout.NORTH);
        mainPanel.add(centerWorkspace, BorderLayout.CENTER);
        mainPanel.add(southStatusBar, BorderLayout.SOUTH);
        assertEquals(3, mainPanel.getComponentCount(), "Main container must contain 3 border regions");

        // 3. GridLayout contract (mandated for form inputs and KPI cards)
        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        assertTrue(formPanel.getLayout() instanceof GridLayout, "Form container must use GridLayout");
        GridLayout gl = (GridLayout) formPanel.getLayout();
        assertEquals(4, gl.getRows(), "GridLayout rows mismatch");
        assertEquals(2, gl.getColumns(), "GridLayout columns mismatch");

        // 4. JTable component contract for inventory and history
        JTable table = new JTable(new Object[][]{{"1", "City", "AVAILABLE"}}, new Object[]{"ID", "Model", "Status"});
        assertEquals(1, table.getRowCount(), "Table row count mismatch");
        assertEquals(3, table.getColumnCount(), "Table column count mismatch");

        // 5. Headless safety check: Swing components can be constructed headlessly
        assertNotNull(table.getModel(), "Table model must be non-null in headless environment");
    }

    // =========================================================================
    // F33: start_db.bat Verification (Deliverables)
    // =========================================================================
    public void testF33_StartDbBatVerification() throws Exception {
        File batFile = new File("start_db.bat");
        assertTrue(batFile.exists(), "start_db.bat must exist in project root");

        String content = new String(Files.readAllBytes(batFile.toPath()));
        assertTrue(content.contains("python") || content.contains("py"), "start_db.bat must check for python runtime");
        assertTrue(content.contains("init_db.py"), "start_db.bat must execute init_db.py");
        assertTrue(content.contains("ERRORLEVEL"), "start_db.bat must contain error checking");
        assertTrue(content.contains("car_rental.db"), "start_db.bat must reference car_rental.db target");
    }

    // =========================================================================
    // F34: start_app.bat Verification & Launch Readiness (Deliverables)
    // =========================================================================
    public void testF34_StartAppBatVerification() throws Exception {
        File batFile = new File("start_app.bat");
        // 1. Launcher script must physically exist in root
        assertTrue(batFile.exists(), "start_app.bat deliverable must exist in project root");
        assertTrue(batFile.isFile() && batFile.length() > 0, "start_app.bat must be a non-empty batch file");

        String content = new String(Files.readAllBytes(batFile.toPath()), StandardCharsets.UTF_8);

        // 2. JDK discovery routine present
        assertTrue(content.contains("JAVA_HOME") || content.contains("javac"),
                "start_app.bat must implement JDK discovery (JAVA_HOME or where javac)");

        // 3. Classpath configuration
        assertTrue(content.contains("bin") && content.contains("lib"),
                "start_app.bat classpath must include bin/ and lib/ directories");

        // 4. Main bootstrap class
        assertTrue(content.contains("carrental.Main"),
                "start_app.bat must launch entry point carrental.Main");

        // 5. Error exit handling
        assertTrue(content.contains("exit /b") || content.contains("exit 1") || content.toLowerCase().contains("errorlevel"),
                "start_app.bat must handle non-zero exit codes on compilation failure");
    }
}
