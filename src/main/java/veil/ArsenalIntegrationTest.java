package veil;
import java.lang.reflect.Field;
import java.util.*;

/** Host-authoritative return of the original 32-weapon branch. */
public final class ArsenalIntegrationTest {
    private ArsenalIntegrationTest(){}
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    @SuppressWarnings("unchecked")
    static ArsenalSystems.State state(ArenaSystems host,int id)throws Exception{
        Field f=ArsenalSystems.class.getDeclaredField("active");f.setAccessible(true);
        return ((Map<Integer,ArsenalSystems.State>)f.get(host.arsenal)).get(id);
    }
    public static void main(String[] args)throws Exception{
        check(ArsenalCatalog.WEAPONS.length==32,"Original arsenal catalog lost");
        int[] counts=new int[3];int loot=0;
        for(int i=0;i<32;i++){
            var weapon=ArsenalCatalog.get(i);
            check(weapon.index()==i&&weapon.id().startsWith("w"),"Weapon index drift");
            if(weapon.mapSpawn()){
                loot++;check(i<8&&weapon.faction()==-1,"Advanced weapon in world loot");
                check(FactionWeaponMaterials.recolor(i,"receiver",0,0x667788)==0x667788,
                    "Shared salvage art was changed");
            }else{
                check(i>=8&&weapon.faction()>=0&&weapon.faction()<3&&
                    weapon.tier()>=1&&weapon.tier()<=3,"Missing faction or bench gate");
                counts[weapon.faction()]++;
                int receiver=FactionWeaponMaterials.recolor(i,"receiver",0,0x667788);
                int glow=FactionWeaponMaterials.recolor(i,"plasma_core",1,0x667788);
                check(receiver!=glow&&receiver!=0x667788,
                    "Faction finish failed to distinguish the animated components");
            }
        }
        check(loot==8&&Arrays.equals(counts,new int[]{9,9,6}),
            "Branch manufacturer distribution changed");
        check(ArsenalCatalog.factionIndex(CharacterRoster.Faction.THE_UNRAVELED)==0&&
            ArsenalCatalog.factionIndex(CharacterRoster.Faction.THE_PENITENT)==1&&
            ArsenalCatalog.factionIndex(CharacterRoster.Faction.THE_BEGOTTEN)==2,
            "Prototype faction names were not reconciled");
        ArenaSystems host=new ArenaSystems(20261023);
        for(var pick:host.arsenal.loot)
            if(pick.type==0)check(pick.value>=0&&pick.value<8,
                "Advanced weapon spawned instead of fabricated");
        for(int faction=0;faction<3;faction++){
            int id=faction+1;
            var player=host.join(id);
            String key="acct_"+Integer.toHexString(faction+10).repeat(64);
            check(host.assignAccount(id,key),"Authenticated binding rejected");
            check(host.enrollFaction(id,CharacterRoster.Faction.values()[faction]),
                "Persistent faction enrollment rejected");
            var view=host.arsenal.view(id);
            check(view.faction()==faction,"Arsenal deviates from host faction");
            check(!host.arsenal.faction(id,(faction+1)%3).ok(),
                "Client-side faction switch was accepted");
            World.Device bench=new World.Device(player.x,player.y,ArenaSystems.FIELD_BENCH);
            bench.ownerKey=key;bench.ownerId=id;bench.tier=3;
            host.world.devices.add(bench);
            player.scrap=999;
            var inventory=state(host,id);
            Arrays.fill(inventory.materials,999);
            inventory.researched=0xffffffffL;
            inventory.owned=0xffL;
            for(int weapon=8;weapon<32;weapon++){
                if(ArsenalCatalog.get(weapon).faction()==faction)continue;
                var previous=host.arsenal.view(id).owned();
                var denied=host.arsenal.craft(id,weapon);
                check(!denied.ok()&&"FACTION_RECIPE".equals(denied.reason()),
                    "Foreign weapon illegally manufactured: "+weapon+" by "+faction);
                check(host.arsenal.view(id).owned()==previous,
                    "Denied craft changed inventory");
            }
            for(int weapon=8;weapon<32;weapon++){
                if(ArsenalCatalog.get(weapon).faction()!=faction)continue;
                var success=host.arsenal.craft(id,weapon);
                check(success.ok(),"Own weapon fabrication failed: "+weapon+" "+success.reason());
                check((host.arsenal.view(id).owned()&(1L<<weapon))!=0,
                    "Crafted item not granted on host");
            }
        }
        // Confirm the imported textured component data still loads and moves.
        ArsenalMeshes meshes=new ArsenalMeshes();
        for(int i=0;i<32;i++){
            var still=meshes.posed(i,0,0);
            var fired=meshes.posed(i,1,.45);
            check(still.triangleCount()>50&&!still.triangles().equals(fired.triangles()),
                "Working original geometry or rigid fire pose lost "+i);
        }
        System.out.println("PASS ArsenalIntegrationTest: 32 originals, 8 loot, 24 host-gated crafts, 3 palettes and 32 moving meshes");
    }
}
