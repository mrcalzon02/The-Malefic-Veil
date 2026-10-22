package veil;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.zip.*;
import javax.swing.JOptionPane;

/**
 * Restart-time updater: discovers published releases INCLUDING prereleases,
 * asks the user, validates bytes, and transactionally replaces managed files.
 * Launch from a temporary copy of updater.jar so Windows can replace the jar.
 */
public final class DesktopUpdater {
    private static final String API=
        "https://api.github.com/repos/mrcalzon02/The-Malefic-Veil/releases";
    private static final int MAX_API=8*1024*1024,MAX_ZIP=160*1024*1024,
        MAX_EXTRACTED=512*1024*1024,MAX_FILES=16000;
    private static final String[] MANAGED={
        "MaleficVeil.jar","updater.jar","version.txt","assets/game",
        "lib/shared-psx-weather-java17-v1.jar"
    };
    private static final HttpClient HTTP=HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.ALWAYS).build();

    private DesktopUpdater(){}

    private static byte[] fetch(URI url,int cap)throws IOException,InterruptedException{
        HttpRequest request=HttpRequest.newBuilder(url).timeout(Duration.ofMinutes(4))
            .header("Accept","application/vnd.github+json")
            .header("User-Agent","MaleficVeil-DevUpdater/2").GET().build();
        HttpResponse<InputStream> response=HTTP.send(request,HttpResponse.BodyHandlers.ofInputStream());
        try(InputStream in=response.body()){
            if(response.statusCode()!=200)
                throw new IOException("HTTP "+response.statusCode()+" from "+url.getHost());
            byte[] bytes=in.readNBytes(cap+1);
            if(bytes.length>cap)throw new IOException("Update download exceeds safety limit");
            return bytes;
        }
    }
    private static List<ReleaseCatalog.Release> releases()throws Exception{
        List<ReleaseCatalog.Release> all=new ArrayList<>();
        // The API's creation ordering is not necessarily publication ordering.
        for(int page=1;page<=20;page++){
            String json=new String(fetch(URI.create(API+"?per_page=100&page="+page),MAX_API),
                StandardCharsets.UTF_8);
            all.addAll(ReleaseCatalog.parse(json));
            if(ReleaseCatalog.resultCount(json)<100)break;
        }
        return all;
    }
    static String sha256(byte[] bytes)throws Exception{
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder out=new StringBuilder(64);
        for(byte b:digest)out.append(String.format(Locale.ROOT,"%02x",b&255));
        return out.toString();
    }
    static void log(Path root,String line){
        String entry=Instant.now()+" "+line;
        System.out.println(entry);
        try{
            Path dir=root.resolve("logs");Files.createDirectories(dir);
            Files.writeString(dir.resolve("updater.log"),entry+System.lineSeparator(),
                StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND,
                StandardOpenOption.SYNC);
        }catch(IOException error){System.err.println("Could not write updater.log: "+error);}
    }
    static String installed(Path root)throws IOException{
        Path version=root.resolve("version.txt");
        return Files.isRegularFile(version)?Files.readString(version).trim():"";
    }
    static Properties metadata(ReleaseCatalog.Release release)throws Exception{
        URI url=release.asset("update.properties");
        if(url==null)throw new IOException("Release lacks update.properties");
        Properties props=new Properties();
        try(InputStream in=new ByteArrayInputStream(fetch(url,32768))){props.load(in);}
        String version=props.getProperty("version",""),name=props.getProperty("archive",""),
            digest=props.getProperty("sha256","");
        if(!version.matches("[0-9a-f]{40}")||
           !name.equals("MaleficVeil-"+version.substring(0,12)+".zip")||
           !digest.matches("[0-9a-f]{64}")||
           !release.tag().equals("dev-"+version.substring(0,12))||
           release.asset(name)==null)
            throw new IOException("Release tag, archive, or checksum metadata mismatch");
        return props;
    }
    static boolean isNewer(ReleaseCatalog.Release latest,String local,
                           List<ReleaseCatalog.Release> index){
        if(local.matches("[0-9a-f]{40}")&&
            local.substring(0,12).equals(latest.tag().substring(4)))return false;
        for(ReleaseCatalog.Release r:index){
            if(r.tag().equals("dev-"+local.substring(0,Math.min(12,local.length())))&&
                r.published().isAfter(latest.published()))return false;
        }
        return true;
    }
    private static boolean approve(ReleaseCatalog.Release latest){
        if(java.awt.GraphicsEnvironment.isHeadless()){
            System.out.println("Interactive approval unavailable; run with --install to consent.");
            return false;
        }
        return JOptionPane.showConfirmDialog(null,
            "Development update "+latest.tag()+" is available.\n"+
            "Download and install it now? (Saved worlds are preserved,\n"+
            "but pre-1.0 save compatibility is not guaranteed.)",
            "The Malefic Veil — Update Available",JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE)==JOptionPane.YES_OPTION;
    }
    private static boolean allowed(String name){
        if(name.isBlank()||name.startsWith("/")||name.contains("\\")||name.contains(":")||
            name.contains("//")||name.contains("\0"))return false;
        for(String part:name.split("/"))if(part.equals(".")||part.equals(".."))return false;
        return name.equals("MaleficVeil.jar")||name.equals("updater.jar")||
            name.equals("version.txt")||name.equals("assets")||
            name.equals("assets/")||name.equals("assets/game")||
            name.equals("assets/game/")||name.startsWith("assets/game/")||
            name.equals("lib")||name.equals("lib/")||
            name.equals("lib/shared-psx-weather-java17-v1.jar");
    }
    /** Strict expansion into an isolated, disposable staging directory. */
    static void installZip(byte[] zip,Path directory)throws IOException{
        Files.createDirectories(directory);
        long expanded=0;int files=0;
        try(ZipInputStream input=new ZipInputStream(new ByteArrayInputStream(zip))){
            for(ZipEntry entry;(entry=input.getNextEntry())!=null;){
                String name=entry.getName();
                if(!allowed(name))throw new IOException("Unsafe update entry: "+name);
                Path target=directory.resolve(name).normalize();
                if(!target.startsWith(directory))throw new IOException("Path escapes install root");
                if(entry.isDirectory()){Files.createDirectories(target);continue;}
                if(++files>MAX_FILES)throw new IOException("Excessive archive entries");
                Files.createDirectories(target.getParent());
                try(OutputStream out=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)){
                    byte[] buffer=new byte[65536];int count;
                    while((count=input.read(buffer))!=-1){
                        expanded+=count;
                        if(expanded>MAX_EXTRACTED)
                            throw new IOException("Expanded update exceeds size limit");
                        out.write(buffer,0,count);
                    }
                }
                input.closeEntry();
            }
        }
    }
    private static void deleteTree(Path folder)throws IOException{
        if(!Files.exists(folder))return;
        try(var paths=Files.walk(folder)){
            for(Path p:paths.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);
        }
    }
    private static void move(Path source,Path target)throws IOException{
        Files.createDirectories(target.getParent());
        try{Files.move(source,target,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException unsupported){Files.move(source,target);}
    }
    private static void state(Path root,String value)throws IOException{
        // Journal transitions are atomic: a kill cannot leave a truncated phase.
        Path temporary=root.resolve(".update-state.tmp");
        Files.writeString(temporary,value+"\n",StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.SYNC);
        Path journal=root.resolve(".update-state");
        try{Files.move(temporary,journal,StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException unsupported){
            Files.move(temporary,journal,StandardCopyOption.REPLACE_EXISTING);
        }
    }
    /** Deterministic crash recovery, also executed before ANY network request. */
    static void recover(Path root)throws IOException{
        Path flag=root.resolve(".update-state");
        Path backup=root.resolve(".update-backup");
        Path stage=root.resolve(".update-stage");
        if(!Files.exists(flag)){
            deleteTree(backup);deleteTree(stage);
            Files.deleteIfExists(root.resolve(".update-state.tmp"));
            return;
        }
        String phase=Files.readString(flag).trim();
        if(!phase.equals("BACKUP")&&!phase.equals("INSTALL"))
            throw new IOException("Unknown updater transaction state: "+phase);
        if(phase.equals("INSTALL"))
            for(String name:MANAGED)deleteTree(root.resolve(name));
        for(String name:MANAGED){
            Path original=backup.resolve(name);
            if(Files.exists(original)){
                deleteTree(root.resolve(name));
                move(original,root.resolve(name));
            }
        }
        deleteTree(backup);deleteTree(stage);
        Files.delete(flag);
        log(root,"RECOVERED interrupted update transaction ("+phase+")");
    }
    private static void requirePayload(Path stage,String version)throws IOException{
        for(String path:MANAGED)if(!Files.exists(stage.resolve(path)))
            throw new IOException("Update missing required file/directory "+path);
        if(!Files.isRegularFile(stage.resolve("MaleficVeil.jar"))||
           !Files.isRegularFile(stage.resolve("updater.jar"))||
           !Files.isRegularFile(stage.resolve("lib/shared-psx-weather-java17-v1.jar"))||
           !Files.readString(stage.resolve("version.txt")).trim().equals(version))
            throw new IOException("Update payload does not match target release");
    }
    /** Backup -> install -> commit; no save, logs or runtime state are migrated. */
    static void applyVerified(Path root,byte[] archive,String version)throws IOException{
        Files.createDirectories(root);
        recover(root);
        Path stage=root.resolve(".update-stage"),backup=root.resolve(".update-backup");
        try{
            installZip(archive,stage);
            requirePayload(stage,version);
            Files.createDirectories(backup);
            state(root,"BACKUP");
            for(String name:MANAGED)if(Files.exists(root.resolve(name)))
                move(root.resolve(name),backup.resolve(name));
            state(root,"INSTALL");
            for(String name:MANAGED)move(stage.resolve(name),root.resolve(name));
            if(!installed(root).equals(version))
                throw new IOException("Post-install version verification failed");
            // Mark success BEFORE removing backups. An interrupted cleanup is harmless.
            Files.delete(root.resolve(".update-state"));
            deleteTree(backup);deleteTree(stage);
        }catch(IOException failure){
            try{recover(root);}
            catch(IOException rollback){
                failure.addSuppressed(rollback);
                throw new IOException("UPDATE FAILED AND ROLLBACK FAILED; inspect logs",failure);
            }
            throw failure;
        }
    }
    public static void main(String[] args){
        Path root=Path.of(".").toAbsolutePath().normalize();String mode="--ask";
        for(String arg:args){
            if(arg.startsWith("--"))mode=arg;
            else root=Path.of(arg).toAbsolutePath().normalize();
        }
        try{
            if(!Set.of("--ask","--check","--install").contains(mode))
                throw new IOException("Usage: DesktopUpdater [install-directory] [--ask|--check|--install]");
            log(root,"START mode="+mode+" location="+root);
            recover(root);
            List<ReleaseCatalog.Release> index=releases();
            ReleaseCatalog.Release newest=ReleaseCatalog.newest(index);
            if(newest==null){log(root,"NO_PUBLISHED_RELEASE No eligible development release found");return;}
            Properties props=metadata(newest);
            String version=props.getProperty("version"),local=installed(root);
            if(!isNewer(newest,local,index)){
                log(root,"CURRENT_OR_NEWER installed="+local+" latest="+newest.tag());
                return;
            }
            log(root,"UPDATE_AVAILABLE current="+(local.isEmpty()?"none":local)+
                " next="+version+" published="+newest.published()+" (prereleases included)");
            if(mode.equals("--check"))return;
            if(mode.equals("--ask")&&!approve(newest)){
                log(root,"UPDATE_DECLINED");return;
            }
            log(root,"DOWNLOAD_START "+newest.tag());
            byte[] bytes=fetch(newest.asset(props.getProperty("archive")),MAX_ZIP);
            if(!sha256(bytes).equals(props.getProperty("sha256")))
                throw new IOException("Archive SHA-256 mismatch; refusing to install");
            applyVerified(root,bytes,version);
            log(root,"INSTALL_VERIFIED version="+version);
        }catch(Exception failure){
            log(root,"UPDATE_ERROR "+failure.getClass().getSimpleName()+": "+failure.getMessage());
            for(Throwable cause:failure.getSuppressed())
                log(root,"ROLLBACK_ERROR "+cause);
            if(Files.exists(root.resolve(".update-state"))){
                System.err.println("Unsafe interrupted update. See logs/updater.log");
                System.exit(2);
            }
        }
    }
}
