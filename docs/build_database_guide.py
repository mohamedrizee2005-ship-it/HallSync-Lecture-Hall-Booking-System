"""Build the readable guide from the schema actually validated in CI."""
from pathlib import Path
from collections import defaultdict
import json,html,textwrap,math
from reportlab.platypus import SimpleDocTemplate,Paragraph,Spacer,Table,TableStyle,PageBreak,Preformatted,Flowable,KeepTogether,CondPageBreak
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet,ParagraphStyle
from reportlab.lib.pagesizes import A4
from pypdf import PdfReader

ROOT=Path(__file__).resolve().parents[1];DOCS=ROOT/'docs'
data=json.loads((DOCS/'database_validation.json').read_text())
manifest=json.loads((ROOT/'database/procedure_manifest.json').read_text())
NAVY=colors.HexColor('#123354');BLUE=colors.HexColor('#1763A0');PALE=colors.HexColor('#EDF4FA');GREY=colors.HexColor('#536477')
styles=getSampleStyleSheet()
styles.add(ParagraphStyle(name='BodyGuide',fontName='Helvetica',fontSize=9.5,leading=14,spaceAfter=8,textColor=NAVY))
styles.add(ParagraphStyle(name='TitleGuide',fontName='Helvetica-Bold',fontSize=25,leading=30,textColor=NAVY,spaceAfter=18))
styles.add(ParagraphStyle(name='HGuide',fontName='Helvetica-Bold',fontSize=17,leading=22,textColor=BLUE,spaceAfter=13,keepWithNext=True))
styles.add(ParagraphStyle(name='SubGuide',fontName='Helvetica-Bold',fontSize=11.5,leading=16,textColor=NAVY,spaceBefore=10,spaceAfter=7,keepWithNext=True))
styles.add(ParagraphStyle(name='CellGuide',fontName='Helvetica',fontSize=8.4,leading=11.5,textColor=NAVY))
styles.add(ParagraphStyle(name='HeadCell',fontName='Helvetica-Bold',fontSize=8.5,leading=11.5,textColor=colors.white))
styles.add(ParagraphStyle(name='CodeGuide',fontName='Courier',fontSize=7.5,leading=10.5,textColor=NAVY,leftIndent=6,spaceAfter=10))
story=[];md=[]
def esc(s):return html.escape(str(s))
def p(text):
    story.append(Paragraph(esc(text),styles['BodyGuide']));md.append(text+'\n')
def heading(text,new=True):
    if new and story:story.append(CondPageBreak(520))
    story.append(Paragraph(esc(text),styles['HGuide']));md.append('\n## '+text+'\n')
def sub(text):
    story.append(Paragraph(esc(text),styles['SubGuide']));md.append('\n### '+text+'\n')
def code(text):
    lines=[]
    for line in text.strip().splitlines():
        lines.extend(textwrap.wrap(line,99,subsequent_indent='    ',replace_whitespace=False,drop_whitespace=False) or [''])
    story.append(Preformatted('\n'.join(lines),styles['CodeGuide']));md.append('\n~~~sql\n'+text.strip()+'\n~~~\n')
def table(headers,rows,widths=None):
    md.append('| '+' | '.join(headers)+' |')
    md.append('| '+' | '.join(['---']*len(headers))+' |')
    for row in rows:md.append('| '+' | '.join(str(x).replace('|','/') for x in row)+' |')
    md.append('')
    cells=[[Paragraph(esc(h),styles['HeadCell']) for h in headers]]
    cells += [[Paragraph(esc(x),styles['CellGuide']) for x in row] for row in rows]
    t=Table(cells,colWidths=widths,repeatRows=1,hAlign='LEFT')
    t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),BLUE),('VALIGN',(0,0),(-1,-1),'TOP'),
      ('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,PALE]),
      ('LEFTPADDING',(0,0),(-1,-1),7),('RIGHTPADDING',(0,0),(-1,-1),7),
      ('TOPPADDING',(0,0),(-1,-1),6),('BOTTOMPADDING',(0,0),(-1,-1),6),
      ('LINEBELOW',(0,0),(-1,0),.7,BLUE),('LINEBELOW',(0,1),(-1,-1),.2,colors.HexColor('#D9E5EF'))]))
    story.append(t);story.append(Spacer(1,10))
def bullets(items):
    for x in items:p('- '+x)
class States(Flowable):
    def __init__(self):Flowable.__init__(self);self.width=500;self.height=245
    def draw(self):
        c=self.canv
        boxes={'Pending':(35,175,135,42),'Approved':(295,175,135,42),
          'Rejected':(15,40,130,42),'Cancelled':(185,40,130,42),'Completed':(355,40,130,42)}
        def arrow(x1,y1,x2,y2,label,dx=0,dy=0):
            c.setStrokeColor(BLUE);c.setLineWidth(1.2);c.line(x1,y1,x2,y2)
            a=math.atan2(y2-y1,x2-x1)
            path=c.beginPath();path.moveTo(x2,y2)
            for ang in [a+2.65,a-2.65]:path.lineTo(x2+8*math.cos(ang),y2+8*math.sin(ang))
            path.close();c.setFillColor(BLUE);c.drawPath(path,fill=1,stroke=0)
            c.setFont('Helvetica',8);c.setFillColor(NAVY);c.drawCentredString((x1+x2)/2+dx,(y1+y2)/2+dy,label)
        arrow(170,196,295,196,'approve',0,9)
        arrow(80,175,80,82,'reject',-25,0)
        arrow(130,175,225,82,'cancel',-14,-5)
        arrow(330,175,275,82,'cancel',20,0)
        arrow(400,175,420,82,'after end',30,0)
        for name,(x,y,w,h) in boxes.items():
            c.setFillColor(PALE if name in ['Pending','Approved'] else colors.HexColor('#E7ECEF'))
            c.setStrokeColor(BLUE);c.roundRect(x,y,w,h,6,fill=1,stroke=1)
            c.setFillColor(NAVY);c.setFont('Helvetica-Bold',10);c.drawCentredString(x+w/2,y+17,name)

