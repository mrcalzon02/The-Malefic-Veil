package veil;

import java.util.*;

/**
 * Paid rail locomotive and freight simulation against actual authored track
 * segments. Host ticks, damage, fuel, cargo and movement are authoritative.
 * This module is not yet connected to LAN commands or rendered rolling stock.
 */
public final class RailTransport {
    public record TrainState(int id,int ownerId,CharacterRoster.Faction faction,
        double x,double y,double speedMetresPerSecond,double fuelUnits,int hp,
        String cargo,int cargoQuantity,boolean brakeApplied,int wagons) {}
    private static final class Train {
        final int id,owner;
        final CharacterRoster.Faction faction;
        final TransportInfrastructure.Axis axis;
        final int direction;
        double x,y,speed,fuel;
        int hp=320,quantity,wagons;
        String cargo;
        boolean brake=true;
        Train(int id,int owner,CharacterRoster.Faction faction,
              TransportInfrastructure.Axis axis,int direction,int x,int y){
            this.id=id;this.owner=owner;this.faction=faction;
            this.axis=axis;this.direction=direction;
            this.x=x+.5;this.y=y+.5;
        }
        TrainState state(){return new TrainState(id,owner,faction,x,y,speed,
            fuel,hp,cargo,quantity,brake,wagons);}
    }
    private final TransportInfrastructure infra;
    private final Map<String,Integer> supplies=new HashMap<>();
    private final Map<Integer,Train> trains=new LinkedHashMap<>();
    private int nextId=1;
    private final Set<String> deliveryReceipts=new LinkedHashSet<>();
    public record TrainSave(int id,int owner,CharacterRoster.Faction faction,
        TransportInfrastructure.Axis axis,int direction,double x,double y,
        double speed,double fuel,int hp,int quantity,String cargo,
        boolean brake,int wagons) {}
    public record Snapshot(long worldSeed,int ownerId,
        CharacterRoster.Faction faction,Map<String,Integer> stock,
        List<TrainSave> trains,Set<String> deliveryReceipts,int nextId) {}
    public TransportInfrastructure infrastructure(){return infra;}
    synchronized boolean delivered(String receipt){return deliveryReceipts.contains(receipt);}
    synchronized boolean canAcceptDelivery(String item,int amount,String receipt){
        return item!=null&&amount>0&&amount<=512&&
            receipt!=null&&receipt.matches("[a-zA-Z0-9_-]{6,80}")&&
            deliveryReceipts.size()<4096&&!deliveryReceipts.contains(receipt)&&
            supplies.getOrDefault(item,0)<=1_000_000-amount;
    }
    synchronized void acceptDelivery(String item,int amount,String receipt){
        if(!canAcceptDelivery(item,amount,receipt))
            throw new IllegalStateException("Unverified rail cargo delivery");
        supplies.merge(item,amount,Integer::sum);
        deliveryReceipts.add(receipt);
    }
    public synchronized Snapshot snapshot(){
        List<TrainSave> saved=new ArrayList<>();
        for(Train t:trains.values())saved.add(new TrainSave(t.id,t.owner,
            t.faction,t.axis,t.direction,t.x,t.y,t.speed,t.fuel,t.hp,
            t.quantity,t.cargo,t.brake,t.wagons));
        return new Snapshot(infra.world().seed(),infra.ownerId(),
            infra.faction(),Map.copyOf(supplies),List.copyOf(saved),
            Set.copyOf(deliveryReceipts),nextId);
    }
    /**
     * Restore is a host-checkpoint operation, not a network command.
     * Reject a foreign seed, unsupported track, implausible cargo/physics,
     * invalid receipts or duplicate locomotive before mutating any state.
     */
    public synchronized void restore(Snapshot saved){
        if(saved==null||saved.worldSeed()!=infra.world().seed()||
           saved.ownerId()!=infra.ownerId()||saved.faction()!=infra.faction()||
           saved.stock()==null||saved.trains()==null||
           saved.deliveryReceipts()==null||saved.nextId()<1||
           saved.nextId()>1_000_000||saved.trains().size()>1024||
           saved.deliveryReceipts().size()>4096)
            throw new IllegalArgumentException("Corrupt rail checkpoint");
        Map<String,Integer> items=new HashMap<>();
        for(var e:saved.stock().entrySet()){
            if(e.getKey()==null||e.getKey().isBlank()||e.getValue()==null||
               e.getValue()<1||e.getValue()>1_000_000)
                throw new IllegalArgumentException("Corrupt rail inventory");
            items.put(e.getKey(),e.getValue());
        }
        Set<String> receipts=new LinkedHashSet<>();
        for(String receipt:saved.deliveryReceipts())
            if(receipt==null||!receipt.matches("[a-zA-Z0-9_-]{6,80}")||
               !receipts.add(receipt))
                throw new IllegalArgumentException("Corrupt rail receipt");
        Map<Integer,Train> restored=new LinkedHashMap<>();
        for(TrainSave line:saved.trains()){
            if(line==null||line.id()<1||line.id()>=saved.nextId()||
               line.owner()!=infra.ownerId()||line.faction()!=infra.faction()||
               line.axis()==null||Math.abs(line.direction())!=1||
               !Double.isFinite(line.x())||!Double.isFinite(line.y())||
               !Double.isFinite(line.speed())||!Double.isFinite(line.fuel())||
               line.speed()<0||line.speed()>12.01||
               line.fuel()<0||line.fuel()>100||
               line.hp()<0||line.hp()>320||
               line.wagons()<0||line.wagons()>3||
               line.quantity()<0||line.quantity()>40*(1+line.wagons())||
               (line.quantity()==0)!=(line.cargo()==null)||
               (line.cargo()!=null&&!line.cargo().matches("[A-Z_]{2,60}"))||
               !infra.hasRailSegment((int)Math.floor(line.x()),
                   (int)Math.floor(line.y()),line.axis()))
                throw new IllegalArgumentException("Corrupt saved locomotive");
            Train restoredTrain=new Train(line.id(),line.owner(),
                line.faction(),line.axis(),line.direction(),
                (int)Math.floor(line.x()),(int)Math.floor(line.y()));
            restoredTrain.x=line.x();restoredTrain.y=line.y();
            restoredTrain.speed=line.speed();restoredTrain.fuel=line.fuel();
            restoredTrain.hp=line.hp();restoredTrain.quantity=line.quantity();
            restoredTrain.cargo=line.cargo();restoredTrain.brake=line.brake();
            restoredTrain.wagons=line.wagons();
            if(restored.putIfAbsent(line.id(),restoredTrain)!=null)
                throw new IllegalArgumentException("Duplicate saved locomotive");
        }
        // Commit together only after all validation has succeeded.
        supplies.clear();supplies.putAll(items);
        deliveryReceipts.clear();deliveryReceipts.addAll(receipts);
        trains.clear();trains.putAll(restored);
        nextId=saved.nextId();
    }

