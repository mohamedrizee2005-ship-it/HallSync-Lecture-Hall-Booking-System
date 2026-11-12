package lk.ac.kln.hallsync.auth;

import com.password4j.Argon2Function;
import com.password4j.Password;
import com.password4j.SecureString;
import com.password4j.types.Argon2;

/** Password4j supplies Argon2id; no custom cryptographic algorithm is implemented here. */
public final class Argon2PasswordHasher implements PasswordHasher {
    private static final Argon2Function FUNCTION =
            Argon2Function.getInstance(19456, 2, 1, 32, Argon2.ID);

    @Override
    public String hash(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("A password is required.");
        }
        SecureString secret = new SecureString(password);
        try {
            return Password.hash(secret).addRandomSalt(16).with(FUNCTION).getResult();
        } finally {
            secret.clear();
        }
    }

    @Override
    public boolean verify(char[] password, String encodedHash) {
        if (password == null || password.length == 0 || encodedHash == null
                || !encodedHash.startsWith("$argon2id$")) {
            return false;
        }
        SecureString secret = new SecureString(password);
        try {
            Argon2Function storedParameters = Argon2Function.getInstanceFromHash(encodedHash);
            return Password.check(secret, encodedHash).with(storedParameters);
        } catch (RuntimeException malformedHash) {
            // A malformed stored hash must fail authentication, not create a session.
            return false;
        } finally {
            secret.clear();
        }
    }
}
