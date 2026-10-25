package veil;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/** Real socket parsing plus a host-owned craft/throw, active late join and collapse lifecycle. */
public final class FieldReplicationTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void until(BooleanSupplier predicate,String message)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(7);
        while(System.nanoTime()<deadline){if(predicate.getAsBoolean())return;Thread.sleep(10);}
        throw new AssertionError(message);
    }
    private static void compatibility()throws Exception{
        try(ServerSocket listener=new ServerSocket(0)){
            var release=new CountDownLatch(1);var executor=Executors.newSingleThreadExecutor();
            Future<?> sender=executor.submit(()->{
                try(Socket socket=listener.accept();var out=new PrintWriter(socket.getOutputStream(),true);var in=new BufferedReader(new InputStreamReader(socket.getInputStream()))){
                    out.println("WELCOME|1|123|3");check(in.readLine().startsWith("AUTH|"),"Identity handshake missing");
                    var view=new ThrownDeployables.View(71,2,4,5,0,.9,90,12345,false,2500,100,200,.3,.4,12,true);
                    String packet=LanServer.fieldPacket(view);
                    out.println(packet.substring(0,packet.lastIndexOf('|'))); // Previous 16-field snapshot.
                    out.println(LanServer.fieldPacket(new ThrownDeployables.View(72,2,4,5,0,.9,90,12345,false,2500,100,200,.3,.4,12,true)));
                    out.println(LanServer.fieldPacket(new ThrownDeployables.View(73,2,4,5,0,.9,0,0,false,2500,-1,300,.3,.4,12,false)));
                    out.println("FIELD|bad|packet");
                    check(release.await(7,TimeUnit.SECONDS),"Receiver never consumed snapshots");
                    out.println("FIELD_GONE|73");
                }catch(Exception error){throw new RuntimeException(error);}
            });
            try(NetLink client=new NetLink("127.0.0.1",listener.getLocalPort())){
                until(()->client.activeFields.size()==3,"Legacy/new/collapse snapshots not parsed");
                check(!client.activeFields.get(71).tracking(),"Legacy packet invented target lock");
                var tracked=client.activeFields.get(72);var pose=tracked.view();
                check(tracked.tracking()&&pose.tracking()&&pose.ammoStock()==12,"Tracking/finite stock lost on wire");
                check(pose.heading()==.9&&pose.aimYaw()==.3&&pose.aimPitch()==.4,"Snapshot aiming changed");
                check(pose.ageMs()>=2500&&pose.ageMs()<=2750&&pose.operationAgeMs()>=100,"Late-join animation clocks reset");
                var collapsed=client.activeFields.get(73).view();
                check(collapsed.state()==ThrownDeployables.State.DESTROYED&&collapsed.damageAgeMs()>=300,"Late collapse snapshot restarts or vanishes");
                release.countDown();until(()->!client.activeFields.containsKey(73),"FIELD_GONE failed to remove finished collapse");sender.get(7,TimeUnit.SECONDS);
            }finally{release.countDown();executor.shutdownNow();}
        }
    }
    private static void liveHost()throws Exception{
        try(LanServer server=new LanServer(0,20261024L);NetLink owner=new NetLink("127.0.0.1",server.port())){
            until(()->owner.accountKey()!=null,"Owner authentication timeout");
            owner.selectFaction(CharacterRoster.Faction.THE_BEGOTTEN);
            until(()->server.match.factions.faction(owner.accountKey())==CharacterRoster.Faction.THE_BEGOTTEN,"Faction selection timeout");
            var match=server.match;var blueprint=ThrownDeployables.catalog().get(17);
            synchronized(match){
                for(var npc:match.world.npcs)npc.hp=0;
                var maker=match.player(owner.id);boolean found=false;
                for(int x=4;x<World.SIZE-6&&!found;x++)for(int y=4;y<World.SIZE-6&&!found;y++){
                    double px=x+.5,py=y+.5;
                    if(match.world.canWalk(px,py)&&match.world.canWalk(px+3.5,py)&&match.world.lineClear(px,py,px+3.5,py)){
                        maker.x=px;maker.y=py;maker.angle=0;found=true;
                    }
                }
                check(found,"Safe throw lane unavailable");
                for(var ingredient:blueprint.ingredients().entrySet())check(maker.survival.acquire(ingredient.getKey(),ingredient.getValue(),true),"Raw material setup failed");
            }
            owner.fieldCraft(ThrownDeployables.Role.HEALTH.ordinal());
            until(()->match.player(owner.id).survival.count(blueprint.item())==1,"Host wire craft failed");
            owner.fieldThrow(ThrownDeployables.Role.HEALTH.ordinal());
            until(()->!owner.activeFields.isEmpty(),"Host throw not replicated");
            int id=owner.activeFields.keySet().iterator().next();
            until(()->match.thrown.views().stream().anyMatch(v->v.id()==id&&v.state()==ThrownDeployables.State.ACTIVE),"Host deployment never became active");
            try(NetLink late=new NetLink("127.0.0.1",server.port())){
                until(()->late.activeFields.containsKey(id),"Late join missed existing field");
                var received=late.activeFields.get(id).view();
                check(!received.flying()&&received.ageMs()>=ThrownDeployables.ACTIVATION_MS,"Late join replayed throw/deployment");
                check(received.remainingMs()<ThrownDeployables.LIFETIME_MS,"Late join restarted lifetime");
                synchronized(match){
                    var enemy=match.player(late.id);
                    check(match.assignAccount(late.id,"acct_"+"f".repeat(64)),"Test enemy identity unavailable");
                    check(match.enrollFaction(late.id,CharacterRoster.Faction.THE_UNRAVELED),"Enemy faction setup failed");
                    check(match.thrown.damage(enemy.id,id,500),"Authoritative destruction rejected");
                }
                until(()->owner.activeFields.containsKey(id)&&owner.activeFields.get(id).hp()==0&&late.activeFields.containsKey(id)&&late.activeFields.get(id).hp()==0,"Collapse not replicated to both peers");
                check(owner.activeFields.get(id).view().state()==ThrownDeployables.State.DESTROYED,"Destroyed state not visible");
                until(()->!owner.activeFields.containsKey(id)&&!late.activeFields.containsKey(id),"Host collapse removal not replicated");
                check(!owner.fieldBursts.isEmpty()&&!late.fieldBursts.isEmpty(),"Harmless burst missing on peer");
            }
        }
    }
    public static void main(String[] args)throws Exception{
        compatibility();liveHost();
        System.out.println("PASS FieldReplicationTest: legacy/new FIELD, tracking/stock/clocks, collapse snapshot, real wire craft/throw, active late join, two-peer collapse/removal");
    }
}
