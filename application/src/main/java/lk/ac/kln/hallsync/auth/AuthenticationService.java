package lk.ac.kln.hallsync.auth;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/** Creates an identity only after an active account passes password verification. */
public final class AuthenticationService {
    private final AccountRepository accounts;
    private final PasswordHasher passwords;

    public AuthenticationService(AccountRepository accounts, PasswordHasher passwords) {
        this.accounts = Objects.requireNonNull(accounts);
        this.passwords = Objects.requireNonNull(passwords);
    }

    /** Existing sessions lose access immediately after an account is disabled or its role changes. */
    public boolean stillValid(AuthenticatedUser user) throws SQLException {
        var found=accounts.findForLogin(user.getUsername());
        return found.isPresent() && found.get().isActive() && found.get().getAccountId()==user.getAccountId() && found.get().getRole().equals(user.getRole());
    }

    public Optional<AuthenticatedUser> signIn(String username, char[] password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.length == 0) {
            return Optional.empty();
        }
        if (Character.codePointCount(password, 0, password.length) > 128) {
            return Optional.empty();
        }
        String clean = username.strip();
        if (clean.codePointCount(0, clean.length()) > 50) {
            return Optional.empty();
        }
        Optional<LoginRecord> found = accounts.findForLogin(clean);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        LoginRecord account = found.get();
        if (!account.isActive() || !passwords.verify(password, account.getPasswordHash())) {
            return Optional.empty();
        }
        return Optional.of(new AuthenticatedUser(
                account.getAccountId(), account.getUsername(), account.getRole()));
    }
}
