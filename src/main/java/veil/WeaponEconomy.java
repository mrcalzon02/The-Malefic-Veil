package veil;
import java.util.*;

/** Canonical physical weapons. Combat integration is a separate release gate. */
public final class WeaponEconomy {
    public static final int FIRST_WORLD_PICKUP=7;
    public enum Origin { CITY_SCAVENGE, CRAFTED }
    public record Weapon(SurvivalCrafting.Item item,String label,Origin origin,
                         int mass,int pickupKind,String recipeId,String role) {
        public boolean combatIntegrated(){return false;}
    }
    private static final List<Weapon> WORLD=List.of(
        new Weapon(SurvivalCrafting.Item.RUSTED_CROWBAR,"Rusted Crowbar",Origin.CITY_SCAVENGE,4,7,null,"Melee control"),
        new Weapon(SurvivalCrafting.Item.FIRE_AXE,"Fire Axe",Origin.CITY_SCAVENGE,5,8,null,"Heavy melee"),
        new Weapon(SurvivalCrafting.Item.NAIL_BAT,"Nail Bat",Origin.CITY_SCAVENGE,4,9,null,"Improvised impact"),
        new Weapon(SurvivalCrafting.Item.SURVIVAL_KNIFE,"Survival Knife",Origin.CITY_SCAVENGE,2,10,null,"Fast blade"),
        new Weapon(SurvivalCrafting.Item.OLD_REVOLVER,"Old Revolver",Origin.CITY_SCAVENGE,5,11,null,"Sidearm"),
        new Weapon(SurvivalCrafting.Item.PUMP_SHOTGUN,"Pump Shotgun",Origin.CITY_SCAVENGE,9,12,null,"Scattergun"),
        new Weapon(SurvivalCrafting.Item.HUNTING_RIFLE,"Hunting Rifle",Origin.CITY_SCAVENGE,8,13,null,"Precision firearm"),
        new Weapon(SurvivalCrafting.Item.COMPACT_SMG,"Compact SMG",Origin.CITY_SCAVENGE,7,14,null,"Automatic firearm")
    );
    private static final List<Weapon> CRAFTED=List.of(
        new Weapon(SurvivalCrafting.Item.WOODEN_CLUB,"WOODEN CLUB",Origin.CRAFTED,3,-1,"weapon-wooden-club","Fabricated equipment"),
        new Weapon(SurvivalCrafting.Item.STONE_SHIV,"STONE SHIV",Origin.CRAFTED,2,-1,"weapon-stone-shiv","Fabricated equipment"),
        new Weapon(SurvivalCrafting.Item.STONE_HATCHET,"STONE HATCHET",Origin.CRAFTED,4,-1,"weapon-stone-hatchet","Fabricated equipment"),
        new Weapon(SurvivalCrafting.Item.WOODEN_SPEAR,"WOODEN SPEAR",Origin.CRAFTED,4,-1,"weapon-wooden-spear","Fabricated equipment"),
        new Weapon(SurvivalCrafting.Item.SHORT_BOW,"SHORT BOW",Origin.CRAFTED,4,-1,"weapon-short-bow","Fabricated equipment"),
        new Weapon(SurvivalCrafting.Item.SPIKED_CLUB,"SPIKED CLUB",Origin.CRAFTED,5,-1,"weapon-spiked-club","Fabricated equipment"),
        new Weapon(SurvivalCrafting.Item.REINFORCED_SPEAR,"REINFORCED SPEAR",Origin.CRAFTED,5,-1,"weapon-reinforced-spear","Fabricated equipment"),
        new Weapon(SurvivalCrafting.Item.SALVAGE_MACHETE,"SALVAGE MACHETE",Origin.CRAFTED,5,-1,"weapon-salvage-machete","Fabricated equipment"),
        new Weapon(SurvivalCrafting.Item.HAND_CROSSBOW,"HAND CROSSBOW",Origin.CRAFTED,5,-1,"weapon-hand-crossbow","Fabricated equipment"),
        new Weapon(SurvivalCrafting.Item.PIPE_PISTOL,"PIPE PISTOL",Origin.CRAFTED,5,-1,"weapon-pipe-pistol","Fabricated equipment"),
        new Weapon(SurvivalCrafting.Item.BOLT_RIFLE,"BOLT RIFLE",Origin.CRAFTED,8,-1,"weapon-bolt-rifle","Fabricated equipment"),
        new Weapon(SurvivalCrafting.Item.SCRAP_SHOTGUN,"SCRAP SHOTGUN",Origin.CRAFTED,9,-1,"weapon-scrap-shotgun","Fabricated equipment")
    );
    private static final List<Weapon> ALL;
    private static final Map<SurvivalCrafting.Item,Weapon> INDEX;
    static {
        List<Weapon> values=new ArrayList<>(WORLD);values.addAll(CRAFTED);
        Map<SurvivalCrafting.Item,Weapon> index=new EnumMap<>(SurvivalCrafting.Item.class);
        for(Weapon w:values)
            if(index.putIfAbsent(w.item(),w)!=null)throw new IllegalStateException("Duplicate weapon "+w.item());
        for(int i=0;i<WORLD.size();i++)
            if(WORLD.get(i).pickupKind()!=FIRST_WORLD_PICKUP+i)
                throw new IllegalStateException("World pickup ordering changed");
        ALL=List.copyOf(values);INDEX=Map.copyOf(index);
    }
    private WeaponEconomy(){}
    public static List<Weapon> all(){return ALL;}
    public static List<Weapon> worldWeapons(){return WORLD;}
    public static List<Weapon> craftableWeapons(){return CRAFTED;}
    public static Weapon forItem(SurvivalCrafting.Item item){return INDEX.get(item);}
    public static Weapon forPickupKind(int kind){
        int index=kind-FIRST_WORLD_PICKUP;
        return index>=0&&index<WORLD.size()?WORLD.get(index):null;
    }
    public static String pickupMesh(int kind){
        Weapon item=forPickupKind(kind);
        return item==null?null:"pickup-weapon-"+item.item().name().toLowerCase(Locale.ROOT).replace('_','-');
    }
}
