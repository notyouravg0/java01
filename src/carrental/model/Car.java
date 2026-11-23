package carrental.model;

/**
 * Concrete Car entity extending Vehicle and implementing Rentable.
 * Mandated by Syllabus Units 1 & 2.
 */
public class Car extends Vehicle implements Rentable {
    protected int seatingCapacity; // Number of passenger seats (e.g. 4, 5, 7)
    protected String fuelType;       // "PETROL", "DIESEL", "ELECTRIC", "CNG", "HYBRID"
    protected String transmission;   // "MANUAL", "AUTOMATIC"

    public Car() {
        super();
        this.seatingCapacity = 5;
        this.fuelType = "PETROL";
        this.transmission = "MANUAL";
    }

    public Car(int vehicleId, String registrationNumber, String make, String model,
               int year, double dailyRate, double mileage, String status,
               int seatingCapacity, String fuelType, String transmission) {
        super(vehicleId, registrationNumber, make, model, year, dailyRate, mileage, status);
        this.seatingCapacity = seatingCapacity > 0 ? seatingCapacity : 5;
        this.fuelType = (fuelType != null && !fuelType.trim().isEmpty()) ? fuelType.trim().toUpperCase() : "PETROL";
        this.transmission = (transmission != null && !transmission.trim().isEmpty()) ? transmission.trim().toUpperCase() : "MANUAL";
    }

    public Car(String registrationNumber, String make, String model,
               int year, double dailyRate, double mileage,
               int seatingCapacity, String fuelType, String transmission) {
        this(0, registrationNumber, make, model, year, dailyRate, mileage, "AVAILABLE",
             seatingCapacity, fuelType, transmission);
    }

    // --- Aliases for Car ID ---
    public int getCarId() { return getVehicleId(); }
    public void setCarId(int carId) { setVehicleId(carId); }

    // --- Getters & Setters ---
    public int getSeatingCapacity() { return seatingCapacity; }
    public void setSeatingCapacity(int seatingCapacity) {
        if (seatingCapacity <= 0) throw new IllegalArgumentException("Seating capacity must be at least 1");
        this.seatingCapacity = seatingCapacity;
    }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) {
        this.fuelType = fuelType != null ? fuelType.trim().toUpperCase() : "PETROL";
    }

    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) {
        this.transmission = transmission != null ? transmission.trim().toUpperCase() : "MANUAL";
    }

    // --- Implementation of Rentable Interface ---
    @Override
    public void rent() {
        if (!isAvailable()) {
            throw new IllegalStateException("Car " + registrationNumber + " is not available for rent. Current status: " + status);
        }
        this.status = "RENTED";
    }

    @Override
    public void returnVehicle() {
        this.status = "AVAILABLE";
    }

    @Override
    public double calculateRate(int days) {
        return calculateBaseRent(days);
    }

    // --- Implementation of Vehicle Abstract Methods ---
    @Override
    public String getVehicleType() {
        return "CAR";
    }

    @Override
    public double calculateBaseRent(int days) {
        if (days <= 0) throw new IllegalArgumentException("Rental days must be at least 1, received: " + days);
        return days * this.dailyRate;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %s (%d) - Plate: %s | %s | %s | %d-seater | ₹%.2f/day | %s",
                getVehicleType(), make, model, year, registrationNumber, transmission, fuelType, seatingCapacity, dailyRate, status);
    }
}
