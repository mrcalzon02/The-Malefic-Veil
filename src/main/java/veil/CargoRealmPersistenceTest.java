package veil;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Cargo and yard depletion survive a real checksummed save/restart, no material minting. */
public final class CargoRealmPersistenceTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static CargoHaulage.Site park(ArenaSystems game){
        game.join(10);
        ArenaSystems.Player p=game.player(10);
        for(var site:game.cargo.sites()){
            for(int dx=-4;dx<=4;dx++)for(int dy=-4;dy<=4;dy++){
                if(Math.hypot(dx,dy)>4||Math.hypot(dx,dy)<1.3)continue;
                double x=site.x()+.5+dx,y=site.y()+.5+dy;
                if(game.world.lineClear(site.x()+.5,site.y()+.5,x,y)&&
                    game.vehicles.restore(0,x,y,0,90,200)){
                    p.x=site.x()+.5;p.y=site.y()+.5;
                    return site;
                }
            }
        }
        throw new AssertionError("No valid resource depot and truck frontage");
    }
    public static void main(String[] args)throws Exception {
        Path root=Files.createTempDirectory("malefic-cargo-realm-");
        try{
            ArenaSystems game=new ArenaSystems(42L);
            var yard=park(game);
            check(game.loadTruck(10,0,yard.id()),"Cargo loaded");
            game.cargo.advance(10_000);
            check(game.cargo.sites().get(yard.id()).stock()==0,"Depo emptied");
            DurableRealm.save(game,root);
            byte[] stored=Files.readAllBytes(DurableRealm.file(root));
            ArenaSystems loaded=new ArenaSystems(42L);
            check(DurableRealm.load(loaded,root),"V2 cargo realm accepted");
            check(yard.resource().equals(loaded.vehicles.cargo(0).resourceType()),
                "Truck lost load after restart");
            check(loaded.cargo.sites().get(yard.id()).stock()==0,
                "Restart spawned free cargo from depleted source yard");
            check(loaded.cargo.time()==10_000,"Refill timer base lost");
            check(!loaded.cargo.load(99,yard.x()+.5,yard.y()+.5,0,yard.id()),
                "No cargo duplication from depleted source");
            check(loaded.vehicles.damageCar(0,125),"Damage parked cargo truck");
            loaded.cargo.advance(0);
            check(loaded.vehicles.cargo(0).resourceType()==null,"Cargo failed ejection after restart");
            check(loaded.cargo.drops().size()==1,"Damaged cargo must survive as a single pickup");
            DurableRealm.save(loaded,root);
            ArenaSystems dropped=new ArenaSystems(42L);
            check(DurableRealm.load(dropped,root),"V2 wreck realm accepted");
            check(dropped.cargo.drops().size()==1&&dropped.cargo.drops().get(0).resource().equals(yard.resource()),
                "Dropped cargo did not survive restart");
            byte[] v2=Files.readAllBytes(DurableRealm.file(root));
            // A byte flip in the newer cargo section must be detected by CRC.
            v2[v2.length-16]^=1;
            Files.write(DurableRealm.file(root),v2);
            ArenaSystems backup=new ArenaSystems(42L);
            check(DurableRealm.load(backup,root),"Validated backup recovered from corrupt cargo overlay");
            check(backup.cargo.sites().get(yard.id()).stock()==0,
                "Backup recovery minted depleted yard cargo");
            System.out.println("PASS CargoRealmPersistenceTest: v2 truck load, yard cooldown, drop and CRC recovery");
        }finally{
            try(var files=Files.walk(root)) {
                files.sorted(Comparator.reverseOrder()).forEach(path->{
                    try{Files.deleteIfExists(path);}catch(IOException ignored){}
                });
            }
        }
    }
}
