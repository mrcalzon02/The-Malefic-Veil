package veil;

import java.nio.file.*;
import java.util.*;

/** Functional host construction of the complete first shared E2 prerequisite
 * graph, actual material debits and multiple recipes in one forge. No cosmetic
 * pixel/shape gates. The origin camp/warehouse are trusted fixtures; both were
 * already end-to-end acceptance-tested via player crafting and placement. */
public final class PhysicalFacilityDependencyTest {
    private static void check(boolean yes,String message){
        if(!yes)throw new AssertionError(message);
    }
    private static void ticks(ArenaSystems host,int seconds){
        for(int i=0;i<seconds;i++)host.advance(1000);
    }
    private static World.Device seeded(ArenaSystems host,int kind,
            double x,double y,String owner){
        World.Device d=new World.Device(x,y,kind);d.ownerKey=owner;
        host.world.devices.add(d);return d;
    }
    private static double[] site(ArenaSystems host){
        for(int x=-124;x<=224;x+=6)
            for(int y=-124;y<=224;y+=6){
                if(host.world.detailedCity(x,y)||!host.world.canWalk(x,y))continue;
                int free=0;
                for(double dx=-4.2;dx<=4.2;dx+=1.4)
                    for(double dy=-4.2;dy<=4.2;dy+=1.4){
                        double xx=x+dx,yy=y+dy;
                        if(host.world.canWalk(xx,yy)&&
                           !host.world.worldStructureAt(xx,yy,.55))free++;
                    }
                if(free>=42)return new double[]{x,y};
            }
        throw new AssertionError("No wilderness parcel with sufficient legal plots");
    }
    private static World.Device paidBuild(ArenaSystems host,ArenaSystems.Player p,
                        CampStockpile crate,double x,double y,int kind){
        String id=ArenaSystems.factionFacility(kind);
        var blueprint=CraftingEraRegistry.facility(id);
        check(blueprint!=null,"Missing authoritative facility blueprint "+kind);
        for(var entry:blueprint.materials().entrySet())
            check(crate.delivered(SurvivalCrafting.Item.valueOf(entry.getKey()),
                entry.getValue()),"Cannot fund physical "+id+" "+entry.getKey());
        for(double dx=-4.2;dx<=4.2;dx+=1.4)
            for(double dy=-4.2;dy<=4.2;dy+=1.4){
                p.x=x+dx-1.1;p.y=y+dy;p.angle=0;
                var review=host.reviewBuild(p.id,kind);
                if(review.allowed()){
                    check(host.build(p.id,kind),"Host rejected its authoritative build preflight");
                    for(var entry:blueprint.materials().entrySet())
                        check(crate.count(SurvivalCrafting.Item.valueOf(entry.getKey()))==0,
                            "Paid "+id+" stock was not consumed");
                    World.Device built=host.world.devices.get(host.world.devices.size()-1);
                    check(built.kind==kind&&p.accountKey.equals(built.ownerKey),
                        "Physical facility ownership corrupted after construction");
                    check(!host.build(p.id,kind),
                        "Duplicate physical facility was placed without cost");
                    return built;
                }
            }
        throw new AssertionError("No legal near-camp footprint for "+id);
    }
    /** A construction can consume a split material bill, but only with
     * complete same-account stock in physically nearby persistent crates.
     * Unlike E5 factory recipes, no single crate must hold all ingredients. */
    private static void splitWarehouseConstruction()throws Exception{
        final long seed=20261020L;
        String owner="acct_"+"e".repeat(64),foreign="acct_"+"a".repeat(64);
        ArenaSystems host=new ArenaSystems(seed);
        ArenaSystems.Player p=host.join(1);
        check(host.assignAccount(1,owner),
            "Split construction operator needs host account ownership");
        double[] point=site(host);
        double x=point[0],y=point[1];
        seeded(host,ArenaSystems.CAMP_STAKE,x-1.2,y,owner);
        World.Device timber=seeded(host,ArenaSystems.STORAGE_CRATE,x+1.2,y,owner);
        World.Device stone=seeded(host,ArenaSystems.STORAGE_CRATE,x-1.2,y+1.3,owner);
        World.Device alien=seeded(host,ArenaSystems.STORAGE_CRATE,x+1.2,y+1.3,foreign);
        check(timber.storage.delivered(SurvivalCrafting.Item.TIMBER,12)&&
            stone.storage.delivered(SurvivalCrafting.Item.FIELD_STONE,12)&&
            stone.storage.delivered(SurvivalCrafting.Item.FIBER,6)&&
            alien.storage.delivered(SurvivalCrafting.Item.FIELD_STONE,3),
            "Could not stage authentically owned split construction inputs");
        // Pick a legal wilderness footprint while all genuine materials
        // exist, then hold those exact player coordinates for denied/paid
        // preflights. The previous single-crate rule made this impossible.
        boolean available=false;
        for(double dx=-4.2;dx<=4.2&&!available;dx+=1.4)
            for(double dy=-4.2;dy<=4.2;dy+=1.4){
                p.x=x+dx-1.1;p.y=y+dy;p.angle=0;
                if(host.reviewBuild(1,ArenaSystems.CLAIM_HEART).allowed()){
                    available=true;break;
                }
            }
        check(available,"Two owner crates could not fund a legal claim heart");
        int before=host.world.devices.size();
        check(stone.storage.consume(Map.of(SurvivalCrafting.Item.FIELD_STONE,1)),
            "Cannot create a real one-unit material deficit");
        Map<SurvivalCrafting.Item,Integer> timberBefore=timber.storage.snapshot();
        Map<SurvivalCrafting.Item,Integer> stoneBefore=stone.storage.snapshot();
        Map<SurvivalCrafting.Item,Integer> alienBefore=alien.storage.snapshot();
        check(host.reviewBuild(1,ArenaSystems.CLAIM_HEART).reason().equals(
                "NEEDS_STOCKED_OWN_CRATE")&&
            !host.build(1,ArenaSystems.CLAIM_HEART)&&
            host.world.devices.size()==before&&
            timber.storage.snapshot().equals(timberBefore)&&
            stone.storage.snapshot().equals(stoneBefore)&&
            alien.storage.snapshot().equals(alienBefore),
            "Insufficient owned stock or foreign materials partially funded construction");
        check(stone.storage.delivered(SurvivalCrafting.Item.FIELD_STONE,1),
            "Cannot reintroduce the one genuinely missing owned stone");
        ArenaSystems.BuildOutcome outcome=host.buildOutcome(1,ArenaSystems.CLAIM_HEART);
        check(outcome.built()&&outcome.deviceIndex()==before&&
            timber.storage.snapshot().isEmpty()&&stone.storage.snapshot().isEmpty()&&
            alien.storage.snapshot().equals(alienBefore)&&
            host.world.devices.get(before).kind==ArenaSystems.CLAIM_HEART&&
            owner.equals(host.world.devices.get(before).ownerKey),
            "Split construction did not debit both exact owned crates once");
        check(!host.build(1,ArenaSystems.CLAIM_HEART)&&
            host.world.devices.size()==before+1,
            "Constructed claim heart can be duplicated without paying again");
        Path root=Files.createTempDirectory("veil-split-construction-");
        try{
            HostCheckpoint.save(host,root);
            ArenaSystems restored=new ArenaSystems(seed);
            check(HostCheckpoint.load(restored,root),
                "Paid split construction did not survive durable checkpoint");
            check(restored.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.CLAIM_HEART&&owner.equals(d.ownerKey))
                .count()==1&&
                restored.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&owner.equals(d.ownerKey))
                .allMatch(d->d.storage.snapshot().isEmpty())&&
                restored.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&foreign.equals(d.ownerKey))
                .mapToInt(d->d.storage.count(SurvivalCrafting.Item.FIELD_STONE)).sum()==3,
                "Restart duplicated materials or crossed an account's warehouse boundary");
        }finally{
            try(var walk=Files.walk(root)){
                for(Path file:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(file);
            }
        }
    }
    public static void main(String[] args)throws Exception{
        long seed=20261020L;
        String owner="acct_"+"9".repeat(64);
        ArenaSystems host=new ArenaSystems(seed);
        ArenaSystems.Player p=host.join(1);
        check(host.assignAccount(1,owner),"Fixture requires authenticated owner");
        double[] site=site(host);
        double x=site[0],y=site[1];
        seeded(host,ArenaSystems.CAMP_STAKE,x-1.2,y,owner);
        World.Device crate=seeded(host,ArenaSystems.STORAGE_CRATE,x+1.2,y,owner);
        p.x=x;p.y=y;p.angle=0;
        check(host.reviewBuild(1,ArenaSystems.MATERIALS_RECYCLER).reason().equals(
            "MISSING_PHYSICAL_PREREQUISITE"),
            "Unbuilt late support facility bypassed its dependency");
        World.Device[] built=new World.Device[14];
        int[] order={
            ArenaSystems.CLAIM_HEART,ArenaSystems.HAND_WORKSHOP,
            ArenaSystems.DRY_STORE,ArenaSystems.COOKING_HEARTH,
            ArenaSystems.RAIN_CATCHER,ArenaSystems.CARGO_LAYDOWN,
            ArenaSystems.FORAGER_GARDEN,ArenaSystems.COMPOST_YARD,
            ArenaSystems.HERB_PLOT,ArenaSystems.CHARCOAL_KILN,
            ArenaSystems.STONE_FURNACE,ArenaSystems.FORGE,
            ArenaSystems.FREIGHT_BAY,ArenaSystems.MATERIALS_RECYCLER
        };
        for(int i=0;i<order.length;i++)
            built[i]=paidBuild(host,p,crate.storage,x,y,order[i]);
        for(var d:built){
            var spec=CraftingEraRegistry.facility(ArenaSystems.factionFacility(d.kind));
            for(String dependency:spec.requires())
                check(host.world.devices.stream().anyMatch(installed->
                    installed.hp>0&&installed.ownerKey.equals(owner)&&
                    dependency.equals(ArenaSystems.factionFacility(installed.kind))),
                    "Installed real "+spec.id()+" with missing "+dependency);
        }

        World.Device forge=built[11];
        String selected="fasteners";
        var fasteners=CraftingEraRegistry.process(selected);
        check(fasteners.facility().equals("forge"),
            "Forge work order must use canonical manufacturing definition");
        check(crate.storage.delivered(SurvivalCrafting.Item.METAL_BILLET,1),
            "Shared metal input cannot be deposited");
        p.x=forge.x+.4;p.y=forge.y;
        check(!host.startFactionBatch(1,"penitent-engrave-reliquary"),
            "Forged mismatched-facility recipe was accepted");
        check(host.startFactionBatch(1,selected),
            "Physically built forge cannot accept an exact registered order");
        check(forge.productionRecipeId.equals(selected)&&forge.productionActive&&
            crate.storage.count(SurvivalCrafting.Item.METAL_BILLET)==0,
            "Forge did not reserve real billet and correct job");
        ticks(host,fasteners.seconds());
        check(!forge.productionActive&&forge.productionRecipeId.isEmpty()&&
            forge.productionCompleted==1&&
            crate.storage.count(SurvivalCrafting.Item.FASTENERS)==fasteners.quantity(),
            "Active forge did not produce correct output exactly once");

        Path save=Files.createTempDirectory("veil-real-support-dag-");
        try{
            HostCheckpoint.save(host,save);
            ArenaSystems loaded=new ArenaSystems(seed);
            check(HostCheckpoint.load(loaded,save),
                "Complete physical support settlement could not be reloaded");
            check(loaded.world.devices.stream()
                .filter(d->d.kind>=ArenaSystems.CLAIM_HEART&&
                    d.kind<=ArenaSystems.MATERIALS_RECYCLER&&d.hp>0).count()==14,
                "Durable host lost or duplicated installed prerequisite structures");
            check(loaded.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE)
                .mapToInt(d->d.storage.count(SurvivalCrafting.Item.FASTENERS)).sum()==4,
                "Durable support workshop forgot paid forged components");
        }finally{
            try(var stream=Files.walk(save)){
                for(Path pth:stream.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(pth);
            }
        }
        splitWarehouseConstruction();
        System.out.println("PASS PhysicalFacilityDependencyTest: 14 support sites, split owned construction payments, denied partial/foreign costs, persisted paid forge and checkpoint");
    }
}
