package lk.ac.kln.hallsync.desktop;

import lk.ac.kln.hallsync.ApplicationServices;
import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.*;
import lk.ac.kln.hallsync.service.CsvExporter;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;

public final class ReportsPanel extends JPanel implements Refreshable {
    private final ApplicationServices app; private final AuthenticatedUser user;
    private final JComboBox<ReportKind> kind=new JComboBox<>(ReportKind.values()); private final JTable table=new JTable();
    private final JLabel note=new JLabel("Scheduled minutes measure reserved time, not actual attendance or measured utilisation.");
    private ReportTable loaded; private ReportKind loadedKind;
    public ReportsPanel(ApplicationServices app, AuthenticatedUser user) {
        super(new BorderLayout(12,14)); this.app=app; this.user=user; setBorder(BorderFactory.createEmptyBorder(18,18,18,18));
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT)); JButton refresh=new JButton("Load report"),export=new JButton("Export loaded report as CSV");
        actions.add(kind); actions.add(refresh); actions.add(export); add(actions,BorderLayout.NORTH); add(new JScrollPane(table)); add(note,BorderLayout.SOUTH);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); table.setRowHeight(29);
        refresh.addActionListener(e -> refresh()); export.addActionListener(e -> UiErrors.attempt(this,this::export));
        kind.addActionListener(e -> { loaded=null; loadedKind=null; table.setModel(new DefaultTableModel()); });
    }
    @Override public void refresh() {
        ReportKind chosen=(ReportKind)kind.getSelectedItem();
        UiTasks.run(this,"Loading report...",() -> app.reports.load(user,chosen),report -> {
            loaded=report; loadedKind=chosen;
            DefaultTableModel model=new DefaultTableModel(report.columns().toArray(),0) { @Override public boolean isCellEditable(int row,int col) { return false; } };
            report.rows().forEach(row -> model.addRow(row.toArray())); table.setModel(model); table.setAutoCreateRowSorter(true);
            for (int i=0;i<table.getColumnCount();i++) { table.getColumnModel().getColumn(i).setPreferredWidth(150); }
            note.setText(report.rows().size() + " rows. Scheduled minutes are reserved time, not measured actual use.");
        });
    }
    private void export() {
        if (loaded==null) { throw new IllegalArgumentException("Load the chosen report before exporting."); }
        JFileChooser chooser=new JFileChooser(); chooser.setSelectedFile(new java.io.File("HallSync_" + loadedKind.name() + ".csv"));
        if (chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) { return; }
        Path file=chooser.getSelectedFile().toPath();
        if (java.nio.file.Files.exists(file) && !UiErrors.confirm(this,"Replace the existing file?")) { return; }
        ReportTable snapshot=loaded;
        UiTasks.run(this,"Writing CSV report...",() -> { CsvExporter.write(file,snapshot); return file; },path -> UiErrors.info(this,"CSV report saved to " + path));
    }
}
