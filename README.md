# HallSync

A browser-based lecture hall booking workspace for the Faculty of Science, University of Kelaniya.

**Java OOP backend · HTML, CSS & JavaScript interface · MySQL**

![HallSync dark sign-in](.github/assets/hallsync-dark.webp)

## Features

- Responsive room discovery with date, time, capacity and building filters.
- A guided booking flow with course and student-group selection.
- Lecturer self-registration with administrator verification before sign-in.
- Separate lecturer-account approvals and hall-booking approvals, with retained decision history.
- Personal reservations, independent booking approvals and cancellation.
- Weekly campus schedule and room equipment details.
- Administrator catalogues, hall maintenance, account provisioning and reports.
- CSV report export, server-side sessions and salted Argon2id password hashing.
- Light and dark themes with a saved appearance preference and reduced-motion support.
- Real, locally bundled University of Kelaniya photography with source credits.
- A faculty-grouped department selector and a source-linked Kelaniya campus directory.

## Run an existing installation

1. Extract into a new folder and copy your working `application/db.local.properties` into the new `application` directory.
2. In MySQL Workbench, run **database/Upgrade_Lecturer_Approvals.sql** once using your MySQL administrator account. It adds lecturer registration and review without changing existing passwords, users or bookings. Do not rerun the full installer.
3. Double-click **run_hallsync.cmd**. HallSync builds and opens in your browser.
4. Sign in using your existing HallSync account. Keep the launcher window open while using the application.
5. Open **Kelaniya campus → Load campus details** as an administrator to add missing university department and building names.

The existing MySQL schema and accounts are reused. Do not drop your database or rerun the full installer. Existing passwords remain valid. New account passwords require **8–128 characters**.

## First installation

Requirements: **JDK 17+**, **Maven**, and **MySQL 8.0.46**. MySQL Workbench can administer the database. Node.js, XAMPP and a separate web server are not required.

1. In Workbench, execute `database/HallSync_Full_Setup.sql` once, then `database/Create_Application_Login.sql`. Keep the generated database password private.
2. Run `run_hallsync.cmd`. Use the browser connection form to enter the limited MySQL login. It tests the connection and saves a new private local settings file.
3. On the sign-in page choose **Create administrator**. Enter your MySQL administrator credentials and the application account details. The database administrator password is used once and is not saved.
4. Sign in and manage staff through **People** and **Lecturer approvals**. Student registration grants view-only access. Lecturer registration creates an inactive account and a Pending application; it grants no booking access until approval.
5. Open **Kelaniya campus**. Choose **Set up hall**, confirm its actual seating capacity with the faculty office, and save. Add its working equipment in **Hall care**. Load existing timetable reservations before trusting availability.

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

The database contains 12 tables, 4 reporting views and 30 procedures. Times use Asia/Colombo. Booking approval and maintenance coordinate under hall-row locks. Pending requests do not reserve halls; terminal records remain in history.

## Accounts and lecturer verification

| Role | Registration | Access |
| --- | --- | --- |
| Student | Public student registration | View halls, availability and schedules |
| Lecturer | Public lecturer application; administrator verifies and approves | Sign in after approval; submit and manage own booking requests |
| Administrator | Local privileged setup or provisioning by an existing administrator | Manage campus data, accounts, lecturer applications, bookings and reports |

Lecturer applicants provide their name, email, username, department, staff ID and a password. The password is hashed with Argon2id; reviewers never receive it. A new Lecturer account is stored with `IsActive=0` and a separate `LECTURER_APPLICATION` record is Pending. Until approval, authentication and database actor checks deny lecturer access. Student signup cannot grant Lecturer or AdminStaff access, and there is no public administrator registration route.

In **Lecturer approvals**, an administrator checks university staff records and confirms that the lecturer actually submitted this registration through an independent university channel. A matching name, staff ID or email alone does not establish identity. There is no automatic email verification or notification service in this version. The administrator explicitly confirms verification, then approves. Account activation and the decision commit in one transaction; competing reviews have only one winner.

Rejection requires a reason and leaves the account inactive. The reviewer, decision time and note remain in history. Decisions are final in this version; rejected applicants should contact the administrator to resolve corrections. Public resubmission and password recovery are not implemented.

