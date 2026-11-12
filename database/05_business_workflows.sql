-- STUDY / REFERENCE COMPONENT. Normal first installation uses HallSync_Full_Setup.sql.
-- Do not execute this as an extra installation step after the full installer.
-- Install business procedures after 04. Reinstalling procedures preserves table data.
-- Write procedures own their transaction; call outside an existing transaction.
-- Actor IDs must come from an authenticated application session, not a form.
USE hallsync_db;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET SESSION time_zone = '+05:30';
SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

DELIMITER $$

DROP PROCEDURE IF EXISTS _hs_require_actor$$
CREATE PROCEDURE _hs_require_actor(IN p_actor INT, IN p_permission VARCHAR(10))
SQL SECURITY DEFINER
BEGIN

DECLARE v_role VARCHAR(20) DEFAULT NULL;
DECLARE v_active BOOLEAN DEFAULT FALSE;
DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_role=NULL;
SELECT Role,IsActive INTO v_role,v_active FROM USER_ACCOUNT WHERE AccountID=p_actor FOR SHARE;
IF v_role IS NULL OR NOT v_active THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='An active authenticated account is required.'; END IF;
IF p_permission='ADMIN' AND v_role<>'AdminStaff' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Only an administrator may perform this action.'; END IF;
IF p_permission='REQUEST' AND v_role NOT IN ('Lecturer','AdminStaff') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Only lecturers or authorised staff may request bookings.'; END IF;

END$$

DROP PROCEDURE IF EXISTS _hs_lock_hall$$
CREATE PROCEDURE _hs_lock_hall(IN p_hall INT, OUT p_capacity INT, OUT p_status VARCHAR(10))
SQL SECURITY DEFINER
BEGIN

DECLARE CONTINUE HANDLER FOR NOT FOUND SET p_capacity=NULL;
SET p_capacity=NULL;
SELECT Capacity,OperationalStatus INTO p_capacity,p_status FROM LECTURE_HALL WHERE HallID=p_hall FOR UPDATE;
IF p_capacity IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Lecture hall not found.'; END IF;

END$$

DROP PROCEDURE IF EXISTS _hs_validate_details$$
CREATE PROCEDURE _hs_validate_details(IN p_date DATE,IN p_start TIME,IN p_end TIME,IN p_purpose TEXT,IN p_type VARCHAR(20),IN p_attendance DECIMAL(20,6),IN p_course INT,IN p_groups JSON)
SQL SECURITY DEFINER
BEGIN

IF p_date IS NULL OR p_start IS NULL OR p_end IS NULL OR p_start<'00:00:00' OR p_end>='24:00:00' OR p_end<=p_start THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Use same-day clock times with start before end.'; END IF;
IF TIMESTAMP(p_date,p_start)<=CURRENT_TIMESTAMP THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='The booking start must be in the future in Asia/Colombo.'; END IF;
IF p_purpose IS NULL OR CHAR_LENGTH(TRIM(p_purpose)) NOT BETWEEN 1 AND 255 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Purpose needs 1 to 255 characters after trimming.'; END IF;
IF p_attendance IS NULL OR p_attendance<=0 OR p_attendance<>FLOOR(p_attendance) OR p_attendance>2147483647 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected attendance must be positive.'; END IF;
IF p_type IS NULL OR p_type NOT IN ('Lecture','Tutorial','Exam','Event') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unknown booking type.'; END IF;
IF p_groups IS NULL OR JSON_TYPE(p_groups)<>'ARRAY' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Group IDs must be a JSON array; use JSON_ARRAY() when empty.'; END IF;
IF p_type IN ('Lecture','Tutorial') AND (p_course IS NULL OR JSON_LENGTH(p_groups)=0) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Lectures and tutorials require a course and at least one group.'; END IF;
IF p_course IS NOT NULL AND NOT EXISTS(SELECT 1 FROM COURSE_UNIT WHERE CourseID=p_course) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Course unit not found.'; END IF;
IF EXISTS(SELECT 1 FROM JSON_TABLE(p_groups,'$[*]' COLUMNS(ordinal_number FOR ORDINALITY,gid BIGINT PATH '$' ERROR ON EMPTY ERROR ON ERROR)) j WHERE j.gid IS NULL OR j.gid NOT BETWEEN 1 AND 2147483647 OR JSON_TYPE(JSON_EXTRACT(p_groups,CONCAT('$[',j.ordinal_number-1,']')))<>'INTEGER') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Each group ID must be a positive integer JSON number.'; END IF;
IF (SELECT COUNT(DISTINCT j.gid) FROM JSON_TABLE(p_groups,'$[*]' COLUMNS(gid INT PATH '$')) j)<>JSON_LENGTH(p_groups) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Group IDs must not be repeated.'; END IF;
IF EXISTS(SELECT 1 FROM JSON_TABLE(p_groups,'$[*]' COLUMNS(gid INT PATH '$')) j LEFT JOIN STUDENT_GROUP g ON g.GroupID=j.gid WHERE g.GroupID IS NULL) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A selected student group does not exist.'; END IF;

