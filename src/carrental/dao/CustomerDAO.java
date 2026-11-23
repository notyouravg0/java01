package carrental.dao;

import carrental.db.DBConnection;
import carrental.model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit 4 Data Access Object for Customers.
 * Handles customer insertion with auto-generated key retrieval and multi-attribute lookup.
 */
public class CustomerDAO {

    public CustomerDAO() {
    }

    /**
     * Inserts a new customer profile and returns the generated customer ID.
     */
    public int insertCustomer(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (name, phone, driving_license, email, address, member_tier) " +
                     "VALUES (?, ?, ?, ?, ?, ?);";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            String name = customer.getName() != null ? customer.getName() : customer.getFullName();
            ps.setString(1, name);
            ps.setString(2, customer.getPhone());
            ps.setString(3, customer.getDrivingLicense());
            ps.setString(4, customer.getEmail());
            ps.setString(5, customer.getAddress());
            ps.setString(6, customer.getMemberTier() != null ? customer.getMemberTier().name() : "REGULAR");

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    customer.setCustomerId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Finds a customer by primary key customer_id.
     */
    public Customer getCustomerById(int customerId) throws SQLException {
        String sql = "SELECT * FROM customers WHERE customer_id = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCustomer(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds a customer by phone number.
     */
    public Customer getCustomerByPhone(String phone) throws SQLException {
        String sql = "SELECT * FROM customers WHERE TRIM(phone) = TRIM(?);";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCustomer(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds a customer by unique driving license number.
     */
    public Customer getCustomerByLicense(String license) throws SQLException {
        String sql = "SELECT * FROM customers WHERE UPPER(TRIM(driving_license)) = UPPER(TRIM(?));";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, license);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCustomer(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all registered customer profiles.
     */
    public List<Customer> getAllCustomers() throws SQLException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers ORDER BY customer_id ASC;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToCustomer(rs));
            }
        }
        return list;
    }

    private Customer mapRowToCustomer(ResultSet rs) throws SQLException {
        int id = rs.getInt("customer_id");
        String name = rs.getString("name");
        String phone = rs.getString("phone");
        String license = rs.getString("driving_license");
        String email = rs.getString("email");
        String address = rs.getString("address");
        String tierStr = rs.getString("member_tier");

        Customer.MemberTier tier = Customer.MemberTier.fromString(tierStr);
        return new Customer(id, name, phone, license, email, address, tier);
    }
}
