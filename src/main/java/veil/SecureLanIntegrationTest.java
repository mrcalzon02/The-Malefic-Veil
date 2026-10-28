package veil;

import hbnet.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** TLS sockets into the existing persistent authority, not a separate arena implementation. */
public final class SecureLanIntegrationTest {
    private static void until(BooleanSupplier value)throws Exception{long end=System.nanoTime()+8_000_000_000L;while(System.nanoTime()<end){if(value.getAsBoolean())return;Thread.sleep(20);}throw new AssertionError("Secure world observation timed out");}
    private static void check(boolean value){if(!value)throw new AssertionError("Secure LAN invariant");}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("veil-tls-");
        try{
            Hbnet.Server tls=Setup.initialize(root.resolve("network"),null);char[] key=Files.readString(root.resolve("network/room.secret")).toCharArray();Hbnet.Identity one=Hbnet.Identity.create(),two=Hbnet.Identity.create();
            Hbnet.Client a=new Hbnet.Client(tls.fingerprint,"malefic-veil",one,key),b=new Hbnet.Client(tls.fingerprint,"malefic-veil",two,key);
            String account="acct_"+one.id();
            try(LanServer server=new LanServer(0,9944,root.resolve("world"),false,HostAiSettings.fromProperties(),tls);
                NetLink first=new NetLink("localhost",server.port(),a);NetLink second=new NetLink("localhost",server.port(),b)){
                until(()->account.equals(first.accountKey())&&second.accountKey()!=null);check(!first.accountKey().equals(second.accountKey()));
                first.selectFaction(CharacterRoster.Faction.THE_UNRAVELED);second.selectFaction(CharacterRoster.Faction.THE_UNRAVELED);until(()->first.faction().equals("THE_UNRAVELED"));
                first.state(1.5,1.5,0,1);until(()->second.others.containsKey(first.id)&&second.others.get(first.id).opacity<.5);
                check(server.match.player(first.id).accountKey.equals(account));server.saveNow();check(server.ban(account));until(()->!first.connected());
            }
            try(LanServer server=new LanServer(0,9944,root.resolve("world"),false,HostAiSettings.fromProperties(),tls)){
                check(!new HostModeration(root.resolve("world")).permitted(account));
                try{new NetLink("127.0.0.1",server.port(),a).close();throw new AssertionError("Durable ban bypassed");}catch(java.io.IOException expected){}
                check(server.unban(account));
                try(NetLink rejoined=new NetLink("127.0.0.1",server.port(),a)){until(()->account.equals(rejoined.accountKey()));check(rejoined.accountKey().equals(account));}
            }
            a.clear();b.clear();Arrays.fill(key,'\0');System.out.println("PASS SecureLanIntegrationTest: two signed TLS identities, host stance/visibility, persistence restart, durable ban and DNS-independent rejoin");
        }finally{try(var files=Files.walk(root)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
}
