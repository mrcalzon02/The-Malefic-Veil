package veil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/** Import and real triangle-render gate using five exact CC0 Drive meshes. */
public final class UrbanAssetTest {
    private static void check(boolean pass,String message){if(!pass)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        World world=new World(20261020L);
        UrbanScene city=new UrbanScene(world);
        check(city.models().size()==5,"all five curated models loaded");
        int triangles=0;
        for(var asset:city.models().entrySet()){
            GlbMesh model=asset.getValue();
            check(model.triangleCount()>2,asset.getKey()+" did not parse geometry");
            check(model.bounds()[1]>=0,asset.getKey()+" invalid floor");
            triangles+=model.triangleCount();
        }
        check(triangles>150,"incomplete source geometry");
        check(city.instances().size()>35,"seeded city lacks meaningful prop density");
        double[] walls=new double[320];Arrays.fill(walls,40);
        BufferedImage image=new BufferedImage(320,200,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.setColor(Color.BLACK);g.fillRect(0,0,320,200);
        int nearby=new PropRenderer().draw(g,city,1.5,1.5,0.2,walls,320,200);
        g.dispose();
        check(nearby>=3,"default spawn missing visible geometry");
        Set<Integer> pixels=new HashSet<>();
        int painted=0;
        for(int y=0;y<200;y++)for(int x=0;x<320;x++){
            int pixel=image.getRGB(x,y);
            if((pixel&0xffffff)!=0){painted++;pixels.add(pixel);}
        }
        check(painted>40,"source triangles did not reach actual framebuffer");
        check(pixels.size()>2,"no distinct source materials in screenshot");
        try{GlbMesh.read(new byte[28]);throw new AssertionError("malformed GLB accepted");}
        catch(java.io.IOException expected){}
        System.out.println("PASS UrbanAssetTest: "+triangles+" original GLB triangles, "+city.instances().size()+
            " deterministic placements, "+painted+" visible pixels, "+pixels.size()+" shades");
    }
}
