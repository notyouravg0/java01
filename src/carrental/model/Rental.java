package carrental.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Transaction entity POJO representing a car rental agreement with billing audit trail.
 * Mandated by Syllabus Unit 1.
 */
public class Rental {

    public enum RentalStatus {
        ACTIVE,
        RETURNED,
        COMPLETED,
        CANCELLED;

        public static RentalStatus fromString(String status) {
            if (status == null) return ACTIVE;
            String upper = status.trim().toUpperCase();
            if ("COMPLETED".equals(upper)) return COMPLETED;
            if ("RETURNED".equals(upper)) return RETURNED;
            if ("CANCELLED".equals(upper)) return CANCELLED;
            try {
                return RentalStatus.valueOf(upper);
            } catch (IllegalArgumentException e) {
                return ACTIVE;
            }
        }
    }

    public static final double DEFAULT_INSURANCE_RATE_PER_DAY = 250.0;
    public static final double DEFAULT_SECURITY_DEPOSIT = 3000.0;
    public static final double LATE_FEE_MULTIPLIER = 1.5;

    private int rentalId;
    private int carId;
    private int customerId;
    private LocalDate rentalDate;
    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;
    private int rentalDays;
    private double dailyRate;
    private double baseRent;
    private double discountAmount;
    private double insuranceFee;
    private double securityDeposit;
    private double startMileage;
    private double endMileage;
    private double lateFee;
    private double damageFee;
    private double totalAmount;
    private RentalStatus status;
    private String notes;

    // Optional entity references for UI display convenience
    private Car car;
    private Customer customer;

    public Rental() {
        this.status = RentalStatus.ACTIVE;
        this.rentalDate = LocalDate.now();
        this.expectedReturnDate = LocalDate.now().plusDays(1);
        this.rentalDays = 1;
    }

    public Rental(int rentalId, int carId, int customerId,
                  LocalDate rentalDate, LocalDate expectedReturnDate, LocalDate actualReturnDate,
                  int rentalDays, double dailyRate, double baseRent,
                  double discountAmount, double insuranceFee, double securityDeposit,
                  double startMileage, double endMileage,
                  double lateFee, double damageFee, double totalAmount,
                  RentalStatus status, String notes) {
        this.rentalId = rentalId;
        this.carId = carId;
        this.customerId = customerId;
        this.rentalDate = rentalDate != null ? rentalDate : LocalDate.now();
        this.expectedReturnDate = expectedReturnDate != null ? expectedReturnDate : this.rentalDate.plusDays(1);
        this.actualReturnDate = actualReturnDate;
        this.rentalDays = rentalDays > 0 ? rentalDays : 1;
        this.dailyRate = dailyRate;
        this.baseRent = baseRent;
        this.discountAmount = discountAmount;
        this.insuranceFee = insuranceFee;
        this.securityDeposit = securityDeposit;
        this.startMileage = startMileage;
        this.endMileage = endMileage;
        this.lateFee = lateFee;
        this.damageFee = damageFee;
        this.totalAmount = totalAmount;
        this.status = status != null ? status : RentalStatus.ACTIVE;
        this.notes = notes != null ? notes : "";
    }

    // --- Overloaded Rent Calculation Methods (Syllabus Unit 1 Explicit Mandate) ---

    /**
     * Overload 1: Base rent calculation.
     */
    public double calculateRent(int days) {
        if (days <= 0) throw new IllegalArgumentException("Rental days must be >= 1");
        return Math.round((days * this.dailyRate) * 100.0) / 100.0;
    }

    /**
     * Overload 2: Base rent with customer tier or promo discount percentage.
     */
    public double calculateRent(int days, double discountPercent) {
        double base = calculateRent(days);
        double clampedDiscount = Math.max(0.0, Math.min(100.0, discountPercent));
        double discounted = base * (1.0 - (clampedDiscount / 100.0));
        return Math.round(discounted * 100.0) / 100.0;
    }

