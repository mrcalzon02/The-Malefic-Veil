package veil;

import java.util.ArrayList;
import java.util.List;

/**
 * One height/footprint contract for every seeded city, not a hand-placed
 * rectangular patch. Reads ContinentLayout's FINAL post-pruning city mask,
 * roads and seed. Coordinates are the host's six-metre world parcels.
 */
public final class CityPlateTerrain {
    public static final double FOUNDATION_DEPTH=2.0;
    public static final double EMBANKMENT_WIDTH=24.0;
    public static final double ROAD_RAMP_LENGTH=18.0;
    public record Footprint(char city,double distance,boolean inside){}
    /** Exposed side of a generated city cell, top of plate down to terrain grade. */
    public record Edge(char city,double ax,double az,double bx,double bz,
                       double top,double bottom){}
    private final ContinentLayout layout;
    private final int offsetX,offsetZ;
    private final double[] level=new double[7];
    /** Crossing where an actual generated intercity road reaches a city road. */
    public record Gate(char city,double ax,double az,double bx,double bz,
                       double nx,double nz,double top){}
    private final List<Edge> edges;
    private final List<Gate> gates;

    public CityPlateTerrain(World world){
        layout=world.continent();
        offsetX=world.worldOffsetX();offsetZ=world.worldOffsetY();
        for(ContinentLayout.City city:layout.cities()){
            int i=city.id()-'A';
            // City A is the pre-existing y=0 detailed city. Other seeded sites
            // retain their own natural regional grade, quantized for stable lots.
            level[i]=i==0?0:Math.rint(Math.max(0,Math.min(12,
                layout.elevation(city.x(),city.y())*10))*4)/4.0;
        }
        List<Edge> seams=new ArrayList<>();
        List<Gate> crossings=new ArrayList<>();
        for(int pz=0;pz<ContinentLayout.SIZE;pz++)
            for(int px=0;px<ContinentLayout.SIZE;px++){
                char id=layout.cityAt(px,pz);
                if(id=='\0')continue;
                double minX=worldX(px),minZ=worldZ(pz);
                double top=plateHeight(id),base=top-FOUNDATION_DEPTH;
                if(layout.cityAt(px,pz-1)!=id)
                    boundary(seams,crossings,id,layout.road(px,pz)&&layout.road(px,pz-1),
                        minX,minZ,minX+World.PARCEL_METERS,minZ,0,-1,top,base);
                if(layout.cityAt(px+1,pz)!=id)
                    boundary(seams,crossings,id,layout.road(px,pz)&&layout.road(px+1,pz),
                        minX+World.PARCEL_METERS,minZ,
                        minX+World.PARCEL_METERS,minZ+World.PARCEL_METERS,1,0,top,base);
                if(layout.cityAt(px,pz+1)!=id)
                    boundary(seams,crossings,id,layout.road(px,pz)&&layout.road(px,pz+1),
                        minX+World.PARCEL_METERS,minZ+World.PARCEL_METERS,
                        minX,minZ+World.PARCEL_METERS,0,1,top,base);
                if(layout.cityAt(px-1,pz)!=id)
                    boundary(seams,crossings,id,layout.road(px,pz)&&layout.road(px-1,pz),
                        minX,minZ+World.PARCEL_METERS,minX,minZ,-1,0,top,base);
            }
        edges=List.copyOf(seams);
        gates=List.copyOf(crossings);
    }
    private static void boundary(List<Edge> walls,List<Gate> ramps,char city,
            boolean road,double ax,double az,double bx,double bz,
            double nx,double nz,double top,double base){
        if(road)ramps.add(new Gate(city,ax,az,bx,bz,nx,nz,top));
        else walls.add(new Edge(city,ax,az,bx,bz,top,base));
    }
    public List<Gate> gates(){return gates;}
    public ContinentLayout layout(){return layout;}
    public List<Edge> edges(){return edges;}
    public double worldX(double parcel){return offsetX+parcel*World.PARCEL_METERS;}
    public double worldZ(double parcel){return offsetZ+parcel*World.PARCEL_METERS;}
    public char cityAt(double x,double z){
        return layout.cityAt((int)Math.floor((x-offsetX)/World.PARCEL_METERS),
            (int)Math.floor((z-offsetZ)/World.PARCEL_METERS));
    }
    public double plateHeight(char id){
        if(id<'A'||id>'G')throw new IllegalArgumentException("Unknown city "+id);
        return level[id-'A'];
    }
    /** Nearest authoritative city cell rectangle, including concavities and holes.
     * Only cells within the actual embankment range are inspected. */
    public Footprint closest(double x,double z){
        int px=(int)Math.floor((x-offsetX)/World.PARCEL_METERS);
        int pz=(int)Math.floor((z-offsetZ)/World.PARCEL_METERS);
        char local=layout.cityAt(px,pz);
        if(local!='\0')return new Footprint(local,0,true);
        double shortest=EMBANKMENT_WIDTH*EMBANKMENT_WIDTH;
        char chosen='\0';
        int radius=(int)Math.ceil(EMBANKMENT_WIDTH/World.PARCEL_METERS)+1;
        for(int iz=pz-radius;iz<=pz+radius;iz++)
            for(int ix=px-radius;ix<=px+radius;ix++){
                char id=layout.cityAt(ix,iz);
                if(id=='\0')continue;
                double left=worldX(ix),top=worldZ(iz);
                double dx=Math.max(0,Math.max(left-x,x-left-World.PARCEL_METERS));
                double dz=Math.max(0,Math.max(top-z,z-top-World.PARCEL_METERS));
                double d2=dx*dx+dz*dz;
                if(d2<shortest||(d2==shortest&&(chosen=='\0'||id<chosen))){
                    chosen=id;shortest=d2;
                }
            }
        return chosen=='\0'?new Footprint('\0',Double.POSITIVE_INFINITY,false):
            new Footprint(chosen,Math.sqrt(shortest),false);
    }
    public double height(double x,double z,double rawHeight){
        Footprint near=closest(x,z);
        if(near.city()=='\0')return rawHeight;
        double plate=plateHeight(near.city());
        if(near.inside())return plate;
        double t=Math.min(1,near.distance()/EMBANKMENT_WIDTH);
        t=t*t*(3-2*t);
        double grade=plate-FOUNDATION_DEPTH+(rawHeight-(plate-FOUNDATION_DEPTH))*t;
        // A highway crossing is not a two-metre masonry cliff: its generated
        // city edge must stay flush with the city road and descend gradually.
        for(Gate gate:gates){
            if(gate.city()!=near.city())continue;
            double mx=(gate.ax()+gate.bx())*.5,mz=(gate.az()+gate.bz())*.5;
            double along=(x-mx)*gate.nx()+(z-mz)*gate.nz();
            if(along<0||along>ROAD_RAMP_LENGTH)continue;
            double sx=gate.bx()-gate.ax(),sz=gate.bz()-gate.az();
            double progress=((x-gate.ax())*sx+(z-gate.az())*sz)/
                (World.PARCEL_METERS*World.PARCEL_METERS);
            double shoulder=Math.max(0,Math.max(-progress,progress-1))*World.PARCEL_METERS;
            if(shoulder>=1.5)continue;
            double falloff=along/ROAD_RAMP_LENGTH;
            falloff=falloff*falloff*(3-2*falloff);
            double edgeFade=shoulder/1.5;
            edgeFade=edgeFade*edgeFade*(3-2*edgeFade);
            double weight=(1-falloff)*(1-edgeFade);
            grade+=(plate-grade)*weight;
        }
        return grade;
    }
}
