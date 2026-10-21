package veil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.*;
import java.util.*;

/**
 * Per-server bearer credentials for trusted LAN testing. NOT internet-safe without TLS:
 * AUTH messages traverse the prototype socket in plaintext. Never log a raw token.
 */
public final class PlayerIdentity {
    private PlayerIdentity(){}
    public static String sha256(String text){
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public static String account(String token){
        if(token==null||!token.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid identity");
        return "acct_"+sha256(token);
    }
    /** A stable secret per remote host/port, stored under the user's home rather than in repo. */
    public static String forServer(String host,int port)throws IOException {
        if(host==null||host.isBlank()||port<0||port>65535)throw new IOException("Invalid server");
        Path dir=Path.of(System.getProperty("user.home"),".malefic-veil","identities");
        Files.createDirectories(dir);
        Path key=dir.resolve(sha256(host.toLowerCase(Locale.ROOT)+":"+port)+".key");
        if(Files.exists(key))return read(key);
        byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
        String generated=HexFormat.of().formatHex(bytes);
        Path temp=Files.createTempFile(dir,"identity-",".tmp");
        try{
            try{Files.setPosixFilePermissions(temp,Set.of(
                PosixFilePermission.OWNER_READ,PosixFilePermission.OWNER_WRITE));
            }catch(UnsupportedOperationException ignored){/* Windows ACLs follow local account profile */}
            Files.writeString(temp,generated+"\n",StandardCharsets.UTF_8);
            try{Files.move(temp,key,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){Files.move(temp,key);}
            catch(FileAlreadyExistsException ignored){/* first successful identity wins */}
        }finally{Files.deleteIfExists(temp);}
        return read(key);
    }
    private static String read(Path file)throws IOException{
        if(!Files.isRegularFile(file)||Files.size(file)>100)
            throw new IOException("Invalid existing identity file");
        String token=Files.readString(file,StandardCharsets.UTF_8).trim();
        if(!token.matches("[a-f0-9]{64}"))throw new IOException("Corrupt identity file");
        return token;
    }
}
