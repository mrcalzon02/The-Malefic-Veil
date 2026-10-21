package veil;

import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Real dedicated socket authentication, moderation, operator saves, and persisted restart. */
public final class DedicatedHostTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    private static void until(BooleanSupplier condition,String message)throws Exception {
        long deadline=System.nanoTime()+6_000_000_000L;
        while(System.nanoTime()<deadline){
            if(condition.getAsBoolean())return;
            Thread.sleep(20);
        }
        throw new AssertionError("Timed out: "+message);
    }
    private static void aim(ArenaSystems game,int id){
        ArenaSystems.Player player=game.player(id);
        for(CityLayout.Plot plot:game.world.city.plots()){
            if(plot.template().category()!=CityLayout.Category.OPEN||plot.template().variant()!=2)continue;
            double x=(plot.x()+plot.right())*CityLayout.CELL/2.0+.5;
            double y=(plot.y()+plot.bottom())*CityLayout.CELL/2.0+.5;
            if(!game.world.canWalk(x-1.1,y)||!game.world.canWalk(x,y))continue;
            player.x=x-1.1;player.y=y;player.angle=0;player.moveAt=game.time();
            return;
        }
        throw new AssertionError("No vacant city lot for denial test");
    }
    public static void main(String[] args)throws Exception{
        Path temp=Files.createTempDirectory("veil-dedicated-host-");
        String account;
        try{
            try(LanServer server=new LanServer(0,92233L,temp);
                NetLink client=new NetLink("127.0.0.1",server.port())){
                until(()->client.accountKey()!=null,"dedicated account authentication");
                account=client.accountKey();
                check(account.matches("acct_[a-f0-9]{64}"),"server account is hashed");
                check(server.status().contains("persistent=true"),"server in persistent mode");
                check(server.connectedPlayers().stream().anyMatch(s->s.contains(account)),
                    "operators see authenticated accounts");
                ArenaSystems.Player owner=server.match.player(client.id);
                owner.cash=1000;owner.scrap=100;
                aim(server.match,client.id);
                client.build(ArenaSystems.BARRICADE);
                until(()->!client.buildResults.isEmpty(),"host city-build rejection delivered");
                check(client.buildResults.poll().reason().equals("CITY_PROTECTED"),
                    "durable host forbids player structures in empty city parcels");
                check(server.match.world.devices.isEmpty()&&owner.cash==1000&&owner.scrap==100,
                    "denied construction preserves world and inventory");
                // Simulate a pre-rule persisted structure fixture for migration
                // compatibility; this never enters the player build pathway.
                World.Device historical=new World.Device(owner.x+1.1,owner.y,ArenaSystems.BARRICADE);
                historical.ownerId=client.id;historical.ownerKey=account;
                server.match.world.devices.add(historical);
                check(server.match.world.devices.get(0).ownerKey.equals(account),
                    "historic ownership keyed by account rather than socket");
                server.saveNow();
                check(Files.isRegularFile(DurableRealm.file(temp)),"manual save materialized");
                check(server.ban(account),"operator ban accepted");
                until(()->!client.connected(),"ban removes active socket");
                until(()->server.match.player(client.id)==null,"disconnect releases host actor");
            }
            try(LanServer resumed=new LanServer(0,92233L,temp)){
                check(resumed.match.world.devices.size()==1,"legacy base survives host restart");
                check(resumed.match.world.devices.get(0).ownerKey.equals(account),"base ownership persists");
                HostModeration decisions=new HostModeration(temp);
                check(!decisions.permitted(account),"ban survives complete host process cycle");
                check(resumed.status().contains("structures=1"),"operator status reflects world load");
            }
            check(Files.readString(temp.resolve("audit.log")).contains("ban account="),
                "operator audit contains moderation action");
            System.out.println("PASS DedicatedHostTest: auth, immutable cities, legacy saves, admin ban and restart");
        }finally{
            try(var stream=Files.walk(temp)){
                stream.sorted(Comparator.reverseOrder()).forEach(path->{
                    try{Files.deleteIfExists(path);}catch(java.io.IOException ignored){}
                });
            }
        }
    }
}
