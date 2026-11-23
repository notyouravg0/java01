package carrental.model;

/**
 * Concrete SUV specialization with AWD and 7-seater surcharges.
 * Mandated by Syllabus Unit 2.
 */
public class SUV extends Car {
    private boolean isAwd;                    // All-Wheel Drive flag
    private int groundClearanceMm;            // Ground clearance in mm (e.g. 210 mm)
    private boolean isSevenSeater;            // 7-passenger capacity flag
    private double allTerrainSurcharge;       // Daily surcharge for AWD/all-terrain capacity (e.g. ₹300.00/day)

    public SUV() {
        super();
        this.isAwd = false;
        this.groundClearanceMm = 200;
        this.isSevenSeater = false;
        this.allTerrainSurcharge = 300.0;
    }

    public SUV(int vehicleId, String registrationNumber, String make, String model,
               int year, double dailyRate, double mileage, String status,
               int seatingCapacity, String fuelType, String transmission,
               boolean isAwd, int groundClearanceMm, boolean isSevenSeater, double allTerrainSurcharge) {
        super(vehicleId, registrationNumber, make, model, year, dailyRate, mileage, status,
              seatingCapacity, fuelType, transmission);
        this.isAwd = isAwd;
        this.groundClearanceMm = groundClearanceMm > 0 ? groundClearanceMm : 200;
        this.isSevenSeater = isSevenSeater;
        this.allTerrainSurcharge = Math.max(0.0, allTerrainSurcharge);
        if (isSevenSeater && this.seatingCapacity < 7) {
            this.seatingCapacity = 7;
        }
    }

    public SUV(String registrationNumber, String make, String model,
               int year, double dailyRate, double mileage,
               int seatingCapacity, String fuelType, String transmission,
               boolean isAwd, int groundClearanceMm, boolean isSevenSeater, double allTerrainSurcharge) {
        this(0, registrationNumber, make, model, year, dailyRate, mileage, "AVAILABLE",
             seatingCapacity, fuelType, transmission, isAwd, groundClearanceMm, isSevenSeater, allTerrainSurcharge);
    }

    public boolean isAwd() { return isAwd; }
    public void setAwd(boolean awd) { isAwd = awd; }

    public int getGroundClearanceMm() { return groundClearanceMm; }
    public void setGroundClearanceMm(int groundClearanceMm) {
        if (groundClearanceMm <= 0) throw new IllegalArgumentException("Ground clearance must be positive");
        this.groundClearanceMm = groundClearanceMm;
    }

    public boolean isSevenSeater() { return isSevenSeater; }
    public void setSevenSeater(boolean sevenSeater) {
        isSevenSeater = sevenSeater;
        if (sevenSeater && this.seatingCapacity < 7) {
            this.seatingCapacity = 7;
        }
    }

    public double getAllTerrainSurcharge() { return allTerrainSurcharge; }
    public void setAllTerrainSurcharge(double allTerrainSurcharge) {
        this.allTerrainSurcharge = Math.max(0.0, allTerrainSurcharge);
    }

    @Override
    public String getVehicleType() {
        return "SUV";
    }

    /**
     * Business Rule for SUV:
     * 1. Base rent = days * dailyRate.
     * 2. If isAwd, add (days * allTerrainSurcharge).
     * 3. If isSevenSeater, add 10% premium on daily rate: (days * dailyRate * 0.10).
     */
    @Override
    public double calculateBaseRent(int days) {
        if (days <= 0) throw new IllegalArgumentException("Rental days must be at least 1");
        double base = days * this.dailyRate;
        double surcharge = isAwd ? (days * this.allTerrainSurcharge) : 0.0;
        double capacityPremium = isSevenSeater ? (days * this.dailyRate * 0.10) : 0.0;
        double total = base + surcharge + capacityPremium;
        return Math.round(total * 100.0) / 100.0;
    }

    @Override
    public String toString() {
        return String.format("[SUV] %s %s (%d) | %s | %dmm GC | %s | ₹%.2f/day | %s",
                make, model, year, isAwd ? "AWD" : "2WD", groundClearanceMm,
                isSevenSeater ? "7-Seater" : "5-Seater", dailyRate, status);
    }
}
