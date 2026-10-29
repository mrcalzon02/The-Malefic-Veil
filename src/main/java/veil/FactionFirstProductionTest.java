package veil;

import java.nio.file.*;
import java.util.*;

/** First live recipes across independent faction workstations. Host validates
 * ownership, faction, physical ingredient debits, distinct operating conditions,
 * timed output, crash-safe realm/account readback and no replay. */
public final class FactionFirstProductionTest {
    static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    static void advance(ArenaSystems match,int seconds){
        for(int n=0;n<seconds;n++)match.advance(1000);
    }
    static void cleanup(Path root)throws Exception{
        try(var stream=Files.walk(root))
            {for(Path p:stream.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
    }
    static double[] site(ArenaSystems match){
        for(int x=-130;x<=230;x+=3)for(int y=-130;y<=230;y+=3){
            if(match.world.detailedCity(x,y)||!match.world.canWalk(x,y)||
               match.world.worldStructureAt(x,y,.55))continue;
            for(int sign:new int[]{1,-1}){
                double other=x+sign*2.2;
                if(match.world.canWalk(other,y)&&
                   !match.world.worldStructureAt(other,y,.55))
                    return new double[]{x,y,other};
            }
        }
        throw new AssertionError("No physical wilderness factory and storage pair");
    }
    static void exercise(int kind,CharacterRoster.Faction faction)throws Exception{
        long seed=20261026L;
        String key="acct_"+"e".repeat(63)+(kind-8);
        ArenaSystems host=new ArenaSystems(seed);
        var owner=host.join(1);
        check(host.assignAccount(1,key)&&host.enrollFaction(1,faction),
            "Authenticated faction character must be enrolled");
        double[] xy=site(host);
        World.Device work=new World.Device(xy[0],xy[1],kind);
        World.Device crate=new World.Device(xy[2],xy[1],ArenaSystems.STORAGE_CRATE);
        work.ownerKey=crate.ownerKey=key;
        host.world.devices.add(work);host.world.devices.add(crate);
        owner.x=xy[0]+.7;owner.y=xy[1];
        String id=ArenaSystems.firstProcess(kind);
        var process=CraftingEraRegistry.process(id);
        check(process!=null&&process.facility().equals(ArenaSystems.factionFacility(kind)),
            "Runtime factory must use its exact independently registered recipe");
        var needed=process.inputs();
        for(var input:needed.entrySet())
            check(crate.storage.delivered(SurvivalCrafting.Item.valueOf(input.getKey()),
                input.getValue()),"Trusted recipe fixture input missing");
        var other=host.join(2);
        other.x=owner.x;other.y=owner.y;
        check(!host.startFactionBatch(2),"Unauthenticated guest operated a private factory");
        check(host.startFactionBatch(1),"Correct faction operator cannot start stocked work order");
        check(!host.startFactionBatch(1),"Repeating start accepted without another paid batch");
        check(work.productionActive&&work.productionMs==0,
            "Host did not reserve its actual factory batch");
        for(var input:needed.entrySet())
            check(crate.storage.count(SurvivalCrafting.Item.valueOf(input.getKey()))==0,
                "Factory failed atomic input debit: "+input.getKey());

        if(kind==ArenaSystems.PENITENT_VOTIVE_ALTAR){
            owner.x=xy[0]+12;owner.y=xy[1];
            advance(host,15);
            check(work.productionMs==0,
                "Penitent ritual advanced without live faction witnesses");
            owner.x=xy[0]+.7;
        }
        int total=process.seconds();
        advance(host,100);
        check(work.productionActive&&work.productionMs==100_000,
            "First production progress wrong or incorrect faction algorithm");

        Path root=Files.createTempDirectory("veil-distinct-factory-");
        try{
            HostCheckpoint.save(host,root);
            ArenaSystems restored=new ArenaSystems(seed);
            check(HostCheckpoint.load(restored,root),"Paired checkpoint lost world/account batch");
            restored.join(1);
            check(restored.assignAccount(1,key),"Recovered faction operator was not recognized");
            var running=restored.world.devices.stream()
                .filter(d->d.kind==kind).findFirst().orElseThrow();
            var stock=restored.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE).findFirst().orElseThrow();
            check(running.productionActive&&running.productionMs==100_000&&
                stock.storage.snapshot().isEmpty(),
                "Restart forgot reserved materials or rolled back paid production");
            check(restored.factions.faction(key)==faction,
                "Factory restart bypassed permanent faction identity");
            var worker=restored.player(1);
            worker.x=running.x+.7;worker.y=running.y;
            advance(restored,total-101);
            check(running.productionActive&&stock.storage.snapshot().isEmpty(),
                "Worksite delivered output before actual production elapsed");
            advance(restored,1);
            var result=SurvivalCrafting.Item.valueOf(process.output());
            check(!running.productionActive&&running.productionCompleted==1&&
                stock.storage.count(result)==process.quantity(),
                "Completed "+id+" did not deliver its unique output into owned storage");
            advance(restored,5);
            check(stock.storage.count(result)==process.quantity()&&
                !restored.startFactionBatch(1),
                "Idle simulation or replay duplicated a finished batch");
            HostCheckpoint.save(restored,root);
            ArenaSystems second=new ArenaSystems(seed);
            check(HostCheckpoint.load(second,root),"Completed factory checkpoint unreadable");
            var permanent=second.world.devices.stream()
                .filter(d->d.kind==kind).findFirst().orElseThrow();
            var warehouse=second.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE).findFirst().orElseThrow();
            check(!permanent.productionActive&&permanent.productionCompleted==1&&
                warehouse.storage.count(result)==process.quantity(),
                "Reload minted or deleted a real faction output");
            System.out.println("PASS "+faction+" "+id+" -> "+result+
                " ("+total+" s, independent progress and durable inventory)");
        }finally{cleanup(root);}
    }
    public static void main(String[] args)throws Exception{
        exercise(ArenaSystems.UNRAVELED_SCRAP_OUTPOST,
            CharacterRoster.Faction.THE_UNRAVELED);
        exercise(ArenaSystems.PENITENT_VOTIVE_ALTAR,
            CharacterRoster.Faction.THE_PENITENT);
        exercise(ArenaSystems.BEGOTTEN_BROOD_NEST,
            CharacterRoster.Faction.THE_BEGOTTEN);
        System.out.println("PASS FactionFirstProductionTest: three authentic independent E1 factory processes");
    }
}
