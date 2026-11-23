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
    /** Malformed construction bills must fail before any future host debit. */
    private static void badConstruction(String label, Map<String,Integer> bill) {
        var facilities = new LinkedHashMap<>(CraftingEraRegistry.facilities());
        var old = facilities.get("resilience-grid");
        facilities.put(old.id(), new CraftingEraRegistry.Facility(
            old.id(), old.era(), old.faction(), old.requires(), bill,
            old.width(), old.depth(), old.seconds(), old.utilityDemand()));
        rejected(label, () -> CraftingEraRegistry.validateFacilitySupplyGraph(
            facilities, CraftingEraRegistry.processes()));
    }
    private static void badConstructionStats(String label, int width, int seconds, int utility) {
        var facilities = new LinkedHashMap<>(CraftingEraRegistry.facilities());
        var old = facilities.get("resilience-grid");
        facilities.put(old.id(), new CraftingEraRegistry.Facility(
            old.id(), old.era(), old.faction(), old.requires(), old.materials(),
            width, old.depth(), seconds, utility));
        rejected(label, () -> CraftingEraRegistry.validateFacilitySupplyGraph(
            facilities, CraftingEraRegistry.processes()));
    }
    private static void badProcess(String label, String facility, String output,
                                   Map<String,Integer> inputs, int quantity, int seconds, int utility) {
        var processes = new LinkedHashMap<>(CraftingEraRegistry.processes());
        var old = processes.get("resilience-core");
        processes.put(old.id(), new CraftingEraRegistry.Process(old.id(),
            old.era(), old.faction(), facility, inputs, output,
            quantity, seconds, utility));
        rejected(label, () -> CraftingEraRegistry.validateFacilitySupplyGraph(
            CraftingEraRegistry.facilities(), processes));
    }
    private static void invalidBillsAndProcesses() {
        var plant = CraftingEraRegistry.facility("resilience-grid");
        var cost = new LinkedHashMap<>(plant.materials());
        cost.put("METAL_STOCK", 0);
        badConstruction("zero construction material", cost);
        cost.put("METAL_STOCK", -1);
        badConstruction("negative construction material", cost);
        cost.put("METAL_STOCK", null);
        badConstruction("null construction material count", cost);
        badConstruction("missing construction bill", Map.of());
        badConstruction("null construction bill", null);
        cost = new LinkedHashMap<>(plant.materials());
        cost.put("", 1);
        badConstruction("blank construction material", cost);
        cost = new LinkedHashMap<>(plant.materials());
        cost.put("METAL_STOCK", Integer.MAX_VALUE);
        badConstruction("overflowing aggregate construction bill", cost);
        badConstructionStats("zero construction width", 0, plant.seconds(), plant.utilityDemand());
        badConstructionStats("zero construction duration", plant.width(), 0, plant.utilityDemand());
        badConstructionStats("negative facility utility", plant.width(), plant.seconds(), -1);
        var original = CraftingEraRegistry.process("resilience-core");
        var inputs = new LinkedHashMap<>(original.inputs());
        inputs.put("MACHINE_PARTS", 0);
        badProcess("zero process input", original.facility(), original.output(), inputs,
            original.quantity(), original.seconds(), original.utilityDemand());
        badProcess("missing process inputs", original.facility(), original.output(), Map.of(),
            original.quantity(), original.seconds(), original.utilityDemand());
        badProcess("blank process output", original.facility(), "",
            original.inputs(), original.quantity(), original.seconds(), original.utilityDemand());
        badProcess("missing producer station", "nonexistent-incursion-station", original.output(),
            original.inputs(), original.quantity(), original.seconds(), original.utilityDemand());
        badProcess("zero process output count", original.facility(), original.output(),
            original.inputs(), 0, original.seconds(), original.utilityDemand());
        badProcess("zero process time", original.facility(), original.output(),
            original.inputs(), original.quantity(), 0, original.utilityDemand());
        badProcess("negative process utility", original.facility(), original.output(),
            original.inputs(), original.quantity(), original.seconds(), -1);
        var facilities = new LinkedHashMap<>(CraftingEraRegistry.facilities());
        var grave = facilities.get("monument-graveyard");
        var cheap = new LinkedHashMap<>(grave.materials());
        cheap.put("METAL_STOCK", 23);
        facilities.put(grave.id(), new CraftingEraRegistry.Facility(grave.id(), grave.era(),
            grave.faction(), grave.requires(), cheap, grave.width(), grave.depth(),
            grave.seconds(), grave.utilityDemand()));
        rejected("reduced graveyard metal cost", () ->
            CraftingEraRegistry.validateFacilitySupplyGraph(facilities, CraftingEraRegistry.processes()));
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
        invalidBillsAndProcesses();
        System.out.println("PASS CraftingEraIncursionSupplyTest: " + passed +
            " real-registry and negative graph checks");
    }
}