story.append(Spacer(1,72))
story.append(Paragraph('HallSync',styles['TitleGuide']))
story.append(Paragraph('Complete Database Implementation Guide',styles['HGuide']))
p('Lecture Hall Booking Management System')
p('Faculty of Science prototype | University of Kelaniya, Sri Lanka')
p('MySQL 8.0.46 | Database implementation v2 | 11 November 2026')
table(['Tables','Columns','Foreign keys'],[['11','61','14']], [169,169,169])
p('A study guide for understanding the database, running the complete setup, explaining the rules in a presentation, and connecting an object-oriented application later.')
p('HallSync_Full_Setup.sql creates the database when missing, preserves existing department rows and completes the database without dropping tables.')
p('Validation: '+str(data['checks_passed'])+' checks passed against MySQL '+data['mysql_version']+'. This includes independent-connection approval races, approval-versus-closure races, and a hall-lock waiting check.')
p('The scripts are complete in the repository. They create the database objects on your laptop only when you execute them in Workbench.')

heading('1. Start here: the one-file full setup')
p('Use database/HallSync_Full_Setup.sql once. It supports a new database, an empty hallsync_db, or your matching DEPARTMENT-only database.')
bullets(['Download the latest repository ZIP and extract it to a normal writable folder.',
'Open your existing MySQL Workbench connection and open HallSync_Full_Setup.sql.',
'Enable Stop Script Execution On Errors. Execute the complete file once with no text selected.',
'If an error appears, stop and send the error message. Do not drop the database or blindly rerun the file.',
'Refresh SCHEMAS. The final verification grid should show the expected object counts below.'])
table(['Object','Expected'],[['Base tables','11'],['Columns in base tables','61'],['Primary keys','11'],['Foreign keys','14'],['Additional unique keys','7'],['CHECK constraints','28'],['Views','4'],['Procedures','25 (19 public + 6 internal)']],[330,177])
p('The full setup creates the database and DEPARTMENT table when missing. Its preflight then requires exactly that matching table before adding the remaining objects. It stops over a partially completed, fully installed or different schema; do not rerun it after success.')
p('MySQL DDL can commit independently. A multi-statement installation is not one rollbackable transaction. Preserve the error output if installation stops partway through.')
code('USE hallsync_db;\nSHOW TABLES;\nSELECT COUNT(*) AS TableCount FROM information_schema.tables\nWHERE table_schema = DATABASE() AND table_type = \'BASE TABLE\';')

heading('2. Files and execution order')
table(['File','Purpose and when to run'],[
['HallSync_Full_Setup.sql','Recommended setup. Creates the database, tables, procedures, views, demo records and application role, then verifies them. Run once.'],
['01_create_database.sql','Earlier lesson: creates the database. The full setup already handles this step.'],
['02_create_department.sql','Earlier lesson: creates the first table. The full setup already handles this step.'],
['03_insert_sample_department.sql','Earlier sample-data lesson. Repeating its INSERT creates another department row.'],
['complete_existing_database.sql','Earlier alternative for a matching department-only database; not an additional step after the full setup.'],
['fresh_install.sql','Earlier alternative requiring an unused database name; not an additional step after the full setup.'],
['04_complete_schema.sql','Individual script: validates DEPARTMENT and adds the other 10 tables.'],
['05_business_workflows.sql','Individual script: installs public procedures and internal helpers.'],
['06_views_and_reports.sql','Individual script: creates four reporting views.'],
['07_demo_data.sql','Individual script: synthetic catalogue and SQL demonstration accounts; keeps existing matching records.'],
['08_verify_database.sql','Read-only structure and data-integrity checks; safe to run after setup.'],
['09_workflow_example.sql','Optional changing-data example: creates and approves a future demo Event request.'],
['10_application_role.sql','Creates a least-privilege MySQL role; does not create a login or password.'],
['tests/run_checks.py','Developer validation. Creates isolated generated schemas, tests them, then removes only those schemas.']
],[174,333])
p('For normal setup, run only HallSync_Full_Setup.sql once. The numbered files and older combined installers remain lesson or compatibility references. After success, 08_verify_database.sql can recheck the installation without changing data.')
p('Every setup/business connection uses Asia/Colombo civil time through the +05:30 offset. This works without loading named MySQL timezone tables. Application connections must follow the same convention.')

