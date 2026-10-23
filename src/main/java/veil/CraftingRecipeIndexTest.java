package veil;
import java.util.*;

/** End-to-end contract: in-game book cannot invent requirements, unlocks, or yield. */
public final class CraftingRecipeIndexTest {
    public static void main(String[] args){
        var records=CraftingRecipeIndex.recipes();
        int expected=SurvivalCrafting.recipes().size()+
            CraftingEraRegistry.processes().size()+CraftingEraRegistry.facilities().size();
        if(records.size()!=expected)throw new AssertionError("Missing indexed recipe/structure");
        if(SurvivalCrafting.recipes().size()<22||CraftingEraRegistry.processes().size()<41)
            throw new AssertionError("Crafting expansion truncated");
        for(var original:SurvivalCrafting.recipes().values()){
            var index=CraftingRecipeIndex.recipe("hand:"+original.id());
            if(index==null||index.quantity()!=original.quantity()||
               index.inputs().size()!=original.requires().size())
                throw new AssertionError("Hand recipe drift: "+original.id());
            for(var item:original.requires().entrySet())
                if(!Objects.equals(item.getValue(),index.inputs().get(item.getKey().name())))
                    throw new AssertionError("Cost drift: "+original.id());
            if(index.availability()==CraftingRecipeIndex.Availability.INDUSTRIAL_PLANNED)
                throw new AssertionError("Playable craft wrongly labeled planned");
        }
        for(var industrial:CraftingEraRegistry.processes().values()){
            var index=CraftingRecipeIndex.recipe("process:"+industrial.id());
            if(index==null||!index.inputs().equals(industrial.inputs())||
               !index.station().equals(industrial.facility())||
               index.availability()!=CraftingRecipeIndex.Availability.INDUSTRIAL_PLANNED)
                throw new AssertionError("Process display invented execution: "+industrial.id());
        }
        for(var facility:CraftingEraRegistry.facilities().values()){
            var indexed=CraftingRecipeIndex.recipe("facility:"+facility.id());
            if(indexed==null||!indexed.inputs().equals(facility.materials())||
               !indexed.prerequisites().equals(facility.requires()))
                throw new AssertionError("Facility requirements drift: "+facility.id());
        }
        if(!CraftingRecipeIndex.uses("TIMBER").stream().anyMatch(r->
            r.key().equals("facility:claim-heart")))
            throw new AssertionError("Facility construction not in ingredient uses");
        if(!CraftingRecipeIndex.uses("FACILITY:claim-heart").stream().anyMatch(r->
            r.key().equals("facility:hand-workshop")))
            throw new AssertionError("Facility prerequisites not in reverse dependency chain");
        if(CraftingRecipeIndex.makes("FIELD_STONE").size()!=0)
            throw new AssertionError("Harvestable material acquired free craft");
        for(String required:List.of("SORTED_ORE","BALLISTIC_SUPPLY","SACRED_SUPPLY",
                "SPINE_SUPPLY","BANDAGE","CLEAN_WATER"))
            if(!CraftingRecipeIndex.hasRecipe(required))
                throw new AssertionError("Missing expanded product "+required);
        System.out.println("PASS CraftingRecipeIndexTest recipes="+records.size()+
            " hand="+SurvivalCrafting.recipes().size()+
            " processes="+CraftingEraRegistry.processes().size()+
            " facilities="+CraftingEraRegistry.facilities().size()+
            " itemIds="+CraftingRecipeIndex.items().size()+
            " reverseDependencies=verified");
    }
}
