package lk.ac.kln.hallsync.repository;

import lk.ac.kln.hallsync.model.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class JdbcCatalogRepository implements CatalogRepository {
    private final JdbcExecutor db;
    public JdbcCatalogRepository(JdbcExecutor db) { this.db = db; }

    @Override public List<Building> buildings() throws SQLException {
        return db.query("SELECT BuildingID,BuildingCode,BuildingName,LocationDescription FROM BUILDING ORDER BY BuildingCode",
                r -> new Building(r.getInt(1), r.getString(2), r.getString(3), r.getString(4)));
    }
    @Override public List<Facility> facilities() throws SQLException {
        return db.query("SELECT FacilityID,FacilityName,Description FROM FACILITY ORDER BY FacilityName",
                r -> new Facility(r.getInt(1), r.getString(2), r.getString(3)));
    }
    @Override public List<CourseUnit> courses() throws SQLException {
        return db.query("SELECT CourseID,DepartmentID,CourseCode,CourseTitle FROM COURSE_UNIT ORDER BY CourseCode",
                r -> new CourseUnit(r.getInt(1), r.getInt(2), r.getString(3), r.getString(4)));
    }
    @Override public List<StudentGroup> groups() throws SQLException {
        return db.query("SELECT GroupID,DepartmentID,GroupName,IntakeYear,StudyYear FROM STUDENT_GROUP ORDER BY IntakeYear,GroupName",
                r -> new StudentGroup(r.getInt(1), r.getInt(2), r.getString(3), r.getInt(4), r.getInt(5)));
    }
    @Override public List<LectureHall> halls() throws SQLException {
        return db.query("SELECT h.HallID,b.BuildingCode,b.BuildingName,h.HallNo,h.Floor,h.Capacity,h.OperationalStatus "
                + "FROM LECTURE_HALL h JOIN BUILDING b ON b.BuildingID=h.BuildingID ORDER BY b.BuildingCode,h.HallNo",
                r -> hall(r, r.getString("OperationalStatus")));
    }
    @Override public List<HallFacility> equipment() throws SQLException {
        return db.query("SELECT HallID,BuildingCode,HallNo,FacilityID,FacilityName,Quantity,WorkingQuantity "
                + "FROM v_hall_facilities ORDER BY BuildingCode,HallNo,FacilityName",
                r -> new HallFacility(r.getInt("HallID"), r.getString("BuildingCode") + " / " + r.getString("HallNo"),
                        r.getInt("FacilityID"), r.getString("FacilityName"), r.getInt("Quantity"), r.getInt("WorkingQuantity")));
    }
    @Override public List<LectureHall> available(LocalDate date, LocalTime start, LocalTime end, int attendance) throws SQLException {
        return db.callRows("{CALL sp_find_available_halls(?,?,?,?)}", r -> hall(r, "Active"), date, start, end, attendance);
    }
    private static LectureHall hall(ResultSet r, String status) throws SQLException {
        return new LectureHall(r.getInt("HallID"), r.getString("BuildingCode"), r.getString("BuildingName"),
                r.getString("HallNo"), r.getString("Floor"), r.getInt("Capacity"), status);
    }
    @Override public int addBuilding(int actor, String code, String name, String location) throws SQLException {
        return db.callForId("{CALL sp_add_building(?,?,?,?,?)}", actor, code, name, location);
    }
    @Override public int addFacility(int actor, String name, String description) throws SQLException {
        return db.callForId("{CALL sp_add_facility(?,?,?,?)}", actor, name, description);
    }
    @Override public int addCourse(int actor, int department, String code, String title) throws SQLException {
        return db.callForId("{CALL sp_add_course(?,?,?,?,?)}", actor, department, code, title);
    }
    @Override public int addGroup(int actor, int department, String name, int intake, int year) throws SQLException {
        return db.callForId("{CALL sp_add_student_group(?,?,?,?,?,?)}", actor, department, name, intake, year);
    }
    @Override public int addHall(int actor, int building, String number, String floor, int capacity) throws SQLException {
        return db.callForId("{CALL sp_add_hall(?,?,?,?,?,?)}", actor, building, number, floor, capacity);
    }
    @Override public void editHall(int actor,int hall,int building,String number,String floor,int capacity,String status) throws SQLException {
        db.call("{CALL sp_edit_hall(?,?,?,?,?,?,?)}",actor,hall,building,number,floor,capacity,status);
    }
    @Override public void removeHall(int actor,int hall) throws SQLException { db.call("{CALL sp_remove_hall(?,?)}",actor,hall); }
    @Override public void updateHall(int actor, int hall, int capacity, String status) throws SQLException {
        db.call("{CALL sp_update_hall(?,?,?,?)}", actor, hall, capacity, status);
    }
    @Override public void setEquipment(int actor, int hall, int facility, int quantity, int working) throws SQLException {
        db.call("{CALL sp_set_hall_facility(?,?,?,?,?)}", actor, hall, facility, quantity, working);
    }
}
