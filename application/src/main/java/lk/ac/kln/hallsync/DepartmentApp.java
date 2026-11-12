package lk.ac.kln.hallsync;

import lk.ac.kln.hallsync.auth.Argon2PasswordHasher;
import lk.ac.kln.hallsync.auth.AuthenticationService;
import lk.ac.kln.hallsync.auth.JdbcAccountRepository;
import lk.ac.kln.hallsync.repository.DepartmentRepository;
import lk.ac.kln.hallsync.repository.JdbcDepartmentRepository;
import lk.ac.kln.hallsync.service.DepartmentService;
import lk.ac.kln.hallsync.ui.DepartmentConsole;
import lk.ac.kln.hallsync.ui.TerminalInput;
import java.io.IOException;
import java.nio.file.Path;

/** Assembles the objects needed by the department feature. */
public final class DepartmentApp {
    private DepartmentApp() { }

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: DepartmentApp [config-file]");
            System.exit(1);
        }
        try {
            Path configPath = Path.of(args.length == 1 ? args[0] : "db.local.properties");
            DatabaseConfig config = DatabaseConfig.load(configPath);
            DatabaseConnectionFactory connections = new DatabaseConnectionFactory(config);
            DepartmentRepository repository = new JdbcDepartmentRepository(connections);
            DepartmentService departments = new DepartmentService(repository);
            AuthenticationService authentication = new AuthenticationService(
                    new JdbcAccountRepository(connections), new Argon2PasswordHasher());
            new DepartmentConsole(departments, authentication, new TerminalInput()).run();
        } catch (IllegalArgumentException error) {
            System.err.println("Configuration error: " + error.getMessage());
            System.exit(1);
        } catch (IOException error) {
            System.err.println("Cannot read the configuration or terminal input ("
                    + error.getClass().getSimpleName() + "). Check your working folder and file.");
            System.exit(1);
        }
    }
}
