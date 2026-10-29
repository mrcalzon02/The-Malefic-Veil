package veil;

import java.util.concurrent.TimeUnit;

/** Real inventory/host consumption, including socket results; no UI screenshots. */
public final class SurvivalConsumptionTest {
    private SurvivalConsumptionTest(){}
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static String awaitResult(NetLink peer)throws Exception{
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);
        String result;
        while(System.nanoTime()<until){
            result=peer.consumeResults.poll();
            if(result!=null)return result;
            Thread.sleep(10);
        }
        throw new AssertionError("No authoritative consume result over TCP");
    }
    public static void main(String[] args)throws Exception{
        SurvivalCrafting kit=new SurvivalCrafting();
        check(kit.acquire(SurvivalCrafting.Item.EDIBLES,3,false)&&
            kit.acquire(SurvivalCrafting.Item.CLEAN_WATER,1,false)&&
            kit.acquire(SurvivalCrafting.Item.FIREWOOD,1,false),
            "Missing real ration ingredients");
        check(kit.craft("camp-ration",SurvivalCrafting.Station.FIRE)&&
            kit.count(SurvivalCrafting.Item.RATIONS)==1,
            "Cooked rations not minted from exact ingredients");
        check(!kit.consume(SurvivalCrafting.Item.RATIONS)&&
            kit.count(SurvivalCrafting.Item.RATIONS)==1,
            "Full hunger wasted crafted food");
        for(int minute=0;minute<50;minute++)kit.advance(60_000);
        check(kit.status().hunger()==50&&kit.status().hydration()==0,
            "Survival need decay drifted");
        check(kit.consume(SurvivalCrafting.Item.RATIONS)&&
            kit.status().hunger()==90&&kit.count(SurvivalCrafting.Item.RATIONS)==0,
            "Rations did not restore measured hunger exactly once");
        check(kit.acquire(SurvivalCrafting.Item.SMOKED_RATIONS,1,false)&&
            kit.consume(SurvivalCrafting.Item.SMOKED_RATIONS)&&
            kit.status().hunger()==100,
            "Smoked rations cannot restore hunger");
        check(!kit.consume(SurvivalCrafting.Item.DIRTY_WATER),
            "Unsafe water was treated as potable");
        check(kit.acquire(SurvivalCrafting.Item.CLEAN_WATER,1,false)&&
            kit.consume(SurvivalCrafting.Item.CLEAN_WATER)&&
            kit.status().hydration()==40,
            "Purified water did not replenish hydration");

        try(LanServer server=new LanServer(0,20261026L);
            NetLink client=new NetLink("127.0.0.1",server.port())){
            ArenaSystems.Player actor=server.match.player(client.id);
            check(actor!=null,"Authenticated server actor missing");
            actor.survival.advance(60_000);
            check(actor.survival.acquire(SurvivalCrafting.Item.EDIBLES,1,false)&&
                actor.survival.acquire(SurvivalCrafting.Item.DIRTY_WATER,1,false),
                "Host test ingredients unavailable");
            client.useSupply("EDIBLES");
            check(awaitResult(client).equals("EDIBLES|OK"),
                "Server did not acknowledge real food consumption");
            check(actor.survival.status().hunger()==100&&
                actor.survival.count(SurvivalCrafting.Item.EDIBLES)==0,
                "Network consumption did not atomically debit inventory and feed actor");
            client.useSupply("EDIBLES");
            check(awaitResult(client).equals("EDIBLES|DENIED"),
                "Double consume created free nutrition");
            client.useSupply("DIRTY_WATER");
            check(awaitResult(client).equals("DIRTY_WATER|DENIED")&&
                actor.survival.count(SurvivalCrafting.Item.DIRTY_WATER)==1,
                "Network permitted poisonous or non-potable water");

            // Ineligible dead actors cannot consume carried food even with a valid packet.
            check(actor.survival.acquire(SurvivalCrafting.Item.EDIBLES,1,false),
                "Cannot stage death rule test");
            actor.dead=true;
            client.useSupply("EDIBLES");
            check(awaitResult(client).equals("EDIBLES|DENIED")&&
                actor.survival.count(SurvivalCrafting.Item.EDIBLES)==1,
                "Dead actor consumed supplies");
        }
        System.out.println("PASS SurvivalConsumptionTest: cooked rations, safe water, atomic need updates and host socket denial");
    }
}
