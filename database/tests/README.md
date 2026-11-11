# Database validation

The read-only GitHub Actions workflow runs these checks against a disposable MySQL 8.0.46 service.

The runner creates generated `hallsync_validation_*` schemas, exercises schema constraints and workflows, and tests independent connections racing to approve or block the same hall.
It removes only the schemas and temporary login/roles created by that invocation. It never resets `hallsync_db`.

Local execution needs Python, PyMySQL, argon2-cffi and an administrator-level test-server login with CREATE DATABASE / CREATE USER / role-grant privileges.
Use an isolated MySQL instance for development checks. Supply connection values through `HALLSYNC_TEST_HOST`, `HALLSYNC_TEST_USER` and `HALLSYNC_TEST_PASSWORD`; do not save credentials in source files.

Run `python database/tests/run_checks.py` from the repository root.
The test report is written to `docs/database_validation.json`.
The tests also assemble the combined installation scripts from their individual source files.

End-user authentication is outside these SQL contract tests. The application must derive actor IDs from authenticated sessions.

The full rule suite now installs **HallSync_Full_Setup.sql** for both the department-only upgrade and the new-database scenario.
