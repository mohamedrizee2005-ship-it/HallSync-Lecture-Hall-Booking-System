package lk.ac.kln.hallsync.repository;
import lk.ac.kln.hallsync.model.*;
import java.sql.SQLException;
import java.time.LocalDate;
public interface ReportRepository {
 ReportTable load(ReportKind kind) throws SQLException;
 default ReportTable load(ReportKind kind,LocalDate from,LocalDate to)throws SQLException{throw new UnsupportedOperationException("Date filters unavailable.");}
}
