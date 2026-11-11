-- HallSync - Step 2: create the DEPARTMENT table.
-- Prerequisite: 01_create_database.sql has completed successfully.
-- Run once. If any statement fails, stop and inspect the error.

USE hallsync_db;

CREATE TABLE DEPARTMENT (
    DepartmentID INT NOT NULL AUTO_INCREMENT,
    DepartmentName VARCHAR(100) NOT NULL,
    PRIMARY KEY (DepartmentID),
    CONSTRAINT ck_department_departmentname
        CHECK (CHAR_LENGTH(TRIM(DepartmentName)) > 0)
) ENGINE = InnoDB;

DESCRIBE DEPARTMENT;
