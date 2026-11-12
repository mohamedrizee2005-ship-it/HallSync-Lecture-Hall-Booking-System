package lk.ac.kln.hallsync;

import lk.ac.kln.hallsync.auth.AdminPasswordTool;
import lk.ac.kln.hallsync.auth.Argon2PasswordHasher;
import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.auth.AuthenticationService;
import lk.ac.kln.hallsync.auth.JdbcAccountRepository;
import lk.ac.kln.hallsync.model.Department;
import lk.ac.kln.hallsync.repository.JdbcDepartmentRepository;
import lk.ac.kln.hallsync.service.DepartmentService;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/** Explicitly invoked CI integration checks; never targets the real hallsync_db. */
public final class DepartmentIntegrationCheck {
    private static int passed;

    public static void main(String[] args) throws Exception {
        if (!"1".equals(System.getenv("HALLSYNC_RUN_INTEGRATION_TESTS"))) {
            throw new IllegalStateException("Use the isolated CI test runner.");
        }
        DatabaseConfig config = DatabaseConfig.load(Path.of(args[0]));
        DatabaseConnectionFactory connections = new DatabaseConnectionFactory(config);
        try (Connection connection = connections.open()) {
            if (!connection.getCatalog().startsWith("hallsync_connection_")) {
                throw new IllegalStateException("An isolated connection-test schema is required.");
            }
        }
        char[] password = System.getenv("HALLSYNC_APPLICATION_TEST_PASSWORD").toCharArray();
        try {
            run(connections, password);
        } finally {
            Arrays.fill(password, '\0');
        }
        System.out.println("TEST_JAVA_VERSION=" + System.getProperty("java.version"));
        System.out.println("DEPARTMENT_CHECKS_PASSED=" + passed);
    }

