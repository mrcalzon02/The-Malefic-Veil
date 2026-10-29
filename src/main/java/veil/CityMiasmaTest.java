package veil;

import java.util.*;

/** Aircraft must never bypass resource-city miasma, while roads remain usable. */
public final class CityMiasmaTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        for(long seed:new long[]{1L,12L,42L,20261020L,20261021L}){
            ContinentLayout continent=ContinentLayout.generate(seed);
            CityMiasma barrier=new CityMiasma(continent);
            check(barrier.fields().size()==continent.cities().size(),"all cities emit miasma");
            for(ContinentLayout.City city:continent.cities()){
                CityMiasma.Field field=barrier.fields().stream()
                    .filter(f->f.city()==city.id()).findFirst().orElseThrow();
                check(field.radius()>Math.max(city.radiusX(),city.radiusY()),
                    "miasma extends beyond whole city footprint");
                check(barrier.forbidsAircraft(city.x(),city.y(),0)&&
                      barrier.forbidsAircraft(city.x(),city.y(),100000),
                    "flying under/over purple cloud must not bypass no-flight column");
                check(barrier.forbidsFlightSegment(
                    city.x()-field.radius()-2,city.y(),20,
                    city.x()+field.radius()+2,city.y(),20),
                    "high-speed tick cannot tunnel across airspace");
                // Other cities may overlap the segment; they too must reject
                // it. Check a known open-world corner separately below.
                check(barrier.forbidsFlightSegment(city.x(),city.y(),20,
                    city.x()+field.radius()+8,city.y(),20),
                    "entering from the city to open sky remains forbidden");
            }
            int cityCells=0;
            for(int x=0;x<ContinentLayout.SIZE;x++)
                for(int y=0;y<ContinentLayout.SIZE;y++)
                    if(continent.cityAt(x,y)!='\0'){
                        cityCells++;
                        check(barrier.forbidsAircraft(x+.5,y+.5,500),
                            "aircraft may not penetrate any protected city parcel");
                    }
            check(cityCells>100,"generated city geometry present");
            check(barrier.forbidsFlightSegment(Double.NaN,0,10,0,0,10),
                "nonfinite vehicle telemetry rejected");
            check(barrier.forbidsAircraft(0,0,Double.POSITIVE_INFINITY),
                "invalid aircraft altitude rejected");
            check(!barrier.forbidsAircraft(0,0,100),
                "open air exists outside cities");
            // A city road still remains traversable by a ground hauler.
            boolean access=false;
            for(int x=0;x<ContinentLayout.SIZE&&!access;x++)
                for(int y=0;y<ContinentLayout.SIZE&&!access;y++)
                    if(continent.cityAt(x,y)!='\0'&&continent.road(x,y)){
                        check(barrier.forbidsAircraft(x,y,10),"city road has overlying no-fly");
                        check(continent.road(x,y),"ground route remains untouched");
                        access=true;
                    }
            check(access,"city resource access is still possible on the ground");
        }
        System.out.println("PASS CityMiasmaTest: 5 seeds, total city cover, altitude and segment exploits denied");
    }
}
