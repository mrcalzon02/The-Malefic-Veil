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

    /**
     * Explicit saved-editor → selected saved-world handoff. The world configuration
     * and both independent on-disk revisions must agree before any host overwrite.
     * This does not hot-replicate water to clients or lock other processes.
     */
    public static Snapshot importSavedEditorWater(Path editorFile,
            WorldCreationConfig selectedWorld,String expectedEditorRevision,
            String expectedHostRevision)throws IOException {
        if(editorFile==null||selectedWorld==null||
           expectedEditorRevision==null||expectedHostRevision==null)
            throw new IOException("Water import requires an explicit source, world and two revisions");
        Path source=editorFile.toAbsolutePath().normalize();
        Path destination=selectedWorld.directory().resolve("city-A.water").toAbsolutePath().normalize();
        if(source.equals(destination)||source.getParent()==null||
           !source.getFileName().toString().equals("city-A.water")||
           !source.getParent().getFileName().toString().equals("seed-"+selectedWorld.seed()))
            throw new IOException("Select the saved water document for this editor world");
        if(!Files.isRegularFile(source))
            throw new IOException("Save the editor water document before host export");
        if(!selectedWorld.equals(WorldCreationConfig.load(selectedWorld.directory())))
            throw new IOException("Selected host world changed; reselect before export");
        WaterDocument authored=WaterDocument.read(Files.newInputStream(source));
        if(!authored.level().equals("seed-"+selectedWorld.seed()))
            throw new IOException("Editor water belongs to a different world seed");
        if(!authored.revision().equals(expectedEditorRevision))
            throw new IOException("Saved editor water changed; reload before export");
        HostWorldWater host=new HostWorldWater(selectedWorld.directory(),selectedWorld.seed());
        if(!host.snapshot().revision().equals(expectedHostRevision))
            throw new IOException("Selected host water revision changed; confirm again");
        // A second source read closes the ordinary edit-before-write window.
        if(!authored.equals(WaterDocument.read(Files.newInputStream(source))))
            throw new IOException("Editor source changed during water export");
        host.save(authored,expectedHostRevision);
        return host.snapshot();
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
