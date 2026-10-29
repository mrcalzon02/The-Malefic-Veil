package veil;

import java.awt.image.BufferedImage;
import java.awt.*;
import java.util.*;

/** Portable Beckenburg focus behavior: line aim, sight/occlusion, prompt and nearest-hit. */
public final class InteractionFocusTest {
    private static void check(boolean condition,String why){
        if(!condition)throw new AssertionError(why);
    }
    public static void main(String[] ignored){
        var objects=java.util.List.of(
            new InteractionFocus.Candidate(InteractionFocus.Kind.PICKUP,3,"Medical kit",
                1.0,0,.25,1.15),
            new InteractionFocus.Candidate(InteractionFocus.Kind.PICKUP,4,"Supplies",
                1.8,0,.32,2.0),
            new InteractionFocus.Candidate(InteractionFocus.Kind.RESOURCE,5,"Timber",
                1.6,.6,.18,2.05));
        var hit=InteractionFocus.pick(0,0,0,objects,(x,y,a,b)->true);
        check(hit!=null&&hit.id()==3,"front sight finds nearest actually hit object");
        check(InteractionFocus.pick(0,0,Math.PI,objects,(x,y,a,b)->true)==null,
            "object behind is not nearest-object auto selected");
        check(InteractionFocus.pick(0,0,Math.PI/2,objects,(x,y,a,b)->true)==null,
            "object beside player does not project a prompt");
        hit=InteractionFocus.pick(0,0,0,objects,(x,y,a,b)->a!=1.0);
        check(hit!=null&&hit.id()==4,"a blocked object is skipped for unobstructed target");
        check(InteractionFocus.pick(0,0,0,objects,(x,y,a,b)->false)==null,
            "a wall occludes interaction");
        check(InteractionFocus.pick(0,0,0,
            java.util.List.of(new InteractionFocus.Candidate(
                InteractionFocus.Kind.PICKUP,20,"Far",8,0,.3,1)),(a,b,c,d)->true)==null,
            "no long-range grab");
        check(InteractionFocus.pick(0,0,0,
            java.util.List.of(new InteractionFocus.Candidate(
                InteractionFocus.Kind.PICKUP,20,"Close",0,.04,.25,.63)),
            (a,b,c,d)->true)!=null,"overlapping small items remain interactable");
        BufferedImage screen=new BufferedImage(320,200,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=screen.createGraphics();
        g.setColor(Color.BLACK);g.fillRect(0,0,320,200);
        InteractionFocus.draw(g,320,200,new InteractionFocus.Target(
            InteractionFocus.Kind.RESOURCE,5,"Timber",1.3));
        g.dispose();
        int lit=0;
        for(int y=95;y<142;y++)for(int x=150;x<320;x++)
            if(screen.getRGB(x,y)!=Color.BLACK.getRGB())lit++;
        check(lit>90,"crosshair-adjacent hand E prompt must actually render");
        System.out.println("PASS InteractionFocusTest: geometry, front aim, occlusion, compact E prompt");
    }
}
