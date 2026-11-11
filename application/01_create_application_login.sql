-- Application connection lesson: run ONCE using your local administrator.
-- Prerequisite: HallSync_Full_Setup.sql completed successfully.
-- Enable Stop Script Execution On Errors in Workbench.
-- The CREATE USER result includes a PRIVATE generated password.
-- Copy it into db.local.properties. Do not share that result grid.
-- If this login already exists, stop and inspect it instead of recreating it.

USE hallsync_db;

-- Stop before creating a login if the application role has not been installed.
SHOW GRANTS FOR 'hallsync_application';

CREATE USER 'hallsync_app'@'127.0.0.1'
    IDENTIFIED BY RANDOM PASSWORD
    REQUIRE SSL;

GRANT 'hallsync_application' TO 'hallsync_app'@'127.0.0.1';

SET DEFAULT ROLE 'hallsync_application'
    TO 'hallsync_app'@'127.0.0.1';

SHOW GRANTS FOR 'hallsync_app'@'127.0.0.1';
