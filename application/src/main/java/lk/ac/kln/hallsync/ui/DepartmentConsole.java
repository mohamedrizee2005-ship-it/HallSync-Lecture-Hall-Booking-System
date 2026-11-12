package lk.ac.kln.hallsync.ui;

import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.auth.AuthenticationService;
import lk.ac.kln.hallsync.model.Department;
import lk.ac.kln.hallsync.service.DepartmentService;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Terminal interface; persistence and permission rules live in other classes. */
public final class DepartmentConsole {
    private final DepartmentService departments;
    private final AuthenticationService authentication;
    private final TerminalInput input;
    private AuthenticatedUser user;

    public DepartmentConsole(DepartmentService departments, AuthenticationService authentication,
                             TerminalInput input) {
        this.departments = departments;
        this.authentication = authentication;
        this.input = input;
    }

    public void run() throws IOException {
        while (true) {
            System.out.println("\nHallSync - Departments");
            System.out.println("Signed in: " + (user == null ? "guest"
                    : user.getUsername() + " (" + user.getRole() + ")"));
            System.out.println("1 List departments\n2 Find by ID\n3 Filter by name");
            System.out.println("4 Add department (administrator)\n5 Sign in\n6 Sign out\n0 Exit");
            String choice = input.readLine("Choose: ");
            if (choice == null || "0".equals(choice.strip())) {
                System.out.println("Goodbye.");
                return;
            }
            try {
                switch (choice.strip()) {
                    case "1":
                        printDepartments(departments.list());
                        break;
                    case "2":
                        findById();
                        break;
                    case "3":
                        printDepartments(departments.filterByName(input.readLine("Name contains: ")));
                        break;
                    case "4":
                        addDepartment();
                        break;
                    case "5":
                        signIn();
                        break;
                    case "6":
                        user = null;
                        System.out.println("Signed out.");
                        break;
                    default:
                        System.out.println("Choose one of the displayed options.");
                }
            } catch (IllegalArgumentException | SecurityException | IllegalStateException error) {
                System.out.println(error.getMessage());
            } catch (SQLException error) {
                if ("45000".equals(error.getSQLState())) {
                    System.out.println("Request rejected: " + error.getMessage());
                } else {
                    System.out.printf("Database operation failed (SQLState=%s, code=%d).%n",
                            error.getSQLState(), error.getErrorCode());
                }
            }
        }
    }

    private void findById() throws IOException, SQLException {
        String text = input.readLine("Department ID: ");
        int id;
        try {
            id = Integer.parseInt(text == null ? "" : text.strip());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Department ID must be a positive integer.");
        }
        Optional<Department> found = departments.findById(id);
        if (found.isPresent()) {
            printDepartment(found.get());
        } else {
            System.out.println("No department found with that ID.");
        }
    }

    private void addDepartment() throws IOException, SQLException {
        if (user == null || !user.isAdministrator()) {
            throw new SecurityException("Sign in as an administrator to add a department.");
        }
        Department created = departments.add(user, input.readLine("New department name: "));
        System.out.println("Department added:");
        printDepartment(created);
    }

    private void signIn() throws IOException, SQLException {
        // A failed account switch must not retain the previous user's privileges.
        user = null;
        String username = input.readLine("Application username: ");
        char[] password = input.readPassword("Application password: ");
        try {
            Optional<AuthenticatedUser> result = authentication.signIn(username, password);
            if (result.isPresent()) {
                user = result.get();
                System.out.println("Signed in successfully.");
            } else {
                System.out.println("Sign-in failed. Check your username and password.");
            }
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static void printDepartments(List<Department> values) {
        if (values.isEmpty()) {
            System.out.println("No departments found.");
        }
        for (Department department : values) {
            printDepartment(department);
        }
    }

    private static void printDepartment(Department department) {
        System.out.printf("%d - %s%n", department.getDepartmentId(), department.getDepartmentName());
    }
}
