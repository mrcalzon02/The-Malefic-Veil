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
    public static final int SCHEMA_VERSION=2;
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
        // Functional coarse/crude settlement index: shapes can use authored
        // primitive meshes until exact PSX replacements pass asset gates.
        facility("field-shelter",Era.ENCAMPMENT,Faction.ALL,"claim-heart",
            6,8,300,0,"TIMBER",10,"FIBER",8,"FIELD_STONE",4);
        facility("forager-garden",Era.ENCAMPMENT,Faction.ALL,"rain-catcher",
            12,12,450,0,"TIMBER",12,"FIBER",6,"FIELD_STONE",6);
        facility("seed-nursery",Era.ENCAMPMENT,Faction.ALL,"forager-garden",
            6,10,600,0,"TIMBER",12,"FIBER",8,"FIELD_STONE",5);
        facility("compost-yard",Era.ENCAMPMENT,Faction.ALL,"forager-garden",
            8,8,420,0,"TIMBER",10,"FIELD_STONE",7,"FIBER",5);
        facility("herb-plot",Era.ENCAMPMENT,Faction.ALL,"forager-garden compost-yard",
            8,12,500,0,"TIMBER",8,"FIBER",6,"FIELD_STONE",5);
        facility("root-cellar",Era.ENCAMPMENT,Faction.ALL,"dry-store",
            8,10,600,0,"TIMBER",18,"FIELD_STONE",12,"FIBER",4);
        facility("smokehouse",Era.ENCAMPMENT,Faction.ALL,"cooking-hearth dry-store",
            8,10,600,0,"TIMBER",16,"FIELD_STONE",10,"FIBER",5);
        facility("fiber-loom",Era.ENCAMPMENT,Faction.ALL,"hand-workshop",
            6,8,420,0,"TIMBER",14,"FIBER",12);
        facility("watch-post",Era.ENCAMPMENT,Faction.ALL,"field-shelter",
            6,8,900,0,"TIMBER",14,"FIELD_STONE",8,"FIBER",6);
        facility("palisade-section",Era.ENCAMPMENT,Faction.ALL,"claim-heart",
            2,8,240,0,"TIMBER",12,"FIELD_STONE",3,"FIBER",3);
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
        facility("grain-mill",Era.SALVAGE,Faction.ALL,"forager-garden hand-workshop",
            8,10,1500,0,"TIMBER",18,"FIELD_STONE",16,"FIBER",6);
        facility("irrigation-works",Era.SALVAGE,Faction.ALL,"forager-garden cargo-laydown",
            10,12,2100,0,"METAL_STOCK",1,"MACHINE_PARTS",1,"FIELD_STONE",16);
        facility("animal-pen",Era.SALVAGE,Faction.ALL,"forager-garden rain-catcher",
            12,14,1800,0,"TIMBER",30,"FIBER",12,"FIELD_STONE",8);
        facility("materials-recycler",Era.SALVAGE,Faction.ALL,"forge freight-bay",
            12,10,2100,0,"METAL_STOCK",2,"MACHINE_PARTS",3,"TIMBER",10);
        facility("wall-gate-system",Era.ENCAMPMENT,Faction.ALL,"claim-heart palisade-section",
            6,10,840,0,"TIMBER",18,"FIELD_STONE",12,"FIBER",8);
        facility("cold-store",Era.SALVAGE,Faction.ALL,"root-cellar freight-bay",
            10,12,2400,0,"TIMBER",22,"FIELD_STONE",18,"METAL_STOCK",2);
        facility("barracks",Era.SALVAGE,Faction.ALL,"watch-post dry-store",
            12,16,2400,0,"TIMBER",28,"FIBER",16,"FIELD_STONE",20,
            "METAL_STOCK",1);
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
        facility("purification-works",Era.ELECTRIC,Faction.ALL,"water-works charcoal-kiln",
            12,10,3600,6,"METAL_STOCK",4,"MACHINE_PARTS",3,"FIELD_STONE",24);
        facility("fuel-refinery",Era.ELECTRIC,Faction.ALL,"water-works precision-lathe",
            14,14,4800,10,"METAL_STOCK",5,"MACHINE_PARTS",6,"RAW_ORE",12);
        facility("controlled-greenhouse",Era.ELECTRIC,Faction.ALL,"forager-garden water-works",
            12,16,3600,8,"METAL_STOCK",3,"MACHINE_PARTS",2,"TIMBER",18);
        facility("industrial-workshop",Era.AUTOMATION,Faction.ALL,"precision-lathe freight-bay",
            14,14,7200,12,"METAL_STOCK",6,"MACHINE_PARTS",6,"FIELD_STONE",28);
        facility("motor-pool",Era.AUTOMATION,Faction.ALL,"industrial-workshop fuel-refinery",
            16,18,7200,12,"METAL_STOCK",6,"MACHINE_PARTS",5,"FIELD_STONE",30);
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
        facility("security-grid",Era.AUTOMATION,Faction.ALL,"signal-relay command-hall",
            16,14,7200,16,"METAL_STOCK",7,"MACHINE_PARTS",6,
            "CONDUCTOR_WIRE",8);
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
        // Parallel faction-owned production spines. ALL facilities below are
        // generic infrastructure, NOT a common faction upgrade path.
        // Unraveled: scavenged metal -> machinery -> powered sensing -> defense.
        facility("unraveled-scrap-outpost",Era.ENCAMPMENT,Faction.UNRAVELED,
            "claim-heart hand-workshop",8,10,720,0,
            "TIMBER",12,"FIELD_STONE",8,"FIBER",4);
        facility("unraveled-machinist-bay",Era.SALVAGE,Faction.UNRAVELED,
            "unraveled-scrap-outpost materials-recycler",10,12,1500,0,
            "METAL_STOCK",2,"MACHINE_PARTS",2,"FIELD_STONE",12);
        facility("unraveled-pressure-shop",Era.STEAM,Faction.UNRAVELED,
            "unraveled-machinist-bay mechanical-shaft",12,12,2400,3,
            "METAL_STOCK",4,"MACHINE_PARTS",3,"RAW_ORE",8);
        facility("unraveled-sensor-lab",Era.ELECTRIC,Faction.UNRAVELED,
            "unraveled-pressure-shop electric-bus",10,12,3600,8,
            "METAL_STOCK",4,"MACHINE_PARTS",5,"CONDUCTOR_WIRE",4);
        facility("unraveled-defense-works",Era.AUTOMATION,Faction.UNRAVELED,
            "unraveled-sensor-lab security-grid",16,16,6000,16,
            "METAL_STOCK",8,"MACHINE_PARTS",8,"CONTROL_BOARD",3);
        // Penitent: rites and incense -> reliquary -> resonant hymn -> wards.
        facility("penitent-votive-altar",Era.ENCAMPMENT,Faction.PENITENT,
            "claim-heart herb-plot",8,8,840,0,
            "TIMBER",10,"FIELD_STONE",10,"FIBER",8);
        facility("penitent-reliquary",Era.SALVAGE,Faction.PENITENT,
            "penitent-votive-altar forge",10,12,2100,0,
            "METAL_STOCK",2,"FIELD_STONE",16,"MEDICINAL_HERBS",10);
        facility("penitent-cantor-dais",Era.STEAM,Faction.PENITENT,
            "penitent-reliquary mechanical-shaft",12,10,2700,3,
            "METAL_STOCK",3,"MACHINE_PARTS",2,"FIELD_STONE",18);
        facility("penitent-choir-relay",Era.ELECTRIC,Faction.PENITENT,
            "penitent-cantor-dais electric-bus",12,12,4200,8,
            "METAL_STOCK",5,"MACHINE_PARTS",4,"MEDICINAL_HERBS",16);
        facility("penitent-ward-sanctum",Era.AUTOMATION,Faction.PENITENT,
            "penitent-choir-relay terror-collector",16,16,7200,16,
            "METAL_STOCK",8,"MACHINE_PARTS",7,"FIELD_STONE",24);
        // Begotten: living feed -> propagules -> chitin -> membranes -> organs.
        facility("begotten-brood-nest",Era.ENCAMPMENT,Faction.BEGOTTEN,
            "claim-heart forager-garden",10,12,960,0,
            "TIMBER",12,"EDIBLES",12,"FIBER",9);
        facility("begotten-spore-basin",Era.SALVAGE,Faction.BEGOTTEN,
            "begotten-brood-nest charcoal-kiln",12,12,1800,0,
            "EDIBLES",18,"MEDICINAL_HERBS",8,"FIELD_STONE",16);
        facility("begotten-chitin-vat",Era.STEAM,Faction.BEGOTTEN,
            "begotten-spore-basin steam-boiler",12,16,3600,3,
            "METAL_STOCK",2,"EDIBLES",25,"FIELD_STONE",18);
        facility("begotten-membrane-lab",Era.ELECTRIC,Faction.BEGOTTEN,
            "begotten-chitin-vat water-works",12,14,4800,8,
            "MACHINE_PARTS",4,"EDIBLES",30,"MEDICINAL_HERBS",12);
        facility("begotten-organ-lattice",Era.AUTOMATION,Faction.BEGOTTEN,
            "begotten-membrane-lab nutrient-digestor",16,20,7200,16,
            "METAL_STOCK",6,"MACHINE_PARTS",6,"EDIBLES",40);
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
        process("compost-ferment",Era.ENCAMPMENT,Faction.ALL,"compost-yard",
            "COMPOST",2,480,0,"EDIBLES",2,"FIBER",2);
        process("grow-field-crops",Era.ENCAMPMENT,Faction.ALL,"forager-garden",
            "FIELD_CROPS",4,3600,0,"EDIBLES",2,"CLEAN_WATER",3,"COMPOST",1);
        process("grow-medicinals",Era.ENCAMPMENT,Faction.ALL,"herb-plot",
            "HERBAL_BUNDLE",2,3600,0,"MEDICINAL_HERBS",1,"CLEAN_WATER",2,"COMPOST",1);
        process("weave-cloth",Era.ENCAMPMENT,Faction.ALL,"fiber-loom",
            "CLOTH_ROLL",2,420,0,"FIBER",4);
        process("smoke-rations",Era.ENCAMPMENT,Faction.ALL,"smokehouse",
            "SMOKED_RATIONS",2,540,0,"EDIBLES",4,"FIREWOOD",2);
        process("cellar-vegetables",Era.ENCAMPMENT,Faction.ALL,"root-cellar",
            "CELLARED_CROPS",2,420,0,"FIELD_CROPS",3,"FIBER",1);
        process("mill-crops",Era.SALVAGE,Faction.ALL,"grain-mill",
            "MEAL_SACK",2,600,0,"FIELD_CROPS",4,"FIELD_STONE",1);
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
        // Supplemental interconnected production chains. These remain
        // industrial DESCRIPTORS until a host-authoritative machine runner
        // debits stock/power and applies real facility access controls.
        process("sort-mineral-feed",Era.SALVAGE,Faction.ALL,"materials-recycler",
            "SORTED_ORE",2,480,0,"RAW_ORE",4,"CHARCOAL",1);
        process("temper-alloy",Era.STEAM,Faction.ALL,"forge",
            "HARDENED_ALLOY",1,780,0,"METAL_BILLET",3,"CHARCOAL",2);
        process("tracking-electronics",Era.ELECTRIC,Faction.ALL,"precision-lathe",
            "TRACKING_CIRCUIT",1,720,6,"CONTROL_BOARD",1,"CONDUCTOR_WIRE",2,
            "SORTED_ORE",1);
        process("precision-feedlines",Era.AUTOMATION,Faction.ALL,"industrial-workshop",
            "PRECISION_FEEDLINE",2,780,12,"DRIVE_SHAFT",1,"CONDUCTOR_WIRE",3);
        process("ballistic-resupply",Era.AUTOMATION,Faction.UNRAVELED,"industrial-workshop",
            "BALLISTIC_SUPPLY",2,960,12,"HARDENED_ALLOY",2,"FASTENERS",4,
            "PRECISION_FEEDLINE",1,"TRACKING_CIRCUIT",1);
        process("votive-incense",Era.ENCAMPMENT,Faction.PENITENT,"cooking-hearth",
            "VOTIVE_INCENSE",2,300,0,"MEDICINAL_HERBS",2,"FIREWOOD",2);
        process("witness-seal",Era.SALVAGE,Faction.PENITENT,"forge",
            "WITNESS_SEAL",2,840,0,"METAL_BILLET",1,"VOTIVE_INCENSE",2);
        process("chorus-resonance",Era.AUTOMATION,Faction.PENITENT,"research-archive",
            "CHORUS_RESONANCE",1,1200,12,"WITNESS_SEAL",2,"CONTROL_BOARD",1);
        process("bound-liturgy",Era.AUTOMATION,Faction.PENITENT,"terror-collector",
            "BOUND_LITURGY",1,1440,12,"CHORUS_RESONANCE",1,"DREAD_IMPRINT",1);
        process("sacred-supply",Era.INCURSION,Faction.PENITENT,"dread-works",
            "SACRED_SUPPLY",1,2400,25,"BOUND_LITURGY",2,"DREAD_WARD",1);
        process("growth-feed",Era.ENCAMPMENT,Faction.BEGOTTEN,"cooking-hearth",
            "GROWTH_FEED",2,360,0,"EDIBLES",3,"CLEAN_WATER",1);
        process("viable-spores",Era.SALVAGE,Faction.BEGOTTEN,"materials-recycler",
            "VIABLE_SPORES",2,900,0,"GROWTH_FEED",2,"MEDICINAL_HERBS",2);
        process("grown-chitin",Era.ELECTRIC,Faction.BEGOTTEN,"controlled-greenhouse",
            "GROWN_CHITIN",2,1440,8,"VIABLE_SPORES",2,"FIELD_CROPS",3);
        process("binding-resin",Era.AUTOMATION,Faction.BEGOTTEN,"nutrient-digestor",
            "BINDING_RESIN",1,1260,12,"GROWN_CHITIN",2,"NUTRIENT_STOCK",1);
        process("spine-supply",Era.INCURSION,Faction.BEGOTTEN,"incursion-anchor",
            "SPINE_SUPPLY",1,2400,25,"BINDING_RESIN",2,"BREACH_SEED",1);
        process("field-maintenance",Era.AUTOMATION,Faction.UNRAVELED,"motor-pool",
            "VEHICLE_REPAIR_KIT",1,900,12,"HARDENED_ALLOY",1,
            "FASTENERS",3,"MACHINE_PARTS",2);
        // Five former "legacy-only" construction entries now have real,
        // costed/power-gated definitions and meaningful downstream workflows.
        process("cold-preservation",Era.SALVAGE,Faction.ALL,"cold-store",
            "PRESERVED_RATIONS",3,900,0,"SMOKED_RATIONS",2,
            "CLEAN_WATER",1,"CLOTH_ROLL",1);
        process("guard-provisions",Era.SALVAGE,Faction.ALL,"barracks",
            "GUARD_PROVISIONS",2,960,0,"RATIONS",3,"CLOTH_ROLL",1);
        process("treat-contaminated-water",Era.ELECTRIC,Faction.ALL,"purification-works",
            "STERILE_WATER",4,780,6,"DIRTY_WATER",6,"CHARCOAL",2);
        process("secure-ammo-cache",Era.AUTOMATION,Faction.UNRAVELED,"security-grid",
            "DEFENSE_CACHE",2,1440,16,"BALLISTIC_SUPPLY",2,
            "TRACKING_CIRCUIT",1);
        process("fortified-rations",Era.AUTOMATION,Faction.ALL,"industrial-workshop",
            "FIELD_MRE",3,1200,12,"PRESERVED_RATIONS",2,"GUARD_PROVISIONS",2);
        // Those three spines are materially separate at every era, with
        // distinct stations and icon-backed outputs, not re-skinned clones.
        process("unraveled-recover-scrap",Era.ENCAMPMENT,Faction.UNRAVELED,
            "unraveled-scrap-outpost","SCRAP_METAL",2,300,0,
            "RAW_ORE",2,"FIREWOOD",1);
        process("unraveled-make-bearings",Era.SALVAGE,Faction.UNRAVELED,
            "unraveled-machinist-bay","BEARING_SET",2,720,0,
            "SCRAP_METAL",3,"FASTENERS",1);
        process("unraveled-pressure-vessel",Era.STEAM,Faction.UNRAVELED,
            "unraveled-pressure-shop","PRESSURE_TANK",1,780,3,
            "METAL_BILLET",2,"BEARING_SET",2);
        process("unraveled-calibrate-sensors",Era.ELECTRIC,Faction.UNRAVELED,
            "unraveled-sensor-lab","SENSOR_HEAD",2,900,8,
            "CONTROL_BOARD",2,"CONDUCTOR_WIRE",3,"PRESSURE_TANK",1);
        process("unraveled-defense-scanner",Era.AUTOMATION,Faction.UNRAVELED,
            "unraveled-defense-works","RELAY_BOX",1,1500,16,
            "SENSOR_HEAD",2,"TRACKING_CIRCUIT",1,"MACHINE_PARTS",3);

        process("penitent-weave-ritual",Era.ENCAMPMENT,Faction.PENITENT,
            "penitent-votive-altar","RITUAL_CLOTH",2,360,0,
            "CLOTH_ROLL",1,"MEDICINAL_HERBS",1);
        process("penitent-engrave-reliquary",Era.SALVAGE,Faction.PENITENT,
            "penitent-reliquary","RELIQUARY_PLATE",1,840,0,
            "METAL_BILLET",2,"RITUAL_CLOTH",1);
        process("penitent-cast-censer",Era.STEAM,Faction.PENITENT,
            "penitent-cantor-dais","BRONZE_CENSER",1,900,3,
            "RELIQUARY_PLATE",2,"WITNESS_SEAL",1);
        process("penitent-score-hymn",Era.ELECTRIC,Faction.PENITENT,
            "penitent-choir-relay","CHOIR_SCORE",1,1260,8,
            "BRONZE_CENSER",1,"CONTROL_BOARD",1);
        process("penitent-sanctify-shell",Era.AUTOMATION,Faction.PENITENT,
            "penitent-ward-sanctum","CONSECRATED_SHELL",1,1800,16,
            "CHOIR_SCORE",2,"DREAD_IMPRINT",3,"BOUND_LITURGY",1);

        process("begotten-mix-slurry",Era.ENCAMPMENT,Faction.BEGOTTEN,
            "begotten-brood-nest","NUTRIENT_SLURRY",2,420,0,
            "EDIBLES",4,"DIRTY_WATER",2);
        process("begotten-culture-spores",Era.SALVAGE,Faction.BEGOTTEN,
            "begotten-spore-basin","SPORE_POD",2,960,0,
            "NUTRIENT_SLURRY",2,"MEDICINAL_HERBS",2);
        process("begotten-harden-chitin",Era.STEAM,Faction.BEGOTTEN,
            "begotten-chitin-vat","CHITIN_SHARDS",2,960,3,
            "SPORE_POD",2,"RAW_ORE",1);
        process("begotten-grow-membrane",Era.ELECTRIC,Faction.BEGOTTEN,
            "begotten-membrane-lab","LIVING_MEMBRANE",1,1440,8,
            "CHITIN_SHARDS",3,"CLEAN_WATER",2);
        process("begotten-pulse-organ",Era.AUTOMATION,Faction.BEGOTTEN,
            "begotten-organ-lattice","ORGAN_CORE",1,2100,16,
            "LIVING_MEMBRANE",2,"NUTRIENT_STOCK",2,"GROWTH_FEED",2);
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
        validateFacilitySupplyGraph(FACILITIES,PROCESSES);
        // Industrial outputs are produced by distinct, ordered processes;
        // no same-era cycle and no ingredient manufactured in a later era.
        Set<String> available=new HashSet<>(EXTERNAL);
        Map<String,Faction> outputOwnership=new HashMap<>();
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
            for(var ingredient:p.inputs().entrySet()){
                require(available.contains(ingredient.getKey())&&ingredient.getValue()>0,
                    "Unobtainable ingredient "+ingredient+" for "+p.id());
                Faction owner=outputOwnership.getOrDefault(ingredient.getKey(),Faction.ALL);
                require(owner==Faction.ALL||owner==p.faction(),
                    "Uncontracted foreign-faction manufacturing input "+
                        ingredient.getKey()+" ("+owner+") in "+p.id()+
                        " ("+p.faction()+")");
            }
            outputOwnership.put(p.output(),p.faction());
            available.add(p.output());
        }
        require(FACILITIES.get("monument-graveyard").materials().get("METAL_STOCK")==24&&
                FACILITIES.get("monument-graveyard").materials().get("MACHINE_PARTS")==16,
            "Monument cost bypass");
    }
    /**
     * A construction dependency may come from a manufactured ingredient as
     * well as a listed parent facility. Check BOTH edges or a factory could
     * demand its own output before it can be built. Hand-crafted alternatives
     * remain valid independent sources, not fictional industrial shortcuts.
     */
    static void validateFacilitySupplyGraph(Map<String,Facility> facilities,
                                             Map<String,Process> processes){
        Map<String,Process> manufactured=new HashMap<>();
        for(Process p:processes.values()){
            if(manufactured.putIfAbsent(p.output(),p)!=null)
                throw new IllegalArgumentException("Duplicate industrial output "+p.output());
        }
        Set<String> handProduced=new HashSet<>();
        for(var hand:SurvivalCrafting.recipes().values())
            handProduced.add(hand.result().name());
        Map<String,Set<String>> dependencies=new HashMap<>();
        for(Facility facility:facilities.values()){
            Set<String> parents=new LinkedHashSet<>(facility.requires());
            for(String material:facility.materials().keySet()){
                if(EXTERNAL.contains(material)||handProduced.contains(material))continue;
                Process source=manufactured.get(material);
                require(source!=null,
                    "Unobtainable construction ingredient "+material+" for "+facility.id());
                require(source.era().ordinal()<=facility.era().ordinal(),
                    "Construction requires later-era product "+material+" for "+facility.id());
                require(source.faction()==Faction.ALL||source.faction()==facility.faction(),
                    "Uncontracted foreign-faction construction input "+material+
                    " for "+facility.id());
                require(facilities.containsKey(source.facility()),
                    "Construction material has missing producer facility "+source.facility());
                parents.add(source.facility());
            }
            dependencies.put(facility.id(),Set.copyOf(parents));
        }
        Map<String,Integer> colors=new HashMap<>();
        for(String id:dependencies.keySet())
            dfsSupply(id,dependencies,colors);
    }
    private static void dfsSupply(String id,Map<String,Set<String>> deps,
                                  Map<String,Integer> colors){
        int mark=colors.getOrDefault(id,0);
        require(mark!=1,"Facility/material production cycle at "+id);
        if(mark==2)return;
        require(deps.containsKey(id),"Missing construction parent "+id);
        colors.put(id,1);
        for(String parent:deps.get(id))dfsSupply(parent,deps,colors);
        colors.put(id,2);
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
