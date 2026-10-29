package veil;

import java.util.*;

/** Host ledger invariants for salvage, looked-at trades and timed drops. */
public final class CityLootEconomyTest {
    private static void check(boolean yes,String message){
        if(!yes)throw new AssertionError(message);
    }
    public static void main(String[] args){
        for(long seed:new long[]{11,93,20261021}){
            World world=new World(seed);
            CityLootEconomy loot=new CityLootEconomy(world);
            check(!loot.containers().isEmpty(),"No loot props");
            check(loot.containers().equals(new CityLootEconomy(new World(seed)).containers()),
                "Non-deterministic containers");
            for(var c:loot.containers())
                check(world.city.categoryAt((int)c.x(),(int)c.y())!=CityLayout.Category.SAFE,
                    "Safe plot contains loot");
            var c=loot.containers().get(0);
            SurvivalCrafting bag=new SurvivalCrafting();
            // Physical prop itself blocks its center; test nearby open approach.
            boolean found=false;
            for(double ax=c.x()-2;ax<=c.x()+2&&!found;ax+=.5)
                for(double ay=c.y()-2;ay<=c.y()+2&&!found;ay+=.5)
                    if(world.canWalk(ax,ay)&&world.lineClear(ax,ay,c.x(),c.y())){
                        check(loot.scavenge(c.id(),ax,ay,bag),"Loot acquisition failed");
                        check(!loot.scavenge(c.id(),ax,ay,bag),"Duplicated container");
                        found=true;
                    }
            check(found,"Container has no accessible approach");
            SurvivalCrafting receiver=new SurvivalCrafting();
            var item=c.item();
            check(!loot.trade(bag,receiver,item,1,1.5,1.5,0,8.5,1.5),
                "Remote trade accepted");
            double[] p=world.spawns[0];
            check(loot.drop(bag,item,1,p[0],p[1],1000),"Drop failed");
            check(loot.ground(1000).size()==1,"Drop not visible");
            long id=loot.ground(1000).get(0).id();
            check(!loot.pickup(id,receiver,p[0],p[1],1000+CityLootEconomy.GROUND_LIFETIME_MS),
                "Expired drop collected");
            check(loot.ground(1000+CityLootEconomy.GROUND_LIFETIME_MS).isEmpty(),
                "Expired item persists");
        }
        System.out.println("PASS CityLootEconomyTest");
    }
}
