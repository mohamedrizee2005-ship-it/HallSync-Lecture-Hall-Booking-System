# HallSync - Lecture Hall Booking System

Java Swing and MySQL group-project prototype for the University of Kelaniya, Faculty of Science.

## Implemented v1

- Application sign-in and student-only public registration, with Argon2id password hashing.
- Hall catalogue, equipment lookup, future availability and an approved/completed timetable.
- Lecturer/staff booking requests, Pending edits and future cancellations.
- Independent administrator approval/rejection, ended-session completion and retained history.
- Administrator catalogues: departments, buildings, facilities, courses, groups and lecture halls.
- Hall capacity/status changes, equipment quantities, maintenance blocks and block cancellation.
- Administrator account provisioning and account filtering by department.
- Four database reports with UTF-8 CSV export.

Each Java class is in its own file. The earlier department console and connection checker remain available for studying the smaller example.

## Run in VS Code on Windows

1. Get the latest **main** branch using Git clone/pull, or download and extract the complete repository ZIP.
2. Keep your working `application/db.local.properties` private. Copy that local file from your previous working copy into the new `application` folder when using a new ZIP.
3. Open the **whole repository folder** in VS Code. You should see `.vscode`, `application`, `database` and this README at the top level.
4. Install the recommended **Extension Pack for Java**. Keep your working JDK and Maven installation.
5. Existing database: run [application/03_upgrade_desktop_reads.sql](application/03_upgrade_desktop_reads.sql) with your MySQL administrator connection in Workbench. It adds two read routines and preserves all tables/data. **Do not rerun the full installer on an existing database.** A first installation follows [database/README.md](database/README.md) instead.
6. Follow [application/README.md](application/README.md) for the first real application administrator. MySQL connection login and HallSync application login are different accounts.
7. Choose **Run and Debug > HallSync: Full desktop application**, then press **F5**. Maven builds before the sign-in window opens.

Alternatively, double-click `run_hallsync.cmd` after Java, Maven, the database and private settings are ready. Study PDFs are provided separately and are not stored in this repository.

## Verification and scope

GitHub Actions builds Maven on JDK 21 and 26. Tests use disposable MySQL 8.0.46 schemas and exercise JDBC authentication, departments, booking workflows, concurrent approvals and real Swing sign-in/navigation under a virtual display. Workflows have read-only repository permissions.

This is a local academic desktop prototype using a trusted application environment and a restricted MySQL login. A public or untrusted student-facing deployment needs a server-side authentication/database boundary. Synthetic data is not the official university timetable; enter real existing future reservations before relying on availability.

The agreed v1 excludes recurring series, automatic timetable generation, individual student enrolment and multi-stage approvals. It retains booking history. General catalogue renaming/deletion, password recovery and public hosting are outside this version.
