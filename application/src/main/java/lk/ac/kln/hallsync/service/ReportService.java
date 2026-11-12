package lk.ac.kln.hallsync.service;

import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.*;
import lk.ac.kln.hallsync.repository.ReportRepository;
import java.sql.SQLException;

public final class ReportService {
    private final ReportRepository repository;
    public ReportService(ReportRepository repository) { this.repository = repository; }
    public ReportTable load(AuthenticatedUser user,ReportKind kind,java.time.LocalDate from,java.time.LocalDate to) throws SQLException {
        AccessRules.administrator(user);
        if(kind==null)throw new IllegalArgumentException("Choose a report.");
        if(from==null&&to==null)return repository.load(kind);
        if(from==null||to==null||to.isBefore(from)||java.time.temporal.ChronoUnit.DAYS.between(from,to)>366)throw new IllegalArgumentException("Choose a date range of at most 367 days.");
        return repository.load(kind,from,to);
    }
    public ReportTable load(AuthenticatedUser user, ReportKind kind) throws SQLException {
        AccessRules.administrator(user);
        if (kind == null) { throw new IllegalArgumentException("Choose a report."); }
        return repository.load(kind);
    }
}
