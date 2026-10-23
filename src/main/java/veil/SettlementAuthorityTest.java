package veil;

import java.util.*;

/** Policy-only gate. Actual wilderness site and physical construction still gated. */
public final class SettlementAuthorityTest {
    private static void check(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    private static String account(char hex){
        return "acct_"+String.valueOf(hex).repeat(64);
    }
    private static SettlementAuthority.Identity identity(String id,char owner,
                   CharacterRoster.Faction faction,int level){
        return new SettlementAuthority.Identity(id,account(owner),faction,level);
    }
    public static void main(String[] args){
        Map<String,SettlementAuthority.Identity> directory=new HashMap<>();
        var unraveled=CharacterRoster.Faction.THE_UNRAVELED;
        var penitent=CharacterRoster.Faction.THE_PENITENT;
        directory.put("char_founder",identity("char_founder",'a',unraveled,1));
        directory.put("char_helper",identity("char_helper",'b',unraveled,50));
        directory.put("char_enemy",identity("char_enemy",'c',penitent,50));
        SettlementAuthority base=new SettlementAuthority(directory::get);
        check(base.registerVerifiedClaim("base_one","char_founder"),"trusted charter may form");
        check(!base.registerVerifiedClaim("base_two","char_founder"),"only one charter per character");
        check(!base.registerVerifiedClaim("base_one","char_helper"),"claim IDs unique");
        long version=base.charter("base_one").revision();
        var refused=base.reviewPlacement("base_one","char_helper","field-shelter",version);
        check(!refused.accepted()&&refused.reason().equals("BUILD_NOT_AUTHORIZED"),
            "allied high-level helper cannot build by default");
        refused=base.reviewPlacement("base_one","char_enemy","field-shelter",version);
        check(!refused.accepted()&&refused.reason().equals("FACTION_DENIED"),
            "enemy faction cannot build");
        for(String foreign:List.of("penitent-votive-altar","begotten-brood-nest")){
            refused=base.reviewPlacement("base_one","char_founder",foreign,version);
            check(!refused.accepted()&&refused.reason().equals("BLUEPRINT_FACTION_LOCK"),
                "Host approved foreign factory "+foreign);
            refused=base.recordPlaced("base_one","char_founder",
                "foreign_"+foreign,foreign,version);
            check(!refused.accepted()&&base.charter("base_one").revision()==version,
                "Host recorded foreign factory "+foreign);
            refused=base.setBuilderGrant("base_one","char_founder","char_helper",
                Set.of(foreign),version);
            check(!refused.accepted()&&refused.reason().equals("FACTION_BLUEPRINT_GRANT"),
                "Host granted an unauthorized foreign blueprint "+foreign);
        }
        check(base.reviewPlacement("base_one","char_founder",
            "unraveled-scrap-outpost",version).accepted(),
            "Owner cannot access its own faction industrial site");
        check(base.reviewPlacement("base_one","char_founder",
            "field-shelter",version).accepted(),
            "Common wilderness infrastructure must remain shared");
        var grant=base.setBuilderGrant("base_one","char_founder","char_helper",
            Set.of("field-shelter"),version);
        check(grant.accepted(),"founder can grant precisely one blueprint");
        version=grant.revision();
        check(base.reviewPlacement("base_one","char_helper","field-shelter",version).accepted(),
            "authorized helper can place approved structure");
        refused=base.reviewPlacement("base_one","char_helper","aa-heavy",version);
        check(!refused.accepted()&&refused.reason().equals("BUILD_NOT_AUTHORIZED"),
            "approved shelter does not allow forbidden defense");
        refused=base.setBuilderGrant("base_one","char_helper","char_enemy",
            Set.of("aa-heavy"),version);
        check(!refused.accepted()&&refused.reason().equals("NOT_FOUNDER"),
            "crew helper cannot delegate charter authority");
        for(int i=0;i<4;i++){
            var r=base.recordPlaced("base_one","char_helper","building_"+i,
                "field-shelter",version);
            check(r.accepted(),"four starter facilities may be placed");
            version=r.revision();
        }
        var repeated=base.recordPlaced("base_one","char_helper","building_0",
            "field-shelter",version-1);
        check(repeated.accepted()&&repeated.reason().equals("ALREADY_RECORDED")&&
            repeated.revision()==version,"retry with stale revision is idempotent");
        refused=base.recordPlaced("base_one","char_helper","building_extra",
            "field-shelter",version);
        check(!refused.accepted()&&refused.reason().equals("OWNER_LEVEL_CAP"),
            "level-50 helper cannot increase level-1 founder budget");
        check(base.charter("base_one").structures().size()==4,"rejected build did not mutate");
        var tower=base.recordPlaced("base_one","char_founder","tower_fast","aa-fast",version);
        check(tower.accepted(),"one early defense capacity slot exists independent of tech");
        version=tower.revision();
        refused=base.recordPlaced("base_one","char_founder","tower_heavy","aa-heavy",version);
        check(!refused.accepted()&&refused.reason().equals("OWNER_LEVEL_CAP"),
            "fast and heavy towers share defense capacity");
        directory.put("char_founder",identity("char_founder",'a',unraveled,20));
        check(base.reviewPlacement("base_one","char_founder","aa-heavy",version).accepted(),
            "founder leveling increases allowed defenses without new charter");
        var upgraded=base.recordPlaced("base_one","char_founder","tower_heavy","aa-heavy",version);
        check(upgraded.accepted(),"defense unlock uses founder level dynamically");
        version=upgraded.revision();
        var saved=base.snapshot();
        SettlementAuthority restored=new SettlementAuthority(directory::get);
        restored.restore(saved);
        var clean=restored.snapshot();
        var charter=saved.charters().get(0);
        Map<String,String> injected=new TreeMap<>(charter.structures());
        injected.put("foreign_saved_site","penitent-reliquary");
        var forged=new SettlementAuthority.Charter(charter.id(),
            charter.founderCharacterId(),charter.founderAccountKey(),
            charter.faction(),charter.revision(),charter.builderGrants(),injected);
        try{
            restored.restore(new SettlementAuthority.Snapshot(List.of(forged)));
            throw new AssertionError("Foreign factory injected through saved charter");
        }catch(IllegalArgumentException expected){}
        check(clean.equals(restored.snapshot()),"Rejected save mutated trusted settlement");
        check(saved.equals(restored.snapshot()),"round-trip charter and builder ACL snapshot");
        directory.put("char_founder",identity("char_founder",'a',unraveled,1));
        var reduced=restored.reviewPlacement("base_one","char_founder","aa-fast",version);
        check(!reduced.accepted()&&reduced.reason().equals("OWNER_LEVEL_CAP"),
            "hardcore rank reset blocks capacity without destroying previous towers");
        check(restored.charter("base_one").structures().size()==6,
            "hardcore rank reset never deletes or refunds structures");
        refused=restored.setBuilderGrant("base_one","char_founder","char_enemy",
            Set.of("field-shelter"),version);
        check(!refused.accepted()&&refused.reason().equals("TARGET_NOT_ELIGIBLE"),
            "no cross-faction construction grants");
        var transfer=restored.transferFounder("base_one","char_founder","char_helper",version);
        check(transfer.accepted(),"founder can transfer ownership explicitly");
        check(restored.charter("base_one").founderCharacterId().equals("char_helper"),
            "transfer actually changes controlling character");
        refused=restored.reviewPlacement("base_one","char_founder","field-shelter",transfer.revision());
        check(!refused.accepted()&&refused.reason().equals("BUILD_NOT_AUTHORIZED"),
            "old founder has no automatic building rights after transfer");
        check(restored.reviewPlacement("base_one","char_helper","field-shelter",transfer.revision()).accepted(),
            "new founder level controls available capacity");
        long before=restored.charter("base_one").revision();
        var err=restored.recordPlaced("base_one","char_helper","building_0",
            "aa-heavy",before);
        check(!err.accepted()&&err.reason().equals("STRUCTURE_CONFLICT")&&
            restored.charter("base_one").revision()==before,
            "reused structure identity cannot change type");
        check(Set.of("aa-fast","aa-heavy").stream().allMatch(
            k->SettlementAuthority.blueprintKinds().get(k)==SettlementAuthority.Kind.DEFENSE),
            "anti-air categories are hardcoded server-side");
        System.out.println("PASS SettlementAuthorityTest: ACL, level caps, faction isolation, two AA slots, transfer, restore, hardcore reset");
    }
}
