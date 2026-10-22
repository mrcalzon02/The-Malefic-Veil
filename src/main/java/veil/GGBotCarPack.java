package veil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/**
 * GGBotNet PSX Style Cars (CC0): source OBJ UVs and PNG atlases are authoritative.
 * This library is scenery only. Kenney cargo trucks and Vehicles remain untouched.
 *
 * Source: https://ggbot.itch.io/psx-style-cars
 * The source ZIP is fetched and SHA-verified by the existing release packaging
 * workflow. Local developers may place it at assets/game/ggbot/psx-style-cars.zip.
 */
public final class GGBotCarPack {
    public static final String ARCHIVE="psx-style-cars.zip";
    private static final String SOURCE_SHA256=
        "db67b0b0fbaa02454a5d000dc9ec1cc53f360e8f41ab44f3fb1c7e8f71e699e4";
    private static GGBotCarPack singleton;
    private final Map<String,byte[]> contents=new HashMap<>();
    private final Map<String,Source> models=new TreeMap<>();
    private final Map<String,GlbMesh> cache=new HashMap<>();
    private record Source(String obj,String png){}

    private GGBotCarPack(byte[] bytes)throws IOException {
        if(bytes.length<100_000||bytes.length>10_000_000 ||
                !hash(bytes).equals(SOURCE_SHA256))
            throw new IOException("GGBot source archive missing, modified, or unverified");
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            int count=0;
            while((entry=zip.getNextEntry())!=null) {
                if(entry.isDirectory())continue;
                if(++count>180 || entry.getName().contains("..") ||
                    entry.getName().startsWith("/") || entry.getName().contains("\\" ))
                    throw new IOException("Unsafe GGBot archive entry");
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                byte[] buffer=new byte[8192];
                int n;
                while((n=zip.read(buffer))!=-1){
                    if(out.size()+n>3_000_000)throw new IOException("Oversized car asset");
                    out.write(buffer,0,n);
                }
                if(contents.putIfAbsent(entry.getName(),out.toByteArray())!=null)
                    throw new IOException("Duplicate car asset "+entry.getName());
            }
        }
        List<String> objects=contents.keySet().stream().filter(p->
            p.matches("Car [0-9][0-9]/[^/]+(?i:\\.obj)")).sorted().toList();
        if(objects.size()!=10)throw new IOException("Expected ten original car meshes");
        for(String obj:objects) {
            String folder=obj.substring(0,obj.indexOf('/')+1);
            String stem=obj.substring(folder.length(),obj.length()-4);
            String mtl=folder+stem+".mtl";
            String blend=folder+stem+".blend";
            if(!contents.containsKey(mtl)||!contents.containsKey(blend)||
                !new String(contents.get(blend),0,Math.min(8,contents.get(blend).length),
                    StandardCharsets.ISO_8859_1).equals("BLENDER-"))
                throw new IOException("Source reference missing for "+obj);
            String material=new String(contents.get(mtl),StandardCharsets.UTF_8);
            String atlas=null;
            for(String line:material.split("\\R"))
                if(line.trim().startsWith("map_Kd "))atlas=line.trim().substring(7).trim();
            if(atlas==null)throw new IOException("No source diffuse atlas: "+mtl);
            boolean sourceAtlas=false;
            for(String candidate:contents.keySet())
                if(candidate.equalsIgnoreCase(folder+atlas))sourceAtlas=true;
            if(!sourceAtlas)throw new IOException("Broken MTL atlas: "+mtl);
            final String lower=stem.toLowerCase(Locale.ROOT);
            for(String png:contents.keySet()) {
                if(!png.startsWith(folder)||!png.toLowerCase(Locale.ROOT).endsWith(".png"))
                    continue;
                String base=png.substring(folder.length(),png.length()-4);
                String filename=base.toLowerCase(Locale.ROOT);
                if(!filename.equals(lower)&&!filename.startsWith(lower+"_"))continue;
                // The taxi and police variants have distinct dedicated geometry.
                if(lower.equals("car5")&&
                    (filename.startsWith("car5_police")||filename.startsWith("car5_taxi")))
                    continue;
                String id="ggbot-"+folder.substring(4,6)+"-"+filename;
                if(models.putIfAbsent(id,new Source(obj,png))!=null)
                    throw new IOException("Duplicate GGBot model id "+id);
            }
        }
        if(models.size()!=39)throw new IOException("Expected 39 source-UV car appearances, got "+models.size());
    }

    private static String hash(byte[] bytes)throws IOException {
        try{
            byte[] raw=MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(raw);
        }catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
    }
    public static synchronized GGBotCarPack load()throws IOException {
        if(singleton!=null)return singleton;
        Path local=Path.of("assets","game","ggbot",ARCHIVE);
        byte[] data=null;
        if(Files.isRegularFile(local))data=Files.readAllBytes(local);
        else try(InputStream stream=GGBotCarPack.class.getResourceAsStream("/ggbot/"+ARCHIVE)){
            if(stream!=null)data=stream.readAllBytes();
        }
        if(data==null)throw new FileNotFoundException("Missing approved GGBot archive: "+local);
        singleton=new GGBotCarPack(data);
        return singleton;
    }
    /** Developer/source checkout fallback only; shipping builds must pass required(). */
    public static boolean available(){
        try{return load()!=null;}catch(IOException e){return false;}
    }
    public static GGBotCarPack required()throws IOException{return load();}
    public Set<String> names(){return Collections.unmodifiableSet(models.keySet());}
    public synchronized GlbMesh model(String name)throws IOException {
        if(!models.containsKey(name))throw new IOException("Unknown GGBot car "+name);
        GlbMesh cached=cache.get(name);
        if(cached!=null)return cached;
        Source pair=models.get(name);
        GlbMesh parsed=GlbMesh.texturedObj(contents.get(pair.obj()),
            contents.get(pair.png()),name);
        if(parsed.triangleCount()<250||parsed.triangleCount()>550)
            throw new IOException("Unexpected GGBot triangle count "+name);
        cache.put(name,parsed);
        return parsed;
    }
    /** Only contemporary color variants for the current non-snowy city. */
    public List<String> civilianModels(){
        return models.keySet().stream()
            .filter(s->!s.contains("_snow"))
            .filter(s->!s.contains("police"))
            .filter(s->!s.contains("taxi"))
            .filter(s->!s.startsWith("ggbot-06-"))
            .toList();
    }
    /** Rare police and taxi parking variants with dedicated source geometry. */
    public List<String> serviceModels(){
        return models.keySet().stream().filter(s->
            s.contains("police")||s.contains("taxi")).toList();
    }
    public List<String> wreckModels(){
        return models.keySet().stream().filter(s->s.startsWith("ggbot-06-")).toList();
    }
}