heading('3. The design: nine entities become eleven tables')
p('Each strong entity becomes a table. Each 1:N relationship stores the parent identifier in a child foreign-key column. Two M:N relationships need junction tables, bringing the total from nine entities to eleven tables.')
purposes={
'DEPARTMENT':'Department catalogue shared by accounts, courses and groups.',
'BUILDING':'Building codes and locations used to identify lecture halls.',
'FACILITY':'Equipment types, such as Projector. Quantities belong to hall assignments.',
'USER_ACCOUNT':'People, account activation, one effective role and password hashes.',
'COURSE_UNIT':'Course catalogue and its owning department.',
'STUDENT_GROUP':'Teaching groups by department and intake, not individual enrolment.',
'LECTURE_HALL':'Physical hall identity, capacity and operational status.',
'BOOKING':'One request, one responsible requester, one hall and at most one review decision.',
'HALL_BLOCK':'Dated maintenance or other hall closures, including multi-day periods.',
'HALL_FACILITY':'Hall-to-facility M:N assignment, with total and working quantities.',
'BOOKING_GROUP':'Booking-to-student-group M:N assignment.'}
order=list(purposes)
table(['Table','Purpose'],[[t,purposes[t]] for t in order],[135,372])
p('Key legend: PRI = primary key, UNI = unique key, FK = foreign key. Req. means NOT NULL.')
p('A report is a query over existing data, so the design has no stored REPORT entity. The four views provide report datasets. SQL tables describe stored facts; OOP classes will also describe behaviour and do not have to map one-to-one to tables.')
p('Names are not substituted for identifiers. For example, a booking stores RequesterID and HallID, while joins retrieve the current account and hall names.')

descriptions={
'DepartmentID':'Department identifier. Auto-generated in DEPARTMENT; a parent reference elsewhere.',
'DepartmentName':'Required catalogue name. The CHECK rejects empty strings and ordinary-space-only names. Names are not a declared unique key.',
'BuildingID':'Building identifier; halls reference the building that contains them.',
'BuildingCode':'Stable unique code used for building lookup.',
'BuildingName':'Required readable building name.',
'LocationDescription':'Optional description of the building location.',
'FacilityID':'Facility-type identifier. The same type can occur in several halls.',
'FacilityName':'Unique type name, such as Projector.',
'Description':'Optional explanation of the facility type.',
'AccountID':'Account identifier. Reused as requester, reviewer or block recorder through differently named FKs.',
'FullName':'Required display name.',
'Email':'Unique account email; application validates email format.',
'Username':'Unique login identifier; the database default collation makes lookup case-insensitive.',
'PasswordHash':'Encoded salted password hash. ASCII binary collation preserves its case-sensitive representation.',
'Role':'One effective role: Student, Lecturer or AdminStaff. Privileged roles are provisioned by an administrator.',
'IsActive':'BOOLEAN alias for TINYINT(1). A CHECK restricts values to 0 or 1.',
'CourseID':'Course identifier. Optional in a booking overall; mandatory for Lecture and Tutorial.',
'CourseCode':'Unique catalogue code.',
'CourseTitle':'Required readable course title.',
'GroupID':'Student-group identifier.',
'GroupName':'Teaching-group label; its department/intake/name combination is unique.',
'IntakeYear':'Unsigned small integer, checked between 1900 and 9999.',
'StudyYear':'Unsigned tiny integer, checked to be at least 1.',
'HallID':'Lecture-hall identifier. Every booking and hall block refers to one hall.',
'HallNo':'Room identifier, unique within its building rather than across all buildings.',
'Floor':'Optional text so values such as G or 1 can be represented.',
'Capacity':'Positive integer capacity. Procedures compare attendance with this parent value.',
'OperationalStatus':'Active or Inactive. Time availability is calculated from reservations and blocks.',
'BookingID':'Unique identifier for one request, including requests retained after cancellation or rejection.',
'RequesterID':'Responsible account. Supplied from the authenticated session; there is no separate booked-for lecturer.',
'BookingDate':'Session date, interpreted with StartTime and EndTime in Asia/Colombo.',
'StartTime':'Same-day start clock time. Procedures require the combined start instant to be in the future.',
'EndTime':'Same-day end clock time, strictly after StartTime and before 24:00.',
'Purpose':'Required reason, limited to 255 characters.',
'BookingType':'Lecture, Tutorial, Exam or Event. Academic types impose extra course/group requirements.',
'ExpectedAttendance':'Positive integer expected headcount. Individual student enrolments are outside v1.',
'BookingStatus':'Current lifecycle state: Pending, Approved, Rejected, Cancelled or Completed.',
'RequestedAt':'Local date/time when the request was recorded.',
'ReviewerID':'Optional administrator reference; different from RequesterID when a review exists.',
'Decision':'Approved or Rejected. Preserve an earlier approval after later cancellation.',
'ReviewedAt':'Optional local review timestamp; cannot be before RequestedAt.',
'ReviewNote':'Optional review explanation, up to 500 characters.',
'BlockID':'Unique hall-closure identifier.',
'RecordedByID':'Administrator who recorded the closure.',
'BlockStart':'Closure start date/time; can be on a different date from BlockEnd.',
'BlockEnd':'Closure end date/time, strictly later than BlockStart.',
'Reason':'Required explanation of the closure.',
'BlockStatus':'Active or Cancelled. Cancellation retains the closure record.',
'Quantity':'Positive integer total of this facility type in this hall.',
'WorkingQuantity':'Integer between zero and Quantity inclusive; zero working units is valid.'}
bytable=defaultdict(list)
for col in data['columns']:bytable[col['table'].upper()].append(col)
dictionary_order=['DEPARTMENT','BUILDING','FACILITY','USER_ACCOUNT','COURSE_UNIT','STUDENT_GROUP','LECTURE_HALL','HALL_FACILITY','BOOKING','HALL_BLOCK','BOOKING_GROUP']
for index,t in enumerate(dictionary_order,1):
    heading(str(index+3)+'. Data dictionary: '+t,new=index not in [2,3,6,8,11])
    p(purposes[t])
    rows=[]
    for col in sorted(bytable[t],key=lambda x:x['ordinal']):
        typ=col['type'].replace(',',', ')
        if col['column']=='IsActive':typ='BOOLEAN / TINYINT(1)'
        key=col['key'] or '-'
        if t in ['HALL_FACILITY','BOOKING_GROUP']:key='PK part + FK'
        elif any(x['table'].upper()==t and x['column']==col['column'] for x in data['foreign_keys']):key='FK'+(' + UNIQUE' if key=='UNI' else '')
        detail=descriptions.get(col['column'],'Stored attribute.')
        if col['extra']=='auto_increment':detail+=' MySQL generates this primary-key value; gaps are allowed.'
        if col['default'] is not None:detail+=' Default: '+str(col['default'])+'.'
        rows.append([col['column'],typ,'Yes' if col['nullable']=='NO' else 'No',key,detail])
    table(['Column','SQL type','Req.','Key','Meaning / reason'],rows,[100,104,48,55,200])
    if t=='USER_ACCOUNT':
        p('PasswordHash stores no plaintext password. Hashing and password verification use a maintained Argon2id library in the application. The procedure checks the encoded prefix and length, not the original password or its strength.')
    if t=='BOOKING':
        p('A review is stored in this row because the agreed workflow allows at most one review decision. The reviewer ID, decision and review time are populated together. ReviewNote remains optional. The groups are separate BOOKING_GROUP rows.')
    if t in ['HALL_FACILITY','BOOKING_GROUP']:
        p('The two key columns are unique together. Either individual value can repeat in other rows. This represents an M:N relationship without a comma-separated list.')
    if t=='LECTURE_HALL':
        p('The hall has no permanent Booked flag. One hall can be reserved for one period and free for another. OperationalStatus describes the physical hall, while availability is calculated for the requested interval.')

