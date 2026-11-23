package carrental.ui;

import carrental.exception.CarUnavailableException;
import carrental.model.Car;
import carrental.model.Customer;
import carrental.model.Rental;
import carrental.service.RentalService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Reservation & Customer Booking View.
 * Streamlined two-column card layout with rounded inputs and real-time confirmation.
 */
public class CustomerBookingPanel extends JPanel {

    private final RentalService service;
    private final JComboBox<String> carCombo;
    private final JComboBox<String> custCombo;
    private final JTextField daysField;
    private final JCheckBox insuranceCheck;
    private final JTextArea resultArea;
    private final Runnable globalRefresh;

    // For new customer registration
    private final JTextField nameField, phoneField, licenseField, emailField, addressField;
    private final JComboBox<String> tierCombo;

    private List<Car> availableCars;
    private List<Customer> allCustomers;

    public CustomerBookingPanel(RentalService service, Runnable globalRefresh) {
        this.service = service;
        this.globalRefresh = globalRefresh;
        setLayout(new BorderLayout(0, 0));
        setBackground(TricolourTheme.BG_CREAM);

        // Header
        add(TricolourTheme.createHeaderPanel("Vehicle Reservation & Client Registration"), BorderLayout.NORTH);

        // Center Container
        JPanel contentPanel = new JPanel(new BorderLayout(0, 14));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Forms Grid: 2 Cards Side-by-Side
        JPanel formsGrid = new JPanel(new GridLayout(1, 2, 16, 0));
        formsGrid.setOpaque(false);

        // ---------------------------------------------------------------------
        // Card 1: New Vehicle Reservation
        // ---------------------------------------------------------------------
        TricolourTheme.RoundedCardPanel bookingCard = new TricolourTheme.RoundedCardPanel(new BorderLayout(0, 12), 12, TricolourTheme.BORDER_GRAY);
        bookingCard.setBorder(new EmptyBorder(16, 18, 16, 18));

        JLabel bookTitle = new JLabel("Reserve a Vehicle");
        bookTitle.setFont(TricolourTheme.HEADER_FONT);
        bookTitle.setForeground(TricolourTheme.NAVY);
        bookingCard.add(bookTitle, BorderLayout.NORTH);

        JPanel bookingForm = new JPanel(new GridLayout(4, 1, 0, 10));
        bookingForm.setOpaque(false);

        carCombo = new JComboBox<>();
        TricolourTheme.styleComboBox(carCombo);

        custCombo = new JComboBox<>();
        TricolourTheme.styleComboBox(custCombo);

        daysField = new JTextField("3");
        TricolourTheme.styleTextField(daysField);

        insuranceCheck = new JCheckBox("Full Comprehensive Insurance (\u20B9250.00 / day)");
        insuranceCheck.setFont(TricolourTheme.LABEL_FONT);
        insuranceCheck.setForeground(TricolourTheme.TEXT_DARK);
        insuranceCheck.setOpaque(false);

        bookingForm.add(createFieldWrapper("Select Available Vehicle:", carCombo));
        bookingForm.add(createFieldWrapper("Select Registered Customer:", custCombo));
        bookingForm.add(createFieldWrapper("Rental Duration (Days):", daysField));
        bookingForm.add(insuranceCheck);

        bookingCard.add(bookingForm, BorderLayout.CENTER);

        JPanel bookBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bookBtnRow.setOpaque(false);
        JButton bookBtn = TricolourTheme.styledButton("Confirm Booking", TricolourTheme.GREEN);
        bookBtn.addActionListener(e -> bookCar());

        // Mouse hover effect (Unit 5 requirement)
        bookBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                bookBtn.setBackground(TricolourTheme.GREEN_DARK);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                bookBtn.setBackground(TricolourTheme.GREEN);
            }
        });

        JButton refreshBtn = TricolourTheme.styledButton("Refresh Lists", TricolourTheme.SAFFRON);
        refreshBtn.addActionListener(e -> refreshData());

        bookBtnRow.add(bookBtn);
        bookBtnRow.add(refreshBtn);
        bookingCard.add(bookBtnRow, BorderLayout.SOUTH);

        // ---------------------------------------------------------------------
        // Card 2: Register New Customer
        // ---------------------------------------------------------------------
        TricolourTheme.RoundedCardPanel regCard = new TricolourTheme.RoundedCardPanel(new BorderLayout(0, 12), 12, TricolourTheme.BORDER_GRAY);
        regCard.setBorder(new EmptyBorder(16, 18, 16, 18));

        JLabel regTitle = new JLabel("Register New Client");
        regTitle.setFont(TricolourTheme.HEADER_FONT);
        regTitle.setForeground(TricolourTheme.NAVY);
        regCard.add(regTitle, BorderLayout.NORTH);

        JPanel regForm = new JPanel(new GridLayout(6, 1, 0, 8));
        regForm.setOpaque(false);

        nameField = new JTextField(); TricolourTheme.styleTextField(nameField);
        phoneField = new JTextField(); TricolourTheme.styleTextField(phoneField);
        licenseField = new JTextField(); TricolourTheme.styleTextField(licenseField);
        emailField = new JTextField(); TricolourTheme.styleTextField(emailField);
        addressField = new JTextField(); TricolourTheme.styleTextField(addressField);
        tierCombo = new JComboBox<>(new String[]{"REGULAR", "SILVER", "GOLD"});
        TricolourTheme.styleComboBox(tierCombo);

        regForm.add(createFieldWrapper("Full Legal Name *:", nameField));
        regForm.add(createFieldWrapper("Mobile Phone (10 digits) *:", phoneField));
        regForm.add(createFieldWrapper("Driving License No. *:", licenseField));
        regForm.add(createFieldWrapper("Email Address:", emailField));
        regForm.add(createFieldWrapper("Residential City / Address:", addressField));
        regForm.add(createFieldWrapper("Membership Tier:", tierCombo));

        regCard.add(regForm, BorderLayout.CENTER);

        JPanel regBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        regBtnRow.setOpaque(false);
        JButton regBtn = TricolourTheme.styledButton("Register Client", TricolourTheme.NAVY);
        regBtn.addActionListener(e -> registerCustomer());
        regBtnRow.add(regBtn);
        regCard.add(regBtnRow, BorderLayout.SOUTH);

        formsGrid.add(bookingCard);
        formsGrid.add(regCard);
        contentPanel.add(formsGrid, BorderLayout.CENTER);

        // ---------------------------------------------------------------------
        // Card 3: Booking Activity & Receipts Output (Bottom)
        // ---------------------------------------------------------------------
        TricolourTheme.RoundedCardPanel resultCard = new TricolourTheme.RoundedCardPanel(new BorderLayout(0, 6), 12, TricolourTheme.BORDER_GRAY);
        resultCard.setBorder(new EmptyBorder(10, 16, 10, 16));
        resultCard.setPreferredSize(new Dimension(0, 115));

        JLabel receiptTitle = new JLabel("Booking Status & Receipts");
        receiptTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        receiptTitle.setForeground(TricolourTheme.TEXT_MUTED);
        resultCard.add(receiptTitle, BorderLayout.NORTH);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        resultArea.setBackground(TricolourTheme.WHITE);
        resultArea.setForeground(TricolourTheme.TEXT_DARK);
        resultArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        resultArea.setText("Select a vehicle and registered customer above to generate a new rental reservation.");

        resultCard.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        contentPanel.add(resultCard, BorderLayout.SOUTH);

        add(contentPanel, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createFieldWrapper(String label, JComponent input) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TricolourTheme.TEXT_DARK);
        p.add(lbl, BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);
        return p;
    }

    public void refreshData() {
        try {
            availableCars = service.getAvailableCars();
            allCustomers = service.getAllCustomers();

            carCombo.removeAllItems();
            for (Car c : availableCars) {
                carCombo.addItem(String.format("%s - %s %s [%s]  |  \u20B9%.0f/day",
                        c.getRegistrationNumber(), c.getMake(), c.getModel(), c.getVehicleType(), c.getDailyRate()));
            }

            custCombo.removeAllItems();
            for (Customer c : allCustomers) {
                custCombo.addItem(String.format("%s  (%s Tier - %.0f%% off)  |  Ph: %s",
                        c.getFullName(), c.getMemberTier(), c.getTierDiscountPercent(), c.getPhone()));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading lists: " + ex.getMessage(),
                    "Data Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void bookCar() {
        if (carCombo.getSelectedItem() == null || custCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Please select both a vehicle and a customer.",
                    "Missing Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int carId = availableCars.get(carCombo.getSelectedIndex()).getCarId();
            int custId = allCustomers.get(custCombo.getSelectedIndex()).getCustomerId();
            int days = Integer.parseInt(daysField.getText().trim());
            boolean ins = insuranceCheck.isSelected();

            Rental r = service.bookCar(custId, carId, days, ins);
            resultArea.setText(String.format(
                    "CONFIRMED  |  Rental #%d  |  Vehicle: %s  |  Customer: %s\n" +
                    "Duration   : %d Days  |  Due Return: %s\n" +
                    "Financials : Base: \u20B9%.2f  |  Discount: -\u20B9%.2f  |  Insurance: \u20B9%.2f  |  Total Charged: \u20B9%.2f",
                    r.getRentalId(), carCombo.getSelectedItem(),
                    allCustomers.get(custCombo.getSelectedIndex()).getFullName(),
                    days, r.getExpectedReturnDate(),
                    r.getBaseRent(), r.getDiscountAmount(),
                    r.getInsuranceFee(), r.getTotalAmount()));
            refreshData();
            if (globalRefresh != null) globalRefresh.run();

        } catch (CarUnavailableException ex) {
            resultArea.setText("RESERVATION FAILED: " + ex.getMessage()
                    + " [Plate: " + ex.getRegistrationNumber() + " | State: " + ex.getStatus() + "]");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive integer for rental days.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            resultArea.setText("ERROR: " + ex.getMessage());
        }
    }

    private void registerCustomer() {
        try {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String license = licenseField.getText().trim();
            String email = emailField.getText().trim();
            String address = addressField.getText().trim();
            String tier = (String) tierCombo.getSelectedItem();

            Customer c = new Customer(name, phone, license, email, address,
                    Customer.MemberTier.fromString(tier));
            Customer registered = service.registerCustomer(c);
            JOptionPane.showMessageDialog(this,
                    "Client successfully registered! Assigned ID: #" + registered.getCustomerId(),
                    "Registration Complete", JOptionPane.INFORMATION_MESSAGE);

            nameField.setText(""); phoneField.setText(""); licenseField.setText("");
            emailField.setText(""); addressField.setText("");
            refreshData();
            if (globalRefresh != null) globalRefresh.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Registration failed: " + ex.getMessage(),
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
