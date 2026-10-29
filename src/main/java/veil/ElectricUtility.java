package veil;
import java.util.*;
/** Shared physical utility. Faction technology and manufacturing stay separate. */
public final class ElectricUtility {
    public static final int CAPACITY=24; // E5 assembly machines demand up to 18.
    public static final long MAX_CHARGE_MS=1_200_000L,STEAM_STARTUP_MS=60_000L;
    public static final Map<SurvivalCrafting.Item,Integer> FUEL=Map.of(
        SurvivalCrafting.Item.CHARCOAL,3,SurvivalCrafting.Item.CLEAN_WATER,2);
    private ElectricUtility(){}
    /** Only an authenticated generator next to a paid live steam boiler
     * may capture a startup pressure reserve. */
    public static World.Device turbineSource(List<World.Device> devices,World.Device generator){
        if(generator==null||generator.hp<=0||
           generator.kind!=ArenaSystems.GENERATOR||generator.ownerKey==null)return null;
        for(World.Device boiler:devices)
            if(boiler.hp>0&&boiler.kind==ArenaSystems.STEAM_BOILER&&
               generator.ownerKey.equals(boiler.ownerKey)&&
               Math.hypot(generator.x-boiler.x,generator.y-boiler.y)<=8&&
               boiler.steamMs>=STEAM_STARTUP_MS)return boiler;
        return null;
    }
    /** A live same-owner power bus must connect factory -> bus -> generator.
     * Each generating plant offers only CAPACITY per simulation tick. */
    public static World.Device available(List<World.Device> devices,World.Device machine,
                      int load,Map<World.Device,Integer> allocated){
        return available(devices,machine,load,allocated,1);
    }
    /** Runtime reservation must hold enough actual charge for the ENTIRE
     * requested simulation tick, not merely one remaining millisecond. */
    public static World.Device available(List<World.Device> devices,World.Device machine,
                      int load,Map<World.Device,Integer> allocated,long requiredMs){
        if(machine==null||machine.hp<=0||machine.ownerKey==null||
           load<1||load>CAPACITY||requiredMs<1)return null;
        for(World.Device bus:devices){
            if(bus.hp<=0||bus.kind!=ArenaSystems.ELECTRIC_BUS||
               !machine.ownerKey.equals(bus.ownerKey)||
               Math.hypot(machine.x-bus.x,machine.y-bus.y)>8)continue;
            for(World.Device generator:devices){
                if(generator.hp<=0||generator.kind!=ArenaSystems.GENERATOR||
                   !machine.ownerKey.equals(generator.ownerKey)||
                   Math.hypot(bus.x-generator.x,bus.y-generator.y)>8||
                   generator.electricMs<requiredMs)continue;
                if(allocated.getOrDefault(generator,0)+load<=CAPACITY)
                    return generator;
            }
        }
        return null;
    }

    /** A single generator remains capped at 24 load. Incursion processes may
     * reserve up to two separately built, fueled and charged generators over
     * real same-owner bus connections. This is a READ-ONLY proposal: callers
     * commit every returned per-generator share together, or none of them.
     * Legacy <=24-load jobs continue to require one complete source. */
    public static Map<World.Device,Integer> allocation(List<World.Device> devices,
            World.Device machine,int load,Map<World.Device,Integer> allocated,
            long requiredMs){
        if(machine==null||machine.hp<=0||machine.ownerKey==null||
           load<1||load>2*CAPACITY||requiredMs<1)return Map.of();
        if(load<=CAPACITY){
            World.Device single=available(devices,machine,load,allocated,requiredMs);
            return single==null?Map.of():Map.of(single,load);
        }
        LinkedHashMap<World.Device,Integer> plan=new LinkedHashMap<>();
        int remaining=load;
        for(World.Device bus:devices){
            if(bus.hp<=0||bus.kind!=ArenaSystems.ELECTRIC_BUS||
               !machine.ownerKey.equals(bus.ownerKey)||
               Math.hypot(machine.x-bus.x,machine.y-bus.y)>8)continue;
            for(World.Device generator:devices){
                if(generator.hp<=0||generator.kind!=ArenaSystems.GENERATOR||
                   !machine.ownerKey.equals(generator.ownerKey)||
                   Math.hypot(bus.x-generator.x,bus.y-generator.y)>8||
                   generator.electricMs<requiredMs||
                   plan.containsKey(generator))continue;
                int room=CAPACITY-allocated.getOrDefault(generator,0);
                if(room<=0)continue;
                int share=Math.min(room,remaining);
                plan.put(generator,share);
                remaining-=share;
                if(remaining==0)return Collections.unmodifiableMap(plan);
            }
        }
        return Map.of();
    }
}
