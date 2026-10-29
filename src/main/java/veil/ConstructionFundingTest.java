package veil;

import java.nio.file.*;
import java.util.*;

/** The Incursion monument's actual 384-unit bill must be payable from
 * three ordinary 128-unit crates, with no partial charge on refusal.
 * This tests the funding prerequisite, NOT an unlocked monument device. */
public final class ConstructionFundingTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private static void fill(CampStockpile crate,SurvivalCrafting.Item item,int count){
        check(crate.delivered(item,count),"Could not stage "+item+" x"+count);
    }
    private static Map<SurvivalCrafting.Item,Integer> monumentBill(){
        var spec=CraftingEraRegistry.facility("monument-graveyard");
        check(spec!=null,"Incursion monument blueprint missing");
        EnumMap<SurvivalCrafting.Item,Integer> result=
            new EnumMap<>(SurvivalCrafting.Item.class);
        for(var entry:spec.materials().entrySet())
            result.put(SurvivalCrafting.Item.valueOf(entry.getKey()),
                entry.getValue());
        return result;
    }
    private static void unchanged(List<CampStockpile> crates,
            List<Map<SurvivalCrafting.Item,Integer>> before,String message){
        for(int i=0;i<crates.size();i++)
            check(crates.get(i).snapshot().equals(before.get(i)),message+" crate "+i);
    }
    private static List<Map<SurvivalCrafting.Item,Integer>> snapshots(
            List<CampStockpile> crates){
        return crates.stream().map(CampStockpile::snapshot).toList();
    }
    public static void main(String[] args)throws Exception{
        var bill=monumentBill();
        check(bill.get(SurvivalCrafting.Item.FIELD_STONE)==140,
            "Actual monument cost no longer exercises the 128-unit stack cap");
        check(bill.values().stream().mapToInt(Integer::intValue).sum()==384,
            "Monument total changed; revise exact three-crate fixture");

        String owner="acct_"+"e".repeat(64);
        ArenaSystems host=new ArenaSystems(20261028L);
        check(host.assignAccount(host.join(1).id,owner),"Host owner binding");
        // DurableRealm correctly refuses city-owned construction. Select
        // three genuinely legal wilderness parcels rather than hard-coding
        // coordinates that may fall inside the generated city.
        List<double[]> parcels=new ArrayList<>();
        for(int x=World.SIZE+3;x<World.SIZE+60&&parcels.size()<3;x++)
            for(int y=2;y<World.SIZE+50&&parcels.size()<3;y++){
                if(host.world.detailedCity(x,y)||!host.world.canWalk(x,y)||
                   host.world.worldStructureAt(x,y,.55))continue;
                boolean clear=true;
                for(double[] at:parcels)
                    if(Math.hypot(at[0]-x,at[1]-y)<2){clear=false;break;}
                if(clear)parcels.add(new double[]{x,y});
            }
        check(parcels.size()==3,"Could not find three valid wilderness crate parcels");
        List<CampStockpile> crates=new ArrayList<>();
        for(double[] at:parcels){
            World.Device device=new World.Device(at[0],at[1],
                ArenaSystems.STORAGE_CRATE);
            device.ownerKey=owner;
            host.world.devices.add(device);
            crates.add(device.storage);
        }
        CampStockpile a=crates.get(0),b=crates.get(1),c=crates.get(2);
        fill(a,SurvivalCrafting.Item.FIELD_STONE,128);
        fill(b,SurvivalCrafting.Item.FIELD_STONE,12);
        fill(b,SurvivalCrafting.Item.TIMBER,90);
        fill(b,SurvivalCrafting.Item.FIBER,26);
        fill(c,SurvivalCrafting.Item.FIBER,34);
        fill(c,SurvivalCrafting.Item.METAL_STOCK,24);
        fill(c,SurvivalCrafting.Item.MACHINE_PARTS,16);
        fill(c,SurvivalCrafting.Item.MEDICINAL_HERBS,30);
        fill(c,SurvivalCrafting.Item.RAW_ORE,24);
        for(CampStockpile crate:crates)
            check(crate.units()==CampStockpile.MAX_UNITS,
                "Monument stock was not staged in real capped crates");

        var before=snapshots(crates);
        check(!ConstructionFunding.pay(List.of(a,b),bill),
            "Removed third crate somehow funded a full monument");
        unchanged(crates,before,"Removed crate partially debited");
        check(!ConstructionFunding.pay(List.of(a,a,b,c),bill),
            "Duplicate reference counted one crate twice");
        unchanged(crates,before,"Duplicate crate partially debited");
        check(a.consume(Map.of(SurvivalCrafting.Item.FIELD_STONE,1)),
            "Cannot stage one-unit shortage");
        var shortStock=snapshots(crates);
        check(!ConstructionFunding.pay(crates,bill),
            "One missing stone should reject the entire bill");
        unchanged(crates,shortStock,"Shortage partially debited");
        fill(a,SurvivalCrafting.Item.FIELD_STONE,1);

        check(ConstructionFunding.pay(crates,bill),
            "Three genuine 128-unit crates could not pay 384-unit bill");
        for(CampStockpile crate:crates)
            check(crate.snapshot().isEmpty(),"Exact monument debit left stock");
        check(!ConstructionFunding.pay(crates,bill),
            "Already-paid crates were charged twice");

        Path root=Files.createTempDirectory("veil-incursion-funding-");
        try{
            HostCheckpoint.save(host,root);
            ArenaSystems restarted=new ArenaSystems(20261028L);
            check(HostCheckpoint.load(restarted,root),
                "Committed multi-crate stock debit could not be restored");
            check(restarted.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&
                    owner.equals(d.ownerKey)).count()==3,
                "Restart lost one of the original three crates");
            check(restarted.world.devices.stream()
                .filter(d->d.kind==ArenaSystems.STORAGE_CRATE&&
                    owner.equals(d.ownerKey))
                .allMatch(d->d.storage.snapshot().isEmpty()),
                "Restart restored previously spent construction materials");
        }finally{
            try(var walk=Files.walk(root)){
                for(Path file:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(file);
            }
        }
        System.out.println("PASS ConstructionFundingTest: exact 384-unit monument bill, 3 capped crates, refusal atomicity and checkpoint");
    }
}
