package veil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/**
 * Commit the host's persistent physical-world and account inventories as
 * one generation. The pointer is the commit record; independent legacy file
 * mirrors are NOT the authority when a generation exists.
 *
 * A server crash before pointer installation replays the preceding complete
 * pair. Crash after installation replays the new pair. Incomplete stages and
 * partially written legacy mirrors can never mix these two generations.
 * Cargo, fixed crates and arsenal live in realm.dat; carried materials,
 * accounts, hardcore state and skills live in accounts.dat.
 */
public final class HostCheckpoint {
    private static final String CURRENT="host-ledger.current";
    private static final String PREVIOUS="host-ledger.previous";
    private static final String REALM="realm.dat",ACCOUNTS="accounts.dat";
    private HostCheckpoint(){}
    private record Pointer(String directory,String realmDigest,String accountsDigest,
                           long seed,boolean hardcore){}
    private static String sha(Path path)throws IOException {
        try{
            byte[] data=Files.readAllBytes(path);
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(data);
            return HexFormat.of().formatHex(hash);
        }catch(NoSuchAlgorithmException e){throw new IOException("SHA-256 unavailable",e);}
    }
    private static byte[] format(Pointer p){
        return (p.directory()+"|"+p.realmDigest()+"|"+p.accountsDigest()+
            "|"+p.seed()+"|"+p.hardcore()+"\n").getBytes(StandardCharsets.US_ASCII);
    }
    private static Pointer readPointer(Path root,String name)throws IOException{
        String data=Files.readString(root.resolve(name),StandardCharsets.US_ASCII).trim();
        String[] v=data.split("\\|",-1);
        if(v.length!=5||!v[0].matches("ledger-[0-9a-f-]{36}")||
           !v[1].matches("[a-f0-9]{64}")||!v[2].matches("[a-f0-9]{64}")||
           !Set.of("true","false").contains(v[4]))
            throw new IOException("Invalid host-ledger commit record");
        long seed;
        try{seed=Long.parseLong(v[3]);}
        catch(NumberFormatException e){throw new IOException("Invalid saved seed",e);}
        return new Pointer(v[0],v[1],v[2],seed,Boolean.parseBoolean(v[4]));
    }
    private static Path verified(Path root,Pointer pointer,long seed,boolean hardcore)
            throws IOException{
        if(pointer.seed()!=seed||pointer.hardcore()!=hardcore)
            throw new IOException("Committed host-ledger world rules mismatch");
        Path dir=root.resolve(pointer.directory());
        if(!Files.isDirectory(dir)||
           !sha(dir.resolve(REALM)).equals(pointer.realmDigest())||
           !sha(dir.resolve(ACCOUNTS)).equals(pointer.accountsDigest()))
            throw new IOException("Host-ledger generation corrupted or incomplete");
        return dir;
    }
    private static void install(Path root,String name,byte[] content)throws IOException{
        Path temp=Files.createTempFile(root,"ledger-",".tmp");
        try{
            Files.write(temp,content);
            if(!Arrays.equals(Files.readAllBytes(temp),content))
                throw new IOException("Commit-pointer temp write mismatch");
            try{Files.move(temp,root.resolve(name),
                StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException unavailable){
                Files.move(temp,root.resolve(name),StandardCopyOption.REPLACE_EXISTING);
            }
            if(!Arrays.equals(Files.readAllBytes(root.resolve(name)),content))
                throw new IOException("Commit-pointer remote readback mismatch");
        }finally{Files.deleteIfExists(temp);}
    }
    public static boolean load(ArenaSystems match,Path root)throws IOException{
        boolean current=Files.exists(root.resolve(CURRENT));
        boolean previous=Files.exists(root.resolve(PREVIOUS));
        if(!current&&!previous){
            // Migration: pre-journal saves stay readable and are not rewritten
            // until the first successful paired checkpoint is committed.
            boolean world=DurableRealm.load(match,root);
            boolean accounts=AccountDirectory.load(match,root);
            return world||accounts;
        }
        IOException first=null;
        for(String file:List.of(CURRENT,PREVIOUS)){
            if(!Files.exists(root.resolve(file)))continue;
            try{
                Pointer pointer=readPointer(root,file);
                Path gen=verified(root,pointer,match.world.seed,match.hardcore);
                // Files and checksums are verified before either ledger changes.
                if(!DurableRealm.load(match,gen)||!AccountDirectory.load(match,gen))
                    throw new IOException("Committed generation missing one ledger");
                if(file.equals(PREVIOUS))
                    System.err.println("Primary host ledger rejected; recovered previous complete pair");
                return true;
            }catch(IOException bad){
                if(first==null)first=bad;else first.addSuppressed(bad);
            }
        }
        throw new IOException("No complete host-ledger pair; refusing independent-file reset",first);
    }
    public static void save(ArenaSystems match,Path root)throws IOException{
        synchronized(match){
            Files.createDirectories(root);
            // Reject old damaged pointers rather than rewriting the only
            // known recoverable copy. The paired data must be intact first.
            Pointer old=Files.exists(root.resolve(CURRENT))?
                readPointer(root,CURRENT):null;
            if(old!=null)verified(root,old,match.world.seed,match.hardcore);
            String name="ledger-"+UUID.randomUUID();
            Path stage=Files.createTempDirectory(root,"ledger-stage-");
            Path committed=root.resolve(name);
            try{
                DurableRealm.save(match,stage);
                AccountDirectory.save(match,stage);
                Pointer fresh=new Pointer(name,sha(stage.resolve(REALM)),
                    sha(stage.resolve(ACCOUNTS)),match.world.seed,match.hardcore);
                try{Files.move(stage,committed,StandardCopyOption.ATOMIC_MOVE);}
                catch(AtomicMoveNotSupportedException unavailable){
                    Files.move(stage,committed);
                }
                verified(root,fresh,match.world.seed,match.hardcore);
                // Preserve the preceding fully verified commit record before
                // changing CURRENT. Incomplete stages are never referenced.
                if(old!=null)install(root,PREVIOUS,format(old));
                install(root,CURRENT,format(fresh));
                verified(root,readPointer(root,CURRENT),match.world.seed,match.hardcore);
                // Mirror legacy locations for existing administrative tools.
                // These are explicitly nonauthoritative once CURRENT exists.
                DurableRealm.save(match,root);
                AccountDirectory.save(match,root);
            }finally{
                if(Files.exists(stage))try(var walk=Files.walk(stage)){
                    for(Path p:walk.sorted(Comparator.reverseOrder()).toList())
                        Files.deleteIfExists(p);
                }
            }
        }
    }
}
