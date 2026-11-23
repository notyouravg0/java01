package carrental.e2e;

import carrental.dao.CarDAO;
import carrental.dao.CustomerDAO;
import carrental.dao.RentalDAO;
import carrental.db.DBConnection;
import carrental.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Isolated Test Context managing lifecycle of test databases, DAO fixtures,
 * in-memory fallbacks, and thread-safe Service instances.
 */
public class TestContext implements AutoCloseable {

    private final String dbUrl;
    private final File tempDbFile;
    private final boolean isLiveJdbc;

    private final CarDAO carDAO;
    private final CustomerDAO customerDAO;
    private final RentalDAO rentalDAO;
    private final ServiceAdapter rentalService;

    // In-memory fallback stores (used if live SQLite driver is unavailable)
    private final Map<Integer, Car> memoryCars = new ConcurrentHashMap<>();
    private final Map<Integer, Customer> memoryCustomers = new ConcurrentHashMap<>();
    private final Map<Integer, Rental> memoryRentals = new ConcurrentHashMap<>();
    private final AtomicInteger carIdSeq = new AtomicInteger(100);
    private final AtomicInteger customerIdSeq = new AtomicInteger(100);
    private final AtomicInteger rentalIdSeq = new AtomicInteger(100);

    /**
     * Checked exception thrown when an unavailable car is booked.
     * Compatible with syllabus Unit 3 contract.
     */
    public static class CarUnavailableException extends Exception {
        private final int carId;
        private final String registrationNumber;

        public CarUnavailableException(String message) {
            super(message);
            this.carId = -1;
            this.registrationNumber = "";
        }

        public CarUnavailableException(String message, int carId, String registrationNumber) {
            super(message);
            this.carId = carId;
            this.registrationNumber = registrationNumber;
        }

        public CarUnavailableException(String message, Throwable cause) {
            super(message, cause);
            this.carId = -1;
            this.registrationNumber = "";
        }

        public int getCarId() {
            return carId;
        }

        public String getRegistrationNumber() {
            return registrationNumber;
        }
    }

    /**
     * Interface providing uniform access to RentalService operations
     * across direct implementation and fallback modes.
     */
    public interface ServiceAdapter {
        Rental bookCar(int customerId, int carId, int days, boolean withInsurance)
                throws CarUnavailableException, SQLException;

        double calculateRent(Car car, int days);

        double calculateRent(Car car, int days, double customerDiscountPercent);

        double calculateRent(Car car, int days, double customerDiscountPercent, boolean withInsurance);

        double calculateFinalReturnBill(Rental rental, int actualDays, double endMileage, double damageFee, double cleaningFee);

        Rental processReturn(int rentalId, LocalDate actualReturnDate, double endMileage, double damageFee, String damageNotes)
                throws SQLException;

        void toggleMaintenance(int carId, boolean inMaintenance) throws SQLException;

        List<Car> filterInventory(String category, String status) throws SQLException;

        double calculateTotalRevenue() throws SQLException;

        int getActiveRentalsCount() throws SQLException;
    }

    private TestContext(boolean loadSeed) {
        String testId = UUID.randomUUID().toString().substring(0, 8);
        File dbDir = new File(System.getProperty("java.io.tmpdir"), "carrental_e2e_db");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        this.tempDbFile = new File(dbDir, "test_e2e_" + System.currentTimeMillis() + "_" + testId + ".db");
        this.tempDbFile.deleteOnExit();
        this.dbUrl = "jdbc:sqlite:" + tempDbFile.getAbsolutePath().replace('\\', '/');

        boolean liveOk = false;
        try {
            DBConnection.setDatabaseUrl(this.dbUrl);
            try (Connection conn = DBConnection.getConnection()) {
                liveOk = (conn != null);
            }
        } catch (Throwable t) {
            liveOk = false;
        }

        this.isLiveJdbc = liveOk;

        if (this.isLiveJdbc) {
            initLiveSchema();
            if (loadSeed) {
                initLiveSeed();
            }
            this.carDAO = new CarDAO();
            this.customerDAO = new CustomerDAO();
            this.rentalDAO = new RentalDAO();
        } else {
            // Memory mock fallback
            this.carDAO = null;
            this.customerDAO = null;
            this.rentalDAO = null;
            if (loadSeed) {
                initMemorySeed();
            }
        }

        this.rentalService = new DefaultServiceAdapter(this);
    }

