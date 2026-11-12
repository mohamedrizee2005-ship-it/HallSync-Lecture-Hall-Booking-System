package lk.ac.kln.hallsync.repository;
import lk.ac.kln.hallsync.model.RoomIssue;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
public final class JdbcRoomIssueRepository implements RoomIssueRepository {
 private final JdbcExecutor db;
 public JdbcRoomIssueRepository(JdbcExecutor db){this.db=db;}
 public int report(int actor,int hall,String category,String details)throws SQLException{return db.callForId("{CALL sp_report_room_issue(?,?,?,?,?)}",actor,hall,category,details);}
 public List<RoomIssue> list(int actor)throws SQLException{return db.callRows("{CALL sp_list_room_issues(?)}",r->new RoomIssue(r.getInt("IssueID"),r.getInt("HallID"),r.getString("HallLabel"),r.getInt("ReportedByID"),r.getString("ReporterName"),r.getString("Category"),r.getString("Details"),r.getString("IssueStatus"),r.getObject("CreatedAt",LocalDateTime.class),r.getString("ReviewerName"),r.getObject("ReviewedAt",LocalDateTime.class),r.getString("ReviewNote")),actor);}
 public void resolve(int actor,int issue,String note)throws SQLException{db.call("{CALL sp_resolve_room_issue(?,?,?)}",actor,issue,note);}
}
