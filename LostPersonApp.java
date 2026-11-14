/*
 * Police Lost Person Management System - single-file version.
 *
 * Save as LostPersonApp.java, then:
 *   javac LostPersonApp.java
 *   java LostPersonApp
 * (or just press Run in VS Code)
 *
 * Demo login:  admin / police123
 */

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableModel;

// ============================================================
// LostPersonApp
// ============================================================
/**
 * Application entry point.
 *
 * Compile:  javac -d out src/police/lostperson/*.java
 * Run:      java -cp out police.lostperson.LostPersonApp
 */
public class LostPersonApp {

    public static void main(String[] args) {
        // Build all Swing windows on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                // Cross-platform look so custom button colors render the same everywhere
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // fall back to the default look and feel
            }
            new LoginFrame().setVisible(true);
        });
    }
}

// ============================================================
// Theme
// ============================================================
/** Shared colors, fonts, an HTML-wrap helper and a styled-button factory. */
final class Theme {
    static final Color NAVY = new Color(10, 36, 99);
    static final Color GOLD = new Color(212, 175, 55);
    static final Color RED = new Color(176, 24, 36);
    static final Font PLAIN = new Font("SansSerif", Font.PLAIN, 14);
    static final Font BOLD = new Font("SansSerif", Font.BOLD, 14);

    private Theme() { }

    /** Wraps text in HTML with a fixed width so long values wrap inside a JLabel. */
    static String html(String text, int widthPx) {
        String safe = text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\r", "")
                .replace("\n", "<br>");
        return "<html><body style='width:" + widthPx + "px'>" + safe + "</body></html>";
    }

    /** Creates a flat, colored button that renders the same on every look and feel. */
    static JButton button(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setUI(new BasicButtonUI()); // honors custom background colors
        b.setFont(BOLD);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}

// ============================================================
// Person
// ============================================================
/** Simple data holder for one lost-person record. */
class Person {
    private final String id;
    private final String fullName;
    private final int age;
    private final String gender;
    private final String height;
    private final String lastSeenLocation;
    private final LocalDate dateLastSeen;
    private final String clothing;
    private final String features;
    private final String contactName;
    private final String contactPhone;
    private final String notes;
    private final BufferedImage photo;   // may be null (no photo uploaded)
    private final LocalDateTime reportedOn = LocalDateTime.now();

    Person(String id, String fullName, int age, String gender, String height,
               String lastSeenLocation, LocalDate dateLastSeen, String clothing,
               String features, String contactName, String contactPhone, String notes,
               BufferedImage photo) {
        this.id = id;
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.height = height;
        this.lastSeenLocation = lastSeenLocation;
        this.dateLastSeen = dateLastSeen;
        this.clothing = clothing;
        this.features = features;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.notes = notes;
        this.photo = photo;
    }

    String getId() { return id; }
    String getFullName() { return fullName; }
    int getAge() { return age; }
    String getGender() { return gender; }
    String getHeight() { return height; }
    String getLastSeenLocation() { return lastSeenLocation; }
    LocalDate getDateLastSeen() { return dateLastSeen; }
    String getClothing() { return clothing; }
    String getFeatures() { return features; }
    String getContactName() { return contactName; }
    String getContactPhone() { return contactPhone; }
    String getNotes() { return notes; }
    BufferedImage getPhoto() { return photo; }
    LocalDateTime getReportedOn() { return reportedOn; }
}

// ============================================================
// RecordStore
// ============================================================
/**
 * Simple in-memory store for saved records. Records live as long as the program
 * runs (they survive logout/login) and are lost on exit. Replace the internals
 * with a database or file later without touching the windows.
 */
final class RecordStore {
    private static final List<Person> RECORDS = new ArrayList<>();
    private static int nextId = 1;

    private RecordStore() { }

    /** Returns the next case number, e.g. LP-001. */
    static String nextId() {
        return String.format("LP-%03d", nextId++);
    }

    static void add(Person person) {
        RECORDS.add(person);
    }

    /** Read-only live view of all saved records, oldest first. */
    static List<Person> getAll() {
        return Collections.unmodifiableList(RECORDS);
    }

    static int count() {
        return RECORDS.size();
    }
}

// ============================================================
// BadgeImage
// ============================================================
/** Loads the embedded Uganda Police badge (see BadgeData at the end of this file). */
final class BadgeImage {
    private static BufferedImage image;
    private static boolean loaded;

    private BadgeImage() { }

    /** The badge, or null if it could not be decoded (callers then draw a plain shield). */
    static synchronized BufferedImage get() {
        if (!loaded) {
            loaded = true;
            try {
                byte[] bytes = Base64.getDecoder().decode(BadgeData.base64());
                image = ImageIO.read(new ByteArrayInputStream(bytes));
            } catch (IOException | IllegalArgumentException ex) {
                image = null;
            }
        }
        return image;
    }

