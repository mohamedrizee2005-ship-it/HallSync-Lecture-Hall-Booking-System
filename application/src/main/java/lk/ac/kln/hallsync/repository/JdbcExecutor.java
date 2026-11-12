package lk.ac.kln.hallsync.repository;

import lk.ac.kln.hallsync.DatabaseConnectionFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Owns JDBC resource cleanup. SQL strings are constants in repositories, never user input. */
public final class JdbcExecutor {
    @FunctionalInterface public interface RowMapper<T> { T map(ResultSet row) throws SQLException; }
    private final DatabaseConnectionFactory connections;
    public JdbcExecutor(DatabaseConnectionFactory connections) { this.connections = connections; }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters) throws SQLException {
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, parameters);
            try (ResultSet rows = s.executeQuery()) { return read(rows, mapper); }
        }
    }

    public <T> List<T> callRows(String sql, RowMapper<T> mapper, Object... parameters) throws SQLException {
        try (Connection c = connections.open(); CallableStatement s = c.prepareCall(sql)) {
            bind(s, parameters);
            try (ResultSet rows = s.executeQuery()) { return read(rows, mapper); }
        }
    }

    /** Every write procedure owns its transaction; the new connection is in auto-commit mode. */
    public void call(String sql, Object... parameters) throws SQLException {
        try (Connection c = connections.open(); CallableStatement s = c.prepareCall(sql)) {
            bind(s, parameters); s.execute();
        }
    }

    /** These procedures have one final OUT INTEGER parameter. */
    public int callForId(String sql, Object... parameters) throws SQLException {
        try (Connection c = connections.open(); CallableStatement s = c.prepareCall(sql)) {
            bind(s, parameters);
            s.registerOutParameter(parameters.length + 1, Types.INTEGER);
            s.execute(); return s.getInt(parameters.length + 1);
        }
    }

    private static void bind(PreparedStatement s, Object[] values) throws SQLException {
        for (int i = 0; i < values.length; i++) { s.setObject(i + 1, values[i]); }
    }
    private static <T> List<T> read(ResultSet rows, RowMapper<T> mapper) throws SQLException {
        List<T> result = new ArrayList<>();
        while (rows.next()) { result.add(mapper.map(rows)); }
        return List.copyOf(result);
    }
}
