package veil;

import java.awt.*;
import java.awt.image.BufferedImage;

/** Deterministic full-host rules test, no Swing window or remote services needed. */
public final class ArenaSystemsTest {
    private static int checks;
    private static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    private static void advance(ArenaSystems a,long total){
        for(long t=0;t<total;t+=100)a.advance(100);
    }
    private static void isolateNpcs(ArenaSystems a){a.world.npcs.clear();}
    private static void moveTo(ArenaSystems a,int id,double x,double y) {
        ArenaSystems.Player p=a.player(id);
        // Test harness can set spawn-state directly; public move() is separately boundary-tested.
        p.x=x;p.y=y;p.moveAt=a.time();a.refreshVisibility(p);
    }
    public static void main(String[] args){
        ArenaSystems host=new ArenaSystems(42);
        isolateNpcs(host);
        ArenaSystems.Player shooter=host.join(1),target=host.join(2);
        host.choose(1,2);host.choose(2,1);
        check(shooter.visibility>.5,"daylight visibility");
        check(ArenaSystems.opacity(0.21)==.21,"remote 21% alpha");
        check(Math.abs(ArenaSystems.transparencyPercent(.21)-79)<.001,"79% transparent character");
        check(!host.move(2,120,120,0,0),"teleport rejection");
        check(!host.move(2,Double.NaN,3,0,0),"nonfinite coordinates rejected");
        moveTo(host,2,4.5,1.5);
        check(host.move(1,1.5,1.5,0,1),"crouch stance accepted");
        double crouch=shooter.visibility;
        check(host.move(1,1.5,1.5,0,2),"sprint stance accepted");
        check(shooter.visibility>crouch,"crouch is more transparent than sprint");
        host.advance(100);
        check(host.ability(1),"shade ability");
        check(shooter.visibility<.1,"shade makes player near invisible to PvP");
        check(!host.ability(1),"ability cooldown");
        check(host.fire(1,0),"pistol shot accepted");
        check(target.hp==75,"host owns real PvP HP loss");
        check(shooter.pistolClip==11,"host owns fired ammo");
        check(!host.fire(1,0),"shot cooldown rejected");
        host.damage(target,20,14);
        int damaged=target.hp;
        advance(host,4000);
        check(target.hp==damaged,"no early passive regen");
        advance(host,2000);
        check(target.hp>damaged,"post-combat regen");
        int before=target.hp;
        check(host.useMedkit(2),"medkit usable");
        check(target.hp>before&&target.medkits==1,"medkit heals and consumes one");
        check(!host.reload(1,3),"invalid weapon rejected");
        shooter.pistolClip=2;
        check(host.reload(1,0),"reload");
        check(shooter.pistolClip==12&&shooter.pistolReserve==38,"ammo supply bookkeeping");
        World.Pickup item=host.world.pickups.get(0);
        check(!host.pickup(1,0),"distant pickup rejected");
        moveTo(host,1,item.x,item.y);
        shooter.hp=45;
        check(host.pickup(1,0),"nearby pickup");
        check(shooter.hp==75&&item.taken,"health pickup restored 30 HP");
        check(!host.pickup(2,0),"pickup cannot be duplicated");
        moveTo(host,1,1.5,1.5);
        shooter.cash=500;shooter.scrap=30;
        shooter.angle=0;
        check(host.build(1,ArenaSystems.TURRET),"turret assembly");
        check(host.world.devices.size()==1&&shooter.cash==400&&shooter.scrap==27,"turret costs");
        check(!host.build(1,ArenaSystems.TURRET),"deny overlapping structure");
        shooter.angle=Math.PI/2;
        check(host.build(1,ArenaSystems.HEALTH_STATION),"health station assembly");
        check(host.world.devices.size()==2,"two real structures");
        shooter.hp=40;shooter.sanity=50;
        advance(host,500);
        check(shooter.hp>=43&&shooter.sanity>=52,"medical station restores health and sanity");
        moveTo(host,1,6.5,1.5);
        shooter.angle=Math.PI;
        check(host.build(1,ArenaSystems.REVIVE_STATION),"revive station assembly");
        shooter.angle=Math.PI/2;
        check(host.build(1,ArenaSystems.BARRICADE),"barricade structure assembly");
        check(host.world.devices.size()==4,"four distinct devices committed to world");
        check(host.world.devices.get(3).kind==ArenaSystems.BARRICADE,"barricade kind");
        shooter.cash=0;shooter.scrap=0;
        check(!host.build(1,ArenaSystems.TURRET),"insufficient resources cannot build");
        host.damage(target,200,60);
        check(target.dead&&target.hp==0,"fatal damage");
        check(!host.respawn(2,2),"respawn lockout");
        advance(host,1300);
        check(host.respawn(2,2),"spawn select accepted");
        check(target.hp==100&&!target.dead,"respawn resets state");
        check(Math.abs(target.x-host.world.spawns[2][0])<.01,"selected spawn used");
        check(!host.respawn(2,2),"living player cannot respawn");
        // Turrets must actually attack targets, rather than merely exist.
        ArenaSystems turretHost=new ArenaSystems(123);isolateNpcs(turretHost);
        ArenaSystems.Player builder=turretHost.join(3);
        builder.cash=1000;builder.scrap=20;builder.angle=0;
        check(turretHost.build(3,ArenaSystems.TURRET),"test turret built");
        ArenaSystems.Player intruder=turretHost.join(4);
        moveTo(turretHost,4,4.5,1.5);
        int old=intruder.hp;
        advance(turretHost,100);
        check(intruder.hp<old,"host turret deals actual damage");
        // Remote draw opacity must actually blend pixel colour.
        BufferedImage bg=new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
        bg.setRGB(0,0,0xff000000);
        Graphics2D g=bg.createGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)ArenaSystems.opacity(.25)));
        g.setColor(Color.WHITE);g.fillRect(0,0,1,1);g.dispose();
        int blended=bg.getRGB(0,0)&255;
        check(blended>=62&&blended<=65,"25% visible draws as alpha-composited sprite");
        System.out.println("PASS ArenaSystemsTest: "+checks+" assertions across visibility, PvP, regen, healing, pickups, ammunition, turrets, structures, respawn");
    }
}
