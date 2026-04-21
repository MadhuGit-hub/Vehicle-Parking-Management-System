package demo;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ParkingManagementSystem extends JFrame {

    // ─── DB Config ──────────────────────────────────────────────────────────────
    private static final String DB_URL = "jdbc:mysql://localhost:3306/parking_management";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "5972";

    // ─── Palette ────────────────────────────────────────────────────────────────
    private static final Color C_BG = new Color(13, 17, 23);
    private static final Color C_SURFACE = new Color(22, 27, 34);
    private static final Color C_CARD = new Color(30, 38, 48);
    private static final Color C_BORDER = new Color(48, 54, 61);
    private static final Color C_ACCENT = new Color(0, 210, 120);
    private static final Color C_ACCENT2 = new Color(88, 166, 255);
    private static final Color C_WARN = new Color(255, 170, 0);
    private static final Color C_DANGER = new Color(248, 81, 73);
    private static final Color C_TEXT = new Color(230, 237, 243);
    private static final Color C_TEXT_DIM = new Color(125, 138, 150);
    private static final Font F_MONO = new Font("Monospaced", Font.BOLD, 13);
    private static final Font F_TITLE = new Font("Dialog", Font.BOLD, 22);
    private static final Font F_LABEL = new Font("Dialog", Font.BOLD, 13);
    private static final Font F_BODY = new Font("Dialog", Font.PLAIN, 13);

    // ─── State ──────────────────────────────────────────────────────────────────
    private Connection conn;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private String currentUser = "";
    private JLabel statusBar;

    private JLabel lbl_total, lbl_avail, lbl_occupied, lbl_revenue;
    private DefaultTableModel mdl_parked, mdl_slots, mdl_report, mdl_history, mdl_overdue;
    private javax.swing.Timer clockTimer;
    private javax.swing.Timer autoRefreshTimer;

    // ══════════════════════════════════════════════════════════════════════════════
    public ParkingManagementSystem() {
        initDB();
        buildUI();
        showCard("Login");
        setVisible(true);
    }

    private void initDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Database Error: " + e.getMessage() + "\n\nEnsure MySQL is running.",
                    "Connection Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buildUI() {
        setTitle("ParkSys — Vehicle Parking Management");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 760);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(C_BG);

        mainPanel.add(buildLoginPanel(), "Login");
        mainPanel.add(buildDashboard(), "Dashboard");
        mainPanel.add(buildEntryPanel(), "Entry");
        mainPanel.add(buildExitPanel(), "Exit");
        mainPanel.add(buildSlotsPanel(), "Slots");
        mainPanel.add(buildReportsPanel(), "Reports");
        mainPanel.add(buildHistoryPanel(), "History");
        mainPanel.add(buildOverduePanel(), "Overdue");
        mainPanel.add(buildSettingsPanel(), "Settings");

        statusBar = new JLabel("  Ready");
        statusBar.setFont(F_BODY);
        statusBar.setForeground(C_TEXT_DIM);
        statusBar.setBackground(C_SURFACE);
        statusBar.setOpaque(true);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));

        add(mainPanel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    private void showCard(String name) {
        cardLayout.show(mainPanel, name);
        statusBar.setText("  " + name);
        statusBar.setForeground(C_TEXT_DIM);
        if (autoRefreshTimer != null)
            autoRefreshTimer.stop();
        switch (name) {
            case "Dashboard" -> {
                refreshDashboard();
                autoRefreshTimer = new javax.swing.Timer(30000, e -> refreshDashboard());
                autoRefreshTimer.start();
            }
            case "Slots" -> loadSlots();
            case "Reports" -> loadReportFiltered("Today", "All", null, null, null);
            case "History" -> loadHistoryFor(null);
            case "Overdue" -> loadOverdueFor(6);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // LOGIN
    // ══════════════════════════════════════════════════════════════════════════════
    private JPanel buildLoginPanel() {
        JPanel root = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, C_BG, getWidth(), getHeight(), new Color(20, 30, 50)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.05f));
                g2.setColor(C_ACCENT);
                g2.fillOval(-100, -100, 400, 400);
                g2.setColor(C_ACCENT2);
                g2.fillOval(getWidth() - 300, getHeight() - 300, 500, 500);
                g2.setComposite(AlphaComposite.SrcOver);
            }
        };

        JPanel card = roundedPanel(C_CARD, 16);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));
        card.setPreferredSize(new Dimension(420, 400));

        JLabel icon = new JLabel("P", JLabel.CENTER);
        icon.setFont(new Font("Dialog", Font.BOLD, 56));
        icon.setForeground(C_ACCENT);
        icon.setAlignmentX(CENTER_ALIGNMENT);

        JLabel title = new JLabel("ParkSys", JLabel.CENTER);
        title.setFont(new Font("Dialog", Font.BOLD, 28));
        title.setForeground(C_ACCENT);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Owner Management Portal", JLabel.CENTER);
        sub.setFont(F_BODY);
        sub.setForeground(C_TEXT_DIM);
        sub.setAlignmentX(CENTER_ALIGNMENT);

        JTextField userField = darkField("admin");
        JPasswordField passField = new JPasswordField("admin123");
        styleField(passField);

        JButton loginBtn = accentButton("Sign In", C_ACCENT);

        JLabel errLabel = new JLabel(" ", JLabel.CENTER);
        errLabel.setForeground(C_DANGER);
        errLabel.setFont(F_BODY);
        errLabel.setAlignmentX(CENTER_ALIGNMENT);

        card.add(icon);
        card.add(Box.createVerticalStrut(4));
        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(sub);
        card.add(Box.createVerticalStrut(28));
        card.add(fieldLabel("Username"));
        card.add(Box.createVerticalStrut(6));
        card.add(userField);
        card.add(Box.createVerticalStrut(14));
        card.add(fieldLabel("Password"));
        card.add(Box.createVerticalStrut(6));
        card.add(passField);
        card.add(Box.createVerticalStrut(22));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(10));
        card.add(errLabel);

        ActionListener doLogin = e -> {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword()).trim();
            if (u.isEmpty() || p.isEmpty()) {
                errLabel.setText("Enter both fields");
                return;
            }
            if (authAdmin(u, p)) {
                currentUser = u;
                errLabel.setText(" ");
                showCard("Dashboard");
            } else {
                errLabel.setText("Invalid credentials");
                passField.setText("");
            }
        };
        loginBtn.addActionListener(doLogin);
        passField.addActionListener(doLogin);
        root.add(card);
        return root;
    }

    private boolean authAdmin(String u, String p) {
        if (conn == null)
            return false;
        try (PreparedStatement s = conn.prepareStatement(
                "SELECT 1 FROM admin WHERE username=? AND password=?")) {
            s.setString(1, u);
            s.setString(2, p);
            return s.executeQuery().next();
        } catch (SQLException ex) {
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // DASHBOARD
    // ══════════════════════════════════════════════════════════════════════════════
    private JPanel buildDashboard() {
        JPanel root = darkPanel();
        root.setLayout(new BorderLayout());
        root.add(buildTopBar("Dashboard"), BorderLayout.NORTH);

        JPanel content = darkPanel();
        content.setLayout(new BorderLayout(16, 16));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel statsRow = darkPanel();
        statsRow.setLayout(new GridLayout(1, 4, 12, 0));
        lbl_total = new JLabel("0");
        lbl_avail = new JLabel("0");
        lbl_occupied = new JLabel("0");
        lbl_revenue = new JLabel("0");
        statsRow.add(statCard("Total Slots", lbl_total, "P", C_ACCENT2));
        statsRow.add(statCard("Available", lbl_avail, "OK", C_ACCENT));
        statsRow.add(statCard("Occupied", lbl_occupied, "V", C_WARN));
        statsRow.add(statCard("Today Revenue", lbl_revenue, "Rs", C_DANGER));

        JPanel navRow = darkPanel();
        navRow.setLayout(new GridLayout(1, 6, 10, 0));
        String[][] nav = { { "Entry", "Entry" }, { "Exit", "Exit" }, { "Slots", "Slots" },
                { "Reports", "Reports" }, { "History", "History" }, { "Overdue", "Overdue" } };
        for (String[] item : nav) {
            JButton b = navButton(item[0]);
            String c = item[1];
            b.addActionListener(e -> showCard(c));
            navRow.add(b);
        }

        String[] cols = { "ID", "Vehicle No", "Type", "Owner", "Phone", "Slot", "Entry Time", "Est. Amount" };
        mdl_parked = readonlyModel(cols);
        JTable tbl = styledTable(mdl_parked);
        JPanel tableCard = roundedPanel(C_CARD, 12);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel tblTitle = new JLabel("  Currently Parked Vehicles");
        tblTitle.setFont(F_LABEL);
        tblTitle.setForeground(C_TEXT);
        tableCard.add(tblTitle, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(null);
        sp.getViewport().setBackground(C_CARD);
        tableCard.add(sp, BorderLayout.CENTER);

        JPanel center = darkPanel();
        center.setLayout(new BorderLayout(0, 12));
        center.add(navRow, BorderLayout.NORTH);
        center.add(tableCard, BorderLayout.CENTER);

        content.add(statsRow, BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private void refreshDashboard() {
        if (conn == null)
            return;
        try {
            try (Statement s = conn.createStatement();
                    ResultSet r = s.executeQuery(
                            "SELECT COUNT(*) t, SUM(status='Available') a, SUM(status='Occupied') o FROM parking_slots")) {
                if (r.next()) {
                    lbl_total.setText(String.valueOf(r.getInt("t")));
                    lbl_avail.setText(String.valueOf(r.getInt("a")));
                    lbl_occupied.setText(String.valueOf(r.getInt("o")));
                }
            }
            try (Statement s = conn.createStatement();
                    ResultSet r = s.executeQuery(
                            "SELECT COALESCE(SUM(amount),0) rev FROM payments WHERE DATE(payment_time)=CURDATE()")) {
                if (r.next())
                    lbl_revenue.setText("Rs" + fmt(r.getDouble("rev")));
            }
            mdl_parked.setRowCount(0);
            String sql = "SELECT v.id, v.vehicle_number, v.vehicle_type, v.owner_name, v.phone_number, " +
                    "p.slot_number, v.entry_time, p.hourly_rate, " +
                    "TIMESTAMPDIFF(MINUTE, v.entry_time, NOW()) mins " +
                    "FROM vehicles v JOIN parking_slots p ON v.slot_id=p.id " +
                    "WHERE v.status='Parked' ORDER BY v.entry_time DESC";
            try (Statement s = conn.createStatement(); ResultSet r = s.executeQuery(sql)) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM HH:mm");
                while (r.next()) {
                    double hrs = r.getLong("mins") / 60.0;
                    double est = Math.ceil(hrs) * r.getDouble("hourly_rate");
                    mdl_parked.addRow(new Object[] {
                            r.getInt("id"), r.getString("vehicle_number"), r.getString("vehicle_type"),
                            r.getString("owner_name"), r.getString("phone_number"),
                            r.getString("slot_number"), sdf.format(r.getTimestamp("entry_time")),
                            "Rs" + fmt(est)
                    });
                }
            }
        } catch (SQLException e) {
            err(e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // VEHICLE ENTRY — no remarks, phone validation
    // ══════════════════════════════════════════════════════════════════════════════
    private JPanel buildEntryPanel() {
        JPanel root = darkPanel();
        root.setLayout(new BorderLayout());
        root.add(buildTopBar("Vehicle Entry"), BorderLayout.NORTH);

        JPanel center = darkPanel();
        center.setLayout(new GridBagLayout());

        JPanel form = roundedPanel(C_CARD, 14);
        form.setLayout(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(28, 40, 28, 40));
        form.setPreferredSize(new Dimension(520, 490));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField fVehNo = darkField("e.g. TN01AB1234");
        JComboBox<String> cType = darkCombo("Car", "Motorcycle", "Truck", "Bus", "Auto");
        JTextField fOwner = darkField("Owner name");
        JTextField fPhone = darkField("10-digit mobile number");
        JComboBox<String> cSlot = darkCombo();

        JLabel phoneHint = new JLabel(" ");
        phoneHint.setFont(new Font("Dialog", Font.PLAIN, 11));
        phoneHint.setForeground(C_DANGER);

        fPhone.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String ph = fPhone.getText().trim();
                if (ph.isEmpty()) {
                    phoneHint.setText(" ");
                    return;
                }
                if (!ph.matches("\\d{10}")) {
                    phoneHint.setText("Must be exactly 10 digits");
                    phoneHint.setForeground(C_WARN);
                } else {
                    phoneHint.setText("Valid");
                    phoneHint.setForeground(C_ACCENT);
                }
            }
        });

        JButton btnSlots = smallButton("Find Available Slots", C_ACCENT2);
        JButton btnPark = accentButton("Park Vehicle", C_ACCENT);
        JButton btnBack = smallButton("Back", C_SURFACE);
        JLabel statusLbl = new JLabel(" ");
        statusLbl.setFont(F_BODY);
        statusLbl.setForeground(C_ACCENT);

        addRow(form, g, 0, "Vehicle Number *", fVehNo);
        addRow(form, g, 1, "Vehicle Type *", cType);
        addRow(form, g, 2, "Owner Name", fOwner);
        addRow(form, g, 3, "Phone Number", fPhone);
        g.gridx = 1;
        g.gridy = 4;
        g.gridwidth = 1;
        form.add(phoneHint, g);
        g.gridx = 0;
        g.gridy = 5;
        g.gridwidth = 2;
        form.add(btnSlots, g);
        addRow(form, g, 6, "Select Slot *", cSlot);
        g.gridy = 7;
        form.add(btnPark, g);
        g.gridy = 8;
        form.add(statusLbl, g);
        g.gridy = 9;
        form.add(btnBack, g);

        btnSlots.addActionListener(e -> fillSlots(cSlot, (String) cType.getSelectedItem()));
        btnPark.addActionListener(e -> {
            String ph = fPhone.getText().trim();
            if (!ph.isEmpty() && !ph.matches("\\d{10}")) {
                statusLbl.setText("Enter a valid 10-digit phone number");
                statusLbl.setForeground(C_DANGER);
                return;
            }
            String res = doEntry(fVehNo.getText().trim(), (String) cType.getSelectedItem(),
                    fOwner.getText().trim(), ph, (String) cSlot.getSelectedItem());
            statusLbl.setText(res);
            statusLbl.setForeground(res.startsWith("OK") ? C_ACCENT : C_DANGER);
            if (res.startsWith("OK")) {
                fVehNo.setText("");
                fOwner.setText("");
                fPhone.setText("");
                phoneHint.setText(" ");
            }
        });
        btnBack.addActionListener(e -> showCard("Dashboard"));

        center.add(form);
        root.add(center, BorderLayout.CENTER);
        return root;
    }

    private void fillSlots(JComboBox<String> cb, String type) {
        cb.removeAllItems();
        if (conn == null)
            return;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT slot_number, hourly_rate FROM parking_slots WHERE status='Available' AND slot_type=?")) {
            ps.setString(1, type);
            ResultSet r = ps.executeQuery();
            int count = 0;
            while (r.next()) {
                cb.addItem(r.getString("slot_number") + " (Rs" + r.getDouble("hourly_rate") + "/hr)");
                count++;
            }
            if (count == 0)
                cb.addItem("No slots available for " + type);
        } catch (SQLException e) {
            err(e.getMessage());
        }
    }

    private String doEntry(String vno, String vtype, String owner, String phone, String slot) {
        if (vno.isEmpty())
            return "Vehicle number required";
        if (slot == null || slot.startsWith("No slots"))
            return "Select a valid slot";
        try (PreparedStatement chk = conn.prepareStatement(
                "SELECT 1 FROM vehicles WHERE vehicle_number=? AND status='Parked'")) {
            chk.setString(1, vno);
            if (chk.executeQuery().next())
                return "Vehicle " + vno + " is already parked!";
        } catch (SQLException e) {
            return e.getMessage();
        }
        String slotNo = slot.split(" ")[0];
        String sql = "INSERT INTO vehicles (vehicle_number,vehicle_type,owner_name,phone_number,slot_id) " +
                "VALUES(?,?,?,?,(SELECT id FROM parking_slots WHERE slot_number=?))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vno);
            ps.setString(2, vtype);
            ps.setString(3, owner.isEmpty() ? "Unknown" : owner);
            ps.setString(4, phone.isEmpty() ? "N/A" : phone);
            ps.setString(5, slotNo);
            ps.executeUpdate();
            conn.createStatement().executeUpdate(
                    "UPDATE parking_slots SET status='Occupied' WHERE slot_number='" + slotNo + "'");
            return "OK Parked in slot " + slotNo;
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // VEHICLE EXIT
    // ══════════════════════════════════════════════════════════════════════════════
    private JPanel buildExitPanel() {
        JPanel root = darkPanel();
        root.setLayout(new BorderLayout());
        root.add(buildTopBar("Vehicle Exit & Payment"), BorderLayout.NORTH);

        JPanel center = darkPanel();
        center.setLayout(new GridLayout(1, 2, 16, 0));
        center.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // LEFT
        JPanel left = roundedPanel(C_CARD, 14);
        left.setLayout(new BorderLayout(0, 12));
        left.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel searchBar = darkPanel();
        searchBar.setLayout(new BorderLayout(8, 0));
        JTextField fSearch = darkField("Enter vehicle number...");
        JButton btnSearch = smallButton("Search", C_ACCENT2);
        searchBar.add(fSearch, BorderLayout.CENTER);
        searchBar.add(btnSearch, BorderLayout.EAST);
        JTextArea details = new JTextArea();
        details.setFont(F_MONO);
        details.setEditable(false);
        details.setBackground(new Color(13, 17, 23));
        details.setForeground(C_TEXT);
        details.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        details.setLineWrap(true);
        JScrollPane detailScroll = new JScrollPane(details);
        detailScroll.setBorder(BorderFactory.createLineBorder(C_BORDER));
        detailScroll.getViewport().setBackground(new Color(13, 17, 23));
        left.add(sectionLabel("Find Vehicle"), BorderLayout.NORTH);
        left.add(searchBar, BorderLayout.SOUTH);
        left.add(detailScroll, BorderLayout.CENTER);

        // RIGHT
        JPanel right = roundedPanel(C_CARD, 14);
        right.setLayout(new GridBagLayout());
        JPanel payForm = darkPanel();
        payForm.setLayout(new BoxLayout(payForm, BoxLayout.Y_AXIS));
        payForm.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel lHours = infoLabel("Parking Duration: --");
        JLabel lRate = infoLabel("Hourly Rate: --");
        JLabel lAmount = new JLabel("Rs 0.00");
        lAmount.setFont(new Font("Dialog", Font.BOLD, 36));
        lAmount.setForeground(C_ACCENT);
        lAmount.setAlignmentX(CENTER_ALIGNMENT);
        JTextField fManual = darkField("Override amount (optional - for discounts)");
        JComboBox<String> cMethod = darkCombo("Cash", "Card", "UPI", "Net Banking", "Wallet");
        JButton btnCalc = smallButton("Recalculate", C_WARN);
        JButton btnProcess = accentButton("Process Payment and Exit", C_ACCENT);
        JButton btnBack = smallButton("Back", C_SURFACE);
        JLabel result = new JLabel(" ");
        result.setFont(F_BODY);
        result.setAlignmentX(CENTER_ALIGNMENT);

        payForm.add(sectionLabel("Payment"));
        payForm.add(Box.createVerticalStrut(16));
        payForm.add(lHours);
        payForm.add(Box.createVerticalStrut(6));
        payForm.add(lRate);
        payForm.add(Box.createVerticalStrut(16));
        payForm.add(lAmount);
        payForm.add(Box.createVerticalStrut(14));
        payForm.add(fieldLabel("Manual Override (Rs) - for discounts"));
        payForm.add(Box.createVerticalStrut(4));
        payForm.add(fManual);
        payForm.add(Box.createVerticalStrut(14));
        payForm.add(fieldLabel("Payment Method"));
        payForm.add(Box.createVerticalStrut(6));
        payForm.add(cMethod);
        payForm.add(Box.createVerticalStrut(10));
        payForm.add(btnCalc);
        payForm.add(Box.createVerticalStrut(8));
        payForm.add(btnProcess);
        payForm.add(Box.createVerticalStrut(6));
        payForm.add(result);
        payForm.add(Box.createVerticalStrut(6));
        payForm.add(btnBack);
        right.add(payForm);

        final int[] vid = { -1 };
        final double[] rate = { 0 };

        btnSearch.addActionListener(e -> {
            String vno = fSearch.getText().trim();
            if (vno.isEmpty()) {
                details.setText("Enter a vehicle number.");
                return;
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT v.*, p.slot_number, p.hourly_rate FROM vehicles v " +
                            "JOIN parking_slots p ON v.slot_id=p.id " +
                            "WHERE v.vehicle_number=? AND v.status='Parked'")) {
                ps.setString(1, vno);
                ResultSet r = ps.executeQuery();
                if (r.next()) {
                    vid[0] = r.getInt("id");
                    rate[0] = r.getDouble("hourly_rate");
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    long mins = (System.currentTimeMillis() - r.getTimestamp("entry_time").getTime()) / 60000;
                    details.setText(
                            "Vehicle : " + r.getString("vehicle_number") +
                                    "\nType    : " + r.getString("vehicle_type") +
                                    "\nOwner   : " + r.getString("owner_name") +
                                    "\nPhone   : " + r.getString("phone_number") +
                                    "\nSlot    : " + r.getString("slot_number") +
                                    "\nEntry   : " + sdf.format(r.getTimestamp("entry_time")) +
                                    "\nParked  : " + (mins / 60) + "h " + (mins % 60) + "m" +
                                    "\nRate    : Rs" + r.getDouble("hourly_rate") + "/hr");
                    calcAmount(lHours, lRate, lAmount, r.getTimestamp("entry_time"), rate[0]);
                    fManual.setText("");
                } else {
                    vid[0] = -1;
                    details.setText("No parked vehicle found: " + vno);
                }
            } catch (SQLException ex) {
                err(ex.getMessage());
            }
        });

        btnCalc.addActionListener(e -> {
            if (vid[0] == -1) {
                result.setText("Search a vehicle first.");
                return;
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT entry_time FROM vehicles WHERE id=?")) {
                ps.setInt(1, vid[0]);
                ResultSet r = ps.executeQuery();
                if (r.next())
                    calcAmount(lHours, lRate, lAmount, r.getTimestamp("entry_time"), rate[0]);
            } catch (SQLException ex) {
                err(ex.getMessage());
            }
        });

        btnProcess.addActionListener(e -> {
            if (vid[0] == -1) {
                result.setText("Search a vehicle first.");
                return;
            }
            try {
                String manualText = fManual.getText().trim();
                double amt = manualText.isEmpty()
                        ? Double.parseDouble(lAmount.getText().replace("Rs", "").replace(",", "").trim())
                        : Double.parseDouble(manualText);
                if (doExit(vid[0], amt, (String) cMethod.getSelectedItem())) {
                    result.setText("Payment done! Rs" + fmt(amt) + " via " + cMethod.getSelectedItem());
                    result.setForeground(C_ACCENT);
                    details.setText("");
                    fSearch.setText("");
                    fManual.setText("");
                    vid[0] = -1;
                    lAmount.setText("Rs 0.00");
                    lHours.setText("Parking Duration: --");
                    lRate.setText("Hourly Rate: --");
                }
            } catch (NumberFormatException ex) {
                result.setText("Invalid amount");
                result.setForeground(C_DANGER);
            }
        });

        btnBack.addActionListener(e -> showCard("Dashboard"));
        center.add(left);
        center.add(right);
        root.add(center, BorderLayout.CENTER);
        return root;
    }

    private void calcAmount(JLabel lHours, JLabel lRate, JLabel lAmount, Timestamp entry, double rate) {
        double hours = (System.currentTimeMillis() - entry.getTime()) / 3_600_000.0;
        double amt = Math.ceil(hours) * rate;
        lHours.setText("Parking Duration: " + String.format("%.2f hrs", hours));
        lRate.setText("Hourly Rate: Rs" + fmt(rate));
        lAmount.setText("Rs" + fmt(amt));
    }

    private boolean doExit(int vid, double amount, String method) {
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement p = conn.prepareStatement(
                    "UPDATE vehicles SET exit_time=NOW(), total_amount=?, status='Exited' WHERE id=?")) {
                p.setDouble(1, amount);
                p.setInt(2, vid);
                p.executeUpdate();
            }
            try (PreparedStatement p = conn.prepareStatement(
                    "UPDATE parking_slots SET status='Available' WHERE id=" +
                            "(SELECT slot_id FROM vehicles WHERE id=?)")) {
                p.setInt(1, vid);
                p.executeUpdate();
            }
            try (PreparedStatement p = conn.prepareStatement(
                    "INSERT INTO payments(vehicle_id,amount,payment_method) VALUES(?,?,?)")) {
                p.setInt(1, vid);
                p.setDouble(2, amount);
                p.setString(3, method);
                p.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                /* ignore */ }
            err(e.getMessage());
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                /* ignore */ }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // SLOTS
    // ══════════════════════════════════════════════════════════════════════════════
    private JPanel buildSlotsPanel() {
        JPanel root = darkPanel();
        root.setLayout(new BorderLayout());
        root.add(buildTopBar("Parking Slots"), BorderLayout.NORTH);

        JPanel content = darkPanel();
        content.setLayout(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel addForm = roundedPanel(C_CARD, 12);
        addForm.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 12));
        JTextField fSlotNo = darkField("e.g. A7");
        fSlotNo.setPreferredSize(new Dimension(100, 34));
        JComboBox<String> cType = darkCombo("Car", "Motorcycle", "Truck", "Bus", "Auto");
        JTextField fRate = darkField("Rate Rs/hr");
        fRate.setPreferredSize(new Dimension(100, 34));
        JButton btnAdd = smallButton("Add Slot", C_ACCENT);
        JButton btnRefresh = smallButton("Refresh", C_ACCENT2);
        JButton btnBack = smallButton("Back", C_SURFACE);
        JLabel addStatus = new JLabel(" ");
        addStatus.setForeground(C_ACCENT);

        addForm.add(fieldLabel("Slot:"));
        addForm.add(fSlotNo);
        addForm.add(fieldLabel("Type:"));
        addForm.add(cType);
        addForm.add(fieldLabel("Rs/hr:"));
        addForm.add(fRate);
        addForm.add(btnAdd);
        addForm.add(btnRefresh);
        addForm.add(btnBack);
        addForm.add(addStatus);

        String[] cols = { "Slot", "Type", "Status", "Rate/hr", "Current Vehicle", "Entry Time" };
        mdl_slots = readonlyModel(cols);
        JTable tbl = styledTable(mdl_slots);
        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                setBackground(sel ? C_ACCENT2.darker() : (row % 2 == 0 ? C_CARD : new Color(28, 36, 46)));
                setForeground(C_TEXT);
                String st = (String) t.getValueAt(row, 2);
                if (!sel && "Available".equals(st))
                    setForeground(C_ACCENT);
                else if (!sel && "Occupied".equals(st))
                    setForeground(C_WARN);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                setFont(F_BODY);
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(tbl);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(C_CARD);
        JPanel tableCard = roundedPanel(C_CARD, 12);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tableCard.add(scroll);

        btnAdd.addActionListener(e -> {
            String sn = fSlotNo.getText().trim(), rt = fRate.getText().trim();
            String tp = (String) cType.getSelectedItem();
            if (sn.isEmpty() || rt.isEmpty()) {
                addStatus.setText("Fill all fields");
                addStatus.setForeground(C_DANGER);
                return;
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO parking_slots(slot_number,slot_type,hourly_rate) VALUES(?,?,?)")) {
                ps.setString(1, sn);
                ps.setString(2, tp);
                ps.setDouble(3, Double.parseDouble(rt));
                ps.executeUpdate();
                addStatus.setText("Slot " + sn + " added");
                addStatus.setForeground(C_ACCENT);
                fSlotNo.setText("");
                fRate.setText("");
                loadSlots();
            } catch (Exception ex) {
                addStatus.setText(ex.getMessage());
                addStatus.setForeground(C_DANGER);
            }
        });
        btnRefresh.addActionListener(e -> loadSlots());
        btnBack.addActionListener(e -> showCard("Dashboard"));

        content.add(addForm, BorderLayout.NORTH);
        content.add(tableCard, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private void loadSlots() {
        if (conn == null)
            return;
        mdl_slots.setRowCount(0);
        try (Statement s = conn.createStatement();
                ResultSet r = s.executeQuery(
                        "SELECT p.slot_number, p.slot_type, p.status, p.hourly_rate, " +
                                "COALESCE(v.vehicle_number,'--') vno, v.entry_time " +
                                "FROM parking_slots p LEFT JOIN vehicles v ON p.id=v.slot_id AND v.status='Parked' " +
                                "ORDER BY p.slot_number")) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM HH:mm");
            while (r.next()) {
                Timestamp et = r.getTimestamp("entry_time");
                mdl_slots.addRow(new Object[] {
                        r.getString("slot_number"), r.getString("slot_type"), r.getString("status"),
                        "Rs" + fmt(r.getDouble("hourly_rate")), r.getString("vno"),
                        et != null ? sdf.format(et) : "--"
                });
            }
        } catch (SQLException e) {
            err(e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // REPORTS — with CSV + TXT download
    // ══════════════════════════════════════════════════════════════════════════════
    private JPanel buildReportsPanel() {
        JPanel root = darkPanel();
        root.setLayout(new BorderLayout());
        root.add(buildTopBar("Business Reports"), BorderLayout.NORTH);

        JPanel content = darkPanel();
        content.setLayout(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel controls = roundedPanel(C_CARD, 12);
        controls.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 12));
        JComboBox<String> cPeriod = darkCombo("Today", "Yesterday", "This Week", "This Month", "All Time");
        JComboBox<String> cMethod = darkCombo("All", "Cash", "Card", "UPI", "Net Banking", "Wallet");
        JButton btnGen = smallButton("Generate", C_ACCENT);
        JButton btnExportCSV = smallButton("Export CSV", C_ACCENT2);
        JButton btnExportTXT = smallButton("Export TXT", C_WARN);
        JButton btnBack = smallButton("Back", C_SURFACE);
        controls.add(fieldLabel("Period:"));
        controls.add(cPeriod);
        controls.add(fieldLabel("Method:"));
        controls.add(cMethod);
        controls.add(btnGen);
        controls.add(btnExportCSV);
        controls.add(btnExportTXT);
        controls.add(btnBack);

        JPanel summary = darkPanel();
        summary.setLayout(new GridLayout(1, 3, 12, 0));
        JLabel lVeh = new JLabel("0"), lRev = new JLabel("Rs0"), lAvg = new JLabel("0h");
        summary.add(statCard("Vehicles Served", lVeh, "V", C_ACCENT2));
        summary.add(statCard("Total Revenue", lRev, "Rs", C_ACCENT));
        summary.add(statCard("Avg Duration", lAvg, "T", C_WARN));

        String[] cols = { "Date/Time", "Vehicle No", "Type", "Slot", "Duration", "Amount", "Method" };
        mdl_report = readonlyModel(cols);
        JTable tbl = styledTable(mdl_report);
        JScrollPane scroll = new JScrollPane(tbl);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(C_CARD);
        JPanel tableCard = roundedPanel(C_CARD, 12);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tableCard.add(scroll);

        JPanel north = darkPanel();
        north.setLayout(new BorderLayout(0, 10));
        north.add(controls, BorderLayout.NORTH);
        north.add(summary, BorderLayout.CENTER);

        content.add(north, BorderLayout.NORTH);
        content.add(tableCard, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        btnGen.addActionListener(e -> loadReportFiltered((String) cPeriod.getSelectedItem(),
                (String) cMethod.getSelectedItem(), lVeh, lRev, lAvg));
        btnExportCSV.addActionListener(e -> exportReport("csv"));
        btnExportTXT.addActionListener(e -> exportReport("txt"));
        btnBack.addActionListener(e -> showCard("Dashboard"));
        return root;
    }

    private void loadReportFiltered(String period, String method,
            JLabel lVeh, JLabel lRev, JLabel lAvg) {
        if (conn == null)
            return;
        mdl_report.setRowCount(0);
        String df = switch (period) {
            case "Today" -> "DATE(p.payment_time)=CURDATE()";
            case "Yesterday" -> "DATE(p.payment_time)=CURDATE()-INTERVAL 1 DAY";
            case "This Week" -> "p.payment_time>=DATE_SUB(CURDATE(),INTERVAL 7 DAY)";
            case "This Month" -> "MONTH(p.payment_time)=MONTH(CURDATE()) AND YEAR(p.payment_time)=YEAR(CURDATE())";
            default -> "1=1";
        };
        String mf = "All".equals(method) ? "" : " AND p.payment_method='" + method + "'";
        String sql = "SELECT p.payment_time, v.vehicle_number, v.vehicle_type, ps.slot_number, " +
                "TIMESTAMPDIFF(MINUTE,v.entry_time,v.exit_time) mins, p.amount, p.payment_method " +
                "FROM payments p JOIN vehicles v ON p.vehicle_id=v.id " +
                "JOIN parking_slots ps ON v.slot_id=ps.id " +
                "WHERE " + df + mf + " ORDER BY p.payment_time DESC";
        try (Statement s = conn.createStatement(); ResultSet r = s.executeQuery(sql)) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM HH:mm");
            int cnt = 0;
            double total = 0;
            long totalMins = 0;
            while (r.next()) {
                cnt++;
                double amt = r.getDouble("amount");
                total += amt;
                long mins = r.getLong("mins");
                totalMins += mins;
                mdl_report.addRow(new Object[] {
                        sdf.format(r.getTimestamp("payment_time")),
                        r.getString("vehicle_number"), r.getString("vehicle_type"),
                        r.getString("slot_number"),
                        (mins / 60) + "h " + (mins % 60) + "m",
                        "Rs" + fmt(amt), r.getString("payment_method")
                });
            }
            if (lVeh != null) {
                lVeh.setText(String.valueOf(cnt));
                lRev.setText("Rs" + fmt(total));
                lAvg.setText(cnt > 0 ? (totalMins / cnt / 60) + "h" + (totalMins / cnt % 60) + "m" : "0h");
            }
        } catch (SQLException e) {
            err(e.getMessage());
        }
    }

    private void exportReport(String format) {
        if (mdl_report.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Generate a report first!", "Nothing to export",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("parking_report_" +
                new SimpleDateFormat("yyyyMMdd_HHmm").format(new java.util.Date()) + "." + format));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(fc.getSelectedFile()))) {
            String sep = "csv".equals(format) ? "," : " | ";
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < mdl_report.getColumnCount(); c++)
                headers.add(mdl_report.getColumnName(c));
            pw.println(String.join(sep, headers));
            if ("txt".equals(format))
                pw.println("-".repeat(80));
            for (int row = 0; row < mdl_report.getRowCount(); row++) {
                List<String> cells = new ArrayList<>();
                for (int c = 0; c < mdl_report.getColumnCount(); c++)
                    cells.add(mdl_report.getValueAt(row, c).toString());
                pw.println(String.join(sep, cells));
            }
            JOptionPane.showMessageDialog(this,
                    "Exported to:\n" + fc.getSelectedFile().getAbsolutePath(),
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // HISTORY
    // ══════════════════════════════════════════════════════════════════════════════
    private JPanel buildHistoryPanel() {
        JPanel root = darkPanel();
        root.setLayout(new BorderLayout());
        root.add(buildTopBar("Vehicle History"), BorderLayout.NORTH);

        JPanel content = darkPanel();
        content.setLayout(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel bar = roundedPanel(C_CARD, 12);
        bar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 12));
        JTextField fSearch = darkField("Search by vehicle number...");
        fSearch.setPreferredSize(new Dimension(240, 34));
        JButton btnSearch = smallButton("Search", C_ACCENT2);
        JButton btnAll = smallButton("Show All", C_SURFACE);
        JButton btnExport = smallButton("Export CSV", C_WARN);
        JButton btnBack = smallButton("Back", C_SURFACE);
        bar.add(fSearch);
        bar.add(btnSearch);
        bar.add(btnAll);
        bar.add(btnExport);
        bar.add(btnBack);

        String[] cols = { "ID", "Vehicle No", "Type", "Owner", "Phone", "Slot", "Entry", "Exit", "Duration", "Amount",
                "Status" };
        mdl_history = readonlyModel(cols);
        JTable tbl = styledTable(mdl_history);
        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean f, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, f, row, col);
                setBackground(sel ? C_ACCENT2.darker() : (row % 2 == 0 ? C_CARD : new Color(28, 36, 46)));
                setForeground(C_TEXT);
                String st = (String) t.getValueAt(row, 10);
                if (!sel && "Parked".equals(st))
                    setForeground(C_ACCENT);
                if (!sel && "Exited".equals(st))
                    setForeground(C_TEXT_DIM);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                setFont(F_BODY);
                return this;
            }
        });
        JScrollPane scroll = new JScrollPane(tbl);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(C_CARD);
        JPanel tableCard = roundedPanel(C_CARD, 12);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tableCard.add(scroll);

        content.add(bar, BorderLayout.NORTH);
        content.add(tableCard, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        btnAll.addActionListener(e -> loadHistoryFor(null));
        btnSearch.addActionListener(e -> loadHistoryFor(fSearch.getText().trim()));
        btnExport.addActionListener(e -> exportModel(mdl_history, "history"));
        btnBack.addActionListener(e -> showCard("Dashboard"));
        fSearch.addActionListener(e -> loadHistoryFor(fSearch.getText().trim()));
        return root;
    }

    private void loadHistoryFor(String vno) {
        if (conn == null)
            return;
        mdl_history.setRowCount(0);
        String where = (vno != null && !vno.isEmpty())
                ? "WHERE v.vehicle_number LIKE '%" + vno + "%'"
                : "";
        String sql = "SELECT v.id, v.vehicle_number, v.vehicle_type, v.owner_name, v.phone_number, " +
                "p.slot_number, v.entry_time, v.exit_time, " +
                "TIMESTAMPDIFF(MINUTE,v.entry_time,COALESCE(v.exit_time,NOW())) mins, " +
                "v.total_amount, v.status " +
                "FROM vehicles v JOIN parking_slots p ON v.slot_id=p.id " + where +
                " ORDER BY v.entry_time DESC LIMIT 300";
        try (Statement s = conn.createStatement(); ResultSet r = s.executeQuery(sql)) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM HH:mm");
            while (r.next()) {
                long mins = r.getLong("mins");
                Timestamp ex = r.getTimestamp("exit_time");
                mdl_history.addRow(new Object[] {
                        r.getInt("id"), r.getString("vehicle_number"), r.getString("vehicle_type"),
                        r.getString("owner_name"), r.getString("phone_number"),
                        r.getString("slot_number"), sdf.format(r.getTimestamp("entry_time")),
                        ex != null ? sdf.format(ex) : "--",
                        (mins / 60) + "h " + (mins % 60) + "m",
                        "Rs" + fmt(r.getDouble("total_amount")), r.getString("status")
                });
            }
        } catch (SQLException e) {
            err(e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // OVERDUE — vehicles parked longer than threshold
    // ══════════════════════════════════════════════════════════════════════════════
    private JPanel buildOverduePanel() {
        JPanel root = darkPanel();
        root.setLayout(new BorderLayout());
        root.add(buildTopBar("Overdue and Long-Stay Vehicles"), BorderLayout.NORTH);

        JPanel content = darkPanel();
        content.setLayout(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel bar = roundedPanel(C_CARD, 12);
        bar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 12));
        JTextField fHours = darkField("6");
        fHours.setPreferredSize(new Dimension(60, 34));
        JButton btnLoad = smallButton("Find Overdue", C_DANGER);
        JButton btnExport = smallButton("Export CSV", C_WARN);
        JButton btnBack = smallButton("Back", C_SURFACE);
        JLabel hint = new JLabel("Vehicles parked longer than:");
        hint.setFont(F_BODY);
        hint.setForeground(C_TEXT_DIM);
        bar.add(hint);
        bar.add(fHours);
        bar.add(fieldLabel("hours"));
        bar.add(btnLoad);
        bar.add(btnExport);
        bar.add(btnBack);

        String[] cols = { "ID", "Vehicle No", "Type", "Owner", "Phone", "Slot", "Entry Time", "Hours Parked",
                "Est. Charge" };
        mdl_overdue = readonlyModel(cols);
        JTable tbl = styledTable(mdl_overdue);
        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean f, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, f, row, col);
                setBackground(sel ? C_DANGER.darker() : new Color(60, 20, 20));
                setForeground(sel ? Color.WHITE : new Color(255, 180, 160));
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                setFont(F_BODY);
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(tbl);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(60, 20, 20));
        JPanel tableCard = roundedPanel(new Color(40, 18, 18), 12);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel warn = new JLabel("  These vehicles have exceeded the threshold. Consider contacting owners.");
        warn.setFont(F_BODY);
        warn.setForeground(C_WARN);
        tableCard.add(warn, BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);

        content.add(bar, BorderLayout.NORTH);
        content.add(tableCard, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        btnLoad.addActionListener(e -> {
            try {
                loadOverdueFor(Integer.parseInt(fHours.getText().trim()));
            } catch (NumberFormatException ex) {
                err("Enter a valid number of hours");
            }
        });
        btnExport.addActionListener(e -> exportModel(mdl_overdue, "overdue"));
        btnBack.addActionListener(e -> showCard("Dashboard"));
        return root;
    }

    private void loadOverdueFor(int thresholdHours) {
        if (conn == null)
            return;
        mdl_overdue.setRowCount(0);
        String sql = "SELECT v.id, v.vehicle_number, v.vehicle_type, v.owner_name, v.phone_number, " +
                "p.slot_number, v.entry_time, " +
                "TIMESTAMPDIFF(MINUTE,v.entry_time,NOW()) mins, p.hourly_rate " +
                "FROM vehicles v JOIN parking_slots p ON v.slot_id=p.id " +
                "WHERE v.status='Parked' AND TIMESTAMPDIFF(HOUR,v.entry_time,NOW())>=" + thresholdHours +
                " ORDER BY v.entry_time ASC";
        try (Statement s = conn.createStatement(); ResultSet r = s.executeQuery(sql)) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            while (r.next()) {
                long mins = r.getLong("mins");
                double hrs = mins / 60.0;
                double est = Math.ceil(hrs) * r.getDouble("hourly_rate");
                mdl_overdue.addRow(new Object[] {
                        r.getInt("id"), r.getString("vehicle_number"), r.getString("vehicle_type"),
                        r.getString("owner_name"), r.getString("phone_number"),
                        r.getString("slot_number"), sdf.format(r.getTimestamp("entry_time")),
                        String.format("%.1f hrs", hrs), "Rs" + fmt(est)
                });
            }
        } catch (SQLException e) {
            err(e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // SETTINGS
    // ══════════════════════════════════════════════════════════════════════════════
    private JPanel buildSettingsPanel() {
        JPanel root = darkPanel();
        root.setLayout(new BorderLayout());
        root.add(buildTopBar("Settings"), BorderLayout.NORTH);

        JPanel center = darkPanel();
        center.setLayout(new GridBagLayout());

        JPanel card = roundedPanel(C_CARD, 14);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        card.setPreferredSize(new Dimension(480, 420));

        JTextField fUser = darkField(currentUser);
        JPasswordField fOld = new JPasswordField();
        styleField(fOld);
        JPasswordField fNew = new JPasswordField();
        styleField(fNew);
        JPasswordField fCnf = new JPasswordField();
        styleField(fCnf);
        JButton btnSave = accentButton("Save Password", C_ACCENT);
        JButton btnBack = smallButton("Back", C_SURFACE);
        JLabel status = new JLabel(" ");
        status.setFont(F_BODY);
        status.setAlignmentX(CENTER_ALIGNMENT);

        card.add(sectionLabel("Change Admin Password"));
        card.add(Box.createVerticalStrut(20));
        card.add(fieldLabel("Username"));
        card.add(Box.createVerticalStrut(6));
        card.add(fUser);
        card.add(Box.createVerticalStrut(14));
        card.add(fieldLabel("Current Password"));
        card.add(Box.createVerticalStrut(6));
        card.add(fOld);
        card.add(Box.createVerticalStrut(14));
        card.add(fieldLabel("New Password"));
        card.add(Box.createVerticalStrut(6));
        card.add(fNew);
        card.add(Box.createVerticalStrut(14));
        card.add(fieldLabel("Confirm New"));
        card.add(Box.createVerticalStrut(6));
        card.add(fCnf);
        card.add(Box.createVerticalStrut(24));
        card.add(btnSave);
        card.add(Box.createVerticalStrut(8));
        card.add(status);
        card.add(Box.createVerticalStrut(8));
        card.add(btnBack);

        btnSave.addActionListener(e -> {
            String u = fUser.getText().trim(), oldP = new String(fOld.getPassword());
            String newP = new String(fNew.getPassword()), cnf = new String(fCnf.getPassword());
            if (!newP.equals(cnf)) {
                status.setText("Passwords dont match");
                status.setForeground(C_DANGER);
                return;
            }
            if (newP.length() < 6) {
                status.setText("Min 6 characters");
                status.setForeground(C_DANGER);
                return;
            }
            if (!authAdmin(u, oldP)) {
                status.setText("Wrong current password");
                status.setForeground(C_DANGER);
                return;
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE admin SET password=? WHERE username=?")) {
                ps.setString(1, newP);
                ps.setString(2, u);
                ps.executeUpdate();
                status.setText("Password updated");
                status.setForeground(C_ACCENT);
                fOld.setText("");
                fNew.setText("");
                fCnf.setText("");
            } catch (SQLException ex) {
                status.setText(ex.getMessage());
                status.setForeground(C_DANGER);
            }
        });
        btnBack.addActionListener(e -> showCard("Dashboard"));
        center.add(card);
        root.add(center, BorderLayout.CENTER);
        return root;
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // TOP BAR
    // ══════════════════════════════════════════════════════════════════════════════
    private JPanel buildTopBar(String pageTitle) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(C_SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));

        JLabel logo = new JLabel("P ParkSys");
        logo.setFont(new Font("Dialog", Font.BOLD, 18));
        logo.setForeground(C_ACCENT);

        JLabel page = new JLabel(pageTitle);
        page.setFont(F_LABEL);
        page.setForeground(C_TEXT_DIM);
        page.setHorizontalAlignment(JLabel.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(C_SURFACE);
        JLabel lClock = new JLabel();
        lClock.setFont(F_MONO);
        lClock.setForeground(C_TEXT_DIM);

        if (clockTimer != null)
            clockTimer.stop();
        clockTimer = new javax.swing.Timer(1000,
                e -> lClock.setText(new SimpleDateFormat("EEE dd-MM-yyyy  HH:mm:ss").format(new java.util.Date())));
        clockTimer.start();
        lClock.setText(new SimpleDateFormat("EEE dd-MM-yyyy  HH:mm:ss").format(new java.util.Date()));

        JButton btnDash = smallButton("Home", C_SURFACE);
        JButton btnSettings = smallButton("Settings", C_SURFACE);
        JButton btnLogout = smallButton("Logout", C_DANGER);
        btnDash.addActionListener(e -> showCard("Dashboard"));
        btnSettings.addActionListener(e -> showCard("Settings"));
        btnLogout.addActionListener(e -> {
            currentUser = "";
            showCard("Login");
        });

        right.add(lClock);
        right.add(Box.createHorizontalStrut(8));
        right.add(btnDash);
        right.add(btnSettings);
        right.add(btnLogout);

        bar.add(logo, BorderLayout.WEST);
        bar.add(page, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // GENERIC CSV EXPORT
    // ══════════════════════════════════════════════════════════════════════════════
    private void exportModel(DefaultTableModel model, String name) {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data to export!", "Empty", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(name + "_" +
                new SimpleDateFormat("yyyyMMdd_HHmm").format(new java.util.Date()) + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(fc.getSelectedFile()))) {
            List<String> h = new ArrayList<>();
            for (int c = 0; c < model.getColumnCount(); c++)
                h.add(model.getColumnName(c));
            pw.println(String.join(",", h));
            for (int row = 0; row < model.getRowCount(); row++) {
                List<String> cells = new ArrayList<>();
                for (int c = 0; c < model.getColumnCount(); c++)
                    cells.add("\"" + model.getValueAt(row, c).toString() + "\"");
                pw.println(String.join(",", cells));
            }
            JOptionPane.showMessageDialog(this,
                    "Exported to:\n" + fc.getSelectedFile().getAbsolutePath(), "Done", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // UI HELPERS
    // ══════════════════════════════════════════════════════════════════════════════
    private JPanel darkPanel() {
        JPanel p = new JPanel();
        p.setBackground(C_BG);
        return p;
    }

    private JPanel roundedPanel(Color bg, int arc) {
        return new JPanel() {
            {
                setOpaque(false);
                setBackground(bg);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.dispose();
            }
        };
    }

    private JPanel statCard(String title, JLabel valueLabel, String icon, Color accent) {
        JPanel card = roundedPanel(C_CARD, 12);
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JLabel ico = new JLabel(icon + " " + title);
        ico.setFont(F_BODY);
        ico.setForeground(C_TEXT_DIM);
        valueLabel.setFont(new Font("Dialog", Font.BOLD, 26));
        valueLabel.setForeground(accent);
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(0, 3));
        bar.setOpaque(false);
        card.add(ico, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(bar, BorderLayout.SOUTH);
        return card;
    }

    private JButton accentButton(String text, Color color) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(
                        getModel().isPressed() ? color.darker() : getModel().isRollover() ? color.brighter() : color);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(F_LABEL);
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        return b;
    }

    private JButton smallButton(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() : getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(F_BODY);
        b.setForeground(C_TEXT);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return b;
    }

    private JButton navButton(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? C_ACCENT.darker() : C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                if (getModel().isRollover()) {
                    g2.setColor(C_ACCENT);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Dialog", Font.BOLD, 13));
        b.setForeground(C_TEXT);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JTextField darkField(String placeholder) {
        JTextField f = new JTextField(placeholder);
        styleField(f);
        return f;
    }

    private void styleField(JTextField f) {
        f.setBackground(C_BG);
        f.setForeground(C_TEXT);
        f.setCaretColor(C_ACCENT);
        f.setFont(F_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    }

    @SafeVarargs
    private <T> JComboBox<T> darkCombo(T... items) {
        JComboBox<T> cb = new JComboBox<>(items);
        cb.setBackground(C_BG);
        cb.setForeground(C_TEXT);
        cb.setFont(F_BODY);
        cb.setBorder(BorderFactory.createLineBorder(C_BORDER, 1, true));
        cb.setRenderer((list, val, idx, sel, focus) -> {
            JLabel l = new JLabel(val == null ? "" : val.toString());
            l.setOpaque(true);
            l.setFont(F_BODY);
            l.setBackground(sel ? C_ACCENT.darker() : C_BG);
            l.setForeground(C_TEXT);
            l.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            return l;
        });
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return cb;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_LABEL);
        l.setForeground(C_TEXT_DIM);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_TITLE);
        l.setForeground(C_TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel infoLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_BODY);
        l.setForeground(C_TEXT_DIM);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setBackground(C_CARD);
        t.setForeground(C_TEXT);
        t.setFont(F_BODY);
        t.setRowHeight(34);
        t.setGridColor(C_BORDER);
        t.setShowGrid(true);
        t.setSelectionBackground(C_ACCENT2.darker());
        t.setSelectionForeground(C_TEXT);
        t.setFillsViewportHeight(true);
        JTableHeader h = t.getTableHeader();
        h.setBackground(C_SURFACE);
        h.setForeground(C_TEXT_DIM);
        h.setFont(F_LABEL);
        h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));
        h.setReorderingAllowed(false);
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean f, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, f, row, col);
                setBackground(sel ? C_ACCENT2.darker() : (row % 2 == 0 ? C_CARD : new Color(28, 36, 46)));
                setForeground(C_TEXT);
                setFont(F_BODY);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return this;
            }
        });
        return t;
    }

    private DefaultTableModel readonlyModel(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    private void addRow(JPanel form, GridBagConstraints g, int row, String label, JComponent comp) {
        g.gridwidth = 1;
        g.gridx = 0;
        g.gridy = row;
        JLabel l = new JLabel(label);
        l.setFont(F_LABEL);
        l.setForeground(C_TEXT_DIM);
        form.add(l, g);
        g.gridx = 1;
        form.add(comp, g);
    }

    private String fmt(double v) {
        return String.format("%.2f", v);
    }

    private void err(String msg) {
        statusBar.setText("  " + msg);
        statusBar.setForeground(C_DANGER);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new ParkingManagementSystem();
        });
    }
}