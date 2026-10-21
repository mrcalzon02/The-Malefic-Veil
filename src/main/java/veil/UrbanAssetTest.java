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
        check(city.models().size()>25,"seed must use many unique original models");
        int triangles=0;
        for(var asset:city.models().entrySet()){
            GlbMesh model=asset.getValue();
            check(model.triangleCount()>2,asset.getKey()+" did not parse geometry");
            check(model.bounds()[1]>=0,asset.getKey()+" invalid floor");
            triangles+=model.triangleCount();
        }
        check(triangles>1200,"seed must parse substantial original source geometry");
        check(city.instances().size()>90,"seeded city lacks meaningful prop density");
        KenneyAssetPack pack=KenneyAssetPack.load();
        check(pack.modelNames().size()==124,"all source models included");
        check(pack.textureNames().size()==22,"all original palettes bundled");
        for(String id:pack.modelNames()){
            GlbMesh parsed=GlbMesh.read(pack.model(id),pack::texture);
            check(parsed.triangleCount()>0,id+" has no triangles");
        }
        for(String id:java.util.List.of("truck-green","wall-a-flat-window","road-asphalt-damaged")){
            GlbMesh textured=GlbMesh.asset(id);
            long colors=textured.triangles().stream().map(GlbMesh.Triangle::rgb).distinct().count();
            check(colors>10,"Source UV texture palette flattened for "+id+": "+colors+" colors");
        }
        // The ZIP must load as a bundled classpath resource in the runnable JAR as well.
        check(pack.model("road-asphalt-straight.glb").length>1000,"source archive truncated");
        check(!world.canWalk(5.2,1.5),"actual barricade should be solid to players");
        check(!world.lineClear(4.4,1.5,6.4,1.5),"barricade should occlude projectile ray");
        check(world.canWalk(world.spawns[0][0],world.spawns[0][1]),"spawn blocked by decorations");
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
