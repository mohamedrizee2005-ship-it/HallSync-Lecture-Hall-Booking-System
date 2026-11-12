package lk.ac.kln.hallsync.service;

public final class Validation {
    private Validation() { }
    public static String text(String value, String label, int maximum) {
        if (value == null || value.isBlank()) { throw new IllegalArgumentException(label + " is required."); }
        String clean = value.strip();
        if (clean.codePointCount(0, clean.length()) > maximum) {
            throw new IllegalArgumentException(label + " must have at most " + maximum + " characters.");
        }
        return clean;
    }
    public static String optional(String value, String label, int maximum) {
        return value == null || value.isBlank() ? null : text(value, label, maximum);
    }
    public static int positive(int value, String label) {
        if (value <= 0) { throw new IllegalArgumentException(label + " must be a positive integer."); }
        return value;
    }
    public static Integer optionalId(Integer value, String label) { return value == null ? null : positive(value, label); }
}
