-- STUDY / REFERENCE COMPONENT. Normal first installation uses HallSync_Full_Setup.sql.
-- Do not execute this as an extra installation step after the full installer.
USE hallsync_db;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET SESSION time_zone = '+05:30';
SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

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
