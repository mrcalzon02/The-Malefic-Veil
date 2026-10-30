package veil;

import java.util.*;

/**
 * Finite paid aircraft-carrier hulls on ocean parcels, with a genuine deck
 * surface used by aircraft physics. Ship movement is discrete/host-controlled
 * until streamed marine physics, buoyancy, ocean obstacles and GPU ship art
 * are integrated. No client may assert positions, ownership or free airframes.
 */
public final class AircraftCarrier {
    public enum Axis { EAST_WEST, NORTH_SOUTH }
    public record Deck(int id,int ownerId,CharacterRoster.Faction faction,
                       double x,double y,Axis axis,int lengthParcels,
                       int deckHp,double fuelUnits,boolean atAnchor) {
        public int lengthMetres(){return 6*lengthParcels;}
        public double heading(){return axis==Axis.EAST_WEST?0:Math.PI/2;}
        public AircraftFlight.Airfield airfield(){
            return new AircraftFlight.Airfield(x,y,heading(),lengthMetres(),faction);
        }
    }
    public static final int LENGTH=18,HALF_WIDTH=2,MAX_HP=600;
    private final ContinentLayout world;
    private final CityMiasma miasma;
    private final int ownerId;
    private final CharacterRoster.Faction faction;
    private final Map<String,Integer> stock=new HashMap<>();
    private final Map<Integer,Deck> decks=new LinkedHashMap<>();
    private final Set<Integer> activeFlights=new HashSet<>();
    private int nextId=1;
    public AircraftCarrier(ContinentLayout world,CityMiasma miasma,
            int ownerId,CharacterRoster.Faction faction,Map<String,Integer> paidStock){
        this.world=Objects.requireNonNull(world);this.miasma=Objects.requireNonNull(miasma);
        this.faction=Objects.requireNonNull(faction);
        if(ownerId<0)throw new IllegalArgumentException("Unowned carrier");
        this.ownerId=ownerId;
        for(var e:paidStock.entrySet()){
            if(e.getKey()==null||e.getValue()==null||
               e.getValue()<0||e.getValue()>1_000_000)
                throw new IllegalArgumentException("Invalid carrier stock");
            stock.put(e.getKey(),e.getValue());
        }
    }
    public ContinentLayout world(){return world;}
    public CityMiasma miasma(){return miasma;}
    public CharacterRoster.Faction faction(){return faction;}
    public synchronized Map<String,Integer> stocks(){return Map.copyOf(stock);}
    public synchronized List<Deck> decks(){return List.copyOf(decks.values());}
    public synchronized Deck deck(int id){return decks.get(id);}
    public synchronized boolean activeFlight(int id){return activeFlights.contains(id);}
    public static Map<String,Integer> buildCost(CharacterRoster.Faction faction){
        Map<String,Integer> cost=new LinkedHashMap<>(Map.of(
            "METAL_STOCK",80,"MACHINE_PARTS",40,"HARDENED_ALLOY",20,
            "PRECISION_MODULE",8,"ASSEMBLY_CONTROLLER",5));
        switch(faction){
            case THE_UNRAVELED -> cost.put("RESILIENCE_CORE",4);
            case THE_PENITENT -> {
                cost.put("DREAD_WARD",4);cost.put("RELIQUARY_PLATE",12);
            }
            case THE_BEGOTTEN -> {
                cost.put("ORGAN_CORE",8);cost.put("GROWN_CHITIN",25);
                cost.put("LIVING_MEMBRANE",16);
            }
        }
        return Map.copyOf(cost);
    }
    private boolean canPay(Map<String,Integer> bill){
        for(var line:bill.entrySet())
            if(stock.getOrDefault(line.getKey(),0)<line.getValue())return false;
        return true;
    }
    private void debit(Map<String,Integer> bill){
        for(var line:bill.entrySet())
            stock.merge(line.getKey(),-line.getValue(),Integer::sum);
    }
    /** Ocean cells must underlie the ENTIRE hull and remain miasma-free.
     * This is a water berth validation, not a magical carrier spawn on grass.
     */
    public synchronized boolean clearSea(double cx,double cy,Axis axis,int exceptId){
        if(axis==null||!Double.isFinite(cx)||!Double.isFinite(cy))return false;
        int length=LENGTH;
        for(int along=0;along<length;along++){
            double offset=along-(length-1)*.5;
            for(int across=-HALF_WIDTH;across<=HALF_WIDTH;across++){
                int px=(int)Math.floor(cx+
                    (axis==Axis.EAST_WEST?offset:across));
                int py=(int)Math.floor(cy+
                    (axis==Axis.NORTH_SOUTH?offset:across));
                if(!world.inBounds(px,py)||
                   world.biome(px,py)!=ContinentLayout.Biome.WATER||
                   miasma.forbidsAircraft(px+.5,py+.5,0))
                    return false;
            }
        }
        for(Deck existing:decks.values())if(existing.id()!=exceptId&&
           Math.hypot(existing.x()-cx,existing.y()-cy)<LENGTH)return false;
        return true;
    }
    /** Paid in full from delivered faction supplies, hull persists as a
     * world-owned vessel object. A repair is not free and a ship is not a
     * land-based runway or settlement.
     */
    public synchronized int construct(int caller,double cx,double cy,Axis axis){
        if(caller!=ownerId||!clearSea(cx,cy,axis,-1)||
           !canPay(buildCost(faction)))return -1;
        debit(buildCost(faction));
        int id=nextId++;
        decks.put(id,new Deck(id,ownerId,faction,cx,cy,axis,
            LENGTH,MAX_HP,0,true));
        return id;
    }
    public synchronized boolean refuel(int caller,int id,int canisters){
        Deck d=decks.get(id);
        if(caller!=ownerId||d==null||!d.atAnchor()||
           canisters<1||canisters>20||d.fuelUnits()+canisters*25>300||
           !canPay(Map.of("CHARCOAL",canisters)))return false;
        debit(Map.of("CHARCOAL",canisters));
        decks.put(id,new Deck(id,d.ownerId(),d.faction(),d.x(),d.y(),d.axis(),
            d.lengthParcels(),d.deckHp(),d.fuelUnits()+25*canisters,true));
        return true;
    }
    /** Naval transit is restricted to a one-parcel cardinal move per tick.
     * The deck is unavailable to aircraft during transit or if damaged.
     */
    public synchronized boolean sail(int caller,int id,int dx,int dy){
        Deck d=decks.get(id);
        if(d==null||caller!=ownerId||activeFlights.contains(id)||
           d.deckHp()<=300||d.fuelUnits()<1||
           Math.abs(dx)+Math.abs(dy)!=1||
           !clearSea(d.x()+dx,d.y()+dy,d.axis(),id))return false;
        decks.put(id,new Deck(id,d.ownerId(),d.faction(),d.x()+dx,d.y()+dy,
            d.axis(),d.lengthParcels(),d.deckHp(),d.fuelUnits()-1,true));
        return true;
    }
    public synchronized boolean damage(int id,int amount){
        Deck d=decks.get(id);
        if(d==null||amount<=0||amount>300)return false;
        decks.put(id,new Deck(id,d.ownerId(),d.faction(),d.x(),d.y(),d.axis(),
            d.lengthParcels(),Math.max(0,d.deckHp()-amount),
            d.fuelUnits(),d.atAnchor()));
        return true;
    }
    public synchronized boolean repair(int caller,int id){
        Deck d=decks.get(id);
        if(caller!=ownerId||d==null||d.deckHp()==MAX_HP||
           !canPay(Map.of("MACHINE_PARTS",3,"HARDENED_ALLOY",2)))return false;
        debit(Map.of("MACHINE_PARTS",3,"HARDENED_ALLOY",2));
        decks.put(id,new Deck(id,d.ownerId(),d.faction(),d.x(),d.y(),d.axis(),
            d.lengthParcels(),MAX_HP,d.fuelUnits(),true));
        return true;
    }
    public synchronized boolean usableDeck(int id,AirPowerCatalog.Airframe plane){
        Deck d=decks.get(id);
        return d!=null&&plane!=null&&plane.faction()==faction&&
            d.deckHp()>MAX_HP/2&&d.atAnchor()&&
            d.lengthMetres()>=plane.requiredRunwayMetres()&&
            clearSea(d.x(),d.y(),d.axis(),id);
    }
    public synchronized boolean reserveDeck(int caller,int id,
                                             AirPowerCatalog.Airframe plane){
        if(caller!=ownerId||activeFlights.contains(id)||!usableDeck(id,plane))return false;
        activeFlights.add(id);return true;
    }
    public synchronized boolean releaseDeck(int caller,int id){
        return caller==ownerId&&activeFlights.remove(id);
    }
    public synchronized double deckElevation(int id,double x,double y){
        Deck d=decks.get(id);
        if(d==null)return Double.NaN;
        double dx=x-d.x(),dy=y-d.y();
        double along=d.axis()==Axis.EAST_WEST?dx:dy;
        double across=d.axis()==Axis.EAST_WEST?dy:dx;
        return Math.abs(along)<d.lengthParcels()*.5&&
               Math.abs(across)<HALF_WIDTH+.5?8.0:Double.NaN;
    }
}
