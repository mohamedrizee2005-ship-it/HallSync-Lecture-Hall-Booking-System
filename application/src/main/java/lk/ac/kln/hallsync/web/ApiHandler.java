package lk.ac.kln.hallsync.web;

import com.google.gson.*;
import com.sun.net.httpserver.*;
import lk.ac.kln.hallsync.auth.*;
import lk.ac.kln.hallsync.model.*;
import lk.ac.kln.hallsync.service.*;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

/** Same-origin HTTP adapter. All actors come from server-side verified sessions. */
public final class ApiHandler implements HttpHandler {
    private final ServerState state;
    private final BrowserSessions sessions;
    private final String origin;
    private final String publicOrigin;
    private final Set<String> allowedHosts;
    public ApiHandler(ServerState state,BrowserSessions sessions,String origin) { this(state,sessions,origin,null); }
    public ApiHandler(ServerState state,BrowserSessions sessions,String localOrigin,String publicOrigin) {
        this.state=state;this.sessions=sessions;this.publicOrigin=PublicOrigin.validate(publicOrigin);this.origin=this.publicOrigin==null?localOrigin:this.publicOrigin;
        this.allowedHosts=new HashSet<>();allowedHosts.add(java.net.URI.create(localOrigin).getRawAuthority());if(this.publicOrigin!=null)allowedHosts.add(java.net.URI.create(this.publicOrigin).getRawAuthority());
    }
    @Override public void handle(HttpExchange x) throws IOException {
        try {
            if(!x.getRemoteAddress().getAddress().isLoopbackAddress())throw new HttpError(403,"Local access only.");
            if(!allowedHosts.contains(x.getRequestHeaders().getFirst("Host")))throw new HttpError(403,"Open HallSync using its local launch address.");
            BrowserSessions.Session session=sessions.get(x);
            if(x.getRequestMethod().equals("POST")) {
                if(!origin.equals(x.getRequestHeaders().getFirst("Origin")) || !session.csrf().equals(x.getRequestHeaders().getFirst("X-CSRF-Token"))) {
                    throw new HttpError(403,"Your session could not be verified. Refresh HallSync and try again.");
                }
                String type=x.getRequestHeaders().getFirst("Content-Type");
                if(type==null||!type.toLowerCase(Locale.ROOT).startsWith("application/json"))throw new HttpError(415,"Send JSON data.");
                byte[] bytes=x.getRequestBody().readNBytes(65537);
                if(bytes.length>65536)throw new HttpError(413,"This request is too large.");
                JsonElement parsed=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8));
                if(!parsed.isJsonObject())throw new IllegalArgumentException("Enter valid form details.");
                send(x,200,post(x,session,parsed.getAsJsonObject()));
            } else if(x.getRequestMethod().equals("GET")) {
                if(x.getRequestURI().getPath().equals("/api/reports/download")) { download(x,session);return; }
                send(x,200,get(x,session));
            } else { x.getResponseHeaders().set("Allow","GET, POST");throw new HttpError(405,"This action is not supported."); }
        } catch(HttpError e) { sendError(x,e.status(),e.getMessage()); }
        catch(SecurityException e) { sendError(x,403,e.getMessage()); }
        catch(SQLException e) {
            String message=switch(e.getErrorCode()) {
                case 1045 -> "The database username or password was not accepted.";
                case 1062 -> "That username, email, code or group already exists.";
                case 1142,1370 -> "This database login does not have the required permission.";
                case 1305 -> "A required database update is missing. Ask the administrator to apply the supplied upgrade scripts.";
                case 1452 -> "A selected record is no longer available. Refresh and try again.";
                case 1205,1213 -> "Another request changed these records. Refresh and try again.";
                default -> "45000".equals(e.getSQLState()) ? e.getMessage() : "We could not reach the database. Check MySQL is running.";
            };
            sendError(x,"45000".equals(e.getSQLState())||e.getErrorCode()==1062?409:400,message);
        } catch(IllegalArgumentException|DateTimeException|JsonParseException e) {
            sendError(x,400,e instanceof IllegalArgumentException && !(e instanceof NumberFormatException) ? e.getMessage() : "Check the dates, times and numbers in this form.");
        } catch(IllegalStateException e) { sendError(x,409,e.getMessage()); }
        catch(Exception e) { sendError(x,500,"This action could not finish. Check your local connection and try again."); }
        finally { x.close(); }
    }
    private Object get(HttpExchange x,BrowserSessions.Session s) throws Exception {
        String path=x.getRequestURI().getPath();Map<String,String> q=query(x);
        if(path.equals("/api/state"))return snapshot(s);
        if(path.equals("/api/campus"))return CampusDirectory.data();
        if(path.equals("/api/public/departments"))return state.app().departments.list();
        AuthenticatedUser user=require(s);var app=state.app();
        return switch(path) {
            case "/api/reference" -> Map.of("departments",app.departments.list(),"buildings",app.catalog.buildings(),"facilities",app.catalog.facilities(),"courses",app.catalog.courses(),"groups",app.catalog.groups(),"halls",app.catalog.halls(),"equipment",app.catalog.equipment());
            case "/api/bookings" -> "all".equals(q.get("scope"))?app.bookings.all(user):app.bookings.mine(user);
            case "/api/bookings/groups" -> app.bookings.groupsForEdit(user,Integer.parseInt(q.get("id")));
            case "/api/schedule" -> app.bookings.schedule(user,LocalDate.parse(q.get("from")),LocalDate.parse(q.get("to")));
            case "/api/availability" -> app.catalog.available(user,LocalDate.parse(q.get("date")),LocalTime.parse(q.get("start")),LocalTime.parse(q.get("end")),Integer.parseInt(q.get("attendance")));
            case "/api/lecturer-applications" -> state.app().lecturerApplications.list(user,optionalQuery(q,"status"));
            case "/api/accounts" -> app.accounts.list(user,nullableInteger(q.get("department")));
            case "/api/blocks" -> app.blocks.list(user);
            case "/api/reports" -> app.reports.load(user,ReportKind.valueOf(q.get("kind")));
            default -> throw new HttpError(404,"This page was not found.");
        };
    }
    private Object post(HttpExchange x,BrowserSessions.Session s,JsonObject b) throws Exception {
        String path=x.getRequestURI().getPath();
        if(publicOrigin!=null&&path.startsWith("/api/setup/"))throw new HttpError(403,"Database and administrator initialization is available only in local mode.");
        if(path.equals("/api/setup/connection")) {
            s.mayAuthenticate();char[] password=text(b,"password").toCharArray();
            try { state.connect(text(b,"url"),text(b,"username"),password); }
            catch(Exception e) { s.failedAuthentication();throw e; }
            finally { Arrays.fill(password,'\0'); }
            return snapshot(s);
        }
        if(path.equals("/api/setup/admin")) {
            s.mayAuthenticate();char[] dbPassword=text(b,"databasePassword").toCharArray(),password=text(b,"password").toCharArray();
            try { return Map.of("id",new AdministratorSetupService().create(state.configuration(),text(b,"databaseUser"),dbPassword,account(b),password)); }
            catch(Exception e) { s.failedAuthentication();throw e; }
            finally { Arrays.fill(dbPassword,'\0');Arrays.fill(password,'\0'); }
        }
        if(path.equals("/api/auth/signin")) {
            s.mayAuthenticate();char[] password=text(b,"password").toCharArray();
            try {
                var found=state.app().authentication.signIn(text(b,"username"),password);
                if(found.isEmpty()) { s.failedAuthentication();throw new HttpError(401,"The username or password is incorrect, or this account is inactive."); }
                return snapshot(sessions.rotate(x,s,found.get()));
            } finally { Arrays.fill(password,'\0'); }
        }
        if(path.equals("/api/auth/register-lecturer")) {
            if(!state.lecturerRegistrationEnabled())throw new HttpError(503,"Lecturer registration is not enabled yet. Ask your administrator to install the lecturer approval update.");
            char[] password=text(b,"password").toCharArray();
            try { return Map.of("id",state.app().lecturerApplications.register(account(b),text(b,"staffId"),password),"status","Pending"); }
            finally { Arrays.fill(password,'\0'); }
        }
        if(path.equals("/api/auth/register")) {
            char[] password=text(b,"password").toCharArray();
            try { return Map.of("id",state.app().accounts.registerStudent(account(b),password)); }
            finally { Arrays.fill(password,'\0'); }
        }
        if(path.equals("/api/auth/logout"))return snapshot(sessions.rotate(x,s,null));
        AuthenticatedUser user=require(s);var app=state.app();
        if(path.equals("/api/lecturer-applications/review")) {
            JsonElement verified=b.get("identityVerified");boolean confirmed=verified!=null&&verified.isJsonPrimitive()&&verified.getAsJsonPrimitive().isBoolean()&&verified.getAsBoolean();
            app.lecturerApplications.review(user,integer(b,"id"),text(b,"decision"),optionalText(b,"note"),confirmed);
            return Map.of("saved",true);
        }
        if(path.equals("/api/campus/import"))return CampusDirectory.install(app,user);
        if(path.equals("/api/bookings")) {
            BookingDraft draft=draft(b);Integer id=optionalInt(b,"id");
            if(id==null)return Map.of("id",app.bookings.create(user,draft));
            app.bookings.update(user,id,draft);return Map.of("id",id);
        }
        if(path.equals("/api/bookings/action")) {
            int id=integer(b,"id");
            switch(text(b,"action")) {
                case "Approved","Rejected" -> app.bookings.review(user,id,text(b,"action"),optionalText(b,"note"));
                case "Cancelled" -> app.bookings.cancel(user,id);
                case "Completed" -> app.bookings.complete(user,id);
                default -> throw new IllegalArgumentException("Choose a valid booking action.");
            }
            return Map.of("id",id);
        }
        if(path.equals("/api/accounts")) {
            char[] password=text(b,"password").toCharArray();
            try { return Map.of("id",app.accounts.provision(user,account(b),password,text(b,"role"))); }
            finally { Arrays.fill(password,'\0'); }
        }
        if(path.equals("/api/blocks"))return Map.of("id",app.blocks.create(user,integer(b,"hallId"),LocalDateTime.parse(text(b,"start")),LocalDateTime.parse(text(b,"end")),text(b,"reason")));
        if(path.equals("/api/blocks/cancel")) { app.blocks.cancel(user,integer(b,"id"));return Map.of("saved",true); }
        AccessRules.administrator(user);
        return switch(path) {
            case "/api/catalog/departments" -> Map.of("id",app.departments.add(user,text(b,"name")).getDepartmentId());
            case "/api/catalog/buildings" -> Map.of("id",app.catalog.addBuilding(user,text(b,"code"),text(b,"name"),optionalText(b,"location")));
            case "/api/catalog/facilities" -> Map.of("id",app.catalog.addFacility(user,text(b,"name"),optionalText(b,"description")));
            case "/api/catalog/courses" -> Map.of("id",app.catalog.addCourse(user,integer(b,"departmentId"),text(b,"code"),text(b,"title")));
            case "/api/catalog/groups" -> Map.of("id",app.catalog.addGroup(user,integer(b,"departmentId"),text(b,"name"),integer(b,"intakeYear"),integer(b,"studyYear")));
            case "/api/catalog/halls" -> Map.of("id",app.catalog.addHall(user,integer(b,"buildingId"),text(b,"number"),optionalText(b,"floor"),integer(b,"capacity")));
            case "/api/catalog/hall-status" -> { app.catalog.updateHall(user,integer(b,"hallId"),integer(b,"capacity"),text(b,"status"));yield Map.of("saved",true); }
            case "/api/catalog/equipment" -> { app.catalog.setEquipment(user,integer(b,"hallId"),integer(b,"facilityId"),integer(b,"quantity"),integer(b,"workingQuantity"));yield Map.of("saved",true); }
            default -> throw new HttpError(404,"This action was not found.");
        };
    }
    private Map<String,Object> snapshot(BrowserSessions.Session s) {
        Map<String,Object> data=new LinkedHashMap<>();data.put("connected",state.ready());data.put("lecturerRegistrationEnabled",state.lecturerRegistrationEnabled());data.put("localSetupAllowed",publicOrigin==null);data.put("hasConfig",state.hasFile());data.put("problem",state.problem());data.put("csrf",s.csrf());
        var user=s.user();data.put("user",user==null?null:Map.of("id",user.getAccountId(),"username",user.getUsername(),"role",user.getRole(),"admin",user.isAdministrator(),"canRequest",user.isAdministrator()||"Lecturer".equals(user.getRole())));
        return data;
    }
    private static AuthenticatedUser require(BrowserSessions.Session s) { if(s.user()==null)throw new HttpError(401,"Sign in to continue.");return s.user(); }
    private static AccountDraft account(JsonObject b) { return new AccountDraft(optionalInt(b,"departmentId"),text(b,"fullName"),text(b,"email"),text(b,"username")); }
    private static BookingDraft draft(JsonObject b) {
        List<Integer> groups=new ArrayList<>();JsonElement g=b.get("groupIds");
        if(g!=null&&!g.isJsonNull()) { if(!g.isJsonArray())throw new IllegalArgumentException("Choose valid student groups.");for(JsonElement id:g.getAsJsonArray())groups.add(strictInteger(id)); }
        return new BookingDraft(integer(b,"hallId"),optionalInt(b,"courseId"),LocalDate.parse(text(b,"date")),LocalTime.parse(text(b,"start")),LocalTime.parse(text(b,"end")),text(b,"purpose"),BookingType.valueOf(text(b,"type")),integer(b,"attendance"),groups);
    }
    private static String text(JsonObject b,String key) {
        JsonElement v=b.get(key);if(v==null||v.isJsonNull()||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Complete the "+key+" field.");return v.getAsString();
    }
    private static String optionalText(JsonObject b,String key) { return !b.has(key)||b.get(key).isJsonNull()?null:text(b,key); }
    private static int strictInteger(JsonElement v) {
        if(!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Use a whole number.");
        try { return v.getAsBigDecimal().intValueExact(); }catch(ArithmeticException e) { throw new IllegalArgumentException("Use a whole number within the supported range."); }
    }
    private static int integer(JsonObject b,String key) { if(!b.has(key)||b.get(key).isJsonNull())throw new IllegalArgumentException("Complete the "+key+" field.");return strictInteger(b.get(key)); }
    private static Integer optionalInt(JsonObject b,String key) { return !b.has(key)||b.get(key).isJsonNull()?null:integer(b,key); }
    private static String optionalQuery(Map<String,String> query,String key) { String value=query.get(key);return value==null||value.isBlank()?null:value; }
    private static Integer nullableInteger(String v) { return v==null||v.isBlank()?null:Integer.valueOf(v); }
    private static Map<String,String> query(HttpExchange x) {
        Map<String,String> result=new HashMap<>();String raw=x.getRequestURI().getRawQuery();
        if(raw!=null)for(String part:raw.split("&")) { String[] pair=part.split("=",2);result.put(URLDecoder.decode(pair[0],StandardCharsets.UTF_8),pair.length==2?URLDecoder.decode(pair[1],StandardCharsets.UTF_8):""); }return result;
    }
    private void download(HttpExchange x,BrowserSessions.Session s) throws Exception {
        ReportKind kind=ReportKind.valueOf(query(x).get("kind"));ReportTable report=state.app().reports.load(require(s),kind);
        var file=Files.createTempFile("hallsync-report-",".csv");
        try { CsvExporter.write(file,report);byte[] bytes=Files.readAllBytes(file);x.getResponseHeaders().set("Content-Type","text/csv; charset=utf-8");x.getResponseHeaders().set("Content-Disposition","attachment; filename=HallSync_"+kind.name()+".csv");securityHeaders(x);x.sendResponseHeaders(200,bytes.length);x.getResponseBody().write(bytes); }
        finally { Files.deleteIfExists(file); }
    }
    private static void sendError(HttpExchange x,int status,String message) throws IOException { sendPayload(x,status,Map.of("ok",false,"message",message==null?"Check the form and try again.":message)); }
    private static void send(HttpExchange x,int status,Object data) throws IOException { sendPayload(x,status,Map.of("ok",true,"data",data)); }
    private static void sendPayload(HttpExchange x,int status,Object value) throws IOException {
        byte[] bytes=JsonCodec.GSON.toJson(value).getBytes(StandardCharsets.UTF_8);securityHeaders(x);x.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");x.sendResponseHeaders(status,bytes.length);x.getResponseBody().write(bytes);
    }
    public static void securityHeaders(HttpExchange x) {
        x.getResponseHeaders().set("Cache-Control","no-store");x.getResponseHeaders().set("X-Content-Type-Options","nosniff");
        x.getResponseHeaders().set("Referrer-Policy","no-referrer");x.getResponseHeaders().set("X-Frame-Options","DENY");
        x.getResponseHeaders().set("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
    }
}
