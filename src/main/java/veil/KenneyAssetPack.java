package veil;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
/** Read the original Kenney Retro Urban Kit 2.0 CC0 ZIP without trusting arbitrary paths. */
public final class KenneyAssetPack {
    public static final String FILE="retro-urban-kit-2.0.zip";
    private final Map<String,byte[]> models,textures;
    private static volatile KenneyAssetPack instance;
    private KenneyAssetPack(Map<String,byte[]> m,Map<String,byte[]> t){
        models=Collections.unmodifiableMap(m);textures=Collections.unmodifiableMap(t);
    }
    public static KenneyAssetPack load()throws IOException{
        if(instance!=null)return instance;
        synchronized(KenneyAssetPack.class){
            if(instance==null){
                Path local=Path.of("assets","game","kenney",FILE);
                if(Files.isRegularFile(local))try(InputStream in=Files.newInputStream(local)){
                    instance=read(in);
                }else try(InputStream in=KenneyAssetPack.class.getResourceAsStream("/kenney/"+FILE)){
                    if(in==null)throw new FileNotFoundException("CC0 city pack not packaged");
                    instance=read(in);
                }
            }
            return instance;
        }
    }
    public static KenneyAssetPack read(InputStream stream)throws IOException{
        Map<String,byte[]> m=new LinkedHashMap<>(),t=new LinkedHashMap<>();
        String prefix="Models/GLB format/";
        long all=0;
        try(ZipInputStream zip=new ZipInputStream(new BufferedInputStream(stream))){
            ZipEntry entry;
            while((entry=zip.getNextEntry())!=null){
                String name=entry.getName();
                boolean mesh=name.startsWith(prefix)&&name.endsWith(".glb")
                    &&name.indexOf('/',prefix.length())<0;
                boolean texture=name.startsWith(prefix+"Textures/")&&name.endsWith(".png")
                    &&name.indexOf('/',(prefix+"Textures/").length())<0;
                if(!mesh&&!texture)continue;
                String key=name.substring((prefix+(mesh?"":"Textures/")).length());
                if(!key.matches(mesh?"[a-z0-9-]+\\.glb":"[A-Za-z0-9_-]+\\.png"))
                    throw new IOException("Unsafe ZIP entry "+name);
                ByteArrayOutputStream b=new ByteArrayOutputStream();
                byte[] buffer=new byte[8192];int count;
                while((count=zip.read(buffer))!=-1){
                    all+=count;
                    if(b.size()+count>3_000_000||all>25_000_000)
                        throw new IOException("Oversized GLB/texture data");
                    b.write(buffer,0,count);
                }
                if((mesh?m:t).putIfAbsent(key,b.toByteArray())!=null)
                    throw new IOException("Duplicate ZIP path "+name);
            }
        }
        if(m.size()!=124||t.size()!=22)
            throw new IOException("Unrecognized licensed source ZIP: GLBs="+m.size()+" PNGs="+t.size());
        return new KenneyAssetPack(m,t);
    }
    public Set<String> modelNames(){return models.keySet();}
    public Set<String> textureNames(){return textures.keySet();}
    public byte[] model(String id)throws IOException{
        if(id==null||!id.matches("[a-z0-9-]+\\.glb"))throw new IOException("Bad model identifier");
        byte[] data=models.get(id);
        if(data==null)throw new FileNotFoundException("Not in urban pack: "+id);
        return data.clone();
    }
    public byte[] texture(String id)throws IOException{
        if(id==null||!id.matches("[A-Za-z0-9_-]+\\.png"))throw new IOException("Bad texture identifier");
        byte[] data=textures.get(id);
        if(data==null)throw new FileNotFoundException("Not in urban pack: "+id);
        return data.clone();
    }
}
