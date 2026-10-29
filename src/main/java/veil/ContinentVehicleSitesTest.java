package veil;

import java.util.*;

/** Seeded discovery plans: no vehicle is spawned onto a road or a protected player spawn. */
public final class ContinentVehicleSitesTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    public static void main(String[] args){
        for(long seed:new long[]{1,12,42,20261020L,20261021L}){
            ContinentLayout world=ContinentLayout.generate(seed);
            ContinentPopulation a=new ContinentPopulation(world);
            ContinentPopulation b=new ContinentPopulation(world);
            var one=a.vehicleSpawns();
            check(one.equals(b.vehicleSpawns()),"seed-stable vehicle sites "+seed);
            check(one.size()>=7,"at least one discovery opportunity per city, seed "+seed);
            int parked=0,shoulder=0;
            Set<String> positions=new HashSet<>();
            for(int i=0;i<one.size();i++){
                var v=one.get(i);
                check(v.id()==i,"stable contiguous site identifiers");
                check(positions.add(v.x()+","+v.y()),"no overlapping vehicle sites");
                check(world.land(v.x(),v.y())&&!world.road(v.x(),v.y()),
                    "vehicle must be on dry off-road ground");
                check(world.road(v.roadX(),v.roadY())&&
                    Math.abs(v.roadX()-v.x())+Math.abs(v.roadY()-v.y())==1,
                    "discoverable parking must connect to road");
                check(v.sourceCar()>=1&&v.sourceCar()<=8,"source-backed GGBot car ID");
                check(v.hp()>=0&&v.hp()<=200&&v.fuel()>=0&&v.fuel()<=100,
                    "bounded host candidate state");
                check(v.cargoSlots()>=2&&v.cargoSlots()<=10,"bounded catalog cargo");
                if(v.sourceCar()==6)check(v.condition()==ContinentVehicleSites.Condition.WRECKED,
                    "burned-out authored model cannot start drivable");
                if(v.condition()==ContinentVehicleSites.Condition.WRECKED)
                    check(v.hp()==0&&v.fuel()==0,"wreck cannot yield an operational vehicle");
                else check(v.hp()>0,"working or disabled candidate retains repairable hull");
                for(var player:a.playerSpawns())
                    check(Math.abs(player.x()-v.x())+Math.abs(player.y()-v.y())>=6,
                        "no discovery vehicle in player spawn clearance");
                if(v.kind()==ContinentVehicleSites.Kind.CITY_PARKING){
                    parked++;
                    check(v.city()!=0&&world.cityAt(v.x(),v.y())==v.city()&&
                        world.cityAt(v.roadX(),v.roadY())==v.city(),
                        "parked car must be inside matching city");
                }else{
                    shoulder++;
                    check(v.city()==0&&world.cityAt(v.x(),v.y())==0&&
                        world.buildableGround(v.x(),v.y())&&
                        world.cityAt(v.roadX(),v.roadY())==0,
                        "highway shoulder must be eligible wilderness");
                }
            }
            check(parked>0&&shoulder>0,
                "cities and intercity shoulders both offer discovery, seed "+seed);
        }
        System.out.println("PASS ContinentVehicleSitesTest: 5 seeds, deterministic parking, shoulders, safe spawns");
    }
}