    /** Uses the badge as the window / taskbar icon. */
    static void applyIcon(Window window) {
        BufferedImage b = get();
        if (b != null) window.setIconImage(b);
    }
}

// ============================================================
// ShieldIcon
// ============================================================
/** Draws the Uganda Police badge on a white plate (falls back to a plain shield). */
class ShieldIcon extends JComponent {
    ShieldIcon() {
        this(70, 80);
    }

    ShieldIcon(int width, int height) {
        Dimension d = new Dimension(width, height);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        int w = getWidth();
        int h = getHeight();

        BufferedImage badge = BadgeImage.get();
        if (badge != null) {
            // White rounded plate so the blue badge stands out on navy backgrounds
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, w / 5, w / 5);
            int pad = Math.max(4, w / 14);
            double scale = Math.min((w - 2.0 * pad) / badge.getWidth(), (h - 2.0 * pad) / badge.getHeight());
            int iw = (int) (badge.getWidth() * scale);
            int ih = (int) (badge.getHeight() * scale);
            g2.drawImage(badge, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
            g2.dispose();
            return;
        }

        // Fallback: shield outline
        Path2D shield = new Path2D.Double();
        shield.moveTo(w * 0.1, h * 0.05);
        shield.lineTo(w * 0.9, h * 0.05);
        shield.lineTo(w * 0.9, h * 0.5);
        shield.curveTo(w * 0.9, h * 0.8, w * 0.5, h * 0.97, w * 0.5, h * 0.97);
        shield.curveTo(w * 0.5, h * 0.97, w * 0.1, h * 0.8, w * 0.1, h * 0.5);
        shield.closePath();

        g2.setColor(Theme.GOLD);
        g2.fill(shield);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(shield);

        // Five-pointed star in the middle
        double cx = w / 2.0;
        double cy = h * 0.46;
        double outer = w * 0.28;
        double inner = outer * 0.42;
        Path2D star = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = (i % 2 == 0) ? outer : inner;
            double angle = Math.toRadians(-90 + i * 36);
            double x = cx + r * Math.cos(angle);
            double y = cy + r * Math.sin(angle);
            if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
        }
        star.closePath();
        g2.setColor(Theme.NAVY);
        g2.fill(star);

        g2.dispose();
    }
}

// ============================================================
// PhotoPlaceholder
// ============================================================
/** Grey silhouette used where a photo would go; scales to any size. */
class PhotoPlaceholder extends JComponent {

    private BufferedImage photo;   // null = show the grey silhouette

    /** Shows the given picture instead of the silhouette (null goes back to the silhouette). */
    void setPhoto(BufferedImage photo) {
        this.photo = photo;
        repaint();
    }

    PhotoPlaceholder() {
        this(150, 180);
    }

    PhotoPlaceholder(int width, int height) {
        Dimension d = new Dimension(width, height);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = Math.max(16, w / 10);

        // Frame and background
        g2.setColor(new Color(230, 232, 238));
        g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
        g2.setColor(Theme.NAVY);
        g2.setStroke(new BasicStroke(Math.max(2f, w / 60f)));
        g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

        if (photo != null) {
            // Scale to fill the frame (centre-cropped), clipped to the rounded corners
            Shape oldClip = g2.getClip();
            g2.setClip(new java.awt.geom.RoundRectangle2D.Double(1, 1, w - 3, h - 3, arc, arc));
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            double scale = Math.max((double) w / photo.getWidth(), (double) h / photo.getHeight());
            int iw = (int) Math.ceil(photo.getWidth() * scale);
            int ih = (int) Math.ceil(photo.getHeight() * scale);
            g2.drawImage(photo, (w - iw) / 2, (h - ih) / 4, iw, ih, null);
            g2.setClip(oldClip);
            g2.setColor(Theme.NAVY);
            g2.setStroke(new BasicStroke(Math.max(2f, w / 60f)));
            g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
            g2.dispose();
            return;
        }

        // Head and shoulders (proportional to the component size)
        g2.setColor(new Color(175, 180, 192));
        int head = (int) (w * 0.37);
        g2.fillOval((w - head) / 2, (int) (h * 0.16), head, head);
        int shoulders = (int) (w * 0.73);
        g2.fillArc((w - shoulders) / 2, (int) (h * 0.55), shoulders, shoulders, 0, 180);

        // Caption
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, w / 12)));
        String text = "PHOTO";
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (w - tw) / 2, h - h / 15);

        g2.dispose();
    }
}

// ============================================================
// HeaderPanel
// ============================================================
/** Navy banner with the shield logo placeholder and system name. */
class HeaderPanel extends JPanel {
    HeaderPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.NAVY);
        setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));

        ShieldIcon shield = new ShieldIcon(110, 128);
        shield.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("POLICE LOST PERSON", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Management System - Admin Login", SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        subtitle.setForeground(Theme.GOLD);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(shield);
        add(Box.createVerticalStrut(14));
        add(title);
        add(Box.createVerticalStrut(4));
        add(subtitle);
    }
}

