package veil;

import java.util.*;

/** Continental mesh must use actual seed, tile geometry, and bounded visibility. */
public final class ContinentVistaTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        for(long seed:new long[]{20261020L,75L}){
            World world=new World(seed);
            ContinentVista land=new ContinentVista(world);
            check(Math.abs(land.sample(0,77).elevation())<.000001,
                "City edge must be flush with the streets");
            check(Math.abs(land.sample(World.SIZE,77).elevation())<.000001,
                "Far city edge terrain must align");
            var patches=land.patches();
            check(patches.size()>=40&&patches.size()<=120,"Terrain sector count outside bounded cache");
            int triangles=0;
            for(ContinentVista.Patch p:patches){
                check(p.vertexData().length==p.triangles()*3*ContinentVista.STRIDE,
                    "Unaligned GPU terrain vertex buffer");
                check(p.triangles()>0,"Empty terrain chunk");
                triangles+=p.triangles();
                for(float f:p.vertexData())check(Float.isFinite(f),"Terrain contains NaN/inf");
            }
            check(triangles>10000,"The void beyond the 132x132 city is not filled");
            check(land.sample(-30,25).biome()!=null&&land.sample(160,166).biome()!=null,
                "Continent samples missing biome");
            var foliage=land.foliage();
            check(foliage.equals(land.foliage()),"Foliage is not seeded repeatably");
            var source=KenneyAssetPack.load().modelNames();
            int sourceTrees=0;
            for(var p:foliage){
                check(p.x()<0||p.y()<0||p.x()>=World.SIZE||p.y()>=World.SIZE,
                    "Wilderness vegetation spawned over authored city");
                check(!p.solid(),"Preview-only trees cannot silently acquire server collision");
                check(source.contains(p.model()+".glb"),"Wilderness mesh absent from source kit");
                check(Math.abs(p.heightOffset()-land.sample(p.x(),p.y()).elevation())<.001,
                    "Tree root does not follow terrain elevation");
                sourceTrees++;
            }
            check(sourceTrees>=5,"No continental woodland generated");
            UrbanScene gpuScene=new UrbanScene(world,foliage);
            check(gpuScene.instances().size()==world.urbanProps.size()+foliage.size(),
                "Lost GPU wilderness models");
            System.out.println("PASS ContinentVistaTest seed="+seed+" patches="+patches.size()+
                " triangles="+triangles+" wildernessTrees="+sourceTrees);
        }
    }
}
