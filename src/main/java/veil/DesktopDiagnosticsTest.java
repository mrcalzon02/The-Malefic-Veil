package veil;

import java.nio.file.*;

/** Verify each breadcrumb is visible on disk immediately, without clean shutdown. */
public final class DesktopDiagnosticsTest {
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("malefic-diagnostics-test-");
        String previous=System.getProperty("veil.diagnostics.dir");
        try {
            System.setProperty("veil.diagnostics.dir",dir.toString());
            DesktopDiagnostics.note("TEST_START_STAGE");
            Path log=dir.resolve("game-events.log");
            if(!Files.exists(log)||!Files.readString(log).contains("TEST_START_STAGE"))
                throw new AssertionError("Stage not immediately persisted");
            DesktopDiagnostics.note("TEST_SECOND_STAGE");
            if(!Files.readString(log).contains("TEST_SECOND_STAGE"))
                throw new AssertionError("Subsequent stage missing");
            System.out.println("PASS DesktopDiagnosticsTest");
        }finally{
            if(previous==null)System.clearProperty("veil.diagnostics.dir");
            else System.setProperty("veil.diagnostics.dir",previous);
            try(var files=Files.list(dir)){
                for(Path file:files.toList())Files.deleteIfExists(file);
            }
            Files.deleteIfExists(dir);
        }
    }
}
