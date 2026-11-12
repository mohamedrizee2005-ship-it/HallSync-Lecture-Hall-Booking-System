package lk.ac.kln.hallsync.model;

/** A DEPARTMENT row represented as a Java object. */
public final class Department {
    private final int departmentId;
    private final String departmentName;

    public Department(int departmentId, String departmentName) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }
}
