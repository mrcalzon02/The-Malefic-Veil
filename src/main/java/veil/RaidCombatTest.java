package veil;

import java.util.List;

/** Live PvP shot path: online damage, logout grace, protected receipt and durable HP. */
public final class RaidCombatTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void aimAtVacant(ArenaSystems game,int ownerId){
        ArenaSystems.Player owner=game.player(ownerId);
        for(CityLayout.Plot plot:game.world.city.plots()){
            if(plot.template().category()!=CityLayout.Category.OPEN||plot.template().variant()!=2)continue;
            double x=(plot.x()+plot.right())*CityLayout.CELL/2.0+.5;
            double y=(plot.y()+plot.bottom())*CityLayout.CELL/2.0+.5;
            if(!game.world.canWalk(x-1.1,y)||!game.world.canWalk(x,y))continue;
            owner.x=x-1.1;owner.y=y;owner.angle=0;owner.moveAt=game.time();
            if(game.reviewBuild(ownerId,ArenaSystems.BARRICADE).allowed())return;
        }
        throw new AssertionError("No vacant test footprint");
    }
    public static void main(String[] unused){
        ArenaSystems game=new ArenaSystems(53);
        game.world.npcs.clear();
        ArenaSystems.Player owner=game.join(1),shooter=game.join(2);
        String account="acct_"+"a".repeat(64);
        check(game.assignAccount(1,account),"owner identity");
        owner.cash=1000;owner.scrap=100;
        aimAtVacant(game,1);
        check(game.build(1,ArenaSystems.BARRICADE),"server built raidable barricade");
        World.Device device=game.world.devices.get(0);
        shooter.x=owner.x;shooter.y=owner.y;shooter.angle=0;shooter.moveAt=game.time();
        // A real bullet travels to the server-owned structure (not a fabricated damage callback).
        ArenaSystems.ShotReport online=game.fireReport(2,0,1);
        check(online.fired()&&online.status().equals("DEVICE")&&online.damage()==25,
            "online opponent can inflict verified structure damage");
        check(device.hp==75,"authoritative structure HP decreased");
        game.leave(1);
        long now=System.currentTimeMillis();
        game.raidProtection.restore(List.of(new RaidProtection.Presence(account,now-31*60000,0)),now);
        check(game.raidProtection.protectedOffline(account,now),"owner protected beyond grace");
        game.advance(300);
        int previous=device.hp;
        ArenaSystems.ShotReport shielded=game.fireReport(2,0,2);
        check(shielded.fired()&&shielded.status().equals("SHIELDED")&&shielded.damage()==0,
            "protected base shot yields explicit combat receipt");
        check(device.hp==previous,"no base damage while offline shielded");
        check(shooter.pistolClip==10,"protected shots still consume ammunition");
        game.join(3);
        check(game.assignAccount(3,account),"owner reconnect claims original account");
        check(!game.raidProtection.protectedOffline(account,System.currentTimeMillis()),
            "returning owner immediately opens base to PvP");
        game.advance(300);
        ArenaSystems.ShotReport reopened=game.fireReport(2,0,3);
        check(reopened.fired()&&reopened.status().equals("DEVICE")&&reopened.damage()==25,
            "owner rejoin removes shield in actual shot path");
        System.out.println("PASS RaidCombatTest: actual PvP base damage, offline grace, shielded receipt, rejoin");
    }
}
