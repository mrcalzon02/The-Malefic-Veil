package veil;

import java.util.*;

/** Executable host-core acceptance for paid modular runway/rail and ship decks. */
public final class TransportInfrastructureTest {
    private static void yes(boolean test,String why){
        if(!test)throw new AssertionError(why);
    }
    private static TransportInfrastructure.Runway chooseRunway(ContinentLayout land,
            CityMiasma mist,TransportInfrastructure host,int owner){
        // A continuous 13 x 3 parcel pad, with one-parcel shoulders, suffices
        // for the Unraveled light aircraft; wider/taller sites remain possible.
        for(int y=17;y<ContinentLayout.SIZE-17;y+=2)
            for(int x=17;x<ContinentLayout.SIZE-30;x+=2){
                var result=host.planRunway(owner,x,y,
                    TransportInfrastructure.Axis.EAST_WEST,13);
                if(result!=null)return result;
            }
        throw new AssertionError("Procedural land has no valid 78m strip");
    }
    private static int[] railSite(ContinentLayout world,
            TransportInfrastructure built){
        for(int y=20;y<ContinentLayout.SIZE-20;y++)
            for(int x=20;x<ContinentLayout.SIZE-30;x++){
                boolean good=true;
                for(int k=0;k<6;k++){
                    int xx=x+k;
                    if(!world.buildableGround(xx,y)||world.road(xx,y))
                        good=false;
                    for(var segment:built.segments())
                        if(segment.x()==xx&&segment.y()==y)good=false;
                }
                if(good)return new int[]{x,y};
            }
        throw new AssertionError("Rail site unavailable");
    }
    private static AircraftCarrier.Deck seaSite(AircraftCarrier fleet,
            CharacterRoster.Faction faction,int owner){
        for(int y=20;y<ContinentLayout.SIZE-20;y++)
            for(int x=20;x<ContinentLayout.SIZE-25;x++){
                if(!fleet.clearSea(x+.5,y+.5,AircraftCarrier.Axis.EAST_WEST,-1))
                    continue;
                int id=fleet.construct(owner,x+.5,y+.5,AircraftCarrier.Axis.EAST_WEST);
                if(id>0)return fleet.deck(id);
            }
        throw new AssertionError("Ocean has no safe 108m carrier corridor");
    }
    public static void main(String[] args){
        int owner=41;
        var faction=CharacterRoster.Faction.THE_UNRAVELED;
        var world=ContinentLayout.generate(20261020);
        var mist=new CityMiasma(world);
        var inventory=new HashMap<String,Integer>();
        for(String id:List.of("METAL_STOCK","MACHINE_PARTS","FIELD_STONE",
                "HARDENED_ALLOY","FASTENERS","TIMBER","CHARCOAL",
                "GEAR_ASSEMBLY","ASSEMBLY_CONTROLLER","RESILIENCE_CORE",
                "TRACKING_CIRCUIT"))
            inventory.put(id,1000);
        var built=new TransportInfrastructure(world,mist,owner,faction,inventory);
        yes(built.planRunway(owner,0,0,
            TransportInfrastructure.Axis.EAST_WEST,13)==null,
            "City/water runway accepted");
        var strip=chooseRunway(world,mist,built,owner);
        var needle=AirPowerCatalog.get("unraveled_needlewing");
        yes(!built.operationalRunway(strip.id(),needle),
            "Unbuilt plans cannot commission aircraft");
        var first=built.stock();
        yes(!built.buildNextRunwaySection(999,strip.id()),
            "Other player can build on owner's land");
        yes(first.equals(built.stock()),"Unauthorized operation paid a bill");
        yes(!built.buildNextRunwaySection(owner,strip.id()),
            "Construction without utility energy is free");
        built.depositMeteredEnergy(120);
        for(int i=0;i<strip.count();i++)
            yes(built.buildNextRunwaySection(owner,strip.id()),
                "Physical runway tile could not be erected "+i);
        yes(built.runway(strip.id()).constructed()==13,
            "Runway has gaps");
        yes(built.operationalRunway(strip.id(),needle),
            "Fully paved 78m strip should be certified");
        yes(!built.operationalRunway(strip.id(),AirPowerCatalog.get("unraveled_harrier")),
            "Short runway must deny medium aircraft");
        yes(!built.buildNextRunwaySection(owner,strip.id()),
            "Already paid runway fabricated an extra tile");
        int ix=strip.startX()+6,iy=strip.startY();
        yes(built.damage(ix,iy,80),"Airfield damage did not affect mesh");
        yes(!built.operationalRunway(strip.id(),needle),
            "Cratered runway can still operate");
        yes(!built.repair(999,ix,iy),"Foreign repair permitted");
        yes(built.repair(owner,ix,iy),"Paid runway maintenance failed");
        yes(built.operationalRunway(strip.id(),needle),
            "Certified runway did not recover after real repair");
        yes(built.planRunway(owner,strip.startX(),strip.startY(),
            strip.axis(),13)==null,"Duplicate runway placement overlaps");

        var rs=railSite(world,built);
        for(int i=0;i<6;i++)
            yes(built.buildRail(owner,rs[0]+i,rs[1],
                TransportInfrastructure.Axis.EAST_WEST),
                "Rail placement failed at "+i);
        yes(!built.buildRail(owner,rs[0],rs[1],
            TransportInfrastructure.Axis.NORTH_SOUTH),"Overlapped rail accepted");
        yes(built.connectedRail(rs[0],rs[1],rs[0]+1,rs[1]),
            "Two consecutive rail blocks are not joined");
        var freight=new RailTransport(built,inventory);
        yes(freight.commission(999,rs[0],rs[1],
            TransportInfrastructure.Axis.EAST_WEST,1)==-1,
            "Enemy cannot create engine");
        int train=freight.commission(owner,rs[0],rs[1],
            TransportInfrastructure.Axis.EAST_WEST,1);
        yes(train>0,"Actual paid freight locomotive not commissioned");
        yes(freight.commission(owner,rs[0],rs[1],
            TransportInfrastructure.Axis.EAST_WEST,1)==-1,
            "Duplicate train on occupied siding");
        yes(freight.load(owner,train,"METAL_STOCK",10),
            "Rail cargo must be transferred from real stocks");
        yes(!freight.load(owner,train,"METAL_STOCK",10),
            "Freight duplicate load accepted");
        yes(freight.refuel(owner,train,10),"Paid train fuel missing");
        yes(!freight.setBrake(999,train,false),
            "Unauthorized throttle/brake control");
        yes(freight.setBrake(owner,train,false),"Brake release failed");
        for(int i=0;i<120;i++)freight.advance(250);
        yes(freight.state(train).x()>rs[0]+.5,
            "Train did not travel along real built track");
        yes(freight.state(train).fuelUnits()<10,
            "Train traction consumes no fuel");
        yes(freight.setBrake(owner,train,true),"Train could not brake");
        for(int i=0;i<100;i++)freight.advance(250);
        yes(freight.state(train).speedMetresPerSecond()==0,
            "Rail brake did not stop train");
        yes(freight.unload(owner,train),"Train could not unload paid cargo");

        // Two finished airframes are independently manufactured, then
        // withdrawn once for runway and once for the paid ocean carrier.
        var recipe=CraftingEraRegistry.process(needle.recipeId());
        var stock=new HashMap<>(recipe.inputs());
        recipe.inputs().forEach((k,v)->stock.put(k,v*2));
        stock.put(needle.fuelItem(),8);
        var factory=new AircraftWorkshop(faction,recipe.facility(),
            Set.of("airfield-runway","aircraft-service-hangar",recipe.facility()),stock);
        factory.depositMeteredEnergy(140);
        for(int n=0;n<2;n++){
            yes(factory.begin(needle.modelId()),"Paid factory job failed");
            for(int i=0;i<120;i++)
                yes(factory.advance(60_000),"Airframe job energy starvation");
        }
        yes(factory.snapshot().stock().get(recipe.output())==2,
            "Airframe manufacture duplicated or lost a hull");
        var plane=AircraftFlight.commission(needle,factory,world,mist,built,
            strip.id(),owner);
        yes(plane!=null,"True built runway must permit airframe commissioning");
        yes(plane.refuel(owner,factory,4),"Finished plane cannot refuel");
        yes(plane.controls(owner,new AircraftFlight.Controls(1,.5,0)),
            "Pilot cannot initiate takeoff");
        boolean flying=false;
        for(int i=0;i<100;i++){
            plane.advance(250);
            if(plane.snapshot().airborne()){flying=true;break;}
        }
        yes(flying,"Physical runway never launched paid plane");

        Map<String,Integer> shipStock=new HashMap<>();
        AircraftCarrier.buildCost(faction).forEach((k,v)->shipStock.put(k,v+10));
        shipStock.put("CHARCOAL",40);
        var sea=new AircraftCarrier(world,mist,owner,faction,shipStock);
        var deck=seaSite(sea,faction,owner);
        yes(sea.usableDeck(deck.id(),needle),
            "Paid 108m carrier deck not usable by light aircraft");
        yes(!sea.usableDeck(deck.id(),AirPowerCatalog.get("unraveled_harrier")),
            "Short deck cannot operate medium aircraft");
        yes(sea.damage(deck.id(),350),"Host could not damage deck");
        yes(!sea.usableDeck(deck.id(),needle),
            "Damaged deck still offers free launch");
        yes(sea.repair(owner,deck.id()),"Repair requires paid real materials");
        yes(sea.usableDeck(deck.id(),needle),"Ship repair did not restore deck");
        var shipPlane=AircraftFlight.commissionCarrier(needle,factory,world,
            mist,sea,deck.id(),owner,owner);
        yes(shipPlane!=null,"Paid carrier could not embark second paid aircraft");
        yes(sea.activeFlight(deck.id()),"Deck flight collision not reserved");
        yes(!sea.sail(owner,deck.id(),1,0),
            "Carrier moved under occupied takeoff deck");
        yes(shipPlane.refuel(owner,factory,4),"Embarked plane lacked paid energy");
        yes(shipPlane.controls(owner,new AircraftFlight.Controls(1,.5,0)),
            "Embarked pilot could not advance");
        boolean offDeck=false;
        for(int i=0;i<100;i++){
            shipPlane.advance(250);
            if(shipPlane.snapshot().airborne()){offDeck=true;break;}
        }
        yes(offDeck&&shipPlane.snapshot().altitudeMetres()>0,
            "Carrier launch did not occur from raised physical deck");
        yes(factory.snapshot().stock().getOrDefault(recipe.output(),0)==0,
            "One produced airframe duplicated across ship and runway");
        System.out.println("PASS TRANSPORT_INFRASTRUCTURE paid 6m runways, craters, "+
            "rail freight, fuel, and waterborne aircraft carrier deck");
    }
}
