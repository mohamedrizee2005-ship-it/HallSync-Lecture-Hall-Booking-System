package lk.ac.kln.hallsync.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Request data. The requester is deliberately absent: it comes from sign-in. */
public record BookingDraft(int hallId, Integer courseId, LocalDate date,
                           LocalTime start, LocalTime end, String purpose,
                           BookingType type, int attendance, List<Integer> groupIds) {
    public BookingDraft { groupIds = groupIds == null ? List.of() : List.copyOf(groupIds); }
}
