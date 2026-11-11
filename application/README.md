# HallSync application - first connection lesson

Goal: connect a small Java program to your existing MySQL database and read the department list. The interface and booking screens come in later lessons.

Java is a practical choice for explaining classes, objects and encapsulation in the OOP presentation. JDBC is Java's database API; MySQL Connector/J implements that API for MySQL. Workbench and this Java program are two clients of the same MySQL server. GitHub stores their source files; it is not the database server.

## 1. Confirm the database and tools

The full database setup must have completed successfully. If you already ran `HallSync_Full_Setup.sql`, keep that installation. You can run the read-only `database/08_verify_database.sql` to confirm the 11 tables, 4 views and 25 procedures.

Open a Windows terminal and run:

```powershell
java -version
javac -version
mvn -version
```

Use a JDK, not just a Java runtime. This project targets Java 17 or newer; JDK 21 is suitable. Maven 3.9 or newer downloads the driver and builds the program. If a command is not recognised, install/configure that tool before continuing. Share the three version outputs to get guidance for your own setup.

## 2. Create the application's MySQL login once

Open `application/01_create_application_login.sql` in your existing Workbench administrator connection. Enable **Stop Script Execution On Errors** and execute the entire file once.

Find the `CREATE USER` result grid containing **Generated password**. Copy that password privately for the next step. Do not send or commit this grid or password. If the login already exists, stop and inspect the error; do not delete it or run the database installer again.

| SQL statement | Why it is needed |
| --- | --- |
| `SHOW GRANTS FOR 'hallsync_application'` | Checks that the database's application role exists before creating the login. |
| `CREATE USER ... IDENTIFIED BY RANDOM PASSWORD` | MySQL creates the separate `hallsync_app` login with a private generated password. |
| `REQUIRE SSL` | Requires an encrypted connection for that login. |
| `GRANT 'hallsync_application' ...` | Gives the login the limited permissions already defined by the project. |
| `SET DEFAULT ROLE ...` | Activates that role automatically when the login connects. |

This login is restricted to `127.0.0.1`, matching the local JDBC URL. Use the same port as your Workbench connection; the example uses 3306.

The MySQL login is different from a Student, Lecturer or AdminStaff record in `USER_ACCOUNT`. MySQL manages the database login's password. Application users will later have Argon2id password hashes and authenticated sessions. This program checks the database connection; it does not log in a lecturer or student.

## 3. Save your private local configuration

Download/extract the latest repository ZIP, or pull the latest commit in your existing checkout. Open the `application` folder in a terminal.

```powershell
Copy-Item db.example.properties db.local.properties
notepad db.local.properties
```

Keep `db.url` and `db.user` as provided unless your port differs. Replace the `db.password` placeholder with the generated MySQL password and save. Do not add quotation marks around the password. Java properties use backslash escapes: write a literal backslash as `\\` if your password contains one.

Only the safe example is committed. `.gitignore` excludes `db.local.properties` and Maven's `target` build output. Ignoring a file does not encrypt it or remove a copy already committed; keep your local configuration private.

For automated checks, `HALLSYNC_DB_PASSWORD` can supply the password instead; it takes precedence over the file. The Java program does not load `.env` files automatically.

## 4. Build and run

In the `application` directory:

```powershell
mvn clean package
java -jar target/hallsync-connection-check.jar
```

The first build needs internet access to download Maven dependencies. The packaged JAR includes the MySQL JDBC driver. It reads `db.local.properties` from your current directory; an optional command-line argument can specify a different configuration path.

Expected output includes:

```text
Database: hallsync_db
MySQL: 8.0.46
Login: hallsync_app@127.0.0.1
Active role: ...hallsync_application...
Session time zone: +05:30
TLS: enabled
Departments (up to 100):
1 - Industrial Management
Connection check succeeded.
```

The exact server version suffix, role formatting and department identifiers may differ. The checker uses SELECT and SHOW statements. A successful connection is the first milestone; the earlier database verification confirms the complete schema.

## 5. Understand the new files and concepts

