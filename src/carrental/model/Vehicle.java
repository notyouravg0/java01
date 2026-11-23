package carrental.model;

import java.util.Objects;

/**
 * Abstract base class encapsulating common vehicular attributes and polymorphic contracts.
 * Mandated by Syllabus Unit 2.
 */
public abstract class Vehicle {
    protected int vehicleId;
    protected String registrationNumber; // Unique license plate (e.g. "MH-12-AB-1234")
    protected String make;               // Manufacturer (e.g. "Toyota", "Tata")
    protected String model;              // Model designation (e.g. "City", "Harrier")
    protected int year;                  // Manufacturing year (e.g. 2023)
    protected double dailyRate;          // Standard base rate per day (> 0.0)
    protected double mileage;            // Current odometer reading in km (>= 0.0)
    protected String status;             // "AVAILABLE", "RENTED", "MAINTENANCE"

    public Vehicle() {
        this.status = "AVAILABLE";
    }

    public Vehicle(int vehicleId, String registrationNumber, String make, String model,
                   int year, double dailyRate, double mileage, String status) {
        if (dailyRate <= 0) throw new IllegalArgumentException("Daily rate must be positive: " + dailyRate);
        if (mileage < 0) throw new IllegalArgumentException("Mileage cannot be negative: " + mileage);
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Registration number cannot be empty");
        }
        this.vehicleId = vehicleId;
        this.registrationNumber = registrationNumber.trim().toUpperCase();
        this.make = make != null ? make.trim() : "";
        this.model = model != null ? model.trim() : "";
        this.year = year;
        this.dailyRate = dailyRate;
        this.mileage = mileage;
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "AVAILABLE";
    }

    public Vehicle(String registrationNumber, String make, String model, int year, double dailyRate, double mileage) {
        this(0, registrationNumber, make, model, year, dailyRate, mileage, "AVAILABLE");
    }

    // --- Abstract Methods Mandated by Syllabus Unit 2 ---
    public abstract String getVehicleType();
    public abstract double calculateBaseRent(int days);

    // --- Accessors and Mutators ---
    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) {
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Registration number cannot be empty");
        }
        this.registrationNumber = registrationNumber.trim().toUpperCase();
    }

    // Aliases for DAO / UI compatibility
    public String getLicensePlate() { return registrationNumber; }
    public void setLicensePlate(String licensePlate) { setRegistrationNumber(licensePlate); }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make != null ? make.trim() : ""; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model != null ? model.trim() : ""; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public double getDailyRate() { return dailyRate; }
    public void setDailyRate(double dailyRate) {
        if (dailyRate <= 0) throw new IllegalArgumentException("Daily rate must be positive");
        this.dailyRate = dailyRate;
    }

    public double getMileage() { return mileage; }
    public void setMileage(double mileage) {
        if (mileage < 0) throw new IllegalArgumentException("Mileage cannot be negative");
        this.mileage = mileage;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        if (status != null) this.status = status.trim().toUpperCase();
    }

    public boolean isAvailable() {
        return "AVAILABLE".equalsIgnoreCase(this.status);
    }

    public void setAvailable(boolean available) {
        this.status = available ? "AVAILABLE" : "RENTED";
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %s (%d) - Plate: %s | ₹%.2f/day | Status: %s",
                getVehicleType(), make, model, year, registrationNumber, dailyRate, status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vehicle vehicle = (Vehicle) o;
        return Objects.equals(registrationNumber, vehicle.registrationNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registrationNumber);
    }
}
