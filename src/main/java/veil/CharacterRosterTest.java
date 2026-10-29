package veil;

import java.util.*;

/** Registry invariants without claiming that pending FBX models are renderable. */
public final class CharacterRosterTest {
    private static void check(boolean valid,String reason){if(!valid)throw new AssertionError(reason);}
    public static void main(String[] args){
        Set<String> ids=new HashSet<>();
        for(var model:CharacterRoster.all()){
            check(ids.add(model.id()),"Duplicate appearance identifier");
            check(model.title()!=null&&!model.title().isBlank(),"Missing display title");
            check(model.sourceRequirement()!=null&&!model.sourceRequirement().isBlank(),"Missing provenance gate");
            if(model.runtimeReady())check(model.sourceRequirement().equals("PROJECT_AUTHORED"),
                "Unverified archive model advertised as ready");
        }
        for(var faction:CharacterRoster.Faction.values())
            check(CharacterRoster.byFaction(faction).size()>=3,"Faction requires three planned appearances");
        check(CharacterRoster.all().stream().filter(CharacterRoster.Appearance::runtimeReady).count()==4,
            "Only four existing authored models may be runtime-ready");
        check(CharacterRoster.get("not-a-character")==null,"Unknown character should not resolve");
        System.out.println("PASS CharacterRosterTest: three factions, unique IDs and provenance readiness");
    }
}
