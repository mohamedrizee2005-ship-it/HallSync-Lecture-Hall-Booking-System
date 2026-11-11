"""Exercise the actual packaged JDBC checker on a disposable CI MySQL server."""
from pathlib import Path
import importlib.util
import os
import subprocess
import tempfile
import uuid

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("database_checks", ROOT / "database/tests/run_checks.py")
db_checks = importlib.util.module_from_spec(spec)
spec.loader.exec_module(db_checks)


def main():
    suffix = uuid.uuid4().hex[:10]
    schema = "hallsync_connection_" + suffix
    role = "hs_connection_" + suffix
    user = "hs_java_" + suffix
    c = db_checks.connect()
    user_created = False
    try:
        db_checks.install(c, (ROOT / "database/HallSync_Full_Setup.sql").read_text(), schema, role)
        login_sql = (ROOT / "application/01_create_application_login.sql").read_text()
        # The CI service receives TCP connections through Docker's bridge.
        # The unique temporary CI login uses %, while the laptop script stays loopback-only.
        login_sql = login_sql.replace("hallsync_db", schema).replace("hallsync_application", role)
        login_sql = login_sql.replace("hallsync_app", user).replace("'127.0.0.1'", "'%'")
        password = None
        for sql in db_checks.statements(login_sql):
            rows = db_checks.query(c, sql)
            if sql.startswith("CREATE USER"):
                user_created = True
                password = rows[0][2]
        assert password, "MySQL must return a generated private password"
        with tempfile.TemporaryDirectory() as directory:
            config = Path(directory) / "db.local.properties"
            config.write_text(
                f"db.url=jdbc:mysql://127.0.0.1:3306/{schema}\ndb.user={user}\n", encoding="utf-8"
            )
            env = dict(os.environ, HALLSYNC_DB_PASSWORD=password)
            command = ["java", "-jar", str(ROOT / "application/target/hallsync-connection-check.jar"), str(config)]
            good = subprocess.run(command, env=env, capture_output=True, text=True, timeout=45)
            assert good.returncode == 0, good.stderr
            assert f"Database: {schema}" in good.stdout
            assert role in good.stdout
            assert "Session time zone: +05:30" in good.stdout
            assert "TLS: enabled" in good.stdout
            assert "Industrial Management" in good.stdout
            assert "Connection check succeeded." in good.stdout
            assert password not in good.stdout + good.stderr
            print(good.stdout, end="")
            bad = subprocess.run(command, env=dict(env, HALLSYNC_DB_PASSWORD="invalid-" + suffix),
                                 capture_output=True, text=True, timeout=45)
            assert bad.returncode != 0 and "code=1045" in bad.stderr
            assert password not in bad.stdout + bad.stderr
            missing = subprocess.run(command[:-1] + [str(config.parent / "missing.properties")], env=env,
                                     capture_output=True, text=True, timeout=45)
            assert missing.returncode != 0 and "Configuration error" in missing.stderr
        print("JDBC integration passed: generated login, default role, encrypted connection, timezone, department read, invalid-password rejection and missing-config handling.")
    finally:
        if user_created:
            db_checks.query(c, f"DROP USER IF EXISTS '{user}'@'%'")
        # Names are unique to this invocation; never delete hallsync_db.
        db_checks.query(c, "DROP DATABASE IF EXISTS " + schema)
        db_checks.query(c, f"DROP ROLE IF EXISTS '{role}'")
        c.close()


if __name__ == "__main__":
    main()
