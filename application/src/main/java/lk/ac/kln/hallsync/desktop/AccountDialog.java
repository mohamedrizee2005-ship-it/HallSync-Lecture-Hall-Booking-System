package lk.ac.kln.hallsync.desktop;

import lk.ac.kln.hallsync.ApplicationServices;
import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.*;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/** Public registration is fixed to Student; only a signed-in admin sees role provisioning. */
public final class AccountDialog extends JDialog {
    public AccountDialog(Window owner, ApplicationServices app, AuthenticatedUser admin,
                         List<LookupOption> departments, Runnable saved) {
        super(owner,admin == null ? "Register a student account" : "Provision an application account",ModalityType.APPLICATION_MODAL);
        FormPanel form = new FormPanel(); JComboBox<LookupOption> department = form.choices("Department (optional)",departments,true);
        JTextField name = form.text("Full name",""); JTextField email = form.text("Email",""); JTextField username = form.text("Username","");
        JComboBox<String> role = new JComboBox<>(new String[]{"Student","Lecturer","AdminStaff"});
        if (admin != null) { form.field("Role",role); } else { form.field("Role",new JLabel("Student (assigned automatically)")); }
        JPasswordField password = form.field("Password (15-128 characters)",new JPasswordField(28));
        JPasswordField confirmation = form.field("Repeat password",new JPasswordField(28));
        JLabel notice = new JLabel("Choose a private password or passphrase. Staff privileges require an administrator.");
        notice.setBorder(BorderFactory.createEmptyBorder(12,12,4,12));
        JButton save = new JButton(admin == null ? "Register student" : "Create account"); JButton cancel = new JButton("Cancel");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.add(cancel); buttons.add(save);
        save.addActionListener(e -> UiErrors.attempt(this,() -> {
            char[] first = password.getPassword(); char[] second = confirmation.getPassword();
            boolean match = Arrays.equals(first,second); Arrays.fill(second,'\0'); password.setText(""); confirmation.setText("");
            if (!match) { Arrays.fill(first,'\0'); throw new IllegalArgumentException("The passwords do not match. Enter them again."); }
            AccountDraft details = new AccountDraft(FormPanel.optionalId(department),name.getText(),email.getText(),username.getText());
            String chosenRole = (String)role.getSelectedItem();
            UiTasks.run(this,"Securing the password and creating the account...",() -> {
                try { return admin == null ? app.accounts.registerStudent(details,first) : app.accounts.provision(admin,details,first,chosenRole); }
                finally { Arrays.fill(first,'\0'); }
            },id -> { UiErrors.info(this,"Account created. Account ID: " + id + ". Use the chosen username and password to sign in."); dispose(); saved.run(); });
        }));
        cancel.addActionListener(e -> { password.setText(""); confirmation.setText(""); dispose(); });
        setLayout(new BorderLayout()); add(notice,BorderLayout.NORTH); add(form); add(buttons,BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save); setDefaultCloseOperation(DISPOSE_ON_CLOSE); pack(); setLocationRelativeTo(owner);
    }
}
