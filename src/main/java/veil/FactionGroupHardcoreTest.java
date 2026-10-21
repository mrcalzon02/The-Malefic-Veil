package veil;

import java.nio.file.*;
import java.util.*;

/** Verify same-faction allegiance is absolute despite separate crews and hardcore resets. */
public final class FactionGroupHardcoreTest {
    private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    private static String key(char ch){return "acct_"+String.valueOf(ch).repeat(64);}
    private static CharacterProgression.Snapshot skills(){
        EnumMap<CharacterProgression.Skill,Long> xp=new EnumMap<>(CharacterProgression.Skill.class);
        for(var skill:CharacterProgression.Skill.values())xp.put(skill,9000L);
        return new CharacterProgression.Snapshot(Map.copyOf(xp),3_600_000L);
    }
    public static void main(String[] args)throws Exception{
        String civilianA=key('a'),civilianB=key('b'),civilianC=key('c');
        String killer=key('d'),monster=key('e');
        FactionGroups crews=new FactionGroups();
        check(crews.enroll(civilianA,CharacterRoster.Faction.THE_UNRAVELED),"Civilian enroll");
        check(crews.enroll(civilianB,CharacterRoster.Faction.THE_UNRAVELED),"Other civilian enroll");
        check(crews.enroll(civilianC,CharacterRoster.Faction.THE_UNRAVELED),"Third civilian enroll");
        check(crews.enroll(killer,CharacterRoster.Faction.THE_PENITENT),"Killer enroll");
        check(crews.enroll(monster,CharacterRoster.Faction.THE_BEGOTTEN),"Monster enroll");
        check(!crews.enroll(killer,CharacterRoster.Faction.THE_BEGOTTEN),
            "One player switched from Killer to Monster faction");
        check(crews.allied(civilianA,civilianB)&&crews.allied(civilianB,civilianC),
            "Every civilian crew must be naturally allied");
        check(!crews.allied(civilianA,killer)&&!crews.allied(killer,monster),
            "Factional enemies were allied");
        FactionGroups.Crew first=crews.create(civilianA,"Ash Tenders");
        FactionGroups.Crew second=crews.create(civilianB,"Rust Refuge");
        check(first!=null&&second!=null&&!first.id().equals(second.id()),
            "Cannot establish multiple allied independent groups");
        check(!crews.invite(civilianA,killer,1000),"Cross-faction killer invitation accepted");
        check(crews.invite(civilianA,civilianC,1000),"Unraveled invite refused");
        check(crews.accept(civilianC,2000),"Invitation accepted");
        check(!crews.accept(civilianC,2000),"Duplicated group acceptance");
        check(crews.promote(civilianA,civilianC),"Founder promotes officer");
        check(!crews.remove(civilianC,civilianA),"Officer expelled founder");
        check(crews.transfer(civilianA,civilianC),"Explicit leadership transfer");
        check(crews.remove(civilianA,civilianA),"Old founder may leave after transfer");
        check(crews.crewFor(civilianA)==null&&crews.allied(civilianA,civilianC),
            "Leaving a group stripped permanent faction allegiance");
        FactionGroups restored=new FactionGroups();
        restored.restore(crews.snapshot());
        check(restored.allied(civilianA,civilianB)&&restored.crewFor(civilianC)!=null,
            "Faction/crew durable snapshot lost allies or membership");
        check(!restored.invite(civilianC,monster,3000),"Restored crew may invite monsters");

        ArenaSystems normal=new ArenaSystems(42L,false),hardcore=new ArenaSystems(42L,true);
        ArenaSystems.Player n=normal.join(1),h=hardcore.join(1);
        for(ArenaSystems.Player player:List.of(n,h)){
            player.progression.restore(skills());
            player.frags=18;player.cash=200;player.pistolClip=12;
            check(player.survival.acquire(SurvivalCrafting.Item.TIMBER,5,false),"Owned materials");
        }
        check(n.progression.overallLevel()>1&&h.progression.overallLevel()>1,
            "Grinding skill setup did not gain a rank");
        normal.damage(n,1000,0);hardcore.damage(h,1000,0);
        check(n.dead&&h.dead,"Neither simulated combat victim died");
        check(n.progression.overallLevel()>1&&n.progression.view().aliveMs()==0,
            "Normal mode should reset streak but retain earned skills");
        check(h.progression.overallLevel()==1&&h.progression.view().aliveMs()==0&&h.frags==0,
            "Hardcore must erase earned RPG levels and personal combat score");
        check(n.cash==0&&h.cash==0&&n.pistolClip==0&&h.pistolClip==0&&
            n.survival.inventory().isEmpty()&&h.survival.inventory().isEmpty(),
            "Death didn't remove all physical carried resources in both modes");

        ArenaSystems arena=new ArenaSystems(42L);
        var one=arena.join(10);var two=arena.join(11);var foe=arena.join(12);
        check(arena.assignAccount(10,civilianA)&&arena.assignAccount(11,civilianB)&&
            arena.assignAccount(12,killer),"Identity auth");
        check(arena.enrollFaction(10,CharacterRoster.Faction.THE_UNRAVELED)&&
            arena.enrollFaction(11,CharacterRoster.Faction.THE_UNRAVELED)&&
            arena.enrollFaction(12,CharacterRoster.Faction.THE_PENITENT),"Faction binding");
        check(arena.allied(10,11)&&!arena.allied(10,12),"Host faction-wide allegiance");
        arena.world.npcs.clear();
        one.x=1.5;one.y=1.5;one.angle=0;
        two.x=4.5;two.y=1.5;
        foe.x=6.5;foe.y=1.5;
        var shot=arena.fireReport(10,0,1);
        check(shot.fired()&&two.hp==100&&foe.hp==100,
            "Friendly player absorbed bullet without injury or shoot-through");
        arena.advance(300);
        two.x=110;two.y=110;foe.x=4.5;foe.y=1.5;
        shot=arena.fireReport(10,0,2);
        check(shot.fired()&&foe.hp<100,"Opposing killer must remain damageable");
        // A different Unraveled crew cannot be attacked through legacy base devices.
        World.Device alliedDevice=new World.Device(6.5,6.5,ArenaSystems.BARRICADE);
        alliedDevice.ownerKey=civilianB;alliedDevice.hp=100;
        arena.world.devices.add(alliedDevice);
        check(!arena.damageDevice(10,0,20,System.currentTimeMillis())&&alliedDevice.hp==100,
            "Friendly fire through another crew's base was accepted");
        Path temp=Files.createTempDirectory("veil-faction-hardcore-");
        try{
            HostRules.bind(temp,true);
            check(HostRules.bind(temp,true),"Persisted hardcore host mode");
            try{HostRules.bind(temp,false);throw new AssertionError("Hardcore downgrade accepted");}
            catch(java.io.IOException expected){}
            ArenaSystems persistent=new ArenaSystems(42L,true);
            var p=persistent.join(21);
            check(persistent.assignAccount(21,civilianA)&&
                persistent.enrollFaction(21,CharacterRoster.Faction.THE_UNRAVELED),
                "Stored player enrollment");
            persistent.factions.restore(crews.snapshot());
            p.progression.restore(skills());
            AccountDirectory.save(persistent,temp);
            ArenaSystems newSession=new ArenaSystems(42L,true);
            check(AccountDirectory.load(newSession,temp),"Account metadata loaded");
            check(newSession.factions.allied(civilianA,civilianB)&&
                newSession.factions.crewFor(civilianC)!=null,
                "Faction and crews lost on dedicated host restart");
            var reconnect=newSession.join(21);
            check(newSession.assignAccount(21,civilianA)&&
                reconnect.progression.overallLevel()>1,
                "Character skill data did not survive new hosting session");
            newSession.damage(reconnect,1000,0);
            AccountDirectory.save(newSession,temp);
            ArenaSystems afterDeath=new ArenaSystems(42L,true);
            check(AccountDirectory.load(afterDeath,temp),"Post-death directory load");
            afterDeath.join(21);check(afterDeath.assignAccount(21,civilianA),
                "Known account lost after death");
            check(afterDeath.player(21).progression.overallLevel()==1,
                "Hardcore death can be undone with reconnect/restart");
            check(afterDeath.factions.allied(civilianA,civilianC),
                "Character permadeath incorrectly destroyed civilization allegiance");
        }finally{
            try(var stream=Files.walk(temp)){
                stream.sorted(Comparator.reverseOrder()).forEach(p->{
                    try{Files.deleteIfExists(p);}catch(java.io.IOException ignored){}
                });
            }
        }
        System.out.println("PASS FactionGroupHardcoreTest: all same-faction crews allied, "+
            "friendly-fire disabled, hardcore total XP reset and durable host account continuity");
    }
}
