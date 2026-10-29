package veil;

import java.util.*;

/** Slow RPG, server handshake shield, E0 host crafting, threat indicators and no-death skips. */
public final class CharacterProgressionTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        CharacterProgression xp=new CharacterProgression();
        check(xp.overallLevel()==1&&xp.level(CharacterProgression.Skill.ENGINEERING)==1,
            "Starter levels should be one with zero earned XP");
        check(CharacterProgression.levelThreshold(2)>=500&&
            CharacterProgression.levelThreshold(10)>100_000,
            "RPG progression isn't progressively expensive");
        check(xp.reward(CharacterProgression.Skill.ENGINEERING,10,1000,5000)==10,
            "First verified work gets XP");
        check(xp.reward(CharacterProgression.Skill.ENGINEERING,100,1001,5000)==0,
            "Repeated action packet farmed skill XP");
        for(int tick=0;tick<1800;tick++)xp.aliveTick(1000);
        check(Math.abs(xp.multiplier()-1.1)<.0001,"Survival streak bonus at 30 minutes");
        check(xp.reward(CharacterProgression.Skill.SURVIVAL,20,6000,0)==22,
            "Host survival streak didn't increase actual XP");
        var normal=xp.snapshot();
        xp.died();
        check(xp.snapshot().aliveMs()==0&&xp.snapshot().experience().equals(normal.experience()),
            "Normal death erased permanent XP or kept streak");
        xp.restore(normal);
        xp.resetCharacter();
        check(xp.level(CharacterProgression.Skill.ENGINEERING)==1&&
            xp.snapshot().aliveMs()==0&&
            xp.snapshot().experience().values().stream().allMatch(value->value==0),
            "Hardcore death didn't clear all skills and streak");
        check(CharacterProgression.threat(10,4).threat()==CharacterProgression.Threat.FADED&&
            CharacterProgression.threat(10,4).colorHex().equals("#8DD8F5"),
            "Enemies 6 below must read as pale blue");
        check(CharacterProgression.threat(10,10).threat()==CharacterProgression.Threat.EQUAL&&
            CharacterProgression.threat(10,10).colorHex().equals("#66CF75"),
            "Equal level must be green");
        check(CharacterProgression.threat(10,13).threat()==CharacterProgression.Threat.DANGEROUS&&
            CharacterProgression.threat(10,13).colorHex().equals("#F3A33A"),
            "Enemies 1-5 above must be orange");
        check(CharacterProgression.threat(10,16).threat()==CharacterProgression.Threat.DEADLY&&
            CharacterProgression.threat(10,16).icon().equals("☠"),
            "Enemies 6 above require red skull marker");
        ArenaSystems arena=new ArenaSystems(20261020L,true);
        var player=arena.join(1);
        check(player.protectedUntil==0,"Pure arena player has a fake network loading shield");
        check(arena.beginLoading(1),"Actual network ready shield failed");
        check(player.protectedUntil-arena.time()==10_000,"Network loading needs 10s protection");
        arena.damage(player,50,0);
        check(player.hp==100,"Damage penetrated connection protection");
        check(arena.sceneReady(1),"Scene ready must arm short orientation grace");
        check(player.protectedUntil-arena.time()==6000,
            "Scene ready must leave ~6 seconds to orient in dangerous world");
        check(!arena.sceneReady(1),"Spam SCENE_READY extended immortal connection");
        arena.damage(player,40,0);
        check(player.hp==100,"Orientation protection penetrated");
        check(player.survival.acquire(SurvivalCrafting.Item.TIMBER,2,false)&&
            player.survival.acquire(SurvivalCrafting.Item.FIBER,4,false),
            "Authorized E0 material acquisition");
        check(arena.craftHand(1,"tent"),"Host denied honest primitive tent craft");
        check(player.survival.count(SurvivalCrafting.Item.TENT_KIT)==1&&
            player.survival.count(SurvivalCrafting.Item.FIBER)==0&&
            player.survival.count(SurvivalCrafting.Item.TIMBER)==0,
            "Crafting failed atomic material debit");
        check(player.progression.snapshot().experience().get(
            CharacterProgression.Skill.ENGINEERING)>0,
            "Real hand crafting did not award engineering XP");
        check(player.protectedUntil==0,"Crafting while invulnerable is exploit");
        arena.damage(player,1000,0);
        check(player.dead&&player.survival.inventory().isEmpty(),
            "Hardcore killing blow failed complete resource wipe");
        arena.advance(1000);arena.advance(200);
        check(arena.respawn(1,0),"Respawn requires real host delay");
        long grace=player.protectedUntil-arena.time();
        check(grace>=5000&&grace<=8000,"Random respawn grace out of requested [5s,8s]");
        check(player.pistolClip==0&&player.shellClip==0&&player.pistolReserve==0&&
            player.survival.inventory().isEmpty(),
            "Respawn illegally restored carried weapons/ammo/inventory");
        arena.damage(player,20,0);
        check(player.hp==100,"Safe postspawn grace not enforced");
        check(arena.ability(1),"Active class action should cancel postspawn grace");
        arena.damage(player,20,0);
        check(player.hp==80,"Player remained immortal after initiating an ability");
        System.out.println("PASS CharacterProgressionTest: 5 slow skills, life XP bonus, "+
            "hardcore wipe, 4 monster threat bands, active crafting, login and respawn grace");
    }
}
