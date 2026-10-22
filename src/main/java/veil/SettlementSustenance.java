package veil;

import java.util.*;

/**
 * Third settlement pressure: faction-bound sustenance recovered on expedition.
 * Host-only ledger. World nodes are deterministic; settlement balances and
 * spent charges remain server-owned and must be persisted by the realm service.
 */
public final class SettlementSustenance {
    public enum Material { RESONANCE_SHARD, PENITENT_RELIC, LOST_SOUL }
    public record Node(int id,double x,double y,Material material,int yield) {}
    public record Account(CharacterRoster.Faction faction,int level,int stored,
                          long elapsedMs,boolean active) {}
    private static final class Balance {
        CharacterRoster.Faction faction;
        int level,stored;
        long elapsed;
        boolean active=true;
        Balance(CharacterRoster.Faction f,int level){faction=f;this.level=level;}
        Account view(){return new Account(faction,level,stored,elapsed,active);}
    }
    private final World world;
    private final List<Node> nodes=new ArrayList<>();
    private final Set<Integer> harvested=new HashSet<>();
    private final Map<String,Balance> settlements=new TreeMap<>();
    public static final long HOUR=3_600_000L;
    public static final int RESERVE_HOURS=168;
    public static int reserveCapacity(int level){
        int rate=hourlyCost(level);
        return rate==0?1008:rate*RESERVE_HOURS;
    }
    public SettlementSustenance(World world){
        this.world=Objects.requireNonNull(world);
        int id=0;
        for(CityLayout.Plot plot:world.city.plots()){
            var category=plot.template().category();
            if(category==CityLayout.Category.SAFE)continue;
            long roll=World.mix(world.seed^((long)plot.x()<<32)^plot.y()^0x734714cdda45L);
            // Dense city concentrations, with a significant but not universal
            // chance of finding a cache on a walkable plot.
            int chance=category==CityLayout.Category.RESOURCE?70:
                category==CityLayout.Category.HAZARD?55:24;
            if(Math.floorMod(roll,100)>=chance)continue;
            Material material=((roll>>>10)&1)==0?
                Material.RESONANCE_SHARD:Material.PENITENT_RELIC;
            int yield=2+(int)Math.floorMod(roll>>>22,4);
            boolean placed=false;
            for(int x=plot.x()*CityLayout.CELL+1;x<plot.right()*CityLayout.CELL-1&&!placed;x++)
                for(int y=plot.y()*CityLayout.CELL+1;y<plot.bottom()*CityLayout.CELL-1&&!placed;y++){
                    double px=x+.5,py=y+.5;
                    if(!world.canWalk(px,py))continue;
                    boolean close=false;
                    for(double[] spawn:world.spawns)
                        if(Math.hypot(px-spawn[0],py-spawn[1])<7)close=true;
                    if(close)continue;
                    nodes.add(new Node(id++,px,py,material,yield));
                    placed=true;
                }
        }
    }
    public static Material material(CharacterRoster.Faction faction){
        return switch(Objects.requireNonNull(faction)){
            case THE_UNRAVELED -> Material.RESONANCE_SHARD;
            case THE_PENITENT -> Material.PENITENT_RELIC;
            case THE_BEGOTTEN -> Material.LOST_SOUL;
        };
    }
    /** Costs begin only once a settlement reaches the high-base tiers. */
    public static int hourlyCost(int level){
        if(level<1||level>50)throw new IllegalArgumentException("level 1..50");
        if(level<10)return 0;
        return 1+(level-10)/8; // 1 at 10, 6 at level 50
    }
    /** Gathering is a meaningful skill multiplier without infinite extraction. */
    public static int harvestedAmount(int base,int scavengingLevel){
        if(base<1||base>100||scavengingLevel<1||scavengingLevel>50)
            throw new IllegalArgumentException("invalid yield or skill");
        return base+(base*(scavengingLevel-1))/49; // up to double at 50
    }
    public synchronized List<Node> nodes(){return List.copyOf(nodes);}
    public synchronized boolean register(String id,CharacterRoster.Faction faction,int level){
        if(id==null||!id.matches("[A-Za-z0-9_-]{3,96}")||faction==null||
            level<1||level>50||settlements.containsKey(id))return false;
        settlements.put(id,new Balance(faction,level));return true;
    }
    public synchronized Account account(String id){
        Balance b=settlements.get(id);return b==null?null:b.view();
    }
    public synchronized boolean setLevel(String id,int level){
        Balance b=settlements.get(id);
        if(b==null||level<1||level>50)return false;
        b.level=level;return true;
    }
    public synchronized boolean deposit(String id,Material item,int units){
        Balance b=settlements.get(id);
        if(b==null||item!=material(b.faction)||units<1||units>10000||
            b.stored>reserveCapacity(b.level)-units)return false;
        b.stored+=units;
        if(b.stored>=hourlyCost(b.level))b.active=true;
        return true;
    }
    /** Host validates player identity/faction and location before calling. */
    public synchronized int harvest(String settlement,Material kind,int nodeId,
                                     double x,double y,int skill){
        Balance b=settlements.get(settlement);
        if(b==null||b.faction==CharacterRoster.Faction.THE_BEGOTTEN||
            material(b.faction)!=kind||nodeId<0||nodeId>=nodes.size()||
            harvested.contains(nodeId))return 0;
        Node n=nodes.get(nodeId);
        if(n.material()!=kind||Math.hypot(x-n.x(),y-n.y())>2.5||
            !world.lineClear(x,y,n.x(),n.y()))return 0;
        int amount=harvestedAmount(n.yield(),skill);
        if(!deposit(settlement,kind,amount))return 0;
        harvested.add(nodeId);return amount;
    }
    /** Award a soul only after an actual Lost death, never per shot or respawn. */
    public synchronized int lostKill(String settlement,int scavengingLevel){
        Balance b=settlements.get(settlement);
        if(b==null||b.faction!=CharacterRoster.Faction.THE_BEGOTTEN)return 0;
        int n=harvestedAmount(2,scavengingLevel);
        return deposit(settlement,Material.LOST_SOUL,n)?n:0;
    }
    /** Whole-hours charged exactly once; unfunded bases become dormant, not deleted. */
    public synchronized void advance(String settlement,long elapsedMs){
        Balance b=settlements.get(settlement);
        if(b==null||elapsedMs<0||elapsedMs>HOUR)throw new IllegalArgumentException("tick");
        b.elapsed+=elapsedMs;
        while(b.elapsed>=HOUR){
            b.elapsed-=HOUR;
            int charge=hourlyCost(b.level);
            if(b.stored>=charge){b.stored-=charge;b.active=true;}
            else b.active=false;
        }
    }
}
