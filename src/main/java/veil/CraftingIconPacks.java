package veil;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;

/**
 * The 150 independently sliced crafting icons, indexed by stable recipe/item ID.
 * Does not mint items or define a recipe. Reads five checked-in sprite ZIPs and
 * verifies every PNG against an integrity manifest. No network access.
 */
public final class CraftingIconPacks {
    public record Entry(String id,String label,String group,String zipEntry,String sha256) {}
    private static final List<String> GROUPS=List.of(
        "shared_core","shared_equipment","unraveled_industry",
        "penitent_ritual","begotten_biology");
    private final Map<String,Entry> catalog;
    private final Map<String,BufferedImage> decoded=new HashMap<>();
    private final Path directory;

    private CraftingIconPacks(Path directory,Map<String,Entry> catalog){
        this.directory=directory;
        this.catalog=Collections.unmodifiableMap(new LinkedHashMap<>(catalog));
    }
    public static CraftingIconPacks load(Path repositoryRoot)throws IOException{
        Objects.requireNonNull(repositoryRoot);
        Path folder=repositoryRoot.resolve("assets/game/crafting");
        Path csv=folder.resolve("icons_index.csv");
        if(!Files.isRegularFile(csv))throw new FileNotFoundException("Crafting icon manifest missing: "+csv);
        List<String> lines=Files.readAllLines(csv);
        if(lines.size()!=151||!lines.get(0).equals("id,label,pack,entry,sha256"))
            throw new IOException("Expected 150 icon-index entries");
        Map<String,Entry> entries=new LinkedHashMap<>();
        Map<String,Integer> counts=new HashMap<>();
        for(int n=1;n<lines.size();n++){
            String[] cells=lines.get(n).strip().split(",",-1);
            if(cells.length!=5)throw new IOException("Malformed icon manifest row "+n);
            String id=cells[0],label=cells[1],group=cells[2],zipEntry=cells[3],sha=cells[4];
            if(!id.matches("[A-Z0-9_]{2,80}")||label.isBlank()||label.length()>80||
               !GROUPS.contains(group)||!zipEntry.equals("icons/"+id+"_small.png")||
               !sha.matches("[a-f0-9]{64}")||entries.putIfAbsent(id,
                new Entry(id,label,group,zipEntry,sha))!=null)
                throw new IOException("Corrupt or duplicate item icon ID: "+id);
            counts.merge(group,1,Integer::sum);
        }
        for(String group:GROUPS){
            if(counts.getOrDefault(group,0)!=30)throw new IOException("Missing sprites: "+group);
            if(!Files.isRegularFile(folder.resolve("packs/"+group+"_sprites_only.zip")))
                throw new FileNotFoundException("Sprite pack missing: "+group);
        }
        return new CraftingIconPacks(folder,entries);
    }
    public Map<String,Entry> entries(){return catalog;}
    public String label(String itemId){
        Entry e=catalog.get(itemId);
        return e==null?null:e.label();
    }
    /** Decode a single PNG, fail closed on tampered or missing ZIP contents. */
    public synchronized BufferedImage icon(String itemId)throws IOException{
        Entry desc=catalog.get(itemId);
        if(desc==null)throw new FileNotFoundException("Unknown crafting item icon: "+itemId);
        BufferedImage cached=decoded.get(itemId);
        if(cached!=null)return cached;
        Path zipPath=directory.resolve("packs/"+desc.group()+"_sprites_only.zip");
        byte[] bytes;
        try(ZipFile zip=new ZipFile(zipPath.toFile())){
            ZipEntry match=zip.getEntry(desc.zipEntry());
            if(match==null||match.isDirectory()||match.getSize()<1000||match.getSize()>250_000)
                throw new IOException("Invalid sprite entry: "+desc.zipEntry());
            try(InputStream in=zip.getInputStream(match)){
                bytes=in.readNBytes(250_001);
            }
        }
        if(bytes.length>250_000)throw new IOException("Sprite exceeded size budget: "+itemId);
        String hash=HexFormat.of().formatHex(sha256(bytes));
        if(!hash.equals(desc.sha256()))throw new IOException("Sprite SHA256 mismatch: "+itemId);
        BufferedImage image=ImageIO.read(new ByteArrayInputStream(bytes));
        if(image==null||image.getWidth()!=128||image.getHeight()!=128||
           !image.getColorModel().hasAlpha())
            throw new IOException("Invalid sprite dimensions/alpha: "+itemId);
        decoded.put(itemId,image);
        return image;
    }
    private static byte[] sha256(byte[] bytes)throws IOException{
        try{return MessageDigest.getInstance("SHA-256").digest(bytes);}
        catch(NoSuchAlgorithmException ex){throw new IOException(ex);}
    }
}
