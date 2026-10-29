package veil;

import java.util.*;

/** Harvesting respects immutable cities and established world structures. */
public final class WildernessResourcesTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        for(long seed:new long[]{42,887766,20261020}){
            ContinentLayout map=ContinentLayout.generate(seed);
            WildernessResources fields=new WildernessResources(map);
            List<WildernessResources.Node> sources=fields.nodes();
            check(sources.size()>100,"Continental gatherables too sparse");
            var again=new WildernessResources(map).nodes();
            check(sources.equals(again),"Wilderness gatherables not reproducible");
            Set<WildernessResources.Type> kinds=EnumSet.noneOf(WildernessResources.Type.class);
            for(var node:sources){
                check(!map.cityProtected(node.x(),node.y())&&!map.road(node.x(),node.y())&&
                    !map.worldStructureAt(node.x(),node.y()),"Gatherable spawned in city or generated structure");
                kinds.add(node.type());
            }
            check(kinds.contains(WildernessResources.Type.TIMBER)&&
                kinds.contains(WildernessResources.Type.FIELD_STONE)&&
                kinds.contains(WildernessResources.Type.FIBER),
                "Survival-critical resource types missing");
            var target=sources.get(0);
            check(fields.harvest(target.id(),target.x()+50,target.y()+50)==null,
                "Harvest range spoofing accepted");
            for(int i=0;i<target.capacity();i++)
                check(fields.harvest(target.id(),target.x(),target.y())==target.type(),
                    "Nearby wilderness harvest rejected");
            check(fields.harvest(target.id(),target.x(),target.y())==null,
                "Exhausted resource replicated without cooldown");
            fields.advance(600_000);
            check(fields.harvest(target.id(),target.x(),target.y())==target.type(),
                "Wild resource failed to replenish");
            System.out.println("PASS WildernessResourcesTest seed="+seed+" resources="+sources.size());
        }
    }
}
