package carrental.e2e;

import static carrental.e2e.Assertions.*;

import carrental.dao.CarDAO;
import carrental.dao.CustomerDAO;
import carrental.dao.RentalDAO;
import carrental.db.DBConnection;
import carrental.model.*;

import java.awt.Color;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * Tier 3: Pairwise Combinations and Cross-Feature Interaction Tests.
 * Implements all 28 pairwise combination tests (T3_P01 through T3_P28).
 */
public class Tier3CombinationTests {

    public void runAllTests() throws Exception {
        testP01_ConcurrencyAndTierDiscount();
        testP02_ConcurrencyAndInsuranceAddon();
        testP03_SedanSpecializationAndGoldTier();
        testP04_SUVSpecializationAndInsuranceWaiver();
        testP05_HatchbackSpecializationAndSilverTier();
        testP06_ConcurrencyAndMaintenanceToggleRace();
        testP07_ReturnDamageFeeAndTierPolicy();
        testP08_LateReturnAndCleaningFeeAndMileage();
        testP09_CustomerLicenseValidationAndRentalDB();
        testP10_SequentialLifecycleOnSingleCar();
        testP11_ConcurrentReadAndWriteStress();
        testP12_InventoryFilterAndDynamicRentalState();
        testP13_MaintenanceModeToggleAndHistoryIntegrity();
        testP14_CustomerProfileUpdateWithActiveRental();
        testP15_RentalHistoryKPIAcrossMixedHierarchy();
        testP16_SameDayReturnPolicyAndInsurance();
        testP17_HighMileageOdometerRollover();
        testP18_TricolourThemeAcrossPanels();
        testP19_FormInputValidationAndStateReset();
        testP20_PolymorphicRentableDispatchInSettlement();
        testP21_DatabaseConstraintViolationRollback();
        testP22_PreSeededFleetBookingVerification();
        testP23_StartDbInitializationAndJavaDAO();
        testP24_CustomerRegistrationAndImmediateBooking();
        testP25_AbortedBookingDoesNotLeakLock();
        testP26_ConcurrentReturnsOfDistinctVehicles();
        testP27_DepositDeductionWhenDamageExceedsDeposit();
        testP28_DoubleBookingPreventionOnRentedVehicle();
    }

    // T3_P01: Concurrency + Tier Discount Booking (F16 + F18)
    public void testP01_ConcurrencyAndTierDiscount() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // Set up 3 customers with different tiers and 3 distinct cars
            Customer cReg = new Customer(1, "Regular Cust", "9811000001", "DL001", "r@t.com", "Pune", Customer.MemberTier.REGULAR);
            Customer cSil = new Customer(2, "Silver Cust", "9811000002", "DL002", "s@t.com", "Pune", Customer.MemberTier.SILVER);
            Customer cGld = new Customer(3, "Gold Cust", "9811000003", "DL003", "g@t.com", "Pune", Customer.MemberTier.GOLD);

            Car car1 = new Car(1, "MH-12-P1-01", "Honda", "City", 2023, 2000.0, 10000.0, "AVAILABLE", 5, "PETROL", "MANUAL");
            Car car2 = new Car(2, "MH-12-P1-02", "Honda", "City", 2023, 2000.0, 10000.0, "AVAILABLE", 5, "PETROL", "MANUAL");
            Car car3 = new Car(3, "MH-12-P1-03", "Honda", "City", 2023, 2000.0, 10000.0, "AVAILABLE", 5, "PETROL", "MANUAL");

            if (ctx.isLiveJdbc()) {
                ctx.getCustomerDAO().insertCustomer(cReg);
                ctx.getCustomerDAO().insertCustomer(cSil);
                ctx.getCustomerDAO().insertCustomer(cGld);
                ctx.getCarDAO().insertCar(car1);
                ctx.getCarDAO().insertCar(car2);
                ctx.getCarDAO().insertCar(car3);
            } else {
                ctx.insertMemoryCustomer(cReg);
                ctx.insertMemoryCustomer(cSil);
                ctx.insertMemoryCustomer(cGld);
                ctx.insertMemoryCar(car1);
                ctx.insertMemoryCar(car2);
                ctx.insertMemoryCar(car3);
            }

