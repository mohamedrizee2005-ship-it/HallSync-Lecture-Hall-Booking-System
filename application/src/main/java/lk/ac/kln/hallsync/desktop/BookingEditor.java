package lk.ac.kln.hallsync.desktop;

import lk.ac.kln.hallsync.ApplicationServices;
import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.*;
import lk.ac.kln.hallsync.service.BookingService;
import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.util.List;
import java.util.stream.IntStream;

public final class BookingEditor extends JDialog {
    private record Choices(List<LectureHall> halls, List<CourseUnit> courses, List<StudentGroup> groups) { }
    public static void openNew(Component owner, ApplicationServices app, AuthenticatedUser user, Integer hall,
                               LocalDate date, LocalTime start, LocalTime end, int attendance, Runnable saved) {
        load(owner,app,c -> new BookingEditor(owner,app,user,c,null,List.of(),hall,date,start,end,attendance,saved).setVisible(true));
    }
    public static void openEdit(Component owner, ApplicationServices app, AuthenticatedUser user, Booking old, List<Integer> groups, Runnable saved) {
        load(owner,app,c -> new BookingEditor(owner,app,user,c,old,groups,old.hallId(),old.date(),old.start(),old.end(),old.attendance(),saved).setVisible(true));
    }
    private static void load(Component owner, ApplicationServices app, java.util.function.Consumer<Choices> ready) {
        UiTasks.run(owner,"Loading halls, courses and student groups...",() -> new Choices(app.catalog.halls(),app.catalog.courses(),app.catalog.groups()),ready);
    }
    private BookingEditor(Component owner, ApplicationServices app, AuthenticatedUser user, Choices choices, Booking old,
                          List<Integer> oldGroups, Integer preferredHall, LocalDate date, LocalTime start, LocalTime end, int attendance, Runnable saved) {
        super(SwingUtilities.getWindowAncestor(owner),old == null ? "Request a lecture hall" : "Edit Pending booking " + old.id(),ModalityType.APPLICATION_MODAL);
        FormPanel form = new FormPanel(); form.field("Responsible requester",new JLabel(user.getUsername() + " (signed in)"));
        JComboBox<LookupOption> hall=form.choices("Lecture hall",choices.halls().stream().map(LectureHall::option).toList(),false); FormPanel.select(hall,preferredHall);
        JTextField day=form.text("Date (YYYY-MM-DD)",date.toString()), begins=form.text("Start (24-hour HH:mm)",start.toString()), ends=form.text("End (same day)",end.toString());
        JComboBox<BookingType> type=form.field("Booking type",new JComboBox<>(BookingType.values())); if (old != null) { type.setSelectedItem(old.type()); }
        JComboBox<LookupOption> course=form.choices("Course (required for teaching)",choices.courses().stream().map(CourseUnit::option).toList(),true);
        if (old != null) { FormPanel.select(course,old.courseId()); }
        JList<LookupOption> groups=new JList<>(choices.groups().stream().map(StudentGroup::option).toArray(LookupOption[]::new));
        groups.setVisibleRowCount(5); groups.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        groups.setSelectedIndices(IntStream.range(0,groups.getModel().getSize()).filter(i -> oldGroups.contains(groups.getModel().getElementAt(i).id())).toArray());
        form.field("Student groups (Ctrl+click)",new JScrollPane(groups));
        JTextField purpose=form.text("Purpose",old == null ? "" : old.purpose()), people=form.text("Expected attendance",String.valueOf(attendance));
        JLabel note=new JLabel("Asia/Colombo time. A Pending request does not reserve the hall."); note.setBorder(BorderFactory.createEmptyBorder(14,14,0,14));
        JButton save=new JButton(old == null ? "Submit request" : "Save Pending booking"), cancel=new JButton("Cancel");
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.add(cancel); buttons.add(save);
        save.addActionListener(e -> UiErrors.attempt(this,() -> {
            BookingDraft draft=new BookingDraft(FormPanel.requiredId(hall),FormPanel.optionalId(course),FormPanel.date(day),
                    FormPanel.time(begins),FormPanel.time(ends),purpose.getText(),(BookingType)type.getSelectedItem(),
                    FormPanel.integer(people,"Attendance"),groups.getSelectedValuesList().stream().map(LookupOption::id).toList());
            UiTasks.run(this,"Validating and saving your request...",() -> {
                if (old == null) { return app.bookings.create(user,draft); }
                app.bookings.update(user,old.id(),draft); return old.id();
            },id -> { UiErrors.info(this,"Booking " + id + " is Pending. An independent administrator must approve it."); dispose(); saved.run(); });
        }));
        cancel.addActionListener(e -> dispose()); setLayout(new BorderLayout()); add(note,BorderLayout.NORTH); add(form); add(buttons,BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save); setDefaultCloseOperation(DISPOSE_ON_CLOSE); pack(); setLocationRelativeTo(owner);
    }
    public static void openBlank(Component owner, ApplicationServices app, AuthenticatedUser user, Runnable saved) {
        openNew(owner,app,user,null,LocalDate.now(BookingService.ZONE).plusDays(1),LocalTime.of(9,0),LocalTime.of(10,0),30,saved);
    }
}
