package lk.ac.kln.hallsync.repository;

import lk.ac.kln.hallsync.model.HallBlock;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public final class JdbcHallBlockRepository implements HallBlockRepository {
    private final JdbcExecutor db;
    public JdbcHallBlockRepository(JdbcExecutor db) { this.db = db; }
    @Override public List<HallBlock> list(int actor) throws SQLException {
        return db.callRows("{CALL sp_list_hall_blocks(?)}", r -> new HallBlock(r.getInt("BlockID"), r.getInt("HallID"),
                r.getString("BuildingCode") + " / " + r.getString("HallNo"), r.getString("RecordedByName"),
                r.getObject("BlockStart", LocalDateTime.class), r.getObject("BlockEnd", LocalDateTime.class),
                r.getString("Reason"), r.getString("BlockStatus")), actor);
    }
    @Override public int create(int actor, int hall, LocalDateTime start, LocalDateTime end, String reason) throws SQLException {
        return db.callForId("{CALL sp_create_hall_block(?,?,?,?,?,?)}", actor, hall, start, end, reason);
    }
    @Override public void cancel(int actor, int block) throws SQLException { db.call("{CALL sp_cancel_hall_block(?,?)}", actor, block); }
}
