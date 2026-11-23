package carrental.dao;

import carrental.db.DBConnection;
import carrental.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit 4 Data Access Object for Car inventory.
 * Employs PreparedStatement, explicit executeQuery, and polymorphic entity construction.
 */
public class CarDAO {

    public CarDAO() {
    }

    /**
     * Inserts a Car, Sedan, SUV, or Hatchback polymorphically into table 'cars'.
     */
    public void insertCar(Car car) throws SQLException {
        String sql = "INSERT INTO cars (" +
                "registration_number, make, model, year, category, daily_rate, mileage, " +
                "status, is_available, seating_capacity, fuel_type, transmission, " +
                "boot_space_liters, comfort_category, long_term_discount_percent, " +
                "is_awd, ground_clearance_mm, is_seven_seater, all_terrain_surcharge, " +
                "is_compact_city, fuel_economy_kmpl, economy_discount_rate" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            String category = "CAR";
            if (car instanceof Sedan) category = "SEDAN";
            else if (car instanceof SUV) category = "SUV";
            else if (car instanceof Hatchback) category = "HATCHBACK";

            ps.setString(1, car.getRegistrationNumber());
            ps.setString(2, car.getMake());
            ps.setString(3, car.getModel());
            ps.setInt(4, car.getYear());
            ps.setString(5, category);
            ps.setDouble(6, car.getDailyRate());
            ps.setDouble(7, car.getMileage());
            ps.setString(8, car.getStatus());
            ps.setInt(9, car.isAvailable() ? 1 : 0);
            ps.setInt(10, car.getSeatingCapacity());
            ps.setString(11, car.getFuelType());
            ps.setString(12, car.getTransmission());

            // Polymorphic parameter binding
            if (car instanceof Sedan) {
                Sedan s = (Sedan) car;
                ps.setDouble(13, s.getBootSpaceLiters());
                ps.setString(14, s.getComfortCategory());
                ps.setDouble(15, s.getLongTermDiscountPercent());
                ps.setNull(16, Types.INTEGER);
                ps.setNull(17, Types.INTEGER);
                ps.setNull(18, Types.INTEGER);
                ps.setNull(19, Types.REAL);
                ps.setNull(20, Types.INTEGER);
                ps.setNull(21, Types.REAL);
                ps.setNull(22, Types.REAL);
            } else if (car instanceof SUV) {
                SUV suv = (SUV) car;
                ps.setNull(13, Types.REAL);
                ps.setNull(14, Types.VARCHAR);
                ps.setNull(15, Types.REAL);
                ps.setInt(16, suv.isAwd() ? 1 : 0);
                ps.setInt(17, suv.getGroundClearanceMm());
                ps.setInt(18, suv.isSevenSeater() ? 1 : 0);
                ps.setDouble(19, suv.getAllTerrainSurcharge());
                ps.setNull(20, Types.INTEGER);
                ps.setNull(21, Types.REAL);
                ps.setNull(22, Types.REAL);
            } else if (car instanceof Hatchback) {
                Hatchback h = (Hatchback) car;
                ps.setNull(13, Types.REAL);
                ps.setNull(14, Types.VARCHAR);
                ps.setNull(15, Types.REAL);
                ps.setNull(16, Types.INTEGER);
                ps.setNull(17, Types.INTEGER);
                ps.setNull(18, Types.INTEGER);
                ps.setNull(19, Types.REAL);
                ps.setInt(20, h.isCompactCity() ? 1 : 0);
                ps.setDouble(21, h.getFuelEconomyKmpl());
                ps.setDouble(22, h.getEconomyDiscountRate());
            } else {
                for (int i = 13; i <= 22; i++) {
                    ps.setNull(i, Types.NULL);
                }
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    car.setCarId(rs.getInt(1));
                }
            }
        }
    }

    /**
     * Retrieves a car by car_id, polymorphically instantiating Sedan, SUV, or Hatchback.
     */
    public Car getCarById(int carId) throws SQLException {
        String sql = "SELECT * FROM cars WHERE car_id = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, carId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCar(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves a car by unique registration plate.
     */
    public Car getCarByRegistration(String regNum) throws SQLException {
        String sql = "SELECT * FROM cars WHERE UPPER(TRIM(registration_number)) = UPPER(TRIM(?));";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, regNum);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCar(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all cars in the fleet.
     */
    public List<Car> getAllCars() throws SQLException {
        List<Car> list = new ArrayList<>();
        String sql = "SELECT * FROM cars ORDER BY car_id ASC;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToCar(rs));
            }
        }
        return list;
    }

    /**
     * Retrieves all currently available cars.
     */
    public List<Car> getAvailableCars() throws SQLException {
        List<Car> list = new ArrayList<>();
        String sql = "SELECT * FROM cars WHERE status = 'AVAILABLE' AND is_available = 1 ORDER BY car_id ASC;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToCar(rs));
            }
        }
        return list;
    }

    /**
     * Updates car status ('AVAILABLE', 'RENTED', 'MAINTENANCE').
     */
    public void updateCarStatus(int carId, String status) throws SQLException {
        String sql = "UPDATE cars SET status = ?, is_available = ?, updated_at = datetime('now', 'localtime') WHERE car_id = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String upperStatus = status.trim().toUpperCase();
            ps.setString(1, upperStatus);
            ps.setInt(2, "AVAILABLE".equalsIgnoreCase(upperStatus) ? 1 : 0);
            ps.setInt(3, carId);
            ps.executeUpdate();
        }
    }

    /**
     * Updates odometer mileage for a vehicle.
     */
    public void updateCarMileage(int carId, double mileage) throws SQLException {
        String sql = "UPDATE cars SET mileage = ?, updated_at = datetime('now', 'localtime') WHERE car_id = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, mileage);
            ps.setInt(2, carId);
            ps.executeUpdate();
        }
    }

    /**
     * Polymorphic factory method reconstructing concrete entity from ResultSet.
     */
    private Car mapRowToCar(ResultSet rs) throws SQLException {
        int id = rs.getInt("car_id");
        String reg = rs.getString("registration_number");
        String make = rs.getString("make");
        String model = rs.getString("model");
        int year = rs.getInt("year");
        String category = rs.getString("category");
        double dailyRate = rs.getDouble("daily_rate");
        double mileage = rs.getDouble("mileage");
        String status = rs.getString("status");
        int seating = rs.getInt("seating_capacity");
        String fuel = rs.getString("fuel_type");
        String trans = rs.getString("transmission");

        if ("SEDAN".equalsIgnoreCase(category)) {
            double boot = rs.getDouble("boot_space_liters");
            String comfort = rs.getString("comfort_category");
            double discount = rs.getDouble("long_term_discount_percent");
            return new Sedan(id, reg, make, model, year, dailyRate, mileage, status,
                    seating, fuel, trans, boot, comfort, discount);
        } else if ("SUV".equalsIgnoreCase(category)) {
            boolean awd = rs.getInt("is_awd") == 1;
            int clearance = rs.getInt("ground_clearance_mm");
            boolean seven = rs.getInt("is_seven_seater") == 1;
            double surcharge = rs.getDouble("all_terrain_surcharge");
            return new SUV(id, reg, make, model, year, dailyRate, mileage, status,
                    seating, fuel, trans, awd, clearance, seven, surcharge);
        } else if ("HATCHBACK".equalsIgnoreCase(category)) {
            boolean compact = rs.getInt("is_compact_city") == 1;
            double economy = rs.getDouble("fuel_economy_kmpl");
            double discountRate = rs.getDouble("economy_discount_rate");
            return new Hatchback(id, reg, make, model, year, dailyRate, mileage, status,
                    seating, fuel, trans, compact, economy, discountRate);
        } else {
            return new Car(id, reg, make, model, year, dailyRate, mileage, status,
                    seating, fuel, trans);
        }
    }
}
