package veil;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

/**
 * First real static-GLB pass: projects source mesh triangles in the raycast camera,
 * uses per-column wall depth masks, source material IDs and model-instance transforms.
 * Perspective texturing, per-pixel z buffer, skinned animation: later release gate.
 */
public final class PropRenderer {
    private static final double FOV=1.18, TAN=Math.tan(FOV/2);
    private int drawDistance=19, highDetailDistance=9, meshBudget=80;
    public void setBudgets(PerformanceSettings settings){
        drawDistance=settings.propDistance;
        highDetailDistance=settings.highDetailDistance;
        meshBudget=settings.meshBudget;
    }
    private record Face(int[] px,int[] py,int color,double depth,double opacity,
                        GlbMesh.Surface source,BufferedImage atlas,double shade){}
    /** Original source texel coordinates -> projected screen-space triangle. */
    private static AffineTransform atlasToScreen(Face face){
        GlbMesh.Surface uv=face.source();
        BufferedImage image=face.atlas();
        double u0=uv.au()*image.getWidth(),v0=(1-uv.av())*image.getHeight();
        double u1=uv.bu()*image.getWidth(),v1=(1-uv.bv())*image.getHeight();
        double u2=uv.cu()*image.getWidth(),v2=(1-uv.cv())*image.getHeight();
        double du1=u1-u0,dv1=v1-v0,du2=u2-u0,dv2=v2-v0;
        double det=du1*dv2-du2*dv1;
        if(!Double.isFinite(det)||Math.abs(det)<1e-8)return null;
        double dx1=face.px()[1]-face.px()[0],dx2=face.px()[2]-face.px()[0];
        double dy1=face.py()[1]-face.py()[0],dy2=face.py()[2]-face.py()[0];
        double a=(dx1*dv2-dx2*dv1)/det,b=(dx2*du1-dx1*du2)/det;
        double d=(dy1*dv2-dy2*dv1)/det,e=(dy2*du1-dy1*du2)/det;
        return new AffineTransform(a,d,b,e,face.px()[0]-a*u0-b*v0,
            face.py()[0]-d*u0-e*v0);
    }
    public int draw(Graphics2D g,UrbanScene scene,double playerX,double playerY,
                    double heading,double[] wallDepth,int width,int height){
        return draw(g,scene.models(),scene.nearby(playerX,playerY,drawDistance+7),
            playerX,playerY,heading,wallDepth,width,height);
    }
    /** Shared projection for licensed environment geometry and authored gameplay meshes. */
    public int draw(Graphics2D g,Map<String,GlbMesh> models,
                    java.util.List<UrbanScene.ModelInstance> instances,
                    double playerX,double playerY,double heading,double[] wallDepth,
                    int width,int height){
        return draw(g,models,instances,playerX,playerY,heading,wallDepth,
            width,height,1.55,0);
    }
    /**
     * Real in-engine elevated inspection camera. Default world camera still
     * uses eye height 1.55 and pitch 0 through the existing overload, so
     * gameplay wall-depth and weapon behavior remain bit-for-bit unchanged.
     */
    public int draw(Graphics2D g,Map<String,GlbMesh> models,
                    java.util.List<UrbanScene.ModelInstance> instances,
                    double playerX,double playerY,double heading,double[] wallDepth,
                    int width,int height,double eyeHeight,double pitch){
        if(!Double.isFinite(eyeHeight)||!Double.isFinite(pitch)||
           Math.abs(pitch)>1.20)throw new IllegalArgumentException("Invalid camera pitch");
        final double cp=Math.cos(pitch),sp=Math.sin(pitch);
        java.util.List<Face> faces=new ArrayList<>();
        // One depth threshold per column per frame, not one expensive trig call
        // for every column of every Kenney source triangle.
        double[] visibleDepth=new double[width];
        double focal=width/(2*TAN);
        for(int col=0;col<width;col++){
            double angle=Math.atan(((col+.5)/width-.5)*2*TAN);
            visibleDepth[col]=wallDepth[col]*Math.cos(angle);
        }
        int meshCount=0;
        int triangleBudget=Math.min(14000,Math.max(1500,meshBudget*115));
        int submittedTriangles=0;
        for(UrbanScene.ModelInstance placement:instances){
            if(placement.opacity()<=0)continue;
            double dx=placement.x()-playerX,dy=placement.y()-playerY;
            double dist=Math.hypot(dx,dy);
            if(dist>drawDistance||dist<.12)continue;
            double right=-Math.sin(heading)*dx+Math.cos(heading)*dy;
            double forward=Math.cos(heading)*dx+Math.sin(heading)*dy;
            if(forward<.06||Math.abs(right)>forward*(TAN+0.5)+2)continue;
            GlbMesh mesh=models.get(placement.model());
            if(mesh==null)continue;
            if(meshCount>=meshBudget)break;
            meshCount++;
            double yaw=placement.heading(),cy=Math.cos(yaw),sy=Math.sin(yaw);
            int faceStep=dist>highDetailDistance?4:1;
            int faceIndex=0;
            for(GlbMesh.Surface surface:mesh.surfaces()){
                GlbMesh.Triangle f=surface.triangle();
                if(submittedTriangles>=triangleBudget)break;
                if(faceIndex++%faceStep!=0)continue;
                int[] sx=new int[3],screenY=new int[3];
                double minDepth=Double.POSITIVE_INFINITY,meanDepth=0;
                boolean near=false;
                for(int i=0;i<3;i++){
                    float vx=i==0?f.ax():i==1?f.bx():f.cx();
                    float vy=i==0?f.ay():i==1?f.by():f.cy();
                    float vz=i==0?f.az():i==1?f.bz():f.cz();
                    double ox=(vx*cy-vz*sy)*placement.scale();
                    double oy=(vx*sy+vz*cy)*placement.scale();
                    double a=dx+ox,b=dy+oy;
                    double side=-Math.sin(heading)*a+Math.cos(heading)*b;
                    double horizontal=Math.cos(heading)*a+Math.sin(heading)*b;
                    double vertical=vy*placement.scale()+placement.heightOffset()-eyeHeight;
                    double z=horizontal*cp-vertical*sp;
                    double elevation=vertical*cp+horizontal*sp;
                    if(z<.07){near=true;break;}
                    minDepth=Math.min(z,minDepth);meanDepth+=z;
                    sx[i]=(int)Math.round(width*.5+side/z*focal);
                    screenY[i]=(int)Math.round(height*.5-elevation/z*focal);
                }
                if(near)continue;
                if(sx[0]==sx[1]&&sx[1]==sx[2])continue;
                int lo=Math.max(0,Math.min(sx[0],Math.min(sx[1],sx[2])));
                int hi=Math.min(width-1,Math.max(sx[0],Math.max(sx[1],sx[2])));
                if(lo>hi)continue;
                // Do not draw behind walls. Approximate by center depth until per-pixel Z gate.
                boolean exposed=false;
                for(int col=lo;col<=hi;col++){
                    if(minDepth<visibleDepth[col]){exposed=true;break;}
                }
                if(!exposed)continue;
                int source=f.rgb();
                double shade=Math.max(.31,1-dist/27);
                int r=(int)(((source>>16)&255)*shade),green=(int)(((source>>8)&255)*shade);
                int blue=(int)((source&255)*shade);
                BufferedImage atlas=null;
                if(surface.texture()!=null&&surface.texture().startsWith("ggbot-")){
                    try{atlas=mesh.textureImage(surface.texture());}
                    catch(IOException e){throw new IllegalStateException("Broken GGBot PNG atlas",e);}
                }
                faces.add(new Face(sx,screenY,(r<<16)|(green<<8)|blue,
                    meanDepth/3,placement.opacity(),surface,atlas,shade));
                submittedTriangles++;
            }
        }
        faces.sort((a,b)->Double.compare(b.depth(),a.depth()));
        Shape old=g.getClip();
        Composite previous=g.getComposite();
        Object interpolation=g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for(Face face:faces){
            // Painter's order for static props; background wall always stays authoritative.
            Polygon triangle=new Polygon(face.px(),face.py(),3);
            int min=Math.max(0,Math.min(face.px()[0],Math.min(face.px()[1],face.px()[2])));
            int max=Math.min(width-1,Math.max(face.px()[0],Math.max(face.px()[1],face.px()[2])));
            if(min>max)continue;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                (float)Math.max(0,Math.min(1,face.opacity()))));
            g.setColor(new Color(face.color()));
            // Coalesce contiguous visible columns into clipping runs. Creating
            // one java.awt.geom.Area per column was pathological for large cities.
            int run=-1;
            for(int col=min;col<=max+1;col++){
                boolean visible=col<=max&&face.depth()<visibleDepth[col];
                if(visible&&run<0)run=col;
                else if(!visible&&run>=0){
                    g.setClip(run,0,col-run,height);
                    if(old!=null)g.clip(old);
                    if(face.atlas()!=null){
                        // Clip the original PNG atlas to the projected triangle.
                        // Affine PSX UV mapping preserves the artist's per-corner UVs;
                        // painting one sampled color over an entire face does not.
                        g.clip(triangle);
                        AffineTransform uv=atlasToScreen(face);
                        if(uv==null){
                            g.setColor(new Color(face.color()));
                            g.fillPolygon(triangle);
                        }else{
                            g.drawImage(face.atlas(),uv,null);
                            if(face.shade()<.999){
                                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                    (float)Math.max(0,Math.min(1,face.opacity()*(1-face.shade())))));
                                g.setColor(Color.BLACK);
                                g.fillPolygon(triangle);
                                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                    (float)Math.max(0,Math.min(1,face.opacity()))));
                            }
                        }
                    }else g.fillPolygon(triangle);
                    run=-1;
                }
            }
        }
        g.setClip(old);
        g.setComposite(previous);
        if(interpolation==null)g.getRenderingHints().remove(RenderingHints.KEY_INTERPOLATION);
        else g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,interpolation);
        return meshCount;
    }
}
