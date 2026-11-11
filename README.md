# HallSync-Lecture-Hall-Booking-System

## Purpose

HallSync is a university group project to manage lecture hall availability and booking requests for the Faculty of Science, University of Kelaniya, Sri Lanka.

The project aims to prevent conflicting approved bookings, help authorised users find suitable halls, and maintain booking records. The system uses MySQL and will connect to an object-oriented application.

## Users

| User | Responsibility |
| --- | --- |
| Student | View hall details and availability. |
| Lecturer | Submit and manage their own booking requests. |
| Administrator (AdminStaff) | Manage halls and review requests; their own requests require a different administrator to review them. |

Staff privileges are provisioned by an administrator. Public registration can create only Student accounts.

## Booking workflow

1. A requester selects a hall, date and same-day times, and supplies purpose and attendance.
2. The request is recorded as Pending. Lecture/Tutorial also requires a course and student group.
3. An administrator reviews it. Approval rechecks capacity, hall status, approved reservations and active closures.
4. The approver cannot review their own request.
5. Approved, Rejected, Cancelled and Completed records retain their history.

Pending requests do not reserve a slot. Only Pending bookings can be edited; moving an Approved booking requires cancellation and a new request.

## Database implementation

Start with [the database instructions](database/README.md).

- **Your existing one-table database:** run [complete_existing_database.sql](database/complete_existing_database.sql) once.
- **New empty installation:** use [fresh_install.sql](database/fresh_install.sql) when the database name is unused.
- **Verify afterwards:** run [08_verify_database.sql](database/08_verify_database.sql).
- **Learn the design:** read [the guide](docs/Database_Guide.md) or download [the PDF](docs/HallSync_Complete_Database_Guide.pdf).
- **Check evidence:** [MySQL validation report](docs/database_validation.json).

The database package contains 11 tables, 61 columns, 14 foreign keys, 28 CHECK constraints, 4 report views and 19 public workflow procedures.
The OOP application, password verification, user sessions and interface are the next implementation stage.

## Prototype scope

This is a Faculty of Science prototype, with synthetic demonstration records rather than an official university inventory or timetable.
Existing future timetable reservations must be loaded before real availability is trusted.
The first version excludes recurring series, automatic timetabling, individual enrolment tracking and multi-stage approvals.
