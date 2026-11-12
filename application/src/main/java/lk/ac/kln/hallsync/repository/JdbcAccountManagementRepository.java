package lk.ac.kln.hallsync.repository;

import lk.ac.kln.hallsync.model.*;
import java.sql.SQLException;
import java.util.List;

public final class JdbcAccountManagementRepository implements AccountManagementRepository {
    private final JdbcExecutor db;
    public JdbcAccountManagementRepository(JdbcExecutor db) { this.db = db; }
    @Override public int registerStudent(AccountDraft d, String hash) throws SQLException {
        return db.callForId("{CALL sp_register_student(?,?,?,?,?,?)}", d.departmentId(), d.fullName(), d.email(), d.username(), hash);
    }
    @Override public int provision(int actor, AccountDraft d, String hash, String role) throws SQLException {
        return db.callForId("{CALL sp_provision_account(?,?,?,?,?,?,?,?)}", actor, d.departmentId(), d.fullName(), d.email(), d.username(), hash, role);
    }
    @Override public void manage(int actor,int account,String role,boolean active) throws SQLException { db.call("{CALL sp_manage_account(?,?,?,?)}",actor,account,role,active); }
    @Override public List<AccountSummary> list(int actor, Integer department) throws SQLException {
        return db.callRows("{CALL sp_list_accounts(?,?)}", r -> new AccountSummary(r.getInt("AccountID"),
                r.getObject("DepartmentID", Integer.class), r.getString("DepartmentName"), r.getString("FullName"),
                r.getString("Email"), r.getString("Username"), r.getString("Role"), r.getBoolean("IsActive")), actor, department);
    }
}
