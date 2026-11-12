package lk.ac.kln.hallsync;

import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.desktop.*;
import lk.ac.kln.hallsync.model.LookupOption;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/** Runs the actual Swing sign-in and loads every role-specific tab under Xvfb in CI. */
public final class DesktopSmokeCheck {
    private static int passed;
    private static void check(String label, boolean result) {
        if (!result) { throw new AssertionError(label); } passed++; System.out.println("PASS - " + label);
    }
    public static void main(String[] args) throws Exception {
        if (!"1".equals(System.getenv("HALLSYNC_RUN_INTEGRATION_TESTS"))) { throw new IllegalStateException("Explicit test enablement required."); }
        DatabaseConnectionFactory connections=new DatabaseConnectionFactory(DatabaseConfig.load(Path.of(args[0])));
        try (Connection c=connections.open()) { if (!c.getCatalog().startsWith("hallsync_connection_")) { throw new IllegalStateException("Disposable schema required."); } }
        ApplicationServices app=new ApplicationServices(connections);
        String password=System.getenv("HALLSYNC_APPLICATION_TEST_PASSWORD");
        try {
            SwingUtilities.invokeAndWait(() -> {
                DesktopTheme.apply();
                for (String username : List.of("ci_admin","ci_lecturer","ci_student")) {
                    LoginDialog login=new LoginDialog(app);
                    javax.swing.Timer fill=new javax.swing.Timer(200,e -> {
                        all(login,JTextField.class).stream().filter(t -> !(t instanceof JPasswordField)).findFirst().orElseThrow().setText(username);
                        all(login,JPasswordField.class).get(0).setText(password);
                        all(login,JButton.class).stream().filter(b -> b.getText().equals("Sign in")).findFirst().orElseThrow().doClick();
                    });
                    fill.setRepeats(false); fill.start(); login.setVisible(true);
                    AuthenticatedUser user=login.identity().orElseThrow();
                    check("Swing sign-in succeeds for " + user.getRole(),user.getUsername().equals(username));
                    DashboardFrame frame=new DashboardFrame(app,user,() -> {}); frame.open();
                    JTabbedPane tabs=(JTabbedPane)frame.getContentPane().getComponent(1);
                    int expected=user.isAdministrator() ? 9 : "Lecturer".equals(user.getRole()) ? 3 : 2;
                    check("Role-specific navigation for " + user.getRole(),tabs.getTabCount()==expected);
                    for (int i=0;i<tabs.getTabCount();i++) {
                        tabs.setSelectedIndex(i);
                        if (tabs.getSelectedComponent() instanceof CataloguesPanel catalogues) {
                            JTabbedPane catalogTabs=all(catalogues,JTabbedPane.class).get(0);
                            for (int j=0;j<catalogTabs.getTabCount();j++) {
                                catalogTabs.setSelectedIndex(j);
                                check("Catalogue screen loads " + catalogTabs.getTitleAt(j),!all(catalogTabs.getSelectedComponent(),JTable.class).isEmpty());
                            }
                        }
                        check(user.getRole() + " screen loads " + tabs.getTitleAt(i),!all(tabs.getSelectedComponent(),JTable.class).isEmpty());
                    }
                    tabs.setSelectedIndex(0);
                    capture(frame,"application/target/desktop-" + user.getRole() + ".png");
                    if (user.isAdministrator()) { tabs.setSelectedIndex(3); capture(frame,"application/target/desktop-booking-review.png"); }
                    frame.dispose();
                }
                AccountDialog registration=new AccountDialog(null,app,null,List.of(new LookupOption(1,"Test department")),() -> {});
                check("Public registration has no role selector",all(registration,JComboBox.class).size()==1);
                check("Registration uses hidden password fields",all(registration,JPasswordField.class).size()==2);
                registration.dispose();
                System.out.println("DESKTOP_CHECKS_PASSED=" + passed);
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> { for (Window window : Window.getWindows()) { window.dispose(); } });
        }
    }
    private static void capture(Window window,String name) {
        try {
            BufferedImage image=new BufferedImage(window.getWidth(),window.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics=image.createGraphics(); window.printAll(graphics); graphics.dispose(); ImageIO.write(image,"png",Path.of(name).toFile());
        } catch (Exception e) { throw new IllegalStateException("Unable to render the desktop test image.",e); }
    }
    private static <T> List<T> all(Component root,Class<T> type) {
        List<T> found=new ArrayList<>(); if (type.isInstance(root)) { found.add(type.cast(root)); }
        if (root instanceof Container parent) { for (Component child : parent.getComponents()) { found.addAll(all(child,type)); } }
        return found;
    }
}
