# HallSync database

**One full installer:** [HallSync_Full_Setup.sql](HallSync_Full_Setup.sql), for MySQL 8.0.46 and `hallsync_db`.

## Installation

In an administrator Workbench connection, enable **Stop Script Execution On Errors** and run the full installer once. It supports a new/empty database or the matching Department-only starting point and preserves existing department rows. It stops for inspection if the complete schema is already installed. It does not drop tables or databases.

An existing working desktop database needs only [the small read upgrade](../application/03_upgrade_desktop_reads.sql) if not already applied. Do not rerun the full installer. Then follow [the application setup](../application/README.md).

## Read the SQL in this order

The complete file has eight numbered sections:

| Section | Learn |
| --- | --- |
| 1 | Create/select the database; UTF-8 character set and collation. |
| 2 | Department: the first parent table. |
| 3 | Preflight and remaining tables; primary keys, foreign keys, UNIQUE and CHECK rules. |
| 4 | Procedures; input validation, ownership, transactions and hall-row locking. |
| 5 | Views used for booking details, equipment, status totals and scheduled usage. |
| 6 | Synthetic demo data; the seeded passwords are not usable sign-in passwords. |
| 7 | Restricted application role and procedure/view privileges. |
| 8 | Read-only object-count and integrity verification. |

The numbered `01_...sql` through `10_...sql` files are smaller reference components for studying the same ideas. They are **not additional installation steps**. The older duplicate combined installers have been removed. `08_verify_database.sql` can inspect an existing database without modifying rows.

## Expected structure

| Object | Count |
| --- | --- |
| Tables / columns | 11 / 61 |
| Primary keys / foreign keys | 11 / 14 |
| Other unique keys / CHECKs | 7 / 28 |
| Views / procedures | 4 / 27 (21 public, 6 internal) |

Pending does not reserve a hall. Independent approval rechecks activity, hall status/capacity, overlapping approvals and blocks under a hall lock. Only owners edit Pending requests. Approved moves require cancellation/new request. Rejected, Cancelled and Completed are terminal; history remains.

Lecture/Tutorial needs a course and a group. Times use Asia/Colombo (+05:30); the start timestamp must be future. Department names are required but are not unique in this schema. Application passwords are Argon2id hashes. Load real existing future reservations before trusting real university availability.

Tests use disposable schemas, never your laptop database. See [tests/README.md](tests/README.md) and [the validation snapshot](tests/database_validation.json).
