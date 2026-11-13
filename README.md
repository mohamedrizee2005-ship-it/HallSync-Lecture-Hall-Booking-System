# HallSync · University of Kelaniya

Lecture hall booking and campus space management with a **Java OOP backend, HTML/CSS/JavaScript interface and MySQL 8.0.46**.

![HallSync university sign-in](.github/assets/hallsync-dark.webp)

## Start or update

Choose the guide you need:

| What do you want to do? | Open this guide |
| --- | --- |
| Run HallSync on a laptop for the first time | [First-time setup — Windows](#first-time-setup-windows) |
| Install a newer GitHub version on the same laptop | [Update an existing installation](#update-an-existing-installation) |
| Fix a connection or understand which password to use | [Connection help](#connection-help) |

## First-time setup (Windows)

These steps are for Windows 10/11. You need a **Java JDK**, **Apache Maven**, **MySQL Server 8.0.46** and **MySQL Workbench**. Install the tools once on each laptop. If a tool is already installed and works, keep it and continue with the next step.

HallSync runs a Java backend and opens its interface in your browser. Maven downloads its Java libraries automatically. Node.js, Python, XAMPP and a separate web server are not required to run it. VS Code is optional for editing the code.

### Step 1: Install Java JDK

1. Open the [official Eclipse Temurin downloads](https://adoptium.net/temurin/releases/?version=21&os=windows&arch=x64&package=jdk).
2. Choose **JDK 21 → Windows → x64 → MSI** for a typical Intel/AMD Windows laptop. Download the JDK, rather than only a Java runtime.
3. Open the installer. Keep **Add to PATH** enabled and select the option to set **JAVA_HOME**, then finish the installation. The [official Windows installation guide](https://adoptium.net/installation/windows/) shows these options.
4. Open a **new Command Prompt** and run the following commands, one at a time:

```cmd
java -version
javac -version
```

Both should show a version number. The project requires JDK 17 or later; Java 21 and 26 are checked in CI. If your existing JDK 26 works, you can use it.

### Step 2: Install Apache Maven

1. Open the [official Maven downloads](https://maven.apache.org/download.cgi) and download a stable **Maven 3 Binary zip archive**. Choose the binary ZIP rather than the source ZIP.
2. Right-click the ZIP → **Extract All**. Move the extracted Maven folder to a permanent location, such as `C:\Tools`.
3. Open that Maven folder, then open **bin**. Check that it contains `mvn.cmd`. Copy the full folder address from File Explorer. For example, it might be `C:\Tools\apache-maven-3.10.0\bin`; use the actual version you downloaded.
4. In Windows Search, open **Edit environment variables for your account**. Under **User variables**, select **Path → Edit → New**, paste that bin-folder address, then click **OK** on the open dialogs.
5. Close Command Prompt, open a new one and run:

```cmd
mvn -version
```

You should see the Maven version and the JDK it uses. If Windows says Maven is not recognized, check that the Path entry points to the folder containing `mvn.cmd`, then open a new Command Prompt. See the [official Maven Windows guide](https://maven.apache.org/guides/getting-started/windows-prerequisites.html).

### Step 3: Install MySQL Server and Workbench

**MySQL Server stores the data. MySQL Workbench is the program you use to run the SQL setup. Install both.**

1. Download and open the [official MySQL Installer for Windows](https://dev.mysql.com/downloads/installer/). Use MySQL Server **8.0.46**, the version used by this project.
2. Choose **Custom** and select **MySQL Server 8.0.46** and **MySQL Workbench**. Follow the installer prompts and install any prerequisites it requests.
3. During server configuration, keep TCP/IP enabled and the port at **3306**, unless that port is already used. Choose and privately save your MySQL **root password**. This is your **MySQL administrator password**.
4. Configure MySQL to run as a Windows service and start automatically. Complete the installer.
5. Open Workbench. Open the local connection created by the installer. If none appears, click **+** beside **MySQL Connections**, enter host `127.0.0.1`, port `3306` and username `root`, then click **Test Connection** and enter the root password you chose.
6. Open that working connection. You will use it to execute the HallSync SQL file in Step 5.

If you already use MySQL and Workbench successfully, keep that installation and your existing administrator password. Workbench can be closed after setup; the MySQL Server service must remain running.

### Step 4: Download and extract HallSync

1. Open the [HallSync repository](https://github.com/mohamedrizee2005-ship-it/HallSync-Lecture-Hall-Booking-System).
2. Click **Code → Download ZIP**, or use [Download the latest ZIP](https://github.com/mohamedrizee2005-ship-it/HallSync-Lecture-Hall-Booking-System/archive/refs/heads/main.zip).
3. Right-click the downloaded ZIP → **Extract All**. Choose a folder on your laptop and finish extracting.
4. Open the extracted folder until you can see **`run_hallsync.cmd`**, **`application`** and **`database`** together. This is the project root. Run HallSync from here, rather than from inside the ZIP.

Downloading the ZIP gives you the project files. Step 5 creates the database on your own MySQL Server; it does not download someone else's accounts or bookings.

### Step 5: Create the database with the one SQL file

1. In Workbench, open your **MySQL administrator connection** from Step 3.
2. Choose **File → Open SQL Script** and open **`database/HallSync_Setup.sql`** from the extracted HallSync folder.
3. Enable **Stop Script Execution On Errors** in the SQL editor toolbar. Clear any text selection, then press **Ctrl + Shift + Enter** to run the whole script.
4. Check the **Action Output** panel. If execution stops with an error, fix that first. Successful completion returns a result saying **HallSync 3.0 ready**.
5. When the script creates the `hallsync_app` account for the first time, Workbench also shows its **generated password** in a result grid. Copy it and keep it private. The GRANT/privilege list is not the password. If that password result is missing, use [Connection help](#connection-help).
6. Refresh the **Schemas** panel. You should see **`hallsync_db`**.

This single file creates the tables, rules, procedures, reference data and application connection account. It also supports upgrades of compatible existing HallSync installations. Keep the supplied SQL file; you do not need to create a separate file for every table or query. Saving an SQL file alone does not execute it.

### Step 6: Run HallSync and save its database connection

1. In the extracted project root, double-click **`run_hallsync.cmd`**.
2. Keep internet access available while Maven builds the application and downloads any required libraries. Wait for the browser to open, and keep the console window open while using HallSync.
3. On the connection page, enter:

| Connection field | What to enter |
| --- | --- |
| Database address | `jdbc:mysql://127.0.0.1:3306/hallsync_db` |
| Connection username | `hallsync_app` |
| Connection password | The private generated password from Step 5 |

4. Click **Test & connect**. HallSync checks the connection and automatically creates **`application/db.local.properties`** on your laptop. The sign-in page then appears.

Use the MySQL host and port from Step 3 if you chose different ones. The connection form uses `hallsync_app`; the root credentials are used only for privileged setup in Workbench and Step 7.

**Keep `db.local.properties` private and keep a backup.** It saves your connection settings for later runs and updates. It is not included in the GitHub ZIP and must never be uploaded to GitHub.

If the browser does not open, use the exact address printed in the console. It is normally `http://127.0.0.1:8080`; HallSync selects a free port if that one is busy. If double-clicking fails, open Command Prompt in the extracted project root and run:

```cmd
run_hallsync.cmd --console
```

### Step 7: Create the first HallSync administrator

1. On the sign-in page, expand **Local installation setup → Create administrator**.
2. Enter your **MySQL admin username** (usually `root`) and the **MySQL admin password** you chose during MySQL installation. These credentials authorize this one-time setup and are not saved by the form.
3. Enter the administrator's name, email and a HallSync username, such as `project_admin`. Choose a separate **HallSync sign-in password** of 8–128 characters and confirm it.
4. Click **Create administrator**. Return to sign-in and use the HallSync username and password you just chose.

The generated `hallsync_app` password connects Java to MySQL. Your new `project_admin` password signs you into HallSync. They serve different purposes; see the [password table](#connection-help) if you are unsure.

If this database already has a working HallSync administrator, sign in with that account instead of creating another one.

### Step 8: Prepare the campus and check the three roles

1. As administrator, open **Campus manager**. Add your actual course units and student groups. A Lecture/Tutorial request needs a course unit and at least one group.
2. Review the supplied halls and add any confirmed local buildings, rooms, capacities and equipment. Published-capacity halls are already Active. Enter existing timetable reservations as bookings before relying on availability.
3. Sign out. Create a student account or register as a lecturer. The new account waits for administrator approval.
4. Sign back in as administrator, open **Account approvals**, verify the applicant and approve the registration.
5. Sign in with the approved account. A student can view halls and schedules. A lecturer can find a hall and submit a booking request; the administrator reviews that request separately.

See [Three roles](#three-roles) and [Request a hall as a lecturer](#request-a-hall-as-a-lecturer) below for the normal workflows.

For later daily use, keep MySQL running, open **`run_hallsync.cmd`** and sign in. Your saved connection settings are reused.

## Update an existing installation

Use this section when HallSync already works on your laptop and a newer version is available in GitHub. Reuse your installed tools, MySQL database and HallSync sign-in account.

### Normal update — keep your local settings

1. **Stop the old HallSync application.** Close its console window, or press **Ctrl + C** in that window. Keep MySQL Server running.
2. **Save your private settings.** Copy **`application/db.local.properties`** out of the old project folder to a safe private location before replacing or deleting that folder.
3. **Download the new version.** Use **Code → Download ZIP** or [Download the latest ZIP](https://github.com/mohamedrizee2005-ship-it/HallSync-Lecture-Hall-Booking-System/archive/refs/heads/main.zip). Extract it into a **new folder**.
4. **Copy the one settings file.** Paste your saved **`db.local.properties`** into the new version's **`application`** folder. Keep that exact filename.
5. **Check whether the database setup changed.** For a Java/interface-only update, continue to Step 6. If `database/HallSync_Setup.sql` changed, follow [When an update changes the database](#when-an-update-changes-the-database) first. The GitHub commit's changed-file list shows which files changed.
6. **Start the new version.** Double-click the new folder's **`run_hallsync.cmd`**. It rebuilds the application from the updated code automatically; let the build finish.
7. **Sign in and check your data.** Your existing accounts and bookings should appear from the same database. Keep the old folder and backups until you confirm the new version works.

The saved file should be in this location:

**New project folder → `application` → `db.local.properties`**

For a normal code update on the same laptop, the private settings file is the only old project file you need to copy. Maven builds a new `target` folder from the new code. Your actual accounts and bookings stay in **MySQL Server**, not in the extracted project folder. Copying `db.local.properties` preserves the connection; it is not a database backup.

An update to **README.md only** changes the instructions. You can read the new instructions on GitHub without reinstalling the application.

### When an update changes the database

1. Stop HallSync. In Workbench, open your MySQL administrator connection and use **Server → Data Export**. Select **`hallsync_db`**, choose **Dump Structure and Data**, export it to a private **Self-Contained File**, and wait for the export to finish. Include stored procedures/functions if that export option is available.
2. Open the **new version's** **`database/HallSync_Setup.sql`**. Enable **Stop Script Execution On Errors**, then execute the whole file with **Ctrl + Shift + Enter**.
3. Check for errors before starting HallSync. Supported repeat runs retain existing accounts, password hashes, IDs, bookings and decisions. The existing `hallsync_app` connection password is preserved, so keep using your copied settings file.
4. Start the new launcher and sign in with your existing HallSync account.

**Keep the existing `hallsync_db` database.** Dropping it would remove your saved records. If the setup reports an incompatible schema, keep the backup and resolve the error before continuing.

### If you deleted the old project folder

If MySQL still has `hallsync_db`, your records may still be there. Extract the new ZIP and run it. If you have a private backup of `db.local.properties`, put it in `application` first. Otherwise, use the connection form with your existing `hallsync_app` password. If that password is lost, follow [Connection help](#connection-help). Sign in with your existing HallSync account after reconnecting.

A different laptop normally has a different MySQL Server. Follow [First-time setup](#first-time-setup-windows) there and use that laptop's own connection credentials. Sharing the repository link shares the code; it does not create a shared live database.

## Connection help

### Which password do I use?

| Password | Where it comes from | Where you use it |
| --- | --- | --- |
| **MySQL administrator password** | You chose it when installing/configuring MySQL, usually for `root` | Workbench administrator connection and the one-time **Create administrator** form |
| **MySQL connection password** | The SQL setup generates it for `hallsync_app` | HallSync **Test & connect**; saved privately in `application/db.local.properties` |
| **HallSync sign-in password** | You choose it when creating or registering a HallSync account | HallSync sign-in page with your application username |

There is no shared default administrator password in the downloaded project.

### The generated connection password is missing or lost

For an existing `hallsync_app` account, rerunning setup keeps its password and does not generate a new one. Reuse your existing private settings if you have them.

If you need a new connection password, run this once in Workbench using your **MySQL administrator connection**:

```sql
ALTER USER 'hallsync_app'@'127.0.0.1'
IDENTIFIED BY RANDOM PASSWORD;
```

Workbench returns the new generated password in a result grid. Keep it private. Stop HallSync. If `application/db.local.properties` already exists, move it to a private backup location, then restart HallSync and enter the new password in **Test & connect**. HallSync creates a fresh settings file.

This changes only the database connection password. Your HallSync sign-in passwords and saved bookings stay the same. Any other HallSync instance using this MySQL account must also use the new connection password.

### Common startup problems

| What you see | What to do |
| --- | --- |
| `java` or `javac` is not recognized | Finish [Step 1](#step-1-install-java-jdk), check the JDK PATH setting and open a new Command Prompt. |
| `mvn` is not recognized | Repeat the bin-folder PATH check in [Step 2](#step-2-install-apache-maven). |
| The launcher disappears or shows an error | Open Command Prompt in the extracted project root and run `run_hallsync.cmd --console`. Read the error that stays on screen. |
| The build cannot download libraries | Check your internet connection and run the launcher again. |
| Saved database settings cannot connect | Start the MySQL Server service and restart HallSync. Check that the saved host, port and connection password match that MySQL Server. If the settings changed, move the private settings file aside and reconnect through the form. |
| The database username or password is not accepted | Use `hallsync_app` and its generated connection password on the connection page. Check the password table above; use the reset steps only if that connection password is lost. |
| The browser does not open | Open the exact local address printed in the console. Keep the console open while you use the application. |

Never upload passwords, `db.local.properties` or private database backups to GitHub.

## Three roles

| Role | Registration and approval | Main actions |
| --- | --- | --- |
| Student | Public registration → inactive → administrator verifies university identity → approved | View active halls and published schedules, filter courses, export calendar, submit and track own room reports |
| Lecturer | Public registration with staff ID → inactive → administrator verifies identity → approved | Student viewing actions plus request halls, manage own Pending requests, cancel own reservations, view own agenda/calendar |
| Administrator | Local privileged initialization or provisioning by an existing administrator | Manage halls and access, approve registrations and bookings, maintain equipment, create closures, resolve room reports, view dated reports and exports |

Registration and booking approval are separate. Public signup cannot create an administrator. A matching name, staff ID or email is not proof of ownership: the administrator checks university records and confirms the registration through an independent university channel. This is manual verification, not university single sign-on or automatic email verification. Reviewers never receive passwords. Passwords use salted Argon2id hashes and require 8–128 characters.

Existing administrator-provisioned users remain valid. New student and lecturer self-registrations require approval. Rejection requires a reason and keeps the account inactive. A decision and account activation commit together, with reviewer, time and note retained. Rejected applications cannot be replayed into Approved. Contact the administrator for corrections; public password recovery and resubmission are outside this release.

Administrators can change other accounts' roles or active state in **People → Manage**. They cannot change their own access, activate an unverified applicant through that shortcut, or remove the final active administrator. Server requests recheck the current role and active state, so old session privileges do not survive a role change.

## Request a hall as a lecturer

1. Choose **Register as a lecturer** and submit your name, email, username, department, staff ID and password. Wait for approval in **Account approvals**.
2. Sign in and open **Find a hall**. Choose a future date, start/end time and attendance. Filter by building, faculty or working equipment. Published capacities are included; equipment filters use locally recorded working quantities.
3. Select **View space → Book this space**. Complete your purpose and session type. Lectures and tutorials require a course unit and at least one student group; an administrator adds these through Campus manager.
4. Review and send the request. It starts **Pending** and reserves no room. A different administrator approves it through **Booking approvals** after the database rechecks capacity, conflicts and closures.
5. View progress in **My bookings**. Only Pending requests can be edited. To move an Approved session, cancel it and create a new request.

Students have no booking or administration controls. Their timetable is the published campus schedule with a course filter; it does not claim to know enrollment. Lecturers can switch to **My approved bookings** and export an ICS calendar. Import the downloaded ICS into Google Calendar, Outlook or another calendar application; it is a snapshot, not a live synchronization feed. **Course materials** links to the university learning portal.

## Campus data and sources

The university directory includes seven faculty references and 57 department names. Faculty names identify directory entries and locations; the application's institutional brand is **University of Kelaniya** throughout.

The setup seeds **14 Active teaching rooms with published capacities**, including all three listed F4 rooms (201: 350; 102: 100; 101: 80 seats) and selected K14, K1, K2, K27 and K28 rooms. Capacities come from the [official university classroom directory](https://cbs.kln.ac.lk/classrooms), checked on 12 November 2026. The directory is a published reference, not a live reservation feed or a guarantee of current seating. Existing matching halls are never overwritten or automatically reactivated by a repeat setup/import.

A8 Auditorium's **sixth-floor** location is sourced from the [university's 2023 SDG archive](https://sdgpt.kln.ac.lk/archives/2023). Its capacity remains unconfirmed, so it is a setup suggestion rather than an automatically bookable hall. Room codes such as `201` do not prove a floor level. Other unpublished floors remain empty; administrators can enter confirmed values in **Campus manager → Lecture halls → Manage**.

The equipment type catalogue includes projection, audio and connectivity, supported by the [university's teaching-facility description](https://fct.kln.ac.lk/facility/lecture-rooms). Per-room equipment counts and working quantities are not published there, so they are entered after local inspection. HallSync does not fabricate smart boards, AC units or working inventories. **Hall care** manages quantities; **Room reports** lets students and lecturers flag discrepancies and administrators record resolutions.

Source URLs and reference dates are included in `application/src/main/resources/campus.json` and linked from hall details. Campus photography is bundled locally and credited in [PHOTO_CREDITS.md](PHOTO_CREDITS.md). It depicts campus buildings and grounds, not the interior of every listed room.

## Booking integrity

- Times use Asia/Colombo. Start must be before end, the start must be in the future, purpose must be nonblank and attendance must be a positive integer within capacity.
- **Active** describes an operational hall. **Available for your time** is calculated for the chosen interval; a booking never permanently switches a hall's operational state.
- Overlap uses `existing_start < requested_end AND existing_end > requested_start`. Adjacent sessions are allowed. Pending requests may compete; only one conflicting request can be approved.
- Approval and maintenance use the same hall-row lock and coordinated transaction. Two simultaneous administrators cannot approve conflicting reservations. A closure cannot silently displace an approved booking.
- The requester comes from the authenticated session and owns the booking. An administrator cannot approve their own hall request.
- Cancelled, Rejected and Completed bookings remain terminal and are retained with history. Completed is allowed only after an Approved session ends.
- Hall edits cannot invalidate future approved reservations. Retiring a hall with history makes it Inactive; an unused hall can be removed. Quantity must be positive and `0 <= WorkingQuantity <= Quantity`.
- Students and lecturers see only their own discrepancy reports; administrators see the queue. Resolution requires an administrator and a note.

## Reports and operation

**Reports** includes inclusive date-range filtering (up to 367 days), booking status distribution, scheduled hall hours, detailed tables and CSV download. **Print / PDF** opens the browser print dialog; choose **Save as PDF**. Equipment inventory is current and is explicitly labelled independently of the booking date filter. Scheduled hours measure reservation duration, not actual room occupancy.

The main discovery/overview/approval screens refresh every minute while visible and idle. Submission and approval always validate against the current database. A manual Refresh action is available. Automatic report delivery, recurring bookings, automatic timetable generation, enrollment tracking and assignment management are excluded to keep this a focused booking system. Reports are exported on demand; no email is sent automatically.

Local operation is the default: Java binds to loopback, and browsers never connect directly to MySQL. The backend login uses a limited database role and TLS. Browser mutations require same-origin JSON and a CSRF token; sessions use HttpOnly/SameSite cookies, and authentication attempts are rate limited. The application is a university project suitable for local demonstrations; a real university launch still requires approved identity integration, a confirmed inventory/timetable, HTTPS hosting, operational backups and an institution-led security review.

## Code layout and verification

| Location | Purpose |
| --- | --- |
| `database/HallSync_Setup.sql` | Single repeatable schema, upgrades, procedures, UOK seed and connection-login setup |
| `application/src/main/java/lk/ac/kln/hallsync/model` | Domain records and status/type enums |
| `application/src/main/java/lk/ac/kln/hallsync/auth` | Password hashing, authentication and privileged initialization |
| `application/src/main/java/lk/ac/kln/hallsync/service` | Role rules, validation, booking, reports, calendar and room-report operations |
| `application/src/main/java/lk/ac/kln/hallsync/repository` | Repository interfaces and JDBC implementations with bound parameters |
| `application/src/main/java/lk/ac/kln/hallsync/web` | HTTP adapters, sessions, static assets and campus reference import |
| `application/src/main/resources/web` | Responsive UI, light/dark themes and locally bundled assets |
| `application/src/test` and `application/tests` | Java/JDBC/API and real-browser checks on disposable schemas |
| `database/tests` | Constraints, privilege boundaries, transactions and concurrent approval checks |

MySQL is the supported database. Existing normalized names are retained: `USER_ACCOUNT`, `DEPARTMENT`, `BUILDING`, `LECTURE_HALL`, `FACILITY`, `HALL_FACILITY`, `COURSE_UNIT`, `STUDENT_GROUP`, `BOOKING`, `BOOKING_GROUP`, `HALL_BLOCK`, `ACCOUNT_APPLICATION` and `ROOM_ISSUE`. The older lecturer application table is renamed safely during upgrade. A shared booking/timetable model avoids storing the same reservation in two conflicting `bookings` and `schedules` tables. The database has 13 tables, 80 columns, 19 foreign keys, 35 CHECK constraints, four reporting views and 37 procedures.

VS Code: open the extracted root and select **HallSync: Browser application**. NetBeans: open the `application` Maven project. To build and run manually:

```cmd
cd application
mvn clean package
java -jar target/hallsync.jar
```

GitHub Actions checks Java 21/26 with MySQL 8.0.46, including fresh/repeated setup, upgrading the older account table, data/password preservation, three-role registration and booking flows, unauthorized requests, session role changes, calendar/CSV export, room reports, hall retirement, mobile layout, long identities and competing approvals. Tests use isolated generated schema names and synthetic accounts, never a laptop's `hallsync_db`.

## Release validation

HallSync 3.0 passed 128 database checks and 305 application checks on each of Java 21 and Java 26 against MySQL 8.0.46. The latter include 119 API checks and 46 real-browser checks. A separate Windows job builds and launches the extracted project from a path with spaces and checks the fresh connection page. Normal-install checks confirm 14 published-capacity active halls with no demo users, invented bookings or assumed equipment quantities.

[View the report interface](.github/assets/hallsync-reports.webp). Interface previews use isolated test data.
