package veil;

import java.nio.file.*;
import java.util.*;

/** Real crash recovery: a unit exists in exactly one of the two ledgers
 * after a commit, and torn snapshots never combine generations. */
public final class HostCheckpointTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void wipe(Path root)throws Exception{
        try(var files=Files.walk(root))
            {for(Path p:files.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);}
    }
    private static ArenaSystems read(Path root,long seed,String key)throws Exception{
        ArenaSystems restored=new ArenaSystems(seed);
        check(HostCheckpoint.load(restored,root),"No committed world/account pair");
        restored.join(1);
        check(restored.assignAccount(1,key),"Authenticated account not recoverable");
        return restored;
    }
    private static int total(ArenaSystems host,SurvivalCrafting.Item kind){
        int result=host.player(1).survival.count(kind);
        for(World.Device d:host.world.devices)
            if(d.kind==ArenaSystems.STORAGE_CRATE)result+=d.storage.count(kind);
        return result;
    }
    public static void main(String[] args)throws Exception{
        long seed=20261026L;
        String key="acct_"+"f".repeat(64);
        Path root=Files.createTempDirectory("veil-paired-host-");
        try{
            ArenaSystems host=new ArenaSystems(seed);
            ArenaSystems.Player owner=host.join(1);
            check(host.assignAccount(1,key),"Account binding");
            check(owner.survival.acquire(SurvivalCrafting.Item.TIMBER,8,false),
                "Cannot harvest checkpoint resource");
            // The world object is a test fixture, not an in-game structure
            // minting path; the dedicated camp construction test covers that.
            double siteX=Double.NaN,siteY=Double.NaN;
            for(int x=World.SIZE+3;x<World.SIZE+50&&!Double.isFinite(siteX);x++)
                for(int y=2;y<World.SIZE+40;y++)
                    if(host.world.canWalk(x,y)&&!host.world.worldStructureAt(x,y,.55)){
                        siteX=x;siteY=y;break;
                    }
            check(Double.isFinite(siteX),"No legal wilderness fixture location");
            World.Device crate=new World.Device(siteX,siteY,ArenaSystems.STORAGE_CRATE);
            crate.ownerKey=key;
            host.world.devices.add(crate);
            owner.x=siteX+1.2;owner.y=siteY;
            check(host.transferCampStock(1,false,SurvivalCrafting.Item.TIMBER,3),
                "Initial physical ledger transfer failed");
            check(total(host,SurvivalCrafting.Item.TIMBER)==8,"Host gained phantom supply");
            HostCheckpoint.save(host,root);
            check(Files.isRegularFile(root.resolve("host-ledger.current")),
                "No generation pointer committed");
            ArenaSystems first=read(root,seed,key);
            check(first.player(1).survival.count(SurvivalCrafting.Item.TIMBER)==5&&
                first.world.devices.stream().filter(d->d.kind==ArenaSystems.STORAGE_CRATE)
                    .mapToInt(d->d.storage.count(SurvivalCrafting.Item.TIMBER)).sum()==3,
                "The first committed pair was torn");

            // Transfer from the stored physical world to a carried account:
            // both records MUST advance to the same committed generation.
            check(host.transferCampStock(1,true,SurvivalCrafting.Item.TIMBER,2),
                "Crate withdrawal failed");
            HostCheckpoint.save(host,root);
            ArenaSystems second=read(root,seed,key);
            check(second.player(1).survival.count(SurvivalCrafting.Item.TIMBER)==7&&
                second.world.devices.stream().filter(d->d.kind==ArenaSystems.STORAGE_CRATE)
                    .mapToInt(d->d.storage.count(SurvivalCrafting.Item.TIMBER)).sum()==1,
                "Next checkpoint did not include both sides of transfer");
            check(total(second,SurvivalCrafting.Item.TIMBER)==8,
                "Committed transfer minted or deleted physical supplies");

            // Simulate a power loss during a third save. This unreferenced
            // stage must never be mistaken for a committed replacement.
            Path crashed=Files.createDirectory(root.resolve("ledger-stage-incomplete"));
            Files.writeString(crashed.resolve("realm.dat"),"unfinished");
            check(total(read(root,seed,key),SurvivalCrafting.Item.TIMBER)==8,
                "Incomplete save stage changed the ledger head");

            String pointer=Files.readString(root.resolve("host-ledger.current"));
            String directory=pointer.substring(0,pointer.indexOf('|'));
            Files.writeString(root.resolve(directory).resolve("accounts.dat"),
                "corrupted-incomplete-account-file");
            ArenaSystems recovered=read(root,seed,key);
            check(recovered.player(1).survival.count(SurvivalCrafting.Item.TIMBER)==5&&
                recovered.world.devices.stream().filter(d->d.kind==ArenaSystems.STORAGE_CRATE)
                    .mapToInt(d->d.storage.count(SurvivalCrafting.Item.TIMBER)).sum()==3,
                "Recovery combined older physical stock with newer backpack");
            check(total(recovered,SurvivalCrafting.Item.TIMBER)==8,
                "Recovery duplicated previously withdrawn timber");
            // A pointer cannot fall through to mismatched root mirrors.
            Files.writeString(root.resolve("host-ledger.previous"),"corrupt");
            Files.writeString(root.resolve("host-ledger.current"),"also-corrupt");
            try{HostCheckpoint.load(new ArenaSystems(seed),root);
                throw new AssertionError("Double-damaged ledger silently reset");}
            catch(java.io.IOException expected){/* correct fail closed */}
        }finally{wipe(root);}
        System.out.println("PASS HostCheckpointTest: two ledgers committed together, stage ignored, previous pair recovery, no duplicated material or silent reset");
    }
}
