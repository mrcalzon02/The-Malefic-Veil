package veil;
import java.util.*;

/** End-to-end contract: in-game book cannot invent requirements, unlocks, or yield. */
public final class CraftingRecipeIndexTest {
    public static void main(String[] args){
        var records=CraftingRecipeIndex.recipes();
        int expected=SurvivalCrafting.recipes().size()+ThrownDeployables.catalog().size()+
            CraftingEraRegistry.processes().size()+CraftingEraRegistry.facilities().size();
        if(records.size()!=expected)throw new AssertionError("Missing indexed recipe/structure");
        for(var kit:ThrownDeployables.catalog()){
            var indexed=CraftingRecipeIndex.recipe("throw:"+kit.id());
            if(indexed==null||!indexed.output().equals(kit.item().name())||
               !indexed.faction().equals(kit.faction().name()))
                throw new AssertionError("Faction throwable lost: "+kit.id());
        }
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
        for(var faction:CraftingEraRegistry.Faction.values()){
            var scoped=CraftingRecipeIndex.recipesFor(faction);
            if(scoped.isEmpty())throw new AssertionError("Empty recipe catalogue "+faction);
            for(var row:scoped)
                if(!CraftingRecipeIndex.belongsTo(row,faction))
                    throw new AssertionError("Foreign economy in faction catalogue: "+row.key());
        }
        if(CraftingRecipeIndex.recipesFor(CraftingEraRegistry.Faction.UNRAVELED).stream()
            .anyMatch(row->row.key().equals("process:bound-liturgy"))||
           CraftingRecipeIndex.recipesFor(CraftingEraRegistry.Faction.BEGOTTEN).stream()
            .anyMatch(row->row.key().equals("process:ballistic-resupply"))||
           CraftingRecipeIndex.recipesFor(CraftingEraRegistry.Faction.PENITENT).stream()
            .anyMatch(row->row.key().equals("process:spine-supply")))
            throw new AssertionError("Faction industrial recipes were pooled");
        if(!CraftingRecipeIndex.makes("BOUND_LITURGY",
                CraftingEraRegistry.Faction.UNRAVELED).isEmpty()||
           !CraftingRecipeIndex.uses("FACILITY:terror-collector",
                CraftingEraRegistry.Faction.UNRAVELED).isEmpty()||
           CraftingRecipeIndex.visibleItem("SACRED_SUPPLY",
                CraftingEraRegistry.Faction.BEGOTTEN))
            throw new AssertionError("Foreign economy dependencies leaked into item book");
        if(!CraftingRecipeIndex.recipesFor(CraftingEraRegistry.Faction.ALL)
                .stream().allMatch(row->row.faction().equals("ALL")))
            throw new AssertionError("Neutral faction sees foreign processes");
        System.out.println("PASS CraftingRecipeIndexTest recipes="+records.size()+
            " hand="+SurvivalCrafting.recipes().size()+
            " processes="+CraftingEraRegistry.processes().size()+
            " facilities="+CraftingEraRegistry.facilities().size()+
            " itemIds="+CraftingRecipeIndex.items().size()+
            " reverseDependencies=verified");
    }
}
