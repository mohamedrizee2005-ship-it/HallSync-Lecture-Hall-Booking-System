# HallSync Java application

Department operations: list, find by ID, filter by name and add after an active AdminStaff account signs in. Java classes are separate under `src/main/java/lk/ac/kln/hallsync`.

## VS Code

Open the repository root, not one Java file. Install the recommended **Extension Pack for Java** (`vscjava.vscode-java-pack`). The shared `.vscode` settings provide Maven build and terminal launch configurations.

Use **Run and Debug** and choose **HallSync: Department menu**. Password entry uses the Integrated Terminal. Choose **HallSync: Connection check** for connection diagnostics.

## Existing local connection

Keep your working `application/db.local.properties`. The safe template is `db.example.properties`; real settings are ignored by Git. A first installation needs the database and the limited login from `01_create_application_login.sql`.

From a terminal inside `application`:

```cmd
mvn clean package
java -jar target/hallsync-connection-check.jar
java -cp target/hallsync-connection-check.jar lk.ac.kln.hallsync.DepartmentApp
```

JDK 17 or newer is required; JDK 21 and 26 are checked in CI. Maven downloads MySQL Connector/J and Password4j.

## Application administrator setup

The MySQL connection login `hallsync_app` is different from an application account in `USER_ACCOUNT`. Existing synthetic demo account passwords were discarded.

If you already have a usable active AdminStaff account, sign in with it. Otherwise, initialize your own project administrator:

```cmd
java -cp target/hallsync-connection-check.jar lk.ac.kln.hallsync.auth.AdminPasswordTool
```

Enter and confirm a private 15-128 character application password. The tool prints an Argon2id hash. Copy `02_provision_project_admin.example.sql` to `02_provision_project_admin.local.sql`, replace its hash placeholder privately, and execute that setup section once using your Workbench administrator connection. Sign in to the menu as `project_admin` with the password you chose.

Keep local configuration, local setup SQL and hash output private. The shared example intentionally has no usable password or hash. If an initialization query fails, preserve the error rather than rerunning the full database installer.

## Structure

- `model`: department row data.
- `repository`: department contract and parameterized JDBC implementation.
- `service`: input validation and administrator authorization.
- `auth`: private login lookup, Argon2id verification and authenticated identity.
- `ui`: menu and hidden terminal password input.
- `DepartmentApp`: constructs and connects those objects.
- `ConnectionCheck`: diagnostic entry point using the same repository.

The separate learning PDF explains the files in study order and includes VS Code, SQL mapping, OOP, testing and presentation questions.
