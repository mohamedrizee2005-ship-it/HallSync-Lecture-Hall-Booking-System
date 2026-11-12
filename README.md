# HallSync - Lecture Hall Booking System

A working **Java desktop + MySQL** university-project prototype for the Faculty of Science, University of Kelaniya. It uses Java OOP with separate classes. You can download the whole project and study it one workflow at a time.

## What is complete

| User | Available features |
| --- | --- |
| Student | Register, sign in, find available halls, view equipment and the approved timetable. |
| Lecturer | Student viewing features, plus own booking requests, Pending edits and future cancellation. |
| Administrator | Bookings and independent approvals/rejections, completion, hall blocks, equipment, reference data, account provisioning/filtering and reports/CSV. |

The database includes 11 tables, 4 views and 27 procedures. Approval prevents conflicting reservations inside a transaction. History is retained. An administrator cannot review their own booking.

## Run your existing project

You already installed Java, Maven and MySQL and connected successfully. Keep them.

1. On this page choose **Code > Download ZIP**, then extract the ZIP.
2. Copy your working `application/db.local.properties` from the earlier project into the new `application` folder. If you do not copy it, the new first-connection form can test and save your existing MySQL login details.
3. If you have not already applied it, run [application/03_upgrade_desktop_reads.sql](application/03_upgrade_desktop_reads.sql) with your Workbench administrator connection. It preserves data and can repeat. **Do not drop your database or rerun the full installer.**
4. Double-click **run_hallsync.cmd**. It checks for Java/Maven, builds, then opens HallSync.
5. Sign in with your **HallSync application account**. This is different from the MySQL connection login `hallsync_app`. If you have not created a usable administrator yet, follow [application/README.md](application/README.md#first-application-administrator).

To use VS Code: **File > Open Folder**, select the whole repository folder, install **Extension Pack for Java**, then choose **Run and Debug > HallSync: Full desktop application > F5**. You do not need to copy Java classes individually.

## First installation on another laptop

| Order | Do this once |
| --- | --- |
| 1 | Install a JDK, Maven and MySQL 8.0.46. JDK 17+ is supported; CI checks JDK 21 and 26. |
| 2 | In Workbench run [database/HallSync_Full_Setup.sql](database/HallSync_Full_Setup.sql). It is the **only full database installer**. |
| 3 | Run [application/01_create_application_login.sql](application/01_create_application_login.sql). Keep its generated MySQL password private. |
| 4 | Start `run_hallsync.cmd`. Enter that limited login in the first-connection form and click **Test and save connection**. |
| 5 | Follow the application README to create the first usable HallSync administrator, then sign in. |

The first-connection form writes only a new local settings file. It does not install/reset the database, create accounts or overwrite an existing working file.

## Simple project map

| Location | Purpose |
| --- | --- |
| `run_hallsync.cmd` | Start here on Windows. |
| `database/HallSync_Full_Setup.sql` | Complete SQL, with eight numbered study sections. |
| `database/01_...sql` to `10_...sql` | Smaller reference components for studying SQL; not extra installation steps. |
| `application/src/main/java/lk/ac/kln/hallsync/` | All production Java source, grouped by responsibility. |
| `application/pom.xml` | Maven dependencies and build settings. |
| `.vscode/` | Editor build/run settings. |
| `.github/workflows/` | Automated checks on disposable test databases. |

## Study in this order

All Java paths below start inside `application/src/main/java/lk/ac/kln/hallsync/`.

| Step | Read | Understand |
| --- | --- | --- |
| 1 | `database/HallSync_Full_Setup.sql`, sections 1-3 | Tables, attributes, primary/foreign keys and constraints. |
| 2 | `model/Department.java` | A small object with private fields and getters. |
| 3 | `DatabaseConfig.java`, `DatabaseConnectionFactory.java` | Private settings and a JDBC connection. |
| 4 | `repository/DepartmentRepository.java`, `JdbcDepartmentRepository.java` | An interface, its implementation and bound SQL. |
| 5 | `service/DepartmentService.java` | Input validation and administrator permission. |
| 6 | `HallSyncApp.java`, `ApplicationServices.java`, `desktop/LoginDialog.java` | Start-up, object composition and sign-in. |
| 7 | `desktop/BookingEditor.java` > `service/BookingService.java` > `repository/JdbcBookingRepository.java` > SQL section 4 | One full booking flow across all layers. |
| 8 | `desktop/BookingsPanel.java` and `sp_review_booking` | Approval, self-review prevention, overlap and transactions. |
| 9 | `CatalogService`, `HallBlockService`, `AccountService`, `ReportService` | Remaining features, one service at a time. |
| 10 | SQL sections 5-8 and the test classes | Views, demo data, limited privileges and verification. |

Models carry data. Services apply rules. Repositories talk to MySQL. Screens collect input and display results. Each class remains in its own file so your group can explain its responsibility.

## Scope and checks

This completes the agreed **local desktop v1**. Recurring booking series, automatic timetable generation, individual student enrolment, multi-stage approvals, public hosting and password recovery are outside it. Public deployment would require a trusted server-side boundary for shared database access. Synthetic demo data is not an official university timetable.

GitHub Actions checks the actual Maven build, authentication, catalogues, booking workflows, concurrent approvals, first-connection setup and real Swing navigation on JDK 21/26 against disposable MySQL 8.0.46. The separate database suite checks constraints and workflows. No test resets your laptop database.

This runs as a desktop window; no webpage or web server is needed. A project webpage can link to this repository/download. The application itself is not hosted in a browser.

Keep passwords, `db.local.properties`, `*.local.sql` and build output private. Study PDFs remain separate from GitHub.
