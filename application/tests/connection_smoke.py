"""Exercise the actual packaged JDBC checker on a disposable CI MySQL server."""
from pathlib import Path
import importlib.util
import os
import json
from datetime import date, timedelta
import secrets
from argon2 import PasswordHasher
import subprocess
import tempfile
import uuid
import zipfile
from browser_checks import run as browser_checks

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
    bootstrap_user = "hs_bootstrap_" + suffix
    bootstrap_created = False
    try:
        full_sql=(ROOT/'database/HallSync_Setup.sql').read_text()
        upgrade_sql=full_sql
        db_checks.install(c,full_sql,schema,role)
        # Simulate the exact 2.2 account application table shape before this upgrade.
        db_checks.query(c,f'RENAME TABLE {schema}.ACCOUNT_APPLICATION TO {schema}.LECTURER_APPLICATION')
        db_checks.query(c,f'ALTER TABLE {schema}.LECTURER_APPLICATION DROP COLUMN RequestedRole')
        db_checks.query(c,f'DROP TABLE {schema}.ROOM_ISSUE')
        assert db_checks.scalar(c,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=%s AND table_type='BASE TABLE'",(schema,))==12
        # Preserve an existing reservation as well as every original account/password.
        a=db_checks.scalar(c,f"SELECT AccountID FROM {schema}.USER_ACCOUNT WHERE Username='demo_lecturer'")
        h=db_checks.scalar(c,f"SELECT MIN(HallID) FROM {schema}.LECTURE_HALL")
        db_checks.query(c,f"CALL {schema}.sp_create_booking(%s,%s,NULL,%s,'20:00','21:00','Migration preservation probe','Event',1,JSON_ARRAY(),@upgrade_probe)",(a,h,date.today()+timedelta(days=91)))
        def snapshot():
            return [db_checks.query(c,f'SELECT * FROM {schema}.{table} ORDER BY 1') for table in ['USER_ACCOUNT','BOOKING','DEPARTMENT','BUILDING']]
        before=snapshot();db_checks.install(c,upgrade_sql,schema,role);db_checks.install(c,upgrade_sql,schema,role)
        assert snapshot()==before,'Lecturer upgrade changed existing data'
        print('12-table installation upgrades safely; users, password hashes, department IDs and reservations are retained.')
        campus=json.loads((ROOT/'application/src/main/resources/campus.json').read_text())
        for faculty in campus['faculties']:
            for name in faculty['departments']:
                assert db_checks.scalar(c,f"SELECT COUNT(*) FROM {schema}.DEPARTMENT WHERE DepartmentName=%s",(name,))==1
        print('Kelaniya reference seed includes every sourced department without duplicate names.')
        login_sql=full_sql[full_sql.index('-- SECTION 9:'):].replace('hallsync_db',schema).replace('hallsync_application',role).replace("'hallsync_app'","'"+user+"'").replace("'127.0.0.1'","'%'")
        db_checks.query(c,'SET @hallsync_manage_login=1')
        password=None
        for sql in db_checks.statements(login_sql):
            rows=db_checks.query(c,sql)
            if sql.startswith('CREATE USER'):
                user_created=True
                assert rows and len(rows[0])==3,'Installer must return a generated login password'
                password=rows[0][2]
        # Repeating the installer login section must preserve that password.
        for sql in db_checks.statements(login_sql):db_checks.query(c,sql)
        db_checks.query(c,'SET @hallsync_manage_login=0')
        assert password, "MySQL must return a generated private password"
        with zipfile.ZipFile(ROOT/"application/target/hallsync.jar") as archive:
            manifest=archive.read("META-INF/MANIFEST.MF").decode()
            assert "Main-Class: lk.ac.kln.hallsync.HallSyncApp" in manifest
        print("Executable JAR manifest points to the HallSync browser launcher.")
        with tempfile.TemporaryDirectory() as directory:
            config = Path(directory) / "db.local.properties"
            config.write_text(
                f"db.url=jdbc:mysql://127.0.0.1:3306/{schema}\ndb.user={user}\n", encoding="utf-8"
            )
            env = dict(os.environ, HALLSYNC_DB_PASSWORD=password)
            command = ["java", "-cp", str(ROOT / "application/target/hallsync.jar"), "lk.ac.kln.hallsync.ConnectionCheck", str(config)]
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

            counts_before = db_checks.query(c, f"SELECT (SELECT COUNT(*) FROM {schema}.USER_ACCOUNT),(SELECT COUNT(*) FROM {schema}.DEPARTMENT)")
            upgrade = full_sql
            db_checks.install(c,upgrade,schema,role)
            db_checks.install(c,upgrade,schema,role)
            assert counts_before == db_checks.query(c, f"SELECT (SELECT COUNT(*) FROM {schema}.USER_ACCOUNT),(SELECT COUNT(*) FROM {schema}.DEPARTMENT)")
            print('Desktop read upgrade preserves existing rows and can repeat safely.')

            application_password = "கற்றல்-" + secrets.token_urlsafe(24)
            encoded = PasswordHasher(time_cost=2, memory_cost=19456, parallelism=1).hash(application_password)
            actor = db_checks.scalar(c, "SELECT AccountID FROM " + schema + ".USER_ACCOUNT WHERE Role='AdminStaff' AND IsActive=1 ORDER BY AccountID LIMIT 1")
            department = db_checks.scalar(c, "SELECT MIN(DepartmentID) FROM " + schema + ".DEPARTMENT")
            for username, account_role in [('ci_admin','AdminStaff'), ('ci_student','Student'), ('ci_lecturer','Lecturer'), ('ci_inactive','AdminStaff'), ('ci_bad_hash','Student')]:
                db_checks.query(c, f"CALL {schema}.sp_provision_account(%s,%s,%s,%s,%s,%s,%s,@fixture_id)",
                                (actor,department,username,username+'@example.invalid',username,encoded,account_role))
            db_checks.query(c, f"UPDATE {schema}.USER_ACCOUNT SET IsActive=0 WHERE Username='ci_inactive'")
            db_checks.query(c, f"UPDATE {schema}.USER_ACCOUNT SET PasswordHash='$argon2id$malformed' WHERE Username='ci_bad_hash'")
            ended_day = date.today() - timedelta(days=2)
            fixture_hall = db_checks.scalar(c,f"SELECT MIN(HallID) FROM {schema}.LECTURE_HALL")
            fixture_requester = db_checks.scalar(c,f"SELECT AccountID FROM {schema}.USER_ACCOUNT WHERE Username='ci_lecturer'")
            fixture_reviewer = db_checks.scalar(c,f"SELECT AccountID FROM {schema}.USER_ACCOUNT WHERE Username='ci_admin'")
            db_checks.query(c,f"INSERT INTO {schema}.BOOKING(RequesterID,HallID,BookingDate,StartTime,EndTime,Purpose,BookingType,ExpectedAttendance,BookingStatus,RequestedAt,ReviewerID,Decision,ReviewedAt) VALUES(%s,%s,%s,'08:00','09:00','CI ended approved session','Event',10,'Approved',%s,%s,'Approved',%s)",
                            (fixture_requester,fixture_hall,ended_day,str(ended_day)+' 06:00:00',fixture_reviewer,str(ended_day)+' 07:00:00'))
            bootstrap_rows = db_checks.query(c,f"CREATE USER '{bootstrap_user}'@'%' IDENTIFIED BY RANDOM PASSWORD REQUIRE SSL")
            bootstrap_created = True
            bootstrap_password = bootstrap_rows[0][2]
            db_checks.query(c,f"GRANT ALL PRIVILEGES ON {schema}.* TO '{bootstrap_user}'@'%'")
            classpath = os.pathsep.join([str(ROOT / 'application/target/test-classes'),
                                        str(ROOT / 'application/target/hallsync.jar')])
            feature_env = dict(env, HALLSYNC_RUN_INTEGRATION_TESTS='1',
                               HALLSYNC_APPLICATION_TEST_PASSWORD=application_password,
                               HALLSYNC_BOOTSTRAP_TEST_USER=bootstrap_user,HALLSYNC_BOOTSTRAP_TEST_PASSWORD=bootstrap_password)
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
            workflows = subprocess.run(['java','-cp',classpath,'lk.ac.kln.hallsync.FullApplicationIntegrationCheck',str(config)],
                                       env=feature_env,capture_output=True,text=True,timeout=180)
            assert workflows.returncode == 0, workflows.stdout + workflows.stderr
            assert password not in workflows.stdout + workflows.stderr and application_password not in workflows.stdout + workflows.stderr
            print(workflows.stdout,end='')
            # Simulate an existing installation without the new reference names.
            db_checks.query(c,f"DELETE FROM {schema}.DEPARTMENT WHERE DepartmentName='Chemistry'")
            db_checks.query(c,f"DELETE FROM {schema}.BUILDING WHERE BuildingCode='B1'")
            web = subprocess.run(['java','-cp',classpath,'lk.ac.kln.hallsync.WebIntegrationCheck',str(config)],
                                 cwd=ROOT,env=feature_env,capture_output=True,text=True,timeout=180)
            assert web.returncode == 0, web.stdout + web.stderr
            assert password not in web.stdout + web.stderr and application_password not in web.stdout + web.stderr
            assert bootstrap_password not in web.stdout + web.stderr
            print(web.stdout,end='')
            browser = browser_checks(config,feature_env)
            before=snapshot();applications_before=db_checks.query(c,f'SELECT * FROM {schema}.ACCOUNT_APPLICATION ORDER BY ApplicationID')
            db_checks.install(c,upgrade_sql,schema,role)
            assert snapshot()==before and applications_before==db_checks.query(c,f'SELECT * FROM {schema}.ACCOUNT_APPLICATION ORDER BY ApplicationID')
            print('Repeating the lecturer upgrade also preserves application review history.')
            workflow_checks = [line.removeprefix('PASS - ') for line in workflows.stdout.splitlines() if line.startswith('PASS - ')]
            web_checks = [line.removeprefix('PASS - ') for line in web.stdout.splitlines() if line.startswith('PASS - ')]
            complete_report = dict(java_version=java_version,mysql_version=report['mysql_version'],
                                   department_checks=passed,workflow_checks=workflow_checks,web_checks=web_checks,browser_checks=browser,
                                   total_checks=len(passed)+len(workflow_checks)+len(web_checks)+len(browser),read_upgrade_preserves_data=True,lecturer_upgrade_preserves_data=True)
            (ROOT/'application/target/application-validation.json').write_text(json.dumps(complete_report,indent=2))
        print("JDBC integration passed: generated login, default role, encrypted connection, timezone, department read, invalid-password rejection and missing-config handling.")
    finally:
        if bootstrap_created:
            db_checks.query(c,f"DROP USER IF EXISTS '{bootstrap_user}'@'%'")
        if user_created:
            db_checks.query(c, f"DROP USER IF EXISTS '{user}'@'%'")
        # Names are unique to this invocation; never delete hallsync_db.
        db_checks.query(c, "DROP DATABASE IF EXISTS " + schema)
        db_checks.query(c, f"DROP ROLE IF EXISTS '{role}'")
        c.close()


if __name__ == "__main__":
    main()
