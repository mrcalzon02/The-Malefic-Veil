package veil.gpu;

import veil.*;

/** Actual seed/GLB rooftop and ground shelter regression in desktop GPU CI. */
public final class GpuRoofExposureTest {
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError(why);
    }
    public static void main(String[] args)throws Exception{
        World world=new World(20261020L);
        UrbanScene scene=new UrbanScene(world);
        GpuRoofExposure cover=new GpuRoofExposure(scene);
        check(cover.roofCount()>0,"Real generated source rooftops absent");
        var roof=cover.pointNear(66,66);
        check(cover.covered(roof.x(),roof.y()-1,roof.z()),
            "Upward geometric ray missed source roof triangle");
        check(!cover.covered(roof.x(),roof.y()+60,roof.z()),
            "Above-roof surface incorrectly sheltered");
        int indoors=0,outdoors=0,tops=0;
        for(var instance:scene.instances()){
            if(instance.model().startsWith("roof-")||instance.model().equals("wall-b-roof")){
                check(cover.exposure(instance,scene.models().get(instance.model()))==1,
                    "Source roof must be sky exposed");
                tops++;
            }
            if(!instance.model().startsWith("road-"))continue;
            float exposure=cover.exposure(instance,scene.models().get(instance.model()));
            if(exposure==0)indoors++;
            else if(exposure==1)outdoors++;
            else throw new AssertionError("Exposure must be 0 or 1");
        }
        check(indoors>0&&outdoors>0&&tops>0,
            "Missing real sheltered/exposed source-ground pair "+indoors+"/"+outdoors);
        System.out.println("PASS GPU_ROOF_GEOMETRY_SOURCE roofs="+cover.roofCount()+
            " shelteredGround="+indoors+" openGround="+outdoors+" exposedRoofs="+tops);
    }
}
