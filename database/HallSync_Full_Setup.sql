-- HallSync_Full_Setup.sql
-- Complete setup for MySQL 8.0.46: database, 12 tables, workflows, views,
-- demo records, application role and final verification.
-- Run this whole file ONCE with Stop Script Execution On Errors enabled.
-- Use a local MySQL administrator connection.
--
-- Supported starting points:
--   1. No hallsync_db yet.
--   2. An empty hallsync_db.
--   3. hallsync_db containing only the matching DEPARTMENT table and its data.
-- Existing department rows are preserved. No database or table is dropped.
-- If other tables already exist, the preflight stops for inspection.
-- Demo account hashes have random discarded passwords, not usable login passwords.
--
-- SECTION 1: CREATE / SELECT THE DATABASE
CREATE DATABASE IF NOT EXISTS hallsync_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE hallsync_db;

SELECT DATABASE() AS CurrentDatabase;

-- SECTION 2: CREATE THE DEPARTMENT TABLE WHEN MISSING
CREATE TABLE IF NOT EXISTS DEPARTMENT (
    DepartmentID INT NOT NULL AUTO_INCREMENT,
    DepartmentName VARCHAR(100) NOT NULL,
    PRIMARY KEY (DepartmentID),
    CONSTRAINT ck_department_departmentname
        CHECK (CHAR_LENGTH(TRIM(DepartmentName)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- SECTION 3: VALIDATE THE STARTING STRUCTURE AND COMPLETE THE DATABASE
USE hallsync_db;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET SESSION time_zone = '+05:30';
SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

DELIMITER $$
DROP PROCEDURE IF EXISTS _hs_schema_preflight$$
CREATE PROCEDURE _hs_schema_preflight()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name=DATABASE() AND default_character_set_name='utf8mb4' AND default_collation_name='utf8mb4_0900_ai_ci') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing database must use utf8mb4 / utf8mb4_0900_ai_ci.';
 END IF;
 IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE')<>1
 OR NOT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND LOWER(table_name)='department' AND engine='InnoDB') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected only DEPARTMENT. Stop and inspect; do not reset.';
 END IF;
 IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND LOWER(table_name)='department')<>2
 OR NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND LOWER(table_name)='department' AND column_name='DepartmentID' AND column_type='int' AND is_nullable='NO' AND column_key='PRI' AND extra LIKE '%auto_increment%')
 OR NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND LOWER(table_name)='department' AND column_name='DepartmentName' AND data_type='varchar' AND character_maximum_length=100 AND is_nullable='NO' AND character_set_name='utf8mb4' AND collation_name='utf8mb4_0900_ai_ci') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing DEPARTMENT does not match this setup definition.';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND LOWER(table_name)='department' AND constraint_name='ck_department_departmentname' AND constraint_type='CHECK' AND enforced='YES') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing DEPARTMENT lacks the required enforced name CHECK.';
 END IF;
