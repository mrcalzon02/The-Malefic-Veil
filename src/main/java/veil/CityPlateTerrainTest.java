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
            // Roads across real generated city boundaries must have a
            // continuous graded approach, never a retaining wall across them.
            Map<Character,Integer> crossings=new HashMap<>();
            for(CityPlateTerrain.Gate gate:plate.gates()){
                crossings.merge(gate.city(),1,Integer::sum);
                double x=(gate.ax()+gate.bx())*.5;
                double z=(gate.az()+gate.bz())*.5;
                double nx=gate.nx(),nz=gate.nz();
                boolean duplicated=plate.edges().stream().anyMatch(edge->
                    edge.city()==gate.city()&&edge.ax()==gate.ax()&&
                    edge.az()==gate.az()&&edge.bx()==gate.bx()&&edge.bz()==gate.bz());
                check(!duplicated,"Retaining wall blocks a generated intercity highway");
                if(gate.city()!='A'&&
                   (x<-ContinentVista.EMBANKMENT_WIDTH||
                    x>World.SIZE+ContinentVista.EMBANKMENT_WIDTH||
                    z<-ContinentVista.EMBANKMENT_WIDTH||
                    z>World.SIZE+ContinentVista.EMBANKMENT_WIDTH)){
                    check(Math.abs(vista.sample(x+nx*.001,z+nz*.001).elevation()-
                        gate.top())<.05,"Procedural city highway has a vertical entrance cliff");
                    check(Double.isFinite(vista.sample(x+nx*12,z+nz*12).elevation()),
                        "Non-finite generated city ramp terrain");
                }
            }
            for(var city:layout.cities())
                check(crossings.getOrDefault(city.id(),0)>0,
                    "Generated city lacks a terrain-compatible route exit "+city.id());
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
            // Inspect actual GPU vertex data, not only the sampled height
            // function. The top and vertical wall must both be present.
            if(seed==20261020L||seed==887766L)
            for(char id:new char[]{'B','C','D','E','F','G'}){
                ContinentVista site=new ContinentVista(world,id);
                double expected=plate.plateHeight(id);
                double cx=site.cityCenterX(id),cz=site.cityCenterZ(id);
                double maxRadius=World.PARCEL_METERS*
                    (Math.max(layout.cities().get(id-'A').radiusX(),
                              layout.cities().get(id-'A').radiusY())+3);
                boolean sawTop=false,sawFoot=false;
                for(ContinentVista.Patch patch:site.patches()){
                    float[] verts=patch.vertexData();
                    for(int pos=0;pos<verts.length;pos+=ContinentVista.STRIDE){
                        double x=verts[pos],y=verts[pos+1],z=verts[pos+2];
                        if(Math.hypot(x-cx,z-cz)>maxRadius)continue;
                        if(Math.abs(y-expected)<.0001)sawTop=true;
                        if(Math.abs(y-(expected-CityPlateTerrain.FOUNDATION_DEPTH))<.0001)
                            sawFoot=true;
                    }
                }
                check(sawTop,"No actual 3D surface vertices on generated city "+id);
                check(sawFoot,"No 3D foundation-foot vertices on generated city "+id);
            }
            System.out.println("PASS CityPlateTerrainTest seed="+seed+
                " cities="+counts.size()+" provenEdges="+measured+
                " cityCShape="+layout.cities().get(2).shape());
        }
    }
}
