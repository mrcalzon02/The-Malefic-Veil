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
    public static void main(String[] unused)throws Exception{
        try(LanServer host=new LanServer(0,887766L);
            NetLink attacker=new NetLink("127.0.0.1",host.port());
            NetLink victim=new NetLink("127.0.0.1",host.port())){
            host.match.world.npcs.clear(); // isolate network PvP scenario from hostile random spawns
            check(attacker.seed==887766L&&victim.seed==887766L,"host seed");
            check(attacker.id!=victim.id,"unique IDs");
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
            shooter.cash=400;shooter.scrap=20;shooter.angle=Math.PI/2;
            attacker.build(1);
            until(()->host.match.world.devices.size()==1,"healing station built over socket");
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
            // Assemble several distinct real structures through the connected client.
            shooter.x=1.5;shooter.y=1.5;shooter.angle=0;
            attacker.build(ArenaSystems.TURRET);
            until(()->host.match.world.devices.size()==2,"turret constructed over socket");
            shooter.x=6.5;shooter.y=1.5;shooter.angle=Math.PI;
            attacker.build(ArenaSystems.REVIVE_STATION);
            until(()->host.match.world.devices.size()==3,"revival structure assembled over socket");
            shooter.angle=Math.PI/2;
            attacker.build(ArenaSystems.BARRICADE);
            until(()->host.match.world.devices.size()==4,"barricade assembled over socket");
            check(host.match.world.devices.get(3).kind==ArenaSystems.BARRICADE,"correct constructed structure kind");
            // Live turret damage occurs in the host tick, not the client.
            prey.x=4.5;prey.y=1.5;
            int beforeTurret=prey.hp;
            until(()->prey.hp<beforeTurret,"turret attacked opposing player");
            // Revive station heals its nearby dead owner after the revive countdown.
            host.match.damage(shooter,300,10);
            check(shooter.dead,"fatal wound recorded before revival");
            until(()->!shooter.dead&&shooter.hp==35,"revival station resurrected fallen player");
            attacker.respawn(0); // living actor must NOT be allowed to respawn
            Thread.sleep(150);
            check(!shooter.dead&&shooter.hp>0,"invalid respawn cannot reset living actor");
            System.out.println("PASS LanSmokeTest: host, PvP alpha/damage, regen, pickup broadcast, consumables, turret, heal/revive, barricade");
        }
    }
}
