package veil;

import java.util.*;

/** Planned abilities share stable identifiers with the faction codex.
 * Metadata describes balance intent; it never grants an unimplemented power. */
public final class ClassAbilityCatalog {
    public enum Slot { PASSIVE, PRIMARY, SECONDARY, SIGNATURE }
    public record Skill(String id,String classId,Slot slot,String title,int cooldownSeconds,
                        int durationSeconds,int meterCost,String summary) {}
    private static Skill s(String role,Slot slot,String name,int cooldown,int duration,int cost,String detail){
        return new Skill(role+"-"+slot.name().toLowerCase(Locale.ROOT),role,slot,name,
            cooldown,duration,cost,detail);
    }
    private static final List<Skill> ALL=List.of(
        s("unraveled-warden",Slot.PASSIVE,"Brace",0,0,0,"Reduced stagger while stationary"),
        s("unraveled-warden",Slot.PRIMARY,"Interlock",24,15,0,"Temporary ballistic screen"),
        s("unraveled-warden",Slot.SECONDARY,"Holdfast",28,8,0,"Defensive stagger-resistance circle"),
        s("unraveled-warden",Slot.SIGNATURE,"Last Redoubt",0,12,80,"Visible allied damage-resistance field"),
        s("unraveled-stitcher",Slot.PASSIVE,"Triage",0,0,0,"Injury awareness and faster medical use"),
        s("unraveled-stitcher",Slot.PRIMARY,"Suture Shot",22,3,0,"Interrupted heal over time"),
        s("unraveled-stitcher",Slot.SECONDARY,"Field Tourniquet",30,8,0,"Status resistance and wound relief"),
        s("unraveled-stitcher",Slot.SIGNATURE,"Keep Breathing",0,8,80,"Interruptible stabilization and healing pulses"),
        s("unraveled-hollow",Slot.PASSIVE,"Stillness",0,0,0,"Reduced visibility while motionless"),
        s("unraveled-hollow",Slot.PRIMARY,"Thinning",28,7,0,"Semi-transparent concealment"),
        s("unraveled-hollow",Slot.SECONDARY,"False Footsteps",25,5,0,"Audible decoy"),
        s("unraveled-hollow",Slot.SIGNATURE,"Between Seconds",0,6,80,"Brief stealth mobility window"),
        s("penitent-confessor",Slot.PASSIVE,"Litany",0,0,0,"Ambient sanity-drain mitigation"),
        s("penitent-confessor",Slot.PRIMARY,"Rebuke",24,0,0,"Telegraphed psychic displacement"),
        s("penitent-confessor",Slot.SECONDARY,"Forced Truth",26,6,0,"Line-of-sight target mark"),
        s("penitent-confessor",Slot.SIGNATURE,"The Great Confession",0,8,80,"Sanity-pressure ritual zone"),
        s("penitent-flagellant",Slot.PASSIVE,"Testament",0,4,0,"Enemy-damage-triggered speed"),
        s("penitent-flagellant",Slot.PRIMARY,"Blood Price",22,7,0,"Health-for-melee-power"),
        s("penitent-flagellant",Slot.SECONDARY,"Thorned Vow",30,5,0,"Capped melee damage reflection"),
        s("penitent-flagellant",Slot.SIGNATURE,"Ecstasy of Ruin",0,10,80,"High-risk melee and speed enhancement"),
        s("penitent-votary",Slot.PASSIVE,"Shared Burden",0,0,0,"Nearby ally sanity resilience"),
        s("penitent-votary",Slot.PRIMARY,"Vigil Flame",28,10,0,"Visible ritual recovery circle"),
        s("penitent-votary",Slot.SECONDARY,"Psalm of Binding",30,3,0,"Short telegraphed slow"),
        s("penitent-votary",Slot.SIGNATURE,"Ninefold Vigil",0,12,80,"Choir field support and warning"),
        s("begotten-harrower",Slot.PASSIVE,"Scent of Fear",0,0,0,"Directional awareness of low-sanity prey"),
        s("begotten-harrower",Slot.PRIMARY,"Skitter",20,0,0,"Collision-swept predator dash"),
        s("begotten-harrower",Slot.SECONDARY,"Listening Horror",26,5,0,"Movement-listening pulses"),
        s("begotten-harrower",Slot.SIGNATURE,"The Long Hunt",0,10,80,"LOS-bound pursuit mark"),
        s("begotten-devourer",Slot.PASSIVE,"Carrion Memory",0,0,0,"Capped heal on valid kill participation"),
        s("begotten-devourer",Slot.PRIMARY,"Rending Rush",26,0,0,"Telegraphed collision charge"),
        s("begotten-devourer",Slot.SECONDARY,"Hardened Husk",32,5,0,"Slower but tougher"),
        s("begotten-devourer",Slot.SIGNATURE,"Hunger Made Flesh",0,10,80,"Capped enemy-melee lifesteal"),
        s("begotten-mourner",Slot.PASSIVE,"Funeral Hush",0,0,0,"Localized auditory suppression"),
        s("begotten-mourner",Slot.PRIMARY,"Grief Pool",28,7,0,"Visible slowing sanity pool"),
        s("begotten-mourner",Slot.SECONDARY,"Echo of Loss",30,4,0,"Short sound-indicator disruption"),
        s("begotten-mourner",Slot.SIGNATURE,"Black Lament",0,10,80,"Telegraphed dread field")
    );
    private ClassAbilityCatalog(){}
    public static List<Skill> all(){return ALL;}
    public static List<Skill> forClass(String classId){
        if(FactionClasses.get(classId)==null)return List.of();
        return ALL.stream().filter(s->s.classId().equals(classId)).toList();
    }
}
