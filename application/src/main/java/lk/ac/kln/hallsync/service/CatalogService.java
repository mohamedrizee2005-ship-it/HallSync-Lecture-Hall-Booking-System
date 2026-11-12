package lk.ac.kln.hallsync.service;

import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.model.*;
import lk.ac.kln.hallsync.repository.CatalogRepository;
import java.sql.SQLException;
import java.time.*;
import java.util.List;

public final class CatalogService {
    private final CatalogRepository repository;
    private final Clock clock;
    public CatalogService(CatalogRepository repository, Clock clock) { this.repository = repository; this.clock = clock; }
    // Reference-data choices contain no private account data and are also used for registration.
    public List<Building> buildings() throws SQLException { return repository.buildings(); }
    public List<Facility> facilities() throws SQLException { return repository.facilities(); }
    public List<CourseUnit> courses() throws SQLException { return repository.courses(); }
    public List<StudentGroup> groups() throws SQLException { return repository.groups(); }
    public List<LectureHall> halls() throws SQLException { return repository.halls(); }
    public List<HallFacility> equipment() throws SQLException { return repository.equipment(); }
    public List<LectureHall> available(AuthenticatedUser user, LocalDate date, LocalTime start, LocalTime end, int attendance) throws SQLException {
        AccessRules.signedIn(user); BookingService.validateInterval(date, start, end, clock);
        return repository.available(date, start, end, Validation.positive(attendance, "Attendance"));
    }
    public int addBuilding(AuthenticatedUser user, String code, String name, String location) throws SQLException {
        int actor = AccessRules.administrator(user);
        return repository.addBuilding(actor, Validation.text(code,"Building code",20), Validation.text(name,"Building name",100), Validation.optional(location,"Location",255));
    }
    public int addFacility(AuthenticatedUser user, String name, String description) throws SQLException {
        int actor = AccessRules.administrator(user);
        return repository.addFacility(actor, Validation.text(name,"Facility name",100), Validation.optional(description,"Description",255));
    }
    public int addCourse(AuthenticatedUser user, int department, String code, String title) throws SQLException {
        int actor = AccessRules.administrator(user);
        return repository.addCourse(actor, Validation.positive(department,"Department"), Validation.text(code,"Course code",20), Validation.text(title,"Course title",150));
    }
    public int addGroup(AuthenticatedUser user, int department, String name, int intake, int year) throws SQLException {
        int actor = AccessRules.administrator(user);
        if (intake < 1900 || intake > 9999 || year < 1 || year > 255) {
            throw new IllegalArgumentException("Intake must be 1900-9999 and study year must be 1-255.");
        }
        return repository.addGroup(actor, Validation.positive(department,"Department"), Validation.text(name,"Group name",100), intake, year);
    }
    public int addHall(AuthenticatedUser user, int building, String number, String floor, int capacity) throws SQLException {
        int actor = AccessRules.administrator(user);
        return repository.addHall(actor, Validation.positive(building,"Building"), Validation.text(number,"Hall number",20),
                Validation.optional(floor,"Floor",10), Validation.positive(capacity,"Capacity"));
    }
    public void editHall(AuthenticatedUser user,int hall,int building,String number,String floor,int capacity,String status) throws SQLException {
        int actor=AccessRules.administrator(user);
        if(!"Active".equals(status)&&!"Inactive".equals(status))throw new IllegalArgumentException("Choose Active or Inactive.");
        repository.editHall(actor,Validation.positive(hall,"Hall"),Validation.positive(building,"Building"),Validation.text(number,"Hall number",20),Validation.optional(floor,"Floor",10),Validation.positive(capacity,"Capacity"),status);
    }
    public void removeHall(AuthenticatedUser user,int hall) throws SQLException { repository.removeHall(AccessRules.administrator(user),Validation.positive(hall,"Hall")); }
    public void updateHall(AuthenticatedUser user, int hall, int capacity, String status) throws SQLException {
        int actor = AccessRules.administrator(user);
        if (!"Active".equals(status) && !"Inactive".equals(status)) { throw new IllegalArgumentException("Choose Active or Inactive."); }
        repository.updateHall(actor, Validation.positive(hall,"Hall"), Validation.positive(capacity,"Capacity"), status);
    }
    public void setEquipment(AuthenticatedUser user, int hall, int facility, int total, int working) throws SQLException {
        int actor = AccessRules.administrator(user);
        if (total <= 0 || working < 0 || working > total) { throw new IllegalArgumentException("Require total > 0 and 0 <= working <= total."); }
        repository.setEquipment(actor, Validation.positive(hall,"Hall"), Validation.positive(facility,"Facility"), total, working);
    }
}
