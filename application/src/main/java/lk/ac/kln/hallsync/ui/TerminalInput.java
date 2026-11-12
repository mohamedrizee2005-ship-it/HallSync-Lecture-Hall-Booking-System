package lk.ac.kln.hallsync.ui;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Reads public input normally and passwords through a non-echoing console. */
public final class TerminalInput {
    private final Console console = System.console();
    private final BufferedReader reader =
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    public String readLine(String prompt) throws IOException {
        if (console != null) {
            return console.readLine("%s", prompt);
        }
        System.out.print(prompt);
        return reader.readLine();
    }

    public char[] readPassword(String prompt) {
        if (console == null) {
            throw new IllegalStateException(
                    "Password entry needs a terminal. Use the VS Code Integrated Terminal or Command Prompt.");
        }
        char[] value = console.readPassword("%s", prompt);
        return value == null ? new char[0] : value;
    }
}
