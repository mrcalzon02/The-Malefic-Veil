package veil;

import java.nio.file.*;
import java.util.*;

/** Narrow migration acceptance: actual owned gun IDs survive save/restart and
 * enter one non-recoverable death receipt. This is NOT corpse escrow/retrieval. */
public final class DeathWeaponIdentityCheckpointTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void wipe(Path root)throws Exception{
        try(var files=Files.walk(root)){
            for(Path p:files.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);
        }
    }
    private static ArenaSystems restore(Path root,long seed,String owner)throws Exception{
        ArenaSystems arena=new ArenaSystems(seed);
        check(HostCheckpoint.load(arena,root),"Paired checkpoint rejected");
        arena.join(1);
        check(arena.assignAccount(1,owner),"Saved account not assigned");
        return arena;
    }
    public static void main(String[] args)throws Exception{
        long seed=20261115L;
        String owner="acct_"+"a".repeat(64);
        Path root=Files.createTempDirectory("veil-death-instance-");
        try{
            ArenaSystems first=new ArenaSystems(seed);
            first.join(1);
            check(first.assignAccount(1,owner),"First authenticated account binding");
            Map<Integer,String> carried=first.arsenal.ownedItemInstances(1);
            check(carried.size()==1&&carried.containsKey(0),"Starter gun lacks unique item ID");
            String firstId=carried.get(0);
            check(firstId.matches("[0-9a-f-]{36}"),"Invalid authoritative gun identity");

            HostCheckpoint.save(first,root);
            ArenaSystems alive=restore(root,seed,owner);
            check(alive.arsenal.ownedItemInstances(1).equals(carried),
                "Committed carried gun changed identity on restart");
            alive.advance(3000);
            ArenaSystems.Player p=alive.player(1);
            alive.damage(p,1000,0);
            check(p.dead,"Fatal combat was not applied");
            ArenaSystems.DeathLossAudit lost=alive.lastDeathLoss(owner);
            check(lost!=null&&lost.sequence()==1&&
                lost.lostWeaponInstances().equals(carried),
                "Death audit did not record the exact stolen gun identity");
            check(alive.arsenal.ownedItemInstances(1).isEmpty(),
                "The same weapon identity remained in carried inventory after death");
            alive.damage(p,1000,0);
            check(lost.equals(alive.lastDeathLoss(owner)),
                "Repeated fatal damage produced a second receipt");

            HostCheckpoint.save(alive,root);
            ArenaSystems dead=restore(root,seed,owner);
            check(lost.equals(dead.lastDeathLoss(owner))&&
                dead.arsenal.ownedItemInstances(1).isEmpty(),
                "Paired checkpoint restored a lost gun or changed death item ID");
            dead.advance(2000);
            check(dead.respawn(1,0),"Respawn was rejected");
            check(dead.arsenal.ownedItemInstances(1).isEmpty(),
                "Respawn granted a free copy of the lost weapon");
            ArenaSystems.Player returned=dead.player(1);
            int pickup=-1;
            for(int i=0;i<dead.arsenal.loot.size();i++){
                var item=dead.arsenal.loot.get(i);
                if(item.type==0&&!item.taken){pickup=i;break;}
            }
            check(pickup>=0,"Map weapon absent");
            var item=dead.arsenal.loot.get(pickup);
            returned.x=item.x;returned.y=item.y;
            check(dead.arsenal.pickup(1,pickup).ok(),"Legitimate weapon pickup rejected");
            Map<Integer,String> second=dead.arsenal.ownedItemInstances(1);
            check(second.size()==1&&!second.containsValue(firstId),
                "Second physical gun recycled the permanently lost instance");
            dead.advance(3000);
            dead.damage(returned,1000,0);
            check(returned.dead,"Second fatal combat rejected");
            ArenaSystems.DeathLossAudit next=dead.lastDeathLoss(owner);
            check(next.sequence()==2&&next.lostWeaponInstances().equals(second)&&
                !next.eventId().equals(lost.eventId())&&
                dead.arsenal.ownedItemInstances(1).isEmpty(),
                "Second distinct death duplicated or refunded the first gun");
            HostCheckpoint.save(dead,root);
            ArenaSystems finalWorld=restore(root,seed,owner);
            check(finalWorld.lastDeathLoss(owner).equals(next)&&
                finalWorld.arsenal.ownedItemInstances(1).isEmpty(),
                "Latest exact item-loss receipt did not survive checkpoint");
            System.out.println("PASS DeathWeaponIdentityCheckpointTest: gun UUID, death ID, retry, empty respawn, new pickup, second loss, paired restart");
        }finally{wipe(root);}
    }
}
