package lk.ac.kln.hallsync.auth;

/** A signed-in application identity. It contains no password or password hash. */
public final class AuthenticatedUser {
    private final int accountId;
    private final String username;
    private final String role;

    // Only code in this authentication package constructs a session identity.
    AuthenticatedUser(int accountId, String username, String role) {
        this.accountId = accountId;
        this.username = username;
        this.role = role;
    }

    public int getAccountId() { return accountId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public boolean isAdministrator() { return "AdminStaff".equals(role); }
}
