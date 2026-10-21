package veil;

import java.util.*;

/** Registry and seeded encounter-casting invariants; no unlicensed runtime assets assumed. */
public final class PsxCastingAndLostTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        check(PsxCasting.all().size()==9,"All nine faction classes need one source audition");
        Set<String> sourcePaths=new HashSet<>(),ids=new HashSet<>();
        for(var candidate:PsxCasting.all()){
            check(FactionClasses.get(candidate.classId())!=null,"Unknown role "+candidate.classId());
            check(sourcePaths.add(candidate.sourcePath()),"Duplicate candidate FBX");
            check(candidate.triangles()>100&&!candidate.runtimeIntegrated(),"Unverified asset mislabeled live");
        }
        check(PsxCasting.forClass("begotten-devourer").sourceFile().equals("Character_Monster_08.fbx"),
            "Hunched model 08 is Devourer, not Mourner");
        check(LostNpcCatalog.all().size()==15,"Remaining Lost NPC source pool");
        for(var npc:LostNpcCatalog.all()){
            check(ids.add(npc.id()),"Duplicate NPC ID");
            check(sourcePaths.add(npc.sourceFile()),"Source reserved for both NPC and playable candidate");
        }
        check(LostNpcCatalog.all().stream().filter(v->!v.sourceTextureVerified()).count()==1,
            "Missing texture must remain an explicit source gate");
        for(var kind:ContinentPopulation.EncounterKind.values())
            check(!LostNpcCatalog.availableFor(kind).isEmpty(),"Encounter context missing NPC variant");
        var map=ContinentLayout.generate(20261020L);
        var population=new ContinentPopulation(map);
        var first=LostNpcCatalog.plan(map.seed(),population);
        var second=LostNpcCatalog.plan(map.seed(),population);
        check(first.equals(second),"Encounter picks must be repeatable");
        check(first.size()==population.npcSpawns().size(),"All markers assigned");
        for(var encounter:first){
            check(ids.contains(encounter.variantId()),"Lost encounter source not indexed");
            check(encounter.groupSize()>=1&&encounter.groupSize()<=3,"Group size must be bounded");
        }
        System.out.println("PASS PsxCastingAndLostTest: 9 source classes, 15 Lost variants, seeded NPC placements");
    }
}
