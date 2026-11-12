package lk.ac.kln.hallsync.web;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.*;

/** Loopback-only application server with bundled, offline-ready web assets. */
public final class WebServer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService workers=Executors.newFixedThreadPool(8);
    private final ServerState state;
    private final String publicOrigin;
    public WebServer(Path settings,int port) throws IOException {
        this(settings,port,null);
    }
    public WebServer(Path settings,int port,String publicOrigin) throws IOException {
        this.publicOrigin=PublicOrigin.validate(publicOrigin);
        state=new ServerState(settings);
        if(this.publicOrigin!=null&&!state.ready())throw new IllegalStateException("Connect MySQL locally before enabling shared access.");
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",port),32);
        String origin="http://127.0.0.1:"+server.getAddress().getPort();
        server.createContext("/api/",new ApiHandler(state,new BrowserSessions(this.publicOrigin!=null),origin,this.publicOrigin));server.createContext("/",this::asset);server.setExecutor(workers);
    }
    public void start() { server.start(); }
    public URI address() { return URI.create("http://127.0.0.1:"+server.getAddress().getPort()); }
    public URI browserAddress() { return publicOrigin==null?address():URI.create(publicOrigin); }
    public ServerState state() { return state; }
    private void asset(HttpExchange x) throws IOException {
        try {
            if(!x.getRequestMethod().equals("GET")&&!x.getRequestMethod().equals("HEAD")) { x.sendResponseHeaders(405,-1);return; }
            String path=x.getRequestURI().getPath();if(path.equals("/"))path="/index.html";
            // Exact allowlist prevents exposing local settings, source files or traversal paths.
            Map<String,String> types=Map.ofEntries(
                Map.entry("/index.html","text/html; charset=utf-8"),Map.entry("/app.css","text/css; charset=utf-8"),Map.entry("/app.js","text/javascript; charset=utf-8"),Map.entry("/theme.js","text/javascript; charset=utf-8"),
                Map.entry("/assets/brand.svg","image/svg+xml"),Map.entry("/assets/kelaniya-science.jpg","image/jpeg"),Map.entry("/assets/kelaniya-campus.jpg","image/jpeg"),Map.entry("/assets/kelaniya-computing.jpg","image/jpeg"));
            String type=types.get(path);if(type==null) { x.sendResponseHeaders(404,-1);return; }
            try(InputStream input=WebServer.class.getResourceAsStream("/web"+path)) {
                if(input==null) { x.sendResponseHeaders(404,-1);return; }
                byte[] bytes=input.readAllBytes();ApiHandler.securityHeaders(x);x.getResponseHeaders().set("Content-Type",type);
                if(x.getRequestMethod().equals("HEAD"))x.sendResponseHeaders(200,-1);
                else { x.sendResponseHeaders(200,bytes.length);x.getResponseBody().write(bytes); }
            }
        } finally { x.close(); }
    }
    @Override public void close() { server.stop(0);workers.shutdownNow(); }
}
