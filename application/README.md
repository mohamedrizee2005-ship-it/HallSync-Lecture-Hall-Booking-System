# HallSync Java application

Open the entire repository in VS Code and select **Run and Debug > HallSync: Full desktop application**. Keep the working database and private local settings.

## 1. Reuse the working connection

Keep `application/db.local.properties`. It is ignored by Git. A new extracted ZIP needs a copy of that one local file from your previous working `application` folder. The example properties file is only a template.

From a terminal inside `application`:

```cmd
mvn clean package
java -jar target/hallsync-connection-check.jar
java -cp target/hallsync-connection-check.jar lk.ac.kln.hallsync.HallSyncApp
```

The diagnostic JAR name/default entry point are kept for compatibility. The last command launches the desktop. Maven resolves dependencies; no manual Java-source copying or JDBC JAR download is needed. JDK 17+ is required; JDK 21 and 26 are checked in CI.

## 2. Upgrade an existing database

Run `03_upgrade_desktop_reads.sql` in your current Workbench administrator connection. It adds `sp_list_hall_blocks` and `sp_list_accounts`, then grants their execution to the application role. Both routines require an active administrator. Account results exclude hashes.

This upgrade preserves all tables, data, users and existing booking procedures and can repeat. A fresh installation using the latest `database/HallSync_Full_Setup.sql` already includes it. Do not rerun the full installer against an installed database.

## 3. First application administrator

`hallsync_app` is the **MySQL connection login**. HallSync sign-in uses a separate account in `USER_ACCOUNT`. Synthetic demo passwords were discarded and cannot be used to sign in.

If you already have an active AdminStaff account with a known password, use it. Otherwise run this in the Integrated Terminal or Command Prompt:

```cmd
java -cp target/hallsync-connection-check.jar lk.ac.kln.hallsync.auth.AdminPasswordTool
```

Enter and confirm a private 15-128 character password/passphrase. Copy `02_provision_project_admin.example.sql` to `02_provision_project_admin.local.sql`, replace the hash placeholder privately, and run only that setup section once using your Workbench administrator connection. Sign in as `project_admin` with your chosen application password.

Keep the hash, password, local setup SQL and local properties out of GitHub. If the account already exists, use it; do not delete data or rerun the database installer to fix sign-in.

## 4. Screens and rules

- **Student:** availability, equipment lookup and approved/completed timetable. Public registration always creates Student.
- **Lecturer:** those features plus personal booking requests, Pending edits and future cancellation.
- **AdminStaff:** requester features plus independent review, completion, catalogue creation, hall configuration, equipment, blocks, account provisioning/filtering and reports.

Lecture/Tutorial requests require a course and at least one group. Requests belong to the signed-in requester. Pending does not reserve a hall. Approval rechecks active accounts, capacity, blocks and overlap inside the existing database transaction. An administrator cannot review their own request. Approved moves require cancellation plus a new request. Rejected, Cancelled and Completed are terminal.

Times use Asia/Colombo. Availability is a snapshot. Blocks cannot silently displace Approved bookings. Hall changes cannot invalidate future approvals. Equipment requires `Quantity > 0` and `0 <= WorkingQuantity <= Quantity`.

Use **Accounts** to provision lecturers and a second administrator. Public registration cannot choose staff roles. A second independent administrator is needed to approve an administrator's own request.

## 5. Code structure and OOP

- `model`: immutable data classes; most are Java records. Department remains a conventional immutable class.
- `repository`: interfaces and parameterised JDBC implementations. JdbcExecutor owns JDBC cleanup and procedure calls.
- `service`: validation, ownership and role checks. Actor IDs come from authenticated identities.
- `auth`: Argon2id, private login lookup and verified identity.
- `desktop`: Swing screens, forms, reusable read-only tables and background workers.
- `ApplicationServices`: creates and connects services/repositories.
- `HallSyncApp`: desktop sign-in, dashboard and sign-out.
- `DepartmentApp` / `ConnectionCheck`: earlier learning and diagnostic entry points.

Swing components stay on the event dispatch thread. UiTasks/SwingWorker runs JDBC and hashing in the background. Write procedures own their transaction; Java does not wrap another transaction around them. Bound parameters separate input values from SQL. CSV exports preserve UTF-8, escape fields and neutralise spreadsheet formula prefixes.

Repository interfaces demonstrate abstraction and polymorphism. Feature panels inherit reusable behaviour from TablePanel. RecordTableModel overrides AbstractTableModel methods. Composition connects services to repositories. Roles are permissions, not Java class inheritance.

This trusted local prototype must not distribute shared database credentials to untrusted clients. A public deployment needs a backend boundary. TLS REQUIRED encrypts the connection; a remote deployment also requires certificate/host verification.

## 6. Save and update

Ctrl+S saves local source. Maven builds it. Service calls change database rows. Git commit/push uploads tracked source. These are separate actions.

Use a proper Git clone for future updates: save/commit your own tracked changes, fetch/pull main and review conflicts. A ZIP is a source snapshot without Git history. Do not reset or overwrite private local settings to update it.

The separate PDF explains Windows/VS Code setup, architecture, workflows, tests and presentation questions. No study PDF is committed here.