heading('15. Primary keys, foreign keys and normalisation')
table(['Child table.column','Parent table.column'],[[r['table']+'.'+r['column'],r['parent']+'.'+r['parent_column']] for r in data['foreign_keys']],[260,247])
p('A foreign key checks that a referenced parent exists. It does not prove that an account is an administrator, that attendance fits the hall, or that two time intervals do not overlap. Those checks belong in the business procedures and authenticated application flow.')
p('RequesterID and ReviewerID both reference USER_ACCOUNT.AccountID but describe different relationships. They are not new entities. A reviewer can review many bookings; an ordinary many-side FK must therefore not be made UNIQUE.')
p('The junctions prevent repeated pairs. For example, one booking can serve groups 10 and 11, and group 10 can appear in other bookings. Only the duplicate pair is prohibited.')
p('Parent records referenced by history use default InnoDB NO ACTION behaviour. Normal application permissions prohibit raw booking deletes. Deactivating a user or hall is different from deleting the referenced record.')

heading('16. Constraints and data types')
table(['Choice','Reason'],[
['INT identifiers','Compact surrogate keys; generated IDs identify rows and are not record counts.'],
['VARCHAR lengths','Bound the size of text values. The chosen limits are project design choices, not measured university limits.'],
['DATE + TIME','Same-day bookings use a date and two clock times. Overnight bookings are outside this version.'],
['DATETIME','Records request, review and hall-closure local timestamps. DATETIME itself stores no timezone identifier.'],
['ENUM','Restricts the known role, status, type and decision vocabulary for this prototype.'],
['NULL','Represents an optional or not-yet-known fact. It differs from an empty string and from zero.'],
['NOT NULL + CHECK','NOT NULL rejects a missing value; a length check also rejects empty or ordinary-space-only text.'],
['utf8mb4','Unicode storage suitable for multilingual data. Connection encoding must also use utf8mb4.'],
['ascii_bin PasswordHash','Encoded hashes are ASCII and case-sensitive, even though catalogue names use the database collation.'],
['InnoDB','Supports foreign keys, row locks and transactions.']
],[145,362])
p('TRIM in a CHECK is used to evaluate the condition; it does not rewrite the stored column. The public write procedures trim selected human-readable inputs before saving them.')
p('The attendance API accepts a decimal parameter so it can reject a fractional value before inserting the required integer. Other raw form input, including email syntax and password policy, must also be validated by the application.')

heading('17. Row-level CHECK inventory')
p('These 28 checks are taken from the validated MySQL schema. Required foreign-key links and uniqueness rules are additional constraints.')
table(['Table / constraint','Expression'],[[r['table']+' / '+r['name'],r['clause']] for r in data['constraints']],[236,271])
p('A permanent future-date CHECK is intentionally absent: an approved booking must remain valid history after its date passes. Future-start validation happens when creating, editing or approving a request.')

heading('18. Booking lifecycle and retained history')
story.append(States());md.append('Transitions: Pending -> Approved / Rejected / Cancelled. Approved -> Cancelled / Completed. Rejected, Cancelled and Completed are terminal.\n')
table(['State','Rules'],[
['Pending','Can be edited by its requester. Can be approved, rejected or cancelled. Does not reserve a slot.'],
['Approved','Reserves the hall interval. Rescheduling requires cancellation and a new request.'],
['Rejected','Terminal; retains its single rejection decision.'],
['Cancelled','Terminal; retains a previous approval if it had one, along with group links.'],
['Completed','Terminal; reached only from Approved after the session end.']
],[105,402])
p('An authorised administrator can approve or reject another account request. Lecturers and authorised AdminStaff can submit their own requests; an administrator request still needs a different administrator to review it. Public registration creates only Student accounts.')
p('The v1 schema preserves lifecycle records and one decision. It is not a complete audit log of every edit, cancellation actor or previous catalogue value.')

