package veil;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

/** Actual shipped Swing renderer captures: city center, camera pitch and exterior approaches. */
public final class CityVisualRegressionTest {
    private record View(String name,double x,double z,double heading,double pitch){}
    private static final View[] VIEWS={
        new View("01-spawn-street",1.5,1.5,.2,0),
        new View("02-central-road",61.5,61.5,1.10,0),
        new View("03-look-up",61.5,61.5,1.10,.75),
        new View("04-look-down",61.5,61.5,1.10,-.75),
        new View("05-west-city-edge",3,66,Math.PI,0),
        new View("06-outside-facing-city",-18,66,0,-.15),
        new View("07-outside-facing-wilds",-18,66,Math.PI,-.1),
        new View("08-southern-approach",66,-18,Math.PI/2,-.1)
    };
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
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
                Field x=MaleficVeil.class.getDeclaredField("px");
                Field z=MaleficVeil.class.getDeclaredField("py");
                Field heading=MaleficVeil.class.getDeclaredField("angle");
                Field pitch=MaleficVeil.class.getDeclaredField("cameraPitch");
                for(Field f:new Field[]{x,z,heading,pitch})f.setAccessible(true);
                panel.setSize(960,600);
                Map<String,Integer> signatures=new HashMap<>();
                for(View view:VIEWS){
                    x.setDouble(panel,view.x());
                    z.setDouble(panel,view.z());
                    heading.setDouble(panel,view.heading());
                    pitch.setDouble(panel,view.pitch());
                    BufferedImage image=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);
                    Graphics2D g=image.createGraphics();
                    panel.paint(g);
                    g.dispose();
                    Set<Integer> colors=new HashSet<>();
                    int signature=0;
                    for(int row=20;row<530;row+=9)for(int col=12;col<950;col+=9){
                        int color=image.getRGB(col,row);
                        colors.add(color);
                        signature=31*signature+color;
                    }
                    check(colors.size()>16,"Flat or empty scene: "+view.name()+" colors="+colors.size());
                    File file=new File("build/test-visuals/city-regression-"+view.name()+".png");
                    file.getParentFile().mkdirs();
                    check(ImageIO.write(image,"png",file)&&file.length()>5500,
                        "Missing rendered PNG "+view.name());
                    signatures.put(view.name(),signature);
                    System.out.println("CITY_VISUAL_CAPTURE pose="+view.name()+
                        " colors="+colors.size()+" bytes="+file.length()+" pitch="+view.pitch());
                }
                check(!signatures.get("02-central-road").equals(signatures.get("03-look-up")),
                    "Pitch up does not move the horizon or source geometry");
                check(!signatures.get("03-look-up").equals(signatures.get("04-look-down")),
                    "Pitch down does not move the horizon or source geometry");
                System.out.println("PASS CityVisualRegressionTest frames="+VIEWS.length);
            }catch(Throwable t){error[0]=t;}
        });
        if(error[0]!=null)throw new AssertionError("Real Swing city capture failed",error[0]);
    }
}
