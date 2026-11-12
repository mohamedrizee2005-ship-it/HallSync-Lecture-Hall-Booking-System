-- STUDY / REFERENCE COMPONENT. Normal first installation uses HallSync_Full_Setup.sql.
-- Do not execute this as an extra installation step after the full installer.
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
