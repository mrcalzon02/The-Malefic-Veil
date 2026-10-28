package veil;

import java.nio.file.*;
import java.util.*;

/** Only a nearby owned host belt can be inspected. Its report must reflect
 * physical source, downstream capacity, powered work and queued AUTO demand.
 * Inspection is read-only, LAN safe, and survives V11 restart. */
public final class ConveyorInspectionTest {
    private static void check(boolean yes,String reason){
        if(!yes)throw new AssertionError(reason);
    }
    private static World.Device add(ArenaSystems host,int kind,
            double x,double y,String owner){
        World.Device d=new World.Device(x,y,kind);
        d.ownerKey=owner;host.world.devices.add(d);return d;
    }
    private static void tick(ArenaSystems host,int seconds){
        for(int i=0;i<seconds;i++)host.advance(1000);
    }
    private static ArenaSystems.ConveyorInspection expect(ArenaSystems host,
            int playerId,String state){
        var report=host.inspectConveyor(playerId);
        check(report.state().equals(state),
            "Expected "+state+" from physical belt, got "+report.wire());
        check(report.wire().split("\\|").length==9,
            "LAN response omitted an authoritative conveyor field");
        return report;
    }
    private static void clear(Path root)throws Exception{
        try(var walk=Files.walk(root)){
            for(Path path:walk.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(path);
        }
    }
    public static void main(String[] args)throws Exception{
        long seed=20261020L;
        String owner="acct_"+"e".repeat(64),foreign="acct_"+"a".repeat(64);
        var host=new ArenaSystems(seed);
        var player=host.join(1);
        check(host.assignAccount(1,owner)&&
            host.enrollFaction(1,CharacterRoster.Faction.THE_UNRAVELED),
            "E5 owner account required");
        double[] site=FactionFirstProductionTest.site(host);
        double x=site[0],y=site[1];
        add(host,ArenaSystems.CAMP_STAKE,x,y-3,owner);
        var source=add(host,ArenaSystems.STORAGE_CRATE,x-3,y,owner);
        var receiver=add(host,ArenaSystems.STORAGE_CRATE,x+3,y,owner);
        var foreignStock=add(host,ArenaSystems.STORAGE_CRATE,x-2.9,y+.3,foreign);
        var belt=add(host,ArenaSystems.CONVEYOR_LINE,x,y,owner);
        add(host,ArenaSystems.ELECTRIC_BUS,x,y+2.5,owner);
        var generator=add(host,ArenaSystems.GENERATOR,x+.6,y+3,owner);
        var consumer=add(host,ArenaSystems.INDUSTRIAL_WORKSHOP,x+7.1,y,owner);
        player.x=x;player.y=y;
        var enemy=host.join(2);
        check(host.assignAccount(2,foreign),"Foreign account missing");
        enemy.x=x;enemy.y=y;
        expect(host,2,"NO_OWN_CONVEYOR");
        expect(host,1,"DISABLED");
        check(host.programConveyor(1,"MACHINE_PARTS"),
            "Host refused real manual physical route");
        var empty=expect(host,1,"WAIT_INPUT");
        check(empty.source()==0&&empty.space()==128&&
            empty.load()==ConveyorLogistics.DEMAND,
            "Missing source report misrepresented real crate capacity");
        check(foreignStock.storage.delivered(SurvivalCrafting.Item.MACHINE_PARTS,5),
            "Could not stage a foreign-owner isolation guard");
        expect(host,1,"WAIT_INPUT");
        receiver.hp=0;
        expect(host,1,"WAIT_ROUTE");
        receiver.hp=100;
        check(source.storage.delivered(SurvivalCrafting.Item.MACHINE_PARTS,3),
            "Cannot stage actual paid transfer stock");
        expect(host,1,"WAIT_POWER");
        generator.electricMs=1;
        expect(host,1,"WAIT_POWER");
        tick(host,1);
        check(belt.routeMs==0&&generator.electricMs==1&&
            source.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==3,
            "Almost-empty source powered a supposedly-ready conveyor");
        generator.electricMs=ElectricUtility.MAX_CHARGE_MS;
        var ready=expect(host,1,"READY");
        check(ready.source()==3&&ready.space()==128&&
            ready.detail().equals("MACHINE_PARTS"),
            "Ready inspector did not select the actual transfer item");
        tick(host,1);
        var moving=expect(host,1,"MOVING");
        check(moving.elapsed()==1000&&moving.duration()==5000&&
            moving.source()==3&&generator.electricMs==
                ElectricUtility.MAX_CHARGE_MS-1000,
            "Inspection advanced conveyor without paid power or forgot progress");
        check(receiver.storage.delivered(SurvivalCrafting.Item.EDIBLES,128),
            "Cannot fill real destination stock capacity");
        expect(host,1,"WAIT_STORAGE");
        tick(host,3);
        check(belt.routeMs==1000&&receiver.storage.count(
            SurvivalCrafting.Item.MACHINE_PARTS)==0,
            "Warehouse backpressure fabricated delivery or advanced route meter");
        check(receiver.storage.consume(Map.of(SurvivalCrafting.Item.EDIBLES,128)),
            "Could not free physical receiver capacity");
        tick(host,4);
        check(receiver.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==1&&
            source.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==2&&
            foreignStock.storage.count(SurvivalCrafting.Item.MACHINE_PARTS)==5,
            "Resumed transfer ignored paid clock or crossed account boundary");
        check(host.programConveyor(1,"AUTO"),"Could not switch to automatic route");
        var noQueue=expect(host,1,"WAIT_DEMAND");
        check(noQueue.detail().equals("NO_QUEUE"),
            "Idle AUTO conveyor claimed source starvation without a real job");
        player.x=consumer.x;player.y=consumer.y;
        check(host.programNextAutomation(1,AutomationOrders.repeat("ballistic-resupply")),
            "Consumer refused persistent real E5 input demand");
        // Payable materials are intentionally staged in receiving warehouse
        // except the one item that must physically travel via AUTO.
        check(receiver.storage.delivered(SurvivalCrafting.Item.HARDENED_ALLOY,2)&&
            receiver.storage.delivered(SurvivalCrafting.Item.FASTENERS,4)&&
            receiver.storage.delivered(SurvivalCrafting.Item.TRACKING_CIRCUIT,1),
            "Cannot stage accurate downstream production material deficits");
        player.x=x;player.y=y;
        var lacking=expect(host,1,"WAIT_INPUT");
        check(lacking.detail().equals("MISSING_PRECISION_FEEDLINE_1"),
            "AUTO report did not show exactly the missing physical ingredient");
        check(source.storage.delivered(SurvivalCrafting.Item.PRECISION_FEEDLINE,1),
            "Cannot supply real AUTO production feedline");
        expect(host,1,"READY");
        check(receiver.storage.delivered(SurvivalCrafting.Item.EDIBLES,120),
            "Cannot create full receiving warehouse for AUTO backpressure");
        var full=expect(host,1,"WAIT_STORAGE");
        check(full.detail().equals("FULL_RECEIVER"),
            "AUTO did not distinguish receiving storage from missing ingredients");
        check(receiver.storage.consume(Map.of(SurvivalCrafting.Item.EDIBLES,120)),
            "Cannot reopen paid AUTO receiving space");
        tick(host,1);
        var running=expect(host,1,"MOVING");
        check(running.elapsed()==1000&&running.source()==1,
            "AUTO inspector did not read real powered transfer progress");
        Path root=Files.createTempDirectory("veil-conveyor-inspection-");
        try{
            HostCheckpoint.save(host,root);
            var saved=new ArenaSystems(seed);
            check(HostCheckpoint.load(saved,root),
                "V11 conveyor inspection network could not reload");
            var resumed=saved.join(1);
            check(saved.assignAccount(1,owner),"Saved owner lost conveyor access");
            resumed.x=x;resumed.y=y;
            var restored=expect(saved,1,"MOVING");
            check(restored.mode().equals("AUTO")&&
                restored.direction().equals("WEST_TO_EAST")&&
                restored.elapsed()==1000&&restored.source()==1&&restored.space()==120,
                "Saved AUTO direction, paid progress or real warehouse stock changed");
            tick(saved,4);
            var funded=saved.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.INDUSTRIAL_WORKSHOP)
                .findFirst().orElseThrow();
            check(funded.productionActive&&
                funded.productionRecipeId.equals("ballistic-resupply")&&
                saved.world.devices.stream().filter(d->
                    d.kind==ArenaSystems.STORAGE_CRATE&&d.x==receiver.x)
                .findFirst().orElseThrow().storage.count(
                    SurvivalCrafting.Item.PRECISION_FEEDLINE)==0,
                "Restored belt did not deliver into exactly-once paid downstream work");
        }finally{clear(root);}
        player.x=x+20;player.y=y+20;
        expect(host,1,"NO_OWN_CONVEYOR");
        // Real TCP check: the client must receive the nine typed fields,
        // while a neighboring unauthenticated guest cannot probe owner stock.
        try(LanServer server=new LanServer(0,seed);
            NetLink client=new NetLink("127.0.0.1",server.port())){
            // A real NetLink immediately sends AUTH. Do NOT race it with a
            // manual account assignment: the host must decide who owns this
            // session, and its AUTH_OK is the authoritative identity receipt.
            long deadline=System.nanoTime()+5_000_000_000L;
            while(client.accountKey()==null&&System.nanoTime()<deadline)
                Thread.sleep(20);
            String authenticated=client.accountKey();
            check(authenticated!=null&&authenticated.startsWith("acct_")&&
                authenticated.equals(server.match.player(client.id).accountKey),
                "Real TCP handshake did not bind a persistent owner");
            synchronized(server.match){
                var local=server.match.player(client.id);
                local.x=x;local.y=y;
                add(server.match,ArenaSystems.CONVEYOR_LINE,x,y,authenticated);
            }
            client.inspectConveyor();
            deadline=System.nanoTime()+5_000_000_000L;
            while(client.conveyorInspection==null&&System.nanoTime()<deadline)
                Thread.sleep(20);
            check(client.conveyorInspection!=null&&
                client.conveyorInspection.state().equals("DISABLED"),
                "Authenticated LAN failed to parse its own conveyor inspection: "+
                    client.conveyorInspection);
            // Same-machine second connection must be a non-owner duplicate
            // guest; it can receive NO_OWN_CONVEYOR, never owner stock.
            try(NetLink stranger=new NetLink("127.0.0.1",server.port())){
                synchronized(server.match){
                    var guest=server.match.player(stranger.id);
                    guest.x=x;guest.y=y;
                }
                stranger.inspectConveyor();
                deadline=System.nanoTime()+5_000_000_000L;
                while(stranger.conveyorInspection==null&&System.nanoTime()<deadline)
                    Thread.sleep(20);
                check(stranger.conveyorInspection!=null&&
                    stranger.conveyorInspection.state().equals("NO_OWN_CONVEYOR"),
                    "Real TCP inspection leaked other account's conveyor existence");
            }
        }
        System.out.println("PASS ConveyorInspectionTest: private physical and TCP status, power, storage, AUTO deficits and V11 restart");
    }
}
