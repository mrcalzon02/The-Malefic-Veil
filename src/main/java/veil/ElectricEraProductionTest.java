package veil;

import java.nio.file.*;
import java.util.*;

/** E4 acceptance: actually build the generator, bus, lathe and three distinct
 * faction factories, pay fuel, interrupt the grid, save a running job and
 * verify each factory's unique finished product exactly once. Prerequisite
 * older-era buildings/raw stock are controlled fixtures; ALL E4 operations
 * use the same authoritative host actions as the game client. */
public final class ElectricEraProductionTest {
    private static void check(boolean v,String reason){if(!v)throw new AssertionError(reason);}
    private static void tick(ArenaSystems game,int seconds){
        for(int i=0;i<seconds;i++)game.advance(1000);
    }
    private static World.Device fixture(ArenaSystems game,int kind,double x,double y,String owner){
        World.Device d=new World.Device(x,y,kind);d.ownerKey=owner;game.world.devices.add(d);
        return d;
    }
    private static void supply(CampStockpile crate,Map<String,Integer> cost){
        for(var e:cost.entrySet())
            check(crate.delivered(SurvivalCrafting.Item.valueOf(e.getKey()),e.getValue()),
                "Raw resource stock cannot fund "+e.getKey());
    }
    private static World.Device build(ArenaSystems game,ArenaSystems.Player owner,
        World.Device crate,int kind,double x,double y){
        var spec=CraftingEraRegistry.facility(ArenaSystems.factionFacility(kind));
        check(spec!=null,"E4 infrastructure absent from canonical facility registry");
        supply(crate.storage,spec.materials());
        for(double dx=-3.4;dx<=3.4;dx+=.425)
            for(double dy=-3.4;dy<=3.4;dy+=.425){
                owner.x=x+dx-1.1;owner.y=y+dy;owner.angle=0;
                if(game.reviewBuild(owner.id,kind).allowed()&&game.build(owner.id,kind)){
                    World.Device d=game.world.devices.get(game.world.devices.size()-1);
                    for(var e:spec.materials().entrySet())
                        check(crate.storage.count(SurvivalCrafting.Item.valueOf(e.getKey()))==0,
                            "Physical E4 site did not pay original "+e.getKey());
                    return d;
                }
            }
        throw new AssertionError("Could not legally build physical "+spec.id());
    }
    private static void fundGenerator(ArenaSystems game,ArenaSystems.Player owner,
                                      World.Device generator,World.Device stock){
        supply(stock.storage,Map.of("CHARCOAL",3,"CLEAN_WATER",2));
        owner.x=generator.x+.35;owner.y=generator.y;
        check(game.chargeGenerator(owner.id),"Paid E4 generator startup rejected");
    }
    private static void checkFaction(int kind,CharacterRoster.Faction faction)throws Exception{
        long seed=20261020L;
        String identity="acct_"+"d".repeat(63)+(kind-38);
        ArenaSystems game=new ArenaSystems(seed);
        var owner=game.join(1);
        check(game.assignAccount(1,identity)&&game.enrollFaction(1,faction),
            "Authentic faction operator cannot enroll");
        double[] parcel=FactionFirstProductionTest.site(game);
        double x=parcel[0],y=parcel[1];
        fixture(game,ArenaSystems.CAMP_STAKE,x-1.2,y-1.0,identity);
        World.Device stock=fixture(game,ArenaSystems.STORAGE_CRATE,x+1.1,y+1.4,identity);
        World.Device boiler=fixture(game,ArenaSystems.STEAM_BOILER,x-2.1,y+2.0,identity);
        boiler.steamMs=180_000L;
        fixture(game,ArenaSystems.FORGE,x-2.6,y-.3,identity);
        fixture(game,ArenaSystems.METAL_PRESS,x-2.5,y-2.4,identity);
        fixture(game,ArenaSystems.RAIN_CATCHER,x-3,y+1,identity);
        int old=switch(kind){
            case ArenaSystems.UNRAVELED_SENSOR_LAB -> ArenaSystems.UNRAVELED_PRESSURE_SHOP;
            case ArenaSystems.PENITENT_CHOIR_RELAY -> ArenaSystems.PENITENT_CANTOR_DAIS;
            case ArenaSystems.BEGOTTEN_MEMBRANE_LAB -> ArenaSystems.BEGOTTEN_CHITIN_VAT;
            default -> throw new IllegalArgumentException("Unrecognized faction's E4 site");
        };
        fixture(game,old,x-1.8,y-3.8,identity);
        owner.x=x;owner.y=y;
        check(game.reviewBuild(1,kind).reason().equals("MISSING_PHYSICAL_PREREQUISITE"),
            "E4 construction bypassed physical utility predecessors");
        World.Device generator=build(game,owner,stock,ArenaSystems.GENERATOR,x,y);
        World.Device bus=build(game,owner,stock,ArenaSystems.ELECTRIC_BUS,x,y);
        World.Device lathe=build(game,owner,stock,ArenaSystems.PRECISION_LATHE,x,y);
        if(kind==ArenaSystems.BEGOTTEN_MEMBRANE_LAB)
            build(game,owner,stock,ArenaSystems.WATER_WORKS,x,y);
        var enemy=game.join(2);
        check(game.assignAccount(2,"acct_"+"a".repeat(64)),"Enemy account fixture");
        var rival=faction==CharacterRoster.Faction.THE_PENITENT?
            CharacterRoster.Faction.THE_BEGOTTEN:CharacterRoster.Faction.THE_PENITENT;
        check(game.enrollFaction(2,rival),"Enemy faction enrollment");
        enemy.x=x;enemy.y=y;
        check(game.reviewBuild(2,kind).reason().equals("WRONG_FACTION_FACILITY"),
            "E4 faction-only construction failed to check identity");
        World.Device plant=build(game,owner,stock,kind,x,y);
        var process=CraftingEraRegistry.process(ArenaSystems.firstProcess(kind));
        check(process.era()==CraftingEraRegistry.Era.ELECTRIC&&
            process.utilityDemand()==8&&
            process.facility().equals(ArenaSystems.factionFacility(kind)),
            "The factory was not registered in its separate E4 economy");
        supply(stock.storage,process.inputs());
        owner.x=plant.x+.4;owner.y=plant.y;
        check(!game.startFactionBatch(1,process.id()),"Cold unpowered E4 factory ran");
        check(!game.startFactionBatch(2,process.id()),"Outsider started owner's E4 job");
        check(!game.chargeGenerator(2),"Outsider powered somebody else's generator");
        owner.x=generator.x+.4;owner.y=generator.y;
        check(!game.chargeGenerator(1),"E4 generator created electricity without stock");
        fundGenerator(game,owner,generator,stock);
        check(generator.electricMs==ElectricUtility.MAX_CHARGE_MS&&
            boiler.steamMs==180_000-ElectricUtility.STEAM_STARTUP_MS,
            "Generator charge did not spend the previous steam-era reserve");
        owner.x=plant.x+.4;owner.y=plant.y;
        check(game.startFactionBatch(1,process.id()),"Genuine electrical factory failed startup");
        check(!game.startFactionBatch(1,process.id()),"Duplicate paid E4 job accepted");
        tick(game,100);
        check(plant.productionMs==100_000&&
            generator.electricMs==ElectricUtility.MAX_CHARGE_MS-100_000,
            "Active E4 factory did not consume charged electricity");
        bus.hp=0;
        tick(game,10);
        check(plant.productionMs==100_000&&
            generator.electricMs==ElectricUtility.MAX_CHARGE_MS-100_000,
            "Destroyed electric bus failed to pause live machine");
        bus.hp=100;
        Path root=Files.createTempDirectory("veil-e4-");
        try{
            HostCheckpoint.save(game,root);
            ArenaSystems resumed=new ArenaSystems(seed);
            check(HostCheckpoint.load(resumed,root),"E4 paired host checkpoint unreadable");
            resumed.join(1);
            check(resumed.assignAccount(1,identity),"Restored account lost its identity");
            World.Device running=resumed.world.devices.stream().filter(d->d.kind==kind)
                .findFirst().orElseThrow();
            World.Device power=resumed.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.GENERATOR).findFirst().orElseThrow();
            World.Device persistedBoiler=resumed.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STEAM_BOILER).findFirst().orElseThrow();
            World.Device stored=resumed.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE).findFirst().orElseThrow();
            check(running.productionActive&&running.productionMs==100_000&&
                running.productionRecipeId.equals(process.id())&&
                power.electricMs==ElectricUtility.MAX_CHARGE_MS-100_000,
                "E4 restart forgot machine receipt or exact powered reserve");
            var operator=resumed.player(1);
            operator.x=running.x+.4;operator.y=running.y;
            tick(resumed,500);
            check(running.productionMs==600_000&&power.electricMs==600_000,
                "Reconnected electrical grid did not advance or debit correctly");
            if(kind==ArenaSystems.PENITENT_CHOIR_RELAY){
                operator.x=running.x+20;operator.y=running.y;
                tick(resumed,10);
                check(running.productionMs==600_000&&power.electricMs==600_000,
                    "Unstaffed Penitent choir advanced or burned electrical reserve");
            }
            supply(stored.storage,Map.of("CHARCOAL",3,"CLEAN_WATER",2));
            operator.x=power.x+.4;operator.y=power.y;
            check(resumed.chargeGenerator(1),"Paid electrical recharge denied");
            check(persistedBoiler.steamMs==180_000-2*ElectricUtility.STEAM_STARTUP_MS,
                "Electricity recharge bypassed boiler fuel accounting");
            operator.x=running.x+.4;operator.y=running.y;
            tick(resumed,process.seconds()-600);
            SurvivalCrafting.Item output=SurvivalCrafting.Item.valueOf(process.output());
            check(!running.productionActive&&running.productionCompleted==1&&
                stored.storage.count(output)==process.quantity(),
                "Faction E4 product did not finish exactly once at stored world crate");
            tick(resumed,5);
            check(stored.storage.count(output)==process.quantity(),
                "Completed E4 work order replayed output");
            if(kind==ArenaSystems.UNRAVELED_SENSOR_LAB){
                World.Device realLathe=resumed.world.devices.stream()
                    .filter(d->d.kind==ArenaSystems.PRECISION_LATHE).findFirst().orElseThrow();
                var wire=CraftingEraRegistry.process("copper-wire");
                supply(stored.storage,wire.inputs());
                operator.x=realLathe.x+.4;operator.y=realLathe.y;
                check(resumed.startFactionBatch(1,wire.id()),
                    "Real powered precision lathe denied a canonical E4 recipe");
                tick(resumed,wire.seconds());
                check(stored.storage.count(SurvivalCrafting.Item.CONDUCTOR_WIRE)==wire.quantity(),
                    "Shared electrical precision lathe did not manufacture conductor wire");
            }
            HostCheckpoint.save(resumed,root);
            ArenaSystems finalWorld=new ArenaSystems(seed);
            check(HostCheckpoint.load(finalWorld,root),
                "Completed independent E4 production cannot reload");
            check(finalWorld.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE)
                .mapToInt(d->d.storage.count(output)).sum()==process.quantity(),
                "Completed E4 output duplicated or disappeared after checkpoint");
            System.out.println("PASS "+faction+" E4 "+process.id()+" powered and recoverable");
        }finally{
            try(var files=Files.walk(root)){
                for(Path p:files.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
    public static void main(String[] args)throws Exception{
        checkFaction(ArenaSystems.UNRAVELED_SENSOR_LAB,CharacterRoster.Faction.THE_UNRAVELED);
        checkFaction(ArenaSystems.PENITENT_CHOIR_RELAY,CharacterRoster.Faction.THE_PENITENT);
        checkFaction(ArenaSystems.BEGOTTEN_MEMBRANE_LAB,CharacterRoster.Faction.THE_BEGOTTEN);
        System.out.println("PASS ElectricEraProductionTest: E4 genuinely powered distinct factions");
    }
}
