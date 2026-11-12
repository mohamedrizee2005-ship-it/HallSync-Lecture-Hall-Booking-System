package lk.ac.kln.hallsync.auth;

import lk.ac.kln.hallsync.*;
import lk.ac.kln.hallsync.model.AccountDraft;
import lk.ac.kln.hallsync.repository.*;
import lk.ac.kln.hallsync.service.AccountService;
import java.sql.SQLException;

/** Local bootstrap requires a privileged MySQL login; its credentials are never saved. */
public final class AdministratorSetupService {
    public int create(DatabaseConfig application,String databaseUser,char[] databasePassword,AccountDraft details,char[] accountPassword) throws Exception {
        DatabaseConfig privileged=DatabaseConfig.fromValues(application.getUrl(),databaseUser,databasePassword);
        DatabaseConnectionFactory connections=new DatabaseConnectionFactory(privileged);
        int actor;
        // The restricted application login cannot perform this direct account-table read.
        try(var c=connections.open();var s=c.prepareStatement("SELECT AccountID FROM USER_ACCOUNT WHERE Role='AdminStaff' AND IsActive=1 ORDER BY AccountID LIMIT 1");var rows=s.executeQuery()) {
            if(!rows.next())throw new IllegalStateException("Initial administrator records are missing. Check the database installation.");
            actor=rows.getInt(1);
        }
        var identity=new AuthenticatedUser(actor,"local_database_setup","AdminStaff");
        var service=new AccountService(new JdbcAccountManagementRepository(new JdbcExecutor(connections)),new Argon2PasswordHasher());
        return service.provision(identity,details,accountPassword,"AdminStaff");
    }
}
