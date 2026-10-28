package veil;

import java.util.*;

/**
 * Host-side construction payment from a fixed set of already ownership- and
 * distance-validated crates. Unlike a production batch, a blueprint may
 * span several crates and exceed the per-crate stack cap.
 *
 * Locks every participating stockpile for the complete preflight and debit.
 * A missing unit, duplicate crate, or changed stock refuses the entire bill.
 * This is an in-memory transaction; durable atomicity is supplied by the
 * paired HostCheckpoint world/account save, not by this helper.
 */
final class ConstructionFunding {
    private ConstructionFunding(){}

    static boolean pay(List<CampStockpile> crates,
                       Map<SurvivalCrafting.Item,Integer> bill){
        if(crates==null||crates.isEmpty()||bill==null||bill.isEmpty())
            return false;
        Set<CampStockpile> unique=Collections.newSetFromMap(
            new IdentityHashMap<CampStockpile,Boolean>());
        for(CampStockpile crate:crates)
            if(crate==null||!unique.add(crate))return false;
        for(var item:bill.entrySet())
            if(item.getKey()==null||item.getValue()==null||item.getValue()<1)
                return false;
        return withLocks(crates,0,bill);
    }

    private static boolean withLocks(List<CampStockpile> crates,int index,
            Map<SurvivalCrafting.Item,Integer> bill){
        if(index==crates.size())return payLocked(crates,bill);
        synchronized(crates.get(index)){
            return withLocks(crates,index+1,bill);
        }
    }

    /** All stockpiles remain locked until either every debit succeeds or
     * every changed stockpile has been restored to its exact prior state. */
    private static boolean payLocked(List<CampStockpile> crates,
            Map<SurvivalCrafting.Item,Integer> bill){
        EnumMap<SurvivalCrafting.Item,Integer> remaining=
            new EnumMap<>(SurvivalCrafting.Item.class);
        remaining.putAll(bill);
        List<Map<SurvivalCrafting.Item,Integer>> before=new ArrayList<>();
        List<Map<SurvivalCrafting.Item,Integer>> debits=new ArrayList<>();
        for(CampStockpile crate:crates){
            Map<SurvivalCrafting.Item,Integer> snapshot=crate.snapshot();
            before.add(snapshot);
            EnumMap<SurvivalCrafting.Item,Integer> take=
                new EnumMap<>(SurvivalCrafting.Item.class);
            for(var item:remaining.entrySet()){
                int n=Math.min(item.getValue(),
                    snapshot.getOrDefault(item.getKey(),0));
                if(n>0){
                    take.put(item.getKey(),n);
                    remaining.put(item.getKey(),item.getValue()-n);
                }
            }
            debits.add(take);
        }
        for(int count:remaining.values())if(count!=0)return false;

        int committed=0;
        try{
            for(int i=0;i<crates.size();i++){
                if(!debits.get(i).isEmpty()){
                    if(!crates.get(i).consume(debits.get(i)))
                        throw new IllegalStateException("Construction stock changed during debit");
                }
                committed=i+1;
            }
            return true;
        }catch(RuntimeException failure){
            for(int i=committed-1;i>=0;i--)
                crates.get(i).restore(before.get(i));
            throw failure;
        }
    }
}
