package veil;

import dev.mrcalzon.water.*;
import java.util.*;

/** Independent water library + Malefic seeded coastline and storm coupling. */
public final class VeilOceanTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    public static void main(String[] args){
        World world=new World(20261020L);
        ContinentVista vista=new ContinentVista(world);
        VeilOcean ocean=new VeilOcean(world,vista);
        check(ocean.module().bodies().size()==1,
            "The continent must be one bounded ocean, not unconnected water tiles");
        int found=0,dry=0;double seaX=0,seaZ=0,landX=0,landZ=0;
        for(int x=0;x<ContinentLayout.SIZE;x++)
            for(int z=0;z<ContinentLayout.SIZE;z++){
                double wx=vista.cityPlates().worldX(x+.5);
                double wz=vista.cityPlates().worldZ(z+.5);
                if(world.continent().biome(x,z)==ContinentLayout.Biome.WATER){
                    if(found++==0){seaX=wx;seaZ=wz;}
                    check(ocean.wet(wx,wz)&&ocean.bottom(wx,wz)<0,
                        "Source coastline water has no real shared-library water surface");
                }else{
                    if(dry++==0){landX=wx;landZ=wz;}
                    check(!ocean.wet(wx,wz),
                        "The ocean flooded authoritative seeded land or protected city");
                }
            }
        check(found>0&&dry>0,"Seed has no real land/sea interface");
        check(Double.isNaN(ocean.height(landX,landZ,1,"CLEAR"))&&
            ocean.color(landX,landZ,1,"CLEAR")==0,
            "Water renderer shaded dry land");
        double first=ocean.height(seaX,seaZ,1,"CLEAR");
        double second=ocean.height(seaX,seaZ,2,"CLEAR");
        check(Double.isFinite(first)&&Double.isFinite(second)&&
            Math.abs(first-second)>1e-5,
            "The imported geometric waves are static");
        check(ocean.effects("THUNDERSTORM").envelope()>
            ocean.effects("CLEAR").envelope(),
            "Real host weather does not affect ocean wave amplitude");
        int color=ocean.color(seaX,seaZ,2,"RAIN");
        check((color>>>24)>0&&(color&0xffffff)!=0,
            "Independent scrolling water texture produces no visible surface color");
        check(!ocean.wet(vista.cityPlates().worldX(-1),vista.cityPlates().worldZ(-1)),
            "Sea renderer silently creates bodies beyond the bounded continent");
        System.out.println("PASS VeilOceanTest: "+found+" sea and "+dry+
            " land parcels, source coastline, waves, storm response, real optical layers");
    }
}
