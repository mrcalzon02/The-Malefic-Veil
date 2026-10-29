package veil;

import java.nio.file.*;
import java.util.*;

/** Honest camp building and field delivery with bounded physical inventory. */
public final class CampCargoUnloadTest {
    static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    static void clean(Path dir)throws Exception{
        try(var walk=Files.walk(dir))
            {for(Path p:walk.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);}
    }
    static void place(ArenaSystems game,ArenaSystems.Player p){
        check(p.survival.acquire(SurvivalCrafting.Item.TIMBER,2,false),"gather timber");
        check(p.survival.acquire(SurvivalCrafting.Item.FIBER,4,false),"gather fiber");
        check(game.craftHand(p.id,"tent"),"craft tent");
        for(int x=World.SIZE+4;x<World.SIZE+70;x+=2)
          for(int y=4;y<World.SIZE+32;y+=2){
            p.x=x+.5;p.y=y+.5;p.angle=0;
            var site=game.reviewBuild(p.id,ArenaSystems.CAMP_STAKE);
            if(!site.allowed())continue;
            for(double[] delta:new double[][]{{2.7,1.9},{2.7,-1.9},{3.0,1.0},{3.0,-1.0}}){
                double cx=p.x+delta[0],cy=p.y+delta[1];
                if(Math.hypot(cx-site.x(),cy-site.y())>=4.0)continue;
                if(!game.vehicles.restore(0,cx,cy,0,100,200))continue;
                check(game.build(p.id,ArenaSystems.CAMP_STAKE),"host build camp");
                return;
            }
          }
        throw new AssertionError("No physically legal wilderness camp and truck parking");
    }
    public static void main(String[] args)throws Exception{
        long seed=20261026L;
        String owner="acct_"+"a".repeat(64);
        ArenaSystems game=new ArenaSystems(seed);
        ArenaSystems.Player p=game.join(1);
        check(game.assignAccount(1,owner),"bind account");
        place(game,p);
        var camp=game.world.devices.get(game.world.devices.size()-1);
        check(!game.world.detailedCity(camp.x,camp.y),"city can't be cargo camp");
        check(!game.unloadTruck(1,0),"empty truck unloaded phantom stock");
        check(game.vehicles.loadCargo(0,"METAL_STOCK"),"loaded parked metal truck");
        double px=p.x,py=p.y;
        p.x=10.5;p.y=10.5;
        check(!game.unloadTruck(1,0),"remote city unload approved");
        check("METAL_STOCK".equals(game.vehicles.cargo(0).resourceType()),
            "failed unload consumed the real load");
        p.x=px;p.y=py;
        check(game.unloadTruck(1,0),"owned wilderness cargo refused");
        check(p.survival.count(SurvivalCrafting.Item.METAL_STOCK)==1&&
            game.vehicles.cargo(0).resourceType()==null,
            "cargo not atomically moved to authentic inventory");
        check(!game.unloadTruck(1,0),"duplicate unload succeeded");
        check(game.vehicles.loadCargo(0,"MACHINE_PARTS"),"second resource pickup");
        ArenaSystems.Player stranger=game.join(2);
        stranger.x=p.x;stranger.y=p.y;
        check(!game.unloadTruck(2,0),"foreign player used private camp");
        check(game.unloadTruck(1,0),"owner cannot unload machine parts");
        check(p.survival.count(SurvivalCrafting.Item.MACHINE_PARTS)==1,
            "metal and machine parts illegally merged");
        check(p.survival.craft("vehicle-repair",SurvivalCrafting.Station.WORKBENCH),
            "delivered ingredients cannot craft actual vehicle repair");
        check(p.survival.count(SurvivalCrafting.Item.VEHICLE_REPAIR_KIT)==1&&
            p.survival.count(SurvivalCrafting.Item.METAL_STOCK)==0&&
            p.survival.count(SurvivalCrafting.Item.MACHINE_PARTS)==0,
            "real recipe failed to debit cargo stock");
        check(game.vehicles.loadCargo(0,"METAL_STOCK"),"refill for save");
        check(game.unloadTruck(1,0),"repeat real delivery");
        Path dir=Files.createTempDirectory("veil-camp-delivery-");
        try{
            AccountDirectory.save(game,dir);
            DurableRealm.save(game,dir);
            ArenaSystems fresh=new ArenaSystems(seed);
            check(DurableRealm.load(fresh,dir),"restore physical camp");
            check(AccountDirectory.load(fresh,dir),"restore owned inventory");
            fresh.join(1);
            check(fresh.assignAccount(1,owner),"reconnect character");
            check(fresh.player(1).survival.count(SurvivalCrafting.Item.METAL_STOCK)==1,
                "delivered freight lost across authenticated restart");
            check(fresh.vehicles.cargo(0).resourceType()==null,
                "cargo duplicated when server restarted");
        }finally{clean(dir);}
        System.out.println("PASS CampCargoUnloadTest: real camp, bounded material delivery, no remote/unowned/replayed cargo, crafted output, persisted account and truck");
    }
}
