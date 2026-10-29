package veil;

import java.util.*;

/**
 * Server-owned city scavenging and temporary item handoffs. This service never
 * accepts a client-supplied item yield, trade inventory, or pickup position.
 * The host must provide authoritative character positions and inventories.
 */
public final class CityLootEconomy {
    public static final long GROUND_LIFETIME_MS=20L*60_000L;
    public enum Source { DUMPSTER, TRUCK }
    public record Container(int id,Source source,double x,double y,
                            SurvivalCrafting.Item item,int quantity) {}
    public record GroundItem(long id,double x,double y,SurvivalCrafting.Item item,
                             int quantity,long expiresAtMs) {}
    private final World world;
    private final Map<Integer,Container> containers=new LinkedHashMap<>();
    private final Set<Integer> looted=new HashSet<>();
    private final Map<Long,GroundItem> ground=new LinkedHashMap<>();
    private long nextDropId=1;

    public CityLootEconomy(World world){
        this.world=Objects.requireNonNull(world);
        int index=0;
        for(World.UrbanProp prop:world.urbanProps){
            Source source=prop.model().equals("detail-dumpster-closed")?
                Source.DUMPSTER:prop.model().startsWith("truck-")?Source.TRUCK:null;
            if(source==null)continue;
            if(world.city.categoryAt((int)prop.x(),(int)prop.y())==
                CityLayout.Category.SAFE)continue;
            long roll=World.mix(world.seed^((long)index*0x9e3779b97f4a7c15L)^
                Double.doubleToLongBits(prop.x())^
                Long.rotateLeft(Double.doubleToLongBits(prop.y()),17));
            SurvivalCrafting.Item[] pool=source==Source.TRUCK?
                new SurvivalCrafting.Item[]{SurvivalCrafting.Item.FIBER,
                    SurvivalCrafting.Item.FIELD_STONE,SurvivalCrafting.Item.RAW_ORE,
                    SurvivalCrafting.Item.TIMBER,SurvivalCrafting.Item.EDIBLES}:
                new SurvivalCrafting.Item[]{SurvivalCrafting.Item.FIBER,
                    SurvivalCrafting.Item.EDIBLES,SurvivalCrafting.Item.DIRTY_WATER,
                    SurvivalCrafting.Item.MEDICINAL_HERBS,SurvivalCrafting.Item.TIMBER};
            int amount=1+(int)Math.floorMod(roll>>>19,3);
            containers.put(index,new Container(index,source,prop.x(),prop.y(),
                pool[(int)Math.floorMod(roll,pool.length)],amount));
            index++;
        }
    }
    public synchronized List<Container> containers(){return List.copyOf(containers.values());}
    public synchronized boolean looted(int id){return looted.contains(id);}
    /** Container is depleted only after the carried-inventory commit succeeds. */
    public synchronized boolean scavenge(int id,double x,double y,SurvivalCrafting bag){
        Container c=containers.get(id);
        if(c==null||looted.contains(id)||bag==null||!near(x,y,c.x(),c.y(),3)||
            !world.lineClear(x,y,c.x(),c.y())||
            !bag.acquire(c.item(),c.quantity(),false))return false;
        looted.add(id);
        return true;
    }
    private static boolean near(double ax,double ay,double bx,double by,double range){
        return Double.isFinite(ax)&&Double.isFinite(ay)&&
            Double.isFinite(bx)&&Double.isFinite(by)&&
            Math.hypot(ax-bx,ay-by)<=range;
    }
    /** Ground items expire in real elapsed host time even with no players nearby. */
    public synchronized void expire(long nowMs){
        ground.values().removeIf(g->nowMs>=g.expiresAtMs());
    }
    public synchronized List<GroundItem> ground(long nowMs){
        expire(nowMs);return List.copyOf(ground.values());
    }
    public synchronized boolean drop(SurvivalCrafting bag,SurvivalCrafting.Item item,
                int amount,double x,double y,long nowMs){
        expire(nowMs);
        if(bag==null||item==null||amount<1||amount>1000||
            !Double.isFinite(x)||!Double.isFinite(y)||nowMs<0||
            ground.size()>=1024||!world.canWalk(x,y)||!bag.debit(item,amount))return false;
        long id=nextDropId++;
        ground.put(id,new GroundItem(id,x,y,item,amount,nowMs+GROUND_LIFETIME_MS));
        return true;
    }
    public synchronized boolean pickup(long id,SurvivalCrafting recipient,
                double x,double y,long nowMs){
        expire(nowMs);
        GroundItem g=ground.get(id);
        if(g==null||recipient==null||!near(x,y,g.x(),g.y(),2.5)||
            !world.lineClear(x,y,g.x(),g.y())||
            !recipient.acquire(g.item(),g.quantity(),false))return false;
        ground.remove(id);
        return true;
    }
    /** The calling host supplies actual player coordinates and facing. */
    public synchronized boolean trade(SurvivalCrafting giver,SurvivalCrafting receiver,
                SurvivalCrafting.Item item,int count,
                double gx,double gy,double facing,double rx,double ry){
        if(giver==null||receiver==null||giver==receiver||
            !Double.isFinite(facing)||!near(gx,gy,rx,ry,3)||
            !world.lineClear(gx,gy,rx,ry))return false;
        double dx=rx-gx,dy=ry-gy;
        if(dx*Math.cos(facing)+dy*Math.sin(facing)<
            Math.hypot(dx,dy)*Math.cos(Math.PI/6))return false;
        return giver.transferTo(receiver,item,count);
    }
}
