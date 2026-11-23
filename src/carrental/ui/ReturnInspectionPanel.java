package carrental.ui;

import carrental.model.Rental;
import carrental.service.RentalService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Vehicle Return & Inspection View.
 * Settlement processing, odometer verification, damage assessment, and closing invoice.
 * Clean card layout with rounded inputs and settlement statement.
 */
public class ReturnInspectionPanel extends JPanel {

    private final RentalService service;
    private final JComboBox<String> rentalCombo;
    private final JTextField mileageField;
    private final JTextField damageField;
    private final JTextField notesField;
    private final JTextArea resultArea;
    private final Runnable globalRefresh;

    private List<Rental> activeRentals;

    public ReturnInspectionPanel(RentalService service, Runnable globalRefresh) {
        this.service = service;
        this.globalRefresh = globalRefresh;
        setLayout(new BorderLayout(0, 0));
        setBackground(TricolourTheme.BG_CREAM);

        // Header
        add(TricolourTheme.createHeaderPanel("Vehicle Return & Inspection Processing"), BorderLayout.NORTH);

        // Content Area
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // ---------------------------------------------------------------------
        // Left Card: Return Inspection Form
        // ---------------------------------------------------------------------
        TricolourTheme.RoundedCardPanel formCard = new TricolourTheme.RoundedCardPanel(new BorderLayout(0, 16), 12, TricolourTheme.BORDER_GRAY);
        formCard.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel formTitle = new JLabel("Check-in & Inspection Parameters");
        formTitle.setFont(TricolourTheme.HEADER_FONT);
        formTitle.setForeground(TricolourTheme.NAVY);
        formCard.add(formTitle, BorderLayout.NORTH);

        JPanel inputsGrid = new JPanel(new GridLayout(4, 1, 0, 12));
        inputsGrid.setOpaque(false);

        rentalCombo = new JComboBox<>();
        TricolourTheme.styleComboBox(rentalCombo);
        rentalCombo.addActionListener(e -> onRentalSelected());

        mileageField = new JTextField();
        TricolourTheme.styleTextField(mileageField);

        damageField = new JTextField("0");
        TricolourTheme.styleTextField(damageField);

        notesField = new JTextField("Clean vehicle return; normal wear.");
        TricolourTheme.styleTextField(notesField);

        inputsGrid.add(createFieldWrapper("Select Active Agreement:", rentalCombo));
        inputsGrid.add(createFieldWrapper("Final Odometer Reading (km) *:", mileageField));
        inputsGrid.add(createFieldWrapper("Assessed Repair / Damage Fee (\u20B9):", damageField));
        inputsGrid.add(createFieldWrapper("Inspection Inspector Notes:", notesField));

        formCard.add(inputsGrid, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);

        JButton returnBtn = TricolourTheme.styledButton("Process Return", TricolourTheme.GREEN);
        returnBtn.addActionListener(e -> processReturn());

        JButton refreshBtn = TricolourTheme.styledButton("Refresh Active", TricolourTheme.SAFFRON);
        refreshBtn.addActionListener(e -> refreshData());

        btnRow.add(returnBtn);
        btnRow.add(refreshBtn);
        formCard.add(btnRow, BorderLayout.SOUTH);

        // ---------------------------------------------------------------------
        // Right Card: Final Settlement Statement
        // ---------------------------------------------------------------------
        TricolourTheme.RoundedCardPanel resultCard = new TricolourTheme.RoundedCardPanel(new BorderLayout(0, 10), 12, TricolourTheme.BORDER_GRAY);
        resultCard.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel resultTitle = new JLabel("Settlement Statement & Closing Audit");
        resultTitle.setFont(TricolourTheme.HEADER_FONT);
        resultTitle.setForeground(TricolourTheme.NAVY);
        resultCard.add(resultTitle, BorderLayout.NORTH);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        resultArea.setBackground(TricolourTheme.WHITE);
        resultArea.setForeground(TricolourTheme.TEXT_DARK);
        resultArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JScrollPane scroll = new JScrollPane(resultArea);
        scroll.setBorder(new TricolourTheme.RoundedBorder(TricolourTheme.BORDER_GRAY, 8));
        resultCard.add(scroll, BorderLayout.CENTER);

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

    private void onRentalSelected() {
        int idx = rentalCombo.getSelectedIndex();
        if (idx >= 0 && idx < activeRentals.size()) {
            Rental r = activeRentals.get(idx);
            double estimatedEnd = r.getStartMileage() + 150.0;
            mileageField.setText(String.format("%.0f", estimatedEnd));
            damageField.setText("0");
        }
    }

    public void refreshData() {
        try {
            activeRentals = service.getActiveRentals();
            rentalCombo.removeAllItems();
            for (Rental r : activeRentals) {
                rentalCombo.addItem(String.format("Rental #%d  |  Vehicle #%d  |  Client #%d  |  Due: %s",
                        r.getRentalId(), r.getCarId(), r.getCustomerId(), r.getExpectedReturnDate()));
            }
            if (activeRentals.isEmpty()) {
                resultArea.setText("No currently active rentals pending return.");
                mileageField.setText("");
            } else {
                onRentalSelected();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error fetching active rentals: " + ex.getMessage(),
                    "Data Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void processReturn() {
        if (rentalCombo.getSelectedItem() == null || activeRentals.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No active rental agreement selected.",
                    "Missing Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Rental selected = activeRentals.get(rentalCombo.getSelectedIndex());
            double endMileage = Double.parseDouble(mileageField.getText().trim());
            double damageFee = Double.parseDouble(damageField.getText().trim());
            String notes = notesField.getText().trim();

            Rental returned = service.processReturn(
                    selected.getRentalId(),
                    LocalDate.now(),
                    endMileage,
                    damageFee,
                    notes
            );

            StringBuilder sb = new StringBuilder();
            sb.append("============================================================\n");
            sb.append("               SETTLEMENT CLOSING STATEMENT                 \n");
            sb.append("============================================================\n\n");
            sb.append(String.format("Agreement ID       : #%d (CLOSED)\n", returned.getRentalId()));
            sb.append(String.format("Vehicle ID         : #%d\n", returned.getCarId()));
            sb.append(String.format("Date Checked Out   : %s\n", returned.getRentalDate()));
            sb.append(String.format("Date Returned      : %s\n", returned.getActualReturnDate()));
            sb.append(String.format("Scheduled Duration : %d Days\n", returned.getRentalDays()));
            sb.append("------------------------------------------------------------\n");
            sb.append("ODOMETER METRICS:\n");
            sb.append(String.format("  Starting Mileage : %,.1f km\n", returned.getStartMileage()));
            sb.append(String.format("  Ending Mileage   : %,.1f km\n", returned.getEndMileage()));
            sb.append(String.format("  Distance Driven  : %,.1f km\n\n", returned.getEndMileage() - returned.getStartMileage()));
            sb.append("------------------------------------------------------------\n");
            sb.append("FINANCIAL RECONCILIATION:\n");
            sb.append(String.format("  Agreed Base Rent :  \u20B9%,.2f\n", returned.getBaseRent()));
            sb.append(String.format("  Applied Discount : -\u20B9%,.2f\n", returned.getDiscountAmount()));
            sb.append(String.format("  Insurance Fee    :  \u20B9%,.2f\n", returned.getInsuranceFee()));
            sb.append(String.format("  Late Surcharge   :  \u20B9%,.2f\n", returned.getLateFee()));
            sb.append(String.format("  Assessed Damage  :  \u20B9%,.2f\n", returned.getDamageFee()));
            sb.append("------------------------------------------------------------\n");
            sb.append(String.format("NET TOTAL BILLED   :  \u20B9%,.2f\n", returned.getTotalAmount()));
            sb.append("============================================================\n\n");
            sb.append(String.format("Inspection Report  : %s\n", returned.getNotes()));
            sb.append(String.format("Vehicle Availability: %s\n", (damageFee > 5000.0 ? "Sent to MAINTENANCE" : "Restored to AVAILABLE")));

            resultArea.setText(sb.toString());
            refreshData();
            if (globalRefresh != null) globalRefresh.run();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric values for mileage and damages.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            resultArea.setText("Settlement Error: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Processing failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
