package veil;

import java.util.*;

/**
 * Versioned design-data for three genuinely different faction trade graphs.
 * Stage outputs are symbolic stock receipts, NOT spawnable inventory. The
 * streamed economy must supply facility/worksite owners, inputs, transport,
 * elapsed production time and transactional item provenance.
 */
public final class FactionTradeNetworks {
    public enum Method { FREIGHT_ASSEMBLY, TIMED_RITUAL, LIVING_MATURATION }
    public enum Specialty {
        EXTRACTION, METALLURGY, ELECTRONICS, ASSEMBLY,
        OFFERINGS, RELIQUARIES, CHOIR, CONSECRATION,
        CULTIVATION, INOCULATION, GROWTH, SYMBIOSIS
    }
    public record Stage(String id,CharacterRoster.Faction faction,
                        Specialty specialty,Method method,
                        List<String> predecessors,Map<String,Integer> ingredients,
                        String product,int quantity,int minutes,int expiresMinutes,
                        int simultaneousOperators) {}
    public record Gate(boolean allowed,String reason) {}
    private static final Map<CharacterRoster.Faction,List<Stage>> CHAINS;
    private static Stage stage(String id,CharacterRoster.Faction faction,
            Specialty role,Method method,String parents,String product,
            int quantity,int minutes,int expiry,int operators,Object... inputs){
        Map<String,Integer> resources=new LinkedHashMap<>();
        if(inputs.length==0||inputs.length%2!=0)
            throw new IllegalArgumentException("Stages consume resources");
        for(int i=0;i<inputs.length;i+=2){
            String item=(String)inputs[i];int amount=(Integer)inputs[i+1];
            if(item==null||item.isBlank()||amount<=0||
               resources.putIfAbsent(item,amount)!=null)
                throw new IllegalArgumentException("Invalid ingredient");
        }
        return new Stage(id,faction,role,method,
            parents.isBlank()?List.of():List.of(parents.split(" ")),
            Map.copyOf(resources),product,quantity,minutes,expiry,operators);
    }
    static {
        CharacterRoster.Faction u=CharacterRoster.Faction.THE_UNRAVELED;
        CharacterRoster.Faction p=CharacterRoster.Faction.THE_PENITENT;
        CharacterRoster.Faction b=CharacterRoster.Faction.THE_BEGOTTEN;
        Method freight=Method.FREIGHT_ASSEMBLY,ritual=Method.TIMED_RITUAL,
               living=Method.LIVING_MATURATION;
        CHAINS=Map.of(
            u,List.of(
                stage("u-ore-survey",u,Specialty.EXTRACTION,freight,"",
                    "SORTED_ORE",2,18,0,1,"RAW_ORE",5,"FIREWOOD",2),
                stage("u-alloy-smelt",u,Specialty.METALLURGY,freight,
                    "u-ore-survey","HARDENED_ALLOY",1,65,0,1,
                    "SORTED_ORE",3,"CHARCOAL",3),
                stage("u-circuit-batch",u,Specialty.ELECTRONICS,freight,"",
                    "TRACKING_CIRCUIT",1,45,0,1,
                    "CONDUCTOR_WIRE",3,"MACHINE_PARTS",1),
                stage("u-feedline-machining",u,Specialty.ASSEMBLY,freight,
                    "u-alloy-smelt","PRECISION_FEEDLINE",1,55,0,1,
                    "HARDENED_ALLOY",2,"FASTENERS",4),
                stage("u-defense-ordnance",u,Specialty.ASSEMBLY,freight,
                    "u-feedline-machining u-circuit-batch","BALLISTIC_SUPPLY",2,80,0,1,
                    "PRECISION_FEEDLINE",1,"TRACKING_CIRCUIT",1,
                    "HARDENED_ALLOY",1)
            ),
            p,List.of(
                stage("p-incense-refining",p,Specialty.OFFERINGS,ritual,"",
                    "VOTIVE_INCENSE",2,30,180,1,
                    "MEDICINAL_HERBS",5,"CLEAN_WATER",2),
                stage("p-reliquary-seals",p,Specialty.RELIQUARIES,ritual,"",
                    "WITNESS_SEAL",1,50,0,2,
                    "METAL_BILLET",2,"DREAD_IMPRINT",1),
                stage("p-choir-attunement",p,Specialty.CHOIR,ritual,"",
                    "CHORUS_RESONANCE",1,25,90,3,
                    "MEDICINAL_HERBS",3,"DREAD_IMPRINT",1),
                stage("p-threefold-rite",p,Specialty.CONSECRATION,ritual,
                    "p-incense-refining p-reliquary-seals p-choir-attunement",
                    "BOUND_LITURGY",1,45,120,3,
                    "VOTIVE_INCENSE",1,"WITNESS_SEAL",1,
                    "CHORUS_RESONANCE",1),
                stage("p-judgment-ammunition",p,Specialty.CONSECRATION,ritual,
                    "p-threefold-rite","SACRED_SUPPLY",2,35,360,2,
                    "BOUND_LITURGY",1,"METAL_BILLET",2)
            ),
            b,List.of(
                stage("b-nutrient-crops",b,Specialty.CULTIVATION,living,"",
                    "GROWTH_FEED",3,70,300,1,
                    "EDIBLES",5,"CLEAN_WATER",3),
                stage("b-spore-culture",b,Specialty.INOCULATION,living,"",
                    "VIABLE_SPORES",2,120,240,1,
                    "MEDICINAL_HERBS",4,"CLEAN_WATER",2),
                stage("b-chitin-grow",b,Specialty.GROWTH,living,
                    "b-nutrient-crops b-spore-culture","GROWN_CHITIN",1,180,360,1,
                    "GROWTH_FEED",3,"VIABLE_SPORES",1),
                stage("b-symbiotic-resin",b,Specialty.SYMBIOSIS,living,
                    "b-spore-culture","BINDING_RESIN",1,100,300,1,
                    "VIABLE_SPORES",2,"RAW_ORE",2),
                stage("b-organic-ammunition",b,Specialty.SYMBIOSIS,living,
                    "b-chitin-grow b-symbiotic-resin","SPINE_SUPPLY",2,140,240,1,
                    "GROWN_CHITIN",1,"BINDING_RESIN",1,
                    "GROWTH_FEED",2)
            )
        );
        validate();
    }
    private FactionTradeNetworks(){}
    public static List<Stage> stages(CharacterRoster.Faction faction){
        return CHAINS.getOrDefault(faction,List.of());
    }
    public static Stage stageById(String id){
        if(id==null)return null;
        for(List<Stage> chain:CHAINS.values())for(Stage s:chain)
            if(s.id().equals(id))return s;
        return null;
    }
    public static void validate(){
        Set<Method> methods=EnumSet.noneOf(Method.class);
        Set<String> ids=new HashSet<>(),outputs=new HashSet<>();
        for(CharacterRoster.Faction faction:CharacterRoster.Faction.values()){
            List<Stage> stages=stages(faction);
            if(stages.size()<5)throw new IllegalStateException("Incomplete faction trade stages");
            Set<String> previousIds=new HashSet<>(),available=new HashSet<>();
            Set<Specialty> roles=EnumSet.noneOf(Specialty.class);
            for(Stage s:stages){
                if(s.faction()!=faction||!ids.add(s.id())||
                   !outputs.add(s.product())||s.quantity()<1||s.minutes()<1||
                   s.expiresMinutes()<0||s.simultaneousOperators()<1||
                   s.ingredients().isEmpty())
                    throw new IllegalStateException("Invalid faction recipe stage "+s.id());
                if(!previousIds.containsAll(s.predecessors()))
                    throw new IllegalStateException("Stage order/cycle for "+s.id());
                for(String predecessor:s.predecessors()){
                    Stage parent=stageById(predecessor);
                    if(parent==null||!s.ingredients().containsKey(parent.product()))
                        throw new IllegalStateException("Missing physical predecessor input "+s.id());
                }
                previousIds.add(s.id());
                available.add(s.product());roles.add(s.specialty());methods.add(s.method());
            }
            if(roles.size()<4)throw new IllegalStateException("No distributed specializations");
            if(stages.get(stages.size()-1).predecessors().size()<1)
                throw new IllegalStateException("Final product skips stage cooperation");
        }
        if(methods.size()!=3)throw new IllegalStateException("Faction networks are just recipe reskins");
    }
    /**
     * Planning gate for final endgame order. Proof of three independent
     * settlement contributions is by signed, consumed source receipts, NOT
     * by asking a client to claim it visited three bases.
     */
    public static Gate reviewFinalBatch(CharacterRoster.Faction faction,
            String product,Map<String,String> stageSettlementReceipts,
            int concurrentOperators,boolean shipmentFresh){
        List<Stage> chain=stages(faction);
        if(chain.isEmpty())return new Gate(false,"UNKNOWN_FACTION");
        Stage finalStage=chain.get(chain.size()-1);
        if(!finalStage.product().equals(product))return new Gate(false,"WRONG_OUTPUT");
        if(stageSettlementReceipts==null)return new Gate(false,"MISSING_PROVENANCE");
        Set<String> distinct=new HashSet<>();
        for(Stage stage:chain){
            String settlement=stageSettlementReceipts.get(stage.id());
            if(settlement==null||!settlement.matches("base_[A-Za-z0-9_-]{2,92}"))
                return new Gate(false,"MISSING_PROVENANCE");
            distinct.add(settlement);
        }
        if(distinct.size()<3)return new Gate(false,"COOPERATION_REQUIRED");
        if(concurrentOperators<finalStage.simultaneousOperators())
            return new Gate(false,"INSUFFICIENT_OPERATORS");
        if(!shipmentFresh)return new Gate(false,"EXPIRED_OR_MISSING_CARGO");
        return new Gate(true,"READY_FOR_HOST_VALIDATION");
    }
}
