package carrental.model;

/**
 * Concrete Hatchback specialization with economy discount rate and city handling.
 * Mandated by Syllabus Unit 2.
 */
public class Hatchback extends Car {
    private boolean isCompactCity;             // Compact city parking/maneuvering indicator
    private double fuelEconomyKmpl;           // Fuel efficiency in km/l (e.g. 21.5 km/l)
    private double economyDiscountRate;       // Discount rate percentage (e.g. 5.0%)

    public Hatchback() {
        super();
        this.isCompactCity = true;
        this.fuelEconomyKmpl = 20.0;
        this.economyDiscountRate = 5.0;
    }

    public Hatchback(int vehicleId, String registrationNumber, String make, String model,
                     int year, double dailyRate, double mileage, String status,
                     int seatingCapacity, String fuelType, String transmission,
                     boolean isCompactCity, double fuelEconomyKmpl, double economyDiscountRate) {
        super(vehicleId, registrationNumber, make, model, year, dailyRate, mileage, status,
              seatingCapacity, fuelType, transmission);
        this.isCompactCity = isCompactCity;
        this.fuelEconomyKmpl = fuelEconomyKmpl > 0 ? fuelEconomyKmpl : 20.0;
        this.economyDiscountRate = Math.max(0.0, Math.min(100.0, economyDiscountRate));
    }

    public Hatchback(String registrationNumber, String make, String model,
                     int year, double dailyRate, double mileage,
                     int seatingCapacity, String fuelType, String transmission,
                     boolean isCompactCity, double fuelEconomyKmpl, double economyDiscountRate) {
        this(0, registrationNumber, make, model, year, dailyRate, mileage, "AVAILABLE",
             seatingCapacity, fuelType, transmission, isCompactCity, fuelEconomyKmpl, economyDiscountRate);
    }

    public boolean isCompactCity() { return isCompactCity; }
    public void setCompactCity(boolean compactCity) { isCompactCity = compactCity; }

    public double getFuelEconomyKmpl() { return fuelEconomyKmpl; }
    public void setFuelEconomyKmpl(double fuelEconomyKmpl) {
        if (fuelEconomyKmpl <= 0) throw new IllegalArgumentException("Fuel economy must be positive");
        this.fuelEconomyKmpl = fuelEconomyKmpl;
    }

    public double getEconomyDiscountRate() { return economyDiscountRate; }
    public void setEconomyDiscountRate(double economyDiscountRate) {
        this.economyDiscountRate = Math.max(0.0, Math.min(100.0, economyDiscountRate));
    }

    @Override
    public String getVehicleType() {
        return "HATCHBACK";
    }

    /**
     * Business Rule for Hatchback:
     * Standard rent = days * dailyRate.
     * Applies economyDiscountRate discount: rent * (1 - economyDiscountRate/100).
     */
    @Override
    public double calculateBaseRent(int days) {
        if (days <= 0) throw new IllegalArgumentException("Rental days must be at least 1");
        double base = days * this.dailyRate;
        if (economyDiscountRate > 0.0) {
            base = base * (1.0 - (economyDiscountRate / 100.0));
        }
        return Math.round(base * 100.0) / 100.0;
    }

    @Override
    public String toString() {
        return String.format("[HATCHBACK] %s %s (%d) | %.1f kmpl | City-Ready | ₹%.2f/day | %s",
                make, model, year, fuelEconomyKmpl, dailyRate, status);
    }
}