// ============================================================
// LoginFrame
// ============================================================
/**
 * Admin login window: username/password form under a police-themed header.
 * Demo credentials are hard-coded (admin / police123).
 */
class LoginFrame extends JFrame {

    // Hard-coded demo credentials (replace with a database check in production)
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "police123";

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18); // masked input

    public LoginFrame() {
        // Window title
        super("Police Lost Person Management System - Admin Login");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BadgeImage.applyIcon(this);
        setLayout(new BorderLayout());

        add(new HeaderPanel(), BorderLayout.NORTH);
        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        setSize(440, 670);
        setResizable(false);          // non-resizable
        setLocationRelativeTo(null);  // centered on screen
    }

    /** Builds the username/password form: labels above the fields. */
    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 10, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(formLabel("Username"), gbc);

        styleField(usernameField);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 22, 0);
        panel.add(usernameField, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 6, 0);
        panel.add(formLabel("Password"), gbc);

        styleField(passwordField);
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 28, 0);
        panel.add(passwordField, gbc);

        // Buttons
        JButton loginButton = Theme.button("Login", Theme.NAVY, Color.WHITE);
        JButton clearButton = Theme.button("Clear", new Color(225, 228, 235), Theme.NAVY);
        for (JButton b : new JButton[]{loginButton, clearButton}) {
            b.setFont(new Font("SansSerif", Font.BOLD, 18));
            b.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        }

        loginButton.addActionListener(this::handleLogin);
        clearButton.addActionListener(e -> clearFields());

        JPanel buttons = new JPanel(new GridLayout(1, 2, 18, 0));
        buttons.setOpaque(false);
        buttons.add(loginButton);
        buttons.add(clearButton);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(buttons, gbc);

        // Pressing Enter triggers Login
        getRootPane().setDefaultButton(loginButton);

        return panel;
    }

    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 18));
        l.setForeground(Theme.NAVY);
        return l;
    }

    /** Applies a consistent look to text inputs. */
    private void styleField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 16));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 195, 205)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
    }

    /** Small footer with a demo hint. */
    private JPanel buildFooter() {
        JPanel footer = new JPanel();
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        JLabel note = new JLabel("Authorized personnel only");
        note.setFont(new Font("SansSerif", Font.ITALIC, 14));
        note.setForeground(Color.GRAY);
        footer.add(note);
        return footer;
    }

    /** Validates credentials; opens the dashboard or shows an error. */
    private void handleLogin(ActionEvent e) {
        String username = usernameField.getText().trim();
        char[] passwordChars = passwordField.getPassword();
        String password = new String(passwordChars);
        Arrays.fill(passwordChars, '\0'); // wipe the char array

        if (ADMIN_USER.equals(username) && ADMIN_PASS.equals(password)) {
            dispose(); // close the login window
            SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
        } else {
            JOptionPane.showMessageDialog(this,
                    "Invalid username or password. Please try again.",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }

    /** Clears both inputs and returns focus to the username box. */
    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        usernameField.requestFocusInWindow();
    }
}

// ============================================================
// RecordsDialog
// ============================================================
/** Table of every saved record; double-click or use the button to open a poster. */
class RecordsDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private final List<Person> records;

    RecordsDialog(Window owner, List<Person> records) {
        super(owner, "All Lost Person Records", Dialog.ModalityType.MODELESS);
        this.records = records;
        setLayout(new BorderLayout());

        // Table model (cells are read-only)
        String[] columns = {"ID", "Full Name", "Age", "Gender", "Last Seen Location", "Date Last Seen", "Contact"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (Person p : records) {
            model.addRow(new Object[]{
                    p.getId(), p.getFullName(), p.getAge(), p.getGender(),
                    p.getLastSeenLocation(), p.getDateLastSeen().format(DATE_FMT),
                    p.getContactName() + " (" + p.getContactPhone() + ")"});
        }

        JTable table = new JTable(model);
        table.setFont(Theme.PLAIN);
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setFont(Theme.BOLD);
        table.getTableHeader().setBackground(Theme.NAVY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewSelected(table);
                }
            }
        });

        // Summary line on top
        JLabel summary = new JLabel(records.isEmpty()
                ? "No records saved yet."
                : "Total records: " + records.size() + "   (double-click a row to open its poster)");
        summary.setFont(Theme.BOLD);
        summary.setForeground(Theme.NAVY);
        summary.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        // Buttons
        JButton view = Theme.button("View Poster", Theme.NAVY, Color.WHITE);
        JButton close = Theme.button("Close", new Color(225, 228, 235), Theme.NAVY);
        view.addActionListener(e -> viewSelected(table));
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttons.add(view);
        buttons.add(close);

        add(summary, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        setSize(860, 420);
        setLocationRelativeTo(owner);
    }

    /** Opens the poster for the selected row (sorting-safe). */
    private void viewSelected(JTable table) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a record first.",
                    "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        new PublicDisplayFrame(records.get(modelRow)).setVisible(true);
    }
}

