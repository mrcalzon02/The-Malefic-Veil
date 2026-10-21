package veil;

import java.util.*;
import java.util.function.BooleanSupplier;

/** Actual host and LAN loop: depot -> truck -> damage-drop -> recovery. */
public final class CargoHaulageTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static void until(BooleanSupplier yes,String why)throws Exception{
        long deadline=System.nanoTime()+6_000_000_000L;
        while(System.nanoTime()<deadline){if(yes.getAsBoolean())return;Thread.sleep(20);}
        throw new AssertionError("Timeout "+why);
    }
    private static CargoHaulage.Site position(ArenaSystems match,int player,int car){
        ArenaSystems.Player p=match.player(player);
        for(CargoHaulage.Site yard:match.cargo.sites()){
            for(int dx=-4;dx<=4;dx++)for(int dy=-4;dy<=4;dy++){
                if(Math.hypot(dx,dy)>4||Math.hypot(dx,dy)<1.3)continue;
                double cx=yard.x()+.5+dx,cy=yard.y()+.5+dy;
                if(!match.world.lineClear( yard.x()+.5,yard.y()+.5,cx,cy))continue;
                if(match.vehicles.restore(car,cx,cy,0,100,200)){
                    p.x=yard.x()+.5;p.y=yard.y()+.5;p.moveAt=match.time();
                    return yard;
                }
            }
        }
        throw new AssertionError("No viable loading truck lane for seeded RESOURCE plot");
    }
    public static void main(String[] args)throws Exception{
        for(long seed:new long[]{42L,887766L,20261020L}){
            ArenaSystems game=new ArenaSystems(seed);
            check(game.cargo.sites().size()>=4,"Resource-city yard population");
            Set<String> kinds=new HashSet<>();
            for(CargoHaulage.Site yard:game.cargo.sites()){
                kinds.add(yard.resource());
                check(game.world.city.categoryAt(yard.x(),yard.y())==CityLayout.Category.RESOURCE,
                    "Cargo yard is not a source-generated resource building");
                check(KenneyAssetPack.load().modelNames().contains(yard.groundMesh()+".glb"),
                    "Ground cargo art absent from exact Kenney source kit");
            }
            check(kinds.equals(Set.of("METAL_STOCK","MACHINE_PARTS")),
                "Both distinct bulk cargo types must exist");
            int player=1,truck=0;game.join(player);
            var site=position(game,player,truck);
            check(!game.loadTruck(999,truck,site.id()),"Unverified actor must not load");
            check(game.loadTruck(player,truck,site.id()),"Valid nearby player loads parked truck");
            check(game.vehicles.cargo(truck).resourceType().equals(site.resource()),"Cargo type lost");
            check(game.vehicles.cargo(truck).model().equals("truck-green-cargo"),
                "Loaded truck must use actual Kenney cargo mesh");
            check(!game.loadTruck(player,truck,site.id()),"No duplicate cargo from one yard");
            check(game.cargo.sites().get(site.id()).stock()==0,"Yard stock not depleted");
            check(game.vehicles.damageCar(truck,121),"Host-authoritative damage to cargo truck");
            game.cargo.advance(0);
            check(game.vehicles.cargo(truck).resourceType()==null,
                "Truck cargo must eject below damage threshold");
            check(game.cargo.drops().size()==1,"Exactly one recoverable load dropped");
            var drop=game.cargo.drops().get(0);
            check(drop.resource().equals(site.resource()),"Drop resource identity lost");
            game.cargo.advance(0);
            check(game.cargo.drops().size()==1,"Damage tick duplicated cargo drop");
            check(game.vehicles.restore(truck,drop.x(),drop.y(),0,100,200),
                "Repair fixture puts recovered truck at wreck");
            ArenaSystems.Player p=game.player(player);
            p.x=drop.x();p.y=drop.y();
            check(game.recoverTruckLoad(player,truck,drop.id()),"Recover lost load to truck");
            check(!game.recoverTruckLoad(player,truck,drop.id()),"No recovery duplication");
            check(game.cargo.drops().isEmpty(),"Recovered cargo must leave ground");
            game.cargo.advance(CargoHaulage.REFILL_MS>1000?1000:0);
            for(int t=1000;t<CargoHaulage.REFILL_MS;t+=1000)
                game.cargo.advance(1000);
            check(game.cargo.sites().get(site.id()).stock()==1,"Resource yard refill schedule");
            System.out.println("PASS CargoHaulageTest seed="+seed+" yards="+game.cargo.sites().size()+
                " loaded="+site.resource()+" recovered=1");
        }
        try(LanServer server=new LanServer(0,42);
            NetLink client=new NetLink("127.0.0.1",server.port())){
            until(()->!client.cargoYards.isEmpty()&&!client.cars.isEmpty(),
                "Server provides seeded resource yard and car state");
            var site=position(server.match,client.id,0);
            client.loadCargo(0,site.id());
            until(()->!client.cargoResults.isEmpty(),"LAN cargo operation acknowledgement");
            check("LOAD|OK".equals(client.cargoResults.poll()),
                "LAN host rejected actual nearby truck resource pickup");
            until(()->site.resource().equals(client.truckCargo.get(0)),
                "Loaded truck type replicated to client");
        }
        System.out.println("PASS CargoHaulageTest: authoritative two-resource loaded truck LAN pickup");
    }
}
