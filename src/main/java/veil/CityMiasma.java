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
    private static double distanceSquared(double x,double y,double cx,double cy){
        return (x-cx)*(x-cx)+(y-cy)*(y-cy);
    }
}
