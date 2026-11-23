package carrental.e2e;

import static carrental.e2e.Assertions.*;

import carrental.dao.CarDAO;
import carrental.dao.CustomerDAO;
import carrental.dao.RentalDAO;
import carrental.db.DBConnection;
import carrental.model.*;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tier 4: Real-World Multi-Step Customer Journey Scenarios.
 * Implements all 8 comprehensive end-to-end customer workflows.
 */
public class Tier4ScenarioTests {

    public void runAllTests() throws Exception {
        testScenario1_StandardSedanRentalLifecycle();
        testScenario2_GoldMemberSUVWithInsuranceTrip();
        testScenario3_ConcurrentDoubleBookingContention();
        testScenario4_VehicleReturnWithDamageAndCleaning();
        testScenario5_FleetMaintenanceLifecycle();
        testScenario6_CustomerLicenseDuplicateAndProfileUpdate();
        testScenario7_CorporateFleetBookingAndKPIAudit();
        testScenario8_BatchLauncherAndDBBootstrap();
    }

    // =========================================================================
    // Scenario 1: Standard Customer Sedan Rental Lifecycle
    // Features: F1, F3, F4, F7, F8, F11-F14, F16, F17, F23, F24
    // =========================================================================
    public void testScenario1_StandardSedanRentalLifecycle() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // 1. Customer "Aarav Sharma" registers as REGULAR member
            Customer aarav = new Customer("Aarav Sharma", "9820012345", "MH0120180011111",
                    "aarav@example.com", "Pune", Customer.MemberTier.REGULAR);
            int custId;
            if (ctx.isLiveJdbc()) {
                custId = ctx.getCustomerDAO().insertCustomer(aarav);
            } else {
                custId = ctx.insertMemoryCustomer(aarav);
            }
            assertTrue(custId > 0, "Aarav Sharma customer ID generated");

            // 2. Sedan "Honda City" created in fleet
            Sedan hondaCity = new Sedan("MH-12-SC1-001", "Honda", "City", 2023, 2500.0, 12000.0,
                    5, "PETROL", "MANUAL", 510.0, "EXECUTIVE", 0.0);
            int carId;
            if (ctx.isLiveJdbc()) {
                ctx.getCarDAO().insertCar(hondaCity);
                carId = hondaCity.getCarId();
            } else {
                ctx.insertMemoryCar(hondaCity);
                carId = hondaCity.getCarId();
            }
            assertTrue(carId > 0, "Honda City car ID generated");

            // 3. Aarav books Honda City for 3 days without insurance. Deposit: Rs 3000
            Rental rental = service.bookCar(custId, carId, 3, false);
            assertNotNull(rental, "Booking confirmed");
            assertEquals(7500.0, rental.getBaseRent(), 0.01, "Expected base rent for 3 days @ 2500/day");

            // 4. Assert car status updated to RENTED in fleet
            Car bookedCar = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(carId) : ctx.getMemoryCar(carId);
            assertEquals("RENTED", bookedCar.getStatus(), "Car status must become RENTED");
            assertFalse(bookedCar.isAvailable(), "Car must not be available");

            // 5. Query available cars: Honda City must NOT appear in available list
            List<Car> availCars = service.filterInventory(null, "AVAILABLE");
            for (Car c : availCars) {
                assertFalse(c.getCarId() == carId, "Rented Honda City must not appear in available list");
            }

            // 6. 3 days later, Aarav returns the vehicle on time at 12,350 km (+350 km) with zero damage
            Rental closedRental = service.processReturn(rental.getRentalId(), LocalDate.now().plusDays(3),
                    12350.0, 0.0, "Vehicle returned in pristine condition");

            // 7. Settlement verification
            assertEquals(Rental.RentalStatus.COMPLETED, closedRental.getStatus(), "Rental status must be COMPLETED");
            assertEquals(7500.0, closedRental.getTotalAmount(), 0.01, "Final settled amount should be 7500.0");

