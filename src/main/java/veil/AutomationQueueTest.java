package veil;

import java.nio.file.*;
import java.util.*;

/** Actual V10 queued E5 fabrication, not a recipe-index simulation.
 * Host authorization, stock-on-start, power pauses, repeated cold restarts,
 * one-slot idempotency and exactly-once output at a physical settlement. */
public final class AutomationQueueTest {
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError(why);
    }
    private static World.Device fixture(ArenaSystems host,int kind,double x,double y,String owner){
        World.Device site=new World.Device(x,y,kind);
        site.ownerKey=owner;host.world.devices.add(site);return site;
    }
    private static World.Device find(ArenaSystems host,int kind){
        return host.world.devices.stream().filter(d->d.kind==kind)
            .findFirst().orElseThrow();
    }
    private static void supplies(CampStockpile warehouse,Map<String,Integer> need){
        for(var entry:need.entrySet())
            check(warehouse.delivered(SurvivalCrafting.Item.valueOf(entry.getKey()),entry.getValue()),
                "Missing authentic fixture material "+entry);
    }
    private static void tick(ArenaSystems host,int seconds){
        for(int n=0;n<seconds;n++){
            host.advance(1000);
            if(n%25==0)for(var player:host.roster()){
                if(player.dead)continue;
                var status=player.survival.status();
                if(status.hunger()<45){
                    check(player.survival.acquire(SurvivalCrafting.Item.EDIBLES,1,true),
                        "Operator food fixture failed");
                    check(host.consumeSurvival(player.id,SurvivalCrafting.Item.EDIBLES),
                        "Food refused during long E5 batch");
                }
                if(status.hydration()<45){
                    check(player.survival.acquire(SurvivalCrafting.Item.CLEAN_WATER,1,true),
                        "Operator hydration fixture failed");
                    check(host.consumeSurvival(player.id,SurvivalCrafting.Item.CLEAN_WATER),
                        "Water refused during long E5 batch");
                }
            }
        }
    }
    private static void clean(Path dir)throws Exception{
        try(var paths=Files.walk(dir)){
            for(Path p:paths.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);
        }
    }
    public static void main(String[] args)throws Exception{
        final long seed=20261020L;
        String owner="acct_"+"e".repeat(64);
        String recipeId="precision-feedlines";
        var job=CraftingEraRegistry.process(recipeId);
        check(job!=null&&job.era()==CraftingEraRegistry.Era.AUTOMATION&&
            job.facility().equals("industrial-workshop"),
            "Canonical E5 workshop job not found");
        ArenaSystems host=new ArenaSystems(seed);
        var player=host.join(1);
        check(host.assignAccount(1,owner)&&
            host.enrollFaction(1,CharacterRoster.Faction.THE_UNRAVELED),
            "Authenticated factory owner required");
        double[] point=FactionFirstProductionTest.site(host);
        double x=point[0],y=point[1];
        fixture(host,ArenaSystems.CAMP_STAKE,x-2.4,y-2,owner);
        var warehouse=fixture(host,ArenaSystems.STORAGE_CRATE,x+1.1,y+.1,owner);
        var workshop=fixture(host,ArenaSystems.INDUSTRIAL_WORKSHOP,x,y,owner);
        fixture(host,ArenaSystems.ELECTRIC_BUS,x+.2,y+2.5,owner);
        var generator=fixture(host,ArenaSystems.GENERATOR,x-1,y+3.1,owner);
        var boiler=fixture(host,ArenaSystems.STEAM_BOILER,x-1.7,y+3.8,owner);
        generator.electricMs=ElectricUtility.MAX_CHARGE_MS;
        boiler.steamMs=180_000;
        var stranger=host.join(2);
        check(host.assignAccount(2,"acct_"+"a".repeat(64))&&
            host.enrollFaction(2,CharacterRoster.Faction.THE_PENITENT),
            "Adversary needs independent authentic faction");
        stranger.x=x;stranger.y=y;
        check(!host.programNextAutomation(2,recipeId),
            "Other faction queued on private machine");
        check(!host.programNextAutomation(1,"penitent-sanctify-shell"),
            "Unraveled operator queued a Penitent-only recipe");
        player.x=x+.35;player.y=y;
        check(!host.programNextAutomation(1,"copper-wire"),
            "Queue accepted non-E5 recipe");
        check(host.programNextAutomation(1,recipeId),
            "Idle owned E5 machine did not accept one queued recipe");
        check(workshop.queuedRecipeId.equals(recipeId)&&
            warehouse.storage.snapshot().isEmpty(),
            "Pending recipe incorrectly debited or created stock");
        tick(host,5);
        check(!workshop.productionActive&&workshop.queuedRecipeId.equals(recipeId),
            "Without ingredients, scheduler must remain pending");
        check(host.programNextAutomation(1,"NONE"),"Could not cancel empty reservation");
        check(workshop.queuedRecipeId.isEmpty(),
            "Cancellation left a phantom pending job");
        supplies(warehouse.storage,job.inputs());
        check(host.startFactionBatch(1,recipeId),"Live first production refused real input");
        check(host.programNextAutomation(1,recipeId),
            "Next E5 order could not be queued while the first is active");
        check(!host.programNextAutomation(1,recipeId),
            "Duplicate pending receipt accepted");
        check(!host.programNextAutomation(2,"NONE"),
            "Different faction cancelled private queue");
        for(var material:job.inputs().entrySet())
            check(warehouse.storage.count(SurvivalCrafting.Item.valueOf(material.getKey()))==0,
                "First job did not debit exactly its real ingredients");
        tick(host,100);
        check(workshop.productionMs==100_000&&
            workshop.queuedRecipeId.equals(recipeId)&&
            generator.electricMs==ElectricUtility.MAX_CHARGE_MS-100_000,
            "First E5 job did not consume paid electricity while queuing next");

        Path root=Files.createTempDirectory("veil-queued-e5-");
        try{
            HostCheckpoint.save(host,root);
            ArenaSystems restored=new ArenaSystems(seed);
            check(HostCheckpoint.load(restored,root),"Active+queued V10 checkpoint unreadable");
            restored.join(1);
            check(restored.assignAccount(1,owner),"Account no longer owns queued machine");
            World.Device live=find(restored,ArenaSystems.INDUSTRIAL_WORKSHOP);
            World.Device stock=find(restored,ArenaSystems.STORAGE_CRATE);
            World.Device grid=find(restored,ArenaSystems.ELECTRIC_BUS);
            World.Device power=find(restored,ArenaSystems.GENERATOR);
            check(live.productionActive&&live.productionMs==100_000&&
                live.queuedRecipeId.equals(recipeId)&&
                live.productionRecipeId.equals(recipeId),
                "V10 restart split paid active and unpaid next work order");
            grid.hp=0;
            tick(restored,10);
            check(live.productionMs==100_000&&
                live.queuedRecipeId.equals(recipeId),
                "Grid failure advanced or lost queued work");
            grid.hp=100;
            tick(restored,job.seconds()-100);
            var item=SurvivalCrafting.Item.valueOf(job.output());
            check(!live.productionActive&&live.productionCompleted==1&&
                live.queuedRecipeId.equals(recipeId)&&
                stock.storage.count(item)==job.quantity(),
                "First product was not delivered once while second remained unpaid");
            HostCheckpoint.save(restored,root);
            ArenaSystems second=new ArenaSystems(seed);
            check(HostCheckpoint.load(second,root),
                "Waiting V10 pending receipt failed second checkpoint readback");
            second.join(1);
            check(second.assignAccount(1,owner),"Factory membership lost on reload");
            var waiting=find(second,ArenaSystems.INDUSTRIAL_WORKSHOP);
            var raw=find(second,ArenaSystems.STORAGE_CRATE);
            var battery=find(second,ArenaSystems.GENERATOR);
            check(waiting.queuedRecipeId.equals(recipeId)&&
                waiting.productionCompleted==1&&!waiting.productionActive,
                "Unpaid second job duplicated work at restart");
            tick(second,8);
            check(!waiting.productionActive&&raw.storage.count(item)==job.quantity(),
                "Missing material queue minted a second item");
            supplies(raw.storage,job.inputs());
            tick(second,1);
            check(waiting.productionActive&&waiting.queuedRecipeId.isEmpty()&&
                waiting.productionRecipeId.equals(recipeId),
                "Queue did not consume actual supplied stock and atomically start");
            for(var material:job.inputs().entrySet())
                check(raw.storage.count(SurvivalCrafting.Item.valueOf(material.getKey()))==0,
                    "Queued launch did not debit reserved input once");
            tick(second,410);
            check(battery.electricMs<20_000&&waiting.productionMs<job.seconds()*1000L,
                "Second job should require additional powered operating time");
            var actor=second.player(1);
            supplies(raw.storage,Map.of("CHARCOAL",3,"CLEAN_WATER",2));
            actor.x=battery.x+.3;actor.y=battery.y;
            check(second.chargeGenerator(1),"Cannot pay electric recharge from actual crate");
            actor.x=waiting.x+.3;actor.y=waiting.y;
            tick(second,job.seconds()-waiting.productionMs/1000L>0?
                (int)(job.seconds()-waiting.productionMs/1000L):0);
            check(!waiting.productionActive&&waiting.productionCompleted==2&&
                raw.storage.count(item)==job.quantity()*2,
                "Two paid E5 batches must deliver exact twice-produced output");
            tick(second,8);
            check(raw.storage.count(item)==job.quantity()*2,
                "Finished queue replayed manufactured stock");
            HostCheckpoint.save(second,root);
            ArenaSystems finalRealm=new ArenaSystems(seed);
            check(HostCheckpoint.load(finalRealm,root),
                "Completed V10 receipt failed recovery");
            var complete=find(finalRealm,ArenaSystems.INDUSTRIAL_WORKSHOP);
            check(complete.queuedRecipeId.isEmpty()&&!complete.productionActive&&
                complete.productionCompleted==2,
                "Completed queue falsely restored as waiting or active");
            check(find(finalRealm,ArenaSystems.STORAGE_CRATE).storage.count(item)==
                job.quantity()*2,"Restart duplicated or deleted manufactured output");
            System.out.println("PASS AutomationQueueTest: two paid batches, one pending, three V10 checkpoints, no duplicated inputs");
        }finally{clean(root);}
    }
}
