package veil;

import java.util.*;

/** Faction-exclusive class definitions; gameplay IDs 0..3 are the only implemented abilities. */
public final class FactionClasses {
    public record Role(String id,CharacterRoster.Faction faction,String name,String purpose,
                       int existingAbilityId) {
        public boolean playable(){return existingAbilityId>=0;}
    }
    private static final List<Role> ROLES=List.of(
        new Role("unraveled-warden",CharacterRoster.Faction.THE_UNRAVELED,"Warden","Fortifications and defensive equipment",0),
        new Role("unraveled-stitcher",CharacterRoster.Faction.THE_UNRAVELED,"Stitcher","Field recovery and improvised medicine",1),
        new Role("unraveled-hollow",CharacterRoster.Faction.THE_UNRAVELED,"Hollow","Veil concealment and stealth",2),
        new Role("penitent-confessor",CharacterRoster.Faction.THE_PENITENT,"Confessor","Psychic repulsion and restored composure",3),
        new Role("penitent-flagellant",CharacterRoster.Faction.THE_PENITENT,"Flagellant","Risk-reward close combat and self-sacrifice",-1),
        new Role("penitent-votary",CharacterRoster.Faction.THE_PENITENT,"Votary","Ritual support, cleansing and zeal",-1),
        new Role("begotten-harrower",CharacterRoster.Faction.THE_BEGOTTEN,"Harrower","Stalking and terror",-1),
        new Role("begotten-devourer",CharacterRoster.Faction.THE_BEGOTTEN,"Devourer","Brute resilience and consumption",-1),
        new Role("begotten-mourner",CharacterRoster.Faction.THE_BEGOTTEN,"Mourner","Dread and area corruption",-1)
    );
    private FactionClasses(){}
    public static List<Role> all(){return ROLES;}
    public static List<Role> faction(CharacterRoster.Faction faction){
        return ROLES.stream().filter(r->r.faction()==faction).toList();
    }
    public static Role get(String id){
        return ROLES.stream().filter(r->r.id().equals(id)).findFirst().orElse(null);
    }
}