heading('19. Availability and overlapping intervals')
code('newStart < existingEnd AND newEnd > existingStart')
table(['Existing approved session','New request','Result'],[
['09:00-10:00','09:30-10:30','Conflict'],
['09:00-10:00','10:00-11:00','Allowed adjacency'],
['09:00-10:00','08:00-09:00','Allowed adjacency'],
['09:00-10:00','09:00-10:00','Conflict']
],[169,169,169])
p('The interval is half-open: the end of one session can equal the start of another. Booking comparisons use the same hall and booking date. For closures, TIMESTAMP(BookingDate, StartTime/EndTime) allows comparison with multi-day DATETIME block intervals.')
p('An approval also requires an active hall, sufficient capacity, an active eligible requester and a future start instant. Lecture/Tutorial requires a course and at least one valid group.')
p('Availability search is a snapshot. Another transaction can reserve the hall after the screen loads. Approval always rechecks the current data under the common hall lock.')
p('Pending requests can overlap each other. Once one is approved, approving another overlapping request fails and leaves that second request Pending.')

heading('20. Transactions, locks and maintenance priority')
bullets(['Begin a transaction using READ COMMITTED.',
'Lock the relevant LECTURE_HALL row with SELECT ... FOR UPDATE.',
'For an edit moving a Pending request, lock both halls in ascending HallID order.',
'Lock the booking row, confirm its hall has not changed, and validate the actor and lifecycle state.',
'Recheck future time, course/groups, capacity, approved bookings and active hall blocks.',
'Write all related changes together, then COMMIT. On a SQL exception, ROLLBACK and return the error.'])
p('The common hall row is the coordination point. Booking approval and hall-block creation take the same lock. The second operation waits, then sees the first committed result before deciding whether it can proceed.')
p('The selected READ COMMITTED isolation level avoids checking a snapshot created before waiting for the hall lock. Relevant hall-capacity and operational-status changes follow the same protocol.')
p('A maintenance block cannot silently displace an approved booking. Conflicting future reservations must first be cancelled explicitly. A hall cannot become inactive or too small while that would invalidate an existing not-ended approved reservation.')
p('Each public write procedure owns its transaction. Call it with application autocommit enabled and without an existing outer transaction. Handle SQLSTATE 45000 as a business-rule rejection; deadlocks and timeouts need rollback-aware retry. A network timeout after commit must not be blindly retried as a new request.')

heading('21. Public procedure reference')
descs={
'sp_create_booking':'Creates a Pending booking and group links atomically. The actor is the requester.',
'sp_update_pending_booking':'Owner-only Pending edit; can move halls using ordered locking. Replaces its group set atomically.',
'sp_review_booking':'Admin review of Pending; rechecks availability on approval and writes review fields together.',
'sp_cancel_booking':'Owner or admin cancels a future Pending/Approved request, preserving review and group history.',
'sp_complete_booking':'Admin marks an ended Approved session Completed.',
'sp_create_hall_block':'Admin creates a dated closure after checking approved reservations under the hall lock.',
'sp_cancel_hall_block':'Admin cancels an Active closure while retaining its row.',
'sp_update_hall':'Admin changes capacity/status only when approved reservations remain valid.',
'sp_set_hall_facility':'Admin inserts or updates a hall/type pair with valid total and working quantities.',
'sp_register_student':'Public registration contract. Role is fixed to Student; input is already a password hash.',
'sp_provision_account':'Administrator creates an account, including privileged lecturer/staff roles.',
'sp_get_login_record':'Backend-only username lookup for password verification; its hash must never reach public clients.',
'sp_find_available_halls':'Read-only future availability and capacity filter; it does not reserve a slot.',
'sp_add_department':'Admin adds a department catalogue record.',
'sp_add_building':'Admin adds a building catalogue record.',
'sp_add_facility':'Admin adds a facility-type catalogue record.',
'sp_add_course':'Admin adds a course with an existing owning department.',
'sp_add_student_group':'Admin adds a teaching group with department/intake identity.',
'sp_add_hall':'Admin adds a physical hall in an existing building.'}
for item in manifest:
    group_start=len(story)
    sub(item['name'])
    p(descs[item['name']])
    code(item['name']+'('+item['params']+')')
    story[group_start:]=[KeepTogether(story[group_start:])]
p('The six _hs_* helpers are implementation details: actor validation, hall locking, input validation, slot checks, group insertion and account-field validation. Application accounts receive EXECUTE on public procedures only, not helpers.')

heading('22. Run a complete booking example')
p('After setup, the synthetic catalogue contains HS-DEMO / DEMO-01, a practice course, one student group and four demonstration accounts. The codes are invented for testing, not an official University of Kelaniya inventory.')
code("""SET @lecturer = (SELECT AccountID FROM USER_ACCOUNT WHERE Username='demo_lecturer');
SET @admin = (SELECT AccountID FROM USER_ACCOUNT WHERE Username='demo_admin_a');
SET @hall = (SELECT h.HallID FROM LECTURE_HALL h
 JOIN BUILDING b ON b.BuildingID=h.BuildingID
 WHERE b.BuildingCode='HS-DEMO' AND h.HallNo='DEMO-01');
SET @date = DATE_ADD(CURRENT_DATE(), INTERVAL 30 DAY);
CALL sp_create_booking(@lecturer,@hall,NULL,@date,'09:00','10:00',
 'Demo seminar','Event',30,JSON_ARRAY(),@booking);
CALL sp_review_booking(@admin,@booking,'Approved','Checked availability');
SELECT * FROM v_booking_details WHERE BookingID=@booking;""")
p('This Event does not need a course or student group. A Lecture/Tutorial passes an existing CourseID and JSON_ARRAY(group_id, ...). JSON is only an input format; the database stores one BOOKING_GROUP row per group.')
p('Run database/09_workflow_example.sql once if you want this demonstration to create data. Repeating it can create another Pending request or fail because the earlier session is already approved.')
p('To demonstrate cancellation later, call sp_cancel_booking with the requester or an administrator. The status becomes Cancelled while the earlier Approved decision remains recorded.')