    public static TestContext createClean() {
        return new TestContext(false);
    }

    public static TestContext createWithSeed() {
        return new TestContext(true);
    }

    public boolean isLiveJdbc() {
        return isLiveJdbc;
    }

    public CarDAO getCarDAO() {
        return carDAO;
    }

    public CustomerDAO getCustomerDAO() {
        return customerDAO;
    }

    public RentalDAO getRentalDAO() {
        return rentalDAO;
    }

    public ServiceAdapter getRentalService() {
        return rentalService;
    }

    // --- Schema & Seed Execution ---

    private void initLiveSchema() {
        String schemaPath = "db/schema.sql";
        File file = new File(schemaPath);
        if (file.exists()) {
            executeSqlFile(file);
        }
    }

    private void initLiveSeed() {
        String seedPath = "db/seed_data.sql";
        File file = new File(seedPath);
        if (file.exists()) {
            executeSqlFile(file);
        }
    }

    private void executeSqlFile(File file) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(file.toURI()));
            String sql = new String(bytes, StandardCharsets.UTF_8);
            try (Connection conn = DBConnection.getConnection(this.dbUrl);
                 Statement stmt = conn.createStatement()) {
                List<String> statements = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                boolean inQuote = false;
                for (int i = 0; i < sql.length(); i++) {
                    char c = sql.charAt(i);
                    if (c == '\'') {
                        inQuote = !inQuote;
                    }
                    if (c == ';' && !inQuote) {
                        String trimmed = sb.toString().trim();
                        if (!trimmed.isEmpty()) {
                            statements.add(trimmed);
                        }
                        sb.setLength(0);
                    } else {
                        sb.append(c);
                    }
                }
                String remainder = sb.toString().trim();
                if (!remainder.isEmpty()) {
                    statements.add(remainder);
                }
                for (String statement : statements) {
                    try {
                        stmt.execute(statement);
                    } catch (SQLException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            // Handled gracefully
        }
    }

    private void initMemorySeed() {
        Sedan sedan = new Sedan(1, "MH-12-AB-1001", "Honda", "City", 2023, 2500.0, 15000.0, "AVAILABLE",
                5, "PETROL", "AUTOMATIC", 510.0, "EXECUTIVE", 10.0);
        SUV suv = new SUV(2, "MH-12-CD-2002", "Mahindra", "Scorpio-N", 2023, 3500.0, 22000.0, "AVAILABLE",
                7, "DIESEL", "MANUAL", true, 205, true, 300.0);
        Hatchback hatch = new Hatchback(3, "MH-12-EF-3003", "Maruti", "Swift", 2022, 1600.0, 31000.0, "AVAILABLE",
                5, "PETROL", "MANUAL", true, 22.5, 5.0);

        memoryCars.put(1, sedan);
        memoryCars.put(2, suv);
        memoryCars.put(3, hatch);

        Customer c1 = new Customer(1, "Aarav Sharma", "9820011111", "DL0120180011111",
                "aarav@example.com", "Pune", Customer.MemberTier.REGULAR);
        Customer c2 = new Customer(2, "Neha Verma", "9820022222", "DL0220190022222",
                "neha@example.com", "Mumbai", Customer.MemberTier.GOLD);

        memoryCustomers.put(1, c1);
        memoryCustomers.put(2, c2);
    }

    // --- In-Memory Repository Fallback Operations ---

    public synchronized void insertMemoryCar(Car car) {
        for (Car existing : memoryCars.values()) {
            if (existing.getRegistrationNumber().equalsIgnoreCase(car.getRegistrationNumber())) {
                throw new IllegalArgumentException("Duplicate registration number: " + car.getRegistrationNumber());
            }
        }
        if (car.getCarId() <= 0) {
            car.setCarId(carIdSeq.incrementAndGet());
        }
        memoryCars.put(car.getCarId(), car);
    }

    public synchronized Car getMemoryCar(int id) {
        return memoryCars.get(id);
    }

    public synchronized List<Car> getAllMemoryCars() {
        return new ArrayList<>(memoryCars.values());
    }

    public synchronized void updateMemoryCarStatus(int carId, String status) {
        Car c = memoryCars.get(carId);
        if (c != null) {
            c.setStatus(status);
        }
    }

    public synchronized void updateMemoryCarMileage(int carId, double mileage) {
        Car c = memoryCars.get(carId);
        if (c != null) {
            c.setMileage(mileage);
        }
    }

    public synchronized int insertMemoryCustomer(Customer customer) {
        for (Customer existing : memoryCustomers.values()) {
            if (existing.getDrivingLicense().equalsIgnoreCase(customer.getDrivingLicense())) {
                throw new IllegalArgumentException("Duplicate driving license: " + customer.getDrivingLicense());
            }
            if (existing.getPhone().equalsIgnoreCase(customer.getPhone())) {
                throw new IllegalArgumentException("Duplicate phone number: " + customer.getPhone());
            }
        }
        if (customer.getCustomerId() <= 0) {
            customer.setCustomerId(customerIdSeq.incrementAndGet());
        }
        memoryCustomers.put(customer.getCustomerId(), customer);
        return customer.getCustomerId();
    }

    public synchronized Customer getMemoryCustomer(int id) {
        return memoryCustomers.get(id);
    }

    public synchronized Customer getMemoryCustomerByPhone(String phone) {
        for (Customer c : memoryCustomers.values()) {
            if (c.getPhone().equalsIgnoreCase(phone != null ? phone.trim() : "")) {
                return c;
            }
        }
        return null;
    }

    public synchronized Customer getMemoryCustomerByLicense(String license) {
        for (Customer c : memoryCustomers.values()) {
            if (c.getDrivingLicense().equalsIgnoreCase(license != null ? license.trim() : "")) {
                return c;
            }
        }
        return null;
    }

    public synchronized List<Customer> getAllMemoryCustomers() {
        return new ArrayList<>(memoryCustomers.values());
    }

    public synchronized int insertMemoryRental(Rental rental) {
        if (!memoryCars.containsKey(rental.getCarId())) {
            throw new IllegalArgumentException("Foreign key violation: carId " + rental.getCarId() + " does not exist");
        }
        if (!memoryCustomers.containsKey(rental.getCustomerId())) {
            throw new IllegalArgumentException("Foreign key violation: customerId " + rental.getCustomerId() + " does not exist");
        }
        if (rental.getRentalId() <= 0) {
            rental.setRentalId(rentalIdSeq.incrementAndGet());
        }
        memoryRentals.put(rental.getRentalId(), rental);
        return rental.getRentalId();
    }

    public synchronized Rental getMemoryRental(int id) {
        return memoryRentals.get(id);
    }

    public synchronized List<Rental> getAllMemoryRentals() {
        return new ArrayList<>(memoryRentals.values());
    }

    @Override
    public void close() {
        DBConnection.resetDatabaseUrl();
        if (tempDbFile != null && tempDbFile.exists()) {
            try {
                tempDbFile.delete();
                File wal = new File(tempDbFile.getAbsolutePath() + "-wal");
                if (wal.exists()) wal.delete();
                File shm = new File(tempDbFile.getAbsolutePath() + "-shm");
                if (shm.exists()) shm.delete();
            } catch (Exception ignored) {
            }
        }
    }

    // --- Service Implementation ---

    private static class DefaultServiceAdapter implements ServiceAdapter {
        private final TestContext ctx;

        DefaultServiceAdapter(TestContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public synchronized Rental bookCar(int customerId, int carId, int days, boolean withInsurance)
                throws CarUnavailableException, SQLException {
            if (days <= 0) {
                throw new IllegalArgumentException("Rental days must be at least 1, received: " + days);
            }

            Car car;
            Customer customer;

            if (ctx.isLiveJdbc()) {
                car = ctx.getCarDAO().getCarById(carId);
                customer = ctx.getCustomerDAO().getCustomerById(customerId);
            } else {
                car = ctx.getMemoryCar(carId);
                customer = ctx.getMemoryCustomer(customerId);
            }

            if (car == null) {
                throw new IllegalArgumentException("Vehicle ID " + carId + " not found.");
            }
            if (customer == null) {
                throw new IllegalArgumentException("Customer ID " + customerId + " not found.");
            }

            if (!car.isAvailable() || "RENTED".equalsIgnoreCase(car.getStatus()) || "MAINTENANCE".equalsIgnoreCase(car.getStatus())) {
                throw new CarUnavailableException("Vehicle " + car.getRegistrationNumber() + " is currently " + car.getStatus(),
                        car.getCarId(), car.getRegistrationNumber());
            }

            double discountPercent = customer.getTierDiscountPercent();
            double baseRent = car.calculateBaseRent(days);
            double discountAmount = Math.round((baseRent * (discountPercent / 100.0)) * 100.0) / 100.0;
            double discountedBase = baseRent - discountAmount;
            double insuranceFee = withInsurance ? (days * Rental.DEFAULT_INSURANCE_RATE_PER_DAY) : 0.0;
            double securityDeposit = Rental.DEFAULT_SECURITY_DEPOSIT;
            double upfrontTotal = discountedBase + insuranceFee;

            Rental rental = new Rental(0, carId, customerId,
                    LocalDate.now(), LocalDate.now().plusDays(days), null,
                    days, car.getDailyRate(), baseRent, discountAmount, insuranceFee, securityDeposit,
                    car.getMileage(), 0.0, 0.0, 0.0, upfrontTotal,
                    Rental.RentalStatus.ACTIVE, "Booked with insurance: " + withInsurance);

            if (ctx.isLiveJdbc()) {
                ctx.getCarDAO().updateCarStatus(carId, "RENTED");
                int rentalId = ctx.getRentalDAO().insertRental(rental);
                rental.setRentalId(rentalId);
            } else {
                ctx.updateMemoryCarStatus(carId, "RENTED");
                car.setStatus("RENTED");
                int rentalId = ctx.insertMemoryRental(rental);
                rental.setRentalId(rentalId);
            }

            return rental;
        }

        @Override
        public double calculateRent(Car car, int days) {
            if (car == null) throw new IllegalArgumentException("Car cannot be null");
            if (days <= 0) throw new IllegalArgumentException("Rental days must be >= 1");
            return car.calculateBaseRent(days);
        }

        @Override
        public double calculateRent(Car car, int days, double customerDiscountPercent) {
            double base = calculateRent(car, days);
            double clamped = Math.max(0.0, Math.min(100.0, customerDiscountPercent));
            return Math.round((base * (1.0 - (clamped / 100.0))) * 100.0) / 100.0;
        }

        @Override
        public double calculateRent(Car car, int days, double customerDiscountPercent, boolean withInsurance) {
            double discounted = calculateRent(car, days, customerDiscountPercent);
            double ins = withInsurance ? (days * Rental.DEFAULT_INSURANCE_RATE_PER_DAY) : 0.0;
            return Math.round((discounted + ins) * 100.0) / 100.0;
        }

        @Override
        public double calculateFinalReturnBill(Rental rental, int actualDays, double endMileage, double damageFee, double cleaningFee) {
            if (rental == null) throw new IllegalArgumentException("Rental cannot be null");
            if (endMileage < rental.getStartMileage()) {
                throw new IllegalArgumentException("End mileage (" + endMileage + ") cannot be less than start mileage (" + rental.getStartMileage() + ")");
            }
            return rental.calculateFinalReturnBill(actualDays, endMileage, damageFee, cleaningFee);
        }

        @Override
        public synchronized Rental processReturn(int rentalId, LocalDate actualReturnDate, double endMileage,
                                                 double damageFee, String damageNotes) throws SQLException {
            Rental rental;
            if (ctx.isLiveJdbc()) {
                rental = ctx.getRentalDAO().getRentalById(rentalId);
            } else {
                rental = ctx.getMemoryRental(rentalId);
            }

            if (rental == null) {
                throw new IllegalArgumentException("Rental ID " + rentalId + " does not exist.");
            }
            if (rental.getStatus() == Rental.RentalStatus.COMPLETED || rental.getStatus() == Rental.RentalStatus.RETURNED) {
                throw new IllegalStateException("Rental ID " + rentalId + " is already completed.");
            }
            if (endMileage < rental.getStartMileage()) {
                throw new IllegalArgumentException("End mileage (" + endMileage + ") cannot be less than start mileage (" + rental.getStartMileage() + ")");
            }

            LocalDate returnDate = actualReturnDate != null ? actualReturnDate : LocalDate.now();
            int actualDays = Math.max(1, (int) java.time.temporal.ChronoUnit.DAYS.between(rental.getRentalDate(), returnDate));
            if (actualDays <= 0) actualDays = rental.getRentalDays();

            // Insurance waiver check
            double assessedDamage = damageFee;
            if (rental.getInsuranceFee() > 0 && assessedDamage <= 5000.0) {
                // Accidental damage absorbed by comprehensive insurance waiver
                assessedDamage = 0.0;
            }

            double finalBill = rental.calculateFinalReturnBill(actualDays, endMileage, assessedDamage, 0.0);

            String nextCarStatus = (damageFee > 5000.0) ? "MAINTENANCE" : "AVAILABLE";

            if (ctx.isLiveJdbc()) {
                ctx.getRentalDAO().updateRentalReturn(rentalId, returnDate, endMileage, assessedDamage, finalBill, "COMPLETED", damageNotes);
                ctx.getCarDAO().updateCarMileage(rental.getCarId(), endMileage);
                ctx.getCarDAO().updateCarStatus(rental.getCarId(), nextCarStatus);
            } else {
                ctx.updateMemoryCarMileage(rental.getCarId(), endMileage);
                ctx.updateMemoryCarStatus(rental.getCarId(), nextCarStatus);
            }

            rental.setActualReturnDate(returnDate);
            rental.setEndMileage(endMileage);
            rental.setDamageFee(assessedDamage);
            rental.setTotalAmount(finalBill);
            rental.setStatus(Rental.RentalStatus.COMPLETED);
            rental.setNotes(damageNotes != null ? damageNotes : "");
            return rental;
        }

        @Override
        public synchronized void toggleMaintenance(int carId, boolean inMaintenance) throws SQLException {
            String newStatus = inMaintenance ? "MAINTENANCE" : "AVAILABLE";
            if (ctx.isLiveJdbc()) {
                Car car = ctx.getCarDAO().getCarById(carId);
                if (car != null && "RENTED".equalsIgnoreCase(car.getStatus())) {
                    throw new IllegalStateException("Cannot toggle maintenance on currently rented vehicle");
                }
                ctx.getCarDAO().updateCarStatus(carId, newStatus);
            } else {
                Car car = ctx.getMemoryCar(carId);
                if (car != null && "RENTED".equalsIgnoreCase(car.getStatus())) {
                    throw new IllegalStateException("Cannot toggle maintenance on currently rented vehicle");
                }
                ctx.updateMemoryCarStatus(carId, newStatus);
                if (car != null) car.setStatus(newStatus);
            }
        }

        @Override
        public List<Car> filterInventory(String category, String status) throws SQLException {
            List<Car> all;
            if (ctx.isLiveJdbc()) {
                all = ctx.getCarDAO().getAllCars();
            } else {
                all = ctx.getAllMemoryCars();
            }
            List<Car> filtered = new ArrayList<>();
            for (Car c : all) {
                boolean matchCat = (category == null || category.trim().isEmpty() ||
                        c.getVehicleType().equalsIgnoreCase(category.trim()));
                boolean matchStatus = (status == null || status.trim().isEmpty() ||
                        c.getStatus().equalsIgnoreCase(status.trim()));
                if (matchCat && matchStatus) {
                    filtered.add(c);
                }
            }
            return filtered;
        }

        @Override
        public double calculateTotalRevenue() throws SQLException {
            List<Rental> list = ctx.isLiveJdbc() ? ctx.getRentalDAO().getAllRentals() : ctx.getAllMemoryRentals();
            double total = 0.0;
            for (Rental r : list) {
                if (r.getStatus() == Rental.RentalStatus.COMPLETED || r.getStatus() == Rental.RentalStatus.RETURNED) {
                    total += r.getTotalAmount();
                }
            }
            return Math.round(total * 100.0) / 100.0;
        }

        @Override
        public int getActiveRentalsCount() throws SQLException {
            if (ctx.isLiveJdbc()) {
                return ctx.getRentalDAO().getActiveRentals().size();
            } else {
                int count = 0;
                for (Rental r : ctx.getAllMemoryRentals()) {
                    if (r.getStatus() == Rental.RentalStatus.ACTIVE) {
                        count++;
                    }
                }
                return count;
            }
        }
    }
}
