package carrental.model;

/**
 * Interface defining rental contract operations for rentable assets.
 * Mandated by Syllabus Unit 2.
 */
public interface Rentable {
    /**
     * Marks the rentable entity as rented.
     * @throws IllegalStateException if vehicle is not available.
     */
    void rent();

    /**
     * Marks the rentable entity as returned / available.
     */
    void returnVehicle();

    /**
     * Checks whether the vehicle is currently available for rent.
     * @return true if status is AVAILABLE, false otherwise.
     */
    boolean isAvailable();

    /**
     * Computes rental cost for the specified number of days.
     * @param days rental duration in days (>= 1)
     * @return calculated rental cost
     * @throws IllegalArgumentException if days <= 0
     */
    double calculateRate(int days);
}
