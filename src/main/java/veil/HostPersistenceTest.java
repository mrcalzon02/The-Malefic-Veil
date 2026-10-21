package veil;

import java.nio.file.*;
import java.util.*;

/** No silent save reset: atomic overlay, backup recovery, identities and logout raid windows. */
public final class HostPersistenceTest {
    private static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
    private static void aim(ArenaSystems game,int id){
        ArenaSystems.Player p=game.player(id);
        for(CityLayout.Plot lot:game.world.city.plots()){
            if(lot.template().category()!=CityLayout.Category.OPEN||lot.template().variant()!=2)continue;
            double x=(lot.x()+lot.right())*CityLayout.CELL/2.0+.5;
            double y=(lot.y()+lot.bottom())*CityLayout.CELL/2.0+.5;
            if(!game.world.canWalk(x-1.1,y)||!game.world.canWalk(x,y))continue;
            p.x=x-1.1;p.y=y;p.angle=0;p.moveAt=game.time();
            return;
        }
        throw new AssertionError("No vacant city lot for immutable-build test");
    }
    public static void main(String[] args)throws Exception {
        RaidProtection protection=new RaidProtection(30);
        protection.login("acct_test",1000000L);
        protection.logout("acct_test",1005000L);
        check(!protection.protectedOffline("acct_test",1005000L+29*60000L),"logout cannot instantly shield base");
        check(protection.protectedOffline("acct_test",1005000L+30*60000L),"protection after grace");
        protection.login("acct_test",1005000L+31*60000L);
        check(!protection.protectedOffline("acct_test",1005000L+50*60000L),"online base vulnerable");
        protection.logout("acct_test",1005000L+32*60000L);
        RaidProtection restart=new RaidProtection(30);
        restart.restore(protection.snapshot(),1005000L+33*60000L);
        check(!restart.protectedOffline("acct_test",1005000L+33*60000L),"restart preserves remaining grace");
        check(restart.protectedOffline("acct_test",1005000L+63*60000L),"restart preserves offline protection");
        Path dir=Files.createTempDirectory("veil-realm-test-");
        try{
            String owner="acct_"+"1".repeat(64);
            ArenaSystems game=new ArenaSystems(99913);
            game.world.npcs.clear(); // avoid combat while preparing a test structure
            // Keep canonical NPC catalog for persistence validation.
            game=new ArenaSystems(99913);
            game.join(1);
            check(game.assignAccount(1,owner),"stable account ownership");
            game.player(1).cash=999;game.player(1).scrap=999;
            aim(game,1);
            check(game.reviewBuild(1,ArenaSystems.BARRICADE).reason().equals("CITY_PROTECTED"),
                "empty lot is also fixed city land");
            check(!game.build(1,ArenaSystems.BARRICADE),"persistent host must reject city building");
            check(game.world.devices.isEmpty(),"rejected city build not saved");
            // Import a legacy structure fixture to verify existing saves remain
            // recoverable. This fixture is not constructible via player actions.
            World.Device historic=new World.Device(game.player(1).x+1.1,
                game.player(1).y,ArenaSystems.BARRICADE);
            historic.ownerId=1;historic.ownerKey=owner;
            game.world.devices.add(historic);
            check(owner.equals(game.world.devices.get(0).ownerKey),
                "legacy structure ownership not tied only to transient peer ID");
            game.world.pickups.get(0).taken=true;
            game.world.npcs.get(0).hp=44;
            game.leave(1);
            DurableRealm.save(game,dir);
            DurableRealm.save(game,dir);
            check(Files.exists(DurableRealm.file(dir))&&Files.exists(dir.resolve("realm.bak")),"primary and recovery backup");
            ArenaSystems restored=new ArenaSystems(99913);
            check(DurableRealm.load(restored,dir),"load saved world");
            check(restored.world.devices.size()==1&&owner.equals(restored.world.devices.get(0).ownerKey),
                "persisted legacy player-owned structure");
            check(restored.world.pickups.get(0).taken&&restored.world.npcs.get(0).hp==44,
                "pickups and NPC mutation persist");
            restored.join(2);
            check(restored.assignAccount(2,"acct_"+"2".repeat(64)),"attacker identity");
            check(!restored.damageDevice(2,0,25,System.currentTimeMillis()+31*60000L),
                "offline protection blocks direct base damage");
            check(restored.world.devices.get(0).hp==100,"denied raid leaves base untouched");
            Files.writeString(DurableRealm.file(dir),"corrupted data");
            ArenaSystems recovered=new ArenaSystems(99913);
            check(DurableRealm.load(recovered,dir),"verified last-good backup restored after corruption");
            check(recovered.world.devices.size()==1,"backup retains imported legacy structure");
            boolean mismatch=false;
            try{DurableRealm.load(new ArenaSystems(99914),dir);}
            catch(java.io.IOException expected){mismatch=true;}
            check(mismatch,"wrong seed never silently starts a different world");
            HostModeration moderation=new HostModeration(dir);
            check(moderation.ban(owner),"ban persisted");
            check(!moderation.permitted(owner),"ban blocks account");
            moderation.setAllowlist(true);
            check(moderation.allow("acct_"+"2".repeat(64)),"allowlist account stored");
            HostModeration next=new HostModeration(dir);
            check(next.allowlistEnabled()&&!next.permitted(owner)&&next.permitted("acct_"+"2".repeat(64)),
                "moderation changes survive restart");
        }finally{
            try(var paths=Files.walk(dir)){
                paths.sorted(Comparator.reverseOrder()).forEach(path->{
                    try{Files.deleteIfExists(path);}catch(java.io.IOException ignored){}
                });
            }
        }
        System.out.println("PASS HostPersistenceTest: logout timers, checksummed saves, backup recovery, seed guard, moderation");
    }
}
