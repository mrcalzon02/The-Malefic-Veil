package veil;
import java.util.*;

/** Physical owner-programmed directional conveyor between distinct, owned crates.
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
        return west==null||east==null||west==east?null:
            conveyor.routeReverse?new Route(east,west):new Route(west,east);
    }
    /** A host-derived E5 input request: one physically stocked sender, one
     * separate receiving crate beside a pending same-owner machine. No item
     * is synthesized, reserved or transferred by inspecting this plan. */
    public record InputTransfer(Route route,SurvivalCrafting.Item item){}
    public static InputTransfer autoInput(World.Device belt,List<World.Device> devices){
        if(belt==null||belt.hp<=0||belt.kind!=ArenaSystems.CONVEYOR_LINE||
           belt.ownerKey==null||!belt.ownerKey.startsWith("acct_")||
           devices==null)return null;
        boolean reverse=belt.routeReverse;
        InputTransfer best=null;
        int bestStaged=-1;
        double bestDistance=Double.MAX_VALUE;
        for(World.Device machine:devices){
            if(machine.hp<=0||machine.queuedRecipeId.isEmpty()||
               !belt.ownerKey.equals(machine.ownerKey))continue;
            var job=CraftingEraRegistry.process(AutomationOrders.recipe(machine.queuedRecipeId));
            if(job==null||job.era()!=CraftingEraRegistry.Era.AUTOMATION||
               !job.facility().equals(ArenaSystems.factionFacility(machine.kind)))continue;
            for(World.Device target:devices){
                if(!eligibleCrate(belt,target,reverse)||
                   Math.hypot(machine.x-target.x,machine.y-target.y)>5.5)continue;
                int staged=0;
                List<String> names=new ArrayList<>(job.inputs().keySet());
                Collections.sort(names);
                for(String name:names){
                    var item=SurvivalCrafting.Item.valueOf(name);
                    staged+=Math.min(job.inputs().get(name),target.storage.count(item));
                }
                // Prefer crates already holding other components of the SAME
                // paid recipe, rather than scattering parts across warehouses.
                for(String name:names){
                    var item=SurvivalCrafting.Item.valueOf(name);
                    if(target.storage.count(item)>=job.inputs().get(name)||
                       !target.storage.canAdd(item,1))continue;
                    for(World.Device source:devices){
                        if(source==target||!eligibleCrate(belt,source,!reverse)||
                           source.storage.count(item)<=0)continue;
                        double distance=Math.hypot(machine.x-target.x,machine.y-target.y)+
                            Math.hypot(belt.x-source.x,belt.y-source.y);
                        if(staged>bestStaged||
                           (staged==bestStaged&&distance<bestDistance)){
                            bestStaged=staged;bestDistance=distance;
                            best=new InputTransfer(new Route(source,target),item);
                        }
                    }
                }
            }
        }
        return best;
    }
    private static boolean eligibleCrate(World.Device belt,World.Device crate,boolean west){
        if(crate.hp<=0||crate.kind!=ArenaSystems.STORAGE_CRATE||
           !belt.ownerKey.equals(crate.ownerKey))return false;
        double dx=crate.x-belt.x,dy=Math.abs(crate.y-belt.y);
        return dy<=2.7&&Math.hypot(dx,dy)<=6.0&&
            (west?dx<=-1.0:dx>=1.0);
    }
    /**
     * Complete a paid manufacturing batch into a nearby physical crate.
     * If an owner-programmed conveyor can carry that output into a crate
     * serving a genuinely queued E5 consumer, prefer its reachable source
     * over arbitrary insertion order. All receipts stay in actual storage.
     *
     * Does not require the belt to be currently powered: output delivery
     * is not belt travel. The belt must still pay its normal grid load and
     * transfer time before anything reaches the next factory.
     */
    public static World.Device outputCrate(World.Device producer,
            SurvivalCrafting.Item output,int quantity,List<World.Device> devices){
        if(producer==null||producer.hp<=0||producer.ownerKey==null||
           output==null||quantity<1||devices==null)return null;
        World.Device fallback=null,priority=null;
        int score=-1;
        double bestDistance=Double.MAX_VALUE;
        for(World.Device candidate:devices){
            if(candidate.hp<=0||candidate.kind!=ArenaSystems.STORAGE_CRATE||
               !producer.ownerKey.equals(candidate.ownerKey)||
               Math.hypot(candidate.x-producer.x,candidate.y-producer.y)>5.5||
               !candidate.storage.canAdd(output,quantity))continue;
            if(fallback==null)fallback=candidate;
            for(World.Device belt:devices){
                if(belt.hp<=0||belt.kind!=ArenaSystems.CONVEYOR_LINE||
                   !producer.ownerKey.equals(belt.ownerKey)||
                   belt.routeItem.isEmpty())continue;
                boolean auto=belt.routeItem.equals("AUTO");
                if(!auto&&!belt.routeItem.equals(output.name()))continue;
                if(!eligibleCrate(belt,candidate,!belt.routeReverse))continue;
                Route manual=auto?null:between(belt,devices);
                if(!auto&&(manual==null||manual.source()!=candidate))continue;
                for(World.Device consumer:devices){
                    if(consumer.hp<=0||consumer.queuedRecipeId.isEmpty()||
                       !producer.ownerKey.equals(consumer.ownerKey))continue;
                    var job=CraftingEraRegistry.process(AutomationOrders.recipe(consumer.queuedRecipeId));
                    if(job==null||job.era()!=CraftingEraRegistry.Era.AUTOMATION||
                       !job.facility().equals(ArenaSystems.factionFacility(consumer.kind))||
                       !job.inputs().containsKey(output.name()))continue;
                    int need=job.inputs().get(output.name());
                    for(World.Device receiver:devices){
                        if(receiver==candidate||!eligibleCrate(belt,receiver,belt.routeReverse)||
                           (!auto&&manual.destination()!=receiver)||
                           Math.hypot(receiver.x-consumer.x,receiver.y-consumer.y)>5.5||
                           receiver.storage.count(output)>=need||
                           !receiver.storage.canAdd(output,1))continue;
                        int staged=0;
                        for(var ingredient:job.inputs().entrySet()){
                            var item=SurvivalCrafting.Item.valueOf(ingredient.getKey());
                            staged+=Math.min(ingredient.getValue(),receiver.storage.count(item));
                        }
                        double distance=Math.hypot(belt.x-candidate.x,belt.y-candidate.y)+
                            Math.hypot(receiver.x-consumer.x,receiver.y-consumer.y);
                        if(staged>score||(staged==score&&distance<bestDistance)){
                            score=staged;bestDistance=distance;priority=candidate;
                        }
                    }
                }
            }
        }
        return priority!=null?priority:fallback;
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
