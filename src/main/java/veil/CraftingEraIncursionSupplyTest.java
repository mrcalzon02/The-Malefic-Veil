package veil;

import java.util.LinkedHashMap;
import java.util.Map;

/** Exact production-registry negative probes for the three distinct Incursion factories. */
public final class CraftingEraIncursionSupplyTest {
    private static int passed;
    private static void check(boolean actual, String why) {
        if (!actual) throw new AssertionError(why);
        passed++;
    }
    private static void rejected(String label, Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { passed++; return; }
        throw new AssertionError("Invalid producer graph accepted: " + label);
    }
    private static void withExtraMaterial(String id, String resource) {
        var facilities = new LinkedHashMap<>(CraftingEraRegistry.facilities());
        var old = facilities.get(id);
        if (old == null) throw new AssertionError("Missing authentic facility " + id);
        var bill = new LinkedHashMap<>(old.materials());
        bill.put(resource, 1);
        facilities.put(id, new CraftingEraRegistry.Facility(
            old.id(), old.era(), old.faction(), old.requires(), bill,
            old.width(), old.depth(), old.seconds(), old.utilityDemand()));
        CraftingEraRegistry.validateFacilitySupplyGraph(facilities, CraftingEraRegistry.processes());
    }
    private static void duplicateIndustrialOutput() {
        var recipes = new LinkedHashMap<>(CraftingEraRegistry.processes());
        var old = recipes.get("resilience-core");
        recipes.put("test-repeated-output", new CraftingEraRegistry.Process(
            "test-repeated-output", old.era(), old.faction(), old.facility(),
            old.inputs(), old.output(), old.quantity(), old.seconds(), old.utilityDemand()));
        CraftingEraRegistry.validateFacilitySupplyGraph(CraftingEraRegistry.facilities(), recipes);
    }
    private static void absentProducerFacility() {
        var recipes = new LinkedHashMap<>(CraftingEraRegistry.processes());
        var old = recipes.get("resilience-core");
        recipes.put(old.id(), new CraftingEraRegistry.Process(
            old.id(), old.era(), old.faction(), "test-missing-plant",
            old.inputs(), old.output(), old.quantity(), old.seconds(), old.utilityDemand()));
        CraftingEraRegistry.validateFacilitySupplyGraph(CraftingEraRegistry.facilities(), recipes);
    }
    public static void main(String[] args) {
        var facilities = CraftingEraRegistry.facilities();
        var recipes = CraftingEraRegistry.processes();
        CraftingEraRegistry.validate();
        CraftingEraRegistry.validateFacilitySupplyGraph(facilities, recipes);
        passed++;
        String[] plants = {"resilience-grid", "dread-works", "incursion-anchor"};
        String[] outputs = {"RESILIENCE_CORE", "DREAD_WARD", "BREACH_SEED"};
        CraftingEraRegistry.Faction[] owners = {
            CraftingEraRegistry.Faction.UNRAVELED,
            CraftingEraRegistry.Faction.PENITENT,
            CraftingEraRegistry.Faction.BEGOTTEN
        };
        for (int i = 0; i < plants.length; i++) {
            var facility = facilities.get(plants[i]);
            check(facility != null && facility.faction() == owners[i] &&
                  facility.era() == CraftingEraRegistry.Era.INCURSION,
                  "Wrong distinct Incursion facility contract " + plants[i]);
            final int current = i;
            check(recipes.values().stream().anyMatch(p -> p.output().equals(outputs[current]) &&
                  p.facility().equals(plants[current]) && p.faction() == owners[current]),
                  "Missing original Incursion producer " + outputs[i]);
            rejected("self-production " + plants[i],
                () -> withExtraMaterial(plants[current], outputs[current]));
            rejected("foreign manufactured item " + plants[i],
                () -> withExtraMaterial(plants[current], outputs[(current + 1) % 3]));
        }
        rejected("unsourced Incursion material", () ->
            withExtraMaterial("incursion-anchor", "TEST_MISSING_INCURSION_INPUT"));
        rejected("future output in encampment facility", () ->
            withExtraMaterial("unraveled-scrap-outpost", "RESILIENCE_CORE"));
        rejected("duplicate industrial output", CraftingEraIncursionSupplyTest::duplicateIndustrialOutput);
        rejected("producer station absent from facility registry", CraftingEraIncursionSupplyTest::absentProducerFacility);
        System.out.println("PASS CraftingEraIncursionSupplyTest: " + passed +
            " real-registry and negative graph checks");
    }
}
