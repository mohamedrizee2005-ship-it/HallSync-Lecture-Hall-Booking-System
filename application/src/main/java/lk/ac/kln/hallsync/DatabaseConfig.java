package lk.ac.kln.hallsync;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;
import java.util.Set;
import java.util.Arrays;

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
        char[] secret = password.toCharArray();
        try { return fromValues(url, username, secret); }
        finally { Arrays.fill(secret, '\0'); }
    }

    /** Validates explicitly entered settings; no environment override is applied here. */
    public static DatabaseConfig fromValues(String url, String username, char[] password) {
        if (url == null || !url.trim().startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("db.url must be a MySQL JDBC URL.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("The MySQL connection username is required.");
        }
        String secret = password == null ? "" : new String(password);
        if (secret.isBlank() || secret.startsWith("REPLACE_WITH_")) {
            throw new IllegalArgumentException("Set your private MySQL password locally.");
        }
        return new DatabaseConfig(url.trim(), username.trim(), secret);
    }

    /** First-run save only. Existing configuration is never overwritten. */
    public void saveNew(Path path) throws IOException {
        Properties settings = new Properties();
        settings.setProperty("db.url", url);
        settings.setProperty("db.user", username);
        settings.setProperty("db.password", password);
        FileAttribute<?>[] attributes = Files.getFileStore(path.toAbsolutePath().getParent())
                .supportsFileAttributeView("posix")
                ? new FileAttribute<?>[]{PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))}
                : new FileAttribute<?>[0];
        try (var channel = Files.newByteChannel(path, Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE), attributes);
             Writer writer = Channels.newWriter(channel, StandardCharsets.UTF_8)) {
            // Properties.store escapes backslashes, quotes and leading spaces correctly.
            settings.store(writer, "Private HallSync connection settings. Never upload this file.");
        }
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
