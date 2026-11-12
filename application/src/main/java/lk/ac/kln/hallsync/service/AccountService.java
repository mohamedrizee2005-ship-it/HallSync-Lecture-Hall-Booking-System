package lk.ac.kln.hallsync.service;

import lk.ac.kln.hallsync.auth.*;
import lk.ac.kln.hallsync.model.*;
import lk.ac.kln.hallsync.repository.AccountManagementRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public final class AccountService {
    private final AccountManagementRepository repository;
    private final PasswordHasher passwords;
    public AccountService(AccountManagementRepository repository, PasswordHasher passwords) { this.repository = repository; this.passwords = passwords; }
    /** Public registration has no role parameter. It always creates a Student. */
    public int registerStudent(AccountDraft details, char[] password) throws SQLException {
        AccountDraft clean = validate(details); AdminPasswordTool.validateNewPassword(password);
        return repository.registerStudent(clean,passwords.hash(password));
    }
    public int provision(AuthenticatedUser user, AccountDraft details, char[] password, String role) throws SQLException {
        int actor = AccessRules.administrator(user);
        if (!Set.of("Student","Lecturer","AdminStaff").contains(role == null ? "" : role)) { throw new IllegalArgumentException("Choose a valid role."); }
        AccountDraft clean = validate(details); AdminPasswordTool.validateNewPassword(password);
        return repository.provision(actor,clean,passwords.hash(password),role);
    }
    public void manage(AuthenticatedUser user,int account,String role,boolean active) throws SQLException {
        int actor=AccessRules.administrator(user);Validation.positive(account,"Account");
        if(actor==account)throw new IllegalArgumentException("You cannot change your own access.");
        if(!Set.of("Student","Lecturer","AdminStaff").contains(role==null?"":role))throw new IllegalArgumentException("Choose a valid role.");
        repository.manage(actor,account,role,active);
    }
    public List<AccountSummary> list(AuthenticatedUser user, Integer department) throws SQLException {
        int actor = AccessRules.administrator(user); return repository.list(actor,Validation.optionalId(department,"Department"));
    }
    static AccountDraft validate(AccountDraft d) {
        if (d == null) { throw new IllegalArgumentException("Account details are required."); }
        String email = Validation.text(d.email(),"Email",254);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) { throw new IllegalArgumentException("Enter an email address such as name@example.com."); }
        String username = Validation.text(d.username(),"Username",50);
        if (username.codePoints().anyMatch(Character::isWhitespace)) { throw new IllegalArgumentException("A username cannot contain whitespace."); }
        return new AccountDraft(Validation.optionalId(d.departmentId(),"Department"),Validation.text(d.fullName(),"Full name",150),email,username);
    }
}
