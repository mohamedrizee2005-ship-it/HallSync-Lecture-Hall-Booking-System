package lk.ac.kln.hallsync.model;

public record Facility(int id, String name, String description) {
    public LookupOption option() { return new LookupOption(id, name); }
}
