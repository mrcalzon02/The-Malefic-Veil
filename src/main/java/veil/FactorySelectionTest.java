package veil;

import java.util.*;

/** Factory actions and the inspection screen must target the SAME closest
 * reachable owned worksite, not whichever structure was built first.
 * Preserve deliberate multi-factory queues and independent account rights. */
public final class FactorySelectionTest {
    private static void check(boolean yes,String reason){
        if(!yes)throw new AssertionError(reason);
    }
    private static World.Device fixture(ArenaSystems host,int kind,
            double x,double y,String key){
        var d=new World.Device(x,y,kind);
        d.ownerKey=key;host.world.devices.add(d);return d;
    }
    public static void main(String[] args){
        String owner="acct_"+"e".repeat(64);
        String foreign="acct_"+"a".repeat(64);
        String id="precision-feedlines";
        ArenaSystems host=new ArenaSystems(20261020L);
        var operator=host.join(1);
        check(host.assignAccount(1,owner)&&
              host.enrollFaction(1,CharacterRoster.Faction.THE_UNRAVELED),
              "Owned machine interaction requires an authenticated faction");
        double[] site=FactionFirstProductionTest.site(host);
        double x=site[0],y=site[1];
        fixture(host,ArenaSystems.CAMP_STAKE,x-2,y-2,owner);
        // Insert the distant and foreign devices before the intended site:
        // naive creation-order programming chooses the WRONG machine.
        var first=fixture(host,ArenaSystems.INDUSTRIAL_WORKSHOP,x,y,owner);
        var alien=fixture(host,ArenaSystems.INDUSTRIAL_WORKSHOP,x+2.15,y+.3,foreign);
        var nearest=fixture(host,ArenaSystems.INDUSTRIAL_WORKSHOP,x+2.2,y,owner);
        var warehouse=fixture(host,ArenaSystems.STORAGE_CRATE,x+1.0,y+.4,owner);
        fixture(host,ArenaSystems.ELECTRIC_BUS,x+.2,y+2.5,owner);
        var generator=fixture(host,ArenaSystems.GENERATOR,x-1,y+3.1,owner);
        generator.electricMs=ElectricUtility.MAX_CHARGE_MS;
        operator.x=nearest.x+.2;operator.y=nearest.y;
        check(host.inspectFactory(1).state().equals("IDLE"),
            "Nearest factory not inspectable in the dense settlement");
        String repeat=AutomationOrders.repeat(id);
        check(host.programNextAutomation(1,repeat)&&
            nearest.queuedRecipeId.equals(repeat)&&
            first.queuedRecipeId.isEmpty()&&alien.queuedRecipeId.isEmpty(),
            "Repeat command targeted first-created or foreign machine");
        check(host.inspectFactory(1).next().equals("REPEAT_"+id),
            "Inspector target disagrees with nearest programmed worksite");
        operator.x=first.x+.25;operator.y=first.y;
        check(host.programNextAutomation(1,id)&&
            first.queuedRecipeId.equals(id)&&
            nearest.queuedRecipeId.equals(repeat),
            "Nearest second owned machine did not accept an independent queued job");
        operator.x=nearest.x+.2;operator.y=nearest.y;
        check(host.programNextAutomation(1,"NONE")&&
            nearest.queuedRecipeId.isEmpty()&&first.queuedRecipeId.equals(id),
            "Cancellation targeted the first registered order instead of nearest");
        operator.x=first.x+.25;operator.y=first.y;
        check(host.programNextAutomation(1,"NONE")&&
            first.queuedRecipeId.isEmpty(),
            "First machine could not cancel its own order by proximity");
        var recipe=CraftingEraRegistry.process(id);
        for(var ingredient:recipe.inputs().entrySet())
            check(warehouse.storage.delivered(
                SurvivalCrafting.Item.valueOf(ingredient.getKey()),
                ingredient.getValue()),"Could not stock real manufacturing inputs");
        operator.x=nearest.x+.2;operator.y=nearest.y;
        check(host.startFactionBatch(1,id)&&
            nearest.productionActive&&!first.productionActive&&!alien.productionActive,
            "Explicit production started older registered machine rather than nearest");
        for(var ingredient:recipe.inputs().entrySet())
            check(warehouse.storage.count(
                SurvivalCrafting.Item.valueOf(ingredient.getKey()))==0,
                "Nearest production did not consume exactly one physical batch payment");
        check(host.inspectFactory(1).recipe().equals(id)&&
            host.inspectFactory(1).state().equals("RUNNING"),
            "Nearest inspection not aligned with operating production");
        System.out.println("PASS FactorySelectionTest: nearest owner-selected queue, cancel, inspection and real production");
    }
}
