package carrental.exception;

/**
 * Checked exception thrown when an unavailable or maintenance vehicle is booked,
 * or when concurrent booking contention occurs.
 * Mandated by Syllabus Unit 3.
 */
public class CarUnavailableException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int carId;
    private final String registrationNumber;
    private final String status;

    public CarUnavailableException(String message) {
        super(message);
        this.carId = -1;
        this.registrationNumber = "";
        this.status = "UNKNOWN";
    }

    public CarUnavailableException(String message, int carId, String registrationNumber) {
        super(message);
        this.carId = carId;
        this.registrationNumber = (registrationNumber != null) ? registrationNumber : "";
        this.status = "UNKNOWN";
    }

    public CarUnavailableException(String message, int carId, String registrationNumber, String status) {
        super(message);
        this.carId = carId;
        this.registrationNumber = (registrationNumber != null) ? registrationNumber : "";
        this.status = (status != null) ? status : "UNKNOWN";
    }

    public CarUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.carId = -1;
        this.registrationNumber = "";
        this.status = "UNKNOWN";
    }

    public CarUnavailableException(int carId, String registrationNumber, String status) {
        super(String.format("Vehicle %s (ID: %d) is currently %s",
                (registrationNumber != null ? registrationNumber : ""), carId, (status != null ? status : "UNKNOWN")));
        this.carId = carId;
        this.registrationNumber = (registrationNumber != null) ? registrationNumber : "";
        this.status = (status != null) ? status : "UNKNOWN";
    }

    public CarUnavailableException(String message, int carId, String registrationNumber, Throwable cause) {
        super(message, cause);
        this.carId = carId;
        this.registrationNumber = (registrationNumber != null) ? registrationNumber : "";
        this.status = "UNKNOWN";
    }

    public CarUnavailableException(String message, int carId, String registrationNumber, String status, Throwable cause) {
        super(message, cause);
        this.carId = carId;
        this.registrationNumber = (registrationNumber != null) ? registrationNumber : "";
        this.status = (status != null) ? status : "UNKNOWN";
    }

    public int getCarId() {
        return carId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getStatus() {
        return status;
    }
}
