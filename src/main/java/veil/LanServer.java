package veil;

import java.io.*;
import java.net.*;
import java.nio.file.*;
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
    private final ScheduledExecutorService saves=Executors.newSingleThreadScheduledExecutor();
    private final Semaphore connectionSlots=new Semaphore(32);
    private final HostModeration moderation;
    private final Path dataDirectory;
    private final long startup=System.currentTimeMillis();
    private final ConcurrentMap<Integer,Peer> connections=new ConcurrentHashMap<>();
    public final ArenaSystems match;
    private volatile boolean running=true;
    private int nextId;
    private static final class Peer {
        int id;
        Socket socket;
        PrintWriter out;
        boolean authenticated;
        int commands;
        long commandWindow=System.nanoTime();
        synchronized void send(String message){out.println(message);}
    }
    public LanServer(int port,long seed)throws IOException {this(port,seed,null);}
    /** Persistent dedicated mode binds loopback by default; explicit veil.bind exposes it. */
    public LanServer(int port,long seed,Path saveDir)throws IOException {
        dataDirectory=saveDir;
        moderation=saveDir==null?null:new HostModeration(saveDir);
        match=new ArenaSystems(seed);
        if(saveDir!=null)DurableRealm.load(match,saveDir);
        listener=saveDir==null?new ServerSocket(port):
            new ServerSocket(port,32,InetAddress.getByName(System.getProperty("veil.bind","127.0.0.1")));
        if(saveDir!=null){
            saves.scheduleWithFixedDelay(()->{
                try{DurableRealm.save(match,dataDirectory);}
                catch(Exception e){System.err.println("AUTOSAVE FAILED: "+e);audit("autosave_failure "+e.getClass().getSimpleName());}
            },60,60,TimeUnit.SECONDS);
        }
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
                for(Vehicles.State car:match.vehicles.states())broadcast(carPacket(car),-1);
                for(int i=0;i<match.world.npcs.size();i++){
                    World.Npc n=match.world.npcs.get(i);
                    broadcast("NPC|"+i+"|"+n.x+"|"+n.y+"|"+n.hp,-1);
                }
            }catch(RuntimeException e){System.err.println("Arena tick failed: "+e);}
        },100,100,TimeUnit.MILLISECONDS);
        workers.execute(()->{
            while(running)try{
                Socket s=listener.accept();
                if(!connectionSlots.tryAcquire()){s.close();continue;}
                workers.execute(()->{
                    try{serve(s);}finally{connectionSlots.release();}
                });
            }catch(IOException e){if(running)System.err.println("Accept failed: "+e);}
        });
    }
    private synchronized int id(){return ++nextId;}
    private void broadcast(String message,int except){
        for(Peer q:connections.values())if(q.id!=except)q.send(message);
    }
    private static String peerPacket(ArenaSystems.Player p){
        return "PEER|"+p.id+"|"+p.x+"|"+p.y+"|"+p.angle+"|"+p.hp+"|"+p.visibility+"|"+p.classId;
    }
    private static String carPacket(Vehicles.State car){
        return "CAR|"+car.id()+"|"+car.x()+"|"+car.y()+"|"+car.heading()+"|"+
            car.speed()+"|"+car.fuel()+"|"+car.hp()+"|"+car.driverId();
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
        for(Vehicles.State car:match.vehicles.states())peer.send(carPacket(car));
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
            peer.send("WELCOME|"+peer.id+"|"+match.world.seed+"|3");
            connections.put(peer.id,peer);
            sendWorld(peer);peer.send(selfPacket(actor));
            broadcast(peerPacket(actor),peer.id);
            String line;
            while((line=in.readLine())!=null){
                if(line.length()>256)break;
                long nowNanos=System.nanoTime();
                if(nowNanos-peer.commandWindow>1_000_000_000L){
                    peer.commandWindow=nowNanos;peer.commands=0;
                }
                if(++peer.commands>120){audit("rate_limit id="+peer.id);break;}
                String[] s=line.split("\\|");
                try{
                    boolean changed=false;
                    if(moderation!=null&&!peer.authenticated&&!"AUTH".equals(s[0])){
                        peer.send("AUTH_DENIED|REQUIRED");return;
                    }
                    switch(s[0]){
                        case "AUTH" -> {
                            if(s.length!=2)continue;
                            String account;
                            try{account=PlayerIdentity.account(s[1]);}
                            catch(IllegalArgumentException invalid){peer.send("AUTH_DENIED|INVALID");break;}
                            if(moderation!=null&&!moderation.permitted(account)){
                                peer.send("AUTH_DENIED|ACCESS");audit("access_denied account="+account);
                                return;
                            }
                            if(match.assignAccount(peer.id,account)){
                                peer.authenticated=true;
                                peer.send("AUTH_OK|"+account);
                                audit("login id="+peer.id+" account="+account);
                            }else if(moderation==null){
                                // Local multi-client smoke tests can share a machine's stored key.
                                // The second session stays an unowned guest; never duplicate an account.
                                peer.send("AUTH_GUEST|DUPLICATE");
                            }else{peer.send("AUTH_DENIED|DUPLICATE");return;}
                        }
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
                        case "RIDE" -> {
                            if(s.length!=2)continue;
                            int index=Integer.parseInt(s[1]);
                            changed=match.enterVehicle(peer.id,index);
                            peer.send("RIDE_RESULT|"+(changed?"OK":"REJECTED"));
                        }
                        case "EXIT" -> {
                            if(s.length!=1)continue;
                            changed=match.exitVehicle(peer.id);
                            peer.send("RIDE_RESULT|"+(changed?"EXITED":"REJECTED"));
                        }
                        case "DRIVE" -> {
                            if(s.length!=3)continue;
                            changed=match.driveVehicle(peer.id,
                                Double.parseDouble(s[1]),Double.parseDouble(s[2]));
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
            if(peer.authenticated) audit("logout id="+peer.id);
            connections.remove(peer.id);
            match.leave(peer.id);
            broadcast("LEFT|"+peer.id,-1);
        }
    }
    private void audit(String event){
        if(moderation!=null)try{moderation.audit(event);}
        catch(IOException ex){System.err.println("Audit write failed: "+ex);}
    }
    public int port(){return listener.getLocalPort();}
    public String status(){
        return "uptime="+((System.currentTimeMillis()-startup)/1000)+"s"+
            " peers="+connections.size()+" structures="+match.world.devices.size()+
            " cars="+match.vehicles.states().size()+" npcs="+match.world.npcs.size()+
            " persistent="+(dataDirectory!=null);
    }
    public List<String> connectedPlayers(){
        List<String> out=new ArrayList<>();
        for(Peer p:connections.values()){
            ArenaSystems.Player actor=match.player(p.id);
            if(actor!=null)out.add("id="+p.id+" account="+actor.accountKey+" hp="+actor.hp);
        }
        Collections.sort(out);return out;
    }
    public boolean kick(int id){
        Peer p=connections.get(id);
        if(p==null)return false;
        audit("kick id="+id);
        try{p.socket.close();}catch(IOException ignored){}
        return true;
    }
    public boolean ban(String account)throws IOException{
        if(moderation==null)return false;
        boolean result=moderation.ban(account);
        if(result){
            audit("ban account="+account);
            for(Peer p:connections.values()){
                ArenaSystems.Player player=match.player(p.id);
                if(player!=null&&account.equals(player.accountKey))kick(p.id);
            }
        }
        return result;
    }
    public boolean unban(String account)throws IOException{
        if(moderation==null)return false;
        boolean result=moderation.unban(account);
        if(result)audit("unban account="+account);
        return result;
    }
    public void saveNow()throws IOException{
        if(dataDirectory==null)throw new IOException("In-memory LAN match has no save directory");
        DurableRealm.save(match,dataDirectory);audit("manual_save");
    }
    @Override public synchronized void close()throws IOException{
        if(!running)return;
        running=false;
        listener.close();
        for(Peer p:connections.values())try{p.socket.close();}catch(IOException ignored){}
        ticks.shutdownNow();saves.shutdownNow();workers.shutdownNow();
        if(dataDirectory!=null)saveNow();
    }
    public static void main(String[] args)throws Exception{
        int port=args.length>0?Integer.parseInt(args[0]):7777;
        long seed=args.length>1?Long.parseLong(args[1]):20261020L;
        Path root=args.length>2?Path.of(args[2]):Path.of("server-data");
        LanServer host=new LanServer(port,seed,root);
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            try{host.close();}catch(IOException e){System.err.println("Shutdown save failed: "+e);}
        },"Veil-server-shutdown"));
        System.out.println("Malefic Veil dedicated host "+host.port()+" seed "+seed+
            " bind="+host.listener.getInetAddress().getHostAddress()+" storage="+root);
        System.out.println("Commands: status, players, save, kick ID, ban ACCOUNT, unban ACCOUNT,"+
            " allow ACCOUNT, deny ACCOUNT, allowlist on/off, shutdown");
        BufferedReader console=new BufferedReader(new InputStreamReader(System.in));
        String line;
        while((line=console.readLine())!=null){
            try{
                String[] input=line.trim().split("\\s+");
                if(input.length==0)continue;
                String result=switch(input[0].toLowerCase(Locale.ROOT)){
                    case "status" -> host.status();
                    case "players" -> String.join("\\n",host.connectedPlayers());
                    case "save" -> {host.saveNow();yield "Saved and verified";}
                    case "kick" -> input.length==2&&host.kick(Integer.parseInt(input[1]))?"Kicked":"Unknown player";
                    case "ban" -> input.length==2&&host.ban(input[1])?"Banned":"Invalid/duplicate account";
                    case "unban" -> input.length==2&&host.unban(input[1])?"Unbanned":"Unknown account";
                    case "allow" -> input.length==2&&host.moderation.allow(input[1])?"Allowed":"Invalid/duplicate account";
                    case "deny" -> input.length==2&&host.moderation.deny(input[1])?"Removed":"Unknown account";
                    case "allowlist" -> {
                        if(input.length==2&&(input[1].equals("on")||input[1].equals("off"))){
                            host.moderation.setAllowlist(input[1].equals("on"));
                        }
                        yield "Allowlist "+(host.moderation.allowlistEnabled()?"ON":"OFF");
                    }
                    case "shutdown" -> {host.close();yield "Stopped"; }
                    default -> "Unknown command";
                };
                System.out.println(result);
                if(!host.running)break;
            }catch(Exception e){System.err.println("Admin command rejected: "+e.getMessage());}
        }
        if(host.running)new CountDownLatch(1).await(); // no console under systemd; shutdown hook owns save
    }
}
