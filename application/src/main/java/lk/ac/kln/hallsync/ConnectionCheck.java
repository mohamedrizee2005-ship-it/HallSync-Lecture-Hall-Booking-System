package lk.ac.kln.hallsync;

import lk.ac.kln.hallsync.model.Department;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** First application lesson: connect and read; no booking changes. */
public final class ConnectionCheck {
    private ConnectionCheck() { }

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: java -jar hallsync-connection-check.jar [config-file]");
            System.exit(1);
        }
        Path configPath = Path.of(args.length == 1 ? args[0] : "db.local.properties");
        try {
            DatabaseConfig config = DatabaseConfig.load(configPath);
            DatabaseConnectionFactory factory = new DatabaseConnectionFactory(config);
            try (Connection connection = factory.open()) {
                printSession(connection);
                checkEncryption(connection);
                printDepartments(connection);
            }
            System.out.println("Connection check succeeded.");
        } catch (IOException | IllegalArgumentException error) {
            System.err.println("Configuration error: check the config-file path and required settings.");
            System.err.println("See application/README.md. Keep passwords private.");
            System.exit(1);
        } catch (SQLException error) {
            // Codes are useful for troubleshooting without exposing credentials.
            System.err.printf("MySQL connection/read failed (SQLState=%s, code=%d).%n",
                    error.getSQLState(), error.getErrorCode());
            System.err.println("See application/README.md for connection troubleshooting.");
            System.exit(1);
        }
    }

    private static void printSession(Connection connection) throws SQLException {
        String sql = "SELECT DATABASE(), VERSION(), CURRENT_USER(), CURRENT_ROLE(), @@session.time_zone";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            rows.next();
            System.out.println("Database: " + rows.getString(1));
            System.out.println("MySQL: " + rows.getString(2));
            System.out.println("Login: " + rows.getString(3));
            System.out.println("Active role: " + rows.getString(4));
            System.out.println("Session time zone: " + rows.getString(5));
        }
    }

    private static void checkEncryption(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SHOW SESSION STATUS LIKE 'Ssl_cipher'");
             ResultSet rows = statement.executeQuery()) {
            if (!rows.next() || rows.getString(2).isBlank()) {
                throw new SQLException("An encrypted connection is required.", "08001");
            }
            System.out.println("TLS: enabled");
        }
    }

    private static void printDepartments(Connection connection) throws SQLException {
        String sql = "SELECT DepartmentID, DepartmentName FROM DEPARTMENT ORDER BY DepartmentID LIMIT ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, 100);
            try (ResultSet rows = statement.executeQuery()) {
                System.out.println("Departments (up to 100):");
                while (rows.next()) {
                    Department department = new Department(
                            rows.getInt("DepartmentID"),
                            rows.getString("DepartmentName"));
                    System.out.printf("%d - %s%n",
                            department.getDepartmentId(), department.getDepartmentName());
                }
            }
        }
    }
}
