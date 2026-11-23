package carrental.model;

/**
 * Concrete Sedan specialization with comfort category multiplier and long term discount.
 * Mandated by Syllabus Unit 2.
 */
public class Sedan extends Car {
    private double bootSpaceLiters;            // Trunk volume in liters (e.g. 510.0 L)
    private String comfortCategory;            // "EXECUTIVE", "PREMIUM", "LUXURY"
    private double longTermDiscountPercent;    // Discount for rentals >= 7 days (e.g. 10.0%)

    public Sedan() {
        super();
        this.bootSpaceLiters = 450.0;
        this.comfortCategory = "EXECUTIVE";
        this.longTermDiscountPercent = 10.0;
    }

    public Sedan(int vehicleId, String registrationNumber, String make, String model,
                 int year, double dailyRate, double mileage, String status,
                 int seatingCapacity, String fuelType, String transmission,
                 double bootSpaceLiters, String comfortCategory, double longTermDiscountPercent) {
        super(vehicleId, registrationNumber, make, model, year, dailyRate, mileage, status,
              seatingCapacity, fuelType, transmission);
        this.bootSpaceLiters = bootSpaceLiters > 0 ? bootSpaceLiters : 450.0;
        this.comfortCategory = comfortCategory != null ? comfortCategory.trim().toUpperCase() : "EXECUTIVE";
        this.longTermDiscountPercent = Math.max(0.0, Math.min(100.0, longTermDiscountPercent));
    }

    public Sedan(String registrationNumber, String make, String model,
                 int year, double dailyRate, double mileage,
                 int seatingCapacity, String fuelType, String transmission,
                 double bootSpaceLiters, String comfortCategory, double longTermDiscountPercent) {
        this(0, registrationNumber, make, model, year, dailyRate, mileage, "AVAILABLE",
             seatingCapacity, fuelType, transmission, bootSpaceLiters, comfortCategory, longTermDiscountPercent);
    }

    public double getBootSpaceLiters() { return bootSpaceLiters; }
    public void setBootSpaceLiters(double bootSpaceLiters) {
        if (bootSpaceLiters <= 0) throw new IllegalArgumentException("Boot space must be positive");
        this.bootSpaceLiters = bootSpaceLiters;
    }

    public String getComfortCategory() { return comfortCategory; }
    public void setComfortCategory(String comfortCategory) {
        this.comfortCategory = comfortCategory != null ? comfortCategory.trim().toUpperCase() : "EXECUTIVE";
    }

    public double getLongTermDiscountPercent() { return longTermDiscountPercent; }
    public void setLongTermDiscountPercent(double longTermDiscountPercent) {
        this.longTermDiscountPercent = Math.max(0.0, Math.min(100.0, longTermDiscountPercent));
    }

    @Override
    public String getVehicleType() {
        return "SEDAN";
    }

    /**
     * Business Rule for Sedan:
     * 1. Multiplier based on comfortCategory: LUXURY = 1.15, PREMIUM = 1.10, EXECUTIVE = 1.00.
     * 2. If days >= 7, applies longTermDiscountPercent.
     */
    @Override
    public double calculateBaseRent(int days) {
        if (days <= 0) throw new IllegalArgumentException("Rental days must be at least 1");
        double multiplier = 1.00;
        if ("LUXURY".equalsIgnoreCase(comfortCategory)) {
            multiplier = 1.15;
        } else if ("PREMIUM".equalsIgnoreCase(comfortCategory)) {
            multiplier = 1.10;
        }

        double gross = (days * this.dailyRate) * multiplier;
        if (days >= 7 && longTermDiscountPercent > 0.0) {
            gross = gross * (1.0 - (longTermDiscountPercent / 100.0));
        }
        return Math.round(gross * 100.0) / 100.0;
    }

    @Override
    public String toString() {
        return String.format("[SEDAN] %s %s (%d) | Boot: %.0fL | %s Category | ₹%.2f/day | %s",
                make, model, year, bootSpaceLiters, comfortCategory, dailyRate, status);
    }
}
