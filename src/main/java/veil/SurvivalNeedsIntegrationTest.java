package veil;

import java.util.Map;

/** Actual host ticking, direct need damage, food/water recovery and shared death
 * lifecycle. Functional gameplay regression; no screenshot assertions. */
public final class SurvivalNeedsIntegrationTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private static void tick(ArenaSystems game,int seconds){
        for(int i=0;i<seconds;i++)game.advance(1000);
    }
    private static void vitals(ArenaSystems.Player p,int hunger,int hydration){
        p.survival.restoreStatus(new SurvivalCrafting.Status(hunger,hydration,
            p.survival.inventory()));
    }
    public static void main(String[] args){
        ArenaSystems game=new ArenaSystems(20261026L,false);
        ArenaSystems.Player p=game.join(1);
        game.world.npcs.clear();
        p.hp=30;
        vitals(p,75,0);
        // Health may not increase merely because no monster is currently near.
        tick(game,4);
        check(p.hp==30,"Passive healing bypassed severe dehydration");
        tick(game,1);
        check(p.hp==27,"Dehydration must debit 3 HP per five host seconds");
        check(!p.dead,"One deprivation tick should not instantly kill the player");
        check(p.survival.acquire(SurvivalCrafting.Item.CLEAN_WATER,1,false),
            "A player could not legitimately carry drinkable water");
        check(game.consumeSurvival(1,SurvivalCrafting.Item.CLEAN_WATER),
            "Authorized drinking failed to restore hydration");
        check(p.survival.status().hydration()>0&&
            p.survival.count(SurvivalCrafting.Item.CLEAN_WATER)==0,
            "Water was not debited on recovery");
        tick(game,5);
        check(p.hp>27,"Natural regeneration failed to resume after drinking");

        // A player who becomes dehydrated late in a world-clock period must
        // receive five seconds of THEIR OWN unmet-need time before injury.
        ArenaSystems staggered=new ArenaSystems(20261029L,false);
        tick(staggered,4);
        ArenaSystems.Player late=staggered.join(2);
        staggered.world.npcs.clear();
        late.hp=30;
        vitals(late,0,0);
        tick(staggered,1); // world reaches five seconds, actor has only one
        check(late.hp==30,"Global clock caused premature injury to late joiner");
        tick(staggered,4);
        check(late.hp==25,"Late joiner did not receive one combined need tick");

        // Fully replenishing both needs clears accrued exposure. A later
        // depletion begins a new five-second period, not the old remainder.
        ArenaSystems recovered=new ArenaSystems(20261030L,false);
        ArenaSystems.Player eater=recovered.join(3);
        recovered.world.npcs.clear();
        eater.hp=30;
        vitals(eater,100,0);
        tick(recovered,4);
        check(eater.hp==30,"Injury occurred before first five-second interval");
        check(eater.survival.acquire(SurvivalCrafting.Item.CLEAN_WATER,1,false),
            "Recovery water unavailable");
        check(recovered.consumeSurvival(3,SurvivalCrafting.Item.CLEAN_WATER),
            "Hydration recovery was rejected");
        tick(recovered,2); // permits the host to observe fully recovered needs
        eater.hp=30;
        vitals(eater,100,0);
        tick(recovered,4); // crosses world-clock ten-second boundary
        check(eater.hp==30,"Old global exposure damaged newly dehydrated actor");
        tick(recovered,1);
        check(eater.hp==27,"New dehydration period did not apply exactly once");

        // Hunger and thirst stack, but safe arrival protection still prevents
        // repeated instant deaths while the player gets their bearings.
        ArenaSystems protectedGame=new ArenaSystems(20261027L,true);
        ArenaSystems.Player protectedPlayer=protectedGame.join(1);
        protectedGame.world.npcs.clear();
        vitals(protectedPlayer,0,0);
        protectedPlayer.hp=12;
        check(protectedGame.beginLoading(1),"Real connection protection unavailable");
        tick(protectedGame,5);
        check(protectedPlayer.hp==12,"Need damage breached login orientation shield");
        tick(protectedGame,5);
        check(protectedPlayer.hp==7,"Both unmet needs should stack to five HP after grace");
        check(protectedPlayer.survival.acquire(SurvivalCrafting.Item.EDIBLES,1,false),
            "Could not gather food while starving");
        check(protectedGame.consumeSurvival(1,SurvivalCrafting.Item.EDIBLES),
            "Eating did not resolve hunger");
        tick(protectedGame,5);
        check(protectedPlayer.hp==4,"After food, only thirst should cause damage");

        ArenaSystems deadly=new ArenaSystems(20261028L,true);
        ArenaSystems.Player victim=deadly.join(1);
        deadly.world.npcs.clear();
        vitals(victim,0,0);
        victim.hp=4;
        check(victim.survival.acquire(SurvivalCrafting.Item.TIMBER,2,false),
            "Physical carried resources unavailable before lethal hazard");
        check(deadly.arsenal.view(1).owned()!=0,"Current starter arsenal unexpectedly absent");
        tick(deadly,5);
        check(victim.dead&&victim.hp==0,"Severe deprivation did not cause real death");
        check(deadly.arsenal.view(1).owned()==0&&
            victim.survival.inventory().isEmpty(),
            "Starvation death failed to use total physical inventory loss");
        tick(deadly,2);
        check(deadly.respawn(1,0),"Player trapped in permanent dead state");
        check(!victim.dead&&victim.hp==100&&deadly.arsenal.view(1).owned()==0,
            "Respawn from deprivation granted free weapon or left player dead");
        System.out.println("PASS SurvivalNeedsIntegrationTest: per-player need cadence, real damage, authorized recovery, protection and inventory-safe death");
    }
}