// ============================================================
// PublicDisplayFrame
// ============================================================
/**
 * Public-facing "MISSING PERSON" notice (kiosk / website preview style).
 * Read-only, large fonts, dark blue and white. Data comes straight from the
 * Person record the admin just saved.
 */
class PublicDisplayFrame extends JFrame {

    // Change this to your local emergency number
    private static final String EMERGENCY_NUMBER = "999";

    private static final Color LIGHT_BLUE = new Color(170, 195, 235);
    private static final Color PALE_BLUE = new Color(222, 234, 249);
    private static final Color RULE = new Color(150, 150, 150);
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    PublicDisplayFrame(Person p) {
        super("Missing Person Notice - " + p.getFullName());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // closing never exits the app
        BadgeImage.applyIcon(this);
        setLayout(new BorderLayout());

        // The poster: navy frame with a thin gold inner line
        JPanel poster = new JPanel(new BorderLayout());
        poster.setBackground(Color.WHITE);
        poster.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(8, 8, 8, 8, Theme.NAVY),
                BorderFactory.createMatteBorder(2, 2, 2, 2, Theme.GOLD)));
        poster.add(buildHeader(p), BorderLayout.NORTH);
        poster.add(buildBody(p), BorderLayout.CENTER);
        poster.add(buildFooter(p), BorderLayout.SOUTH);

        // Light grey page around the poster
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(new Color(236, 238, 242));
        page.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        page.add(poster, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        add(scroll, BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(780, screen.width - 40), Math.min(940, screen.height - 60));
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------- header

    /** Badge, department name, case number and the big MISSING PERSON title. */
    private JPanel buildHeader(Person p) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);

        // Navy bar
        JPanel top = new JPanel(new BorderLayout(18, 0));
        top.setBackground(Theme.NAVY);
        top.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel names = new JPanel();
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.setOpaque(false);
        names.add(Box.createVerticalGlue());
        names.add(leftLabel("POLICE DEPARTMENT", 30, Font.BOLD, Color.WHITE));
        names.add(leftLabel("MISSING PERSONS UNIT", 17, Font.BOLD, LIGHT_BLUE));
        names.add(Box.createVerticalGlue());

        JPanel shieldWrap = new JPanel(new GridBagLayout()); // keeps the shield at its own size
        shieldWrap.setOpaque(false);
        shieldWrap.add(new ShieldIcon(84, 98));

        JPanel logoAndName = new JPanel(new BorderLayout(14, 0));
        logoAndName.setOpaque(false);
        logoAndName.add(shieldWrap, BorderLayout.WEST);
        logoAndName.add(names, BorderLayout.CENTER);

        JPanel caseBox = new JPanel();
        caseBox.setLayout(new BoxLayout(caseBox, BoxLayout.Y_AXIS));
        caseBox.setOpaque(false);
        caseBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.GOLD),
                BorderFactory.createEmptyBorder(0, 20, 0, 6)));
        caseBox.add(Box.createVerticalGlue());
        caseBox.add(leftLabel("CASE NO.", 16, Font.BOLD, LIGHT_BLUE));
        caseBox.add(leftLabel(p.getId(), 34, Font.BOLD, Color.WHITE));
        caseBox.add(Box.createVerticalGlue());

        top.add(logoAndName, BorderLayout.CENTER);
        top.add(caseBox, BorderLayout.EAST);

        // Title block
        JPanel title = new JPanel();
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.setBackground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 8, 10));
        title.add(centered("MISSING PERSON", 60, Font.BOLD, Theme.NAVY));
        title.add(centered("IF YOU HAVE SEEN THIS PERSON, PLEASE CONTACT THE POLICE IMMEDIATELY.",
                14, Font.BOLD, Theme.RED));

        header.add(top, BorderLayout.NORTH);
        header.add(title, BorderLayout.CENTER);
        return header;
    }

    // --------------------------------------------------------------- body

    /** Photo, name, stats bar, last-seen box and the description rows. */
    private JPanel buildBody(Person p) {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(10, 28, 14, 28));

        // Photo placeholder (left)
        JPanel photoWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        photoWrap.setOpaque(false);
        PhotoPlaceholder photoBox = new PhotoPlaceholder(215, 310);
        photoBox.setPhoto(p.getPhoto());
        photoWrap.add(photoBox);

        // Name + stats + last seen (right)
        JPanel identity = new JPanel();
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.setOpaque(false);

        JLabel name = new JLabel(Theme.html(p.getFullName().toUpperCase(), 440));
        name.setFont(new Font("SansSerif", Font.BOLD, 36));
        name.setForeground(Theme.NAVY);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        identity.add(name);
        identity.add(Box.createVerticalStrut(8));

        JPanel rule = new JPanel();
        rule.setBackground(Theme.GOLD);
        rule.setPreferredSize(new Dimension(10, 2));
        rule.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        rule.setAlignmentX(Component.LEFT_ALIGNMENT);
        identity.add(rule);
        identity.add(Box.createVerticalStrut(14));

        JPanel stats = new JPanel(new GridLayout(1, 3));
        stats.setBackground(Theme.NAVY);
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.add(statCell("AGE", String.valueOf(p.getAge()), false));
        stats.add(statCell("GENDER", p.getGender(), true));
        stats.add(statCell("HEIGHT", p.getHeight().isEmpty() ? "Not given" : p.getHeight(), true));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, stats.getPreferredSize().height));
        identity.add(stats);
        identity.add(Box.createVerticalStrut(14));

        JPanel lastSeen = new JPanel();
        lastSeen.setLayout(new BoxLayout(lastSeen, BoxLayout.Y_AXIS));
        lastSeen.setBackground(PALE_BLUE);
        lastSeen.setAlignmentX(Component.LEFT_ALIGNMENT);
        lastSeen.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(150, 175, 215)),
                        BorderFactory.createMatteBorder(0, 5, 0, 0, Theme.NAVY)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        lastSeen.add(leftLabel("LAST SEEN", 15, Font.BOLD, Theme.NAVY));
        lastSeen.add(Box.createVerticalStrut(4));
        JLabel loc = new JLabel(Theme.html(p.getLastSeenLocation(), 400));
        loc.setFont(new Font("SansSerif", Font.BOLD, 22));
        loc.setForeground(Theme.NAVY);
        loc.setAlignmentX(Component.LEFT_ALIGNMENT);
        lastSeen.add(loc);
        lastSeen.add(leftLabel(p.getDateLastSeen().format(DATE_FMT), 17, Font.PLAIN, new Color(30, 30, 40)));
        identity.add(lastSeen);
        identity.add(Box.createVerticalGlue());

        JPanel upper = new JPanel(new BorderLayout(26, 0));
        upper.setOpaque(false);
        upper.add(photoWrap, BorderLayout.WEST);
        upper.add(identity, BorderLayout.CENTER);

        // Description section
        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);
        details.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        JLabel heading = new JLabel("DESCRIPTION");
        heading.setFont(new Font("SansSerif", Font.BOLD, 22));
        heading.setForeground(Theme.NAVY);
        heading.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.GOLD),
                BorderFactory.createEmptyBorder(0, 0, 4, 0)));
        JPanel headingWrap = new JPanel(new BorderLayout());
        headingWrap.setOpaque(false);
        headingWrap.add(heading, BorderLayout.CENTER);
        headingWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, headingWrap.getPreferredSize().height));
        details.add(headingWrap);

        details.add(descriptionRow("Clothing:", orDefault(p.getClothing(), "Not provided")));
        details.add(descriptionRow("Distinctive Marks:", orDefault(p.getFeatures(), "None known.")));
        details.add(descriptionRow("Additional Notes:", orDefault(p.getNotes(), "None.")));

        body.add(upper, BorderLayout.NORTH);
        body.add(details, BorderLayout.CENTER);
        return body;
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    /** One cell of the navy stats bar: small label over a big value. */
    private JPanel statCell(String label, String value, boolean divider) {
        JPanel cell = new JPanel();
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setOpaque(false);
        cell.setBorder(BorderFactory.createCompoundBorder(
                divider ? BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.GOLD)
                        : BorderFactory.createEmptyBorder(),
                BorderFactory.createEmptyBorder(10, 6, 10, 6)));
        cell.add(centered(label, 14, Font.BOLD, new Color(215, 225, 245)));
        cell.add(Box.createVerticalStrut(2));
        cell.add(centered(value, 24, Font.BOLD, Color.WHITE));
        return cell;
    }

    /** "Label:  value" row with a dotted line underneath; long values wrap. */
    private JPanel descriptionRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(RULE);
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        1f, new float[]{2f, 3f}, 0f));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(9, 2, 9, 2));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.BOLD, 17));
        l.setForeground(Theme.NAVY);
        l.setVerticalAlignment(SwingConstants.TOP);
        l.setPreferredSize(new Dimension(190, 24));

        JLabel v = new JLabel(Theme.html(value, 460));
        v.setFont(new Font("SansSerif", Font.PLAIN, 17));
        v.setForeground(new Color(30, 30, 40));
        v.setVerticalAlignment(SwingConstants.TOP);

        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    // ------------------------------------------------------------- footer

    /** Emergency line | contact person, then a small "issued by" line. */
    private JPanel buildFooter(Person p) {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.NAVY);

        JPanel columns = new JPanel(new GridLayout(1, 2));
        columns.setOpaque(false);
        columns.setBorder(BorderFactory.createEmptyBorder(18, 22, 12, 22));
        columns.add(contactColumn("IF YOU HAVE ANY INFORMATION, CALL", "Police Emergency",
                EMERGENCY_NUMBER, 44, false));
        columns.add(contactColumn("OR CONTACT", p.getContactName(),
                p.getContactPhone(), 34, true));

        JLabel issued = new JLabel("Issued by the Police Department  |  Case No. " + p.getId()
                + "  |  " + p.getReportedOn().format(DATE_FMT)
                + "  |  Any information, however small, may help.");
        issued.setFont(new Font("SansSerif", Font.PLAIN, 12));
        issued.setForeground(LIGHT_BLUE);
        JPanel issuedWrap = new JPanel(new BorderLayout());
        issuedWrap.setOpaque(false);
        issuedWrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(40, 66, 130)),
                BorderFactory.createEmptyBorder(9, 22, 10, 22)));
        issuedWrap.add(issued, BorderLayout.CENTER);

        footer.add(columns, BorderLayout.CENTER);
        footer.add(issuedWrap, BorderLayout.SOUTH);
        return footer;
    }

    private JPanel contactColumn(String title, String line, String phone, int phoneSize, boolean divider) {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);
        col.setBorder(divider
                ? BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 1, 0, 0, LIGHT_BLUE),
                        BorderFactory.createEmptyBorder(0, 24, 0, 0))
                : BorderFactory.createEmptyBorder(0, 0, 0, 24));
        col.add(leftLabel(title, 14, Font.BOLD, Color.WHITE));
        col.add(Box.createVerticalStrut(4));
        col.add(leftLabel(line, 18, Font.PLAIN, Color.WHITE));
        col.add(leftLabel(phone, phoneSize, Font.BOLD, Color.WHITE));
        return col;
    }

    // ------------------------------------------------------------ buttons

    /** Print (placeholder) on the left, Close on the right. */
    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 204, 214)),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));

        JButton print = Theme.button("Print", new Color(225, 228, 235), Theme.NAVY);
        JButton close = Theme.button("Close", new Color(225, 228, 235), Theme.NAVY);

        print.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Printing is not available yet in this demo version.",
                "Print", JOptionPane.INFORMATION_MESSAGE));
        close.addActionListener(e -> dispose());

        bar.add(print, BorderLayout.WEST);
        bar.add(close, BorderLayout.EAST);
        return bar;
    }

    /** Centered label helper (used in BoxLayout columns). */
    private JLabel centered(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    /** Left-aligned label helper (used in BoxLayout columns). */
    private JLabel leftLabel(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
}

// ============================================================
// MainFrame
// ============================================================
/** Main dashboard: menu bar, scrollable data-entry form and action buttons. */
class MainFrame extends JFrame {

    private static final String[] GENDERS = {"-- Select --", "Male", "Female", "Other"};

    // Form inputs
    private final JTextField nameField = new JTextField(25);
    private final JTextField ageField = new JTextField(25);
    private final JComboBox<String> genderBox = new JComboBox<>(GENDERS);
    private final JTextField heightField = new JTextField(25);
    private final JTextField locationField = new JTextField(25);
    private final JSpinner dateSpinner =
            new JSpinner(new SpinnerDateModel(new Date(), null, new Date(), Calendar.DAY_OF_MONTH));
    private final JTextArea clothingArea = new JTextArea(3, 25);
    private final JTextArea featuresArea = new JTextArea(3, 25);
    private final JTextField contactNameField = new JTextField(25);
    private final JTextField contactPhoneField = new JTextField(25);
    private final JTextArea notesArea = new JTextArea(4, 25);

    private BufferedImage selectedPhoto;                                  // null = none chosen
    private final PhotoPlaceholder photoPreview = new PhotoPlaceholder(96, 120);
    private final JLabel photoNameLabel = new JLabel("No photo selected");

    private final JLabel countLabel = new JLabel();

    MainFrame() {
        super("Lost Person Information Management - Admin Panel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BadgeImage.applyIcon(this);
        setLayout(new BorderLayout());

        setJMenuBar(buildMenuBar());
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildFormScroll(), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        updateCount();
        setSize(900, 720);
        setMinimumSize(new Dimension(700, 480));
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------- menu

    /** File -> Logout / Exit, Help -> About. */
    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem logout = new JMenuItem("Logout");
        JMenuItem exit = new JMenuItem("Exit");
        logout.addActionListener(e -> logout());
        exit.addActionListener(e -> exitApp());
        file.add(logout);
        file.addSeparator();
        file.add(exit);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Lost Person Information Management\n"
                        + "Police Admin Panel - demo version 1.0\n\n"
                        + "Records are kept in memory only and are cleared when the program closes.",
                "About", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);

        bar.add(file);
        bar.add(help);
        return bar;
    }

    private void logout() {
        int choice = JOptionPane.showConfirmDialog(this, "Log out of the admin panel?",
                "Logout", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }

    private void exitApp() {
        int choice = JOptionPane.showConfirmDialog(this, "Exit the application?",
                "Exit", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    // ------------------------------------------------------------- layout

    /** Navy title bar with a live record counter. */
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.NAVY);
        bar.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("Lost Person Information Management - Admin Panel");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        countLabel.setFont(Theme.BOLD);
        countLabel.setForeground(Theme.GOLD);

        JPanel left = new JPanel(new BorderLayout(12, 0));
        left.setOpaque(false);
        left.add(new ShieldIcon(34, 40), BorderLayout.WEST);
        left.add(title, BorderLayout.CENTER);

        bar.add(left, BorderLayout.WEST);
        bar.add(countLabel, BorderLayout.EAST);
        return bar;
    }

    /** The large data-entry form (GridBagLayout) wrapped in a scroll pane. */
    private JScrollPane buildFormScroll() {
        styleField(nameField);
        styleField(ageField);
        styleField(heightField);
        styleField(locationField);
        styleField(contactNameField);
        styleField(contactPhoneField);
        styleArea(clothingArea);
        styleArea(featuresArea);
        styleArea(notesArea);

        genderBox.setFont(Theme.PLAIN);
        genderBox.setBackground(Color.WHITE);

        dateSpinner.setFont(Theme.PLAIN);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));
        ((JSpinner.DefaultEditor) dateSpinner.getEditor()).getTextField().setFont(Theme.PLAIN);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 30));

        int row = 0;
        addRow(form, row++, "Photo of Missing Person", buildPhotoPicker(), false);
        addRow(form, row++, "Full Name", nameField, true);
        addRow(form, row++, "Age", ageField, true);
        addRow(form, row++, "Gender", genderBox, true);
        addRow(form, row++, "Height", heightField, false);
        addRow(form, row++, "Last Seen Location", locationField, true);
        addRow(form, row++, "Date Last Seen", dateSpinner, true);
        addRow(form, row++, "Clothing Description", clothingArea, false);
        addRow(form, row++, "Distinctive Features", featuresArea, false);
        addRow(form, row++, "Contact Person Name", contactNameField, true);
        addRow(form, row++, "Contact Phone", contactPhoneField, true);
        addRow(form, row++, "Additional Notes", notesArea, false);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    /** Preview box + "Choose Photo" / "Remove" buttons. */
    private JPanel buildPhotoPicker() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        panel.setOpaque(false);

        JButton choose = Theme.button("Choose Photo...", Theme.NAVY, Color.WHITE);
        JButton remove = Theme.button("Remove", new Color(225, 228, 235), Theme.NAVY);
        choose.addActionListener(e -> choosePhoto());
        remove.addActionListener(e -> setPhoto(null, null));

        photoNameLabel.setFont(Theme.PLAIN);
        photoNameLabel.setForeground(Color.GRAY);

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setOpaque(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        buttons.add(choose);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(remove);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        photoNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(buttons);
        side.add(Box.createVerticalStrut(8));
        side.add(photoNameLabel);

        panel.add(photoPreview);
        panel.add(side);
        return panel;
    }

    /** Opens a file chooser and loads the picked image (JPG, PNG, GIF or BMP). */
    private void choosePhoto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a photo of the missing person");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Images (jpg, jpeg, png, gif, bmp)", "jpg", "jpeg", "png", "gif", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                JOptionPane.showMessageDialog(this, "That file is not a supported image.",
                        "Invalid Photo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            setPhoto(img, file.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not read the image:\n" + ex.getMessage(),
                    "Invalid Photo", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setPhoto(BufferedImage img, String fileName) {
        selectedPhoto = img;
        photoPreview.setPhoto(img);
        photoNameLabel.setText(img == null ? "No photo selected" : fileName);
    }

    /** Section heading spanning both columns. */
    private void addHeading(JPanel form, int row, String title, String hint) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 20));
        t.setForeground(Theme.NAVY);
        JLabel h = new JLabel(hint);
        h.setFont(new Font("SansSerif", Font.ITALIC, 12));
        h.setForeground(Theme.RED);
        p.add(t, BorderLayout.WEST);
        p.add(h, BorderLayout.EAST);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 12, 5);
        form.add(p, gbc);
    }

    /** Adds one "label + input" row; text areas are wrapped in their own scroll pane. */
    private void addRow(JPanel form, int row, String text, JComponent input, boolean required) {
        boolean tall = input instanceof JTextArea;

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
        gbc.insets = new Insets(6, 5, 6, 5);

        String labelText = required
                ? "<html>" + text + " <font color='red'>*</font></html>"
                : text;
        JLabel label = new JLabel(labelText);
        label.setFont(Theme.BOLD);
        Dimension d = label.getPreferredSize();
        label.setPreferredSize(new Dimension(Math.max(d.width, 240), d.height));
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = tall ? GridBagConstraints.NORTHWEST : GridBagConstraints.WEST;
        form.add(label, gbc);

        JComponent field = input;
        if (tall) {
            JScrollPane sp = new JScrollPane(input);
            sp.setBorder(BorderFactory.createLineBorder(new Color(190, 195, 205)));
            field = sp;
        }
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        if (input instanceof JSpinner) {
            gbc.fill = GridBagConstraints.NONE;          // small date box, not stretched
            input.setPreferredSize(new Dimension(170, 34));
        } else {
            gbc.fill = GridBagConstraints.HORIZONTAL;
        }
        form.add(field, gbc);
    }

    private void styleField(JTextField f) {
        f.setFont(Theme.PLAIN);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 195, 205)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
    }

    private void styleArea(JTextArea a) {
        a.setFont(Theme.PLAIN);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createEmptyBorder(5, 7, 5, 7));
    }

    /** Bottom bar with the three action buttons. */
    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        bar.setBackground(new Color(240, 242, 247));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 204, 214)));

        JButton save = Theme.button("Save / Display Information", Theme.NAVY, Color.WHITE);
        JButton clear = Theme.button("Clear Form", new Color(225, 228, 235), Theme.NAVY);
        JButton view = Theme.button("View All Records", Theme.GOLD, Theme.NAVY);

        save.addActionListener(e -> saveRecord());
        clear.addActionListener(e -> clearForm());
        view.addActionListener(e -> new RecordsDialog(this, RecordStore.getAll()).setVisible(true));

        bar.add(save);
        bar.add(clear);
        bar.add(view);
        return bar;
    }

    // ------------------------------------------------------------ actions

    /** Validates the form, stores the record, then shows the poster. */
    private void saveRecord() {
        List<String> errors = new ArrayList<>();
        JComponent focus = null;

        // Full name
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            errors.add("Full Name is required.");
            focus = nameField;
        }

        // Age (whole number 0-120)
        String ageText = ageField.getText().trim();
        int age = 0;
        if (ageText.isEmpty()) {
            errors.add("Age is required.");
            if (focus == null) focus = ageField;
        } else {
            try {
                age = Integer.parseInt(ageText);
                if (age < 0 || age > 120) {
                    errors.add("Age must be between 0 and 120.");
                    if (focus == null) focus = ageField;
                }
            } catch (NumberFormatException ex) {
                errors.add("Age must be a whole number.");
                if (focus == null) focus = ageField;
            }
        }

        // Gender
        if (genderBox.getSelectedIndex() == 0) {
            errors.add("Please select a Gender.");
            if (focus == null) focus = genderBox;
        }

        // Last seen location
        String location = locationField.getText().trim();
        if (location.isEmpty()) {
            errors.add("Last Seen Location is required.");
            if (focus == null) focus = locationField;
        }

        // Date last seen (cannot be in the future)
        LocalDate seen = ((Date) dateSpinner.getValue()).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        if (seen.isAfter(LocalDate.now())) {
            errors.add("Date Last Seen cannot be in the future.");
            if (focus == null) focus = dateSpinner;
        }

        // Contact person
        String contactName = contactNameField.getText().trim();
        if (contactName.isEmpty()) {
            errors.add("Contact Person Name is required.");
            if (focus == null) focus = contactNameField;
        }

        String phone = contactPhoneField.getText().trim();
        String digits = phone.replaceAll("\\D", "");
        if (phone.isEmpty()) {
            errors.add("Contact Phone is required.");
            if (focus == null) focus = contactPhoneField;
        } else if (!phone.matches("[0-9+()\\-\\s]+") || digits.length() < 7 || digits.length() > 15) {
            errors.add("Contact Phone must contain 7 to 15 digits (only digits, + ( ) - allowed).");
            if (focus == null) focus = contactPhoneField;
        }

        // Show every problem at once
        if (!errors.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fix the following:\n\n\u2022 " + String.join("\n\u2022 ", errors),
                    "Missing or Invalid Information", JOptionPane.WARNING_MESSAGE);
            if (focus != null) focus.requestFocusInWindow();
            return;
        }

        // Store the record in memory
        Person person = new Person(
                RecordStore.nextId(),
                name, age, (String) genderBox.getSelectedItem(),
                heightField.getText().trim(), location, seen,
                clothingArea.getText().trim(), featuresArea.getText().trim(),
                contactName, phone, notesArea.getText().trim(), selectedPhoto);
        RecordStore.add(person);
        updateCount();

        // Open the public display window, then reset the form for the next entry
        new PublicDisplayFrame(person).setVisible(true);
        clearForm();
    }

    /** Resets every input to its default. */
    private void clearForm() {
        nameField.setText("");
        ageField.setText("");
        genderBox.setSelectedIndex(0);
        heightField.setText("");
        locationField.setText("");
        dateSpinner.setValue(new Date());
        clothingArea.setText("");
        featuresArea.setText("");
        contactNameField.setText("");
        contactPhoneField.setText("");
        notesArea.setText("");
        setPhoto(null, null);
        nameField.requestFocusInWindow();
    }

    private void updateCount() {
        countLabel.setText("Saved records: " + RecordStore.count());
    }
}

// ============================================================
// BadgeData (embedded Uganda Police badge, PNG as Base64)
// ============================================================
final class BadgeData {
    private BadgeData() { }

    static String base64() {
        return String.join("",
            "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");
    }
}
