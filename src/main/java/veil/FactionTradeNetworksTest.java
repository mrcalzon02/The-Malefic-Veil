package veil;

import java.util.*;

/** Source-contract gates: three factions have materially different cooperation graphs. */
public final class FactionTradeNetworksTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        FactionTradeNetworks.validate();
        Set<FactionTradeNetworks.Method> methods=EnumSet.noneOf(FactionTradeNetworks.Method.class);
        Set<String> allProducts=new HashSet<>();
        for(var faction:CharacterRoster.Faction.values()){
            var chain=FactionTradeNetworks.stages(faction);
            check(chain.size()>=5,"endgame has actual staged production for "+faction);
            Set<FactionTradeNetworks.Specialty> roles=EnumSet.noneOf(FactionTradeNetworks.Specialty.class);
            for(var stage:chain){
                roles.add(stage.specialty());methods.add(stage.method());
                check(allProducts.add(stage.product()),"different faction item outputs");
                check(stage.ingredients().values().stream().allMatch(n->n>0),
                    "no free goods");
                check(stage.minutes()>0,"every stage takes time");
            }
            check(roles.size()>=4,"multiple production specializations");
            var finalStage=chain.get(chain.size()-1);
            Map<String,String> oneBase=new HashMap<>();
            for(var stage:chain)oneBase.put(stage.id(),"base_alpha");
            var denied=FactionTradeNetworks.reviewFinalBatch(faction,
                finalStage.product(),oneBase,4,true);
            check(!denied.allowed()&&denied.reason().equals("COOPERATION_REQUIRED"),
                "one base cannot satisfy max-endgame production");
            Map<String,String> threeBases=new HashMap<>();
            for(int i=0;i<chain.size();i++)
                threeBases.put(chain.get(i).id(),
                    i%3==0?"base_alpha":i%3==1?"base_beta":"base_gamma");
            var eligible=FactionTradeNetworks.reviewFinalBatch(faction,
                finalStage.product(),threeBases,4,true);
            check(eligible.allowed(),"multi-base supply is eligible for future host validation");
            denied=FactionTradeNetworks.reviewFinalBatch(faction,
                finalStage.product(),threeBases,4,false);
            check(!denied.allowed()&&denied.reason().equals("EXPIRED_OR_MISSING_CARGO"),
                "spoiled or missing goods cannot be counted as fresh");
            denied=FactionTradeNetworks.reviewFinalBatch(faction,
                finalStage.product(),threeBases,0,true);
            check(!denied.allowed()&&denied.reason().equals("INSUFFICIENT_OPERATORS"),
                "timed joint ritual/production stages need operators");
        }
        check(methods.size()==3,"three different economic stage mechanics");
        var human=FactionTradeNetworks.stages(CharacterRoster.Faction.THE_UNRAVELED);
        check(human.stream().allMatch(s->s.method()==FactionTradeNetworks.Method.FREIGHT_ASSEMBLY),
            "industrial faction has actual freight assembly");
        var cult=FactionTradeNetworks.stages(CharacterRoster.Faction.THE_PENITENT);
        check(cult.stream().anyMatch(s->s.simultaneousOperators()>=3&&s.expiresMinutes()>0),
            "ritual faction uses multiplayer synchronized ephemeral goods");
        var monster=FactionTradeNetworks.stages(CharacterRoster.Faction.THE_BEGOTTEN);
        check(monster.stream().filter(s->s.expiresMinutes()>0).count()>=4&&
            monster.stream().anyMatch(s->s.minutes()>=180),
            "bio-faction requires grown perishable stock rather than instant recipes");
        System.out.println("PASS FactionTradeNetworksTest: three distinct economic graphs, stages, cooperation receipts");
    }
}
