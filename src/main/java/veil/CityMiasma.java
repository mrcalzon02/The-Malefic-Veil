package veil;

import java.util.*;

/**
 * Immutable continental exclusion volumes for all playable aircraft. The
 * purple cloud is a rendering requirement, NOT a permission/safety control:
 * the streamed server must reject swept movement and aircraft materialization.
 *
 * Coordinates are currently MACRO parcels. Convert the identical boundaries
 * to fine streamed world coordinates before enabling any actual aircraft.
 */
public final class CityMiasma {
    public static final int CLEARANCE_PARCELS=8;
    public record Field(char city,double centerX,double centerY,double radius) {}
    private final List<Field> fields;

    public CityMiasma(ContinentLayout layout){
        Objects.requireNonNull(layout);
        List<Field> result=new ArrayList<>();
        for(ContinentLayout.City city:layout.cities()){
            // Diagonal circle encloses irregular/square city mask and adds
            // defensive air standoff; no altitude exploit over the smoke.
            double radius=Math.hypot(city.radiusX(),city.radiusY())+
                CLEARANCE_PARCELS;
            result.add(new Field(city.id(),city.x(),city.y(),radius));
        }
        fields=List.copyOf(result);
    }
    public List<Field> fields(){return fields;}
    /** The prohibition is a vertical column up to and beyond the flight ceiling. */
    public boolean forbidsAircraft(double x,double y,double altitude){
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(altitude))
            return true;
        for(Field f:fields)if(distanceSquared(x,y,f.centerX(),f.centerY())<=
                             f.radius()*f.radius())return true;
        return false;
    }
    /**
     * Reject tunnelling across a city in one network tick, even if both
     * endpoints are outside. Used for aircraft only; never restrict ground
     * scavengers, trucks, road traffic or access to city resource yards.
     */
    public boolean forbidsFlightSegment(double fromX,double fromY,double fromAltitude,
            double toX,double toY,double toAltitude){
        if(!Double.isFinite(fromX)||!Double.isFinite(fromY)||
           !Double.isFinite(toX)||!Double.isFinite(toY)||
           !Double.isFinite(fromAltitude)||!Double.isFinite(toAltitude))return true;
        double dx=toX-fromX,dy=toY-fromY;
        double magnitude=dx*dx+dy*dy;
        if(!Double.isFinite(magnitude))return true;
        for(Field f:fields){
            double parameter=magnitude==0?0:Math.max(0,Math.min(1,
                ((f.centerX()-fromX)*dx+(f.centerY()-fromY)*dy)/magnitude));
            double x=fromX+parameter*dx,y=fromY+parameter*dy;
            if(distanceSquared(x,y,f.centerX(),f.centerY())<=f.radius()*f.radius())
                return true;
        }
        return false;
    }
    /** Host movement response for aircraft, in macro coordinates.
     * Airborne paths contact a padded exclusion column and slide left/right
     * along its tangent. No displacement is granted for invalid/inside starts.
     * Never call this as a substitute for forbidsFlightSegment at spawn time.
     */
    public record FlightDeflection(double x,double y,boolean deflected) {}
    public FlightDeflection deflectFlight(double x,double y,double wantedX,double wantedY){
        if(!Double.isFinite(x)||!Double.isFinite(y)||
           !Double.isFinite(wantedX)||!Double.isFinite(wantedY))
            return new FlightDeflection(x,y,true);
        if(forbidsAircraft(x,y,0))
            return new FlightDeflection(x,y,true);
        double atX=x,atY=y,endX=wantedX,endY=wantedY;
        boolean blocked=false;
        for(int turn=0;turn<fields.size()+1;turn++){
            double dx=endX-atX,dy=endY-atY;
            double a=dx*dx+dy*dy;
            if(a<1e-12)break;
            double first=Double.POSITIVE_INFINITY;
            Field collided=null;
            for(Field field:fields){
                double radius=field.radius()+.2; // two-tenths parcel standoff
                double rx=atX-field.centerX(),ry=atY-field.centerY();
                double b=2*(rx*dx+ry*dy);
                double d=b*b-4*a*(rx*rx+ry*ry-radius*radius);
                if(d<0)continue;
                double t=(-b-Math.sqrt(d))/(2*a);
                if(t>=0&&t<=1&&t<first){
                    first=t;collided=field;
                }
            }
            if(collided==null){
                if(!forbidsFlightSegment(atX,atY,10,endX,endY,10))
                    return new FlightDeflection(endX,endY,blocked);
                break;
            }
            blocked=true;
            double contactX=atX+dx*Math.max(0,first-1e-5);
            double contactY=atY+dy*Math.max(0,first-1e-5);
            double rx=contactX-collided.centerX();
            double ry=contactY-collided.centerY();
            double length=Math.hypot(rx,ry);
            if(length<1e-9)break;
            double tx=-ry/length,ty=rx/length;
            double side=tx*dx+ty*dy;
            if(Math.abs(side)<1e-8)side=(collided.city()%2==0)?-1:1;
            double remaining=Math.hypot(dx,dy)*(1-first)*.72;
            endX=contactX+Math.copySign(remaining,side)*tx;
            endY=contactY+Math.copySign(remaining,side)*ty;
            atX=contactX;atY=contactY;
            if(forbidsAircraft(atX,atY,10))break;
        }
        // A multi-column corner or malformed target cannot teleport a plane
        // through a protected field. The last known exterior point is safe.
        return new FlightDeflection(atX,atY,true);
    }

    private static double distanceSquared(double x,double y,double cx,double cy){
        return (x-cx)*(x-cx)+(y-cy)*(y-cy);
    }
}
