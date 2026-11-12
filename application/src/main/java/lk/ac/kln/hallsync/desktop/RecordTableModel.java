package lk.ac.kln.hallsync.desktop;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.function.Function;

/** Reusable read-only table model; editing always goes through a service. */
public final class RecordTableModel<T> extends AbstractTableModel {
    private final String[] columns;
    private final Function<T,Object[]> cells;
    private List<T> rows = List.of();
    public RecordTableModel(String[] columns, Function<T,Object[]> cells) { this.columns = columns.clone(); this.cells = cells; }
    public void setRows(List<T> rows) { this.rows = List.copyOf(rows); fireTableDataChanged(); }
    public T row(int index) { return rows.get(index); }
    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return columns.length; }
    @Override public String getColumnName(int column) { return columns[column]; }
    @Override public Object getValueAt(int row, int column) { return cells.apply(rows.get(row))[column]; }
    @Override public Class<?> getColumnClass(int column) {
        for (int i = 0; i < rows.size(); i++) { Object v = getValueAt(i,column); if (v != null) { return v.getClass(); } }
        return Object.class;
    }
    @Override public boolean isCellEditable(int row, int column) { return false; }
}
