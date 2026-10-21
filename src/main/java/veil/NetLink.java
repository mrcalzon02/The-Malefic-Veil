package veil;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
/** Thin async LAN wire client; never modifies Swing components on socket reader. */
public final class NetLink implements AutoCloseable {
    public static final class Remote {
        public volatile double x,y,angle,opacity;
        public volatile int hp;
        Remote(double x,double y,double a,int hp,double opacity){
            this.x=x;this.y=y;this.angle=a;this.hp=hp;this.opacity=opacity;
        }
    }
    public record Self(int health,int sanity,int cash,int supplies,int medkits,
        int pistolReserve,int shellReserve,int pistolClip,int shellClip,boolean dead,int frags,
        double x,double y,double angle){}
    public record DeviceEvent(int index,int kind,double x,double y,int hp){}
    public record NpcEvent(int index,double x,double y,int hp){}
    private final Socket socket;
    private final PrintWriter writer;
    public final long seed;
    public final int id;
    public final ConcurrentHashMap<Integer,Remote> others=new ConcurrentHashMap<>();
    public final ConcurrentLinkedQueue<Integer> pickedUp=new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<DeviceEvent> structures=new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<NpcEvent> npcs=new ConcurrentLinkedQueue<>();
    private volatile Self self;
    public NetLink(String host,int port)throws IOException {
        socket=new Socket();
        socket.connect(new InetSocketAddress(host,port),4000);
        BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);
        String hello=reader.readLine();
        String[] parts=hello==null?new String[0]:hello.split("\\|");
        if(parts.length!=4||!parts[0].equals("WELCOME")||!parts[3].equals("2"))
            throw new IOException("Unsupported or invalid LAN protocol");
        try{id=Integer.parseInt(parts[1]);seed=Long.parseLong(parts[2]);}
        catch(NumberFormatException e){throw new IOException("Malformed host greeting",e);}
        Thread t=new Thread(()->{
            String line;
            try{
                while((line=reader.readLine())!=null){
                    String[] s=line.split("\\|");
                    try{
                        switch(s[0]){
                            case "PEER" -> {
                                if(s.length!=7)continue;
                                int pid=Integer.parseInt(s[1]);
                                if(pid!=id)others.put(pid,new Remote(Double.parseDouble(s[2]),
                                    Double.parseDouble(s[3]),Double.parseDouble(s[4]),Integer.parseInt(s[5]),
                                    ArenaSystems.opacity(Double.parseDouble(s[6]))));
                            }
                            case "LEFT" -> {if(s.length==2)others.remove(Integer.parseInt(s[1]));}
                            case "PICKUP" -> {if(s.length==2)pickedUp.add(Integer.parseInt(s[1]));}
                            case "DEVICE" -> {
                                if(s.length==6)structures.add(new DeviceEvent(Integer.parseInt(s[1]),
                                    Integer.parseInt(s[2]),Double.parseDouble(s[3]),Double.parseDouble(s[4]),Integer.parseInt(s[5])));
                            }
                            case "NPC" -> {
                                if(s.length==5)npcs.add(new NpcEvent(Integer.parseInt(s[1]),
                                    Double.parseDouble(s[2]),Double.parseDouble(s[3]),Integer.parseInt(s[4])));
                            }
                            case "SELF" -> {
                                if(s.length==15)self=new Self(Integer.parseInt(s[1]),Integer.parseInt(s[2]),
                                    Integer.parseInt(s[3]),Integer.parseInt(s[4]),Integer.parseInt(s[5]),
                                    Integer.parseInt(s[6]),Integer.parseInt(s[7]),Integer.parseInt(s[8]),
                                    Integer.parseInt(s[9]),Integer.parseInt(s[10])!=0,Integer.parseInt(s[11]),
                                    Double.parseDouble(s[12]),Double.parseDouble(s[13]),Double.parseDouble(s[14]));
                            }
                            default -> {}
                        }
                    }catch(NumberFormatException ignored){}
                }
            }catch(IOException ignored){}
        },"Veil-LAN-reader");
        t.setDaemon(true);t.start();
    }
    public Self self(){return self;}
    private synchronized void send(String s){writer.println(s);}
    public void choose(int id){send("CHOOSE|"+id);}
    public void state(double x,double y,double angle,int stanceFlags){send("STATE|"+x+"|"+y+"|"+angle+"|"+stanceFlags);}
    public void fire(int gun){send("FIRE|"+gun);}
    public void pickup(int index){send("PICKUP|"+index);}
    public void useMedkit(){send("MEDKIT");}
    public void reload(int weapon){send("RELOAD|"+weapon);}
    public void ability(){send("ABILITY");}
    public void build(int kind){send("BUILD|"+kind);}
    public void respawn(int index){send("RESPAWN|"+index);}
    @Override public void close()throws IOException{socket.close();}
}
