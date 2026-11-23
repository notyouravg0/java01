package carrental.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * Modern Indian Tricolour Theme constants, rounded UI components, and styling utilities.
 * Professional, clean card-based design with rounded input boxes, buttons, and high-contrast tables.
 */
public class TricolourTheme {

    // Indian Flag Color Tokens
    public static final Color SAFFRON = new Color(0xFF, 0x99, 0x33);       // #FF9933
    public static final Color WHITE = new Color(0xFF, 0xFF, 0xFF);         // #FFFFFF
    public static final Color GREEN = new Color(0x13, 0x88, 0x08);         // #138808
    public static final Color NAVY = new Color(0x00, 0x00, 0x80);          // #000080

    // Modern Enterprise Color Tokens
    public static final Color NAVY_DARK = new Color(0x0F, 0x17, 0x2A);     // Slate 900
    public static final Color NAVY_HEADER = new Color(0x1E, 0x29, 0x3B);   // Slate 800 - Table Header
    public static final Color SAFFRON_LIGHT = new Color(0xFE, 0xF3, 0xC7); // Warm Saffron tint (Selection)
    public static final Color SAFFRON_HOVER = new Color(0xEA, 0x86, 0x24);
    public static final Color GREEN_LIGHT = new Color(0xDC, 0xFC, 0xE7);   // Soft emerald tint
    public static final Color GREEN_DARK = new Color(0x0E, 0x6E, 0x06);    // Deep emerald hover
    public static final Color BG_CREAM = new Color(0xF8, 0xFA, 0xFC);      // Slate light gray background
    public static final Color BG_CARD = new Color(0xFF, 0xFF, 0xFF);       // Card background
    public static final Color TEXT_DARK = new Color(0x0F, 0x17, 0x2A);     // Slate 900 - High contrast text
    public static final Color TEXT_MUTED = new Color(0x47, 0x55, 0x69);    // Slate 600 - Readable secondary
    public static final Color ROW_ALT = new Color(0xF8, 0xFA, 0xFC);       // Soft zebra stripe
    public static final Color BORDER_GRAY = new Color(0xCB, 0xD5, 0xE1);   // Slate 300 - Clean visible border
    public static final Color BORDER_INPUT = new Color(0x94, 0xA3, 0xB8);  // Slate 400 - Input border
    public static final Color INPUT_BG = new Color(0xFF, 0xFF, 0xFF);      // Clean input background
    public static final Color FOCUS_COLOR = new Color(0xFF, 0x99, 0x33);   // Saffron focus outline