heading('23. Views, reports and indexes')
table(['View','Meaning'],[
['v_booking_details','One row per booking, joined with requester, hall/building, optional course and reviewer.'],
['v_hall_facilities','Hall/type assignments with total, working and calculated non-working quantities.'],
['v_booking_status_totals','Counts booking rows by their current state.'],
['v_hall_usage','Counts approved/completed sessions and sums reserved minutes by hall/date.']
],[170,337])
p('Joining a booking to multiple student groups can multiply result rows. Count bookings before joining, use an appropriate distinct count, or aggregate the junction separately. The provided status totals avoid this duplication.')
p('Unique keys cover email, username, building code, facility name, course code, department/intake/group-name, and building/hall-number. The two junction primary keys also enforce unique pairs.')
p('The booking interval index starts with HallID, BookingDate and BookingStatus. The block interval index starts with HallID and BlockStatus. These help find relevant rows; indexes alone do not forbid overlapping intervals.')
p('Reserved minutes are scheduled session time, not actual measured hall use. The system does not track individual student attendance or enrolments.')

heading('24. Security and the application boundary')
bullets(['The database validates roles for the supplied actor ID, but it cannot establish a human login session.',
'The application must verify the password, check activation and derive actor IDs from that authenticated session.',
'Never accept RequesterID or reviewer identity from an editable user form.',
'Use a maintained Argon2id library. Validate password policy before hashing; the hash cannot reveal the original password length.',
'The procedure hash-prefix check is a guard against obvious plaintext input, not proof that an encoded hash is genuine or uses adequate cost settings.',
'Use prepared/parameterised calls in the application and a dedicated database login, not the setup administrator.',
'Assign hallsync_application to that login. It permits approved interfaces and reads, while denying direct table writes and internal helper calls.'])
p('The demo hashes were generated from random discarded passwords. They enable SQL procedure demonstrations by account ID, but do not provide known login credentials. Provision real account credentials during the authentication lesson.')
p('A privileged database administrator can bypass normal application rules through direct SQL or DDL. Operational users must use the restricted application interfaces. A shared database login is not an end-user identity system; production campus deployment should place database credentials in a trusted backend.')
p('A first administrator and existing timetable data are controlled DBA bootstrap operations. Ordinary public registration can never choose Lecturer or AdminStaff.')

heading('25. Verification and test evidence')
p('The read-only 08 script checks metadata and searches for conflicting approved bookings, approved/block overlaps, academic bookings without groups, and future reservations invalidated by hall capacity/status.')
p('This package passed '+str(data['checks_passed'])+' checks on MySQL '+data['mysql_version']+'. The report is docs/database_validation.json. Tests create generated hallsync_validation_* schemas and never reset your hallsync_db.')
table(['Category','What was checked'],[
['Structure','11 tables, 61 columns, 11 PKs, 14 FKs, 7 other unique keys, 28 CHECKs, 4 views and 25 procedures.'],
['Upgrade and fresh setup','Existing department ID/name preserved; demo seed repeats safely; fresh installation builds the same shape.'],
['Integrity','Missing parents, duplicate equipment pairs, blank names, zero capacity and invalid equipment counts.'],
['Business rules','Invalid times, fractional/zero/too-large attendance, course/group requirements, privileges and self-approval.'],
['Lifecycle','Pending edits, rollback of invalid changes, cancellation history and terminal states.'],
['Concurrency','5 simultaneous conflicting-approval races, 3 approval/block races and a check that approval waits on the common hall lock.'],
['Least privilege','Application login cannot insert raw bookings, read the raw password table or call internal helpers.']
],[126,381])
sub('Passed check names')
for start in range(0,len(data['checks']),22):
    if start:story.append(PageBreak())
    table(['Check','Result'],[[r['name'],'PASS'] for r in data['checks'][start:start+22]],[445,62])

heading('26. Timetable setup, scope and limitations')
p('Availability is reliable only after existing future timetable reservations are entered with their responsible requester and a different authorised reviewer. Demo records are not a substitute for the real timetable. Controlled initial import is distinct from an ordinary user booking on behalf of someone else.')
bullets(['No recurring booking series, automatic timetable generation, individual student enrolment tracking or multi-stage approvals.',
'One booking uses one hall and one same-day session. Hall blocks can span multiple days.',
'Faculty of Science is the fixed prototype context; there is no separate faculty catalogue.',
'Reports are derived from stored facts. The model does not store a complete revision-by-revision audit log.',
'Catalogue labels can change over time; the booking keeps identifiers and its own purpose/attendance, not historical snapshots of every parent field.',
'Account credential verification, session handling, UI validation and user-facing messages are application work.',
'No real university hall inventory, staff accounts or timetable has been imported by these scripts.'])
p('Store all system times consistently in Asia/Colombo. A start later today is valid when it is still in the future; an already started session cannot be submitted or approved as a new request.')

