package lk.ac.kln.hallsync.repository;
import lk.ac.kln.hallsync.model.RoomIssue;
import java.sql.SQLException;
import java.util.List;
public interface RoomIssueRepository {
 int report(int actor,int hall,String category,String details) throws SQLException;
 List<RoomIssue> list(int actor) throws SQLException;
 void resolve(int actor,int issue,String note) throws SQLException;
}
