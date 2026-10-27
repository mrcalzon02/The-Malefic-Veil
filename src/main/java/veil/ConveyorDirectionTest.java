package veil;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

/** Host-integrated reversible conveyor, sorter, storage backpressure, V11
 * checkpoint recovery and backward-compatible V10 receipt migration. */
public final class ConveyorDirectionTest {
    private static void check(boolean ok,String reason){
        if(!ok)throw new AssertionError(reason);
    }
    private static World.Device add(ArenaSystems host,int kind,double x,double y,String owner){
        World.Device d=new World.Device(x,y,kind);
        d.ownerKey=owner;host.world.devices.add(d);return d;
    }
    private static World.Device one(ArenaSystems host,int kind){
        return host.world.devices.stream().filter(d->d.kind==kind).findFirst().orElseThrow();
    }
    private static void tick(ArenaSystems host,int seconds){
        for(int n=0;n<seconds;n++)host.advance(1000);
    }
    /** A real serialized V11 snapshot transformed into the identical V10
     * layout, removing only the V11 per-device direction boolean. */
    private static byte[] asV10(byte[] v11)throws Exception{
        var in=new DataInputStream(new ByteArrayInputStream(v11,0,v11.length-8));
        var buffer=new ByteArrayOutputStream();
        var out=new DataOutputStream(buffer);
        out.writeInt(in.readInt());
        check(in.readInt()==11,"Unexpected test snapshot version");
        out.writeInt(10);
        out.writeLong(in.readLong());out.writeLong(in.readLong());
        int count=in.readInt();out.writeInt(count);
        for(int i=0;i<count;i++){
            out.writeInt(in.readInt());
            out.writeDouble(in.readDouble());out.writeDouble(in.readDouble());
            out.writeInt(in.readInt());out.writeUTF(in.readUTF());out.writeInt(in.readInt());
            int stocks=in.readInt();out.writeInt(stocks);
            for(int j=0;j<stocks;j++){
                out.writeInt(in.readInt());out.writeInt(in.readInt());
            }
            out.writeBoolean(in.readBoolean());out.writeLong(in.readLong());
            out.writeInt(in.readInt());out.writeUTF(in.readUTF());
            out.writeLong(in.readLong());out.writeLong(in.readLong());
            out.writeUTF(in.readUTF());out.writeLong(in.readLong());
            out.writeUTF(in.readUTF());
            in.readBoolean(); // Strip V11-only direction field.
        }
        out.write(in.readAllBytes());out.flush();
        byte[] bytes=buffer.toByteArray();
        CRC32 crc=new CRC32();crc.update(bytes);
        var framed=new ByteArrayOutputStream();framed.write(bytes);
        try(var last=new DataOutputStream(framed)){last.writeLong(crc.getValue());}
        return framed.toByteArray();
    }
    private static void clean(Path path)throws Exception{
        try(var paths=Files.walk(path)){
            for(Path p:paths.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);
        }
    }
    public static void main(String[] args)throws Exception{
        final long seed=20261020L;
        String owner="acct_"+"e".repeat(64);
        var host=new ArenaSystems(seed);
        var player=host.join(1);
        check(host.assignAccount(1,owner)&&
              host.enrollFaction(1,CharacterRoster.Faction.THE_UNRAVELED),
              "Owned E5 conveyor requires an authenticated faction");
        double[] site=FactionFirstProductionTest.site(host);
        double x=site[0],y=site[1];
        add(host,ArenaSystems.CAMP_STAKE,x,y-3,owner);
        var west=add(host,ArenaSystems.STORAGE_CRATE,x-3,y,owner);
        var east=add(host,ArenaSystems.STORAGE_CRATE,x+3,y,owner);
        var conveyor=add(host,ArenaSystems.CONVEYOR_LINE,x,y,owner);
        add(host,ArenaSystems.AUTOMATIC_SORTER,x+.3,y+.3,owner);
        add(host,ArenaSystems.ELECTRIC_BUS,x,y+2.5,owner);
        var generator=add(host,ArenaSystems.GENERATOR,x+.7,y+3,owner);
        generator.electricMs=ElectricUtility.MAX_CHARGE_MS;
        var item=SurvivalCrafting.Item.MACHINE_PARTS;
        var filler=SurvivalCrafting.Item.EDIBLES;
        check(east.storage.delivered(item,4)&&west.storage.delivered(filler,126),
            "Cannot stage physical east-side stock and nearly-full receiver");
        player.x=x;player.y=y;
        var intruder=host.join(2);
        check(host.assignAccount(2,"acct_"+"a".repeat(64)),
            "Second account fixture not authorized");
        intruder.x=x;intruder.y=y;
        check(!host.programConveyor(2,item.name(),true),
            "Another account reprogrammed private automation");
        check(!host.programConveyor(1,"FAKE_MATERIAL",true),
            "Host accepted nonexistent conveyor material");
        check(host.programConveyor(1,item.name(),true)&&
              conveyor.routeReverse,"Owner could not reverse fixed conveyor");
        tick(host,4);
        check(conveyor.routeMs==4000&&east.storage.count(item)==4,
            "Conveyor ran without five paid seconds");
        Path root=Files.createTempDirectory("veil-route-v11-");
        Path legacy=Files.createTempDirectory("veil-route-v10-");
        try{
            HostCheckpoint.save(host,root);
            var restored=new ArenaSystems(seed);
            check(HostCheckpoint.load(restored,root),"Interrupted V11 route failed recovery");
            var belt=one(restored,ArenaSystems.CONVEYOR_LINE);
            var receiver=restored.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&d.x<x)
                .findFirst().orElseThrow();
            var sender=restored.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&d.x>x)
                .findFirst().orElseThrow();
            check(belt.routeReverse&&belt.routeMs==4000&&
                sender.storage.count(item)==4,"Route direction/timer lost in V11");
            tick(restored,1);
            check(receiver.storage.count(item)==2&&
                sender.storage.count(item)==2&&
                receiver.storage.units()==CampStockpile.MAX_UNITS,
                "Actual sorter did not move two east-to-west units");
            tick(restored,10);
            check(receiver.storage.count(item)==2&&sender.storage.count(item)==2&&
                belt.routeMs==0,"Full output warehouse copied or destroyed cargo");
            check(receiver.storage.consume(Map.of(filler,2)),
                "Receiver capacity could not be released");
            tick(restored,5);
            check(receiver.storage.count(item)==4&&sender.storage.count(item)==0,
                "Receiver reopening did not release remaining stock");
            var operator=restored.join(1);
            check(restored.assignAccount(1,owner),"Lost account after V11 restart");
            operator.x=x;operator.y=y;
            check(restored.programConveyor(1,item.name(),false)&&
                !belt.routeReverse&&belt.routeMs==0,
                "Owner failed to reconfigure west-to-east direction");
            tick(restored,5);
            check(receiver.storage.count(item)==2&&sender.storage.count(item)==2,
                "Return trip changed material conservation");
            DurableRealm.save(restored,legacy);
            Path file=DurableRealm.file(legacy);
            Files.write(file,asV10(Files.readAllBytes(file)));
            var oldFormat=new ArenaSystems(seed);
            check(DurableRealm.load(oldFormat,legacy),
                "Legacy V10 receipt no longer opens in the V11 reader");
            var migrated=one(oldFormat,ArenaSystems.CONVEYOR_LINE);
            check(!migrated.routeReverse&&migrated.routeItem.equals(item.name())&&
                migrated.routeMs==0,"Historical conveyor defaults not preserved");
            check(oldFormat.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE)
                .mapToInt(d->d.storage.count(item)).sum()==4,
                "Legacy save migration altered physical supply");
            check(restored.programConveyor(1,"NONE",true)&&
                belt.routeItem.isEmpty()&&!belt.routeReverse&&belt.routeMs==0,
                "Disabling route did not reset direction");
            HostCheckpoint.save(restored,root);
            var disabled=new ArenaSystems(seed);
            check(HostCheckpoint.load(disabled,root)&&
                !one(disabled,ArenaSystems.CONVEYOR_LINE).routeReverse,
                "Cancelled reverse conveyor revived after restart");
        }finally{clean(root);clean(legacy);}
        System.out.println("PASS ConveyorDirectionTest: owner routes, sorter, backpressure, V10/V11 recovery");
    }
}
