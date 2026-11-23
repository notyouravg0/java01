package carrental.ui;

import carrental.db.DBConnection;
import carrental.service.RentalService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Main Application Frame - Car Rental System.
 * Clean, professional desktop UI with Indian Tricolour accents and rounded components.
 * Syllabus Unit 5: Swing BorderLayout, GridLayout, Mouse Events.
 */
public class CarRentalApp extends JFrame {

    private final RentalService service;
    private CarInventoryPanel inventoryPanel;
    private CustomerBookingPanel bookingPanel;
    private RentCalculationPanel calcPanel;
    private ReturnInspectionPanel returnPanel;
    private RentalHistoryPanel historyPanel;

    public CarRentalApp() {
        // Initialize database if not already ready
        initDatabase();

        service = new RentalService();

        setTitle("Car Rental System - Fleet & Reservation Portal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 780);
        setMinimumSize(new Dimension(1020, 680));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(TricolourTheme.BG_CREAM);

        // =====================================================================
        // Top Navigation & Branding Bar (Sleek Slate Navy with Tricolour Accent)
        // =====================================================================
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(0x0F, 0x17, 0x2A)); // Slate 900
        topBar.setPreferredSize(new Dimension(0, 62));
        topBar.setBorder(new EmptyBorder(10, 24, 10, 24));

        // Left Branding
        JPanel brandPanel = new JPanel(new GridLayout(2, 1, 0, 1));
        brandPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("CAR RENTAL SYSTEM");
        titleLabel.setFont(TricolourTheme.APP_TITLE_FONT);
        titleLabel.setForeground(TricolourTheme.WHITE);

        JLabel subtitleLabel = new JLabel("Enterprise Fleet Management & Reservation Portal");
        subtitleLabel.setFont(TricolourTheme.SUBTITLE_FONT);
        subtitleLabel.setForeground(new Color(0x94, 0xA3, 0xB8)); // Slate 400

        brandPanel.add(titleLabel);
        brandPanel.add(subtitleLabel);
        topBar.add(brandPanel, BorderLayout.WEST);

        // Right Minimalist Tricolour Accent Badge
        JPanel tricolourBadge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 14));
        tricolourBadge.setOpaque(false);

        JPanel flagPill = new JPanel(new GridLayout(1, 3, 2, 0));
        flagPill.setPreferredSize(new Dimension(54, 8));
        flagPill.setOpaque(false);

        JPanel s1 = new JPanel(); s1.setBackground(TricolourTheme.SAFFRON);
        JPanel s2 = new JPanel(); s2.setBackground(TricolourTheme.WHITE);
        JPanel s3 = new JPanel(); s3.setBackground(TricolourTheme.GREEN);
        flagPill.add(s1); flagPill.add(s2); flagPill.add(s3);

        tricolourBadge.add(flagPill);
        topBar.add(tricolourBadge, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // =====================================================================
        // Modern Tabbed Navigation (Center)
        // =====================================================================
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(TricolourTheme.WHITE);
        tabs.setForeground(TricolourTheme.TEXT_DARK);
        tabs.setBorder(BorderFactory.createEmptyBorder());

        // Global refresh coordinator
        Runnable globalRefresh = () -> {
            if (inventoryPanel != null) inventoryPanel.refreshData();
            if (calcPanel != null) calcPanel.refreshData();
            if (returnPanel != null) returnPanel.refreshData();
            if (historyPanel != null) historyPanel.refreshData();
            if (bookingPanel != null) bookingPanel.refreshData();
        };

        inventoryPanel = new CarInventoryPanel(service);
        bookingPanel = new CustomerBookingPanel(service, globalRefresh);
        calcPanel = new RentCalculationPanel(service);
        returnPanel = new ReturnInspectionPanel(service, globalRefresh);
        historyPanel = new RentalHistoryPanel(service);

        tabs.addTab("  Fleet Inventory  ", inventoryPanel);
        tabs.addTab("  Reservations & Booking  ", bookingPanel);
        tabs.addTab("  Rate Calculator  ", calcPanel);
        tabs.addTab("  Return & Inspection  ", returnPanel);
        tabs.addTab("  Activity & Analytics  ", historyPanel);

        // Auto-refresh when switching tabs
        tabs.addChangeListener(e -> {
            Component sel = tabs.getSelectedComponent();
            if (sel instanceof CarInventoryPanel) ((CarInventoryPanel) sel).refreshData();
            else if (sel instanceof CustomerBookingPanel) ((CustomerBookingPanel) sel).refreshData();
            else if (sel instanceof RentCalculationPanel) ((RentCalculationPanel) sel).refreshData();
            else if (sel instanceof ReturnInspectionPanel) ((ReturnInspectionPanel) sel).refreshData();
            else if (sel instanceof RentalHistoryPanel) ((RentalHistoryPanel) sel).refreshData();
        });

        add(tabs, BorderLayout.CENTER);

        // (Bottom bar removed as requested: no team member/group/module info displayed)

        setVisible(true);
    }

    /**
     * Initializes the SQLite database: creates tables and seeds data if needed.
     */
    private void initDatabase() {
        try {
            File dbDir = new File("db");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }

            Connection conn = DBConnection.getConnection();

            File schemaFile = new File("db/schema.sql");
            if (schemaFile.exists()) {
                carrental.db.DBInit.executeSqlScript(conn, schemaFile);
            }

            try (Statement stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT COUNT(*) FROM cars")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    File seedFile = new File("db/seed_data.sql");
                    if (seedFile.exists()) {
                        carrental.db.DBInit.executeSqlScript(conn, seedFile);
                    }
                }
            }

            DBConnection.closeConnection(conn);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Database initialization notice: " + ex.getMessage() + "\n\n" +
                    "Please run start_db.bat to verify SQLite configuration.",
                    "Database Notice", JOptionPane.WARNING_MESSAGE);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(CarRentalApp::new);
    }
}
