package veil;

import java.util.*;

/**
 * Bounded server-side industrial work orders. Data-only factory component:
 * network, authorization, ownership, physical ports and durable zone journal
 * must be supplied by a future server-side base implementation.
 *
 * A process reserves its real inputs at the START, advances only if facilities
 * have service/power and creates each output at most once. There are no
 * render-frame tick dependencies or unlimited offline catch-up.
 */
public final class IndustrialWorkstation {
    public enum State { IDLE, RUNNING, NO_POWER, NO_SERVICE, NO_ROOM, NEEDS_REPAIR }
    public record Job(String requestId,String processId,long elapsedMs,
                      Map<String,Integer> reserved) {}
    public record Snapshot(String facilityId,CraftingEraRegistry.Faction faction,
                           Map<String,Integer> stock,List<String> acceptedRequests,
                           Job job,int wear,int completed) {}
    public static final int MAX_STACKS=64, MAX_UNITS=512,MAX_RECEIPTS=256;
    private final CraftingEraRegistry.Facility facility;
    private final CraftingEraRegistry.Faction faction;
    private final LinkedHashMap<String,Integer> stock=new LinkedHashMap<>();
    private final LinkedHashSet<String> accepted=new LinkedHashSet<>();
    private Job active;
    private State state=State.IDLE;
    private int wear,completed;
    public IndustrialWorkstation(String facilityId,CraftingEraRegistry.Faction faction){
        facility=CraftingEraRegistry.facility(facilityId);
        if(facility==null||faction==null||faction==CraftingEraRegistry.Faction.ALL)
            throw new IllegalArgumentException("Facility and playable faction required");
        if(facility.faction()!=CraftingEraRegistry.Faction.ALL&&facility.faction()!=faction)
            throw new IllegalArgumentException("Wrong-faction machinery");
        this.faction=faction;
    }
    public synchronized String facilityId(){return facility.id();}
    public synchronized State state(){return state;}
    public synchronized int wear(){return wear;}
    public synchronized int completed(){return completed;}
    public synchronized Job active(){return active;}
    public synchronized Map<String,Integer> stock(){return Map.copyOf(stock);}
    public synchronized int count(String resource){return stock.getOrDefault(resource,0);}
    private int units(){int total=0;for(int count:stock.values())total+=count;return total;}
    private boolean hasSpace(String name,int quantity){
        return quantity>0&&quantity<=MAX_UNITS&&units()+quantity<=MAX_UNITS&&
            (stock.containsKey(name)||stock.size()<MAX_STACKS);
    }
    /**
     * TRUSTED HOST PORT. Caller MUST validate actual crew stock, machine
     * inventory rights and transfer receipt before depositing source materials.
     */
    public synchronized boolean deposit(String resource,int quantity){
        if(resource==null||resource.isBlank()||!hasSpace(resource,quantity))return false;
        stock.merge(resource,quantity,Integer::sum);return true;
    }
    /** Trusted withdrawal. Future host wraps in a durable storage transaction. */
    public synchronized boolean withdraw(String resource,int quantity){
        if(resource==null||quantity<1||count(resource)<quantity)return false;
        int left=count(resource)-quantity;
        if(left==0)stock.remove(resource);else stock.put(resource,left);
        return true;
    }
    private CraftingEraRegistry.Process recipe(String id){
        var entry=CraftingEraRegistry.process(id);
        if(entry==null||!entry.facility().equals(facility.id())||
            entry.faction()!=CraftingEraRegistry.Faction.ALL&&entry.faction()!=faction)
            return null;
        return entry;
    }
    public synchronized boolean start(String requestId,String recipeId){
        if(requestId==null||requestId.isBlank()||requestId.length()>80||
            accepted.size()>=MAX_RECEIPTS||accepted.contains(requestId)||active!=null||
            wear>=100)return false;
        var process=recipe(recipeId);
        if(process==null)return false;
        for(var item:process.inputs().entrySet())
            if(count(item.getKey())<item.getValue())return false;
        // Debit all inputs only after complete validation.
        for(var item:process.inputs().entrySet())
            withdraw(item.getKey(),item.getValue());
        accepted.add(requestId);
        active=new Job(requestId,recipeId,0,process.inputs());
        state=State.RUNNING;
        return true;
    }
    /**
     * Bounded authoritative tick. Interruption does not return inputs.
     * A finished order blocked by full output remains pending, never minted.
     */
    public synchronized State advance(long millis,int suppliedUtility,boolean operational){
        if(millis<0||millis>1000||suppliedUtility<0)
            throw new IllegalArgumentException("Unbounded workstation simulation tick");
        if(active==null){state=State.IDLE;return state;}
        CraftingEraRegistry.Process recipe=recipe(active.processId());
        if(recipe==null)throw new IllegalStateException("Active recipe missing");
        if(!operational){state=State.NO_SERVICE;return state;}
        if(wear>=100){state=State.NEEDS_REPAIR;return state;}
        if(suppliedUtility<recipe.utilityDemand()){
            state=State.NO_POWER;return state;
        }
        long duration=recipe.seconds()*1000L;
        long updated=Math.min(duration,active.elapsedMs()+millis);
        active=new Job(active.requestId(),active.processId(),updated,active.reserved());
        if(updated<duration){state=State.RUNNING;return state;}
        if(!hasSpace(recipe.output(),recipe.quantity())){
            state=State.NO_ROOM;return state;
        }
        stock.merge(recipe.output(),recipe.quantity(),Integer::sum);
        wear=Math.min(100,wear+Math.max(1,recipe.utilityDemand()/3+1));
        completed++;
        active=null;
        state=State.IDLE;
        return state;
    }
    /**
     * Aborting before any productive work returns reserved material (only if
     * buffer capacity permits). Once work starts, its spent materials are lost.
     * This prevents free progress/reset and stop/start resource exploits.
     */
    public synchronized boolean abort(){
        if(active==null)return false;
        if(active.elapsedMs()==0){
            int recovered=0;
            for(int n:active.reserved().values())recovered+=n;
            if(units()+recovered>MAX_UNITS||stock.size()+active.reserved().size()>MAX_STACKS)
                return false;
            for(var item:active.reserved().entrySet())
                stock.merge(item.getKey(),item.getValue(),Integer::sum);
        }
        active=null;state=State.IDLE;return true;
    }
    /** Facility maintenance costs real stock; low-era repairs do not demand trucks. */
    public synchronized boolean repair(){
        if(wear<=0)return false;
        String supply=facility.era().ordinal()<CraftingEraRegistry.Era.STEAM.ordinal()?
            "TIMBER":"MACHINE_PARTS";
        if(!withdraw(supply,1))return false;
        wear=Math.max(0,wear-25);
        if(active!=null&&state==State.NEEDS_REPAIR)state=State.RUNNING;
        return true;
    }
    public synchronized Snapshot snapshot(){
        return new Snapshot(facility.id(),faction,Map.copyOf(stock),
            List.copyOf(accepted),active,wear,completed);
    }
    public synchronized void restore(Snapshot snap){
        if(snap==null||!snap.facilityId().equals(facility.id())||
            snap.faction()!=faction||snap.stock().size()>MAX_STACKS||
            snap.acceptedRequests().size()>MAX_RECEIPTS||
            snap.wear()<0||snap.wear()>100||snap.completed()<0)
            throw new IllegalArgumentException("Wrong or corrupt machine save");
        long amount=0;
        for(var entry:snap.stock().entrySet()){
            if(entry.getKey()==null||entry.getKey().isBlank()||entry.getValue()==null||
                entry.getValue()<1)throw new IllegalArgumentException("Corrupt inventory");
            amount+=entry.getValue();
        }
        if(amount>MAX_UNITS||new HashSet<>(snap.acceptedRequests()).size()!=
            snap.acceptedRequests().size())
            throw new IllegalArgumentException("Impossible industrial inventory");
        if(snap.job()!=null){
            var job=snap.job(),process=recipe(job.processId());
            if(process==null||job.requestId()==null||
               !snap.acceptedRequests().contains(job.requestId())||
               job.elapsedMs()<0||job.elapsedMs()>process.seconds()*1000L||
               !job.reserved().equals(process.inputs()))
                throw new IllegalArgumentException("Forged active factory job");
        }
        // No partial state is applied until validations have passed.
        stock.clear();stock.putAll(snap.stock());
        accepted.clear();accepted.addAll(snap.acceptedRequests());
        active=snap.job();
        wear=snap.wear();completed=snap.completed();
        state=active==null?State.IDLE:State.RUNNING;
    }
}
