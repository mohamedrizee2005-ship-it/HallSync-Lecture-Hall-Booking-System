package lk.ac.kln.hallsync.service;

import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.HallBlock;
import lk.ac.kln.hallsync.repository.HallBlockRepository;
import java.sql.SQLException;
import java.time.*;
import java.util.List;

public final class HallBlockService {
    private final HallBlockRepository repository;
    private final Clock clock;
    public HallBlockService(HallBlockRepository repository, Clock clock) { this.repository = repository; this.clock = clock.withZone(BookingService.ZONE); }
    public List<HallBlock> list(AuthenticatedUser user) throws SQLException { return repository.list(AccessRules.administrator(user)); }
    public int create(AuthenticatedUser user, int hall, LocalDateTime start, LocalDateTime end, String reason) throws SQLException {
        int actor = AccessRules.administrator(user);
        if (start == null || end == null || !start.isBefore(end) || !end.isAfter(LocalDateTime.now(clock)) || start.getNano() != 0 || end.getNano() != 0) {
            throw new IllegalArgumentException("Use a valid block interval ending in the future, to whole seconds.");
        }
        return repository.create(actor, Validation.positive(hall,"Hall"), start, end, Validation.text(reason,"Reason",255));
    }
    public void cancel(AuthenticatedUser user, int id) throws SQLException { repository.cancel(AccessRules.administrator(user),Validation.positive(id,"Block ID")); }
}