END$$

DROP PROCEDURE IF EXISTS _hs_check_slot$$
CREATE PROCEDURE _hs_check_slot(IN p_hall INT,IN p_date DATE,IN p_start TIME,IN p_end TIME,IN p_attendance DECIMAL(20,6),IN p_exclude INT)
SQL SECURITY DEFINER
BEGIN

DECLARE v_capacity INT;
DECLARE v_status VARCHAR(10);
-- Caller holds this hall row lock in the same transaction.
SELECT Capacity,OperationalStatus INTO v_capacity,v_status FROM LECTURE_HALL WHERE HallID=p_hall;
IF v_status<>'Active' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='The hall is inactive.'; END IF;
IF p_attendance>v_capacity THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected attendance exceeds hall capacity.'; END IF;
IF EXISTS(SELECT 1 FROM BOOKING WHERE HallID=p_hall AND BookingDate=p_date AND BookingStatus='Approved' AND (p_exclude IS NULL OR BookingID<>p_exclude) AND p_start<EndTime AND p_end>StartTime) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Conflicts with an approved booking.'; END IF;
IF EXISTS(SELECT 1 FROM HALL_BLOCK WHERE HallID=p_hall AND BlockStatus='Active' AND BlockStart<TIMESTAMP(p_date,p_end) AND BlockEnd>TIMESTAMP(p_date,p_start)) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Conflicts with an active hall block.'; END IF;

END$$

DROP PROCEDURE IF EXISTS _hs_insert_groups$$
CREATE PROCEDURE _hs_insert_groups(IN p_booking INT,IN p_groups JSON)
SQL SECURITY DEFINER
BEGIN

INSERT INTO BOOKING_GROUP(BookingID,GroupID) SELECT p_booking,j.gid FROM JSON_TABLE(p_groups,'$[*]' COLUMNS(gid INT PATH '$')) j;

END$$

DROP PROCEDURE IF EXISTS _hs_validate_account$$
CREATE PROCEDURE _hs_validate_account(IN p_name TEXT,IN p_email TEXT,IN p_username TEXT,IN p_hash TEXT)
SQL SECURITY DEFINER
BEGIN

IF p_name IS NULL OR CHAR_LENGTH(TRIM(p_name)) NOT BETWEEN 1 AND 150 OR p_email IS NULL OR CHAR_LENGTH(TRIM(p_email)) NOT BETWEEN 3 AND 254 OR p_username IS NULL OR CHAR_LENGTH(TRIM(p_username)) NOT BETWEEN 1 AND 50 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid account name, email or username length.'; END IF;
IF p_hash IS NULL OR CHAR_LENGTH(p_hash)>255 OR p_hash NOT LIKE '$argon2id$%' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Supply an encoded Argon2id hash from a password library.'; END IF;
-- Prefix validation is not cryptographic verification or a password-length check.

END$$

