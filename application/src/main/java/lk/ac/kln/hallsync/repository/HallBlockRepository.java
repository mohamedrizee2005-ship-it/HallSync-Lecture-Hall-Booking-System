package lk.ac.kln.hallsync.repository;

import lk.ac.kln.hallsync.model.HallBlock;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface HallBlockRepository {
    List<HallBlock> list(int actor) throws SQLException;
    int create(int actor, int hall, LocalDateTime start, LocalDateTime end, String reason) throws SQLException;
    void cancel(int actor, int block) throws SQLException;
}
