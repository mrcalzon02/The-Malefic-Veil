package veil;

import java.util.*;

/**
 * Real-world offline-base vulnerability policy. A logout never yields instant invulnerability:
 * bases are raidable for the configured grace period, then protected until an owner returns.
 * Restart-safe timestamps are UTC epoch milliseconds, never arena tick time.
 */
public final class RaidProtection {
    public record Presence(String account,long lastLogoutUtcMs,int sessions) {}
    private final long graceMs;
    private final Map<String,Presence> owners=new HashMap<>();
    public RaidProtection(long graceMinutes) {
        if(graceMinutes<0||graceMinutes>60*24*7)throw new IllegalArgumentException("Invalid grace");
        graceMs=Math.multiplyExact(graceMinutes,60_000);
    }
    private static boolean valid(String account){
        return account!=null&&account.matches("[a-zA-Z0-9:_-]{1,96}");
    }
    public synchronized void login(String account,long nowUtcMs){
        if(!valid(account)||nowUtcMs<0)throw new IllegalArgumentException("Invalid account or clock");
        Presence old=owners.get(account);
        owners.put(account,new Presence(account,old==null?nowUtcMs:old.lastLogoutUtcMs(),
            (old==null?0:old.sessions())+1));
    }
    public synchronized void logout(String account,long nowUtcMs){
        if(!valid(account)||nowUtcMs<0)throw new IllegalArgumentException("Invalid account or clock");
        Presence old=owners.get(account);
        if(old==null||old.sessions()==0)return;
        int left=old.sessions()-1;
        owners.put(account,new Presence(account,left==0?nowUtcMs:old.lastLogoutUtcMs(),left));
    }
    public synchronized boolean protectedOffline(String account,long nowUtcMs){
        if(!valid(account)||nowUtcMs<0)return false;
        Presence p=owners.get(account);
        if(p==null||p.sessions()>0)return false;
        // A clock rolling backwards must not accidentally grant premature protection.
        return nowUtcMs>=p.lastLogoutUtcMs()&&nowUtcMs-p.lastLogoutUtcMs()>=graceMs;
    }
    /** Milliseconds remaining until the protection shield activates; zero when already active. */
    public synchronized long remainingVulnerableMs(String account,long nowUtcMs){
        if(!valid(account)||nowUtcMs<0)return -1;
        Presence p=owners.get(account);
        if(p==null||p.sessions()>0)return -1;
        long elapsed=Math.max(0,nowUtcMs-p.lastLogoutUtcMs());
        return Math.max(0,graceMs-elapsed);
    }
    public synchronized List<Presence> snapshot(){
        return owners.values().stream().sorted(Comparator.comparing(Presence::account)).toList();
    }
    /** Restore accounts after a clean shutdown. Active sessions are always discarded. */
    public synchronized void restore(List<Presence> saved,long restartUtcMs){
        Objects.requireNonNull(saved);
        if(restartUtcMs<0)throw new IllegalArgumentException("Invalid clock");
        Map<String,Presence> validated=new HashMap<>();
        for(Presence item:saved){
            if(item==null||!valid(item.account())||item.lastLogoutUtcMs()<0||
               item.lastLogoutUtcMs()>restartUtcMs||item.sessions()<0)
                throw new IllegalArgumentException("Invalid persisted owner");
            // If owner was online at crash/shutdown, restart the logout grace from boot.
            long at=item.sessions()>0?restartUtcMs:item.lastLogoutUtcMs();
            if(validated.putIfAbsent(item.account(),new Presence(item.account(),at,0))!=null)
                throw new IllegalArgumentException("Duplicate persisted owner");
        }
        owners.clear();owners.putAll(validated);
    }
}
