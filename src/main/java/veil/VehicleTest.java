package veil;

import java.util.function.BooleanSupplier;

/** Host-authoritative vehicle gate: physics, anti-teleport, occupied seats and real sockets. */
public final class VehicleTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static void until(BooleanSupplier test,String why)throws Exception{
        long limit=System.nanoTime()+5_000_000_000L;
        while(System.nanoTime()<limit){if(test.getAsBoolean())return;Thread.sleep(20);}
        throw new AssertionError("Timeout: "+why);
    }
    private static void beside(ArenaSystems game,int playerId,Vehicles.State car){
        ArenaSystems.Player p=game.player(playerId);
        for(int k=0;k<32;k++){
            double a=k*Math.PI/16;
            double x=car.x()+Math.cos(a)*1.65,y=car.y()+Math.sin(a)*1.65;
            if(game.world.canWalk(x,y)){
                p.x=x;p.y=y;p.moveAt=game.time();return;
            }
        }
        throw new AssertionError("Car has no on-foot approach");
    }
    public static void main(String[] args)throws Exception{
        ArenaSystems arena=new ArenaSystems(2468);
        arena.world.npcs.clear();
        check(!arena.vehicles.states().isEmpty(),"deterministic vehicle placement");
        Vehicles.State car=arena.vehicles.states().get(0);
        check(car.driverId()==-1&&car.fuel()>0&&car.hp()==200,"parked working car");
        arena.join(1);arena.join(2);
        beside(arena,1,car);beside(arena,2,car);
        check(arena.enterVehicle(1,car.id()),"player enters available car");
        check(!arena.enterVehicle(2,car.id()),"occupied car has one driver");
        check(!arena.move(1,car.x()+9,car.y()+9,0,2),"driving cannot bypass host movement validation");
        check(!arena.driveVehicle(1,1.1,0),"throttle cannot exceed server bound");
        check(!arena.driveVehicle(1,Double.NaN,0),"NaN throttle rejected");
        check(arena.driveVehicle(1,1,0),"valid car throttle accepted");
        double oldFuel=car.fuel();
        for(int i=0;i<25;i++)arena.advance(100);
        Vehicles.State moving=arena.vehicles.state(car.id());
        check(moving.fuel()<oldFuel,"fuel consumed only when driving");
        check(Math.hypot(arena.player(1).x-moving.x(),arena.player(1).y-moving.y())<.001,
            "host actor follows authoritative car, not client STATE");
        check(moving.hp()>=0&&moving.hp()<=200,"collision damage remains bounded");
        arena.leave(1);
        check(arena.vehicles.state(car.id()).driverId()==-1,"disconnect releases vehicle");
        try(LanServer server=new LanServer(0,2468);
            NetLink client=new NetLink("127.0.0.1",server.port())){
            server.match.world.npcs.clear();
            until(()->!client.cars.isEmpty(),"vehicle snapshots");
            NetLink.Car target=client.cars.values().iterator().next();
            beside(server.match,client.id,server.match.vehicles.state(target.id()));
            client.ride(target.id());
            until(()->server.match.vehicles.carDrivenBy(client.id)==target.id(),"network seat accepted");
            until(()->client.drivenCar()==target.id(),"occupancy broadcast");
            client.drive(1,0);
            until(()->server.match.vehicles.state(target.id()).fuel()<100,"network input advances fuel");
            client.drive(0,0);
            check(server.match.vehicles.carDrivenBy(client.id)==target.id(),"socket driver remains assigned");
            client.exitCar();
            until(()->!client.rideResults.isEmpty(),"ride result returned");
        }
        System.out.println("PASS VehicleTest: deterministic spawn, physics, fuel, anti-cheat, occupancy, disconnect, LAN");
    }
}
