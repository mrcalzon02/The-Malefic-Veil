package veil;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Beckenburg-derived centre-ray interaction focus for the Malefic Veil's
 * horizontal PSX raycast view. The target selected for the prompt MUST be
 * independently verified on the host when E is pressed.
 */
public final class InteractionFocus {
    public enum Kind { PICKUP, VEHICLE, CARGO_YARD, CARGO_DROP, RESOURCE }
    public record Candidate(Kind kind,int id,String name,double x,double y,
                            double radius,double maxReach) {}
    public record Target(Kind kind,int id,String name,double distance) {}
    @FunctionalInterface public interface Sight {
        boolean clear(double ax,double ay,double bx,double by);
    }
    public static String pickupName(int kind){
        return switch(kind){
            case 0 -> "HEALTH";
            case 1 -> "SANITY";
            case 2 -> "PISTOL AMMO";
            case 3 -> "SHOTGUN SHELLS";
            case 4 -> "CASH";
            case 5 -> "SUPPLIES";
            case 6 -> "MEDKIT";
            default -> "UNKNOWN";
        };
    }
    private InteractionFocus(){}
    /** Closest physical aim intersection, not a nearest-distance auto selection. */
    public static Target pick(double x,double y,double heading,
                              Collection<Candidate> candidates,Sight sight){
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(heading)||
           candidates==null||sight==null)return null;
        double dx=Math.cos(heading),dy=Math.sin(heading);
        Target winner=null;
        double best=Double.POSITIVE_INFINITY;
        for(Candidate c:candidates){
            if(c==null||c.kind()==null||c.name()==null||
               !Double.isFinite(c.x())||!Double.isFinite(c.y())||
               !Double.isFinite(c.radius())||!Double.isFinite(c.maxReach())||
               c.radius()<=0||c.radius()>2||c.maxReach()<c.radius()||c.maxReach()>6)
                continue;
            double ox=c.x()-x,oy=c.y()-y,range=Math.hypot(ox,oy);
            if(range>c.maxReach())continue;
            double forward=ox*dx+oy*dy;
            double lateral=Math.abs(ox*dy-oy*dx);
            if(range>c.radius() && (forward<=0||lateral>c.radius()))continue;
            double t=range<=c.radius()?0:
                Math.max(0,forward-Math.sqrt(Math.max(0,c.radius()*c.radius()-lateral*lateral)));
            if(t>c.maxReach()||t>=best)continue;
            if(!sight.clear(x,y,c.x(),c.y()))continue;
            winner=new Target(c.kind(),c.id(),c.name(),t);best=t;
        }
        return winner;
    }
    public static void draw(Graphics2D graphics,int width,int height,Target target){
        if(target==null||graphics==null)return;
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            // Port of Beckenburg's compact hand-glyph + E keycap, rather than
            // a sprawling modal panel or a dependency on emoji icon fonts.
            int x=width/2+12,y=height/2+19;
            g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,10));
            String label=(target.kind()==Kind.PICKUP?"GRAB ":
                target.kind()==Kind.RESOURCE?"HARVEST ":"USE ")+target.name();
            int w=g.getFontMetrics().stringWidth(label)+43;
            if(x+w>width-3)x=Math.max(3,width-w-3);
            g.setColor(new Color(8,13,17,224));
            g.fillRoundRect(x-5,y-13,w,27,7,7);
            g.setColor(new Color(239,227,199));
            g.setStroke(new BasicStroke(1.4f));
            g.drawRoundRect(x+2,y-7,3,12,2,2);
            g.drawRoundRect(x+6,y-6,3,11,2,2);
            g.drawRoundRect(x+10,y-4,3,9,2,2);
            g.drawArc(x-1,y-2,17,12,180,180);
            g.drawRoundRect(x+17,y-9,14,18,3,3);
            g.drawString("E",x+21,y+4);
            g.drawString(label,x+35,y+4);
        }finally{g.dispose();}
    }
}
