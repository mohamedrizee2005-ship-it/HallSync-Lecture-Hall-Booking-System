# HallSync database

Database: `hallsync_db`. Target server: MySQL 8.0.46.
The nine-entity ER model becomes 11 tables with 61 columns and 14 foreign keys.

## One full setup file

Use **HallSync_Full_Setup.sql** for the complete setup from database creation through final verification. It supports a new database, an empty database, or the matching department-only database you already created.

1. Download and extract this repository.
2. In your existing Workbench connection, open **HallSync_Full_Setup.sql**.
3. Enable **Stop Script Execution On Errors**, clear any text selection, and execute the whole file once.
4. Refresh SCHEMAS and review the final verification grid.

This creates the database and department table when missing, preserves any existing department rows, and adds the remaining tables, workflow procedures, views, demo records and application role. It does not drop a database or table.

If execution stops, keep the error and table list. DDL is not one rollbackable batch: do not blindly rerun or reset a partly completed installation.

## Expected verification counts

| Object | Count |
| --- | --- |
| Base tables | 11 |
| Columns | 61 |
| Primary keys | 11 |
| Foreign keys | 14 |
| Other unique keys | 7 |
| CHECK constraints | 28 |
| Views | 4 |
| Procedures | 25: 19 public, 6 internal |

Use `08_verify_database.sql` to recheck metadata and existing reservations without changing data.

## Earlier lesson files

The numbered scripts and the earlier `fresh_install.sql` / `complete_existing_database.sql` profiles remain as lesson and compatibility references. Workbench users can use just **HallSync_Full_Setup.sql**.

The individual 04-10 scripts remain readable source files. They are already included in the combined continuation.
`09_workflow_example.sql` is an optional changing-data demonstration, run once after setup.

## Rules and responsibilities

- Pending requests do not reserve the hall. Approval rechecks capacity and overlaps.
- Lecturers and authorised AdminStaff request their own bookings. AdminStaff may not review their own requests.
- Students view availability; public registration fixes their role to Student.
- Only Pending bookings may be edited. Approved rescheduling means cancellation and a new request.
- Rejected, Cancelled and Completed bookings are terminal and retained.
- Booking approvals, hall blocks, edits and capacity/status changes coordinate through hall-row locks.
- Write procedures own their transactions; call outside an existing transaction.
- Every application actor ID must come from a verified login session. The database does not authenticate the human behind a supplied ID.
- Use Argon2id in the application. SQL stores the encoded hash; its prefix check is not cryptographic validation.
- Synthetic demo account passwords are random and discarded. They are for SQL demonstrations, not ready-made login credentials.
- The MySQL role denies raw table writes. Inspect any pre-existing grants before reusing a role; the grant script adds permissions rather than revoking older ones.
- Load the real future timetable before treating availability as complete.

## Documentation and tests

Read [the complete guide](../docs/Database_Guide.md) or download [the PDF](../docs/HallSync_Complete_Database_Guide.pdf).
Validation evidence is in [database_validation.json](../docs/database_validation.json).

The GitHub Actions job uses a disposable MySQL 8.0.46 service and isolated generated validation schemas.
It runs functional, rollback, privilege and independent-connection concurrency checks.
The workflow has read-only repository permission and uploads reviewable artifacts; it does not push repository changes.