DROP PROCEDURE IF EXISTS sp_create_booking$$
CREATE PROCEDURE sp_create_booking(IN p_actor INT,IN p_hall INT,IN p_course INT,IN p_date DATE,IN p_start TIME,IN p_end TIME,IN p_purpose TEXT,IN p_type VARCHAR(20),IN p_attendance DECIMAL(20,6),IN p_groups JSON,OUT p_booking_id INT)
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN

DECLARE v_capacity INT;
DECLARE v_status VARCHAR(10);
CALL _hs_lock_hall(p_hall,v_capacity,v_status);
CALL _hs_require_actor(p_actor,'REQUEST');
CALL _hs_validate_details(p_date,p_start,p_end,p_purpose,p_type,p_attendance,p_course,p_groups);
CALL _hs_check_slot(p_hall,p_date,p_start,p_end,p_attendance,NULL);
INSERT INTO BOOKING(RequesterID,HallID,CourseID,BookingDate,StartTime,EndTime,Purpose,BookingType,ExpectedAttendance)
VALUES(p_actor,p_hall,p_course,p_date,p_start,p_end,TRIM(p_purpose),p_type,p_attendance);
SET p_booking_id=LAST_INSERT_ID();
CALL _hs_insert_groups(p_booking_id,p_groups);

END;
COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_update_pending_booking$$
CREATE PROCEDURE sp_update_pending_booking(IN p_actor INT,IN p_booking INT,IN p_hall INT,IN p_course INT,IN p_date DATE,IN p_start TIME,IN p_end TIME,IN p_purpose TEXT,IN p_type VARCHAR(20),IN p_attendance DECIMAL(20,6),IN p_groups JSON)
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN

DECLARE v_old INT DEFAULT NULL; DECLARE v_current INT DEFAULT NULL; DECLARE v_owner INT;
DECLARE v_state VARCHAR(10); DECLARE v_cap INT; DECLARE v_hs VARCHAR(10);
SELECT HallID INTO v_old FROM BOOKING WHERE BookingID=p_booking;
IF v_old IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Booking not found.'; END IF;
-- Moves lock both halls in ascending ID order.
CALL _hs_lock_hall(LEAST(v_old,p_hall),v_cap,v_hs);
IF v_old<>p_hall THEN CALL _hs_lock_hall(GREATEST(v_old,p_hall),v_cap,v_hs); END IF;
SELECT HallID,RequesterID,BookingStatus INTO v_current,v_owner,v_state FROM BOOKING WHERE BookingID=p_booking FOR UPDATE;
IF v_current IS NULL OR v_current<>v_old THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Booking moved concurrently. Reload and retry.'; END IF;
CALL _hs_require_actor(p_actor,'REQUEST');
IF p_actor<>v_owner OR v_state<>'Pending' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Only the requester may edit their Pending booking.'; END IF;
CALL _hs_validate_details(p_date,p_start,p_end,p_purpose,p_type,p_attendance,p_course,p_groups);
CALL _hs_check_slot(p_hall,p_date,p_start,p_end,p_attendance,p_booking);
UPDATE BOOKING SET HallID=p_hall,CourseID=p_course,BookingDate=p_date,StartTime=p_start,EndTime=p_end,Purpose=TRIM(p_purpose),BookingType=p_type,ExpectedAttendance=p_attendance WHERE BookingID=p_booking;
DELETE FROM BOOKING_GROUP WHERE BookingID=p_booking;
CALL _hs_insert_groups(p_booking,p_groups);

END;
COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_review_booking$$
CREATE PROCEDURE sp_review_booking(IN p_actor INT,IN p_booking INT,IN p_decision VARCHAR(10),IN p_note TEXT)
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN

