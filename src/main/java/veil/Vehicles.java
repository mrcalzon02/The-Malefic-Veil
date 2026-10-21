package veil;

import java.util.*;

/**
 * Server-owned drivable cars. Inputs are intentions, never client-authored transforms.
 * Vehicle collisions use existing authoritative city geometry and other cars.
 * This is a city-sector vertical slice; continental vehicle streaming remains gated.
 */
public final class Vehicles {
    public record State(int id,double x,double y,double heading,double speed,double fuel,int hp,int driverId) {}
    private static final class Car {
        final int id;
        double x,y,heading,speed,fuel=100,throttle,steer;
        int hp=200,driverId=-1;
        Car(int id,double x,double y,double heading){this.id=id;this.x=x;this.y=y;this.heading=heading;}
        State state(){return new State(id,x,y,heading,speed,fuel,hp,driverId);}
    }
    private final World world;
    private final List<Car> cars=new ArrayList<>();
    public Vehicles(World world) {
        this.world=Objects.requireNonNull(world);
        int[][] origins={{5,4},{63,63},{123,122},{25,36},{85,83},{105,17}};
        for(int[] origin:origins) {
            boolean placed=false;
            for(int radius=2;radius<=12&&!placed;radius++)
                for(int dx=-radius;dx<=radius&&!placed;dx++)
                    for(int dy=-radius;dy<=radius&&!placed;dy++) {
                        if(Math.abs(dx)!=radius&&Math.abs(dy)!=radius)continue;
                        double x=origin[0]+dx+.5,y=origin[1]+dy+.5;
                        if(!clear(x,y,null))continue;
                        // Prefer existing road frontage; do not block original spawn safety zones.
                        if(!world.city.road((int)x/CityLayout.CELL,(int)y/CityLayout.CELL))continue;
                        boolean safe=true;
                        for(double[] s:world.spawns)if(Math.hypot(x-s[0],y-s[1])<3)safe=false;
                        if(safe){cars.add(new Car(cars.size(),x,y,0));placed=true;}
                    }
        }
    }
    public synchronized List<State> states(){
        List<State> out=new ArrayList<>();
        for(Car car:cars)out.add(car.state());
        return List.copyOf(out);
    }
    public synchronized State state(int index){
        return index>=0&&index<cars.size()?cars.get(index).state():null;
    }
    private boolean clear(double x,double y,Car self){
        if(!Double.isFinite(x)||!Double.isFinite(y))return false;
        double r=.48;
        if(!world.canWalk(x-r,y-r)||!world.canWalk(x+r,y-r)||
           !world.canWalk(x-r,y+r)||!world.canWalk(x+r,y+r))return false;
        for(Car other:cars)if(other!=self&&other.hp>0&&Math.hypot(x-other.x,y-other.y)<1.5)return false;
        for(World.Device d:world.devices)if(d.hp>0&&Math.hypot(x-d.x,y-d.y)<1.05)return false;
        return true;
    }
    public synchronized int carDrivenBy(int playerId){
        for(Car car:cars)if(car.driverId==playerId)return car.id;
        return -1;
    }
    /** Single seat; driver must be alive and near car. */
    public synchronized boolean enter(int playerId,double x,double y,int carIndex){
        if(carDrivenBy(playerId)>=0||carIndex<0||carIndex>=cars.size())return false;
        Car car=cars.get(carIndex);
        if(car.driverId!=-1||car.hp<=0||Math.hypot(car.x-x,car.y-y)>2.0)return false;
        car.driverId=playerId;car.throttle=0;car.steer=0;return true;
    }
    /** Exit is refused if every nearby dismount location is obstructed. */
    public synchronized double[] exit(int playerId){
        int index=carDrivenBy(playerId);
        if(index<0)return null;
        Car car=cars.get(index);
        if(Math.abs(car.speed)>.6)return null;
        for(int i=0;i<12;i++){
            double angle=car.heading+Math.PI*2*i/12;
            double x=car.x+Math.cos(angle)*1.5,y=car.y+Math.sin(angle)*1.5;
            if(world.canWalk(x,y)&&clearFoot(x,y,car)){
                car.driverId=-1;car.throttle=0;car.steer=0;return new double[]{x,y};
            }
        }
        return null;
    }
    private boolean clearFoot(double x,double y,Car self){
        for(Car car:cars)if(car.hp>0&&Math.hypot(x-car.x,y-car.y)<1.0)return false;
        for(World.Device d:world.devices)if(d.hp>0&&Math.hypot(x-d.x,y-d.y)<.75)return false;
        return true;
    }
    public synchronized void disconnect(int playerId){
        int id=carDrivenBy(playerId);
        if(id>=0){Car car=cars.get(id);car.driverId=-1;car.throttle=0;car.steer=0;car.speed=0;}
    }
    public synchronized boolean input(int playerId,double throttle,double steering){
        int id=carDrivenBy(playerId);
        if(id<0||!Double.isFinite(throttle)||!Double.isFinite(steering)||
           Math.abs(throttle)>1||Math.abs(steering)>1)return false;
        Car car=cars.get(id);car.throttle=car.fuel>0?throttle:0;car.steer=steering;
        return true;
    }
    public synchronized void advance(long ms){
        if(ms<=0||ms>1000)throw new IllegalArgumentException("invalid vehicle tick");
        double dt=ms/1000.0;
        for(Car car:cars){
            if(car.hp<=0){car.speed=0;car.throttle=0;continue;}
            double accel=car.throttle>=0?5.2:6.5;
            car.speed+=car.throttle*accel*dt;
            if(car.throttle==0)car.speed*=Math.exp(-2.0*dt);
            car.speed=Math.max(-3,Math.min(8,car.speed));
            if(car.fuel<=0)car.speed*=Math.exp(-1.7*dt);
            if(Math.abs(car.speed)<.03)car.speed=0;
            double steer=car.steer*Math.min(1,Math.abs(car.speed)/2.0)*dt*1.05*
                Math.signum(car.speed);
            double heading=car.heading+steer;
            double nx=car.x+Math.cos(heading)*car.speed*dt;
            double ny=car.y+Math.sin(heading)*car.speed*dt;
            if(clear(nx,ny,car)){
                car.x=nx;car.y=ny;car.heading=heading;
            }else{
                if(Math.abs(car.speed)>2)car.hp=Math.max(0,car.hp-(int)Math.ceil(Math.abs(car.speed)*2));
                car.speed=0;
            }
            if(car.driverId>=0&&Math.abs(car.throttle)>.01)
                car.fuel=Math.max(0,car.fuel-dt*(.13+Math.abs(car.throttle)*.24));
        }
    }
    /** Refit cars from a validated durable snapshot; never grant a driving session on load. */
    public synchronized boolean restore(int id,double x,double y,double heading,double fuel,int hp){
        if(id<0||id>=cars.size()||!Double.isFinite(heading)||
           !Double.isFinite(fuel)||fuel<0||fuel>100||hp<0||hp>200||!clear(x,y,cars.get(id)))
            return false;
        Car car=cars.get(id);
        car.x=x;car.y=y;car.heading=heading;car.fuel=fuel;car.hp=hp;
        car.speed=0;car.driverId=-1;car.throttle=0;car.steer=0;
        return true;
    }
}
