package veil;

import java.util.*;

/**
 * Actual multiplayer-authoritative AI settlement cooperation, rather than
 * inventory/status counters or fabricated remote production.
 */
public final class FactionAiCooperationTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] ignored){
        ArenaSystems arena=new ArenaSystems(20261021L);
        arena.world.npcs.clear();
        WorldHarvesting resources=new WorldHarvesting(arena.world);
        var rules=HostAiSettings.fromOptions(List.of("--ai-count=3",
            "--ai-mode=SINGLE_FACTION","--ai-team=1","--ai-level=12"));
        FactionAiDirector director=new FactionAiDirector(arena,resources,rules);
        var leader=arena.player(-10000);
        var crew=arena.factions.crewFor(leader.accountKey);
        check(crew!=null&&crew.members().size()==3,
            "Exactly three authenticated same-faction group members");
        check(director.views().stream().allMatch(v->v.group().equals(crew.id())),
            "Every AI must reference its true host-authoritative crew");
        ArenaSystems.Player supplier=null;
        World.Device camp=null;
        for(int botId:new int[]{-10001,-10002}){
            var actor=arena.player(botId);
            double d=Math.hypot(actor.x-leader.x,actor.y-leader.y);
            check(d>1.7&&d<9,"Crew supply roles must deploy within walkable reach");
            check(arena.world.lineClear(actor.x,actor.y,leader.x,leader.y),
                "Spawned teammates must have a real physical path");
            actor.cash=300;actor.scrap=12;
            check(actor.survival.acquire(SurvivalCrafting.Item.TENT_KIT,1,false),
                "Test supplier starts with an item that is normally craftable");
            for(int direction=0;direction<8;direction++){
                actor.angle=direction*Math.PI/4;
                var result=arena.buildOutcome(botId,ArenaSystems.CAMP_STAKE);
                if(result.built()){
                    supplier=actor;
                    camp=arena.world.devices.get(result.deviceIndex());
                    break;
                }
            }
            if(supplier!=null)break;
        }
        check(camp!=null&&supplier!=null,
            "Suppliers must be able to establish a real field camp at crew deployment");
        check(camp.hp>0&&camp.ownerKey.equals(supplier.accountKey),
            "Independent supply base must be physically owned by its builder");
        check(supplier.survival.count(SurvivalCrafting.Item.TENT_KIT)==0,
            "Building a supply camp must consume its actual inventory material");
        check(supplier.survival.acquire(SurvivalCrafting.Item.FIBER,30,false),
            "Supply material obeys real player inventory weight");
        check(arena.factions.allied(supplier.accountKey,leader.accountKey),
            "Same faction crew must be allied independently of base ownership");
        int before=leader.survival.count(SurvivalCrafting.Item.FIBER);
        boolean delivered=false;
        for(int frame=0;frame<180;frame++){
            arena.advance(200);
            resources.advance(200);
            director.advance(200);
            if(leader.survival.count(SurvivalCrafting.Item.FIBER)>before&&
               supplier.survival.count(SurvivalCrafting.Item.FIBER)<30){
                delivered=true;break;
            }
        }
        check(delivered,
            "Cooperative AI must walk to crew leader and transfer REAL carried stock");
        check(supplier.survival.mass()>=0&&leader.survival.mass()<=200,
            "Cooperative logistics cannot overfill carrying capacity");
        check(arena.world.devices.contains(camp)&&camp.ownerKey.equals(supplier.accountKey),
            "Supply transaction cannot transfer or overwrite someone's base ownership");
        System.out.println("PASS FactionAiCooperationTest: physical supply camp, real movement, stock delivery, group cap=3");
    }
}