DECLARE v_hall INT DEFAULT NULL; DECLARE v_current INT DEFAULT NULL; DECLARE v_owner INT;
DECLARE v_state VARCHAR(10); DECLARE v_cap INT; DECLARE v_hs VARCHAR(10);
DECLARE v_date DATE; DECLARE v_start TIME; DECLARE v_end TIME; DECLARE v_att INT;
DECLARE v_course INT; DECLARE v_type VARCHAR(20); DECLARE v_purpose VARCHAR(255); DECLARE v_groups JSON;
SELECT HallID INTO v_hall FROM BOOKING WHERE BookingID=p_booking;
IF v_hall IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Booking not found.'; END IF;
CALL _hs_lock_hall(v_hall,v_cap,v_hs);
SELECT HallID,RequesterID,BookingStatus,BookingDate,StartTime,EndTime,ExpectedAttendance,CourseID,BookingType,Purpose INTO v_current,v_owner,v_state,v_date,v_start,v_end,v_att,v_course,v_type,v_purpose FROM BOOKING WHERE BookingID=p_booking FOR UPDATE;
IF v_current IS NULL OR v_current<>v_hall THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Booking moved concurrently. Reload and retry.'; END IF;
CALL _hs_require_actor(p_actor,'ADMIN');
IF p_actor=v_owner THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='An approver must not review their own booking.'; END IF;
IF v_state<>'Pending' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Only Pending bookings can be reviewed.'; END IF;
IF p_decision IS NULL OR p_decision NOT IN ('Approved','Rejected') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Decision must be Approved or Rejected.'; END IF;
IF p_note IS NOT NULL AND CHAR_LENGTH(p_note)>500 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Review note exceeds 500 characters.'; END IF;
IF p_decision='Approved' THEN
 CALL _hs_require_actor(v_owner,'REQUEST');
 SELECT COALESCE(JSON_ARRAYAGG(GroupID),JSON_ARRAY()) INTO v_groups FROM BOOKING_GROUP WHERE BookingID=p_booking;
 CALL _hs_validate_details(v_date,v_start,v_end,v_purpose,v_type,v_att,v_course,v_groups);
 CALL _hs_check_slot(v_hall,v_date,v_start,v_end,v_att,p_booking);
END IF;
UPDATE BOOKING SET BookingStatus=p_decision,ReviewerID=p_actor,Decision=p_decision,ReviewedAt=CURRENT_TIMESTAMP,ReviewNote=p_note WHERE BookingID=p_booking;

END;
COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_cancel_booking$$
CREATE PROCEDURE sp_cancel_booking(IN p_actor INT,IN p_booking INT)
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN

DECLARE v_hall INT DEFAULT NULL; DECLARE v_current INT DEFAULT NULL; DECLARE v_owner INT;
DECLARE v_state VARCHAR(10); DECLARE v_role VARCHAR(20); DECLARE v_date DATE; DECLARE v_start TIME;
DECLARE v_cap INT; DECLARE v_hs VARCHAR(10);
SELECT HallID INTO v_hall FROM BOOKING WHERE BookingID=p_booking;
IF v_hall IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Booking not found.'; END IF;
CALL _hs_lock_hall(v_hall,v_cap,v_hs);
SELECT HallID,RequesterID,BookingStatus,BookingDate,StartTime INTO v_current,v_owner,v_state,v_date,v_start FROM BOOKING WHERE BookingID=p_booking FOR UPDATE;
IF v_current IS NULL OR v_current<>v_hall THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Booking moved concurrently. Reload and retry.'; END IF;
CALL _hs_require_actor(p_actor,'ACTIVE');
SELECT Role INTO v_role FROM USER_ACCOUNT WHERE AccountID=p_actor;
IF p_actor<>v_owner AND v_role<>'AdminStaff' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Only the requester or an administrator may cancel.'; END IF;
IF v_state NOT IN ('Pending','Approved') OR TIMESTAMP(v_date,v_start)<=CURRENT_TIMESTAMP THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Only future Pending or Approved bookings may be cancelled.'; END IF;
UPDATE BOOKING SET BookingStatus='Cancelled' WHERE BookingID=p_booking;
-- Preserve the earlier approval, if any, and all teaching-group history.

END;
COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_complete_booking$$
CREATE PROCEDURE sp_complete_booking(IN p_actor INT,IN p_booking INT)
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN

