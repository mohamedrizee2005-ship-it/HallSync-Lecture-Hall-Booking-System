package lk.ac.kln.hallsync;

import lk.ac.kln.hallsync.auth.*;
import lk.ac.kln.hallsync.model.*;
import lk.ac.kln.hallsync.repository.*;
import lk.ac.kln.hallsync.service.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Real MySQL workflow tests. Runs only on an explicitly enabled, disposable CI schema. */
public final class FullApplicationIntegrationCheck {
    private static int passed;
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void check(String name, boolean ok) { if (!ok) { throw new AssertionError(name); } passed++; System.out.println("PASS - " + name); }
    private static void fails(String name, Class<? extends Throwable> type, Action action) throws Exception {
        try { action.run(); } catch (Throwable e) { if (type.isInstance(e)) { check(name,true); return; } throw new AssertionError(name + ": unexpected " + e.getClass().getSimpleName(),e); }
        throw new AssertionError(name + ": action unexpectedly succeeded");
    }
    private static BookingDraft event(int hall, LocalDate date, int start, int end, String purpose) {
        return new BookingDraft(hall,null,date,LocalTime.of(start,0),LocalTime.of(end,0),purpose,BookingType.Event,20,List.of());
    }
    public static void main(String[] args) throws Exception {
        if (!"1".equals(System.getenv("HALLSYNC_RUN_INTEGRATION_TESTS"))) { throw new IllegalStateException("Explicit test enablement is required."); }
        DatabaseConnectionFactory connections=new DatabaseConnectionFactory(DatabaseConfig.load(Path.of(args[0])));
        try (Connection c=connections.open()) { if (!c.getCatalog().startsWith("hallsync_connection_")) { throw new IllegalStateException("Disposable test schema required."); } }
        ApplicationServices app=new ApplicationServices(connections); JdbcExecutor db=new JdbcExecutor(connections);
        String password=System.getenv("HALLSYNC_APPLICATION_TEST_PASSWORD"); char[] secret=password.toCharArray();
        try {
            AuthenticatedUser admin=app.authentication.signIn("ci_admin",secret).orElseThrow();
            AuthenticatedUser lecturer=app.authentication.signIn("ci_lecturer",secret).orElseThrow();
            AuthenticatedUser student=app.authentication.signIn("ci_student",secret).orElseThrow();
            int dept=app.departments.add(admin,"Java Workflow Department").getDepartmentId();
            AccountDraft secondAdmin=new AccountDraft(dept,"Second administrator","java_admin_b@example.invalid","java_admin_b");
            int aid=app.accounts.provision(admin,secondAdmin,secret,"AdminStaff");
            AuthenticatedUser adminB=app.authentication.signIn("java_admin_b",secret).orElseThrow();
            check("Admin provisioning creates a usable second administrator",adminB.getAccountId()==aid);
            AccountDraft newStudent=new AccountDraft(dept,"புதிய மாணவர்","java_student@example.invalid","java_student");
            int sid=app.accounts.registerStudent(newStudent,secret);
            AuthenticatedUser registered=app.authentication.signIn("java_student",secret).orElseThrow();
            check("Public registration always creates Student",sid==registered.getAccountId() && "Student".equals(registered.getRole()));
            fails("Student cannot provision a privileged account",SecurityException.class,() -> app.accounts.provision(student,secondAdmin,secret,"Lecturer"));
            fails("Lecturer cannot provision accounts",SecurityException.class,() -> app.accounts.provision(lecturer,secondAdmin,secret,"Student"));
            fails("Account input rejects malformed email",IllegalArgumentException.class,() -> app.accounts.registerStudent(new AccountDraft(dept,"Name","invalid","new_user"),secret));
            fails("Account input rejects weak password",IllegalArgumentException.class,() -> app.accounts.registerStudent(newStudent,"short".toCharArray()));
            fails("Duplicate username/email fails without a second account",SQLException.class,() -> app.accounts.registerStudent(newStudent,secret));
            List<AccountSummary> filtered=app.accounts.list(admin,dept);
            check("Account list filters by department",filtered.size()==2 && filtered.stream().allMatch(a -> Objects.equals(a.departmentId(),dept)));
            check("Account list contains safe metadata",filtered.stream().anyMatch(a -> a.username().equals("java_student") && a.fullName().equals("புதிய மாணவர்")));
            fails("Student cannot list private account metadata",SecurityException.class,() -> app.accounts.list(student,null));
            fails("Database independently rejects lecturer account reads",SQLException.class,() -> new JdbcAccountManagementRepository(db).list(lecturer.getAccountId(),null));
            int building=app.catalog.addBuilding(admin,"JAVA-B","Java teaching building","Synthetic test location");
            int facility=app.catalog.addFacility(admin,"Java projector","Synthetic test equipment");
            int course=app.catalog.addCourse(admin,dept,"JAVA101","Java workflow course");
            int group=app.catalog.addGroup(admin,dept,"Java group",2026,1);
            int hall=app.catalog.addHall(admin,building,"J1","1",80), hallB=app.catalog.addHall(admin,building,"J2","1",60);
            check("Building mapping preserves code and location",app.catalog.buildings().stream().anyMatch(b -> b.id()==building && b.location().equals("Synthetic test location")));
            check("Course mapping preserves department",app.catalog.courses().stream().anyMatch(c -> c.id()==course && c.departmentId()==dept));
            check("Student group mapping preserves intake and study year",app.catalog.groups().stream().anyMatch(g -> g.id()==group && g.intakeYear()==2026 && g.studyYear()==1));
            check("Hall mapping preserves capacity and operational status",app.catalog.halls().stream().anyMatch(h -> h.id()==hall && h.capacity()==80 && h.status().equals("Active")));
            fails("Student cannot add reference data",SecurityException.class,() -> app.catalog.addBuilding(student,"BAD","Bad",null));
            fails("Blank building code fails validation",IllegalArgumentException.class,() -> app.catalog.addBuilding(admin," ","Name",null));
            fails("Oversized optional description fails validation",IllegalArgumentException.class,() -> app.catalog.addFacility(admin,"Name","x".repeat(256)));
            fails("Invalid group intake fails validation",IllegalArgumentException.class,() -> app.catalog.addGroup(admin,dept,"Bad",1899,1));
            fails("Zero hall capacity fails validation",IllegalArgumentException.class,() -> app.catalog.addHall(admin,building,"Bad",null,0));
            app.catalog.setEquipment(admin,hall,facility,3,2);
            check("Hall equipment mapping preserves total and working quantity",app.catalog.equipment().stream().anyMatch(f -> f.hallId()==hall && f.facilityId()==facility && f.quantity()==3 && f.workingQuantity()==2));
            app.catalog.setEquipment(admin,hall,facility,3,0);
            check("Zero working quantity is allowed",app.catalog.equipment().stream().anyMatch(f -> f.hallId()==hall && f.facilityId()==facility && f.workingQuantity()==0));
            fails("Working quantity cannot exceed total",IllegalArgumentException.class,() -> app.catalog.setEquipment(admin,hall,facility,2,3));
            fails("Total quantity must be positive",IllegalArgumentException.class,() -> app.catalog.setEquipment(admin,hall,facility,0,0));
            LocalDate date=LocalDate.now(BookingService.ZONE).plusDays(30);
            BookingDraft teaching=new BookingDraft(hall,course,date,LocalTime.of(9,0),LocalTime.of(10,0),"  Java lecture  ",BookingType.Lecture,40,List.of(group));
            fails("Students cannot request halls",SecurityException.class,() -> app.bookings.create(student,teaching));
            fails("Guests cannot request halls",SecurityException.class,() -> app.bookings.create(null,teaching));
            fails("Start equal to end is rejected",IllegalArgumentException.class,() -> app.bookings.create(lecturer,event(hall,date,10,10,"Bad")));
            fails("Past booking start is rejected",IllegalArgumentException.class,() -> app.bookings.create(lecturer,event(hall,date.minusDays(40),9,10,"Bad")));
            fails("Lecture requires a course",IllegalArgumentException.class,() -> app.bookings.create(lecturer,new BookingDraft(hall,null,date,LocalTime.of(9,0),LocalTime.of(10,0),"Lecture",BookingType.Lecture,20,List.of(group))));
            fails("Tutorial requires a student group",IllegalArgumentException.class,() -> app.bookings.create(lecturer,new BookingDraft(hall,course,date,LocalTime.of(9,0),LocalTime.of(10,0),"Tutorial",BookingType.Tutorial,20,List.of())));
            fails("Duplicate group IDs fail validation",IllegalArgumentException.class,() -> app.bookings.create(lecturer,new BookingDraft(hall,course,date,LocalTime.of(9,0),LocalTime.of(10,0),"Lecture",BookingType.Lecture,20,List.of(group,group))));
            fails("Attendance above hall capacity is rejected by database",SQLException.class,() -> app.bookings.create(lecturer,new BookingDraft(hall,null,date,LocalTime.of(8,0),LocalTime.of(9,0),"Too large",BookingType.Event,81,List.of())));
            int booking=app.bookings.create(lecturer,teaching);
            Booking own=app.bookings.mine(lecturer).stream().filter(b -> b.id()==booking).findFirst().orElseThrow();
            check("Create maps requester from authenticated identity",own.requesterId()==lecturer.getAccountId() && own.status()==BookingStatus.Pending && own.purpose().equals("Java lecture"));
            check("Booking group membership is stored",app.bookings.groupsForEdit(lecturer,booking).equals(List.of(group)));
            check("Pending requests do not reserve the hall",app.catalog.available(student,date,LocalTime.of(9,0),LocalTime.of(10,0),20).stream().anyMatch(h -> h.id()==hall));
            app.bookings.update(lecturer,booking,new BookingDraft(hall,course,date,LocalTime.of(9,0),LocalTime.of(10,0),"Updated lecture",BookingType.Lecture,35,List.of(group)));
            check("Owner can edit Pending details",app.bookings.mine(lecturer).stream().anyMatch(b -> b.id()==booking && b.purpose().equals("Updated lecture") && b.attendance()==35));
            fails("Administrator cannot edit another requester's booking",SecurityException.class,() -> app.bookings.update(admin,booking,teaching));
            fails("Student cannot cancel another requester's booking",SecurityException.class,() -> app.bookings.cancel(student,booking));
            int self=app.bookings.create(admin,event(hall,date,11,12,"Administrator's own event"));
            fails("Administrator cannot approve own request",SecurityException.class,() -> app.bookings.review(admin,self,"Approved",null));
            fails("Administrator cannot reject own request",SecurityException.class,() -> app.bookings.review(admin,self,"Rejected",null));
            app.bookings.review(admin,booking,"Approved","Approved for teaching");
            check("Approval persists reviewer metadata",app.bookings.all(admin).stream().anyMatch(b -> b.id()==booking && b.status()==BookingStatus.Approved && b.reviewedAt()!=null && b.reviewNote().equals("Approved for teaching")));
            check("Approved session appears in public timetable",app.bookings.schedule(student,date,date).stream().anyMatch(s -> s.bookingId()==booking && s.courseCode().equals("JAVA101")));
            check("Approved interval disappears from availability",app.catalog.available(student,date,LocalTime.of(9,0),LocalTime.of(10,0),20).stream().noneMatch(h -> h.id()==hall));
            check("Adjacent half-open interval remains available",app.catalog.available(student,date,LocalTime.of(10,0),LocalTime.of(11,0),20).stream().anyMatch(h -> h.id()==hall));
            fails("Approved booking cannot be edited",IllegalArgumentException.class,() -> app.bookings.update(lecturer,booking,teaching));
            fails("Approved booking cannot be reviewed again",IllegalArgumentException.class,() -> app.bookings.review(admin,booking,"Rejected",null));
            fails("Future booking cannot be marked Completed",IllegalArgumentException.class,() -> app.bookings.complete(admin,booking));
            fails("Conflicting maintenance must not displace approval",SQLException.class,() -> app.blocks.create(admin,hall,date.atTime(9,30),date.atTime(10,30),"Maintenance"));
            fails("Cannot deactivate a hall with future approval",SQLException.class,() -> app.catalog.updateHall(admin,hall,80,"Inactive"));
            fails("Cannot reduce capacity below approved attendance",SQLException.class,() -> app.catalog.updateHall(admin,hall,10,"Active"));
            app.bookings.cancel(lecturer,booking);
            check("Cancellation retains approval and teaching group history",app.bookings.mine(lecturer).stream().anyMatch(b -> b.id()==booking && b.status()==BookingStatus.Cancelled && "Approved".equals(b.decision())) && app.bookings.groupsForEdit(lecturer,booking).contains(group));
            check("Cancellation removes the session from public timetable",app.bookings.schedule(student,date,date).stream().noneMatch(s -> s.bookingId()==booking));
            fails("Cancelled booking cannot return to Pending",IllegalArgumentException.class,() -> app.bookings.update(lecturer,booking,teaching));
            int rejected=app.bookings.create(lecturer,event(hall,date,13,14,"Reject this event")); app.bookings.review(admin,rejected,"Rejected","Not suitable");
            check("Rejected request remains in history",app.bookings.mine(lecturer).stream().anyMatch(b -> b.id()==rejected && b.status()==BookingStatus.Rejected));
            fails("Rejected request cannot be cancelled",IllegalArgumentException.class,() -> app.bookings.cancel(lecturer,rejected));
            app.bookings.cancel(admin,self);
            int block=app.blocks.create(admin,hall,date.atTime(9,0),date.atTime(10,0),"  Planned maintenance  ");
            check("Administrator can list maintenance blocks",app.blocks.list(admin).stream().anyMatch(b -> b.id()==block && b.reason().equals("Planned maintenance") && b.start().equals(date.atTime(9,0))));
            check("Active hall block removes hall from availability",app.catalog.available(student,date,LocalTime.of(9,0),LocalTime.of(10,0),20).stream().noneMatch(h -> h.id()==hall));
            fails("Active block rejects a new request",SQLException.class,() -> app.bookings.create(lecturer,teaching));
            fails("Student cannot read private block records",SecurityException.class,() -> app.blocks.list(student));
            fails("Database independently rejects student block reads",SQLException.class,() -> new JdbcHallBlockRepository(db).list(student.getAccountId()));
            fails("Invalid block interval fails validation",IllegalArgumentException.class,() -> app.blocks.create(admin,hall,date.atTime(10,0),date.atTime(9,0),"Bad"));
            app.blocks.cancel(admin,block);
            check("Cancelled block is retained",app.blocks.list(admin).stream().anyMatch(b -> b.id()==block && b.status().equals("Cancelled")));
            check("Cancelling block restores availability",app.catalog.available(student,date,LocalTime.of(9,0),LocalTime.of(10,0),20).stream().anyMatch(h -> h.id()==hall));
            fails("Cancelled block cannot be cancelled again",SQLException.class,() -> app.blocks.cancel(admin,block));
            app.catalog.updateHall(admin,hallB,60,"Inactive");
            check("Inactive hall is absent from availability",app.catalog.available(student,date,LocalTime.of(9,0),LocalTime.of(10,0),20).stream().noneMatch(h -> h.id()==hallB));
            app.catalog.updateHall(admin,hallB,60,"Active");
            int raceA=app.bookings.create(lecturer,event(hall,date,15,16,"Concurrent request A"));
            int raceB=app.bookings.create(lecturer,event(hall,date,15,16,"Concurrent request B"));
            ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch ready=new CountDownLatch(1);
            try {
                Future<Boolean> a=pool.submit(() -> approve(app,admin,raceA,ready)); Future<Boolean> b=pool.submit(() -> approve(app,adminB,raceB,ready)); ready.countDown();
                check("Two simultaneous JDBC approvals have exactly one winner",a.get(30,TimeUnit.SECONDS) ^ b.get(30,TimeUnit.SECONDS));
            } finally { pool.shutdownNow(); }
            check("Concurrent approval persists exactly one Approved row",app.bookings.all(admin).stream().filter(b -> (b.id()==raceA || b.id()==raceB) && b.status()==BookingStatus.Approved).count()==1);
            int raceApproved=app.bookings.all(admin).stream().filter(b -> (b.id()==raceA || b.id()==raceB) && b.status()==BookingStatus.Approved).findFirst().orElseThrow().id();
            app.bookings.cancel(admin,raceApproved);
            int past=db.query("SELECT BookingID FROM v_booking_details WHERE Purpose='CI ended approved session'",r -> r.getInt(1)).get(0);
            app.bookings.complete(admin,past);
            check("Ended Approved booking can become Completed",app.bookings.all(admin).stream().anyMatch(b -> b.id()==past && b.status()==BookingStatus.Completed));
            fails("Completed booking is terminal",IllegalArgumentException.class,() -> app.bookings.cancel(admin,past));
            for (ReportKind kind : ReportKind.values()) { ReportTable report=app.reports.load(admin,kind); check("Report loads: " + kind.name(),!report.columns().isEmpty() && !report.rows().isEmpty()); }
            fails("Lecturer cannot read administrative reports",SecurityException.class,() -> app.reports.load(lecturer,ReportKind.BookingDetails));
            fails("Student cannot list all booking details",SecurityException.class,() -> app.bookings.all(student));
            fails("Reversed timetable date range fails validation",IllegalArgumentException.class,() -> app.bookings.schedule(student,date,date.minusDays(1)));
            Path csv=Files.createTempFile("hallsync-report-",".csv");
            try {
                CsvExporter.write(csv,new ReportTable(List.of("Text"),List.of(List.of("=1+1"),List.of("comma,quote\"line\nnext"),List.of("தமிழ்"))));
                String content=Files.readString(csv); check("CSV preserves Unicode, escapes commas/quotes and neutralises formulas",content.startsWith("\uFEFF\"Text\"") && content.contains("\"'=1+1\"") && content.contains("quote\"\"line") && content.contains("தமிழ்"));
            } finally { Files.deleteIfExists(csv); }
            System.out.println("FULL_APPLICATION_CHECKS_PASSED=" + passed);
        } finally { Arrays.fill(secret,'\0'); }
    }
    private static boolean approve(ApplicationServices app, AuthenticatedUser admin, int id, CountDownLatch start) throws Exception {
        start.await();
        try { app.bookings.review(admin,id,"Approved","Concurrency check"); return true; }
        catch (SQLException e) { if ("45000".equals(e.getSQLState())) { return false; } throw e; }
    }
}
