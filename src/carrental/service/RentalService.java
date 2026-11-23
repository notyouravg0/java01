package carrental.service;

import carrental.dao.CarDAO;
import carrental.dao.CustomerDAO;
import carrental.dao.RentalDAO;
import carrental.exception.CarUnavailableException;
import carrental.model.Car;
import carrental.model.Customer;
import carrental.model.Rental;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central business service mediating between DAOs and UI/workflows.
 * Implements:
 * - Unit 1: Overloaded rent calculation methods.
 * - Unit 3: Thread-safe synchronized booking engine and CarUnavailableException handling.
 * - Module 1: Car inventory operations and maintenance toggle.
 * - Module 2: Customer registration with phone/license validation and uniqueness checks.
 * - Module 4: Vehicle return, mileage verification, insurance waiver, and damage settlement.
 * - Module 5: Rental audit history and aggregate KPI metrics.
 */
public class RentalService {

    private final CarDAO carDAO;
    private final CustomerDAO customerDAO;
    private final RentalDAO rentalDAO;

    /**
     * Default constructor for production and Swing UI usage.
     * Delegates to the dependency injection constructor with default DAOs.
     */
    public RentalService() {
        this(new CarDAO(), new CustomerDAO(), new RentalDAO());
    }

    /**
     * Dependency injection constructor for testing, mocking, and isolated DB instances.
     *
     * @param carDAO      Data access object for cars
     * @param customerDAO Data access object for customers
     * @param rentalDAO   Data access object for rentals
     */
    public RentalService(CarDAO carDAO, CustomerDAO customerDAO, RentalDAO rentalDAO) {
        this.carDAO = (carDAO != null) ? carDAO : new CarDAO();
        this.customerDAO = (customerDAO != null) ? customerDAO : new CustomerDAO();
        this.rentalDAO = (rentalDAO != null) ? rentalDAO : new RentalDAO();
    }

    // =========================================================================
    // 1. Concurrency & Booking Engine (Syllabus Unit 3)
    // =========================================================================

    /**
     * Thread-safe synchronized booking method that prevents race conditions,
     * verifies availability, computes upfront billing, and atomically records rentals
     * with compensating rollback.
     *
     * @param customerId    Primary key of the customer booking the vehicle
     * @param carId         Primary key of the vehicle being booked
     * @param days          Rental duration in days (must be >= 1)
     * @param withInsurance Whether optional insurance coverage is selected
     * @return Fully populated active Rental agreement
     * @throws CarUnavailableException If the vehicle is rented, in maintenance, or unavailable
     * @throws SQLException            If a database error occurs during query or insertion
     * @throws IllegalArgumentException If parameters are invalid or entities do not exist
     */
    public synchronized Rental bookCar(int customerId, int carId, int days, boolean withInsurance)
            throws CarUnavailableException, SQLException {
        // 1. Primitive parameter validation
        if (days <= 0) {
            throw new IllegalArgumentException("Rental days must be at least 1, received: " + days);
        }
        if (customerId <= 0) {
            throw new IllegalArgumentException("Invalid customer ID: " + customerId);
        }
        if (carId <= 0) {
            throw new IllegalArgumentException("Invalid car ID: " + carId);
        }

        // 2. Referential integrity & Entity Lookup
        Customer customer = customerDAO.getCustomerById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("Customer ID " + customerId + " not found.");
        }

        Car car = carDAO.getCarById(carId);
        if (car == null) {
            throw new IllegalArgumentException("Vehicle ID " + carId + " not found.");
        }

        // 3. Status verification (AVAILABLE vs RENTED vs MAINTENANCE)
        String currentStatus = (car.getStatus() != null) ? car.getStatus().trim().toUpperCase() : "UNKNOWN";
        if (!car.isAvailable() || !"AVAILABLE".equals(currentStatus)) {
            String msg = "Vehicle " + car.getRegistrationNumber() + " is currently " + currentStatus;
            throw new CarUnavailableException(msg, car.getCarId(), car.getRegistrationNumber(), currentStatus);
        }

