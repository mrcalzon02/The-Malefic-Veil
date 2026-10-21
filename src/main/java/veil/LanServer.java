package veil;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Host-authoritative LAN match protocol v2: visibility, hits, inventory, pickups,
 * healing, sanity, structures and NPCs are simulated by one ArenaSystems instance.
 */
public final class LanServer implements AutoCloseable {
    private final ServerSocket listener;
    private final ExecutorService workers=Executors.newCachedThreadPool();
    private final ScheduledExecutorService ticks=Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentMap<Integer,Peer> connections=new ConcurrentHashMap<>();
    public final ArenaSystems match;
    private volatile boolean running=true;
    private int nextId;
    private static final class Peer {
        int id;
        Socket socket;
        PrintWriter out;
        synchronized void send(String message){out.println(message);}
    }
    public LanServer(int port,long seed)throws IOException {
        listener=new ServerSocket(port);
        match=new ArenaSystems(seed);
        ticks.scheduleAtFixedRate(()->{
            try {
                match.advance(100);
                for(Peer p:connections.values()){
                    ArenaSystems.Player self=match.player(p.id);
                    if(self!=null){
                        p.send(selfPacket(self));
                        if(self.dead){
                            List<ArenaSystems.SpawnRisk> risks=match.spawnRisks(p.id);
                            p.send("SPAWNS|"+risks.get(0).score()+"|"+risks.get(1).score()+"|"+risks.get(2).score());
                        }
                    }
                }
                for(ArenaSystems.Player p:match.roster())broadcast(peerPacket(p),p.id);
                for(int i=0;i<match.world.npcs.size();i++){
                    World.Npc n=match.world.npcs.get(i);
                    broadcast("NPC|"+i+"|"+n.x+"|"+n.y+"|"+n.hp,-1);
                }
            }catch(RuntimeException e){System.err.println("Arena tick failed: "+e);}
        },100,100,TimeUnit.MILLISECONDS);
        workers.execute(()->{
            while(running)try{Socket s=listener.accept();workers.execute(()->serve(s));}
            catch(IOException e){if(running)System.err.println("Accept failed: "+e);}
        });
    }
    private synchronized int id(){return ++nextId;}
    private void broadcast(String message,int except){
        for(Peer q:connections.values())if(q.id!=except)q.send(message);
    }
    private static String peerPacket(ArenaSystems.Player p){
        return "PEER|"+p.id+"|"+p.x+"|"+p.y+"|"+p.angle+"|"+p.hp+"|"+p.visibility;
    }
    private static String selfPacket(ArenaSystems.Player p){
        return "SELF|"+p.hp+"|"+p.sanity+"|"+p.cash+"|"+p.scrap+"|"+p.medkits+
            "|"+p.pistolReserve+"|"+p.shellReserve+"|"+p.pistolClip+"|"+p.shellClip+
            "|"+(p.dead?1:0)+"|"+p.frags+"|"+p.x+"|"+p.y+"|"+p.angle+"|"+p.visibility;
    }
    private void sendWorld(Peer peer){
        for(ArenaSystems.Player other:match.roster())if(other.id!=peer.id)peer.send(peerPacket(other));
        for(int i=0;i<match.world.pickups.size();i++)
            if(match.world.pickups.get(i).taken)peer.send("PICKUP|"+i);
        for(int i=0;i<match.world.devices.size();i++){
            World.Device d=match.world.devices.get(i);
            peer.send("DEVICE|"+i+"|"+d.kind+"|"+d.x+"|"+d.y+"|"+d.hp);
        }
    }
    private void serve(Socket socket){
        Peer peer=new Peer();peer.id=id();peer.socket=socket;
        try(socket;
            BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true)){
            socket.setSoTimeout(30000);
            peer.out=out;
            ArenaSystems.Player actor=match.join(peer.id);
            peer.send("WELCOME|"+peer.id+"|"+match.world.seed+"|2");
            connections.put(peer.id,peer);
            sendWorld(peer);peer.send(selfPacket(actor));
            broadcast(peerPacket(actor),peer.id);
            String line;
            while((line=in.readLine())!=null){
                if(line.length()>256)break;
                String[] s=line.split("\\|");
                try{
                    boolean changed=false;
                    switch(s[0]){
                        case "STATE" -> {
                            if(s.length!=5)continue;
                            changed=match.move(peer.id,Double.parseDouble(s[1]),
                                Double.parseDouble(s[2]),Double.parseDouble(s[3]),Integer.parseInt(s[4]));
                        }
                        case "CHOOSE" -> {
                            if(s.length!=2)continue;
                            match.choose(peer.id,Integer.parseInt(s[1]));changed=true;
                        }
                        case "FIRE" -> {
                            if(s.length!=3)continue;
                            long sequence=Long.parseLong(s[1]);
                            ArenaSystems.ShotReport shot=match.fireReport(peer.id,Integer.parseInt(s[2]),sequence);
                            peer.send("SHOT|"+shot.sequence()+"|"+shot.fired()+"|"+shot.status()+"|"+
                                shot.targetId()+"|"+shot.damage()+"|"+shot.ammo());
                            changed=shot.fired();
                        }
                        case "PICKUP" -> {
                            if(s.length!=2)continue;
                            int index=Integer.parseInt(s[1]);
                            changed=match.pickup(peer.id,index);
                            if(changed)broadcast("PICKUP|"+index,-1);
                        }
                        case "MEDKIT" -> changed=match.useMedkit(peer.id);
                        case "RELOAD" -> {
                            if(s.length!=2)continue;
                            changed=match.reload(peer.id,Integer.parseInt(s[1]));
                        }
                        case "ABILITY" -> changed=match.ability(peer.id);
                        case "BUILD" -> {
                            if(s.length!=2)continue;
                            int kind=Integer.parseInt(s[1]);
                            ArenaSystems.BuildOutcome outcome=match.buildOutcome(peer.id,kind);
                            ArenaSystems.BuildReview review=outcome.review();
                            changed=outcome.built();
                            peer.send("BUILD_RESULT|"+kind+"|"+(changed?"OK":review.reason())+"|"+
                                review.cashCost()+"|"+review.suppliesCost());
                            if(changed){
                                int idx=outcome.deviceIndex();
                                World.Device d=match.world.devices.get(idx);
                                broadcast("DEVICE|"+idx+"|"+d.kind+"|"+d.x+"|"+d.y+"|"+d.hp,-1);
                            }
                        }
                        case "PING" -> {
                            if(s.length!=2)continue;
                            long nonce=Long.parseLong(s[1]);
                            if(nonce>0)peer.send("PONG|"+nonce);
                        }
                        case "RESPAWN" -> {
                            if(s.length!=2)continue;
                            int index=Integer.parseInt(s[1]);
                            changed=match.respawn(peer.id,index);
                            peer.send("RESPAWN_RESULT|"+index+"|"+(changed?"OK":"REJECTED"));
                        }
                        default -> { }
                    }
                    if(changed){
                        ArenaSystems.Player updated=match.player(peer.id);
                        if(updated!=null){
                            peer.send(selfPacket(updated));
                            broadcast(peerPacket(updated),peer.id);
                        }
                    }
                }catch(NumberFormatException ignored){/* malformed client command rejected */}
            }
        }catch(IOException ignored){
        }finally{
            connections.remove(peer.id);
            match.leave(peer.id);
            broadcast("LEFT|"+peer.id,-1);
        }
    }
    public int port(){return listener.getLocalPort();}
    @Override public void close()throws IOException{
        running=false;listener.close();
        for(Peer p:connections.values())try{p.socket.close();}catch(IOException ignored){}
        ticks.shutdownNow();workers.shutdownNow();
    }
    public static void main(String[] args)throws Exception{
        int port=args.length>0?Integer.parseInt(args[0]):7777;
        long seed=args.length>1?Long.parseLong(args[1]):20261020L;
        new LanServer(port,seed);
        System.out.println("Malefic Veil LAN v2 host on "+port+" seed "+seed);
    }
}
