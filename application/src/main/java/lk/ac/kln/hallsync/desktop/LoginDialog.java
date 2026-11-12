package lk.ac.kln.hallsync.desktop;

import lk.ac.kln.hallsync.ApplicationServices;
import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.LookupOption;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Optional;

public final class LoginDialog extends JDialog {
    private AuthenticatedUser result;
    public LoginDialog(ApplicationServices app) {
        super((Frame)null,"HallSync - Sign in",true);
        JLabel title = new JLabel("HallSync | Faculty of Science"); title.setFont(title.getFont().deriveFont(Font.BOLD,22f));
        title.setBorder(BorderFactory.createEmptyBorder(22,22,10,22));
        FormPanel form = new FormPanel(); JTextField username = form.text("Application username","");
        JPasswordField password = form.field("Application password",new JPasswordField(28));
        form.field("Connection",new JLabel("Your local hallsync_app settings are already configured."));
        JButton login = new JButton("Sign in"); JButton register = new JButton("Register student"); JButton exit = new JButton("Exit");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,12)); buttons.add(register); buttons.add(exit); buttons.add(login);
        login.addActionListener(e -> {
            String name = username.getText(); char[] secret = password.getPassword(); password.setText("");
            UiTasks.run(this,"Checking your application account...",() -> {
                try { return app.authentication.signIn(name,secret); } finally { Arrays.fill(secret,'\0'); }
            },identity -> {
                if (identity.isPresent()) { result = identity.get(); dispose(); }
                else { UiErrors.info(this,"Sign-in failed. Check your application username and password. The account must be active."); }
            });
        });
        register.addActionListener(e -> UiTasks.run(this,"Loading department choices...",() -> app.departments.list().stream()
                .map(d -> new LookupOption(d.getDepartmentId(),d.getDepartmentName())).toList(),
                departments -> new AccountDialog(this,app,null,departments,() -> {}).setVisible(true)));
        exit.addActionListener(e -> dispose()); setLayout(new BorderLayout()); add(title,BorderLayout.NORTH); add(form); add(buttons,BorderLayout.SOUTH);
        getRootPane().setDefaultButton(login); setDefaultCloseOperation(DISPOSE_ON_CLOSE); pack(); setLocationRelativeTo(null);
    }
    public Optional<AuthenticatedUser> identity() { return Optional.ofNullable(result); }
}
