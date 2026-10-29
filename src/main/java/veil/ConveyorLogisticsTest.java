package veil;

import java.nio.file.*;
import java.util.*;

/** Host integration: powered E5 conveyor, real separate crate ledgers,
 * one paid item each five seconds, strict access and V9 recovery. */
public final class ConveyorLogisticsTest {
    private static void check(boolean valid,String reason){
        if(!valid)throw new AssertionError(reason);
    }
    private static World.Device fixture(ArenaSystems game,int kind,
            double x,double y,String owner){
        World.Device device=new World.Device(x,y,kind);
        device.ownerKey=owner;game.world.devices.add(device);
        return device;
    }
    private static void tick(ArenaSystems game,int seconds){
        for(int n=0;n<seconds;n++)game.advance(1000);
    }
    private static World.Device find(ArenaSystems game,int kind){
        return game.world.devices.stream().filter(d->d.kind==kind)
            .findFirst().orElseThrow();
    }
    public static void main(String[] args)throws Exception{
        long seed=20261020L;
        String owner="acct_"+"e".repeat(64);
        ArenaSystems game=new ArenaSystems(seed);
        var actor=game.join(1);
        check(game.assignAccount(1,owner)&&
            game.enrollFaction(1,CharacterRoster.Faction.THE_UNRAVELED),
            "Authorized settlement operator required");
        double[] parcel=FactionFirstProductionTest.site(game);
        double x=parcel[0],y=parcel[1];
        fixture(game,ArenaSystems.CAMP_STAKE,x,y-2.8,owner);
        World.Device source=fixture(game,ArenaSystems.STORAGE_CRATE,x-2.7,y,owner);
        World.Device target=fixture(game,ArenaSystems.STORAGE_CRATE,x+2.7,y,owner);
        World.Device conveyor=fixture(game,ArenaSystems.CONVEYOR_LINE,x,y,owner);
        World.Device bus=fixture(game,ArenaSystems.ELECTRIC_BUS,x+.5,y+2.2,owner);
        World.Device generator=fixture(game,ArenaSystems.GENERATOR,x-1.4,y+3.0,owner);
        World.Device boiler=fixture(game,ArenaSystems.STEAM_BOILER,x-1.8,y+4.0,owner);
        boiler.steamMs=120_000;
        check(source.storage.delivered(SurvivalCrafting.Item.MACHINE_PARTS,4)&&
            source.storage.delivered(SurvivalCrafting.Item.CHARCOAL,3)&&
            source.storage.delivered(SurvivalCrafting.Item.CLEAN_WATER,2),
            "Failed to stage real physical materials");
        actor.x=x;actor.y=y;
        check(game.programConveyor(1,"MACHINE_PARTS"),
            "Failed to install route on physical owned conveyor");
        check(conveyor.routeItem.equals("MACHINE_PARTS"),
            "Unrelated route was configured instead");
        var intruder=game.join(2);
        intruder.x=x;intruder.y=y;
        check(!game.programConveyor(2,"MACHINE_PARTS"),
            "Anonymous guest remotely reprogrammed a private conveyor");
        tick(game,11);
        check(source.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==4&&
            target.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==0,
            "Cold E5 conveyor moved supplies without any actual electrical budget");
        actor.x=generator.x+.3;actor.y=generator.y;
        check(game.chargeGenerator(1),
            "Authenticated owner could not charge generator with paid fuel");
        check(source.storage.count(SurvivalCrafting.Item.CHARCOAL)==0&&
            source.storage.count(SurvivalCrafting.Item.CLEAN_WATER)==0,
            "Boiler / generator fuel was not debited from real storage");
        check(boiler.steamMs==120_000-ElectricUtility.STEAM_STARTUP_MS,
            "Electric startup failed preceding physical steam reserve debit");
        actor.x=x;actor.y=y;
        tick(game,4);
        check(conveyor.routeMs==4000&&
            source.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==4,
            "Conveyor delivered before a full powered logistics cycle");

        Path root=Files.createTempDirectory("veil-conveyor-e5-");
        try{
            HostCheckpoint.save(game,root);
            ArenaSystems resumed=new ArenaSystems(seed);
            check(HostCheckpoint.load(resumed,root),
                "E5 host paired checkpoint rejected V9 conveyor state");
            resumed.join(1);
            check(resumed.assignAccount(1,owner),
                "Original operator lost settlement permission");
            World.Device input=resumed.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&d.x<x)
                .findFirst().orElseThrow();
            World.Device output=resumed.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&d.x>x)
                .findFirst().orElseThrow();
            World.Device lane=find(resumed,ArenaSystems.CONVEYOR_LINE);
            World.Device panel=find(resumed,ArenaSystems.ELECTRIC_BUS);
            World.Device power=find(resumed,ArenaSystems.GENERATOR);
            check(lane.routeItem.equals("MACHINE_PARTS")&&lane.routeMs==4000&&
                input.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==4,
                "Save split actual source stock and a pending conveyor receipt");
            tick(resumed,1);
            check(lane.routeMs==0&&input.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==3&&
                output.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==1,
                "Restored route did not transfer exactly one authentic item");
            panel.hp=0;
            tick(resumed,9);
            check(input.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==3&&
                output.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==1&&
                lane.routeMs==0,"Offline electrical grid did not stop automated item transfer");
            panel.hp=100;
            tick(resumed,15);
            check(input.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==0&&
                output.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==4,
                "Powered conveyor transferred the wrong number or copied stock");
            check(power.electricMs==ElectricUtility.MAX_CHARGE_MS-20_000,
                "Conveyor did not consume exactly 20 seconds of paid electrical supply");
            tick(resumed,10);
            check(output.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==4&&
                lane.routeMs==0,"Empty source conveyor duplicated materials");
            var operator=resumed.player(1);
            operator.x=lane.x;operator.y=lane.y;
            check(resumed.programConveyor(1,"NONE"),
                "The user could not disable an owned conveyor");
            check(lane.routeItem.isEmpty()&&lane.routeMs==0,
                "Disabled conveyor retained a phantom pending transfer");
            HostCheckpoint.save(resumed,root);
            ArenaSystems loaded=new ArenaSystems(seed);
            check(HostCheckpoint.load(loaded,root),
                "Completed automatic E5 logistics receipt unreadable");
            check(loaded.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE)
                .mapToInt(d->d.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)).sum()==4,
                "A crate-to-crate transport changed the total material supply");
        }finally{
            try(var walk=Files.walk(root)){
                for(Path entry:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(entry);
            }
        }
        System.out.println("PASS ConveyorLogisticsTest: powered west-to-east stock routing, unauthorized programming blocked, V9 replay safe");
    }
}