    private static void run(DatabaseConnectionFactory connections, char[] password) throws Exception {
        configurationChecks();
        JdbcDepartmentRepository repository = new JdbcDepartmentRepository(connections);
        DepartmentService departments = new DepartmentService(repository);
        Argon2PasswordHasher hashes = new Argon2PasswordHasher();
        AuthenticationService authentication = new AuthenticationService(
                new JdbcAccountRepository(connections), hashes);

        AuthenticatedUser admin = authentication.signIn("ci_admin", password).orElseThrow();
        AuthenticatedUser student = authentication.signIn("ci_student", password).orElseThrow();
        AuthenticatedUser lecturer = authentication.signIn("ci_lecturer", password).orElseThrow();
        check("Active administrator signs in using a Python-generated Argon2id hash", admin.isAdministrator());
        check("Student identity retains its real role", "Student".equals(student.getRole()));
        check("Lecturer identity retains its real role", "Lecturer".equals(lecturer.getRole()));
        check("Incorrect application password is rejected",
                authentication.signIn("ci_admin", "incorrect".toCharArray()).isEmpty());
        check("Unknown username is rejected", authentication.signIn("unknown_user", password).isEmpty());
        check("SQL-like username does not bypass authentication",
                authentication.signIn("' OR 1=1 --", password).isEmpty());
        check("Inactive administrator cannot sign in", authentication.signIn("ci_inactive", password).isEmpty());
        check("Malformed stored hash fails closed", authentication.signIn("ci_bad_hash", password).isEmpty());
        check("Blank username is rejected", authentication.signIn("  ", password).isEmpty());
        check("Empty password is rejected", authentication.signIn("ci_admin", new char[0]).isEmpty());
        check("Oversized login password is rejected", authentication.signIn("ci_admin", "x".repeat(129).toCharArray()).isEmpty());

        int initial = departments.list().size();
        check("Existing departments are mapped into objects", initial >= 1);
        check("Find by ID returns the stored row", "Industrial Management".equals(
                departments.findById(1).orElseThrow().getDepartmentName()));
        check("A missing positive ID returns empty", departments.findById(Integer.MAX_VALUE).isEmpty());
        expect("Zero ID is rejected", IllegalArgumentException.class, () -> departments.findById(0));
        expect("Negative ID is rejected", IllegalArgumentException.class, () -> departments.findById(-1));
        check("Empty search clears the filter", departments.filterByName("   ").size() == initial);
        expect("Oversized filter is rejected", IllegalArgumentException.class,
                () -> departments.filterByName("x".repeat(101)));

        expect("Guest cannot add a department", SecurityException.class,
                () -> departments.add(null, "Guest attempt"));
        expect("Student cannot add a department", SecurityException.class,
                () -> departments.add(student, "Student attempt"));
        expect("Lecturer cannot add a department", SecurityException.class,
                () -> departments.add(lecturer, "Lecturer attempt"));
        check("Denied additions leave the catalogue unchanged", departments.list().size() == initial);
        try {
            repository.add(student.getAccountId(), "Procedure bypass attempt");
            throw new AssertionError("The database must reject a non-admin actor");
        } catch (SQLException error) {
            if (!"45000".equals(error.getSQLState())) {
                throw new AssertionError("Unexpected procedure error: SQLState=" + error.getSQLState()
                        + ", code=" + error.getErrorCode() + ", message=" + error.getMessage(), error);
            }
            check("Stored procedure independently rejects a non-admin actor", true);
        }

        expect("Null department name is rejected", IllegalArgumentException.class,
                () -> departments.add(admin, null));
        expect("Blank department name is rejected", IllegalArgumentException.class,
                () -> departments.add(admin, "   "));
        expect("Names over 100 characters are rejected", IllegalArgumentException.class,
                () -> departments.add(admin, "x".repeat(101)));
        check("Invalid names do not insert rows", departments.list().size() == initial);

        Department added = departments.add(admin, "   Applied Science   ");
        check("Administrator creates a department with a returned ID", added.getDepartmentId() > 0);
        check("Name whitespace is normalized", "Applied Science".equals(added.getDepartmentName()));
        check("Created department persists in MySQL", departments.findById(added.getDepartmentId())
                .orElseThrow().getDepartmentName().equals(added.getDepartmentName()));
        check("Filter uses the database case-insensitive collation", departments.filterByName("applied science")
                .stream().anyMatch(d -> d.getDepartmentId() == added.getDepartmentId()));

        Department symbols = departments.add(admin, "Resource 100%_Ready!");
        check("Percent search is literal", departments.filterByName("%_Ready!").stream()
                .anyMatch(d -> d.getDepartmentId() == symbols.getDepartmentId()));
        check("Underscore does not become a single-character wildcard", departments.filterByName("Applied_Science").isEmpty());
        check("Percent does not become a multi-character wildcard", departments.filterByName("Applied%Science").isEmpty());
        check("Escape character can be searched literally", departments.filterByName("!").stream()
                .anyMatch(d -> d.getDepartmentId() == symbols.getDepartmentId()));
        check("SQL-like filter remains data", departments.filterByName("' OR 1=1 --").isEmpty());

        String injection = "Mathematics'); DROP TABLE DEPARTMENT; --";
        Department literal = departments.add(admin, injection);
        check("SQL-like department name is stored as literal text", injection.equals(
                departments.findById(literal.getDepartmentId()).orElseThrow().getDepartmentName()));
        Department unicode = departments.add(admin, "\uD83D\uDCDA".repeat(100));
        check("100 Unicode code points fit VARCHAR(100)", departments.findById(unicode.getDepartmentId())
                .orElseThrow().getDepartmentName().codePointCount(0, unicode.getDepartmentName().length()) == 100);
        expect("101 Unicode code points are rejected", IllegalArgumentException.class,
                () -> departments.add(admin, "\uD83D\uDCDA".repeat(101)));
        Department accent = departments.add(admin, "\u00C9conomie Lab");
        check("Filter respects the accent-insensitive database collation", departments.filterByName("economie")
                .stream().anyMatch(d -> d.getDepartmentId() == accent.getDepartmentId()));
        Department first = departments.add(admin, "Repeated Name");
        Department second = departments.add(admin, "Repeated Name");
        check("Existing non-unique name rule is preserved", first.getDepartmentId() != second.getDepartmentId());

        String hash1 = hashes.hash(password);
        String hash2 = hashes.hash(password);
        check("New hashes use the selected Argon2id parameters", hash1.startsWith("$argon2id$v=19$m=19456,t=2,p=1$"));
        check("Random salts produce different encoded hashes", !hash1.equals(hash2));
        check("Java-generated hash verifies", hashes.verify(password, hash1));
        check("Wrong password fails Java hash verification", !hashes.verify("wrong".toCharArray(), hash1));
        check("Non-Argon2id stored values are rejected", !hashes.verify(password, "plain_text"));
        expect("Setup password shorter than 8 characters is rejected", IllegalArgumentException.class,
                () -> AdminPasswordTool.validateNewPassword("short".toCharArray()));
        AdminPasswordTool.validateNewPassword("a".repeat(8).toCharArray());
        AdminPasswordTool.validateNewPassword("a".repeat(128).toCharArray());
        check("Setup accepts its 8 and 128 character boundaries", true);
        expect("Setup password over 128 characters is rejected", IllegalArgumentException.class,
                () -> AdminPasswordTool.validateNewPassword("a".repeat(129).toCharArray()));
        expect("Whitespace-only setup password is rejected", IllegalArgumentException.class,
                () -> AdminPasswordTool.validateNewPassword(" ".repeat(15).toCharArray()));

        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO DEPARTMENT(DepartmentName) VALUES ('Raw write attempt')")) {
            statement.executeUpdate();
            throw new AssertionError("The application login must not insert directly");
        } catch (SQLException error) {
            check("Application login cannot bypass procedures with raw INSERT", error.getErrorCode() == 1142);
        }
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement("SELECT PasswordHash FROM USER_ACCOUNT");
             ResultSet rows = statement.executeQuery()) {
            throw new AssertionError("Raw USER_ACCOUNT access must remain denied");
        } catch (SQLException error) {
            check("Application login cannot read USER_ACCOUNT directly", error.getErrorCode() == 1142);
        }
    }

    private static void configurationChecks() throws Exception {
        String secret="  private\\value:=\"கற்றல்";
        DatabaseConfig config=DatabaseConfig.fromValues(" jdbc:mysql://127.0.0.1:3306/example "," example_user ",secret.toCharArray());
        check("Entered URL and username are trimmed",config.getUrl().equals("jdbc:mysql://127.0.0.1:3306/example") && config.getUsername().equals("example_user"));
        check("Entered password preserves significant spaces and characters",config.getPassword().equals(secret));
        expect("Connection form rejects non-MySQL URL",IllegalArgumentException.class,() -> DatabaseConfig.fromValues("other:database","user",secret.toCharArray()));
        expect("Connection form rejects blank username",IllegalArgumentException.class,() -> DatabaseConfig.fromValues(config.getUrl()," ",secret.toCharArray()));
        expect("Connection form rejects empty password",IllegalArgumentException.class,() -> DatabaseConfig.fromValues(config.getUrl(),"user",new char[0]));
        expect("Connection form rejects placeholder password",IllegalArgumentException.class,() -> DatabaseConfig.fromValues(config.getUrl(),"user","REPLACE_WITH_PRIVATE_PASSWORD".toCharArray()));
        Path folder=Files.createTempDirectory("hallsync-config-"); Path file=folder.resolve("db.local.properties");
        try {
            config.saveNew(file); Properties stored=new Properties();
            try (var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)) { stored.load(reader); }
            check("Properties saving round-trips backslashes, quotes, spaces and Unicode",secret.equals(stored.getProperty("db.password")) && config.getUrl().equals(stored.getProperty("db.url")));
            expect("First-run save cannot overwrite an existing settings file",FileAlreadyExistsException.class,() -> config.saveNew(file));
            Properties after=new Properties(); try (var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)) { after.load(reader); }
            check("A refused overwrite preserves the existing file",secret.equals(after.getProperty("db.password")));
            if (Files.getFileStore(file).supportsFileAttributeView("posix")) {
                check("New local settings are owner-only on POSIX",Files.getPosixFilePermissions(file).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
            }
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(folder); }
    }

    private static void check(String label, boolean result) {
        if (!result) { throw new AssertionError(label); }
        passed++;
        System.out.println("PASS - " + label);
    }

    private static void expect(String label, Class<? extends Exception> expected, Action action) throws Exception {
        try {
            action.run();
        } catch (Exception error) {
            if (expected.isInstance(error)) {
                check(label, true);
                return;
            }
            throw error;
        }
        throw new AssertionError(label);
    }

    @FunctionalInterface
    private interface Action { void run() throws Exception; }
}
