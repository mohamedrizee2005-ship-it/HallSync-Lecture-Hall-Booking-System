"""Check the default, non-demo install on a generated disposable schema."""
from pathlib import Path
import uuid
import run_checks as db

suffix=uuid.uuid4().hex[:10]
schema='hallsync_normal_'+suffix
role='hs_normal_'+suffix
c=db.connect()
try:
    sql=(Path(__file__).resolve().parents[1]/'HallSync_Setup.sql').read_text()
    # The shared test helper enables fixtures; explicitly exercise the shipped default instead.
    sql=sql.replace('SET @hallsync_test_mode=COALESCE(@hallsync_test_mode,0);','SET @hallsync_test_mode=0;')
    db.install(c,sql,schema,role)
    assert db.scalar(c,'SELECT COUNT(*) FROM USER_ACCOUNT')==0,'A normal install must not create demo accounts'
    assert db.scalar(c,'SELECT COUNT(*) FROM BOOKING')==0,'A normal install must not invent timetable reservations'
    assert db.scalar(c,"SELECT COUNT(*) FROM LECTURE_HALL WHERE OperationalStatus='Active'")==14
    assert db.scalar(c,'SELECT SUM(Capacity) FROM LECTURE_HALL')==1495
    assert db.scalar(c,'SELECT COUNT(*) FROM HALL_FACILITY')==0,'Do not invent per-room working equipment quantities'
    before=db.query(c,'SELECT * FROM LECTURE_HALL ORDER BY HallID')
    db.install(c,sql,schema,role)
    assert before==db.query(c,'SELECT * FROM LECTURE_HALL ORDER BY HallID'),'Repeating normal setup must preserve hall IDs and values'
    print('PASS - Default setup seeds 14 published-capacity halls, no demo users, no invented timetable or equipment, and repeats safely.')
finally:
    db.query(c,'DROP DATABASE IF EXISTS '+schema)
    db.query(c,"DROP ROLE IF EXISTS '"+role+"'")
    c.close()
