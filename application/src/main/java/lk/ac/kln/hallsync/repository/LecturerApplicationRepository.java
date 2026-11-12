package lk.ac.kln.hallsync.repository;

import lk.ac.kln.hallsync.model.*;
import java.sql.SQLException;
import java.util.List;

public interface LecturerApplicationRepository {
    int register(AccountDraft details,String staffId,String passwordHash) throws SQLException;
    List<LecturerApplication> list(int actor,String status) throws SQLException;
    void review(int actor,int application,String decision,String note,boolean identityVerified) throws SQLException;
    boolean installed() throws SQLException;
}
