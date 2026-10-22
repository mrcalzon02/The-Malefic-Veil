package veil;

import java.util.*;

/** End-to-end source & physics checks for city vegetation and derelict vehicles. */
public final class UrbanDressingTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        for(long seed:new long[]{20261020L,11L,93L}){
            World world=new World(seed);
            var dressing=UrbanDressing.generate(seed,world.city,world.spawns);
            check(dressing.equals(UrbanDressing.generate(seed,world.city,world.spawns)),
                "Seeded scenery must be stable");
            check(world.urbanProps.containsAll(dressing),"Scenery missing from authoritative world");
            int trees=0,trucks=0,cars=0;
            var ggbot=GGBotCarPack.available()?GGBotCarPack.required().names():java.util.Set.<String>of();
            var library=KenneyAssetPack.load().modelNames();
            for(World.UrbanProp p:dressing){
                check(library.contains(p.model()+".glb")||ggbot.contains(p.model()),
                    "Unlicensed/missing source "+p.model());
                check(p.solid()&&p.radius()>0,"Visible trunk/hulk lacks authoritative collision");
                check(world.city.tile((int)p.x(),(int)p.y())==0,"Blocked cell receives foliage or vehicle");
                for(double[] spawn:world.spawns)
                    check(Math.hypot(p.x()-spawn[0],p.y()-spawn[1])>=5,
                        "City spawn obstructed");
                if(p.model().startsWith("tree-"))trees++;
                if(p.model().startsWith("truck-"))trucks++;
                if(p.model().startsWith("ggbot-"))cars++;
            }
            check(trees>=3,"No city tree canopy");
            check(trucks>=2,"No parked Kenney trucks");
            if(!ggbot.isEmpty())check(cars>=1,"No GGBot civilian/wreck cars");
            check(world.canWalk(world.spawns[0][0],world.spawns[0][1]),
                "Player starting position lost");
            System.out.println("PASS UrbanDressingTest seed="+seed+" trees="+trees+
                " kenneyTrucks="+trucks+" ggbotCars="+cars);
        }
    }
}
