package veil;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Immutable world creation rules. Saved before host boot and checked on every load. */
public record WorldCreationConfig(String worldName,long seed,boolean hardcore,
                                  String hostType,String enemyDensity,String loot,
                                  int maxPlayers,int aiPopulation) {
    public WorldCreationConfig {
        if(worldName==null||!worldName.matches("[A-Za-z0-9][A-Za-z0-9 _-]{0,47}")||
            !Set.of("solo","multi").contains(hostType)||
            !Set.of("low","normal","high").contains(enemyDensity)||
            !Set.of("scarce","normal","abundant").contains(loot)||
            maxPlayers<1||maxPlayers>32||aiPopulation<0||aiPopulation>24||
            (hostType.equals("solo")&&maxPlayers!=1))
            throw new IllegalArgumentException("Invalid world creation settings");
    }
    public static WorldCreationConfig defaults(){
        return new WorldCreationConfig("New Settlement",new java.security.SecureRandom().nextLong(),
            false,"solo","normal","normal",1,6);
    }
    public static Path root(){return Path.of("worlds").toAbsolutePath().normalize();}
    public String folder(){return worldName.trim().replace(' ','_');}
    public Path directory(){return root().resolve(folder());}
    private Properties properties(){
        Properties p=new Properties();
        p.setProperty("name",worldName);
        p.setProperty("seed",Long.toString(seed));
        p.setProperty("hardcore",Boolean.toString(hardcore));
        p.setProperty("hostType",hostType);
        p.setProperty("enemyDensity",enemyDensity);
        p.setProperty("loot",loot);
        p.setProperty("maxPlayers",Integer.toString(maxPlayers));
        p.setProperty("aiPopulation",Integer.toString(aiPopulation));
        return p;
    }
    public static WorldCreationConfig load(Path directory)throws IOException {
        Properties p=new Properties();
        try(InputStream in=Files.newInputStream(directory.resolve("world.properties"))){p.load(in);}
        try{
            if(p.size()!=8)throw new IllegalArgumentException("Unexpected world fields");
            String h=p.getProperty("hardcore","");
            if(!h.equals("true")&&!h.equals("false"))
                throw new IllegalArgumentException("Invalid hardcore setting");
            WorldCreationConfig result=new WorldCreationConfig(p.getProperty("name"),
                Long.parseLong(p.getProperty("seed")),Boolean.parseBoolean(h),
                p.getProperty("hostType"),p.getProperty("enemyDensity"),
                p.getProperty("loot"),Integer.parseInt(p.getProperty("maxPlayers")),
                Integer.parseInt(p.getProperty("aiPopulation")));
            if(!directory.toAbsolutePath().normalize().equals(result.directory()))
                throw new IllegalArgumentException("World directory/name mismatch");
            return result;
        }catch(RuntimeException invalid){throw new IOException("Corrupt world settings",invalid);}
    }
    public void saveNew()throws IOException{
        Path dir=directory();
        Files.createDirectories(root());
        if(Files.exists(dir))throw new FileAlreadyExistsException("World already exists: "+worldName);
        Files.createDirectory(dir);
        try{
            Path temp=Files.createTempFile(dir,".world-", ".tmp");
            try{
                try(OutputStream out=Files.newOutputStream(temp)){
                    properties().store(out,"The Malefic Veil world rules v1");
                }
                Files.move(temp,dir.resolve("world.properties"),StandardCopyOption.ATOMIC_MOVE);
            }finally{Files.deleteIfExists(temp);}
            if(!equals(load(dir)))throw new IOException("World configuration read-back mismatch");
        }catch(IOException failure){
            Files.deleteIfExists(dir.resolve("world.properties"));
            Files.deleteIfExists(dir);
            throw failure;
        }
    }
    public static List<WorldCreationConfig> list()throws IOException{
        if(!Files.isDirectory(root()))return List.of();
        List<WorldCreationConfig> worlds=new ArrayList<>();
        try(var stream=Files.list(root())){
            for(Path path:stream.filter(Files::isDirectory).sorted().toList()){
                if(Files.exists(path.resolve("world.properties")))worlds.add(load(path));
            }
        }
        return List.copyOf(worlds);
    }
}
