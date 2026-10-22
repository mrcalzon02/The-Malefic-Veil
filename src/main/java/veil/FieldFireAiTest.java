package veil;

import java.nio.file.*;
import java.util.*;

/** Host gameplay acceptance: real fire, water economy, AI restart and permissions. */
public final class FieldFireAiTest {
    private static void check(boolean valid,String why){
        if(!valid)throw new AssertionError(why);
    }
    private static boolean build(ArenaSystems arena,int id,int kind){
        for(int angle=0;angle<8;angle++){
            arena.player(id).angle=angle*Math.PI/4;
            if(arena.buildOutcome(id,kind).built())return true;
        }
        return false;
    }
    public static void main(String[] args)throws Exception{
        long seed=20261028L;
        var rules=HostAiSettings.fromOptions(List.of("--ai-count=1",
            "--ai-mode=SINGLE_FACTION","--ai-team=3","--ai-level=4"));
        ArenaSystems host=new ArenaSystems(seed);
        host.world.npcs.clear();
        var director=new FactionAiDirector(host,new WorldHarvesting(host.world),rules);
        var bot=host.player(-10000);
        check(bot!=null,"Host AI actor required");
        check(bot.survival.acquire(SurvivalCrafting.Item.TIMBER,5,false)&&
              bot.survival.acquire(SurvivalCrafting.Item.FIBER,4,false)&&
              bot.survival.acquire(SurvivalCrafting.Item.FIELD_STONE,2,false)&&
              bot.survival.acquire(SurvivalCrafting.Item.DIRTY_WATER,2,false),
            "Bounded host inventory acquisition");
        check(host.craftHand(bot.id,"tent"),"Actual tent recipe transaction");
        for(int i=0;i<25;i++)host.advance(200);
        check(host.craftHand(bot.id,"campfire"),"Actual campfire recipe transaction");
        check(!build(host,bot.id,ArenaSystems.FIELD_FIRE),
            "Cannot place fire without owned camp");
        check(bot.survival.count(SurvivalCrafting.Item.CAMPFIRE)==1,
            "Failed placement must not debit kit");
        check(!host.craftStation(bot.id,"boil-water"),
            "Carried fire kit does not authorize a FIRE recipe");
        check(build(host,bot.id,ArenaSystems.CAMP_STAKE),"Owner must found field camp");
        check(build(host,bot.id,ArenaSystems.FIELD_FIRE),"Field fire must be built from kit");
        check(bot.survival.count(SurvivalCrafting.Item.CAMPFIRE)==0,
            "Physical station consumes the fire kit");
        check(!build(host,bot.id,ArenaSystems.FIELD_FIRE),
            "No duplicate stations from one kit");
        for(int i=0;i<25;i++)host.advance(200);
        int dirty=bot.survival.count(SurvivalCrafting.Item.DIRTY_WATER);
        check(host.craftStation(bot.id,"boil-water"),"FIRE crafting at physical station");
        check(bot.survival.count(SurvivalCrafting.Item.DIRTY_WATER)==dirty-1&&
              bot.survival.count(SurvivalCrafting.Item.CLEAN_WATER)==1,
            "Water recipe consumes input and yields real clean water");
        check(!host.craftStation(bot.id,"tent"),"Wrong station recipe must fail");
        var enemy=host.join(42);
        enemy.x=bot.x;enemy.y=bot.y;
        check(host.assignAccount(enemy.id,"acct_"+("f".repeat(64)))&&
              host.enrollFaction(enemy.id,CharacterRoster.Faction.THE_UNRAVELED),
            "Hostile faction player identity");
        check(enemy.survival.acquire(SurvivalCrafting.Item.DIRTY_WATER,1,false),
            "Hostile carries actual ingredients");
        check(!host.craftStation(enemy.id,"boil-water")&&
              enemy.survival.count(SurvivalCrafting.Item.DIRTY_WATER)==1,
            "Enemy cannot use another faction's station or lose input");
        Path dir=Files.createTempDirectory("veil-field-fire-");
        try{
            DurableRealm.save(host,dir);
            ArenaSystems next=new ArenaSystems(seed);
            next.world.npcs.clear();
            DurableRealm.load(next,dir);
            var resumed=new FactionAiDirector(next,new WorldHarvesting(next.world),rules);
            var ai=next.player(-10000);
            check(next.world.devices.stream().anyMatch(d->d.hp>0&&
                d.kind==ArenaSystems.FIELD_FIRE&&ai.accountKey.equals(d.ownerKey)),
                "Durable restart preserves physical station and owner identity");
            check(resumed.views().get(0).buildings()>=2,
                "AI recognizes its saved camp infrastructure");
            check(ai.survival.acquire(SurvivalCrafting.Item.DIRTY_WATER,1,false),
                "Real water input after restart");
            for(int i=0;i<12;i++){
                next.advance(200);
                resumed.advance(200);
            }
            check(ai.survival.count(SurvivalCrafting.Item.CLEAN_WATER)>0,
                "Autonomous AI must purify water at recovered physical station");
        }finally{
            try(var walk=Files.walk(dir)){
                walk.sorted(Comparator.reverseOrder()).forEach(path->{
                    try{Files.deleteIfExists(path);}
                    catch(Exception e){throw new IllegalStateException(e);}
                });
            }
        }
        System.out.println("PASS FieldFireAiTest: physical AI FIRE workstation, owner permissions, "+
            "crafting debit, durable restart and autonomous water processing");
    }
}
