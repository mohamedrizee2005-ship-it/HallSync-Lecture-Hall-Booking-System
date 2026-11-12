package lk.ac.kln.hallsync.model;

public record LectureHall(int id, String buildingCode, String buildingName,
                          String number, String floor, int capacity, String status) {
    public String label() { return buildingCode + " / " + number; }
    public LookupOption option() { return new LookupOption(id, label() + " (" + capacity + " seats)"); }
}
