package veil;

import java.util.*;

/** Real manufactured panels -> locked delivery -> paid physical runway,
 * including checkpoint replay, wrong-faction and corruption rejection. */
public final class TransportFactoryDeliveryTest {
    private static void require(boolean ok,String why){
        if(!ok)throw new AssertionError(why);
    }
    private static TransportInfrastructure.Runway findRunway(
            TransportInfrastructure site,int owner){
        for(int y=16;y<ContinentLayout.SIZE-16;y+=2)
            for(int x=16;x<ContinentLayout.SIZE-28;x+=2){
                var runway=site.planRunway(owner,x,y,
                    TransportInfrastructure.Axis.EAST_WEST,13);
                if(runway!=null)return runway;
            }
        throw new AssertionError("No 78m wilderness construction site");
    }
    private static void manufacture(IndustrialWorkstation works,
            CraftingEraRegistry.Process recipe,String request){
        for(var e:recipe.inputs().entrySet())
            require(works.deposit(e.getKey(),e.getValue()),
                "Cannot deposit real component "+e.getKey());
        require(works.start(request,recipe.id()),"Factory cannot begin order");
        require(works.advance(1000,0,true)==IndustrialWorkstation.State.NO_POWER,
            "Powerless factory advanced physical parts");
        for(int t=0;t<recipe.seconds();t++)
            works.advance(1000,recipe.utilityDemand(),true);
        require(works.active()==null,"Factory did not finish work order");
    }
    public static void main(String[] args){
        int owner=83;
        var faction=CharacterRoster.Faction.THE_UNRAVELED;
        var terrain=ContinentLayout.generate(20261020L);
        var mist=new CityMiasma(terrain);
        var base=new TransportInfrastructure(terrain,mist,owner,faction,
            Map.of("FIELD_STONE",40,"FASTENERS",20));
        var strip=findRunway(base,owner);
        base.depositMeteredEnergy(100);
        require(!base.buildNextRunwaySection(owner,strip.id()),
            "Unmanufactured panel gave free physical runway");
        var source=new IndustrialWorkstation(TransportProduction.transportWorks(faction),
            TransportProduction.industryFaction(faction));
        var process=TransportProduction.recipe(
            TransportProduction.Product.RUNWAY_PANEL,faction);
        manufacture(source,process,"runway-job-0");
        require(source.count(process.output())==2,"No two real produced panels");
        var empty=base.stock();
        require(TransportFactoryDelivery.deliverToConstruction(owner+1,source,base,
            TransportProduction.Product.RUNWAY_PANEL,2,"transfer-0001")==
            TransportFactoryDelivery.Result.DENIED,
            "Foreign player stole factory output");
        require(source.count(process.output())==2&&base.stock().equals(empty),
            "Rejected transaction changed source or destination");
        require(TransportFactoryDelivery.deliverToConstruction(owner,source,base,
            TransportProduction.Product.RAIL_SECTION,2,"transfer-0002")==
            TransportFactoryDelivery.Result.NOT_READY,
            "Wrong product invented from other factory order");
        require(TransportFactoryDelivery.deliverToConstruction(owner,source,base,
            TransportProduction.Product.RUNWAY_PANEL,2,"transfer-0001")==
            TransportFactoryDelivery.Result.DELIVERED,
            "Factory cannot deliver produced runway panels");
        require(source.count(process.output())==0&&
            base.stock().get(process.output())==2,
            "Shipment did not debit source and credit exact destination");
        require(TransportFactoryDelivery.deliverToConstruction(owner,source,base,
            TransportProduction.Product.RUNWAY_PANEL,2,"transfer-0001")==
            TransportFactoryDelivery.Result.DUPLICATE,
            "Replayed shipment fabricated another pair of panels");
        require(base.buildNextRunwaySection(owner,strip.id()),
            "Physical runway construction did not consume delivered panel");
        require(base.runway(strip.id()).constructed()==1,
            "No constructed runway tile after transferred manufacturing");
        var savedBase=base.snapshot();
        var savedFactory=source.snapshot();
        var resumedBase=new TransportInfrastructure(terrain,mist,owner,faction,Map.of());
        var resumedSource=new IndustrialWorkstation(TransportProduction.transportWorks(faction),
            TransportProduction.industryFaction(faction));
        resumedBase.restore(savedBase);
        resumedSource.restore(savedFactory);
        require(resumedBase.snapshot().equals(savedBase),
            "Host restart changed paid runway or stock");
        require(resumedSource.snapshot().equals(savedFactory),
            "Host restart changed factory receipt or inventory");
        require(TransportFactoryDelivery.deliverToConstruction(owner,resumedSource,resumedBase,
            TransportProduction.Product.RUNWAY_PANEL,2,"transfer-0001")==
            TransportFactoryDelivery.Result.DUPLICATE,
            "Restart erased delivery receipts");
        require(resumedBase.buildNextRunwaySection(owner,strip.id()),
            "Second supplied runway tile could not be completed");
        require(!resumedBase.buildNextRunwaySection(owner,strip.id()),
            "Out-of-stock construction created a third segment");
        for(int i=1;i<=6;i++){
            manufacture(resumedSource,process,"runway-job-"+i);
            require(TransportFactoryDelivery.deliverToConstruction(owner,
                resumedSource,resumedBase,
                TransportProduction.Product.RUNWAY_PANEL,2,"transfer-"+(1001+i))==
                TransportFactoryDelivery.Result.DELIVERED,
                "Subsequent industrial delivery failed");
        }
        while(resumedBase.runway(strip.id()).constructed()<strip.count())
            require(resumedBase.buildNextRunwaySection(owner,strip.id()),
                "Cannot complete real transported runway components");
        require(resumedBase.operationalRunway(strip.id(),
            AirPowerCatalog.get("unraveled_needlewing")),
            "Complete real factory-funded runway rejected aircraft");
        var beforeInvalid=resumedBase.snapshot();
        var malformed=new TransportInfrastructure.Snapshot(beforeInvalid.worldSeed(),
            beforeInvalid.ownerId(),beforeInvalid.faction(),beforeInvalid.stock(),
            beforeInvalid.segments().subList(1,beforeInvalid.segments().size()),
            beforeInvalid.runways(),beforeInvalid.deliveryReceipts(),
            beforeInvalid.nextRunwayId(),beforeInvalid.constructionEnergy());
        boolean rejected=false;
        try{resumedBase.restore(malformed);}catch(IllegalArgumentException e){rejected=true;}
        require(rejected&&beforeInvalid.equals(resumedBase.snapshot()),
            "Corrupt checkpoint mutated live paid infrastructure");
        var wrongSeed=new TransportInfrastructure.Snapshot(beforeInvalid.worldSeed()+1,
            beforeInvalid.ownerId(),beforeInvalid.faction(),beforeInvalid.stock(),
            beforeInvalid.segments(),beforeInvalid.runways(),
            beforeInvalid.deliveryReceipts(),beforeInvalid.nextRunwayId(),
            beforeInvalid.constructionEnergy());
        rejected=false;
        try{resumedBase.restore(wrongSeed);}catch(IllegalArgumentException e){rejected=true;}
        require(rejected&&beforeInvalid.equals(resumedBase.snapshot()),
            "Foreign world snapshot loaded into another continent");
        var other=new IndustrialWorkstation("penitent-procession-works",
            CraftingEraRegistry.Faction.PENITENT);
        require(TransportFactoryDelivery.deliverToConstruction(owner,other,resumedBase,
            TransportProduction.Product.RUNWAY_PANEL,1,"foreign-1001")==
            TransportFactoryDelivery.Result.DENIED,
            "Enemy faction plant supplied Unraveled runway");
        System.out.println("PASS TRANSPORT_FACTORY_DELIVERY real manufacture, "+
            "host-owned debit, 13 built panels, save/restart, replay and corrupt restore");
    }
}
