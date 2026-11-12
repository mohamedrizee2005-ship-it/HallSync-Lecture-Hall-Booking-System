package lk.ac.kln.hallsync.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Public timetable data excludes requester identities and private review notes. */
public record ScheduleEntry(int bookingId, String hallLabel, String courseCode,
                            LocalDate date, LocalTime start, LocalTime end, String type, String status) { }
