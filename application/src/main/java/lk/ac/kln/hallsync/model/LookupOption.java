package lk.ac.kln.hallsync.model;

/** A labelled database identifier for a selection box. */
public record LookupOption(int id, String label) {
    @Override public String toString() { return label + " [" + id + "]"; }
}
