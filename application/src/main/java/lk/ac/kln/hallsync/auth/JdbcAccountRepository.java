package lk.ac.kln.hallsync.auth;

import lk.ac.kln.hallsync.DatabaseConnectionFactory;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/** Uses the granted login procedure, rather than direct USER_ACCOUNT table reads. */
public final class JdbcAccountRepository implements AccountRepository {
    private final DatabaseConnectionFactory connections;

    public JdbcAccountRepository(DatabaseConnectionFactory connections) {
        this.connections = Objects.requireNonNull(connections);
    }

    @Override
    public Optional<LoginRecord> findForLogin(String username) throws SQLException {
        try (Connection connection = connections.open();
             CallableStatement statement = connection.prepareCall("{CALL sp_get_login_record(?)}")) {
            statement.setString(1, username);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Optional.empty();
                }
                return Optional.of(new LoginRecord(
                        rows.getInt("AccountID"), rows.getString("Username"),
                        rows.getString("PasswordHash"), rows.getString("Role"),
                        rows.getBoolean("IsActive")));
            }
        }
    }
}
