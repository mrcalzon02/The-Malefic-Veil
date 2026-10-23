package veil;

import java.util.*;

/**
 * One canonical, read-only recipe/production dependency index. Derive records
 * from authoritative survival recipes and the planned industry registry:
 * never maintain a duplicate set of material requirements in UI code.
 */
public final class CraftingRecipeIndex {
    public enum Kind { FIELD, INDUSTRIAL, FACILITY }
    public enum Availability { PLAYABLE_HAND, PLAYABLE_FIRE, STATION_UNIMPLEMENTED,
                               INDUSTRIAL_PLANNED, FACILITY_PLANNED }
    public record Record(String key,String name,String output,int quantity,
                         Kind kind,Availability availability,String station,
                         String era,String faction,int seconds,int power,
                         Map<String,Integer> inputs,List<String> prerequisites) {
        public Record {
            inputs=Collections.unmodifiableMap(new TreeMap<>(inputs));
            prerequisites=List.copyOf(prerequisites);
            if(key==null||key.isBlank()||output==null||output.isBlank()||
               quantity<=0||inputs.isEmpty()||station.isBlank())
                throw new IllegalArgumentException("Invalid indexed crafting node");
        }
    }
    private static final List<Record> RECORDS;
    private static final Map<String,Record> BY_KEY;
    private static final Map<String,List<Record>> PRODUCERS;
    private static final Map<String,List<Record>> CONSUMERS;
    private static final Set<String> KNOWN_ITEMS;
    private static String label(String id){
        return id.replace('_',' ').replace('-',' ').toUpperCase(Locale.ROOT);
    }
    static {
        List<Record> rows=new ArrayList<>();
        for(var recipe:SurvivalCrafting.recipes().values()){
            Map<String,Integer> ingredients=new TreeMap<>();
            recipe.requires().forEach((item,amount)->ingredients.put(item.name(),amount));
            rows.add(new Record("hand:"+recipe.id(),label(recipe.id()),
                recipe.result().name(),recipe.quantity(),Kind.FIELD,
                switch(recipe.station()){
                    case HAND->Availability.PLAYABLE_HAND;
                    case FIRE->Availability.PLAYABLE_FIRE;
                    case WORKBENCH->Availability.STATION_UNIMPLEMENTED;
                },recipe.station().name(),"STRANDED","ALL",0,0,ingredients,List.of()));
        }
        for(var job:CraftingEraRegistry.processes().values()){
            rows.add(new Record("process:"+job.id(),label(job.id()),job.output(),
                job.quantity(),Kind.INDUSTRIAL,Availability.INDUSTRIAL_PLANNED,
                job.facility(),job.era().name(),job.faction().name(),
                job.seconds(),job.utilityDemand(),job.inputs(),List.of(job.facility())));
        }
        for(var building:CraftingEraRegistry.facilities().values()){
            rows.add(new Record("facility:"+building.id(),label(building.id()),
                "FACILITY:"+building.id(),1,Kind.FACILITY,
                Availability.FACILITY_PLANNED,"CONSTRUCTION",building.era().name(),
                building.faction().name(),building.seconds(),building.utilityDemand(),
                building.materials(),building.requires()));
        }
        rows.sort(Comparator.comparing((Record r)->r.kind().ordinal())
            .thenComparing(Record::era).thenComparing(Record::key));
        Map<String,Record> keys=new LinkedHashMap<>();
        Map<String,List<Record>> producers=new TreeMap<>();
        Map<String,List<Record>> consumers=new TreeMap<>();
        Set<String> items=new TreeSet<>();
        for(var item:SurvivalCrafting.Item.values())items.add(item.name());
        for(Record row:rows){
            if(keys.putIfAbsent(row.key(),row)!=null)
                throw new IllegalArgumentException("Duplicate indexed key "+row.key());
            producers.computeIfAbsent(row.output(),k->new ArrayList<>()).add(row);
            items.addAll(row.inputs().keySet());
            if(!row.output().startsWith("FACILITY:"))items.add(row.output());
            for(String ingredient:row.inputs().keySet())
                consumers.computeIfAbsent(ingredient,k->new ArrayList<>()).add(row);
            for(String predecessor:row.prerequisites())
                consumers.computeIfAbsent("FACILITY:"+predecessor,
                    k->new ArrayList<>()).add(row);
        }
        RECORDS=List.copyOf(rows);
        BY_KEY=Map.copyOf(keys);
        PRODUCERS=freeze(producers);CONSUMERS=freeze(consumers);
        KNOWN_ITEMS=Set.copyOf(items);
    }
    private static Map<String,List<Record>> freeze(Map<String,List<Record>> map){
        Map<String,List<Record>> immutable=new TreeMap<>();
        map.forEach((key,values)->immutable.put(key,List.copyOf(values)));
        return Collections.unmodifiableMap(immutable);
    }
    private CraftingRecipeIndex(){}
    public static List<Record> recipes(){return RECORDS;}
    public static Record recipe(String key){return BY_KEY.get(key);}
    public static List<Record> makes(String item){return PRODUCERS.getOrDefault(item,List.of());}
    public static List<Record> uses(String item){return CONSUMERS.getOrDefault(item,List.of());}
    public static Set<String> items(){return KNOWN_ITEMS;}
    public static boolean hasRecipe(String item){return !makes(item).isEmpty();}
    /** Plan vs actually callable status is never hidden. */
    public static String availabilityText(Record r){
        return switch(r.availability()){
            case PLAYABLE_HAND->"HOST CRAFT: HAND";
            case PLAYABLE_FIRE->"HOST CRAFT: NEAR CAMPFIRE";
            case STATION_UNIMPLEMENTED->"WORKBENCH EXECUTION NOT WIRED";
            case INDUSTRIAL_PLANNED->"INDUSTRIAL PROCESS: DATA ONLY";
            case FACILITY_PLANNED->"FACILITY PLAN: DATA ONLY";
        };
    }
    /** Named use cases from actual recipe edges, not marketing descriptions. */
    public static String usesText(String item,int maximum){
        List<Record> dependent=uses(item);
        if(dependent.isEmpty())return "NO REGISTERED DOWNSTREAM RECIPE";
        StringJoiner names=new StringJoiner(", ");
        int n=0;
        for(Record r:dependent){
            if(n++>=maximum)break;
            names.add(r.name());
        }
        String tail=dependent.size()>maximum?" +"+(dependent.size()-maximum)+" MORE":"";
        return names+tail;
    }
}