        // 4. Billing & Discount Computation
        double discountPercent = customer.getTierDiscountPercent();
        double baseRent = car.calculateBaseRent(days);
        double discountAmount = Math.round((baseRent * (discountPercent / 100.0)) * 100.0) / 100.0;
        double discountedBase = baseRent - discountAmount;
        double insuranceFee = withInsurance ? (days * Rental.DEFAULT_INSURANCE_RATE_PER_DAY) : 0.0;
        double securityDeposit = Rental.DEFAULT_SECURITY_DEPOSIT;
        double upfrontTotal = Math.round((discountedBase + insuranceFee) * 100.0) / 100.0;

        LocalDate rentalDate = LocalDate.now();
        LocalDate expectedReturnDate = rentalDate.plusDays(days);

        Rental rental = new Rental(0, carId, customerId,
                rentalDate, expectedReturnDate, null,
                days, car.getDailyRate(), baseRent, discountAmount, insuranceFee, securityDeposit,
                car.getMileage(), 0.0, 0.0, 0.0, upfrontTotal,
                Rental.RentalStatus.ACTIVE, "Booked with insurance: " + withInsurance);

        // 5. Atomic persistence with compensating rollback
        carDAO.updateCarStatus(carId, "RENTED");
        try {
            int rentalId = rentalDAO.insertRental(rental);
            rental.setRentalId(rentalId);

            // Update in-memory state
            car.setStatus("RENTED");
            rental.setCar(car);
            rental.setCustomer(customer);

            return rental;
        } catch (SQLException | RuntimeException ex) {
            try {
                carDAO.updateCarStatus(carId, "AVAILABLE");
                car.setStatus("AVAILABLE");
            } catch (SQLException rollbackEx) {
                ex.addSuppressed(rollbackEx);
            }
            throw ex;
        }
    }

    // =========================================================================
    // 2. Overloaded Rent Calculation Methods (Syllabus Unit 1)
    // =========================================================================

    /**
     * Overload 1: Base Rent Calculation.
     * Computes vehicle-specific base rent via polymorphism.
     *
     * @param car  Vehicle to calculate rent for (must not be null)
     * @param days Number of rental days (must be >= 1)
     * @return Base rent amount
     */
    public double calculateRent(Car car, int days) {
        if (car == null) {
            throw new IllegalArgumentException("Car cannot be null");
        }
        if (days <= 0) {
            throw new IllegalArgumentException("Rental days must be >= 1, received: " + days);
        }
        return car.calculateBaseRent(days);
    }

    /**
     * Overload 2: Tier Discount Rent Calculation.
     * Clamps customer discount percentage to [0.0, 100.0] and applies to base rent.
     *
     * @param car                     Vehicle to calculate rent for
     * @param days                    Number of rental days
     * @param customerDiscountPercent Percentage discount from customer tier
     * @return Discounted base rent rounded to 2 decimal places
     */
    public double calculateRent(Car car, int days, double customerDiscountPercent) {
        double base = calculateRent(car, days);
        double clamped = Math.max(0.0, Math.min(100.0, customerDiscountPercent));
        return Math.round((base * (1.0 - (clamped / 100.0))) * 100.0) / 100.0;
    }

    /**
     * Overload 3: Insurance Add-on Calculation.
     * Adds non-discounted flat insurance fee (Rs 250/day) to discounted base rent.
     *
     * @param car                     Vehicle to calculate rent for
     * @param days                    Number of rental days
     * @param customerDiscountPercent Customer loyalty discount percentage
     * @param withInsurance           Whether insurance coverage is included
     * @return Total rent including insurance add-on
     */
    public double calculateRent(Car car, int days, double customerDiscountPercent, boolean withInsurance) {
        double discounted = calculateRent(car, days, customerDiscountPercent);
        double ins = withInsurance ? (days * Rental.DEFAULT_INSURANCE_RATE_PER_DAY) : 0.0;
        return Math.round((discounted + ins) * 100.0) / 100.0;
    }

    /**
     * Overload 4: Final Return Bill Calculation.
     * Validates odometer reading and delegates to rental agreement calculation.
     *
     * @param rental      Rental transaction agreement
     * @param actualDays  Total elapsed days between checkout and return
     * @param endMileage  Closing odometer reading (must be >= startMileage)
     * @param damageFee   Assessed vehicle damage fee
     * @param cleaningFee Assessed vehicle cleaning fee
     * @return Net final return bill
     */
    public double calculateFinalReturnBill(Rental rental, int actualDays, double endMileage,
                                           double damageFee, double cleaningFee) {
        if (rental == null) {
            throw new IllegalArgumentException("Rental cannot be null");
        }
        if (endMileage < rental.getStartMileage()) {
            throw new IllegalArgumentException("End mileage (" + endMileage + ") cannot be less than start mileage (" + rental.getStartMileage() + ")");
        }
        return rental.calculateFinalReturnBill(actualDays, endMileage, damageFee, cleaningFee);
    }

    // =========================================================================
    // 3. Return & Inspection Workflow (Module 4)
    // =========================================================================

    /**
     * 5-parameter return workflow overload for TestContext & UI compatibility.
     * Automatically assesses maintenance if damage fee exceeds Rs 5,000 threshold.
     */
    public synchronized Rental processReturn(int rentalId, LocalDate actualReturnDate, double endMileage,
                                             double damageFee, String damageNotes) throws SQLException {
        return processReturn(rentalId, actualReturnDate, endMileage, damageFee, 0.0, damageNotes, (damageFee > 5000.0));
    }

    /**
     * Complete 7-parameter return workflow with deep cleaning and maintenance override.
     * Validates state, waives minor damage (<= 5,000) for insured rentals, calculates late fee,
     * updates odometer, releases vehicle to AVAILABLE or MAINTENANCE, and closes rental.
     */
    public synchronized Rental processReturn(int rentalId, LocalDate actualReturnDate, double endMileage,
                                             double damageFee, double cleaningFee, String damageNotes,
                                             boolean needsMaintenance) throws SQLException {
        Rental rental = rentalDAO.getRentalById(rentalId);
        if (rental == null) {
            throw new IllegalArgumentException("Rental ID " + rentalId + " does not exist.");
        }
        if (rental.getStatus() == Rental.RentalStatus.COMPLETED || rental.getStatus() == Rental.RentalStatus.RETURNED) {
            throw new IllegalStateException("Rental ID " + rentalId + " is already completed.");
        }
        if (endMileage < rental.getStartMileage()) {
            throw new IllegalArgumentException("End mileage (" + endMileage + ") cannot be less than start mileage (" + rental.getStartMileage() + ")");
        }

        LocalDate returnDate = (actualReturnDate != null) ? actualReturnDate : LocalDate.now();
        int actualDays = (int) ChronoUnit.DAYS.between(rental.getRentalDate(), returnDate);
        if (actualDays <= 0) {
            actualDays = rental.getRentalDays();
        }

        // Comprehensive insurance waiver check: minor damage (<= 5,000) absorbed by waiver
        double assessedDamage = Math.max(0.0, damageFee);
        if (rental.getInsuranceFee() > 0 && assessedDamage <= 5000.0) {
            assessedDamage = 0.0;
        }

        double finalBill = rental.calculateFinalReturnBill(actualDays, endMileage, assessedDamage, cleaningFee);

        int duration = Math.max(1, actualDays);
        int overdueDays = Math.max(0, duration - rental.getRentalDays());
        double calculatedLateFee = overdueDays * (rental.getDailyRate() * Rental.LATE_FEE_MULTIPLIER);

        // Vehicle status decision: severe damage (>5,000) or explicit maintenance flag -> MAINTENANCE, else AVAILABLE
        String nextCarStatus = (damageFee > 5000.0 || needsMaintenance) ? "MAINTENANCE" : "AVAILABLE";

        // Update database records
        rentalDAO.updateRentalReturn(rentalId, returnDate, endMileage, assessedDamage, finalBill, "COMPLETED", damageNotes);
        carDAO.updateCarMileage(rental.getCarId(), endMileage);
        carDAO.updateCarStatus(rental.getCarId(), nextCarStatus);

        // Update in-memory rental entity
        rental.setActualReturnDate(returnDate);
        rental.setEndMileage(endMileage);
        rental.setLateFee(calculatedLateFee);
        rental.setDamageFee(assessedDamage);
        rental.setTotalAmount(finalBill);
        rental.setStatus(Rental.RentalStatus.COMPLETED);
        rental.setNotes(damageNotes != null ? damageNotes : "");

        Car car = carDAO.getCarById(rental.getCarId());
        if (car != null) {
            car.setMileage(endMileage);
            car.setStatus(nextCarStatus);
            rental.setCar(car);
        }

        return rental;
    }

    // =========================================================================
    // 4. Car Inventory Management (Module 1)
    // =========================================================================

    public void addCar(Car car) throws SQLException {
        if (car == null) {
            throw new IllegalArgumentException("Car cannot be null");
        }
        carDAO.insertCar(car);
    }

    public List<Car> getAllCars() throws SQLException {
        return carDAO.getAllCars();
    }

    public List<Car> getAvailableCars() throws SQLException {
        return carDAO.getAvailableCars();
    }

    public Car getCarById(int carId) throws SQLException {
        return carDAO.getCarById(carId);
    }

    public void updateCarStatus(int carId, String status) throws SQLException {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        carDAO.updateCarStatus(carId, status.trim().toUpperCase());
    }

    public void updateCarMileage(int carId, double mileage) throws SQLException {
        if (mileage < 0) {
            throw new IllegalArgumentException("Mileage cannot be negative: " + mileage);
        }
        carDAO.updateCarMileage(carId, mileage);
    }

    public synchronized void setCarMaintenance(int carId, boolean maintenance) throws SQLException {
        toggleMaintenance(carId, maintenance);
    }

    public synchronized void toggleMaintenance(int carId, boolean inMaintenance) throws SQLException {
        Car car = carDAO.getCarById(carId);
        if (car == null) {
            throw new IllegalArgumentException("Vehicle ID " + carId + " not found.");
        }
        if ("RENTED".equalsIgnoreCase(car.getStatus())) {
            throw new IllegalStateException("Cannot toggle maintenance on currently rented vehicle");
        }
        String newStatus = inMaintenance ? "MAINTENANCE" : "AVAILABLE";
        carDAO.updateCarStatus(carId, newStatus);
        car.setStatus(newStatus);
    }

    public List<Car> filterInventory(String category, String status) throws SQLException {
        List<Car> all = carDAO.getAllCars();
        List<Car> filtered = new ArrayList<>();
        if (all != null) {
            for (Car c : all) {
                boolean matchCat = (category == null || category.trim().isEmpty() ||
                        c.getVehicleType().equalsIgnoreCase(category.trim()));
                boolean matchStatus = (status == null || status.trim().isEmpty() ||
                        c.getStatus().equalsIgnoreCase(status.trim()));
                if (matchCat && matchStatus) {
                    filtered.add(c);
                }
            }
        }
        return filtered;
    }

    // =========================================================================
    // 5. Customer Management (Module 2)
    // =========================================================================

    public Customer registerCustomer(Customer customer) throws SQLException, IllegalArgumentException {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        if (customer.getFullName() == null || customer.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty");
        }
        if (customer.getPhone() == null || !customer.getPhone().trim().matches("\\d{10}")) {
            throw new IllegalArgumentException("Invalid phone number. Must be 10 digits.");
        }
        if (customer.getDrivingLicense() == null || customer.getDrivingLicense().trim().length() < 5) {
            throw new IllegalArgumentException("Invalid driving license. Minimum length is 5 characters.");
        }

        // Uniqueness verification
        if (customerDAO.getCustomerByPhone(customer.getPhone().trim()) != null) {
            throw new IllegalArgumentException("Customer with phone " + customer.getPhone() + " already exists.");
        }
        if (customerDAO.getCustomerByLicense(customer.getDrivingLicense().trim()) != null) {
            throw new IllegalArgumentException("Customer with driving license " + customer.getDrivingLicense() + " already exists.");
        }

        int generatedId = customerDAO.insertCustomer(customer);
        customer.setCustomerId(generatedId);
        return customer;
    }

    public Customer getCustomerById(int customerId) throws SQLException {
        return customerDAO.getCustomerById(customerId);
    }

    public Customer getCustomerByPhone(String phone) throws SQLException {
        return customerDAO.getCustomerByPhone(phone);
    }

    public Customer getCustomerByLicense(String license) throws SQLException {
        return customerDAO.getCustomerByLicense(license);
    }

    public List<Customer> getAllCustomers() throws SQLException {
        return customerDAO.getAllCustomers();
    }

    // =========================================================================
    // 6. Rental History & KPI Summary Methods (Module 5)
    // =========================================================================

    public List<Rental> getAllRentals() throws SQLException {
        return rentalDAO.getAllRentals();
    }

    public List<Rental> getActiveRentals() throws SQLException {
        return rentalDAO.getActiveRentals();
    }

    public Rental getRentalById(int rentalId) throws SQLException {
        return rentalDAO.getRentalById(rentalId);
    }

    public int getActiveRentalsCount() throws SQLException {
        List<Rental> active = rentalDAO.getActiveRentals();
        return (active != null) ? active.size() : 0;
    }

    public double calculateTotalRevenue() throws SQLException {
        return getTotalRevenue();
    }

    public double getTotalRevenue() throws SQLException {
        List<Rental> all = rentalDAO.getAllRentals();
        double total = 0.0;
        if (all != null) {
            for (Rental r : all) {
                if (r.getStatus() == Rental.RentalStatus.COMPLETED || r.getStatus() == Rental.RentalStatus.RETURNED) {
                    total += r.getTotalAmount();
                }
            }
        }
        return Math.round(total * 100.0) / 100.0;
    }

    public Map<String, Object> getKPISummary() throws SQLException {
        List<Rental> all = rentalDAO.getAllRentals();
        int activeCount = 0;
        int completedCount = 0;
        double totalRevenue = 0.0;
        double totalDamages = 0.0;
        double totalLateFees = 0.0;

        if (all != null) {
            for (Rental r : all) {
                if (r.getStatus() == Rental.RentalStatus.ACTIVE) {
                    activeCount++;
                } else if (r.getStatus() == Rental.RentalStatus.COMPLETED || r.getStatus() == Rental.RentalStatus.RETURNED) {
                    completedCount++;
                    totalRevenue += r.getTotalAmount();
                    totalDamages += r.getDamageFee();

                    double lateFee = r.getLateFee();
                    if (lateFee <= 0.0 && r.getActualReturnDate() != null && r.getRentalDate() != null) {
                        int duration = (int) ChronoUnit.DAYS.between(r.getRentalDate(), r.getActualReturnDate());
                        int overdue = Math.max(0, duration - r.getRentalDays());
                        if (overdue > 0) {
                            lateFee = overdue * (r.getDailyRate() * Rental.LATE_FEE_MULTIPLIER);
                        }
                    }
                    totalLateFees += lateFee;
                }
            }
        }

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalRevenue", Math.round(totalRevenue * 100.0) / 100.0);
        kpis.put("activeRentalsCount", activeCount);
        kpis.put("completedRentalsCount", completedCount);
        kpis.put("totalRentalsCount", (all != null) ? all.size() : 0);
        kpis.put("totalDamagesCollected", Math.round(totalDamages * 100.0) / 100.0);
        kpis.put("totalLateFeesCollected", Math.round(totalLateFees * 100.0) / 100.0);
        return kpis;
    }
}
