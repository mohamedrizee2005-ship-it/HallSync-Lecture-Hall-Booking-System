package lk.ac.kln.hallsync.repository;

import lk.ac.kln.hallsync.DatabaseConnectionFactory;
import lk.ac.kln.hallsync.model.Department;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** JDBC implementation; all user-supplied values are bound as parameters. */
public final class JdbcDepartmentRepository implements DepartmentRepository {
    private static final String COLUMNS = "DepartmentID, DepartmentName";
    private final DatabaseConnectionFactory connections;

    public JdbcDepartmentRepository(DatabaseConnectionFactory connections) {
        this.connections = Objects.requireNonNull(connections);
    }

    @Override
    public List<Department> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM DEPARTMENT ORDER BY DepartmentID";
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            return readRows(rows);
        }
    }

    @Override
    public Optional<Department> findById(int departmentId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM DEPARTMENT WHERE DepartmentID = ?";
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, departmentId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(readRow(rows)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Department> findByName(String text) throws SQLException {
        String sql = "SELECT " + COLUMNS
                + " FROM DEPARTMENT WHERE DepartmentName LIKE ? ESCAPE '!' ORDER BY DepartmentID";
        // Treat %, _ and ! as literal characters in the user's search text.
        String escaped = text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "%" + escaped + "%");
            try (ResultSet rows = statement.executeQuery()) {
                return readRows(rows);
            }
        }
    }

    @Override
    public Department add(int authenticatedActorId, String name) throws SQLException {
        // The existing procedure rechecks the actor's active administrator role.
        try (Connection connection = connections.open();
             CallableStatement statement = connection.prepareCall("{CALL sp_add_department(?, ?, ?)}")) {
            statement.setInt(1, authenticatedActorId);
            statement.setString(2, name);
            statement.registerOutParameter(3, Types.INTEGER);
            statement.execute();
            return new Department(statement.getInt(3), name);
        }
    }

    private static List<Department> readRows(ResultSet rows) throws SQLException {
        List<Department> departments = new ArrayList<>();
        while (rows.next()) {
            departments.add(readRow(rows));
        }
        return departments;
    }

    private static Department readRow(ResultSet row) throws SQLException {
        return new Department(row.getInt("DepartmentID"), row.getString("DepartmentName"));
    }
}
