-- STUDY / REFERENCE COMPONENT. Normal first installation uses HallSync_Full_Setup.sql.
-- Do not execute this as an extra installation step after the full installer.
-- Synthetic reference data for SQL demonstrations. Existing matching rows are preserved.
-- Random discarded passwords are used for the demo account hashes; these are not login-ready accounts.
-- Real accounts and timetable reservations must be provisioned before real availability is trusted.
USE hallsync_db;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET SESSION time_zone = '+05:30';
SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

INSERT INTO DEPARTMENT(DepartmentName)
SELECT 'Industrial Management' WHERE NOT EXISTS(SELECT 1 FROM DEPARTMENT WHERE DepartmentName='Industrial Management');
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
