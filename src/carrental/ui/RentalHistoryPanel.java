package carrental.ui;

import carrental.model.Rental;
import carrental.service.RentalService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

/**
 * Rental History & Fleet Analytics View.
 * Displays KPI cards and complete searchable transaction audit trail.
 * Clean card-based design with high-contrast table headers and matching borders.
 */
public class RentalHistoryPanel extends JPanel {

    private final RentalService service;
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JLabel revenueVal, activeVal, completedVal, damageVal;

    private static final String[] COLUMNS = {
            "Agreement #", "Vehicle #", "Client #", "Checkout Date", "Due Date",
            "Actual Return", "Days", "Base (\u20B9)", "Discount", "Insurance",
            "Late Fee", "Damage Fee", "Total (\u20B9)", "Status"
    };

    public RentalHistoryPanel(RentalService service) {
        this.service = service;
        setLayout(new BorderLayout(0, 0));
        setBackground(TricolourTheme.BG_CREAM);

        // Header
        add(TricolourTheme.createHeaderPanel("Rental Activity & Transaction Audit Trail"), BorderLayout.NORTH);

        // Main Content Area
        JPanel contentPanel = new JPanel(new BorderLayout(0, 14));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // ---------------------------------------------------------------------
        // Top: 4 Modern KPI Cards Grid
        // ---------------------------------------------------------------------
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.setPreferredSize(new Dimension(0, 88));

        revenueVal = new JLabel("\u20B90.00");
        activeVal = new JLabel("0");
        completedVal = new JLabel("0");
        damageVal = new JLabel("\u20B90.00");

        kpiGrid.add(createKPICard("Gross Revenue", revenueVal, TricolourTheme.GREEN));
        kpiGrid.add(createKPICard("Active Rentals", activeVal, TricolourTheme.SAFFRON));
        kpiGrid.add(createKPICard("Completed Rentals", completedVal, TricolourTheme.NAVY));
        kpiGrid.add(createKPICard("Damage Collected", damageVal, new Color(0xDC, 0x26, 0x26)));

        contentPanel.add(kpiGrid, BorderLayout.NORTH);

        // ---------------------------------------------------------------------
        // Center: Table Card with Toolbar
        // ---------------------------------------------------------------------
        TricolourTheme.RoundedCardPanel tableCard = new TricolourTheme.RoundedCardPanel(new BorderLayout(0, 12), 12, TricolourTheme.BORDER_GRAY);
        tableCard.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel tableToolbar = new JPanel(new BorderLayout());
        tableToolbar.setOpaque(false);

        JLabel tableTitle = new JLabel("All Rental Agreements & Transaction Ledger");
        tableTitle.setFont(TricolourTheme.SECTION_FONT);
        tableTitle.setForeground(TricolourTheme.TEXT_DARK);
        tableToolbar.add(tableTitle, BorderLayout.WEST);

        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightTools.setOpaque(false);

        JLabel hint = new JLabel("Tip: Double-click row for full breakdown");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hint.setForeground(TricolourTheme.TEXT_MUTED);
        rightTools.add(hint);

        JButton refreshBtn = TricolourTheme.styledButton("Refresh Audit", TricolourTheme.GREEN);
        refreshBtn.addActionListener(e -> refreshData());
        rightTools.add(refreshBtn);

        tableToolbar.add(rightTools, BorderLayout.EAST);
        tableCard.add(tableToolbar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        TricolourTheme.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(95);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && e.getClickCount() == 2) {
                    showRentalDetails(row);
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

    private JPanel createKPICard(String title, JLabel valueLabel, Color accentColor) {
        TricolourTheme.RoundedCardPanel card = new TricolourTheme.RoundedCardPanel(new BorderLayout(0, 4), 12, TricolourTheme.BORDER_GRAY);
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Top Accent Stripe
        JPanel stripe = new JPanel();
        stripe.setBackground(accentColor);
        stripe.setPreferredSize(new Dimension(0, 3));
        card.add(stripe, BorderLayout.NORTH);

        JPanel inner = new JPanel(new GridLayout(2, 1, 0, 2));
        inner.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLbl.setForeground(TricolourTheme.TEXT_MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 19));
        valueLabel.setForeground(TricolourTheme.TEXT_DARK);

        inner.add(titleLbl);
        inner.add(valueLabel);
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    public void refreshData() {
        tableModel.setRowCount(0);
        try {
            List<Rental> rentals = service.getAllRentals();
            for (Rental r : rentals) {
                tableModel.addRow(new Object[]{
                        "#" + r.getRentalId(),
                        "#" + r.getCarId(),
                        "#" + r.getCustomerId(),
                        r.getRentalDate(),
                        r.getExpectedReturnDate(),
                        r.getActualReturnDate() != null ? r.getActualReturnDate() : "-",
                        r.getRentalDays(),
                        String.format("%.2f", r.getBaseRent()),
                        String.format("%.2f", r.getDiscountAmount()),
                        String.format("%.2f", r.getInsuranceFee()),
                        String.format("%.2f", r.getLateFee()),
                        String.format("%.2f", r.getDamageFee()),
                        String.format("%.2f", r.getTotalAmount()),
                        r.getStatus()
                });
            }

            // Update KPI metric cards
            Map<String, Object> kpis = service.getKPISummary();
            revenueVal.setText("\u20B9" + String.format("%,.2f", (Double) kpis.get("totalRevenue")));
            activeVal.setText(String.valueOf(kpis.get("activeRentalsCount")));
            completedVal.setText(String.valueOf(kpis.get("completedRentalsCount")));
            damageVal.setText("\u20B9" + String.format("%,.2f", (Double) kpis.get("totalDamagesCollected")));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading history: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showRentalDetails(int row) {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================\n");
        sb.append("         RENTAL TRANSACTION DETAILS      \n");
        sb.append("=========================================\n");
        for (int i = 0; i < COLUMNS.length; i++) {
            sb.append(String.format("%-18s: %s\n", COLUMNS[i], tableModel.getValueAt(row, i)));
        }
        sb.append("=========================================\n");
        JOptionPane.showMessageDialog(this, sb.toString(),
                "Rental Record", JOptionPane.INFORMATION_MESSAGE);
    }
}
