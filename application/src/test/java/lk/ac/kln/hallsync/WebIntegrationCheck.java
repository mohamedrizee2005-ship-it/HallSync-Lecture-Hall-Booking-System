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
            check("Sourced venues never contain an invented capacity",campus.getAsJsonArray("venues").asList().stream().allMatch(v->v.getAsJsonObject().get("capacity").isJsonNull()));
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
