package veil;

/**
 * Design-only capacity feasibility model (one-hour normalized resource units).
 * Maxed defensive base may power its battery, but cannot manufacture every
 * consumable locally within its 28 normal-building slots. Remote settlements
 * must supply actual cargo via authoritative trade; this class transfers none.
 */
public final class SettlementSupplyBalance {
    public static final int MAX_NORMAL_FACILITIES=28;
    public static final int MAX_DEFENSES=10;
    // Command, security control, store, freight, water, maintenance, workshop,
    // and research/communications jointly reserve at least eight plant slots.
    public static final int MIN_CORE_SUPPORT_FACILITIES=8;
    public static final int GENERATOR_OUTPUT_MAX=45;
    public static final int MUNITIONS_OUTPUT_MAX=4;
    public record Report(int defenses,int defenseLevel,int core,int generators,
                         int munitionPlants,int powerDemand,int powerProduced,
                         int munitionDemand,int munitionProduced,int importedMunitionUnits,
                         boolean feasibleLocally,boolean feasibleWithImports){}
    private SettlementSupplyBalance(){}
    public static int powerPerDefense(int level){
        if(level<1||level>50)throw new IllegalArgumentException("Invalid defense level");
        return 4+(int)Math.ceil(28*Math.pow(level/50.0,1.6));
    }
    public static int ammoPerDefense(int level){
        if(level<1||level>50)throw new IllegalArgumentException("Invalid defense level");
        return 1+(int)Math.ceil(6*Math.pow(level/50.0,1.6));
    }
    public static Report plan(int defenses,int level,int generatorPlants,int munitionPlants){
        if(defenses<0||defenses>MAX_DEFENSES||generatorPlants<0||munitionPlants<0||
           generatorPlants+munitionPlants+MIN_CORE_SUPPORT_FACILITIES>MAX_NORMAL_FACILITIES)
            throw new IllegalArgumentException("Invalid 28-slot facility allocation");
        int demand=defenses*powerPerDefense(level);
        int ammo=defenses*ammoPerDefense(level);
        int produced=generatorPlants*GENERATOR_OUTPUT_MAX;
        int ammoProduced=munitionPlants*MUNITIONS_OUTPUT_MAX;
        int imports=Math.max(0,ammo-ammoProduced);
        return new Report(defenses,level,MIN_CORE_SUPPORT_FACILITIES,
            generatorPlants,munitionPlants,demand,produced,ammo,ammoProduced,
            imports,produced>=demand&&ammoProduced>=ammo,
            produced>=demand);
    }
    /** Exhaustively test all distributions of the *remaining* normal slots. */
    public static boolean canFullySupplyWithoutTrade(int defenses,int level){
        int free=MAX_NORMAL_FACILITIES-MIN_CORE_SUPPORT_FACILITIES;
        for(int generation=0;generation<=free;generation++)
            for(int munitions=0;munitions<=free-generation;munitions++)
                if(plan(defenses,level,generation,munitions).feasibleLocally())
                    return true;
        return false;
    }
}