END$$
CALL _hs_schema_preflight()$$
DROP PROCEDURE _hs_schema_preflight$$
DELIMITER ;
-- 2. BUILDING
CREATE TABLE BUILDING (
    BuildingID INT NOT NULL AUTO_INCREMENT,
    BuildingCode VARCHAR(20) NOT NULL,
    BuildingName VARCHAR(100) NOT NULL,
    LocationDescription VARCHAR(255) NULL,
    PRIMARY KEY (BuildingID),
    CONSTRAINT uq_building_code UNIQUE
        (BuildingCode),
    CONSTRAINT ck_building_buildingcode CHECK (
        CHAR_LENGTH(TRIM(BuildingCode)) > 0
    ),
    CONSTRAINT ck_building_buildingname CHECK (
        CHAR_LENGTH(TRIM(BuildingName)) > 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. FACILITY
CREATE TABLE FACILITY (
    FacilityID INT NOT NULL AUTO_INCREMENT,
    FacilityName VARCHAR(100) NOT NULL,
    Description VARCHAR(255) NULL,
    PRIMARY KEY (FacilityID),
    CONSTRAINT uq_facility_name UNIQUE
        (FacilityName),
    CONSTRAINT ck_facility_facilityname CHECK (
        CHAR_LENGTH(TRIM(FacilityName)) > 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. USER_ACCOUNT
CREATE TABLE USER_ACCOUNT (
    AccountID INT NOT NULL AUTO_INCREMENT,
    DepartmentID INT NULL,
    FullName VARCHAR(150) NOT NULL,
    Email VARCHAR(254) NOT NULL,
    Username VARCHAR(50) NOT NULL,
    PasswordHash VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    Role ENUM('Student', 'Lecturer', 'AdminStaff') NOT NULL DEFAULT 'Student',
    IsActive BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (AccountID),
    CONSTRAINT uq_user_email UNIQUE
        (Email),
    CONSTRAINT uq_user_username UNIQUE
        (Username),
    CONSTRAINT fk_user_account_departmentid FOREIGN KEY (DepartmentID)
        REFERENCES DEPARTMENT (DepartmentID),
    CONSTRAINT ck_user_account_fullname CHECK (
        CHAR_LENGTH(TRIM(FullName)) > 0
    ),
    CONSTRAINT ck_user_account_email CHECK (
        CHAR_LENGTH(TRIM(Email)) > 0
    ),
    CONSTRAINT ck_user_account_username CHECK (
        CHAR_LENGTH(TRIM(Username)) > 0
    ),
    CONSTRAINT ck_user_account_passwordhash CHECK (
        CHAR_LENGTH(TRIM(PasswordHash)) > 0
    ),
    CONSTRAINT ck_user_account_active CHECK (
        IsActive IN (0,1)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 5. COURSE_UNIT
CREATE TABLE COURSE_UNIT (
    CourseID INT NOT NULL AUTO_INCREMENT,
    DepartmentID INT NOT NULL,
    CourseCode VARCHAR(20) NOT NULL,
    CourseTitle VARCHAR(150) NOT NULL,
    PRIMARY KEY (CourseID),
    CONSTRAINT uq_course_code UNIQUE
        (CourseCode),
    CONSTRAINT fk_course_unit_departmentid FOREIGN KEY (DepartmentID)
        REFERENCES DEPARTMENT (DepartmentID),
    CONSTRAINT ck_course_unit_coursecode CHECK (
        CHAR_LENGTH(TRIM(CourseCode)) > 0
    ),
    CONSTRAINT ck_course_unit_coursetitle CHECK (
        CHAR_LENGTH(TRIM(CourseTitle)) > 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 6. STUDENT_GROUP
CREATE TABLE STUDENT_GROUP (
    GroupID INT NOT NULL AUTO_INCREMENT,
    DepartmentID INT NOT NULL,
    GroupName VARCHAR(100) NOT NULL,
    IntakeYear SMALLINT UNSIGNED NOT NULL,
    StudyYear TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (GroupID),
    CONSTRAINT uq_group_identity UNIQUE
        (DepartmentID, IntakeYear, GroupName),
    CONSTRAINT fk_student_group_departmentid FOREIGN KEY (DepartmentID)
        REFERENCES DEPARTMENT (DepartmentID),
    CONSTRAINT ck_student_group_groupname CHECK (
        CHAR_LENGTH(TRIM(GroupName)) > 0
    ),
    CONSTRAINT ck_student_group_intake CHECK (
        IntakeYear BETWEEN 1900 AND 9999
    ),
    CONSTRAINT ck_student_group_level CHECK (
        StudyYear >= 1
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 7. LECTURE_HALL
CREATE TABLE LECTURE_HALL (
    HallID INT NOT NULL AUTO_INCREMENT,
    BuildingID INT NOT NULL,
    HallNo VARCHAR(20) NOT NULL,
    Floor VARCHAR(10) NULL,
    Capacity INT NOT NULL,
    OperationalStatus ENUM('Active', 'Inactive') NOT NULL DEFAULT 'Active',
    PRIMARY KEY (HallID),
    CONSTRAINT uq_hall_building_number UNIQUE
        (BuildingID, HallNo),
    CONSTRAINT fk_lecture_hall_buildingid FOREIGN KEY (BuildingID)
        REFERENCES BUILDING (BuildingID),
    CONSTRAINT ck_lecture_hall_hallno CHECK (
        CHAR_LENGTH(TRIM(HallNo)) > 0
    ),
    CONSTRAINT ck_lecture_hall_capacity CHECK (
        Capacity > 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 8. BOOKING
CREATE TABLE BOOKING (
    BookingID INT NOT NULL AUTO_INCREMENT,
    RequesterID INT NOT NULL,
    HallID INT NOT NULL,
    CourseID INT NULL,
    BookingDate DATE NOT NULL,
    StartTime TIME NOT NULL,
    EndTime TIME NOT NULL,
    Purpose VARCHAR(255) NOT NULL,
    BookingType ENUM('Lecture', 'Tutorial', 'Exam', 'Event') NOT NULL,
    ExpectedAttendance INT NOT NULL,
    BookingStatus ENUM(
        'Pending', 'Approved', 'Rejected', 'Cancelled', 'Completed'
    ) NOT NULL DEFAULT 'Pending',
    RequestedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ReviewerID INT NULL,
    Decision ENUM('Approved', 'Rejected') NULL,
    ReviewedAt DATETIME NULL,
    ReviewNote VARCHAR(500) NULL,
    PRIMARY KEY (BookingID),
    CONSTRAINT fk_booking_requesterid FOREIGN KEY (RequesterID)
        REFERENCES USER_ACCOUNT (AccountID),
    CONSTRAINT fk_booking_hallid FOREIGN KEY (HallID)
        REFERENCES LECTURE_HALL (HallID),
    CONSTRAINT fk_booking_courseid FOREIGN KEY (CourseID)
        REFERENCES COURSE_UNIT (CourseID),
    CONSTRAINT fk_booking_reviewerid FOREIGN KEY (ReviewerID)
        REFERENCES USER_ACCOUNT (AccountID),
    CONSTRAINT ck_booking_purpose CHECK (
        CHAR_LENGTH(TRIM(Purpose)) > 0
    ),
    CONSTRAINT ck_booking_attendance CHECK (
        ExpectedAttendance > 0
    ),
    CONSTRAINT ck_booking_clock CHECK (
        StartTime >= '00:00:00' AND StartTime < '24:00:00'
        AND EndTime >= '00:00:00' AND EndTime < '24:00:00'
        AND EndTime > StartTime
    ),
    CONSTRAINT ck_booking_course CHECK (
        BookingType NOT IN ('Lecture','Tutorial')
        OR CourseID IS NOT NULL
    ),
    CONSTRAINT ck_booking_review_bundle CHECK (
        (ReviewerID IS NULL AND Decision IS NULL
         AND ReviewedAt IS NULL AND ReviewNote IS NULL)
        OR
        (ReviewerID IS NOT NULL AND Decision IS NOT NULL
         AND ReviewedAt IS NOT NULL)
    ),
    CONSTRAINT ck_booking_no_self_review CHECK (
        ReviewerID IS NULL OR ReviewerID <> RequesterID
    ),
    CONSTRAINT ck_booking_review_state CHECK (
        (BookingStatus = 'Pending' AND ReviewerID IS NULL)
        OR (BookingStatus IN ('Approved','Completed')
            AND ReviewerID IS NOT NULL AND Decision = 'Approved')
        OR (BookingStatus = 'Rejected'
            AND ReviewerID IS NOT NULL AND Decision = 'Rejected')
        OR (BookingStatus = 'Cancelled'
            AND (ReviewerID IS NULL OR Decision = 'Approved'))
    ),
    CONSTRAINT ck_booking_review_time CHECK (
        ReviewedAt IS NULL OR ReviewedAt >= RequestedAt
    ),
    INDEX ix_booking_hall_interval
        (HallID, BookingDate, BookingStatus, StartTime, EndTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 9. HALL_BLOCK
CREATE TABLE HALL_BLOCK (
    BlockID INT NOT NULL AUTO_INCREMENT,
    HallID INT NOT NULL,
    RecordedByID INT NOT NULL,
    BlockStart DATETIME NOT NULL,
    BlockEnd DATETIME NOT NULL,
    Reason VARCHAR(255) NOT NULL,
    BlockStatus ENUM('Active', 'Cancelled') NOT NULL DEFAULT 'Active',
    PRIMARY KEY (BlockID),
    CONSTRAINT fk_hall_block_hallid FOREIGN KEY (HallID)
        REFERENCES LECTURE_HALL (HallID),
    CONSTRAINT fk_hall_block_recordedbyid FOREIGN KEY (RecordedByID)
        REFERENCES USER_ACCOUNT (AccountID),
    CONSTRAINT ck_hall_block_reason CHECK (
        CHAR_LENGTH(TRIM(Reason)) > 0
    ),
    CONSTRAINT ck_hall_block_interval CHECK (
        BlockEnd > BlockStart
    ),
    INDEX ix_hall_block_interval
        (HallID, BlockStatus, BlockStart, BlockEnd)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 10. HALL_FACILITY
CREATE TABLE HALL_FACILITY (
    HallID INT NOT NULL,
    FacilityID INT NOT NULL,
    Quantity INT NOT NULL,
    WorkingQuantity INT NOT NULL,
    PRIMARY KEY (HallID, FacilityID),
    CONSTRAINT fk_hall_facility_hallid FOREIGN KEY (HallID)
        REFERENCES LECTURE_HALL (HallID),
    CONSTRAINT fk_hall_facility_facilityid FOREIGN KEY (FacilityID)
        REFERENCES FACILITY (FacilityID),
    CONSTRAINT ck_hall_facility_total CHECK (
        Quantity > 0
    ),
    CONSTRAINT ck_hall_facility_working CHECK (
        WorkingQuantity >= 0 AND WorkingQuantity <= Quantity
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 11. BOOKING_GROUP
CREATE TABLE BOOKING_GROUP (
    BookingID INT NOT NULL,
    GroupID INT NOT NULL,
    PRIMARY KEY (BookingID, GroupID),
    CONSTRAINT fk_booking_group_bookingid FOREIGN KEY (BookingID)
        REFERENCES BOOKING (BookingID),
    CONSTRAINT fk_booking_group_groupid FOREIGN KEY (GroupID)
        REFERENCES STUDENT_GROUP (GroupID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;




-- BEGIN LECTURER APPLICATION TABLE
CREATE TABLE IF NOT EXISTS LECTURER_APPLICATION (
    ApplicationID INT NOT NULL AUTO_INCREMENT,
    AccountID INT NOT NULL,
    StaffID VARCHAR(50) NOT NULL,
    ApplicationStatus ENUM('Pending','Approved','Rejected') NOT NULL DEFAULT 'Pending',
    SubmittedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ReviewedByID INT NULL,
    ReviewedAt DATETIME NULL,
    ReviewNote VARCHAR(500) NULL,
    PRIMARY KEY (ApplicationID),
    CONSTRAINT uq_lecturer_application_account UNIQUE (AccountID),
    CONSTRAINT fk_lecturer_application_account FOREIGN KEY (AccountID) REFERENCES USER_ACCOUNT(AccountID),
    CONSTRAINT fk_lecturer_application_reviewer FOREIGN KEY (ReviewedByID) REFERENCES USER_ACCOUNT(AccountID),
    CONSTRAINT ck_lecturer_application_staff CHECK (CHAR_LENGTH(TRIM(StaffID))>0),
    CONSTRAINT ck_lecturer_application_review CHECK (
        (ApplicationStatus='Pending' AND ReviewedByID IS NULL AND ReviewedAt IS NULL AND ReviewNote IS NULL)
        OR (ApplicationStatus='Approved' AND ReviewedByID IS NOT NULL AND ReviewedAt IS NOT NULL)
        OR (ApplicationStatus='Rejected' AND ReviewedByID IS NOT NULL AND ReviewedAt IS NOT NULL
            AND ReviewNote IS NOT NULL AND CHAR_LENGTH(TRIM(ReviewNote))>0)
    ),
    CONSTRAINT ck_lecturer_application_no_self CHECK (ReviewedByID IS NULL OR ReviewedByID<>AccountID),
    CONSTRAINT ck_lecturer_application_time CHECK (ReviewedAt IS NULL OR ReviewedAt>=SubmittedAt),
    INDEX ix_lecturer_application_queue (ApplicationStatus,SubmittedAt,ApplicationID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- END LECTURER APPLICATION TABLE
SELECT COUNT(*) AS TableCount FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE';
-- Expected: 12.


-- SECTION 4: BUSINESS PROCEDURES. Reinstalling procedures preserves table data.
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


-- BEGIN LECTURER APPLICATION PROCEDURES
DROP PROCEDURE IF EXISTS sp_register_lecturer_application$$
CREATE PROCEDURE sp_register_lecturer_application(
 IN p_department INT,IN p_name TEXT,IN p_email TEXT,IN p_username TEXT,
 IN p_password_hash TEXT,IN p_staff_id TEXT,OUT p_application_id INT)
SQL SECURITY DEFINER
BEGIN
 DECLARE v_account INT;
 DECLARE v_started BOOLEAN DEFAULT FALSE;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
 SET SESSION time_zone='+05:30';
 SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
 START TRANSACTION;
 SET v_started=TRUE;
 CALL _hs_validate_account(p_name,p_email,p_username,p_password_hash);
 IF p_department IS NULL OR p_department<=0 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Choose your department.';
 END IF;
 IF p_staff_id IS NULL OR CHAR_LENGTH(TRIM(p_staff_id)) NOT BETWEEN 1 AND 50
 OR REGEXP_LIKE(p_staff_id,'[[:cntrl:]]') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Enter a staff ID of 1 to 50 characters without control characters.';
 END IF;
 -- One namespace for all usernames/emails; pending accounts cannot authenticate.
 INSERT INTO USER_ACCOUNT(DepartmentID,FullName,Email,Username,PasswordHash,Role,IsActive)
 VALUES(p_department,TRIM(p_name),TRIM(p_email),TRIM(p_username),p_password_hash,'Lecturer',0);
 SET v_account=LAST_INSERT_ID();
 INSERT INTO LECTURER_APPLICATION(AccountID,StaffID) VALUES(v_account,TRIM(p_staff_id));
 SET p_application_id=LAST_INSERT_ID();
 COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_list_lecturer_applications$$
CREATE PROCEDURE sp_list_lecturer_applications(IN p_actor INT,IN p_status VARCHAR(20))
SQL SECURITY DEFINER
BEGIN
 DECLARE v_started BOOLEAN DEFAULT FALSE;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
 SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
 START TRANSACTION;
 SET v_started=TRUE;
 CALL _hs_require_actor(p_actor,'ADMIN');
 IF p_status IS NOT NULL AND p_status NOT IN ('Pending','Approved','Rejected') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Choose a valid application status.';
 END IF;
 SELECT a.ApplicationID,a.AccountID,a.StaffID,a.ApplicationStatus,a.SubmittedAt,
        a.ReviewedByID,a.ReviewedAt,a.ReviewNote,r.FullName AS ReviewerName,
        u.DepartmentID,d.DepartmentName,u.FullName,u.Email,u.Username,u.IsActive
 FROM LECTURER_APPLICATION a JOIN USER_ACCOUNT u ON u.AccountID=a.AccountID
 LEFT JOIN DEPARTMENT d ON d.DepartmentID=u.DepartmentID
 LEFT JOIN USER_ACCOUNT r ON r.AccountID=a.ReviewedByID
 WHERE p_status IS NULL OR a.ApplicationStatus=p_status
 ORDER BY (a.ApplicationStatus='Pending') DESC,a.SubmittedAt DESC,a.ApplicationID DESC;
 -- Password hashes are never part of the review list.
 COMMIT;
END$$

DROP PROCEDURE IF EXISTS sp_review_lecturer_application$$
CREATE PROCEDURE sp_review_lecturer_application(
 IN p_actor INT,IN p_application INT,IN p_decision VARCHAR(20),IN p_note TEXT,IN p_identity_verified BOOLEAN)
SQL SECURITY DEFINER
BEGIN
 DECLARE v_account INT DEFAULT NULL;
 DECLARE v_status VARCHAR(20);
 DECLARE v_role VARCHAR(20);
 DECLARE v_active BOOLEAN;
 DECLARE v_started BOOLEAN DEFAULT FALSE;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN IF v_started THEN ROLLBACK; END IF; RESIGNAL; END;
 SET SESSION time_zone='+05:30';
 SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
 START TRANSACTION;
 SET v_started=TRUE;
 CALL _hs_require_actor(p_actor,'ADMIN');
 IF p_decision IS NULL OR p_decision NOT IN ('Approved','Rejected') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Choose Approve or Reject.';
 END IF;
 IF p_note IS NOT NULL AND CHAR_LENGTH(TRIM(p_note))>500 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Review notes must have at most 500 characters.';
 END IF;
 IF p_decision='Approved' AND (p_identity_verified IS NULL OR p_identity_verified<>1) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Confirm the applicant identity through an independent university channel before approving.';
 END IF;
 IF p_decision='Rejected' AND (p_note IS NULL OR CHAR_LENGTH(TRIM(p_note))=0) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Enter a reason for rejecting this application.';
 END IF;
 -- The application row serializes competing reviews; a decision and activation commit together.
 SELECT AccountID,ApplicationStatus INTO v_account,v_status FROM LECTURER_APPLICATION
 WHERE ApplicationID=p_application FOR UPDATE;
 IF v_account IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Lecturer application not found.'; END IF;
 IF v_account=p_actor THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='An administrator cannot review their own application.'; END IF;
 IF v_status<>'Pending' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='This application has already been reviewed.'; END IF;
 SELECT Role,IsActive INTO v_role,v_active FROM USER_ACCOUNT WHERE AccountID=v_account FOR UPDATE;
 IF v_role<>'Lecturer' OR v_active<>0 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='This account is no longer an inactive lecturer applicant. Stop and inspect the account.';
 END IF;
 UPDATE LECTURER_APPLICATION SET ApplicationStatus=p_decision,ReviewedByID=p_actor,ReviewedAt=CURRENT_TIMESTAMP,
 ReviewNote=CASE WHEN p_decision='Approved' THEN COALESCE(NULLIF(TRIM(p_note),''),'Identity confirmed through an independent university channel.') ELSE TRIM(p_note) END
 WHERE ApplicationID=p_application;
 UPDATE USER_ACCOUNT SET IsActive=(p_decision='Approved') WHERE AccountID=v_account;
 COMMIT;
END$$
-- Public readiness information only; the definer checks metadata without exposing it.
DROP PROCEDURE IF EXISTS sp_get_lecturer_registration_status$$
CREATE PROCEDURE sp_get_lecturer_registration_status()
SQL SECURITY DEFINER
BEGIN
 SELECT ((SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema=DATABASE()
   AND routine_name IN ('sp_register_lecturer_application','sp_list_lecturer_applications','sp_review_lecturer_application'))=3
   AND EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='LECTURER_APPLICATION')) AS RegistrationEnabled;
END$$
-- END LECTURER APPLICATION PROCEDURES

DELIMITER ;


USE hallsync_db;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET SESSION time_zone = '+05:30';
SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- SECTION 5: VIEWS AND REPORTS
CREATE OR REPLACE SQL SECURITY DEFINER VIEW v_booking_details AS
SELECT k.BookingID,k.BookingDate,k.StartTime,k.EndTime,k.BookingType,k.Purpose,k.ExpectedAttendance,k.BookingStatus,k.RequestedAt,
 k.RequesterID,r.FullName AS RequesterName,k.HallID,b.BuildingCode,h.HallNo,h.Capacity,k.CourseID,c.CourseCode,c.CourseTitle,
 k.ReviewerID,a.FullName AS ReviewerName,k.Decision,k.ReviewedAt,k.ReviewNote
FROM BOOKING k JOIN USER_ACCOUNT r ON r.AccountID=k.RequesterID
JOIN LECTURE_HALL h ON h.HallID=k.HallID JOIN BUILDING b ON b.BuildingID=h.BuildingID
LEFT JOIN COURSE_UNIT c ON c.CourseID=k.CourseID LEFT JOIN USER_ACCOUNT a ON a.AccountID=k.ReviewerID;

CREATE OR REPLACE SQL SECURITY DEFINER VIEW v_hall_facilities AS
SELECT h.HallID,b.BuildingCode,h.HallNo,f.FacilityID,f.FacilityName,x.Quantity,x.WorkingQuantity,
 x.Quantity-x.WorkingQuantity AS NonWorkingQuantity
FROM HALL_FACILITY x JOIN LECTURE_HALL h ON h.HallID=x.HallID
JOIN BUILDING b ON b.BuildingID=h.BuildingID JOIN FACILITY f ON f.FacilityID=x.FacilityID;

CREATE OR REPLACE SQL SECURITY DEFINER VIEW v_booking_status_totals AS
SELECT BookingStatus,COUNT(*) AS BookingCount FROM BOOKING GROUP BY BookingStatus;

CREATE OR REPLACE SQL SECURITY DEFINER VIEW v_hall_usage AS
SELECT HallID,BookingDate,COUNT(*) AS SessionCount,
 SUM(TIMESTAMPDIFF(MINUTE,StartTime,EndTime)) AS ReservedMinutes
FROM BOOKING WHERE BookingStatus IN ('Approved','Completed') GROUP BY HallID,BookingDate;
-- ReservedMinutes is scheduled time, not measured actual use or student attendance.


-- SECTION 6: CAMPUS REFERENCE DATA AND SYNTHETIC FIXTURES
-- Source-linked campus names and labelled synthetic fixtures. Existing matching rows are preserved.
-- Random discarded passwords are used for the demo account hashes; these are not login-ready accounts.
-- Real accounts and timetable reservations must be provisioned before real availability is trusted.
USE hallsync_db;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET SESSION time_zone = '+05:30';
SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

INSERT INTO DEPARTMENT(DepartmentName)
SELECT 'Industrial Management' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Industrial Management');
-- Source-linked Kelaniya department and building names. See application/src/main/resources/campus.json.
-- Existing IDs are retained. Venue capacities and actual reservations must be confirmed separately.
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Chemistry' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Chemistry');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Mathematics' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Mathematics');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Microbiology' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Microbiology');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Physics and Electronics' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Physics and Electronics');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Plant and Molecular Biology' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Plant and Molecular Biology');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Statistics and Computer Science' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Statistics and Computer Science');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Zoology and Environmental Management' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Zoology and Environmental Management');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Accountancy' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Accountancy');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Commerce and Financial Management' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Commerce and Financial Management');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Finance' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Finance');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Human Resource Management' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Human Resource Management');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Marketing Management' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Marketing Management');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Applied Computing' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Applied Computing');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Computer Systems Engineering' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Computer Systems Engineering');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Software Engineering' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Software Engineering');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Drama, Cinema and Television' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Drama, Cinema and Television');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'English' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='English');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'English Language Teaching' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='English Language Teaching');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Fine Arts' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Fine Arts');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Hindi Studies' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Hindi Studies');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Linguistics' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Linguistics');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Modern Languages' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Modern Languages');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Pali and Buddhist Studies' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Pali and Buddhist Studies');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Sanskrit and Eastern Studies' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Sanskrit and Eastern Studies');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Sinhala' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Sinhala');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Western Classical Culture and Christian Culture' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Western Classical Culture and Christian Culture');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Anatomy' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Anatomy');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Biochemistry and Clinical Chemistry' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Biochemistry and Clinical Chemistry');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Disability Studies' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Disability Studies');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Family Medicine' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Family Medicine');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Forensic Medicine' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Forensic Medicine');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Medical Education' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Medical Education');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Medical Microbiology' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Medical Microbiology');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Medicine' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Medicine');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Obstetrics and Gynaecology' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Obstetrics and Gynaecology');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Paediatrics' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Paediatrics');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Parasitology' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Parasitology');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Pathology' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Pathology');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Pharmacology' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Pharmacology');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Physiology' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Physiology');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Psychiatry' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Psychiatry');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Public Health' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Public Health');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Surgery' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Surgery');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Archaeology' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Archaeology');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Economics' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Economics');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Geography' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Geography');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'History' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='History');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Information Technology' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Information Technology');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'International Studies' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='International Studies');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Library and Information Science' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Library and Information Science');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Mass Communication' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Mass Communication');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Philosophy' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Philosophy');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Political Science' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Political Science');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Social Statistics' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Social Statistics');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Sociology' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Sociology');
INSERT INTO DEPARTMENT(DepartmentName) SELECT 'Sport Science and Physical Education' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Sport Science and Physical Education');
INSERT INTO BUILDING(BuildingCode,BuildingName,LocationDescription) SELECT 'A4','A4 · MIT Building','Faculty of Science, Dalugama' WHERE NOT EXISTS(SELECT 1 FROM BUILDING WHERE BuildingCode='A4');
INSERT INTO BUILDING(BuildingCode,BuildingName,LocationDescription) SELECT 'A7','A7 · Faculty of Science','Faculty of Science, Dalugama' WHERE NOT EXISTS(SELECT 1 FROM BUILDING WHERE BuildingCode='A7');
INSERT INTO BUILDING(BuildingCode,BuildingName,LocationDescription) SELECT 'A8','A8 · Auditorium Building','Faculty of Science, Dalugama' WHERE NOT EXISTS(SELECT 1 FROM BUILDING WHERE BuildingCode='A8');
INSERT INTO BUILDING(BuildingCode,BuildingName,LocationDescription) SELECT 'A11','A11 · Faculty of Science','Faculty of Science, Dalugama' WHERE NOT EXISTS(SELECT 1 FROM BUILDING WHERE BuildingCode='A11');
INSERT INTO BUILDING(BuildingCode,BuildingName,LocationDescription) SELECT 'B1','B1 · Science Building','Faculty of Science, Dalugama' WHERE NOT EXISTS(SELECT 1 FROM BUILDING WHERE BuildingCode='B1');
INSERT INTO BUILDING(BuildingCode,BuildingName,LocationDescription) SELECT 'K14','K14 · Humanities Building','Faculty of Humanities, Dalugama' WHERE NOT EXISTS(SELECT 1 FROM BUILDING WHERE BuildingCode='K14');
SET @demo_department=(SELECT MIN(DepartmentID) FROM DEPARTMENT WHERE DepartmentName='Industrial Management');
INSERT INTO BUILDING(BuildingCode,BuildingName,LocationDescription)
SELECT 'HS-DEMO','HallSync Demo Block','Synthetic practice location'
WHERE NOT EXISTS(SELECT 1 FROM BUILDING WHERE BuildingCode='HS-DEMO');
SET @demo_building=(SELECT BuildingID FROM BUILDING WHERE BuildingCode='HS-DEMO');
INSERT INTO FACILITY(FacilityName,Description)
SELECT 'Projector','Demo equipment type' WHERE NOT EXISTS(SELECT 1 FROM FACILITY WHERE FacilityName='Projector');
SET @demo_facility=(SELECT FacilityID FROM FACILITY WHERE FacilityName='Projector');
INSERT INTO COURSE_UNIT(DepartmentID,CourseCode,CourseTitle)
SELECT @demo_department,'HS-DEMO-101','HallSync Practice Course' WHERE NOT EXISTS(SELECT 1 FROM COURSE_UNIT WHERE CourseCode='HS-DEMO-101');
INSERT INTO STUDENT_GROUP(DepartmentID,GroupName,IntakeYear,StudyYear)
SELECT @demo_department,'HallSync Demo Group A',2026,1
WHERE NOT EXISTS(SELECT 1 FROM STUDENT_GROUP WHERE DepartmentID=@demo_department AND GroupName='HallSync Demo Group A' AND IntakeYear=2026);
INSERT INTO LECTURE_HALL(BuildingID,HallNo,Floor,Capacity)
SELECT @demo_building,'DEMO-01','1',100 WHERE NOT EXISTS(SELECT 1 FROM LECTURE_HALL WHERE BuildingID=@demo_building AND HallNo='DEMO-01');
SET @demo_hall=(SELECT HallID FROM LECTURE_HALL WHERE BuildingID=@demo_building AND HallNo='DEMO-01');
INSERT INTO HALL_FACILITY(HallID,FacilityID,Quantity,WorkingQuantity)
SELECT @demo_hall,@demo_facility,2,2 WHERE NOT EXISTS(SELECT 1 FROM HALL_FACILITY WHERE HallID=@demo_hall AND FacilityID=@demo_facility);
INSERT INTO USER_ACCOUNT(DepartmentID,FullName,Email,Username,PasswordHash,Role) SELECT @demo_department,'Demo Administrator A','demo_admin_a@example.invalid','demo_admin_a','$argon2id$v=19$m=19456,t=2,p=1$TEgTofH6aDZ8F5EJaVwDpA$bB1xKeFOb/8Z+45ExaOfVG+VKmJoOHFDXsUah0bDCs8','AdminStaff' WHERE NOT EXISTS(SELECT 1 FROM USER_ACCOUNT WHERE Username='demo_admin_a');
INSERT INTO USER_ACCOUNT(DepartmentID,FullName,Email,Username,PasswordHash,Role) SELECT @demo_department,'Demo Administrator B','demo_admin_b@example.invalid','demo_admin_b','$argon2id$v=19$m=19456,t=2,p=1$UxBj1mOHy9P11FHfEhbQew$PZ2ywTwdH+Hvf2K95CenhhfiL0p2ggh6MY/x/550e84','AdminStaff' WHERE NOT EXISTS(SELECT 1 FROM USER_ACCOUNT WHERE Username='demo_admin_b');
INSERT INTO USER_ACCOUNT(DepartmentID,FullName,Email,Username,PasswordHash,Role) SELECT @demo_department,'Demo Lecturer','demo_lecturer@example.invalid','demo_lecturer','$argon2id$v=19$m=19456,t=2,p=1$zKYjaFauBb9X7W9Ni0oFNA$eSraz0MW1h3ugjF/aPt42JwgUNSUSDz+ScDYsad2Vak','Lecturer' WHERE NOT EXISTS(SELECT 1 FROM USER_ACCOUNT WHERE Username='demo_lecturer');
INSERT INTO USER_ACCOUNT(DepartmentID,FullName,Email,Username,PasswordHash,Role) SELECT @demo_department,'Demo Student','demo_student@example.invalid','demo_student','$argon2id$v=19$m=19456,t=2,p=1$Si9+QWj8FPKFJxPENvB2KQ$mpOmBn67zbJC9GlXH33Sv3NRwAlLx1mMzp82n7ow5r4','Student' WHERE NOT EXISTS(SELECT 1 FROM USER_ACCOUNT WHERE Username='demo_student');
SELECT AccountID,Username,Role,IsActive FROM USER_ACCOUNT WHERE Username IN ('demo_admin_a','demo_admin_b','demo_lecturer','demo_student') ORDER BY AccountID;


