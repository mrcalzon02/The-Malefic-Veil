package veil;

import java.util.*;

/**
 * Canonical late-game transport manufacturing lookup. Transport operates on
 * finished outputs of real, faction-owned industrial processes, never on
 * improvised item IDs or faction-neutral free vehicle spawns.
 */
public final class TransportProduction {
    public enum Product {
        RUNWAY_PANEL,RAIL_SECTION,RAIL_FUEL,MARINE_FUEL,TRANSPORT_REPAIR,
        RUNWAY_LIGHT,RAIL_SIGNAL,LOCOMOTIVE,FREIGHT_WAGON,CARRIER_KEEL,CARRIER
    }
    private TransportProduction(){}
    public static CraftingEraRegistry.Faction industryFaction(CharacterRoster.Faction faction){
        Objects.requireNonNull(faction);
        return switch(faction){
            case THE_UNRAVELED -> CraftingEraRegistry.Faction.UNRAVELED;
            case THE_PENITENT -> CraftingEraRegistry.Faction.PENITENT;
            case THE_BEGOTTEN -> CraftingEraRegistry.Faction.BEGOTTEN;
        };
    }
    public static String suffix(CharacterRoster.Faction faction){
        return industryFaction(faction).name();
    }
    public static String item(Product product,CharacterRoster.Faction faction){
        Objects.requireNonNull(product);
        return product.name()+"_"+suffix(faction);
    }
    public static String transportWorks(CharacterRoster.Faction faction){
        return switch(faction){
            case THE_UNRAVELED -> "unraveled-transport-forge";
            case THE_PENITENT -> "penitent-procession-works";
            case THE_BEGOTTEN -> "begotten-transit-vat";
        };
    }
    public static String shipyard(CharacterRoster.Faction faction){
        return switch(faction){
            case THE_UNRAVELED -> "unraveled-naval-yard";
            case THE_PENITENT -> "penitent-slaughter-dock";
            case THE_BEGOTTEN -> "begotten-tidal-brood";
        };
    }
    private static String slug(CharacterRoster.Faction faction){
        return suffix(faction).toLowerCase(Locale.ROOT);
    }
    public static String recipeId(Product product,CharacterRoster.Faction faction){
        String kind=switch(product){
            case RUNWAY_PANEL -> "runway-panels";
            case RAIL_SECTION -> "rail-sections";
            case RAIL_FUEL -> "rail-fuel";
            case MARINE_FUEL -> "marine-fuel";
            case TRANSPORT_REPAIR -> "transport-repair";
            case RUNWAY_LIGHT -> "runway-light";
            case RAIL_SIGNAL -> "rail-signal";
            case LOCOMOTIVE -> "locomotive";
            case FREIGHT_WAGON -> "freight-wagon";
            case CARRIER_KEEL -> "carrier-keel";
            case CARRIER -> "carrier";
        };
        return slug(faction)+"-"+kind;
    }
    /** A failed lookup here blocks release rather than accepting an
     * intentionally or accidentally generic factory of a different faction.
     */
    public static CraftingEraRegistry.Process recipe(Product product,
            CharacterRoster.Faction faction){
        var process=CraftingEraRegistry.process(recipeId(product,faction));
        if(process==null||process.faction()!=industryFaction(faction)||
           process.era()!=CraftingEraRegistry.Era.INCURSION||
           !process.output().equals(item(product,faction))||
           process.quantity()<1||process.seconds()<300||
           process.utilityDemand()<10)
            throw new IllegalStateException("Missing/invalid transport recipe "+
                product+" "+faction);
        String required=product==Product.CARRIER||product==Product.CARRIER_KEEL||
                product==Product.MARINE_FUEL?
                shipyard(faction):transportWorks(faction);
        if(!required.equals(process.facility()))
            throw new IllegalStateException("Transport made in wrong factory "+process.id());
        var facility=CraftingEraRegistry.facility(required);
        if(facility==null||facility.faction()!=industryFaction(faction)||
           facility.era()!=CraftingEraRegistry.Era.INCURSION)
            throw new IllegalStateException("Uncommissioned transport factory "+required);
        return process;
    }
    public static void validate(){
        Set<String> outputs=new HashSet<>();
        for(var faction:CharacterRoster.Faction.values()){
            for(var product:Product.values()){
                var process=recipe(product,faction);
                if(!outputs.add(process.output()))
                    throw new IllegalStateException("Duplicate transport output");
            }
            var carrier=recipe(Product.CARRIER,faction);
            if(carrier.inputs().getOrDefault(item(Product.CARRIER_KEEL,faction),0)!=1)
                throw new IllegalStateException("Carrier can bypass shipyard keel");
            if(recipe(Product.LOCOMOTIVE,faction).inputs().size()<4)
                throw new IllegalStateException("Locomotive lacks industry bill");
        }
        if(outputs.size()!=33)throw new IllegalStateException("Transport contract incomplete");
    }
}
