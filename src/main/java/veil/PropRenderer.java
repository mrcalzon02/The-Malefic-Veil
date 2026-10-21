package veil;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.*;

/**
 * First real static-GLB pass: projects source mesh triangles in the raycast camera,
 * uses per-column wall depth masks, source material IDs and model-instance transforms.
 * Perspective texturing, per-pixel z buffer, skinned animation: later release gate.
 */
public final class PropRenderer {
    private static final double FOV=1.18, TAN=Math.tan(FOV/2);
    private record Face(int[] px,int[] py,int color,double depth,double opacity){}
    public int draw(Graphics2D g,UrbanScene scene,double playerX,double playerY,
                    double heading,double[] wallDepth,int width,int height){
        return draw(g,scene.models(),scene.instances(),playerX,playerY,heading,wallDepth,width,height);
    }
    /** Shared projection for licensed environment geometry and authored gameplay meshes. */
    public int draw(Graphics2D g,Map<String,GlbMesh> models,
                    java.util.List<UrbanScene.ModelInstance> instances,
                    double playerX,double playerY,double heading,double[] wallDepth,
                    int width,int height){
        java.util.List<Face> faces=new ArrayList<>();
        int meshCount=0;
        for(UrbanScene.ModelInstance placement:instances){
            if(placement.opacity()<=0)continue;
            double dx=placement.x()-playerX,dy=placement.y()-playerY;
            double dist=Math.hypot(dx,dy);
            if(dist>19||dist<.12)continue;
            double right=-Math.sin(heading)*dx+Math.cos(heading)*dy;
            double forward=Math.cos(heading)*dx+Math.sin(heading)*dy;
            if(forward<.06||Math.abs(right)>forward*(TAN+0.5)+2)continue;
            GlbMesh mesh=models.get(placement.model());
            if(mesh==null)continue;
            meshCount++;
            double yaw=placement.heading(),cy=Math.cos(yaw),sy=Math.sin(yaw);
            for(GlbMesh.Triangle f:mesh.triangles()){
                float[][] vert={
                    {f.ax(),f.ay(),f.az()},{f.bx(),f.by(),f.bz()},{f.cx(),f.cy(),f.cz()}
                };
                int[] sx=new int[3],screenY=new int[3];
                double minDepth=Double.POSITIVE_INFINITY,meanDepth=0;
                boolean near=false;
                for(int i=0;i<3;i++){
                    double ox=(vert[i][0]*cy-vert[i][2]*sy)*placement.scale();
                    double oy=(vert[i][0]*sy+vert[i][2]*cy)*placement.scale();
                    double a=dx+ox,b=dy+oy;
                    double side=-Math.sin(heading)*a+Math.cos(heading)*b;
                    double z=Math.cos(heading)*a+Math.sin(heading)*b;
                    if(z<.07){near=true;break;}
                    minDepth=Math.min(z,minDepth);meanDepth+=z;
                    sx[i]=(int)Math.round(width*.5+side/z*width/(2*TAN));
                    screenY[i]=(int)Math.round(height*.5-
                        (vert[i][1]*placement.scale()-1.55)/z*width/(2*TAN));
                }
                if(near)continue;
                if(sx[0]==sx[1]&&sx[1]==sx[2])continue;
                int lo=Math.max(0,Math.min(sx[0],Math.min(sx[1],sx[2])));
                int hi=Math.min(width-1,Math.max(sx[0],Math.max(sx[1],sx[2])));
                if(lo>hi)continue;
                // Do not draw behind walls. Approximate by center depth until per-pixel Z gate.
                boolean exposed=false;
                for(int col=lo;col<=hi;col++){
                    double off=Math.atan(((col+.5)/width-.5)*2*TAN);
                    if(minDepth<wallDepth[col]*Math.cos(off)){exposed=true;break;}
                }
                if(!exposed)continue;
                int source=f.rgb();
                double shade=Math.max(.31,1-dist/27);
                int r=(int)(((source>>16)&255)*shade),green=(int)(((source>>8)&255)*shade);
                int blue=(int)((source&255)*shade);
                faces.add(new Face(sx,screenY,(r<<16)|(green<<8)|blue,meanDepth/3,placement.opacity()));
            }
        }
        faces.sort((a,b)->Double.compare(b.depth(),a.depth()));
        Shape old=g.getClip();
        Composite previous=g.getComposite();
        for(Face face:faces){
            // Painter's order for static props; background wall always stays authoritative.
            Polygon triangle=new Polygon(face.px(),face.py(),3);
            int min=Math.max(0,Arrays.stream(face.px()).min().orElse(0));
            int max=Math.min(width-1,Arrays.stream(face.px()).max().orElse(width-1));
            Area vis=new Area();
            for(int col=min;col<=max;col++){
                double off=Math.atan(((col+.5)/width-.5)*2*TAN);
                if(face.depth()<wallDepth[col]*Math.cos(off))
                    vis.add(new Area(new Rectangle2D.Double(col,0,1,height)));
            }
            if(vis.isEmpty())continue;
            g.setClip(vis);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                (float)Math.max(0,Math.min(1,face.opacity()))));
            g.setColor(new Color(face.color()));
            g.fillPolygon(triangle);
        }
        g.setClip(old);
        g.setComposite(previous);
        return meshCount;
    }
}
