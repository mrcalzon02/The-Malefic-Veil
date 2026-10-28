package veil;

import java.nio.file.*;
import java.util.*;

/** Two genuine paid E5 factories can otherwise permanently starve a powered
 * conveyor on a 24-load grid. Deterministic five-second belt opportunities
 * must deliver physical stock while both factories continue to progress.
 * No phantom electricity, free warehouse space or duplicate paid receipts. */
public final class FactoryGridFairnessTest {
    private static void check(boolean yes,String reason){
        if(!yes)throw new AssertionError(reason);
    }
    private static World.Device add(ArenaSystems host,int kind,double x,double y,String owner){
        World.Device d=new World.Device(x,y,kind);
        d.ownerKey=owner;host.world.devices.add(d);return d;
    }
    private static void tick(ArenaSystems host,int seconds){
        for(int i=0;i<seconds;i++)host.advance(1000);
    }
    private static void clean(Path root)throws Exception{
        try(var walk=Files.walk(root)){
            for(Path p:walk.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);
        }
    }
    public static void main(String[] args)throws Exception{
        final String owner="acct_"+"e".repeat(64),alien="acct_"+"a".repeat(64);
        final String recipeId="precision-feedlines";
        var recipe=CraftingEraRegistry.process(recipeId);
        check(recipe!=null&&recipe.utilityDemand()==12&&
            ConveyorLogistics.SORTED_DEMAND==4&&ElectricUtility.CAPACITY==24,
            "Fairness fixture expects actual 24-load grid and 12-load E5 machines");
        ArenaSystems game=new ArenaSystems(20261020L);
        var operator=game.join(1);
        check(game.assignAccount(1,owner)&&
            game.enrollFaction(1,CharacterRoster.Faction.THE_UNRAVELED),
            "Grid owner account missing");
        double[] p=FactionFirstProductionTest.site(game);
        double x=p[0],y=p[1];
        add(game,ArenaSystems.CAMP_STAKE,x,y-3,owner);
        World.Device source=add(game,ArenaSystems.STORAGE_CRATE,x-3,y,owner);
        World.Device receiver=add(game,ArenaSystems.STORAGE_CRATE,x+3,y,owner);
        World.Device foreign=add(game,ArenaSystems.STORAGE_CRATE,x-2.7,y+.6,alien);
        World.Device belt=add(game,ArenaSystems.CONVEYOR_LINE,x,y,owner);
        add(game,ArenaSystems.AUTOMATIC_SORTER,x,y+.3,owner);
        World.Device first=add(game,ArenaSystems.INDUSTRIAL_WORKSHOP,x-1.1,y+1,owner);
        World.Device second=add(game,ArenaSystems.INDUSTRIAL_WORKSHOP,x+1.0,y+1,owner);
        World.Device third=add(game,ArenaSystems.INDUSTRIAL_WORKSHOP,x+1.7,y+1.7,owner);
        add(game,ArenaSystems.ELECTRIC_BUS,x,y+2.5,owner);
        World.Device power=add(game,ArenaSystems.GENERATOR,x+.5,y+3,owner);
        power.electricMs=ElectricUtility.MAX_CHARGE_MS;
        for(var entry:recipe.inputs().entrySet())
            check(source.storage.delivered(
                SurvivalCrafting.Item.valueOf(entry.getKey()),entry.getValue()*3),
                "Could not stage three real E5 recipe payments");
        check(source.storage.delivered(SurvivalCrafting.Item.MACHINE_PARTS,6),
            "Could not stage paid conveyor cargo");
        check(foreign.storage.delivered(SurvivalCrafting.Item.MACHINE_PARTS,7),
            "Could not stage foreign-owner isolation guard");
        operator.x=first.x;operator.y=first.y;
        check(game.startFactionBatch(1,recipeId),"First paid E5 batch failed");
        operator.x=second.x;operator.y=second.y;
        check(game.startFactionBatch(1,recipeId)&&
            first.productionActive&&second.productionActive,
            "Second independently paid factory failed to start");
        operator.x=third.x;operator.y=third.y;
        check(game.programNextAutomation(1,AutomationOrders.repeat(recipeId))&&
            third.queuedRecipeId.equals(AutomationOrders.repeat(recipeId)),
            "Could not queue third repeat-order factory on saturated grid");
        operator.x=x;operator.y=y;
        check(game.programConveyor(1,"MACHINE_PARTS")&&
            ConveyorLogistics.sorted(belt,game.world.devices),
            "Did not configure physical sorted cargo belt");
        tick(game,20);
        check(first.productionMs==15_000&&second.productionMs==15_000&&
            !third.productionActive&&third.productionMs==0,
            "Competing paid factories did not each receive fair 15-second service");
        check(third.factoryHold.equals("WAIT_CAPACITY")&&
            third.queuedRecipeId.equals(AutomationOrders.repeat(recipeId)),
            "Unpaid third factory launched without power or lost its repeat order");
        check(source.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==2&&
            receiver.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==4&&
            belt.routeMs==0,
            "Grid starvation prevented two complete physical sorted belt cycles");
        check(power.electricMs==ElectricUtility.MAX_CHARGE_MS-20_000L&&
            foreign.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==7,
            "Grid fairness minted energy or used foreign-owned transport stock");
        check(receiver.storage.delivered(SurvivalCrafting.Item.EDIBLES,124),
            "Could not fill real receiving crate for backpressure test");
        tick(game,10);
        check(source.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==2&&
            receiver.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==4&&
            belt.routeMs==0&&
            first.productionMs==25_000&&second.productionMs==25_000,
            "Full receiving warehouse consumed reserve or stalled paid factories");
        check(receiver.storage.consume(Map.of(SurvivalCrafting.Item.EDIBLES,124)),
            "Unable to free real output space");
        tick(game,5);
        check(source.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==0&&
            receiver.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==6&&
            first.productionMs==25_000&&second.productionMs==30_000,
            "Restored warehouse failed to restart paid transport/factory fair share");
        check(power.electricMs==ElectricUtility.MAX_CHARGE_MS-35_000L&&
            source.storage.count(SurvivalCrafting.Item.DRIVE_SHAFT)==1&&
            foreign.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==7,
            "Grid stopped paying for each host tick or double-debited recipe materials");
        Path root=Files.createTempDirectory("veil-e5-grid-fair-");
        try{
            HostCheckpoint.save(game,root);
            var loaded=new ArenaSystems(20261020L);
            check(HostCheckpoint.load(loaded,root),"Grid-fairness V11 restart failed");
            World.Device savedPower=loaded.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.GENERATOR).findFirst().orElseThrow();
            var machines=loaded.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.INDUSTRIAL_WORKSHOP).toList();
            check(machines.size()==3&&machines.get(0).productionMs==25_000&&
                machines.get(1).productionMs==30_000&&
                !machines.get(2).productionActive&&
                machines.get(2).queuedRecipeId.equals(AutomationOrders.repeat(recipeId))&&
                savedPower.electricMs==ElectricUtility.MAX_CHARGE_MS-35_000L,
                "Saved fairness interrupted paid progress or repeated a queued job");
            check(loaded.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE)
                .mapToInt(d->d.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)).sum()==13,
                "Crash/restart duplicated or lost physical owner/foreign material");
            tick(loaded,10);
            check(machines.get(0).productionMs>25_000&&
                machines.get(1).productionMs>30_000&&
                machines.get(2).queuedRecipeId.equals(AutomationOrders.repeat(recipeId)),
                "Restart stalled active competitors or lost unpaid third repeat");
        }finally{clean(root);}
        System.out.println("PASS FactoryGridFairnessTest: no belt/factory starvation, backpressure, power conservation and V11 recovery");
    }
}
