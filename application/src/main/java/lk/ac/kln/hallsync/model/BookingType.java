package lk.ac.kln.hallsync.model;

/** Names deliberately match the database ENUM values. */
public enum BookingType {
    Lecture, Tutorial, Exam, Event;
    public boolean requiresAcademics() { return this == Lecture || this == Tutorial; }
}
