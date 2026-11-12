-- Existing working database: run only this file once as your Workbench administrator.
-- Repeatable and data-preserving. Do not rerun HallSync_Full_Setup.sql on an installed database.
-- Adds two restricted read routines. No table, row or existing booking routine is removed.
USE hallsync_db;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_list_hall_blocks$$
CREATE PROCEDURE sp_list_hall_blocks(IN p_actor INT)
SQL SECURITY DEFINER
BEGIN
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
 START TRANSACTION;
 CALL _hs_require_actor(p_actor,'ADMIN');
 SELECT x.BlockID,x.HallID,b.BuildingCode,h.HallNo,u.FullName AS RecordedByName,
        x.BlockStart,x.BlockEnd,x.Reason,x.BlockStatus
 FROM HALL_BLOCK x JOIN LECTURE_HALL h ON h.HallID=x.HallID
 JOIN BUILDING b ON b.BuildingID=h.BuildingID
 JOIN USER_ACCOUNT u ON u.AccountID=x.RecordedByID
 ORDER BY x.BlockStart DESC,x.BlockID DESC;
 COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_list_accounts$$
CREATE PROCEDURE sp_list_accounts(IN p_actor INT,IN p_department INT)
SQL SECURITY DEFINER
BEGIN
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
 START TRANSACTION;
 CALL _hs_require_actor(p_actor,'ADMIN');
 SELECT u.AccountID,u.DepartmentID,d.DepartmentName,u.FullName,u.Email,u.Username,u.Role,u.IsActive
 FROM USER_ACCOUNT u LEFT JOIN DEPARTMENT d ON d.DepartmentID=u.DepartmentID
 WHERE p_department IS NULL OR u.DepartmentID=p_department
 ORDER BY u.Username,u.AccountID;
 -- Password hashes are deliberately excluded.
 COMMIT;
END$$

DELIMITER ;
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_list_hall_blocks TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_list_accounts TO 'hallsync_application';

SELECT ROUTINE_NAME FROM information_schema.routines
WHERE routine_schema='hallsync_db' AND ROUTINE_NAME IN ('sp_list_hall_blocks','sp_list_accounts');
-- Expected: both procedure names. Existing tables, bookings and users remain in place.