DECLARE v_hall INT DEFAULT NULL; DECLARE v_current INT DEFAULT NULL; DECLARE v_state VARCHAR(10);
DECLARE v_date DATE; DECLARE v_end TIME; DECLARE v_cap INT; DECLARE v_hs VARCHAR(10);
SELECT HallID INTO v_hall FROM BOOKING WHERE BookingID=p_booking;
IF v_hall IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Booking not found.'; END IF;
CALL _hs_lock_hall(v_hall,v_cap,v_hs);
SELECT HallID,BookingStatus,BookingDate,EndTime INTO v_current,v_state,v_date,v_end FROM BOOKING WHERE BookingID=p_booking FOR UPDATE;
IF v_current IS NULL OR v_current<>v_hall THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Booking moved concurrently. Reload and retry.'; END IF;
CALL _hs_require_actor(p_actor,'ADMIN');
IF v_state<>'Approved' OR TIMESTAMP(v_date,v_end)>CURRENT_TIMESTAMP THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Only an ended Approved booking may become Completed.'; END IF;
UPDATE BOOKING SET BookingStatus='Completed' WHERE BookingID=p_booking;

END;
COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_create_hall_block$$
CREATE PROCEDURE sp_create_hall_block(IN p_actor INT,IN p_hall INT,IN p_start DATETIME,IN p_end DATETIME,IN p_reason TEXT,OUT p_block_id INT)
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN

DECLARE v_cap INT; DECLARE v_hs VARCHAR(10);
CALL _hs_lock_hall(p_hall,v_cap,v_hs);
CALL _hs_require_actor(p_actor,'ADMIN');
IF p_start IS NULL OR p_end IS NULL OR p_end<=p_start OR p_end<=CURRENT_TIMESTAMP THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Use a valid block interval ending in the future.'; END IF;
IF p_reason IS NULL OR CHAR_LENGTH(TRIM(p_reason)) NOT BETWEEN 1 AND 255 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A block reason needs 1 to 255 characters.'; END IF;
IF EXISTS(SELECT 1 FROM BOOKING WHERE HallID=p_hall AND BookingStatus='Approved' AND TIMESTAMP(BookingDate,StartTime)<p_end AND TIMESTAMP(BookingDate,EndTime)>p_start) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Resolve approved bookings explicitly before creating this block.'; END IF;
INSERT INTO HALL_BLOCK(HallID,RecordedByID,BlockStart,BlockEnd,Reason) VALUES(p_hall,p_actor,p_start,p_end,TRIM(p_reason));
SET p_block_id=LAST_INSERT_ID();

END;
COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_cancel_hall_block$$
CREATE PROCEDURE sp_cancel_hall_block(IN p_actor INT,IN p_block INT)
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN

DECLARE v_hall INT DEFAULT NULL; DECLARE v_state VARCHAR(10); DECLARE v_cap INT; DECLARE v_hs VARCHAR(10);
SELECT HallID INTO v_hall FROM HALL_BLOCK WHERE BlockID=p_block;
IF v_hall IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Hall block not found.'; END IF;
CALL _hs_lock_hall(v_hall,v_cap,v_hs);
SELECT BlockStatus INTO v_state FROM HALL_BLOCK WHERE BlockID=p_block FOR UPDATE;
CALL _hs_require_actor(p_actor,'ADMIN');
IF v_state<>'Active' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='The hall block is already cancelled.'; END IF;
UPDATE HALL_BLOCK SET BlockStatus='Cancelled' WHERE BlockID=p_block;

END;
COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_update_hall$$
CREATE PROCEDURE sp_update_hall(IN p_actor INT,IN p_hall INT,IN p_capacity INT,IN p_status VARCHAR(10))
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN

DECLARE v_cap INT; DECLARE v_hs VARCHAR(10);
CALL _hs_lock_hall(p_hall,v_cap,v_hs);
CALL _hs_require_actor(p_actor,'ADMIN');
IF p_capacity IS NULL OR p_capacity<=0 OR p_status IS NULL OR p_status NOT IN ('Active','Inactive') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Use positive capacity and Active or Inactive status.'; END IF;
IF EXISTS(SELECT 1 FROM BOOKING WHERE HallID=p_hall AND BookingStatus='Approved' AND TIMESTAMP(BookingDate,EndTime)>CURRENT_TIMESTAMP AND (p_status='Inactive' OR ExpectedAttendance>p_capacity)) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='This change would invalidate an approved reservation.'; END IF;
UPDATE LECTURE_HALL SET Capacity=p_capacity,OperationalStatus=p_status WHERE HallID=p_hall;

