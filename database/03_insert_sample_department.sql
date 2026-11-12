-- STUDY / REFERENCE COMPONENT. Normal first installation uses HallSync_Full_Setup.sql.
-- Do not execute this as an extra installation step after the full installer.
-- HallSync - Step 3: insert and view a sample department.
-- Prerequisite: 02_create_department.sql has completed successfully.
-- Run the INSERT once. Repeating it creates another department row.
-- To view existing records again, execute only the SELECT statement.

USE hallsync_db;

INSERT INTO DEPARTMENT (DepartmentName)
VALUES ('Industrial Management');

SELECT DepartmentID, DepartmentName
FROM DEPARTMENT
ORDER BY DepartmentID;
