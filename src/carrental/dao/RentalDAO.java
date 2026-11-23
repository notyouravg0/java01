package carrental.dao;

import carrental.db.DBConnection;
import carrental.model.Rental;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit 4 Data Access Object for Rentals.
 * Manages active bookings, return settlement updates, and historical audit queries.
 */
public class RentalDAO {

    public RentalDAO() {
    }

    /**
     * Inserts a rental record, returning the generated rental ID.
     */
    public int insertRental(Rental rental) throws SQLException {
        String sql = "INSERT INTO rentals (" +
                "customer_id, car_id, rental_date, expected_return_date, actual_return_date, " +
                "rental_days, daily_rate, base_rent, discount_amount, insurance_fee, security_deposit, " +
                "start_mileage, end_mileage, late_fee, damage_fee, " +
                "total_amount, status, notes" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, rental.getCustomerId());
            ps.setInt(2, rental.getCarId());
            ps.setString(3, rental.getRentalDate().toString());
            ps.setString(4, rental.getExpectedReturnDate().toString());
            ps.setString(5, rental.getActualReturnDate() != null ? rental.getActualReturnDate().toString() : null);
            ps.setInt(6, rental.getRentalDays() > 0 ? rental.getRentalDays() : 1);
            ps.setDouble(7, rental.getDailyRate());
            ps.setDouble(8, rental.getBaseRent());
            ps.setDouble(9, rental.getDiscountAmount());
            ps.setDouble(10, rental.getInsuranceFee());
            ps.setDouble(11, rental.getSecurityDeposit());
            ps.setDouble(12, rental.getStartMileage());
            if (rental.getEndMileage() > 0) {
                ps.setDouble(13, rental.getEndMileage());
            } else {
                ps.setNull(13, Types.REAL);
            }
            ps.setDouble(14, rental.getLateFee());
            ps.setDouble(15, rental.getDamageFee());
            ps.setDouble(16, rental.getTotalAmount());
            ps.setString(17, rental.getStatus() != null ? rental.getStatus().name() : "ACTIVE");
            ps.setString(18, rental.getNotes() != null ? rental.getNotes() : "");

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    rental.setRentalId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Finds a rental by primary key rental_id.
     */
    public Rental getRentalById(int rentalId) throws SQLException {
        String sql = "SELECT * FROM rentals WHERE rental_id = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rentalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRental(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all currently ongoing/active rentals.
     */
    public List<Rental> getActiveRentals() throws SQLException {
        List<Rental> list = new ArrayList<>();
        String sql = "SELECT * FROM rentals WHERE status = 'ACTIVE' ORDER BY rental_id DESC;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToRental(rs));
            }
        }
        return list;
    }

    /**
     * Retrieves all rental transactions (historical audit trail).
     */
    public List<Rental> getAllRentals() throws SQLException {
        List<Rental> list = new ArrayList<>();
        String sql = "SELECT * FROM rentals ORDER BY rental_id DESC;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToRental(rs));
            }
        }
        return list;
    }

    /**
     * Updates return fields upon vehicle check-in.
     * Maps 'RETURNED' to 'COMPLETED' to satisfy database check constraints.
     */
    public void updateRentalReturn(int rentalId, LocalDate actualReturn, double endMileage,
                                   double damageFee, double totalAmount, String status, String notes)
            throws SQLException {
        String sql = "UPDATE rentals SET " +
                "actual_return_date = ?, " +
                "end_mileage = ?, " +
                "damage_fee = ?, " +
                "total_amount = ?, " +
                "status = ?, " +
                "notes = ?, " +
                "updated_at = datetime('now', 'localtime') " +
                "WHERE rental_id = ?;";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String dbStatus = status != null ? status.trim().toUpperCase() : "COMPLETED";
            if ("RETURNED".equalsIgnoreCase(dbStatus)) {
                dbStatus = "COMPLETED";
            }

            ps.setString(1, actualReturn != null ? actualReturn.toString() : LocalDate.now().toString());
            ps.setDouble(2, endMileage);
            ps.setDouble(3, damageFee);
            ps.setDouble(4, totalAmount);
            ps.setString(5, dbStatus);
            ps.setString(6, notes != null ? notes : "");
            ps.setInt(7, rentalId);

            ps.executeUpdate();
        }
    }

    private Rental mapRowToRental(ResultSet rs) throws SQLException {
        int id = rs.getInt("rental_id");
        int customerId = rs.getInt("customer_id");
        int carId = rs.getInt("car_id");
        String rentDateStr = rs.getString("rental_date");
        String expDateStr = rs.getString("expected_return_date");
        String actDateStr = rs.getString("actual_return_date");
        int rentalDays = rs.getInt("rental_days");
        double dailyRate = rs.getDouble("daily_rate");
        double baseRent = rs.getDouble("base_rent");
        double discountAmount = rs.getDouble("discount_amount");
        double insuranceFee = rs.getDouble("insurance_fee");
        double securityDeposit = rs.getDouble("security_deposit");
        double startMileage = rs.getDouble("start_mileage");
        double endMileage = rs.getDouble("end_mileage");
        double lateFee = rs.getDouble("late_fee");
        double damageFee = rs.getDouble("damage_fee");
        double totalAmount = rs.getDouble("total_amount");
        String statusStr = rs.getString("status");
        String notes = rs.getString("notes");

        LocalDate rentalDate = LocalDate.parse(rentDateStr);
        LocalDate expectedReturnDate = LocalDate.parse(expDateStr);
        LocalDate actualReturnDate = (actDateStr != null && !actDateStr.trim().isEmpty()) ?
                LocalDate.parse(actDateStr) : null;

        if (rentalDays <= 0) {
            rentalDays = (int) ChronoUnit.DAYS.between(rentalDate, expectedReturnDate);
            if (rentalDays <= 0) rentalDays = 1;
        }

        if (baseRent <= 0) {
            baseRent = rentalDays * dailyRate;
        }

        Rental.RentalStatus status;
        if ("COMPLETED".equalsIgnoreCase(statusStr) || "RETURNED".equalsIgnoreCase(statusStr)) {
            status = Rental.RentalStatus.COMPLETED;
        } else if ("CANCELLED".equalsIgnoreCase(statusStr)) {
            status = Rental.RentalStatus.CANCELLED;
        } else {
            status = Rental.RentalStatus.ACTIVE;
        }

        return new Rental(id, carId, customerId, rentalDate, expectedReturnDate, actualReturnDate,
                rentalDays, dailyRate, baseRent, discountAmount, insuranceFee, securityDeposit,
                startMileage, endMileage, lateFee, damageFee, totalAmount, status, notes);
    }
}
