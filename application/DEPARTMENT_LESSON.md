# HallSync - Department model lesson

Prerequisite: your connection checker already prints `Connection check succeeded.` This lesson uses that same database and connection.

## 1. What we added

`src/main/java/lk/ac/kln/hallsync/model/Department.java` represents one row of the `DEPARTMENT` table as a Java object. `ConnectionCheck.java` now constructs that object inside its existing result loop and displays its values through getters.

| Database column | Java field | Java type |
| --- | --- | --- |
| `DepartmentID` (`INT`) | `departmentId` | `int` |
| `DepartmentName` (`VARCHAR(100)`) | `departmentName` | `String` |

For your existing record, one object contains ID `1` and name `Industrial Management`. Other returned rows produce their own objects with their own values.

## 2. Update your existing local project

1. Download the latest repository ZIP and extract it into a separate temporary folder.
2. Copy the downloaded project's `application/src` folder into your existing working `application` folder. Allow the source folders to merge and replace the updated Java source files.
3. Continue using your existing working `application` folder with its `pom.xml` and private `db.local.properties`.
4. Check that the new file exists at `src/main/java/lk/ac/kln/hallsync/model/Department.java`, and that `ConnectionCheck.java` imports it.
5. Open Command Prompt in that existing `application` folder and run:

```cmd
mvn clean package
java -jar target/hallsync-connection-check.jar
```

The build should report `BUILD SUCCESS`. The program still displays your department row and ends with `Connection check succeeded.` The row is now represented by a Java object before it is displayed.

This source update uses your existing database installation and login. The new model requires no SQL setup commands.

## 3. Read the model class

```java
package lk.ac.kln.hallsync.model;

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
```

| Code | Meaning and reason |
| --- | --- |
| `package lk.ac.kln.hallsync.model` | Groups model classes. The package matches the source folder hierarchy. |
| `public final class Department` | Declares a class accessible from other packages. `final` on the class prevents subclasses. |
| `private` fields | Other classes access the data through the class's public methods. This demonstrates encapsulation. |
| `final` fields | Each field is assigned during construction and cannot later be reassigned. |
| `int` and `String` | The identifier is a primitive integer; the name is a reference to a string. |
| `Department(...)` | The constructor receives the two values needed for a new object. Its name matches the class and it has no return type. |
| `this.departmentId = departmentId` | The left side is the field of this object; the right side is the constructor parameter. |
| `getDepartmentId()` and `getDepartmentName()` | Getter methods return the object's data. |

There are no setters. The class has immutable state: its integer cannot be changed, its name reference cannot be replaced, and `String` itself is immutable. This makes each object a stable representation of the row values it was given.

This first constructor assigns the supplied values. It does not implement input validation. Existing database constraints still enforce required names and the column length during database writes; later lessons will handle input validation in the application as well.

## 4. Understand where the object is created

The existing SQL query still runs in `ConnectionCheck.printDepartments`. Inside `while (rows.next())`, the code now reads:

```java
Department department = new Department(
        rows.getInt("DepartmentID"),
        rows.getString("DepartmentName"));

System.out.printf("%d - %s%n",
        department.getDepartmentId(),
        department.getDepartmentName());
```

`rows.next()` moves to a returned database row. `getInt` and `getString` read its columns. `new Department(...)` calls the constructor to create an object. The local variable `department` holds a reference to that object, and its getter methods supply the values for printing.

Creating an object affects Java memory. Saving a change to MySQL requires an explicit database operation. The model itself holds data; connection handling and query execution remain outside it.

## 5. How to explain it in your presentation

- **What is the class?** `Department` is the blueprint for objects containing a department ID and name.
- **What is the object?** The `new Department(...)` expression creates one instance for the current row.
- **Where is encapsulation?** The fields are private and the public getters provide access to their values.
- **Why use a constructor?** It supplies an object's initial values when the object is created.
- **Why use final fields?** These values represent the retrieved row at that moment and remain stable in the object.
- **Where is the database logic?** It currently remains in the reader. The next repository lesson will give querying a separate class.
- **Did we add inheritance?** This lesson demonstrates a model object and encapsulation. There is no parent-child domain relationship to implement here.

## 6. What comes next

After the updated program succeeds on your laptop, introduce `DepartmentRepository`. It will read rows and return `Department` objects, letting the caller work with objects while the repository handles JDBC details.
