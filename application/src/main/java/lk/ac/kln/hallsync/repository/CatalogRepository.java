package lk.ac.kln.hallsync.repository;

import lk.ac.kln.hallsync.model.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface CatalogRepository {
    List<Building> buildings() throws SQLException;
    List<Facility> facilities() throws SQLException;
    List<CourseUnit> courses() throws SQLException;
    List<StudentGroup> groups() throws SQLException;
    List<LectureHall> halls() throws SQLException;
    List<HallFacility> equipment() throws SQLException;
    List<LectureHall> available(LocalDate date, LocalTime start, LocalTime end, int attendance) throws SQLException;
    int addBuilding(int actor, String code, String name, String location) throws SQLException;
    int addFacility(int actor, String name, String description) throws SQLException;
    int addCourse(int actor, int department, String code, String title) throws SQLException;
    int addGroup(int actor, int department, String name, int intake, int year) throws SQLException;
    int addHall(int actor, int building, String number, String floor, int capacity) throws SQLException;
    void editHall(int actor,int hall,int building,String number,String floor,int capacity,String status) throws SQLException;
    void removeHall(int actor,int hall) throws SQLException;
    void updateHall(int actor, int hall, int capacity, String status) throws SQLException;
    void setEquipment(int actor, int hall, int facility, int quantity, int working) throws SQLException;
}
