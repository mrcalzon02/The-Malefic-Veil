package veil;

import java.util.*;
/** E0 is affordable without cargo, charter, powered machines or rare drops. */
public final class EarlyCampCraftingTest {
    private static void check(boolean condition,String reason){
        if(!condition)throw new AssertionError(reason);
    }
    public static void main(String[] args){
        SurvivalCrafting inventory=new SurvivalCrafting();
        Map<String,SurvivalCrafting.Item> starters=Map.of(
            "campfire",SurvivalCrafting.Item.CAMPFIRE,
            "tent",SurvivalCrafting.Item.TENT_KIT,
            "crate",SurvivalCrafting.Item.CRATE_KIT,
            "fence",SurvivalCrafting.Item.FENCE_SECTION,
            "stake-trap",SurvivalCrafting.Item.STAKE_TRAP,
            "noise-alarm",SurvivalCrafting.Item.NOISE_ALARM,
            "workbench",SurvivalCrafting.Item.WORKBENCH);
        EnumMap<SurvivalCrafting.Item,Integer> minimum=new EnumMap<>(SurvivalCrafting.Item.class);
        for(String id:starters.keySet()){
            SurvivalCrafting.Recipe recipe=SurvivalCrafting.recipes().get(id);
            check(recipe!=null,"Missing essential "+id);
            check(recipe.station()==SurvivalCrafting.Station.HAND,
                "Naked character can't craft "+id);
            for(var part:recipe.requires().entrySet()){
                check(part.getKey()!=SurvivalCrafting.Item.MACHINE_PARTS&&
                    part.getKey()!=SurvivalCrafting.Item.METAL_STOCK,
                    "E0 recipe wrongly depends on truck cargo: "+id);
                check(part.getKey()==SurvivalCrafting.Item.TIMBER||
                    part.getKey()==SurvivalCrafting.Item.FIBER||
                    part.getKey()==SurvivalCrafting.Item.FIELD_STONE,
                    "Unharvestable E0 dependency: "+id);
                minimum.merge(part.getKey(),part.getValue(),Integer::sum);
            }
        }
        check(minimum.get(SurvivalCrafting.Item.TIMBER)<=24 &&
              minimum.get(SurvivalCrafting.Item.FIBER)<=14 &&
              minimum.get(SurvivalCrafting.Item.FIELD_STONE)<=8,
              "Starting camp requires too much primitive gathering");
        // One E0 provisioning trip fits completely inside a bounded backpack.
        for(var entry:minimum.entrySet())
            check(inventory.acquire(entry.getKey(),entry.getValue(),false),
                "Couldn't pick up starter ingredient "+entry.getKey());
        check(inventory.mass()<=200,"Start requires impossible backpack weight");
        for(String id:List.of("campfire","tent","crate","fence","stake-trap","noise-alarm","workbench"))
            check(inventory.craft(id,SurvivalCrafting.Station.HAND),
                "Cannot make "+id+" from exact primitive ingredients");
        for(var part:minimum.entrySet())
            check(inventory.count(part.getKey())==0,
                "Consumed materials still carried, creating duplicates");
        for(var part:starters.entrySet())
            check(inventory.count(part.getValue())>=1,
                "Craft didn't produce required kit "+part.getKey());
        check(inventory.count(SurvivalCrafting.Item.FENCE_SECTION)==2,
            "Fence recipe must produce two sections, not one");
        check(!inventory.craft("campfire",SurvivalCrafting.Station.HAND),
            "Free campfire exploit from empty input");
        check(!inventory.craft("vehicle-repair",SurvivalCrafting.Station.HAND),
            "Industrial cargo repair should not be doable by naked hands");
        System.out.println("PASS EarlyCampCraftingTest: accessible 7-item E0 survival pack, no heavy cargo or duplication");
    }
}