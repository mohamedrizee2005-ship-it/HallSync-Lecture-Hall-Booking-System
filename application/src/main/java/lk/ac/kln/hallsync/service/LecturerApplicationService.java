package lk.ac.kln.hallsync.service;

import lk.ac.kln.hallsync.auth.*;
import lk.ac.kln.hallsync.model.*;
import lk.ac.kln.hallsync.repository.LecturerApplicationRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/** Verification applications never grant access until an authorised review commits. */
public final class LecturerApplicationService {
    private final LecturerApplicationRepository repository;
    private final PasswordHasher passwords;
    public LecturerApplicationService(LecturerApplicationRepository repository,PasswordHasher passwords) { this.repository=repository;this.passwords=passwords; }
    public boolean installed() throws SQLException { return repository.installed(); }
    public int register(AccountDraft details,String staffId,char[] password) throws SQLException {
        AccountDraft clean=AccountService.validate(details);
        if(clean.departmentId()==null)throw new IllegalArgumentException("Choose your department.");
        String staff=Validation.text(staffId,"Staff ID",50);
        if(staff.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("A staff ID cannot contain control characters.");
        AdminPasswordTool.validateNewPassword(password);
        return repository.register(clean,staff,passwords.hash(password));
    }
    public List<LecturerApplication> list(AuthenticatedUser user,String status) throws SQLException {
        int actor=AccessRules.administrator(user);
        if(status!=null&&!Set.of("Pending","Approved","Rejected").contains(status))throw new IllegalArgumentException("Choose a valid application status.");
        return repository.list(actor,status);
    }
    public void review(AuthenticatedUser user,int application,String decision,String note,boolean identityVerified) throws SQLException {
        int actor=AccessRules.administrator(user);Validation.positive(application,"Application");
        if(!Set.of("Approved","Rejected").contains(decision==null?"":decision))throw new IllegalArgumentException("Choose Approve or Reject.");
        String clean=Validation.optional(note,"Review note",500);
        if("Approved".equals(decision)&&!identityVerified)throw new IllegalArgumentException("Confirm the lecturer's identity through an independent university channel before approval.");
        if("Rejected".equals(decision)&&(clean==null||clean.isBlank()))throw new IllegalArgumentException("Enter a reason for rejecting this application.");
        repository.review(actor,application,decision,clean,identityVerified);
    }
}