    // Typography
    public static final Font APP_TITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 17);
    public static final Font SECTION_FONT = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font TABLE_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 12);

    // =========================================================================
    // Rounded Custom Border
    // =========================================================================
    public static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        private final int thickness;
        private final Insets insets;

        public RoundedBorder(Color color, int radius, int thickness, Insets insets) {
            this.color = color;
            this.radius = radius;
            this.thickness = thickness;
            this.insets = insets != null ? insets : new Insets(6, 12, 6, 12);
        }

        public RoundedBorder(Color color, int radius) {
            this(color, radius, 1, new Insets(6, 12, 6, 12));
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.draw(new RoundRectangle2D.Float(
                    x + thickness / 2.0f,
                    y + thickness / 2.0f,
                    width - thickness,
                    height - thickness,
                    radius,
                    radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return insets;
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = this.insets.left;
            insets.top = this.insets.top;
            insets.right = this.insets.right;
            insets.bottom = this.insets.bottom;
            return insets;
        }
    }

    // =========================================================================
    // Rounded Card Panel
    // =========================================================================
    public static class RoundedCardPanel extends JPanel {
        private final int radius;
        private final Color borderColor;

        public RoundedCardPanel(LayoutManager layout, int radius, Color borderColor) {
            super(layout);
            this.radius = radius;
            this.borderColor = borderColor;
            setOpaque(false);
            setBackground(BG_CARD);
        }

        public RoundedCardPanel(int radius) {
            this(new BorderLayout(), radius, BORDER_GRAY);
        }

        public RoundedCardPanel() {
            this(12);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Card background fill
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, radius, radius));
            // Card border
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, getWidth() - 1.2f, getHeight() - 1.2f, radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =========================================================================
    // Modern Rounded Button
    // =========================================================================
    public static JButton styledButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color currentColor = getBackground();
                if (getModel().isPressed()) {
                    currentColor = bg.darker();
                } else if (getModel().isRollover()) {
                    if (bg.equals(GREEN)) currentColor = GREEN_DARK;
                    else if (bg.equals(SAFFRON)) currentColor = SAFFRON_HOVER;
                    else currentColor = bg.darker();
                }
                g2.setColor(currentColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(BUTTON_FONT);
        btn.setBackground(bg);
        btn.setForeground(WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(140, 36));
        btn.setBorder(new EmptyBorder(6, 16, 6, 16));
        return btn;
    }

    // =========================================================================
    // Rounded Text Field Styler
    // =========================================================================
    public static void styleTextField(JTextField field) {
        field.setFont(LABEL_FONT);
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_DARK);
        field.setCaretColor(TEXT_DARK);
        field.setBorder(new RoundedBorder(BORDER_INPUT, 10, 1, new Insets(7, 12, 7, 12)));
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 36));
    }

    // =========================================================================
    // Rounded Selection Box (JComboBox) Styler
    // =========================================================================
    public static <T> void styleComboBox(JComboBox<T> combo) {
        combo.setFont(LABEL_FONT);
        combo.setBackground(INPUT_BG);
        combo.setForeground(TEXT_DARK);
        combo.setBorder(new RoundedBorder(BORDER_INPUT, 10, 1, new Insets(4, 10, 4, 8)));
        combo.setPreferredSize(new Dimension(combo.getPreferredSize().width, 36));
        combo.setOpaque(false);

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setFont(LABEL_FONT);
                lbl.setBorder(new EmptyBorder(7, 12, 7, 12));
                if (isSelected) {
                    lbl.setBackground(SAFFRON_LIGHT);
                    lbl.setForeground(TEXT_DARK);
                } else {
                    lbl.setBackground(WHITE);
                    lbl.setForeground(TEXT_DARK);
                }
                return lbl;
            }
        });
    }

    // =========================================================================
    // Modern High-Contrast Table Styler
    // =========================================================================
    public static void styleTable(JTable table) {
        table.setFont(TABLE_FONT);
        table.setRowHeight(36);
        table.setGridColor(new Color(0xE2, 0xE8, 0xF0));
        table.setSelectionBackground(SAFFRON_LIGHT);
        table.setSelectionForeground(TEXT_DARK);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setBackground(WHITE);

        // Header Styling with Custom Renderer ensuring bold dark-slate background and bright white font
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(NAVY_HEADER);
        header.setForeground(WHITE);
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setOpaque(true);

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                lbl.setOpaque(true);
                lbl.setBackground(NAVY_HEADER);
                lbl.setForeground(WHITE);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                lbl.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(0x33, 0x41, 0x55)),
                        new EmptyBorder(0, 12, 0, 12)
                ));
                return lbl;
            }
        });

        // Cell Styling with comfortable padding and high-contrast alternating rows
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                if (isSelected) {
                    c.setBackground(SAFFRON_LIGHT);
                    c.setForeground(TEXT_DARK);
                } else {
                    c.setBackground(row % 2 == 0 ? WHITE : ROW_ALT);
                    c.setForeground(TEXT_DARK);
                }
                return c;
            }
        });
    }

    /**
     * Wraps a styled JTable in a cleanly framed JScrollPane with matching borders.
     */
    public static JScrollPane createTableScrollPane(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(WHITE);
        scroll.setBackground(WHITE);
        scroll.setBorder(new RoundedBorder(BORDER_GRAY, 8, 1, new Insets(0, 0, 0, 0)));

        // Corner component to match table header
        JPanel corner = new JPanel();
        corner.setBackground(NAVY_HEADER);
        scroll.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);

        return scroll;
    }

    // =========================================================================
    // Header Panel for Views
    // =========================================================================
    public static JPanel createHeaderPanel(String title) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(WHITE);
        header.setPreferredSize(new Dimension(0, 50));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_GRAY),
                new EmptyBorder(8, 20, 8, 20)));

        JLabel lbl = new JLabel(title);
        lbl.setFont(TITLE_FONT);
        lbl.setForeground(TEXT_DARK);
        header.add(lbl, BorderLayout.WEST);

        // Minimalist Tricolour Accent Indicator
        JPanel stripe = new JPanel(new GridLayout(1, 3, 2, 0));
        stripe.setOpaque(false);
        stripe.setPreferredSize(new Dimension(42, 6));

        JPanel p1 = new JPanel(); p1.setBackground(SAFFRON);
        JPanel p2 = new JPanel(); p2.setBackground(new Color(0xE2, 0xE8, 0xF0));
        JPanel p3 = new JPanel(); p3.setBackground(GREEN);
        stripe.add(p1); stripe.add(p2); stripe.add(p3);

        JPanel stripeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 13));
        stripeWrapper.setOpaque(false);
        stripeWrapper.add(stripe);
        header.add(stripeWrapper, BorderLayout.EAST);

        return header;
    }

    /** Compatibility footer (kept invisible / zero-height for backwards compatibility) */
    public static JPanel createFooterPanel(String text) {
        JPanel footer = new JPanel();
        footer.setPreferredSize(new Dimension(0, 0));
        footer.setVisible(false);
        return footer;
    }

    /** Creates a labelled text field pair in a mini-panel. */
    public static JPanel labeledField(String labelText, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(LABEL_FONT);
        lbl.setForeground(TEXT_DARK);
        styleTextField(field);
        p.add(lbl, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    /** Creates a clean section border for cards */
    public static Border sectionBorder(String title) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        new RoundedBorder(BORDER_GRAY, 10),
                        "  " + title + "  ", 0, 0, HEADER_FONT, NAVY),
                new EmptyBorder(10, 12, 10, 12));
    }
}
