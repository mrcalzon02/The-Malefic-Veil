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
        check(!kit.craft("weapon-wooden-club",SurvivalCrafting.Station.HAND),
            "Neutral/legacy craft bypassed faction ownership");
        check(kit.craft("weapon-wooden-club",SurvivalCrafting.Station.HAND,
            CraftingEraRegistry.Faction.UNRAVELED),
            "Correctly enrolled Unraveled character cannot craft first weapon");
        check(kit.count(SurvivalCrafting.Item.WOODEN_CLUB)==1&&
              kit.count(SurvivalCrafting.Item.TIMBER)==0&&
              kit.count(SurvivalCrafting.Item.FIBER)==0,
            "Crafted weapon did not consume exact ingredients");
        check(!kit.craft("weapon-wooden-club",SurvivalCrafting.Station.HAND),
            "Duplicated weapon without ingredients");
        check(!kit.craft("weapon-pipe-pistol",SurvivalCrafting.Station.HAND),
            "Workbench gun craftable without station");
        for(var faction:CraftingEraRegistry.Faction.values()){
            long count=WeaponEconomy.craftableWeapons().stream()
                .filter(w->w.manufacturer()==faction).count();
            check(count==(faction==CraftingEraRegistry.Faction.ALL?0:4),
                "Not four independent faction weapon recipes: "+faction);
        }
        for(var weapon:WeaponEconomy.craftableWeapons()){
            var recipe=SurvivalCrafting.recipes().get(weapon.recipeId());
            var indexed=CraftingRecipeIndex.recipe("hand:"+recipe.id());
            check(indexed!=null&&indexed.faction().equals(weapon.manufacturer().name()),
                "Recipe index presents universal or foreign weapon "+weapon.item());
            var materials=new SurvivalCrafting();
            for(var input:recipe.requires().entrySet())
                check(materials.acquire(input.getKey(),input.getValue(),
                    input.getKey()==SurvivalCrafting.Item.MACHINE_PARTS||
                    input.getKey()==SurvivalCrafting.Item.METAL_STOCK),
                    "Cannot acquire fixture ingredients "+weapon.item());
            var unchanged=materials.inventory();
            for(var faction:CraftingEraRegistry.Faction.values()){
                boolean own=faction==weapon.manufacturer();
                check(CraftingRecipeIndex.recipesFor(faction).contains(indexed)==own,
                    "Foreign faction recipe visible "+weapon.item());
                check(WeaponEconomy.canCraft(recipe,faction)==own,
                    "Permission mismatch "+weapon.item()+"/"+faction);
                if(!own){
                    check(!materials.craft(recipe.id(),recipe.station(),faction),
                        "Foreign faction illegally minted "+weapon.item());
                    check(materials.inventory().equals(unchanged),
                        "Denied faction craft consumed inventory "+weapon.item());
                }
            }
            check(!materials.craft(recipe.id(),recipe.station()),
                "Legacy neutral craft leaked "+weapon.item());
            check(materials.craft(recipe.id(),recipe.station(),weapon.manufacturer()),
                "Own faction weapon was rejected "+weapon.item());
            check(materials.count(weapon.item())==1,
                "Authorized craft did not mint exactly one "+weapon.item());
            check(!materials.craft(recipe.id(),recipe.station(),weapon.manufacturer()),
                "Duplicated faction weapon after debit "+weapon.item());
        }
        ArenaSystems host=new ArenaSystems(1024);
        host.world.npcs.clear();
        CharacterRoster.Faction[] realFactions=CharacterRoster.Faction.values();
        String[] starters={"weapon-wooden-club","weapon-stone-shiv","weapon-stone-hatchet"};
        for(int i=0;i<3;i++){
            int id=i+1;
            var player=host.join(id);
            check(host.assignAccount(id,"acct_"+String.valueOf("abc".charAt(i)).repeat(64)),
                "Authenticated account");
            check(host.enrollFaction(id,realFactions[i]),"Host faction enrollment");
            var own=SurvivalCrafting.recipes().get(starters[i]);
            var foreign=SurvivalCrafting.recipes().get(starters[(i+1)%3]);
            for(var recipe:List.of(own,foreign))
                for(var input:recipe.requires().entrySet())
                    if(player.survival.count(input.getKey())<input.getValue())
                        check(player.survival.acquire(input.getKey(),
                            input.getValue()-player.survival.count(input.getKey()),false),
                            "Host test ingredients");
            var before=player.survival.inventory();
            check(!host.craftHand(id,foreign.id()),"Host manufactured foreign weapon");
            check(player.survival.inventory().equals(before),
                "Denied host craft consumed materials");
            check(host.craftHand(id,own.id()),"Host rejected authorized weapon");
            check(player.survival.count(own.result())==1,"Host missed inventory output");
        }
        System.out.println("PASS WeaponEconomyTest: 8 world pickups, 12 faction-owned recipes and real host enforcement");
    }
}
