package lk.ac.kln.hallsync.desktop;

import lk.ac.kln.hallsync.ApplicationServices;
import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import javax.swing.*;
import java.awt.*;

public final class DashboardFrame extends JFrame {
    private final JTabbedPane tabs=new JTabbedPane(JTabbedPane.LEFT,JTabbedPane.SCROLL_TAB_LAYOUT);
    public DashboardFrame(ApplicationServices app, AuthenticatedUser user, Runnable signedOut) {
        super("HallSync - Lecture Hall Booking System");
        JPanel header=new JPanel(new BorderLayout(16,0)); header.setBackground(DesktopTheme.NAVY); header.setBorder(BorderFactory.createEmptyBorder(18,22,18,22));
        JLabel brand=new JLabel("HallSync | Faculty of Science"); brand.setForeground(Color.WHITE); brand.setFont(brand.getFont().deriveFont(Font.BOLD,22f));
        JLabel identity=new JLabel(user.getUsername() + " | " + user.getRole() + " | Asia/Colombo"); identity.setForeground(Color.WHITE);
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,14,0)); right.setOpaque(false); right.add(identity); JButton logout=new JButton("Sign out"); right.add(logout);
        header.add(brand,BorderLayout.WEST); header.add(right,BorderLayout.EAST); add(header,BorderLayout.NORTH);
        tabs.addTab("Hall availability",new AvailabilityPanel(app,user)); tabs.addTab("Timetable",new SchedulePanel(app,user));
        if (user.isAdministrator() || "Lecturer".equals(user.getRole())) { tabs.addTab("My bookings",new BookingsPanel(app,user,false)); }
        if (user.isAdministrator()) {
            tabs.addTab("Review bookings",new BookingsPanel(app,user,true)); tabs.addTab("Catalogues",new CataloguesPanel(app,user));
            tabs.addTab("Hall equipment",new EquipmentPanel(app,user)); tabs.addTab("Hall blocks",new HallBlocksPanel(app,user));
            tabs.addTab("Accounts",new AccountsPanel(app,user)); tabs.addTab("Reports",new ReportsPanel(app,user));
        }
        tabs.addChangeListener(e -> refreshSelected()); add(tabs);
        JLabel footer=new JLabel("University of Kelaniya project prototype | Pending requests do not reserve halls | Refresh to see current data");
        footer.setBorder(BorderFactory.createEmptyBorder(10,20,10,20)); add(footer,BorderLayout.SOUTH);
        logout.addActionListener(e -> { if (UiErrors.confirm(this,"Sign out of HallSync?")) { dispose(); signedOut.run(); } });
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); setMinimumSize(new Dimension(1000,650));
        Rectangle screen=GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1280,screen.width-30),Math.min(800,screen.height-30)); setLocationRelativeTo(null);
    }
    public void open() { setVisible(true); refreshSelected(); }
    private void refreshSelected() { if (tabs.getSelectedComponent() instanceof Refreshable panel) { panel.refresh(); } }
}
