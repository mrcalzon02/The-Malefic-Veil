package veil;

import dev.mrcalzon.water.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * One immutable, author-owned water snapshot for a persistent dedicated world.
 * The world save's city-A.water uses the SAME WaterDocument schema as the
 * native editor. Only explicit server-side reload/save may change the snapshot;
 * connected clients cannot supply geometry or override server weather.
 *
 * This is authoritative authored-footprint metadata, not swimming/collision
 * or a replacement for the seeded continental ocean.
 */
public final class HostWorldWater {
    public record Snapshot(WaterDocument document, WaterModule module) {
        public String revision(){return document.revision();}
        public boolean wet(double x,double z){
            for(var body:module.bodies())if(body.wet(x,z))return true;
            return false;
        }
    }
    private final Path path;
    private final String level;
    private volatile Snapshot current;

    public HostWorldWater(Path worldDirectory,long seed)throws IOException {
        level="seed-"+seed;
        path=worldDirectory==null?null:worldDirectory.resolve("city-A.water");
        current=readDisk();
    }

    private Snapshot validate(WaterDocument document)throws IOException {
        if(!level.equals(document.level()))
            throw new IOException("Authored water belongs to a different world seed");
        try{
            var bodies=new ArrayList<WaterModule.Body>();
            for(WaterPlacement placement:document.placements())
                bodies.add(placement.body(WaterTextures::builtin,
                    p->(x,z)->p.y()-3,List.of()));
            return new Snapshot(document,new WaterModule(bodies));
        }catch(RuntimeException invalid){
            throw new IOException("Invalid authored water geometry or texture",invalid);
        }
    }

    private Snapshot readDisk()throws IOException {
        if(path==null||!Files.exists(path))
            return validate(new WaterDocument(level,List.of()));
        if(!Files.isRegularFile(path))
            throw new IOException("World authored water is not a regular file");
        return validate(WaterDocument.read(Files.newInputStream(path)));
    }

    public Snapshot snapshot(){return current;}

    /** Explicit operator reload; a stale host view must not silently replace it. */
    public synchronized void reload(String expectedRevision)throws IOException {
        if(expectedRevision==null||!expectedRevision.equals(current.revision()))
            throw new IOException("Stale host water revision");
        current=readDisk();
    }

    /** Server-side import, with disk revision comparison before atomic replacement.
     * No cross-process filesystem lock is implied by this optimistic guard.
     */
    public synchronized void save(WaterDocument next,String expectedRevision)
        throws IOException {
        if(path==null)throw new IOException("In-memory world has no water save path");
        if(expectedRevision==null||!expectedRevision.equals(current.revision())||
           !expectedRevision.equals(readDisk().revision()))
            throw new IOException("Authored water changed; reload before saving");
        Snapshot validated=validate(next);
        next.save(path);
        current=validated;
    }
}
