package veil.gpu;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Verifiable, on-demand rendering of the original 150 128x128 crafting sprites.
 * Only original source icon PNGs are used. No fabricated stand-in pictures.
 */
public final class GpuCraftingIcons implements AutoCloseable {
    public record Entry(String id,String label,String pack,String entry,String sha256){}
    private final Map<String,Entry> catalog=new TreeMap<>();
    private final Map<String,Texture> loaded=new HashMap<>();
    private final Map<String,byte[]> zips=new HashMap<>();
    private final List<String> packs=List.of("shared_core","shared_equipment",
        "unraveled_industry","penitent_ritual","begotten_biology");
    public GpuCraftingIcons(){
        try(InputStream stream=resource("/crafting/icons_index.csv")){
            List<String> rows=new String(stream.readAllBytes(),StandardCharsets.UTF_8)
                .lines().toList();
            if(rows.size()!=151||!rows.get(0).equals("id,label,pack,entry,sha256"))
                throw new IOException("Original 150-icon manifest is incomplete");
            Map<String,Integer> count=new HashMap<>();
            for(int i=1;i<rows.size();i++){
                String[] col=rows.get(i).strip().split(",",-1);
                if(col.length!=5||!col[0].matches("[A-Z0-9_]{2,80}")||
                   !packs.contains(col[2])||
                   !col[3].equals("icons/"+col[0]+"_small.png")||
                   !col[4].matches("[a-f0-9]{64}"))
                    throw new IOException("Corrupt item icon manifest row "+i);
                if(catalog.putIfAbsent(col[0],
                    new Entry(col[0],col[1],col[2],col[3],col[4]))!=null)
                    throw new IOException("Duplicated item sprite "+col[0]);
                count.merge(col[2],1,Integer::sum);
            }
            for(String pack:packs){
                if(count.getOrDefault(pack,0)!=30)
                    throw new IOException("Incorrect source sprite count "+pack);
                try(InputStream zip=resource("/crafting/packs/"+pack+"_sprites_only.zip")){
                    zips.put(pack,zip.readAllBytes());
                }
            }
            System.out.println("CRAFTING_150_ICON_PACKS_READY entries="+catalog.size()+
                " originalZips="+zips.size());
        }catch(IOException issue){
            throw new IllegalStateException("Missing/corrupt real crafting icons",issue);
        }
    }
    private static InputStream resource(String path)throws IOException{
        InputStream stream=GpuCraftingIcons.class.getResourceAsStream(path);
        if(stream==null)throw new FileNotFoundException("Missing bundled crafting asset "+path);
        return stream;
    }
    public boolean contains(String id){return catalog.containsKey(id);}
    public String pack(String id){
        Entry source=catalog.get(id);
        return source==null?"":source.pack();
    }
    public Set<String> ids(){return Collections.unmodifiableSet(catalog.keySet());}
    public String label(String id){
        Entry item=catalog.get(id);
        return item==null?id.replace('_',' '):item.label();
    }
    /** A miss is explicitly represented as an unillustrated item, not a false icon. */
    public synchronized Texture icon(String id){
        Entry desc=catalog.get(id);
        if(desc==null)return null;
        Texture cached=loaded.get(id);
        if(cached!=null)return cached;
        byte[] png=null;
        try(ZipInputStream zip=new ZipInputStream(
            new ByteArrayInputStream(zips.get(desc.pack())))){
            ZipEntry item;
            while((item=zip.getNextEntry())!=null){
                if(item.getName().equals(desc.entry())){
                    png=zip.readNBytes(250001);
                    break;
                }
            }
            if(png==null||png.length<1000||png.length>250000)
                throw new IOException("Icon entry missing/oversize: "+id);
            String actual=HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(png));
            if(!actual.equals(desc.sha256()))
                throw new IOException("Tampered source icon "+id);
            Pixmap decoded=new Pixmap(png,0,png.length);
            try{
                if(decoded.getWidth()!=128||decoded.getHeight()!=128||
                   decoded.getFormat()!=Pixmap.Format.RGBA8888)
                    throw new IOException("Unexpected source icon dimensions: "+id);
                cached=new Texture(decoded);
                cached.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
                loaded.put(id,cached);
            }finally{decoded.dispose();}
            return cached;
        }catch(IOException|NoSuchAlgorithmException failure){
            throw new IllegalStateException("Original crafting icon failed validation "+id,failure);
        }
    }
    public void draw(SpriteBatch batch,String id,float x,float y,float size){
        Texture texture=icon(id);
        if(texture!=null)batch.draw(texture,x,y,size,size);
    }
    @Override public synchronized void close(){
        loaded.values().forEach(Texture::dispose);
        loaded.clear();zips.clear();
    }
}
