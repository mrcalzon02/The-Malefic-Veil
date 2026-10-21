package veil;

import java.util.function.BooleanSupplier;

/** Actual loopback sockets, real protocol reader/writer and host match state. */
public final class LanSmokeTest {
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    private static void until(BooleanSupplier condition,String why)throws Exception{
        long deadline=System.nanoTime()+5_000_000_000L;
        while(System.nanoTime()<deadline){
            if(condition.getAsBoolean())return;
            Thread.sleep(20);
        }
        throw new AssertionError("Timed out: "+why);
    }
    /** Find a walkable vacant CITY location: player build must still be denied. */
    private static void aimAtVacant(ArenaSystems host,int id) {
        ArenaSystems.Player p=host.player(id);
        for(CityLayout.Plot lot:host.world.city.plots()){
            if(lot.template().category()!=CityLayout.Category.OPEN||lot.template().variant()!=2)continue;
            double x=(lot.x()+lot.right())*CityLayout.CELL/2.0+.5;
            double y=(lot.y()+lot.bottom())*CityLayout.CELL/2.0+.5;
            if(!host.world.canWalk(x-1.1,y)||!host.world.canWalk(x,y))continue;
            if(host.world.devices.stream().anyMatch(d->Math.hypot(d.x-x,d.y-y)<3.5))continue;
            p.x=x-1.1;p.y=y;p.angle=0;p.moveAt=host.time();
            return;
        }
        throw new AssertionError("No empty city lot to test on seed "+host.world.seed);
    }
    /**
     * Legacy structure fixture tests network replication, healing, revive and
     * turret behavior without exposing a production bypass to city build rules.
     */
    private static World.Device legacyDevice(ArenaSystems host,int owner,int kind){
        aimAtVacant(host,owner);
        ArenaSystems.Player player=host.player(owner);
        double x=player.x+1.1,y=player.y;
        World.Device device=new World.Device(x,y,kind);
        device.ownerId=owner;
        device.ownerKey=player.accountKey;
        host.world.devices.add(device);
        return device;
    }
    private static void deniedCityBuild(ArenaSystems host,NetLink client,int kind)throws Exception{
        aimAtVacant(host,client.id);
        int old=host.world.devices.size();
        ArenaSystems.Player player=host.player(client.id);
        int cash=player.cash,scrap=player.scrap;
        client.build(kind);
        until(()->!client.buildResults.isEmpty(),"city build rejection returned");
        NetLink.BuildEvent denied=client.buildResults.poll();
        check(denied.reason().equals("CITY_PROTECTED"),"server must reject city parcel");
        check(host.world.devices.size()==old&&player.cash==cash&&player.scrap==scrap,
            "denied building request changed resources or structures");
    }
    public static void main(String[] unused)throws Exception{
        try(LanServer host=new LanServer(0,887766L);
            NetLink attacker=new NetLink("127.0.0.1",host.port());
            NetLink victim=new NetLink("127.0.0.1",host.port())){
            host.match.world.npcs.clear(); // isolate network PvP scenario from hostile random spawns
            check(attacker.seed==887766L&&victim.seed==887766L,"host seed");
            check(attacker.id!=victim.id,"unique IDs");
            attacker.ping();
            until(()->attacker.roundTripMs()>=0,"host RTT response");
            check(attacker.connected(),"socket alive after heartbeat");
            attacker.choose(2);
            attacker.state(1.5,1.5,0,1);
            until(()->victim.others.containsKey(attacker.id),"peer joined");
            until(()->victim.others.get(attacker.id).opacity<.5,"host crouch visibility replicated");
            attacker.ability();
            until(()->victim.others.get(attacker.id).opacity<.1,"Shade PvP transparency");
            ArenaSystems.Player prey=host.match.player(victim.id);
            // Test a combat scenario with a positioned authoritative actor, not fabricated damage messages.
            prey.x=4.5;prey.y=1.5;
            attacker.fire(0);
            until(()->prey.hp<=75,"authoritative combat damage");
            until(()->victim.self()!=null&&victim.self().health()<=75,"health replicated to client");
            check(host.match.player(attacker.id).pistolClip==11,"ammo decremented by host");
            ArenaSystems.Player shooter=host.match.player(attacker.id);
            shooter.cash=400;shooter.scrap=20;
            deniedCityBuild(host.match,attacker,ArenaSystems.HEALTH_STATION);
            World.Device healing=legacyDevice(host.match,attacker.id,ArenaSystems.HEALTH_STATION);
            check(healing.ownerId==attacker.id,"legacy healing fixture has account owner");
            until(()->!attacker.structures.isEmpty()||!victim.structures.isEmpty(),"structure streamed to a client");
            host.match.damage(shooter,45,10);
            attacker.useMedkit();
            until(()->attacker.self()!=null&&attacker.self().medkits()==1,"item usage acknowledged");
            check(shooter.hp>=100,"medkit restores damaged health");
            // Actual host-controlled healing station effect, not merely a placed structure.
            host.match.damage(shooter,30,10);
            int stationWound=shooter.hp;
            until(()->shooter.hp>stationWound,"healing station restores health automatically");
            // Acquire a real shared pickup via the network command and verify visibility to the other client.
            World.Pickup healthDrop=host.match.world.pickups.get(0);
            shooter.x=healthDrop.x;shooter.y=healthDrop.y;shooter.hp=35;
            attacker.pickup(0);
            until(()->healthDrop.taken,"host pickup claimed");
            until(()->shooter.hp>=65,"health pickup restored health");
            until(()->victim.pickedUp.contains(0),"pickup removal replicated to other client");
            check(!host.match.pickup(victim.id,0),"pickup remains exclusively acquired");
            // Deny all four city build requests; independently exercise networked
            // legacy fixture effects until wilderness runtime becomes playable.
            deniedCityBuild(host.match,attacker,ArenaSystems.TURRET);
            World.Device turret=legacyDevice(host.match,attacker.id,ArenaSystems.TURRET);
            deniedCityBuild(host.match,attacker,ArenaSystems.REVIVE_STATION);
            World.Device reviver=legacyDevice(host.match,attacker.id,ArenaSystems.REVIVE_STATION);
            deniedCityBuild(host.match,attacker,ArenaSystems.BARRICADE);
            legacyDevice(host.match,attacker.id,ArenaSystems.BARRICADE);
            until(()->attacker.structures.size()>=4||victim.structures.size()>=4,
                "legacy fixtures replicated to clients");
            check(host.match.world.devices.get(3).kind==ArenaSystems.BARRICADE,"correct constructed structure kind");
            // Live turret damage occurs in the host tick, not the client.
            prey.x=turret.x;prey.y=turret.y+1.1;
            int beforeTurret=prey.hp;
            until(()->prey.hp<beforeTurret,"turret attacked opposing player");
            // Revive station heals its nearby dead owner after the revive countdown.
            shooter.x=reviver.x;shooter.y=reviver.y+1.1;
            host.match.damage(shooter,300,10);
            check(shooter.dead,"fatal wound recorded before revival");
            until(()->!shooter.dead&&shooter.hp==35,"revival station resurrected fallen player");
            attacker.respawn(0); // living actor must NOT be allowed to respawn
            Thread.sleep(150);
            check(!shooter.dead&&shooter.hp>0,"invalid respawn cannot reset living actor");
            System.out.println("PASS LanSmokeTest: host PvP, pickups, city-wide build denial, legacy structure replication/effects");
        }
    }
}
