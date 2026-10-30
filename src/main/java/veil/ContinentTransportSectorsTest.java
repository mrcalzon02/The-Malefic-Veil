package veil;

import java.util.*;

/** Every paid transport object appears in the correct streamed macro sector,
 * including cross-sector carrier geometry, with live damage/HP updates.
 */
public final class ContinentTransportSectorsTest {
    private static void check(boolean yes,String message){
        if(!yes)throw new AssertionError(message);
    }
    public static void main(String[] args){
        int owner=84;
        var faction=CharacterRoster.Faction.THE_UNRAVELED;
        var world=ContinentLayout.generate(20261020L);
        var miasma=new CityMiasma(world);
        var stock=new HashMap<String,Integer>();
        stock.put(TransportProduction.item(
            TransportProduction.Product.RUNWAY_PANEL,faction),80);
        stock.put(TransportProduction.item(
            TransportProduction.Product.RAIL_SECTION,faction),30);
        stock.put("FIELD_STONE",150);stock.put("FASTENERS",80);
        stock.put("TIMBER",80);
        var infra=new TransportInfrastructure(world,miasma,owner,faction,stock);
        infra.depositMeteredEnergy(300);
        TransportInfrastructure.Runway runway=null;
        for(int y=17;y<ContinentLayout.SIZE-25&&runway==null;y+=2)
            for(int x=17;x<ContinentLayout.SIZE-40&&runway==null;x+=2)
                runway=infra.planRunway(owner,x,y,
                    TransportInfrastructure.Axis.EAST_WEST,13);
        check(runway!=null,"No valid wilderness runway in seeded world");
        for(int i=0;i<13;i++)
            check(infra.buildNextRunwaySection(owner,runway.id()),
                "Runway paving failed");
        int[] rails=null;
        for(int y=18;y<ContinentLayout.SIZE-18&&rails==null;y++)
            for(int x=18;x<ContinentLayout.SIZE-28&&rails==null;x++){
                boolean can=true;
                for(int i=0;i<4;i++){
                    int px=x+i;
                    if(!world.buildableGround(px,y)||world.road(px,y)||
                       miasma.forbidsAircraft(px+.5,y+.5,0))
                        can=false;
                    for(var p:infra.segments())
                        if(p.x()==px&&p.y()==y)can=false;
                }
                if(can)rails=new int[]{x,y};
            }
        check(rails!=null,"No rail site");
        for(int i=0;i<4;i++)
            check(infra.buildRail(owner,rails[0]+i,rails[1],
                TransportInfrastructure.Axis.EAST_WEST),"Rail track missing");
        var rail=new RailTransport(infra,Map.of(TransportProduction.item(
            TransportProduction.Product.LOCOMOTIVE,faction),1));
        int train=rail.commission(owner,rails[0],rails[1],
            TransportInfrastructure.Axis.EAST_WEST,1);
        check(train>0,"Paid rail locomotive not commissioned");
        var fleetStock=Map.of(TransportProduction.item(
            TransportProduction.Product.CARRIER,faction),1);
        var fleet=new AircraftCarrier(world,miasma,owner,faction,fleetStock);
        int hull=-1;
        for(int y=20;y<ContinentLayout.SIZE-20&&hull<0;y++)
            for(int x=20;x<ContinentLayout.SIZE-25&&hull<0;x++)
                if(fleet.clearSea(x+.5,y+.5,AircraftCarrier.Axis.EAST_WEST,-1))
                    hull=fleet.construct(owner,x+.5,y+.5,
                        AircraftCarrier.Axis.EAST_WEST);
        check(hull>0,"Paid carrier was not placed on real water");
        var layers=new ContinentTransportSectors(infra,rail,fleet);
        // Public sector rows are render/physics descriptors, never the
        // source train/carrier/warehouse records with private inventory.
        for(Class<?> viewType:List.of(
                ContinentTransportSectors.SegmentVisual.class,
                ContinentTransportSectors.RunwayVisual.class,
                ContinentTransportSectors.TrainVisual.class,
                ContinentTransportSectors.CarrierVisual.class))
            for(var component:viewType.getRecordComponents())
                check(!Set.of("ownerId","cargo","cargoQuantity","fuelUnits",
                        "stock","receipt","deliveryReceipts").contains(component.getName()),
                    "Transport sector leaked private host economy: "+component.getName());

        Set<String> seenSegments=new HashSet<>();
        Set<Integer> seenTrains=new HashSet<>(),seenRunways=new HashSet<>(),
            seenCarriers=new HashSet<>();
        int carrierSectors=0,runwaySectors=0;
        for(int sy=0;sy<ContinentSectors.COUNT;sy++)
            for(int sx=0;sx<ContinentSectors.COUNT;sx++){
                var key=new ContinentSectors.Key(sx,sy);
                var scene=layers.view(key);
                check(scene.key().equals(key),"Wrong sector key");
                for(var s:scene.segments()){
                    check(ContinentSectors.keyFor(s.x(),s.y()).equals(key),
                        "Physical 6m transport tile drawn in wrong sector");
                    check(seenSegments.add(s.x()+":"+s.y()),
                        "Tile duplicated across streamed sectors");
                    check(world.buildableGround(s.x(),s.y()),
                        "Transport geometry leaked into protected world");
                }
                for(var t:scene.locomotives())
                    check(seenTrains.add(t.id()),"One train replicated twice");
                for(var r:scene.runways()){
                    seenRunways.add(r.id());runwaySectors++;
                }
                for(var d:scene.carriers()){
                    seenCarriers.add(d.id());carrierSectors++;
                }
            }
        check(seenSegments.size()==17,"Runway and rail geometry missing");
        check(seenTrains.equals(Set.of(train)),"No single owner train");
        check(seenRunways.equals(Set.of(runway.id()))&&runwaySectors>=2,
            "Runway crossing chunk seams lost its physical footprint");
        check(seenCarriers.equals(Set.of(hull))&&carrierSectors>=2,
            "Large aircraft carrier not streamed into overlapping water sectors");
        int runX=runway.startX()+5,runY=runway.startY();
        var key=ContinentSectors.keyFor(runX,runY);
        var before=layers.view(key);
        check(before.segments().stream().anyMatch(s->
            s.x()==runX&&s.y()==runY&&s.hp()==100),
            "Fresh runway does not expose its actual HP");
        check(infra.damage(runX,runY,60),"Damage to physical runway failed");
        var after=layers.view(key);
        check(after.segments().stream().anyMatch(s->
            s.x()==runX&&s.y()==runY&&s.hp()==40),
            "Streamed transport sector served stale cached HP");
        check(before.segments().stream().anyMatch(s->
            s.x()==runX&&s.y()==runY&&s.hp()==100),
            "Read-only older sector snapshot mutated after damage");
        boolean bad=false;
        try{layers.view(new ContinentSectors.Key(-1,0));}
        catch(IllegalArgumentException expected){bad=true;}
        check(bad,"Invalid sector permitted");
        var different=new TransportInfrastructure(world,miasma,owner+1,
            faction,Map.of());
        bad=false;
        try{new ContinentTransportSectors(different,rail,fleet);}
        catch(IllegalArgumentException expected){bad=true;}
        check(bad,"Mixed world ownership could leak actor snapshots");
        System.out.println("PASS CONTINENT_TRANSPORT_SECTORS paid tiles, trains, "+
            "cross-sector carrier footprints, live damage and boundaries");
    }
}
