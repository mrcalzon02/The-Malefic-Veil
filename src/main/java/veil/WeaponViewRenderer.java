package veil;

import java.awt.*;
import java.util.*;

/** Projects the actual authored 3D gun triangles into a PSX first-person view. */
public final class WeaponViewRenderer {
    private record Face(int[] x,int[] y,int color,double depth){}
    public void draw(Graphics2D g,GlbMesh mesh,int width,int height,double bob){
        java.util.List<Face> faces=new ArrayList<>();
        for(GlbMesh.Triangle t:mesh.triangles()){
            float[][] vertices={{t.ax(),t.ay(),t.az()},{t.bx(),t.by(),t.bz()},{t.cx(),t.cy(),t.cz()}};
            int[] x=new int[3],y=new int[3];
            double avg=0;
            for(int i=0;i<3;i++){
                double a=vertices[i][0],b=vertices[i][1],c=vertices[i][2];
                // Camera 0.72 units behind the grip, slightly above and to the right.
                double perspective=Math.max(.35,1.10-c*.35);
                x[i]=(int)Math.round(width*.5+18+a*145/perspective+c*7);
                y[i]=(int)Math.round(height+6+bob-(b+.10)*122/perspective);
                avg+=perspective;
            }
            faces.add(new Face(x,y,t.rgb(),avg/3));
        }
        faces.sort((a,b)->Double.compare(b.depth(),a.depth()));
        for(Face f:faces){
            g.setColor(new Color(f.color()));
            g.fillPolygon(f.x(),f.y(),3);
        }
    }
}
