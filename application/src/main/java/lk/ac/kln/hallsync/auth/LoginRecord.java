package lk.ac.kln.hallsync.auth;

/** Internal authentication data. Never display this object or its encoded hash. */
public final class LoginRecord {
    private final int accountId;
    private final String username;
    private final String passwordHash;
    private final String role;
    private final boolean active;

    public LoginRecord(int accountId, String username, String passwordHash, String role, boolean active) {
        this.accountId = accountId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    public int getAccountId() { return accountId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }
}
