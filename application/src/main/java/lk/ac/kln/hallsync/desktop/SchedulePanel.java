package lk.ac.kln.hallsync.desktop;

import lk.ac.kln.hallsync.ApplicationServices;
import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.ScheduleEntry;
import lk.ac.kln.hallsync.service.BookingService;
import javax.swing.*;
import java.time.LocalDate;

public final class SchedulePanel extends TablePanel<ScheduleEntry> implements Refreshable {
    private final ApplicationServices app; private final AuthenticatedUser user;
    private final JTextField from=new JTextField(LocalDate.now(BookingService.ZONE).toString(),10);
    private final JTextField to=new JTextField(LocalDate.now(BookingService.ZONE).plusDays(7).toString(),10);
    public SchedulePanel(ApplicationServices app, AuthenticatedUser user) {
        super(new String[]{"Booking ID","Hall","Course","Date","Start","End","Type","Status"},
                s -> new Object[]{s.bookingId(),s.hallLabel(),s.courseCode(),s.date(),s.start(),s.end(),s.type(),s.status()});
        this.app=app; this.user=user; filters.add(new JLabel("From:")); filters.add(from); filters.add(new JLabel("To:")); filters.add(to); button("Load timetable",this::refresh);
    }
    @Override public void refresh() { UiErrors.attempt(this,() -> {
        LocalDate first=FormPanel.date(from),last=FormPanel.date(to);
        UiTasks.run(this,"Loading approved and completed sessions...",() -> app.bookings.schedule(user,first,last),this::setRows);
    }); }
}
