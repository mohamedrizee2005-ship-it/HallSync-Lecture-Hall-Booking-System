package lk.ac.kln.hallsync.auth;

import java.io.Console;
import java.util.Arrays;

/** Local administrator setup helper. Prints an encoded hash, never the plaintext password. */
public final class AdminPasswordTool {
    private AdminPasswordTool() { }

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            System.err.println("Run this tool in the VS Code Integrated Terminal or Command Prompt.");
            System.exit(1);
        }
        char[] first = null;
        char[] second = null;
        boolean failed = false;
        try {
            first = console.readPassword("New application-admin password (15-128 characters): ");
            second = console.readPassword("Repeat password: ");
            validateNewPassword(first);
            if (!Arrays.equals(first, second)) {
                throw new IllegalArgumentException("The two passwords do not match.");
            }
            String hash = new Argon2PasswordHasher().hash(first);
            System.out.println("Private encoded hash for your local administrator setup:");
            System.out.println(hash);
            System.out.println("Paste it into the private setup query. Keep this output out of GitHub.");
        } catch (IllegalArgumentException error) {
            System.err.println(error.getMessage());
            failed = true;
        } finally {
            if (first != null) { Arrays.fill(first, '\0'); }
            if (second != null) { Arrays.fill(second, '\0'); }
        }
        if (failed) { System.exit(1); }
    }

    public static void validateNewPassword(char[] value) {
        int length = value == null ? 0 : Character.codePointCount(value, 0, value.length);
        if (length < 15 || length > 128) {
            throw new IllegalArgumentException("Use a password or passphrase of 15-128 characters.");
        }
        boolean onlyWhitespace = true;
        for (char character : value) {
            if (!Character.isWhitespace(character)) { onlyWhitespace = false; break; }
        }
        if (onlyWhitespace) {
            throw new IllegalArgumentException("A password cannot contain only whitespace.");
        }
    }
}
