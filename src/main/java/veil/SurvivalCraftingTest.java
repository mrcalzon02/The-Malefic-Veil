package veil;

import java.util.*;

/** Crafting resource debit, station gates, spoil-free inventory and thirst. */
public final class SurvivalCraftingTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        SurvivalCrafting a=new SurvivalCrafting();
        check(!a.acquire(SurvivalCrafting.Item.METAL_STOCK,1,false),
            "City bulk loads cannot be placed in backpack instead of truck/base");
        check(a.acquire(SurvivalCrafting.Item.FIBER,5,false),"Gathered fiber");
        check(a.acquire(SurvivalCrafting.Item.MEDICINAL_HERBS,1,false),"Herb");
        check(a.craft("bandage",SurvivalCrafting.Station.HAND),"Bandage recipe");
        check(a.count(SurvivalCrafting.Item.FIBER)==3&&
            a.count(SurvivalCrafting.Item.BANDAGE)==1,"Crafting did not debit inputs");
        check(!a.craft("bandage",SurvivalCrafting.Station.HAND),
            "Recipe creates free medicine");
        check(!a.craft("vehicle-repair",SurvivalCrafting.Station.HAND),
            "Workbench-gated repair craft permitted in hands");
        check(a.acquire(SurvivalCrafting.Item.METAL_STOCK,1,true),
            "Delivered truck load at authorized base enters material store");
        check(a.acquire(SurvivalCrafting.Item.MACHINE_PARTS,1,true),
            "Second truck resource has separate inventory identity");
        check(a.craft("vehicle-repair",SurvivalCrafting.Station.WORKBENCH),
            "Distinct bulk resources combine at workbench");
        check(a.count(SurvivalCrafting.Item.METAL_STOCK)==0&&
            a.count(SurvivalCrafting.Item.MACHINE_PARTS)==0&&
            a.count(SurvivalCrafting.Item.VEHICLE_REPAIR_KIT)==1,
            "Cargo material duplicated by craft");
        check(!a.craft("vehicle-repair",SurvivalCrafting.Station.WORKBENCH),
            "Crafting duplicated consumed resource loads");
        check(!a.consume(SurvivalCrafting.Item.DIRTY_WATER),
            "Unsafe dirty water must not count as hydration");
        check(a.acquire(SurvivalCrafting.Item.DIRTY_WATER,1,false),"Gathered dirty water");
        check(!a.craft("boil-water",SurvivalCrafting.Station.HAND),
            "Cannot purify water without fire");
        check(a.craft("boil-water",SurvivalCrafting.Station.FIRE),
            "Boiling converts dirty water into safe water");
        a.advance(60_000);
        check(a.status().hunger()==99&&a.status().hydration()==98,
            "Survival decay happens once a minute, not once a frame");
        check(a.consume(SurvivalCrafting.Item.CLEAN_WATER)&&
            a.status().hydration()==100,"Drinking purified water");
        check(!a.acquire(SurvivalCrafting.Item.FIELD_STONE,1000,false),
            "Inventory capacity must be bounded");
        check(SurvivalCrafting.recipes().size()>=6,"Crafting chain missing");
        WeaponEconomyTest.main(args);
        System.out.println("PASS SurvivalCraftingTest: source gathering, bulk-gated base crafting and survival");
    }
}
