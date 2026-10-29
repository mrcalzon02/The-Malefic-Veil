package veil;

import java.util.*;

/** Data contract gate for all nine planned kits; metadata is not gameplay execution. */
public final class ClassAbilityCatalogTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        Set<String> ids=new HashSet<>();
        check(ClassAbilityCatalog.all().size()==36,"Nine complete four-slot kits");
        for(var role:FactionClasses.all()){
            var kit=ClassAbilityCatalog.forClass(role.id());
            check(kit.size()==4,"Kit incomplete: "+role.id());
            check(EnumSet.copyOf(kit.stream().map(ClassAbilityCatalog.Skill::slot).toList()).size()==4,
                "Duplicate slots "+role.id());
            for(var skill:kit){
                check(ids.add(skill.id()),"Duplicate ability ID");
                check(skill.cooldownSeconds()>=0&&skill.durationSeconds()>=0,"Negative timing");
                check(skill.meterCost()==(skill.slot()==ClassAbilityCatalog.Slot.SIGNATURE?80:0),
                    "Signature meter contract");
                check(!skill.title().isBlank()&&!skill.summary().isBlank(),"Missing skill text");
            }
        }
        check(ClassAbilityCatalog.forClass("unknown").isEmpty(),"Unknown role should not grant a kit");
        System.out.println("PASS ClassAbilityCatalogTest: 36 unique skills, full slot sets, bounded costs");
    }
}
