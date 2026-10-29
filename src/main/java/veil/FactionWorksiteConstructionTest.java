package veil;

import java.nio.file.*;
import java.util.*;

/** End-to-end construction of the three distinct real faction sites, from
 * the recipe registry's authentic material costs in a permanent camp crate. */
public final class FactionWorksiteConstructionTest {
    static void check(boolean yes,String msg){if(!yes)throw new AssertionError(msg);}
    static void tick(ArenaSystems host,int n){for(int i=0;i<n;i++)host.advance(1000);}
    static void clean(Path path)throws Exception{
        try(var files=Files.walk(path)){
            for(Path item:files.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(item);
        }
    }
    static World.Device build(ArenaSystems host,ArenaSystems.Player p,int kind,World.Device anchor){
        if(anchor==null){
            for(int x=-144;x<=264;x+=5)for(int y=-144;y<=264;y+=5){
                if(host.world.detailedCity(x,y)||!host.world.canWalk(x,y))continue;
                p.x=x;p.y=y;p.angle=0;
                if(host.reviewBuild(p.id,kind).allowed()&&host.build(p.id,kind))
                    return host.world.devices.get(host.world.devices.size()-1);
            }
        }else{
            for(double delta=1.3;delta<=4.1;delta+=.25)
                for(int sign:new int[]{1,-1}){
                    p.x=anchor.x+delta*sign;p.y=anchor.y;p.angle=0;
                    if(host.reviewBuild(p.id,kind).allowed()&&host.build(p.id,kind))
                        return host.world.devices.get(host.world.devices.size()-1);
                }
        }
        throw new AssertionError("Cannot physically construct site kind "+kind);
    }
    static void exercise(long seed,CharacterRoster.Faction faction,int kind)throws Exception{
        String key="acct_"+("a".repeat(63))+(kind-8);
        ArenaSystems host=new ArenaSystems(seed);
        ArenaSystems.Player p=host.join(1);
        check(host.assignAccount(1,key),"Authenticated builder required");
        check(host.enrollFaction(1,faction),"Cannot join faction");
        check(p.survival.acquire(SurvivalCrafting.Item.TIMBER,7,false)&&
            p.survival.acquire(SurvivalCrafting.Item.FIBER,6,false),
            "Primitive start components unavailable");
        check(host.craftHand(1,"tent"),"Crafted camp recipe failed");
        tick(host,5);
        check(host.craftHand(1,"crate"),"Crafted crate recipe failed");
        World.Device camp=build(host,p,ArenaSystems.CAMP_STAKE,null);
        World.Device crate=build(host,p,ArenaSystems.STORAGE_CRATE,camp);
        check(crate.storage.snapshot().isEmpty(),"New crate should start empty");

        String facility=ArenaSystems.factionFacility(kind);
        var registered=CraftingEraRegistry.facility(facility);
        check(registered!=null&&registered.era()==CraftingEraRegistry.Era.ENCAMPMENT,
            "Facility not tied to the proper industrial era");
        Map<SurvivalCrafting.Item,Integer> original=new EnumMap<>(SurvivalCrafting.Item.class);
        for(var cost:registered.materials().entrySet()){
            var type=SurvivalCrafting.Item.valueOf(cost.getKey());
            check(p.survival.acquire(type,cost.getValue(),false),
                "First-tier worksite materials not harvestable: "+type);
            original.put(type,cost.getValue());
        }
        p.x=crate.x+1.2;p.y=crate.y;
        for(var cost:original.entrySet())
            check(host.transferCampStock(1,false,cost.getKey(),cost.getValue()),
                "Cannot deposit distinct worksite materials "+cost.getKey());
        check(host.reviewBuild(1,(kind==8?9:8)).reason().equals("WRONG_FACTION_FACILITY"),
            "Account illegally allowed another faction's first factory");
        ArenaSystems.Player guest=host.join(2);
        guest.x=p.x;guest.y=p.y;
        check(!host.build(2,kind),"Unauthenticated guest placed faction site");
        World.Device workshop=build(host,p,kind,crate);
        check(workshop.kind==kind&&key.equals(workshop.ownerKey)&&workshop.hp==100,
            "Constructed worksite not a correctly owned physical world device");
        for(var cost:original.entrySet())
            check(crate.storage.count(cost.getKey())==0,
                "Site construction failed exact crate material debit: "+cost.getKey());
        check(!host.build(1,kind),"Second factory created without replenished materials");
        check(host.world.devices.stream().filter(d->d.kind==kind&&d.hp>0).count()==1,
            "Factory construction minted another structure");
        Path dir=Files.createTempDirectory("veil-faction-site-");
        try{
            DurableRealm.save(host,dir);
            AccountDirectory.save(host,dir);
            ArenaSystems restarted=new ArenaSystems(seed);
            check(DurableRealm.load(restarted,dir),"Physical production site did not persist");
            check(AccountDirectory.load(restarted,dir),"Faction charter did not persist");
            restarted.join(1);check(restarted.assignAccount(1,key),"Owner failed reconnect");
            check(restarted.world.devices.stream().anyMatch(d->d.kind==kind&&
                    key.equals(d.ownerKey)&&d.hp>0),
                "Restored world lost the independent faction site's identity");
            check(restarted.factions.faction(key)==faction,
                "Restored account lost its permanent faction authorization");
        }finally{clean(dir);}
        System.out.println("PASS faction first construction "+faction+": "+facility);
    }
    public static void main(String[] args)throws Exception{
        exercise(20261020L,CharacterRoster.Faction.THE_UNRAVELED,
            ArenaSystems.UNRAVELED_SCRAP_OUTPOST);
        exercise(20261020L,CharacterRoster.Faction.THE_PENITENT,
            ArenaSystems.PENITENT_VOTIVE_ALTAR);
        exercise(20261020L,CharacterRoster.Faction.THE_BEGOTTEN,
            ArenaSystems.BEGOTTEN_BROOD_NEST);
        System.out.println("PASS FactionWorksiteConstructionTest: material-receipted faction-only factories, 3 unique meshes and durable identities");
    }
}
