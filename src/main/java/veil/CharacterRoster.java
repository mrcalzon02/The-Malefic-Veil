package veil;

import java.util.*;

/**
 * Character selection metadata; never trusts a client-selected mesh for combat,
 * ownership or hitboxes. Archive candidates stay unbound until licensed and converted.
 */
public final class CharacterRoster {
    public enum Faction {
        THE_UNRAVELED("The Unraveled","Survivors whose minds endured the impossible"),
        THE_PENITENT("The Penitent","Human zealots who call madness revelation"),
        THE_BEGOTTEN("The Begotten","Monstrosities born of the breach");
        public final String display,description;
        Faction(String display,String description){this.display=display;this.description=description;}
    }
    public record Appearance(String id,Faction faction,String title,String silhouette,
                             String meshId,String sourceRequirement) {
        public boolean runtimeReady(){return meshId!=null&&!meshId.isBlank();}
    }
    private static final List<Appearance> ALL=List.of(
        new Appearance("unraveled-warden",Faction.THE_UNRAVELED,"The Watcher",
            "Reinforced survivor, unmistakable armored upper body","hero-warden","PROJECT_AUTHORED"),
        new Appearance("unraveled-medic",Faction.THE_UNRAVELED,"The Stitcher",
            "Medical satchel and civilian protective layers","hero-medic","PROJECT_AUTHORED"),
        new Appearance("unraveled-shade",Faction.THE_UNRAVELED,"The Hollow",
            "Hooded silhouette and narrow shoulders","hero-shade","PROJECT_AUTHORED"),
        new Appearance("unraveled-occultist",Faction.THE_UNRAVELED,"The Whisperer",
            "Long coat, occult implements and visible ritual detail","hero-occultist","PROJECT_AUTHORED"),
        new Appearance("penitent-flagellant",Faction.THE_PENITENT,"The Flagellant",
            "Masked ascetic with ragged ceremonial garb",null,"CHARACTERS_PSX_EXACT_ENTRY_PENDING"),
        new Appearance("penitent-votary",Faction.THE_PENITENT,"The Votary",
            "Layered robes and sigil-heavy armor",null,"CHARACTERS_PSX_EXACT_ENTRY_PENDING"),
        new Appearance("penitent-confessor",Faction.THE_PENITENT,"The Confessor",
            "Distinctive high collar, hood and grim mask",null,"CHARACTERS_PSX_EXACT_ENTRY_PENDING"),
        new Appearance("begotten-harrower",Faction.THE_BEGOTTEN,"The Harrower",
            "Tall, visibly distorted inhuman silhouette",null,"MONSTER_ASSET_REVIEW_PENDING"),
        new Appearance("begotten-mourner",Faction.THE_BEGOTTEN,"The Mourner",
            "Collapsed asymmetrical form and long reaching limbs",null,"MONSTER_ASSET_REVIEW_PENDING"),
        new Appearance("begotten-devourer",Faction.THE_BEGOTTEN,"The Devourer",
            "Heavy inhuman mass, recognizable distant profile",null,"MONSTER_ASSET_REVIEW_PENDING")
    );
    private CharacterRoster(){}
    public static List<Appearance> all(){return ALL;}
    public static List<Appearance> byFaction(Faction faction){
        Objects.requireNonNull(faction);
        return ALL.stream().filter(a->a.faction()==faction).toList();
    }
    public static Appearance get(String id){
        return ALL.stream().filter(a->a.id().equals(id)).findFirst().orElse(null);
    }
}
