package lk.ac.kln.hallsync.desktop;

import lk.ac.kln.hallsync.model.LookupOption;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class FormPanel extends JPanel {
    private int row;
    public FormPanel() { super(new GridBagLayout()); setBorder(BorderFactory.createEmptyBorder(8,8,8,8)); }
    public <T extends JComponent> T field(String label, T input) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx=0; c.gridy=row; c.anchor=GridBagConstraints.WEST; c.insets=new Insets(6,4,6,14);
        add(new JLabel(label),c); c.gridx=1; c.weightx=1; c.fill=GridBagConstraints.HORIZONTAL; c.insets=new Insets(6,4,6,4); add(input,c); row++; return input;
    }
    public JTextField text(String label, String initial) { return field(label,new JTextField(initial == null ? "" : initial,28)); }
    public JComboBox<LookupOption> choices(String label, List<LookupOption> options, boolean optional) {
        JComboBox<LookupOption> box = new JComboBox<>(); if (optional) { box.addItem(new LookupOption(0,"None / all")); }
        options.forEach(box::addItem); return field(label,box);
    }
    public boolean confirm(Component owner, String title) {
        return JOptionPane.showConfirmDialog(owner,this,title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }
    public static Integer optionalId(JComboBox<LookupOption> box) {
        LookupOption option = (LookupOption)box.getSelectedItem(); return option == null || option.id()==0 ? null : option.id();
    }
    public static int requiredId(JComboBox<LookupOption> box) {
        Integer id = optionalId(box); if (id == null) { throw new IllegalArgumentException("Choose a record from each required list."); } return id;
    }
    public static void select(JComboBox<LookupOption> box, Integer id) {
        int wanted = id == null ? 0 : id; for (int i=0;i<box.getItemCount();i++) { if (box.getItemAt(i).id()==wanted) { box.setSelectedIndex(i); return; } }
    }
    public static int integer(JTextField input, String label) {
        try { return Integer.parseInt(input.getText().strip()); } catch (NumberFormatException e) { throw new IllegalArgumentException(label + " must be a whole number within the INT range."); }
    }
    public static LocalDate date(JTextField input) {
        try { return LocalDate.parse(input.getText().strip()); } catch (DateTimeParseException e) { throw new IllegalArgumentException("Use a real date in YYYY-MM-DD format."); }
    }
    public static LocalTime time(JTextField input) {
        try { return LocalTime.parse(input.getText().strip()); } catch (DateTimeParseException e) { throw new IllegalArgumentException("Use a 24-hour time such as 09:00 or 14:30."); }
    }
    public static LocalDateTime timestamp(JTextField input) {
        try { return LocalDateTime.parse(input.getText().strip().replace(' ','T')); } catch (DateTimeParseException e) { throw new IllegalArgumentException("Use YYYY-MM-DD HH:mm for each block timestamp."); }
    }
}
