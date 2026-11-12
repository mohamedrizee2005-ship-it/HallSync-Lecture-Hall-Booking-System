package lk.ac.kln.hallsync;

import lk.ac.kln.hallsync.web.WebServer;
import lk.ac.kln.hallsync.web.PublicOrigin;
import java.awt.Desktop;
import java.net.BindException;
import java.nio.file.Path;

/** Starts the local Java backend and opens the bundled browser interface. */
public final class HallSyncApp {
    private HallSyncApp() { }
    public static void main(String[] args) {
        try {
            Path file=Path.of(args.length==0?"db.local.properties":args[0]);
            int preferred=Integer.parseInt(System.getProperty("hallsync.port","8080"));
            String publicOrigin=PublicOrigin.validate(System.getenv("HALLSYNC_PUBLIC_ORIGIN"));
            WebServer server;
            try { server=new WebServer(file,preferred,publicOrigin); } catch(BindException busy) { if(publicOrigin!=null)throw busy;server=new WebServer(file,0); }
            server.start();WebServer running=server;Runtime.getRuntime().addShutdownHook(new Thread(running::close));
            System.out.println("HallSync is ready: "+server.browserAddress());
            System.out.println("Keep this window open while using HallSync. Press Ctrl+C to stop.");
            if(!Boolean.getBoolean("hallsync.noBrowser")&&Desktop.isDesktopSupported()&&Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try { Desktop.getDesktop().browse(server.browserAddress()); } catch(Exception unavailable) { System.out.println("Open the address above in your browser."); }
            }
        } catch(Exception error) { System.err.println("HallSync could not start ("+error.getClass().getSimpleName()+"). Check Java, the launch port and the local configuration.");System.exit(1); }
    }
}
