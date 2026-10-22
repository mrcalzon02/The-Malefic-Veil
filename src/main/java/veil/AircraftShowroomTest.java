package veil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/** Headless screenshot acceptance of actual Java game GLB triangle renderer. */
public final class AircraftShowroomTest {
    private static void check(boolean ok,String reason){
        if(!ok)throw new AssertionError(reason);
    }
    public static void main(String[] args)throws Exception{
        var models=new GameplayMeshes().meshes();
        check(AircraftShowroom.count()==9,"All nine faction model choices");
        Set<Integer> checksums=new HashSet<>();
        for(int model=0;model<AircraftShowroom.count();model++){
            BufferedImage image=new BufferedImage(320,200,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();
            AircraftShowroom.render(g,models,model,-Math.PI/2+.25);
            g.dispose();
            int unique=0,notStage=0;
            Set<Integer> colors=new HashSet<>();
            int[] pix=image.getRGB(0,0,320,200,null,0,320);
            for(int y=36;y<172;y+=2)for(int x=16;x<304;x+=2){
                int rgb=image.getRGB(x,y);colors.add(rgb);
                if(rgb!=0xff31393e&&rgb!=0xff10171e)notStage++;
            }
            unique=colors.size();
            check(unique>15,"Airframe model did not produce a real colored screen image: "+
                AircraftShowroom.id(model)+" colors "+unique);
            check(notStage>200,"Aircraft visually missing in native PSX viewport "+
                AircraftShowroom.id(model));
            check(checksums.add(Arrays.hashCode(pix)),
                "Two distinct aircraft render exactly the same image");
        }
        System.out.println("PASS AircraftShowroomTest: nine distinct actual PropRenderer GLBs, 320x200 viewport");
    }
}
