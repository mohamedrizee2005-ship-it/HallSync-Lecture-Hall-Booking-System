package lk.ac.kln.hallsync.web;

import lk.ac.kln.hallsync.*;
import java.nio.file.Files;
import java.nio.file.Path;

/** Connection state shared by requests; existing local files are never overwritten. */
public final class ServerState {
    private final Path file;
    private volatile DatabaseConfig config;
    private volatile ApplicationServices app;
    private volatile String problem;
    public ServerState(Path file) {
        this.file=file;
        if(Files.exists(file)) {
            try { DatabaseConfig loaded=DatabaseConfig.load(file);verify(loaded);config=loaded;app=new ApplicationServices(new DatabaseConnectionFactory(loaded)); }
            catch(Exception error) { problem="We could not connect using the saved database settings. Check MySQL is running, then restart HallSync."; }
        }
    }
    public boolean ready() { return app!=null; }
    public boolean hasFile() { return Files.exists(file); }
    public String problem() { return problem; }
    public ApplicationServices app() { if(app==null)throw new HttpError(503,"Connect your database to continue.");return app; }
    public DatabaseConfig configuration() { if(config==null)throw new HttpError(503,"Connect your database first.");return config; }
    public synchronized void connect(String url,String user,char[] password) throws Exception {
        if(hasFile()||ready())throw new HttpError(409,"Saved settings already exist. Reuse them or move the local settings file before reconnecting.");
        if("root".equalsIgnoreCase(user.strip()))throw new IllegalArgumentException("Use the hallsync_app connection account, not the database administrator.");
        DatabaseConfig candidate=DatabaseConfig.fromValues(url,user,password);verify(candidate);candidate.saveNew(file);
        config=candidate;app=new ApplicationServices(new DatabaseConnectionFactory(candidate));problem=null;
    }
    private static void verify(DatabaseConfig config) throws Exception {
        try(var c=new DatabaseConnectionFactory(config).open();var s=c.prepareStatement("SELECT BookingID FROM v_booking_details LIMIT 1");var rows=s.executeQuery()) { }
    }
}
