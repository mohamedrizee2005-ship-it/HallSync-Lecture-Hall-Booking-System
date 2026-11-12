package lk.ac.kln.hallsync;

import lk.ac.kln.hallsync.desktop.*;
import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Files;

/** Desktop entry point. Run with application as the working directory. */
public final class HallSyncApp {
    private HallSyncApp() { }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DesktopTheme.apply();
            try {
                Path file=Path.of(args.length==0 ? "db.local.properties" : args[0]);
                DatabaseConfig config;
                if (Files.exists(file)) {
                    config=DatabaseConfig.load(file);
                } else {
                    ConnectionSetupDialog setup=new ConnectionSetupDialog(file); setup.setVisible(true);
                    if (setup.configuration().isEmpty()) { return; }
                    config=setup.configuration().get();
                }
                ApplicationServices app=new ApplicationServices(new DatabaseConnectionFactory(config));
                showLogin(app);
            } catch (Exception error) { UiErrors.show(null,error); }
        });
    }
    private static void showLogin(ApplicationServices app) {
        LoginDialog login=new LoginDialog(app); login.setVisible(true);
        login.identity().ifPresent(user -> new DashboardFrame(app,user,() -> showLogin(app)).open());
    }
}
