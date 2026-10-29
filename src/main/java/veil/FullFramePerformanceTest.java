package veil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Measures the REAL desktop game panel (raycaster, Kenney props, gameplay meshes,
 * weapon/HUD and nearest-neighbor upscale), not just an isolated asset renderer.
 * Never asserts an FPS target against unpredictable CI virtual hardware.
 */
public final class FullFramePerformanceTest {
    private static void check(boolean good,String why){if(!good)throw new AssertionError(why);}
    private static double ms(long nanos){return nanos/1_000_000.0;}
    private static double pct(java.util.List<Long> samples,double rate){
        var sorted=new ArrayList<>(samples);Collections.sort(sorted);
        return ms(sorted.get(Math.min(sorted.size()-1,
            Math.max(0,(int)Math.ceil(rate*sorted.size())-1))));
    }
    public static void main(String[] args)throws Exception {
        System.setProperty("java.awt.headless","true");
        final Throwable[] error={null};
        SwingUtilities.invokeAndWait(()->{
            try {
                Constructor<MaleficVeil> ctor=MaleficVeil.class.getDeclaredConstructor(
                    JFrame.class,long.class,int.class,NetLink.class);
                ctor.setAccessible(true);
                MaleficVeil game=ctor.newInstance(null,20261020L,0,null);
                Field timerField=MaleficVeil.class.getDeclaredField("timer");
                timerField.setAccessible(true);
                ((javax.swing.Timer)timerField.get(game)).stop();
                Field angleField=MaleficVeil.class.getDeclaredField("angle");
                angleField.setAccessible(true);
                Field pxField=MaleficVeil.class.getDeclaredField("px");
                Field pyField=MaleficVeil.class.getDeclaredField("py");
                pxField.setAccessible(true);pyField.setAccessible(true);
                game.setSize(960,600);
                BufferedImage canvas=new BufferedImage(960,600,BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics=canvas.createGraphics();
                java.util.List<Long> samples=new ArrayList<>();
                for(int i=0;i<15;i++){
                    int spawn=i%3;
                    double[][] positions={{1.5,1.5},{61.5,61.5},{121.5,121.5}};
                    pxField.setDouble(game,positions[spawn][0]);
                    pyField.setDouble(game,positions[spawn][1]);
                    angleField.setDouble(game,(i%10)*Math.PI/5.0);
                    long started=System.nanoTime();
                    game.paint(graphics);
                    long elapsed=System.nanoTime()-started;
                    if(i>=3)samples.add(elapsed);
                }
                graphics.dispose();
                double p50=pct(samples,.5),p95=pct(samples,.95);
                check(Double.isFinite(p50)&&Double.isFinite(p95)&&p95>0,"No valid frames recorded");
                Path out=Path.of("build","test-visuals","full-frame-performance.csv");
                Files.createDirectories(out.getParent());
                String csv="metric,value,unit\n"+
                    "full_frame_p50,"+p50+",ms\n"+
                    "full_frame_p95,"+p95+",ms\n"+
                    "full_frame_p95_fps_equivalent,"+(1000.0/p95)+",FPS\n"+
                    "display_width,960,pixels\n"+
                    "display_height,600,pixels\n"+
                    "benchmark_frames,"+samples.size()+",frames\n";
                Files.writeString(out,csv,StandardCharsets.UTF_8);
                System.out.println("PASS FullFramePerformanceTest\n"+csv);
            }catch(Throwable problem){error[0]=problem;}
        });
        if(error[0]!=null)throw new AssertionError("Real panel frame benchmark failed",error[0]);
    }
}
