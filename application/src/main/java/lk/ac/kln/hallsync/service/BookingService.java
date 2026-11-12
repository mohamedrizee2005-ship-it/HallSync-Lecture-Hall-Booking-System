package lk.ac.kln.hallsync.service;

import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.*;
import lk.ac.kln.hallsync.repository.BookingRepository;
import java.sql.SQLException;
import java.time.*;
import java.util.HashSet;
import java.util.List;

public final class BookingService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Colombo");
    private final BookingRepository repository;
    private final Clock clock;
    public BookingService(BookingRepository repository, Clock clock) { this.repository = repository; this.clock = clock.withZone(ZONE); }
    public List<Booking> mine(AuthenticatedUser user) throws SQLException { return repository.forRequester(AccessRules.requester(user)); }
    public List<Booking> all(AuthenticatedUser user) throws SQLException { AccessRules.administrator(user); return repository.all(); }
    public List<ScheduleEntry> schedule(AuthenticatedUser user, LocalDate from, LocalDate to) throws SQLException {
        AccessRules.signedIn(user);
        if (from == null || to == null || to.isBefore(from)) { throw new IllegalArgumentException("Use a valid timetable date range."); }
        return repository.schedule(from, to);
    }
    public int create(AuthenticatedUser user, BookingDraft input) throws SQLException {
        int actor = AccessRules.requester(user); return repository.create(actor, validate(input));
    }
    public void update(AuthenticatedUser user, int id, BookingDraft input) throws SQLException {
        int actor = AccessRules.requester(user); Booking old = owned(user, id);
        if (old.status() != BookingStatus.Pending) { throw new IllegalArgumentException("Only Pending bookings may be edited. Cancel an Approved booking and create a new request."); }
        repository.update(actor, id, validate(input));
    }
    public List<Integer> groupsForEdit(AuthenticatedUser user, int id) throws SQLException {
        AccessRules.requester(user); owned(user, id); return repository.groups(id);
    }
    public void review(AuthenticatedUser user, int id, String decision, String note) throws SQLException {
        int actor = AccessRules.administrator(user); Booking booking = found(id);
        if (actor == booking.requesterId()) { throw new SecurityException("An approver must not review their own booking."); }
        if (booking.status() != BookingStatus.Pending) { throw new IllegalArgumentException("Only Pending bookings may be reviewed."); }
        if (!"Approved".equals(decision) && !"Rejected".equals(decision)) { throw new IllegalArgumentException("Choose Approved or Rejected."); }
        repository.review(actor, id, decision, Validation.optional(note,"Review note",500));
    }
    public void cancel(AuthenticatedUser user, int id) throws SQLException {
        int actor = AccessRules.signedIn(user); Booking booking = found(id);
        if (!user.isAdministrator() && booking.requesterId() != actor) { throw new SecurityException("Only the requester or an administrator may cancel."); }
        if (booking.status() != BookingStatus.Pending && booking.status() != BookingStatus.Approved) {
            throw new IllegalArgumentException("This booking is terminal and cannot be cancelled.");
        }
        repository.cancel(actor, id);
    }
    public void complete(AuthenticatedUser user, int id) throws SQLException {
        int actor = AccessRules.administrator(user); Booking booking = found(id);
        if (booking.status() != BookingStatus.Approved || LocalDateTime.of(booking.date(),booking.end()).isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("Only an ended Approved booking may become Completed.");
        }
        repository.complete(actor, id);
    }
    private Booking owned(AuthenticatedUser user, int id) throws SQLException {
        Booking booking = found(id);
        if (booking.requesterId() != user.getAccountId()) { throw new SecurityException("You may edit only your own booking."); }
        return booking;
    }
    private Booking found(int id) throws SQLException {
        return repository.find(Validation.positive(id,"Booking ID")).orElseThrow(() -> new IllegalArgumentException("Booking not found. Reload the list."));
    }
    private BookingDraft validate(BookingDraft d) {
        if (d == null) { throw new IllegalArgumentException("Booking details are required."); }
        Validation.positive(d.hallId(),"Hall"); Validation.optionalId(d.courseId(),"Course"); Validation.positive(d.attendance(),"Attendance");
        validateInterval(d.date(), d.start(), d.end(), clock);
        if (d.type() == null) { throw new IllegalArgumentException("Choose a booking type."); }
        if (d.type().requiresAcademics() && (d.courseId() == null || d.groupIds().isEmpty())) {
            throw new IllegalArgumentException("Lectures and tutorials require a course and at least one student group.");
        }
        for (Integer group : d.groupIds()) { if (group == null) { throw new IllegalArgumentException("Choose valid groups."); } Validation.positive(group,"Group"); }
        if (new HashSet<>(d.groupIds()).size() != d.groupIds().size()) { throw new IllegalArgumentException("A group cannot be selected twice."); }
        return new BookingDraft(d.hallId(),d.courseId(),d.date(),d.start(),d.end(),Validation.text(d.purpose(),"Purpose",255),d.type(),d.attendance(),d.groupIds());
    }
    public static void validateInterval(LocalDate date, LocalTime start, LocalTime end, Clock clock) {
        if (date == null || start == null || end == null || !start.isBefore(end) || start.getNano() != 0 || end.getNano() != 0) {
            throw new IllegalArgumentException("Use same-day times with start before end, to whole seconds.");
        }
        if (!LocalDateTime.of(date,start).isAfter(LocalDateTime.now(clock.withZone(ZONE)))) {
            throw new IllegalArgumentException("The booking start must be in the future in Asia/Colombo.");
        }
    }
}
