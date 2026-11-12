# HallSync application - setup and learning notes

Start with the root [README](../README.md). The normal Windows entry point is `run_hallsync.cmd`; in VS Code use **HallSync: Full desktop application**.

## Your existing connection

Reuse `db.local.properties` from your working application folder. It is ignored by Git and is never overwritten by first-run setup. If the file is missing, HallSync opens a form for the JDBC URL, limited MySQL username and private password. **Test and save connection** verifies a booking-view read before writing the new file. Backslashes and other special password characters are escaped by Java Properties automatically.

The form does not create/reset the database or provision MySQL accounts. A first installation needs `database/HallSync_Full_Setup.sql` and `01_create_application_login.sql` first. Existing installations need only the repeatable `03_upgrade_desktop_reads.sql` if the two administrator read routines are missing.

`HALLSYNC_DB_PASSWORD` overrides a file password when set. If it is set during first-run setup, its value is initially supplied in the hidden password field; later loading also uses the environment override. To use only the local file in Command Prompt, clear an old override with `set "HALLSYNC_DB_PASSWORD="`.

## First application administrator

The MySQL login `hallsync_app` connects Java to the database. The HallSync sign-in window uses a different account from `USER_ACCOUNT`. Demo account passwords were discarded and cannot be used to sign in.

If you already have a usable active AdminStaff account, use it and skip this setup. Otherwise:

1. From the repository root, open Command Prompt or the VS Code Integrated Terminal.
2. Run the commands below. The password tool needs a terminal that supports hidden input.
3. Enter and confirm a private 15-128 character application password/passphrase. The tool prints its encoded Argon2id hash.
4. Copy `02_provision_project_admin.example.sql` to **02_provision_project_admin.local.sql**. Privately replace the hash placeholder; keep the SQL quotes.
5. Open that local SQL file using your Workbench administrator connection. Execute only its setup section once.
6. Start HallSync. Sign in as **project_admin** with the application password you chose.

```cmd
cd application
mvn clean package
java -cp target/hallsync-connection-check.jar lk.ac.kln.hallsync.auth.AdminPasswordTool
```

Keep the hash, local SQL and password private. If the username already exists, use the existing account; do not delete/reset data. Initial bootstrap through Workbench is a controlled administrator action. Normal screens never let users type another person's actor ID.

## Useful commands

Run these from the `application` folder:

```cmd
mvn clean package
java -jar target/hallsync-connection-check.jar
java -cp target/hallsync-connection-check.jar lk.ac.kln.hallsync.HallSyncApp
```

The first command builds. The second checks the connection. The third opens the full desktop. The diagnostic JAR filename is retained for compatibility. Maven supplies the JDBC and password dependencies.

## Demonstrate the complete project

1. Sign in as an administrator. Use **Accounts** to provision a lecturer with a private password.
2. Confirm a building/hall, course and student group exist in **Catalogues**.
3. Sign in as the lecturer. Search a future date/time and request a suitable hall.
4. For Lecture/Tutorial choose a course and at least one group. Submit and show **Pending**.
5. Sign in as an independent administrator and approve it from **Review bookings**.
6. Repeat the availability search: that hall/time is now unavailable. An adjacent interval remains available.
7. Cancel the future approval and show its retained history.
8. Create/cancel a maintenance block and show the effect on availability.
9. Show equipment quantities, account department filtering and report/CSV export.

An administrator's own request requires another administrator to review it. Student registration always creates Student; staff roles are provisioned by an administrator.

## Read the Java folders

| Folder | Responsibility |
| --- | --- |
| `model` | Immutable row/input models and enums. |
| `repository` | Contracts and parameterised JDBC implementations. |
| `service` | Validation, ownership and role rules. |
| `auth` | Private login lookup, Argon2id and verified identity. |
| `desktop` | Swing screens, reusable tables/forms and background workers. |
| `ui` | Optional earlier department-console learning example. |

`ApplicationServices` composes the objects. `HallSyncApp` starts the desktop. `ConnectionCheck` remains a diagnostic. The root README gives a small study order; start there rather than reading every file at once.

Repository interfaces demonstrate abstraction/polymorphism. Feature panels inherit reusable behaviour from TablePanel. RecordTableModel overrides Swing table methods. Records are immutable data classes, not SQL tables. Roles are permissions rather than User subclasses.

JDBC and hashing run through SwingWorker; Swing widgets stay on the event thread. Write procedures own their transaction and recheck current data/permissions. Java never replaces the database locking protocol. CSV export preserves UTF-8, escapes fields and neutralises spreadsheet formula prefixes.

## Save and update

Ctrl+S saves local source. Maven builds it. Successful service calls change MySQL rows. Git commit/push uploads tracked source. These are separate actions. A ZIP is a source snapshot; use a Git clone for convenient later pull/push. Review local changes before updating and preserve private settings.

The source is an academic local prototype. A public/untrusted client deployment requires a backend boundary; do not distribute shared MySQL credentials. TLS REQUIRED encrypts the local connection; remote identity verification requires suitable certificates/host verification.
