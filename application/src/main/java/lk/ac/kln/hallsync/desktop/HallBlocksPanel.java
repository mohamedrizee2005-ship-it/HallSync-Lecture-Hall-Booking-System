package lk.ac.kln.hallsync.desktop;

import lk.ac.kln.hallsync.ApplicationServices;
import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.*;
import lk.ac.kln.hallsync.service.BookingService;
import javax.swing.*;
import java.time.*;

public final class HallBlocksPanel extends TablePanel<HallBlock> implements Refreshable {
    private final ApplicationServices app; private final AuthenticatedUser user;
    public HallBlocksPanel(ApplicationServices app, AuthenticatedUser user) {
        super(new String[]{"Block ID","Hall","Start","End","Reason","Status","Recorded by"},
                b -> new Object[]{b.id(),b.hallLabel(),b.start(),b.end(),b.reason(),b.status(),b.recordedBy()});
        this.app=app; this.user=user;
        table.getColumnModel().getColumn(2).setPreferredWidth(180); table.getColumnModel().getColumn(3).setPreferredWidth(180);
        button("Refresh",this::refresh); button("Create hall block",this::create); button("Cancel selected block",this::cancel);
    }
    @Override public void refresh() { UiTasks.run(this,"Loading maintenance and closure history...",() -> app.blocks.list(user),this::setRows); }
    private void create() {
        UiTasks.run(this,"Loading halls...",app.catalog::halls,halls -> {
            FormPanel f=new FormPanel(); JComboBox<LookupOption> hall=f.choices("Hall",halls.stream().map(LectureHall::option).toList(),false);
            String tomorrow=LocalDate.now(BookingService.ZONE).plusDays(1).toString();
            JTextField start=f.text("Start (YYYY-MM-DD HH:mm)",tomorrow + " 09:00"),end=f.text("End (Asia/Colombo)",tomorrow + " 12:00"),reason=f.text("Reason","");
            if (!f.confirm(this,"Create hall block")) { return; }
            int id=FormPanel.requiredId(hall); LocalDateTime s=FormPanel.timestamp(start),e=FormPanel.timestamp(end); String r=reason.getText();
            UiTasks.run(this,"Checking approved bookings before blocking the hall...",() -> app.blocks.create(user,id,s,e,r),
                    created -> { UiErrors.info(this,"Hall block created. Block ID: " + created); refresh(); });
        });
    }
    private void cancel() {
        HallBlock block=selected();
        if (UiErrors.confirm(this,"Cancel hall block " + block.id() + "? Its history will be retained.")) {
            UiTasks.run(this,"Cancelling hall block...",() -> { app.blocks.cancel(user,block.id()); return true; },ignored -> refresh());
        }
    }
}
