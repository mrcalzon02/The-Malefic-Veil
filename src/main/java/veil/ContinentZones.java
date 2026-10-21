package veil;

import java.util.*;

/** Stable macro-zone addressing above 8x8 parcel sectors, without creating actors. */
public final class ContinentZones {
    public static final int SECTORS_PER_ZONE=4;
    public static final int ZONES_PER_AXIS=(ContinentSectors.COUNT+SECTORS_PER_ZONE-1)/SECTORS_PER_ZONE;
    public record Key(int x,int y) {}
    public record Bounds(int minParcelX,int minParcelY,int maxParcelXExclusive,int maxParcelYExclusive) {}
    private ContinentZones(){}
    public static Key ofParcel(int x,int y){
        ContinentSectors.Key sector=ContinentSectors.keyFor(x,y);
        return ofSector(sector);
    }
    public static Key ofSector(ContinentSectors.Key sector){
        Objects.requireNonNull(sector);
        if(sector.x()<0||sector.y()<0||sector.x()>=ContinentSectors.COUNT||sector.y()>=ContinentSectors.COUNT)
            throw new IllegalArgumentException("Sector outside world");
        return new Key(sector.x()/SECTORS_PER_ZONE,sector.y()/SECTORS_PER_ZONE);
    }
    public static Bounds bounds(Key key){
        if(key==null||key.x()<0||key.y()<0||key.x()>=ZONES_PER_AXIS||key.y()>=ZONES_PER_AXIS)
            throw new IllegalArgumentException("Zone outside world");
        int unit=ContinentSectors.WIDTH*SECTORS_PER_ZONE;
        return new Bounds(key.x()*unit,key.y()*unit,
            Math.min(ContinentLayout.SIZE,(key.x()+1)*unit),
            Math.min(ContinentLayout.SIZE,(key.y()+1)*unit));
    }
    /** Stable cardinal-plus-diagonal prewarm neighborhood; includes the owning zone. */
    public static List<Key> neighborhood(Key key,int radius){
        bounds(key);
        if(radius<0||radius>2)throw new IllegalArgumentException("Invalid prewarm radius");
        List<Key> output=new ArrayList<>();
        for(int y=Math.max(0,key.y()-radius);y<=Math.min(ZONES_PER_AXIS-1,key.y()+radius);y++)
            for(int x=Math.max(0,key.x()-radius);x<=Math.min(ZONES_PER_AXIS-1,key.x()+radius);x++)
                output.add(new Key(x,y));
        return List.copyOf(output);
    }
}
