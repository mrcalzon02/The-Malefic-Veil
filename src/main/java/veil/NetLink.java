package veil;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class NetLink implements AutoCloseable {
    public static final class Remote {
        public volatile double x,y,angle;
        public volatile int hp;
        Remote(double x,double y,double a,int hp){this.x=x;this.y=y;this.angle=a;this.hp=hp;}
    }
    private final Socket socket;
    private final PrintWriter writer;
    public final long seed;
    public final int id;
    public final ConcurrentHashMap<Integer,Remote> others=new ConcurrentHashMap<>();
    private final AtomicInteger pendingDamage=new AtomicInteger();
    public NetLink(String host,int port)throws IOException {
        socket=new Socket();
        socket.connect(new InetSocketAddress(host,port),4000);
        BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);
        String welcome=reader.readLine();
        String[] parts=welcome==null?new String[0]:welcome.split("\\|");
        if(parts.length!=3||!parts[0].equals("WELCOME"))throw new IOException("Bad protocol greeting");
        try {id=Integer.parseInt(parts[1]);seed=Long.parseLong(parts[2]);}
        catch(NumberFormatException e){throw new IOException("Invalid seed or player ID",e);}
        Thread t=new Thread(()->{
            String line;
            try {
                while((line=reader.readLine())!=null) {
                    String[] s=line.split("\\|");
                    try {
                        if(s[0].equals("PEER")&&s.length==6){
                            int pid=Integer.parseInt(s[1]);
                            if(pid!=id)others.put(pid,new Remote(Double.parseDouble(s[2]),Double.parseDouble(s[3]),Double.parseDouble(s[4]),Integer.parseInt(s[5])));
                        } else if(s[0].equals("LEFT")&&s.length==2)others.remove(Integer.parseInt(s[1]));
                        else if(s[0].equals("HIT")&&s.length==2)pendingDamage.addAndGet(Integer.parseInt(s[1]));
                        else if(s[0].equals("HP")&&s.length==3) {
                            Remote p=others.get(Integer.parseInt(s[1]));
                            if(p!=null)p.hp=Integer.parseInt(s[2]);
                        }
                    }catch(NumberFormatException ignored){}
                }
            }catch(IOException ignored){} 
        },"Veil-network-reader");
        t.setDaemon(true);t.start();
    }
    public int drainDamage(){return pendingDamage.getAndSet(0);}
    public synchronized void state(double x,double y,double angle){writer.println("STATE|"+x+"|"+y+"|"+angle);}
    public synchronized void fire(int gun){writer.println("FIRE|"+gun);}
    public synchronized void respawn(int index){writer.println("RESPAWN|"+index);}
    @Override public void close()throws IOException{socket.close();}
}
