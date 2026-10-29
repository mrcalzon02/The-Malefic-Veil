package veil;

import java.nio.file.*;
import java.util.*;

/** One real, paid, Unraveled-only Incursion facility. This verifies its host
 * construction and durable identity, NOT the missing 25-load power topology. */
public final class IncursionResilienceGridTest {
    private static final int GRID=ArenaSystems.UNRAVELED_RESILIENCE_GRID;
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError(why);
    }
    private static World.Device seed(ArenaSystems host,int kind,
            double x,double y,String owner){
        World.Device device=new World.Device(x,y,kind);
        device.ownerKey=owner;
        host.world.devices.add(device);
        return device;
    }
    private static boolean clear(ArenaSystems host,double x,double y){
        return !host.world.detailedCity(x,y)&&host.world.canWalk(x,y)&&
            !host.world.worldStructureAt(x,y,.55);
    }
    private static double[] site(ArenaSystems host){
        for(int x=-124;x<=224;x+=6)for(int y=-124;y<=224;y+=6){
            if(clear(host,x-2,y)&&clear(host,x+2,y)&&
               clear(host,x,y+2)&&clear(host,x,y-2)&&
               clear(host,x+2,y-2)&&clear(host,x,y))
                return new double[]{x,y};
        }
        throw new AssertionError("No legal wilderness construction site");
    }
    private static Map<SurvivalCrafting.Item,Integer> bill(){
        var spec=CraftingEraRegistry.facility("resilience-grid");
        check(spec!=null&&spec.era()==CraftingEraRegistry.Era.INCURSION&&
            spec.faction()==CraftingEraRegistry.Faction.UNRAVELED&&
            spec.requires().containsAll(List.of("assembly-line","signal-relay")),
            "Incursion blueprint and physical prerequisites missing");
        EnumMap<SurvivalCrafting.Item,Integer> items=
            new EnumMap<>(SurvivalCrafting.Item.class);
        for(var entry:spec.materials().entrySet())
            items.put(SurvivalCrafting.Item.valueOf(entry.getKey()),entry.getValue());
        return items;
    }
    private static void clean(Path dir)throws Exception{
        try(var walk=Files.walk(dir)){
            for(Path file:walk.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(file);
        }
    }
    public static void main(String[] args)throws Exception{
        String owner="acct_"+"a".repeat(64),foreign="acct_"+"b".repeat(64);
        long seed=20261028L;
        ArenaSystems host=new ArenaSystems(seed);
        ArenaSystems.Player p=host.join(1);
        check(host.assignAccount(1,owner)&&
            host.enrollFaction(1,CharacterRoster.Faction.THE_UNRAVELED),
            "Unraveled owner authorization failed");
        ArenaSystems.Player rival=host.join(2);
        check(host.assignAccount(2,foreign)&&
            host.enrollFaction(2,CharacterRoster.Faction.THE_PENITENT),
            "Foreign owner authorization failed");
        check(ArenaSystems.kindForFacility("resilience-grid")==GRID&&
            "resilience-grid".equals(ArenaSystems.factionFacility(GRID))&&
            "resilience-core".equals(ArenaSystems.firstProcess(GRID)),
            "Physical kind / recipe mapping is not bidirectional");
        var process=CraftingEraRegistry.process("resilience-core");
        check(process!=null&&process.facility().equals("resilience-grid")&&
            process.utilityDemand()==25&&ElectricUtility.CAPACITY==24,
            "Do not silently reduce Incursion's genuine power requirement");
        Map<SurvivalCrafting.Item,Integer> cost=bill();
        double[] at=site(host);double x=at[0],y=at[1];
        seed(host,ArenaSystems.CAMP_STAKE,x-2,y,owner);
        World.Device assembly=seed(host,ArenaSystems.ASSEMBLY_LINE,x+2,y,owner);
        World.Device signal=seed(host,ArenaSystems.SIGNAL_RELAY,x,y+2,owner);
        World.Device own=seed(host,ArenaSystems.STORAGE_CRATE,x,y-2,owner);
        World.Device alien=seed(host,ArenaSystems.STORAGE_CRATE,x+2,y-2,foreign);
        for(var item:cost.entrySet()){
            int amount=item.getValue();
            if(item.getKey()==SurvivalCrafting.Item.FIELD_STONE)amount--;
            check(own.storage.delivered(item.getKey(),amount),
                "Cannot stage owned authentic material "+item.getKey());
        }
        check(alien.storage.delivered(SurvivalCrafting.Item.FIELD_STONE,1),
            "Cannot stage foreign stock isolation check");
        // Find a real legal footprint with the same camp, two installed
        // prerequisites, and one funded warehouse in range.
        boolean found=false;
        for(double dx=-2.8;dx<=2.8&&!found;dx+=1.4)
            for(double dy=-2.8;dy<=2.8;dy+=1.4){
                p.x=x+dx-1.1;p.y=y+dy;p.angle=0;
                var review=host.reviewBuild(1,GRID);
                if(review.reason().equals("NEEDS_STOCKED_OWN_CRATE")){
                    found=true;break;
                }
            }
        check(found,"No legal grid site with only a one-stone owned shortage");
        rival.x=p.x;rival.y=p.y;rival.angle=p.angle;
        check(host.reviewBuild(2,GRID).reason().equals("WRONG_FACTION_FACILITY")&&
            !host.build(2,GRID),"Penitent player built Unraveled grid");
        Map<SurvivalCrafting.Item,Integer> before=own.storage.snapshot();
        check(!host.build(1,GRID)&&own.storage.snapshot().equals(before)&&
            alien.storage.count(SurvivalCrafting.Item.FIELD_STONE)==1,
            "Foreign materials paid the grid or refusal partially debited");
        signal.ownerKey=foreign;
        check(host.reviewBuild(1,GRID).reason().equals("MISSING_PHYSICAL_PREREQUISITE"),
            "Foreign-owned relay counted as a real prerequisite");
        signal.ownerKey=owner;
        signal.hp=0;
        check(host.reviewBuild(1,GRID).reason().equals("MISSING_PHYSICAL_PREREQUISITE"),
            "Destroyed relay counted as a real prerequisite");
        signal.hp=100;
        check(own.storage.delivered(SurvivalCrafting.Item.FIELD_STONE,1),
            "Cannot repair the authentic one-stone shortage");
        int prior=host.world.devices.size();
        var outcome=host.buildOutcome(1,GRID);
        check(outcome.built()&&outcome.deviceIndex()==prior&&
            host.world.devices.size()==prior+1,
            "Paid Incursion grid did not create exactly one host device");
        World.Device grid=host.world.devices.get(prior);
        check(grid.kind==GRID&&owner.equals(grid.ownerKey)&&grid.hp==100&&
            own.storage.snapshot().isEmpty()&&
            alien.storage.count(SurvivalCrafting.Item.FIELD_STONE)==1,
            "Grid identity, payment, or owner isolation corrupted");
        check(!host.build(1,GRID)&&host.world.devices.size()==prior+1,
            "Unraveled grid was duplicated without a second payment");
        // A 25-load factory cannot start on a 24-load generator. Even a
        // fully stocked warehouse must remain untouched until real power exists.
        for(var input:process.inputs().entrySet())
            check(own.storage.delivered(
                SurvivalCrafting.Item.valueOf(input.getKey()),input.getValue()),
                "Cannot stage real resilience-core recipe inputs");
        Map<SurvivalCrafting.Item,Integer> staged=own.storage.snapshot();
        p.x=grid.x;p.y=grid.y;
        check(!host.startFactionBatch(1,"resilience-core")&&
            !grid.productionActive&&own.storage.snapshot().equals(staged),
            "Unpowered 25-load grid consumed stock or fabricated production");
        Path root=Files.createTempDirectory("veil-resilience-grid-");
        try{
            HostCheckpoint.save(host,root);
            ArenaSystems restarted=new ArenaSystems(seed);
            check(HostCheckpoint.load(restarted,root),
                "Paid grid did not survive durable host checkpoint");
            check(restarted.world.devices.stream().filter(d->
                d.kind==GRID&&d.hp>0&&owner.equals(d.ownerKey)).count()==1,
                "Restart lost or duplicated the distinct Incursion device");
            check(restarted.world.devices.stream().anyMatch(d->
                d.kind==ArenaSystems.STORAGE_CRATE&&owner.equals(d.ownerKey)&&
                d.storage.snapshot().equals(staged)),
                "Restart lost unspent production inputs");
            check(restarted.world.devices.stream().anyMatch(d->
                d.kind==ArenaSystems.STORAGE_CRATE&&foreign.equals(d.ownerKey)&&
                d.storage.count(SurvivalCrafting.Item.FIELD_STONE)==1),
                "Restart transferred foreign stock to Unraveled owner");
        }finally{clean(root);}
        System.out.println("PASS IncursionResilienceGridTest: paid faction-only grid, distinct kind, host restart, no free 25-load power");
    }
}
