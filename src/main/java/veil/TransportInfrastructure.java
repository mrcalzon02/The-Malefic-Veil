package veil;

import java.util.*;

/**
 * Host-side physical six-metre infrastructure overlay. Every occupied macro
 * parcel is a paid, damageable segment, not an invisible global unlock.
 *
 * This is not yet a live streamed-world/GUI placement service. Only a
 * server-owned controller may supply the settled stock and operate this ledger.
 */
public final class TransportInfrastructure {
    public static final int PARCEL_METRES=6;
    public enum Axis { EAST_WEST, NORTH_SOUTH }
    public enum Kind { RUNWAY, RAIL }
    public record Segment(int x,int y,Kind kind,Axis axis,int hp,int ownerId,
                          CharacterRoster.Faction faction) {}
    public record Runway(int id,int startX,int startY,Axis axis,int count,
                         int ownerId,CharacterRoster.Faction faction,int constructed) {
        public int lengthMetres(){return count*PARCEL_METRES;}
        public double centerX(){return startX+.5+(axis==Axis.EAST_WEST?(count-1)*.5:0);}
        public double centerY(){return startY+.5+(axis==Axis.NORTH_SOUTH?(count-1)*.5:0);}
        public double heading(){return axis==Axis.EAST_WEST?0:Math.PI/2;}
        public AircraftFlight.Airfield airfield(){
            return new AircraftFlight.Airfield(centerX(),centerY(),heading(),
                lengthMetres(),faction);
        }
    }
    private static final int SEGMENT_HP=100;
    private final ContinentLayout world;
    private final CityMiasma miasma;
    private final int ownerId;
    private final CharacterRoster.Faction faction;
    private final Map<String,Integer> stocks=new HashMap<>();
    private final Map<Long,Segment> tiles=new HashMap<>();
    private final Map<Integer,Runway> runways=new LinkedHashMap<>();
    private int nextRunwayId=1;
    private double energy;
    public TransportInfrastructure(ContinentLayout world,CityMiasma miasma,
            int ownerId,CharacterRoster.Faction faction,Map<String,Integer> paidStock){
        this.world=Objects.requireNonNull(world);this.miasma=Objects.requireNonNull(miasma);
        this.faction=Objects.requireNonNull(faction);
        if(ownerId<0)throw new IllegalArgumentException("Unowned infrastructure");
        this.ownerId=ownerId;
        for(var line:paidStock.entrySet()){
            if(line.getKey()==null||line.getValue()==null||
               line.getValue()<0||line.getValue()>1_000_000)
                throw new IllegalArgumentException("Untrusted inventory");
            if(line.getValue()>0)stocks.put(line.getKey(),line.getValue());
        }
    }
    public ContinentLayout world(){return world;}
    public CityMiasma miasma(){return miasma;}
    public CharacterRoster.Faction faction(){return faction;}
    public int ownerId(){return ownerId;}
    private static long key(int x,int y){
        return ((long)x<<32)|(y&0xffffffffL);
    }
    private int atX(Runway r,int index){
        return r.startX()+(r.axis()==Axis.EAST_WEST?index:0);
    }
    private int atY(Runway r,int index){
        return r.startY()+(r.axis()==Axis.NORTH_SOUTH?index:0);
    }
    private boolean owns(int caller){return ownerId==caller;}
    public synchronized Map<String,Integer> stock(){return Map.copyOf(stocks);}
    public synchronized Collection<Segment> segments(){return List.copyOf(tiles.values());}
    public synchronized Collection<Runway> runways(){return List.copyOf(runways.values());}
    public synchronized Runway runway(int id){return runways.get(id);}
    public synchronized double constructionEnergy(){return energy;}
    /** Transfer may only be invoked after a real utility-grid debit. */
    public synchronized void depositMeteredEnergy(double units){
        if(!Double.isFinite(units)||units<0||energy+units>1_000_000)
            throw new IllegalArgumentException("Invalid energy transfer");
        energy+=units;
    }
    private boolean validGround(int x,int y){
        return world.buildableGround(x,y)&&!world.road(x,y)&&
            !miasma.forbidsAircraft(x+.5,y+.5,0)&&!tiles.containsKey(key(x,y));
    }
    /** Construction consumes produced transport panels from the faction's
     * staffed/energized industrial chain; raw ore/wood alone cannot pave.
     */
    private static Map<String,Integer> cost(Kind kind,
            CharacterRoster.Faction faction){
        var bill=new LinkedHashMap<String,Integer>();
        if(kind==Kind.RUNWAY){
            bill.put(TransportProduction.item(
                TransportProduction.Product.RUNWAY_PANEL,faction),1);
            switch(faction){
                case THE_UNRAVELED -> {
                    bill.put("FIELD_STONE",2);bill.put("FASTENERS",1);
                }
                case THE_PENITENT -> bill.put("VOTIVE_INCENSE",1);
                case THE_BEGOTTEN -> bill.put("NUTRIENT_STOCK",2);
            }
        }else{
            bill.put(TransportProduction.item(
                TransportProduction.Product.RAIL_SECTION,faction),1);
            switch(faction){
                case THE_UNRAVELED -> bill.put("TIMBER",2);
                case THE_PENITENT -> bill.put("WITNESS_SEAL",1);
                case THE_BEGOTTEN -> bill.put("GROWTH_FEED",2);
            }
        }
        return Map.copyOf(bill);
    }
    public static Map<String,Integer> materialCost(Kind kind,
            CharacterRoster.Faction faction){
        return cost(kind,faction);
    }
    private boolean canPay(Map<String,Integer> bill){
        return bill.entrySet().stream().allMatch(e->
            stocks.getOrDefault(e.getKey(),0)>=e.getValue());
    }
    private void debit(Map<String,Integer> bill){
        for(var e:bill.entrySet()){
            int left=stocks.get(e.getKey())-e.getValue();
            if(left==0)stocks.remove(e.getKey());else stocks.put(e.getKey(),left);
        }
    }
    /** Proposed runway reserves a distinct site but NO materials and NO cells.
     * A runway has to be paid section by section; unfinished spans cannot fly.
     */
    public synchronized Runway planRunway(int caller,int startX,int startY,
                                          Axis axis,int count){
        if(!owns(caller)||axis==null||count<12||count>60)return null;
        for(int i=0;i<count;i++){
            int x=startX+(axis==Axis.EAST_WEST?i:0);
            int y=startY+(axis==Axis.NORTH_SOUTH?i:0);
            if(!validGround(x,y))return null;
            double height=world.elevation(x,y);
            if(Math.abs(height-world.elevation(startX,startY))>3)return null;
            for(int off:new int[]{-1,1}){
                int lateralX=x+(axis==Axis.NORTH_SOUTH?off:0);
                int lateralY=y+(axis==Axis.EAST_WEST?off:0);
                if(!world.buildableGround(lateralX,lateralY)||
                   world.road(lateralX,lateralY)||
                   miasma.forbidsAircraft(lateralX+.5,lateralY+.5,0)||
                   tiles.containsKey(key(lateralX,lateralY)))return null;
            }
        }
        Runway proposed=new Runway(nextRunwayId,startX,startY,axis,count,
            ownerId,faction,0);
        // Preserve the original aircraft runway collision and grade test too.
        // The first iteration supports at least the light-aircraft strip.
        var smallest=AirPowerCatalog.all().stream()
            .filter(p->p.faction()==faction&&p.role()==AirPowerMeshes.Role.LIGHT)
            .findFirst().orElseThrow();
        if(!AircraftFlight.validAirfield(world,miasma,proposed.airfield(),smallest))
            return null;
        // All proposed runway footprints must remain independent.
        for(Runway r:runways.values())for(int i=0;i<r.count();i++)
            for(int j=0;j<count;j++)if(atX(r,i)==startX+(axis==Axis.EAST_WEST?j:0)&&
               atY(r,i)==startY+(axis==Axis.NORTH_SOUTH?j:0))return null;
        runways.put(nextRunwayId++,proposed);
        return proposed;
    }
    /** Build one 6m section from real material stock and metered power.
     * Updates occupied geometry immediately; no finished runway shortcut.
     */
    public synchronized boolean buildNextRunwaySection(int caller,int runwayId){
        Runway r=runways.get(runwayId);
        if(!owns(caller)||r==null||r.constructed()>=r.count())return false;
        int x=atX(r,r.constructed()),y=atY(r,r.constructed());
        Map<String,Integer> bill=cost(Kind.RUNWAY,faction);
        if(!validGround(x,y)||!canPay(bill)||energy<2)return false;
        debit(bill);energy-=2;
        tiles.put(key(x,y),new Segment(x,y,Kind.RUNWAY,r.axis(),
            SEGMENT_HP,ownerId,faction));
        runways.put(runwayId,new Runway(r.id(),r.startX(),r.startY(),r.axis(),
            r.count(),r.ownerId(),r.faction(),r.constructed()+1));
        return true;
    }
    /** Damaged tiles invalidate a runway; maintenance is a paid operation. */
    public synchronized boolean damage(int x,int y,int damage){
        Segment old=tiles.get(key(x,y));
        if(old==null||damage<1||damage>100)return false;
        tiles.put(key(x,y),new Segment(x,y,old.kind(),old.axis(),
            Math.max(0,old.hp()-damage),old.ownerId(),old.faction()));
        return true;
    }
    public synchronized boolean repair(int caller,int x,int y){
        Segment old=tiles.get(key(x,y));
        if(!owns(caller)||old==null||old.hp()==SEGMENT_HP||
           !canPay(Map.of(TransportProduction.item(
               TransportProduction.Product.TRANSPORT_REPAIR,faction),1))||
           energy<1)return false;
        debit(Map.of(TransportProduction.item(
            TransportProduction.Product.TRANSPORT_REPAIR,faction),1));
        energy-=1;
        tiles.put(key(x,y),new Segment(x,y,old.kind(),old.axis(),
            SEGMENT_HP,old.ownerId(),old.faction()));
        return true;
    }
    public synchronized boolean operationalRunway(int runwayId,
                                                 AirPowerCatalog.Airframe plane){
        Runway r=runways.get(runwayId);
        if(r==null||plane==null||plane.faction()!=faction||
           r.constructed()!=r.count()||
           r.lengthMetres()<plane.requiredRunwayMetres())return false;
        for(int i=0;i<r.count();i++){
            Segment seg=tiles.get(key(atX(r,i),atY(r,i)));
            if(seg==null||seg.kind()!=Kind.RUNWAY||seg.hp()<=50)return false;
        }
        return AircraftFlight.validAirfield(world,miasma,r.airfield(),plane);
    }
    /** Rails use the same immutable parcel geometry and faction supply spine,
     * but are independently damaged/placed and never count as airstrip paving.
     */
    public synchronized boolean buildRail(int caller,int x,int y,Axis axis){
        if(!owns(caller)||axis==null||!world.buildableGround(x,y)||
           world.road(x,y)||tiles.containsKey(key(x,y)))return false;
        Map<String,Integer> bill=cost(Kind.RAIL,faction);
        if(!canPay(bill)||energy<1)return false;
        // Railway grade limit applies across the whole next joint.
        for(int direction:new int[]{-1,1}){
            int nx=x+(axis==Axis.EAST_WEST?direction:0);
            int ny=y+(axis==Axis.NORTH_SOUTH?direction:0);
            Segment neighbor=tiles.get(key(nx,ny));
            if(neighbor!=null&&neighbor.kind()==Kind.RAIL&&
               neighbor.axis()==axis&&
               Math.abs(world.elevation(nx,ny)-world.elevation(x,y))>1.5)
                return false;
        }
        debit(bill);energy-=1;
        tiles.put(key(x,y),new Segment(x,y,Kind.RAIL,axis,SEGMENT_HP,ownerId,faction));
        return true;
    }
    public synchronized boolean connectedRail(int x,int y,int nx,int ny){
        Segment a=tiles.get(key(x,y)),b=tiles.get(key(nx,ny));
        if(a==null||b==null||a.hp()<=50||b.hp()<=50||
           a.kind()!=Kind.RAIL||b.kind()!=Kind.RAIL||
           a.axis()!=b.axis())return false;
        if(a.axis()==Axis.EAST_WEST)return y==ny&&Math.abs(nx-x)==1;
        return x==nx&&Math.abs(ny-y)==1;
    }
    public synchronized boolean railExists(int x,int y){
        Segment a=tiles.get(key(x,y));
        return a!=null&&a.kind()==Kind.RAIL&&a.hp()>50;
    }
}