| File | Responsibility | Concept to explain |
| --- | --- | --- |
| `pom.xml` | Declares Java target, Connector/J and the build plugins. | Maven is a build tool; it is not an OOP concept. |
| `DatabaseConfig.java` | Loads and validates the local settings. | Encapsulation: private final fields, controlled construction and getters. `final` prevents field reassignment. |
| `DatabaseConnectionFactory.java` | Uses settings to open a JDBC connection with UTF-8, TLS and Sri Lanka time. | Abstraction: callers use `open()`. Composition: a factory holds a configuration object. |
| `ConnectionCheck.java` | Runs the example, reads the session information and departments, and reports success or failure. | Objects and method calls; JDBC interfaces and exception handling. |
| `db.example.properties` | Safe configuration template with no usable password. | Separates settings from source code. |
| `01_create_application_login.sql` | Provisions the database connection account. | Database privileges and roles; separate from application user roles. |
| `tests/connection_smoke.py` | Tests the packaged program against a temporary MySQL instance in GitHub Actions. | Integration testing; this Python helper is not the main application. |
| `../.github/workflows/application.yml` | Builds Java and runs the connection checks on GitHub. | Automation, using a temporary test database and read-only repository access. |

Read the Java classes in the order `DatabaseConfig`, `DatabaseConnectionFactory`, then `ConnectionCheck`.

`new DatabaseConnectionFactory(config)` creates an object; its constructor receives the configuration. `factory.open()` returns a JDBC `Connection`. `PreparedStatement` binds the row limit with `setInt`, keeping the SQL structure separate from its value. `ResultSet` holds returned rows; `next()` moves through them.

Each `try (...)` is **try-with-resources**: Java closes the connection, statement or result set automatically, including on errors. `SQLException` represents database failures. The public error output includes SQLState and numeric codes without printing the password.

No inheritance hierarchy is needed for this lesson. Later we will justify domain classes and a `BookingRepository` interface using their actual responsibilities.

## 6. Troubleshooting

| Result | What to check |
| --- | --- |
| `java`, `javac` or `mvn` not recognised | JDK/Maven installation and Windows PATH; reopen the terminal after changing PATH. |
| Configuration error | Run from `application`, create `db.local.properties`, and replace the password placeholder. |
| SQLState `28000`, code `1045` | Database username/password, account host and required encrypted connection. |
| SQLState beginning `08` | MySQL service, host, port, connection timeout and TLS availability. |
| Code `1049` | The database name is wrong or setup has not completed. |
| Code `1142` or `1370` | The role/grants or default-role activation need inspection. Do not solve this by running the application as root. |
| Code `1146` | Check the selected schema and that the database installation completed. |
| `No suitable driver` (code 0) | Run the packaged JAR built by Maven, rather than running a single Java file without the driver. |
| Login creation says the user already exists | Keep the account and inspect its grants; creating another password or deleting the database is not the next step. |

TLS mode REQUIRED encrypts this local connection but does not verify server identity. For a remote deployment, configure trusted certificates and VERIFY_IDENTITY. Keep database credentials in a trusted backend; this local learning checker should not be distributed with a shared privileged database password.

## 7. What comes next

Send the version outputs first, then the connection result after running the steps. Never send the password or the contents of `db.local.properties`.

After the connection works on your laptop, the next lesson is `Department` and `DepartmentRepository`: map database rows into Java objects and explain why each class exists. Booking operations and the interface will follow that foundation.

## Official references

- [MySQL Connector/J 9.7.0 release and MySQL 8.0 compatibility](https://dev.mysql.com/doc/relnotes/connector-j/en/news-9-7-0.html)
- [Connector/J through Maven](https://dev.mysql.com/doc/connector-j/en/connector-j-installing-maven.html)
- [Connector/J time-zone properties](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-datetime-types-processing.html)
- [MySQL CREATE USER and generated passwords](https://dev.mysql.com/doc/refman/8.0/en/create-user.html)
- [MySQL roles and default roles](https://dev.mysql.com/doc/refman/8.0/en/roles.html)
- [Maven installation](https://maven.apache.org/install.html)
