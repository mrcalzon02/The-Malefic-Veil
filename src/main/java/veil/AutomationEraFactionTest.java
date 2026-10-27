package veil;

import java.nio.file.*;
import java.util.*;

/** Actual E5 factory construction and long electrical-era production.
 * No UI mocks or automatic reward grants: work is paid from a physical
 * camp crate, supply is bounded and the host controls faction identity. */
public final class AutomationEraFactionTest {
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    private static World.Device fixture(ArenaSystems host,int kind,
                                       double x,double y,String owner){
        World.Device d=new World.Device(x,y,kind);
        d.ownerKey=owner;host.world.devices.add(d);return d;
    }
    private static void stock(CampStockpile crate,Map<String,Integer> materials){
        for(var e:materials.entrySet())
            check(crate.delivered(SurvivalCrafting.Item.valueOf(e.getKey()),e.getValue()),
                "Fixture cannot stock "+e.getKey());
    }
    private static void simulate(ArenaSystems host,int seconds){
        for(int i=0;i<seconds;i++){
            host.advance(1000);
            if(i%30==0)for(var player:host.roster())if(!player.dead){
                var status=player.survival.status();
                if(status.hunger()<38){
                    check(player.survival.acquire(SurvivalCrafting.Item.EDIBLES,1,true),
                        "No real food provision available");
                    check(host.consumeSurvival(player.id,SurvivalCrafting.Item.EDIBLES),
                        "Host denied food consumption");
                }
                if(status.hydration()<38){
                    check(player.survival.acquire(SurvivalCrafting.Item.CLEAN_WATER,1,true),
                        "No real water provision available");
                    check(host.consumeSurvival(player.id,SurvivalCrafting.Item.CLEAN_WATER),
                        "Host denied water consumption");
                }
            }
        }
    }
    private static World.Device build(ArenaSystems host,ArenaSystems.Player p,
                                       CampStockpile stock,int kind,double x,double y){
        var spec=CraftingEraRegistry.facility(ArenaSystems.factionFacility(kind));
        check(spec!=null&&spec.era()==CraftingEraRegistry.Era.AUTOMATION,
            "E5 blueprint absent from original industrial catalog");
        stock(stock,spec.materials());
        for(double dx=-3;dx<=3;dx+=.5)
            for(double dy=-3;dy<=3;dy+=.5){
                p.x=x+dx-1.1;p.y=y+dy;p.angle=0;
                if(host.reviewBuild(p.id,kind).allowed()&&host.build(p.id,kind)){
                    World.Device built=host.world.devices.get(host.world.devices.size()-1);
                    for(var e:spec.materials().entrySet())
                        check(stock.count(SurvivalCrafting.Item.valueOf(e.getKey()))==0,
                            "Host failed to debit real E5 facility cost");
                    return built;
                }
            }
        throw new AssertionError("No legal physical plot for "+spec.id());
    }
    private static void testOne(int kind,CharacterRoster.Faction faction)throws Exception{
        long seed=20261020L;
        String identity="acct_"+"d".repeat(63)+(kind-52);
        ArenaSystems host=new ArenaSystems(seed);
        var owner=host.join(1);
        check(host.assignAccount(1,identity)&&host.enrollFaction(1,faction),
            "Missing persistent faction registration");
        double[] parcel=FactionFirstProductionTest.site(host);
        double x=parcel[0],y=parcel[1];
        fixture(host,ArenaSystems.CAMP_STAKE,x-2.4,y-2.4,identity);
        World.Device storage=fixture(host,ArenaSystems.STORAGE_CRATE,x+1.1,y+1.3,identity);
        var boiler=fixture(host,ArenaSystems.STEAM_BOILER,x-1.7,y+4.5,identity);
        boiler.steamMs=200_000;
        var generator=fixture(host,ArenaSystems.GENERATOR,x-1.6,y+3.1,identity);
        var bus=fixture(host,ArenaSystems.ELECTRIC_BUS,x+.2,y+3.0,identity);
        var blueprint=CraftingEraRegistry.facility(ArenaSystems.factionFacility(kind));
        owner.x=x;owner.y=y;
        check(host.reviewBuild(1,kind).reason().equals("MISSING_PHYSICAL_PREREQUISITE"),
            "Unbuilt E5 industry illegally bypassed owner site prerequisites");
        for(String needed:blueprint.requires()){
            int installed=ArenaSystems.kindForFacility(needed);
            check(installed>=0,"Catalog dependency has no real E5 or earlier world facility: "+needed);
            fixture(host,installed,x+5.1,y+4.4,identity);
        }
        var stranger=host.join(2);
        check(host.assignAccount(2,"acct_"+"a".repeat(64)),
            "Unauthorized account could not establish identity");
        CharacterRoster.Faction rival=faction==CharacterRoster.Faction.THE_PENITENT?
            CharacterRoster.Faction.THE_BEGOTTEN:CharacterRoster.Faction.THE_PENITENT;
        check(host.enrollFaction(2,rival),"Enemy faction was not selectable");
        stranger.x=x;stranger.y=y;
        check(host.reviewBuild(2,kind).reason().equals("WRONG_FACTION_FACILITY"),
            "E5 faction factory was available to another faction");
        World.Device factory=build(host,owner,storage.storage,kind,x,y);
        var recipe=CraftingEraRegistry.process(ArenaSystems.firstProcess(kind));
        check(recipe!=null&&recipe.era()==CraftingEraRegistry.Era.AUTOMATION&&
            recipe.facility().equals(blueprint.id())&&recipe.utilityDemand()==16,
            "E5 work order is not the registered faction-only recipe");
        stock(storage.storage,recipe.inputs());
        owner.x=factory.x+.35;owner.y=factory.y;
        check(!host.startFactionBatch(1,recipe.id()),
            "Unpowered high-era E5 factory incorrectly ran using steam");
        check(!host.startFactionBatch(2,recipe.id()),
            "A different faction started the owner's E5 job");
        owner.x=generator.x+.35;owner.y=generator.y;
        stock(storage.storage,Map.of("CHARCOAL",3,"CLEAN_WATER",2));
        check(host.chargeGenerator(1),
            "Owner could not charge the real E4 grid to run E5");
        owner.x=factory.x+.35;owner.y=factory.y;
        check(host.startFactionBatch(1,recipe.id()),
            "Host rejected supplied, powered E5 original faction process");
        check(!host.startFactionBatch(1,recipe.id()),
            "Duplicate E5 work order accepted without another input debit");
        for(var e:recipe.inputs().entrySet())
            check(storage.storage.count(SurvivalCrafting.Item.valueOf(e.getKey()))==0,
                "E5 work order did not physically reserve all ingredients");
        simulate(host,1000);
        check(factory.productionActive&&factory.productionMs==1_000_000&&
            generator.electricMs==ElectricUtility.MAX_CHARGE_MS-1_000_000,
            "Advanced production did not consume actual shared electricity");
        bus.hp=0;
        simulate(host,5);
        check(factory.productionMs==1_000_000&&
            generator.electricMs==200_000,
            "Destroyed E5 electric route permitted disconnected work");
        bus.hp=100;
        Path dir=Files.createTempDirectory("veil-e5-faction-");
        try{
            HostCheckpoint.save(host,dir);
            ArenaSystems recovered=new ArenaSystems(seed);
            check(HostCheckpoint.load(recovered,dir),
                "E5 original power + faction work order failed durable readback");
            recovered.join(1);
            check(recovered.assignAccount(1,identity),
                "Authenticated E5 operator was not recovered");
            var machine=recovered.world.devices.stream()
                .filter(d->d.kind==kind).findFirst().orElseThrow();
            var power=recovered.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.GENERATOR).findFirst().orElseThrow();
            var physicalStock=recovered.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE).findFirst().orElseThrow();
            check(machine.productionActive&&machine.productionMs==1_000_000&&
                machine.productionRecipeId.equals(recipe.id())&&
                power.electricMs==200_000,
                "V9 reload lost original faction job or shared utility consumption");
            var operator=recovered.player(1);
            operator.x=machine.x+.35;operator.y=machine.y;
            simulate(recovered,200);
            check(machine.productionMs==1_200_000&&power.electricMs==0,
                "Uncharged generator did not stall when battery was consumed");
            simulate(recovered,10);
            check(machine.productionMs==1_200_000,
                "E5 manufactured without recharging shared electricity");
            operator.x=power.x+.35;operator.y=power.y;
            stock(physicalStock.storage,Map.of("CHARCOAL",3,"CLEAN_WATER",2));
            check(recovered.chargeGenerator(1),
                "Could not pay for second E5 grid fuel charge");
            operator.x=machine.x+.35;operator.y=machine.y;
            simulate(recovered,recipe.seconds()-1200);
            var finished=SurvivalCrafting.Item.valueOf(recipe.output());
            check(!machine.productionActive&&machine.productionCompleted==1&&
                physicalStock.storage.count(finished)==recipe.quantity(),
                "E5 factory failed unique no-duplication output receipt");
            simulate(recovered,5);
            check(physicalStock.storage.count(finished)==recipe.quantity(),
                "Idle E5 factory replayed a paid batch");
            HostCheckpoint.save(recovered,dir);
            ArenaSystems twice=new ArenaSystems(seed);
            check(HostCheckpoint.load(twice,dir),
                "Completed E5 factory could not reload a previous work receipt");
            check(twice.world.devices.stream().filter(d->d.kind==kind)
                    .allMatch(d->!d.productionActive&&d.productionCompleted==1),
                "V9 save reopened an already delivered work order");
            check(twice.world.devices.stream().filter(d->d.kind==ArenaSystems.STORAGE_CRATE)
                .mapToInt(d->d.storage.count(finished)).sum()==recipe.quantity(),
                "V9 readback minted or destroyed exact faction output");
            System.out.println("PASS "+faction+" "+recipe.id()+" -> "+recipe.output());
        }finally{
            try(var entries=Files.walk(dir)){
                for(Path p:entries.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
    public static void main(String[] args)throws Exception{
        testOne(ArenaSystems.UNRAVELED_DEFENSE_WORKS,
            CharacterRoster.Faction.THE_UNRAVELED);
        testOne(ArenaSystems.PENITENT_WARD_SANCTUM,
            CharacterRoster.Faction.THE_PENITENT);
        testOne(ArenaSystems.BEGOTTEN_ORGAN_LATTICE,
            CharacterRoster.Faction.THE_BEGOTTEN);
        System.out.println("PASS AutomationEraFactionTest: three costed distinct advanced manufacturing spines");
    }
}