-- SECTION 7: RESTRICTED APPLICATION ROLE
-- Creates a least-privilege role, not a database login or password.
-- Assign it to a dedicated backend DB account during application connection setup.
-- The application authenticates users and supplies actor IDs from trusted sessions.
CREATE ROLE IF NOT EXISTS 'hallsync_application';
GRANT SELECT ON hallsync_db.DEPARTMENT TO 'hallsync_application';
GRANT SELECT ON hallsync_db.BUILDING TO 'hallsync_application';
GRANT SELECT ON hallsync_db.FACILITY TO 'hallsync_application';
GRANT SELECT ON hallsync_db.COURSE_UNIT TO 'hallsync_application';
GRANT SELECT ON hallsync_db.STUDENT_GROUP TO 'hallsync_application';
GRANT SELECT ON hallsync_db.LECTURE_HALL TO 'hallsync_application';
GRANT SELECT ON hallsync_db.HALL_FACILITY TO 'hallsync_application';
GRANT SELECT ON hallsync_db.BOOKING_GROUP TO 'hallsync_application';
GRANT SELECT ON hallsync_db.v_booking_details TO 'hallsync_application';
GRANT SELECT ON hallsync_db.v_hall_facilities TO 'hallsync_application';
GRANT SELECT ON hallsync_db.v_booking_status_totals TO 'hallsync_application';
GRANT SELECT ON hallsync_db.v_hall_usage TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_create_booking TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_update_pending_booking TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_review_booking TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_cancel_booking TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_complete_booking TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_create_hall_block TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_cancel_hall_block TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_update_hall TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_set_hall_facility TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_register_student TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_provision_account TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_get_login_record TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_find_available_halls TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_add_department TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_add_building TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_add_facility TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_add_course TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_add_student_group TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_add_hall TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_list_hall_blocks TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_list_accounts TO 'hallsync_application';



