package lk.ac.kln.hallsync.model;

public enum BookingStatus {
    Pending, Approved, Rejected, Cancelled, Completed;
    public boolean isTerminal() { return this == Rejected || this == Cancelled || this == Completed; }
}