END;
COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_set_hall_facility$$
CREATE PROCEDURE sp_set_hall_facility(IN p_actor INT,IN p_hall INT,IN p_facility INT,IN p_quantity INT,IN p_working INT)
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN

DECLARE v_cap INT; DECLARE v_hs VARCHAR(10);
CALL _hs_lock_hall(p_hall,v_cap,v_hs);
CALL _hs_require_actor(p_actor,'ADMIN');
IF p_quantity IS NULL OR p_quantity<=0 OR p_working IS NULL OR p_working<0 OR p_working>p_quantity THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Require quantity > 0 and 0 <= working quantity <= quantity.'; END IF;
INSERT INTO HALL_FACILITY(HallID,FacilityID,Quantity,WorkingQuantity) VALUES(p_hall,p_facility,p_quantity,p_working)
ON DUPLICATE KEY UPDATE Quantity=p_quantity,WorkingQuantity=p_working;

END;
COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_register_student$$
CREATE PROCEDURE sp_register_student(IN p_department INT,IN p_name TEXT,IN p_email TEXT,IN p_username TEXT,IN p_password_hash TEXT,OUT p_account_id INT)
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN

CALL _hs_validate_account(p_name,p_email,p_username,p_password_hash);
INSERT INTO USER_ACCOUNT(DepartmentID,FullName,Email,Username,PasswordHash,Role)
VALUES(p_department,TRIM(p_name),TRIM(p_email),TRIM(p_username),p_password_hash,'Student');
SET p_account_id=LAST_INSERT_ID();

END;
COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_provision_account$$
CREATE PROCEDURE sp_provision_account(IN p_actor INT,IN p_department INT,IN p_name TEXT,IN p_email TEXT,IN p_username TEXT,IN p_password_hash TEXT,IN p_role VARCHAR(20),OUT p_account_id INT)
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN

CALL _hs_require_actor(p_actor,'ADMIN');
CALL _hs_validate_account(p_name,p_email,p_username,p_password_hash);
IF p_role IS NULL OR p_role NOT IN ('Student','Lecturer','AdminStaff') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unknown account role.'; END IF;
INSERT INTO USER_ACCOUNT(DepartmentID,FullName,Email,Username,PasswordHash,Role)
VALUES(p_department,TRIM(p_name),TRIM(p_email),TRIM(p_username),p_password_hash,p_role);
SET p_account_id=LAST_INSERT_ID();

END;
COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_get_login_record$$
CREATE PROCEDURE sp_get_login_record(IN p_username VARCHAR(50))
SQL SECURITY DEFINER
BEGIN

SELECT AccountID,Username,PasswordHash,Role,IsActive FROM USER_ACCOUNT WHERE Username=TRIM(p_username);
-- Application verifies the password and creates the session. Never expose this record publicly.

END$$

DROP PROCEDURE IF EXISTS sp_find_available_halls$$
CREATE PROCEDURE sp_find_available_halls(IN p_date DATE,IN p_start TIME,IN p_end TIME,IN p_attendance DECIMAL(20,6))
SQL SECURITY DEFINER
BEGIN

