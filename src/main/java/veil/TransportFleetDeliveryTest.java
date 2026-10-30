package veil;

import java.util.*;

/** Real paid transport operations from factory output to rail and naval actors.
 * Source/destination snapshot replay must never create duplicate rolling stock,
 * cargo, carrier hulls or fuel.
 */
public final class TransportFleetDeliveryTest {
    private static void check(boolean yes,String reason){
        if(!yes)throw new AssertionError(reason);
    }
    private static void make(IndustrialWorkstation station,
            TransportProduction.Product kind,CharacterRoster.Faction faction,
            String receipt){
        var p=TransportProduction.recipe(kind,faction);
        for(var line:p.inputs().entrySet()){
            int amount=line.getValue()-station.count(line.getKey());
            if(amount>0)check(station.deposit(line.getKey(),amount),
                "Cannot fund real transport process "+p.id());
        }
        check(station.start(receipt,p.id()),"Cannot run "+p.id());
        check(station.advance(1000,0,true)==IndustrialWorkstation.State.NO_POWER,
            "Powerless plant advanced "+p.id());
        for(int i=0;i<p.seconds();i++)
            station.advance(1000,p.utilityDemand(),true);
        check(station.active()==null&&station.count(p.output())>=p.quantity(),
            "Transport production failed "+p.id());
    }
    private static int[] railSite(ContinentLayout world,CityMiasma cloud){
        for(int y=20;y<ContinentLayout.SIZE-20;y++)
            for(int x=20;x<ContinentLayout.SIZE-24;x++){
                boolean good=true;
                for(int i=0;i<5;i++)
                    if(!world.buildableGround(x+i,y)||world.road(x+i,y)||
                       cloud.forbidsAircraft(x+i+.5,y+.5,0))good=false;
                if(good)return new int[]{x,y};
            }
        throw new AssertionError("No five-cell wilderness railway site");
    }
    private static int carrierSite(AircraftCarrier fleet,int owner){
        for(int y=20;y<ContinentLayout.SIZE-20;y++)
            for(int x=20;x<ContinentLayout.SIZE-24;x++){
                double cx=x+.5,cy=y+.5;
                if(fleet.clearSea(cx,cy,AircraftCarrier.Axis.EAST_WEST,-1)){
                    int id=fleet.construct(owner,cx,cy,AircraftCarrier.Axis.EAST_WEST);
                    if(id>0)return id;
                }
            }
        throw new AssertionError("No valid 18x5 ocean hull site");
    }
    public static void main(String[] args){
        int owner=63;
        var faction=CharacterRoster.Faction.THE_UNRAVELED;
        var layout=ContinentLayout.generate(20261020L);
        var cloud=new CityMiasma(layout);
        var trackStock=new HashMap<String,Integer>();
        trackStock.put(TransportProduction.item(
            TransportProduction.Product.RAIL_SECTION,faction),5);
        trackStock.put("TIMBER",12);
        var infrastructure=new TransportInfrastructure(layout,cloud,owner,faction,
            trackStock);
        infrastructure.depositMeteredEnergy(10);
        int[] site=railSite(layout,cloud);
        for(int i=0;i<5;i++)
            check(infrastructure.buildRail(owner,site[0]+i,site[1],
                TransportInfrastructure.Axis.EAST_WEST),
                "Five legitimate railway panels must be laid");
        RailTransport rail=new RailTransport(infrastructure,Map.of());
        var works=new IndustrialWorkstation(TransportProduction.transportWorks(faction),
            TransportProduction.industryFaction(faction));
        make(works,TransportProduction.Product.LOCOMOTIVE,faction,"rail-batch-loco");
        check(TransportFactoryDelivery.deliverToRail(owner+1,works,rail,
            TransportProduction.Product.LOCOMOTIVE,1,"rail-deliver-loco")==
            TransportFactoryDelivery.Result.DENIED,
            "Unauthorized railway factory delivery");
        check(works.count(TransportProduction.item(
            TransportProduction.Product.LOCOMOTIVE,faction))==1,
            "Unauthorized railway transfer debited source");
        check(TransportFactoryDelivery.deliverToRail(owner,works,rail,
            TransportProduction.Product.LOCOMOTIVE,1,"rail-deliver-loco")==
            TransportFactoryDelivery.Result.DELIVERED,
            "Produced locomotive cannot reach the rail depot");
        check(TransportFactoryDelivery.deliverToRail(owner,works,rail,
            TransportProduction.Product.LOCOMOTIVE,1,"rail-deliver-loco")==
            TransportFactoryDelivery.Result.DUPLICATE,
            "Repeated delivery minted extra locomotive");
        int train=rail.commission(owner,site[0],site[1],
            TransportInfrastructure.Axis.EAST_WEST,1);
        check(train>0&&!rail.stocks().containsKey(TransportProduction.item(
            TransportProduction.Product.LOCOMOTIVE,faction)),
            "Paid locomotive did not transfer into a physical train");
        make(works,TransportProduction.Product.FREIGHT_WAGON,faction,
            "rail-batch-wagon");
        check(TransportFactoryDelivery.deliverToRail(owner,works,rail,
            TransportProduction.Product.FREIGHT_WAGON,1,"rail-deliver-wagon")==
            TransportFactoryDelivery.Result.DELIVERED,
            "Produced freight car never reached depot");
        check(rail.attachWagon(owner,train)&&rail.state(train).wagons()==1,
            "Produced wagon not attached to the owned locomotive");
        make(works,TransportProduction.Product.RAIL_FUEL,faction,
            "rail-batch-fuel");
        check(TransportFactoryDelivery.deliverToRail(owner,works,rail,
            TransportProduction.Product.RAIL_FUEL,3,"rail-deliver-fuel")==
            TransportFactoryDelivery.Result.DELIVERED,
            "Manufactured traction fuel delivery failed");
        check(rail.refuel(owner,train,3)&&rail.state(train).fuelUnits()==30,
            "Train refuel ignored finite produced energy");
        check(rail.setBrake(owner,train,false),"Locomotive brake cannot release");
        for(int i=0;i<12;i++)rail.advance(250);
        check(rail.state(train).x()>site[0]+.5&&
            rail.state(train).fuelUnits()<30,
            "Locomotive not moving on actual track with finite fuel");
        var factorySave=works.snapshot();
        var trackSave=infrastructure.snapshot();
        var railSave=rail.snapshot();
        var restoreTrack=new TransportInfrastructure(layout,cloud,owner,faction,Map.of());
        restoreTrack.restore(trackSave);
        var restoreRail=new RailTransport(restoreTrack,Map.of());
        restoreRail.restore(railSave);
        var restoreWorks=new IndustrialWorkstation(
            TransportProduction.transportWorks(faction),
            TransportProduction.industryFaction(faction));
        restoreWorks.restore(factorySave);
        check(restoreRail.snapshot().equals(railSave)&&
            restoreWorks.snapshot().equals(factorySave)&&
            restoreTrack.snapshot().equals(trackSave),
            "Locomotive, paid cargo or production state changed on restart");
        check(TransportFactoryDelivery.deliverToRail(owner,restoreWorks,restoreRail,
            TransportProduction.Product.RAIL_FUEL,3,"rail-deliver-fuel")==
            TransportFactoryDelivery.Result.DUPLICATE,
            "Restart replay generated second locomotive fuel batch");
        var malformedTrain=new RailTransport.TrainSave(train,owner,faction,
            TransportInfrastructure.Axis.EAST_WEST,1,
            site[0]+.5,site[1]+.5,1000,30,320,0,null,false,1);
        var corruptRail=new RailTransport.Snapshot(railSave.worldSeed(),
            railSave.ownerId(),railSave.faction(),railSave.stock(),
            List.of(malformedTrain),railSave.deliveryReceipts(),railSave.nextId());
        boolean denied=false;
        try{restoreRail.restore(corruptRail);}
        catch(IllegalArgumentException expected){denied=true;}
        check(denied&&restoreRail.snapshot().equals(railSave),
            "Impossible locomotive speed mutated real freight");

        // Destroying track under a real stopped/stranded train must not
        // delete that paid train when the combined host ledger reloads.
        var trainBeforeDamage=restoreRail.state(train);
        int damagedX=(int)Math.floor(trainBeforeDamage.x());
        int damagedY=(int)Math.floor(trainBeforeDamage.y());
        check(restoreTrack.damage(damagedX,damagedY,100),
            "Host could not destroy segment beneath real locomotive");
        var wreckedTrack=restoreTrack.snapshot();
        var strandedTrain=restoreRail.snapshot();
        var loadedWreck=new TransportInfrastructure(layout,cloud,owner,faction,Map.of());
        loadedWreck.restore(wreckedTrack);
        var loadedTrain=new RailTransport(loadedWreck,Map.of());
        loadedTrain.restore(strandedTrain);
        check(loadedTrain.state(train).equals(trainBeforeDamage)&&
            !loadedWreck.railExists(damagedX,damagedY),
            "Railway save lost a paid train after physical track destruction");
        var absentTrackTrain=new RailTransport.TrainSave(train,owner,faction,
            TransportInfrastructure.Axis.EAST_WEST,1,
            site[0]+7.5,site[1]+.5,0,15,320,0,null,true,1);
        var absentTrackSave=new RailTransport.Snapshot(strandedTrain.worldSeed(),
            strandedTrain.ownerId(),strandedTrain.faction(),strandedTrain.stock(),
            List.of(absentTrackTrain),strandedTrain.deliveryReceipts(),
            strandedTrain.nextId());
        denied=false;
        try{loadedTrain.restore(absentTrackSave);}
        catch(IllegalArgumentException expected){denied=true;}
        check(denied&&loadedTrain.snapshot().equals(strandedTrain),
            "Train forged on terrain without track during restart");

        var shipyard=new IndustrialWorkstation(TransportProduction.shipyard(faction),
            TransportProduction.industryFaction(faction));
        make(shipyard,TransportProduction.Product.CARRIER_KEEL,faction,
            "naval-batch-keel");
        make(shipyard,TransportProduction.Product.CARRIER,faction,
            "naval-batch-ship");
        var marine=new AircraftCarrier(layout,cloud,owner,faction,Map.of());
        check(TransportFactoryDelivery.deliverToCarrier(owner,works,marine,
            TransportProduction.Product.CARRIER,1,"naval-wrong-factory")==
            TransportFactoryDelivery.Result.DENIED,
            "Rail depot minted a carrier hull");
        check(TransportFactoryDelivery.deliverToCarrier(owner,shipyard,marine,
            TransportProduction.Product.CARRIER,1,"naval-deliver-hull")==
            TransportFactoryDelivery.Result.DELIVERED,
            "Manufactured shipyard hull never reached ship");
        int hull=carrierSite(marine,owner);
        check(marine.deck(hull)!=null&&
            !marine.stocks().containsKey(TransportProduction.item(
                TransportProduction.Product.CARRIER,faction)),
            "Carrier ship materialized without spending its real hull");
        make(shipyard,TransportProduction.Product.MARINE_FUEL,faction,
            "naval-batch-fuel");
        check(TransportFactoryDelivery.deliverToCarrier(owner,shipyard,marine,
            TransportProduction.Product.MARINE_FUEL,4,"naval-deliver-fuel")==
            TransportFactoryDelivery.Result.DELIVERED,
            "Faction marine energy failed to reach ship");
        check(marine.refuel(owner,hull,4)&&marine.deck(hull).fuelUnits()==100,
            "Paid naval fuel does not power vessel");
        make(works,TransportProduction.Product.TRANSPORT_REPAIR,faction,
            "rail-batch-repair");
        check(TransportFactoryDelivery.deliverToCarrier(owner,works,marine,
            TransportProduction.Product.TRANSPORT_REPAIR,2,
            "naval-deliver-repair")==TransportFactoryDelivery.Result.DELIVERED,
            "Manufactured repair kits cannot travel from land factory to carrier");
        check(marine.damage(hull,200)&&marine.damage(hull,200)&&
            marine.repair(owner,hull)&&marine.deck(hull).deckHp()==AircraftCarrier.MAX_HP,
            "Carrier repair failed to consume legitimate manufactured maintenance");
        var shipSave=marine.snapshot();
        var restoredMarine=new AircraftCarrier(layout,cloud,owner,faction,Map.of());
        restoredMarine.restore(shipSave);
        check(restoredMarine.snapshot().equals(shipSave),
            "Shipyard inventory, carrier hull or fuel lost on restart");
        check(TransportFactoryDelivery.deliverToCarrier(owner,shipyard,
            restoredMarine,TransportProduction.Product.CARRIER,1,
            "naval-deliver-hull")==TransportFactoryDelivery.Result.DUPLICATE,
            "Saved carrier receipt minted a second hull");
        var badDeck=new AircraftCarrier.Deck(hull,owner,faction,
            site[0],site[1],AircraftCarrier.Axis.EAST_WEST,
            AircraftCarrier.LENGTH,AircraftCarrier.MAX_HP,0,true);
        var forged=new AircraftCarrier.Snapshot(shipSave.worldSeed(),
            shipSave.ownerId(),shipSave.faction(),shipSave.stock(),
            List.of(badDeck),shipSave.activeFlights(),shipSave.deliveryReceipts(),
            shipSave.nextId());
        denied=false;
        try{restoredMarine.restore(forged);}
        catch(IllegalArgumentException expected){denied=true;}
        check(denied&&restoredMarine.snapshot().equals(shipSave),
            "Forged landlocked carrier modified valid ship");
        System.out.println("PASS TRANSPORT_FLEET_DELIVERY actual paid rail/carrier "+
            "manufacturing, fuel, wagons, repair, receipts and restart snapshots");
    }
}
