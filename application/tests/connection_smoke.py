"""Exercise the actual packaged JDBC checker on a disposable CI MySQL server."""
from pathlib import Path
import importlib.util
import os
import json
import secrets
from argon2 import PasswordHasher
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

            application_password = "கற்றல்-" + secrets.token_urlsafe(24)
            encoded = PasswordHasher(time_cost=2, memory_cost=19456, parallelism=1).hash(application_password)
            actor = db_checks.scalar(c, "SELECT AccountID FROM " + schema + ".USER_ACCOUNT WHERE Role='AdminStaff' AND IsActive=1 ORDER BY AccountID LIMIT 1")
            department = db_checks.scalar(c, "SELECT MIN(DepartmentID) FROM " + schema + ".DEPARTMENT")
            for username, account_role in [('ci_admin','AdminStaff'), ('ci_student','Student'), ('ci_lecturer','Lecturer'), ('ci_inactive','AdminStaff'), ('ci_bad_hash','Student')]:
                db_checks.query(c, f"CALL {schema}.sp_provision_account(%s,%s,%s,%s,%s,%s,%s,@fixture_id)",
                                (actor,department,username,username+'@example.invalid',username,encoded,account_role))
            db_checks.query(c, f"UPDATE {schema}.USER_ACCOUNT SET IsActive=0 WHERE Username='ci_inactive'")
            db_checks.query(c, f"UPDATE {schema}.USER_ACCOUNT SET PasswordHash='$argon2id$malformed' WHERE Username='ci_bad_hash'")
            classpath = os.pathsep.join([str(ROOT / 'application/target/test-classes'),
                                        str(ROOT / 'application/target/hallsync-connection-check.jar')])
            feature_env = dict(env, HALLSYNC_RUN_INTEGRATION_TESTS='1',
                               HALLSYNC_APPLICATION_TEST_PASSWORD=application_password)
            feature = subprocess.run(['java','-cp',classpath,'lk.ac.kln.hallsync.DepartmentIntegrationCheck',str(config)],
                                     env=feature_env,capture_output=True,text=True,timeout=120)
            assert feature.returncode == 0, feature.stdout + feature.stderr
            assert application_password not in feature.stdout + feature.stderr
            assert password not in feature.stdout + feature.stderr
            print(feature.stdout, end='')
            passed = [line.removeprefix('PASS - ') for line in feature.stdout.splitlines() if line.startswith('PASS - ')]
            java_version = next(line.split('=',1)[1] for line in feature.stdout.splitlines() if line.startswith('TEST_JAVA_VERSION='))
            report = dict(java_version=java_version, mysql_version=db_checks.scalar(c,'SELECT VERSION()'),
                          checks_passed=len(passed), checks=passed)
            (ROOT/'application/target/department-validation.json').write_text(json.dumps(report,indent=2))
            menu = subprocess.run(['java','-cp',str(ROOT/'application/target/hallsync-connection-check.jar'),
                                   'lk.ac.kln.hallsync.DepartmentApp',str(config)],
                                  input='1\n2\n1\n3\nIndustrial\n4\n0\n', env=env,
                                  capture_output=True,text=True,timeout=45)
            assert menu.returncode == 0, menu.stderr
            assert 'Industrial Management' in menu.stdout
            assert 'Sign in as an administrator' in menu.stdout
            assert 'Goodbye.' in menu.stdout
            print('Department menu: list, find, filter, guest authorization and clean exit passed.')
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