SET SESSION time_zone='+05:30';
IF p_date IS NULL OR p_start IS NULL OR p_end IS NULL OR p_start<'00:00:00' OR p_end>='24:00:00' OR p_end<=p_start OR p_attendance IS NULL OR p_attendance<=0 OR p_attendance<>FLOOR(p_attendance) OR p_attendance>2147483647 OR TIMESTAMP(p_date,p_start)<=CURRENT_TIMESTAMP THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Use valid future same-day times and positive attendance.'; END IF;
SELECT h.HallID,b.BuildingCode,b.BuildingName,h.HallNo,h.Floor,h.Capacity
FROM LECTURE_HALL h JOIN BUILDING b ON b.BuildingID=h.BuildingID
WHERE h.OperationalStatus='Active' AND h.Capacity>=p_attendance
AND NOT EXISTS(SELECT 1 FROM BOOKING k WHERE k.HallID=h.HallID AND k.BookingDate=p_date AND k.BookingStatus='Approved' AND p_start<k.EndTime AND p_end>k.StartTime)
AND NOT EXISTS(SELECT 1 FROM HALL_BLOCK x WHERE x.HallID=h.HallID AND x.BlockStatus='Active' AND x.BlockStart<TIMESTAMP(p_date,p_end) AND x.BlockEnd>TIMESTAMP(p_date,p_start))
ORDER BY h.Capacity,h.HallID;
-- This is a snapshot, not a reservation. Approval checks again under a lock.

END$$

DROP PROCEDURE IF EXISTS sp_add_department$$
CREATE PROCEDURE sp_add_department(IN p_actor INT,IN p_name VARCHAR(100),OUT p_id INT)
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN
CALL _hs_require_actor(p_actor,'ADMIN');
INSERT INTO DEPARTMENT(DepartmentName) VALUES(TRIM(p_name));
SET p_id=LAST_INSERT_ID();
END;
COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_add_building$$
CREATE PROCEDURE sp_add_building(IN p_actor INT,IN p_code VARCHAR(20),IN p_name VARCHAR(100),IN p_location VARCHAR(255),OUT p_id INT)
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN
CALL _hs_require_actor(p_actor,'ADMIN');
INSERT INTO BUILDING(BuildingCode,BuildingName,LocationDescription) VALUES(TRIM(p_code),TRIM(p_name),p_location);
SET p_id=LAST_INSERT_ID();
END;
COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_add_facility$$
CREATE PROCEDURE sp_add_facility(IN p_actor INT,IN p_name VARCHAR(100),IN p_description VARCHAR(255),OUT p_id INT)
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN
CALL _hs_require_actor(p_actor,'ADMIN');
INSERT INTO FACILITY(FacilityName,Description) VALUES(TRIM(p_name),p_description);
SET p_id=LAST_INSERT_ID();
END;
COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_add_course$$
CREATE PROCEDURE sp_add_course(IN p_actor INT,IN p_department INT,IN p_code VARCHAR(20),IN p_title VARCHAR(150),OUT p_id INT)
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN
CALL _hs_require_actor(p_actor,'ADMIN');
INSERT INTO COURSE_UNIT(DepartmentID,CourseCode,CourseTitle) VALUES(p_department,TRIM(p_code),TRIM(p_title));
SET p_id=LAST_INSERT_ID();
END;
COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_add_student_group$$
CREATE PROCEDURE sp_add_student_group(IN p_actor INT,IN p_department INT,IN p_name VARCHAR(100),IN p_intake SMALLINT UNSIGNED,IN p_year TINYINT UNSIGNED,OUT p_id INT)
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN
CALL _hs_require_actor(p_actor,'ADMIN');
INSERT INTO STUDENT_GROUP(DepartmentID,GroupName,IntakeYear,StudyYear) VALUES(p_department,TRIM(p_name),p_intake,p_year);
SET p_id=LAST_INSERT_ID();
END;
COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_add_hall$$
CREATE PROCEDURE sp_add_hall(IN p_actor INT,IN p_building INT,IN p_number VARCHAR(20),IN p_floor VARCHAR(10),IN p_capacity INT,OUT p_id INT)
SQL SECURITY DEFINER
BEGIN
DECLARE v_started BOOLEAN DEFAULT FALSE;
DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
SET SESSION time_zone='+05:30';
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SET v_started=TRUE;
BEGIN
CALL _hs_require_actor(p_actor,'ADMIN');
INSERT INTO LECTURE_HALL(BuildingID,HallNo,Floor,Capacity) VALUES(p_building,TRIM(p_number),p_floor,p_capacity);
SET p_id=LAST_INSERT_ID();
END;
COMMIT;
END$$

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
