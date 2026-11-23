package carrental.ui;

import carrental.model.Car;
import carrental.service.RentalService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Fleet Inventory View.
 * Displays vehicle fleet with real-time status, specs, and maintenance toggle.
 * Minimalist, modern card layout with high-contrast table headers and matching borders.
 */
public class CarInventoryPanel extends JPanel {

    private final RentalService service;
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JLabel statsLabel;

    private static final String[] COLUMNS = {
            "ID", "Plate Number", "Category", "Make", "Model", "Year",
            "Daily Rate (\u20B9)", "Fuel", "Transmission", "Seats", "Current Status"
    };

    public CarInventoryPanel(RentalService service) {
        this.service = service;
        setLayout(new BorderLayout(0, 0));
        setBackground(TricolourTheme.BG_CREAM);

        // Header
        add(TricolourTheme.createHeaderPanel("Fleet Inventory"), BorderLayout.NORTH);

        // Main Content Area with generous margins
        JPanel contentPanel = new JPanel(new BorderLayout(0, 14));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Action Toolbar Card (Top)
        TricolourTheme.RoundedCardPanel toolbarCard = new TricolourTheme.RoundedCardPanel(new BorderLayout(), 12, TricolourTheme.BORDER_GRAY);
        toolbarCard.setBorder(new EmptyBorder(12, 18, 12, 18));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftActions.setOpaque(false);

        JButton refreshBtn = TricolourTheme.styledButton("Refresh Fleet", TricolourTheme.GREEN);
        refreshBtn.addActionListener(e -> refreshData());

        JButton toggleBtn = TricolourTheme.styledButton("Toggle Maintenance", TricolourTheme.SAFFRON);
        toggleBtn.addActionListener(e -> toggleMaintenance());

        leftActions.add(refreshBtn);
        leftActions.add(toggleBtn);
        toolbarCard.add(leftActions, BorderLayout.WEST);

        statsLabel = new JLabel("Loading fleet...");
        statsLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statsLabel.setForeground(TricolourTheme.TEXT_DARK);
        JPanel rightInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        rightInfo.setOpaque(false);
        rightInfo.add(statsLabel);
        toolbarCard.add(rightInfo, BorderLayout.EAST);

        contentPanel.add(toolbarCard, BorderLayout.NORTH);

        // Table Card Container
        TricolourTheme.RoundedCardPanel tableCard = new TricolourTheme.RoundedCardPanel(new BorderLayout(0, 12), 12, TricolourTheme.BORDER_GRAY);
        tableCard.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Prominent, High-Contrast Table Section Title
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel tableTitle = new JLabel("Vehicle Fleet Directory");
        tableTitle.setFont(TricolourTheme.SECTION_FONT);
        tableTitle.setForeground(TricolourTheme.TEXT_DARK);
        titleRow.add(tableTitle, BorderLayout.WEST);

        JLabel hintLabel = new JLabel("Tip: Double-click any row to view full specifications");
        hintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hintLabel.setForeground(TricolourTheme.TEXT_MUTED);
        titleRow.add(hintLabel, BorderLayout.EAST);

        tableCard.add(titleRow, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        TricolourTheme.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(45);
        table.getColumnModel().getColumn(1).setPreferredWidth(125);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && e.getClickCount() == 2) {
                    showCarDetails(row);
                }
            }
        });

        // Framed Table ScrollPane with unified matching border
        JScrollPane scroll = TricolourTheme.createTableScrollPane(table);
        tableCard.add(scroll, BorderLayout.CENTER);

        contentPanel.add(tableCard, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        refreshData();
    }

    public void refreshData() {
        tableModel.setRowCount(0);
        try {
            List<Car> cars = service.getAllCars();
            int availableCount = 0;
            for (Car c : cars) {
                if (c.isAvailable()) availableCount++;
                tableModel.addRow(new Object[]{
                        c.getCarId(),
                        c.getRegistrationNumber(),
                        c.getVehicleType(),
                        c.getMake(),
                        c.getModel(),
                        c.getYear(),
                        String.format("%.2f", c.getDailyRate()),
                        c.getFuelType(),
                        c.getTransmission(),
                        c.getSeatingCapacity(),
                        c.getStatus()
                });
            }
            statsLabel.setText(String.format("Fleet: %d vehicles  |  Available: %d",
                    cars.size(), availableCount));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading fleet: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showCarDetails(int row) {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================\n");
        sb.append("             VEHICLE DETAILS             \n");
        sb.append("=========================================\n");
        for (int i = 0; i < COLUMNS.length; i++) {
            sb.append(String.format("%-18s: %s\n", COLUMNS[i], tableModel.getValueAt(row, i)));
        }
        sb.append("=========================================\n");
        JOptionPane.showMessageDialog(this, sb.toString(),
                "Vehicle Specifications", JOptionPane.INFORMATION_MESSAGE);
    }

    private void toggleMaintenance() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a vehicle row from the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int carId = (int) tableModel.getValueAt(row, 0);
        String current = (String) tableModel.getValueAt(row, 10);
        try {
            if ("MAINTENANCE".equals(current)) {
                service.toggleMaintenance(carId, false);
            } else if ("AVAILABLE".equals(current)) {
                service.toggleMaintenance(carId, true);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Cannot toggle: vehicle is currently " + current + ".",
                        "Status Conflict", JOptionPane.WARNING_MESSAGE);
                return;
            }
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error updating status: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
