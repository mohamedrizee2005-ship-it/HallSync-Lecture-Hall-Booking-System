package lk.ac.kln.hallsync.desktop;

import lk.ac.kln.hallsync.ApplicationServices;
import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.*;
import javax.swing.*;

public final class BookingsPanel extends TablePanel<Booking> implements Refreshable {
    private final ApplicationServices app; private final AuthenticatedUser user; private final boolean reviewMode;
    public BookingsPanel(ApplicationServices app, AuthenticatedUser user, boolean reviewMode) {
        super(new String[]{"Booking ID","Status","Date","Start","End","Hall","Course","Type","People","Requester"},
                b -> new Object[]{b.id(),b.status(),b.date(),b.start(),b.end(),b.hallLabel(),b.courseCode(),b.type(),b.attendance(),b.requesterName()});
        this.app=app; this.user=user; this.reviewMode=reviewMode;
        button("Refresh",this::refresh); button("Details",this::details);
        if (reviewMode) {
            button("Approve",() -> review("Approved")); button("Reject",() -> review("Rejected")); button("Mark Completed",this::complete);
        } else {
            button("New request",() -> BookingEditor.openBlank(this,app,user,this::refresh)); button("Edit Pending",this::edit);
        }
        button("Cancel booking",this::cancel);
    }
    @Override public void refresh() { UiTasks.run(this,"Loading bookings...",() -> reviewMode ? app.bookings.all(user) : app.bookings.mine(user),this::setRows); }
    private void edit() {
        Booking booking=selected();
        if (booking.status()!=BookingStatus.Pending) { throw new IllegalArgumentException("Only Pending bookings may be edited. Cancel an Approved booking and make a new request."); }
        UiTasks.run(this,"Loading teaching groups...",() -> app.bookings.groupsForEdit(user,booking.id()),
                groups -> BookingEditor.openEdit(this,app,user,booking,groups,this::refresh));
    }
    private void review(String decision) {
        Booking booking=selected();
        String note=JOptionPane.showInputDialog(this,"Optional review note (maximum 500 characters)",decision + " - booking " + booking.id(),JOptionPane.PLAIN_MESSAGE);
        if (note == null) { return; }
        if (UiErrors.confirm(this,decision + " booking " + booking.id() + "?")) {
            UiTasks.run(this,"Checking the booking and saving the decision...",() -> { app.bookings.review(user,booking.id(),decision,note); return true; },ignored -> refresh());
        }
    }
    private void cancel() {
        Booking booking=selected();
        if (UiErrors.confirm(this,"Cancel future booking " + booking.id() + "? Its history will be retained.")) {
            UiTasks.run(this,"Cancelling the booking...",() -> { app.bookings.cancel(user,booking.id()); return true; },ignored -> refresh());
        }
    }
    private void complete() {
        Booking booking=selected();
        if (UiErrors.confirm(this,"Mark ended Approved booking " + booking.id() + " as Completed?")) {
            UiTasks.run(this,"Checking the end time...",() -> { app.bookings.complete(user,booking.id()); return true; },ignored -> refresh());
        }
    }
    private void details() {
        Booking b=selected(); JTextArea text=new JTextArea(16,65); text.setEditable(false); text.setLineWrap(true); text.setWrapStyleWord(true);
        text.setText("Booking " + b.id() + " | " + b.status() + "\nRequester: " + b.requesterName()
                + "\nHall: " + b.hallLabel() + "\nDate: " + b.date() + "\nTime: " + b.start() + " - " + b.end()
                + " (Asia/Colombo)\nType: " + b.type() + "\nCourse: " + value(b.courseCode()) + "\nAttendance: " + b.attendance()
                + "\nPurpose: " + b.purpose() + "\nRequested at: " + b.requestedAt() + "\nReviewer: " + value(b.reviewerName())
                + "\nDecision: " + value(b.decision()) + "\nReviewed at: " + value(b.reviewedAt()) + "\nReview note: " + value(b.reviewNote()));
        JOptionPane.showMessageDialog(this,new JScrollPane(text),"Booking details",JOptionPane.PLAIN_MESSAGE);
    }
    private static String value(Object v) { return v == null ? "-" : v.toString(); }
}
