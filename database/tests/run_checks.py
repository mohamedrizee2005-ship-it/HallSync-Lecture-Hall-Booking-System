"""Validate an isolated MySQL schema. Never resets hallsync_db or any existing schema."""
from pathlib import Path
from datetime import date,timedelta,datetime,time
from concurrent.futures import ThreadPoolExecutor
import os,json,secrets,uuid,threading,time as clock,hashlib
import pymysql
from argon2 import PasswordHasher

ROOT=Path(__file__).resolve().parents[1]
REPORT_DIR=ROOT/'tests'
CONFIG=dict(host=os.getenv('HALLSYNC_TEST_HOST','127.0.0.1'),
 user=os.getenv('HALLSYNC_TEST_USER','root'),
 password=os.getenv('HALLSYNC_TEST_PASSWORD',''),autocommit=True,charset='utf8mb4')
checks=[]
def connect(db=None,user=None,password=None):
    opts=dict(CONFIG)
    if db:opts['database']=db
    if user:opts.update(user=user,password=password)
    return pymysql.connect(**opts)
def query(c,sql,args=()):
    with c.cursor() as cur:
        cur.execute(sql,args or None)
        rows=cur.fetchall()
        while cur.nextset():pass
        return rows
def scalar(c,sql,args=()):return query(c,sql,args)[0][0]
def call(c,name,args=(),out=False):
    markers=['%s']*len(args)+(['@hs_out'] if out else [])
    rows=query(c,'CALL '+name+'('+','.join(markers)+')',args)
    return scalar(c,'SELECT @hs_out') if out else rows
def ok(name,condition):
    if not condition:raise AssertionError(name)
    checks.append({'name':name,'passed':True})
def error(name,fn,code=1644):
    try:fn()
    except pymysql.MySQLError as e:
        if e.args[0]!=code:raise AssertionError((name,'wrong error',e.args)) from e
    else:raise AssertionError(name+' unexpectedly succeeded')
    ok(name,True)
def statements(text):
    delimiter=';';buf=[]
    for line in text.splitlines():
        s=line.strip()
        if not s or s.startswith('--'):continue
        if s.upper().startswith('DELIMITER '):
            if buf:raise ValueError('Unterminated SQL before DELIMITER')
            delimiter=s.split(None,1)[1];continue
        buf.append(line)
        if s.endswith(delimiter):
            command='\n'.join(buf).rstrip()
            yield command[:-len(delimiter)].strip();buf=[]
    if buf:raise ValueError('Unterminated SQL at end')
def install(c,text,schema,role):
    text=text.replace('hallsync_db',schema).replace('hallsync_application',role)
    for command in statements(text):
        try:query(c,command)
        except Exception:
            print('FAILED SQL:',command[:500],flush=True);raise
def metadata(c,db):
    cols=query(c,"SELECT TABLE_NAME,COLUMN_NAME,ORDINAL_POSITION,COLUMN_TYPE,IS_NULLABLE,COLUMN_DEFAULT,EXTRA,COLUMN_KEY FROM information_schema.columns WHERE table_schema=%s AND table_name IN (SELECT table_name FROM information_schema.tables WHERE table_schema=%s AND table_type='BASE TABLE') ORDER BY TABLE_NAME,ORDINAL_POSITION",(db,db))
    columns=[dict(zip(['table','column','ordinal','type','nullable','default','extra','key'],r)) for r in cols]
    fks=query(c,"SELECT TABLE_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.key_column_usage WHERE table_schema=%s AND referenced_table_name IS NOT NULL ORDER BY TABLE_NAME,COLUMN_NAME",(db,))
    constraints=query(c,"SELECT t.TABLE_NAME,c.CONSTRAINT_NAME,c.CHECK_CLAUSE FROM information_schema.check_constraints c JOIN information_schema.table_constraints t ON c.CONSTRAINT_SCHEMA=t.CONSTRAINT_SCHEMA AND c.CONSTRAINT_NAME=t.CONSTRAINT_NAME WHERE c.CONSTRAINT_SCHEMA=%s ORDER BY t.TABLE_NAME,c.CONSTRAINT_NAME",(db,))
    return dict(columns=columns,foreign_keys=[dict(zip(['table','column','parent','parent_column'],r)) for r in fks],
      constraints=[dict(zip(['table','name','clause'],r)) for r in constraints])