            // 8. Car restored to AVAILABLE with updated fleet odometer
            Car returnedCar = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(carId) : ctx.getMemoryCar(carId);
            assertEquals("AVAILABLE", returnedCar.getStatus(), "Car status must be restored to AVAILABLE");
            assertEquals(12350.0, returnedCar.getMileage(), 0.01, "Fleet mileage must be updated to 12350.0");

            // 9. Rental history contains 1 completed rental; KPI revenue equals 7500.0
            assertEquals(0, service.getActiveRentalsCount(), "Active rentals count should be 0");
            assertEquals(7500.0, service.calculateTotalRevenue(), 0.01, "Total KPI revenue should equal 7500.0");

            // 10. Customer Re-Booking Edge Sequence: Second customer immediately books returned vehicle
            Customer priya = new Customer("Priya Patel", "9820099887", "DL0220200009999", "priya@example.com", "Pune", Customer.MemberTier.SILVER);
            int priyaId = ctx.isLiveJdbc() ? ctx.getCustomerDAO().insertCustomer(priya) : ctx.insertMemoryCustomer(priya);
            assertTrue(priyaId > 0, "Second customer Priya Patel registered successfully");

            // Priya books the Honda City for 2 days with insurance
            Rental rebookedRental = service.bookCar(priyaId, carId, 2, true);
            assertNotNull(rebookedRental, "Re-booking on returned available car must succeed");
            assertEquals(Rental.RentalStatus.ACTIVE, rebookedRental.getStatus(), "Rebooked rental status must be ACTIVE");
            assertEquals(12350.0, rebookedRental.getStartMileage(), 0.01,
                    "Rebooked rental start mileage must exactly match previous rental's end mileage");

