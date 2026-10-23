package veil;

import java.util.*;

/** Registry-only validation. Does not imply ArenaSystems executes weapon effects. */
public final class LostArsenalTest {
    private static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    public static void main(String[] args){
        check(LostArsenal.all().size()==25,"Expected 25 family-specific profiles");
        Set<String> ids=new HashSet<>();
        for(var weapon:LostArsenal.all()){
            check(ids.add(weapon.id()),"Duplicate ID");
            check(LostArsenal.get(weapon.id()).equals(weapon),"Lookup drift");
            check(!weapon.runtimeIntegrated(),"Design must not be claimed live");
            check(weapon.telegraph().length()>8&&weapon.counter().length()>8,
                "Weapon needs actual telegraph and counterplay");
            check(weapon.reach()>0&&weapon.windupSeconds()>0&&
                weapon.recoverySeconds()>0,"Invalid timing/range");
        }
        for(var kind:LostNpcCatalog.Kind.values()){
            var entries=LostArsenal.forKind(kind);
            check(entries.size()==5,"Every family requires five distinct tools");
            var slots=EnumSet.noneOf(LostArsenal.Slot.class);
            for(var e:entries)slots.add(e.slot());
            check(slots.size()==5,"Missing combat role for "+kind);
        }
        System.out.println("PASS LostArsenalTest: 25 scoped profiles, tells, counters and roles");
    }
}
