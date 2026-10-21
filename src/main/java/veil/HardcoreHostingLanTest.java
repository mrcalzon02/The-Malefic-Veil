package veil;

import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Actual socket, real account, hardcore hosting mode, clan enforcement and restart. */
public final class HardcoreHostingLanTest {
    private static void check(boolean valid,String why){if(!valid)throw new AssertionError(why);}
    private static void until(BooleanSupplier predicate,String reason)throws Exception{
        long until=System.nanoTime()+8_000_000_000L;
        while(System.nanoTime()<until){
            if(predicate.getAsBoolean())return;
            Thread.sleep(15);
        }
        throw new AssertionError("Timeout "+reason);
    }
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("veil-hardcore-socket-");
        String account;
        try{
            try(LanServer host=new LanServer(0,20261020L,dir,true);
                NetLink player=new NetLink("127.0.0.1",host.port())){
                until(()->player.accountKey()!=null,"hardware-backed authentication");
                until(player::hardcore,"explicit server hardcore announcement");
                account=player.accountKey();
                check(host.status().contains("mode=HARDCORE"),"Host failed operator-mode report");
                player.fire(0);
                until(()->player.factionResults.stream().anyMatch(s->
                    s.equals("FACTION_RESULT|REQUIRED")),"Unaffiliated attacker bypassed selection");
                player.factionResults.clear();
                player.selectFaction(CharacterRoster.Faction.THE_UNRAVELED);
                until(()->player.faction().equals("THE_UNRAVELED"),"Live faction selection");
                player.createCrew("Camp Survivors");
                until(()->player.crew().startsWith("crew_"),"Real crew creation");
                check(player.crewRole().equals("FOUNDER"),"Wrong crew ownership granted");
                player.sceneReady();
                ArenaSystems.Player character=host.match.player(player.id);
                until(()->character.ready,"Host received finished scene load");
                long shield=character.protectedUntil-host.match.time();
                check(shield>=5000&&shield<=6000,
                    "World loading shield not reduced to six seconds on readiness");
                host.match.damage(character,70,0);
                check(character.hp==100,"Connecting player was killed before bearings recovered");
                var gained=new EnumMap<CharacterProgression.Skill,Long>(CharacterProgression.Skill.class);
                for(var skill:CharacterProgression.Skill.values())gained.put(skill,12000L);
                character.progression.restore(new CharacterProgression.Snapshot(Map.copyOf(gained),3_600_000L));
                character.frags=4;
                character.protectedUntil=0;
                host.match.damage(character,1000,0);
                check(character.dead&&character.frags==0&&character.progression.overallLevel()==1,
                    "Hardcore death left XP or kill record intact");
                check(character.pistolClip==0&&character.pistolReserve==0,
                    "Hardcore death retains equipment");
                host.saveNow();
                check(Files.exists(dir.resolve("accounts.dat")),"Durable character records absent");
                check(Files.exists(dir.resolve("host-rules.dat")),"Server mode isn't persisted");
            }
            try(LanServer reopened=new LanServer(0,20261020L,dir,true)){
                check(reopened.match.hardcore,"Server silently downgraded mode");
                check(reopened.match.factions.faction(account)==CharacterRoster.Faction.THE_UNRAVELED,
                    "Reconnected hardcore player lost identity/faction");
                check(reopened.match.factions.crewFor(account)!=null,
                    "Crew disappeared on dedicated host restart");
                var recovered=reopened.match.skillsSnapshot().get(account);
                check(recovered!=null&&recovered.experience().values().stream().allMatch(v->v==0L),
                    "Hardcore death cheated by host save/restart");
            }
            try{
                new LanServer(0,20261020L,dir,false);
                throw new AssertionError("Host mode switch reset hardcore contract");
            }catch(java.io.IOException expected){}
            System.out.println("PASS HardcoreHostingLanTest: real socket mode "+
                "selection gate, protected load, faction crew, permadeath and durable restart");
        }finally{
            try(var stream=Files.walk(dir)){
                stream.sorted(Comparator.reverseOrder()).forEach(file->{
                    try{Files.deleteIfExists(file);}catch(java.io.IOException ignored){}
                });
            }
        }
    }
}
