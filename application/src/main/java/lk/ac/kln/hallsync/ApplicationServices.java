package lk.ac.kln.hallsync;

import lk.ac.kln.hallsync.auth.*;
import lk.ac.kln.hallsync.repository.*;
import lk.ac.kln.hallsync.service.*;
import java.time.Clock;

/** Composition root. Each service depends on an interface rather than an interface screen. */
public final class ApplicationServices {
    public final AuthenticationService authentication;
    public final AccountService accounts;
    public final AccountApplicationService accountApplications;
    public final DepartmentService departments;
    public final CatalogService catalog;
    public final BookingService bookings;
    public final HallBlockService blocks;
    public final ReportService reports;
    public final RoomIssueService issues;
    public ApplicationServices(DatabaseConnectionFactory connections) {
        JdbcExecutor db = new JdbcExecutor(connections); Clock clock = Clock.system(BookingService.ZONE);
        PasswordHasher passwords = new Argon2PasswordHasher();
        authentication = new AuthenticationService(new JdbcAccountRepository(connections),passwords);
        accounts = new AccountService(new JdbcAccountManagementRepository(db),passwords);
        accountApplications = new AccountApplicationService(new JdbcAccountApplicationRepository(db),passwords);
        departments = new DepartmentService(new JdbcDepartmentRepository(connections));
        catalog = new CatalogService(new JdbcCatalogRepository(db),clock);
        bookings = new BookingService(new JdbcBookingRepository(db),clock);
        blocks = new HallBlockService(new JdbcHallBlockRepository(db),clock);
        issues = new RoomIssueService(new JdbcRoomIssueRepository(db));
        reports = new ReportService(new JdbcReportRepository(connections));
    }
}
