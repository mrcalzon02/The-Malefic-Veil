package veil;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Independent updater bootstrap. Run from updater.jar BEFORE starting the game. */
public final class DesktopUpdater {
    private static final URI RELEASE=URI.create(
        "https://github.com/mrcalzon02/The-Malefic-Veil/releases/latest/download/");
    private static final HttpClient HTTP=HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(java.time.Duration.ofSeconds(10)).build();
    private DesktopUpdater(){}
    private static byte[] fetch(URI url)throws IOException,InterruptedException{
        HttpRequest request=HttpRequest.newBuilder(url).timeout(java.time.Duration.ofMinutes(3))
            .header("User-Agent","MaleficVeil-Updater").GET().build();
        HttpResponse<byte[]> reply=HTTP.send(request,HttpResponse.BodyHandlers.ofByteArray());
        if(reply.statusCode()!=200)throw new IOException("HTTP "+reply.statusCode()+" for "+url);
        return reply.body();
    }
    static String sha256(byte[] bytes)throws Exception{
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder b=new StringBuilder();
        for(byte x:digest)b.append(String.format(Locale.ROOT,"%02x",x&255));
        return b.toString();
    }
    static void installZip(byte[] zip,Path directory)throws IOException{
        Files.createDirectories(directory);
        try(ZipInputStream input=new ZipInputStream(new ByteArrayInputStream(zip))){
            for(ZipEntry e;(e=input.getNextEntry())!=null;){
                Path target=directory.resolve(e.getName()).normalize();
                if(!target.startsWith(directory)||e.getName().contains("\\"))
                    throw new IOException("Unsafe update archive entry");
                if(e.isDirectory()){Files.createDirectories(target);continue;}
                Files.createDirectories(target.getParent());
                Path tmp=Files.createTempFile(target.getParent(),".update-", ".tmp");
                try{
                    Files.copy(input,tmp,StandardCopyOption.REPLACE_EXISTING);
                    try{Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);}
                    catch(AtomicMoveNotSupportedException unsupported){
                        Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);
                    }
                }finally{Files.deleteIfExists(tmp);}
            }
        }
    }
    public static void main(String[] arguments)throws Exception{
        Path root=Path.of(arguments.length>0?arguments[0]:".").toAbsolutePath().normalize();
        // This process runs from an independent bootstrap jar. It must never
        // overwrite updater.jar while executing, and must never touch user saves.
        try{
            Properties remote=new Properties();
            remote.load(new ByteArrayInputStream(fetch(RELEASE.resolve("update.properties"))));
            String version=remote.getProperty("version","");
            String artifact=remote.getProperty("archive","");
            String expected=remote.getProperty("sha256","");
            if(!version.matches("[a-f0-9]{40}")||
               !artifact.matches("MaleficVeil-[a-f0-9]{12}\\.zip")||
               !expected.matches("[a-f0-9]{64}"))
                throw new IOException("Invalid signed-by-checksum update metadata");
            Path current=root.resolve("version.txt");
            if(Files.exists(current)&&Files.readString(current).trim().equals(version)){
                System.out.println("The Malefic Veil is current ("+version.substring(0,12)+")");
                return;
            }
            System.out.println("Downloading The Malefic Veil "+version.substring(0,12));
            byte[] zip=fetch(RELEASE.resolve(artifact));
            if(!sha256(zip).equalsIgnoreCase(expected))
                throw new IOException("Downloaded update checksum mismatch");
            // Archive content is limited by packager to game.jar, game assets
            // and version.txt. Never let a downloaded archive replace launchers,
            // bootstrap updater, or arbitrary user-writable server data.
            try(ZipInputStream scan=new ZipInputStream(new ByteArrayInputStream(zip))){
                for(ZipEntry e;(e=scan.getNextEntry())!=null;){
                    String name=e.getName();
                    if(!(name.equals("MaleficVeil.jar")||name.equals("version.txt")||
                        name.startsWith("assets/game/")))
                        throw new IOException("Unexpected update entry: "+name);
                }
            }
            installZip(zip,root);
            if(!Files.readString(root.resolve("version.txt")).trim().equals(version))
                throw new IOException("Installed version does not match the release");
            System.out.println("Update installed: "+version.substring(0,12));
        }catch(IOException|InterruptedException e){
            System.err.println("Update unavailable: "+e.getMessage()+
                " — keeping installed game");
        }
    }
}
