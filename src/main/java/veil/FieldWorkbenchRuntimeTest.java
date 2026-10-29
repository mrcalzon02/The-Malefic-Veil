package veil;

import java.nio.file.*;
import java.util.*;

/**
 * Real host path: gather/craft kit -> protected-wilderness deployment ->
 * paid workstation production -> no replay -> durable device reload.
 */
public final class FieldWorkbenchRuntimeTest {
    private static void check(boolean condition,String reason){
        if(!condition)throw new AssertionError(reason);
    }
    public static void main(String[] args)throws Exception{
        long seed=20261020L;
        ArenaSystems host=new ArenaSystems(seed);
        ArenaSystems.Player maker=host.join(1);
        check(host.assignAccount(1,"acct_"+"b".repeat(64)),
            "Workbench owner must have an authenticated identity");
        check(maker.survival.acquire(SurvivalCrafting.Item.TIMBER,8,false)&&
            maker.survival.acquire(SurvivalCrafting.Item.FIBER,6,false)&&
            maker.survival.acquire(SurvivalCrafting.Item.FIELD_STONE,4,false),
            "Wilderness materials must be obtainable within inventory capacity");
        check(maker.survival.craft("tent",SurvivalCrafting.Station.HAND)&&
            maker.survival.craft("workbench",SurvivalCrafting.Station.HAND),
            "Early handmade workshop must come from actual survival recipes");
        check(maker.survival.count(SurvivalCrafting.Item.WORKBENCH)==1,
            "No workstation kit was minted");
        check(!host.craftWorkbench(1,"hand-woven-cloth"),
            "Remote craft without physical bench is not allowed");
        boolean campsite=false;
        for(int x=-144;x<=264&&!campsite;x+=5)for(int y=-144;y<=264&&!campsite;y+=5){
            if(host.world.detailedCity(x,y)||!host.world.canWalk(x,y))continue;
            maker.x=x;maker.y=y;maker.angle=0;
            if(host.reviewBuild(1,ArenaSystems.CAMP_STAKE).allowed()){
                campsite=host.build(1,ArenaSystems.CAMP_STAKE);
            }
        }
        check(campsite,"Could not establish physical wilderness camp");
        World.Device camp=host.world.devices.get(host.world.devices.size()-1);
        check(camp.kind==ArenaSystems.CAMP_STAKE,
            "First structure must be a genuine, host-owned camp");
        boolean placed=false;
        for(double distance=1.4;distance<=3.8&&!placed;distance+=.4){
            maker.x=camp.x+distance;maker.y=camp.y;maker.angle=0;
            var review=host.reviewBuild(1,ArenaSystems.FIELD_BENCH);
            if(review.allowed())placed=host.build(1,ArenaSystems.FIELD_BENCH);
        }
        check(placed,"Actual bench should place near an owned camp in wilderness");
        World.Device bench=host.world.devices.get(host.world.devices.size()-1);
        check(bench.kind==ArenaSystems.FIELD_BENCH&&bench.hp==100&&
            maker.survival.count(SurvivalCrafting.Item.WORKBENCH)==0,
            "World station must consume the crafted kit exactly once");
        check(!host.build(1,ArenaSystems.FIELD_BENCH),
            "A workbench kit cannot be reused for free additional station");
        check(maker.survival.acquire(SurvivalCrafting.Item.FIBER,8,false),
            "Cannot supply bounded real workbench inputs");
        check(host.craftWorkbench(1,"hand-woven-cloth"),
            "Allied nearby workbench must execute real paid recipe");
        check(maker.survival.count(SurvivalCrafting.Item.CLOTH_ROLL)==1&&
            maker.survival.count(SurvivalCrafting.Item.FIBER)==0,
            "Workbench production failed to debit input or minted duplicate items");
        check(!host.craftWorkbench(1,"hand-woven-cloth"),
            "Cooldown/consumption bypass duplicated crafted output");
        maker.x=bench.x+10;maker.y=bench.y+10;
        host.advance(1000);
        check(!host.craftWorkbench(1,"hand-woven-cloth"),
            "Remote workbench production permitted from ten metres away");
        Path dir=Files.createTempDirectory("malefic-real-bench-");
        try{
            DurableRealm.save(host,dir);
            ArenaSystems restored=new ArenaSystems(seed);
            check(DurableRealm.load(restored,dir),"Persistent realm did not reload");
            check(restored.world.devices.stream().anyMatch(d->
                d.kind==ArenaSystems.FIELD_BENCH&&d.hp==100&&
                d.ownerKey.equals(maker.accountKey)),
                "Workbench disappeared or lost ownership on real realm reload");
        }finally{
            try(var files=Files.walk(dir)){
                files.sorted(Comparator.reverseOrder()).forEach(p->{
                    try{Files.deleteIfExists(p);}catch(Exception ignored){}
                });
            }
        }
        System.out.println("PASS FieldWorkbenchRuntimeTest: physical owned station, exact "+
            "item debit, proximity, duplicate prevention, and durable save/load");
    }
}