def main():
    REPORT_DIR.mkdir(exist_ok=True)
    installer=(ROOT/'HallSync_Full_Setup.sql').read_text()
    initial_schema=installer.split('-- SECTION 3:',1)[0]
    seed='-- SECTION 6:'+installer.split('-- SECTION 6:',1)[1].split('-- SECTION 7:',1)[0]
    suffix=uuid.uuid4().hex[:10];db='hallsync_validation_'+suffix;role='hs_app_'+suffix
    c=connect();created=[];roles=[];api_user=None
    try:
        install(c,initial_schema,db,role)
        created.append(db)
        query(c,"INSERT INTO DEPARTMENT(DepartmentName) VALUES('Industrial Management')")
        install(c,(ROOT/'HallSync_Full_Setup.sql').read_text(),db,role);roles.append(role)
        m=metadata(c,db)
        ok('Upgrade preserves existing department ID 1',scalar(c,"SELECT DepartmentName FROM DEPARTMENT WHERE DepartmentID=1")=='Industrial Management')
        ok('12 tables',scalar(c,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=%s AND table_type='BASE TABLE'",(db,))==12)
        ok('69 columns',len(m['columns'])==69)
        for kind,count in [('PRIMARY KEY',12),('FOREIGN KEY',16),('UNIQUE',8),('CHECK',32)]:
            ok(str(count)+' '+kind,scalar(c,"SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=%s AND constraint_type=%s",(db,kind))==count)
        ok('4 views',scalar(c,"SELECT COUNT(*) FROM information_schema.views WHERE table_schema=%s",(db,))==4)
        ok('31 procedures',scalar(c,"SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema=%s",(db,))==31)
        initial_users=scalar(c,'SELECT COUNT(*) FROM USER_ACCOUNT')
        install(c,seed,db,role)
        ok('Demo seed can repeat without duplicate accounts',scalar(c,'SELECT COUNT(*) FROM USER_ACCOUNT')==initial_users)
        a=scalar(c,"SELECT AccountID FROM USER_ACCOUNT WHERE Username='demo_admin_a'")
        b=scalar(c,"SELECT AccountID FROM USER_ACCOUNT WHERE Username='demo_admin_b'")
        l=scalar(c,"SELECT AccountID FROM USER_ACCOUNT WHERE Username='demo_lecturer'")
        s=scalar(c,"SELECT AccountID FROM USER_ACCOUNT WHERE Username='demo_student'")
        h=scalar(c,"SELECT h.HallID FROM LECTURE_HALL h JOIN BUILDING b ON b.BuildingID=h.BuildingID WHERE b.BuildingCode='HS-DEMO'")
        f=scalar(c,"SELECT FacilityID FROM FACILITY WHERE FacilityName='Projector'")
        course=scalar(c,"SELECT CourseID FROM COURSE_UNIT WHERE CourseCode='HS-DEMO-101'")
        group=scalar(c,"SELECT GroupID FROM STUDENT_GROUP WHERE GroupName='HallSync Demo Group A'")
        day=date.today()+timedelta(days=30)
        def book(conn=c,actor=l,hall=h,course_id=None,d=day,start='09:00',end='10:00',purpose='Test',kind='Event',attendance=30,groups=None):
            return call(conn,'sp_create_booking',(actor,hall,course_id,d,start,end,purpose,kind,attendance,json.dumps(groups or [])),True)
        def review(k,actor=a,decision='Approved',conn=c):
            return call(conn,'sp_review_booking',(actor,k,decision,None))
        def edit(k,actor=l,hall=h,course_id=None,d=day,start='11:00',end='12:00',kind='Event',groups=None):
            return call(c,'sp_update_pending_booking',(actor,k,hall,course_id,d,start,end,'Edited',kind,20,json.dumps(groups or [])))
        def block(d,start='09:00',end='10:00',actor=a,conn=c):
            return call(conn,'sp_create_hall_block',(actor,h,datetime.combine(d,time.fromisoformat(start)),datetime.combine(d,time.fromisoformat(end)),'Maintenance'),True)
        for name,sql,code in [
          ('Blank department',"INSERT INTO DEPARTMENT(DepartmentName) VALUES('   ')",3819),
          ('NULL department',"INSERT INTO DEPARTMENT(DepartmentName) VALUES(NULL)",1048),
          ('Missing parent',"INSERT INTO LECTURE_HALL(BuildingID,HallNo,Capacity) VALUES(2147483647,'INVALID',20)",1452),
          ('Zero capacity',f'UPDATE LECTURE_HALL SET Capacity=0 WHERE HallID={h}',3819),
          ('Duplicate hall facility',f'INSERT INTO HALL_FACILITY VALUES({h},{f},1,1)',1062),
          ('Working exceeds total',f'UPDATE HALL_FACILITY SET WorkingQuantity=3 WHERE HallID={h}',3819),
          ('Zero facility quantity',f'UPDATE HALL_FACILITY SET Quantity=0 WHERE HallID={h}',3819)]:
            error(name,lambda sql=sql:query(c,sql),code)
        cases=[
          ('Student request',dict(actor=s)),('Attendance zero',dict(attendance=0)),
          ('Fractional attendance',dict(attendance=1.5)),('Capacity overflow',dict(attendance=101)),
          ('Blank purpose',dict(purpose='   ')),('Equal times',dict(end='09:00')),
          ('Overnight times',dict(start='23:00',end='01:00')),('Past start',dict(d=day-timedelta(days=40))),
          ('Lecture missing course',dict(kind='Lecture',groups=[group])),
          ('Tutorial missing group',dict(kind='Tutorial',course_id=course)),
          ('Unknown course',dict(course_id=2147483647)),
          ('Unknown group',dict(kind='Lecture',course_id=course,groups=[2147483647])),
          ('Duplicate groups',dict(kind='Lecture',course_id=course,groups=[group,group])),
          ('String group ID',dict(kind='Lecture',course_id=course,groups=[str(group)]))]
        for name,kw in cases:error(name,lambda kw=kw:book(**kw))
        ok('Failed requests leave no booking rows',scalar(c,'SELECT COUNT(*) FROM BOOKING')==0)
        k1=book(kind='Lecture',course_id=course,groups=[group])
        k2=book(start='09:30',end='10:30')
        ok('Overlapping Pending requests allowed',scalar(c,"SELECT COUNT(*) FROM BOOKING WHERE BookingStatus='Pending'")==2)
        ok('Group created with booking',scalar(c,'SELECT COUNT(*) FROM BOOKING_GROUP WHERE BookingID=%s',(k1,))==1)
        error('Student cannot approve',lambda:review(k1,actor=s))
        review(k1)
        ok('Approval metadata consistent',query(c,'SELECT BookingStatus,ReviewerID,Decision FROM BOOKING WHERE BookingID=%s',(k1,))[0]==('Approved',a,'Approved'))
        error('Conflicting second approval',lambda:review(k2,actor=b))
        ok('Failed review rolls back',query(c,'SELECT BookingStatus,ReviewerID FROM BOOKING WHERE BookingID=%s',(k2,))[0]==('Pending',None))
        error('Approved edit forbidden',lambda:edit(k1))
        adjacent=book(start='10:00',end='11:00');review(adjacent)
        ok('Adjacent intervals allowed',scalar(c,'SELECT BookingStatus FROM BOOKING WHERE BookingID=%s',(adjacent,))=='Approved')
        error('Block cannot override approval',lambda:block(day,start='09:45',end='10:15'))
        error('Capacity shrink cannot break reservation',lambda:call(c,'sp_update_hall',(a,h,20,'Active')))
        error('Inactive cannot break reservation',lambda:call(c,'sp_update_hall',(a,h,100,'Inactive')))
        error('Other requester cannot cancel',lambda:call(c,'sp_cancel_booking',(s,k1)))
        call(c,'sp_cancel_booking',(l,k1))
        ok('Cancellation retains approval',query(c,'SELECT BookingStatus,Decision,ReviewerID FROM BOOKING WHERE BookingID=%s',(k1,))[0]==('Cancelled','Approved',a))
        ok('Cancellation retains groups',scalar(c,'SELECT COUNT(*) FROM BOOKING_GROUP WHERE BookingID=%s',(k1,))==1)
        error('Cancelled terminal',lambda:review(k1))
        error('Group history protects parent',lambda:query(c,'DELETE FROM STUDENT_GROUP WHERE GroupID=%s',(group,)),1451)
        error('Account history protects parent',lambda:query(c,'DELETE FROM USER_ACCOUNT WHERE AccountID=%s',(l,)),1451)
        own=book(actor=a,d=day+timedelta(days=1))
        error('Self approval prohibited',lambda:review(own,actor=a))
        review(own,actor=b)
        ok('Different admin may approve staff request',scalar(c,'SELECT ReviewerID FROM BOOKING WHERE BookingID=%s',(own,))==b)
        error('Cannot complete before session end',lambda:call(c,'sp_complete_booking',(a,own)))
        d3=day+timedelta(days=2)
        pending=book(d=d3)
        blk=call(c,'sp_create_hall_block',(a,h,datetime.combine(d3-timedelta(days=1),time(22)),datetime.combine(d3,time(10)),'Multi-day closure'),True)
        error('Approval rechecks multi-day closure',lambda:review(pending))
        call(c,'sp_cancel_hall_block',(a,blk));review(pending)
        ok('Cancelled closure releases availability',scalar(c,'SELECT BookingStatus FROM BOOKING WHERE BookingID=%s',(pending,))=='Approved')
        error('Closure cancellation is terminal',lambda:call(c,'sp_cancel_hall_block',(a,blk)))
        error('Lecturer cannot create closure',lambda:block(d3,start='12:00',end='13:00',actor=l))
        d4=day+timedelta(days=3);p=book(d=d4)
        error('Only owner edits Pending',lambda:edit(p,actor=a,d=d4))
        edit(p,course_id=course,d=d4,start='10:00',end='11:00',kind='Tutorial',groups=[group])
        ok('Pending edit updates groups',scalar(c,'SELECT COUNT(*) FROM BOOKING_GROUP WHERE BookingID=%s',(p,))==1)
        error('Invalid edit rolls back',lambda:edit(p,course_id=course,d=d4,kind='Tutorial'))
        ok('Old values retained after failed edit',scalar(c,'SELECT StartTime FROM BOOKING WHERE BookingID=%s',(p,))==timedelta(hours=10))
        review(p,decision='Rejected')
        error('Rejected terminal',lambda:review(p))
        error('Rejected cannot cancel',lambda:call(c,'sp_cancel_booking',(l,p)))
        call(c,'sp_cancel_booking',(l,k2))
        ok('Pending cancellation needs no review',scalar(c,'SELECT ReviewerID FROM BOOKING WHERE BookingID=%s',(k2,)) is None)
        query(c,"INSERT INTO BOOKING(RequesterID,HallID,BookingDate,StartTime,EndTime,Purpose,BookingType,ExpectedAttendance,BookingStatus,RequestedAt,ReviewerID,Decision,ReviewedAt) VALUES(%s,%s,DATE_SUB(CURRENT_DATE(),INTERVAL 1 DAY),'09:00','10:00','Historical fixture','Event',10,'Approved',DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 3 DAY),%s,'Approved',DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 2 DAY))",(l,h,a))
        past=scalar(c,'SELECT LAST_INSERT_ID()');call(c,'sp_complete_booking',(a,past))
        ok('Ended approval can complete',scalar(c,'SELECT BookingStatus FROM BOOKING WHERE BookingID=%s',(past,))=='Completed')
        error('Completed terminal',lambda:review(past))
        hash_value=scalar(c,'SELECT PasswordHash FROM USER_ACCOUNT WHERE AccountID=%s',(l,))
        error('Student cannot provision lecturer',lambda:call(c,'sp_provision_account',(s,1,'New','new@example.invalid','new_user',hash_value,'Lecturer'),True))
        error('Plaintext password refused',lambda:call(c,'sp_register_student',(1,'New','new@example.invalid','new_user','plaintext'),True))
        student=call(c,'sp_register_student',(1,'New','student2@example.invalid','student_two',hash_value),True)
        ok('Public registration fixes Student role',scalar(c,'SELECT Role FROM USER_ACCOUNT WHERE AccountID=%s',(student,))=='Student')
        lecturer=call(c,'sp_provision_account',(a,1,'New Lecturer','lecturer2@example.invalid','lecturer_two',hash_value,'Lecturer'),True)
        ok('Admin provisions privileged role',scalar(c,'SELECT Role FROM USER_ACCOUNT WHERE AccountID=%s',(lecturer,))=='Lecturer')
        query(c,'UPDATE USER_ACCOUNT SET IsActive=FALSE WHERE AccountID=%s',(lecturer,))
        error('Inactive requester refused',lambda:book(actor=lecturer,d=d4,start='14:00',end='15:00'))
        call(c,'sp_set_hall_facility',(a,h,f,2,0))
        ok('Working quantity zero allowed',scalar(c,'SELECT WorkingQuantity FROM HALL_FACILITY WHERE HallID=%s AND FacilityID=%s',(h,f))==0)
        error('Invalid inventory update',lambda:call(c,'sp_set_hall_facility',(a,h,f,2,3)))
        error('Student cannot manage inventory',lambda:call(c,'sp_set_hall_facility',(s,h,f,2,1)))
        error('Lecturer cannot add hall',lambda:call(c,'sp_add_hall',(l,1,'EXTRA','1',50),True))
        newhall=call(c,'sp_add_hall',(a,1,'EXTRA','1',50),True)
        call(c,'sp_update_hall',(a,newhall,40,'Inactive'))
        error('Inactive hall refuses requests',lambda:book(hall=newhall,d=d4))
        call(c,'sp_update_hall',(a,newhall,40,'Active'))
        ok('Unreserved hall state can change',scalar(c,'SELECT Capacity FROM LECTURE_HALL WHERE HallID=%s',(newhall,))==40)
        ok('Details view has one row per booking',scalar(c,'SELECT COUNT(*) FROM v_booking_details')==scalar(c,'SELECT COUNT(*) FROM BOOKING'))
        ok('Status report totals match',scalar(c,'SELECT SUM(BookingCount) FROM v_booking_status_totals')==scalar(c,'SELECT COUNT(*) FROM BOOKING'))
        # True independent-connection races.
        def race(functions):
            gate=threading.Barrier(2)
            def worker(fn):
                x=connect(db)
                try:
                    gate.wait(timeout=15);fn(x);return 0
                except pymysql.MySQLError as exc:return exc.args[0]
                finally:x.close()
            with ThreadPoolExecutor(2) as pool:return list(pool.map(worker,functions))
        for i in range(5):
            d=day+timedelta(days=100+i)
            x=book(d=d);y=book(d=d,start='09:15',end='10:15')
            results=race([lambda conn,x=x:review(x,actor=a,conn=conn),lambda conn,y=y:review(y,actor=b,conn=conn)])
            ok('Concurrent conflicting approvals '+str(i+1),sorted(results)==[0,1644])
            ok('Race leaves exactly one Approved '+str(i+1),scalar(c,"SELECT COUNT(*) FROM BOOKING WHERE BookingID IN (%s,%s) AND BookingStatus='Approved'",(x,y))==1)
        for i in range(3):
            d=day+timedelta(days=120+i);x=book(d=d)
            results=race([lambda conn,x=x:review(x,actor=a,conn=conn),lambda conn,d=d:block(d,actor=b,conn=conn)])
            ok('Concurrent approval versus closure '+str(i+1),sorted(results)==[0,1644])
        held=connect(db);held.begin();query(held,'SELECT HallID FROM LECTURE_HALL WHERE HallID=%s FOR UPDATE',(h,))
        wait_booking=book(hall=newhall,d=day+timedelta(days=140))
        # Use newhall for the waiting proof so request creation does not need the held hall.
        held.rollback();query(held,'START TRANSACTION');query(held,'SELECT HallID FROM LECTURE_HALL WHERE HallID=%s FOR UPDATE',(newhall,))
        def waiting():
            x=connect(db)
            try:review(wait_booking,conn=x)
            finally:x.close()
        with ThreadPoolExecutor(1) as pool:
            future=pool.submit(waiting);clock.sleep(.3)
            ok('Approval waits for the common hall lock',not future.done())
            held.commit();future.result(timeout=15)
        held.close()
        api_user='hs_api_'+suffix;api_password=secrets.token_urlsafe(24)
        query(c,"CREATE USER %s@'%%' IDENTIFIED WITH mysql_native_password BY %s",(api_user,api_password))
        query(c,"GRANT '"+role+"' TO '"+api_user+"'@'%'")
        query(c,"SET DEFAULT ROLE '"+role+"' TO '"+api_user+"'@'%'")
        api=connect(db,api_user,api_password)
        error('Application cannot insert raw bookings',lambda:query(api,"INSERT INTO BOOKING(BookingID) VALUES(999999)"),1142)
        error('Application cannot read raw password table',lambda:query(api,'SELECT * FROM USER_ACCOUNT'),1142)
        error('Application cannot call internal helper',lambda:call(api,'_hs_require_actor',(a,'ADMIN')),1370)
        ok('Application can read approved reporting interface',isinstance(query(api,'SELECT * FROM v_booking_status_totals'),tuple))
        call(api,'sp_find_available_halls',(day+timedelta(days=180),'09:00','10:00',10));ok('Application can call availability interface',True)
        ok('Restricted login can read lecturer registration readiness',call(api,'sp_get_lecturer_registration_status')==((1,),))
        # Lecturer registration is atomic and never grants active access before review.
        def applicant(n,department=1,staff='STAFF-001',username=None,email=None):
            return call(api,'sp_register_lecturer_application',(department,'Applicant '+str(n),email or f'applicant{n}@example.invalid',username or 'applicant'+str(n),hash_value,staff),True)
        def app_account(k):return scalar(c,'SELECT AccountID FROM LECTURER_APPLICATION WHERE ApplicationID=%s',(k,))
        def decide(k,actor=a,decision='Approved',note=None,verified=True,conn=api):
            return call(conn,'sp_review_lecturer_application',(actor,k,decision,note,verified))
        k=applicant(1);who=app_account(k)
        ok('Lecturer self-registration creates an inactive lecturer',query(c,'SELECT Role,IsActive FROM USER_ACCOUNT WHERE AccountID=%s',(who,))[0]==('Lecturer',0))
        error('Pending applicant cannot request a hall through SQL',lambda:book(actor=who,d=day+timedelta(days=200)))
        error('Student cannot inspect lecturer applications',lambda:call(api,'sp_list_lecturer_applications',(s,None)))
        error('Lecturer cannot approve a lecturer application',lambda:decide(k,actor=l))
        error('Student cannot approve a lecturer application',lambda:decide(k,actor=s))
        error('Approval requires independent identity verification',lambda:decide(k,verified=False))
        error('Rejected applications require a reason',lambda:decide(k,decision='Rejected',note='   '))
        ok('Failed review leaves pending account inactive',query(c,'SELECT ApplicationStatus,IsActive FROM LECTURER_APPLICATION x JOIN USER_ACCOUNT u ON u.AccountID=x.AccountID WHERE ApplicationID=%s',(k,))[0]==('Pending',0))
        listing=call(api,'sp_list_lecturer_applications',(a,'Pending'))
        ok('Admin application list excludes hashes',len(listing)>=1 and '$argon2id$' not in str(listing))
        decide(k,note='Confirmed with the lecturer through the faculty office.')
        ok('Account approval and audit data commit together',query(c,'SELECT ApplicationStatus,ReviewedByID,IsActive FROM LECTURER_APPLICATION x JOIN USER_ACCOUNT u ON u.AccountID=x.AccountID WHERE ApplicationID=%s',(k,))[0]==('Approved',a,1))
        own_booking=book(actor=who,d=day+timedelta(days=201))
        ok('Approved lecturer can request a hall but booking remains pending',scalar(c,'SELECT BookingStatus FROM BOOKING WHERE BookingID=%s',(own_booking,))=='Pending')
        error('Reviewed application cannot be decided twice',lambda:decide(k))
        rejected=applicant(2);decide(rejected,decision='Rejected',note='Staff identity could not be confirmed.',verified=False)
        ok('Rejection retains history and blocks access',query(c,'SELECT ApplicationStatus,ReviewNote,IsActive FROM LECTURER_APPLICATION x JOIN USER_ACCOUNT u ON u.AccountID=x.AccountID WHERE ApplicationID=%s',(rejected,))[0]==('Rejected','Staff identity could not be confirmed.',0))
        error('Rejected application cannot be reactivated through review',lambda:decide(rejected))
        before_accounts=scalar(c,'SELECT COUNT(*) FROM USER_ACCOUNT')
        error('Duplicate application username is refused',lambda:applicant(3,username='applicant1'),1062)
        error('Duplicate application email is refused',lambda:applicant(3,email='applicant1@example.invalid'),1062)
        error('Registration needs a department',lambda:applicant(3,department=None))
        error('Registration refuses blank staff ID',lambda:applicant(3,staff='   '))
        error('Registration refuses control characters in staff ID',lambda:applicant(3,staff='ID\nBAD'))
        ok('Failed registration leaves no orphan account',scalar(c,'SELECT COUNT(*) FROM USER_ACCOUNT')==before_accounts)
        error('Private application table cannot be read directly',lambda:query(api,'SELECT * FROM LECTURER_APPLICATION'),1142)
        for n in range(3):
            racing=applicant(20+n);barrier=threading.Barrier(2)
            def competing(actor,decision):
                x=connect(db,api_user,api_password)
                try:
                    barrier.wait(timeout=5);decide(racing,actor=actor,decision=decision,note='Independent review',verified=True,conn=x);return True
                except pymysql.MySQLError as e:
                    if e.args[0]!=1644:raise
                    return False
                finally:x.close()
            with ThreadPoolExecutor(2) as pool:
                fs=[pool.submit(competing,a,'Approved'),pool.submit(competing,b,'Rejected')]
                outcomes=[f.result(timeout=15) for f in fs]
            status,active=query(c,'SELECT ApplicationStatus,IsActive FROM LECTURER_APPLICATION x JOIN USER_ACCOUNT u ON u.AccountID=x.AccountID WHERE ApplicationID=%s',(racing,))[0]
            ok('Concurrent lecturer review has one consistent decision '+str(n+1),sum(outcomes)==1 and active==(status=='Approved'))
        api.close()
        # Fresh install follows the same canonical schema and procedures.
        fresh=db+'_fresh';fresh_role=role+'_fresh'
        install(c,(ROOT/'HallSync_Full_Setup.sql').read_text(),fresh,fresh_role);created.append(fresh);roles.append(fresh_role)
        ok('Fresh installation creates 12 tables',scalar(c,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=%s AND table_type='BASE TABLE'",(fresh,))==12)
        ok('Fresh installation creates 69 columns',len(metadata(c,fresh)['columns'])==69)
        query(c,'USE '+db)
        ok('No approved overlaps remain',scalar(c,"SELECT COUNT(*) FROM BOOKING x JOIN BOOKING y ON x.HallID=y.HallID AND x.BookingDate=y.BookingDate AND x.BookingID<y.BookingID AND x.StartTime<y.EndTime AND x.EndTime>y.StartTime WHERE x.BookingStatus='Approved' AND y.BookingStatus='Approved'")==0)
        report=dict(mysql_version=scalar(c,'SELECT VERSION()'),checks_passed=len(checks),checks=checks,
          tables=12,columns_count=69,primary_keys=12,foreign_keys_count=16,unique_keys=8,check_constraints=32,views=4,procedures=31,
          concurrency={'approval_races':5,'approval_block_races':3,'lock_wait_checked':True},
          department_preserved=True,**m)
        (REPORT_DIR/'database_validation.json').write_text(json.dumps(report,indent=2,default=str))
        print('VALIDATION PASSED',len(checks),'checks; MySQL',report['mysql_version'],flush=True)
    finally:
        if api_user:
            try:query(c,"DROP USER IF EXISTS '"+api_user+"'@'%'")
            except Exception:pass
        for r in roles:
            try:query(c,"DROP ROLE IF EXISTS '"+r+"'")
            except Exception:pass
        # Only schemas created with this run's generated names are removed.
        for name in created:
            if name.startswith('hallsync_validation_'+suffix):query(c,'DROP DATABASE IF EXISTS '+name)
        c.close()
if __name__=='__main__':main()