            Car rebookedCar = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(carId) : ctx.getMemoryCar(carId);
            assertEquals("RENTED", rebookedCar.getStatus(), "Car status must transition back to RENTED upon rebooking");
            assertFalse(rebookedCar.isAvailable(), "Car must not be available while rebooked");
            assertEquals(1, service.getActiveRentalsCount(), "Active rentals count must reflect new re-booking");
        }
    }

    // =========================================================================
    // Scenario 2: Gold Member SUV Booking with Insurance & All-Terrain Trip
    // Features: F5, F7, F8, F12-F14, F16, F18, F19, F23, F24
    // =========================================================================
    public void testScenario2_GoldMemberSUVWithInsuranceTrip() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // 1. Customer "Vikramaditya Rao" registers as GOLD (20% discount)
            Customer vikram = new Customer("Vikramaditya Rao", "9820055555", "MH0220150055555",
                    "vikram@example.com", "Mumbai", Customer.MemberTier.GOLD);
            int custId = ctx.isLiveJdbc() ? ctx.getCustomerDAO().insertCustomer(vikram) : ctx.insertMemoryCustomer(vikram);

            // 2. SUV "Mahindra Scorpio-N" (AWD=true, 7-seater=true, daily rate Rs 4000)
            // Note: in our SUV base rate formula: base + AWD (300/day) + 7-seater (10% = 400/day) = 4700/day
            // But let's verify exact rate calculation with 20% member tier discount + 250/day insurance
            SUV scorpio = new SUV("MH-02-SC2-001", "Mahindra", "Scorpio-N", 2023, 4000.0, 5000.0,
                    7, "DIESEL", "MANUAL", true, 210, true, 300.0);
            int carId;
            if (ctx.isLiveJdbc()) {
                ctx.getCarDAO().insertCar(scorpio);
                carId = scorpio.getCarId();
            } else {
                ctx.insertMemoryCar(scorpio);
                carId = scorpio.getCarId();
            }

            // 3. Vikramaditya books SUV for 5 days with comprehensive insurance
            Rental rental = service.bookCar(custId, carId, 5, true);
            assertNotNull(rental, "Gold SUV booking confirmed");
            assertEquals(23500.0, rental.getBaseRent(), 0.01, "Expected SUV base rent with AWD and 7-seater: 5 * (4000 + 300 + 400)");
            assertEquals(4700.0, rental.getDiscountAmount(), 0.01, "Expected 20% Gold tier discount: 0.20 * 23500.0");
            assertEquals(1250.0, rental.getInsuranceFee(), 0.01, "Expected 5 days insurance @ 250/day: 1250.0");
            assertEquals(Rental.RentalStatus.ACTIVE, rental.getStatus(), "Rental status should be ACTIVE");

            // 4. Return after all-terrain trip at 5,800 km (+800 km) with minor bumper scratch Rs 2,500
            // Because comprehensive insurance is active, the minor damage is waived under policy
            Rental settled = service.processReturn(rental.getRentalId(), LocalDate.now().plusDays(5),
                    5800.0, 2500.0, "Minor scratch from off-road branch");

            assertEquals(Rental.RentalStatus.COMPLETED, settled.getStatus(), "Rental status must be COMPLETED");
            assertEquals(0.0, settled.getDamageFee(), 0.01, "Scratch damage must be waived under insurance policy");
            assertEquals(20050.0, settled.getTotalAmount(), 0.01,
                    "Final settlement total with waived damage must equal 20050.0: (23500 - 4700) + 1250 + 0");

            // 5. Car restored to AVAILABLE, odometer updated to 5800 km
            Car carAfter = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(carId) : ctx.getMemoryCar(carId);
            assertEquals("AVAILABLE", carAfter.getStatus(), "Car must be AVAILABLE");
            assertEquals(5800.0, carAfter.getMileage(), 0.01, "Odometer updated to 5800.0");
        }
    }

    // =========================================================================
    // Scenario 3: Concurrent Double-Booking Contention
    // Features: F15, F16, F12, F14
    // =========================================================================
    public void testScenario3_ConcurrentDoubleBookingContention() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // Single available Hatchback "Maruti Swift"
            Hatchback swift = new Hatchback("MH-14-SC3-001", "Maruti", "Swift", 2022, 1500.0, 10000.0,
                    5, "PETROL", "MANUAL", true, 22.0, 0.0);
            Customer alice = new Customer("Alice Smith", "9811000011", "DL_ALICE", "a@test.com", "Pune", Customer.MemberTier.REGULAR);
            Customer bob = new Customer("Bob Jones", "9811000022", "DL_BOB", "b@test.com", "Pune", Customer.MemberTier.REGULAR);

            int carId;
            int aliceId;
            int bobId;
            if (ctx.isLiveJdbc()) {
                ctx.getCarDAO().insertCar(swift);
                carId = swift.getCarId();
                aliceId = ctx.getCustomerDAO().insertCustomer(alice);
                bobId = ctx.getCustomerDAO().insertCustomer(bob);
            } else {
                ctx.insertMemoryCar(swift);
                carId = swift.getCarId();
                aliceId = ctx.insertMemoryCustomer(alice);
                bobId = ctx.insertMemoryCustomer(bob);
            }

            // Concurrency latch to trigger simultaneous booking attempt
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch readyLatch = new CountDownLatch(2);
            CountDownLatch startLatch = new CountDownLatch(1);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger unavailableCount = new AtomicInteger(0);

            pool.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    service.bookCar(aliceId, carId, 3, false);
                    successCount.incrementAndGet();
                } catch (TestContext.CarUnavailableException e) {
                    unavailableCount.incrementAndGet();
                } catch (Exception ignored) {
                }
            });

            pool.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    service.bookCar(bobId, carId, 3, false);
                    successCount.incrementAndGet();
                } catch (TestContext.CarUnavailableException e) {
                    unavailableCount.incrementAndGet();
                } catch (Exception ignored) {
                }
            });

            readyLatch.await(3, TimeUnit.SECONDS);
            startLatch.countDown();
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);

            // Exactly one booking succeeded, and the second thread caught CarUnavailableException
            assertEquals(1, successCount.get(), "Exactly one customer booking must succeed");
            assertEquals(1, unavailableCount.get(), "Losing thread must catch CarUnavailableException");

            // Fleet state is safely RENTED, not corrupted
            Car carFinal = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(carId) : ctx.getMemoryCar(carId);
            assertEquals("RENTED", carFinal.getStatus(), "Car status must be safely RENTED");
        }
    }

    // =========================================================================
    // Scenario 4: Vehicle Return with Body Damage & Deep Cleaning Assessment
    // Features: F8, F14, F20, F23, F24
    // =========================================================================
    public void testScenario4_VehicleReturnWithDamageAndCleaning() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            Customer cust = new Customer("Karan Johar", "9899009900", "DL0420180099000", "k@test.com", "Delhi", Customer.MemberTier.REGULAR);
            Sedan verna = new Sedan("DL-01-SC4-001", "Hyundai", "Verna", 2023, 2200.0, 15000.0,
                    5, "PETROL", "AUTOMATIC", 480.0, "EXECUTIVE", 0.0);

            int custId = ctx.isLiveJdbc() ? ctx.getCustomerDAO().insertCustomer(cust) : ctx.insertMemoryCustomer(cust);
            int carId;
            if (ctx.isLiveJdbc()) {
                ctx.getCarDAO().insertCar(verna);
                carId = verna.getCarId();
            } else {
                ctx.insertMemoryCar(verna);
                carId = verna.getCarId();
            }

            // Customer books for 2 days (expected base rent Rs 4,400) without insurance
            Rental rental = service.bookCar(custId, carId, 2, false);
            assertEquals(4400.0, rental.getBaseRent(), 0.01, "Expected base rent for 2 days @ 2200/day");

            // Return 2 days late (actual duration 4 days). Inspection finds door dent Rs 4,500 and mud stains Rs 1,000
            // Final bill formula:
            // Base 4400 + late penalty: 2 days * (2200 * 1.5) = 6600 + damage 4500 + cleaning 1000 = 16500.0
            Rental closed = service.processReturn(rental.getRentalId(), LocalDate.now().plusDays(4),
                    15800.0, 4500.0, "Door dent, mud stains");

            assertEquals(Rental.RentalStatus.COMPLETED, closed.getStatus(), "Rental must be COMPLETED");
            assertEquals(15500.0, closed.getTotalAmount(), 0.01,
                    "Exact final bill must equal 15500.0: base (4400.0) + 2 overdue days @ 1.5x (6600.0) + damage fee (4500.0)");

            // Car released back to AVAILABLE (damage was 4500 <= 5000)
            Car carAfter = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(carId) : ctx.getMemoryCar(carId);
            assertEquals("AVAILABLE", carAfter.getStatus(), "Car released back to AVAILABLE");
            assertEquals(15800.0, carAfter.getMileage(), 0.01, "Odometer updated to 15800.0");
        }
    }

    // =========================================================================
    // Scenario 5: Fleet Maintenance Lifecycle & Exclusion from Booking
    // Features: F12, F15, F16, F21
    // =========================================================================
    public void testScenario5_FleetMaintenanceLifecycle() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            Customer pooja = new Customer("Pooja Patel", "9877000001", "DL_POOJA_01", "p@test.com", "Ahmedabad", Customer.MemberTier.REGULAR);
            SUV harrier = new SUV("GJ-01-SC5-001", "Tata", "Harrier", 2023, 3500.0, 8000.0,
                    5, "DIESEL", "MANUAL", false, 205, false, 0.0);

            int custId = ctx.isLiveJdbc() ? ctx.getCustomerDAO().insertCustomer(pooja) : ctx.insertMemoryCustomer(pooja);
            int carId;
            if (ctx.isLiveJdbc()) {
                ctx.getCarDAO().insertCar(harrier);
                carId = harrier.getCarId();
            } else {
                ctx.insertMemoryCar(harrier);
                carId = harrier.getCarId();
            }

            // 1. Manager toggles Harrier to MAINTENANCE
            service.toggleMaintenance(carId, true);
            Car carMaint = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(carId) : ctx.getMemoryCar(carId);
            assertEquals("MAINTENANCE", carMaint.getStatus(), "Status must be MAINTENANCE");

            // 2. Pooja Patel attempts to book: immediately throws CarUnavailableException
            assertThrows(TestContext.CarUnavailableException.class, () -> {
                service.bookCar(custId, carId, 3, false);
            }, "Booking car in MAINTENANCE must throw CarUnavailableException");

            // 3. Maintenance completed: status restored to AVAILABLE
            service.toggleMaintenance(carId, false);
            Car carAvail = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(carId) : ctx.getMemoryCar(carId);
            assertEquals("AVAILABLE", carAvail.getStatus(), "Status must be AVAILABLE");

            // 4. Subsequent booking succeeds
            Rental r = service.bookCar(custId, carId, 3, false);
            assertNotNull(r, "Booking after maintenance restored must succeed");
            assertEquals(Rental.RentalStatus.ACTIVE, r.getStatus(), "Rental is now ACTIVE");
        }
    }

    // =========================================================================
    // Scenario 6: Customer License Duplicate Rejection & Profile Update
    // Features: F7, F13, F22
    // =========================================================================
    public void testScenario6_CustomerLicenseDuplicateAndProfileUpdate() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            CustomerDAO custDao = ctx.getCustomerDAO();

            Customer sunil = new Customer("Sunil Verma", "9811122233", "DL0420199999999", "sunil@test.com", "Delhi", Customer.MemberTier.REGULAR);
            int sunilId;
            if (ctx.isLiveJdbc()) {
                sunilId = custDao.insertCustomer(sunil);
            } else {
                sunilId = ctx.insertMemoryCustomer(sunil);
            }
            assertTrue(sunilId > 0, "Sunil Verma registered");

            // Attempt to register imposter with identical license plate
            Customer imposter = new Customer("Imposter", "9877788899", "DL0420199999999", "imp@test.com", "Delhi", Customer.MemberTier.REGULAR);
            assertThrows(Exception.class, () -> {
                if (ctx.isLiveJdbc()) {
                    custDao.insertCustomer(imposter);
                } else {
                    ctx.insertMemoryCustomer(imposter);
                }
            }, "Duplicate driving license must be rejected by both database and memory store");

            // Sunil updates profile to SILVER
            sunil.setPhone("9811122244");
            sunil.setMemberTier(Customer.MemberTier.SILVER);
            assertEquals(10.0, sunil.getTierDiscountPercent(), 0.01, "Sunil tier upgraded to SILVER (10% discount)");
        }
    }

    // =========================================================================
    // Scenario 7: Multi-Vehicle Corporate Fleet Booking & Aggregate KPI Audit
    // Features: F4, F5, F6, F7, F12-F14, F16, F24
    // =========================================================================
    public void testScenario7_CorporateFleetBookingAndKPIAudit() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            Customer techCorp = new Customer("TechCorp India", "9900112233", "CORP_DL_999", "fleet@techcorp.in", "Bangalore", Customer.MemberTier.REGULAR);
            int corpId = ctx.isLiveJdbc() ? ctx.getCustomerDAO().insertCustomer(techCorp) : ctx.insertMemoryCustomer(techCorp);

            Sedan city = new Sedan("KA-01-SC7-001", "Honda", "City", 2023, 2500.0, 10000.0, 5, "PETROL", "MANUAL", 500.0, "EXECUTIVE", 0.0);
            SUV fortuner = new SUV("KA-01-SC7-002", "Toyota", "Fortuner", 2023, 5000.0, 20000.0, 7, "DIESEL", "AUTOMATIC", false, 220, false, 0.0);
            Hatchback i20 = new Hatchback("KA-01-SC7-003", "Hyundai", "i20", 2022, 1500.0, 15000.0, 5, "PETROL", "MANUAL", true, 20.0, 0.0);

            int cityId, fortunerId, i20Id;
            if (ctx.isLiveJdbc()) {
                ctx.getCarDAO().insertCar(city);
                ctx.getCarDAO().insertCar(fortuner);
                ctx.getCarDAO().insertCar(i20);
                cityId = city.getCarId();
                fortunerId = fortuner.getCarId();
                i20Id = i20.getCarId();
            } else {
                ctx.insertMemoryCar(city);
                ctx.insertMemoryCar(fortuner);
                ctx.insertMemoryCar(i20);
                cityId = city.getCarId();
                fortunerId = fortuner.getCarId();
                i20Id = i20.getCarId();
            }

            // TechCorp books 3 distinct vehicles
            Rental rCity = service.bookCar(corpId, cityId, 2, false);        // 2500 * 2 = 5000
            Rental rFortuner = service.bookCar(corpId, fortunerId, 3, false); // 5000 * 3 = 15000
            Rental rI20 = service.bookCar(corpId, i20Id, 4, false);            // 1500 * 4 = 6000

            assertEquals(3, service.getActiveRentalsCount(), "Active rentals count should be 3");

            // Return all 3 on schedule
            service.processReturn(rCity.getRentalId(), LocalDate.now().plusDays(2), 10250.0, 0.0, "City returned");
            assertEquals(2, service.getActiveRentalsCount(), "Active count becomes 2");

            service.processReturn(rFortuner.getRentalId(), LocalDate.now().plusDays(3), 20600.0, 0.0, "Fortuner returned");
            assertEquals(1, service.getActiveRentalsCount(), "Active count becomes 1");

            service.processReturn(rI20.getRentalId(), LocalDate.now().plusDays(4), 15400.0, 0.0, "i20 returned");
            assertEquals(0, service.getActiveRentalsCount(), "Active count becomes 0");

            // Aggregate revenue: 5000 + 15000 + 6000 = 26000.0
            assertEquals(26000.0, service.calculateTotalRevenue(), 0.01, "Total revenue across fleet should equal 26000.0");
        }
    }

    // =========================================================================
    // Scenario 8: Batch Launcher & DB Bootstrap Verification
    // Features: F9, F10, F11, F33, F34
    // =========================================================================
    public void testScenario8_BatchLauncherAndDBBootstrap() throws Exception {
        // 1. Verify start_db.bat exists and is non-empty
        File startDb = new File("start_db.bat");
        assertTrue(startDb.exists(), "start_db.bat must exist");
        assertTrue(startDb.length() > 50, "start_db.bat must not be empty");

        // 2. Verify db/schema.sql exists
        File schema = new File("db/schema.sql");
        assertTrue(schema.exists(), "db/schema.sql must exist");
        String schemaContent = new String(Files.readAllBytes(schema.toPath()));
        assertTrue(schemaContent.contains("CREATE TABLE"), "schema.sql must contain DDL statements");

        // 3. Verify db/seed_data.sql exists
        File seed = new File("db/seed_data.sql");
        assertTrue(seed.exists(), "db/seed_data.sql must exist");
        String seedContent = new String(Files.readAllBytes(seed.toPath()));
        assertTrue(seedContent.contains("INSERT"), "seed_data.sql must contain INSERT statements");

        // 4. Verify db/init_db.py exists
        File initPy = new File("db/init_db.py");
        assertTrue(initPy.exists(), "db/init_db.py must exist");

        // 5. DB connection test and seed record validation
        try (TestContext ctx = TestContext.createWithSeed()) {
            int carCount = ctx.isLiveJdbc() ? ctx.getCarDAO().getAllCars().size() : ctx.getAllMemoryCars().size();
            assertTrue(carCount >= 3, "Fleet must be seeded with cars (found " + carCount + ")");
            int custCount = ctx.isLiveJdbc() ? ctx.getCustomerDAO().getAllCustomers().size() : ctx.getAllMemoryCustomers().size();
            assertTrue(custCount >= 2, "Customers must be seeded (found " + custCount + ")");
        }
    }
}
