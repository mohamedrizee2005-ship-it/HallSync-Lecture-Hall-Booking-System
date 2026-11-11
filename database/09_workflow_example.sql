USE hallsync_db;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET SESSION time_zone = '+05:30';
SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- Optional demonstration after setup; run once on the synthetic demo hall.
SET @lecturer=(SELECT AccountID FROM USER_ACCOUNT WHERE Username='demo_lecturer');
SET @reviewer=(SELECT AccountID FROM USER_ACCOUNT WHERE Username='demo_admin_a');
SET @hall=(SELECT h.HallID FROM LECTURE_HALL h JOIN BUILDING b ON b.BuildingID=h.BuildingID WHERE b.BuildingCode='HS-DEMO' AND h.HallNo='DEMO-01');
SET @booking_date=DATE_ADD(CURRENT_DATE(),INTERVAL 30 DAY);
CALL sp_create_booking(@lecturer,@hall,NULL,@booking_date,'09:00:00','10:00:00','Demo seminar','Event',30,JSON_ARRAY(),@booking_id);
SELECT @booking_id AS NewBookingID;
CALL sp_review_booking(@reviewer,@booking_id,'Approved','Checked demo availability');
SELECT * FROM v_booking_details WHERE BookingID=@booking_id;
-- Execute separately later to demonstrate cancellation:
-- CALL sp_cancel_booking(@lecturer,@booking_id);
-- An earlier approval decision remains recorded after cancellation.
