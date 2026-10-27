package veil;
import java.util.*;

/** Physical west-to-east conveyor between distinct, owned crates.
 * Routing never references remote account inventory, client coordinates,
 * shared faction stock or temporary deployables. */
public final class ConveyorLogistics {
    public static final int DEMAND=2;
    public static final int SORTED_DEMAND=4;
    public static final long TICK_MS=5_000;
    private ConveyorLogistics(){}
    public record Route(World.Device source,World.Device destination){}
    public static Route between(World.Device conveyor,List<World.Device> devices){
        if(conveyor==null||conveyor.hp<=0||
           conveyor.kind!=ArenaSystems.CONVEYOR_LINE||
           conveyor.ownerKey==null)return null;
        World.Device west=null,east=null;
        double westDist=Double.MAX_VALUE,eastDist=Double.MAX_VALUE;
        for(World.Device d:devices){
            if(d.kind!=ArenaSystems.STORAGE_CRATE||d.hp<=0||
               !conveyor.ownerKey.equals(d.ownerKey))continue;
            double dx=d.x-conveyor.x,dy=Math.abs(d.y-conveyor.y);
            double dist=Math.hypot(dx,dy);
            if(dy>2.7||dist>6||Math.abs(dx)<1.0)continue;
            if(dx<0&&dist<westDist){west=d;westDist=dist;}
            if(dx>0&&dist<eastDist){east=d;eastDist=dist;}
        }
        return west==null||east==null||west==east?null:new Route(west,east);
    }
    /** An actual powered automatic sorter mounted near the belt improves
     * throughput. It never changes recipe/faction stock ownership or acts as
     * free automation merely because it exists in the registry. */
    public static boolean sorted(World.Device conveyor,List<World.Device> devices){
        if(conveyor==null||conveyor.hp<=0||
           conveyor.kind!=ArenaSystems.CONVEYOR_LINE||
           conveyor.ownerKey==null)return false;
        for(World.Device device:devices)
            if(device.kind==ArenaSystems.AUTOMATIC_SORTER&&device.hp>0&&
               conveyor.ownerKey.equals(device.ownerKey)&&
               Math.hypot(device.x-conveyor.x,device.y-conveyor.y)<=3.5)
                return true;
        return false;
    }
    public static boolean ready(Route route,SurvivalCrafting.Item item){
        return route!=null&&item!=null&&
            route.source().storage.count(item)>0&&
            route.destination().storage.canAdd(item,1);
    }
    /** At most one unit per paid tick. Preflight all destinations BEFORE
     * subtracting source inventory; transactional under ArenaSystems host lock. */
    public static boolean moveOne(Route route,SurvivalCrafting.Item item){
        if(!ready(route,item))return false;
        if(!route.source().storage.consume(Map.of(item,1)))return false;
        if(!route.destination().storage.delivered(item,1))
            throw new IllegalStateException("Conveyor destination capacity changed");
        return true;
    }
}
