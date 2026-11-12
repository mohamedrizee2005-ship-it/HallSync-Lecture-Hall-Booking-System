package lk.ac.kln.hallsync.service;
import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.RoomIssue;
import lk.ac.kln.hallsync.repository.RoomIssueRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
public final class RoomIssueService {
 private final RoomIssueRepository repository;
 public RoomIssueService(RoomIssueRepository repository){this.repository=repository;}
 public int report(AuthenticatedUser user,int hall,String category,String details)throws SQLException{
  int actor=AccessRules.signedIn(user);Validation.positive(hall,"Hall");
  if(!Set.of("Equipment","Capacity","Other").contains(category==null?"":category))throw new IllegalArgumentException("Choose a report category.");
  return repository.report(actor,hall,category,Validation.text(details,"Details",1000));
 }
 public List<RoomIssue> list(AuthenticatedUser user)throws SQLException{return repository.list(AccessRules.signedIn(user));}
 public void resolve(AuthenticatedUser user,int issue,String note)throws SQLException{repository.resolve(AccessRules.administrator(user),Validation.positive(issue,"Report"),Validation.text(note,"Resolution note",500));}
}
