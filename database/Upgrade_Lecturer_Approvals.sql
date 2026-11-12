-- HallSync 2.2: run this file once as your Workbench administrator on an existing installation.
-- Repeatable and data-preserving: no database, account, password or booking is removed.
-- Restart HallSync after the upgrade. Fresh installations already include this feature.
USE hallsync_db;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET SESSION time_zone='+05:30';
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
DELIMITER $$
DROP PROCEDURE IF EXISTS _hs_check_lecturer_upgrade$$
CREATE PROCEDURE _hs_check_lecturer_upgrade()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_name='_hs_require_actor')
 OR NOT EXISTS(SELECT 1 FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_name='_hs_validate_account') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing HallSync account procedures are missing. Stop and inspect this database.';
 END IF;
 IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='LECTURER_APPLICATION')
 AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='LECTURER_APPLICATION')<>8 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='An unexpected lecturer application table already exists. Stop and inspect it.';
 END IF;
END$$
CALL _hs_check_lecturer_upgrade()$$
DROP PROCEDURE _hs_check_lecturer_upgrade$$
DELIMITER ;

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

DELIMITER $$
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
-- END LECTURER APPLICATION PROCEDURES

DELIMITER ;
-- BEGIN LECTURER APPLICATION GRANTS
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_register_lecturer_application TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_list_lecturer_applications TO 'hallsync_application';
GRANT EXECUTE ON PROCEDURE hallsync_db.sp_review_lecturer_application TO 'hallsync_application';
-- END LECTURER APPLICATION GRANTS

SELECT 'Lecturer registration and review installed; restart HallSync.' AS Result;
