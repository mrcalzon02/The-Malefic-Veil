package veil;

import java.util.*;

/**
 * Complete cheap proxy-art construction index from the authoritative factory
 * and farming facility catalog. Entries are PLANNED meshing instructions, not
 * rendered/interactive buildings. An actual PSX mesh may replace each proxy
 * without changing its authoritative ID, footprint, function, or save schema.
 */
public final class CrudeStructureIndex {
    public enum Primitive {
        TENT_FRAME, STACKED_CRATES, RAISED_BEDS, FENCED_CORRAL,
        STAKE_WALL, WATER_CISTERN, STONE_HEARTH, WATCH_PLATFORM,
        SMOKESTACK, MACHINE_FRAME, PIPE_ARRAY, TRANSPORT_SHED,
        FOUNDRY_SHED, RITUAL_ALTAR, LIVING_NEST, MONUMENT,
        ROTARY_TOWER, HEAVY_AA_PEDESTAL, GROUND_TURRET
    }
    public record Proxy(String id,Primitive primitive,int width,int depth,
                        CraftingEraRegistry.Era minimumEra,
                        String replacementAssetGate,
                        boolean level50RetrofitAllowed) {}
    private static final Map<String,Proxy> INDEX;
    static {
        Map<String,Proxy> entries=new LinkedHashMap<>();
        for(var f:CraftingEraRegistry.facilities().values()){
            Primitive kind=guess(f.id());
            if(entries.putIfAbsent(f.id(),new Proxy(f.id(),kind,f.width(),
                    f.depth(),f.era(),"SOURCE_VERIFIED_PSX_MESH_PENDING",true))!=null)
                throw new IllegalStateException("Duplicate proxy ID");
        }
        // All three defensive template types also receive constructible
        // proxy targets; faction-specific working art must later replace them.
        entries.put("aa-fast",new Proxy("aa-fast",Primitive.ROTARY_TOWER,
            6,6,CraftingEraRegistry.Era.AUTOMATION,
            "SIX_FACTION_AA_ART_PENDING",true));
        entries.put("aa-heavy",new Proxy("aa-heavy",Primitive.HEAVY_AA_PEDESTAL,
            8,8,CraftingEraRegistry.Era.AUTOMATION,
            "SIX_FACTION_AA_ART_PENDING",true));
        entries.put("automated-turret",new Proxy("automated-turret",
            Primitive.GROUND_TURRET,5,5,CraftingEraRegistry.Era.ELECTRIC,
            "FACTION_DEFENSE_MESH_PENDING",true));
        INDEX=Collections.unmodifiableMap(entries);
    }
    private CrudeStructureIndex(){}
    public static Map<String,Proxy> entries(){return INDEX;}
    public static Proxy get(String id){return INDEX.get(id);}
    private static Primitive guess(String name){
        if(name.equals("monument-graveyard"))return Primitive.MONUMENT;
        if(name.contains("nutrient")||name.contains("incursion")||
           name.contains("greenhouse"))return Primitive.LIVING_NEST;
        if(name.contains("dread")||name.contains("terror"))
            return Primitive.RITUAL_ALTAR;
        if(name.contains("palisade")||name.contains("wall"))
            return Primitive.STAKE_WALL;
        if(name.contains("garden")||name.contains("plot")||
           name.contains("nursery")||name.contains("compost"))
            return Primitive.RAISED_BEDS;
        if(name.contains("animal-pen"))return Primitive.FENCED_CORRAL;
        if(name.contains("rain")||name.contains("water")||
           name.contains("irrigation"))return Primitive.WATER_CISTERN;
        if(name.contains("hearth")||name.contains("charcoal")||
           name.contains("furnace")||name.contains("smokehouse"))
            return Primitive.STONE_HEARTH;
        if(name.contains("watch")||name.contains("signal"))
            return Primitive.WATCH_PLATFORM;
        if(name.contains("store")||name.contains("cargo")||
           name.contains("cellar"))return Primitive.STACKED_CRATES;
        if(name.contains("freight")||name.contains("motor")||
           name.contains("assembly"))return Primitive.TRANSPORT_SHED;
        if(name.contains("forge")||name.contains("smelt")||
           name.contains("foundry"))return Primitive.FOUNDRY_SHED;
        if(name.contains("boiler")||name.contains("generator")||
           name.contains("refinery"))return Primitive.SMOKESTACK;
        if(name.contains("workshop")||name.contains("lathe")||
           name.contains("shaft")||name.contains("sorter")||
           name.contains("press"))return Primitive.MACHINE_FRAME;
        if(name.contains("pipe")||name.contains("conveyor")||
           name.contains("electric"))return Primitive.PIPE_ARRAY;
        return Primitive.TENT_FRAME;
    }
}
