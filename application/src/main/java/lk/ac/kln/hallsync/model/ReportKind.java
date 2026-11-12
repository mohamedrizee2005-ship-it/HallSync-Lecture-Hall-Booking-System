package lk.ac.kln.hallsync.model;

public enum ReportKind {
    BookingDetails("Booking details"), StatusTotals("Booking status totals"),
    HallFacilities("Hall facilities"), HallUsage("Scheduled hall usage");
    private final String label;
    ReportKind(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