heading('27. Troubleshooting in Workbench')
table(['Symptom','Action'],[
['Database/table already exists','You may have run a creation step already. Inspect with 08 and SHOW CREATE TABLE; preserve data instead of resetting.'],
['Preflight expected only DEPARTMENT','Existing tables differ from the supported starting condition, or an earlier installation stopped partway through. Share the exact error and table list.'],
['Syntax error near DELIMITER','Open the SQL file and execute the whole procedure definition. DELIMITER is a client directive, not SQL to send as a server statement.'],
['Procedure does not exist','Inspect setup Action Output, the selected database and the routine list before repairing the installation.'],
['Transaction characteristics cannot change','A write procedure was called within an existing transaction. Resolve that transaction and call the procedure outside an outer transaction.'],
['CHECK / FK / duplicate-key error','Input violates a named rule. Fix the supplied values or create the parent record first.'],
['SQLSTATE 45000','Business-rule rejection. Display its message and leave the existing record unchanged.'],
['Permission denied while saving a file','Use Save Script As in a writable normal folder. This file error is separate from database creation.'],
['Password cannot log in','Demo passwords are intentionally unknown. Real credentials require application-side hashing/provisioning.']
],[183,324])
p('Setup statements can partly succeed before an error. Retain Action Output and request a targeted repair. The scripts do not drop the project database or delete existing department rows.')

heading('28. Presentation questions and answers')
qas=[
('Why 11 tables for nine entities?','The two M:N relationships become HALL_FACILITY and BOOKING_GROUP junction tables.'),
('Why both NOT NULL and CHECK?','NULL and an empty string are different. A required text value also needs an appropriate non-empty rule.'),
('Why not store group IDs in one text column?','Separate junction rows support foreign keys, joins and pair uniqueness without parsing lists.'),
('Does a foreign key prove an administrator role?','It proves that the account exists. Role/activation checks are separate business logic.'),
('Why does Pending not reserve the hall?','Several requests may be reviewed; only an approved reservation guarantees that interval. Approval rechecks under a lock.'),
('Why use a transaction when approving?','The role/time/capacity/conflict checks and review/status update must succeed together or roll back.'),
('Why lock the hall row?','It exists even when no reservation exists yet, giving approvals and blocks a shared coordination point.'),
('Why can 10:00 follow a session ending 10:00?','The overlap test uses strict inequalities, so adjacent half-open intervals do not overlap.'),
('Why keep Decision after cancellation?','BookingStatus describes the current lifecycle, while Decision preserves the single earlier review.'),
('Is a foreign key OOP inheritance?','No. It is a relational reference. Classes, interfaces, encapsulation and polymorphism describe the application design.'),
('What will a BookingRepository do?','Expose save/search/review operations and translate object data into parameterised database calls.'),
('Where are password length rules checked?','Before hashing in the application. Only the salted encoded hash is stored.')]
for q,a in qas:sub(q);p(a)

heading('29. Next: connect an OOP application')
p('After HallSync_Full_Setup.sql succeeds and you confirm the verification results, the next stage is the application model. Start with UserAccount, LectureHall and Booking responsibilities, then an interface such as BookingRepository and a database implementation.')
p('A booking service coordinates authenticated actions. A repository calls the stored procedures and converts their results into objects. The UI presents information and explains rejections. Java can use JDBC with MySQL Connector/J; another chosen OOP language uses its supported MySQL driver.')
p('Encapsulation protects object state, abstraction gives a clear repository contract, and polymorphism allows a test implementation of that contract. Use inheritance only for a justified is-a relationship. The database remains the authority for concurrent reservation decisions.')
p('Your first application connection lesson will create a dedicated database login with a private password, assign hallsync_application, configure the timezone/encoding and test one read. No password belongs in a public repository.')

heading('30. SQL vocabulary used in these scripts')
table(['Keyword / expression','What it means here'],[
['CREATE TABLE / DDL','Defines stored structure. DDL is not the same as inserting department or booking records.'],
['INSERT / UPDATE / DELETE','Change rows. Deleting a Pending booking group set during an edit is distinct from deleting booking history.'],
['PROCEDURE / CALL','A stored operation with parameters; CALL invokes it. Public procedures are the application entry points.'],
['IN / OUT parameters','IN supplies a value. OUT returns a generated identifier through a caller variable.'],
['DECLARE versus @variable','DECLARE creates a local routine variable. An @variable belongs to one client connection.'],
['LAST_INSERT_ID()','Returns that connection generated ID; using MAX(ID) instead could select another user row.'],
['IF / EXISTS / SELECT 1','Conditional checks. EXISTS asks whether any matching row exists, without needing all its columns.'],
['TRIM / CHAR_LENGTH','Evaluate text after removing outer ordinary spaces, and count characters rather than bytes.'],
['FLOOR','Checks that attendance has no fractional part before storing an integer.'],
['JSON_ARRAY / JSON_TABLE','Send several group IDs in one input, then turn them into rows. Group links are stored relationally.'],
['JSON_ARRAYAGG / COALESCE','Collect existing group IDs for revalidation; substitute an empty array when none exist.'],
['SIGNAL SQLSTATE 45000','Raise a controlled business-rule rejection with a useful message.'],
['EXIT HANDLER / RESIGNAL','On a SQL exception, roll back this procedure transaction and propagate the original error.'],
['START TRANSACTION / COMMIT','Begin one atomic business operation and make its successful changes permanent.'],
['ROLLBACK','Undo this transaction changes after failure. It does not undo an entire earlier DDL installation.'],
['FOR UPDATE / FOR SHARE','Take row locks. Hall writes coordinate with FOR UPDATE; account checks hold a shared read lock.'],
['READ COMMITTED','Each new consistent read sees the latest committed data, including after a hall-lock wait.'],
['ON DUPLICATE KEY UPDATE','Update the existing hall/facility pair when its composite key already exists.'],
['JOIN / LEFT JOIN','Require a matching parent or retain a row when optional course/reviewer data is absent.'],
['SQL SECURITY DEFINER','Execute the stored operation using its installer privileges; the caller still needs EXECUTE permission.'],
['GRANT / MySQL role','Control database-login privileges. This is separate from USER_ACCOUNT.Role for application users.'],
['DELIMITER','A Workbench/client directive for sending a complete procedure body containing internal semicolons.']
],[169,338])
p('The controller must still authenticate a human user. SQL SECURITY DEFINER and a MySQL role do not turn a caller-supplied actor ID into proof of identity.')
p('Database CHECK expressions use SQL NULL semantics. Required columns and explicit IS NULL / IS NOT NULL review-bundle checks are necessary to express the intended rules.')
p('MySQL TIME can also represent durations beyond one day. The booking clock CHECK narrows it to the same-day interval supported by this project.')

