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
        METAL_STOCK, MACHINE_PARTS, SHELTER_KIT, VEHICLE_REPAIR_KIT
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
        register(r,"boil-water",Station.FIRE,Map.of(Item.DIRTY_WATER,1),
            Item.CLEAN_WATER,1);
        register(r,"workbench",Station.HAND,Map.of(Item.TIMBER,6,
            Item.METAL_STOCK,1),Item.WORKBENCH,1);
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
    public synchronized int count(Item kind){return items.getOrDefault(kind,0);}
    private static int weight(Item item){return switch(item){
        case METAL_STOCK,MACHINE_PARTS,WORKBENCH,SHELTER_KIT -> 25;
        case FIELD_STONE,RAW_ORE,CAMPFIRE -> 4;
        default -> 1;
    };}
    public synchronized int mass(){
        int total=0;for(var item:items.entrySet())total+=item.getValue()*weight(item.getKey());
        return total;
    }
    /** Bulk cargo can enter this inventory only after legitimate base unloading. */
    public synchronized boolean acquire(Item kind,int amount,boolean atBase){
        if(kind==null||amount<=0||amount>1000)return false;
        if((kind==Item.METAL_STOCK||kind==Item.MACHINE_PARTS)&&!atBase)return false;
        if((long)mass()+(long)weight(kind)*amount>200)return false;
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
