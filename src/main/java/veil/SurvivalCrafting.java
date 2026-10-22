package veil;

import java.util.*;

/**
 * Minimal survival and crafting economy. No free resources or client-side
 * crafting authority: recipe validation, ingredient debit and result minting
 * occur together on the host thread. Inventory weight prevents bottomless loot.
 */
public final class SurvivalCrafting {
    public enum Item {
        TIMBER, FIELD_STONE, FIBER, EDIBLES, MEDICINAL_HERBS, RAW_ORE,
        DIRTY_WATER, CLEAN_WATER, BANDAGE, CAMPFIRE, FILTER, WORKBENCH,
        METAL_STOCK, MACHINE_PARTS, SHELTER_KIT, VEHICLE_REPAIR_KIT,
        TENT_KIT, CRATE_KIT, FENCE_SECTION, STAKE_TRAP, NOISE_ALARM
    }
    public enum Station { HAND, FIRE, WORKBENCH }
    public record Recipe(String id,Station station,Map<Item,Integer> requires,
                         Item result,int quantity){}
    private static final Map<String,Recipe> RECIPES;
    static {
        Map<String,Recipe> r=new LinkedHashMap<>();
        register(r,"bandage",Station.HAND,Map.of(Item.FIBER,2,
            Item.MEDICINAL_HERBS,1),Item.BANDAGE,1);
        register(r,"campfire",Station.HAND,Map.of(Item.TIMBER,3,
            Item.FIELD_STONE,2),Item.CAMPFIRE,1);
        register(r,"water-filter",Station.HAND,Map.of(Item.FIBER,3,
            Item.FIELD_STONE,2),Item.FILTER,1);
        // Accessible no-claim wilderness start: these recipes cannot need
        // truck cargo or an already completed settlement.
        register(r,"tent",Station.HAND,Map.of(Item.FIBER,4,
            Item.TIMBER,2),Item.TENT_KIT,1);
        register(r,"crate",Station.HAND,Map.of(Item.TIMBER,5,
            Item.FIBER,2),Item.CRATE_KIT,1);
        register(r,"fence",Station.HAND,Map.of(Item.TIMBER,3,
            Item.FIBER,1),Item.FENCE_SECTION,2);
        register(r,"stake-trap",Station.HAND,Map.of(Item.TIMBER,2,
            Item.FIBER,1),Item.STAKE_TRAP,1);
        register(r,"noise-alarm",Station.HAND,Map.of(Item.TIMBER,1,
            Item.FIBER,2),Item.NOISE_ALARM,1);
        register(r,"boil-water",Station.FIRE,Map.of(Item.DIRTY_WATER,1),
            Item.CLEAN_WATER,1);
        register(r,"workbench",Station.HAND,Map.of(Item.TIMBER,6,
            Item.FIELD_STONE,4,Item.FIBER,2),Item.WORKBENCH,1);
        register(r,"shelter-kit",Station.WORKBENCH,Map.of(Item.TIMBER,8,
            Item.FIBER,5,Item.METAL_STOCK,1),Item.SHELTER_KIT,1);
        register(r,"vehicle-repair",Station.WORKBENCH,Map.of(Item.METAL_STOCK,1,
            Item.MACHINE_PARTS,1),Item.VEHICLE_REPAIR_KIT,1);
        RECIPES=Map.copyOf(r);
    }
    private static void register(Map<String,Recipe> out,String name,Station station,
                                 Map<Item,Integer> requires,Item result,int amount){
        out.put(name,new Recipe(name,station,Map.copyOf(requires),result,amount));
    }
    public static Map<String,Recipe> recipes(){return RECIPES;}
    private final EnumMap<Item,Integer> items=new EnumMap<>(Item.class);
    private int hunger=100,hydration=100;
    private long clock;
    public record Status(int hunger,int hydration,Map<Item,Integer> inventory){}
    public synchronized Status status(){
        return new Status(hunger,hydration,Map.copyOf(items));
    }
    /** Server death rule: every physically carried survival item is gone. */
    public synchronized void clearOnDeath(){items.clear();}
    /** Solo client and persistence may inspect, but cannot mint consumed items. */
    public synchronized Map<Item,Integer> inventory(){return Map.copyOf(items);}
    public synchronized int count(Item kind){return items.getOrDefault(kind,0);}
    private static int weight(Item item){return switch(item){
        case METAL_STOCK,MACHINE_PARTS,WORKBENCH,SHELTER_KIT -> 25;
        case FIELD_STONE,RAW_ORE,CAMPFIRE,CRATE_KIT -> 4;
        case TENT_KIT -> 3;
        case FENCE_SECTION,STAKE_TRAP -> 2;
        default -> 1;
    };}
    public synchronized int mass(){
        int total=0;for(var item:items.entrySet())total+=item.getValue()*weight(item.getKey());
        return total;
    }
    /** Host preflight allows harvesting to debit the node only if cargo will fit. */
    public synchronized boolean canAcquire(Item kind,int amount,boolean atBase){
        if(kind==null||amount<=0||amount>1000)return false;
        if((kind==Item.METAL_STOCK||kind==Item.MACHINE_PARTS)&&!atBase)return false;
        return (long)mass()+(long)weight(kind)*amount<=200;
    }
    /** Bulk cargo can enter this inventory only after legitimate base unloading. */
    public synchronized boolean acquire(Item kind,int amount,boolean atBase){
        if(!canAcquire(kind,amount,atBase))return false;
        items.merge(kind,amount,Integer::sum);return true;
    }
    public synchronized boolean craft(String recipeId,Station station){
        Recipe recipe=RECIPES.get(recipeId);
        if(recipe==null||station==null||station.ordinal()<recipe.station().ordinal())return false;
        for(var part:recipe.requires().entrySet())
            if(count(part.getKey())<part.getValue())return false;
        int newMass=mass()+weight(recipe.result())*recipe.quantity();
        for(var part:recipe.requires().entrySet())
            newMass-=weight(part.getKey())*part.getValue();
        if(newMass>200)return false;
        for(var part:recipe.requires().entrySet())
            items.put(part.getKey(),count(part.getKey())-part.getValue());
        items.merge(recipe.result(),recipe.quantity(),Integer::sum);
        return true;
    }
    /** Host-authorized structural material debit. Never applies food effects. */
    public synchronized boolean debit(Item item,int quantity){
        if(item==null||quantity<1||quantity>1000||count(item)<quantity)return false;
        items.put(item,count(item)-quantity);
        return true;
    }
    /** Physical group handoff: weight and available stock checked atomically. */
    public synchronized boolean transferTo(SurvivalCrafting recipient,Item item,int quantity){
        if(recipient==null||recipient==this||item==null||quantity<1||quantity>1000||
           count(item)<quantity)return false;
        synchronized(recipient){
            if(!recipient.canAcquire(item,quantity,true))return false;
            items.put(item,count(item)-quantity);
            if(!recipient.acquire(item,quantity,true))
                throw new IllegalStateException("Resource transfer changed after preflight");
        }
        return true;
    }
    public synchronized boolean consume(Item item){
        if(count(item)==0)return false;
        if(item==Item.EDIBLES){hunger=Math.min(100,hunger+30);}
        else if(item==Item.CLEAN_WATER){hydration=Math.min(100,hydration+40);}
        else return false;
        items.put(item,count(item)-1);return true;
    }
    /** Server-only 1-minute survival cadence; never run per render frame. */
    public synchronized void advance(long elapsedMs){
        if(elapsedMs<0||elapsedMs>60_000)throw new IllegalArgumentException("Bad survival tick");
        long old=clock;clock+=elapsedMs;
        long minutes=clock/60_000-old/60_000;
        hunger=Math.max(0,hunger-(int)minutes);
        hydration=Math.max(0,hydration-(int)(2*minutes));
    }
}