    /**
     * Overload 3: Rent with discount percentage and optional daily insurance coverage.
     */
    public double calculateRent(int days, double discountPercent, boolean withInsurance) {
        double discountedBase = calculateRent(days, discountPercent);
        double insurance = withInsurance ? (days * DEFAULT_INSURANCE_RATE_PER_DAY) : 0.0;
        return Math.round((discountedBase + insurance) * 100.0) / 100.0;
    }

    /**
     * Overload 4: Rent with discount, insurance, and refundable security deposit.
     */
    public double calculateRent(int days, double discountPercent, boolean withInsurance, double deposit) {
        double rentWithInsurance = calculateRent(days, discountPercent, withInsurance);
        double validDeposit = Math.max(0.0, deposit);
        return Math.round((rentWithInsurance + validDeposit) * 100.0) / 100.0;
    }

    /**
     * Overload 5: Final return bill calculation for return settlement and damage reconciliation.
     */
    public double calculateFinalReturnBill(int actualDays, double endKm, double assessedDamageFee, double cleaningFee) {
        int duration = Math.max(1, actualDays);
        int overdueDays = Math.max(0, duration - this.rentalDays);
        double calculatedLateFee = overdueDays * (this.dailyRate * LATE_FEE_MULTIPLIER);
        double validDamageFee = Math.max(0.0, assessedDamageFee);
        double validCleaningFee = Math.max(0.0, cleaningFee);

        double netTotal = (this.baseRent - this.discountAmount) + this.insuranceFee + calculatedLateFee + validDamageFee + validCleaningFee;
        return Math.round(netTotal * 100.0) / 100.0;
    }

    // --- Getters & Setters ---
    public int getRentalId() { return rentalId; }
    public void setRentalId(int rentalId) { this.rentalId = rentalId; }

    public int getCarId() { return carId; }
    public void setCarId(int carId) { this.carId = carId; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public LocalDate getRentalDate() { return rentalDate; }
    public void setRentalDate(LocalDate rentalDate) { this.rentalDate = rentalDate; }

    public LocalDate getExpectedReturnDate() { return expectedReturnDate; }
    public void setExpectedReturnDate(LocalDate expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }

    public LocalDate getActualReturnDate() { return actualReturnDate; }
    public void setActualReturnDate(LocalDate actualReturnDate) { this.actualReturnDate = actualReturnDate; }

    public int getRentalDays() { return rentalDays; }
    public void setRentalDays(int rentalDays) {
        if (rentalDays <= 0) throw new IllegalArgumentException("Rental days must be >= 1");
        this.rentalDays = rentalDays;
    }

    public double getDailyRate() { return dailyRate; }
    public void setDailyRate(double dailyRate) { this.dailyRate = dailyRate; }

    public double getBaseRent() { return baseRent; }
    public void setBaseRent(double baseRent) { this.baseRent = baseRent; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public double getInsuranceFee() { return insuranceFee; }
    public void setInsuranceFee(double insuranceFee) { this.insuranceFee = insuranceFee; }

    public double getSecurityDeposit() { return securityDeposit; }
    public void setSecurityDeposit(double securityDeposit) { this.securityDeposit = securityDeposit; }

    public double getStartMileage() { return startMileage; }
    public void setStartMileage(double startMileage) { this.startMileage = startMileage; }

    public double getEndMileage() { return endMileage; }
    public void setEndMileage(double endMileage) { this.endMileage = endMileage; }

    public double getLateFee() { return lateFee; }
    public void setLateFee(double lateFee) { this.lateFee = lateFee; }

    public double getDamageFee() { return damageFee; }
    public void setDamageFee(double damageFee) { this.damageFee = damageFee; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public RentalStatus getStatus() { return status; }
    public void setStatus(RentalStatus status) { this.status = status != null ? status : RentalStatus.ACTIVE; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes != null ? notes : ""; }

    public Car getCar() { return car; }
    public void setCar(Car car) { this.car = car; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public boolean isActive() {
        return this.status == RentalStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return String.format("[Rental #%d] Car #%d | Customer #%d | %s to %s | ₹%.2f | Status: %s",
                rentalId, carId, customerId, rentalDate, expectedReturnDate, totalAmount, status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rental rental = (Rental) o;
        return rentalId == rental.rentalId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rentalId);
    }
}
