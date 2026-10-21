package veil;

import java.util.*;
/** Structural data checks: no skipping stage, recipe cycle, missing cargo or faction loophole. */
public final class CraftingEraRegistryTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        CraftingEraRegistry.validate();
        var buildings=CraftingEraRegistry.facilities();
        var recipes=CraftingEraRegistry.processes();
        check(CraftingEraRegistry.SCHEMA_VERSION==2,"Registry expansion must be versioned");
        check(buildings.size()>=28&&recipes.size()>=18,
            "Incomplete industrial and faction registry");
        EnumSet<CraftingEraRegistry.Era> eras=EnumSet.noneOf(CraftingEraRegistry.Era.class);
        for(var building:buildings.values()){
            eras.add(building.era());
            check(building.width()>=2&&building.depth()>=2,"Invisible facility");
            check(building.seconds()>0,"Instant industrial construction");
            check(building.materials().values().stream().allMatch(n->n>0),"Free materials");
        }
        check(eras.size()==CraftingEraRegistry.Era.values().length,
            "Lost era in full progression");
        var claim=buildings.get("claim-heart");
        check(!claim.materials().containsKey("METAL_STOCK")&&
            !claim.materials().containsKey("MACHINE_PARTS"),
            "First base cannot be founded without preexisting truck cargo");
        var laydown=buildings.get("cargo-laydown");
        check(laydown.era()==CraftingEraRegistry.Era.ENCAMPMENT&&
            !laydown.materials().containsKey("METAL_STOCK")&&
            !laydown.materials().containsKey("MACHINE_PARTS"),
            "Freight bay deadlock: provisional pallet laydown needs bulk freight");
        var freight=buildings.get("freight-bay");
        check(freight.requires().contains("cargo-laydown"),"Freight loop dependency bypass");
        var grave=buildings.get("monument-graveyard");
        check(grave.era()==CraftingEraRegistry.Era.INCURSION&&
            grave.width()>=24&&grave.depth()>=24,
            "Monument shrink exploit");
        check(grave.materials().get("METAL_STOCK")+
            grave.materials().get("MACHINE_PARTS")==40,
            "Graveyard recovery became cheaply craftable");
        for(var id:List.of("camp-stake","claim-heart","hand-workshop","dry-store",
            "cargo-laydown")){
            var stage=buildings.get(id);
            check(stage!=null&&stage.era().ordinal()<=CraftingEraRegistry.Era.ENCAMPMENT.ordinal(),
                "Early survival blocked behind power age: "+id);
        }
        int industrialPowerFacilities=0;
        for(var f:buildings.values()){
            if(f.era().ordinal()>=CraftingEraRegistry.Era.ELECTRIC.ordinal()&&
               f.utilityDemand()>0)industrialPowerFacilities++;
        }
        check(industrialPowerFacilities>=10,"Endgame factories run for free");
        var finalNames=List.of("resilience-grid","dread-works","incursion-anchor");
        Set<CraftingEraRegistry.Faction> parties=EnumSet.noneOf(CraftingEraRegistry.Faction.class);
        for(String id:finalNames){
            var facility=buildings.get(id);
            check(facility!=null&&facility.era()==CraftingEraRegistry.Era.INCURSION,
                "Faction final site incorrectly gated: "+id);
            check(facility.materials().get("METAL_STOCK")>=12&&
                facility.materials().get("MACHINE_PARTS")>=10,
                "Monster/religious endgame cargo-free loophole "+id);
            parties.add(facility.faction());
        }
        check(parties.containsAll(Set.of(CraftingEraRegistry.Faction.UNRAVELED,
            CraftingEraRegistry.Faction.PENITENT,CraftingEraRegistry.Faction.BEGOTTEN)),
            "Faction industry divergence incomplete");
        for(var entry:recipes.entrySet()){
            var recipe=entry.getValue();
            check(recipe.seconds()>0&&recipe.quantity()>0&&
                !recipe.inputs().isEmpty(),"Unbounded free recipe "+entry.getKey());
            check(recipe.era().ordinal()>=buildings.get(recipe.facility()).era().ordinal(),
                "Output precedes its own machine");
        }
        check(recipes.get("dread-ward").inputs().containsKey("DREAD_IMPRINT"),
            "Penitent power must require verified terror encounters");
        check(recipes.get("breach-seed").inputs().containsKey("NUTRIENT_STOCK"),
            "Begotten reality breach bypasses finite organic feed");
        check(recipes.get("resilience-core").inputs().containsKey("MACHINE_PARTS"),
            "Unraveled automation bypasses salvage resource logistics");
        for(String id:List.of("field-shelter","forager-garden","seed-nursery",
            "compost-yard","herb-plot","root-cellar","smokehouse","fiber-loom",
            "grain-mill","irrigation-works","animal-pen","materials-recycler",
            "controlled-greenhouse","fuel-refinery","motor-pool")){
            check(buildings.containsKey(id),"Crude-to-industrial facility not indexed "+id);
        }
        for(String id:List.of("compost-ferment","grow-field-crops",
            "grow-medicinals","weave-cloth","smoke-rations",
            "cellar-vegetables","mill-crops")){
            check(recipes.containsKey(id),"Missing farming/food throughput plan "+id);
            check(recipes.get(id).seconds() > 0, "nonpositive process time");
        }
        System.out.println("PASS CraftingEraRegistryTest: "+buildings.size()+
            " facilities "+recipes.size()+" processes "+eras.size()+
            " eras, separate all-faction logistics and guarded three-branch endgame");
    }
}