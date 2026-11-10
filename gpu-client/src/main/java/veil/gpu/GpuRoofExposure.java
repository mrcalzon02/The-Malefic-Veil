package veil.gpu;

import veil.GlbMesh;
import veil.UrbanScene;
import java.util.*;

/**
 * Upward geometric rays through the real transformed Kenney roof triangles.
 * A zone category alone is NOT roof cover: damaged/absent roofs leak rain.
 * This is a bounded static-roof material mask, not host collision or climate.
 */
final class GpuRoofExposure {
    private static final int CELL=6;
    private static final Set<String> ROOFS=Set.of(
        "roof-metal-type-a","roof-metal-type-b","wall-b-roof");
    record Point(double x,double y,double z) {}
    private record Triangle(double ax,double ay,double az,double bx,double by,
                            double bz,double cx,double cy,double cz,double det) {}
    private final Map<Long,List<Triangle>> buckets=new HashMap<>();
    private final List<Point> samples=new ArrayList<>();
    private int roofCount;
    private static long key(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
    GpuRoofExposure(UrbanScene scene) {
        for(var item:scene.instances()){
            if(!ROOFS.contains(item.model()))continue;
            var model=scene.models().get(item.model());
            if(model==null)throw new IllegalStateException("Missing real roof mesh");
            double cs=Math.cos(item.heading()),sn=Math.sin(item.heading());
            int added=0;
            for(var t:model.triangles()){
                double ax=item.x()+item.scale()*(t.ax()*cs-t.az()*sn);
                double az=item.y()+item.scale()*(t.ax()*sn+t.az()*cs);
                double bx=item.x()+item.scale()*(t.bx()*cs-t.bz()*sn);
                double bz=item.y()+item.scale()*(t.bx()*sn+t.bz()*cs);
                double cx=item.x()+item.scale()*(t.cx()*cs-t.cz()*sn);
                double cz=item.y()+item.scale()*(t.cx()*sn+t.cz()*cs);
                double ay=item.heightOffset()+item.scale()*t.ay();
                double by=item.heightOffset()+item.scale()*t.by();
                double cy=item.heightOffset()+item.scale()*t.cy();
                double det=(bz-cz)*(ax-cx)+(cx-bx)*(az-cz);
                double ux=bx-ax,uy=by-ay,uz=bz-az,vx=cx-ax,vy=cy-ay,vz=cz-az;
                double norm=Math.sqrt(Math.pow(uy*vz-uz*vy,2)+det*det+
                    Math.pow(ux*vy-uy*vx,2));
                if(Math.abs(det)<1e-8||Math.abs(det)<norm*.35)continue;
                var face=new Triangle(ax,ay,az,bx,by,bz,cx,cy,cz,det);
                double minX=Math.min(ax,Math.min(bx,cx)),maxX=Math.max(ax,Math.max(bx,cx));
                double minZ=Math.min(az,Math.min(bz,cz)),maxZ=Math.max(az,Math.max(bz,cz));
                for(int ix=(int)Math.floor(minX/CELL);ix<=(int)Math.floor(maxX/CELL);ix++)
                    for(int iz=(int)Math.floor(minZ/CELL);iz<=(int)Math.floor(maxZ/CELL);iz++)
                        buckets.computeIfAbsent(key(ix,iz),k->new ArrayList<>()).add(face);
                samples.add(new Point((ax+bx+cx)/3,(ay+by+cy)/3,(az+bz+cz)/3));
                added++;
            }
            if(added>0)roofCount++;
        }
    }
    int roofCount(){return roofCount;}
    Point pointNear(double x,double z){
        return samples.stream().min(Comparator.comparingDouble(p->
            (p.x()-x)*(p.x()-x)+(p.z()-z)*(p.z()-z)))
            .orElseThrow(()->new IllegalStateException("Missing real source roof"));
    }
    boolean covered(double x,double elevation,double z){
        var faces=buckets.get(key((int)Math.floor(x/CELL),(int)Math.floor(z/CELL)));
        if(faces==null)return false;
        for(var t:faces){
            double a=((t.bz-t.cz)*(x-t.cx)+(t.cx-t.bx)*(z-t.cz))/t.det;
            double b=((t.cz-t.az)*(x-t.cx)+(t.ax-t.cx)*(z-t.cz))/t.det;
            if(a< -1e-7||b< -1e-7||a+b>1.0000001)continue;
            if(a*t.ay+b*t.by+(1-a-b)*t.cy>elevation+.08)return true;
        }
        return false;
    }
    float exposure(UrbanScene.ModelInstance instance,GlbMesh model){
        if(ROOFS.contains(instance.model()))return 1f;
        double top=instance.heightOffset()+
            instance.scale()*(model==null?1:model.bounds()[4]);
        return covered(instance.x(),top,instance.y())?0f:1f;
    }
}
