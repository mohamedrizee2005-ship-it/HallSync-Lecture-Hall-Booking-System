package lk.ac.kln.hallsync.repository;

import lk.ac.kln.hallsync.model.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public final class JdbcLecturerApplicationRepository implements LecturerApplicationRepository {
    private final JdbcExecutor db;
    public JdbcLecturerApplicationRepository(JdbcExecutor db) { this.db=db; }
    @Override public int register(AccountDraft d,String staffId,String hash) throws SQLException {
        return db.callForId("{CALL sp_register_lecturer_application(?,?,?,?,?,?,?)}",d.departmentId(),d.fullName(),d.email(),d.username(),hash,staffId);
    }
    @Override public List<LecturerApplication> list(int actor,String status) throws SQLException {
        return db.callRows("{CALL sp_list_lecturer_applications(?,?)}",r->new LecturerApplication(
            r.getInt("ApplicationID"),r.getInt("AccountID"),r.getObject("DepartmentID",Integer.class),r.getString("DepartmentName"),
            r.getString("FullName"),r.getString("Email"),r.getString("Username"),r.getString("StaffID"),r.getString("ApplicationStatus"),r.getBoolean("IsActive"),
            r.getObject("SubmittedAt",LocalDateTime.class),r.getObject("ReviewedByID",Integer.class),r.getString("ReviewerName"),r.getObject("ReviewedAt",LocalDateTime.class),r.getString("ReviewNote")),actor,status);
    }
    @Override public void review(int actor,int application,String decision,String note,boolean identityVerified) throws SQLException {
        db.call("{CALL sp_review_lecturer_application(?,?,?,?,?)}",actor,application,decision,note,identityVerified);
    }
    @Override public boolean installed() throws SQLException {
        return db.query("SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_name IN ('sp_register_lecturer_application','sp_list_lecturer_applications','sp_review_lecturer_application')",r->r.getInt(1)).get(0)==3;
    }
}
