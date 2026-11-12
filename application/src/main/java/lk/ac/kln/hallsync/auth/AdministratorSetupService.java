package lk.ac.kln.hallsync.auth;

import lk.ac.kln.hallsync.*;
import lk.ac.kln.hallsync.model.AccountDraft;
import lk.ac.kln.hallsync.service.AccountService;
import java.sql.Statement;

/** Local bootstrap requires privileged MySQL access, including on a completely empty account table. */
public final class AdministratorSetupService {
    public int create(DatabaseConfig application,String databaseUser,char[] databasePassword,AccountDraft details,char[] accountPassword) throws Exception {
        DatabaseConfig privileged=DatabaseConfig.fromValues(application.getUrl(),databaseUser,databasePassword);
        var connections=new DatabaseConnectionFactory(privileged);
        try(var c=connections.open()) {
            // The restricted backend login has no raw account-table SELECT privilege.
            // Test this before accepting initialization, even when there are no accounts yet.
            try(var s=c.prepareStatement("SELECT AccountID FROM USER_ACCOUNT LIMIT 1");var rows=s.executeQuery()) { }
            AccountDraft clean=AccountService.validate(details);
            AdminPasswordTool.validateNewPassword(accountPassword);
            String hash=new Argon2PasswordHasher().hash(accountPassword);
            try(var s=c.prepareStatement("INSERT INTO USER_ACCOUNT(DepartmentID,FullName,Email,Username,PasswordHash,Role,IsActive) VALUES(?,?,?,?,?,'AdminStaff',1)",Statement.RETURN_GENERATED_KEYS)) {
                s.setObject(1,clean.departmentId());s.setString(2,clean.fullName());s.setString(3,clean.email());s.setString(4,clean.username());s.setString(5,hash);s.executeUpdate();
                try(var keys=s.getGeneratedKeys()) { if(!keys.next())throw new IllegalStateException("Administrator ID was not returned.");return keys.getInt(1); }
            }
        }
    }
}
