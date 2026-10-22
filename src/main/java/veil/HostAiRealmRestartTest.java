package veil;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Dedicated host restart must preserve bot identities, crews and real carried resources. */
public final class HostAiRealmRestartTest {
    private static void check(boolean pass,String why){
        if(!pass)throw new AssertionError(why);
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("veil-ai-realm-");
        long seed=20261021L;
        var rules=HostAiSettings.fromOptions(List.of("--ai-count=3",
            "--ai-mode=SINGLE_FACTION","--ai-team=3","--ai-level=8"));
        String identity,crewId,owner;
        int fiber;
        try{
            try(LanServer first=new LanServer(0,seed,root,false,rules)){
                check(first.ai.views().size()==3,"Three actually spawned dedicated-host AI");
                var leader=first.match.player(-10000);
                identity=leader.accountKey;
                owner=first.match.factions.crewFor(identity).founder();
                crewId=first.match.factions.crewId(identity);
                check(first.match.factions.crewFor(identity).members().size()==3,
                    "Three AI belong to an authentic persisted group");
                check(leader.survival.acquire(SurvivalCrafting.Item.FIBER,22,false),
                    "Host-side character can carry honest survival material");
                fiber=leader.survival.count(SurvivalCrafting.Item.FIBER);
                first.saveNow();
            }
            String savedRules=Files.readString(root.resolve("ai-host-settings.properties"));
            try{
                new LanServer(0,seed,root,false,HostAiSettings.fromOptions(
                    List.of("--ai-count=3","--ai-mode=SINGLE_FACTION",
                            "--ai-team=2","--ai-level=8"))).close();
                throw new AssertionError("Reassigned existing AI faction identities on restart");
            }catch(IOException expected){
                check(expected.getMessage().contains("migration"),
                    "Changing existing AI configuration must be an explicit migration");
            }
            check(savedRules.equals(Files.readString(root.resolve("ai-host-settings.properties"))),
                "Rejected changes must leave stored host AI settings untouched");
            try(LanServer again=new LanServer(0,seed,root,false,rules)){
                check(again.ai.views().size()==3,"Bot count restored without replication");
                var leader=again.match.player(-10000);
                check(identity.equals(leader.accountKey),
                    "AI identity must survive host reboot without resetting bot account");
                check(fiber==leader.survival.count(SurvivalCrafting.Item.FIBER),
                    "AI survival cargo journal survives full dedicated host restart");
                var crew=again.match.factions.crewFor(identity);
                check(crew!=null&&crew.members().size()==3&&
                    crew.id().equals(crewId)&&crew.founder().equals(owner),
                    "A group of three keeps the same persistent ID and founder");
                check(again.ai.views().stream().allMatch(
                    v->v.faction()==CharacterRoster.Faction.THE_BEGOTTEN&&v.level()>=8),
                    "Existing Begotten team and selected starting levels persist");
            }
        }finally{
            try(var all=Files.walk(root)){
                all.sorted(Comparator.reverseOrder()).forEach(p->{
                    try{Files.deleteIfExists(p);}catch(IOException ex){
                        throw new UncheckedIOException(ex);
                    }
                });
            }
        }
        System.out.println("PASS HostAiRealmRestartTest: real dedicated realm bot identities, material inventory and three-member crews survive restart");
    }
}
