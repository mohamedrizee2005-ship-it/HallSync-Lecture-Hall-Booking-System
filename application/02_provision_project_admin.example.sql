-- ONE-TIME LOCAL INITIALIZATION. Run with your Workbench administrator connection.
-- Copy to 02_provision_project_admin.local.sql before adding the private hash.
-- Generate that hash with AdminPasswordTool; keep the template in GitHub unchanged.
-- Existing full database setup and its synthetic administrator records are required.
-- Enable Stop Script Execution On Errors. Execute only this setup section once.

USE hallsync_db;

SET @setup_actor = (
    SELECT AccountID FROM USER_ACCOUNT
    WHERE Role = 'AdminStaff' AND IsActive = 1
    ORDER BY AccountID LIMIT 1
);
SET @setup_department = (
    SELECT DepartmentID FROM DEPARTMENT ORDER BY DepartmentID LIMIT 1
);

CALL sp_provision_account(
    @setup_actor,
    @setup_department,
    'HallSync Project Administrator',
    'project_admin@example.invalid',
    'project_admin',
    'PASTE_ARGON2ID_HASH_HERE',
    'AdminStaff',
    @new_account_id
);

SELECT @new_account_id AS NewApplicationAdminID;
