package lk.ac.kln.hallsync;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/** Creates connections; the caller closes each connection after using it. */
public final class DatabaseConnectionFactory {
    private final DatabaseConfig config;

    public DatabaseConnectionFactory(DatabaseConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    public Connection open() throws SQLException {
        Properties options = new Properties();
        options.setProperty("user", config.getUsername());
        options.setProperty("password", config.getPassword());
        options.setProperty("sslMode", "REQUIRED");
        options.setProperty("characterEncoding", "UTF-8");
        options.setProperty("connectionCollation", "utf8mb4_0900_ai_ci");
        // Sri Lanka civil time, without requiring MySQL named timezone tables.
        options.setProperty("connectionTimeZone", "+05:30");
        options.setProperty("forceConnectionTimeZoneToSession", "true");
        options.setProperty("connectTimeout", "5000");
        options.setProperty("socketTimeout", "10000");
        return DriverManager.getConnection(config.getUrl(), options);
    }
}
