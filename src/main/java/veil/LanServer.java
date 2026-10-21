package veil;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * LAN experimental host. Shared city seed and genuine remote combat/presence.
 * This is NOT yet an anti-cheat-authoritative public-internet server.
 */
public final class LanServer implements AutoCloseable {
    private final ServerSocket listener;
    private final ExecutorService threads=Executors.newCachedThreadPool();
    private final Map<Integer,Peer> peers=new ConcurrentHashMap<>();
    private final World map;
    private volatile boolean running=true;
    private int nextId=0;
    private static final class Peer {
        int id; String name; Socket socket; PrintWriter out;
        volatile double x=1.5,y=1.5,angle=0;
        volatile int health=100; volatile long lastShot=0;
        synchronized void send(String s){out.println(s);}
    }
    public LanServer(int port,long seed) throws IOException {
        listener=new ServerSocket(port);
        map=new World(seed);
        threads.execute(()->{
            while(running)try {Socket s=listener.accept();threads.execute(()->serve(s));}
            catch(IOException e){if(running)System.err.println("Accept error: "+e);}
        });
    }
    private synchronized int id(){return ++nextId;}
    private void broadcast(String message,int except) {
        for(Peer p:peers.values())if(p.id!=except)p.send(message);
    }
    private void serve(Socket socket) {
        Peer p=new Peer();p.id=id();p.socket=socket;
        try(socket;
            BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true)) {
            socket.setSoTimeout(20000);
            p.out=out;p.name="Player-"+p.id;
            p.send("WELCOME|"+p.id+"|"+map.seed);
            peers.put(p.id,p);
            for(Peer other:peers.values())if(other.id!=p.id)
                p.send(state(other));
            broadcast(state(p),p.id);
            String line;
            while((line=in.readLine())!=null) {
                if(line.length()>256)break;
                String[] s=line.split("\\|");
                try {
                    switch(s[0]) {
                    case "STATE" -> {
                        if(s.length!=4)continue;
                        double x=Double.parseDouble(s[1]),y=Double.parseDouble(s[2]),a=Double.parseDouble(s[3]);
                        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(a)||!map.canWalk(x,y))continue;
                        // Movement speed is not yet host verified: LAN prototype only.
                        p.x=x;p.y=y;p.angle=a;
                        broadcast(state(p),p.id);
                    }
                    case "FIRE" -> {
                        if(s.length!=2)continue;
                        int weapon=Integer.parseInt(s[1]);
                        if(weapon<0||weapon>1)continue;
                        long now=System.currentTimeMillis();
                        long gap=weapon==0?210:850;
                        if(now-p.lastShot<gap)continue;
                        p.lastShot=now;
                        double max=weapon==0?12:4.5;
                        for(Peer target:peers.values()) {
                            if(target==p||target.health<=0)continue;
                            double dx=target.x-p.x,dy=target.y-p.y,range=Math.hypot(dx,dy);
                            if(range>max||range<.01)continue;
                            double diff=Math.atan2(Math.sin(Math.atan2(dy,dx)-p.angle),Math.cos(Math.atan2(dy,dx)-p.angle));
                            if(Math.abs(diff)>Math.atan2(.4,range)||!map.lineClear(p.x,p.y,target.x,target.y))continue;
                            int dmg=weapon==0?25:Math.max(18,70-(int)(range*8));
                            target.health=Math.max(0,target.health-dmg);
                            target.send("HIT|"+dmg);
                            broadcast("HP|"+target.id+"|"+target.health,-1);
                            break;
                        }
                    }
                    case "RESPAWN" -> {
                        if(s.length!=2)continue;
                        int index=Integer.parseInt(s[1]);
                        if(index<0||index>=map.spawns.length)continue;
                        p.x=map.spawns[index][0];p.y=map.spawns[index][1];
                        p.health=100;
                        broadcast(state(p),-1);
                    }
                    default -> { }
                    }
                }catch(NumberFormatException ignored){/* reject malformed packet */}
            }
        } catch(IOException ignored) {
        } finally {
            peers.remove(p.id);
            broadcast("LEFT|"+p.id,-1);
        }
    }
    private static String state(Peer p){return "PEER|"+p.id+"|"+p.x+"|"+p.y+"|"+p.angle+"|"+p.health;}
    public int port(){return listener.getLocalPort();}
    @Override public void close() throws IOException {
        running=false;listener.close();
        for(Peer p:peers.values())try{p.socket.close();}catch(IOException ignored){}
        threads.shutdownNow();
    }
    public static void main(String[] args)throws Exception{
        int port=args.length>0?Integer.parseInt(args[0]):7777;
        long seed=args.length>1?Long.parseLong(args[1]):20261020L;
        new LanServer(port,seed);
        System.out.println("The Malefic Veil LAN host listening on "+port+" seed "+seed);
    }
}
