package veil;

import java.util.*;

/**
 * Immutable continental sector materialization for host-side interest management.
 * Sector coordinates refer to macro parcels, never legacy city-arena tiles.
 * No mutable gameplay entities are created here: spawn markers are instructions
 * for the authoritative actor service, not extra NPC instances per subscriber.
 */
public final class ContinentSectors {
    public static final int WIDTH=8;
    public static final int COUNT=(ContinentLayout.SIZE+WIDTH-1)/WIDTH;
    public record Key(int x,int y) {}
    public record Parcel(int x,int y,ContinentLayout.Biome biome,double elevation,
                         char city,boolean road,boolean worldOwned,boolean buildable) {}
    public record Sector(Key key,List<Parcel> parcels,
                         List<ContinentPopulation.PlayerSpawn> playerSpawns,
                         List<ContinentPopulation.NpcSpawn> npcSpawns) {}
    private final ContinentLayout layout;
    private final ContinentPopulation population;
    private final Map<Key,List<ContinentPopulation.PlayerSpawn>> playerIndex=new HashMap<>();
    private final Map<Key,List<ContinentPopulation.NpcSpawn>> npcIndex=new HashMap<>();
    private final Map<Key,Sector> cache=new HashMap<>();
    public ContinentSectors(ContinentLayout layout,ContinentPopulation population) {
        this.layout=Objects.requireNonNull(layout);
        this.population=Objects.requireNonNull(population);
        for(var spawn:population.playerSpawns())
            playerIndex.computeIfAbsent(keyFor(spawn.x(),spawn.y()),k->new ArrayList<>()).add(spawn);
        for(var spawn:population.npcSpawns())
            npcIndex.computeIfAbsent(keyFor(spawn.x(),spawn.y()),k->new ArrayList<>()).add(spawn);
    }
    public static Key keyFor(int x,int y) {
        if(x<0||y<0||x>=ContinentLayout.SIZE||y>=ContinentLayout.SIZE)
            throw new IllegalArgumentException("Parcel outside continent");
        return new Key(x/WIDTH,y/WIDTH);
    }
    public synchronized Sector sector(Key key) {
        if(key==null||key.x()<0||key.y()<0||key.x()>=COUNT||key.y()>=COUNT)
            throw new IllegalArgumentException("Invalid sector");
        Sector result=cache.get(key);
        if(result!=null)return result;
        List<Parcel> parcels=new ArrayList<>();
        for(int y=key.y()*WIDTH;y<Math.min((key.y()+1)*WIDTH,ContinentLayout.SIZE);y++)
            for(int x=key.x()*WIDTH;x<Math.min((key.x()+1)*WIDTH,ContinentLayout.SIZE);x++)
                parcels.add(new Parcel(x,y,layout.biome(x,y),layout.elevation(x,y),
                    layout.cityAt(x,y),layout.road(x,y),layout.worldStructureAt(x,y),
                    layout.buildableGround(x,y)));
        result=new Sector(key,List.copyOf(parcels),
            List.copyOf(playerIndex.getOrDefault(key,List.of())),
            List.copyOf(npcIndex.getOrDefault(key,List.of())));
        cache.put(key,result);
        return result;
    }
    /** Stable, clamped region around an actor, suitable for per-peer subscriptions. */
    public List<Key> interest(int x,int y,int radius) {
        if(radius<0||radius>8)throw new IllegalArgumentException("Invalid radius");
        Key center=keyFor(x,y);
        List<Key> keys=new ArrayList<>();
        for(int cy=Math.max(0,center.y()-radius);cy<=Math.min(COUNT-1,center.y()+radius);cy++)
            for(int cx=Math.max(0,center.x()-radius);cx<=Math.min(COUNT-1,center.x()+radius);cx++)
                keys.add(new Key(cx,cy));
        return List.copyOf(keys);
    }
    public synchronized int cachedSectorCount(){return cache.size();}
}