Existing administrator-provisioned lecturers remain valid. An administrator may still provision an already-verified staff account directly through **People**. Administrator accounts require their own username and password; there is no password-free or universal administrator login.

Lecturer approval and booking approval are independent. After lecturer approval, a booking starts Pending and reserves no room until a separate booking decision. An administrator cannot approve their own hall booking. Students cannot request halls.

The additional ER entity is `LECTURER_APPLICATION`: its unique applicant `AccountID` references `USER_ACCOUNT`; optional `ReviewedByID` references the reviewing administrator in `USER_ACCOUNT`. Existing user IDs, passwords and booking relationships are retained. The upgrade adds only the new table and three restricted procedures, with no raw account-table access granted to the application login.

## Campus reference data

The source-linked catalogue contains 57 department entries across Science, Commerce and Management Studies, Computing and Technology, Humanities, Medicine and Social Sciences. Graduate Studies is included as a faculty without a separate department list. Sources and the check date are recorded in `application/src/main/resources/campus.json` and linked from the interface.

Six verified building codes are included: A4, A7, A8, A11, B1 and K14. Nine sourced venue names are available as setup suggestions. Building descriptions identify the faculty; they are not claims about official building titles. The catalogue is not an exhaustive campus inventory. It deliberately supplies no guessed capacities, floors, equipment or live reservations. An administrator must confirm these locally before making halls bookable. Existing names, IDs and bookings are preserved by the repeatable import.

Synthetic SQL fixtures remain labelled as demonstration data. The `HS-DEMO` hall is excluded from the normal discovery and new-booking interface. Real venue capacities must not be inferred from demo data.

Photography depicts the university campus, not individual room interiors. See [PHOTO_CREDITS.md](PHOTO_CREDITS.md) for authors, dates and image licenses. Files are bundled locally; no external image service is needed at runtime. Some views are historical photographs.

## Local use and sharing

Local use is the default and needs no hosting subscription. The Java server binds only to `127.0.0.1`; MySQL stays behind it. Visitors use a browser, so a separate desktop installation is unnecessary.

For a temporary group demonstration, [Cloudflare Quick Tunnels](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/) can provide an HTTPS address without a Cloudflare account or domain. Install `cloudflared` using [Cloudflare's official downloads](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/downloads/). Complete database connection and administrator creation locally first, then close the local HallSync server.

1. In one terminal, start the tunnel and keep it open:

   ```cmd
   cloudflared tunnel --url http://127.0.0.1:8080
   ```

2. Copy the exact HTTPS address printed by `cloudflared`. In another terminal, from `application`, replace the example below with that address:

   ```cmd
   set "HALLSYNC_PUBLIC_ORIGIN=https://YOUR-EXACT-HOST.trycloudflare.com"
   java -Dhallsync.port=8080 -jar target/hallsync.jar
   ```

3. Open the HTTPS address. Staff use their provisioned HallSync accounts; public registration grants Student access only. Keep MySQL, Java and the tunnel running. The public URL ends when the tunnel stops and changes on the next tunnel launch. To limit a demo to specific visitors, Cloudflare also documents the optional `--allowed-mail` flag.

Shared mode accepts only its configured HTTPS origin, marks session cookies Secure, keeps CSRF checks and disables database connection/administrator bootstrap endpoints. The tunnel and Java must use the same fixed port; shared mode fails if that port is busy. Do not expose MySQL port 3306 to the internet. Stop Java and the tunnel with Ctrl+C when finished. For local use again, open a new terminal without `HALLSYNC_PUBLIC_ORIGIN` or run `set "HALLSYNC_PUBLIC_ORIGIN="` before launching.

A temporary tunnel is for demonstrations, not an always-on university service. Permanent deployment needs an approved server with Java/MySQL, HTTPS, a stable domain, backups and managed administrator access. Static-only hosting cannot run this Java/MySQL backend.

Authentication uses expiring HttpOnly/SameSite cookies, session rotation and CSRF checks. Database credentials and `*.local.sql` files remain private. Passwords use salted Argon2id hashes.

GitHub Actions checks the Maven build on JDK 21/26, real MySQL workflows, campus import preservation, lecturer registration/review and upgrade preservation, HTTP authorization, shared-origin restrictions, theme persistence and desktop/mobile browser interactions. Tests use disposable schemas and do not reset an installed database.
