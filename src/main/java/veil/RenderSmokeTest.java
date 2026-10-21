package veil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.lang.reflect.*;
import javax.imageio.ImageIO;

/**
 * Headless real Swing renderer smoke test: creates and paints the actual game panel.
 * Writes a screenshot artifact suitable for human build review.
 */
public final class RenderSmokeTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        System.setProperty("java.awt.headless","true");
        final Throwable[] error={null};
        SwingUtilities.invokeAndWait(()->{
            try{
                Constructor<MaleficVeil> c=MaleficVeil.class.getDeclaredConstructor(
                    JFrame.class,long.class,int.class,NetLink.class);
                c.setAccessible(true);
                MaleficVeil panel=c.newInstance(null,20261020L,2,null);
                Field timer=MaleficVeil.class.getDeclaredField("timer");
                timer.setAccessible(true);
                ((javax.swing.Timer)timer.get(panel)).stop();
                panel.setSize(960,600);
                BufferedImage image=new BufferedImage(960,600,BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=image.createGraphics();
                panel.paint(g);g.dispose();
                java.util.Set<Integer> colors=new java.util.HashSet<>();
                for(int y=20;y<500;y+=11)for(int x=20;x<940;x+=11)colors.add(image.getRGB(x,y));
                check(colors.size()>12,"renderer produced insufficient visual variation: "+colors.size());
                File out=new File("build/test-visuals/arena-screenshot.png");
                out.getParentFile().mkdirs();
                check(ImageIO.write(image,"png",out),"failed to write PNG");
                check(out.length()>4000,"render artifact unexpectedly empty");
                System.out.println("PASS RenderSmokeTest: "+colors.size()+" visible colors, "+out.length()+" PNG bytes");
            }catch(Throwable t){error[0]=t;}
        });
        if(error[0]!=null)throw new AssertionError("Real Swing panel render failed",error[0]);
    }
}
