package lk.ac.kln.hallsync.model;

public record Building(int id, String code, String name, String location) {
    public LookupOption option() { return new LookupOption(id, code + " - " + name); }
}
