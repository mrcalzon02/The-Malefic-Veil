package veil;

import java.util.*;

/** Headless server-contract regression: all faction airframes, real debits,
 * metered energy, runway/landing authority and swept city miasma diversion. */
public final class AircraftFlightTest {
    private static void check(boolean yes,String why){
        if(!yes)throw new AssertionError(why);
    }
    private static void finish(AircraftWorkshop shop){
        var job=shop.snapshot().job();
        check(job!=null,"No actual production job");
        long runs=(job.requiredMs()+59_999)/60_000;
        for(long step=0;step<runs;step++)
            check(shop.advance(60_000),"Metered energy must advance production");
        check(shop.snapshot().job()==null,"Manufacture was not completed");
    }
    private static AircraftFlight.Airfield locate(ContinentLayout world,
            CityMiasma miasma,AirPowerCatalog.Airframe plane){
        for(int y=16;y<ContinentLayout.SIZE-16;y+=2)
            for(int x=16;x<ContinentLayout.SIZE-16;x+=2)
                for(double heading:new double[]{0,Math.PI/2}){
                    var pad=new AircraftFlight.Airfield(x+.5,y+.5,
                        heading,plane.requiredRunwayMetres(),plane.faction());
                    if(AircraftFlight.validAirfield(world,miasma,pad,plane))
                        return pad;
                }
        throw new AssertionError("No available wilderness airfield for "+plane.modelId());
    }
    public static void main(String[] args){
        var all=AirPowerCatalog.all();
        check(all.size()==9,"All nine original aircraft must have flight contracts");
        Set<String> outputs=new HashSet<>();
        for(var plane:all){
            var process=CraftingEraRegistry.process(plane.recipeId());
            check(process!=null&&outputs.add(process.output()),
                "Missing or duplicate faction-specific airframe recipe");
            check(process.seconds()>3600&&process.inputs().size()>=4,
                "No instant/cheap aircraft");
            check(plane.requiredRunwayMetres()>0&&plane.idleFuelPerSecond()>0&&
                plane.throttleFuelPerSecond()>0,"Free runway or energy");
        }
        for(int faction=0;faction<3;faction++){
            var small=all.get(faction*3);
            var medium=all.get(faction*3+1);
            var heavy=all.get(faction*3+2);
            check(small.requiredRunwayMetres()<medium.requiredRunwayMetres()&&
                  medium.requiredRunwayMetres()<heavy.requiredRunwayMetres(),
                "Heavy airframes must require greater runway investment");
            check(small.maxSpeedParcelsPerSecond()>medium.maxSpeedParcelsPerSecond()&&
                  medium.maxSpeedParcelsPerSecond()>heavy.maxSpeedParcelsPerSecond(),
                "Missing aircraft speed/role distinction");
            check(CraftingEraRegistry.process(small.recipeId()).inputs().get(
                switch(faction){
                    case 0 -> "UNRAVELED_AIRFRAME_SPAR";
                    case 1 -> "PENITENT_HARVESTER_FRAME";
                    default -> "BEGOTTEN_FLIGHT_CARAPACE";
                })==1,"Faction construction material not consumed");
        }

        var needle=all.get(0);
        var recipe=CraftingEraRegistry.process(needle.recipeId());
        var structure=CraftingEraRegistry.process("unraveled-airframe-spar");
        var fuelRecipe=CraftingEraRegistry.process("unraveled-refine-avgas");
        // Fund the actual component, fuel and airframe input recipes, not the
        // already-made structural part or prefilled fuel canisters.
        Map<String,Integer> stock=new HashMap<>();
        for(var step:List.of(structure,fuelRecipe,recipe))
            step.inputs().forEach((item,quantity)->{
                if(!item.equals(structure.output()))
                    stock.merge(item,quantity,Integer::sum);
            });
        var sites=Set.of("airfield-runway","aircraft-service-hangar",
            "unraveled-flight-works");
        var shop=new AircraftWorkshop(needle.faction(),recipe.facility(),sites,stock);
        var before=shop.snapshot().stock();
        check(!shop.begin("penitent_shrike"),"Enemy faction manufacturing bypass");
        check(!shop.beginProcess("penitent-distill-ichor"),
            "Foreign faction fuel must be restricted");
        check(before.equals(shop.snapshot().stock()),"Failed queue must not debit stock");
        check(shop.beginProcess(structure.id()),"Paid spar job should start");
        check(!shop.begin(needle.modelId()),"Duplicate fabrication job accepted");
        check(!before.equals(shop.snapshot().stock()),"Inputs not consumed");
        check(!shop.advance(60_000),"Powerless work must remain paused");
        check(shop.snapshot().job().elapsedMs()==0,"Unpowered job advanced");
        shop.depositMeteredEnergy(200);
        finish(shop);
        check(shop.snapshot().stock().getOrDefault(structure.output(),0)==1,
            "Finished structural frame must appear only once");
        check(shop.beginProcess(fuelRecipe.id()),"Host should refine paid flight fuel");
        finish(shop);
        check(shop.snapshot().stock().getOrDefault(needle.fuelItem(),0)==4,
            "Fuel canisters must be manufactured from finite inputs");
        check(shop.begin(needle.modelId()),"Paid airframe job should start");
        finish(shop);
        check(shop.snapshot().stock().getOrDefault(recipe.output(),0)==1,
            "Finished output must appear once, never spawn for free");
        check(shop.snapshot().energyUnits()<200,"Factory must pay finite utility");

        var world=ContinentLayout.generate(20261020L);
        var miasma=new CityMiasma(world);
        var pad=locate(world,miasma,needle);
        check(AircraftFlight.commission(needle,shop,world,miasma,
            new AircraftFlight.Airfield(pad.x(),pad.y(),pad.heading(),10,
                pad.faction()),31)==null,
            "Short runway must not consume a finished aircraft");
        check(shop.snapshot().stock().get(recipe.output())==1,
            "Failed commissioning lost paid airframe");
        var flight=AircraftFlight.commission(needle,shop,world,miasma,pad,31);
        check(flight!=null,"Completed airframe should commission at valid runway");
        check(AircraftFlight.commission(needle,shop,world,miasma,pad,31)==null,
            "Reusing one paid airframe must not duplicate a craft");
        check(!flight.refuel(999,shop,4),"Unauthorized pilot refuel accepted");
        check(flight.refuel(31,shop,4),"Fuel canisters must be paid for by host");
        check(flight.snapshot().fuelUnits()==100,"Canister energy units missing");
        check(!flight.refuel(31,shop,1),"No physical fuel canister remains available");
        check(!flight.controls(999,new AircraftFlight.Controls(1,.5,0)),
            "Remote non-pilot can steer");
        check(flight.controls(31,new AircraftFlight.Controls(1,.5,0)),
            "Pilot should submit bounded control intentions");
        boolean airborne=false;
        for(int i=0;i<100;i++){
            flight.advance(250);
            if(flight.snapshot().airborne()){airborne=true;break;}
        }
        check(airborne,"Paid and fuelled light plane cannot take off");
        check(flight.snapshot().fuelUnits()<100,"Flight consumes no fuel");
        check(flight.controls(31,new AircraftFlight.Controls(0,-1,0)),
            "Host must accept descent intention");
        boolean landed=false;
        for(int i=0;i<80;i++){
            flight.advance(100);
            if(!flight.snapshot().airborne()){landed=true;break;}
        }
        check(landed&&flight.snapshot().hp()==needle.maxHp(),
            "Safe aligned runway touchdown should not crash: "+flight.snapshot()+
            " from airfield "+pad);
        // Head-on and angled swept airspace entry must NEVER penetrate cities.
        boolean redirected=false;
        for(var f:miasma.fields()){
            for(int dir=0;dir<16;dir++){
                double a=dir*2*Math.PI/16;
                double sx=f.centerX()+Math.cos(a)*(f.radius()+3);
                double sy=f.centerY()+Math.sin(a)*(f.radius()+3);
                if(miasma.forbidsAircraft(sx,sy,10))continue;
                double ex=f.centerX()-Math.cos(a)*(f.radius()+3);
                double ey=f.centerY()-Math.sin(a)*(f.radius()+3);
                var impact=miasma.deflectFlight(sx,sy,ex,ey);
                if(!impact.deflected())continue;
                check(!miasma.forbidsAircraft(impact.x(),impact.y(),10),
                    "Miasma deflection leaves craft inside a protected city");
                check(!miasma.forbidsFlightSegment(sx,sy,10,
                    impact.x(),impact.y(),10),
                    "Miasma slide tunneled through another city's airspace");
                redirected=true;break;
            }
            if(redirected)break;
        }
        check(redirected,"No tested solid miasma collision response");
        check(miasma.forbidsFlightSegment(Double.NaN,0,10,1,1,10),
            "Malformed client motion entered airspace");
        System.out.println("PASS AIRCRAFT_FLIGHT 9 paid recipes, factory power, fuel, runway, takeoff, landing, miasma");
    }
}
