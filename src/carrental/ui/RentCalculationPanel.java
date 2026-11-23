package carrental.ui;

import carrental.model.Car;
import carrental.model.Customer;
import carrental.service.RentalService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Rental Rate & Cost Calculator View.
 * Interactive quotation tool showing tier discounts, insurance add-ons, and pricing breakdowns.
 * Clean card styling with rounded inputs and live quotation view.
 */
public class RentCalculationPanel extends JPanel {

    private final RentalService service;
    private final JComboBox<String> carCombo;
    private final JComboBox<String> custCombo;
    private final JTextField daysField;
    private final JCheckBox insuranceCheck;
    private final JTextArea resultArea;

    private List<Car> allCars;
    private List<Customer> allCustomers;

    public RentCalculationPanel(RentalService service) {
        this.service = service;
        setLayout(new BorderLayout(0, 0));
        setBackground(TricolourTheme.BG_CREAM);

        // Header
        add(TricolourTheme.createHeaderPanel("Rental Rate & Quotation Calculator"), BorderLayout.NORTH);

        // Content Area with margins
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // ---------------------------------------------------------------------
        // Left: Calculator Configuration Card
        // ---------------------------------------------------------------------
        TricolourTheme.RoundedCardPanel formCard = new TricolourTheme.RoundedCardPanel(new BorderLayout(0, 16), 12, TricolourTheme.BORDER_GRAY);
        formCard.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel formTitle = new JLabel("Quotation Parameters");
        formTitle.setFont(TricolourTheme.HEADER_FONT);
        formTitle.setForeground(TricolourTheme.NAVY);
        formCard.add(formTitle, BorderLayout.NORTH);

        JPanel inputsGrid = new JPanel(new GridLayout(4, 1, 0, 12));
        inputsGrid.setOpaque(false);

        carCombo = new JComboBox<>();
        TricolourTheme.styleComboBox(carCombo);

        custCombo = new JComboBox<>();
        TricolourTheme.styleComboBox(custCombo);

        daysField = new JTextField("3");
        TricolourTheme.styleTextField(daysField);

        insuranceCheck = new JCheckBox("Include Comprehensive Insurance (\u20B9250.00 / day)");
        insuranceCheck.setFont(TricolourTheme.LABEL_FONT);
        insuranceCheck.setForeground(TricolourTheme.TEXT_DARK);
        insuranceCheck.setOpaque(false);

        inputsGrid.add(createFieldWrapper("Target Vehicle:", carCombo));
        inputsGrid.add(createFieldWrapper("Customer Tier / Discount Profile:", custCombo));
        inputsGrid.add(createFieldWrapper("Estimated Rental Days:", daysField));
        inputsGrid.add(insuranceCheck);

        formCard.add(inputsGrid, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);

        JButton calcBtn = TricolourTheme.styledButton("Calculate Quotation", TricolourTheme.GREEN);
        calcBtn.addActionListener(e -> calculateRent());

        JButton refreshBtn = TricolourTheme.styledButton("Refresh Lists", TricolourTheme.SAFFRON);
        refreshBtn.addActionListener(e -> refreshData());

        btnRow.add(calcBtn);
        btnRow.add(refreshBtn);
        formCard.add(btnRow, BorderLayout.SOUTH);

        // ---------------------------------------------------------------------
        // Right: Detailed Quotation Breakdown Card
        // ---------------------------------------------------------------------
        TricolourTheme.RoundedCardPanel resultCard = new TricolourTheme.RoundedCardPanel(new BorderLayout(0, 10), 12, TricolourTheme.BORDER_GRAY);
        resultCard.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel resultTitle = new JLabel("Pricing Breakdown & Rate Quotation");
        resultTitle.setFont(TricolourTheme.HEADER_FONT);
        resultTitle.setForeground(TricolourTheme.NAVY);
        resultCard.add(resultTitle, BorderLayout.NORTH);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        resultArea.setBackground(TricolourTheme.WHITE);
        resultArea.setForeground(TricolourTheme.TEXT_DARK);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JScrollPane resultScroll = new JScrollPane(resultArea);
        resultScroll.setBorder(new TricolourTheme.RoundedBorder(TricolourTheme.BORDER_GRAY, 8));
        resultCard.add(resultScroll, BorderLayout.CENTER);

        contentPanel.add(formCard);
        contentPanel.add(resultCard);

        add(contentPanel, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel createFieldWrapper(String label, JComponent input) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
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
            allCars = service.getAllCars();
            allCustomers = service.getAllCustomers();

            carCombo.removeAllItems();
            for (Car c : allCars) {
                carCombo.addItem(String.format("%s - %s %s [%s]  (\u20B9%.0f/day)",
                        c.getRegistrationNumber(), c.getMake(), c.getModel(), c.getVehicleType(), c.getDailyRate()));
            }

            custCombo.removeAllItems();
            custCombo.addItem("Standard Rate (No Customer Discount - 0%)");
            for (Customer c : allCustomers) {
                custCombo.addItem(String.format("%s  (%s Tier - %.0f%% Discount)",
                        c.getFullName(), c.getMemberTier(), c.getTierDiscountPercent()));
            }

            if (!allCars.isEmpty()) {
                calculateRent();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading quotation data: " + ex.getMessage(),
                    "Data Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void calculateRent() {
        if (carCombo.getSelectedItem() == null || allCars.isEmpty()) {
            return;
        }
        try {
            Car car = allCars.get(carCombo.getSelectedIndex());
            int days = Integer.parseInt(daysField.getText().trim());
            boolean ins = insuranceCheck.isSelected();

            int custIdx = custCombo.getSelectedIndex();
            double discountPercent = 0.0;
            String custLabel = "Standard Client (0% Discount)";
            if (custIdx > 0 && custIdx - 1 < allCustomers.size()) {
                Customer cust = allCustomers.get(custIdx - 1);
                discountPercent = cust.getTierDiscountPercent();
                custLabel = cust.getFullName() + " (" + cust.getMemberTier() + " Tier)";
            }

            double baseRent = service.calculateRent(car, days);
            double discountedRent = service.calculateRent(car, days, discountPercent);
            double totalWithInsurance = service.calculateRent(car, days, discountPercent, ins);
            double savings = baseRent - discountedRent;
            double insuranceTotal = ins ? (days * 250.0) : 0.0;

            StringBuilder sb = new StringBuilder();
            sb.append("============================================================\n");
            sb.append("                 ESTIMATED RENTAL INVOICE                   \n");
            sb.append("============================================================\n\n");
            sb.append(String.format("Vehicle Plate    : %s\n", car.getRegistrationNumber()));
            sb.append(String.format("Model & Type     : %s %s (%s)\n", car.getMake(), car.getModel(), car.getVehicleType()));
            sb.append(String.format("Base Daily Rate  : \u20B9%,.2f / day\n", car.getDailyRate()));
            sb.append(String.format("Customer Profile : %s\n", custLabel));
            sb.append(String.format("Duration         : %d Days\n\n", days));
            sb.append("------------------------------------------------------------\n");
            sb.append("FEE BREAKDOWN:\n");
            sb.append(String.format("  Gross Base Rental Rate (%d days)   : \u20B9%,.2f\n", days, baseRent));
            if (discountPercent > 0.0) {
                sb.append(String.format("  Loyalty Tier Discount (-%.0f%%)       : -\u20B9%,.2f\n", discountPercent, savings));
                sb.append(String.format("  Subtotal After Tier Discount        : \u20B9%,.2f\n", discountedRent));
            } else {
                sb.append("  Loyalty Tier Discount               : None (0.00)\n");
            }

            if (ins) {
                sb.append(String.format("  Comprehensive Insurance (%d x \u20B9250): +\u20B9%,.2f\n", days, insuranceTotal));
            } else {
                sb.append("  Comprehensive Insurance Coverage    : Not Selected (\u20B90.00)\n");
            }
            sb.append("------------------------------------------------------------\n");
            sb.append(String.format("TOTAL ESTIMATED CHARGE                : \u20B9%,.2f\n", totalWithInsurance));
            sb.append("============================================================\n\n");
            sb.append("* Final settlement upon vehicle return may adjust for fuel, extra days, or vehicle condition.");

            resultArea.setText(sb.toString());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive number of days.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            resultArea.setText("Calculation Error: " + ex.getMessage());
        }
    }
}
