package veil;
import java.awt.*;
import java.util.List;

/** Readable prototype traces, electric arcs, projectiles, blast/puddle/smoke volumes. */
public final class ArsenalEffects {
    static Point project(double x,double y,double up,double px,double py,double a,int w,int h){double dx=x-px,dy=y-py,front=Math.cos(a)*dx+Math.sin(a)*dy,right=-Math.sin(a)*dx+Math.cos(a)*dy;if(front<.1)return null;return new Point((int)(w*.5+right/front*w*.746),(int)(h*.5-up/front*w*.746));}
    public static void draw(Graphics2D g,List<ArsenalSystems.Effect> list,World world,double px,double py,double a,int w,int h){
        for(var e:list){double distance=Math.hypot(e.x()-px,e.y()-py);if(distance>30||!world.lineClear(px,py,e.x(),e.y()))continue;
            boolean line=e.kind().equals("BEAM")||e.kind().equals("TRACE")||e.kind().equals("ARC");Point p=distance<.5?new Point(w/2,h/2):project(e.x(),e.y(),.2,px,py,a,w,h);if(p==null)continue;
            Color c=new Color(e.color());g.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),line?220:135));
            if(line){Point end=project(e.tx(),e.ty(),.2,px,py,a,w,h);if(end==null)continue;if(e.kind().equals("ARC")){int mx=(p.x+end.x)/2+((e.id()%7)-3)*2,my=(p.y+end.y)/2-4;g.drawLine(p.x,p.y,mx,my);g.drawLine(mx,my,end.x,end.y);}else g.drawLine(p.x,p.y,end.x,end.y);}
            else if(e.kind().equals("SMOKE")&&distance<e.size()){g.setColor(new Color(100,105,114,155));g.fillRect(0,0,w,h);}
            else{int r=(int)Math.max(2,Math.min(70,e.size()/Math.max(.5,distance)*w*.45));g.fillOval(p.x-r,p.y-r,r*2,r*2);}
        }
    }
}
