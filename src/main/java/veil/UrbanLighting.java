package veil;

import java.util.*;

/** A small bounded light index, sourced only from actual placed Kenney fixtures. */
public final class UrbanLighting {
    public record Light(double x,double z,double y,double radius,double intensity,String meshId) {}
    private static final int CELL=12;
    private final Map<Long,List<Light>> buckets=new HashMap<>();
    private final List<Light> lights;
    private static long key(int x,int y){return ((long)x<<32)|(y&0xffffffffL);}
    public UrbanLighting(World world){
        Objects.requireNonNull(world);
        ArrayList<Light> all=new ArrayList<>();
        for(World.UrbanProp p:world.urbanProps){
            if(p.model().equals("detail-light-single")||p.model().equals("detail-light-double")||
                p.model().equals("detail-light-traffic")){
                double height=p.heightOffset()+(p.model().equals("detail-light-double")?2.35:1.9)*p.scale();
                double radius=p.model().equals("detail-light-traffic")?7.0:10.0;
                Light fixture=new Light(p.x(),p.y(),height,radius,1.0,p.model());
                all.add(fixture);
                int bx=(int)Math.floor(p.x()/CELL),by=(int)Math.floor(p.y()/CELL);
                buckets.computeIfAbsent(key(bx,by),k->new ArrayList<>()).add(fixture);
            }
        }
        lights=List.copyOf(all);
    }
    public List<Light> lights(){return lights;}
    /** Nearest eight only: hard bound on fragment shader work. */
    public List<Light> nearby(double x,double z,int limit){
        if(!Double.isFinite(x)||!Double.isFinite(z)||limit<0||limit>16)
            throw new IllegalArgumentException("Light query outside limits");
        int bx=(int)Math.floor(x/CELL),by=(int)Math.floor(z/CELL);
        List<Light> found=new ArrayList<>();
        for(int yy=by-2;yy<=by+2;yy++)for(int xx=bx-2;xx<=bx+2;xx++){
            List<Light> row=buckets.get(key(xx,yy));
            if(row==null)continue;
            for(Light light:row){
                double dx=light.x()-x,dz=light.z()-z;
                if(dx*dx+dz*dz<35*35)found.add(light);
            }
        }
        found.sort(Comparator.comparingDouble(l->{
            double dx=l.x()-x,dz=l.z()-z;
            return dx*dx+dz*dz;
        }));
        return List.copyOf(found.subList(0,Math.min(limit,found.size())));
    }
}
