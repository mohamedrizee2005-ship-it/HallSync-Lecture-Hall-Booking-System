-- Run once after steps 01 and 02. Keep the existing DEPARTMENT and its data.
-- Enable Stop Script Execution On Errors in Workbench.
USE hallsync_db;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET SESSION time_zone = '+05:30';
SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

DELIMITER $$
DROP PROCEDURE IF EXISTS _hs_schema_preflight$$
CREATE PROCEDURE _hs_schema_preflight()
BEGIN
 IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE')<>1
 OR NOT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND LOWER(table_name)='department' AND engine='InnoDB') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected only DEPARTMENT. Stop and inspect; do not reset.';
 END IF;
 IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND LOWER(table_name)='department')<>2
 OR NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND LOWER(table_name)='department' AND column_name='DepartmentID' AND column_type='int' AND is_nullable='NO' AND column_key='PRI' AND extra LIKE '%auto_increment%')
 OR NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND LOWER(table_name)='department' AND column_name='DepartmentName' AND data_type='varchar' AND character_maximum_length=100 AND is_nullable='NO') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DEPARTMENT does not match step 02.';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND LOWER(table_name)='department' AND constraint_name='ck_department_departmentname' AND constraint_type='CHECK' AND enforced='YES') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected the enforced department name CHECK from step 02.';
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


SELECT COUNT(*) AS TableCount FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE';
-- Expected: 11.
