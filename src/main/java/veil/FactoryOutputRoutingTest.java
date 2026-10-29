package veil;

import java.util.*;

/**
 * Actual live fixed-world crate selection for completed output: only owner
 * warehouses within producer reach, fully payable cargo quantities, and
 * active, valid downstream factory demand may outrank the legacy fallback.
 * The full paid two-stage operation is separately exercised by
 * AutomaticFactoryFeedTest.
 */
public final class FactoryOutputRoutingTest {
    private static void check(boolean actual,String reason){
        if(!actual)throw new AssertionError(reason);
    }
    private static World.Device add(List<World.Device> devices,int kind,
            double x,double y,String owner){
        var d=new World.Device(x,y,kind);
        d.ownerKey=owner;devices.add(d);return d;
    }
    private static void shouldSelect(World.Device expected,World.Device producer,
            List<World.Device> devices,String reason){
        check(ConveyorLogistics.outputCrate(producer,
            SurvivalCrafting.Item.PRECISION_FEEDLINE,2,devices)==expected,reason);
    }
    public static void main(String[] args){
        String owner="acct_"+"e".repeat(64);
        String alien="acct_"+"a".repeat(64);
        var devices=new ArrayList<World.Device>();
        var producer=add(devices,ArenaSystems.INDUSTRIAL_WORKSHOP,-8,0,owner);
        var decoy=add(devices,ArenaSystems.STORAGE_CRATE,-7.5,.4,owner);
        var source=add(devices,ArenaSystems.STORAGE_CRATE,-5,0,owner);
        var receiver=add(devices,ArenaSystems.STORAGE_CRATE,5,0,owner);
        var belt=add(devices,ArenaSystems.CONVEYOR_LINE,0,0,owner);
        var consumer=add(devices,ArenaSystems.INDUSTRIAL_WORKSHOP,8,0,owner);
        consumer.queuedRecipeId="ballistic-resupply";
        check(receiver.storage.delivered(SurvivalCrafting.Item.HARDENED_ALLOY,2)&&
              receiver.storage.delivered(SurvivalCrafting.Item.FASTENERS,4)&&
              receiver.storage.delivered(SurvivalCrafting.Item.TRACKING_CIRCUIT,1),
              "Could not stage actual downstream E5 ingredients");
        shouldSelect(decoy,producer,devices,
            "Without a configured belt legacy output destination must stay stable");
        belt.routeItem="AUTO";
        shouldSelect(source,producer,devices,
            "Live AUTO route failed to override earlier wrong output crate");
        consumer.queuedRecipeId=AutomationOrders.repeat("ballistic-resupply");
        shouldSelect(source,producer,devices,
            "Repeating downstream order lost priority for real conveyor output");
        consumer.queuedRecipeId="ballistic-resupply";
        belt.routeItem="PRECISION_FEEDLINE";
        shouldSelect(source,producer,devices,
            "Explicit matching manual belt failed to prioritize its real source");
        belt.routeItem="MACHINE_PARTS";
        shouldSelect(decoy,producer,devices,
            "Unrelated manual item route commandeered factory output");
        belt.routeItem="AUTO";
        consumer.queuedRecipeId="";
        shouldSelect(decoy,producer,devices,
            "AUTO without actual downstream demand must preserve legacy fallback");
        consumer.queuedRecipeId="ballistic-resupply";
        receiver.ownerKey=alien;
        shouldSelect(decoy,producer,devices,
            "Foreign faction warehouse was treated as an output destination");
        receiver.ownerKey=owner;
        check(receiver.storage.delivered(SurvivalCrafting.Item.EDIBLES,121),
            "Could not completely fill recipient to test backpressure");
        shouldSelect(decoy,producer,devices,
            "Fully packed downstream warehouse improperly reserved output");
        check(receiver.storage.consume(Map.of(SurvivalCrafting.Item.EDIBLES,121)),
            "Cannot release test destination capacity");
        check(source.storage.delivered(SurvivalCrafting.Item.EDIBLES,127),
            "Could not fill source crate to one remaining free unit");
        shouldSelect(decoy,producer,devices,
            "Factory split a two-unit output into an insufficient source");
        check(source.storage.consume(Map.of(SurvivalCrafting.Item.EDIBLES,127)),
            "Cannot clear temporarily packed output source");
        belt.hp=0;
        shouldSelect(decoy,producer,devices,
            "Destroyed conveyor still selected an inaccessible source");
        belt.hp=100;
        producer.ownerKey=alien;
        check(ConveyorLogistics.outputCrate(producer,
            SurvivalCrafting.Item.PRECISION_FEEDLINE,2,devices)==null,
            "Production from foreign factory accessed a private settlement");
        producer.ownerKey=owner;
        // Second physical chain reverses which side is the source and proves
        // that an earlier, closer-but-unreachable crate loses to the real belt.
        var reverse=new ArrayList<World.Device>();
        var eastProducer=add(reverse,ArenaSystems.INDUSTRIAL_WORKSHOP,8,0,owner);
        var eastDecoy=add(reverse,ArenaSystems.STORAGE_CRATE,7.5,.4,owner);
        var eastSource=add(reverse,ArenaSystems.STORAGE_CRATE,5,0,owner);
        var westTarget=add(reverse,ArenaSystems.STORAGE_CRATE,-5,0,owner);
        var westConsumer=add(reverse,ArenaSystems.INDUSTRIAL_WORKSHOP,-8,0,owner);
        westConsumer.queuedRecipeId="ballistic-resupply";
        var reverseBelt=add(reverse,ArenaSystems.CONVEYOR_LINE,0,0,owner);
        reverseBelt.routeReverse=true;
        reverseBelt.routeItem="AUTO";
        shouldSelect(eastSource,eastProducer,reverse,
            "East-to-west AUTO feeder ignored physical reverse source");
        reverseBelt.routeItem="PRECISION_FEEDLINE";
        shouldSelect(eastSource,eastProducer,reverse,
            "Reversed manual belt failed to connect its actual source");
        reverseBelt.routeReverse=false;
        shouldSelect(eastDecoy,eastProducer,reverse,
            "Reversed route was treated as bidirectional without programming");
        System.out.println("PASS FactoryOutputRoutingTest: demand-directed output, manual/AUTO reverse, ownership, full-stock fallback");
    }
}
