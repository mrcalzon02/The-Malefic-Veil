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
    private static final int BUCKET=12;
    private final Map<Long,List<ModelInstance>> spatial=new HashMap<>();
    private static long bucketId(int x,int y){return ((long)x<<32)|(y&0xffffffffL);}
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
            items.add(new ModelInstance(p.model(),p.x(),p.y(),p.heading(),p.scale(),1,p.heightOffset()));
        positions=List.copyOf(items);
        for(ModelInstance item:positions){
            int bx=(int)Math.floor(item.x()/BUCKET),by=(int)Math.floor(item.y()/BUCKET);
            spatial.computeIfAbsent(bucketId(bx,by),k->new ArrayList<>()).add(item);
        }
    }
    public Map<String,GlbMesh> models(){return Collections.unmodifiableMap(models);}
    public List<ModelInstance> instances(){return positions;}
    /** Spatially bounded candidates; distant city meshes are never visited each render tick. */
    public List<ModelInstance> nearby(double x,double y,double distance){
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(distance)||
           distance<0||distance>100)throw new IllegalArgumentException("Invalid visual radius");
        int lowX=(int)Math.floor((x-distance)/BUCKET),highX=(int)Math.floor((x+distance)/BUCKET);
        int lowY=(int)Math.floor((y-distance)/BUCKET),highY=(int)Math.floor((y+distance)/BUCKET);
        List<ModelInstance> result=new ArrayList<>();
        for(int by=lowY;by<=highY;by++)for(int bx=lowX;bx<=highX;bx++){
            List<ModelInstance> bucket=spatial.get(bucketId(bx,by));
            if(bucket!=null)result.addAll(bucket);
        }
        result.sort(Comparator.comparingDouble(item->
            Math.pow(item.x()-x,2)+Math.pow(item.y()-y,2)));
        return result;
    }
}
