package veil;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Local-server-only allowlist, bans, and moderation audit (never exposed as client commands). */
public final class HostModeration {
    private final Path root;
    private final Set<String> banned=new TreeSet<>(),allowed=new TreeSet<>();
    private boolean allowlist;
    public HostModeration(Path root)throws IOException{
        this.root=Objects.requireNonNull(root);
        Path file=root.resolve("moderation.properties");
        if(Files.exists(file)){
            Properties properties=new Properties();
            try(InputStream in=Files.newInputStream(file)){properties.load(in);}
            allowlist=Boolean.parseBoolean(properties.getProperty("allowlist","false"));
            for(String key:properties.stringPropertyNames()){
                if(key.startsWith("banned.")&&valid(key.substring(7)))banned.add(key.substring(7));
                if(key.startsWith("allowed.")&&valid(key.substring(8)))allowed.add(key.substring(8));
            }
        }
    }
    private static boolean valid(String account){return account!=null&&account.matches("acct_[a-f0-9]{64}");}
    public synchronized boolean permitted(String account){
        return valid(account)&&!banned.contains(account)&&(!allowlist||allowed.contains(account));
    }
    public synchronized boolean ban(String account)throws IOException {
        if(!valid(account))return false;
        boolean change=banned.add(account);
        if(change)save();
        return change;
    }
    public synchronized boolean unban(String account)throws IOException {
        boolean change=banned.remove(account);
        if(change)save();
        return change;
    }
    public synchronized boolean allow(String account)throws IOException{
        if(!valid(account))return false;
        boolean change=allowed.add(account);if(change)save();return change;
    }
    public synchronized boolean deny(String account)throws IOException{
        boolean change=allowed.remove(account);if(change)save();return change;
    }
    public synchronized void setAllowlist(boolean value)throws IOException{
        allowlist=value;save();
    }
    public synchronized boolean allowlistEnabled(){return allowlist;}
    public synchronized int bans(){return banned.size();}
    public synchronized int allowedCount(){return allowed.size();}
    private void save()throws IOException{
        Files.createDirectories(root);
        Properties properties=new Properties();
        properties.setProperty("allowlist",String.valueOf(allowlist));
        for(String account:banned)properties.setProperty("banned."+account,"true");
        for(String account:allowed)properties.setProperty("allowed."+account,"true");
        Path temp=Files.createTempFile(root,"moderation-",".tmp");
        try{
            try(OutputStream out=Files.newOutputStream(temp)){properties.store(out,"Malefic Veil host access rules");}
            try{Files.move(temp,root.resolve("moderation.properties"),
                StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){
                Files.move(temp,root.resolve("moderation.properties"),StandardCopyOption.REPLACE_EXISTING);
            }
        }finally{Files.deleteIfExists(temp);}
    }
    public synchronized void audit(String action)throws IOException{
        Files.createDirectories(root);
        if(action.indexOf('\n')>=0||action.indexOf('\r')>=0)throw new IllegalArgumentException("Unsafe audit");
        Files.writeString(root.resolve("audit.log"),java.time.Instant.now()+" "+action+"\n",
            StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
}
