package lk.ac.kln.hallsync.repository;

import lk.ac.kln.hallsync.model.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {
    List<Booking> forRequester(int requester) throws SQLException;
    List<Booking> all() throws SQLException;
    Optional<Booking> find(int booking) throws SQLException;
    List<Integer> groups(int booking) throws SQLException;
    List<ScheduleEntry> schedule(LocalDate from, LocalDate to) throws SQLException;
    int create(int actor, BookingDraft draft) throws SQLException;
    void update(int actor, int booking, BookingDraft draft) throws SQLException;
    void review(int actor, int booking, String decision, String note) throws SQLException;
    void cancel(int actor, int booking) throws SQLException;
    void complete(int actor, int booking) throws SQLException;
}
