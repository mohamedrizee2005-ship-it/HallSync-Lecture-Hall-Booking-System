package lk.ac.kln.hallsync.web;

import com.sun.net.httpserver.HttpExchange;
import lk.ac.kln.hallsync.auth.AuthenticatedUser;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory, expiring browser sessions. No authentication data is stored in localStorage. */
public final class BrowserSessions {
    private final boolean secure;
    public BrowserSessions() { this(false); }
    public BrowserSessions(boolean secure) { this.secure=secure; }
    private static final long TTL=Duration.ofMinutes(30).toMillis();
    private final ConcurrentHashMap<String,Session> sessions=new ConcurrentHashMap<>();
    private final SecureRandom random=new SecureRandom();
    public static final class Session {
        private final String id,csrf;
        private volatile long touched=System.currentTimeMillis();
        private volatile AuthenticatedUser user;
        private int failures;
        private long blockedUntil;
        private Session(String id,String csrf,AuthenticatedUser user) { this.id=id;this.csrf=csrf;this.user=user; }
        public String csrf() { return csrf; }
        public AuthenticatedUser user() { return user; }
        public synchronized void mayAuthenticate() {
            if (blockedUntil>System.currentTimeMillis()) { throw new HttpError(429,"Too many attempts. Please wait one minute and try again."); }
        }
        public synchronized void failedAuthentication() {
            if (++failures>=5) { blockedUntil=System.currentTimeMillis()+60000;failures=0; }
        }
    }
    public Session get(HttpExchange exchange) {
        long now=System.currentTimeMillis(); sessions.entrySet().removeIf(e -> now-e.getValue().touched>TTL);
        String id=null,cookie=exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie!=null) { for (String part:cookie.split(";")) { String[] pair=part.strip().split("=",2);if(pair.length==2&&pair[0].equals("HALLSYNC_SESSION"))id=pair[1]; } }
        Session found=id==null?null:sessions.get(id);
        if(found!=null) { found.touched=now;return found; }
        if(sessions.size()>=500)throw new HttpError(503,"Please close unused sessions and try again.");
        return create(exchange,null);
    }
    public Session rotate(HttpExchange exchange,Session previous,AuthenticatedUser user) {
        sessions.remove(previous.id);return create(exchange,user);
    }
    private Session create(HttpExchange exchange,AuthenticatedUser user) {
        Session session=new Session(token(),token(),user);sessions.put(session.id,session);
        exchange.getResponseHeaders().add("Set-Cookie","HALLSYNC_SESSION="+session.id+"; Path=/; HttpOnly; SameSite=Strict"+(secure?"; Secure":""));
        return session;
    }
    private String token() { byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
}
