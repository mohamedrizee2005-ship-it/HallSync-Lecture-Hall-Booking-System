package lk.ac.kln.hallsync.model;

public record StudentGroup(int id, int departmentId, String name, int intakeYear, int studyYear) {
    public LookupOption option() { return new LookupOption(id, name + " / " + intakeYear); }
}