    public RailTransport(TransportInfrastructure infra,Map<String,Integer> realStock) {
        this.infra=Objects.requireNonNull(infra);
        for(var p:realStock.entrySet()){
            if(p.getKey()==null||p.getValue()==null||p.getValue()<0||
               p.getValue()>1_000_000)throw new IllegalArgumentException("Invalid stock");
            supplies.put(p.getKey(),p.getValue());
        }
    }
    private Map<String,Integer> locomotiveBill(){
        return Map.of(TransportProduction.item(
            TransportProduction.Product.LOCOMOTIVE,infra.faction()),1);
    }
    private boolean debit(Map<String,Integer> bill){
        for(var e:bill.entrySet())
            if(supplies.getOrDefault(e.getKey(),0)<e.getValue())return false;
        for(var e:bill.entrySet()){
            int remaining=supplies.get(e.getKey())-e.getValue();
            if(remaining==0)supplies.remove(e.getKey());
            else supplies.put(e.getKey(),remaining);
        }
        return true;
    }
    public synchronized Map<String,Integer> stocks(){return Map.copyOf(supplies);}
    public synchronized List<TrainState> states(){
        return trains.values().stream().map(Train::state).toList();
    }
    public synchronized TrainState state(int id){
        Train t=trains.get(id);return t==null?null:t.state();
    }
    /** Three physically connected, maintained rail cells are the minimum
     * commissioning siding. Construction stock is taken exactly once.
     */
    public synchronized int commission(int caller,int x,int y,
        TransportInfrastructure.Axis axis,int direction){
        if(caller!=infra.ownerId()||axis==null||Math.abs(direction)!=1)
            return -1;
        if(!infra.railExists(x,y))return -1;
        for(int i=1;i<=2;i++){
            int nx=x+(axis==TransportInfrastructure.Axis.EAST_WEST?direction*i:0);
            int ny=y+(axis==TransportInfrastructure.Axis.NORTH_SOUTH?direction*i:0);
            int px=x+(axis==TransportInfrastructure.Axis.EAST_WEST?direction*(i-1):0);
            int py=y+(axis==TransportInfrastructure.Axis.NORTH_SOUTH?direction*(i-1):0);
            if(!infra.connectedRail(px,py,nx,ny))return -1;
        }
        for(Train t:trains.values())if(Math.hypot(t.x-x-.5,t.y-y-.5)<3)return -1;
        if(!debit(locomotiveBill()))return -1;
        int id=nextId++;
        trains.put(id,new Train(id,caller,infra.faction(),axis,direction,x,y));
        return id;
    }
    public synchronized boolean load(int caller,int id,String item,int quantity){
        Train t=trains.get(id);
        if(t==null||t.owner!=caller||t.hp<=0||t.speed>.01||
           item==null||!item.matches("[A-Z_]{2,60}")||
           quantity<1||quantity>40*(1+t.wagons)||t.cargo!=null||
           supplies.getOrDefault(item,0)<quantity)return false;
        if(!debit(Map.of(item,quantity)))return false;
        t.cargo=item;t.quantity=quantity;
        return true;
    }
    public synchronized boolean unload(int caller,int id){
        Train t=trains.get(id);
        if(t==null||t.owner!=caller||t.speed>.01||t.cargo==null)return false;
        supplies.merge(t.cargo,t.quantity,Integer::sum);
        t.cargo=null;t.quantity=0;return true;
    }
    /** One real manufactured rail-fuel charge stores ten traction units. */
    public synchronized boolean refuel(int caller,int id,int charges){
        Train t=trains.get(id);
        if(t==null||t.owner!=caller||t.speed>.01||
           charges<1||charges>10||t.fuel+charges*10>100||
           !debit(Map.of(TransportProduction.item(
               TransportProduction.Product.RAIL_FUEL,infra.faction()),charges)))
            return false;
        t.fuel+=charges*10;return true;
    }
    /** Attach a produced wagon at rest. Its weight reduces maximum traction
     * speed, but increases genuine inventory freight capacity.
     */
    public synchronized boolean attachWagon(int caller,int id){
        Train t=trains.get(id);
        if(t==null||t.owner!=caller||t.hp<=0||t.speed>.01||
           t.wagons>=3||!debit(Map.of(TransportProduction.item(
               TransportProduction.Product.FREIGHT_WAGON,infra.faction()),1)))
            return false;
        t.wagons++;return true;
    }
    public synchronized boolean setBrake(int caller,int id,boolean applied){
        Train t=trains.get(id);
        if(t==null||t.owner!=caller||t.hp<=0)return false;
        t.brake=applied;return true;
    }
    public synchronized void advance(long milliseconds){
        if(milliseconds<1||milliseconds>250)
            throw new IllegalArgumentException("Unbounded rail tick");
        double dt=milliseconds/1000.0;
        for(Train t:trains.values()){
            if(t.hp<=0){t.speed=0;continue;}
            if(t.brake||t.fuel<=0)t.speed=Math.max(0,t.speed-3.5*dt);
            else {
                double limit=t.cargo==null?12:8;
                limit=Math.max(3,limit-t.wagons*1.1);
                t.speed=Math.min(limit,t.speed+1.8*dt);
            }
            if(t.speed<.01)continue;
            double parcels=t.speed*dt/TransportInfrastructure.PARCEL_METRES;
            double nx=t.x+(t.axis==TransportInfrastructure.Axis.EAST_WEST?
                parcels*t.direction:0);
            double ny=t.y+(t.axis==TransportInfrastructure.Axis.NORTH_SOUTH?
                parcels*t.direction:0);
            int oldX=(int)Math.floor(t.x),oldY=(int)Math.floor(t.y);
            int newX=(int)Math.floor(nx),newY=(int)Math.floor(ny);
            if(!infra.railExists(oldX,oldY)||
               ((oldX!=newX||oldY!=newY)&&
                 !infra.connectedRail(oldX,oldY,newX,newY))){
                if(t.speed>3)t.hp=Math.max(0,t.hp-(int)Math.ceil(t.speed*4));
                t.speed=0;t.brake=true;
                continue;
            }
            t.x=nx;t.y=ny;
            t.fuel=Math.max(0,t.fuel-(.03+.012*t.speed)*dt);
        }
    }
}
