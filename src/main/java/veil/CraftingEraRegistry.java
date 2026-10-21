package veil;

import java.util.*;

/**
 * Versioned DATA registry of facility prerequisites and industry processes.
 *
 * No meshes, client controls or world mutation live here; the authoritative
 * wilderness/base runtime must instantiate and transactionally execute these
 * descriptors in later release gates. Validate data on class initialization
 * so missing prereqs and circular or free recipes are CI-breaking.
 */
public final class CraftingEraRegistry {
    public static final int SCHEMA_VERSION=1;
    public enum Era { STRANDED, ENCAMPMENT, SALVAGE, STEAM, ELECTRIC, AUTOMATION, INCURSION }
    public enum Faction { ALL, UNRAVELED, PENITENT, BEGOTTEN }
    public record Facility(String id,Era era,Faction faction,
                           List<String> requires,Map<String,Integer> materials,
                           int width,int depth,int seconds,int utilityDemand){}
    public record Process(String id,Era era,Faction faction,String facility,
                          Map<String,Integer> inputs,String output,int quantity,
                          int seconds,int utilityDemand){}
    private static final LinkedHashMap<String,Facility> FACILITIES=new LinkedHashMap<>();
    private static final LinkedHashMap<String,Process> PROCESSES=new LinkedHashMap<>();
    private static final Set<String> EXTERNAL=Set.of(
        "TIMBER","FIELD_STONE","FIBER","EDIBLES","MEDICINAL_HERBS",
        "RAW_ORE","DIRTY_WATER","METAL_STOCK","MACHINE_PARTS","DREAD_IMPRINT");

