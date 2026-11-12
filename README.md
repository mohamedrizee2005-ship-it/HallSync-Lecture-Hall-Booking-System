# HallSync

A browser-based lecture hall booking workspace for the Faculty of Science, University of Kelaniya.

**Java OOP backend · HTML, CSS & JavaScript interface · MySQL**

## Features

- Responsive room discovery with date, time, capacity and building filters.
- A guided booking flow with course and student-group selection.
- Personal reservations, independent approvals, cancellation and retained history.
- Weekly campus schedule and room equipment details.
- Administrator catalogues, hall maintenance, account provisioning and reports.
- CSV report export, server-side sessions and salted Argon2id password hashing.
- Accessible forms, keyboard-friendly dialogs and reduced-motion support.

## Run an existing installation

1. Extract into a new folder and copy your working `application/db.local.properties` into the new `application` directory.
2. Double-click **run_hallsync.cmd**. HallSync builds and opens in your browser.
3. Sign in using your existing HallSync account. Keep the launcher window open while using the application.

The existing MySQL schema and accounts are reused. Do not drop your database or rerun the full installer. Existing passwords remain valid. New account passwords require **8–128 characters**.

## First installation

Requirements: **JDK 17+**, **Maven**, and **MySQL 8.0.46**. MySQL Workbench can administer the database. Node.js, XAMPP and a separate web server are not required.

1. In Workbench, execute `database/HallSync_Full_Setup.sql` once, then `database/Create_Application_Login.sql`. Keep the generated database password private.
2. Run `run_hallsync.cmd`. Use the browser connection form to enter the limited MySQL login. It tests the connection and saves a new private local settings file.
3. On the sign-in page choose **Create administrator**. Enter your MySQL administrator credentials and the application account details. The database administrator password is used once and is not saved.
4. Sign in and provision staff accounts through **People**. Public registration creates Student accounts only.

For an older database missing the administrator read routines, run `database/Upgrade_Desktop_Reads.sql` once using Workbench. It preserves data. `database/Verify_Database.sql` provides read-only verification.

## Build and launch

From `application`:

```cmd
mvn clean package
java -jar target/hallsync.jar
```

The default address is `http://127.0.0.1:8080`. If that port is busy, the launcher chooses a free port and prints the exact address. Use that printed address if browser opening is unavailable. Stop the server with Ctrl+C.

VS Code: open the repository root and select **HallSync: Browser application**.
NetBeans: open the `application` Maven project. Main class: `lk.ac.kln.hallsync.HallSyncApp`.

Optional connection diagnostic:

```cmd
java -cp target/hallsync.jar lk.ac.kln.hallsync.ConnectionCheck
```

The previous local password-hash helper and administrator SQL template remain available as an alternative to browser initialization.

## Data and access

The database contains 11 tables, 4 reporting views and 27 procedures. Times use Asia/Colombo. Booking approval and maintenance coordinate under hall-row locks. Pending requests do not reserve halls; terminal records remain in history.

The server binds to the local computer only. Authentication uses expiring HttpOnly/SameSite cookies, session rotation and CSRF checks. Database credentials and `*.local.sql` files must remain private. Seed data and room illustrations are representative; load the real timetable before live use. Public hosting requires a reviewed HTTPS deployment and server configuration.

GitHub Actions checks the Maven build on JDK 21/26, real MySQL workflows, HTTP authorization/security and browser interactions. Tests use disposable schemas and do not reset an installed database.
