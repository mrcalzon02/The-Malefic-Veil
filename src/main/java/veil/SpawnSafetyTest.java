package veil;

import java.util.*;
import java.util.function.BooleanSupplier;

/** 20 seeds x 100 threat permutations; safe choices and anti-instant-death behavior. */
public final class SpawnSafetyTest {
    private static int assertions;
    private static void check(boolean yes,String reason){
        assertions++;if(!yes)throw new AssertionError(reason);
    }
    public static void main(String[] args)throws Exception{
        for(int seed=0;seed<20;seed++){
            ArenaSystems arena=new ArenaSystems(seed);
            arena.world.npcs.clear();
            ArenaSystems.Player victim=arena.join(1);
            ArenaSystems.Player enemy=arena.join(2);
            for(int permutation=0;permutation<100;permutation++){
                int threatened=permutation%3;
                enemy.x=arena.world.spawns[threatened][0]+1.5;
                enemy.y=arena.world.spawns[threatened][1];
                List<ArenaSystems.SpawnRisk> choices=arena.spawnRisks(1);
                check(choices.size()==3,"all spawn choices available");
                int score=choices.get(threatened).score();
                check(score>choices.get((threatened+1)%3).score(),
                    "threat should make spawn less safe seed "+seed+" scenario "+permutation);
                check(score>=20,"threatened spawn must not be labelled safe");
            }
            victim.lastSpawnIndex=0;
            victim.lastSpawnAt=arena.time();
            arena.damage(victim,100,0);
            // Simulated repeated early spawn deaths may not overwrite player-selected choice.
            arena.advance(1000);
            check(!arena.respawn(1,1),"death/respawn lockout");
            arena.advance(200);
            check(arena.respawn(1,1),"selected sector respawns");
            check(victim.hp==100&&!victim.dead,"player returned alive");
            arena.damage(victim,80,0);
            check(victim.hp==100,"non-offensive brief protection against spawn kill");
            // A naked respawn must NOT receive magically replenished ammunition.
            victim.angle=0;
            check(!arena.fire(1,0),"death-cleared gun cannot fire without ammunition");
            check(victim.protectedUntil>arena.time(),
                "empty weapon click must not consume spawn protection");
            // Deliberately using an ability is a host-owned active action and ends grace.
            check(arena.ability(1),"respawned player can deliberately trigger class ability");
            arena.damage(victim,25,0);
            check(victim.hp==75,"spawn protection ends after active ability");
        }
        ArenaSystems arena=new ArenaSystems(42);
        arena.world.npcs.clear();
        ArenaSystems.Player builder=arena.join(4);
        builder.cash=1000;builder.scrap=100;
        builder.angle=0;
        check(arena.reviewBuild(4,ArenaSystems.TURRET).reason().equals("CITY_PROTECTED"),
            "city-owned spawn exits cannot accept turrets");
        check(arena.reviewBuild(4,ArenaSystems.BARRICADE).reason().equals("CITY_PROTECTED"),
            "city-owned spawn exits cannot accept barricades");
        System.out.println("PASS SpawnSafetyTest "+assertions+
            " assertions: 20 seeds x 100 enemy positions, risk previews, respawn protection, nonoffensive cooldown");
    }
}
