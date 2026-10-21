package veil;

import java.io.IOException;
import java.util.*;

/** Selected licensed Kenney meshes placed by seed in the modular city. */
public final class UrbanScene {
    public record ModelInstance(String model,double x,double y,double heading,double scale){}
    private static final String[] IDS={
        "road-asphalt-straight","detail-barrier-type-a","detail-bench",
        "detail-dumpster-closed","detail-light-single"
    };
    private final Map<String,GlbMesh> models=new LinkedHashMap<>();
    private final List<ModelInstance> positions;
    public UrbanScene(World city)throws IOException{
        for(String id:IDS)models.put(id,GlbMesh.asset(id));
        List<ModelInstance> items=new ArrayList<>();
        // Five recognizable props by the default spawn for first-person QA.
        items.add(new ModelInstance("detail-barrier-type-a",5.2,1.5,0,1.7));
        items.add(new ModelInstance("detail-bench",4.3,2.5,0,1.8));
        items.add(new ModelInstance("detail-dumpster-closed",9.1,2.2,0,1.6));
        items.add(new ModelInstance("detail-light-single",7.8,2.2,0,3.0));
        items.add(new ModelInstance("road-asphalt-straight",4.7,1.5,Math.PI/2,1.0));
        for(int blockX=0;blockX<World.SIZE/12;blockX++){
            for(int blockY=0;blockY<World.SIZE/12;blockY++){
                long hash=World.mix(city.seed^((long)blockX*42137)^((long)blockY*99497));
                if((hash&3)==0)continue;
                int x=blockX*12,y=blockY*12;
                if(x>World.SIZE-10||y>World.SIZE-10)continue;
                String model=IDS[(int)Math.floorMod(hash>>>5,IDS.length)];
                double px=x+5.5,py=y+1.45;
                if(city.canWalk(px,py))items.add(new ModelInstance(model,px,py,0,
                    model.contains("light")?2.8:model.contains("road")?1.0:1.5));
            }
        }
        positions=List.copyOf(items);
    }
    public Map<String,GlbMesh> models(){return Collections.unmodifiableMap(models);}
    public List<ModelInstance> instances(){return positions;}
}
