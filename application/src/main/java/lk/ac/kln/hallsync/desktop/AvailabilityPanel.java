package lk.ac.kln.hallsync.desktop;

import lk.ac.kln.hallsync.ApplicationServices;
import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.*;
import javax.swing.*;
import java.time.*;
import java.util.List;

public final class AvailabilityPanel extends TablePanel<LectureHall> implements Refreshable {
    private final ApplicationServices app; private final AuthenticatedUser user;
    private final JTextField date = new JTextField(LocalDate.now(lk.ac.kln.hallsync.service.BookingService.ZONE).plusDays(1).toString(),10);
    private final JTextField start = new JTextField("09:00",6), end = new JTextField("10:00",6), attendance = new JTextField("30",5);
    public AvailabilityPanel(ApplicationServices app, AuthenticatedUser user) {
        super(new String[]{"Hall ID","Building","Hall","Floor","Capacity","Status"},h -> new Object[]{h.id(),h.buildingName(),h.label(),h.floor(),h.capacity(),h.status()});
        this.app=app; this.user=user;
        filters.add(new JLabel("Date:")); filters.add(date); filters.add(new JLabel("Start:")); filters.add(start);
        filters.add(new JLabel("End:")); filters.add(end); filters.add(new JLabel("People:")); filters.add(attendance);
        button("All halls",this::refresh); button("Find available halls",this::search); button("Equipment in selected hall",this::equipment);
        if (user.isAdministrator() || "Lecturer".equals(user.getRole())) {
            button("Request selected hall",() -> BookingEditor.openNew(this,app,user,selected().id(),FormPanel.date(date),
                    FormPanel.time(start),FormPanel.time(end),FormPanel.integer(attendance,"Attendance"),this::search));
        }
        table.setToolTipText("Availability is a snapshot. A Pending request does not reserve a hall; approval checks again.");
    }
    @Override public void refresh() { UiTasks.run(this,"Loading halls...",app.catalog::halls,this::setRows); }
    private void search() {
        LocalDate d=FormPanel.date(date); LocalTime s=FormPanel.time(start), e=FormPanel.time(end); int n=FormPanel.integer(attendance,"Attendance");
        UiTasks.run(this,"Checking availability in Asia/Colombo...",() -> app.catalog.available(user,d,s,e,n),this::setRows);
    }
    private void equipment() {
        LectureHall hall = selected();
        UiTasks.run(this,"Loading hall equipment...",app.catalog::equipment,all -> {
            TablePanel<HallFacility> panel = new TablePanel<>(new String[]{"Facility","Total","Working","Not working"},
                    f -> new Object[]{f.facilityName(),f.quantity(),f.workingQuantity(),f.quantity()-f.workingQuantity()});
            panel.setRows(all.stream().filter(f -> f.hallId()==hall.id()).toList()); panel.setPreferredSize(new java.awt.Dimension(700,350));
            JOptionPane.showMessageDialog(this,panel,hall.label(),JOptionPane.PLAIN_MESSAGE);
        });
    }
}
