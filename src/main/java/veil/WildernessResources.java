package veil;

import java.util.*;

/** Seeded wilderness gathering nodes indexed by continental macro parcels.
 * WorldHarvesting maps these nodes into the SAME world-metre coordinates used
 * by city actors, vehicles, combat and settlements; no second gameplay region.
 * Dense cities and existing highways never contain wilderness resource nodes.
 */
public final class WildernessResources {
    public enum Type { TIMBER, FIELD_STONE, FIBER, EDIBLES, MEDICINAL_HERBS, RAW_ORE, DIRTY_WATER }
    public record Node(int id,int x,int y,Type type,int capacity,int remaining,long replenishesAt) {}
    private static final class Resource {
        final int id,x,y,capacity;
        final Type type;
        int remaining;
        long replenishesAt;
        Resource(int id,int x,int y,Type type,int capacity){
            this.id=id;this.x=x;this.y=y;this.type=type;
            this.capacity=capacity;this.remaining=capacity;
        }
        Node view(){return new Node(id,x,y,type,capacity,remaining,replenishesAt);}
    }
    private static final long REPLENISH_MS=600_000;
    private final ContinentLayout map;
    private final List<Resource> nodes=new ArrayList<>();
    private long now;
    public WildernessResources(ContinentLayout map){
        this.map=Objects.requireNonNull(map);
        // Jitter one sampling candidate per 7x7 macro block, not a rectangular
        // arrangement of objects each N world coordinates.
        for(int by=4;by<ContinentLayout.SIZE-6;by+=7)
            for(int bx=4;bx<ContinentLayout.SIZE-6;bx+=7){
                long roll=World.mix(map.seed() ^ (bx*0x9e3779b97f4a7c15L) ^
                    (by*0x632be59bd9b4e019L));
                int x=bx+(int)Math.floorMod(roll,7);
                int y=by+(int)Math.floorMod(roll>>>16,7);
                if(map.cityProtected(x,y)||map.road(x,y)||!map.land(x,y)||
                    map.worldStructureAt(x,y))continue;
                Type type=switch(map.biome(x,y)){
                    case FOREST -> switch((int)(roll>>>23)&3){
                        case 0 -> Type.MEDICINAL_HERBS;
                        case 1 -> Type.EDIBLES;
                        case 2 -> Type.FIBER;
                        default -> Type.TIMBER;
                    };
                    case PLAINS -> (roll&3)==0?Type.EDIBLES:Type.FIBER;
                    case HILLS -> (roll&7)==0?Type.RAW_ORE:Type.FIELD_STONE;
                    case MOUNTAINS -> Type.FIELD_STONE;
                    case MARSH -> (roll&1)==0?Type.DIRTY_WATER:Type.MEDICINAL_HERBS;
                    case BEACH -> Type.FIELD_STONE;
                    case WATER -> null;
                };
                if(type==null)continue;
                int count=2+(int)((roll>>>36)&3);
                nodes.add(new Resource(nodes.size(),x,y,type,count));
            }
        if(nodes.isEmpty())throw new IllegalStateException("No wilderness harvesting sites");
    }
    public synchronized List<Node> nodes(){
        List<Node> result=new ArrayList<>(nodes.size());
        for(Resource resource:nodes)result.add(resource.view());
        return List.copyOf(result);
    }
    public synchronized Node node(int id){
        return id>=0&&id<nodes.size()?nodes.get(id).view():null;
    }
    public record Depletion(int id,int remaining,long replenishesAt) {}
    public record Snapshot(long seed,long clock,List<Depletion> depletion) {}
    /** Durable node state, keyed to the same deterministic continental seed. */
    public synchronized Snapshot snapshot(){
        List<Depletion> changes=new ArrayList<>();
        for(Resource r:nodes)if(r.remaining<r.capacity)
            changes.add(new Depletion(r.id,r.remaining,r.replenishesAt));
        return new Snapshot(map.seed(),now,List.copyOf(changes));
    }
    public synchronized void restore(Snapshot save){
        if(save==null||save.seed()!=map.seed()||save.clock()<0||
           save.depletion()==null||save.depletion().size()>nodes.size())
            throw new IllegalArgumentException("Invalid wilderness resource save");
        Set<Integer> seen=new HashSet<>();
        for(Depletion d:save.depletion()){
            if(d==null||d.id()<0||d.id()>=nodes.size()||!seen.add(d.id())||
               d.remaining()<0||d.remaining()>=nodes.get(d.id()).capacity||
               d.replenishesAt()<0||d.replenishesAt()>save.clock()+600_000L)
                throw new IllegalArgumentException("Corrupt wilderness depletion");
        }
        for(Resource r:nodes){r.remaining=r.capacity;r.replenishesAt=0;}
        now=save.clock();
        for(Depletion d:save.depletion()){
            Resource r=nodes.get(d.id());
            r.remaining=d.remaining();r.replenishesAt=d.replenishesAt();
        }
    }
    /** Exact coordinate/range validation belongs to server's player position. */
    public synchronized Type harvest(int id,int x,int y){
        if(id<0||id>=nodes.size())return null;
        Resource r=nodes.get(id);
        if(r.remaining<=0||Math.hypot(r.x-x,r.y-y)>1.3)return null;
        if(!map.buildableGround(r.x,r.y)&&map.biome(r.x,r.y)!=ContinentLayout.Biome.MOUNTAINS)
            return null;
        r.remaining--;
        if(r.remaining==0)r.replenishesAt=now+REPLENISH_MS;
        return r.type;
    }
    public synchronized void advance(long elapsed){
        if(elapsed<0||elapsed>REPLENISH_MS)throw new IllegalArgumentException("Bad time step");
        now+=elapsed;
        for(Resource r:nodes)if(r.remaining==0&&r.replenishesAt<=now){
            r.remaining=r.capacity;r.replenishesAt=0;
        }
    }
}
