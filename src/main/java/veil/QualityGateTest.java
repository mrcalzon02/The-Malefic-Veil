package veil;

import java.util.*;
import java.util.function.BooleanSupplier;

/** QoL gate: server confirmations, duplicate suppression, construction rejection, route preservation. */
public final class QualityGateTest {
    private static int assertions;
    private static void check(boolean yes,String message){
        assertions++;if(!yes)throw new AssertionError(message);
    }
    private static void until(BooleanSupplier f)throws Exception{
        long deadline=System.nanoTime()+4_000_000_000L;
        while(System.nanoTime()<deadline){if(f.getAsBoolean())return;Thread.sleep(15);}
        throw new AssertionError("Timeout waiting for host packet");
    }
    /** Select an open CITY lot specifically to prove it is not buildable. */
    private static void aimAtVacant(ArenaSystems game,int id) {
        ArenaSystems.Player p=game.player(id);
        for(CityLayout.Plot lot:game.world.city.plots()){
            if(lot.template().category()!=CityLayout.Category.OPEN||lot.template().variant()!=2)continue;
            double x=(lot.x()+lot.right())*CityLayout.CELL/2.0+.5;
            double y=(lot.y()+lot.bottom())*CityLayout.CELL/2.0+.5;
            if(!game.world.canWalk(x-1.1,y)||!game.world.canWalk(x,y))continue;
            p.x=x-1.1;p.y=y;p.angle=0;p.moveAt=game.time();
            return;
        }
        throw new AssertionError("No empty city lot to test");
    }
    public static void main(String[] args)throws Exception{
        ArenaSystems world=new ArenaSystems(42);
        world.world.npcs.clear();
        ArenaSystems.Player shooter=world.join(1),victim=world.join(2);
        shooter.angle=0;victim.x=4.5;victim.y=1.5;
        ArenaSystems.ShotReport hit=world.fireReport(1,0,1);
        check(hit.fired()&&hit.status().equals("PLAYER"),"host hit must be explicit");
        check(hit.targetId()==2&&hit.damage()==25&&hit.ammo()==11,"accurate hit receipt");
        check(!world.fireReport(1,0,1).fired(),"duplicate request replay rejected");
        check(!world.fireReport(1,0,2).fired(),"cooldown reject");
        check(world.fireReport(1,0,2).status().equals("DUPLICATE"),"replay of cooldown still rejected");
        check(world.player(1).pistolClip==11,"reject costs no additional ammo");
        int money=shooter.cash,supplies=shooter.scrap;
        shooter.angle=Math.PI;
        ArenaSystems.BuildReview obstructed=world.reviewBuild(1,ArenaSystems.BARRICADE);
        check(!obstructed.allowed(),"blocked build must be rejected");
        check(!world.build(1,ArenaSystems.BARRICADE),"cannot bypass build review");
        check(shooter.cash==money&&shooter.scrap==supplies,"rejected build is free");
        shooter.x=14.5;shooter.y=1.5;shooter.angle=0;
        check(world.reviewBuild(1,ArenaSystems.BARRICADE).reason().equals("CITY_PROTECTED"),
            "streets are fixed world structures");
        aimAtVacant(world,1);
        check(world.reviewBuild(1,ArenaSystems.BARRICADE).reason().equals("CITY_PROTECTED"),
            "even empty city plots remain world-owned");
        check(!world.build(1,ArenaSystems.BARRICADE),"no player building in cities");
        check(world.world.devices.isEmpty(),"city rejects structure mutation");
        check(shooter.cash==money&&shooter.scrap==supplies,"denied city build never debits");
        for(long seed=1;seed<=20;seed++){
            ArenaSystems arena=new ArenaSystems(seed);
            arena.world.npcs.clear();
            ArenaSystems.Player builder=arena.join(42);
            builder.cash=10000;builder.scrap=1000;
            aimAtVacant(arena,42);
            check(!arena.build(42,ArenaSystems.BARRICADE),"city-lot builds denied seed "+seed);
            check(arena.world.devices.isEmpty()&&builder.cash==10000&&builder.scrap==1000,
                "denied city construction cannot mutate world/inventory seed "+seed);
        }
        // Even simultaneous host requests at the same empty lot must both fail.
        ArenaSystems contested=new ArenaSystems(42);
        ArenaSystems.Player alpha=contested.join(51),beta=contested.join(52);
        for(ArenaSystems.Player player:List.of(alpha,beta)){
            player.cash=300;player.scrap=10;
        }
        aimAtVacant(contested,51);
        beta.x=alpha.x;beta.y=alpha.y;beta.angle=alpha.angle;
        java.util.concurrent.CountDownLatch start=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var first=pool.submit(()->{start.await();return contested.buildOutcome(51,ArenaSystems.TURRET);});
            var second=pool.submit(()->{start.await();return contested.buildOutcome(52,ArenaSystems.TURRET);});
            start.countDown();
            ArenaSystems.BuildOutcome one=first.get(),two=second.get();
            check(!one.built()&&!two.built(),"city protects against simultaneous requests");
            check(one.review().reason().equals("CITY_PROTECTED")&&
                two.review().reason().equals("CITY_PROTECTED"),"both get city feedback");
            check(contested.world.devices.isEmpty(),"no phantom construction");
            check(alpha.cash==300&&beta.cash==300&&alpha.scrap==10&&beta.scrap==10,
                "concurrent rejection never charges either client");
        } finally {pool.shutdownNow();}
        try(LanServer server=new LanServer(0,999L);
            NetLink a=new NetLink("127.0.0.1",server.port());
            NetLink b=new NetLink("127.0.0.1",server.port())){
            server.match.world.npcs.clear();
            for(int step=0;step<11;step++)server.match.advance(1000);
            ArenaSystems.Player target=server.match.player(b.id);
            target.x=4.5;target.y=1.5;
            server.match.player(a.id).angle=0;
            long seq=a.fire(0);
            until(()->!a.shots.isEmpty());
            NetLink.ShotEvent result=a.shots.poll();
            check(result.sequence()==seq&&result.fired()&&result.damage()==25,"real socket shot receipt");
            check(result.targetId()==b.id,"host returns true victim ID");
            ArenaSystems.Player actor=server.match.player(a.id);
            actor.angle=Math.PI;
            a.build(ArenaSystems.BARRICADE);
            until(()->!a.buildResults.isEmpty());
            NetLink.BuildEvent denied=a.buildResults.poll();
            check(!denied.reason().equals("OK"),"real socket rejection reason");
        }
        VehicleTest.main(new String[0]);
        HostPersistenceTest.main(new String[0]);
        DedicatedHostTest.main(new String[0]);
        RaidCombatTest.main(new String[0]);
        ContinentPopulationTest.main(new String[0]);
        ContinentVehicleSitesTest.main(new String[0]);
        SettlementAuthorityTest.main(new String[0]);
        AntiAirBalanceTest.main(new String[0]);
        CityMiasmaTest.main(new String[0]);
        CraftingEraRegistryTest.main(new String[0]);
        CrudeStructureIndexTest.main(new String[0]);
        EquipmentUpgradesTest.main(new String[0]);
        SettlementSupplyBalanceTest.main(new String[0]);
        FactionTradeNetworksTest.main(new String[0]);
        CraftingIconPacksTest.main(new String[0]);
        InteractionFocusTest.main(new String[0]);
        WorldHarvestingTest.main(new String[0]);
        FactionAiDirectorTest.main(new String[0]);
        FactionAiTacticsTest.main(new String[0]);
        FactionAiCooperationTest.main(new String[0]);
        HostAiNetworkTest.main(new String[0]);
        HostAiRealmRestartTest.main(new String[0]);
        AirPowerMeshesTest.main(new String[0]);
        AircraftShowroomTest.main(new String[0]);
        System.out.println("PASS QualityGateTest "+assertions+
            " checks: host-confirmed hits, anti-replay, immutable cities, 20 map seeds");
    }
}
