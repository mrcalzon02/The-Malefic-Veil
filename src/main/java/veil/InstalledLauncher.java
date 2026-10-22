package veil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.Arrays;

/**
 * Stable native-installer entrypoint. Lives in Launcher.jar (outside the update
 * payload), so Windows never locks MaleficVeil.jar while applying an update.
 *
 * jpackage embeds its own Java runtime. Neither PATH nor a user JDK is needed.
 * The original game's working directory stays the installed app directory.
 */
public final class InstalledLauncher {
    private InstalledLauncher() {}

    static Path applicationDirectory() throws URISyntaxException {
        Path jar=Path.of(InstalledLauncher.class.getProtectionDomain()
            .getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
        return jar.getParent();
    }
    static Path runtimeJava() {
        Path bin=Path.of(System.getProperty("java.home"),"bin");
        Path javaw=bin.resolve("javaw.exe");
        if(Files.isRegularFile(javaw))return javaw;
        Path java=bin.resolve(System.getProperty("os.name","").toLowerCase()
            .contains("win")?"java.exe":"java");
        return java;
    }
    static void note(Path root,String message){
        try{
            Path logs=root.resolve("logs");
            Files.createDirectories(logs);
            Files.writeString(logs.resolve("launcher.log"),
                Instant.now()+" "+message+System.lineSeparator(),
                StandardCharsets.UTF_8,StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,StandardOpenOption.SYNC);
        }catch(IOException error){System.err.println("Launcher diagnostics failure: "+error);}
    }
    static int execute(Path root,Path java,Path log,String... command)
        throws IOException,InterruptedException {
        ProcessBuilder builder=new ProcessBuilder(command);
        builder.directory(root.toFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()));
        Process process=builder.start();
        return process.waitFor();
    }
    public static void main(String[] arguments){
        Path updaterCopy=null;
        try {
            Path root=applicationDirectory();
            Files.createDirectories(root.resolve("logs"));
            note(root,"NATIVE_LAUNCHER_START java="+System.getProperty("java.home")+
                " arguments="+Arrays.toString(arguments));
            String mode="--ask";
            if(arguments.length>0){
                if(arguments.length!=1||!Arrays.asList("--ask","--check","--install")
                    .contains(arguments[0])){
                    note(root,"INVALID_ARGUMENTS");
                    System.exit(2);
                }
                mode=arguments[0];
            }
            Path java=runtimeJava();
            if(!Files.isRegularFile(java))
                throw new IOException("Bundled Java runtime is missing: "+java);
            Path updater=root.resolve("updater.jar");
            Path game=root.resolve("MaleficVeil.jar");
            if(!Files.isRegularFile(updater)||!Files.isRegularFile(game))
                throw new IOException("Installation is missing game or updater.jar");
            updaterCopy=Files.createTempFile("malefic-veil-updater-", ".jar");
            Files.copy(updater,updaterCopy,StandardCopyOption.REPLACE_EXISTING);
            note(root,"UPDATER_BEGIN "+mode);
            int updateExit=execute(root,java,root.resolve("logs/updater-console.log"),
                java.toString(),"-cp",updaterCopy.toString(),"veil.DesktopUpdater",
                root.toString(),mode);
            note(root,"UPDATER_EXIT code="+updateExit);
            if(updateExit!=0)System.exit(updateExit);
            if(mode.equals("--check"))return;
            note(root,"GAME_START");
            int gameExit=execute(root,java,root.resolve("logs/game-console.log"),
                java.toString(),"-jar",game.toString());
            note(root,"GAME_EXIT code="+gameExit);
            if(gameExit!=0)System.exit(gameExit);
        }catch(Exception failure){
            try{
                Path root=applicationDirectory();
                note(root,"LAUNCH_FAILURE "+failure.getClass().getName()+": "+
                    failure.getMessage());
            }catch(Exception ignored){System.err.println("Launcher failed: "+failure);}
            System.exit(3);
        }finally{
            if(updaterCopy!=null)try{Files.deleteIfExists(updaterCopy);}
                catch(IOException ignored){}
        }
    }
}
