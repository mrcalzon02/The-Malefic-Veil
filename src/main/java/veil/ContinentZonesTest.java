package veil;

import java.util.*;

/** Pure zone-address tests; no actor simulation or assumptions about rendered continent. */
public final class ContinentZonesTest {
    private static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
    public static void main(String[] args){
        int count=ContinentZones.ZONES_PER_AXIS;
        check(count==7,"Seven zones on each axis");
        int[] visits=new int[ContinentLayout.SIZE*ContinentLayout.SIZE];
        for(int zy=0;zy<count;zy++)for(int zx=0;zx<count;zx++){
            var key=new ContinentZones.Key(zx,zy);
            var b=ContinentZones.bounds(key);
            check(b.maxParcelXExclusive()>b.minParcelX()&&b.maxParcelYExclusive()>b.minParcelY(),"Nonempty zone");
            for(int y=b.minParcelY();y<b.maxParcelYExclusive();y++)
                for(int x=b.minParcelX();x<b.maxParcelXExclusive();x++){
                    visits[y*ContinentLayout.SIZE+x]++;
                    check(ContinentZones.ofParcel(x,y).equals(key),"Parcel mapped to owning zone");
                    check(ContinentZones.ofSector(ContinentSectors.keyFor(x,y)).equals(key),"Sector ownership agrees");
                }
        }
        for(int n:visits)check(n==1,"Zone partition must cover each parcel exactly once");
        check(ContinentZones.neighborhood(new ContinentZones.Key(0,0),1).size()==4,"Corner prewarm bounded");
        check(ContinentZones.neighborhood(new ContinentZones.Key(3,3),1).size()==9,"Interior prewarm");
        check(ContinentZones.neighborhood(new ContinentZones.Key(6,6),0).equals(
            List.of(new ContinentZones.Key(6,6))),"Exact single zone");
        try{ContinentZones.ofParcel(-1,0);throw new AssertionError("Negative parcel accepted");}
        catch(IllegalArgumentException expected){}
        try{ContinentZones.bounds(new ContinentZones.Key(count,0));throw new AssertionError("Invalid zone accepted");}
        catch(IllegalArgumentException expected){}
        System.out.println("PASS ContinentZonesTest: 49 zones, exact partition, neighbor prewarm");
    }
}
