package veil;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
/** Thin async LAN wire client; never modifies Swing components on socket reader. */
public final class NetLink implements AutoCloseable {
    public static final class Remote {
        public volatile double x,y,angle,opacity;
        public volatile int hp,classId;
        Remote(double x,double y,double a,int hp,double opacity,int classId){
            this.x=x;this.y=y;this.angle=a;this.hp=hp;this.opacity=opacity;this.classId=classId;
        }
    }
    public record Rpg(int level,long experience,long next,long aliveMs,double streakBonus,
                      int marksmanship,int athletics,int engineering,int scavenging,int survival,
                      long protectionMs,int hunger,int hydration){}
    public record NpcRank(int level,String archetype){}
    public record Self(int health,int sanity,int cash,int supplies,int medkits,
        int pistolReserve,int shellReserve,int pistolClip,int shellClip,boolean dead,int frags,
        double x,double y,double angle,double visibility){}
    public record DeviceEvent(int index,int kind,double x,double y,int hp){}
    public record NpcEvent(int index,double x,double y,int hp){}
    public record ShotEvent(long sequence,boolean fired,String outcome,int targetId,int damage,int ammo){}
    public record BuildEvent(int kind,String reason,int cashCost,int suppliesCost){}
    public record Car(int id,double x,double y,double heading,double speed,double fuel,int hp,int driverId){}
    public record CargoYard(int id,int x,int y,String type,int stock){}
    public record CargoDrop(int id,double x,double y,String type){}

    private final Socket socket;
    private final PrintWriter writer;
    public final long seed;
    public final int id;
    public final ConcurrentHashMap<Integer,Remote> others=new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Integer,Car> cars=new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Integer,NpcRank> npcRanks=new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String,Integer> survivalInventory=new ConcurrentHashMap<>();
    private volatile Rpg rpg;
    private volatile boolean hardcore;
    private volatile String faction="NONE",crew="NONE",crewRole="NONE";
    public Rpg rpg(){return rpg;}
    public boolean hardcore(){return hardcore;}
    public String faction(){return faction;}
    public String crew(){return crew;}
    public String crewRole(){return crewRole;}

