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
    /** Position player beside a visually empty but protected CITY plot. */
    private static void aimAtVacant(ArenaSystems game,int id) {
        ArenaSystems.Player p=game.player(id);
        for(CityLayout.Plot lot:game.world.city.plots()) {
            if(lot.template().category()!=CityLayout.Category.OPEN||lot.template().variant()!=2)continue;
            double tx=(lot.x()+lot.right())*CityLayout.CELL/2.0+.5;
            double ty=(lot.y()+lot.bottom())*CityLayout.CELL/2.0+.5;
            double px=tx-1.1;
            if(!game.world.canWalk(px,ty)||!game.world.canWalk(tx,ty))continue;
            p.x=px;p.y=ty;p.angle=0;p.moveAt=game.time();
            game.refreshVisibility(p);return;
        }
        throw new AssertionError("No accessible empty city lot on seed "+game.world.seed);
    }
    private static World.Device historical(ArenaSystems game,int id,int kind){
        // Existing-structure fixture only; the player build command remains prohibited.
        aimAtVacant(game,id);
        ArenaSystems.Player p=game.player(id);
        World.Device device=new World.Device(p.x+1.1,p.y,kind);
        device.ownerId=id;device.ownerKey=p.accountKey;
        game.world.devices.add(device);
        return device;
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
        check(shooter.visibility<=.20,"Shade lowers PvP opacity while sprinting");
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
        shooter.cash=500;shooter.scrap=30;
        moveTo(host,1,14.5,1.5);shooter.angle=0;
        check(host.reviewBuild(1,ArenaSystems.TURRET).reason().equals("CITY_PROTECTED"),
            "city street is immutable");
        aimAtVacant(host,1);
        for(int kind=0;kind<=3;kind++){
            check(host.reviewBuild(1,kind).reason().equals("CITY_PROTECTED"),
                "all four structure classes forbidden in vacant city lot");
            check(!host.build(1,kind),"no kind can bypass city protection");
        }
        check(host.world.devices.isEmpty()&&shooter.cash==500&&shooter.scrap==30,
            "city building cannot create devices or charge money");
        // Preserve existing healing/ability behavior tests with historic fixture
        // data, NOT new constructible city devices.
        historical(host,1,ArenaSystems.TURRET);
        World.Device healer=historical(host,1,ArenaSystems.HEALTH_STATION);
        moveTo(host,1,healer.x,healer.y);
        shooter.hp=40;shooter.sanity=50;
        advance(host,500);
        check(shooter.hp>=43&&shooter.sanity>=52,"legacy medical station still heals");
        historical(host,1,ArenaSystems.REVIVE_STATION);
        historical(host,1,ArenaSystems.BARRICADE);
        check(host.world.devices.size()==4,"legacy environment fixture count");
        check(host.world.devices.get(3).kind==ArenaSystems.BARRICADE,"legacy barricade kind");
        shooter.cash=0;shooter.scrap=0;
        check(!host.build(1,ArenaSystems.TURRET),"city build denied regardless of resources");
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
        aimAtVacant(turretHost,3);
        check(!turretHost.build(3,ArenaSystems.TURRET),
            "test turret cannot be player-built inside city");
        World.Device turret=historical(turretHost,3,ArenaSystems.TURRET);
        ArenaSystems.Player intruder=turretHost.join(4);
        moveTo(turretHost,4,turret.x,turret.y+1.1);
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
        System.out.println("PASS ArenaSystemsTest: "+checks+" assertions across PvP, immutable city, historic device effects, respawn");
    }
}
