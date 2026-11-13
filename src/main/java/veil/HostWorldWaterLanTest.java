package veil;

import dev.mrcalzon.water.*;
import java.nio.file.*;
import java.util.*;

/** Real TCP join/rejoin acceptance for selected-world authored-water snapshots. */
public final class HostWorldWaterLanTest {
    private static void check(boolean passed,String message){
        if(!passed)throw new AssertionError(message);
    }
    private static WaterDocument water(long seed,double x){
        WaterSettings settings=new WaterSettings()
            .with("wave.height",.23)
            .with("colour.shallow","436977");
        return new WaterDocument("seed-"+seed,List.of(
            new WaterPlacement("courtyard-pool",x,.25,65,8,6,.15,settings,
                WaterPlacement.Shape.OCTAGON)));
    }
    private static WaterDocument received(NetLink client)throws InterruptedException {
        long deadline=System.nanoTime()+8_000_000_000L;
        while(System.nanoTime()<deadline){
            WaterDocument doc=client.authoredWater();
            if(doc!=null)return doc;
            if(!client.connected())break;
            Thread.sleep(20);
        }
        throw new AssertionError("Real LAN client did not receive host water snapshot");
    }
    private static void checkSocket(NetLink link,WaterDocument expected)throws Exception {
        WaterDocument seen=received(link);
        check(seen.equals(expected),"Client lost the original polygon/settings/order");
        check(seen.revision().equals(expected.revision()),"Water revision differed on LAN");
        check(seen.placements().get(0).id().equals("courtyard-pool"),
              "Water identity lost in client transport");
        check(seen.placements().get(0).shape()==WaterPlacement.Shape.OCTAGON,
              "Client shoreline changed octagon shape");
        check(seen.placements().get(0).settings().number("wave.height")==.23,
              "Client lost authored wave settings");
    }
    private static void rejectMalformed(long seed,WaterDocument correct){
        List<String> frames=HostWaterWire.lines(correct);
        HostWaterWire.Receiver otherWorld=new HostWaterWire.Receiver(seed+1);
        for(String frame:frames)
            check(otherWorld.accept(frame)==null,"Client accepted wrong world seed");
        HostWaterWire.Receiver corrupt=new HostWaterWire.Receiver(seed);
        for(int i=0;i<frames.size();i++){
            String frame=frames.get(i);
            if(i==frames.size()-1)frame="WATER_END|"+"0".repeat(64);
            check(corrupt.accept(frame)==null,"Client accepted a bad water revision");
        }
        HostWaterWire.Receiver truncated=new HostWaterWire.Receiver(seed);
        check(truncated.accept(frames.get(0))==null,"Premature water begin published");
        check(truncated.accept(frames.get(frames.size()-1))==null,
              "Incomplete water transfer published");
        HostWaterWire.Receiver reordered=new HostWaterWire.Receiver(seed);
        reordered.accept(frames.get(0));
        reordered.accept("WATER_BODY|1|"+frames.get(1).split("\\|")[2]);
        check(reordered.accept(frames.get(frames.size()-1))==null,
              "Out-of-order water body was published");
        HostWaterWire.Receiver valid=new HostWaterWire.Receiver(seed);
        WaterDocument seen=null;
        for(String frame:frames)seen=valid.accept(frame);
        check(correct.equals(seen),"Verified wire transfer did not recover");
    }
    public static void main(String[] args)throws Exception {
        long seed=57113;
        Path dir=Files.createTempDirectory("malefic-water-lan-");
        try{
            WaterDocument first=water(seed,60);
            first.save(dir.resolve("city-A.water"));
            rejectMalformed(seed,first);
            try(LanServer host=new LanServer(0,seed,dir)){
                try(NetLink left=new NetLink("127.0.0.1",host.port());
                    NetLink right=new NetLink("127.0.0.1",host.port())){
                    checkSocket(left,first);
                    checkSocket(right,first);
                    check(host.authoredWater.snapshot().wet(60,65)&&
                          !host.authoredWater.snapshot().wet(75,65),
                          "Host wet/dry boundary changed during network join");
                }
            }
            WaterDocument moved=water(seed,83);
            moved.save(dir.resolve("city-A.water"));
            try(LanServer restarted=new LanServer(0,seed,dir);
                NetLink joining=new NetLink("127.0.0.1",restarted.port())){
                checkSocket(joining,moved);
                check(!joining.authoredWater().revision().equals(first.revision()),
                      "Reconnect reused old host water revision");
            }
            // An empty world must explicitly clear a former authored shoreline.
            new WaterDocument("seed-"+seed,List.of()).save(dir.resolve("city-A.water"));
            try(LanServer empty=new LanServer(0,seed,dir);
                NetLink joining=new NetLink("127.0.0.1",empty.port())){
                check(received(joining).placements().isEmpty(),
                      "An empty host water document did not clear the client view");
            }
            System.out.println("PASS HostWorldWaterLanTest: two real TCP clients, "+
                "selected durable polygon/settings/revision, restart/move, "+
                "empty world, cross-seed/revision/order fail-closed");
        }finally{
            try(var files=Files.walk(dir)){
                for(Path file:files.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(file);
            }
        }
    }
}
