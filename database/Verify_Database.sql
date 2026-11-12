-- Read-only schema and booking-integrity verification.
USE hallsync_db;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET SESSION time_zone = '+05:30';
SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- Metadata and integrity inspection. These statements only read data.
SELECT 'Base tables' AS ObjectType,COUNT(*) AS ActualCount,11 AS ExpectedCount
FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE'
UNION ALL SELECT 'Columns',COUNT(*),61 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name IN (SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE')
UNION ALL SELECT 'Primary keys',COUNT(*),11 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_type='PRIMARY KEY'
UNION ALL SELECT 'Foreign keys',COUNT(*),14 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_type='FOREIGN KEY'
UNION ALL SELECT 'Other unique keys',COUNT(*),7 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_type='UNIQUE'
UNION ALL SELECT 'CHECK constraints',COUNT(*),28 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_type='CHECK'
UNION ALL SELECT 'Views',COUNT(*),4 FROM information_schema.views WHERE table_schema=DATABASE()
UNION ALL SELECT 'Procedures',COUNT(*),27 FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_type='PROCEDURE';

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
