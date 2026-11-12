# HallSync

Lecture hall booking management system for the Faculty of Science, University of Kelaniya.

Java Swing desktop application with a MySQL database.

## Features

- Account authentication and student registration.
- Hall availability, equipment details and timetable viewing.
- Lecturer booking requests, Pending edits and cancellation.
- Independent approval/rejection and booking completion.
- Hall maintenance blocks, capacity/status management and equipment quantities.
- Department, building, course and student-group management.
- Administrator account provisioning and department filtering.
- Booking, equipment and hall-usage reports with CSV export.

## Requirements

- JDK 17 or newer and Apache Maven.
- MySQL 8.0.46; MySQL Workbench for database administration.
- Windows for `run_hallsync.cmd`. The Maven application also runs on other Java desktop platforms.

## Installation

1. Extract the project archive.
2. In MySQL Workbench, run [database/HallSync_Full_Setup.sql](database/HallSync_Full_Setup.sql) once for a new database.
3. Run [database/Create_Application_Login.sql](database/Create_Application_Login.sql) once and retain the generated connection password privately.
4. Start **run_hallsync.cmd**. The first-connection form tests and saves the limited MySQL login locally.
5. Configure an application administrator as described below, then sign in.

For an existing database, preserve `application/db.local.properties` and run [database/Upgrade_Desktop_Reads.sql](database/Upgrade_Desktop_Reads.sql) only if its two read procedures are missing. The upgrade preserves data. Do not rerun the full installer on an installed schema.

## Administrator account

The MySQL connection login and HallSync application accounts are separate. Initial seed account passwords are unavailable.

From the `application` directory:

```cmd
mvn clean package
java -cp target/hallsync.jar lk.ac.kln.hallsync.auth.AdminPasswordTool
```

Choose a private application password. Copy `database/Create_Project_Admin.example.sql` to `database/Create_Project_Admin.local.sql`, insert the generated Argon2id hash privately, and execute that local file once using the Workbench administrator connection. Sign in as `project_admin` with the chosen application password. Existing usable application administrators can skip initialization.

Administrators provision lecturer/staff accounts through **Accounts**. Public registration creates Student accounts. Booking decisions require an administrator other than the requester.

## Run

Windows: **run_hallsync.cmd**.

From the `application` directory:

```cmd
mvn clean package
java -jar target/hallsync.jar
```

VS Code: open the repository root and select **HallSync: Full desktop application**.
NetBeans: open the `application` Maven project; the main class is `lk.ac.kln.hallsync.HallSyncApp`.

Connection diagnostics:

```cmd
java -cp target/hallsync.jar lk.ac.kln.hallsync.ConnectionCheck
```

## Database and verification

The schema contains 11 tables, 4 reporting views and 27 procedures. Times use Asia/Colombo. Approval and maintenance operations coordinate through hall-row locks. Pending requests do not reserve halls; historical terminal records are retained.

[database/Verify_Database.sql](database/Verify_Database.sql) performs read-only structural and integrity checks. GitHub Actions builds on JDK 21/26 and runs JDBC, Swing and database checks against disposable MySQL instances.

Seed records are synthetic. Existing timetable reservations must be entered before using availability for live operations. This deployment uses trusted local database access; a public client deployment requires a server-side boundary.

Local connection settings, passwords and `*.local.sql` files must remain private.
