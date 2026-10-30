package veil;

import java.util.*;

/**
 * Server-side airframe production ledger: consumes *actual deposited* inventory
 * once, then consumes metered utility energy over elapsed authoritative ticks.
 * No client may construct this object or send arbitrary stock/energy to it.
 *
 * This is a reusable host core, not yet bound to world-streamed hangar entities,
 * settlement ownership permissions or LAN commands.
 */
public final class AircraftWorkshop {
    public record Job(String recipeId,long elapsedMs,long requiredMs,int utilityDemand) {}
    public record Snapshot(Map<String,Integer> stock,double energyUnits,Job job) {}
    private final CharacterRoster.Faction faction;
    private final String factoryId;
    private final Set<String> commissionedFacilities;
    private final Map<String,Integer> stock=new HashMap<>();
    private double reservedEnergyUnits;
    private Job job;
    public AircraftWorkshop(CharacterRoster.Faction faction,String factoryId,
            Set<String> commissionedFacilities,Map<String,Integer> authoritativeStock) {
        this.faction=Objects.requireNonNull(faction);
        this.factoryId=Objects.requireNonNull(factoryId);
        this.commissionedFacilities=Set.copyOf(commissionedFacilities);
        if(!this.commissionedFacilities.contains(factoryId)||
           !this.commissionedFacilities.contains("airfield-runway")||
           !this.commissionedFacilities.contains("aircraft-service-hangar"))
            throw new IllegalArgumentException("Airframes need actual completed runway and hangar");
        for(var line:authoritativeStock.entrySet()){
            if(line.getKey()==null||line.getValue()==null||line.getValue()<0||
               line.getValue()>1_000_000)
                throw new IllegalArgumentException("Invalid host stock");
            if(line.getValue()>0)stock.put(line.getKey(),line.getValue());
        }
    }
    public CharacterRoster.Faction faction(){return faction;}
    /** One canister is a finite 25-unit batch of faction-specific power source.
     * The host must reject client stock updates; only completed paid recipes
     * or independently verified delivered cargo enter this inventory.
     */
    public synchronized boolean withdrawFuelCanisters(String modelId,int count){
        AirPowerCatalog.Airframe model=AirPowerCatalog.get(modelId);
        if(model==null||model.faction()!=faction||count<1||count>20)return false;
        int stored=stock.getOrDefault(model.fuelItem(),0);
        if(stored<count)return false;
        if(stored==count)stock.remove(model.fuelItem());
        else stock.put(model.fuelItem(),stored-count);
        return true;
    }
    public synchronized Snapshot snapshot(){
        return new Snapshot(Map.copyOf(stock),reservedEnergyUnits,job);
    }
    /** Trusted utility-grid transfer only. Energy measured as power-units * hours. */
    public synchronized void depositMeteredEnergy(double energyUnits){
        if(!Double.isFinite(energyUnits)||energyUnits<0||
           reservedEnergyUnits+energyUnits>1_000_000)
            throw new IllegalArgumentException("Invalid utility grant");
        reservedEnergyUnits+=energyUnits;
    }
    /** Select one of the nine named airframes, not a client-authored output. */
    public synchronized boolean begin(String modelId){
        AirPowerCatalog.Airframe plane=AirPowerCatalog.get(modelId);
        return plane!=null&&plane.faction()==faction&&beginProcess(plane.recipeId());
    }
    /** Faction-owned Incursion job: fuel, component OR finished airframe. */
    public synchronized boolean beginProcess(String processId){
        if(processId==null||job!=null)return false;
        var recipe=CraftingEraRegistry.process(processId);
        if(recipe==null||!recipe.facility().equals(factoryId)||
           !commissionedFacilities.contains(recipe.facility())||
           recipe.era()!=CraftingEraRegistry.Era.INCURSION||
           recipe.faction()!=switch(faction){
               case THE_UNRAVELED -> CraftingEraRegistry.Faction.UNRAVELED;
               case THE_PENITENT -> CraftingEraRegistry.Faction.PENITENT;
               case THE_BEGOTTEN -> CraftingEraRegistry.Faction.BEGOTTEN;
           })return false;
        for(var part:recipe.inputs().entrySet())
            if(stock.getOrDefault(part.getKey(),0)<part.getValue())return false;
        for(var part:recipe.inputs().entrySet()){
            int left=stock.get(part.getKey())-part.getValue();
            if(left==0)stock.remove(part.getKey());
            else stock.put(part.getKey(),left);
        }
        job=new Job(recipe.id(),0,recipe.seconds()*1000L,recipe.utilityDemand());
        return true;
    }
    /**
     * Tick must come from the authoritative server clock, not client delta.
     * Insufficient stored energy pauses production without creating progress.
     */
    public synchronized boolean advance(long milliseconds){
        if(milliseconds<1||milliseconds>60_000)
            throw new IllegalArgumentException("Untrusted airframe build tick");
        if(job==null)return false;
        long step=Math.min(milliseconds,job.requiredMs()-job.elapsedMs());
        double energyNeeded=job.utilityDemand()*step/3_600_000.0;
        if(reservedEnergyUnits+1e-9<energyNeeded)return false;
        reservedEnergyUnits=Math.max(0,reservedEnergyUnits-energyNeeded);
        long completed=job.elapsedMs()+step;
        if(completed==job.requiredMs()){
            var result=CraftingEraRegistry.process(job.recipeId());
            String item=result.output();
            stock.merge(item,result.quantity(),Math::addExact);
            job=null;
        }else job=new Job(job.recipeId(),completed,job.requiredMs(),job.utilityDemand());
        return true;
    }
    /**
     * The server may transfer exactly one paid finished airframe to an
     * aircraft hangar. This NEVER automatically instantiates a flight entity.
     */
    public synchronized boolean withdrawCompleted(String modelId){
        var spec=AirPowerCatalog.get(modelId);
        if(spec==null||spec.faction()!=faction)return false;
        String item=CraftingEraRegistry.process(spec.recipeId()).output();
        int count=stock.getOrDefault(item,0);
        if(count<1)return false;
        if(count==1)stock.remove(item);else stock.put(item,count-1);
        return true;
    }
}