    private static Map<String,Integer> cost(Object... parts){
        if(parts.length==0||parts.length%2!=0)throw new IllegalArgumentException("Empty material cost");
        LinkedHashMap<String,Integer> map=new LinkedHashMap<>();
        for(int i=0;i<parts.length;i+=2){
            String key=(String)parts[i];
            int amount=(Integer)parts[i+1];
            if(key==null||key.isBlank()||amount<1||map.putIfAbsent(key,amount)!=null)
                throw new IllegalArgumentException("Invalid material "+key);
        }
        return Collections.unmodifiableMap(map);
    }
    private static void facility(String id,Era era,Faction faction,String deps,
                                 int width,int depth,int seconds,int utility,Object... materials){
        List<String> dependencies=deps.isBlank()?List.of():List.of(deps.split(" "));
        Facility f=new Facility(id,era,faction,dependencies,cost(materials),width,depth,seconds,utility);
        if(FACILITIES.putIfAbsent(id,f)!=null)throw new IllegalArgumentException("Duplicate facility "+id);
    }
    private static void process(String id,Era era,Faction faction,String facility,
                                String output,int quantity,int seconds,int utility,Object... materials){
        Process p=new Process(id,era,faction,facility,cost(materials),output,quantity,seconds,utility);
        if(PROCESSES.putIfAbsent(id,p)!=null)throw new IllegalArgumentException("Duplicate process "+id);
    }
    static {
        // T0 temporary wilderness camp: cost is paid by SurvivalCrafting
        // recipes. This registry describes fixed structures and progression.
        facility("camp-stake",Era.STRANDED,Faction.ALL,"",2,2,5,0,"TIMBER",1);
        facility("claim-heart",Era.ENCAMPMENT,Faction.ALL,"",8,8,900,0,
            "TIMBER",12,"FIELD_STONE",12,"FIBER",6);
        facility("hand-workshop",Era.ENCAMPMENT,Faction.ALL,"claim-heart",6,6,240,0,
            "TIMBER",10,"FIELD_STONE",6,"FIBER",4);
        facility("dry-store",Era.ENCAMPMENT,Faction.ALL,"claim-heart",8,8,360,0,
            "TIMBER",12,"FIELD_STONE",8,"FIBER",6);
        facility("cooking-hearth",Era.ENCAMPMENT,Faction.ALL,"claim-heart",6,6,150,0,
            "TIMBER",10,"FIELD_STONE",12,"FIBER",3);
        facility("rain-catcher",Era.ENCAMPMENT,Faction.ALL,"claim-heart",6,6,180,0,
            "TIMBER",8,"FIELD_STONE",6,"FIBER",8);
        // Break the freight-bay chicken-and-egg requirement. Up to three
        // actual arriving pallets can be deposited here before full freight.
        facility("cargo-laydown",Era.ENCAMPMENT,Faction.ALL,"claim-heart",8,10,450,0,
            "TIMBER",12,"FIELD_STONE",10,"FIBER",6);
        facility("charcoal-kiln",Era.SALVAGE,Faction.ALL,"hand-workshop cooking-hearth",
            8,8,900,0,"TIMBER",14,"FIELD_STONE",16,"FIBER",3);
        facility("stone-furnace",Era.SALVAGE,Faction.ALL,"charcoal-kiln",8,10,1800,0,
            "FIELD_STONE",30,"TIMBER",12,"RAW_ORE",4);
        facility("forge",Era.SALVAGE,Faction.ALL,"stone-furnace cargo-laydown",
            12,12,2400,0,"METAL_STOCK",3,"MACHINE_PARTS",1,
            "FIELD_STONE",20,"RAW_ORE",8);
        facility("freight-bay",Era.SALVAGE,Faction.ALL,"dry-store cargo-laydown hand-workshop",
            16,18,2400,0,"METAL_STOCK",4,"MACHINE_PARTS",3,
            "FIELD_STONE",24,"TIMBER",16);
        facility("steam-boiler",Era.STEAM,Faction.ALL,"forge rain-catcher",
            12,14,3600,0,"METAL_STOCK",4,"MACHINE_PARTS",2,
            "FIELD_STONE",28,"RAW_ORE",8);
        facility("mechanical-shaft",Era.STEAM,Faction.ALL,"steam-boiler forge",
            10,8,1800,0,"METAL_STOCK",2,"MACHINE_PARTS",1,"TIMBER",14);
        facility("metal-press",Era.STEAM,Faction.ALL,"mechanical-shaft",
            10,12,3000,2,"METAL_STOCK",3,"MACHINE_PARTS",2,"FIELD_STONE",16);
        facility("conveyor-line",Era.STEAM,Faction.ALL,"mechanical-shaft freight-bay",
            10,10,3000,2,"METAL_STOCK",2,"MACHINE_PARTS",3,"TIMBER",12);
        facility("generator",Era.ELECTRIC,Faction.ALL,"steam-boiler forge",
            10,10,3600,0,"METAL_STOCK",4,"MACHINE_PARTS",5,"RAW_ORE",10);
        facility("electric-bus",Era.ELECTRIC,Faction.ALL,"generator",
            10,8,2400,3,"METAL_STOCK",3,"MACHINE_PARTS",3,"FIELD_STONE",12);
        facility("precision-lathe",Era.ELECTRIC,Faction.ALL,"electric-bus metal-press",
            12,12,3600,6,"METAL_STOCK",5,"MACHINE_PARTS",4,"RAW_ORE",8);
        facility("automatic-sorter",Era.ELECTRIC,Faction.ALL,"electric-bus conveyor-line",
            8,10,3000,4,"METAL_STOCK",3,"MACHINE_PARTS",5,"FIELD_STONE",12);
        facility("water-works",Era.ELECTRIC,Faction.ALL,"generator rain-catcher",
            10,10,3600,4,"METAL_STOCK",2,"MACHINE_PARTS",2,"FIELD_STONE",18);
        facility("industrial-workshop",Era.AUTOMATION,Faction.ALL,"precision-lathe freight-bay",
            14,14,7200,12,"METAL_STOCK",6,"MACHINE_PARTS",6,"FIELD_STONE",28);
        facility("assembly-line",Era.AUTOMATION,Faction.ALL,"industrial-workshop automatic-sorter",
            18,20,9600,18,"METAL_STOCK",10,"MACHINE_PARTS",9,"FIELD_STONE",35);
        facility("command-hall",Era.AUTOMATION,Faction.ALL,"industrial-workshop dry-store",
            14,16,7200,8,"METAL_STOCK",8,"MACHINE_PARTS",6,"TIMBER",24);
        facility("research-archive",Era.AUTOMATION,Faction.ALL,"command-hall water-works",
            14,16,9000,12,"METAL_STOCK",8,"MACHINE_PARTS",8,"MEDICINAL_HERBS",20);
        facility("infirmary",Era.AUTOMATION,Faction.ALL,"water-works command-hall",
            10,12,6000,8,"METAL_STOCK",2,"MACHINE_PARTS",2,
            "TIMBER",16,"MEDICINAL_HERBS",14);
        facility("signal-relay",Era.AUTOMATION,Faction.ALL,"command-hall generator",
            8,12,5400,6,"METAL_STOCK",3,"MACHINE_PARTS",5,"RAW_ORE",8);
        facility("nutrient-digestor",Era.AUTOMATION,Faction.BEGOTTEN,"water-works industrial-workshop",
            14,14,9000,12,"METAL_STOCK",6,"MACHINE_PARTS",6,"EDIBLES",24);
        facility("terror-collector",Era.AUTOMATION,Faction.PENITENT,"research-archive signal-relay",
            14,14,9000,12,"METAL_STOCK",6,"MACHINE_PARTS",6,"MEDICINAL_HERBS",24);
        facility("resilience-grid",Era.INCURSION,Faction.UNRAVELED,"assembly-line signal-relay",
            22,22,14400,25,"METAL_STOCK",16,"MACHINE_PARTS",12,"FIELD_STONE",60);
        facility("dread-works",Era.INCURSION,Faction.PENITENT,"terror-collector assembly-line",
            22,22,14400,25,"METAL_STOCK",16,"MACHINE_PARTS",12,"FIELD_STONE",60);
        facility("incursion-anchor",Era.INCURSION,Faction.BEGOTTEN,"nutrient-digestor assembly-line",
            22,22,14400,25,"METAL_STOCK",16,"MACHINE_PARTS",12,"FIELD_STONE",60);
        // Shared extreme final recovery monument: identical 40-truck price.
        facility("monument-graveyard",Era.INCURSION,Faction.ALL,
            "research-archive infirmary signal-relay command-hall",
            24,24,28800,30,"METAL_STOCK",24,"MACHINE_PARTS",16,
            "FIELD_STONE",140,"TIMBER",90,"FIBER",60,
            "MEDICINAL_HERBS",30,"RAW_ORE",24);

        // Processing is NEVER automatic just because a descriptor exists.
        // There must be actual staffed, supplied, powered, persisted machines.
        process("split-firewood",Era.ENCAMPMENT,Faction.ALL,"cooking-hearth",
            "FIREWOOD",3,60,0,"TIMBER",2);
        process("boil-water",Era.ENCAMPMENT,Faction.ALL,"cooking-hearth",
            "CLEAN_WATER",1,90,0,"DIRTY_WATER",1,"FIREWOOD",1);
        process("preserve-rations",Era.ENCAMPMENT,Faction.ALL,"cooking-hearth",
            "RATIONS",2,180,0,"EDIBLES",3,"CLEAN_WATER",1);
        process("charcoal-burn",Era.SALVAGE,Faction.ALL,"charcoal-kiln",
            "CHARCOAL",2,420,0,"TIMBER",4,"FIREWOOD",1);
        process("firebrick-batch",Era.SALVAGE,Faction.ALL,"stone-furnace",
            "FIRED_BRICKS",2,300,0,"FIELD_STONE",4,"CHARCOAL",1);
        process("iron-bloom",Era.SALVAGE,Faction.ALL,"forge",
            "IRON_BLOOM",1,600,0,"RAW_ORE",4,"CHARCOAL",3);
        process("steel-billet",Era.SALVAGE,Faction.ALL,"forge",
            "METAL_BILLET",1,420,0,"IRON_BLOOM",2,"CHARCOAL",1);
        process("fasteners",Era.SALVAGE,Faction.ALL,"forge",
            "FASTENERS",4,240,0,"METAL_BILLET",1);
        process("driveshaft",Era.STEAM,Faction.ALL,"metal-press",
            "DRIVE_SHAFT",1,420,2,"METAL_BILLET",3,"FASTENERS",2);
        process("gear-train",Era.STEAM,Faction.ALL,"metal-press",
            "GEAR_ASSEMBLY",1,420,2,"METAL_BILLET",2,"FASTENERS",2);
        process("steam-pump-head",Era.STEAM,Faction.ALL,"metal-press",
            "PUMP_HEAD",1,540,2,"DRIVE_SHAFT",1,"GEAR_ASSEMBLY",1,"MACHINE_PARTS",1);
        process("copper-wire",Era.ELECTRIC,Faction.ALL,"precision-lathe",
            "CONDUCTOR_WIRE",2,300,6,"RAW_ORE",3,"METAL_BILLET",1);
        process("controller-board",Era.ELECTRIC,Faction.ALL,"precision-lathe",
            "CONTROL_BOARD",1,480,6,"CONDUCTOR_WIRE",3,"MACHINE_PARTS",1);
        process("precision-fit",Era.ELECTRIC,Faction.ALL,"precision-lathe",
            "PRECISION_MODULE",1,600,6,"METAL_BILLET",3,"GEAR_ASSEMBLY",2,
            "MACHINE_PARTS",1);
        process("production-controller",Era.AUTOMATION,Faction.ALL,"assembly-line",
            "ASSEMBLY_CONTROLLER",1,900,18,"CONTROL_BOARD",2,
            "PRECISION_MODULE",2,"MACHINE_PARTS",2);
        process("biomass-feed",Era.AUTOMATION,Faction.BEGOTTEN,"nutrient-digestor",
            "NUTRIENT_STOCK",1,900,12,"EDIBLES",5,"CLEAN_WATER",3);
        process("resilience-core",Era.INCURSION,Faction.UNRAVELED,"resilience-grid",
            "RESILIENCE_CORE",1,2400,25,"ASSEMBLY_CONTROLLER",2,
            "METAL_STOCK",2,"MACHINE_PARTS",2);
        process("dread-ward",Era.INCURSION,Faction.PENITENT,"dread-works",
            "DREAD_WARD",1,2400,25,"ASSEMBLY_CONTROLLER",2,
            "DREAD_IMPRINT",3,"METAL_STOCK",2);
        process("breach-seed",Era.INCURSION,Faction.BEGOTTEN,"incursion-anchor",
            "BREACH_SEED",1,3600,25,"ASSEMBLY_CONTROLLER",3,
            "NUTRIENT_STOCK",8,"METAL_STOCK",4,"MACHINE_PARTS",3);
        validate();
    }
    private CraftingEraRegistry(){}
    public static Map<String,Facility> facilities(){return Collections.unmodifiableMap(FACILITIES);}
    public static Map<String,Process> processes(){return Collections.unmodifiableMap(PROCESSES);}
    public static Facility facility(String id){return FACILITIES.get(id);}
    public static Process process(String id){return PROCESSES.get(id);}
    private static void require(boolean truth,String reason){
        if(!truth)throw new IllegalArgumentException(reason);
    }
    public static void validate(){
        for(Facility f:FACILITIES.values()){
            require(f.width()>0&&f.depth()>0&&f.seconds()>0&&f.utilityDemand()>=0,
                "Bad dimensions, time or utility for "+f.id());
            require(!f.materials().isEmpty(),"Free facility "+f.id());
            for(String dependency:f.requires()){
                Facility precursor=FACILITIES.get(dependency);
                require(precursor!=null,"Missing predecessor "+dependency+" for "+f.id());
                require(precursor.era().ordinal()<=f.era().ordinal(),
                    "Chronology reversal "+dependency+" -> "+f.id());
                require(precursor.faction()==Faction.ALL||precursor.faction()==f.faction(),
                    "Faction lock bypass "+f.id());
            }
        }
        Map<String,Integer> colors=new HashMap<>();
        for(String facility:FACILITIES.keySet())dfsFacility(facility,colors);
        // Industrial outputs are produced by distinct, ordered processes;
        // no same-era cycle and no ingredient manufactured in a later era.
        Set<String> available=new HashSet<>(EXTERNAL);
        for(Process p:PROCESSES.values()){
            Facility machine=FACILITIES.get(p.facility());
            require(machine!=null,"Unknown machine "+p.id());
            require(machine.era().ordinal()<=p.era().ordinal(),
                "Recipe precedes station "+p.id());
            require(machine.faction()==Faction.ALL||machine.faction()==p.faction(),
                "Recipe bypasses faction machine "+p.id());
            require(p.quantity()>0&&p.seconds()>0&&p.utilityDemand()>=0,
                "Free/instant output "+p.id());
            require(!available.contains(p.output()),
                "Recipe output duplicates another source/recipe "+p.id());
            for(var ingredient:p.inputs().entrySet())
                require(available.contains(ingredient.getKey())&&ingredient.getValue()>0,
                    "Unobtainable ingredient "+ingredient+" for "+p.id());
            available.add(p.output());
        }
        require(FACILITIES.get("monument-graveyard").materials().get("METAL_STOCK")==24&&
                FACILITIES.get("monument-graveyard").materials().get("MACHINE_PARTS")==16,
            "Monument cost bypass");
    }
    private static void dfsFacility(String name,Map<String,Integer> colors){
        int mark=colors.getOrDefault(name,0);
        require(mark!=1,"Cycle in facilities at "+name);
        if(mark==2)return;
        colors.put(name,1);
        for(String parent:FACILITIES.get(name).requires())dfsFacility(parent,colors);
        colors.put(name,2);
    }
}
