package lk.ac.kln.hallsync.desktop;

import lk.ac.kln.hallsync.DatabaseConfig;
import lk.ac.kln.hallsync.DatabaseConnectionFactory;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

/** First-run connection form. It tests existing credentials and saves only a new local file. */
public final class ConnectionSetupDialog extends JDialog {
    private DatabaseConfig result;
    public ConnectionSetupDialog(Path file) {
        super((Frame)null,"HallSync - First connection",true);
        FormPanel form=new FormPanel();
        form.field("Before starting",new JLabel("Install the database and create the limited login using the README steps."));
        JTextField url=form.text("MySQL JDBC address","jdbc:mysql://127.0.0.1:3306/hallsync_db");
        JTextField username=form.text("MySQL connection username","hallsync_app");
        JPasswordField password=form.field("Private MySQL password",new JPasswordField(28));
        String override=System.getenv("HALLSYNC_DB_PASSWORD");
        if (override != null && !override.isBlank()) {
            password.setText(override);
            form.field("Environment",new JLabel("HALLSYNC_DB_PASSWORD is set and also overrides later file loading."));
        }
        form.field("Saved locally",new JLabel(file.toAbsolutePath().toString()));
        JButton save=new JButton("Test and save connection"),cancel=new JButton("Cancel");
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.add(cancel); buttons.add(save);
        save.addActionListener(e -> UiErrors.attempt(this,() -> {
            if ("root".equalsIgnoreCase(username.getText().strip())) {
                throw new IllegalArgumentException("Use the limited hallsync_app connection login, not the MySQL root login.");
            }
            char[] secret=password.getPassword(); DatabaseConfig candidate;
            try { candidate=DatabaseConfig.fromValues(url.getText(),username.getText(),secret); }
            finally { Arrays.fill(secret,'\0'); password.setText(""); }
            UiTasks.run(this,"Testing the connection before saving...",() -> {
                try (var c=new DatabaseConnectionFactory(candidate).open();
                     var statement=c.prepareStatement("SELECT BookingID FROM v_booking_details LIMIT 1");
                     var rows=statement.executeQuery()) {
                    // A successful view read checks the connection and installed booking schema.
                }
                candidate.saveNew(file); return candidate;
            },config -> { result=config; dispose(); });
        }));
        cancel.addActionListener(e -> { password.setText(""); dispose(); });
        setLayout(new BorderLayout()); add(form); add(buttons,BorderLayout.SOUTH); getRootPane().setDefaultButton(save);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); pack(); setLocationRelativeTo(null);
    }
    public Optional<DatabaseConfig> configuration() { return Optional.ofNullable(result); }
}
