package veil;

import java.util.*;

/**
 * Three sovereign economies, not cosmetic variants of one manufacturing tree.
 * Shared salvage inputs are permitted; faction-owned machines and intermediate
 * products cannot appear in another faction's executable/visible catalogue.
 */
public final class FactionIndustrySeparationTest {
    private static void check(boolean ok,String error){
        if(!ok)throw new AssertionError(error);
    }
    private static void checkSpine(CraftingEraRegistry.Faction faction,
                                   String[] facilities,String[] steps){
        check(facilities.length==5&&steps.length==5,"Incomplete faction production spine");
        Set<CraftingEraRegistry.Era> stages=EnumSet.noneOf(CraftingEraRegistry.Era.class);
        for(int i=0;i<5;i++){
            final String stageFacility=facilities[i],stageRecipe=steps[i];
            var building=CraftingEraRegistry.facility(facilities[i]);
            var process=CraftingEraRegistry.process(steps[i]);
            check(building!=null&&process!=null,"Missing faction stage "+facilities[i]);
            check(building.faction()==faction&&process.faction()==faction,
                "Wrong faction owner of "+facilities[i]);
            check(process.facility().equals(building.id()),
                "Process not attached to its own faction machine: "+steps[i]);
            check(building.era()==process.era(),
                "Production does not align with its stage "+steps[i]);
            stages.add(building.era());
            check(CraftingRecipeIndex.recipesFor(faction).stream().anyMatch(r->
                r.key().equals("process:"+stageRecipe)),"Own recipe is missing from "+faction);
            for(var other:CraftingEraRegistry.Faction.values()){
                if(other==faction)continue;
                check(CraftingRecipeIndex.recipesFor(other).stream().noneMatch(r->
                    r.key().equals("process:"+steps[i])||
                    r.key().equals("facility:"+stageFacility)),
                    "Leaked faction chain "+facilities[i]+" into "+other);
                check(CraftingRecipeIndex.makes(process.output(),other).isEmpty(),
                    "Foreign output became craftable for "+other);
                check(!CraftingRecipeIndex.visibleItem(process.output(),other),
                    "Foreign produced item advertised to "+other);
            }
            if(i>0){
                check(building.requires().contains(facilities[i-1]),
                    "Faction manufacturing stage bypasses predecessor at "+facilities[i]);
                check(process.inputs().containsKey(
                    CraftingEraRegistry.process(steps[i-1]).output())||
                    i==4&&process.inputs().containsKey(
                        CraftingEraRegistry.process(steps[2]).output()),
                    "Faction stage does not materially consume its chain at "+steps[i]);
            }
        }
        check(stages.equals(EnumSet.of(CraftingEraRegistry.Era.ENCAMPMENT,
            CraftingEraRegistry.Era.SALVAGE,CraftingEraRegistry.Era.STEAM,
            CraftingEraRegistry.Era.ELECTRIC,CraftingEraRegistry.Era.AUTOMATION)),
            "A faction lacked a dedicated early-to-industrial branch");
    }
    public static void main(String[] args){
        CraftingEraRegistry.validate();
        checkSpine(CraftingEraRegistry.Faction.UNRAVELED,
            new String[]{"unraveled-scrap-outpost","unraveled-machinist-bay",
                "unraveled-pressure-shop","unraveled-sensor-lab","unraveled-defense-works"},
            new String[]{"unraveled-recover-scrap","unraveled-make-bearings",
                "unraveled-pressure-vessel","unraveled-calibrate-sensors",
                "unraveled-defense-scanner"});
        checkSpine(CraftingEraRegistry.Faction.PENITENT,
            new String[]{"penitent-votive-altar","penitent-reliquary",
                "penitent-cantor-dais","penitent-choir-relay","penitent-ward-sanctum"},
            new String[]{"penitent-weave-ritual","penitent-engrave-reliquary",
                "penitent-cast-censer","penitent-score-hymn","penitent-sanctify-shell"});
        checkSpine(CraftingEraRegistry.Faction.BEGOTTEN,
            new String[]{"begotten-brood-nest","begotten-spore-basin",
                "begotten-chitin-vat","begotten-membrane-lab","begotten-organ-lattice"},
            new String[]{"begotten-mix-slurry","begotten-culture-spores",
                "begotten-harden-chitin","begotten-grow-membrane","begotten-pulse-organ"});
        check(CraftingRecipeIndex.visibleItem("TIMBER",
            CraftingEraRegistry.Faction.ALL),"Universal raw materials must stay visible");
        check(!CraftingRecipeIndex.visibleItem("CONSECRATED_SHELL",
            CraftingEraRegistry.Faction.UNRAVELED),"No foreign religious production");
        check(!CraftingRecipeIndex.visibleItem("ORGAN_CORE",
            CraftingEraRegistry.Faction.PENITENT),"No foreign biological production");
        check(!CraftingRecipeIndex.visibleItem("RELAY_BOX",
            CraftingEraRegistry.Faction.BEGOTTEN),"No foreign mechanical production");
        System.out.println("PASS FactionIndustrySeparationTest three five-stage distinct spines"+
            " with server-faction scoped dependency graph");
    }
}
