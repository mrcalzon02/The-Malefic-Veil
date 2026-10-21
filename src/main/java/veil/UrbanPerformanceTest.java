package veil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Real Java2D dense-city benchmark and correctness gate.
 * Timings are telemetry, never brittle CI hardware-dependent FPS assertions.
 */
public final class UrbanPerformanceTest {
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    private static double millis(long nanos){return nanos/1_000_000.0;}
    private static double percentile(java.util.List<Long> samples,double p){
        var copy=new ArrayList<>(samples);Collections.sort(copy);
        int at=Math.max(0,Math.min(copy.size()-1,(int)Math.ceil(p*copy.size())-1));
        return millis(copy.get(at));
    }
    public static void main(String[] args)throws Exception {
        long start=System.nanoTime();
        World world=new World(20261020L);
        double generationMs=millis(System.nanoTime()-start);
        // Identical seeds must build exactly the same real Kenney tile geometry.
        World duplicate=new World(20261020L);
        check(world.urbanProps.equals(duplicate.urbanProps),"City tile geometry differs across identical seeds");
        check(world.urbanProps.size()>1000,"Modular Kenney construction unexpectedly sparse");
        long loadStart=System.nanoTime();
        UrbanScene scene=new UrbanScene(world);
        double sceneLoadMs=millis(System.nanoTime()-loadStart);
        UrbanScene second=new UrbanScene(duplicate);
        check(scene.batches().instanceCount()==world.urbanProps.size(),"Instance buffer incomplete");
        check(scene.batches().uniqueGeometryCount()==scene.models().size(),"Shared geometry count mismatch");
        for(String id:scene.models().keySet())
            check(scene.models().get(id)==second.models().get(id),
                "Parsed GLB not reused across scenes: "+id);
        long expandedTriangles=scene.batches().naiveTriangleCount();
        long sourceTriangles=scene.batches().sourceTriangleCount();
        check(expandedTriangles>sourceTriangles*5,"Source geometry is not significantly instanced");

        Random random=new Random(91317);
        java.util.List<Long> queries=new ArrayList<>();
        int maxNearby=0;
        for(int n=0;n<80;n++){
            double px=random.nextInt(World.SIZE)+.5,py=random.nextInt(World.SIZE)+.5;
            long at=System.nanoTime();
            var nearby=scene.nearby(px,py,26);
            queries.add(System.nanoTime()-at);
            maxNearby=Math.max(maxNearby,nearby.size());
            for(var item:nearby)check(Math.hypot(item.x()-px,item.y()-py)<=26.00001,
                "Visibility query includes an out-of-range instance");
            if(n<5){
                // Compare accelerated query to full list so culling cannot
                // accidentally remove legitimate nearby world geometry.
                long expected=scene.instances().stream().filter(item->
                    Math.hypot(item.x()-px,item.y()-py)<=26.0).count();
                check(expected==nearby.size(),"Visibility grid missing nearby Kenney instance");
            }
        }
        check(maxNearby<scene.instances().size()/2,
            "Visual candidate culling still processes most of the world");

        BufferedImage frame=new BufferedImage(320,200,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=frame.createGraphics();
        double[] depth=new double[320];Arrays.fill(depth,40.0);
        PropRenderer renderer=new PropRenderer();
        java.util.List<Long> renderTimes=new ArrayList<>();
        for(int n=0;n<9;n++){
            g.setClip(null);
            g.setColor(Color.BLACK);g.fillRect(0,0,320,200);
            double heading=n*Math.PI/5;
            long at=System.nanoTime();
            int drawn=renderer.draw(g,scene,world.spawns[0][0],world.spawns[0][1],
                heading,depth,320,200);
            long nanos=System.nanoTime()-at;
            if(n>=2)renderTimes.add(nanos); // discard JIT/cache warmups
            check(drawn<=80,"Renderer violated configured model count budget");
        }
        g.dispose();

        long physicsStart=System.nanoTime();
        for(int i=0;i<10000;i++){
            double x=(i*17%World.SIZE)+.45,y=(i*37%World.SIZE)+.45;
            world.canWalk(x,y);
            if(i%20==0)world.lineClear(x,y,12.5,13.5);
        }
        double physicsMs=millis(System.nanoTime()-physicsStart);

        String csv="metric,value,unit\n"+
            "generated_world_tiles,"+world.urbanProps.size()+",instances\n"+
            "unique_meshes,"+scene.models().size()+",meshes\n"+
            "triangles_if_individually_duplicated,"+expandedTriangles+",triangles\n"+
            "triangles_stored_once,"+sourceTriangles+",triangles\n"+
            "max_visibility_candidates,"+maxNearby+",instances\n"+
            "world_generation,"+generationMs+",ms\n"+
            "scene_import,"+sceneLoadMs+",ms\n"+
            "spatial_query_p50,"+percentile(queries,.50)+",ms\n"+
            "spatial_query_p95,"+percentile(queries,.95)+",ms\n"+
            "software_render_p50,"+percentile(renderTimes,.50)+",ms\n"+
            "software_render_p95,"+percentile(renderTimes,.95)+",ms\n"+
            "movement_and_los_10k,"+physicsMs+",ms\n";
        Path report=Path.of("build","test-visuals","urban-performance.csv");
        Files.createDirectories(report.getParent());
        Files.writeString(report,csv,StandardCharsets.UTF_8);
        System.out.println("PASS UrbanPerformanceTest\n"+csv);
    }
}
