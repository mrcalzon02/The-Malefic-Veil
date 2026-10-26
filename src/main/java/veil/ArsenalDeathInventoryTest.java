package veil;

import java.util.Arrays;

/** Host-verified physical arsenal loss: death, respawn, save/load and rearm.
 * This is a functional inventory and combat test, not a visual snapshot. */
public final class ArsenalDeathInventoryTest {
    private ArsenalDeathInventoryTest(){}
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private static int loot(ArenaSystems arena,int type,int value){
        for(int i=0;i<arena.arsenal.loot.size();i++){
            ArsenalSystems.Loot item=arena.arsenal.loot.get(i);
            if(item.type==type&&item.value==value&&!item.taken)return i;
        }
        throw new AssertionError("Missing required honest map pickup "+type+"/"+value);
    }
    private static void acquire(ArenaSystems arena,int id,int index){
        ArsenalSystems.Loot item=arena.arsenal.loot.get(index);
        ArenaSystems.Player player=arena.player(id);
        player.x=item.x;player.y=item.y;
        ArsenalSystems.Result result=arena.arsenal.pickup(id,index);
        check(result.ok(),"Host rejected physical map pickup: "+result.reason());
    }
    private static void scenario(boolean hardcore)throws Exception{
        final long seed=20261023L;
        final String account="acct_"+(hardcore?"b":"a").repeat(64);
        ArenaSystems arena=new ArenaSystems(seed,hardcore);
        ArenaSystems.Player player=arena.join(1);
        check(arena.assignAccount(1,account),"Persistent identity missing");
        check(arena.enrollFaction(1,CharacterRoster.Faction.THE_UNRAVELED),
            "Valid faction enrollment rejected");
        check((arena.arsenal.view(1).owned()&1L)!=0,"Current starter Vesper not recorded");

        // Pick up an additional real map gun and manufacturing materials.
        acquire(arena,1,loot(arena,0,1));
        acquire(arena,1,loot(arena,1,1));
        check(arena.arsenal.select(1,1).ok(),"Host could not equip looted weapon");
        ArsenalSystems.View equipped=arena.arsenal.view(1);
        check((equipped.owned()&3L)==3L&&equipped.materials()[1]>0,
            "Physical carried weapons/materials absent before killing blow");

        arena.damage(player,1000,0);
        check(player.dead,"Combat death not registered");
        ArsenalSystems.View fallen=arena.arsenal.view(1);
        check(fallen.owned()==0&&Arrays.stream(fallen.clip()).allMatch(n->n==0)&&
            Arrays.stream(fallen.reserve()).allMatch(n->n==0)&&
            Arrays.stream(fallen.materials()).allMatch(n->n==0),
            "Physical arsenal survived death in "+(hardcore?"hardcore":"normal")+" mode");
        check(player.cash==0&&player.scrap==0&&player.survival.inventory().isEmpty(),
            "Death retained legacy or survival physical items");

        arena.advance(1000);arena.advance(200);
        check(arena.respawn(1,0),"Mandatory respawn could not complete");
        check(arena.arsenal.view(1).owned()==0,"Respawn granted a free gun");
        check(!arena.arsenal.reload(1).ok(),"Disarmed player reloaded absent gun");
        ArsenalSystems.Shot shot=arena.arsenal.fire(1,false,1);
        check(!shot.fired()&&shot.reason().equals("NOT_OWNED"),
            "Player shot a weapon not in the physical inventory");

        // A dead character's durable arsenal is legitimately empty, not corrupt.
        byte[] saved=arena.arsenal.save();
        ArenaSystems loaded=new ArenaSystems(seed,hardcore);
        loaded.arsenal.restore(saved,true);
        loaded.join(1);
        check(loaded.assignAccount(1,account),"Saved account failed to bind");
        check(loaded.arsenal.view(1).owned()==0,
            "Reconnect or reload silently reissued default weapon");

        // Loose ammunition is not a phantom weapon or a valid permission to fire.
        acquire(loaded,1,loot(loaded,2,0));
        shot=loaded.arsenal.fire(1,false,1);
        check(!shot.fired()&&shot.reason().equals("NOT_OWNED"),
            "Collected ammo created an invisible gun");
        check(!loaded.arsenal.reload(1).ok(),"Ammunition alone enabled reload");

        // The player can recover naturally through a still-unclaimed loot weapon.
        acquire(loaded,1,loot(loaded,0,0));
        check(loaded.arsenal.select(1,0).ok(),"Legitimate recovered gun cannot equip");
        check((loaded.arsenal.view(1).owned()&1L)!=0,
            "Rearm did not restore a legitimately collected weapon");
        shot=loaded.arsenal.fire(1,false,2);
        check(shot.fired(),"Recovered physical gun cannot fire: "+shot.reason());
    }
    public static void main(String[] args)throws Exception{
        scenario(false);
        scenario(true);
        System.out.println("PASS ArsenalDeathInventoryTest: normal/hardcore wipe, disarmed respawn, persistence, no ghost firing, legitimate recovery");
    }
}
