package veil;
import java.util.*;

/** Host weapon acquisition, non-duplication, crafting and legacy pickup numbering. */
public final class WeaponEconomyTest {
    private static void check(boolean good,String why){if(!good)throw new AssertionError(why);}
    public static void main(String[] args){
        check(WeaponEconomy.worldWeapons().size()==8,"Eight physical city weapons");
        check(WeaponEconomy.craftableWeapons().size()==12,"Twelve crafted weapons");
        Set<SurvivalCrafting.Item> seen=new HashSet<>();
        for(var weapon:WeaponEconomy.all()){
            check(seen.add(weapon.item()),"Duplicate item ID");
            check(WeaponEconomy.forItem(weapon.item())==weapon,"Index mismatch");
            check(!weapon.combatIntegrated(),"Physical inventory does not mean firing support");
            if(weapon.origin()==WeaponEconomy.Origin.CRAFTED){
                var recipe=SurvivalCrafting.recipes().get(weapon.recipeId());
                check(recipe!=null&&recipe.result()==weapon.item()&&recipe.quantity()==1,
                    "Weapon is not a real survival craft");
            }else check(GameplayMeshes.pickupModel(weapon.pickupKind())!=null,
                    "Missing city pickup proxy");
        }
        for(long seed:new long[]{1L,42L,53L,9876L,20261023L}){
            ArenaSystems game=new ArenaSystems(seed);
            check(game.world.pickups.size()==178,"Legacy pickups must remain, append eight");
            Set<Integer> ids=new HashSet<>();
            for(int i=170;i<178;i++){
                World.Pickup item=game.world.pickups.get(i);
                check(ids.add(item.kind),"Duplicated weapon pickup kind");
                check(WeaponEconomy.forPickupKind(item.kind)!=null,"Missing kind");
                check(game.world.canWalk(item.x,item.y),"Weapon placed inside wall");
                check(game.world.city.categoryAt((int)item.x,(int)item.y)==
                    CityLayout.Category.RESOURCE,"World weapon not placed at salvage resource");
            }
            game.world.npcs.clear();
            var player=game.join(91);
            World.Pickup pickup=game.world.pickups.get(170);
            player.x=pickup.x;player.y=pickup.y;player.moveAt=game.time();
            check(game.pickup(91,170),"Host did not grant weapon");
            check(player.survival.count(WeaponEconomy.forPickupKind(pickup.kind).item())==1,
                "Weapon not in weighted inventory");
            check(!game.pickup(91,170),"Duplicate world pickup");
            player.survival.clearOnDeath();
            check(player.survival.inventory().isEmpty(),"Carried weapon survived death");
        }
        var kit=new SurvivalCrafting();
        check(kit.acquire(SurvivalCrafting.Item.TIMBER,2,false)&&
              kit.acquire(SurvivalCrafting.Item.FIBER,1,false),"Gather weapon ingredients");
        check(kit.craft("weapon-wooden-club",SurvivalCrafting.Station.HAND),
            "Craftable early weapon not produced");
        check(kit.count(SurvivalCrafting.Item.WOODEN_CLUB)==1&&
              kit.count(SurvivalCrafting.Item.TIMBER)==0&&
              kit.count(SurvivalCrafting.Item.FIBER)==0,
            "Crafted weapon did not consume exact ingredients");
        check(!kit.craft("weapon-wooden-club",SurvivalCrafting.Station.HAND),
            "Duplicated weapon without ingredients");
        check(!kit.craft("weapon-pipe-pistol",SurvivalCrafting.Station.HAND),
            "Workbench gun craftable without station");
        System.out.println("PASS WeaponEconomyTest: 8 world pickups and 12 recipes");
    }
}
