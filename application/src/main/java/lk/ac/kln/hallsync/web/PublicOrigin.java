package lk.ac.kln.hallsync.web;

import java.net.URI;
import java.util.Locale;

/** Explicit HTTPS origin for a loopback reverse proxy. No forwarded header is trusted. */
public final class PublicOrigin {
    private PublicOrigin() { }
    public static String validate(String value) {
        if(value==null||value.isBlank())return null;
        URI uri;
        try { uri=URI.create(value.strip()); }catch(IllegalArgumentException e) { throw new IllegalArgumentException("Enter an HTTPS public origin."); }
        if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!(uri.getPath().isEmpty()||uri.getPath().equals("/")))throw new IllegalArgumentException("Public origin must be HTTPS with no path, query or credentials.");
        return "https://"+uri.getHost().toLowerCase(Locale.ROOT)+(uri.getPort()==-1||uri.getPort()==443?"":":"+uri.getPort());
    }
}
