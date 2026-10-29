package veil;

import java.util.*;

/**
 * Immutable instancing-ready batches for the repeated Kenney city modules.
 *
 * Geometry is referenced ONCE per unique GLB mesh. Each six-float instance row is:
 * world X, world Y, rotation in radians, uniform scale, opacity, vertical offset.
 *
 * This is the renderer-independent data contract for uploading one GPU instance
 * buffer and issuing one instanced draw per material/mesh/LOD batch. The current
 * Java2D renderer is a software fallback and still rasterizes individual faces.
 */
public final class UrbanInstanceBatches {
    public static final int STRIDE=6;
    public static final class Batch {
        private final String modelId;
        private final GlbMesh mesh;
        private final float[] transforms;
        private Batch(String modelId,GlbMesh mesh,float[] transforms){
            this.modelId=modelId;
            this.mesh=mesh;
            this.transforms=transforms;
        }
        public String modelId(){return modelId;}
        /** Mesh reference is shared with UrbanScene's one-time source GLB cache. */
        public GlbMesh mesh(){return mesh;}
        public int count(){return transforms.length/STRIDE;}
        public float get(int instance,int component){
            if(component<0||component>=STRIDE||instance<0||instance>=count())
                throw new IndexOutOfBoundsException();
            return transforms[instance*STRIDE+component];
        }
        /** Defensive copy for a future VBO upload. Never expose mutable cache state. */
        public float[] transformBuffer(){return transforms.clone();}
    }
    private final List<Batch> batches;
    private final int instanceCount;
    public UrbanInstanceBatches(Map<String,GlbMesh> meshes,
                               List<UrbanScene.ModelInstance> placements){
        Objects.requireNonNull(meshes);
        Objects.requireNonNull(placements);
        Map<String,List<UrbanScene.ModelInstance>> grouped=new LinkedHashMap<>();
        for(var placement:placements){
            if(!meshes.containsKey(placement.model()))
                throw new IllegalArgumentException("Missing licensed mesh "+placement.model());
            if(!Double.isFinite(placement.x())||!Double.isFinite(placement.y())||
               !Double.isFinite(placement.heading())||!Double.isFinite(placement.scale())||
               !Double.isFinite(placement.opacity())||!Double.isFinite(placement.heightOffset())||
               placement.scale()<=0||placement.opacity()<0||placement.opacity()>1)
                throw new IllegalArgumentException("Invalid instance transform");
            grouped.computeIfAbsent(placement.model(),k->new ArrayList<>()).add(placement);
        }
        List<Batch> result=new ArrayList<>();
        for(var entry:grouped.entrySet()){
            List<UrbanScene.ModelInstance> list=entry.getValue();
            float[] transforms=new float[list.size()*STRIDE];
            for(int n=0;n<list.size();n++){
                UrbanScene.ModelInstance p=list.get(n);
                int at=n*STRIDE;
                transforms[at]=(float)p.x();
                transforms[at+1]=(float)p.y();
                transforms[at+2]=(float)p.heading();
                transforms[at+3]=(float)p.scale();
                transforms[at+4]=(float)p.opacity();
                transforms[at+5]=(float)p.heightOffset();
            }
            result.add(new Batch(entry.getKey(),meshes.get(entry.getKey()),transforms));
        }
        batches=List.copyOf(result);
        instanceCount=placements.size();
    }
    public List<Batch> all(){return batches;}
    public int instanceCount(){return instanceCount;}
    public int uniqueGeometryCount(){return batches.size();}
    public long sourceTriangleCount(){
        long total=0;
        for(Batch b:batches)total+=b.mesh().triangleCount();
        return total;
    }
    public long naiveTriangleCount(){
        long total=0;
        for(Batch b:batches)total+=(long)b.mesh().triangleCount()*b.count();
        return total;
    }
}
