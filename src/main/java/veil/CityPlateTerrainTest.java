package veil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Every procedural city uses exactly the same city/terrain elevation contract. */
public final class CityPlateTerrainTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        for(long seed:new long[]{0,1,42,20261020,-1,887766}){
            World world=new World(seed);
            ContinentLayout layout=world.continent();
            ContinentVista vista=new ContinentVista(world);
            CityPlateTerrain plate=vista.cityPlates();
            Map<Character,Integer> counts=new HashMap<>(),skirtCounts=new HashMap<>();
            Map<Character,Set<String>> shapes=new HashMap<>();
            for(int z=0;z<ContinentLayout.SIZE;z++)
                for(int x=0;x<ContinentLayout.SIZE;x++){
                    char city=layout.cityAt(x,z);
                    if(city=='\0')continue;
                    counts.merge(city,1,Integer::sum);
                    shapes.computeIfAbsent(city,k->new HashSet<>()).add(x+","+z);
                    double wx=plate.worldX(x+.5),wz=plate.worldZ(z+.5);
                    if(!world.detailedCity(wx,wz)&&
                       (wx<-ContinentVista.EMBANKMENT_WIDTH||
                        wx>World.SIZE+ContinentVista.EMBANKMENT_WIDTH||
                        wz<-ContinentVista.EMBANKMENT_WIDTH||
                        wz>World.SIZE+ContinentVista.EMBANKMENT_WIDTH))
                        check(Math.abs(vista.sample(wx,wz).elevation()-plate.plateHeight(city))<1e-5,
                            "City "+city+" parcel "+x+","+z+" not at its registered grade");
                }
            int measured=0;
            for(CityPlateTerrain.Edge edge:plate.edges()){
                skirtCounts.merge(edge.city(),1,Integer::sum);
                double top=plate.plateHeight(edge.city());
                check(Math.abs(edge.top()-top)<1e-6,"Foundation top above/below plate");
                check(Math.abs(edge.bottom()-(top-CityPlateTerrain.FOUNDATION_DEPTH))<1e-6,
                    "Foundation base mismatch");
                check(Math.abs(Math.hypot(edge.bx()-edge.ax(),edge.bz()-edge.az())-
                    World.PARCEL_METERS)<1e-6,"Broken parcel-length retaining edge");
                double mx=(edge.ax()+edge.bx())*.5,mz=(edge.az()+edge.bz())*.5;
                double dx=edge.az()==edge.bz()?0:.005;
                double dz=edge.ax()==edge.bx()?0:.005;
                // On an exposed edge, one normal sample must be in its city
                // and the opposite must be outside the city's exact mask.
                char left=plate.cityAt(mx-dx,mz-dz),right=plate.cityAt(mx+dx,mz+dz);
                check((left==edge.city())!=(right==edge.city()),
                    "Retaining edge does not follow final shaped city boundary");
                double ox=left==edge.city()?mx+dx:mx-dx;
                double oz=left==edge.city()?mz+dz:mz-dz;
                if(!world.detailedCity(ox,oz) &&
                   (ox<-ContinentVista.EMBANKMENT_WIDTH||
                    ox>World.SIZE+ContinentVista.EMBANKMENT_WIDTH||
                    oz<-ContinentVista.EMBANKMENT_WIDTH||
                    oz>World.SIZE+ContinentVista.EMBANKMENT_WIDTH)){
                    check(Math.abs(vista.sample(ox,oz).elevation()-edge.bottom())<.04,
                        "Generated terrain fails to meet city "+edge.city()+
                        " retaining base at "+ox+","+oz);
                    measured++;
                }
            }
            for(ContinentLayout.City city:layout.cities()){
                check(counts.getOrDefault(city.id(),0)>25,
                    "Generated footprint vanished: "+city.id());
                check(skirtCounts.getOrDefault(city.id(),0)>10,
                    "Generated city has no complete retaining skirt: "+city.id());
                check(shapes.get(city.id()).size()==counts.get(city.id()),
                    "Duplicate shape cells");
                // A near-square is not a forced-square world footprint. Exact
                // masks and perimeter lengths derive from the seeded blueprint.
            }
            check(measured>300,"Too few 3D city perimeter seams verified");
            System.out.println("PASS CityPlateTerrainTest seed="+seed+
                " cities="+counts.size()+" provenEdges="+measured+
                " cityCShape="+layout.cities().get(2).shape());
        }
    }
}
