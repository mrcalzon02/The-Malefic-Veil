package veil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.Comparator;

/**
 * Durable startup/crash breadcrumbs. Each event is appended synchronously:
 * the last completed stage survives a forced process termination.
 */
public final class DesktopDiagnostics {
    private static final Object LOCK=new Object();
    private DesktopDiagnostics(){}
    private static Path location(){
        String override=System.getProperty("veil.diagnostics.dir","");
        return Path.of(override.isBlank()?"logs":override);
    }
    public static void note(String event){
        synchronized(LOCK){
            String line=Instant.now()+" pid="+ProcessHandle.current().pid()+" "+event;
            System.out.println(line);
            try{
                Path dir=location();Files.createDirectories(dir);
                Path log=dir.resolve("game-events.log");
                if(Files.isRegularFile(log)&&Files.size(log)>5_000_000L){
                    Files.move(log,dir.resolve("game-events.previous.log"),
                        StandardCopyOption.REPLACE_EXISTING);
                }
                Files.writeString(log,line+System.lineSeparator(),StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,StandardOpenOption.APPEND,
                    StandardOpenOption.SYNC);
            }catch(IOException error){
                System.err.println("Failed to persist game diagnostics: "+error);
            }
        }
    }
    public static void install(String role){
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
            note("UNCAUGHT thread="+thread.getName()+" type="+error.getClass().getName()+
                " message="+error.getMessage());
            StringWriter message=new StringWriter();
            error.printStackTrace(new PrintWriter(message));
            for(String line:message.toString().split("\\R"))note("TRACE "+line);
        });
        note("PROCESS_START role="+role+" java="+System.getProperty("java.version")+
            " os="+System.getProperty("os.name"));
        Runtime.getRuntime().addShutdownHook(new Thread(
            ()->note("PROCESS_SHUTDOWN role="+role),"Veil-diagnostics-shutdown"));
    }
}
