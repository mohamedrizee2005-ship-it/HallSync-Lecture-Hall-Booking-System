package lk.ac.kln.hallsync.service;

import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.Department;
import lk.ac.kln.hallsync.repository.DepartmentRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Department input rules and application-level authorization. */
public final class DepartmentService {
    private final DepartmentRepository departments;

    public DepartmentService(DepartmentRepository departments) {
        this.departments = Objects.requireNonNull(departments);
    }

    public List<Department> list() throws SQLException {
        return departments.findAll();
    }

    public Optional<Department> findById(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Department ID must be a positive integer.");
        }
        return departments.findById(id);
    }

    public List<Department> filterByName(String text) throws SQLException {
        if (text == null) {
            throw new IllegalArgumentException("Search text is required.");
        }
        String clean = text.strip();
        checkLength(clean, "Search text");
        return clean.isEmpty() ? departments.findAll() : departments.findByName(clean);
    }

    public Department add(AuthenticatedUser user, String name) throws SQLException {
        if (user == null || !user.isAdministrator()) {
            throw new SecurityException("Sign in as an administrator to add a department.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Department name cannot be empty.");
        }
        String clean = name.strip();
        checkLength(clean, "Department name");
        return departments.add(user.getAccountId(), clean);
    }

    private static void checkLength(String value, String label) {
        if (value.codePointCount(0, value.length()) > 100) {
            throw new IllegalArgumentException(label + " must contain at most 100 characters.");
        }
    }
}