USE hallsync_db;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET SESSION time_zone = '+05:30';
SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- BEGIN LECTURER APPLICATION GRANTS
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_register_lecturer_application TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_list_lecturer_applications TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_review_lecturer_application TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_get_lecturer_registration_status TO 'hallsync_application';
-- END LECTURER APPLICATION GRANTS

-- SECTION 8: READ-ONLY VERIFICATION
-- Metadata and integrity inspection. These statements only read data.
SELECT 'Base tables' AS ObjectType,COUNT(*) AS ActualCount,12 AS ExpectedCount
FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE'
UNION ALL SELECT 'Columns',COUNT(*),69 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name IN (SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE')
UNION ALL SELECT 'Primary keys',COUNT(*),12 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_type='PRIMARY KEY'
UNION ALL SELECT 'Foreign keys',COUNT(*),16 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_type='FOREIGN KEY'
UNION ALL SELECT 'Other unique keys',COUNT(*),8 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_type='UNIQUE'
UNION ALL SELECT 'CHECK constraints',COUNT(*),32 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_type='CHECK'
UNION ALL SELECT 'Views',COUNT(*),4 FROM information_schema.views WHERE table_schema=DATABASE()
UNION ALL SELECT 'Procedures',COUNT(*),30 FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_type='PROCEDURE';

SELECT TABLE_NAME,GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION) AS PrimaryKeyColumns
FROM information_schema.key_column_usage WHERE table_schema=DATABASE() AND constraint_name='PRIMARY'
GROUP BY TABLE_NAME ORDER BY TABLE_NAME;
SELECT TABLE_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME
FROM information_schema.key_column_usage WHERE table_schema=DATABASE() AND referenced_table_name IS NOT NULL
ORDER BY TABLE_NAME,COLUMN_NAME;
SELECT t.TABLE_NAME,c.CONSTRAINT_NAME,c.CHECK_CLAUSE,t.ENFORCED
FROM information_schema.check_constraints c JOIN information_schema.table_constraints t
ON c.CONSTRAINT_SCHEMA=t.CONSTRAINT_SCHEMA AND c.CONSTRAINT_NAME=t.CONSTRAINT_NAME
WHERE c.CONSTRAINT_SCHEMA=DATABASE() ORDER BY t.TABLE_NAME,c.CONSTRAINT_NAME;
SELECT TABLE_NAME,INDEX_NAME,GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS IndexedColumns
FROM information_schema.statistics WHERE table_schema=DATABASE()
GROUP BY TABLE_NAME,INDEX_NAME ORDER BY TABLE_NAME,INDEX_NAME;

