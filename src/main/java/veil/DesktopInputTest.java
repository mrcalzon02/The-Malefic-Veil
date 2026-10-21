package veil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashSet;

/**
 * Runs the ACTUAL visible Swing game inside a virtual desktop with Robot key/mouse input.
 * A headless bitmap-only test cannot substitute for this event-dispatch check.
 */
public final class DesktopInputTest {
    private static final JFrame[] window=new JFrame[1];
    private static final MaleficVeil[] game=new MaleficVeil[1];
    private static void ensure(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static double position()throws Exception{
        final double[] x=new double[1];
        SwingUtilities.invokeAndWait(()->{
            try{Field f=MaleficVeil.class.getDeclaredField("px");f.setAccessible(true);x[0]=f.getDouble(game[0]);}
            catch(Exception e){throw new RuntimeException(e);}
        });
        return x[0];
    }
    private static int ammunition()throws Exception{
        final int[] clip=new int[1];
        SwingUtilities.invokeAndWait(()->{
            try{Field f=MaleficVeil.class.getDeclaredField("pistolClip");f.setAccessible(true);clip[0]=f.getInt(game[0]);}
            catch(Exception e){throw new RuntimeException(e);}
        });
        return clip[0];
    }
    public static void main(String[] args)throws Exception{
        ensure(!GraphicsEnvironment.isHeadless(),"Virtual/physical desktop required");
        SwingUtilities.invokeAndWait(()->{
            JFrame frame=new JFrame("Malefic Veil Desktop Test");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            try{
                var constructor=MaleficVeil.class.getDeclaredConstructor(JFrame.class,long.class,int.class,NetLink.class);
                constructor.setAccessible(true);
                MaleficVeil panel=constructor.newInstance(frame,20261020L,0,null);
                frame.setContentPane(panel);frame.setSize(960,640);
                frame.setLocation(25,25);frame.setVisible(true);panel.requestFocusInWindow();
                window[0]=frame;game[0]=panel;
            }catch(Exception ex){throw new RuntimeException(ex);}
        });
        Robot robot=new Robot();
        robot.setAutoDelay(60);
        robot.waitForIdle();
        Thread.sleep(200);
        try{
            double initial=position();
            robot.keyPress(KeyEvent.VK_W);
            Thread.sleep(360);
            robot.keyRelease(KeyEvent.VK_W);
            robot.waitForIdle();
            ensure(position()>initial+.12,"Real W key failed to move character");
            int first=ammunition();
            Point p=new Point(460,350);
            SwingUtilities.convertPointToScreen(p,game[0]);
            robot.mouseMove(p.x,p.y);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            Thread.sleep(180);
            robot.waitForIdle();
            ensure(ammunition()<first,"Real mouse click failed to shoot");
            Point where=window[0].getLocationOnScreen();
            BufferedImage image=robot.createScreenCapture(new Rectangle(where.x,where.y,window[0].getWidth(),window[0].getHeight()));
            File output=new File("build/test-visuals/desktop-input.png");output.getParentFile().mkdirs();
            ImageIO.write(image,"png",output);
            HashSet<Integer> pixels=new HashSet<>();
            for(int y=20;y<image.getHeight()-100;y+=11)
                for(int x=20;x<image.getWidth()-20;x+=11)pixels.add(image.getRGB(x,y));
            ensure(pixels.size()>18,"Desktop screenshot lacks scene detail");
            System.out.println("PASS DesktopInputTest: actual Swing Robot movement, real mouse firing, "+pixels.size()+" visible colors");
        }finally{
            SwingUtilities.invokeAndWait(()->{
                game[0].setVisible(false);
                window[0].dispose();
            });
        }
    }
}
