package veil;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

/** Actual V1 -> V8 read and re-save, without silent world or NPC catalog reset. */
public final class V1RealmCompatibilityTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static byte[] downgradeForTest(byte[] current)throws Exception{
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(current,0,current.length-8))){
            check(in.readInt()==0x5645494c&&in.readInt()==8,"Fixture is V8");
            in.readLong();in.readLong();
            int devices=in.readInt();
            for(int i=0;i<devices;i++){
                in.readInt();in.readDouble();in.readDouble();in.readInt();in.readUTF();in.readInt();
                int stacks=in.readInt();
                for(int j=0;j<stacks;j++){in.readInt();in.readInt();}
                in.readBoolean();in.readLong();in.readInt();in.readUTF();in.readLong();in.readLong();
            }
            int cars=in.readInt();
            for(int i=0;i<cars;i++){
                in.readInt();in.readDouble();in.readDouble();in.readDouble();
                in.readDouble();in.readInt();
            }
            int pickups=in.readInt();for(int i=0;i<pickups;i++)in.readBoolean();
            int npcs=in.readInt();
            for(int i=0;i<npcs;i++){in.readDouble();in.readDouble();in.readInt();}
            int owners=in.readInt();
            for(int i=0;i<owners;i++){in.readUTF();in.readLong();in.readInt();}
            int prefixLength=current.length-8-in.available();
            byte[] legacy=Arrays.copyOf(current,prefixLength);
            legacy[4]=0;legacy[5]=0;legacy[6]=0;legacy[7]=1;
            CRC32 checksum=new CRC32();checksum.update(legacy);
            ByteArrayOutputStream frame=new ByteArrayOutputStream();
            frame.write(legacy);
            try(DataOutputStream out=new DataOutputStream(frame)){
                out.writeLong(checksum.getValue());
            }
            return frame.toByteArray();
        }
    }
    public static void main(String[] args)throws Exception {
        Path dir=Files.createTempDirectory("veil-v1-upgrade-");
        try{
            ArenaSystems source=new ArenaSystems(20261020L);
            source.world.pickups.get(0).taken=true;
            source.world.npcs.get(0).hp=51;
            DurableRealm.save(source,dir);
            byte[] actualV1=downgradeForTest(Files.readAllBytes(DurableRealm.file(dir)));
            Files.write(DurableRealm.file(dir),actualV1);
            ArenaSystems reopened=new ArenaSystems(20261020L);
            check(DurableRealm.load(reopened,dir),"Legacy v1 world must still load");
            check(reopened.world.pickups.get(0).taken&&reopened.world.npcs.get(0).hp==51,
                "Old world state lost during format upgrade");
            check(reopened.cargo.sites().stream().allMatch(x->x.stock()==1),
                "New cargo economy must initialize exactly once on v1 migration");
            DurableRealm.save(reopened,dir);
            byte[] v8=Files.readAllBytes(DurableRealm.file(dir));
            check(v8[7]==8,"V1 realm was not upgraded on save");
            ArenaSystems again=new ArenaSystems(20261020L);
            check(DurableRealm.load(again,dir),"Upgraded V8 world is unreadable");
            check(again.world.pickups.get(0).taken&&again.world.npcs.get(0).hp==51,
                "Legacy world data disappeared during V8 roundtrip");
            System.out.println("PASS V1RealmCompatibilityTest: exact legacy header accepted and promoted to V8");
        }finally{
            try(var stream=Files.walk(dir)){
                stream.sorted(Comparator.reverseOrder()).forEach(path->{
                    try{Files.deleteIfExists(path);}catch(IOException ignored){}
                });
            }
        }
    }
}
