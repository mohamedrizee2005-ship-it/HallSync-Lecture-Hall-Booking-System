package lk.ac.kln.hallsync.auth;

/** Password operations supplied by a password-hashing implementation. */
public interface PasswordHasher {
    String hash(char[] password);
    boolean verify(char[] password, String encodedHash);
}
