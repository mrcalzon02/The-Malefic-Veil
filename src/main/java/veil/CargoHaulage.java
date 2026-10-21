package veil;

import java.util.*;

/**
 * Authoritative bulk cargo from fixed RESOURCE city plots. On-foot loading
 * onto a stationary truck; no backpack teleportation of multi-pallet stock.
 * METAL_STOCK and MACHINE_PARTS are distinct crafting resources. Source-kit
 * ground art is pallet/pallet-small; the two truck-cargo meshes show loads.
 */
public final class CargoHaulage {
    public record Site(int id,int x,int y,String plot,String resource,int stock,
                       long refillAt,String groundMesh) {}
    public record Drop(int id,double x,double y,String resource,long createdAt) {}
    private static final class Yard {
        int id,x,y,stock=1;
        long refillAt;
        String plot,resource;
    }
    private final World world;
    private final Vehicles vehicles;
    private final List<Yard> yards=new ArrayList<>();
    private final List<Drop> drops=new ArrayList<>();
    private long clock;
    private int nextDrop=1;
    public static final long REFILL_MS=180_000L;
    public static final int CARGO_DAMAGE_THRESHOLD=80;
    public CargoHaulage(World world,Vehicles vehicles){
        this.world=Objects.requireNonNull(world);
        this.vehicles=Objects.requireNonNull(vehicles);
        for(CityLayout.Plot plot:world.city.plots()){
            if(plot.template().category()!=CityLayout.Category.RESOURCE||yards.size()>=24)continue;
            int x0=plot.x()*CityLayout.CELL,y0=plot.y()*CityLayout.CELL;
            int x1=plot.right()*CityLayout.CELL,y1=plot.bottom()*CityLayout.CELL;
            List<int[]> frontage=new ArrayList<>();
            // A truck on an adjacent road must actually reach the loading bay.
            for(int x=x0+1;x<x1-1;x++)for(int y=y0+1;y<y1-1;y++){
                if(!world.canWalk(x+.5,y+.5))continue;
                boolean adjacent=false;
                for(int dx=-4;dx<=4&&!adjacent;dx++)
                    for(int dy=-4;dy<=4&&!adjacent;dy++){
                        int ax=x+dx,ay=y+dy;
                        if(ax<1||ay<1||ax>=World.SIZE-1||ay>=World.SIZE-1)continue;
                        if(!world.city.road(ax/CityLayout.CELL,ay/CityLayout.CELL))continue;
                        if(world.canWalk(ax+.5,ay+.5)&&Math.hypot(dx,dy)<=4)adjacent=true;
                    }
                if(adjacent)frontage.add(new int[]{x,y});
            }
            if(frontage.isEmpty())continue;
            long salt=World.mix(world.seed^((long)plot.x()<<32)^plot.y()*1000003L);
            int[] at=frontage.get((int)Math.floorMod(salt,frontage.size()));
            boolean metal=(salt&1)==0;
            Yard yard=new Yard();yard.id=yards.size();
            yard.x=at[0];yard.y=at[1];
            yard.plot=plot.template().id();
            yard.resource=metal?"METAL_STOCK":"MACHINE_PARTS";
            yards.add(yard);
        }
        if(yards.size()<4)throw new IllegalStateException("Insufficient roadside cargo yards");
        // Both resource types must be obtainable; do not let seed bias erase one.
        yards.get(0).resource="METAL_STOCK";
        yards.get(1).resource="MACHINE_PARTS";
    }
    private static String groundMesh(String type){
        return "METAL_STOCK".equals(type)?"pallet":"pallet-small";
    }
    public synchronized List<Site> sites(){
        List<Site> out=new ArrayList<>();
        for(Yard yard:yards)out.add(new Site(yard.id,yard.x,yard.y,
            yard.plot,yard.resource,yard.stock,yard.refillAt,groundMesh(yard.resource)));
        return List.copyOf(out);
    }
    public synchronized List<Drop> drops(){return List.copyOf(drops);}
    /** Only the host accepts loading; on foot near both bay and parked truck. */
    public synchronized boolean load(int playerId,double px,double py,int carId,int siteId){
        if(siteId<0||siteId>=yards.size()||!Double.isFinite(px)||!Double.isFinite(py))
            return false;
        Yard yard=yards.get(siteId);
        Vehicles.State car=vehicles.state(carId);
        if(car==null||yard.stock==0||Math.hypot(px-yard.x-.5,py-yard.y-.5)>2.5||
            Math.hypot(car.x()-yard.x-.5,car.y()-yard.y-.5)>4.2||
            Math.hypot(px-car.x(),py-car.y())>4.5||
            vehicles.carDrivenBy(playerId)>=0||Math.abs(car.speed())>.10)return false;
        if(!world.lineClear(px,py,yard.x+.5,yard.y+.5))return false;
        if(!vehicles.loadCargo(carId,yard.resource))return false;
        yard.stock--;yard.refillAt=clock+REFILL_MS;
        return true;
    }
    /** Recover a dropped load by hand-transfer directly to a nearby intact truck. */
    public synchronized boolean recover(int playerId,double px,double py,int carId,int dropId){
        Vehicles.State car=vehicles.state(carId);
        if(car==null||vehicles.carDrivenBy(playerId)>=0||Math.abs(car.speed())>.10)return false;
        for(int i=0;i<drops.size();i++){
            Drop d=drops.get(i);
            if(d.id()!=dropId)continue;
            if(Math.hypot(px-d.x(),py-d.y())>2.5||
                Math.hypot(car.x()-d.x(),car.y()-d.y())>4.2||
                !world.lineClear(px,py,d.x(),d.y()))return false;
            if(!vehicles.loadCargo(carId,d.resource()))return false;
            drops.remove(i);return true;
        }
        return false;
    }
    /** Damage checks always take the truck's load only once. */
    public synchronized void advance(long ms){
        if(ms<0||ms>1000)throw new IllegalArgumentException("Bad cargo world tick");
        clock+=ms;
        for(Yard yard:yards)if(yard.stock==0&&clock>=yard.refillAt)
            yard.stock=1;
        for(Vehicles.State vehicle:vehicles.states()){
            Vehicles.Load load=vehicles.cargo(vehicle.id());
            if(load==null||load.resourceType()==null||vehicle.hp()>CARGO_DAMAGE_THRESHOLD)continue;
            String resource=vehicles.ejectCargo(vehicle.id());
            if(resource!=null){
                drops.add(new Drop(nextDrop++,vehicle.x(),vehicle.y(),resource,clock));
                if(drops.size()>128)throw new IllegalStateException("Cargo drop budget exceeded");
            }
        }
    }
    public synchronized long time(){return clock;}
}
