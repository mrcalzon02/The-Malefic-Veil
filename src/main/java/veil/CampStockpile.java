package veil;
import java.util.*;
/** Fixed-world owner-held crate, never temporary thrown equipment. */
public final class CampStockpile {
    public static final int MAX_UNITS=128, MAX_STACKS=32, MAX_STACK=128;
    private final EnumMap<SurvivalCrafting.Item,Integer> stock=
        new EnumMap<>(SurvivalCrafting.Item.class);
    public synchronized Map<SurvivalCrafting.Item,Integer> snapshot(){return Map.copyOf(stock);}
    public synchronized int count(SurvivalCrafting.Item item){return item==null?0:stock.getOrDefault(item,0);}
    public synchronized int units(){int n=0;for(int x:stock.values())n+=x;return n;}
    public static boolean valid(Map<SurvivalCrafting.Item,Integer> saved){
        if(saved==null||saved.size()>MAX_STACKS)return false;
        int n=0;
        for(var e:saved.entrySet()){
            if(e.getKey()==null||e.getValue()==null||e.getValue()<1||
               e.getValue()>MAX_STACK)return false;
            n+=e.getValue();
        }
        return n<=MAX_UNITS;
    }
    public synchronized boolean canAdd(SurvivalCrafting.Item item,int n){
        return item!=null&&n>0&&n<=MAX_STACK&&count(item)+n<=MAX_STACK&&
            units()+n<=MAX_UNITS&&(stock.containsKey(item)||stock.size()<MAX_STACKS);
    }
    public synchronized boolean delivered(SurvivalCrafting.Item item,int n){
        if(!canAdd(item,n))return false;
        stock.merge(item,n,Integer::sum);return true;
    }
    public synchronized boolean deposit(SurvivalCrafting bag,SurvivalCrafting.Item item,int n){
        if(bag==null||!canAdd(item,n)||bag.count(item)<n)return false;
        if(!bag.debit(item,n))return false;
        stock.merge(item,n,Integer::sum);return true;
    }
    public synchronized boolean withdraw(SurvivalCrafting bag,SurvivalCrafting.Item item,int n){
        if(bag==null||item==null||n<1||count(item)<n||
           !bag.canAcquire(item,n,true))return false;
        if(!bag.acquire(item,n,true))return false;
        int left=count(item)-n;
        if(left==0)stock.remove(item);else stock.put(item,left);
        return true;
    }
    public synchronized void restore(Map<SurvivalCrafting.Item,Integer> saved){
        if(!valid(saved))throw new IllegalArgumentException("Invalid saved crate contents");
        stock.clear();stock.putAll(saved);
    }
    public synchronized void destroy(){stock.clear();}
}
