package lk.ac.kln.hallsync.repository;

import lk.ac.kln.hallsync.model.Department;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Contract for department persistence. */
public interface DepartmentRepository {
    List<Department> findAll() throws SQLException;
    Optional<Department> findById(int departmentId) throws SQLException;
    List<Department> findByName(String text) throws SQLException;
    Department add(int authenticatedActorId, String name) throws SQLException;
}
