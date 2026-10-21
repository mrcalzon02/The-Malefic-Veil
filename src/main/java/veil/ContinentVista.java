package veil;

import java.util.*;

/**
 * Render-only continental terrain around the existing bounded 132x132 city.
 *
 * Terrain and vegetation are sampled from the actual ContinentLayout seed near
 * City A; this never invents a different random continent. Outside-city cells
 * are NOT yet server-authoritative walkable zones. No fake endless flat plane.
 */
public final class ContinentVista {
    public static final int EXTENT=96;
    public static final int STEP=2;
    public static final int CHUNK=32;
    public static final int STRIDE=7; // xyz, rgba
    public record Patch(int left,int top,int width,int depth,int triangles,float[] vertexData){
        public float[] copyVertices(){return vertexData.clone();}
    }
    public record Sample(double elevation,ContinentLayout.Biome biome,boolean road) {}
    private final ContinentLayout layout;
    private final ContinentLayout.City anchor;
    private final long seed;
    public ContinentVista(World world){
        Objects.requireNonNull(world);
        layout=world.continent();
        anchor=layout.cities().get(0);
        seed=world.seed;
    }
    /** Projection keeps the authored city near City A without claiming live cross-zone travel. */
    private double parcelX(double x){return anchor.x()+(x-World.SIZE/2.0)/3.0;}
    private double parcelY(double y){return anchor.y()+(y-World.SIZE/2.0)/3.0;}
    private static double interp(double a,double b,double t){return a+(b-a)*t;}
    private double level(double px,double py){
        int ax=(int)Math.floor(px),ay=(int)Math.floor(py);
        double tx=px-ax,ty=py-ay;
        return interp(interp(layout.elevation(ax,ay),layout.elevation(ax+1,ay),tx),
            interp(layout.elevation(ax,ay+1),layout.elevation(ax+1,ay+1),tx),ty);
    }
    private static double cityDistance(double x,double y){
        double dx=x<0?-x:x>World.SIZE?x-World.SIZE:0;
        double dy=y<0?-y:y>World.SIZE?y-World.SIZE:0;
        return Math.hypot(dx,dy);
    }
    public Sample sample(double x,double y){
        if(!Double.isFinite(x)||!Double.isFinite(y))
            throw new IllegalArgumentException("Invalid continental world coordinate");
        double px=parcelX(x),py=parcelY(y);
        int xx=(int)Math.floor(px),yy=(int)Math.floor(py);
        var biome=layout.biome(xx,yy);
        boolean road=layout.road(xx,yy);
        double target=biome==ContinentLayout.Biome.WATER?-2.8:
            Math.max(.03,Math.min(12.0,level(px,py)*10.0));
        // City street elevations remain exactly zero at the handoff.
        double border=cityDistance(x,y);
        double smooth=Math.max(0,Math.min(1,border/13.0));
        smooth=smooth*smooth*(3-2*smooth);
        return new Sample(target*smooth,biome,road);
    }
    private static boolean outside(int x,int y){
        return x<0||y<0||x>=World.SIZE||y>=World.SIZE;
    }
    private static int rgb(Sample sample,long hash){
        int code=switch(sample.biome()){
            case WATER -> 0x30485a;
            case BEACH -> 0x83765c;
            case PLAINS -> 0x596c45;
            case FOREST -> 0x40593e;
            case HILLS -> 0x656957;
            case MOUNTAINS -> 0x77736c;
            case MARSH -> 0x485a49;
        };
        if(sample.road()&&sample.biome()!=ContinentLayout.Biome.WATER)code=0x60625d;
        double variation=0.93+(hash&7)*.012;
        int r=Math.min(255,(int)(((code>>16)&255)*variation));
        int g=Math.min(255,(int)(((code>>8)&255)*variation));
        int b=Math.min(255,(int)((code&255)*variation));
        return r<<16|g<<8|b;
    }
    private static int emit(float[] target,int at,double x,double height,double z,int rgb){
        target[at++]=(float)x;
        target[at++]=(float)height;
        target[at++]=(float)z;
        target[at++]=((rgb>>>16)&255)/255f;
        target[at++]=((rgb>>>8)&255)/255f;
        target[at++]=(rgb&255)/255f;
        target[at++]=1.0f;
        return at;
    }
    public List<Patch> patches(){
        List<Patch> chunks=new ArrayList<>();
        int start=-EXTENT,end=World.SIZE+EXTENT;
        for(int y0=start;y0<end;y0+=CHUNK)
            for(int x0=start;x0<end;x0+=CHUNK){
                float[] verts=new float[CHUNK*CHUNK/(STEP*STEP)*6*STRIDE];
                int index=0,triangles=0;
                for(int z=y0;z<Math.min(y0+CHUNK,end);z+=STEP)
                    for(int x=x0;x<Math.min(x0+CHUNK,end);x+=STEP){
                        // Don't redraw playable city underneath its real Kenney tiles.
                        if(!outside(x,z)&&!outside(x+STEP-1,z+STEP-1))continue;
                        Sample a=sample(x,z),b=sample(x+STEP,z),
                            c=sample(x+STEP,z+STEP),d=sample(x,z+STEP);
                        long hash=World.mix(seed^((long)x*734287L)^((long)z*2794703L));
                        int color=rgb(sample(x+1,z+1),hash);
                        index=emit(verts,index,x,a.elevation(),z,color);
                        index=emit(verts,index,x+STEP,b.elevation(),z,color);
                        index=emit(verts,index,x+STEP,c.elevation(),z+STEP,color);
                        index=emit(verts,index,x,a.elevation(),z,color);
                        index=emit(verts,index,x+STEP,c.elevation(),z+STEP,color);
                        index=emit(verts,index,x,d.elevation(),z+STEP,color);
                        triangles+=2;
                    }
                if(triangles>0)chunks.add(new Patch(x0,y0,CHUNK,CHUNK,triangles,
                    Arrays.copyOf(verts,index)));
            }
        return List.copyOf(chunks);
    }
    /** Scene-only Kenney trees beyond the authoritative city edge. */
    public List<World.UrbanProp> foliage(){
        List<World.UrbanProp> props=new ArrayList<>();
        for(int z=-EXTENT+4;z<World.SIZE+EXTENT-4;z+=7)
            for(int x=-EXTENT+4;x<World.SIZE+EXTENT-4;x+=7){
                if(!outside(x,z)||cityDistance(x,z)<7)continue;
                long n=World.mix(seed^((long)x*2147483647L)^((long)z*281474976710L));
                Sample at=sample(x+3,z+3);
                boolean grow=switch(at.biome()){
                    case FOREST -> (n&3)!=0;                // dense woodland
                    case PLAINS -> Math.floorMod(n,3)==0;  // isolated copses
                    case MARSH -> (n&1)==0;                 // low wet brush
                    case HILLS -> (n&7)<=1;                 // sporadic trees
                    case BEACH -> (n&31)==0;                // coastal shrubs
                    default -> false;                       // no trees in open water
                };
                if(grow&&!at.road()){
                    String id=at.biome()==ContinentLayout.Biome.MARSH||
                        at.biome()==ContinentLayout.Biome.BEACH?"tree-shrub":
                        (n&3)==0?"tree-pine-small":
                        (n&3)==1?"tree-small":(n&3)==2?"tree-pine-large":"tree-park-large";
                    double px=x+2.5,pz=z+2.5;
                    props.add(new World.UrbanProp(id,px,pz,0,
                        id.equals("tree-shrub")?1.05:(n&8)==0?1.5:1.8,
                        0,sample(px,pz).elevation()));
                }
            }
        return List.copyOf(props);
    }
}
