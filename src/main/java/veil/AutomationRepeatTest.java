package veil;

import java.nio.file.*;
import java.util.*;

/** Persisted unpaid repeat-mode E5 production, with two complete real
 * timed batches, owner denial, fuel/steam recharge, power disruption,
 * shortage backpressure, checkpoint recovery and explicit cancellation. */
public final class AutomationRepeatTest {
    private static void check(boolean yes,String why){
        if(!yes)throw new AssertionError(why);
    }
    private static World.Device fixture(ArenaSystems game,int kind,
            double x,double y,String owner){
        var d=new World.Device(x,y,kind);d.ownerKey=owner;
        game.world.devices.add(d);return d;
    }
    private static World.Device one(ArenaSystems game,int kind){
        return game.world.devices.stream().filter(d->d.kind==kind)
            .findFirst().orElseThrow();
    }
    private static void supplies(World.Device crate,Map<String,Integer> needs,int count){
        for(var entry:needs.entrySet())
            check(crate.storage.delivered(SurvivalCrafting.Item.valueOf(entry.getKey()),
                entry.getValue()*count),"Could not stage genuine repeated ingredient "+entry);
    }
    private static void tick(ArenaSystems game,int seconds){
        for(int n=0;n<seconds;n++){
            game.advance(1000);
            if(n%25!=0)continue;
            for(var actor:game.roster()){
                if(actor.dead)continue;
                var vitals=actor.survival.status();
                if(vitals.hunger()<45){
                    check(actor.survival.acquire(SurvivalCrafting.Item.EDIBLES,1,true),
                        "Worker lacked survival rations");
                    check(game.consumeSurvival(actor.id,SurvivalCrafting.Item.EDIBLES),
                        "Valid worker food rejected");
                }
                if(vitals.hydration()<45){
                    check(actor.survival.acquire(SurvivalCrafting.Item.CLEAN_WATER,1,true),
                        "Worker lacked potable water");
                    check(game.consumeSurvival(actor.id,SurvivalCrafting.Item.CLEAN_WATER),
                        "Valid worker drink rejected");
                }
            }
        }
    }
    private static void clean(Path root)throws Exception{
        try(var paths=Files.walk(root)){
            for(Path p:paths.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);
        }
    }
    public static void main(String[] args)throws Exception{
        final long seed=20261020L;
        final String owner="acct_"+"e".repeat(64);
        final String other="acct_"+"a".repeat(64);
        final String recipeId="precision-feedlines";
        final String repeat=AutomationOrders.repeat(recipeId);
        var spec=CraftingEraRegistry.process(recipeId);
        check(spec!=null&&spec.era()==CraftingEraRegistry.Era.AUTOMATION&&
            spec.utilityDemand()==12&&spec.quantity()==2,
            "Real canonical E5 process expected");
        ArenaSystems game=new ArenaSystems(seed);
        var operator=game.join(1);
        check(game.assignAccount(1,owner)&&
            game.enrollFaction(1,CharacterRoster.Faction.THE_UNRAVELED),
            "Authenticated factory owner required");
        double[] point=FactionFirstProductionTest.site(game);
        double x=point[0],y=point[1];
        fixture(game,ArenaSystems.CAMP_STAKE,x-2.4,y-2,owner);
        var stock=fixture(game,ArenaSystems.STORAGE_CRATE,x+1.1,y+.1,owner);
        var shop=fixture(game,ArenaSystems.INDUSTRIAL_WORKSHOP,x,y,owner);
        var bus=fixture(game,ArenaSystems.ELECTRIC_BUS,x+.2,y+2.5,owner);
        var generator=fixture(game,ArenaSystems.GENERATOR,x-1,y+3.1,owner);
        var boiler=fixture(game,ArenaSystems.STEAM_BOILER,x-1.7,y+3.8,owner);
        generator.electricMs=ElectricUtility.MAX_CHARGE_MS;
        boiler.steamMs=180_000;
        supplies(stock,spec.inputs(),2);
        var intruder=game.join(2);
        check(game.assignAccount(2,other)&&
            game.enrollFaction(2,CharacterRoster.Faction.THE_PENITENT),
            "Independent adversarial faction missing");
        operator.x=x+.35;operator.y=y;
        intruder.x=x;intruder.y=y;
        check(!game.programNextAutomation(2,repeat),
            "Foreign account programmed an owned E5 factory");
        check(!game.programNextAutomation(1,AutomationOrders.repeat("penitent-sanctify-shell"))&&
              !game.programNextAutomation(1,"REPEAT:")&&
              !game.programNextAutomation(1,"REPEAT:copper-wire"),
            "Repeat mode escaped recipe, era, or faction validation");
        check(game.programNextAutomation(1,repeat)&&
            shop.queuedRecipeId.equals(repeat),
            "Repeat instruction did not persist in durable one-slot receipt");
        check(!game.programNextAutomation(1,repeat)&&
            !game.programNextAutomation(1,recipeId),
            "A repeat instruction allowed a duplicate queue on the same machine");
        check(stock.storage.count(SurvivalCrafting.Item.DRIVE_SHAFT)==2,
            "Programming a repeat job pre-debited future input");
        tick(game,1);
        check(shop.productionActive&&shop.productionMs==0&&
            shop.queuedRecipeId.equals(repeat)&&
            stock.storage.count(SurvivalCrafting.Item.DRIVE_SHAFT)==1&&
            stock.storage.count(SurvivalCrafting.Item.CONDUCTOR_WIRE)==3,
            "First paid repeat launch did not atomically debit one batch");
        check(game.inspectFactory(1).next().equals("REPEAT_"+recipeId)&&
            game.inspectFactory(1).wire().split("\\|").length==8,
            "Inspection did not expose valid LAN-safe repeat mode");
        tick(game,100);
        Path root=Files.createTempDirectory("veil-e5-repeat-");
        try{
            HostCheckpoint.save(game,root);
            var restored=new ArenaSystems(seed);
            check(HostCheckpoint.load(restored,root),
                "V11 saved repeat work order could not be restored");
            var worker=restored.join(1);
            check(restored.assignAccount(1,owner),"Worker ownership lost at restart");
            worker.x=x+.35;worker.y=y;
            World.Device machine=one(restored,ArenaSystems.INDUSTRIAL_WORKSHOP);
            World.Device crate=one(restored,ArenaSystems.STORAGE_CRATE);
            World.Device power=one(restored,ArenaSystems.GENERATOR);
            World.Device grid=one(restored,ArenaSystems.ELECTRIC_BUS);
            check(machine.productionMs==100_000&&machine.productionActive&&
                machine.queuedRecipeId.equals(repeat)&&
                crate.storage.count(SurvivalCrafting.Item.DRIVE_SHAFT)==1,
                "Restore lost active paid progress or unpaid repeat instruction");
            tick(restored,spec.seconds()-100);
            check(machine.productionCompleted==1&&machine.productionActive&&
                machine.productionMs==0&&machine.queuedRecipeId.equals(repeat)&&
                crate.storage.count(SurvivalCrafting.Item.PRECISION_FEEDLINE)==2&&
                crate.storage.count(SurvivalCrafting.Item.DRIVE_SHAFT)==0&&
                crate.storage.count(SurvivalCrafting.Item.CONDUCTOR_WIRE)==0,
                "Completed first batch failed to automatically begin a separately paid second");
            grid.hp=0;
            tick(restored,5);
            check(machine.productionMs==0&&
                restored.inspectFactory(1).state().equals("WAIT_POWER"),
                "Unpowered repeating work advanced for free");
            grid.hp=100;
            tick(restored,ElectricUtility.MAX_CHARGE_MS>0?420:0);
            check(machine.productionMs==420_000&&power.electricMs==0,
                "Second cycle misallocated the remaining power reserve");
            tick(restored,1);
            check(machine.productionMs==420_000&&
                restored.inspectFactory(1).state().equals("WAIT_POWER"),
                "Depleted generator did not halt repeating factory");
            check(crate.storage.delivered(SurvivalCrafting.Item.CHARCOAL,3)&&
                crate.storage.delivered(SurvivalCrafting.Item.CLEAN_WATER,2),
                "Could not stage genuinely paid generator recharge");
            worker.x=power.x+.3;worker.y=power.y;
            check(restored.chargeGenerator(1),
                "Cannot recharge real E5 generator with consumed fuel and steam");
            check(crate.storage.count(SurvivalCrafting.Item.CHARCOAL)==0&&
                crate.storage.count(SurvivalCrafting.Item.CLEAN_WATER)==0,
                "Repeat test generator recharge skipped real material debit");
            worker.x=x+.35;worker.y=y;
            tick(restored,spec.seconds()-420);
            check(machine.productionCompleted==2&&!machine.productionActive&&
                machine.queuedRecipeId.equals(repeat)&&
                crate.storage.count(SurvivalCrafting.Item.PRECISION_FEEDLINE)==4&&
                restored.inspectFactory(1).state().equals("WAIT_INPUT"),
                "Unfunded third cycle generated output, lost its order, or ignored input shortage");
            tick(restored,12);
            check(machine.productionCompleted==2&&
                crate.storage.count(SurvivalCrafting.Item.PRECISION_FEEDLINE)==4,
                "Repeating shortage minted free resources across additional ticks");
            HostCheckpoint.save(restored,root);
            var waiting=new ArenaSystems(seed);
            check(HostCheckpoint.load(waiting,root),
                "Unpaid repeating shortage could not survive second checkpoint");
            var returning=waiting.join(1);
            check(waiting.assignAccount(1,owner),"Account lost repeating factory on reload");
            returning.x=x+.35;returning.y=y;
            check(waiting.inspectFactory(1).state().equals("WAIT_INPUT")&&
                waiting.inspectFactory(1).next().equals("REPEAT_"+recipeId),
                "Restart erased repeating shortage inspection");
            check(waiting.programNextAutomation(1,"NONE"),
                "Owner could not explicitly cancel repeating manufacture");
            check(one(waiting,ArenaSystems.INDUSTRIAL_WORKSHOP).queuedRecipeId.isEmpty(),
                "Cancellation left hidden auto-repeat permission");
            supplies(one(waiting,ArenaSystems.STORAGE_CRATE),spec.inputs(),1);
            tick(waiting,4);
            check(!one(waiting,ArenaSystems.INDUSTRIAL_WORKSHOP).productionActive&&
                one(waiting,ArenaSystems.INDUSTRIAL_WORKSHOP).productionCompleted==2,
                "Cancelled repeating recipe restarted using newly stocked inputs");
            HostCheckpoint.save(waiting,root);
            var finalRealm=new ArenaSystems(seed);
            check(HostCheckpoint.load(finalRealm,root)&&
                one(finalRealm,ArenaSystems.INDUSTRIAL_WORKSHOP).queuedRecipeId.isEmpty()&&
                one(finalRealm,ArenaSystems.INDUSTRIAL_WORKSHOP).productionCompleted==2,
                "Cancelled repeat resurrected after cold restart");
        }finally{clean(root);}
        System.out.println("PASS AutomationRepeatTest: two paid E5 cycles, live V11 repeat restore, power/fuel, shortage, cancellation and owner isolation");
    }
}