-- Integrity queries below should return no rows.
SELECT a.BookingID AS FirstBooking,b.BookingID AS ConflictingBooking
FROM BOOKING a JOIN BOOKING b ON a.HallID=b.HallID AND a.BookingDate=b.BookingDate AND a.BookingID<b.BookingID AND a.StartTime<b.EndTime AND a.EndTime>b.StartTime
WHERE a.BookingStatus='Approved' AND b.BookingStatus='Approved';
SELECT k.BookingID,x.BlockID FROM BOOKING k JOIN HALL_BLOCK x ON x.HallID=k.HallID AND x.BlockStart<TIMESTAMP(k.BookingDate,k.EndTime) AND x.BlockEnd>TIMESTAMP(k.BookingDate,k.StartTime)
WHERE k.BookingStatus='Approved' AND x.BlockStatus='Active';
SELECT k.BookingID FROM BOOKING k WHERE k.BookingType IN ('Lecture','Tutorial') AND NOT EXISTS(SELECT 1 FROM BOOKING_GROUP g WHERE g.BookingID=k.BookingID);
SELECT k.BookingID,k.ExpectedAttendance,h.Capacity FROM BOOKING k JOIN LECTURE_HALL h ON h.HallID=k.HallID
WHERE k.BookingStatus='Approved' AND TIMESTAMP(k.BookingDate,k.EndTime)>CURRENT_TIMESTAMP AND (k.ExpectedAttendance>h.Capacity OR h.OperationalStatus<>'Active');
