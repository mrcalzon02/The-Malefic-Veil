package veil;

import java.util.*;

/** Source-verified future NPC appearances from the remaining PSX Killer_M archive.
 * Descriptors drive repeatable *encounter planning*, not live NPC/animation spawning.
 * Missing textures and unverified redistribution remain explicit release gates.
 */
public final class LostNpcCatalog {
    public enum Kind { DRIFTER, ZEALOT, STALKER, BRUTE, WAILER }
    public record Variant(String id,String sourceFile,Kind kind,boolean sourceTextureVerified) {}
    private static Variant v(String name,Kind kind,boolean texture){
        return new Variant("lost-"+name.toLowerCase(Locale.ROOT).replace('_','-'),
            "Characters_psx_1.2/Characters_psx_02/Models/No-Rig/Killer_M/Character_"+name+".fbx",
            kind,texture);
    }
    private static final List<Variant> ALL=List.of(
        v("Killer",Kind.DRIFTER,true),
        v("Killer_01",Kind.DRIFTER,true),
        v("Killer_02",Kind.DRIFTER,true),
        v("Killer_04",Kind.DRIFTER,true),
        v("Killer_05",Kind.ZEALOT,true),
        v("Killer_07",Kind.ZEALOT,true),
        v("Killer_08",Kind.ZEALOT,true),
        v("Killer_10",Kind.BRUTE,true),
        v("Killer_10_01",Kind.BRUTE,false),
        v("Monster",Kind.WAILER,true),
        v("Monster_01",Kind.STALKER,true),
        v("Monster_02",Kind.WAILER,true),
        v("Monster_03",Kind.STALKER,true),
        v("Monster_06",Kind.STALKER,true),
        v("Monster_07",Kind.BRUTE,true)
    );
    public record Encounter(int spawnId,int x,int y,int groupSize,
                            ContinentPopulation.EncounterKind context,String variantId) {}
    private LostNpcCatalog(){}
    public static List<Variant> all(){return ALL;}
    public static List<Variant> availableFor(ContinentPopulation.EncounterKind context){
        Objects.requireNonNull(context);
        List<Kind> allowed=switch(context){
            case CITY_SCAVENGER -> List.of(Kind.DRIFTER,Kind.ZEALOT);
            case ROAD_AMBUSH -> List.of(Kind.DRIFTER,Kind.ZEALOT,Kind.BRUTE);
            case FOREST_HOSTILE -> List.of(Kind.STALKER,Kind.WAILER);
            case MARSH_HOSTILE -> List.of(Kind.WAILER,Kind.STALKER);
            case MOUNTAIN_HOSTILE -> List.of(Kind.BRUTE,Kind.STALKER);
        };
        return ALL.stream().filter(v->v.sourceTextureVerified()&&allowed.contains(v.kind())).toList();
    }
    /** Stable per-world seed assignments, independently of streaming order. */
    public static Encounter classify(long seed,ContinentPopulation.NpcSpawn marker){
        Objects.requireNonNull(marker);
        List<Variant> candidates=availableFor(marker.kind());
        if(candidates.isEmpty())throw new IllegalStateException("No Lost NPC variant for "+marker.kind());
        long value=World.mix(seed^((long)marker.x()*0x9e3779b97f4a7c15L)^
            ((long)marker.y()*0x632be59bd9b4e019L)^((long)marker.id()*19139L));
        Variant appearance=candidates.get((int)Math.floorMod(value,candidates.size()));
        return new Encounter(marker.id(),marker.x(),marker.y(),marker.groupSize(),
            marker.kind(),appearance.id());
    }
    /** Planner only: actor instantiation remains a future zone-runtime gate. */
    public static List<Encounter> plan(long seed,ContinentPopulation population){
        List<Encounter> result=new ArrayList<>();
        for(var marker:population.npcSpawns())result.add(classify(seed,marker));
        return List.copyOf(result);
    }
}
