package veil;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

/** Account V1 migration and V2 durable player survival state, without
 * fabricating cargo or placing account-owned data on temporary guest peers. */
public final class SurvivalPersistenceTest {
    private static void check(boolean test,String reason){
        if(!test)throw new AssertionError(reason);
    }
    private static String key(){return "acct_"+"a".repeat(64);}
    private static void cleanup(Path root)throws IOException{
        try(var walk=Files.walk(root)){
            for(Path p:walk.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);
        }
    }
    /** Minimal valid historical V1 directory: zero account/skill/faction/crew
     * records, next crew number one, and legacy checksum. */
    private static byte[] legacy(long seed)throws IOException{
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(buffer)){
            out.writeInt(0x56414331);out.writeInt(1);out.writeLong(seed);
            out.writeBoolean(false);
            out.writeInt(0);out.writeInt(0);out.writeInt(0);
            out.writeLong(1);out.writeInt(0);
        }
        CRC32 crc=new CRC32();crc.update(buffer.toByteArray());
        try(DataOutputStream out=new DataOutputStream(buffer)){
            out.writeLong(crc.getValue());
        }
        return buffer.toByteArray();
    }
    public static void main(String[] args)throws Exception{
        final long seed=20261026L;
        Path root=Files.createTempDirectory("veil-account-survival-");
        try {
            ArenaSystems first=new ArenaSystems(seed);
            ArenaSystems.Player p=first.join(1);
            check(first.assignAccount(1,key()),"Known authenticated account not bound");
            check(p.survival.acquire(SurvivalCrafting.Item.TIMBER,5,false),
                "Hand-harvested material missing");
            check(p.survival.acquire(SurvivalCrafting.Item.FIBER,4,false),
                "Hand-harvested fiber missing");
            check(p.survival.acquire(SurvivalCrafting.Item.EDIBLES,2,false),
                "Carried food missing");
            p.survival.advance(60_000);
            SurvivalCrafting.Status before=p.survival.status();
            check(before.hunger()==99&&before.hydration()==98,"Test needs not initialized");
            AccountDirectory.save(first,root);
            ArenaSystems second=new ArenaSystems(seed);
            check(AccountDirectory.load(second,root),"V2 directory did not load");
            ArenaSystems.Player after=second.join(1);
            check(second.assignAccount(1,key()),"Saved player identity not linked");
            check(after.survival.status().equals(before),
                "Backpack, hunger or hydration lost across server restart");

            check(second.craftHand(1,"tent"),"Real carried materials cannot craft after load");
            check(after.survival.count(SurvivalCrafting.Item.TENT_KIT)==1&&
                after.survival.count(SurvivalCrafting.Item.TIMBER)==3&&
                after.survival.count(SurvivalCrafting.Item.FIBER)==0,
                "Saved inventory spent incorrect recipe amounts");
            AccountDirectory.save(second,root);
            ArenaSystems third=new ArenaSystems(seed);
            check(AccountDirectory.load(third,root),"Updated inventory did not load");
            third.join(1);check(third.assignAccount(1,key()),"Identity after second restart");
            check(third.player(1).survival.count(SurvivalCrafting.Item.TENT_KIT)==1&&
                third.player(1).survival.count(SurvivalCrafting.Item.FIBER)==0,
                "Restart duplicated consumed resources");
            third.damage(third.player(1),1000,0);
            check(third.player(1).survival.inventory().isEmpty(),
                "Physical backpack persisted after a killing blow");
            AccountDirectory.save(third,root);
            ArenaSystems last=new ArenaSystems(seed);
            check(AccountDirectory.load(last,root),"Post-death account save cannot load");
            last.join(1);check(last.assignAccount(1,key()),"Post-death account not bound");
            check(last.player(1).survival.inventory().isEmpty(),
                "Backpack resurrected from disk after normal death");
            check(last.player(1).survival.status().hunger()==before.hunger()&&
                last.player(1).survival.status().hydration()==before.hydration(),
                "Survival vital meters unexpectedly reset on death/restart");

            Path old=Files.createDirectory(root.resolve("v1"));
            Files.write(old.resolve("accounts.dat"),legacy(seed));
            ArenaSystems migration=new ArenaSystems(seed);
            check(AccountDirectory.load(migration,old),"Valid V1 accounts rejected");
            check(migration.survivalSnapshot().isEmpty(),"V1 minted ghost inventory");
            AccountDirectory.save(migration,old); // explicitly upgrade to V2
            check(AccountDirectory.load(new ArenaSystems(seed),old),
                "V1-to-V2 converted directory failed readback");
        }finally{cleanup(root);}
        System.out.println("PASS SurvivalPersistenceTest: V2 inventories and needs, no restart duplication, physical death loss, V1 compatibility");
    }
}
