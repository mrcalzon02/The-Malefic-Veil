package veil;

import java.io.IOException;
import java.util.*;

/** License-selected Kenney source models; uses the host's exact tile/prop positions. */
public final class UrbanScene {
    public record ModelInstance(String model,double x,double y,double heading,double scale,
                                double opacity,double heightOffset){
        public ModelInstance(String model,double x,double y,double heading,double scale){
            this(model,x,y,heading,scale,1,0);
        }
        public ModelInstance(String model,double x,double y,double heading,double scale,double opacity){
            this(model,x,y,heading,scale,opacity,0);
        }
    }
    private static final String[] IDS={
        "road-asphalt-straight","detail-barrier-type-a","detail-bench","truck-green",
        "detail-dumpster-closed","detail-light-single"
    };
    private final Map<String,GlbMesh> models=new LinkedHashMap<>();
    private final List<ModelInstance> positions;
    public UrbanScene(World city)throws IOException{
        // Expand only source mesh IDs that the authoritative world actually places.
        KenneyAssetPack pack=KenneyAssetPack.load();
        Set<String> used=new LinkedHashSet<>(Arrays.asList(IDS));
        for(World.UrbanProp p:city.urbanProps)used.add(p.model());
        for(String id:used){
            if(!pack.modelNames().contains(id+".glb"))
                throw new IOException("Generated unlicensed/unavailable urban model "+id);
            models.put(id,GlbMesh.asset(id));
        }
        List<ModelInstance> items=new ArrayList<>();
        for(World.UrbanProp p:city.urbanProps)
            items.add(new ModelInstance(p.model(),p.x(),p.y(),p.heading(),p.scale()));
        positions=List.copyOf(items);
    }
    public Map<String,GlbMesh> models(){return Collections.unmodifiableMap(models);}
    public List<ModelInstance> instances(){return positions;}
}
