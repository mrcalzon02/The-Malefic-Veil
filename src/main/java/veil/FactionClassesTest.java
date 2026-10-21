package veil;

import java.util.*;

/** Verify the faction class contract without claiming locked classes are implemented. */
public final class FactionClassesTest {
    private static void check(boolean x,String m){if(!x)throw new AssertionError(m);}
    public static void main(String[] args){
        Set<String> ids=new HashSet<>(),abilities=new HashSet<>();
        check(FactionClasses.all().size()==9,"Exactly nine faction classes");
        for(var faction:CharacterRoster.Faction.values()){
            var classes=FactionClasses.faction(faction);
            check(classes.size()==3,"Exactly three classes per faction");
            for(var role:classes){
                check(role.faction()==faction,"Faction leak");
                check(ids.add(role.id()),"Duplicate stable ID");
                if(role.playable())check(abilities.add(String.valueOf(role.existingAbilityId())),"Duplicate implemented ability");
                else check(role.existingAbilityId()==-1,"Unimplemented role must not map to a power");
            }
        }
        check(abilities.equals(Set.of("0","1","2","3")),"Only established four abilities permitted");
        check(FactionClasses.get("missing")==null,"Unknown class should be denied");
        System.out.println("PASS FactionClassesTest: nine exclusive classes, honest playable gates");
    }
}
