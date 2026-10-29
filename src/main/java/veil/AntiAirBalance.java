package veil;

import java.util.*;

/**
 * Non-runtime balancing reference for the six faction AA art/industry targets.
 * Flight, homing, detection, projectile collision and anti-air entities are
 * NOT implemented here. Numbers are simulation goals, not current DPS.
 */
public final class AntiAirBalance {
    public enum Role { FAST_TRACKER, HEAVY_BREAKER }
    public enum Target { LIGHT, MEDIUM, HEAVY }
    public record Template(String id,CharacterRoster.Faction faction,Role role,
                           String displayName,String visualContract) {}
    private static final List<Template> TOWERS=List.of(
        new Template("unraveled-needle-array",CharacterRoster.Faction.THE_UNRAVELED,
            Role.FAST_TRACKER,"Needle Array","Servo rotary mount and belt-fed tracers"),
        new Template("unraveled-siegebreaker",CharacterRoster.Faction.THE_UNRAVELED,
            Role.HEAVY_BREAKER,"Siegebreaker","Heavy guided rack with slow turn and bright trail"),
        new Template("penitent-confessors-choir",CharacterRoster.Faction.THE_PENITENT,
            Role.FAST_TRACKER,"Confessor's Choir","Choir ring of fast converging ritual bolts"),
        new Template("penitent-judgment-bell",CharacterRoster.Faction.THE_PENITENT,
            Role.HEAVY_BREAKER,"Judgment Bell","Audible bell telegraph and vast consecrated shell"),
        new Template("begotten-spineweaver",CharacterRoster.Faction.THE_BEGOTTEN,
            Role.FAST_TRACKER,"Spineweaver","Living stalk with rapid barbed spine volleys"),
        new Template("begotten-maw-spire",CharacterRoster.Faction.THE_BEGOTTEN,
            Role.HEAVY_BREAKER,"Maw Spire","Swollen organic cannon with slow piercing pod")
    );
    private AntiAirBalance(){}
    public static List<Template> towers(){return TOWERS;}
    public static double hullGoal(Target target){
        return switch(target){case LIGHT->85;case MEDIUM->240;case HEAVY->520;};
    }
    /** Illustrative armor/evasion curve for each tower role, prior to flight QA. */
    public static double estimatedDamagePerSecond(Role role,Target target){
        if(role==Role.FAST_TRACKER){
            double hit=switch(target){case LIGHT->.90;case MEDIUM->.68;case HEAVY->.95;};
            double penetration=switch(target){case LIGHT->1.0;case MEDIUM->.18;case HEAVY->.12;};
            return 11.0*hit*penetration/.35;
        }
        double hit=switch(target){case LIGHT->.015;case MEDIUM->.055;case HEAVY->.83;};
        return 300.0*hit/9.0;
    }
    /** One tracker + one breaker, continuously supplied and with open sightlines. */
    public static double estimatedArraySurvivalSeconds(Target target){
        return hullGoal(target)/(
            estimatedDamagePerSecond(Role.FAST_TRACKER,target)+
            estimatedDamagePerSecond(Role.HEAVY_BREAKER,target));
    }
}
