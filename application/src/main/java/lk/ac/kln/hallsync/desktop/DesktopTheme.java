package lk.ac.kln.hallsync.desktop;

import javax.swing.*;
import java.awt.*;

public final class DesktopTheme {
    public static final Color NAVY = new Color(18,42,76);
    private DesktopTheme() { }
    public static void apply() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { /* Use Swing's default theme. */ }
        Font font = new Font(Font.SANS_SERIF,Font.PLAIN,14);
        for (Object key : UIManager.getDefaults().keySet().toArray()) {
            if (key.toString().endsWith(".font")) { UIManager.put(key,font); }
        }
        UIManager.put("Table.rowHeight",29);
        UIManager.put("TabbedPane.contentOpaque",false);
        ToolTipManager.sharedInstance().setDismissDelay(15000);
    }
}
