package lk.ac.kln.hallsync;

import lk.ac.kln.hallsync.web.*;
import com.google.gson.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** HTTP/security/workflow checks using a disposable real MySQL database. */
public final class WebIntegrationCheck {
    private static int passed;
    private static String applicationPassword;
    private static URI base;
    private static void check(String name,boolean ok) { if(!ok)throw new AssertionError(name);passed++;System.out.println("PASS - "+name); }
    private static final class Browser {
        final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        final HttpClient client=HttpClient.newBuilder().cookieHandler(cookies).build();
        String csrf;
        HttpResponse<String> request(String path,String method,String body,String token,String origin,String type) throws Exception {
            var b=HttpRequest.newBuilder(base.resolve(path));
            if(token!=null)b.header("X-CSRF-Token",token);
            if(origin!=null)b.header("Origin",origin);
            if(type!=null)b.header("Content-Type",type);
            b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
            var response=client.send(b.build(),HttpResponse.BodyHandlers.ofString());
            if(applicationPassword!=null&&response.body().contains(applicationPassword))throw new AssertionError("Response exposed a password");
            return response;
        }
        HttpResponse<String> raw(String path) throws Exception { return request(path,"GET",null,null,null,null); }
        JsonElement get(String path,int expected) throws Exception { return decode(raw(path),expected); }
        JsonElement post(String path,Object data,int expected) throws Exception { return decode(request(path,"POST",JsonCodec.GSON.toJson(data),csrf,base.toString(),"application/json"),expected); }
        JsonObject state() throws Exception { var d=get("/api/state",200).getAsJsonObject();csrf=d.get("csrf").getAsString();return d; }
        JsonObject login(String user,String password) throws Exception {
            var data=post("/api/auth/signin",Map.of("username",user,"password",password),200).getAsJsonObject();csrf=data.get("csrf").getAsString();return data;
        }
        String cookie() { return cookies.getCookieStore().getCookies().stream().filter(c->c.getName().equals("HALLSYNC_SESSION")).findFirst().orElseThrow().getValue(); }
    }
    private static JsonElement decode(HttpResponse<String> r,int expected) {
        if(r.statusCode()!=expected)throw new AssertionError("Expected HTTP "+expected+" got "+r.statusCode()+": "+r.body());
        JsonObject body=JsonParser.parseString(r.body()).getAsJsonObject();return body.has("data")?body.get("data"):body;
    }
    private static int id(JsonElement e) { return e.getAsJsonObject().get("id").getAsInt(); }
    private static boolean hasId(JsonElement rows,int id) { for(JsonElement r:rows.getAsJsonArray())if(r.getAsJsonObject().get("id").getAsInt()==id)return true;return false; }
    private static Map<String,Object> booking(int hall,String date,String start,String end,String type,Integer course,List<Integer> groups) {
        Map<String,Object> d=new HashMap<>();d.put("hallId",hall);d.put("date",date);d.put("start",start);d.put("end",end);d.put("type",type);d.put("courseId",course);d.put("groupIds",groups);d.put("attendance",25);d.put("purpose","Web reservation <script>alert(1)</script>");return d;
    }
    private static void publicProxyChecks(Path config) throws Exception {
        check("HTTPS public origin is normalized",PublicOrigin.validate("https://Campus.Example/").equals("https://campus.example"));
        for(String bad:List.of("http://example.com","https://example.com/path","https://name:secret@example.com","https://example.com?query=1")) {
            boolean rejected=false;try { PublicOrigin.validate(bad); }catch(IllegalArgumentException e) { rejected=true; }check("Invalid shared origin rejected: "+bad.split(":")[0],rejected);
        }
        String origin="https://campus.example";
        try(WebServer shared=new WebServer(config,0,origin)) {
            shared.start();var client=HttpClient.newHttpClient();URI url=shared.address().resolve("/api/state");
            var response=client.send(HttpRequest.newBuilder(url).GET().build(),HttpResponse.BodyHandlers.ofString());
            String cookie=response.headers().firstValue("Set-Cookie").orElseThrow();
            var state=JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("data");String token=state.get("csrf").getAsString();
            check("Shared mode uses Secure HttpOnly session cookies",cookie.contains("Secure")&&cookie.contains("HttpOnly")&&cookie.contains("SameSite=Strict"));
            check("Shared mode hides local database setup",!state.get("localSetupAllowed").getAsBoolean());
            String session=cookie.split(";",2)[0];
            var request=HttpRequest.newBuilder(shared.address().resolve("/api/setup/admin")).header("Cookie",session).header("Origin",origin).header("X-CSRF-Token",token).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}"));
            check("Shared mode rejects administrator bootstrap",client.send(request.build(),HttpResponse.BodyHandlers.ofString()).statusCode()==403);
            var login=HttpRequest.newBuilder(shared.address().resolve("/api/auth/signin")).header("Cookie",session).header("Origin",origin).header("X-CSRF-Token",token).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JsonCodec.GSON.toJson(Map.of("username","ci_admin","password",applicationPassword))));
            var signed=client.send(login.build(),HttpResponse.BodyHandlers.ofString());
            check("Configured HTTPS proxy origin supports actual sign-in",signed.statusCode()==200&&signed.body().contains("ci_admin"));
            var foreign=HttpRequest.newBuilder(shared.address().resolve("/api/auth/signin")).header("Cookie",session).header("Origin","https://other.example").header("X-CSRF-Token",token).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}"));
            check("Shared mode retains strict origin validation",client.send(foreign.build(),HttpResponse.BodyHandlers.ofString()).statusCode()==403);
        }
        Path missing=Files.createTempDirectory("hallsync-shared-missing-");boolean rejected=false;
        try { new WebServer(missing.resolve("missing.properties"),0,origin); }catch(IllegalStateException e) { rejected=true; }finally { Files.deleteIfExists(missing); }
        check("Shared access requires local database configuration first",rejected);
    }
    public static void main(String[] args) throws Exception {
        if(!"1".equals(System.getenv("HALLSYNC_RUN_INTEGRATION_TESTS")))throw new IllegalStateException("Explicit test enablement required.");
        DatabaseConfig cfg=DatabaseConfig.load(Path.of(args[0]));
        try(var c=new DatabaseConnectionFactory(cfg).open()) { if(!c.getCatalog().startsWith("hallsync_connection_"))throw new IllegalStateException("Disposable schema required."); }
        applicationPassword=System.getenv("HALLSYNC_APPLICATION_TEST_PASSWORD");
        try(WebServer server=new WebServer(Path.of(args[0]),0)) {
            server.start();base=server.address();Browser admin=new Browser();
            var html=admin.raw("/");check("Browser entry point is served",html.statusCode()==200&&html.body().contains("HallSync"));
            check("Static content has CSP and nosniff",html.headers().firstValue("Content-Security-Policy").orElse("").contains("frame-ancestors 'none'")&&html.headers().firstValue("X-Content-Type-Options").orElse("").equals("nosniff"));
            check("Frontend JavaScript is bundled locally",admin.raw("/app.js").body().contains("function renderWizard"));
            check("Styles are bundled locally",admin.raw("/app.css").statusCode()==200);
            check("Real campus photography is bundled locally",admin.raw("/assets/kelaniya-science.jpg").statusCode()==200);
            check("Early theme script is bundled locally",admin.raw("/theme.js").statusCode()==200);
            var campus=admin.get("/api/campus",200).getAsJsonObject();check("Public campus catalogue contains seven faculty references",campus.getAsJsonArray("faculties").size()==7);
            check("Published capacities carry an official university source",campus.getAsJsonArray("venues").asList().stream().allMatch(v->v.getAsJsonObject().get("capacity").isJsonNull()||v.getAsJsonObject().get("source").getAsString().equals("https://cbs.kln.ac.lk/classrooms")));
            check("Private settings cannot be downloaded",admin.raw("/db.local.properties").statusCode()==404);
            check("Traversal cannot expose local files",admin.raw("/%2e%2e/db.local.properties").statusCode()==404);
            var initial=admin.state();check("Setup detects the existing working database",initial.get("connected").getAsBoolean());
            check("Anonymous state contains no user",initial.get("user").isJsonNull());
            check("Session cookie is HttpOnly and SameSite Strict",admin.cookies.getCookieStore().getCookies().get(0).isHttpOnly());
            admin.get("/api/reference",401);check("Anonymous users cannot read private application data",true);
            admin.request("/api/auth/signin","POST","{}",null,base.toString(),"application/json");
            check("Missing CSRF is rejected",admin.request("/api/auth/signin","POST","{}",null,base.toString(),"application/json").statusCode()==403);
            check("Cross-site mutations are rejected",admin.request("/api/auth/signin","POST","{}",admin.csrf,"https://example.invalid","application/json").statusCode()==403);
            check("Form-encoded mutation is rejected",admin.request("/api/auth/signin","POST","username=ci_admin",admin.csrf,base.toString(),"application/x-www-form-urlencoded").statusCode()==415);
            check("Oversized input is rejected",admin.request("/api/auth/signin","POST","x".repeat(65537),admin.csrf,base.toString(),"application/json").statusCode()==413);
            check("Malformed JSON is rejected",admin.request("/api/auth/signin","POST","{broken",admin.csrf,base.toString(),"application/json").statusCode()==400);
            admin.post("/api/auth/signin",Map.of("username","ci_admin","password","wrong"),401);check("Wrong application password is rejected",true);
            String before=admin.cookie(),oldCsrf=admin.csrf;JsonObject signed=admin.login("ci_admin",applicationPassword);
            check("Sign-in rotates the session and CSRF token",!before.equals(admin.cookie())&&!oldCsrf.equals(admin.csrf));
            check("Admin identity comes from authentication",signed.getAsJsonObject("user").get("admin").getAsBoolean());
            var refs=admin.get("/api/reference",200).getAsJsonObject();check("Reference data includes halls and equipment",refs.has("halls")&&refs.has("equipment"));
            admin.get("/api/not-a-route",404);check("Unknown API routes return 404",true);
            Browser guest=new Browser();guest.state();
            guest.post("/api/auth/register",Map.of("username","web_short","fullName","Short","email","web_short@example.invalid","password","1234567"),400);
            check("Seven-character registration password is rejected",true);
            guest.post("/api/auth/register",Map.of("username","web_student","fullName","Web Student","email","web_student@example.invalid","password","eight123","role","AdminStaff"),200);
            guest.post("/api/auth/signin",Map.of("username","web_student","password","eight123"),401);check("Student registration waits for administrator verification",true);
            int studentApplication=0;for(var row:admin.get("/api/account-applications?status=Pending",200).getAsJsonArray())if(row.getAsJsonObject().get("username").getAsString().equals("web_student"))studentApplication=row.getAsJsonObject().get("id").getAsInt();
            admin.post("/api/account-applications/review",Map.of("id",studentApplication,"decision","Approved","identityVerified",true,"note","Confirmed university student identity"),200);
            JsonObject studentState=guest.login("web_student","eight123");
            check("Eight-character registration works and ignores role escalation",studentState.getAsJsonObject("user").get("role").getAsString().equals("Student"));
            guest.get("/api/accounts",403);guest.get("/api/bookings?scope=all",403);guest.get("/api/reports?kind=BookingDetails",403);
            check("Student cannot access accounts, all bookings or reports",true);
            guest.post("/api/catalog/departments",Map.of("name","Forbidden"),403);check("Student cannot mutate catalogues",true);
            guest.post("/api/campus/import",Map.of(),403);check("Student cannot import campus reference data",true);
            int beforeHalls=admin.get("/api/reference",200).getAsJsonObject().getAsJsonArray("halls").size();
            var imported=admin.post("/api/campus/import",Map.of(),200).getAsJsonObject();
            check("Existing installation gains only missing reference names",imported.get("departmentsAdded").getAsInt()==1&&imported.get("buildingsAdded").getAsInt()==1);
            var repeated=admin.post("/api/campus/import",Map.of(),200).getAsJsonObject();
            check("Campus import preserves existing names on repeated runs",repeated.get("departmentsAdded").getAsInt()==0&&repeated.get("buildingsAdded").getAsInt()==0);
            check("Campus import does not activate unconfirmed venues",imported.get("hallsAdded").getAsInt()==0&&admin.get("/api/reference",200).getAsJsonObject().getAsJsonArray("halls").size()==beforeHalls);
            check("Science departments are available after import",admin.get("/api/public/departments",200).toString().contains("Plant and Molecular Biology"));
            int department=id(admin.post("/api/catalog/departments",Map.of("name","Web Department"),200));
            int building=id(admin.post("/api/catalog/buildings",Map.of("code","WEB","name","Web Campus","location","Test location"),200));
            int facility=id(admin.post("/api/catalog/facilities",Map.of("name","Web Display","description","Test display"),200));
            int course=id(admin.post("/api/catalog/courses",Map.of("departmentId",department,"code","WEB101","title","Web course"),200));
            int group=id(admin.post("/api/catalog/groups",Map.of("departmentId",department,"name","Web Group","intakeYear",2026,"studyYear",1),200));
            int hall=id(admin.post("/api/catalog/halls",Map.of("buildingId",building,"number","W1","floor","1","capacity",60),200));
            check("Administrator can create all catalogue types over HTTP",department>0&&building>0&&facility>0&&course>0&&group>0&&hall>0);
            admin.post("/api/catalog/equipment",Map.of("hallId",hall,"facilityId",facility,"quantity",2,"workingQuantity",1),200);
            admin.post("/api/catalog/equipment",Map.of("hallId",hall,"facilityId",facility,"quantity",2,"workingQuantity",3),400);check("Equipment quantity validation is enforced over HTTP",true);
            admin.post("/api/accounts",Map.of("departmentId",department,"fullName","Web Lecturer","email","web_lecturer@example.invalid","username","web_lecturer","password","eight123","role","Lecturer"),200);
            var accounts=admin.get("/api/accounts?department="+department,200);check("Department-filtered account list works without password hashes",accounts.getAsJsonArray().size()==1&&!accounts.toString().contains("PasswordHash")&&!accounts.toString().contains("passwordHash"));
            Browser lecturer=new Browser();lecturer.state();var identity=lecturer.login("web_lecturer","eight123");int lecturerId=identity.getAsJsonObject("user").get("id").getAsInt();
            String date=LocalDate.now(ZoneId.of("Asia/Colombo")).plusDays(40).toString();
            var first=booking(hall,date,"09:00","10:00","Lecture",course,List.of(group));first.put("actor",signed.getAsJsonObject("user").get("id").getAsInt());
            guest.post("/api/bookings",first,403);check("Student cannot request a booking over HTTP",true);
            lecturer.post("/api/bookings",booking(hall,date,"09:00","10:00","Lecture",null,List.of()),400);check("Teaching requests require course and group",true);
            var fractional=new HashMap<>(first);fractional.put("attendance",25.5);lecturer.post("/api/bookings",fractional,400);check("Fractional attendance is rejected",true);
            int booking=id(lecturer.post("/api/bookings",first,200));
            var own=lecturer.get("/api/bookings?scope=mine",200);boolean owner=false;for(var row:own.getAsJsonArray())if(row.getAsJsonObject().get("id").getAsInt()==booking)owner=row.getAsJsonObject().get("requesterId").getAsInt()==lecturerId;
            check("Requester is derived from session, not supplied actor",owner);
            check("Booking group membership is available to its requester",lecturer.get("/api/bookings/groups?id="+booking,200).getAsJsonArray().get(0).getAsInt()==group);
            var edited=new HashMap<>(first);edited.put("id",booking);edited.put("purpose","Updated web lecture");lecturer.post("/api/bookings",edited,200);check("Requester can edit Pending request over HTTP",true);
            int conflicting=id(lecturer.post("/api/bookings",first,200));
            lecturer.post("/api/bookings/action",Map.of("id",booking,"action","Approved"),403);check("Lecturer cannot approve a request",true);
            int self=id(admin.post("/api/bookings",booking(hall,date,"11:00","12:00","Event",null,List.of()),200));
            admin.post("/api/bookings/action",Map.of("id",self,"action","Approved"),403);check("Admin cannot approve own request over HTTP",true);
            admin.post("/api/bookings/action",Map.of("id",booking,"action","Approved","note","Approved via HTTP"),200);
            check("Independent approval reserves a hall",true);
            admin.post("/api/bookings/action",Map.of("id",conflicting,"action","Approved"),409);check("Overlapping approval is rejected over HTTP",true);
            lecturer.post("/api/bookings",edited,400);check("Approved request cannot be edited over HTTP",true);
            var available=guest.get("/api/availability?date="+date+"&start=09:00&end=10:00&attendance=25",200);check("Availability excludes approved interval",!hasId(available,hall));
            check("Adjacent interval remains available",hasId(guest.get("/api/availability?date="+date+"&start=10:00&end=11:00&attendance=25",200),hall));
            var schedule=guest.get("/api/schedule?from="+date+"&to="+date,200);check("Student timetable excludes private reviewer/requester details",schedule.toString().contains("WEB101")&&!schedule.toString().contains("requesterName")&&!schedule.toString().contains("reviewNote"));
            admin.post("/api/blocks",Map.of("hallId",hall,"start",date+"T09:30","end",date+"T10:30","reason","Conflict"),409);check("Maintenance cannot displace approved session",true);
            lecturer.post("/api/bookings/action",Map.of("id",booking,"action","Cancelled"),200);check("Requester cancellation works over HTTP",true);
            int block=id(admin.post("/api/blocks",Map.of("hallId",hall,"start",date+"T09:00","end",date+"T10:00","reason","Web maintenance"),200));
            check("Admin block history is readable over HTTP",hasId(admin.get("/api/blocks",200),block));
            admin.post("/api/blocks/cancel",Map.of("id",block),200);check("Block cancellation works over HTTP",true);
            for(String kind:List.of("BookingDetails","StatusTotals","HallFacilities","HallUsage"))check("HTTP report loads "+kind,admin.get("/api/reports?kind="+kind,200).getAsJsonObject().has("columns"));
            var csv=admin.raw("/api/reports/download?kind=BookingDetails");check("CSV export is downloadable and UTF-8",csv.statusCode()==200&&csv.headers().firstValue("Content-Disposition").orElse("").contains("attachment")&&csv.body().contains("BookingID"));
            admin.post("/api/setup/connection",Map.of("url",cfg.getUrl(),"username",cfg.getUsername(),"password",cfg.getPassword()),409);check("Connected setup cannot overwrite saved settings",true);
            admin.post("/api/setup/admin",Map.of("databaseUser",cfg.getUsername(),"databasePassword",cfg.getPassword(),"fullName","Forbidden admin","email","forbidden_admin@example.invalid","username","forbidden_admin","password","eight123"),400);
            check("Restricted database login cannot bootstrap an administrator",true);
            admin.post("/api/setup/admin",Map.of("databaseUser",System.getenv("HALLSYNC_BOOTSTRAP_TEST_USER"),"databasePassword",System.getenv("HALLSYNC_BOOTSTRAP_TEST_PASSWORD"),"departmentId",department,"fullName","Web Administrator","email","web_admin@example.invalid","username","web_admin","password","eight123"),200);
            Browser bootstrapped=new Browser();bootstrapped.state();check("Local privileged setup creates a usable administrator without manual hashes",bootstrapped.login("web_admin","eight123").getAsJsonObject("user").get("admin").getAsBoolean());
            check("Database upgrade enables lecturer registration",initial.get("lecturerRegistrationEnabled").getAsBoolean());
            Browser applicant=new Browser();applicant.state();
            Map<String,Object> registration=new HashMap<>();registration.put("departmentId",department);registration.put("fullName","HTTP Lecturer Applicant");registration.put("email","http_applicant@kln.ac.lk");registration.put("username","http_applicant");registration.put("staffId","HTTP-STAFF-01");registration.put("password","eight123");registration.put("role","AdminStaff");registration.put("isActive",true);
            var badRegistration=new HashMap<>(registration);badRegistration.put("staffId","   ");applicant.post("/api/auth/register-lecturer",badRegistration,400);check("Lecturer registration requires a staff ID",true);
            badRegistration=new HashMap<>(registration);badRegistration.remove("departmentId");applicant.post("/api/auth/register-lecturer",badRegistration,400);check("Lecturer registration requires a department",true);
            badRegistration=new HashMap<>(registration);badRegistration.put("password","1234567");applicant.post("/api/auth/register-lecturer",badRegistration,400);check("Lecturer registration enforces the eight-character minimum",true);
            badRegistration=new HashMap<>(registration);badRegistration.put("email","invalid");applicant.post("/api/auth/register-lecturer",badRegistration,400);check("Lecturer registration validates email format",true);
            int application=id(applicant.post("/api/auth/register-lecturer",registration,200));
            applicant.post("/api/auth/signin",Map.of("username","http_applicant","password","eight123"),401);
            check("Pending lecturer cannot sign in even with the correct password",applicant.state().get("user").isJsonNull());
            applicant.post("/api/bookings",booking(hall,date,"15:00","16:00","Event",null,List.of()),401);check("Pending lecturer cannot request halls",true);
            applicant.get("/api/lecturer-applications",401);guest.get("/api/lecturer-applications",403);lecturer.get("/api/lecturer-applications",403);check("Lecturer applications are private to administrators",true);
            Map<String,Object> decision=new HashMap<>();decision.put("id",application);decision.put("decision","Approved");decision.put("identityVerified",true);decision.put("note","Confirmed independently with faculty staff.");decision.put("actor",lecturerId);
            applicant.post("/api/lecturer-applications/review",decision,401);guest.post("/api/lecturer-applications/review",decision,403);lecturer.post("/api/lecturer-applications/review",decision,403);check("Public, Student and Lecturer sessions cannot review applications",true);
            var unverified=new HashMap<>(decision);unverified.put("identityVerified",false);admin.post("/api/lecturer-applications/review",unverified,400);
            unverified.put("identityVerified","true");admin.post("/api/lecturer-applications/review",unverified,400);check("Identity verification requires a real confirmation boolean",true);
            var queue=admin.get("/api/lecturer-applications?status=Pending",200);check("Administrator sees safe pending application metadata",queue.toString().contains("HTTP-STAFF-01")&&!queue.toString().contains("PasswordHash")&&!queue.toString().contains("$argon2id$")&&!queue.toString().contains("eight123"));
            admin.post("/api/lecturer-applications/review",decision,200);
            var approved=applicant.login("http_applicant","eight123");
            check("Approval activates lecturer access without granting AdminStaff",approved.getAsJsonObject("user").get("role").getAsString().equals("Lecturer")&&!approved.getAsJsonObject("user").get("admin").getAsBoolean());
            check("Reviewer identity is taken from the admin session",admin.get("/api/lecturer-applications?status=Approved",200).toString().contains("ci_admin"));
            var draft=booking(hall,LocalDate.parse(date).plusDays(21).toString(),"15:00","16:00","Event",null,List.of());int afterApproval=id(applicant.post("/api/bookings",draft,200));
            boolean remainsPending=false;for(var row:applicant.get("/api/bookings?scope=mine",200).getAsJsonArray())if(row.getAsJsonObject().get("id").getAsInt()==afterApproval)remainsPending=row.getAsJsonObject().get("status").getAsString().equals("Pending");
            check("Approving a lecturer does not automatically approve bookings",remainsPending);
            admin.post("/api/lecturer-applications/review",decision,409);check("Application decisions cannot be applied twice",true);
            var duplicate=new HashMap<>(registration);duplicate.put("email","another_applicant@kln.ac.lk");applicant.post("/api/auth/register-lecturer",duplicate,409);check("Usernames remain unique across public registration paths",true);
            Browser rejected=new Browser();rejected.state();var rejectionRegistration=new HashMap<>(registration);rejectionRegistration.put("username","http_rejected");rejectionRegistration.put("email","http_rejected@kln.ac.lk");rejectionRegistration.put("staffId","HTTP-STAFF-02");int rejectedId=id(rejected.post("/api/auth/register-lecturer",rejectionRegistration,200));
            admin.post("/api/lecturer-applications/review",Map.of("id",rejectedId,"decision","Rejected","note"," "),400);check("Rejection requires a nonblank reason",true);
            admin.post("/api/lecturer-applications/review",Map.of("id",rejectedId,"decision","Rejected","note","Staff identity could not be confirmed."),200);
            rejected.post("/api/auth/signin",Map.of("username","http_rejected","password","eight123"),401);check("Rejected lecturer remains unable to sign in",true);
            check("Rejected applications retain the reviewer reason",admin.get("/api/lecturer-applications?status=Rejected",200).toString().contains("Staff identity could not be confirmed."));
            admin.post("/api/lecturer-applications/review",Map.of("id",rejectedId,"decision","Approved","identityVerified",true),409);check("Rejected application cannot be activated by replaying approval",true);
            // University-wide release: actual HTTP checks with three independent role sessions.
            int studentIssue=id(guest.post("/api/room-issues",Map.of("hallId",hall,"category","Capacity","details","Please verify the posted seating capacity."),200));
            int lecturerIssue=id(lecturer.post("/api/room-issues",Map.of("hallId",hall,"category","Equipment","details","Projector image is not visible."),200));
            check("Student sees own reports without other reporters' private details",hasId(guest.get("/api/room-issues",200),studentIssue)&&!hasId(guest.get("/api/room-issues",200),lecturerIssue));
            check("Lecturer sees own room reports",hasId(lecturer.get("/api/room-issues",200),lecturerIssue)&&!hasId(lecturer.get("/api/room-issues",200),studentIssue));
            check("Administrator sees the room-report queue",hasId(admin.get("/api/room-issues",200),studentIssue)&&hasId(admin.get("/api/room-issues",200),lecturerIssue));
            guest.post("/api/room-issues/resolve",Map.of("id",studentIssue,"note","Forbidden"),403);lecturer.post("/api/room-issues/resolve",Map.of("id",lecturerIssue,"note","Forbidden"),403);check("Only administrators can resolve room reports",true);
            admin.post("/api/room-issues/resolve",Map.of("id",lecturerIssue,"note"," "),400);
            admin.post("/api/room-issues/resolve",Map.of("id",lecturerIssue,"note","Connection checked and restored."),200);admin.post("/api/room-issues/resolve",Map.of("id",lecturerIssue,"note","Replay"),409);check("Resolution records a mandatory note and cannot replay",true);
            guest.post("/api/room-issues",Map.of("hallId",hall,"category","Equipment","details"," "),400);check("Blank discrepancy reports are rejected",true);
            String calendarDate=LocalDate.parse(date).plusDays(10).toString();int calendarBooking=id(lecturer.post("/api/bookings",booking(hall,calendarDate,"09:00","10:00","Event",null,List.of()),200));admin.post("/api/bookings/action",Map.of("id",calendarBooking,"action","Approved"),200);
            var calendar=lecturer.raw("/api/calendar?from="+calendarDate+"&to="+calendarDate+"&scope=mine");
            check("Personal calendar exports approved sessions with Colombo-to-UTC conversion",calendar.statusCode()==200&&calendar.body().contains("BEGIN:VCALENDAR")&&calendar.body().contains("T033000Z")&&calendar.body().contains("UID:booking-"+calendarBooking+"@")&&calendar.headers().firstValue("Content-Type").orElse("").contains("text/calendar"));
            check("Calendar excludes Pending reservations and private review data",!calendar.body().contains("booking-"+conflicting+"@")&&!calendar.body().contains("Reviewer")&&!calendar.body().contains("Password"));
            check("Students can export the public campus schedule",guest.raw("/api/calendar?from="+calendarDate+"&to="+calendarDate).statusCode()==200);
            guest.get("/api/calendar?from="+calendarDate+"&to="+calendarDate+"&scope=mine",403);check("Student cannot access requester-only calendar scope",true);
            lecturer.get("/api/calendar?from="+calendarDate+"&to=2020-01-01",400);check("Invalid calendar date ranges are rejected",true);
            var dated=admin.get("/api/reports?kind=BookingDetails&from="+calendarDate+"&to="+calendarDate,200).getAsJsonObject();int dateColumn=dated.getAsJsonArray("columns").asList().indexOf(new JsonPrimitive("BookingDate"));
            check("Administrator date-filtered report contains only selected dates",dated.getAsJsonArray("rows").size()>0&&dated.getAsJsonArray("rows").asList().stream().allMatch(row->row.getAsJsonArray().get(dateColumn).getAsString().equals(calendarDate)));
            check("CSV export obeys the report date range",admin.raw("/api/reports/download?kind=BookingDetails&from="+calendarDate+"&to="+calendarDate).body().contains(calendarDate));
            admin.get("/api/reports?kind=StatusTotals&from="+calendarDate+"&to=2020-01-01",400);guest.get("/api/reports?kind=StatusTotals&from="+calendarDate+"&to="+calendarDate,403);check("Report date validation and administrator guard are enforced",true);
            int removable=id(admin.post("/api/catalog/halls",Map.of("buildingId",building,"number","REMOVE","capacity",30),200));
            guest.post("/api/catalog/hall-edit",Map.of("hallId",removable,"buildingId",building,"number","RENAMED","floor","G","capacity",35,"status","Active"),403);
            admin.post("/api/catalog/hall-edit",Map.of("hallId",removable,"buildingId",building,"number","RENAMED","floor","G","capacity",35,"status","Active"),200);
            check("Hall management edits code, floor, capacity and state",admin.get("/api/reference",200).toString().contains("RENAMED"));
            lecturer.post("/api/catalog/hall-remove",Map.of("hallId",removable),403);admin.post("/api/catalog/hall-remove",Map.of("hallId",removable),200);check("Unused hall can be removed only by an administrator",!hasId(admin.get("/api/reference",200).getAsJsonObject().get("halls"),removable));
            admin.post("/api/catalog/hall-remove",Map.of("hallId",hall),409);check("Retirement cannot displace a future approved booking",true);
            int historical=id(admin.post("/api/catalog/halls",Map.of("buildingId",building,"number","HISTORY","capacity",30),200));lecturer.post("/api/bookings",booking(historical,calendarDate,"12:00","13:00","Event",null,List.of()),200);admin.post("/api/catalog/hall-remove",Map.of("hallId",historical),200);check("Retiring a used hall preserves its record",hasId(admin.get("/api/reference",200).getAsJsonObject().get("halls"),historical));
            guest.post("/api/accounts/manage",Map.of("id",lecturerId,"role","AdminStaff","active",true),403);lecturer.post("/api/accounts/manage",Map.of("id",lecturerId,"role","AdminStaff","active",true),403);check("Students and lecturers cannot change account access",true);
            int adminId=signed.getAsJsonObject("user").get("id").getAsInt();admin.post("/api/accounts/manage",Map.of("id",adminId,"role","Student","active",false),400);check("Administrator cannot alter their own access",true);
            admin.post("/api/accounts/manage",Map.of("id",lecturerId,"role","Lecturer","active",false),200);lecturer.get("/api/bookings?scope=mine",401);check("Disabled account loses access on its next request",true);
            admin.post("/api/accounts/manage",Map.of("id",lecturerId,"role","Student","active",true),200);lecturer.get("/api/accounts",401);check("Role changes invalidate the previous session role",true);
            String folded=lk.ac.kln.hallsync.service.CalendarExporter.export(List.of(new lk.ac.kln.hallsync.model.ScheduleEntry(999,"தமிழ்".repeat(60),"One\nInjected:line",LocalDate.parse(calendarDate),LocalTime.of(9,0),LocalTime.of(10,0),"Lecture","Approved")));
            check("Calendar escapes newlines and folds long Unicode lines",folded.contains("SUMMARY:One\\nInjected:line")&&Arrays.stream(folded.split("\r\n")).allMatch(line->line.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=75));
            var out=lecturer.post("/api/auth/logout",Map.of(),200).getAsJsonObject();lecturer.csrf=out.get("csrf").getAsString();
            lecturer.get("/api/bookings?scope=mine",401);check("Sign-out removes access to personal reservations",true);
            Path folder=Files.createTempDirectory("hallsync-web-setup-");Path file=folder.resolve("db.local.properties");
            try(WebServer fresh=new WebServer(file,0)) {
                fresh.start();URI previous=base;base=fresh.address();Browser setup=new Browser();setup.state();
                setup.post("/api/setup/connection",Map.of("url",cfg.getUrl(),"username",cfg.getUsername(),"password",cfg.getPassword()),200);
                check("Browser first-connection setup tests and saves the existing database login",Files.exists(file)&&setup.state().get("connected").getAsBoolean());base=previous;
            } finally { Files.deleteIfExists(file);Files.deleteIfExists(folder); }
            publicProxyChecks(Path.of(args[0]));
            System.out.println("WEB_CHECKS_PASSED="+passed);
        }
    }
}
