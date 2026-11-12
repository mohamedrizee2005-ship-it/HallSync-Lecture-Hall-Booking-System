# HallSync - Lecture Hall Booking System

University of Kelaniya, Faculty of Science group-project prototype using Java OOP and MySQL 8.0.46.

## Current implementation

- The database contains 11 tables, 14 foreign keys, 4 views and 25 stored procedures.
- The Java department feature lists departments, finds by ID, filters by name and adds departments after administrator sign-in.
- Each Java class has its own file. The model, repository contracts, JDBC implementations, services, authentication and console interface have separate responsibilities.
- The booking interface and the remaining Java features are later stages.

## Start

1. Follow [database/README.md](database/README.md) for a first database installation.
2. Open this entire repository folder in VS Code and install the recommended Extension Pack for Java.
3. Follow [application/README.md](application/README.md) to reuse your local connection and run the department menu.

The learning PDF is provided separately. Study documents are not stored in this repository.

## Validation

GitHub Actions builds Java on JDK 21 and 26, tests department operations and authentication against disposable MySQL 8.0.46, and runs the database rule suite. The workflows have read-only repository permission.

The database validation snapshot is [database/tests/database_validation.json](database/tests/database_validation.json).

## Scope

Reference data is synthetic. Load real future timetable reservations before trusting real hall availability. The first prototype excludes recurring booking series, automatic timetable generation, individual student enrolment and multi-stage approvals.

The console is a local learning interface. Database credentials belong to a trusted application environment; public student-facing deployment requires a backend boundary.
