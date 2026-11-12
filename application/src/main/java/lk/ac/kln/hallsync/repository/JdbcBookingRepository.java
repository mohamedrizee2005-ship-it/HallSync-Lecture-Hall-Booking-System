package lk.ac.kln.hallsync.repository;

import lk.ac.kln.hallsync.model.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class JdbcBookingRepository implements BookingRepository {
    private final JdbcExecutor db;
    private static final String DETAILS = "SELECT * FROM v_booking_details ";
    public JdbcBookingRepository(JdbcExecutor db) { this.db = db; }
    @Override public List<Booking> forRequester(int requester) throws SQLException {
        return db.query(DETAILS + "WHERE RequesterID=? ORDER BY BookingDate DESC,StartTime DESC,BookingID DESC", JdbcBookingRepository::map, requester);
    }
    @Override public List<Booking> all() throws SQLException {
        return db.query(DETAILS + "ORDER BY BookingDate DESC,StartTime DESC,BookingID DESC", JdbcBookingRepository::map);
    }
    @Override public Optional<Booking> find(int booking) throws SQLException {
        return db.query(DETAILS + "WHERE BookingID=?", JdbcBookingRepository::map, booking).stream().findFirst();
    }
    @Override public List<Integer> groups(int booking) throws SQLException {
        return db.query("SELECT GroupID FROM BOOKING_GROUP WHERE BookingID=? ORDER BY GroupID", r -> r.getInt(1), booking);
    }
    @Override public List<ScheduleEntry> schedule(LocalDate from, LocalDate to) throws SQLException {
        return db.query("SELECT BookingID,BuildingCode,HallNo,CourseCode,BookingDate,StartTime,EndTime,BookingType,BookingStatus "
                + "FROM v_booking_details WHERE BookingStatus IN ('Approved','Completed') AND BookingDate BETWEEN ? AND ? "
                + "ORDER BY BookingDate,StartTime,BuildingCode,HallNo", r -> new ScheduleEntry(r.getInt(1),
                        r.getString(2) + " / " + r.getString(3), r.getString(4), r.getObject(5, LocalDate.class),
                        r.getObject(6, LocalTime.class), r.getObject(7, LocalTime.class), r.getString(8), r.getString(9)), from, to);
    }
    @Override public int create(int actor, BookingDraft d) throws SQLException {
        return db.callForId("{CALL sp_create_booking(?,?,?,?,?,?,?,?,?,?,?)}", actor, d.hallId(), d.courseId(),
                d.date(), d.start(), d.end(), d.purpose(), d.type().name(), d.attendance(), jsonGroups(d.groupIds()));
    }
    @Override public void update(int actor, int booking, BookingDraft d) throws SQLException {
        db.call("{CALL sp_update_pending_booking(?,?,?,?,?,?,?,?,?,?,?)}", actor, booking, d.hallId(), d.courseId(),
                d.date(), d.start(), d.end(), d.purpose(), d.type().name(), d.attendance(), jsonGroups(d.groupIds()));
    }
    @Override public void review(int actor, int booking, String decision, String note) throws SQLException {
        db.call("{CALL sp_review_booking(?,?,?,?)}", actor, booking, decision, note);
    }
    @Override public void cancel(int actor, int booking) throws SQLException { db.call("{CALL sp_cancel_booking(?,?)}", actor, booking); }
    @Override public void complete(int actor, int booking) throws SQLException { db.call("{CALL sp_complete_booking(?,?)}", actor, booking); }
    private static String jsonGroups(List<Integer> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
    }
    private static Booking map(ResultSet r) throws SQLException {
        return new Booking(r.getInt("BookingID"), r.getInt("RequesterID"), r.getString("RequesterName"),
                r.getInt("HallID"), r.getString("BuildingCode") + " / " + r.getString("HallNo"),
                r.getObject("CourseID", Integer.class), r.getString("CourseCode"), r.getObject("BookingDate", LocalDate.class),
                r.getObject("StartTime", LocalTime.class), r.getObject("EndTime", LocalTime.class),
                BookingType.valueOf(r.getString("BookingType")), r.getString("Purpose"), r.getInt("ExpectedAttendance"),
                BookingStatus.valueOf(r.getString("BookingStatus")), r.getObject("RequestedAt", LocalDateTime.class),
                r.getString("ReviewerName"), r.getString("Decision"), r.getObject("ReviewedAt", LocalDateTime.class), r.getString("ReviewNote"));
    }
}