    public final ConcurrentHashMap<Integer,String> truckCargo=new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Integer,CargoYard> cargoYards=new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Integer,CargoDrop> cargoDrops=new ConcurrentHashMap<>();
    public final ConcurrentLinkedQueue<String> cargoResults=new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<String> factionResults=new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<String> craftResults=new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<String> rideResults=new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<Integer> pickedUp=new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<DeviceEvent> structures=new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<NpcEvent> npcs=new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<ShotEvent> shots=new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<BuildEvent> buildResults=new ConcurrentLinkedQueue<>();
    private final AtomicLong shotIds=new AtomicLong();
    private volatile Self self;
    private volatile String accountKey;
    private volatile boolean connected=true;
    private volatile long roundTripMs=-1;
    private final AtomicLong pingIds=new AtomicLong();
    private volatile long waitingPing=-1,pingSentNanos=0;
    private volatile int[] spawnScores={0,0,0};
    private volatile long respawnRemainingMs=0;
    private volatile long respawnTimerReceivedAt=System.nanoTime();
    public long respawnRemainingMs(){
        long elapsed=TimeUnit.NANOSECONDS.toMillis(Math.max(0,System.nanoTime()-respawnTimerReceivedAt));
        return Math.max(0,respawnRemainingMs-elapsed);
    }
    public final ConcurrentLinkedQueue<String> respawnResults=new ConcurrentLinkedQueue<>();
    public NetLink(String host,int port)throws IOException {
        socket=new Socket();
        socket.connect(new InetSocketAddress(host,port),4000);
        BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);
        String hello=reader.readLine();
        String[] parts=hello==null?new String[0]:hello.split("\\|");
        if(parts.length!=4||!parts[0].equals("WELCOME")||!parts[3].equals("3"))
            throw new IOException("Unsupported or invalid LAN protocol");
        try{id=Integer.parseInt(parts[1]);seed=Long.parseLong(parts[2]);}
        catch(NumberFormatException e){throw new IOException("Malformed host greeting",e);}
        writer.println("AUTH|"+PlayerIdentity.forServer(host,port));
        if(writer.checkError())throw new IOException("Authentication greeting not sent");
        Thread t=new Thread(()->{
            String line;
            try{
                while((line=reader.readLine())!=null){
                    String[] s=line.split("\\|");
                    try{
                        switch(s[0]){
                            case "PEER" -> {
                                if(s.length!=8)continue;
                                int pid=Integer.parseInt(s[1]);
                                if(pid!=id)others.put(pid,new Remote(Double.parseDouble(s[2]),
                                    Double.parseDouble(s[3]),Double.parseDouble(s[4]),Integer.parseInt(s[5]),
                                    ArenaSystems.opacity(Double.parseDouble(s[6])),Integer.parseInt(s[7])));
                            }
                            case "LEFT" -> {if(s.length==2)others.remove(Integer.parseInt(s[1]));}
                            case "PICKUP" -> {if(s.length==2)pickedUp.add(Integer.parseInt(s[1]));}
                            case "DEVICE" -> {
                                if(s.length==6)structures.add(new DeviceEvent(Integer.parseInt(s[1]),
                                    Integer.parseInt(s[2]),Double.parseDouble(s[3]),Double.parseDouble(s[4]),Integer.parseInt(s[5])));
                            }
                            case "SERVER_MODE" -> {
                                if(s.length==2)hardcore=s[1].equals("HARDCORE");
                            }
                            case "CRAFT_RESULT" -> {
                                if(s.length==3)craftResults.add(s[1]+"|"+s[2]);
                            }
                            case "FACTION_RESULT", "CREW_RESULT", "CREW_INVITE" -> {
                                factionResults.add(line);
                            }
                            case "FACTION_STATE" -> {
                                if(s.length==4){faction=s[1];crew=s[2];crewRole=s[3];}
                            }
                            case "NPC_RANK" -> {
                                if(s.length==4)npcRanks.put(Integer.parseInt(s[1]),
                                    new NpcRank(Integer.parseInt(s[2]),s[3]));
                            }
                            case "RPG" -> {
                                if(s.length==14)rpg=new Rpg(Integer.parseInt(s[1]),
                                    Long.parseLong(s[2]),Long.parseLong(s[3]),
                                    Long.parseLong(s[4]),Double.parseDouble(s[5]),
                                    Integer.parseInt(s[6]),Integer.parseInt(s[7]),
                                    Integer.parseInt(s[8]),Integer.parseInt(s[9]),
                                    Integer.parseInt(s[10]),Long.parseLong(s[11]),
                                    Integer.parseInt(s[12]),Integer.parseInt(s[13]));
                            }
                            case "INVENTORY" -> {
                                if(s.length==2){
                                    survivalInventory.clear();
                                    if(!s[1].equals("-"))for(String pair:s[1].split(",")){
                                        String[] pairData=pair.split("=");
                                        if(pairData.length==2)survivalInventory.put(
                                            pairData[0],Integer.parseInt(pairData[1]));
                                    }
                                }
                            }
                            case "NPC" -> {
                                if(s.length==5)npcs.add(new NpcEvent(Integer.parseInt(s[1]),
                                    Double.parseDouble(s[2]),Double.parseDouble(s[3]),Integer.parseInt(s[4])));
                            }
                            case "SHOT" -> {
                                if(s.length==7)shots.add(new ShotEvent(Long.parseLong(s[1]),
                                    Boolean.parseBoolean(s[2]),s[3],Integer.parseInt(s[4]),
                                    Integer.parseInt(s[5]),Integer.parseInt(s[6])));
                            }
                            case "BUILD_RESULT" -> {
                                if(s.length==5)buildResults.add(new BuildEvent(Integer.parseInt(s[1]),
                                    s[2],Integer.parseInt(s[3]),Integer.parseInt(s[4])));
                            }
                            case "CAR" -> {
                                if(s.length==9){
                                    Car car=new Car(Integer.parseInt(s[1]),Double.parseDouble(s[2]),
                                        Double.parseDouble(s[3]),Double.parseDouble(s[4]),Double.parseDouble(s[5]),
                                        Double.parseDouble(s[6]),Integer.parseInt(s[7]),Integer.parseInt(s[8]));
                                    if(Double.isFinite(car.x())&&Double.isFinite(car.y())&&
                                       Double.isFinite(car.heading())&&Double.isFinite(car.speed())&&
                                       Double.isFinite(car.fuel()))cars.put(car.id(),car);
                                }
                            }
                            case "TRUCK_CARGO" -> {
                                if(s.length==3){
                                    int car=Integer.parseInt(s[1]);
                                    if(s[2].equals("NONE"))truckCargo.remove(car);
                                    else if(s[2].equals("METAL_STOCK")||
                                        s[2].equals("MACHINE_PARTS"))truckCargo.put(car,s[2]);
                                }
                            }
                            case "CARGO_YARD" -> {
                                if(s.length==6)cargoYards.put(Integer.parseInt(s[1]),
                                    new CargoYard(Integer.parseInt(s[1]),Integer.parseInt(s[2]),
                                        Integer.parseInt(s[3]),s[4],Integer.parseInt(s[5])));
                            }
                            case "CARGO_DROP" -> {
                                if(s.length==5)cargoDrops.put(Integer.parseInt(s[1]),
                                    new CargoDrop(Integer.parseInt(s[1]),Double.parseDouble(s[2]),
                                        Double.parseDouble(s[3]),s[4]));
                            }
                            case "CARGO_DROP_IDS" -> {
                                if(s.length==2){
                                    if(s[1].equals("-"))cargoDrops.clear();
                                    else{
                                        java.util.HashSet<Integer> active=new java.util.HashSet<>();
                                        for(String text:s[1].split(","))active.add(Integer.parseInt(text));
                                        cargoDrops.keySet().removeIf(id->!active.contains(id));
                                    }
                                }
                            }
                            case "CARGO_DROP_TAKEN" -> {
                                if(s.length==2)cargoDrops.remove(Integer.parseInt(s[1]));
                            }
                            case "CARGO_RESULT" -> {
                                if(s.length==3)cargoResults.add(s[1]+"|"+s[2]);
                            }
                            case "RIDE_RESULT" -> {
                                if(s.length==2)rideResults.add(s[1]);
                            }
                            case "AUTH_OK" -> {
                                if(s.length==2)accountKey=s[1];
                            }
                            case "AUTH_DENIED" -> {connected=false;}
                            case "PONG" -> {
                                if(s.length==2){
                                    long nonce=Long.parseLong(s[1]);
                                    if(nonce==waitingPing&&pingSentNanos!=0){
                                        roundTripMs=Math.max(0,TimeUnit.NANOSECONDS.toMillis(
                                            System.nanoTime()-pingSentNanos));
                                        waitingPing=-1;
                                    }
                                }
                            }
                            case "SPAWNS" -> {
                                if(s.length==4)spawnScores=new int[]{
                                    Integer.parseInt(s[1]),Integer.parseInt(s[2]),Integer.parseInt(s[3])};
                            }
                            case "RESPAWN_TIMER" -> {
                                if(s.length==2){
                                    respawnRemainingMs=Math.max(0,Long.parseLong(s[1]));
                                    respawnTimerReceivedAt=System.nanoTime();
                                }
                            }
                            case "RESPAWN_RESULT" -> {
                                if(s.length==3)respawnResults.add(s[2]);
                            }
                            case "SELF" -> {
                                if(s.length==16)self=new Self(Integer.parseInt(s[1]),Integer.parseInt(s[2]),
                                    Integer.parseInt(s[3]),Integer.parseInt(s[4]),Integer.parseInt(s[5]),
                                    Integer.parseInt(s[6]),Integer.parseInt(s[7]),Integer.parseInt(s[8]),
                                    Integer.parseInt(s[9]),Integer.parseInt(s[10])!=0,Integer.parseInt(s[11]),
                                    Double.parseDouble(s[12]),Double.parseDouble(s[13]),Double.parseDouble(s[14]),
                                    ArenaSystems.opacity(Double.parseDouble(s[15])));
                            }
                            default -> {}
                        }
                    }catch(NumberFormatException ignored){}
                }
            }catch(IOException ignored){}
            finally{connected=false;}
        },"Veil-LAN-reader");
        t.setDaemon(true);t.start();
    }
    public Self self(){return self;}
    public String accountKey(){return accountKey;}
    public int[] spawnScores(){return spawnScores.clone();}
    public boolean connected(){return connected;}
    public long roundTripMs(){return roundTripMs;}
    private synchronized void send(String s){
        if(!connected)return;
        writer.println(s);
        if(writer.checkError())connected=false;
    }
    public synchronized void ping(){
        if(!connected)return;
        long nonce=pingIds.incrementAndGet();
        waitingPing=nonce;pingSentNanos=System.nanoTime();
        send("PING|"+nonce);
    }
    public void choose(int id){send("CHOOSE|"+id);}
    public void state(double x,double y,double angle,int stanceFlags){send("STATE|"+x+"|"+y+"|"+angle+"|"+stanceFlags);}
    public long fire(int gun){
        long sequence=shotIds.incrementAndGet();
        send("FIRE|"+sequence+"|"+gun);
        return sequence;
    }
    public void pickup(int index){send("PICKUP|"+index);}
    public void useMedkit(){send("MEDKIT");}
    public void reload(int weapon){send("RELOAD|"+weapon);}
    public void ability(){send("ABILITY");}
    public void build(int kind){send("BUILD|"+kind);}
    public void respawn(int index){send("RESPAWN|"+index);}
    public void sceneReady(){send("SCENE_READY");}
    public void craftHand(String recipeId){send("CRAFT_HAND|"+recipeId);}
    public void selectFaction(CharacterRoster.Faction chosen){send("FACTION_JOIN|"+chosen.name());}
    public void createCrew(String name){send("CREW_CREATE|"+name);}
    public void inviteCrew(int targetPlayerId){send("CREW_INVITE|"+targetPlayerId);}
    public void acceptCrew(){send("CREW_ACCEPT");}
    public void leaveCrew(){send("CREW_LEAVE");}
    public void transferCrew(int successorId){send("CREW_TRANSFER|"+successorId);}
    public void disbandCrew(){send("CREW_DISBAND");}
    public int drivenCar(){
        for(Car car:cars.values())if(car.driverId()==id)return car.id();
        return -1;
    }
    public void ride(int index){send("RIDE|"+index);}
    public void exitCar(){send("EXIT");}
    public void drive(double throttle,double steering){send("DRIVE|"+throttle+"|"+steering);}
    public void loadCargo(int truck,int yard){send("CARGO_LOAD|"+truck+"|"+yard);}
    public void recoverCargo(int truck,int drop){send("CARGO_RECOVER|"+truck+"|"+drop);}

    @Override public void close()throws IOException{connected=false;socket.close();}
}
