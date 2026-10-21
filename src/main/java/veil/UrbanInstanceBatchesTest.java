package veil;

import java.util.*;

/** Instanced city mesh contract: every repeat points at one shared source GLB. */
public final class UrbanInstanceBatchesTest {
    private static void check(boolean result,String reason){if(!result)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception{
        World world=new World(20261020L);
        UrbanScene scene=new UrbanScene(world);
        UrbanInstanceBatches batches=scene.batches();
        check(batches.instanceCount()==scene.instances().size(),"Lost/repeated city instances");
        check(batches.uniqueGeometryCount()>25&&batches.uniqueGeometryCount()<=scene.models().size(),
            "All instanced geometry must be backed by a source mesh");
        check(batches.uniqueGeometryCount()<batches.instanceCount()/10,
            "City meshes are not reused heavily enough to benefit from instancing");
        long packed=0;
        Set<String> unique=new HashSet<>();
        for(UrbanInstanceBatches.Batch batch:batches.all()){
            check(unique.add(batch.modelId()),"Duplicate geometry batch");
            check(batch.mesh()==scene.models().get(batch.modelId()),
                "Batch does not share cached geometry identity");
            check(batch.transformBuffer().length==batch.count()*UrbanInstanceBatches.STRIDE,
                "Mismatched transform buffer");
            for(int i=0;i<batch.count();i++)
                for(int k=0;k<UrbanInstanceBatches.STRIDE;k++)
                    check(Float.isFinite(batch.get(i,k)),"Unusable GPU instance attribute");
            packed+=batch.count();
            if(batch.count()>0){
                float original=batch.get(0,0);
                float[] copy=batch.transformBuffer();copy[0]+=777;
                check(batch.get(0,0)==original,"Mutable GPU instance buffer leaked");
            }
        }
        check(packed==scene.instances().size(),"Wrong aggregate mesh-instance counts");
        check(batches.naiveTriangleCount()>batches.sourceTriangleCount()*5,
            "Repeated meshes did not reuse source geometry");
        boolean threw=false;
        try{new UrbanInstanceBatches(scene.models(),
            List.of(new UrbanScene.ModelInstance("missing",0,0,0,1)));}
        catch(IllegalArgumentException expected){threw=true;}
        check(threw,"Unknown/unlicensed mesh silently accepted");
        System.out.println("PASS UrbanInstanceBatchesTest: "+
            batches.uniqueGeometryCount()+" shared GLB meshes, "+
            batches.instanceCount()+" instance transforms, "+
            batches.sourceTriangleCount()+" unique mesh triangles, "+
            batches.naiveTriangleCount()+" repeated triangle instances");
    }
}
