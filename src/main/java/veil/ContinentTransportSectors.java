package veil;

import java.util.*;

/**
 * Read-only transport overlay for the existing 8x8 continental macro sectors.
 * Each entry comes from a real paid host entity, never from a predicted,
 * client-generated or immutable worldgen blueprint. No live socket authority
 * or GPU asset binding is provided by this class.
 */
public final class ContinentTransportSectors {
    /** Public visual descriptors never expose owner IDs, cargo, fuel,
     * delivery receipts, workstation inventories or access permissions.
     */
    public record SegmentVisual(int x,int y,TransportInfrastructure.Kind kind,
            TransportInfrastructure.Axis axis,int hp,CharacterRoster.Faction faction) {}
    public record RunwayVisual(int id,int startX,int startY,
            TransportInfrastructure.Axis axis,int count,int constructed,
            CharacterRoster.Faction faction) {}
    public record TrainVisual(int id,double x,double y,
            TransportInfrastructure.Axis axis,int direction,int hp,int wagons,
            CharacterRoster.Faction faction) {}
    public record CarrierVisual(int id,double x,double y,AircraftCarrier.Axis axis,
            int lengthParcels,int deckHp,CharacterRoster.Faction faction) {}
    public record View(ContinentSectors.Key key,
            List<SegmentVisual> segments,
            List<RunwayVisual> runways,
            List<TrainVisual> locomotives,
            List<CarrierVisual> carriers) {}
    private final TransportInfrastructure ground;
    private final RailTransport rail;
    private final AircraftCarrier marine;
    public ContinentTransportSectors(TransportInfrastructure ground,
            RailTransport rail,AircraftCarrier marine){
        this.ground=Objects.requireNonNull(ground);
        this.rail=Objects.requireNonNull(rail);
        this.marine=Objects.requireNonNull(marine);
        if(rail.infrastructure()!=ground||marine.world()!=ground.world()||
           marine.miasma()!=ground.miasma()||
           marine.faction()!=ground.faction()||
           marine.ownerId()!=ground.ownerId())
            throw new IllegalArgumentException("Mixed world, faction or ownership");
    }
    private static boolean within(int x,int y,int sx,int sy){
        return x/ContinentSectors.WIDTH==sx&&y/ContinentSectors.WIDTH==sy;
    }
    private static boolean overlaps(int lo,int hi,int sector){
        int left=sector*ContinentSectors.WIDTH;
        int right=left+ContinentSectors.WIDTH-1;
        return lo<=right&&hi>=left;
    }
    private static boolean runwayOverlaps(TransportInfrastructure.Runway r,
                                          int sx,int sy){
        int lastX=r.startX()+(r.axis()==TransportInfrastructure.Axis.EAST_WEST?r.count()-1:0);
        int lastY=r.startY()+(r.axis()==TransportInfrastructure.Axis.NORTH_SOUTH?r.count()-1:0);
        return overlaps(r.startX(),lastX,sx)&&overlaps(r.startY(),lastY,sy);
    }
    private static boolean deckOverlaps(AircraftCarrier.Deck d,int sx,int sy){
        double half=AircraftCarrier.LENGTH/2.0;
        int minX=(int)Math.floor(d.x()-(d.axis()==AircraftCarrier.Axis.EAST_WEST?
            half:AircraftCarrier.HALF_WIDTH+.5));
        int maxX=(int)Math.floor(d.x()+(d.axis()==AircraftCarrier.Axis.EAST_WEST?
            half:AircraftCarrier.HALF_WIDTH+.5));
        int minY=(int)Math.floor(d.y()-(d.axis()==AircraftCarrier.Axis.NORTH_SOUTH?
            half:AircraftCarrier.HALF_WIDTH+.5));
        int maxY=(int)Math.floor(d.y()+(d.axis()==AircraftCarrier.Axis.NORTH_SOUTH?
            half:AircraftCarrier.HALF_WIDTH+.5));
        return overlaps(minX,maxX,sx)&&overlaps(minY,maxY,sy);
    }
    public View view(ContinentSectors.Key key){
        if(key==null||key.x()<0||key.y()<0||
           key.x()>=ContinentSectors.COUNT||key.y()>=ContinentSectors.COUNT)
            throw new IllegalArgumentException("Sector outside continental authority");
        // Snapshot under one lock order: rail -> carrier -> infrastructure.
        // Rail runtime requests infrastructure under the rail monitor.
        // Do not reverse this order or call source-factory APIs here.
        final TransportInfrastructure.Snapshot site;
        final RailTransport.Snapshot locomotives;
        final AircraftCarrier.Snapshot fleet;
        synchronized(rail){
            synchronized(marine){
                synchronized(ground){
                    site=ground.snapshot();
                    locomotives=rail.snapshot();
                    fleet=marine.snapshot();
                }
            }
        }
        int sx=key.x(),sy=key.y();
        List<SegmentVisual> parts=new ArrayList<>();
        for(var s:site.segments())
            if(within(s.x(),s.y(),sx,sy))
                parts.add(new SegmentVisual(s.x(),s.y(),s.kind(),s.axis(),
                    s.hp(),s.faction()));
        List<RunwayVisual> strips=new ArrayList<>();
        for(var r:site.runways())
            if(runwayOverlaps(r,sx,sy))
                strips.add(new RunwayVisual(r.id(),r.startX(),r.startY(),
                    r.axis(),r.count(),r.constructed(),r.faction()));
        List<TrainVisual> trains=new ArrayList<>();
        for(var t:locomotives.trains()){
            int tx=(int)Math.floor(t.x()),ty=(int)Math.floor(t.y());
            if(within(tx,ty,sx,sy))
                trains.add(new TrainVisual(t.id(),t.x(),t.y(),t.axis(),
                    t.direction(),t.hp(),t.wagons(),t.faction()));
        }
        List<CarrierVisual> decks=new ArrayList<>();
        for(var d:fleet.decks())
            if(deckOverlaps(d,sx,sy))
                decks.add(new CarrierVisual(d.id(),d.x(),d.y(),
                    d.axis(),d.lengthParcels(),d.deckHp(),d.faction()));
        parts.sort(Comparator.comparingInt(SegmentVisual::y)
            .thenComparingInt(SegmentVisual::x));
        strips.sort(Comparator.comparingInt(RunwayVisual::id));
        trains.sort(Comparator.comparingInt(TrainVisual::id));
        decks.sort(Comparator.comparingInt(CarrierVisual::id));
        return new View(key,List.copyOf(parts),List.copyOf(strips),
            List.copyOf(trains),List.copyOf(decks));
    }
}
