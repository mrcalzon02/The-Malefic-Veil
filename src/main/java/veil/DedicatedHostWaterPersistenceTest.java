package veil;

import dev.mrcalzon.water.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Actual listening dedicated-host save/close/restart with authored wet polygons. */
public final class DedicatedHostWaterPersistenceTest {
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError(why);
    }
    private static WaterDocument authored(long seed,double x){
        var polygon=new WaterPlacement("courtyard-pool",x,.25,65,8,6,0,
            new WaterSettings(),WaterPlacement.Shape.OCTAGON);
        return new WaterDocument("seed-"+seed,List.of(polygon));
    }
    private static void checkWater(HostWorldWater.Snapshot snapshot,
                                   WaterDocument expected,double wetX,double dryX){
        check(snapshot.document().equals(expected),"Host did not load the authored document");
        check(snapshot.revision().equals(expected.revision()),"Host revision changed");
        check(snapshot.module().bodies().size()==1,"Authored polygon duplicated");
        check(snapshot.module().bodies().get(0).id().equals("courtyard-pool"),
            "Polygon ID changed across host restart");
        check(snapshot.wet(wetX,65)&&!snapshot.wet(dryX,65),
            "Actual SDK wet/dry polygon boundary was lost");
    }
    public static void main(String[] args)throws Exception{
        long seed=92233L;
        Path directory=Files.createTempDirectory("malefic-host-authored-water-");
        Path waterFile=directory.resolve("city-A.water");
        try{
            WaterDocument original=authored(seed,60);
            original.save(waterFile); // Existing editor's real atomic document format
            try(LanServer initial=new LanServer(0,seed,directory)){
                check(initial.port()>0,"Dedicated host did not bind its real listener");
                checkWater(initial.authoredWater.snapshot(),original,60,66);
                initial.saveNow(); // Actual host/account checkpoint alongside authored file
            }
            check(Files.isRegularFile(directory.resolve("host-ledger.current")),
                "Dedicated host did not commit a checkpoint");
            try(LanServer restarted=new LanServer(0,seed,directory)){
                check(restarted.port()>0,"Restarted dedicated host did not bind");
                checkWater(restarted.authoredWater.snapshot(),original,60,66);
                // An external editor moves the single existing ID. A host holding
                // the old revision must reject its stale import, without clobbering.
                WaterDocument outside=authored(seed,84);
                outside.save(waterFile);
                WaterDocument stale=authored(seed,100);
                boolean rejected=false;
                try{restarted.authoredWater.save(stale,original.revision());}
                catch(IOException expected){rejected=true;}
                check(rejected,"Outdated host silently overwrote an editor's water");
                check(Files.readString(waterFile).equals(outside.encode()),
                    "Stale host clobbered the authoritative water file");
                checkWater(restarted.authoredWater.snapshot(),original,60,66);
                restarted.authoredWater.reload(original.revision());
                checkWater(restarted.authoredWater.snapshot(),outside,84,60);
            }
            // A separate, freshly constructed listening host sees the new
            // author-owned polygon, not an old snapshot or a duplicate.
            try(LanServer finalHost=new LanServer(0,seed,directory)){
                checkWater(finalHost.authoredWater.snapshot(),authored(seed,84),84,60);
            }
            // Reject wrong-world files rather than accepting cross-seed water.
            authored(seed+1,84).save(waterFile);
            boolean rejected=false;
            try(LanServer wrong=new LanServer(0,seed,directory)){}
            catch(IOException expected){rejected=true;}
            check(rejected,"Host accepted another world's authored water");
            System.out.println("PASS DedicatedHostWaterPersistenceTest: "+
                "real host restart, authored ID/revision/wet boundary, "+
                "single body, external edit conflict, seed fail-closed");
        }finally{
            if(Files.exists(directory))try(var files=Files.walk(directory)){
                for(Path file:files.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(file);
            }
        }
    }
}
