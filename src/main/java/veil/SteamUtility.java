package veil;

import java.util.*;

/** Shared steam is a physical utility, not a shared faction manufacture.
 * No recipe produces heat for free: each connected boiler consumes stocked
 * fuel and water. Steam shafts cannot connect different player settlements. */
public final class SteamUtility {
    public static final long MAX_CHARGE_MS=1_800_000L;
    public static final int CAPACITY=8;
    public static final Map<SurvivalCrafting.Item,Integer> FUEL=Map.of(
        SurvivalCrafting.Item.FIREWOOD,3,
        SurvivalCrafting.Item.DIRTY_WATER,2);
    private SteamUtility(){}
    /** A power demand must reach a living same-owner shaft and boiler. Each
     * caller supplies a per-tick allocation map, so concurrent jobs share
     * rather than duplicate a boiler's 8 units of capacity. */
    public static World.Device available(List<World.Device> devices,
                    World.Device machine,int units,Map<World.Device,Integer> used){
        if(machine==null||machine.hp<=0||units<1||units>CAPACITY||
            machine.ownerKey==null)return null;
        for(World.Device shaft:devices){
            if(shaft.hp<=0||shaft.kind!=ArenaSystems.MECHANICAL_SHAFT||
               !machine.ownerKey.equals(shaft.ownerKey)||
               Math.hypot(machine.x-shaft.x,machine.y-shaft.y)>8.0)continue;
            for(World.Device boiler:devices){
                if(boiler.hp<=0||boiler.kind!=ArenaSystems.STEAM_BOILER||
                   !machine.ownerKey.equals(boiler.ownerKey)||
                   Math.hypot(shaft.x-boiler.x,shaft.y-boiler.y)>8.0||
                   boiler.steamMs<=0)continue;
                if(used.getOrDefault(boiler,0)+units<=CAPACITY)return boiler;
            }
        }
        return null;
    }
}
