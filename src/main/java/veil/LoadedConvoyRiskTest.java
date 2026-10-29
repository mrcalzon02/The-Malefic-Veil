package veil;

/** Regression: carrying a bulk pallet materially worsens escape speed. */
public final class LoadedConvoyRiskTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    public static void main(String[] args){
        World world=new World(42L);
        Vehicles fleet=new Vehicles(world);
        var initial=fleet.state(0);
        check(fleet.loadCargo(0,"METAL_STOCK"),"Truck must load while parked");
        // Player joins the cab only after loading the pallet.
        check(fleet.enter(7,initial.x(),initial.y(),0),"No driver");
        check(fleet.input(7,1,0),"Throttle rejected");
        for(int i=0;i<50;i++)fleet.advance(100);
        check(fleet.state(0).speed()<=3.2+1e-9,
            "Loaded truck exceeded cargo speed cap");
        check(fleet.cargo(0).resourceType().equals("METAL_STOCK"),
            "Driving consumed cargo");
        System.out.println("PASS LoadedConvoyRiskTest");
    }
}
