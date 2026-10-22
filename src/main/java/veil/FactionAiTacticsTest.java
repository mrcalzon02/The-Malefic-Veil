package veil;

import java.util.*;

/** Real host AI tactical regression: faction-safe shots and actual defense costs. */
public final class FactionAiTacticsTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static World.Npc approaching(ArenaSystems game,ArenaSystems.Player p){
        for(int a=0;a<32;a++){
            double radians=a*Math.PI/16;
            double nx=p.x+Math.cos(radians)*2.45;
            double ny=p.y+Math.sin(radians)*2.45;
            if(game.world.canWalk(nx,ny)&&game.world.lineClear(p.x,p.y,nx,ny))
                return new World.Npc(nx,ny);
        }
        throw new AssertionError("No physically reachable hostile NPC test location");
    }
    public static void main(String[] args)throws Exception{
        final long seed=20261021L;
        var civilian=new ArenaSystems(seed);
        civilian.world.npcs.clear();
        var nodes=new WorldHarvesting(civilian.world);
        var rules=HostAiSettings.fromOptions(List.of(
            "--ai-count=1","--ai-mode=SINGLE_FACTION",
            "--ai-team=1","--ai-level=6"));
        var director=new FactionAiDirector(civilian,nodes,rules);
        var bot=civilian.player(-10000);
        check(bot!=null&&bot.pistolClip>0,"Host civilian has ordinary limited ammunition");
        World.Npc intruder=approaching(civilian,bot);
        civilian.world.npcs.add(intruder);
        int originalAmmo=bot.pistolClip+bot.shellClip,startingHp=intruder.hp;
        for(int frame=0;frame<15;frame++){
            civilian.advance(200);
            nodes.advance(200);
            director.advance(200);
            if(intruder.hp<startingHp)break;
        }
        check(intruder.hp<startingHp,
            "Unraveled AI must defend against a real hostile target using server fire");
        check(bot.pistolClip+bot.shellClip<originalAmmo,
            "AI defense must spend physically carried ammunition");
        check(director.views().get(0).activity()==FactionAiDirector.Activity.DEFENDING,
            "AI host display must expose active defensive behavior");
        bot.hp=48;
        int remainingMedkits=bot.medkits;
        civilian.advance(200);nodes.advance(200);director.advance(200);
        check(bot.hp>=48&&bot.medkits==remainingMedkits-1,
            "AI can heal using a finite medkit, not manufactured health supplies");

        // Buildings are never AI-only counters: their actual world HP decides
        // whether the bot still owns working defensive infrastructure.
        var camp=new World.Device(bot.x+3,bot.y+3,ArenaSystems.CAMP_STAKE);
        camp.ownerKey=bot.accountKey;camp.ownerId=bot.id;
        civilian.world.devices.add(camp);
        civilian.advance(200);nodes.advance(200);director.advance(200);
        check(director.views().get(0).buildings()==1,
            "AI recognizes its physically persisted owned camp");
        camp.hp=0;
        civilian.advance(200);nodes.advance(200);director.advance(200);
        check(director.views().get(0).buildings()==0,
            "Destroyed camps must release the AI's genuine building state");

        // Wild monsters are always allied with the Begotten regardless of
        // what crew the Begotten character joins.
        var monsters=new ArenaSystems(seed+1);
        monsters.world.npcs.clear();
        var wild=new WorldHarvesting(monsters.world);
        var monsterBots=new FactionAiDirector(monsters,wild,
            HostAiSettings.fromOptions(List.of("--ai-count=1",
                "--ai-mode=SINGLE_FACTION","--ai-team=3","--ai-level=6")));
        var begotten=monsters.player(-10000);
        var friendly=approaching(monsters,begotten);
        monsters.world.npcs.add(friendly);
        begotten.angle=Math.atan2(friendly.y-begotten.y,friendly.x-begotten.x);
        int bullets=begotten.pistolClip;
        var missed=monsters.fireReport(begotten.id,0,1);
        check(missed.fired()&&friendly.hp==80,
            "Begotten shot must not damage allied world monsters");
        for(int frame=0;frame<8;frame++){
            monsters.advance(200);wild.advance(200);monsterBots.advance(200);
        }
        check(friendly.hp==80&&begotten.pistolClip==bullets-1,
            "Autonomous Begotten bots must never consume ammo attacking their own monsters");
        var friendlyTurret=new World.Device(friendly.x-1,friendly.y,ArenaSystems.TURRET);
        friendlyTurret.ownerKey=begotten.accountKey;friendlyTurret.ownerId=begotten.id;
        monsters.world.devices.add(friendlyTurret);
        monsters.advance(500);
        check(friendly.hp==80,"Begotten turrets must not kill allied creatures");
        System.out.println("PASS FactionAiTacticsTest: real ammunition, host combat, finite healing, destroyed base reconciliation, monster alliance");
    }
}
