package veil;

import java.io.*;
import java.nio.file.*;
import java.util.zip.CRC32;

/** Server-owned, restart-stable hardmode rule; changing an existing realm's
 * death consequences without a migration is an explicit startup error. */
public final class HostRules {
    private static final int MAGIC=0x56485231; // VHR1
    private HostRules(){}
    public static boolean bind(Path directory,boolean requested)throws IOException{
        if(directory==null)return requested;
        Files.createDirectories(directory);
        Path file=directory.resolve("host-rules.dat");
        if(Files.exists(file)){
            byte[] bytes=Files.readAllBytes(file);
            if(bytes.length!=17)throw new IOException("Invalid host rules file length");
            try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){
                if(in.readInt()!=MAGIC||in.readInt()!=1)throw new IOException("Unknown host rules schema");
                int flag=in.readUnsignedByte();
                if(flag!=0&&flag!=1)throw new IOException("Corrupt hardcore flag");
                long recorded=in.readLong();
                CRC32 crc=new CRC32();crc.update(bytes,0,9);
                if(crc.getValue()!=recorded)throw new IOException("Host rules checksum failed");
                boolean active=flag==1;
                if(active!=requested)throw new IOException(
                    "Existing world hardcore="+active+" conflicts with requested hardcore="+requested+
                    "; explicit audited world migration required");
                return active;
            }
        }
        // Pre-hardcore worlds implicitly used NORMAL rules. Do not silently
        // enable permadeath on a legacy realm lacking an explicit mode file.
        if(requested&&(Files.exists(directory.resolve("realm.dat"))||
           Files.exists(directory.resolve("accounts.dat"))))
            throw new IOException("Legacy realm defaults to NORMAL; enabling hardcore "+
                "requires an explicit character/world migration");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)){
            out.writeInt(MAGIC);out.writeInt(1);out.writeByte(requested?1:0);
            CRC32 crc=new CRC32();crc.update(bytes.toByteArray());
            out.writeLong(crc.getValue());
        }
        Path temporary=Files.createTempFile(directory,"host-rules-",".tmp");
        try{
            Files.write(temporary,bytes.toByteArray());
            try{Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException fallback){
                Files.move(temporary,file);
            }
            if(!java.util.Arrays.equals(bytes.toByteArray(),Files.readAllBytes(file)))
                throw new IOException("Host rule persistence read-back failed");
            return requested;
        }finally{Files.deleteIfExists(temporary);}
    }
}
