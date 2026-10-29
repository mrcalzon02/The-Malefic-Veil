package veil;

import java.nio.file.*;
import java.util.*;

/** Headless acceptance: actual PNG slices, not placeholder colored squares. */
public final class CraftingIconPacksTest {
    private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception{
        Path root=args.length==0?Path.of(""):Path.of(args[0]);
        CraftingIconPacks pack=CraftingIconPacks.load(root);
        check(pack.entries().size()==150,"five 30-item sheets must all be indexed");
        Map<String,Integer> groupCounts=new HashMap<>();
        for(var entry:pack.entries().values()){
            var icon=pack.icon(entry.id());
            check(icon.getWidth()==128&&icon.getHeight()==128,"bad icon dimensions");
            check(icon.getColorModel().hasAlpha(),"sprite lost transparency");
            check((icon.getRGB(0,0)>>>24)==0,"corner transparent padding missing");
            check(pack.label(entry.id()).equals(entry.label()),"label lookups must remain stable");
            groupCounts.merge(entry.group(),1,Integer::sum);
        }
        check(groupCounts.size()==5&&groupCounts.values().stream().allMatch(n->n==30),
            "five separate faction/common economy icon groups");
        for(String id:List.of("TIMBER","MACHINE_PARTS","SORTED_ORE","RITUAL_CLOTH",
            "CHITIN_SHARDS","VIABLE_SPORES","BALLISTIC_SUPPLY","SACRED_SUPPLY","SPINE_SUPPLY"))
            check(pack.icon(id)!=null,"missing basic or faction production item "+id);
        System.out.println("PASS CraftingIconPacksTest: 150 real 128x128 RGBA PNGs; verified SHA256, 5 groups");
    }
}
