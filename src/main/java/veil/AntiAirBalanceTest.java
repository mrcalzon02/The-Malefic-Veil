package veil;

import java.util.*;

/** Static anti-air role/roster counterplay contract; not a claim of actual flight combat. */
public final class AntiAirBalanceTest {
    private static void check(boolean value,String reason){
        if(!value)throw new AssertionError(reason);
    }
    public static void main(String[] args){
        List<AntiAirBalance.Template> towers=AntiAirBalance.towers();
        check(towers.size()==6,"two custom tower templates per playable faction");
        Set<String> unique=new HashSet<>();
        for(var tower:towers){
            check(unique.add(tower.id()),"distinct template identifiers");
            check(tower.displayName()!=null&&!tower.displayName().isBlank(),
                "each tower has a nonempty custom faction name");
        }
        for(var faction:CharacterRoster.Faction.values()){
            check(towers.stream().filter(t->t.faction()==faction).count()==2,
                "two AA towers per faction");
            for(var role:AntiAirBalance.Role.values())
                check(towers.stream().filter(t->t.faction()==faction&&t.role()==role).count()==1,
                    "each faction has both mechanics");
        }
        double trackLight=AntiAirBalance.estimatedDamagePerSecond(
            AntiAirBalance.Role.FAST_TRACKER,AntiAirBalance.Target.LIGHT);
        double trackMedium=AntiAirBalance.estimatedDamagePerSecond(
            AntiAirBalance.Role.FAST_TRACKER,AntiAirBalance.Target.MEDIUM);
        double breakerHeavy=AntiAirBalance.estimatedDamagePerSecond(
            AntiAirBalance.Role.HEAVY_BREAKER,AntiAirBalance.Target.HEAVY);
        double breakerMedium=AntiAirBalance.estimatedDamagePerSecond(
            AntiAirBalance.Role.HEAVY_BREAKER,AntiAirBalance.Target.MEDIUM);
        check(trackLight>trackMedium*4,"fast precision tower punishes light fighters");
        check(breakerHeavy>breakerMedium*8,"heavy projectile punishes heavy fliers");
        double medium=AntiAirBalance.estimatedArraySurvivalSeconds(AntiAirBalance.Target.MEDIUM);
        double light=AntiAirBalance.estimatedArraySurvivalSeconds(AntiAirBalance.Target.LIGHT);
        double heavy=AntiAirBalance.estimatedArraySurvivalSeconds(AntiAirBalance.Target.HEAVY);
        check(Double.isFinite(medium)&&medium>light&&medium>heavy,
            "medium must have longest survival vs one of each ground AA type");
        System.out.println("PASS AntiAirBalanceTest: six art contracts and medium-aircraft AA gap (design estimates)");
    }
}
