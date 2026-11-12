package lk.ac.kln.hallsync.desktop;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Shared table layout, inherited by the feature panels. */
public class TablePanel<T> extends JPanel {
    protected final RecordTableModel<T> model;
    protected final JTable table;
    protected final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
    protected final JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
    private final JLabel count = new JLabel("0 records");
    public TablePanel(String[] columns, Function<T,Object[]> cells) {
        super(new BorderLayout(10,14)); setBorder(BorderFactory.createEmptyBorder(18,18,18,18));
        model = new RecordTableModel<>(columns,cells); table = new JTable(model); table.setRowHeight(29);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);
        for (int i = 0; i < columns.length; i++) { table.getColumnModel().getColumn(i).setPreferredWidth(i == 0 ? 70 : 145); }
        TableRowSorter<RecordTableModel<T>> sorter = new TableRowSorter<>(model); table.setRowSorter(sorter);
        JTextField search = new JTextField(20); search.setToolTipText("Filter these loaded records by any visible column");
        search.getDocument().addDocumentListener(new DocumentListener() {
            private void filter() { sorter.setRowFilter(search.getText().isEmpty() ? null : RowFilter.regexFilter("(?iu)" + Pattern.quote(search.getText()))); }
            public void insertUpdate(DocumentEvent e) { filter(); } public void removeUpdate(DocumentEvent e) { filter(); } public void changedUpdate(DocumentEvent e) { filter(); }
        });
        JPanel top = new JPanel(); top.setLayout(new BoxLayout(top,BoxLayout.Y_AXIS));
        top.add(actions); top.add(Box.createVerticalStrut(12)); top.add(filters);
        filters.add(new JLabel("Filter loaded rows:")); filters.add(search);
        add(top,BorderLayout.NORTH); add(new JScrollPane(table),BorderLayout.CENTER); add(count,BorderLayout.SOUTH);
    }
    protected JButton button(String text, Runnable action) {
        JButton button = new JButton(text); button.addActionListener(e -> UiErrors.attempt(this,action)); actions.add(button); return button;
    }
    public void setRows(List<T> rows) { model.setRows(rows); count.setText(rows.size() + " records loaded. Use Refresh to see database changes."); }
    public T selected() {
        int row = table.getSelectedRow();
        if (row < 0) { throw new IllegalArgumentException("Select one record from the table first."); }
        return model.row(table.convertRowIndexToModel(row));
    }
}
