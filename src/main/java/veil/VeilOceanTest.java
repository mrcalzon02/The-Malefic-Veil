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
        // City A can legitimately have no visible sea. A separate coastal
        // viewport must source non-empty real water parcels, not pass a
        // graphics test with a zero-triangle water mesh.
        ContinentVista coastal=new ContinentVista(world,'E');
        VeilOcean coastalOcean=new VeilOcean(world,coastal);
        int coastalParcels=0;
        for(ContinentVista.Patch patch:coastal.patches())
            for(VeilOcean.SeaParcel parcel:coastalOcean.sourceSeaParcels(
                patch.left(),patch.top(),
                patch.left()+patch.width(),patch.top()+patch.depth())){
                coastalParcels++;
                check(coastalOcean.wet(parcel.west()+parcel.size()*.5,
                        parcel.north()+parcel.size()*.5),
                    "Generated water parcel is not physically wet");
            }
        check(coastalParcels>0,
            "Coastal GPU test sector has no real ocean geometry to exercise");
        boolean surf=false;
        for(int x=1;x<ContinentLayout.SIZE-1&&!surf;x++)
            for(int z=1;z<ContinentLayout.SIZE-1&&!surf;z++){
                if(world.continent().biome(x,z)!=ContinentLayout.Biome.WATER)continue;
                double wx=vista.cityPlates().worldX(x),wz=vista.cityPlates().worldZ(z);
                double size=World.PARCEL_METERS,eps=.20;
                double[][] nearLand={
                    {wx+eps,wz+size*.5,x-1,z},
                    {wx+size-eps,wz+size*.5,x+1,z},
                    {wx+size*.5,wz+eps,x,z-1},
                    {wx+size*.5,wz+size-eps,x,z+1}
                };
                for(double[] edge:nearLand)
                    if(world.continent().biome((int)edge[2],(int)edge[3])!=
                        ContinentLayout.Biome.WATER){
                        check(ocean.wet(edge[0],edge[1])&&
                            ocean.bottom(edge[0],edge[1])>-.7,
                            "Actual coastline does not shallow for wave run-up and foam");
                        surf=true;break;
                    }
            }
        check(surf,"Seeded map never exercises a real land-to-ocean surf boundary");
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
        // Exercise the actual independent optical compositor, not just a
        // Beckenburg class copied into the new source tree.
        int[] pixels=new int[32*24];
        double[] depth=new double[pixels.length];
        Arrays.fill(pixels,0x213046);Arrays.fill(depth,100);
        var view=new dev.mrcalzon.water.WaterModule.Camera(
            seaX,2.2,seaZ,0,-.78,24,32,24);
        var rendered=ocean.module().composite(view,pixels,depth,2.25);
        check(rendered.pixels()>0&&
            Arrays.stream(pixels).anyMatch(c->c!=0x213046),
            "Original water module failed to composite real ocean pixels");
        check(!ocean.wet(vista.cityPlates().worldX(-1),vista.cityPlates().worldZ(-1)),
            "Sea renderer silently creates bodies beyond the bounded continent");
        System.out.println("PASS VeilOceanTest: "+found+" sea and "+dry+
            " land parcels, source coastline, waves, storm response, real optical layers");
    }
}
