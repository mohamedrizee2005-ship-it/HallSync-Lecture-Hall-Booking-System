package lk.ac.kln.hallsync.model;

public record CourseUnit(int id, int departmentId, String code, String title) {
    public LookupOption option() { return new LookupOption(id, code + " - " + title); }
}
