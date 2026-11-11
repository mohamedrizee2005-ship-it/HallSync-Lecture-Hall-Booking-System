package lk.ac.kln.hallsync;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Holds connection settings without putting credentials in Java source. */
public final class DatabaseConfig {
    private final String url;
    private final String username;
    private final String password;

    private DatabaseConfig(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public static DatabaseConfig load(Path path) throws IOException {
        Properties settings = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            settings.load(reader);
        }
        String url = required(settings, "db.url").trim();
        String username = required(settings, "db.user").trim();
        // CI can supply the password privately without writing it to a file.
        String password = System.getenv("HALLSYNC_DB_PASSWORD");
        if (password == null) {
            password = required(settings, "db.password");
        }
        if (!url.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("db.url must be a MySQL JDBC URL.");
        }
        if (password.isBlank() || password.startsWith("REPLACE_WITH_")) {
            throw new IllegalArgumentException("Set your private MySQL password locally.");
        }
        return new DatabaseConfig(url, username, password);
    }

    private static String required(Properties settings, String key) {
        String value = settings.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing configuration setting: " + key);
        }
        return value;
    }

    public String getUrl() { return url; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
