package veil;
import java.util.*;

/** Canonical physical weapons. Combat integration is a separate release gate. */
public final class WeaponEconomy {
    public static final int FIRST_WORLD_PICKUP=7;
    public enum Origin { CITY_SCAVENGE, CRAFTED }
    public record Weapon(SurvivalCrafting.Item item,String label,Origin origin,
                         int mass,int pickupKind,String recipeId,String role,
                         CraftingEraRegistry.Faction manufacturer) {
        public boolean combatIntegrated(){return false;}
    }
    private static final List<Weapon> WORLD=List.of(
        new Weapon(SurvivalCrafting.Item.RUSTED_CROWBAR,"Rusted Crowbar",Origin.CITY_SCAVENGE,4,7,null,"Melee control",CraftingEraRegistry.Faction.ALL),
        new Weapon(SurvivalCrafting.Item.FIRE_AXE,"Fire Axe",Origin.CITY_SCAVENGE,5,8,null,"Heavy melee",CraftingEraRegistry.Faction.ALL),
        new Weapon(SurvivalCrafting.Item.NAIL_BAT,"Nail Bat",Origin.CITY_SCAVENGE,4,9,null,"Improvised impact",CraftingEraRegistry.Faction.ALL),
        new Weapon(SurvivalCrafting.Item.SURVIVAL_KNIFE,"Survival Knife",Origin.CITY_SCAVENGE,2,10,null,"Fast blade",CraftingEraRegistry.Faction.ALL),
        new Weapon(SurvivalCrafting.Item.OLD_REVOLVER,"Old Revolver",Origin.CITY_SCAVENGE,5,11,null,"Sidearm",CraftingEraRegistry.Faction.ALL),
        new Weapon(SurvivalCrafting.Item.PUMP_SHOTGUN,"Pump Shotgun",Origin.CITY_SCAVENGE,9,12,null,"Scattergun",CraftingEraRegistry.Faction.ALL),
        new Weapon(SurvivalCrafting.Item.HUNTING_RIFLE,"Hunting Rifle",Origin.CITY_SCAVENGE,8,13,null,"Precision firearm",CraftingEraRegistry.Faction.ALL),
        new Weapon(SurvivalCrafting.Item.COMPACT_SMG,"Compact SMG",Origin.CITY_SCAVENGE,7,14,null,"Automatic firearm",CraftingEraRegistry.Faction.ALL)
    );
    private static final List<Weapon> CRAFTED=List.of(
        new Weapon(SurvivalCrafting.Item.WOODEN_CLUB,"WOODEN CLUB",Origin.CRAFTED,3,-1,"weapon-wooden-club","Fabricated equipment",CraftingEraRegistry.Faction.UNRAVELED),
        new Weapon(SurvivalCrafting.Item.STONE_SHIV,"STONE SHIV",Origin.CRAFTED,2,-1,"weapon-stone-shiv","Fabricated equipment",CraftingEraRegistry.Faction.PENITENT),
        new Weapon(SurvivalCrafting.Item.STONE_HATCHET,"STONE HATCHET",Origin.CRAFTED,4,-1,"weapon-stone-hatchet","Fabricated equipment",CraftingEraRegistry.Faction.BEGOTTEN),
        new Weapon(SurvivalCrafting.Item.WOODEN_SPEAR,"WOODEN SPEAR",Origin.CRAFTED,4,-1,"weapon-wooden-spear","Fabricated equipment",CraftingEraRegistry.Faction.BEGOTTEN),
        new Weapon(SurvivalCrafting.Item.SHORT_BOW,"SHORT BOW",Origin.CRAFTED,4,-1,"weapon-short-bow","Fabricated equipment",CraftingEraRegistry.Faction.BEGOTTEN),
        new Weapon(SurvivalCrafting.Item.SPIKED_CLUB,"SPIKED CLUB",Origin.CRAFTED,5,-1,"weapon-spiked-club","Fabricated equipment",CraftingEraRegistry.Faction.PENITENT),
        new Weapon(SurvivalCrafting.Item.REINFORCED_SPEAR,"REINFORCED SPEAR",Origin.CRAFTED,5,-1,"weapon-reinforced-spear","Fabricated equipment",CraftingEraRegistry.Faction.PENITENT),
        new Weapon(SurvivalCrafting.Item.SALVAGE_MACHETE,"SALVAGE MACHETE",Origin.CRAFTED,5,-1,"weapon-salvage-machete","Fabricated equipment",CraftingEraRegistry.Faction.BEGOTTEN),
        new Weapon(SurvivalCrafting.Item.HAND_CROSSBOW,"HAND CROSSBOW",Origin.CRAFTED,5,-1,"weapon-hand-crossbow","Fabricated equipment",CraftingEraRegistry.Faction.PENITENT),
        new Weapon(SurvivalCrafting.Item.PIPE_PISTOL,"PIPE PISTOL",Origin.CRAFTED,5,-1,"weapon-pipe-pistol","Fabricated equipment",CraftingEraRegistry.Faction.UNRAVELED),
        new Weapon(SurvivalCrafting.Item.BOLT_RIFLE,"BOLT RIFLE",Origin.CRAFTED,8,-1,"weapon-bolt-rifle","Fabricated equipment",CraftingEraRegistry.Faction.UNRAVELED),
        new Weapon(SurvivalCrafting.Item.SCRAP_SHOTGUN,"SCRAP SHOTGUN",Origin.CRAFTED,9,-1,"weapon-scrap-shotgun","Fabricated equipment",CraftingEraRegistry.Faction.UNRAVELED)
    );
    private static final List<Weapon> ALL;
    private static final Map<SurvivalCrafting.Item,Weapon> INDEX;
    static {
        List<Weapon> values=new ArrayList<>(WORLD);values.addAll(CRAFTED);
        Map<SurvivalCrafting.Item,Weapon> index=new EnumMap<>(SurvivalCrafting.Item.class);
        for(Weapon w:values) {
            if(index.putIfAbsent(w.item(),w)!=null)
                throw new IllegalStateException("Duplicate weapon "+w.item());
            if(w.manufacturer()==null||w.origin()==Origin.CRAFTED&&
                w.manufacturer()==CraftingEraRegistry.Faction.ALL)
                throw new IllegalStateException("Unowned crafted weapon "+w.item());
        }
        for(var faction:List.of(CraftingEraRegistry.Faction.UNRAVELED,
                CraftingEraRegistry.Faction.PENITENT,CraftingEraRegistry.Faction.BEGOTTEN))
            if(CRAFTED.stream().filter(w->w.manufacturer()==faction).count()!=4)
                throw new IllegalStateException("Must have exactly four weapons for "+faction);
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
    public static CraftingEraRegistry.Faction recipeFaction(SurvivalCrafting.Recipe recipe){
        Weapon w=recipe==null?null:forItem(recipe.result());
        return w==null||w.origin()!=Origin.CRAFTED?
            CraftingEraRegistry.Faction.ALL:w.manufacturer();
    }
    public static boolean canCraft(SurvivalCrafting.Recipe recipe,CraftingEraRegistry.Faction faction){
        if(recipe==null||faction==null)return false;
        CraftingEraRegistry.Faction required=recipeFaction(recipe);
        return required==CraftingEraRegistry.Faction.ALL||required==faction;
    }
    public static Weapon forPickupKind(int kind){
        int index=kind-FIRST_WORLD_PICKUP;
        return index>=0&&index<WORLD.size()?WORLD.get(index):null;
    }
    public static String pickupMesh(int kind){
        Weapon item=forPickupKind(kind);
        return item==null?null:"pickup-weapon-"+item.item().name().toLowerCase(Locale.ROOT).replace('_','-');
    }
}
