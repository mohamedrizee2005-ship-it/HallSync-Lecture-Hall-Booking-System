package lk.ac.kln.hallsync.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Booking details from the read view. Group membership is loaded separately when editing. */
public record Booking(int id, int requesterId, String requesterName, int hallId,
                      String hallLabel, Integer courseId, String courseCode,
                      LocalDate date, LocalTime start, LocalTime end, BookingType type,
                      String purpose, int attendance, BookingStatus status,
                      LocalDateTime requestedAt, String reviewerName, String decision,
                      LocalDateTime reviewedAt, String reviewNote) { }
