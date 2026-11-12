package lk.ac.kln.hallsync.web;

import com.google.gson.*;
import lk.ac.kln.hallsync.ApplicationServices;
import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import lk.ac.kln.hallsync.service.AccessRules;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

/** Source-linked campus reference data. Unconfirmed capacities never become bookable halls. */
public final class CampusDirectory {
    private static final JsonObject DATA=load();
    private CampusDirectory() { }
    private static JsonObject load() {
        try(var stream=CampusDirectory.class.getResourceAsStream("/campus.json")) {
            if(stream==null)throw new IllegalStateException("Campus catalogue is missing.");
            return JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
        } catch(IOException e) { throw new UncheckedIOException(e); }
    }
    public static JsonObject data() { return DATA.deepCopy(); }
    private static String key(String name) { return name.strip().toLowerCase(Locale.ROOT).replace("&","and").replaceAll("\\s+"," "); }
    /** Each addition uses existing authorised database procedures; retrying preserves existing rows/IDs. */
    public static synchronized Map<String,Integer> install(ApplicationServices app,AuthenticatedUser user) throws SQLException {
        AccessRules.administrator(user);
        Set<String> departments=new HashSet<>();for(var d:app.departments.list())departments.add(key(d.getDepartmentName()));
        Set<String> buildings=new HashSet<>();for(var b:app.catalog.buildings())buildings.add(key(b.code()));
        int addedDepartments=0,addedBuildings=0,addedHalls=0;
        for(JsonElement f:DATA.getAsJsonArray("faculties"))for(JsonElement n:f.getAsJsonObject().getAsJsonArray("departments")) {
            String name=n.getAsString();if(departments.add(key(name))) { app.departments.add(user,name);addedDepartments++; }
        }
        for(JsonElement e:DATA.getAsJsonArray("buildings")) {
            var b=e.getAsJsonObject();String code=b.get("code").getAsString();
            if(buildings.add(key(code))) { app.catalog.addBuilding(user,code,b.get("name").getAsString(),b.get("location").getAsString());addedBuildings++; }
        }
        var known=app.catalog.halls();var actualBuildings=app.catalog.buildings();
        for(var item:DATA.getAsJsonArray("venues")) {
            var v=item.getAsJsonObject();if(v.get("capacity").isJsonNull())continue;
            String code=v.get("buildingCode").getAsString(),number=v.get("number").getAsString();
            if(known.stream().noneMatch(h->h.buildingCode().equalsIgnoreCase(code)&&h.number().equalsIgnoreCase(number))) {
                int building=actualBuildings.stream().filter(b->b.code().equalsIgnoreCase(code)).findFirst().orElseThrow().id();
                app.catalog.addHall(user,building,number,v.get("floor").isJsonNull()?null:v.get("floor").getAsString(),v.get("capacity").getAsInt());addedHalls++;
            }
        }
        return Map.of("departmentsAdded",addedDepartments,"buildingsAdded",addedBuildings,"hallsAdded",addedHalls);
    }
}