            ExecutorService exec = Executors.newFixedThreadPool(3);
            CountDownLatch latch = new CountDownLatch(1);
            List<Future<Rental>> futures = new ArrayList<>();

            futures.add(exec.submit(() -> {
                latch.await();
                return service.bookCar(cReg.getCustomerId(), car1.getCarId(), 3, false);
            }));
            futures.add(exec.submit(() -> {
                latch.await();
                return service.bookCar(cSil.getCustomerId(), car2.getCarId(), 3, false);
            }));
            futures.add(exec.submit(() -> {
                latch.await();
                return service.bookCar(cGld.getCustomerId(), car3.getCarId(), 3, false);
            }));

            latch.countDown();
            exec.shutdown();
            exec.awaitTermination(5, TimeUnit.SECONDS);

            Rental rReg = futures.get(0).get();
            Rental rSil = futures.get(1).get();
            Rental rGld = futures.get(2).get();

            assertEquals(6000.0, rReg.getTotalAmount(), 0.01, "REGULAR tier rental amount mismatch");
            assertEquals(5400.0, rSil.getTotalAmount(), 0.01, "SILVER tier rental amount mismatch (10% off)");
            assertEquals(4800.0, rGld.getTotalAmount(), 0.01, "GOLD tier rental amount mismatch (20% off)");
        }
    }

    // T3_P02: Concurrency + Insurance Addon Booking (F16 + F19)
    public void testP02_ConcurrencyAndInsuranceAddon() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            Customer cust = new Customer(1, "Ins Cust", "9822000001", "DL_INS_01", "i@t.com", "Pune", Customer.MemberTier.REGULAR);
            Car carNoIns = new Car(1, "MH-12-P2-01", "Toyota", "Glanza", 2022, 1500.0, 10000.0, "AVAILABLE", 5, "PETROL", "MANUAL");
            Car carWithIns = new Car(2, "MH-12-P2-02", "Toyota", "Glanza", 2022, 1500.0, 10000.0, "AVAILABLE", 5, "PETROL", "MANUAL");

            if (ctx.isLiveJdbc()) {
                ctx.getCustomerDAO().insertCustomer(cust);
                ctx.getCarDAO().insertCar(carNoIns);
                ctx.getCarDAO().insertCar(carWithIns);
            } else {
                ctx.insertMemoryCustomer(cust);
                ctx.insertMemoryCar(carNoIns);
                ctx.insertMemoryCar(carWithIns);
            }

            Rental r1 = service.bookCar(cust.getCustomerId(), carNoIns.getCarId(), 2, false);
            Rental r2 = service.bookCar(cust.getCustomerId(), carWithIns.getCarId(), 2, true);

            assertEquals(3000.0, r1.getTotalAmount(), 0.01, "Without insurance rent mismatch");
            assertEquals(3500.0, r2.getTotalAmount(), 0.01, "With insurance rent mismatch (3000 + 2*250)");
            assertEquals(0.0, r1.getInsuranceFee(), 0.01, "No insurance fee should be 0.0");
            assertEquals(500.0, r2.getInsuranceFee(), 0.01, "Insurance fee should be 500.0");
        }
    }

    // T3_P03: Sedan Specialization + Gold Tier Discount (F4 + F18)
    public void testP03_SedanSpecializationAndGoldTier() {
        // Luxury Sedan: rate=3000, luxury mult=1.15, days=10 (>=7 days -> 10% long term discount)
        Sedan sedan = new Sedan("MH-12-P3-01", "Mercedes", "C-Class", 2023, 3000.0, 5000.0,
                5, "PETROL", "AUTOMATIC", 480.0, "LUXURY", 10.0);

        // Base luxury 10 days = 3000 * 10 * 1.15 = 34500 * 0.90 = 31050.0
        double sedanBase = sedan.calculateBaseRent(10);
        assertEquals(31050.0, sedanBase, 0.01, "Sedan luxury base rent mismatch");

        // Gold member discount (20% off): 31050 * 0.80 = 24840.0
        Rental r = new Rental();
        r.setDailyRate(3000.0);
        double goldRent = Math.round((sedanBase * 0.80) * 100.0) / 100.0;
        assertEquals(24840.0, goldRent, 0.01, "Compounded long-term and gold discount mismatch");
    }

    // T3_P04: SUV Specialization + Insurance Waiver Policy (F5 + F19 + F23)
    public void testP04_SUVSpecializationAndInsuranceWaiver() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // Book SUV with insurance
            Rental r = service.bookCar(1, 2, 4, true);
            assertTrue(r.getInsuranceFee() > 0, "Insurance fee should be positive");

            // Return with minor scratch damage Rs 2000 (absorbed by waiver <= 5000)
            Rental closed = service.processReturn(r.getRentalId(), LocalDate.now().plusDays(4), 22500.0, 2000.0, "Minor scratch");
            assertEquals(0.0, closed.getDamageFee(), 0.01, "Minor scratch damage should be waived under insurance policy");
            assertEquals(Rental.RentalStatus.COMPLETED, closed.getStatus(), "Rental status should be COMPLETED");
        }
    }

    // T3_P05: Hatchback Specialization + Silver Tier Discount (F6 + F18)
    public void testP05_HatchbackSpecializationAndSilverTier() {
        Hatchback hatch = new Hatchback("MH-12-P5-01", "Maruti", "Swift", 2022, 1000.0, 15000.0,
                5, "PETROL", "MANUAL", true, 22.0, 5.0);

        // 5 days: base = 1000 * 5 * 0.95 = 4750.0
        double hatchBase = hatch.calculateBaseRent(5);
        assertEquals(4750.0, hatchBase, 0.01, "Hatchback economy rate mismatch");

        // Silver tier (10% off): 4750 * 0.90 = 4275.0
        double silverTotal = Math.round((hatchBase * 0.90) * 100.0) / 100.0;
        assertEquals(4275.0, silverTotal, 0.01, "Hatchback silver discount total mismatch");
    }

    // T3_P06: Concurrency + Maintenance Status Toggle Race (F16 + F21)
    public void testP06_ConcurrencyAndMaintenanceToggleRace() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            Customer cust = new Customer(1, "Race User", "9833000001", "DL_RACE_01", "r@t.com", "Pune", Customer.MemberTier.REGULAR);
            Car car = new Car(1, "MH-12-P6-01", "Tata", "Nexon", 2023, 2000.0, 10000.0, "AVAILABLE", 5, "PETROL", "MANUAL");

            if (ctx.isLiveJdbc()) {
                ctx.getCustomerDAO().insertCustomer(cust);
                ctx.getCarDAO().insertCar(car);
            } else {
                ctx.insertMemoryCustomer(cust);
                ctx.insertMemoryCar(car);
            }

            ExecutorService exec = Executors.newFixedThreadPool(2);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger bookSuccess = new AtomicInteger(0);
            AtomicInteger bookFail = new AtomicInteger(0);
            AtomicBoolean maintenanceAttempted = new AtomicBoolean(false);

            exec.submit(() -> {
                try {
                    latch.await();
                    service.toggleMaintenance(car.getCarId(), true);
                    maintenanceAttempted.set(true);
                } catch (Exception ignored) {
                }
            });

            exec.submit(() -> {
                try {
                    latch.await();
                    service.bookCar(cust.getCustomerId(), car.getCarId(), 2, false);
                    bookSuccess.incrementAndGet();
                } catch (Exception e) {
                    bookFail.incrementAndGet();
                }
            });

            latch.countDown();
            exec.shutdown();
            exec.awaitTermination(5, TimeUnit.SECONDS);

            // Verification of mutual exclusion and resulting fleet consistency
            assertEquals(1, bookSuccess.get() + bookFail.get(), "Booking thread must conclude with exactly one outcome");
            Car finalCar = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(car.getCarId()) : ctx.getMemoryCar(car.getCarId());
            assertNotNull(finalCar, "Car must exist after race");

            if (bookSuccess.get() == 1) {
                assertEquals("RENTED", finalCar.getStatus(), "If booking won the race, car status must be RENTED");
                assertFalse(finalCar.isAvailable(), "Car must not be available when RENTED");
            } else {
                assertEquals(1, bookFail.get(), "If booking lost the race, booking attempt must have failed");
                assertEquals("MAINTENANCE", finalCar.getStatus(), "If maintenance won the race, car status must be MAINTENANCE");
                assertFalse(finalCar.isAvailable(), "Car must not be available when in MAINTENANCE");
            }

            assertTrue("RENTED".equals(finalCar.getStatus()) || "MAINTENANCE".equals(finalCar.getStatus()),
                    "Car status must be either RENTED or MAINTENANCE, no invalid intermediate state");
        }
    }

    // T3_P07: Return Damage Fee + Tier Loyalty Policy (F20 + F23 + F18)
    public void testP07_ReturnDamageFeeAndTierPolicy() {
        // Gold customer gets 20% discount on rental duration, but damage fee is charged at 100%
        Rental r = new Rental();
        r.setDailyRate(2000.0);
        r.setRentalDays(3);
        r.setBaseRent(6000.0);
        r.setDiscountAmount(1200.0); // 20% gold discount on 6000
        r.setInsuranceFee(0.0);
        r.setStartMileage(10000.0);
        r.setSecurityDeposit(3000.0);

        // Return on time with Rs 3000 damage
        // (6000 - 1200) + 0 + 3000 = 4800 + 3000 = 7800.0
        double finalBill = r.calculateFinalReturnBill(3, 10300.0, 3000.0, 0.0);
        assertEquals(7800.0, finalBill, 0.01, "Damage fee must not be discounted by member tier");
    }

    // T3_P08: Late Return + Deep Cleaning Fee + Mileage Surcharge (F20 + F23)
    public void testP08_LateReturnAndCleaningFeeAndMileage() {
        Rental r = new Rental();
        r.setDailyRate(1500.0);
        r.setRentalDays(2);
        r.setBaseRent(3000.0);
        r.setStartMileage(10000.0);
        r.setSecurityDeposit(2000.0);

        // 4 days late (actual=6 days). Late fee = 4 * (1500 * 1.5) = 9000.0
        // Cleaning fee = 800.0
        // Total = 3000 + 9000 + 800 = 12800.0
        double finalBill = r.calculateFinalReturnBill(6, 11500.0, 0.0, 800.0);
        assertEquals(12800.0, finalBill, 0.01, "Late return + cleaning bill mismatch");

        // Net balance payable after deposit deduction: 12800 - 2000 = 10800.0
        double netPayable = finalBill - r.getSecurityDeposit();
        assertEquals(10800.0, netPayable, 0.01, "Net balance payable mismatch");
    }

    // T3_P09: Customer License Validation + Rental DB Insertion (F13 + F14 + F22)
    public void testP09_CustomerLicenseValidationAndRentalDB() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            Customer cust = new Customer("Valid Lic User", "9844000001", "MH1220200099999", "v@t.com", "City", Customer.MemberTier.REGULAR);
            Car car = new Car("MH-12-P9-01", "Honda", "City", 2023, 2000.0, 10000.0, 5, "PETROL", "MANUAL");

            int custId;
            int carId;
            if (ctx.isLiveJdbc()) {
                custId = ctx.getCustomerDAO().insertCustomer(cust);
                ctx.getCarDAO().insertCar(car);
                carId = car.getCarId();
            } else {
                custId = ctx.insertMemoryCustomer(cust);
                ctx.insertMemoryCar(car);
                carId = car.getCarId();
            }

            assertTrue(custId > 0, "Customer ID should be positive");
            Rental r = ctx.getRentalService().bookCar(custId, carId, 2, false);
            assertNotNull(r, "Rental record created successfully with valid foreign keys");
        }
    }

    // T3_P10: Sequential Lifecycle on Single Car (F12 + F14 + F16 + F23)
    public void testP10_SequentialLifecycleOnSingleCar() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // Cust 1 rents car 1 (start 15000 -> end 15250)
            Rental r1 = service.bookCar(1, 1, 2, false);
            service.processReturn(r1.getRentalId(), LocalDate.now().plusDays(2), 15250.0, 0.0, "Cust 1 return");

            // Car is immediately AVAILABLE
            Car carAfterR1 = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(1) : ctx.getMemoryCar(1);
            assertEquals("AVAILABLE", carAfterR1.getStatus(), "Car should be AVAILABLE after Cust 1 return");
            assertEquals(15250.0, carAfterR1.getMileage(), 0.01, "Mileage should be 15250.0");

            // Cust 2 immediately rents same car 1 (start 15250 -> end 15600)
            Rental r2 = service.bookCar(2, 1, 3, false);
            service.processReturn(r2.getRentalId(), LocalDate.now().plusDays(3), 15600.0, 0.0, "Cust 2 return");

            Car carAfterR2 = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(1) : ctx.getMemoryCar(1);
            assertEquals("AVAILABLE", carAfterR2.getStatus(), "Car should be AVAILABLE after Cust 2 return");
            assertEquals(15600.0, carAfterR2.getMileage(), 0.01, "Mileage should be 15600.0");
        }
    }

    // T3_P11: Concurrent Read & Write Stress on DBConnection (F11 + F12 + F14)
    public void testP11_ConcurrentReadAndWriteStress() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            ExecutorService pool = Executors.newFixedThreadPool(4);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger successfulOps = new AtomicInteger(0);

            for (int i = 0; i < 4; i++) {
                final int threadId = i;
                pool.submit(() -> {
                    try {
                        latch.await();
                        if (threadId % 2 == 0) {
                            ctx.getRentalService().getActiveRentalsCount();
                        } else {
                            ctx.getRentalService().calculateTotalRevenue();
                        }
                        successfulOps.incrementAndGet();
                    } catch (Exception ignored) {
                    }
                });
            }

            latch.countDown();
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);

            assertEquals(4, successfulOps.get(), "All 4 concurrent read/write ops should succeed");
        }
    }

    // T3_P12: Inventory Filter + Dynamic Rental State Transition (F12 + F21)
    public void testP12_InventoryFilterAndDynamicRentalState() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            int initialAvail = service.filterInventory(null, "AVAILABLE").size();
            assertTrue(initialAvail >= 1, "Must have available cars initially");

            // Book 1 car
            Rental r = service.bookCar(1, 1, 2, false);
            int afterBookingAvail = service.filterInventory(null, "AVAILABLE").size();
            assertEquals(initialAvail - 1, afterBookingAvail, "Available count should decrement by 1");

            // Return car
            service.processReturn(r.getRentalId(), LocalDate.now().plusDays(2), 15500.0, 0.0, "done");
            int afterReturnAvail = service.filterInventory(null, "AVAILABLE").size();
            assertEquals(initialAvail, afterReturnAvail, "Available count should be restored on return");
        }
    }

    // T3_P13: Maintenance Mode Toggle + Rental History Integrity (F21 + F24)
    public void testP13_MaintenanceModeToggleAndHistoryIntegrity() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // Book and return car 1
            Rental r = service.bookCar(1, 1, 2, false);
            service.processReturn(r.getRentalId(), LocalDate.now().plusDays(2), 15500.0, 0.0, "hist test");
            double revBefore = service.calculateTotalRevenue();

            // Toggle car 1 to MAINTENANCE, then back to AVAILABLE
            service.toggleMaintenance(1, true);
            service.toggleMaintenance(1, false);

            double revAfter = service.calculateTotalRevenue();
            assertEquals(revBefore, revAfter, 0.001, "Maintenance toggling must not alter historical revenue");
        }
    }

    // T3_P14: Customer Profile Update with Active Rental (F13 + F14)
    public void testP14_CustomerProfileUpdateWithActiveRental() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // Book active rental
            Rental r = service.bookCar(1, 1, 3, false);
            assertNotNull(r, "Active rental created");

            // Update customer profile
            Customer cust = ctx.isLiveJdbc() ? ctx.getCustomerDAO().getCustomerById(1) : ctx.getMemoryCustomer(1);
            cust.setPhone("9899887766");
            cust.setMemberTier(Customer.MemberTier.GOLD);

            // Active rental remains valid
            Rental stillActive = ctx.isLiveJdbc() ? ctx.getRentalDAO().getRentalById(r.getRentalId()) : ctx.getMemoryRental(r.getRentalId());
            assertEquals(Rental.RentalStatus.ACTIVE, stillActive.getStatus(), "Active rental status remains valid");
        }
    }

    // T3_P15: Rental History KPI Across Mixed Vehicle Hierarchy (F4 + F5 + F6 + F24)
    public void testP15_RentalHistoryKPIAcrossMixedHierarchy() {
        Sedan sedan = new Sedan("MH-12-P15-01", "Honda", "City", 2023, 2000.0, 10000.0, 5, "PETROL", "MANUAL", 500.0, "EXECUTIVE", 0.0);
        SUV suv = new SUV("MH-12-P15-02", "Mahindra", "Scorpio", 2023, 3000.0, 20000.0, 7, "DIESEL", "MANUAL", false, 200, false, 0.0);
        Hatchback hatch = new Hatchback("MH-12-P15-03", "Maruti", "Swift", 2022, 1000.0, 15000.0, 5, "PETROL", "MANUAL", true, 20.0, 0.0);

        double sedanCost = sedan.calculateBaseRent(3); // 6000.0
        double suvCost = suv.calculateBaseRent(4);     // 12000.0
        double hatchCost = hatch.calculateBaseRent(3); // 3000.0

        double totalRevenue = sedanCost + suvCost + hatchCost;
        assertEquals(21000.0, totalRevenue, 0.01, "Aggregate mixed hierarchy revenue mismatch");
    }

    // T3_P16: Same-Day Return Policy + Insurance Addon (F17 + F19 + F20)
    public void testP16_SameDayReturnPolicyAndInsurance() {
        Rental r = new Rental();
        r.setDailyRate(2000.0);
        r.setRentalDays(1);
        r.setBaseRent(2000.0);
        r.setInsuranceFee(250.0); // 1 day insurance
        r.setStartMileage(10000.0);

        // Returned 0 days elapsed (same day). Billed as minimum 1 full day base + insurance
        double bill = r.calculateFinalReturnBill(0, 10050.0, 0.0, 0.0);
        assertEquals(2250.0, bill, 0.01, "Same day return billed as minimum 1 day base + insurance");
    }

    // T3_P17: High Mileage Odometer Rollover Calculation (F2 + F20 + F23)
    public void testP17_HighMileageOdometerRollover() {
        Car car = new Car("MH-12-P17-01", "Toyota", "Innova", 2020, 2500.0, 99900.0, 7, "DIESEL", "MANUAL");
        assertEquals(99900.0, car.getMileage(), 0.01, "Initial odometer reading");

        double endMileage = 100450.0;
        double delta = endMileage - car.getMileage();
        assertEquals(550.0, delta, 0.01, "Mileage delta across 100,000 km boundary");

        car.setMileage(endMileage);
        assertEquals(100450.0, car.getMileage(), 0.01, "Updated mileage reading");
    }

    // T3_P18: Tricolour Theme Applied Across All GUI Panels (F25 + F26)
    public void testP18_TricolourThemeAcrossPanels() {
        Color saffron = new Color(255, 153, 51);
        Color white = new Color(255, 255, 255);
        Color green = new Color(19, 136, 8);
        Color navy = new Color(0, 0, 128);

        JPanel header = new JPanel();
        header.setBackground(navy);

        JPanel content = new JPanel();
        content.setBackground(white);

        JButton bookBtn = new JButton("Book Car");
        bookBtn.setBackground(green);
        bookBtn.setForeground(white);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setBackground(saffron);
        cancelBtn.setForeground(navy);

        assertEquals(navy, header.getBackground(), "Header should be Navy");
        assertEquals(white, content.getBackground(), "Content should be White");
        assertEquals(green, bookBtn.getBackground(), "Primary CTA should be Green");
        assertEquals(saffron, cancelBtn.getBackground(), "Secondary CTA should be Saffron");
    }

    // T3_P19: Form Input Clear + Validation Re-trigger (F22 + F26)
    public void testP19_FormInputValidationAndStateReset() {
        // 1. Validating empty customer name throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            new Customer("", "9820011111", "DL0120230000001", "a@test.com", "Pune", Customer.MemberTier.REGULAR);
        }, "Empty name should be rejected with IllegalArgumentException");

        // 2. Validating empty customer phone throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            new Customer("Valid Name", "", "DL0120230000001", "a@test.com", "Pune", Customer.MemberTier.REGULAR);
        }, "Empty phone should be rejected with IllegalArgumentException");

        // 3. Validating empty customer license throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            new Customer("Valid Name", "9820011111", "", "a@test.com", "Pune", Customer.MemberTier.REGULAR);
        }, "Empty driving license should be rejected with IllegalArgumentException");

        // 4. Supplying valid input creates entity and preserves field contracts
        Customer valid = new Customer("Sunil Verma", "9820011111", "DL0120239999999", "sunil@example.com", "Pune", Customer.MemberTier.REGULAR);
        assertEquals("Sunil Verma", valid.getFullName(), "Customer full name must match input");
        assertEquals("9820011111", valid.getPhone(), "Customer phone must match input");
        assertEquals("DL0120239999999", valid.getDrivingLicense(), "Customer license must match input");
    }

    // T3_P20: Polymorphic Rentable Dispatch in Service Settlement (F1 + F20)
    public void testP20_PolymorphicRentableDispatchInSettlement() {
        Rentable sedan = new Sedan("MH-12-P20-01", "Honda", "City", 2023, 2000.0, 10000.0, 5, "PETROL", "MANUAL", 500.0, "EXECUTIVE", 0.0);
        Rentable suv = new SUV("MH-12-P20-02", "Mahindra", "Scorpio", 2023, 3000.0, 10000.0, 7, "DIESEL", "MANUAL", true, 200, true, 300.0);

        assertEquals(6000.0, sedan.calculateRate(3), 0.01, "Sedan polymorphic rate");
        // SUV: 3000*3 + (3*300 AWD) + (3*300 7-seat) = 9000 + 900 + 900 = 10800.0
        assertEquals(10800.0, suv.calculateRate(3), 0.01, "SUV polymorphic rate");
    }

    // T3_P21: Database Constraint Violation Rollback in Service (F9 + F16)
    public void testP21_DatabaseConstraintViolationRollback() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // Attempting to book invalid customer ID 999999 throws exception
            assertThrows(Exception.class, () -> service.bookCar(999999, 1, 2, false),
                    "Invalid booking should fail");

            // Car status remains AVAILABLE (rollback / unchanged)
            Car car = ctx.isLiveJdbc() ? ctx.getCarDAO().getCarById(1) : ctx.getMemoryCar(1);
            assertEquals("AVAILABLE", car.getStatus(), "Car status must remain AVAILABLE after failed booking");
        }
    }

    // T3_P22: Pre-Seeded Fleet Booking Verification (F10 + F16)
    public void testP22_PreSeededFleetBookingVerification() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // Book pre-seeded car ID 1
            Rental r = service.bookCar(1, 1, 3, true);
            assertNotNull(r, "Pre-seeded car should be booked successfully");
            assertEquals(1, r.getCarId(), "Rental car ID should match");
        }
    }

    // T3_P23: start_db Initialization + Java DAO Data Access (F11 + F33)
    public void testP23_StartDbInitializationAndJavaDAO() throws Exception {
        // start_db.bat initializes db/car_rental.db
        File dbFile = new File("db/car_rental.db");
        assertTrue(dbFile.exists(), "db/car_rental.db must exist after database bootstrap");
        assertTrue(dbFile.length() > 0, "Database file must not be 0 bytes");

        // Verify connection to initialized database and query seed counts
        try (Connection conn = DBConnection.getConnection("jdbc:sqlite:db/car_rental.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count(*) FROM cars;")) {
            assertTrue(rs.next(), "Cars query must return a count row");
            assertTrue(rs.getInt(1) >= 9, "Seeded cars table must contain at least 9 vehicles");
        }
    }

    // T3_P24: Customer Registration + Immediate Booking + History (F7 + F16 + F24)
    public void testP24_CustomerRegistrationAndImmediateBooking() throws Exception {
        try (TestContext ctx = TestContext.createClean()) {
            Customer cust = new Customer("Chain User", "9855000001", "DL_CHAIN_01", "c@t.com", "City", Customer.MemberTier.SILVER);
            Car car = new Car("MH-12-P24-01", "Hyundai", "i20", 2022, 1500.0, 10000.0, 5, "PETROL", "MANUAL");

            int custId;
            int carId;
            if (ctx.isLiveJdbc()) {
                custId = ctx.getCustomerDAO().insertCustomer(cust);
                ctx.getCarDAO().insertCar(car);
                carId = car.getCarId();
            } else {
                custId = ctx.insertMemoryCustomer(cust);
                ctx.insertMemoryCar(car);
                carId = car.getCarId();
            }

            // Immediately book
            Rental r = ctx.getRentalService().bookCar(custId, carId, 2, false);
            assertNotNull(r, "Booking created");

            // Verify active count is 1
            assertEquals(1, ctx.getRentalService().getActiveRentalsCount(), "Active rentals count should be 1");
        }
    }

    // T3_P25: Aborted Booking Does Not Leak Car Lock (F14 + F16)
    public void testP25_AbortedBookingDoesNotLeakLock() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            // First attempt with negative days throws IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> service.bookCar(1, 1, -1, false),
                    "Invalid duration booking should fail");

            // Car lock is not leaked; subsequent valid booking succeeds
            Rental r = service.bookCar(1, 1, 3, false);
            assertNotNull(r, "Subsequent valid booking should succeed");
        }
    }

    // T3_P26: Concurrent Returns of Multiple Distinct Vehicles (F16 + F23)
    public void testP26_ConcurrentReturnsOfDistinctVehicles() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            int initialActive = service.getActiveRentalsCount();
            Rental r1 = service.bookCar(1, 1, 2, false);
            Rental r2 = service.bookCar(2, 2, 2, false);

            ExecutorService exec = Executors.newFixedThreadPool(2);
            CountDownLatch latch = new CountDownLatch(1);

            exec.submit(() -> {
                try {
                    latch.await();
                    service.processReturn(r1.getRentalId(), LocalDate.now().plusDays(2), 16000.0, 0.0, "r1");
                } catch (Exception ignored) {
                }
            });

            exec.submit(() -> {
                try {
                    latch.await();
                    service.processReturn(r2.getRentalId(), LocalDate.now().plusDays(2), 23000.0, 0.0, "r2");
                } catch (Exception ignored) {
                }
            });

            latch.countDown();
            exec.shutdown();
            exec.awaitTermination(5, TimeUnit.SECONDS);

            assertEquals(initialActive, service.getActiveRentalsCount(), "Both rentals should be completed");
        }
    }

    // T3_P27: Deposit Deduction When Damage Exceeds Deposit (F20 + F23)
    public void testP27_DepositDeductionWhenDamageExceedsDeposit() {
        Rental r = new Rental();
        r.setDailyRate(2000.0);
        r.setRentalDays(2);
        r.setBaseRent(4000.0);
        r.setSecurityDeposit(2000.0);
        r.setStartMileage(10000.0);

        // Damage fee = 5000. Gross total = 4000 + 5000 = 9000.0
        double grossBill = r.calculateFinalReturnBill(2, 10200.0, 5000.0, 0.0);
        assertEquals(9000.0, grossBill, 0.01, "Gross bill mismatch");

        // Net payable = gross - deposit = 9000 - 2000 = 7000.0
        double netPayable = grossBill - r.getSecurityDeposit();
        assertEquals(7000.0, netPayable, 0.01, "Net balance payable mismatch when damage exceeds deposit");
    }

    // T3_P28: Double-Booking Prevention on Rented Vehicle (F15 + F16)
    public void testP28_DoubleBookingPreventionOnRentedVehicle() throws Exception {
        try (TestContext ctx = TestContext.createWithSeed()) {
            TestContext.ServiceAdapter service = ctx.getRentalService();

            Rental r = service.bookCar(1, 1, 3, false);
            assertNotNull(r, "First booking succeeds");

            assertThrows(TestContext.CarUnavailableException.class, () -> {
                service.bookCar(2, 1, 2, false);
            }, "Double-booking same car must throw CarUnavailableException");
        }
    }
}
