package veil;

import java.util.*;

/**
 * Objective host regression: two independently paid locomotives on one
 * continuous track may approach, but must neither overlap nor cross through
 * each other. Invalid overlapping checkpoint data must restore atomically.
 */
public final class RailTrafficSafetyTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private static int[] site(ContinentLayout world,CityMiasma fog,int count){
        for(int y=20;y<ContinentLayout.SIZE-20;y++)
            for(int x=20;x<ContinentLayout.SIZE-count-20;x++){
                boolean suitable=true;
                for(int i=0;i<count;i++){
                    int px=x+i;
                    if(!world.buildableGround(px,y)||world.road(px,y)||
                       fog.forbidsAircraft(px+.5,y+.5,0)||
                       (i>0&&Math.abs(world.elevation(px,y)-
                           world.elevation(px-1,y))>1.5)){
                        suitable=false;break;
                    }
                }
                if(suitable)return new int[]{x,y};
            }
        throw new AssertionError("No continuous flat railway test alignment");
    }
    public static void main(String[] args){
        int owner=81, length=18;
        var faction=CharacterRoster.Faction.THE_UNRAVELED;
        var world=ContinentLayout.generate(20261020L);
        var fog=new CityMiasma(world);
        var start=site(world,fog,length);
        var track=new TransportInfrastructure(world,fog,owner,faction,
            Map.of(TransportProduction.item(
                TransportProduction.Product.RAIL_SECTION,faction),length,
                "TIMBER",length*2));
        track.depositMeteredEnergy(length);
        for(int i=0;i<length;i++)
            check(track.buildRail(owner,start[0]+i,start[1],
                TransportInfrastructure.Axis.EAST_WEST),
                "Could not construct safety regression rail "+i);
        var rail=new RailTransport(track,Map.of(
            TransportProduction.item(
                TransportProduction.Product.LOCOMOTIVE,faction),2,
            TransportProduction.item(
                TransportProduction.Product.RAIL_FUEL,faction),10));
        int a=rail.commission(owner,start[0],start[1],
            TransportInfrastructure.Axis.EAST_WEST,1);
        int b=rail.commission(owner,start[0]+length-1,start[1],
            TransportInfrastructure.Axis.EAST_WEST,-1);
        check(a>0&&b>0,"Two paid opposing engines failed commissioning");
        check(rail.refuel(owner,a,5)&&rail.refuel(owner,b,5),
            "Paid fuel missing");
        check(rail.setBrake(owner,a,false)&&rail.setBrake(owner,b,false),
            "Host cannot release engine brakes");
        boolean blocked=false;
        for(int tick=0;tick<250;tick++){
            rail.advance(250);
            var left=rail.state(a);
            var right=rail.state(b);
            check(left.x()<right.x()&&right.x()-left.x()>=1-1e-7,
                "Opposing locomotives overlapped or crossed at tick "+tick);
            check(left.hp()==320&&right.hp()==320,
                "Automatic block protection caused collision damage");
            blocked|=left.brakeApplied()||right.brakeApplied();
        }
        check(blocked,"Single-track block authority never stopped either train");
        var good=rail.snapshot();
        var badTrains=new ArrayList<>(good.trains());
        var first=badTrains.get(0);
        var second=badTrains.get(1);
        badTrains.set(1,new RailTransport.TrainSave(
            second.id(),second.owner(),second.faction(),second.axis(),
            second.direction(),first.x(),first.y(),second.speed(),
            second.fuel(),second.hp(),second.quantity(),second.cargo(),
            second.brake(),second.wagons()));
        var tampered=new RailTransport.Snapshot(good.worldSeed(),
            good.ownerId(),good.faction(),good.stock(),badTrains,
            good.deliveryReceipts(),good.nextId());
        boolean rejected=false;
        try{rail.restore(tampered);}
        catch(IllegalArgumentException expected){rejected=true;}
        check(rejected&&good.equals(rail.snapshot()),
            "Overlapping checkpoint corrupted a legitimate host railway");
        System.out.println("PASS RAIL_TRAFFIC_SAFETY no head-on overlap, "+
            "safety braking, and atomic saved-position rejection");
    }
}
