package veil;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

/**
 * Regression for genuine v1 checksummed files from the era when city empty lots
 * could contain saved player-built devices. Migration archives the exact bytes,
 * never restores the structure into fixed city terrain, and keeps the rest of
 * the world save playable and writable.
 */
public final class LegacyCitySaveMigrationTest {
    private static void check(boolean valid,String description){
        if(!valid)throw new AssertionError(description);
    }
    private static byte[] oneHistoricalCityDevice(byte[] original,double x,double y,
                                                  String owner)throws IOException {
        // V1 on-disk contract: MAGIC(4), VERSION(4), SEED(8),
        // TIMESTAMP(8), DEVICE_COUNT(4), ... and CRC32 recorded as long.
        if(original.length<40)throw new IOException("Bad source fixture");
        ByteArrayOutputStream payload=new ByteArrayOutputStream();
        payload.write(original,0,24);
        try(DataOutputStream out=new DataOutputStream(payload)){
            out.writeInt(1);
            out.writeInt(ArenaSystems.BARRICADE);
            out.writeDouble(x);out.writeDouble(y);
            out.writeInt(100);out.writeUTF(owner);
            out.write(original,28,original.length-28-8);
        }
        byte[] body=payload.toByteArray();
        CRC32 checksum=new CRC32();
        checksum.update(body);
        ByteArrayOutputStream legacy=new ByteArrayOutputStream();
        legacy.write(body);
        try(DataOutputStream out=new DataOutputStream(legacy)){
            out.writeLong(checksum.getValue());
        }
        return legacy.toByteArray();
    }
    public static void main(String[] args)throws Exception {
        Path root=Files.createTempDirectory("veil-legacy-city-migration-");
        final long seed=887766L;
        final String owner="acct_"+"a".repeat(64);
        try {
            ArenaSystems original=new ArenaSystems(seed);
            original.join(1);
            check(original.assignAccount(1,owner),"Authenticated owner prepared");
            DurableRealm.save(original,root);
            byte[] clean=Files.readAllBytes(DurableRealm.file(root));
            byte[] legacy=oneHistoricalCityDevice(clean,21.5,24.5,owner);
            Files.write(DurableRealm.file(root),legacy);
            ArenaSystems restored=new ArenaSystems(seed);
            check(DurableRealm.load(restored,root),"Genuine legacy snapshot parsed and loaded");
            check(restored.world.devices.isEmpty(),"Legacy city player device must not respawn");
            try(var list=Files.list(root)){
                var archived=list.filter(path->path.getFileName().toString()
                        .startsWith("realm-city-legacy-"))
                    .toList();
                check(archived.size()==1,"One exact legacy snapshot archive expected");
                check(Arrays.equals(Files.readAllBytes(archived.get(0)),legacy),
                    "Migration archive bytes differ from original city save");
            }
            DurableRealm.save(restored,root);
            ArenaSystems next=new ArenaSystems(seed);
            check(DurableRealm.load(next,root),"Sanitized city world save can restart");
            check(next.world.devices.isEmpty(),"Forbidden city devices cannot reappear");
            check(!Arrays.equals(Files.readAllBytes(DurableRealm.file(root)),legacy),
                "Active save still contains obsolete city structure");
            System.out.println("PASS LegacyCitySaveMigrationTest: backup of original "+
                legacy.length+" bytes, city device quarantined, sanitized world reload");
        }finally{
            try(var stream=Files.walk(root)){
                stream.sorted(Comparator.reverseOrder()).forEach(path->{
                    try{Files.deleteIfExists(path);}catch(IOException ignored){}
                });
            }
        }
    }
}
