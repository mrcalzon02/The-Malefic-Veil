package veil;

import java.util.*;

/** Verify that light sources are real Kenney fixtures, never invented emissive cubes. */
public final class UrbanLightingTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        World world=new World(20261020L);
        UrbanLighting lighting=new UrbanLighting(world);
        check(lighting.lights().size()>12,"Insufficient real streetlight coverage");
        int placed=0;
        for(World.UrbanProp prop:world.urbanProps)
            if(prop.model().equals("detail-light-single")||
               prop.model().equals("detail-light-double")||
               prop.model().equals("detail-light-traffic"))placed++;
        check(placed==lighting.lights().size(),"Not all streetlight models emit light");
        for(UrbanLighting.Light light:lighting.lights()){
            check(light.radius()>4&&light.radius()<20,"Invalid radius");
            check(light.intensity()>0,"Invalid light strength");
            check(world.urbanProps.stream().anyMatch(p->
                p.x()==light.x()&&p.y()==light.z()&&p.model().equals(light.meshId())),
                "Ghost light with no model");
        }
        for(int x=1;x<World.SIZE;x+=7)for(int y=1;y<World.SIZE;y+=7){
            var near=lighting.nearby(x,y,8);
            check(near.size()<=8,"GPU light budget overflow");
            for(int i=1;i<near.size();i++){
                var a=near.get(i-1);var b=near.get(i);
                double da=Math.hypot(a.x()-x,a.z()-y);
                double db=Math.hypot(b.x()-x,b.z()-y);
                check(da<=db+.000001,"Lights not nearest-first");
            }
            check(near.equals(lighting.nearby(x,y,8)),"Nonrepeatable light selection");
        }
        check(lighting.nearby(1,1,0).isEmpty(),"Zero light budget ignored");
        try{lighting.nearby(1,1,100);throw new AssertionError("Unbounded lights");}
        catch(IllegalArgumentException expected){}
        System.out.println("PASS UrbanLightingTest: "+placed+" Kenney fixture lights, bounded nearest GPU uniforms");
    }
}