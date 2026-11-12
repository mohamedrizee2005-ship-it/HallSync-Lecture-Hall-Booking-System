package lk.ac.kln.hallsync.auth;

import java.sql.SQLException;
import java.util.Optional;

/** Access to the private login record needed for password verification. */
public interface AccountRepository {
    Optional<LoginRecord> findForLogin(String username) throws SQLException;
}
