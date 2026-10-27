package veil;

import java.nio.file.*;
import java.util.*;

/**
 * A real E5 industrial grid: one generator has 24 load units. A powered
 * sorter/conveyor needs four; two 12-load factories must not both reserve
 * inputs until conveyor demand is removed. Save/restart preserves the ledger.
 */
public final class AutomationPowerCompetitionTest {
    private static void check(boolean ok,String reason){
        if(!ok)throw new AssertionError(reason);
    }
    private static World.Device fixture(ArenaSystems host,int kind,
                                        double x,double y,String owner){
        World.Device device=new World.Device(x,y,kind);
        device.ownerKey=owner;
        host.world.devices.add(device);
        return device;
    }
    private static void tick(ArenaSystems host,int n){
        for(int i=0;i<n;i++)host.advance(1000);
    }
    public static void main(String[] args)throws Exception{
        final long seed=20261020L;
        final String owner="acct_"+"e".repeat(64);
        final String recipeId="precision-feedlines";
        var job=CraftingEraRegistry.process(recipeId);
        check(job!=null&&job.utilityDemand()==12&&
            job.era()==CraftingEraRegistry.Era.AUTOMATION,
            "Expected real E5 12-load workshop recipe");
        ArenaSystems host=new ArenaSystems(seed);
        var operator=host.join(1);
        check(host.assignAccount(1,owner)&&
            host.enrollFaction(1,CharacterRoster.Faction.THE_UNRAVELED),
            "Authenticated factory owner required");
        double[] point=FactionFirstProductionTest.site(host);
        double x=point[0],y=point[1];
        fixture(host,ArenaSystems.CAMP_STAKE,x,y-3,owner);
        World.Device source=fixture(host,ArenaSystems.STORAGE_CRATE,x-3,y,owner);
        World.Device destination=fixture(host,ArenaSystems.STORAGE_CRATE,x+3,y,owner);
        World.Device belt=fixture(host,ArenaSystems.CONVEYOR_LINE,x,y,owner);
        fixture(host,ArenaSystems.AUTOMATIC_SORTER,x,y+.4,owner);
        World.Device first=fixture(host,ArenaSystems.INDUSTRIAL_WORKSHOP,x-1,y+1.3,owner);
        World.Device second=fixture(host,ArenaSystems.INDUSTRIAL_WORKSHOP,x+1,y+1.3,owner);
        fixture(host,ArenaSystems.ELECTRIC_BUS,x,y+2.5,owner);
        World.Device generator=fixture(host,ArenaSystems.GENERATOR,x+1,y+3.2,owner);
        generator.electricMs=ElectricUtility.MAX_CHARGE_MS;
        for(var input:job.inputs().entrySet()){
            check(source.storage.delivered(SurvivalCrafting.Item.valueOf(input.getKey()),
                input.getValue()*2),"Unable to stage two real E5 payments");
        }
        check(source.storage.delivered(SurvivalCrafting.Item.MACHINE_PARTS,3),
            "Unable to stage conveyor cargo");
        operator.x=x;operator.y=y+1;
        check(host.programConveyor(1,"MACHINE_PARTS")&&
            ConveyorLogistics.sorted(belt,host.world.devices),
            "Sorted route not physically configured");
        check(host.programNextAutomation(1,recipeId)&&
            host.programNextAutomation(1,recipeId),
            "Could not configure both separately owned permanent factories");
        check(!host.programNextAutomation(1,recipeId),
            "Overbooked a third work order in two one-slot machines");
        tick(host,1);
        operator.x=second.x;operator.y=second.y;
        check(host.inspectFactory(1).state().equals("WAIT_CAPACITY"),
            "Grid competition must show actionable E5 capacity stall");
        operator.x=x;operator.y=y+1;
        check(first.productionActive&&!second.productionActive&&
            second.queuedRecipeId.equals(recipeId),
            "Conveyor load plus first machine did not block second start");
        check(first.productionMs==0&&generator.electricMs==
            ElectricUtility.MAX_CHARGE_MS-1000,
            "Launch charged phantom processing or failed to charge actual conveyor");
        for(var input:job.inputs().entrySet())
            check(source.storage.count(SurvivalCrafting.Item.valueOf(input.getKey()))==
                input.getValue(),"Queued second job spent its unpaid inputs");
        tick(host,4);
        check(source.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==1&&
            destination.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==2&&
            second.queuedRecipeId.equals(recipeId)&&!second.productionActive,
            "Sorted cargo routing or capacity-aware factory waiting failed");
        check(host.programConveyor(1,"NONE"),"Owner could not stop conveyor");
        tick(host,1);
        check(first.productionActive&&second.productionActive&&
            first.productionMs==5000&&second.productionMs==0&&
            second.queuedRecipeId.isEmpty(),
            "Freed grid capacity did not launch waiting paid E5 factory");
        for(var input:job.inputs().entrySet())
            check(source.storage.count(SurvivalCrafting.Item.valueOf(input.getKey()))==0,
                "Second E5 factory failed exactly-once material debit");
        tick(host,3);
        check(first.productionMs==8000&&second.productionMs==3000,
            "Two concurrent 12-load E5 batches failed to share a 24-load grid");
        Path root=Files.createTempDirectory("veil-e5-grid-load-");
        try{
            HostCheckpoint.save(host,root);
            ArenaSystems restored=new ArenaSystems(seed);
            check(HostCheckpoint.load(restored,root),
                "Power/transport factory network failed crash-safe recovery");
            long active=restored.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.INDUSTRIAL_WORKSHOP&&d.productionActive)
                .count();
            check(active==2&&restored.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.INDUSTRIAL_WORKSHOP)
                .allMatch(d->d.queuedRecipeId.isEmpty()),
                "Restart duplicated or erased running versus queued work");
            int remainingCargo=restored.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE)
                .mapToInt(d->d.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)).sum();
            check(remainingCargo==3,"Conveyor duplicated/destroyed saved physical cargo");
            tick(restored,2);
            check(restored.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.INDUSTRIAL_WORKSHOP)
                .allMatch(d->d.productionMs>0),
                "Both recovered factories did not resume on real grid power");
        }finally{
            try(var walk=Files.walk(root)){
                for(Path p:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
        System.out.println("PASS AutomationPowerCompetitionTest: factories and sorted conveyor share one 24-load grid; exact receipts survive V10 restart");
    }
}
