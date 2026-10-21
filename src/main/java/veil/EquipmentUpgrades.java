package veil;

import java.util.*;

/**
 * Upgrade recipe and scaling DESIGN DATA for structures, defenses and vehicles.
 * Nothing here debits inventory, repairs entities or updates live world state.
 * Upgrade and building transaction gates must consume these amounts once.
 */
public final class EquipmentUpgrades {
    public static final int MAX_LEVEL=50;
    public enum Class { FACILITY, DEFENSE, VEHICLE }
    public record Plan(String assetId,Class kind,int fromLevel,int toLevel,
                       CraftingEraRegistry.Era requiredEra,
                       Map<String,Integer> materials,int workSeconds,
                       double effectiveness,double powerOrFuelCostMultiplier,
                       double upkeepMultiplier,String visibleStage) {}
    private EquipmentUpgrades(){}
    private static boolean known(String id,Class kind){
        if(id==null)return false;
        return switch(kind){
            case FACILITY -> CraftingEraRegistry.facilities().containsKey(id)||
                SettlementAuthority.blueprintKinds().get(id)==SettlementAuthority.Kind.FACILITY;
            case DEFENSE -> SettlementAuthority.blueprintKinds().get(id)==
                SettlementAuthority.Kind.DEFENSE;
            case VEHICLE -> Set.of("ground-hatchback","ground-sedan","ground-wagon",
                "ground-minivan","ground-stepvan","ground-vintage",
                "air-light","air-medium","air-heavy").contains(id);
        };
    }
    public static CraftingEraRegistry.Era eraForLevel(int level){
        if(level<1||level>MAX_LEVEL)throw new IllegalArgumentException("Upgrade level 1..50");
        if(level>=50)return CraftingEraRegistry.Era.INCURSION;
        if(level>=40)return CraftingEraRegistry.Era.AUTOMATION;
        if(level>=30)return CraftingEraRegistry.Era.ELECTRIC;
        if(level>=20)return CraftingEraRegistry.Era.STEAM;
        if(level>=10)return CraftingEraRegistry.Era.SALVAGE;
        return CraftingEraRegistry.Era.ENCAMPMENT;
    }
    public static String visualStage(int level){
        if(level<10)return "CRUDE";
        if(level<20)return "REINFORCED";
        if(level<30)return "MECHANIZED";
        if(level<40)return "ELECTRIFIED";
        if(level<50)return "INDUSTRIAL";
        return "ENDGAME_SPECIALIZED";
    }
    /**
     * Always exactly one level per committed transaction, no interpolation
     * between tiers or paying one cheap low-level recipe for a level-50 jump.
     */
    public static Plan next(String id,Class kind,int currentLevel,
                            int ownerLevel,CraftingEraRegistry.Era availableEra){
        Objects.requireNonNull(kind);Objects.requireNonNull(availableEra);
        if(!known(id,kind)||currentLevel<1||currentLevel>=MAX_LEVEL||
           ownerLevel<1||ownerLevel>MAX_LEVEL)
            throw new IllegalArgumentException("Invalid or maxed upgrade target");
        int next=currentLevel+1;
        CraftingEraRegistry.Era tech=eraForLevel(next);
        if(kind==Class.DEFENSE&&tech.ordinal()<CraftingEraRegistry.Era.AUTOMATION.ordinal())
            tech=CraftingEraRegistry.Era.AUTOMATION;
        if(kind==Class.VEHICLE&&tech.ordinal()<CraftingEraRegistry.Era.SALVAGE.ordinal())
            tech=CraftingEraRegistry.Era.SALVAGE;
        if(kind==Class.VEHICLE&&id.startsWith("air-")&&
           tech.ordinal()<CraftingEraRegistry.Era.INCURSION.ordinal())
            tech=CraftingEraRegistry.Era.INCURSION;
        if(kind==Class.FACILITY){
            var facility=CraftingEraRegistry.facility(id);
            if(facility!=null&&facility.era().ordinal()>tech.ordinal())tech=facility.era();
        }
        if(next>ownerLevel||availableEra.ordinal()<tech.ordinal())
            throw new IllegalStateException(next>ownerLevel?
                "OWNER_LEVEL_LOCKED":"RESEARCH_ERA_LOCKED");
        int n=next;
        int base=kind==Class.DEFENSE?4:kind==Class.VEHICLE?3:2;
        Map<String,Integer> materials=new LinkedHashMap<>();
        if(n<10){
            materials.put("TIMBER",base+n/2);
            materials.put("FIELD_STONE",base+n/3);
        }else if(n<20){
            materials.put("METAL_BILLET",base+n/3);
            materials.put("FASTENERS",base+n/2);
        }else if(n<30){
            materials.put("GEAR_ASSEMBLY",base+n/6);
            materials.put("METAL_BILLET",base+n/4);
        }else if(n<40){
            materials.put("CONTROL_BOARD",base+n/10);
            materials.put("PRECISION_MODULE",base+n/7);
        }else{
            materials.put("ASSEMBLY_CONTROLLER",base+n/10);
            materials.put("PRECISION_MODULE",base+n/6);
            materials.put("MACHINE_PARTS",base+n/9);
        }
        // Runtime will add faction-specific licensed components at high tiers.
        int work=base*75+n*n*6;
        double efficiency=1.0+1.65*Math.pow((n-1.0)/(MAX_LEVEL-1),.8);
        double energy=1.0+1.8*Math.pow((n-1.0)/(MAX_LEVEL-1),1.4);
        double upkeep=1.0+2.1*Math.pow((n-1.0)/(MAX_LEVEL-1),1.5);
        return new Plan(id,kind,currentLevel,next,tech,Map.copyOf(materials),
            work,efficiency,energy,upkeep,visualStage(next));
    }
}
