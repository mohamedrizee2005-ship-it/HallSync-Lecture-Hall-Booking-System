package lk.ac.kln.hallsync.model;

/** Administrator-visible account metadata; never includes password hashes. */
public record AccountSummary(int id, Integer departmentId, String department,
                             String fullName, String email, String username, String role, boolean active) { }
