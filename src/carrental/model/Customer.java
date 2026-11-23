package carrental.model;

import java.util.Objects;

/**
 * Customer entity POJO with loyalty MemberTier classification.
 * Mandated by Syllabus Unit 1.
 */
public class Customer {

    public enum MemberTier {
        REGULAR(0.0),
        SILVER(10.0),
        GOLD(20.0);

        private final double discountPercent;

        MemberTier(double discountPercent) {
            this.discountPercent = discountPercent;
        }

        public double getDiscountPercent() {
            return discountPercent;
        }

        public static MemberTier fromString(String tier) {
            if (tier == null) return REGULAR;
            try {
                return MemberTier.valueOf(tier.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return REGULAR;
            }
        }
    }

    private int customerId;
    private String fullName;
    private String phone;
    private String drivingLicense;
    private String email;
    private String address;
    private MemberTier memberTier;

    public Customer() {
        this.memberTier = MemberTier.REGULAR;
    }

    public Customer(int customerId, String fullName, String phone, String drivingLicense,
                    String email, String address, MemberTier memberTier) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        if (drivingLicense == null || drivingLicense.trim().isEmpty()) {
            throw new IllegalArgumentException("Driving license cannot be empty");
        }
        this.customerId = customerId;
        this.fullName = fullName.trim();
        this.phone = phone.trim();
        this.drivingLicense = drivingLicense.trim().toUpperCase();
        this.email = email != null ? email.trim() : "";
        this.address = address != null ? address.trim() : "";
        this.memberTier = memberTier != null ? memberTier : MemberTier.REGULAR;
    }

    public Customer(String fullName, String phone, String drivingLicense,
                    String email, String address, MemberTier memberTier) {
        this(0, fullName, phone, drivingLicense, email, address, memberTier);
    }

    public Customer(int customerId, String fullName, String phone, String drivingLicense,
                    String email, String address, String memberTierStr) {
        this(customerId, fullName, phone, drivingLicense, email, address, MemberTier.fromString(memberTierStr));
    }

    // --- Accessors & Mutators ---
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty");
        }
        this.fullName = fullName.trim();
    }

    // Compatibility alias
    public String getName() { return fullName; }
    public void setName(String name) { setFullName(name); }

    public String getPhone() { return phone; }
    public void setPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        this.phone = phone.trim();
    }

    public String getDrivingLicense() { return drivingLicense; }
    public void setDrivingLicense(String drivingLicense) {
        if (drivingLicense == null || drivingLicense.trim().isEmpty()) {
            throw new IllegalArgumentException("Driving license cannot be empty");
        }
        this.drivingLicense = drivingLicense.trim().toUpperCase();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email != null ? email.trim() : ""; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address != null ? address.trim() : ""; }

    public MemberTier getMemberTier() { return memberTier; }
    public void setMemberTier(MemberTier memberTier) {
        this.memberTier = memberTier != null ? memberTier : MemberTier.REGULAR;
    }

    public double getTierDiscountPercent() {
        return memberTier != null ? memberTier.getDiscountPercent() : 0.0;
    }

    @Override
    public String toString() {
        return String.format("[ID: %d] %s (%s) - %s Tier", customerId, fullName, phone, memberTier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(phone, customer.phone) ||
               Objects.equals(drivingLicense, customer.drivingLicense);
    }

    @Override
    public int hashCode() {
        return Objects.hash(drivingLicense);
    }
}
