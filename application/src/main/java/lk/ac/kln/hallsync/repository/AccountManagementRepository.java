package lk.ac.kln.hallsync.repository;

import lk.ac.kln.hallsync.model.*;
import java.sql.SQLException;
import java.util.List;

public interface AccountManagementRepository {
    int registerStudent(AccountDraft details, String passwordHash) throws SQLException;
    int provision(int actor, AccountDraft details, String passwordHash, String role) throws SQLException;
    List<AccountSummary> list(int actor, Integer department) throws SQLException;
}
