package veil;

import java.util.*;

/**
 * Deterministic server-owned prototype aircraft lifecycle in continent MACRO
 * parcels (6m per parcel), with metres for actual altitude/runway. This is a
 * host core, NOT yet a LAN-connected pilot or streamed GPU flight actor.
 *
 * All terrain, miasma, placement, inventory and movement authority lives here.
 */
public final class AircraftFlight {
    public static final double PARCEL_METRES=6.0;
    public static final double CANISTER_FUEL_UNITS=25.0;
    public record Airfield(double x,double y,double heading,double lengthMetres,
                           CharacterRoster.Faction faction) {}
    public record Controls(double throttle,double pitch,double yaw) {
        public Controls {
            if(!Double.isFinite(throttle)||!Double.isFinite(pitch)||
               !Double.isFinite(yaw)||throttle<0||throttle>1||
               Math.abs(pitch)>1||Math.abs(yaw)>1)
                throw new IllegalArgumentException("Invalid pilot intention");
        }
    }
    public record State(String modelId,double x,double y,double altitudeMetres,
                        double heading,double speedParcelsPerSecond,
                        double verticalMetresPerSecond,double fuelUnits,
                        int hp,boolean airborne,double takeoffRollMetres,
                        boolean miasmaDeflected) {}
    private final AirPowerCatalog.Airframe plane;
    private final ContinentLayout world;
    private final CityMiasma miasma;
    private final Airfield airfield;
    private final int pilotId;
    private double x,y,altitude,heading,speed,verticalSpeed,fuel,roll;
    private int hp;
    private boolean airborne,lastDeflection;
    private Controls controls=new Controls(0,0,0);

