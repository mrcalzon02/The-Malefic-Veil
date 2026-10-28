package veil;

import java.nio.file.*;
import java.util.*;

/** Host-functional steam era, three incompatible faction factories. Sites
 * are constructed through the actual build call with canonical costs; the
 * preexisting upstream buildings and raw inputs are trusted test fixtures. */
public final class SteamEraProductionTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void ticks(ArenaSystems host,int seconds){
        for(int i=0;i<seconds;i++)host.advance(1000);
    }
    private static World.Device fixture(ArenaSystems host,int kind,
            double x,double y,String key){
        World.Device d=new World.Device(x,y,kind);
        d.ownerKey=key;
        host.world.devices.add(d);
        return d;
    }
    private static void deposit(CampStockpile crate,Map<String,Integer> costs){
        for(var entry:costs.entrySet())
            check(crate.delivered(SurvivalCrafting.Item.valueOf(entry.getKey()),
                entry.getValue()),"Insufficient storage for real material "+entry.getKey());
    }
    private static World.Device build(ArenaSystems host,ArenaSystems.Player p,
            int kind,CampStockpile stock,double x,double y){
        var facility=CraftingEraRegistry.facility(ArenaSystems.factionFacility(kind));
        check(facility!=null,"Steam facility has no canonical construction spec");
        deposit(stock,facility.materials());
        for(double dx=-2.5;dx<=2.5;dx+=.5)
            for(double dy=-2.0;dy<=2.0;dy+=.5){
                p.x=x+dx;p.y=y+dy;p.angle=0;
                if(host.reviewBuild(p.id,kind).allowed()&&host.build(p.id,kind)){
                    World.Device built=host.world.devices.get(host.world.devices.size()-1);
                    for(String item:facility.materials().keySet())
                        check(stock.count(SurvivalCrafting.Item.valueOf(item))==0,
                            "Construction did not spend genuine "+item);
                    return built;
                }
            }
        throw new AssertionError("No legal physical steam factory parcel for "+facility.id());
    }
    private static void exercise(int kind,CharacterRoster.Faction faction)throws Exception{
        long seed=20261020L;
        String key="acct_"+"e".repeat(63)+(kind-31);
        ArenaSystems host=new ArenaSystems(seed);
        ArenaSystems.Player actor=host.join(1);
        check(host.assignAccount(1,key)&&host.enrollFaction(1,faction),
            "Faction owner must authenticate and enroll");
        double[] coords=FactionFirstProductionTest.site(host);
        double x=coords[0],y=coords[1];
        fixture(host,ArenaSystems.CAMP_STAKE,x-1.3,y-1,key);
        World.Device crate=fixture(host,ArenaSystems.STORAGE_CRATE,x+1.0,y+2.4,key);
        var boiler=fixture(host,ArenaSystems.STEAM_BOILER,x-2.1,y+2.8,key);
        var shaft=fixture(host,ArenaSystems.MECHANICAL_SHAFT,x+.3,y+4.0,key);
        int predecessor=switch(kind){
            case ArenaSystems.UNRAVELED_PRESSURE_SHOP ->
                ArenaSystems.UNRAVELED_MACHINIST_BAY;
            case ArenaSystems.PENITENT_CANTOR_DAIS ->
                ArenaSystems.PENITENT_RELIQUARY;
            case ArenaSystems.BEGOTTEN_CHITIN_VAT ->
                ArenaSystems.BEGOTTEN_SPORE_BASIN;
            default -> throw new IllegalArgumentException("Unknown faction E3 site");
        };
        fixture(host,predecessor,x-3,y,key);
        var guest=host.join(2);
        guest.x=x;guest.y=y;
        check(!host.fireSteamBoiler(2),"Unowned guest refueled another faction's boiler");
        int foreign=kind==ArenaSystems.PENITENT_CANTOR_DAIS?
            ArenaSystems.UNRAVELED_PRESSURE_SHOP:ArenaSystems.PENITENT_CANTOR_DAIS;
        actor.x=x;actor.y=y;
        check(host.reviewBuild(1,foreign).reason().equals("WRONG_FACTION_FACILITY"),
            "Faction-only E3 station was not protected");
        World.Device factory=build(host,actor,kind,crate.storage,x,y);
        check(factory.hp>0&&key.equals(factory.ownerKey),
            "E3 factory has no real physical authenticated owner");
        String recipeId=ArenaSystems.firstProcess(kind);
        var recipe=CraftingEraRegistry.process(recipeId);
        check(recipe.facility().equals(ArenaSystems.factionFacility(kind))&&
            recipe.utilityDemand()==3,"E3 must demand genuine 3-unit steam");
        deposit(crate.storage,recipe.inputs());
        actor.x=factory.x+.5;actor.y=factory.y;
        check(!host.startFactionBatch(1,recipeId),
            "Machine ran while connected boiler had no fuel pressure");
        check(!factory.productionActive&&boiler.steamMs==0,
            "Failed no-power start mutated production ledger");
        check(SteamUtility.available(host.world.devices,factory,3,Map.of())==null,
            "Cold unpowered boiler supplied invisible utility");
        actor.x=boiler.x+.5;actor.y=boiler.y;
        check(!host.fireSteamBoiler(1),"Unfueled boiler charged itself");
        check(crate.storage.delivered(SurvivalCrafting.Item.FIREWOOD,3)&&
            crate.storage.delivered(SurvivalCrafting.Item.DIRTY_WATER,2),
            "Cannot deliver genuine boiler fuel and water");
        check(host.fireSteamBoiler(1),"Boiler did not consume paid stock");
        check(crate.storage.count(SurvivalCrafting.Item.FIREWOOD)==0&&
            crate.storage.count(SurvivalCrafting.Item.DIRTY_WATER)==0,
            "Utility fuels duplicated or were not consumed");
        check(boiler.steamMs==SteamUtility.MAX_CHARGE_MS,
            "Boiler capacity was not actually charged");
        actor.x=factory.x+.5;actor.y=factory.y;
        check(host.startFactionBatch(1,recipeId),"Powered E3 batch refused valid stock");
        check(!host.startFactionBatch(1,recipeId),
            "Double-start accepted a second copy of the same inputs");
        for(var entry:recipe.inputs().entrySet())
            check(crate.storage.count(SurvivalCrafting.Item.valueOf(entry.getKey()))==0,
                "E3 did not atomically reserve "+entry.getKey());
        ticks(host,100);
        check(factory.productionActive&&factory.productionMs==100_000&&
            boiler.steamMs==SteamUtility.MAX_CHARGE_MS-100_000,
            "Active machine did not consume real steam at correct rate");
        long remainingPressure=boiler.steamMs;
        boiler.steamMs=1;
        ticks(host,1);
        check(factory.productionMs==100_000&&boiler.steamMs==1&&
            factory.factoryHold.equals("WAIT_STEAM"),
            "One millisecond of steam funded a full paid E3 manufacturing tick");
        actor.x=factory.x+.5;actor.y=factory.y;
        check(host.inspectFactory(1).state().equals("WAIT_STEAM"),
            "E3 inspector declared factory RUNNING with less than one steam tick");
        boiler.steamMs=remainingPressure;

        // A disconnected shaft must freeze processing, not run on a cached
        // boolean or refund materials. The paid steam reserve must survive.
        shaft.hp=0;
        ticks(host,12);
        check(factory.productionMs==100_000&&
            boiler.steamMs==SteamUtility.MAX_CHARGE_MS-100_000,
            "Broken mechanical shaft did not interrupt real factory utility");
        shaft.hp=100;
        Path dir=Files.createTempDirectory("veil-steam-era-");
        try{
            HostCheckpoint.save(host,dir);
            ArenaSystems restored=new ArenaSystems(seed);
            check(HostCheckpoint.load(restored,dir),"Paid E3 job lost its checkpoint");
            restored.join(1);
            check(restored.assignAccount(1,key),"Faction operator identity lost");
            World.Device running=restored.world.devices.stream()
                .filter(d->d.kind==kind).findFirst().orElseThrow();
            World.Device charged=restored.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STEAM_BOILER).findFirst().orElseThrow();
            World.Device warehouse=restored.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE).findFirst().orElseThrow();
            check(running.productionActive&&running.productionMs==100_000&&
                running.productionRecipeId.equals(recipeId)&&
                charged.steamMs==SteamUtility.MAX_CHARGE_MS-100_000,
                "V7 restart lost pressurized boiler or distinct faction work order");
            ArenaSystems.Player restarted=restored.player(1);
            restarted.x=running.x+.5;restarted.y=running.y;
            ticks(restored,recipe.seconds()-100);
            var output=SurvivalCrafting.Item.valueOf(recipe.output());
            check(!running.productionActive&&running.productionCompleted==1&&
                warehouse.storage.count(output)==recipe.quantity(),
                "Powered factory did not produce its own E3 item exactly once");
            ticks(restored,12);
            check(warehouse.storage.count(output)==recipe.quantity(),
                "Idle E3 machine repeated its manufacturing receipt");
            // Host damage, not fixture assignment: the final kill must
            // depressurize a boiler so its destroyed-world save can reload.
            var adversary=restored.join(2);
            check(restored.assignAccount(2,"acct_"+"a".repeat(64)),
                "Adversarial account could not authenticate");
            CharacterRoster.Faction enemy=faction==CharacterRoster.Faction.THE_PENITENT?
                CharacterRoster.Faction.THE_BEGOTTEN:
                CharacterRoster.Faction.THE_PENITENT;
            check(restored.enrollFaction(2,enemy),
                "Enemy raider failed enrollment");
            check(restored.damageDevice(2,restored.world.devices.indexOf(charged),
                120,System.currentTimeMillis()),
                "Legitimate hostile raid failed to destroy live steam boiler");
            check(charged.hp==0&&charged.steamMs==0,
                "Destroyed boiler retained forbidden live pressure reserve");
            HostCheckpoint.save(restored,dir);
            ArenaSystems loaded=new ArenaSystems(seed);
            check(HostCheckpoint.load(loaded,dir),"Completed E3 checkpoint cannot reload");
            check(loaded.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STEAM_BOILER)
                .allMatch(d->d.hp==0&&d.steamMs==0),
                "Destroyed E3 boiler reloaded with illegal power reserve");
            check(loaded.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE)
                .mapToInt(d->d.storage.count(output)).sum()==recipe.quantity(),
                "Completed E3 output duplicated across paired save generations");
        }finally{
            try(var paths=Files.walk(dir)){
                for(Path p:paths.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
        System.out.println("PASS independent steam industry: "+faction+
            " -> "+recipe.output()+"; physical fuel, shaft outage, V7 recovery");
    }
    public static void main(String[] args)throws Exception{
        exercise(ArenaSystems.UNRAVELED_PRESSURE_SHOP,
            CharacterRoster.Faction.THE_UNRAVELED);
        exercise(ArenaSystems.PENITENT_CANTOR_DAIS,
            CharacterRoster.Faction.THE_PENITENT);
        exercise(ArenaSystems.BEGOTTEN_CHITIN_VAT,
            CharacterRoster.Faction.THE_BEGOTTEN);
        System.out.println("PASS SteamEraProductionTest: 3 original E3 factions powered by audited shared utility");
    }
}
