package veil;

import java.nio.file.*;
import java.util.*;

/**
 * Host acceptance for a 25-load Incursion work order: two independently paid,
 * fueled generators, one owned bus, exactly-once materials/output and durable
 * restart. Earlier IncursionResilienceGridTest proves the grid's own payment.
 */
public final class IncursionDualGeneratorTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private static World.Device fixture(ArenaSystems host,int kind,
            double x,double y,String owner){
        World.Device device=new World.Device(x,y,kind);
        device.ownerKey=owner;
        host.world.devices.add(device);
        return device;
    }
    private static void supply(World.Device crate,Map<String,Integer> bill){
        for(var item:bill.entrySet())
            check(crate.storage.delivered(SurvivalCrafting.Item.valueOf(item.getKey()),
                item.getValue()),"Cannot stage paid "+item.getKey());
    }
    private static World.Device paid(ArenaSystems host,ArenaSystems.Player owner,
            World.Device crate,int kind,double x,double y){
        var spec=CraftingEraRegistry.facility(ArenaSystems.factionFacility(kind));
        check(spec!=null,"Missing real build blueprint for "+kind);
        supply(crate,spec.materials());
        Map<SurvivalCrafting.Item,Integer> before=crate.storage.snapshot();
        for(double dx=-1.75;dx<=1.75;dx+=.35)
            for(double dy=-1.75;dy<=1.75;dy+=.35){
                owner.x=x+dx-1.1;owner.y=y+dy;owner.angle=0;
                if(!host.reviewBuild(owner.id,kind).allowed())continue;
                var result=host.buildOutcome(owner.id,kind);
                check(result.built(),"Accepted construction was not committed");
                World.Device built=host.world.devices.get(result.deviceIndex());
                check(built.kind==kind&&owner.accountKey.equals(built.ownerKey),
                    "Paid structure lost owner or identity");
                for(var item:spec.materials().entrySet()){
                    var key=SurvivalCrafting.Item.valueOf(item.getKey());
                    check(crate.storage.count(key)==before.getOrDefault(key,0)-item.getValue(),
                        "Construction did not debit the exact real "+key+" bill");
                }
                return built;
            }
        throw new AssertionError("No legal funded physical "+spec.id()+" placement");
    }
    private static void tick(ArenaSystems host,int seconds){
        for(int i=0;i<seconds;i++)host.advance(1000);
    }
    private static void fuel(ArenaSystems host,ArenaSystems.Player owner,
            World.Device generator,World.Device crate){
        supply(crate,Map.of("CHARCOAL",3,"CLEAN_WATER",2));
        long before=generator.electricMs;
        owner.x=generator.x+.3;owner.y=generator.y;
        check(host.chargeGenerator(owner.id),"Paid generator charge rejected");
        check(generator.electricMs==Math.min(ElectricUtility.MAX_CHARGE_MS,
                before+ElectricUtility.MAX_CHARGE_MS),
            "Generator gained incorrect charged reserve");
        check(crate.storage.count(SurvivalCrafting.Item.CHARCOAL)==0&&
            crate.storage.count(SurvivalCrafting.Item.CLEAN_WATER)==0,
            "Generator minted charge without consuming stored fuel");
    }
    private static double[] site(ArenaSystems host){
        for(int x=-124;x<=224;x+=6)for(int y=-124;y<=224;y+=6){
            if(host.world.detailedCity(x,y))continue;
            boolean open=true;
            for(double[] offset:new double[][]{
                {0,0},{-3.5,-1},{1,-2.2},{-1.7,2},{-1.7,-1},
                {2.3,2.2},{3,-1.2},{4.3,2.2}}){
                double xx=x+offset[0],yy=y+offset[1];
                if(!host.world.canWalk(xx,yy)||
                    host.world.worldStructureAt(xx,yy,.55)){open=false;break;}
            }
            if(open)return new double[]{x,y};
        }
        throw new AssertionError("No open wilderness network parcel");
    }
    private static World.Device one(ArenaSystems host,int kind,String owner){
        return host.world.devices.stream().filter(d->d.kind==kind&&
            owner.equals(d.ownerKey)).findFirst().orElseThrow();
    }
    private static void cleanup(Path root)throws Exception{
        try(var walk=Files.walk(root)){
            for(Path p:walk.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);
        }
    }
    public static void main(String[] args)throws Exception{
        final long seed=20261029L;
        final String ownerKey="acct_"+"c".repeat(64);
        final String foreignKey="acct_"+"f".repeat(64);
        ArenaSystems host=new ArenaSystems(seed);
        var owner=host.join(1);
        check(host.assignAccount(1,ownerKey)&&
            host.enrollFaction(1,CharacterRoster.Faction.THE_UNRAVELED),
            "Real Unraveled operator not enrolled");
        var rival=host.join(2);
        check(host.assignAccount(2,foreignKey)&&
            host.enrollFaction(2,CharacterRoster.Faction.THE_PENITENT),
            "Foreign operator not enrolled");
        double[] at=site(host);double x=at[0],y=at[1];
        fixture(host,ArenaSystems.CAMP_STAKE,x-3.5,y-1,ownerKey);
        World.Device crate=fixture(host,ArenaSystems.STORAGE_CRATE,x+1,y-2.2,ownerKey);
        fixture(host,ArenaSystems.FORGE,x-1.7,y+2,ownerKey);
        World.Device boiler=fixture(host,ArenaSystems.STEAM_BOILER,x-1.7,y-1,ownerKey);
        boiler.steamMs=300_000;
        World.Device grid=fixture(host,ArenaSystems.UNRAVELED_RESILIENCE_GRID,x,y,ownerKey);
        var process=CraftingEraRegistry.process("resilience-core");
        check(process!=null&&process.utilityDemand()==25&&
            ElectricUtility.CAPACITY==24&&
            "resilience-grid".equals(process.facility()),
            "Incursion power and recipe contract changed");
        supply(crate,process.inputs());
        var staged=crate.storage.snapshot();
        owner.x=grid.x+.3;owner.y=grid.y;
        rival.x=owner.x;rival.y=owner.y;
        check(!host.startFactionBatch(1,process.id())&&
            !host.startFactionBatch(2,process.id())&&
            staged.equals(crate.storage.snapshot()),
            "Unpowered or foreign start spent actual recipe stock");

        World.Device first=paid(host,owner,crate,ArenaSystems.GENERATOR,x+2.3,y+2.2);
        World.Device bus=paid(host,owner,crate,ArenaSystems.ELECTRIC_BUS,x+3,y-1.2);
        fuel(host,owner,first,crate);
        owner.x=grid.x+.3;owner.y=grid.y;
        check(ElectricUtility.allocation(host.world.devices,grid,25,Map.of(),1000).isEmpty()&&
            !host.startFactionBatch(1,process.id()),
            "One 24-load generator unlawfully powered a 25-load factory");
        check(crate.storage.snapshot().equals(staged),
            "Insufficient single-generator supply spent batch inputs");
        World.Device second=paid(host,owner,crate,ArenaSystems.GENERATOR,x+4.3,y+2.2);
        fuel(host,owner,second,crate);
        var sources=ElectricUtility.allocation(host.world.devices,grid,25,Map.of(),1000);
        check(sources.size()==2&&sources.values().stream().mapToInt(Integer::intValue).sum()==25&&
            sources.values().stream().allMatch(load->load>0&&load<=24),
            "Two paid plants did not reserve exactly 25 separate load units");
        owner.x=second.x+.3;owner.y=second.y;
        check(host.reviewBuild(1,ArenaSystems.GENERATOR).reason()
                .equals("ALREADY_OWNS_FACTION_SITE"),
            "Owner could construct an unpaid third generating plant");
        owner.x=grid.x+.3;owner.y=grid.y;
        check(!host.startFactionBatch(2,process.id())&&
            host.startFactionBatch(1,process.id())&&
            !host.startFactionBatch(1,process.id()),
            "Faction-only exactly-once factory start failed");
        for(var input:process.inputs().entrySet())
            check(crate.storage.count(SurvivalCrafting.Item.valueOf(input.getKey()))==0,
                "Real resilience-core input was not debited");
        tick(host,600);
        check(grid.productionActive&&grid.productionMs==600_000&&
            first.electricMs==600_000&&second.electricMs==600_000,
            "Paid dual-grid production did not debit both reserves");
        bus.hp=0;tick(host,1);
        check(grid.productionMs==600_000&&first.electricMs==600_000&&
            second.electricMs==600_000,
            "Destroyed physical bus still delivered phantom power");
        bus.hp=100;

        Path root=Files.createTempDirectory("veil-incursion-dual-grid-");
        try{
            HostCheckpoint.save(host,root);
            ArenaSystems resumed=new ArenaSystems(seed);
            check(HostCheckpoint.load(resumed,root),
                "Incursion power network did not survive host checkpoint");
            resumed.join(1);
            check(resumed.assignAccount(1,ownerKey),
                "Recovered account identity cannot operate its factory");
            var operator=resumed.player(1);
            var running=one(resumed,ArenaSystems.UNRAVELED_RESILIENCE_GRID,ownerKey);
            var stock=one(resumed,ArenaSystems.STORAGE_CRATE,ownerKey);
            var steam=one(resumed,ArenaSystems.STEAM_BOILER,ownerKey);
            var plants=resumed.world.devices.stream().filter(d->
                d.kind==ArenaSystems.GENERATOR&&ownerKey.equals(d.ownerKey)).toList();
            check(plants.size()==2&&running.productionActive&&
                running.productionMs==600_000&&plants.stream().allMatch(d->d.electricMs==600_000),
                "Restart lost one paid generator or advanced/duplicated batch");
            tick(resumed,600);
            check(running.productionMs==1_200_000&&
                plants.stream().allMatch(d->d.electricMs==0),
                "25-load work failed to exhaust two actual charged reserves");
            tick(resumed,1);
            check(running.productionMs==1_200_000&&
                stock.storage.count(SurvivalCrafting.Item.RESILIENCE_CORE)==0,
                "Depleted network produced a free output");
            fuel(resumed,operator,plants.get(0),stock);
            fuel(resumed,operator,plants.get(1),stock);
            check(steam.steamMs==60_000,
                "Four paid generator startups did not spend steam reserve");
            tick(resumed,process.seconds()-1200);
            check(!running.productionActive&&running.productionCompleted==1&&
                stock.storage.count(SurvivalCrafting.Item.RESILIENCE_CORE)==1,
                "25-load Incursion machine did not deliver its real finished core");
            tick(resumed,5);
            check(stock.storage.count(SurvivalCrafting.Item.RESILIENCE_CORE)==1,
                "Idle host replayed completed Incursion output");
            HostCheckpoint.save(resumed,root);
            ArenaSystems finalWorld=new ArenaSystems(seed);
            check(HostCheckpoint.load(finalWorld,root)&&
                one(finalWorld,ArenaSystems.STORAGE_CRATE,ownerKey)
                    .storage.count(SurvivalCrafting.Item.RESILIENCE_CORE)==1&&
                one(finalWorld,ArenaSystems.UNRAVELED_RESILIENCE_GRID,ownerKey)
                    .productionCompleted==1,
                "Completed Incursion output duplicated or vanished after second restart");
        }finally{cleanup(root);}
        System.out.println("PASS IncursionDualGeneratorTest: paid 24+1 load, owner isolation, outage, restart, real resilience core");
    }
}