    private AircraftFlight(AirPowerCatalog.Airframe plane,
            ContinentLayout world,CityMiasma miasma,Airfield airfield,int pilotId) {
        this.plane=plane;this.world=world;this.miasma=miasma;
        this.airfield=airfield;this.pilotId=pilotId;
        double back=airfield.lengthMetres()/(2*PARCEL_METRES);
        this.x=airfield.x()-back*Math.cos(airfield.heading());
        this.y=airfield.y()-back*Math.sin(airfield.heading());
        this.heading=airfield.heading();
        this.altitude=ground(x,y);
        this.hp=plane.maxHp();
    }
    private static double angularDistance(double a,double b){
        return Math.abs(Math.atan2(Math.sin(a-b),Math.cos(a-b)));
    }
    /**
     * Authoritative runway placement. Continent sectors are still macro,
     * not live buildable terrain. This denies world structures, protected
     * city airspace, highways, bad slopes, and too-short strips.
     */
    public static boolean validAirfield(ContinentLayout world,CityMiasma miasma,
            Airfield pad,AirPowerCatalog.Airframe plane) {
        if(world==null||miasma==null||pad==null||plane==null||
           pad.faction()!=plane.faction()||
           !Double.isFinite(pad.x())||!Double.isFinite(pad.y())||
           !Double.isFinite(pad.heading())||!Double.isFinite(pad.lengthMetres())||
           pad.lengthMetres()<plane.requiredRunwayMetres()||
           pad.lengthMetres()>360)return false;
        double half=pad.lengthMetres()/(2*PARCEL_METRES);
        double centerHeight=world.elevation((int)Math.floor(pad.x()),
            (int)Math.floor(pad.y()));
        if(!Double.isFinite(centerHeight))return false;
        for(double along=-half;along<=half;along+=.5){
            for(double offset:new double[]{-1,0,1}){
                double px=pad.x()+Math.cos(pad.heading())*along-
                    Math.sin(pad.heading())*offset;
                double py=pad.y()+Math.sin(pad.heading())*along+
                    Math.cos(pad.heading())*offset;
                int tx=(int)Math.floor(px),ty=(int)Math.floor(py);
                if(!world.buildableGround(tx,ty)||world.road(tx,ty)||
                   miasma.forbidsAircraft(px,py,0)||
                   Math.abs(world.elevation(tx,ty)-centerHeight)>3.0)
                    return false;
            }
        }
        return true;
    }
    /**
     * Only a FINISHED factory output is allowed to become an owned host craft.
     * Validation is performed before consuming the paid output to avoid loss
     * on a bad pad. Pilot permission and LAN identity integration are pending.
     */
    public static AircraftFlight commission(AirPowerCatalog.Airframe plane,
            AircraftWorkshop factory,ContinentLayout world,CityMiasma miasma,
            Airfield pad,int pilotId){
        if(plane==null||factory==null||pilotId<0||
           factory.faction()!=plane.faction()||
           !validAirfield(world,miasma,pad,plane))return null;
        if(!factory.withdrawCompleted(plane.modelId()))return null;
        return new AircraftFlight(plane,world,miasma,pad,pilotId);
    }
    public synchronized boolean controls(int callerId,Controls input){
        if(callerId!=pilotId||input==null||hp<=0)return false;
        controls=input;return true;
    }
    public synchronized State snapshot(){
        return new State(plane.modelId(),x,y,altitude,heading,speed,
            verticalSpeed,fuel,hp,airborne,roll,lastDeflection);
    }
    private double ground(double x,double y){
        return world.elevation((int)Math.floor(x),(int)Math.floor(y));
    }
    /** Fuel must exist in the host workshop store and is debited exactly once. */
    public synchronized boolean refuel(int callerId,AircraftWorkshop store,
            int canisters){
        if(callerId!=pilotId||store==null||canisters<1||canisters>20||
           airborne||speed>.05||hp<=0||
           fuel+canisters*CANISTER_FUEL_UNITS>plane.tankUnits()+1e-7)return false;
        if(!store.withdrawFuelCanisters(plane.modelId(),canisters))return false;
        fuel+=canisters*CANISTER_FUEL_UNITS;
        return true;
    }
    /** Explicit server tick; no client-authored transforms, fuel or HP. */
    public synchronized void advance(long milliseconds){
        if(milliseconds<1||milliseconds>250)
            throw new IllegalArgumentException("Flight ticks must be bounded");
        if(hp<=0)return;
        double dt=milliseconds/1000.0;
        lastDeflection=false;
        double powered= fuel>0?controls.throttle():0;
        double burn=fuel>0?(plane.idleFuelPerSecond()+
                            plane.throttleFuelPerSecond()*powered)*dt:0;
        fuel=Math.max(0,fuel-burn);
        speed=Math.max(0,Math.min(plane.maxSpeedParcelsPerSecond(),
            speed+plane.accelerationParcelsPerSecond2()*powered*dt-
            (airborne?.055:.085)*speed*dt-(powered==0?.10*dt:0)));
        double turn=controls.yaw()*plane.yawRadiansPerSecond()*
            (airborne?1:Math.min(1,speed))*dt;
        heading=Math.atan2(Math.sin(heading+turn),Math.cos(heading+turn));
        double dx=Math.cos(heading)*speed*dt,dy=Math.sin(heading)*speed*dt;
        var deflection=miasma.deflectFlight(x,y,x+dx,y+dy);
        lastDeflection=deflection.deflected();
        double distance=Math.hypot(deflection.x()-x,deflection.y()-y);
        if(lastDeflection){
            if(distance>1e-5)
                heading=Math.atan2(deflection.y()-y,deflection.x()-x);
            else speed=Math.min(speed,.5);
        }
        int cellX=(int)Math.floor(deflection.x()),cellY=(int)Math.floor(deflection.y());
        if(!world.inBounds(cellX,cellY)){
            speed=0;return;
        }
        x=deflection.x();y=deflection.y();
        if(!airborne){
            roll+=distance*PARCEL_METRES;
            if(roll>airfield.lengthMetres()){
                // An unsuccessful takeoff cannot roll indefinitely off a
                // non-streamed physical runway.
                speed=0;controls=new Controls(0,0,0);roll=0;return;
            }
            if(speed>=plane.takeoffSpeedParcelsPerSecond()&&controls.pitch()>.20){
                airborne=true;verticalSpeed=1.5;altitude=ground(x,y)+.5;
            }else altitude=ground(x,y);
            return;
        }
        // Higher speed gives more available lift; a stalled aircraft sinks,
        // including on fuel exhaustion. Bank and pitch are bounded intentions.
        double stallRatio=speed/plane.takeoffSpeedParcelsPerSecond();
        double requestedClimb=controls.pitch()*plane.climbMetresPerSecond()*
            Math.min(1,stallRatio);
        if(stallRatio<.67)requestedClimb-=5*(.67-stallRatio);
        verticalSpeed+=Math.max(-5*dt,Math.min(5*dt,
            requestedClimb-verticalSpeed));
        altitude+=verticalSpeed*dt;
        altitude=Math.min(altitude,ground(x,y)+plane.ceilingMetres());
        if(altitude>ground(x,y)+1.5)return;
        if(canLand()){
            airborne=false;altitude=ground(x,y);verticalSpeed=0;roll=0;
            speed*=.5;controls=new Controls(0,0,0);
        }else{
            // No free grass/water landing outside an approved strip.
            hp=Math.max(0,hp-(int)Math.ceil(30+speed*20+
                Math.max(0,-verticalSpeed)*8));
            altitude=ground(x,y);
            verticalSpeed=0;speed=0;controls=new Controls(0,0,0);
            airborne=false;
        }
    }
    private boolean canLand(){
        double dx=x-airfield.x(),dy=y-airfield.y();
        double along=Math.cos(airfield.heading())*dx+Math.sin(airfield.heading())*dy;
        double side=-Math.sin(airfield.heading())*dx+Math.cos(airfield.heading())*dy;
        return Math.abs(along)*PARCEL_METRES<airfield.lengthMetres()*.45&&
            Math.abs(side)<=1.0&&
            angularDistance(heading,airfield.heading())<.48&&
            speed<=plane.landingMaxSpeedParcelsPerSecond()&&
            verticalSpeed<=0&&verticalSpeed>=-3.5&&controls.throttle()<=.45&&
            validAirfield(world,miasma,airfield,plane);
    }
}
