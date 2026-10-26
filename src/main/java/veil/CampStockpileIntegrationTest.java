package veil;

import java.nio.file.*;
import java.util.*;

/** Functional player journey: craft permanent crate, physically place it,
 * stock/unstock, truck freight, hostile misuse and durable realm/account readback. */
public final class CampStockpileIntegrationTest {
    static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    static void tick(ArenaSystems game,int seconds){
        for(int n=0;n<seconds;n++)game.advance(1000);
    }
    static void clean(Path root)throws Exception{
        try(var paths=Files.walk(root))
            {for(Path p:paths.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);}
    }
    static World.Device build(ArenaSystems game,ArenaSystems.Player p,int kind){
        if(kind==ArenaSystems.CAMP_STAKE){
            for(int x=-144;x<=264;x+=5)for(int y=-144;y<=264;y+=5){
                if(game.world.detailedCity(x,y)||!game.world.canWalk(x,y))continue;
                p.x=x;p.y=y;p.angle=0;
                if(game.reviewBuild(p.id,kind).allowed()&&game.build(p.id,kind))
                    return game.world.devices.get(game.world.devices.size()-1);
            }
        }else{
            World.Device camp=game.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.CAMP_STAKE).findFirst().orElseThrow();
            for(double d=1.4;d<=3.8;d+=.4){
                for(int side:new int[]{-1,1}){
                    p.x=camp.x+d*side;p.y=camp.y;p.angle=0;
                    if(game.reviewBuild(p.id,kind).allowed()&&game.build(p.id,kind))
                        return game.world.devices.get(game.world.devices.size()-1);
                }
            }
        }
        throw new AssertionError("Unable to build a legal "+kind+" on actual wilderness");
    }
    public static void main(String[] args)throws Exception{
        long seed=20261020L;
        String owner="acct_"+"d".repeat(64);
        ArenaSystems game=new ArenaSystems(seed);
        ArenaSystems.Player p=game.join(1);
        check(game.assignAccount(1,owner),"Authenticated owner missing");
        check(p.survival.acquire(SurvivalCrafting.Item.TIMBER,7,false)&&
            p.survival.acquire(SurvivalCrafting.Item.FIBER,6,false),
            "Primitive construction materials not obtainable");
        check(game.craftHand(1,"tent"),"Cannot make real camp");
        tick(game,5);
        check(game.craftHand(1,"crate"),"Cannot craft bounded crate kit");
        check(p.survival.count(SurvivalCrafting.Item.CRATE_KIT)==1,
            "Crate kit was not physically manufactured");
        World.Device camp=build(game,p,ArenaSystems.CAMP_STAKE);
        check(!game.world.detailedCity(camp.x,camp.y),"Camp in protected city");
        World.Device crate=build(game,p,ArenaSystems.STORAGE_CRATE);
        check(crate.hp>0&&crate.kind==ArenaSystems.STORAGE_CRATE&&
            owner.equals(crate.ownerKey),"Crate is not owned permanent world geometry");
        check(p.survival.count(SurvivalCrafting.Item.CRATE_KIT)==0,
            "Placement did not consume real crafted crate");
        check(!game.build(1,ArenaSystems.STORAGE_CRATE),
            "Rebuilding should not create free crate equipment");

        check(p.survival.acquire(SurvivalCrafting.Item.TIMBER,5,false),
            "Unable to gather supplies for crate");
        check(game.transferCampStock(1,false,SurvivalCrafting.Item.TIMBER,3),
            "Physical deposit into crate failed");
        check(crate.storage.count(SurvivalCrafting.Item.TIMBER)==3&&
            p.survival.count(SurvivalCrafting.Item.TIMBER)==2,
            "Crate copied items instead of debiting inventory");
        check(!game.transferCampStock(1,false,SurvivalCrafting.Item.TIMBER,3),
            "Unavailable carried stock duplicated");
        check(game.transferCampStock(1,true,SurvivalCrafting.Item.TIMBER,2),
            "Withdrawing at owned crate denied");
        check(crate.storage.count(SurvivalCrafting.Item.TIMBER)==1&&
            p.survival.count(SurvivalCrafting.Item.TIMBER)==4,
            "Stock withdrawal failed to debit or duplicated resources");
        double originalX=p.x,originalY=p.y;
        p.x=20.5;p.y=20.5;
        check(!game.transferCampStock(1,true,SurvivalCrafting.Item.TIMBER,1),
            "Player remotely withdrew base items from a city");
        p.x=originalX;p.y=originalY;
        ArenaSystems.Player foreign=game.join(2);
        foreign.x=p.x;foreign.y=p.y;
        check(!game.transferCampStock(2,true,SurvivalCrafting.Item.TIMBER,1),
            "Unowned guest bypassed settlement authorization");

        boolean parking=false;
        for(int dx=-3;dx<=3&&!parking;dx++)
            for(int dy=-3;dy<=3&&!parking;dy++){
                double tx=crate.x+dx,ty=crate.y+dy;
                double distance=Math.hypot(dx,dy);
                if(distance<1.8||distance>3.9||
                   Math.hypot(tx-p.x,ty-p.y)>4.5)continue;
                parking=game.vehicles.restore(0,tx,ty,0,100,200);
            }
        check(parking,"Cannot park physical truck within visible crate freight reach");
        check(game.vehicles.loadCargo(0,"METAL_STOCK"),"Cannot load physical freight");
        check(game.unloadTruck(1,0),"Owned camp failed real freight unloading");
        check(crate.storage.count(SurvivalCrafting.Item.METAL_STOCK)==1&&
            game.vehicles.cargo(0).resourceType()==null&&
            p.survival.count(SurvivalCrafting.Item.METAL_STOCK)==0,
            "Freight did not move truck to durable crate exactly once");
        check(!game.unloadTruck(1,0),"Empty truck replayed a delivered load");
        check(game.transferCampStock(1,true,SurvivalCrafting.Item.METAL_STOCK,1),
            "Legitimately delivered bulk stock cannot be retrieved for fabrication");
        check(p.survival.count(SurvivalCrafting.Item.METAL_STOCK)==1&&
            crate.storage.count(SurvivalCrafting.Item.METAL_STOCK)==0,
            "Bulk stock and backpack merged or duplicated");

        check(game.transferCampStock(1,false,SurvivalCrafting.Item.METAL_STOCK,1),
            "Carried bulk cannot be deposited back into crate");
        Path dir=Files.createTempDirectory("veil-real-camp-stock-");
        try{
            AccountDirectory.save(game,dir);
            DurableRealm.save(game,dir);
            ArenaSystems restored=new ArenaSystems(seed);
            check(DurableRealm.load(restored,dir),"Persisted crate realm cannot load");
            check(AccountDirectory.load(restored,dir),"Owner profile cannot reload");
            restored.join(1);
            check(restored.assignAccount(1,owner),"Owner identity lost after restart");
            World.Device saved=restored.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE).findFirst().orElseThrow();
            check(saved.storage.count(SurvivalCrafting.Item.METAL_STOCK)==1&&
                saved.storage.count(SurvivalCrafting.Item.TIMBER)==1,
                "Persistent crate lost legitimate item quantities");
            check(restored.player(1).survival.count(SurvivalCrafting.Item.METAL_STOCK)==0,
                "Save readback duplicated same physical freight in backpack");
            check(restored.vehicles.cargo(0).resourceType()==null,
                "Save readback recreated consumed truck cargo");
            check(restored.transferCampStock(1,true,SurvivalCrafting.Item.METAL_STOCK,1),
                "Saved crate cannot be used by original authenticated owner");
        }finally{clean(dir);}
        System.out.println("PASS CampStockpileIntegrationTest: built permanent crate, real resource debit/withdrawal, truck unload, owner permissions and V4 persistence");
    }
}
