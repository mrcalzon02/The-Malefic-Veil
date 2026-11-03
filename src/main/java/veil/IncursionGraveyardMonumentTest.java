package veil;

import java.nio.file.*;
import java.util.*;

/** Host acceptance for paid physical infirmary and graveyard commissioning.
 * The future graveyard death-recovery effect remains a separate gameplay gate. */
public final class IncursionGraveyardMonumentTest {
    private static final int MONUMENT=ArenaSystems.MONUMENT_GRAVEYARD;
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private static World.Device fixture(ArenaSystems host,int kind,
            double x,double y,String owner){
        World.Device d=new World.Device(x,y,kind);
        d.ownerKey=owner;host.world.devices.add(d);return d;
    }
    private static boolean open(ArenaSystems host,double x,double y){
        return !host.world.detailedCity(x,y)&&host.world.canWalk(x,y)&&
            !host.world.worldStructureAt(x,y,.55);
    }
    private static double[] site(ArenaSystems host){
        double[][] offsets={{0,0},{-1.1,0},{-3.5,0},
            {-3,3},{-1,3},{1,3},{3,3},{4,5},{-2.1,3},
            {-2.5,-2},{0,-2.5},{2.5,-2},
            {-4.5,-2.5},{4.5,-2.5},{0,1.5},{0,-4}};
        for(int x=-124;x<=224;x+=6)for(int y=-124;y<=224;y+=6){
            boolean good=true;
            for(double[] offset:offsets)
                if(!open(host,x+offset[0],y+offset[1])){good=false;break;}
            if(good&&host.world.lineClear(x-1.1,y,x,y)&&
                host.world.lineClear(x-2.1,y+3,x-1,y+3))
                return new double[]{x,y};
        }
        throw new AssertionError("No legal wilderness monument parcel");
    }
    private static void fill(World.Device crate,SurvivalCrafting.Item item,int n){
        check(crate.storage.delivered(item,n),"Could not stage "+item+" x"+n);
    }
    private static List<Map<SurvivalCrafting.Item,Integer>> snapshots(World.Device... crates){
        return Arrays.stream(crates).map(d->d.storage.snapshot()).toList();
    }
    private static void cleanup(Path root)throws Exception{
        try(var walk=Files.walk(root)){
            for(Path p:walk.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);
        }
    }
    public static void main(String[] args)throws Exception{
        final long seed=20261101L;
        String owner="acct_"+"a".repeat(64),alien="acct_"+"f".repeat(64);
        ArenaSystems host=new ArenaSystems(seed);
        ArenaSystems.Player p=host.join(1);
        check(host.assignAccount(1,owner)&&
            host.enrollFaction(1,CharacterRoster.Faction.THE_UNRAVELED),
            "Authenticated monument owner");
        ArenaSystems.Player rival=host.join(2);
        check(host.assignAccount(2,alien)&&
            host.enrollFaction(2,CharacterRoster.Faction.THE_PENITENT),
            "Foreign account identity");
        var spec=CraftingEraRegistry.facility("monument-graveyard");
        check(spec!=null&&spec.faction()==CraftingEraRegistry.Faction.ALL&&
            spec.utilityDemand()==30&&
            ArenaSystems.kindForFacility("monument-graveyard")==MONUMENT&&
            ArenaSystems.kindForFacility("infirmary")==ArenaSystems.INFIRMARY,
            "Monument and infirmary must be real mapped host devices");
        check(GameplayMeshes.DEVICE.length>MONUMENT&&
            "facility-monument-graveyard".equals(GameplayMeshes.DEVICE[MONUMENT])&&
            "facility-infirmary".equals(GameplayMeshes.DEVICE[ArenaSystems.INFIRMARY]),
            "Physical world mesh identities absent");
        double[] at=site(host);double x=at[0],y=at[1];
        fixture(host,ArenaSystems.CAMP_STAKE,x-3.5,y,owner);
        fixture(host,ArenaSystems.RESEARCH_ARCHIVE,x-3,y+3,owner);
        fixture(host,ArenaSystems.SIGNAL_RELAY,x+1,y+3,owner);
        fixture(host,ArenaSystems.COMMAND_HALL,x+3,y+3,owner);
        fixture(host,ArenaSystems.WATER_WORKS,x+4,y+5,owner);
        World.Device a=fixture(host,ArenaSystems.STORAGE_CRATE,x-2.5,y-2,owner);
        World.Device b=fixture(host,ArenaSystems.STORAGE_CRATE,x,y-2.5,owner);
        World.Device c=fixture(host,ArenaSystems.STORAGE_CRATE,x+2.5,y-2,owner);
        World.Device first=fixture(host,ArenaSystems.GENERATOR,x-4.5,y-2.5,owner);
        World.Device second=fixture(host,ArenaSystems.GENERATOR,x+4.5,y-2.5,owner);
        World.Device bus=fixture(host,ArenaSystems.ELECTRIC_BUS,x,y+1.5,owner);
        fixture(host,ArenaSystems.STEAM_BOILER,x,y-4,owner);

        // The catalog-only infirmary was an actual monument dependency blocker.
        p.x=x-1.1;p.y=y;p.angle=0;
        check(host.reviewBuild(1,MONUMENT).reason().equals("MISSING_PHYSICAL_PREREQUISITE"),
            "Monument bypassed its unbuilt infirmary");
        var infirmary=CraftingEraRegistry.facility("infirmary");
        check(infirmary!=null&&infirmary.requires().containsAll(
            List.of("water-works","command-hall")),"Infirmary tech prerequisites");
        for(var item:infirmary.materials().entrySet())
            fill(a,SurvivalCrafting.Item.valueOf(item.getKey()),item.getValue());
        p.x=x-2.1;p.y=y+3;p.angle=0;
        var infirmaryReview=host.reviewBuild(1,ArenaSystems.INFIRMARY);
        check(infirmaryReview.allowed(),"Paid infirmary preflight: "+infirmaryReview.reason());
        int beforeCount=host.world.devices.size();
        var ward=host.buildOutcome(1,ArenaSystems.INFIRMARY);
        check(ward.built()&&ward.deviceIndex()==beforeCount&&
            host.world.devices.get(ward.deviceIndex()).kind==ArenaSystems.INFIRMARY&&
            a.storage.snapshot().isEmpty(),"Infirmary was not paid and installed");
        World.Device builtWard=host.world.devices.get(ward.deviceIndex());
        check(!host.build(1,ArenaSystems.INFIRMARY),
            "Second unpaid infirmary was accepted");

        // Two real fuel purchases: one boiler startup, then two generators.
        fill(b,SurvivalCrafting.Item.FIREWOOD,3);
        fill(b,SurvivalCrafting.Item.DIRTY_WATER,2);
        p.x=x;p.y=y-4;
        check(host.fireSteamBoiler(1),"Boiler did not consume real fuel");
        fill(b,SurvivalCrafting.Item.CHARCOAL,6);
        fill(b,SurvivalCrafting.Item.CLEAN_WATER,4);
        p.x=first.x;p.y=first.y;
        check(host.chargeGenerator(1),"First generator not charged from paid fuel");
        p.x=second.x;p.y=second.y;
        check(host.chargeGenerator(1),"Second generator not charged from paid fuel");
        check(b.storage.snapshot().isEmpty()&&
            first.electricMs==ElectricUtility.MAX_CHARGE_MS&&
            second.electricMs==ElectricUtility.MAX_CHARGE_MS,
            "Fuel was not fully debited or power minted incorrectly");

        // The actual 384-unit bill needs all three capped physical crates.
        fill(a,SurvivalCrafting.Item.FIELD_STONE,128);
        fill(b,SurvivalCrafting.Item.FIELD_STONE,12);
        fill(b,SurvivalCrafting.Item.TIMBER,90);
        fill(b,SurvivalCrafting.Item.FIBER,26);
        fill(c,SurvivalCrafting.Item.FIBER,34);
        fill(c,SurvivalCrafting.Item.METAL_STOCK,24);
        fill(c,SurvivalCrafting.Item.MACHINE_PARTS,16);
        fill(c,SurvivalCrafting.Item.MEDICINAL_HERBS,30);
        fill(c,SurvivalCrafting.Item.RAW_ORE,24);
        check(a.storage.units()==128&&b.storage.units()==128&&c.storage.units()==128&&
            spec.materials().get("FIELD_STONE")==140,"Incorrect capped monument bill");
        p.x=x-1.1;p.y=y;p.angle=0;
        rival.x=p.x;rival.y=p.y;rival.angle=0;
        check(!host.build(2,MONUMENT),"Foreign account spent owned monument resources");
        var untouched=snapshots(a,b,c);
        bus.ownerKey=alien;
        check(host.reviewBuild(1,MONUMENT).reason().equals("NEEDS_POWERED_NETWORK")&&
            !host.build(1,MONUMENT)&&snapshots(a,b,c).equals(untouched),
            "Foreign power bus commissioned monument or debited stock");
        bus.ownerKey=owner;
        long firstPower=first.electricMs;
        first.electricMs=0;
        check(host.reviewBuild(1,MONUMENT).reason().equals("NEEDS_POWERED_NETWORK")&&
            !host.build(1,MONUMENT)&&snapshots(a,b,c).equals(untouched),
            "One 24-load generator bypassed 30-load power or debited stock");
        first.electricMs=firstPower;
        check(a.storage.consume(Map.of(SurvivalCrafting.Item.FIELD_STONE,1)),
            "Cannot stage missing-stone regression");
        var shortage=snapshots(a,b,c);
        check(host.reviewBuild(1,MONUMENT).reason().equals("NEEDS_STOCKED_OWN_CRATE")&&
            !host.build(1,MONUMENT)&&snapshots(a,b,c).equals(shortage),
            "383 units partially debited or built a free monument");
        fill(a,SurvivalCrafting.Item.FIELD_STONE,1);

        // A successful review is not permission to spend a crate after its
        // ownership changes. The host must revalidate the entire 384-unit
        // payment and must not debit another account or commissioning power.
        check(host.reviewBuild(1,MONUMENT).allowed(),
            "Complete owned stock was not initially eligible");
        var beforeOwnerChange=snapshots(a,b,c);
        int beforeOwnerChangeDevices=host.world.devices.size();
        long beforeOwnerChangeFirstPower=first.electricMs;
        long beforeOwnerChangeSecondPower=second.electricMs;
        c.ownerKey=alien;
        var foreignCrateAttempt=host.buildOutcome(1,MONUMENT);
        check(!foreignCrateAttempt.built()&&
            "NEEDS_STOCKED_OWN_CRATE".equals(foreignCrateAttempt.review().reason())&&
            host.world.devices.size()==beforeOwnerChangeDevices&&
            snapshots(a,b,c).equals(beforeOwnerChange)&&
            first.electricMs==beforeOwnerChangeFirstPower&&
            second.electricMs==beforeOwnerChangeSecondPower,
            "Stale review spent foreign crate stock, power, or placed a free monument");
        c.ownerKey=owner;
        check(host.reviewBuild(1,MONUMENT).allowed()&&
            snapshots(a,b,c).equals(beforeOwnerChange),
            "Restoring ownership failed to restore the exact paid build preflight");

        // A review is advisory, not a reservation. Revalidate the same
        // physical 384-unit bill at the instant of host commissioning.
        int staleCount=host.world.devices.size();
        long staleFirstPower=first.electricMs,staleSecondPower=second.electricMs;
        var staleStock=snapshots(a,b,c);
        // Device coordinates are immutable: emulate a host move by swapping
        // in a relocated physical crate carrying the exact same stock.
        int crateIndex=host.world.devices.indexOf(c);
        World.Device relocated=new World.Device(x+8,c.y,ArenaSystems.STORAGE_CRATE);
        relocated.ownerKey=owner;
        relocated.storage.restore(c.storage.snapshot());
        check(host.reviewBuild(1,MONUMENT).allowed(),"Moved-crate review fixture");
        host.world.devices.set(crateIndex,relocated);
        var movedCrate=host.buildOutcome(1,MONUMENT);
        check(!movedCrate.built()&&
            "NEEDS_STOCKED_OWN_CRATE".equals(movedCrate.review().reason())&&
            host.world.devices.size()==staleCount&&
            snapshots(a,b,c).equals(staleStock)&&
            first.electricMs==staleFirstPower&&second.electricMs==staleSecondPower,
            "Moved third crate partially paid or commissioned a monument");
        host.world.devices.set(crateIndex,c);

        check(host.reviewBuild(1,MONUMENT).allowed(),"Destroyed-crate review fixture");
        int originalCrateHp=c.hp;
        c.hp=0;
        var unavailableCrate=host.buildOutcome(1,MONUMENT);
        check(!unavailableCrate.built()&&
            "NEEDS_STOCKED_OWN_CRATE".equals(unavailableCrate.review().reason())&&
            host.world.devices.size()==staleCount&&
            snapshots(a,b,c).equals(staleStock)&&
            first.electricMs==staleFirstPower&&second.electricMs==staleSecondPower,
            "Unavailable third crate partially paid or commissioned a monument");
        c.hp=originalCrateHp;

        check(host.reviewBuild(1,MONUMENT).allowed(),"Spent-stone review fixture");
        check(a.storage.consume(Map.of(SurvivalCrafting.Item.FIELD_STONE,1)),
            "Could not stage post-review material consumption");
        var depletedStock=snapshots(a,b,c);
        var spentStone=host.buildOutcome(1,MONUMENT);
        check(!spentStone.built()&&
            "NEEDS_STOCKED_OWN_CRATE".equals(spentStone.review().reason())&&
            host.world.devices.size()==staleCount&&
            snapshots(a,b,c).equals(depletedStock)&&
            first.electricMs==staleFirstPower&&second.electricMs==staleSecondPower,
            "Post-review stone consumption caused a partial debit or free monument");
        fill(a,SurvivalCrafting.Item.FIELD_STONE,1);
        check(snapshots(a,b,c).equals(staleStock)&&
            host.reviewBuild(1,MONUMENT).allowed(),
            "Restored crates did not restore exact 384-unit eligibility");

        var charge=ElectricUtility.allocation(host.world.devices,
            new World.Device(x,y,MONUMENT),30,Map.of(),100);
        // The temporary probe needs owner identity, just like the host review.
        World.Device probe=new World.Device(x,y,MONUMENT);probe.ownerKey=owner;
        charge=ElectricUtility.allocation(host.world.devices,probe,30,Map.of(),100);
        check(charge.size()==2&&charge.values().stream().mapToInt(Integer::intValue).sum()==30,
            "Commissioning did not reserve two actual generators at 30 load");
        var review=host.reviewBuild(1,MONUMENT);
        check(review.allowed(),"Fully paid monument refused: "+review.reason());
        // Persist the fully funded, unbuilt world first. A crash after
        // in-memory payment but before a committed checkpoint must restore
        // ALL stock and power with NO monument, never a torn mixture.
        Path root=Files.createTempDirectory("veil-graveyard-");
        try{
            HostCheckpoint.save(host,root);
            int count=host.world.devices.size();
            var built=host.buildOutcome(1,MONUMENT);
            check(built.built()&&built.deviceIndex()==count&&
                host.world.devices.get(count).kind==MONUMENT&&
                owner.equals(host.world.devices.get(count).ownerKey)&&
                a.storage.snapshot().isEmpty()&&b.storage.snapshot().isEmpty()&&
                c.storage.snapshot().isEmpty(),
                "Host did not atomically pay 384 and build monument");
            check(first.electricMs==firstPower-100&&
                second.electricMs==ElectricUtility.MAX_CHARGE_MS-100,
                "Monument commissioning failed to debit real generator reserves");
            check(!host.build(1,MONUMENT)&&host.world.devices.size()==count+1,
                "Monument duplicated without another payment");
            builtWard.hp=0;
            check(host.world.devices.get(count).hp>0,
                "Existing monument incorrectly vanished with an infirmary outage");

            Path incomplete=Files.createDirectory(root.resolve("ledger-stage-incomplete"));
            Files.writeString(incomplete.resolve("realm.dat"),"unfinished");
            ArenaSystems beforeCommit=new ArenaSystems(seed);
            check(HostCheckpoint.load(beforeCommit,root),
                "Crash-before-commit checkpoint read failed");
            check(beforeCommit.world.devices.stream().noneMatch(d->
                d.kind==MONUMENT&&d.hp>0&&owner.equals(d.ownerKey))&&
                beforeCommit.world.devices.stream().filter(d->
                d.kind==ArenaSystems.STORAGE_CRATE&&owner.equals(d.ownerKey))
                .mapToInt(d->d.storage.units()).sum()==384&&
                beforeCommit.world.devices.stream().filter(d->
                d.kind==ArenaSystems.GENERATOR&&owner.equals(d.ownerKey))
                .mapToLong(d->d.electricMs).sum()==2*ElectricUtility.MAX_CHARGE_MS,
                "Crash before commit restored a partly paid monument or lost stock/power");

            HostCheckpoint.save(host,root);
            // A later torn stage cannot undo or duplicate the committed
            // construction. The pointer must still name one complete pair.
            Files.writeString(incomplete.resolve("accounts.dat"),"unfinished");
            ArenaSystems resumed=new ArenaSystems(seed);
            check(HostCheckpoint.load(resumed,root),"Monument checkpoint read failed");
            check(resumed.world.devices.stream().filter(d->
                d.kind==MONUMENT&&d.hp>0&&owner.equals(d.ownerKey)).count()==1,
                "Save/restart lost or duplicated the paid monument");
            check(resumed.world.devices.stream().filter(d->
                d.kind==ArenaSystems.STORAGE_CRATE&&owner.equals(d.ownerKey)).count()==3&&
                resumed.world.devices.stream().filter(d->
                d.kind==ArenaSystems.STORAGE_CRATE&&owner.equals(d.ownerKey))
                .allMatch(d->d.storage.snapshot().isEmpty()),
                "Restart refunded the spent 384 construction units");
            check(resumed.world.devices.stream().filter(d->
                d.kind==ArenaSystems.GENERATOR&&owner.equals(d.ownerKey))
                .mapToLong(d->d.electricMs).sum()==
                2*ElectricUtility.MAX_CHARGE_MS-200,
                "Restart refunded paid commissioning power");
        }finally{cleanup(root);}
        System.out.println("PASS IncursionGraveyardMonumentTest: paid infirmary, 384 capped units, stale-review isolation, crash-before/after-commit, 30-load power and restart");
    }
}
