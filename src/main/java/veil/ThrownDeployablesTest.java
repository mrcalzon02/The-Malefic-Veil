package veil;

import java.lang.reflect.Field;
import java.util.*;

/** Independent thrown-equipment acceptance: factions, meshes, crafting, impact and harmless decay. */
public final class ThrownDeployablesTest {
    private ThrownDeployablesTest(){}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static String account(char ch){return "acct_"+String.valueOf(ch).repeat(64);}
    private static ArenaSystems ready(ThrownDeployables.Faction faction,char character){
        ArenaSystems host=new ArenaSystems(20261024);
        ArenaSystems.Player maker=host.join(1);
        check(host.assignAccount(1,account(character)),"Authentication for faction test");
        CharacterRoster.Faction selected=CharacterRoster.Faction.values()[faction.ordinal()];
        check(host.enrollFaction(1,selected),"Faction enrollment");
        // Legitimate raw resource acquisition; never mint a throwable directly.
        var blueprint=ThrownDeployables.catalog().get(faction.ordinal()*7);
        for(var input:blueprint.ingredients().entrySet())
            check(maker.survival.acquire(input.getKey(),input.getValue(),true),
                "Gathering test materials "+input.getKey());
        findSafeThrow(host);
        // Existing physical craft station is a manufacturing input, not the deployable.
        if(blueprint.station()!=SurvivalCrafting.Station.HAND){
            int station=blueprint.station()==SurvivalCrafting.Station.FIRE?
                ArenaSystems.FIELD_FIRE:ArenaSystems.FIELD_BENCH;
            World.Device facility=new World.Device(maker.x-1.0,maker.y,station);
            facility.ownerId=1;facility.ownerKey=maker.accountKey;
            host.world.devices.add(facility);
        }
        return host;
    }
    private static void findSafeThrow(ArenaSystems host){
        ArenaSystems.Player p=host.player(1);
        for(int x=4;x<World.SIZE-6;x++)for(int y=4;y<World.SIZE-6;y++){
            double px=x+.5,py=y+.5,aim=2.8;
            if(host.world.canWalk(px,py)&&host.world.canWalk(px-1.0,py)&&
               host.world.canWalk(px+aim,py)&&
               host.world.lineClear(px-1,py,px+aim,py)){
                p.x=px;p.y=py;p.angle=0;return;
            }
        }
        throw new AssertionError("No traversable field landing available");
    }
    public static void main(String[] args)throws Exception{
        check(ThrownDeployables.LIFETIME_MS==3_600_000L,"One-hour gameplay life changed");
        check(ThrownDeployables.catalog().size()==21,"Seven roles times three factions required");
        var models=ThrownDeployableMeshes.build();
        check(models.size()==22&&models.containsKey("thrown-harmless-burst"),
            "Missing field effect or faction mesh");
        Set<String> names=new HashSet<>();
        for(ThrownDeployables.Blueprint bp:ThrownDeployables.catalog()){
            check(names.add(bp.id()),"Duplicate field item identity");
            check(models.containsKey(bp.mesh())&&models.get(bp.mesh()).triangleCount()>30,
                "Model is not distinctive actual polygon geometry: "+bp.id());
            check(CraftingRecipeIndex.recipe("throw:"+bp.id())!=null,
                "Crafting dependency index lost "+bp.id());
            check(bp.item().name().equals("DEPLOY_"+bp.faction()+"_"+bp.role()),
                "Faction equipment inventory ordinal wrong");
            check(bp.ingredients().size()>=3,"Faction manufacture lacks staged materials");
        }
        for(ThrownDeployables.Faction faction:ThrownDeployables.Faction.values()){
            ArenaSystems host=ready(faction,(char)('a'+faction.ordinal()));
            ArenaSystems.Player maker=host.player(1);
            findSafeThrow(host);
            var bp=ThrownDeployables.catalog().get(faction.ordinal()*7);
            check(maker.survival.count(bp.item())==0,"Free pre-existing deployable kit");
            check(host.thrown.craft(1,0).ok(),"Faction crafting rejected "+faction);
            check(maker.survival.count(bp.item())==1,"Crafted item was not minted");
            check(!host.thrown.craft(1,0).ok(),"Crafting cooldown or materials bypass");
            check(host.world.devices.size()<=1,"Field item incorrectly became permanent building");
            check(host.thrown.throwKit(1,0).ok(),"Actual throw rejected "+faction);
            check(maker.survival.count(bp.item())==0,"Throw did not consume the physical kit");
            check(!host.thrown.throwKit(1,0).ok(),"Duplicate throw without item");
            check(host.thrown.views().size()==1&&host.thrown.views().get(0).flying(),
                "Throw failed to enter moving airborne state");
            host.advance(700);
            var landed=host.thrown.views().get(0);
            check(!landed.flying()&&landed.remainingMs()==ThrownDeployables.LIFETIME_MS,
                "One-hour countdown must begin on landing");
            check(host.thrown.occupied(landed.x(),landed.y()),"Wall is not solid cover");
            check(host.thrown.blocksLine(landed.x()-2,landed.y(),landed.x()+2,landed.y()),
                "Deployed wall must block line of sight");
            ArenaSystems.Player enemy=host.join(2);
            check(host.assignAccount(2,account('f')),"Attacker identity");
            check(host.enrollFaction(2,faction==ThrownDeployables.Faction.UNRAVELED?
                CharacterRoster.Faction.THE_PENITENT:CharacterRoster.Faction.THE_UNRAVELED),
                "Attacker faction registration failed");
            int oldHp=maker.hp,oldEnemy=enemy.hp;
            check(host.thrown.damage(2,landed.id(),500),"Enemy should destroy a deployed wall");
            host.advance(100);
            check(host.thrown.views().isEmpty(),"Destroyed field entity still blocking the world");
            check(host.thrown.drainBursts().size()==1&&host.thrown.drainRemoved().size()==1,
                "Harmless destruction burst or removal notification missing");
            check(maker.hp==oldHp&&enemy.hp==oldEnemy,
                "Removal explosion damaged a nearby player");
        }
        // Duration is measured by the host clock, not real-time sleep or client FPS.
        ArenaSystems host=ready(ThrownDeployables.Faction.BEGOTTEN,'d');
        findSafeThrow(host);
        check(host.thrown.craft(1,0).ok()&&host.thrown.throwKit(1,0).ok(),
            "Expiry setup failed");
        host.advance(700);
        int safeHp=host.player(1).hp;
        Field clock=ArenaSystems.class.getDeclaredField("clock");
        clock.setAccessible(true);
        clock.setLong(host,ThrownDeployables.LIFETIME_MS+701);
        host.thrown.advance(1);
        check(host.thrown.views().isEmpty()&&host.thrown.drainBursts().size()==1,
            "Expired one-hour kit must burst and vanish");
        check(host.player(1).hp==safeHp,"Expiration must never deal radial damage");
        System.out.println("ThrownDeployablesTest OK: 21 meshes, 3 economies, host flight, cover, damage and expiry");
    }
}
