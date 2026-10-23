package veil;

import java.nio.file.*;
import java.util.*;

/** Prevent exported recipe spreadsheets from disagreeing with live registries. */
public final class CraftingEconomyCsvTest {
    private static void check(boolean ok,String reason){
        if(!ok)throw new AssertionError(reason);
    }
    private static List<String[]> rows(Path path,int cols)throws Exception{
        List<String[]> values=new ArrayList<>();
        List<String> lines=Files.readAllLines(path);
        check(!lines.isEmpty(),"Missing crafting CSV "+path);
        for(int i=1;i<lines.size();i++){
            if(lines.get(i).isBlank())continue;
            String[] cells=lines.get(i).split(",",-1);
            check(cells.length==cols,"Malformed crafting row "+path+":"+i);
            for(int k=0;k<cells.length;k++){
                String cell=cells[k].trim();
                check(cell.length()>=2&&cell.startsWith("\"")&&cell.endsWith("\""),
                    "Unquoted crafting CSV field "+path+":"+i);
                cells[k]=cell.substring(1,cell.length()-1);
            }
            values.add(cells);
        }
        return values;
    }
    private static Map<String,Map<String,Integer>> inputs(Path path)throws Exception{
        Map<String,Map<String,Integer>> result=new TreeMap<>();
        for(String[] row:rows(path,3)){
            int amount=Integer.parseInt(row[2]);
            check(amount>0,"Nonpositive ingredient");
            Integer old=result.computeIfAbsent(row[0],k->new TreeMap<>())
                .putIfAbsent(row[1],amount);
            check(old==null,"Duplicate recipe ingredient "+row[0]+"/"+row[1]);
        }
        return result;
    }
    public static void main(String[] args)throws Exception{
        Path root=args.length==0?Path.of(""):Path.of(args[0]);
        List<String[]> hand=rows(root.resolve("economy/hand_recipes.csv"),6);
        List<String[]> industrial=rows(root.resolve("economy/industrial_recipes.csv"),12);
        var handInputs=inputs(root.resolve("economy/hand_recipe_inputs.csv"));
        var industrialInputs=inputs(root.resolve("economy/industrial_recipe_inputs.csv"));
        check(hand.size()==SurvivalCrafting.recipes().size(),"Hand CSV is stale");
        check(industrial.size()==CraftingEraRegistry.processes().size(),"Industrial CSV is stale");
        Set<String> allHand=new HashSet<>();
        for(String[] row:hand){
            check(allHand.add(row[0]),"Duplicate hand recipe");
            var source=SurvivalCrafting.recipes().get(row[0]);
            check(source!=null&&row[1].equals(source.station().name())&&
                row[2].equals(source.result().name())&&
                Integer.parseInt(row[3])==source.quantity()&&
                row[4].equals("CANONICAL_CODE")&&
                row[5].equals(WeaponEconomy.recipeFaction(source).name()),
                "Bad hand export or owner "+row[0]);
            Map<String,Integer> expected=new TreeMap<>();
            source.requires().forEach((item,qty)->expected.put(item.name(),qty));
            check(expected.equals(handInputs.get(row[0])),"Hand ingredients drift "+row[0]);
        }
        check(handInputs.keySet().equals(allHand),"Unknown hand input rows");
        Set<String> allIndustrial=new HashSet<>();
        for(String[] row:industrial){
            check(allIndustrial.add(row[0]),"Duplicate industrial process");
            var source=CraftingEraRegistry.processes().get(row[0]);
            check(source!=null&&row[1].equals("INDUSTRIAL")&&
                row[2].equals(source.era().name())&&
                row[3].equals(source.faction().name())&&
                row[4].equals(source.facility())&&
                row[5].equals(source.output())&&
                Integer.parseInt(row[6])==source.quantity()&&
                Integer.parseInt(row[7])==source.seconds()&&
                Integer.parseInt(row[8])==source.utilityDemand()&&
                source.inputs().equals(industrialInputs.get(row[0])),
                "Bad industrial export "+row[0]);
        }
        check(industrialInputs.keySet().equals(allIndustrial),"Unknown industry input rows");
        var built=rows(root.resolve("economy/facilities.csv"),11);
        var priced=inputs(root.resolve("economy/facility_costs.csv"));
        Set<String> seenFacilities=new HashSet<>();
        for(String[] row:built){
            check(seenFacilities.add(row[0]),"Duplicate facility ID in catalog");
            var f=CraftingEraRegistry.facility(row[0]);
            check(f!=null&&row[1].equals(f.era().name())&&
                row[2].equals(f.faction().name())&&
                row[3].equals(String.join(";",f.requires()))&&
                Integer.parseInt(row[4])==f.width()&&
                Integer.parseInt(row[5])==f.depth()&&
                Integer.parseInt(row[6])==f.seconds()&&
                Integer.parseInt(row[7])==f.utilityDemand()&&
                f.materials().equals(priced.get(row[0])),
                "Facility bills/capacity drift: "+row[0]);
        }
        check(seenFacilities.equals(CraftingEraRegistry.facilities().keySet()),
            "Missing or phantom structures in facility catalog");
        check(priced.keySet().equals(seenFacilities),
            "A facility lacks authentic cost or has an orphan cost row");
        check("CLEAN_WATER".equals(
            CraftingRecipeIndex.recipe("hand:boil-water").output()),
            "Previous WORKBENCH/boil-water corruption reintroduced");
        System.out.println("PASS CraftingEconomyCsvTest: "+hand.size()+" hand + "+
            industrial.size()+" industrial formulas and all actual costs verified");
    }
}
