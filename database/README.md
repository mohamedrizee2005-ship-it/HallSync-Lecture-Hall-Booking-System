# HallSync database

Target: MySQL 8.0.46, database `hallsync_db`.

## First installation

Open **HallSync_Full_Setup.sql** in your administrator Workbench connection. Enable **Stop Script Execution On Errors**, clear any text selection, and execute the file once.

Supported starting points: no database, an empty database, or the matching DEPARTMENT-only database. Existing department rows are preserved. A partial or completed installation stops at preflight and requires inspection rather than a blind rerun.

Refresh SCHEMAS and check the final verification grid:

| Object | Expected count |
| --- | --- |
| Base tables | 11 |
| Columns | 61 |
| Primary keys | 11 |
| Foreign keys | 14 |
| Other unique keys | 7 |
| CHECK constraints | 28 |
| Views | 4 |
| Procedures | 25: 19 public and 6 internal |

`08_verify_database.sql` can inspect an existing installation without changing its data. MySQL DDL can commit independently; keep Action Output if setup stops.

## Application connection

The role `hallsync_application` supplies limited catalog/view reads and procedure execution. The application authenticates people separately and supplies actor IDs from verified sessions. Write procedures recheck active roles and own their transactions.

See [application/README.md](../application/README.md) for the existing local connection, VS Code and the department feature.

## Existing rules

Pending requests do not reserve a hall. Approval, hall blocks and relevant changes coordinate with hall-row locks. Administrators cannot review their own requests. Only Pending bookings can be edited; approved rescheduling requires cancellation and a new request. Rejected, Cancelled and Completed records remain terminal and retain history.

Lecture/Tutorial requires a course and student group. Capacity, future times and valid intervals are checked. Every connection uses Sri Lanka time through `+05:30`. Application user passwords use Argon2id hashes; real future timetable data must be loaded before real availability is reliable.

Department names are required and limited to 100 characters. The existing schema does not make department names unique; DepartmentID is the primary key.

## Tests and earlier sources

The numbered scripts and old combined installers remain source/reference files. The normal Workbench entry point is **HallSync_Full_Setup.sql**.

The database suite runs 100 functional, privilege, rollback and independent-connection concurrency checks on disposable schemas. Its report is [tests/database_validation.json](tests/database_validation.json). The learning PDF is distributed separately from this repository.
