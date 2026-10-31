package veil;

import java.nio.file.*;
import java.util.*;

/** Real paid Penitent Incursion construction, owner isolation and durable
 * identity. Powering its 25-load production is a separate acceptance step. */
public final class IncursionDreadWorksTest {
    private static final int WORKS=ArenaSystems.PENITENT_DREAD_WORKS;
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private static World.Device seed(ArenaSystems host,int kind,
            double x,double y,String owner){
        World.Device device=new World.Device(x,y,kind);
        device.ownerKey=owner;
        host.world.devices.add(device);
        return device;
    }
    private static boolean clear(ArenaSystems host,double x,double y){
        return !host.world.detailedCity(x,y)&&host.world.canWalk(x,y)&&
            !host.world.worldStructureAt(x,y,.55);
    }
    private static double[] site(ArenaSystems host){
        for(int x=-124;x<=224;x+=6)for(int y=-124;y<=224;y+=6){
            if(clear(host,x-2,y)&&clear(host,x+2,y)&&
               clear(host,x,y+2)&&clear(host,x,y-2)&&
               clear(host,x-2,y-2)&&clear(host,x+2,y-2)&&clear(host,x,y))
                return new double[]{x,y};
        }
        throw new AssertionError("No legal wilderness Penitent construction site");
    }
    private static Map<SurvivalCrafting.Item,Integer> bill(){
        var spec=CraftingEraRegistry.facility("dread-works");
        check(spec!=null&&spec.era()==CraftingEraRegistry.Era.INCURSION&&
            spec.faction()==CraftingEraRegistry.Faction.PENITENT&&
            spec.requires().containsAll(List.of("terror-collector","assembly-line")),
            "Penitent blueprint or its physical dependencies missing");
        EnumMap<SurvivalCrafting.Item,Integer> cost=
            new EnumMap<>(SurvivalCrafting.Item.class);
        for(var item:spec.materials().entrySet())
            cost.put(SurvivalCrafting.Item.valueOf(item.getKey()),item.getValue());
        return cost;
    }
    private static void clean(Path dir)throws Exception{
        try(var walk=Files.walk(dir))
            for(Path file:walk.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(file);
    }
    public static void main(String[] args)throws Exception{
        final long seed=20261030L;
        String owner="acct_"+"d".repeat(64),foreign="acct_"+"e".repeat(64);
        ArenaSystems host=new ArenaSystems(seed);
        ArenaSystems.Player p=host.join(1);
        check(host.assignAccount(1,owner)&&
            host.enrollFaction(1,CharacterRoster.Faction.THE_PENITENT),
            "Penitent owner binding failed");
        ArenaSystems.Player rival=host.join(2);
        check(host.assignAccount(2,foreign)&&
            host.enrollFaction(2,CharacterRoster.Faction.THE_UNRAVELED),
            "Foreign owner binding failed");
        check(ArenaSystems.kindForFacility("dread-works")==WORKS&&
            "dread-works".equals(ArenaSystems.factionFacility(WORKS))&&
            "dread-ward".equals(ArenaSystems.firstProcess(WORKS)),
            "Dread works physical kind and process mapping absent");
        var process=CraftingEraRegistry.process("dread-ward");
        check(process!=null&&process.facility().equals("dread-works")&&
            process.utilityDemand()==25,
            "Penitent Incursion production must retain its genuine 25-load bill");

        double[] at=site(host);double x=at[0],y=at[1];
        seed(host,ArenaSystems.CAMP_STAKE,x-2,y,owner);
        World.Device terror=seed(host,ArenaSystems.TERROR_COLLECTOR,x,y+2,owner);
        seed(host,ArenaSystems.ASSEMBLY_LINE,x+2,y,owner);
        World.Device own=seed(host,ArenaSystems.STORAGE_CRATE,x,y-2,owner);
        World.Device split=seed(host,ArenaSystems.STORAGE_CRATE,x-2,y-2,owner);
        World.Device alien=seed(host,ArenaSystems.STORAGE_CRATE,x+2,y-2,foreign);
        Map<SurvivalCrafting.Item,Integer> cost=bill();
        for(var item:cost.entrySet()){
            int amount=item.getValue();
            if(item.getKey()==SurvivalCrafting.Item.FIELD_STONE)amount--;
            check(own.storage.delivered(item.getKey(),amount),
                "Cannot stage owned "+item.getKey());
        }
        check(alien.storage.delivered(SurvivalCrafting.Item.FIELD_STONE,1),
            "Cannot stage foreign stone");

        boolean shortage=false;
        for(double dx=-2.8;dx<=2.8&&!shortage;dx+=1.4)
            for(double dy=-2.8;dy<=2.8;dy+=1.4){
                p.x=x+dx-1.1;p.y=y+dy;p.angle=0;
                if(host.reviewBuild(1,WORKS).reason().equals("NEEDS_STOCKED_OWN_CRATE")){
                    shortage=true;break;
                }
            }
        check(shortage,"No real Penitent site reached materials preflight");
        rival.x=p.x;rival.y=p.y;rival.angle=p.angle;
        check(host.reviewBuild(2,WORKS).reason().equals("WRONG_FACTION_FACILITY")&&
            !host.build(2,WORKS),"Unraveled operator built Penitent works");
        Map<SurvivalCrafting.Item,Integer> before=own.storage.snapshot();
        check(!host.build(1,WORKS)&&own.storage.snapshot().equals(before)&&
            split.storage.snapshot().isEmpty()&&
            alien.storage.count(SurvivalCrafting.Item.FIELD_STONE)==1,
            "Foreign crate funded the works or shortage partially debited");
        terror.ownerKey=foreign;
        check(host.reviewBuild(1,WORKS).reason().equals("MISSING_PHYSICAL_PREREQUISITE"),
            "Foreign terror collector counted as Penitent prerequisite");
        terror.ownerKey=owner;terror.hp=0;
        check(host.reviewBuild(1,WORKS).reason().equals("MISSING_PHYSICAL_PREREQUISITE"),
            "Destroyed terror collector counted as prerequisite");
        terror.hp=100;
        check(split.storage.delivered(SurvivalCrafting.Item.FIELD_STONE,1),
            "Cannot complete split owner-owned stone payment");

        boolean legal=false;
        for(double dx=-2.8;dx<=2.8&&!legal;dx+=1.4)
            for(double dy=-2.8;dy<=2.8;dy+=1.4){
                p.x=x+dx-1.1;p.y=y+dy;p.angle=0;
                if(host.reviewBuild(1,WORKS).allowed()){
                    legal=true;break;
                }
            }
        check(legal,"Paid Penitent works lacks a legal world footprint");
        int count=host.world.devices.size();
        var outcome=host.buildOutcome(1,WORKS);
        check(outcome.built()&&outcome.deviceIndex()==count&&
            host.world.devices.size()==count+1,
            "Host did not create exactly one paid Penitent device");
        World.Device works=host.world.devices.get(count);
        check(works.kind==WORKS&&owner.equals(works.ownerKey)&&works.hp==100&&
            own.storage.snapshot().isEmpty()&&split.storage.snapshot().isEmpty()&&
            alien.storage.count(SurvivalCrafting.Item.FIELD_STONE)==1,
            "Split-crate payment, owner or device identity corrupted");
        check(!host.build(1,WORKS)&&host.world.devices.size()==count+1,
            "Owner duplicated the same Incursion facility");

        for(var input:process.inputs().entrySet())
            check(own.storage.delivered(
                SurvivalCrafting.Item.valueOf(input.getKey()),input.getValue()),
                "Cannot stage genuine dread-ward inputs");
        Map<SurvivalCrafting.Item,Integer> staged=own.storage.snapshot();
        p.x=works.x;p.y=works.y;
        check(!host.startFactionBatch(1,"dread-ward")&&
            !works.productionActive&&own.storage.snapshot().equals(staged),
            "Unpowered Penitent works consumed materials or manufactured a ward");

        Path root=Files.createTempDirectory("veil-dread-works-");
        try{
            HostCheckpoint.save(host,root);
            ArenaSystems restarted=new ArenaSystems(seed);
            check(HostCheckpoint.load(restarted,root),
                "Penitent world checkpoint failed");
            check(restarted.world.devices.stream().filter(d->
                d.kind==WORKS&&d.hp>0&&owner.equals(d.ownerKey)).count()==1,
                "Restart lost or duplicated the paid Penitent device");
            check(restarted.world.devices.stream().anyMatch(d->
                d.kind==ArenaSystems.STORAGE_CRATE&&owner.equals(d.ownerKey)&&
                d.storage.snapshot().equals(staged)),
                "Restart lost unspent Penitent production inputs");
            check(restarted.world.devices.stream().anyMatch(d->
                d.kind==ArenaSystems.STORAGE_CRATE&&foreign.equals(d.ownerKey)&&
                d.storage.count(SurvivalCrafting.Item.FIELD_STONE)==1),
                "Restart transferred foreign stone to Penitent");
        }finally{clean(root);}
        System.out.println("PASS IncursionDreadWorksTest: paid split-crate Penitent facility, faction isolation, restart, no free power");
    }
}
