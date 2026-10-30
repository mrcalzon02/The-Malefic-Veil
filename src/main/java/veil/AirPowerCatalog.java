package veil;

import java.util.*;

/**
 * Numeric first-pass aircraft flight/industry contract. Horizontal positions,
 * speed, and runway distances use continental MACRO PARCELS (6 metres each).
 * Altitude is metres above local terrain; fuel is finite faction-specific units.
 * Not a client permission to spawn assets: all mutations belong to the host.
 */
public final class AirPowerCatalog {
    public record Airframe(String modelId,CharacterRoster.Faction faction,
            AirPowerMeshes.Role role,String recipeId,String fuelItem,
            double tankUnits,double idleFuelPerSecond,double throttleFuelPerSecond,
            double maxSpeedParcelsPerSecond,double takeoffSpeedParcelsPerSecond,
            double landingMaxSpeedParcelsPerSecond,double accelerationParcelsPerSecond2,
            double yawRadiansPerSecond,double climbMetresPerSecond,
            double ceilingMetres,double requiredRunwayMetres,int maxHp) {
        public Airframe {
            if(modelId==null||recipeId==null||fuelItem==null||
                faction==null||role==null||maxHp<=0||
                tankUnits<=0||idleFuelPerSecond<=0||throttleFuelPerSecond<=0||
                maxSpeedParcelsPerSecond<=takeoffSpeedParcelsPerSecond||
                takeoffSpeedParcelsPerSecond<=0||
                landingMaxSpeedParcelsPerSecond<=0||
                accelerationParcelsPerSecond2<=0||yawRadiansPerSecond<=0||
                climbMetresPerSecond<=0||ceilingMetres<=20||
                requiredRunwayMetres<=0)
                throw new IllegalArgumentException("Invalid aircraft contract "+modelId);
        }
    }
    private static final List<Airframe> MODELS=List.of(
        new Airframe("unraveled_needlewing",CharacterRoster.Faction.THE_UNRAVELED,
            AirPowerMeshes.Role.LIGHT,"build-unraveled-needlewing",
            "AIR_FUEL_UNRAVELED",120,.018,.125,7.0,1.8,2.4,.65,1.25,8,300,78,100),
        new Airframe("unraveled_harrier",CharacterRoster.Faction.THE_UNRAVELED,
            AirPowerMeshes.Role.MEDIUM,"build-unraveled-harrier",
            "AIR_FUEL_UNRAVELED",170,.030,.18,5.5,2.3,2.9,.50,.85,6,330,126,250),
        new Airframe("unraveled_leviathan",CharacterRoster.Faction.THE_UNRAVELED,
            AirPowerMeshes.Role.HEAVY,"build-unraveled-leviathan",
            "AIR_FUEL_UNRAVELED",260,.05,.28,4.0,2.7,2.9,.40,.48,4,350,180,560),
        new Airframe("penitent_shrike",CharacterRoster.Faction.THE_PENITENT,
            AirPowerMeshes.Role.LIGHT,"build-penitent-shrike",
            "AIR_FUEL_PENITENT",120,.020,.13,7.2,1.85,2.5,.66,1.28,8,300,78,110),
        new Airframe("penitent_censer",CharacterRoster.Faction.THE_PENITENT,
            AirPowerMeshes.Role.MEDIUM,"build-penitent-censer",
            "AIR_FUEL_PENITENT",170,.032,.19,5.6,2.35,3.0,.51,.87,6,330,126,260),
        new Airframe("penitent_cathedral",CharacterRoster.Faction.THE_PENITENT,
            AirPowerMeshes.Role.HEAVY,"build-penitent-cathedral",
            "AIR_FUEL_PENITENT",260,.055,.30,4.0,2.7,3.0,.40,.48,4,350,180,580),
        new Airframe("begotten_gnat",CharacterRoster.Faction.THE_BEGOTTEN,
            AirPowerMeshes.Role.LIGHT,"build-begotten-gnat",
            "AIR_FUEL_BEGOTTEN",110,.025,.14,7.4,1.75,2.4,.68,1.32,9,310,72,90),
        new Airframe("begotten_mantis",CharacterRoster.Faction.THE_BEGOTTEN,
            AirPowerMeshes.Role.MEDIUM,"build-begotten-mantis",
            "AIR_FUEL_BEGOTTEN",160,.04,.20,5.7,2.25,2.9,.53,.90,7,340,120,240),
        new Airframe("begotten_behemoth",CharacterRoster.Faction.THE_BEGOTTEN,
            AirPowerMeshes.Role.HEAVY,"build-begotten-behemoth",
            "AIR_FUEL_BEGOTTEN",250,.06,.32,4.1,2.65,2.9,.42,.51,5,380,174,540)
    );
    private static final Map<String,Airframe> BY_ID;
    static{
        Map<String,Airframe> ids=new LinkedHashMap<>();
        for(Airframe model:MODELS) {
            if(ids.putIfAbsent(model.modelId(),model)!=null)
                throw new IllegalStateException("Duplicate airframe "+model.modelId());
            AirPowerMeshes.Spec mesh=AirPowerMeshes.spec(model.modelId());
            if(mesh==null||mesh.faction()!=model.faction()||mesh.role()!=model.role())
                throw new IllegalStateException("Aircraft model/flight identity mismatch "+model.modelId());
            var process=CraftingEraRegistry.process(model.recipeId());
            if(process==null||process.era()!=CraftingEraRegistry.Era.INCURSION||
                !process.output().equals("AIRFRAME_"+model.modelId().toUpperCase(Locale.ROOT))||
                process.quantity()!=1||process.seconds()<3600||
                process.utilityDemand()<25)
                throw new IllegalStateException("Unfunded aircraft manufacture "+model.modelId());
            var facility=CraftingEraRegistry.facility(process.facility());
            if(facility==null||facility.era()!=CraftingEraRegistry.Era.INCURSION||
                facility.faction()!=switch(model.faction()){
                    case THE_UNRAVELED -> CraftingEraRegistry.Faction.UNRAVELED;
                    case THE_PENITENT -> CraftingEraRegistry.Faction.PENITENT;
                    case THE_BEGOTTEN -> CraftingEraRegistry.Faction.BEGOTTEN;
                })throw new IllegalStateException("Cross-faction aircraft factory "+model.modelId());
            var fuel=CraftingEraRegistry.process(
                switch(model.faction()){
                    case THE_UNRAVELED -> "unraveled-refine-avgas";
                    case THE_PENITENT -> "penitent-distill-ichor";
                    case THE_BEGOTTEN -> "begotten-grow-flight-feed";
                });
            if(fuel==null||!fuel.output().equals(model.fuelItem()))
                throw new IllegalStateException("Aircraft lacks finite faction fuel "+model.modelId());
        }
        BY_ID=Map.copyOf(ids);
    }
    private AirPowerCatalog(){}
    public static List<Airframe> all(){return MODELS;}
    public static Airframe get(String id){return BY_ID.get(id);}
    public static void main(String[] args) {
        if(all().size()!=9)throw new AssertionError("Missing aircraft");
        for(Airframe plane:all()) {
            if(get(plane.modelId())!=plane||plane.tankUnits()<=0)
                throw new AssertionError("Aircraft catalog missing "+plane.modelId());
        }
        System.out.println("PASS AIR_POWER_CATALOG 9 distinct models, faction factories, paid fuels");
    }
}
