package lk.ac.kln.hallsync.desktop;

import javax.swing.*;
import java.awt.Component;
import java.sql.SQLException;

public final class UiErrors {
    private UiErrors() { }
    public static void show(Component owner, Throwable error) {
        String message;
        if (error instanceof SQLException sql) {
            message = switch (sql.getErrorCode()) {
                case 1062 -> "That unique code, email, username or group identity already exists. Check the existing records.";
                case 1045 -> "The MySQL connection login was rejected. Check your private db.local.properties.";
                case 1142,1370 -> "The database login lacks the required permission. Check the hallsync_application role and the desktop read upgrade.";
                case 1305 -> "A required database procedure is missing. For an existing database, run database/Upgrade_Desktop_Reads.sql in Workbench.";
                case 1452 -> "A selected department, hall, course, facility or group no longer exists. Reload your choices.";
                case 1205,1213 -> "Another transaction changed these records. Reload and retry the action.";
                default -> "45000".equals(sql.getSQLState()) ? sql.getMessage() : "Database operation failed (state " + sql.getSQLState() + ", code " + sql.getErrorCode() + ").";
            };
        } else if (error instanceof IllegalArgumentException || error instanceof SecurityException || error instanceof IllegalStateException) {
            message = error.getMessage();
        } else { message = "The operation could not finish. Check the local file path, database connection or selected input."; }
        JTextArea text = new JTextArea(message,4,50); text.setLineWrap(true); text.setWrapStyleWord(true); text.setEditable(false);
        text.setBackground(UIManager.getColor("Panel.background"));
        JOptionPane.showMessageDialog(owner,text,"HallSync",JOptionPane.ERROR_MESSAGE);
    }
    public static void info(Component owner, String message) { JOptionPane.showMessageDialog(owner,message,"HallSync",JOptionPane.INFORMATION_MESSAGE); }
    public static boolean confirm(Component owner, String message) {
        return JOptionPane.showConfirmDialog(owner,message,"Confirm action",JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
    }
    public static void attempt(Component owner, Runnable action) {
        try { action.run(); } catch (RuntimeException error) { show(owner,error); }
    }
}
