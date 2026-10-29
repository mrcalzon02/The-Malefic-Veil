package veil;

import java.util.*;

/**
 * The Lost weapon design registry. The Drifter's opening bone-shiv alone
 * has a host-authoritative telegraph -> avoidable strike -> recovery path.
 * The other 24 profiles are still design data, not executable NPC attacks.
 */
public final class LostArsenal {
    public enum Slot { OPENING, PRESSURE, CONTROL, AREA, ELITE }
    public enum Delivery { MELEE, PROJECTILE, WAVE, HAZARD, CHARGE }
    public record Weapon(String id, LostNpcCatalog.Kind kind, Slot slot,
                         Delivery delivery, double reach, int hpDamage,
                         int sanityDamage, double windupSeconds,
                         double recoverySeconds, String telegraph,
                         String counter, String behavior) {
        /** Only the genuinely wired host attack may claim runtime integration. */
        public boolean runtimeIntegrated() {
            return id.equals("lost-drifter-bone-shiv");
        }
    }
    private static Weapon w(String id, LostNpcCatalog.Kind kind, Slot slot,
          Delivery delivery, double reach, int hp, int sanity, double windup,
          double recovery, String cue, String counter, String behavior) {
        if(reach<=0||hp<0||sanity<0||windup<=0||recovery<=0||
           cue.isBlank()||counter.isBlank()||behavior.isBlank())
            throw new IllegalArgumentException("Invalid Lost weapon "+id);
        return new Weapon(id,kind,slot,delivery,reach,hp,sanity,windup,
            recovery,cue,counter,behavior);
    }
    private static final List<Weapon> ALL=List.of(
        w("lost-drifter-bone-shiv",LostNpcCatalog.Kind.DRIFTER,Slot.OPENING,
            Delivery.MELEE,1.4,12,1,0.3,0.65,
            "arm low; rusted point catches light",
            "step sideways and force a miss",
            "quick two-step approach; single close stab"),
        w("lost-drifter-pipe-hook",LostNpcCatalog.Kind.DRIFTER,Slot.PRESSURE,
            Delivery.MELEE,2.1,15,2,0.52,0.88,
            "pipe scrapes asphalt before a broad hook",
            "back away beyond arc or interrupt windup",
            "wide stagger arc pushes prey into chokepoints"),
        w("lost-drifter-pocket-revolver",LostNpcCatalog.Kind.DRIFTER,Slot.CONTROL,
            Delivery.PROJECTILE,11,11,0,0.95,1.85,
            "distinct cylinder click and extended arm",
            "break sight; punish lengthy reload",
            "rare inaccurate shot; finite rounds per encounter"),
        w("lost-drifter-shard-toss",LostNpcCatalog.Kind.DRIFTER,Slot.AREA,
            Delivery.PROJECTILE,6,7,2,0.7,1.4,
            "glass clinks inside a raised fist",
            "leave marked scatter cone",
            "short cone of glass fragments; limited ground persistence"),
        w("lost-drifter-salvage-cleaver",LostNpcCatalog.Kind.DRIFTER,Slot.ELITE,
            Delivery.MELEE,2,23,4,1,1.35,
            "overhead two-hand cleaver pose",
            "dodge lateral recovery and counterattack",
            "armor-splitting committed blow; never silent one-hit kill"),
        w("lost-zealot-censer-mace",LostNpcCatalog.Kind.ZEALOT,Slot.OPENING,
            Delivery.MELEE,1.9,12,5,0.45,0.88,
            "chain rattles and ember flare",
            "avoid short chained swing; punish reset",
            "physical impact plus modest sanity burn"),
        w("lost-zealot-ash-lash",LostNpcCatalog.Kind.ZEALOT,Slot.PRESSURE,
            Delivery.MELEE,4,9,7,0.73,1.15,
            "corded lash cracks before strike",
            "move into cover; cut diagonally inside reach",
            "mid-range displacement and mark for pursuit"),
        w("lost-zealot-confession-bell",LostNpcCatalog.Kind.ZEALOT,Slot.CONTROL,
            Delivery.WAVE,7,2,16,1.25,2.1,
            "clear bell lift and pulsing circle",
            "break line of sight or exit warning ring",
            "sanity-shock wave; brief sound distortion, capped duration"),
        w("lost-zealot-votive-embers",LostNpcCatalog.Kind.ZEALOT,Slot.AREA,
            Delivery.HAZARD,5.5,6,8,1.1,2.25,
            "two-handed bowl held aloft; ash falls",
            "avoid outlined consecration patch",
            "slow-burning ground strip discourages camping"),
        w("lost-zealot-reliquary-nailgun",LostNpcCatalog.Kind.ZEALOT,Slot.ELITE,
            Delivery.PROJECTILE,10,17,4,1.15,1.9,
            "ratcheting reliquary charge",
            "duck hard cover then close during reload",
            "three separately simulated, avoidable spike shots"),
        w("lost-stalker-talon-rake",LostNpcCatalog.Kind.STALKER,Slot.OPENING,
            Delivery.MELEE,1.5,13,3,0.25,0.74,
            "shoulders bunch; claws turn outward",
            "keep spacing and listen for scrape",
            "fast lateral swipe at exposed flanks"),
        w("lost-stalker-hook-leap",LostNpcCatalog.Kind.STALKER,Slot.PRESSURE,
            Delivery.CHARGE,5.5,17,5,0.95,1.65,
            "creature compresses and hisses at target",
            "sidestep landing corridor; exploit miss recovery",
            "telegraphed pounce ending in finite collision"),
        w("lost-stalker-spine-dart",LostNpcCatalog.Kind.STALKER,Slot.CONTROL,
            Delivery.PROJECTILE,9,8,5,0.68,1.35,
            "spines flare along back",
            "use cover; do not sprint straight back",
            "low damage dart briefly impairs sprint, not paralysis"),
        w("lost-stalker-tendon-snare",LostNpcCatalog.Kind.STALKER,Slot.AREA,
            Delivery.HAZARD,4,3,8,1.05,2,
            "ground scratching and pale cord growth",
            "spot and destroy marked cord",
            "temporary visible foot trap; per-player immunity after escape"),
        w("lost-stalker-mirror-backstab",LostNpcCatalog.Kind.STALKER,Slot.ELITE,
            Delivery.MELEE,1.3,26,9,1.3,2.1,
            "distorted reflection and rear-foot scrape",
            "turn toward warning audio; back near solid cover",
            "rear-flank bonus only after actual verified positioning"),
        w("lost-brute-hammerfist",LostNpcCatalog.Kind.BRUTE,Slot.OPENING,
            Delivery.MELEE,2.2,20,2,0.65,1.1,
            "fist rises high with deep exhale",
            "sidestep the marked impact point",
            "heavy single-target slam with manageable knockback"),
        w("lost-brute-girder-sweep",LostNpcCatalog.Kind.BRUTE,Slot.PRESSURE,
            Delivery.MELEE,3.5,15,4,1.15,1.75,
            "metal grinds across rubble",
            "retreat behind sturdy geometry",
            "very broad physical arc with extended recovery"),
        w("lost-brute-rubble-heave",LostNpcCatalog.Kind.BRUTE,Slot.CONTROL,
            Delivery.PROJECTILE,9,18,1,1.4,2.2,
            "both hands pry up a visible chunk",
            "move behind solid cover during lift",
            "slow ballistic rock, interceptable collision"),
        w("lost-brute-ground-rupture",LostNpcCatalog.Kind.BRUTE,Slot.AREA,
            Delivery.WAVE,4.5,13,5,1.55,2.7,
            "both feet plant; cracked ground pulses",
            "jump or leave indicated fracture zone",
            "ground-bound short ring that interrupts stationary aims"),
        w("lost-brute-wrecking-charge",LostNpcCatalog.Kind.BRUTE,Slot.ELITE,
            Delivery.CHARGE,8,29,6,1.6,3,
            "long roar and straight-line stomp buildup",
            "take lateral cover; exploit hard stop on impact",
            "high commitment straight-line collision, no corner teleport"),
        w("lost-wailer-glass-shriek",LostNpcCatalog.Kind.WAILER,Slot.OPENING,
            Delivery.WAVE,6,3,13,0.62,1.2,
            "inhalation and visible throat glow",
            "break line of sight; seek acoustic shelter",
            "short-direction sanity cone rather than silent damage"),
        w("lost-wailer-breath-siphon",LostNpcCatalog.Kind.WAILER,Slot.PRESSURE,
            Delivery.WAVE,4,5,11,1,1.55,
            "head tracks and surrounding sound dulls",
            "leave narrow tether range",
            "channeled attrition ends on occlusion or distance"),
        w("lost-wailer-resonance-lance",LostNpcCatalog.Kind.WAILER,Slot.CONTROL,
            Delivery.PROJECTILE,12,8,14,1.25,2.05,
            "straight purple filament charges visibly",
            "dodge perpendicular to the beam",
            "narrow aimed sonic bolt; no through-wall hit"),
        w("lost-wailer-grief-chorus",LostNpcCatalog.Kind.WAILER,Slot.AREA,
            Delivery.HAZARD,6,2,10,1.6,2.4,
            "multiple spectral mouths flare in an expanding circle",
            "exit slow pulse boundary",
            "pulsing sanity field; no stacking duplicate effects"),
        w("lost-wailer-silence-bloom",LostNpcCatalog.Kind.WAILER,Slot.ELITE,
            Delivery.HAZARD,5.5,3,20,2,3.2,
            "all ambience drops then violet rings appear",
            "withdraw before final pulse; watch floor ring",
            "large telegraphed finale that disrupts sound cues briefly")
    );
    static {
        Set<String> ids=new HashSet<>();
        for(Weapon weapon:ALL) {
            if(!ids.add(weapon.id()))throw new IllegalStateException("Duplicate Lost weapon ID");
        }
        for(LostNpcCatalog.Kind kind:LostNpcCatalog.Kind.values()) {
            EnumSet<Slot> slots=EnumSet.noneOf(Slot.class);
            for(Weapon weapon:ALL)if(weapon.kind()==kind) {
                if(!slots.add(weapon.slot()))throw new IllegalStateException("Duplicate Lost slot "+kind);
            }
            if(slots.size()!=Slot.values().length)
                throw new IllegalStateException("Incomplete Lost loadout "+kind);
        }
    }
    private LostArsenal(){}
    public static List<Weapon> all(){return ALL;}
    public static List<Weapon> forKind(LostNpcCatalog.Kind kind){
        Objects.requireNonNull(kind);
        return ALL.stream().filter(w->w.kind()==kind).toList();
    }
    public static Weapon get(String id){
        return ALL.stream().filter(w->w.id().equals(id)).findFirst().orElse(null);
    }
}