heading('31. Technical references')
refs=[
('MySQL CREATE TABLE and CHECK constraints','https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html'),
('MySQL foreign keys','https://dev.mysql.com/doc/refman/8.0/en/create-table-foreign-keys.html'),
('MySQL locking reads','https://dev.mysql.com/doc/refman/8.0/en/innodb-locking-reads.html'),
('MySQL transaction isolation','https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html'),
('MySQL JSON_TABLE','https://dev.mysql.com/doc/refman/8.0/en/json-table-functions.html'),
('MySQL stored-object access control','https://dev.mysql.com/doc/refman/8.0/en/stored-objects-security.html'),
('OWASP Password Storage Cheat Sheet','https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html'),
('Java OOP learning material','https://dev.java/learn/language/oop/'),
('MySQL Connector/J','https://dev.mysql.com/doc/connector-j/en/connector-j-overview.html')]
for title,url in refs:sub(title);p(url)
p('Project specification: the agreed HallSync Chen-to-relational mapping, the discussed booking rules, and the SQL files in this repository. Synthetic catalogue values are explicitly labelled as demo data.')

def page(canvas,doc):
    canvas.saveState()
    w,h=A4
    canvas.setStrokeColor(BLUE);canvas.setLineWidth(1);canvas.line(44,h-38,w-44,h-38)
    canvas.setFont('Helvetica-Bold',8);canvas.setFillColor(BLUE)
    canvas.drawString(44,h-30,'HALLSYNC  /  DATABASE IMPLEMENTATION')
    canvas.setFont('Helvetica',8);canvas.setFillColor(GREY)
    canvas.drawString(44,24,'University group project | MySQL 8.0.46')
    canvas.drawRightString(w-44,24,str(doc.page))
    canvas.restoreState()
class GuideDoc(SimpleDocTemplate):
    def afterFlowable(self,flowable):
        if isinstance(flowable,Paragraph) and flowable.style.name=='HGuide':
            count=getattr(self,'bookmark_count',0)+1;self.bookmark_count=count
            key='section_'+str(count)
            self.canv.bookmarkPage(key)
            self.canv.addOutlineEntry(flowable.getPlainText(),key,0)

pdf=DOCS/'HallSync_Complete_Database_Guide.pdf'
doc=GuideDoc(str(pdf),pagesize=A4,rightMargin=44,leftMargin=44,topMargin=56,bottomMargin=42,
 title='HallSync - Complete Database Implementation Guide',author='HallSync project learning guide',pageCompression=1)
doc.build(story,onFirstPage=page,onLaterPages=page)
(DOCS/'Database_Guide.md').write_text('# HallSync - Complete Database Implementation Guide\n\n'+'\n'.join(md))
reader=PdfReader(pdf)
text='\n'.join(p.extract_text() or '' for p in reader.pages)
assert len(reader.pages)>=25 and all(t in text for t in order)
assert all(c['column'] in text for c in data['columns'])
assert 'RequesterID' in text and 'WorkingQuantity' in text and '28' in text
assert 'HallSync_Full_Setup.sql' in text
assert '/JavaScript' not in pdf.read_bytes().decode('latin1')
print('GUIDE BUILT',len(reader.pages),'pages',pdf.stat().st_size,'bytes')

# Render every page for review; selected pages are also provided at reading size.
import fitz
from PIL import Image,ImageDraw
rendered=fitz.open(pdf)
previews=DOCS/'previews';previews.mkdir(exist_ok=True)
for start in range(0,len(rendered),8):
    board=Image.new('RGB',(1000,760),'#dce5ed')
    draw=ImageDraw.Draw(board)
    for slot,index in enumerate(range(start,min(start+8,len(rendered)))):
        pix=rendered[index].get_pixmap(matrix=fitz.Matrix(.45,.45),alpha=False)
        im=Image.frombytes('RGB',(pix.width,pix.height),pix.samples)
        im.thumbnail((230,345))
        x=(slot%4)*250+10;y=(slot//4)*380+25
        board.paste(im,(x,y));draw.text((x,y-18),'Page '+str(index+1),fill='#123354')
    board.save(previews/('board_'+chr(65+start//8)+'.jpg'),quality=88)
for index in [0,1,4,8,11,15,20,25,30]:
    if index<len(rendered):
        pix=rendered[index].get_pixmap(matrix=fitz.Matrix(1.3,1.3),alpha=False)
        im=Image.frombytes('RGB',(pix.width,pix.height),pix.samples)
        im.save(previews/('page_'+chr(65+index)+'.jpg'),quality=90)
print('GUIDE_PREVIEWS',len(rendered),'pages rendered')

