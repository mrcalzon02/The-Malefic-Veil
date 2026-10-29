package veil;

import java.nio.file.*;
import java.util.*;

/** Host-authoritative E5 diagnostics are observations of paid world state,
 * not client-side guessed causes or synthetic resource generation. */
public final class FactoryInspectionTest {
    private static void check(boolean ok,String reason){
        if(!ok)throw new AssertionError(reason);
    }
    private static World.Device device(ArenaSystems game,int kind,
            double x,double y,String owner){
        var d=new World.Device(x,y,kind);
        d.ownerKey=owner;game.world.devices.add(d);return d;
    }
    private static ArenaSystems.FactoryInspection inspect(ArenaSystems game,
            String expected){
        var status=game.inspectFactory(1);
        check(status.state().equals(expected),
            "Expected "+expected+" but reported "+status.state()+" on "+status.facility());
        return status;
    }
    public static void main(String[] args)throws Exception{
        final long seed=20261020L;
        final String owner="acct_"+"e".repeat(64);
        final String jobId="precision-feedlines";
        var job=CraftingEraRegistry.process(jobId);
        check(job!=null&&job.inputs().size()==2,"Expected real two-input E5 job");
        ArenaSystems game=new ArenaSystems(seed);
        var operator=game.join(1);
        check(game.assignAccount(1,owner)&&
            game.enrollFaction(1,CharacterRoster.Faction.THE_UNRAVELED),
            "Factory inspection requires genuine faction ownership");
        double[] parcel=FactionFirstProductionTest.site(game);
        double x=parcel[0],y=parcel[1];
        device(game,ArenaSystems.CAMP_STAKE,x,y-3,owner);
        var shop=device(game,ArenaSystems.INDUSTRIAL_WORKSHOP,x,y,owner);
        device(game,ArenaSystems.ELECTRIC_BUS,x+.2,y+2.5,owner);
        var generator=device(game,ArenaSystems.GENERATOR,x+1,y+3,owner);
        operator.x=x;operator.y=y;
        var guest=game.join(2);
        guest.x=x;guest.y=y;
        check(game.inspectFactory(2).state().equals("NO_OWN_WORKSITE"),
            "Remote/anonymous client received private machine information");
        inspect(game,"IDLE");
        check(game.programNextAutomation(1,jobId),"Queued paid E5 fixture missing");
        game.advance(1000);
        var dark=inspect(game,"WAIT_POWER");
        check(dark.recipe().equals(jobId)&&dark.next().equals(jobId)&&
            shop.queuedRecipeId.equals(jobId)&&!shop.productionActive,
            "Unpowered queued job lost its real receipt");
        generator.electricMs=1;
        inspect(game,"WAIT_POWER");
        game.advance(1000);
        check(shop.queuedRecipeId.equals(jobId)&&!shop.productionActive&&
            generator.electricMs==1,
            "Sub-tick electricity made a queued E5 machine falsely ready");
        generator.electricMs=ElectricUtility.MAX_CHARGE_MS;
        game.advance(1000);
        inspect(game,"WAIT_STORAGE");
        var crateA=device(game,ArenaSystems.STORAGE_CRATE,x+1,y+.1,owner);
        game.advance(1000);
        var missing=inspect(game,"WAIT_INPUT");
        check(missing.detail().contains("_")&&missing.detail().matches("[A-Z_]+_[1-9][0-9]*"),
            "Missing stock item/quantity not reported");
        var crateB=device(game,ArenaSystems.STORAGE_CRATE,x+2,y+.1,owner);
        check(crateA.storage.delivered(SurvivalCrafting.Item.DRIVE_SHAFT,1)&&
            crateB.storage.delivered(SurvivalCrafting.Item.CONDUCTOR_WIRE,3),
            "Could not stage real split input inventory");
        game.advance(1000);
        inspect(game,"WAIT_COMBINE");
        check(crateB.storage.consume(Map.of(SurvivalCrafting.Item.CONDUCTOR_WIRE,3))&&
            crateA.storage.delivered(SurvivalCrafting.Item.CONDUCTOR_WIRE,3),
            "Could not consolidate authentic crate stock");
        game.advance(1000);
        var active=inspect(game,"RUNNING");
        check(shop.productionActive&&shop.queuedRecipeId.isEmpty()&&
            active.elapsed()==0&&active.duration()==job.seconds()&&
            crateA.storage.count(SurvivalCrafting.Item.DRIVE_SHAFT)==0&&
            crateA.storage.count(SurvivalCrafting.Item.CONDUCTOR_WIRE)==0,
            "Launch failed to distinguish payment and actual operating progress");
        generator.electricMs=0;
        game.advance(1000);
        inspect(game,"WAIT_POWER");
        check(shop.productionMs==0,"Dark factory progressed for free");
        generator.electricMs=1;
        inspect(game,"WAIT_POWER");
        game.advance(1000);
        check(shop.productionMs==0&&generator.electricMs==1,
            "Sub-tick electricity incorrectly showed paid E5 work as running");
        // Exercise the actual 100ms dedicated server tick, not only test's
        // one-second steps: 99ms cannot fund one tick; exactly 100ms can.
        generator.electricMs=99;
        game.advance(100);
        inspect(game,"WAIT_POWER");
        check(shop.productionMs==0&&generator.electricMs==99,
            "An underfunded 100ms E5 host tick advanced production");
        generator.electricMs=100;
        game.advance(100);
        check(shop.productionMs==100&&generator.electricMs==0&&
            inspect(game,"WAIT_POWER").elapsed()==0,
            "Exactly one funded 100ms E5 tick failed or overstated charge");
        generator.electricMs=ElectricUtility.MAX_CHARGE_MS;
        check(game.programNextAutomation(1,jobId),
            "Cannot queue another job while a paid batch runs");
        check(inspect(game,"RUNNING").next().equals(jobId),
            "Active versus pending job is missing from host inspection");
        check(crateA.storage.delivered(SurvivalCrafting.Item.EDIBLES,128)&&
            crateB.storage.delivered(SurvivalCrafting.Item.EDIBLES,128),
            "Could not physically fill both output warehouses");
        // First order was genuinely paid; advance only the test's remaining
        // production progress to exercise exact output-capacity behavior.
        shop.productionMs=(long)job.seconds()*1000-1000;
        game.advance(1000);
        var full=inspect(game,"WAIT_OUTPUT");
        check(full.detail().equals(job.output()+"_"+job.quantity())&&
            shop.productionActive&&shop.productionCompleted==0&&
            shop.queuedRecipeId.equals(jobId),
            "Completed batch should remain paid and blocked by real warehouse capacity");
        Path root=Files.createTempDirectory("veil-factory-inspect-");
        try{
            HostCheckpoint.save(game,root);
            ArenaSystems restored=new ArenaSystems(seed);
            check(HostCheckpoint.load(restored,root),
                "V11 restored paid/output-blocked E5 network rejected");
            var resumed=restored.join(1);
            check(restored.assignAccount(1,owner),
                "Original operator lost ownership after checkpoint");
            resumed.x=x;resumed.y=y;
            var recovered=inspect(restored,"WAIT_OUTPUT");
            check(recovered.next().equals(jobId)&&recovered.elapsed()==job.seconds(),
                "Crash recovery lost running-vs-pending receipt or output status");
            var storage=restored.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&d.x<x+1.5)
                .findFirst().orElseThrow();
            check(storage.storage.consume(Map.of(SurvivalCrafting.Item.EDIBLES,128)),
                "Cannot free real capacity in recovered crate");
            restored.advance(1000);
            var next=inspect(restored,"WAIT_INPUT");
            check(next.completed()==1&&next.next().equals(jobId),
                "Paid output delivery must complete once while unpaid next order waits");
            var finalProduct=SurvivalCrafting.Item.valueOf(job.output());
            check(restored.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE)
                .mapToInt(d->d.storage.count(finalProduct)).sum()==job.quantity(),
                "Output recovery duplicated or discarded manufactured items");
            var stranger=restored.join(2);
            stranger.x=x;stranger.y=y;
            check(restored.inspectFactory(2).state().equals("NO_OWN_WORKSITE"),
                "Saved host revealed factory details to an unauthenticated visitor");
        }finally{
            try(var paths=Files.walk(root)){
                for(Path path:paths.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(path);
            }
        }
        System.out.println("PASS FactoryInspectionTest: ownership, power, exact shortage, split inputs, blocked output, and V11 restart");
    }
}
