package veil;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Offline updater checks: payload, traversal, recovery, saves and idempotency. */
public final class DesktopUpdaterTest {
    private static void yes(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    private static byte[] zip(Map<String,String> entries)throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(ZipOutputStream output=new ZipOutputStream(bytes)){
            for(var entry:entries.entrySet()){
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
    private static void delete(Path dir)throws IOException {
        if(!Files.exists(dir))return;
        try(var walk=Files.walk(dir)){
            for(Path p:walk.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);
        }
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("malefic-updater-test-");
        String previous="a".repeat(40),next="b".repeat(40);
        try{
            byte[] contents="verified build".getBytes(StandardCharsets.UTF_8);
            yes(DesktopUpdater.sha256(contents).length()==64,"SHA256 formatting");
            DesktopUpdater.installZip(zip(Map.of("MaleficVeil.jar","verified build")),root);
            yes(Arrays.equals(contents,Files.readAllBytes(root.resolve("MaleficVeil.jar"))),
                "Extracted file corrupted");
            boolean rejected=false;
            try{DesktopUpdater.installZip(zip(Map.of("../evil.dat","escape")),root);}
            catch(IOException expected){rejected=true;}
            yes(rejected,"Path traversal accepted");
            yes(!Files.exists(root.resolve("../evil.dat").normalize()),
                "Traversal escaped installation root");
            rejected=false;
            try{DesktopUpdater.installZip(zip(Map.of("saved-world/world.properties","bad")),root);}
            catch(IOException expected){rejected=true;}
            yes(rejected,"Updater attempted to write save data");
            Files.createDirectories(root.resolve("worlds/settlement"));
            Files.writeString(root.resolve("worlds/settlement/save.dat"),"my pre-1.0 save");
            Files.writeString(root.resolve("MaleficVeil.jar"),"old game");
            Files.writeString(root.resolve("version.txt"),previous);
            Files.writeString(root.resolve("updater.jar"),"old updater");
            Files.createDirectories(root.resolve("assets/game"));
            Files.writeString(root.resolve("assets/game/old.txt"),"old art");
            Files.createDirectories(root.resolve("lib"));
            Files.writeString(root.resolve("lib/shared-psx-weather-java17-v1.jar"),"old weather");
            byte[] release=zip(Map.of(
                "MaleficVeil.jar","new game",
                "version.txt",next,
                "updater.jar","new updater",
                "assets/game/new.txt","new art",
                "lib/shared-psx-weather-java17-v1.jar","new weather"));
            DesktopUpdater.applyVerified(root,release,next);
            yes(DesktopUpdater.installed(root).equals(next),"Upgrade version missing");
            yes(Files.readString(root.resolve("MaleficVeil.jar")).equals("new game"),
                "Upgrade game missing");
            yes(Files.readString(root.resolve("updater.jar")).equals("new updater"),
                "Upgrade bootstrap missing");
            yes(Files.readString(root.resolve("lib/shared-psx-weather-java17-v1.jar"))
                .equals("new weather"),"Shared weather dependency missing");
            yes(!Files.exists(root.resolve("assets/game/old.txt")),"Obsolete assets not retired");
            yes(Files.readString(root.resolve("worlds/settlement/save.dat"))
                .equals("my pre-1.0 save"),"Save data modified by upgrade");
            // Fail validation before changing the live files.
            rejected=false;
            try{DesktopUpdater.applyVerified(root,
                zip(Map.of("MaleficVeil.jar","incomplete")),previous);}
            catch(IOException expected){rejected=true;}
            yes(rejected,"Incomplete upgrade accepted");
            yes(DesktopUpdater.installed(root).equals(next),
                "Invalid archive damaged live version");
            // Model termination after backup and partial replacement.
            Path backup=root.resolve(".update-backup");
            Files.createDirectories(backup);
            Files.move(root.resolve("MaleficVeil.jar"),backup.resolve("MaleficVeil.jar"));
            Files.move(root.resolve("version.txt"),backup.resolve("version.txt"));
            Files.writeString(root.resolve(".update-state"),"BACKUP");
            DesktopUpdater.recover(root);
            yes(DesktopUpdater.installed(root).equals(next),
                "Interrupted backup did not recover");
            Files.createDirectories(backup);
            for(String name:List.of("MaleficVeil.jar","version.txt",
                "updater.jar","assets/game","lib/shared-psx-weather-java17-v1.jar")){
                Path source=root.resolve(name),destination=backup.resolve(name);
                Files.createDirectories(destination.getParent());
                Files.move(source,destination);
            }
            Files.writeString(root.resolve("MaleficVeil.jar"),"partial update");
            Files.writeString(root.resolve("version.txt"),"bad revision");
            Files.writeString(root.resolve(".update-state"),"INSTALL");
            DesktopUpdater.recover(root);
            yes(DesktopUpdater.installed(root).equals(next),
                "Interrupted install did not restore old version");
            yes(Files.readString(root.resolve("MaleficVeil.jar")).equals("new game"),
                "Interrupted install retained corrupt payload");
            yes(Files.readString(root.resolve("worlds/settlement/save.dat"))
                .equals("my pre-1.0 save"),"Recovery damaged saved world");
            yes(!Files.exists(root.resolve(".update-state")),"Journal not cleared");
            System.out.println("PASS DesktopUpdaterTest");
        }finally{delete(root);}
    }
}
