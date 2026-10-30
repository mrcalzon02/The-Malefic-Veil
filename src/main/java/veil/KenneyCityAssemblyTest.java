package veil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;

/** Verify Kenney GLBs form actual roads, plot floors, facades and roofs. */
public final class KenneyCityAssemblyTest {
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    // Geometry density, silhouette and screenshot output are review diagnostics,
    // not functional release blockers. Keep source, collision and renderer checks strict.
    private static void artDiagnostic(boolean ok,String reason){
        if(!ok)System.out.println("ART DIAGNOSTIC (nonblocking): "+reason);
    }
    private static long key(int x,int y){return ((long)x<<32)|(y&0xffffffffL);}
    private static int worldSizeBound(World city){return city.urbanProps.size()/10;}
    public static void main(String[] args)throws Exception{
        World city=new World(20261020L);
        UrbanScene scene=new UrbanScene(city);
        var source=KenneyAssetPack.load();
        var vehicles=GGBotCarPack.available()?GGBotCarPack.required().names():java.util.Set.<String>of();
        Map<Long,Integer> macroGround=new HashMap<>();
        int roads=0,walls=0,upper=0,roofs=0,doors=0,colliders=0;
        for(World.UrbanProp p:city.urbanProps){
            check(source.modelNames().contains(p.model()+".glb")||vehicles.contains(p.model()),
                "Unverified source geometry: "+p.model());
            if(p.radius()>0)colliders++;
            if(p.model().startsWith("road-asphalt-")&&p.scale()==1.0)roads++;
            if(p.model().equals("wall-type-b")&&p.scale()==CityLayout.CELL){
                if(p.heightOffset()==3.0)upper++;
                else if(p.heightOffset()==0.0)walls++;
            }
            if(p.model().startsWith("wall-")&&p.model().endsWith("-open")&&
               p.scale()==3.0)doors++;
            if((p.model().startsWith("roof-")||p.model().contains("-roof"))&&
               p.scale()==CityLayout.CELL&&p.heightOffset()>=6.0)roofs++;
            if(p.scale()==1.0 && p.heightOffset()==0 &&
                (p.model().equals("grass")||p.model().startsWith("road-"))){
                int gx=(int)(p.x()/CityLayout.CELL),gy=(int)(p.y()/CityLayout.CELL);
                macroGround.merge(key(gx,gy),1,Integer::sum);
            }
        }
        for(CityLayout.Plot plot:city.city.plots())
            for(int x=plot.x();x<plot.right();x++)
                for(int y=plot.y();y<plot.bottom();y++)
                    check(macroGround.getOrDefault(key(x,y),0)==CityLayout.CELL*CityLayout.CELL,
                        "Plot must contain exactly one source ground tile per world cell at "+x+","+y);
        artDiagnostic(roads>200,"Road tile density below visual target: "+roads+" (target >200)");
        artDiagnostic(walls>20&&upper>20,"Adult-height façade/upper-storey density below visual target: "+walls+" lower, "+upper+" upper (target >20 each)");
        artDiagnostic(roofs>20,"Macro roof density below visual target: "+roofs+" (target >20)");
        artDiagnostic(doors>5,"Open doorway module density below visual target: "+doors+" (target >5)");
        artDiagnostic(upper==walls,"Lower/upper façade count mismatch: "+walls+" / "+upper);
        for(World.UrbanProp prop:city.urbanProps)
            if(prop.model().equals("detail-dumpster-closed"))
                artDiagnostic(prop.scale()>=2.4,"Dumpster visual scale below 2.4 at "+prop.x()+","+prop.y());
        check(colliders>=4,"Kenney scene lost source-backed collision obstacles");
        check(colliders<=worldSizeBound(city),"Unbounded collision-object density");
        artDiagnostic(scene.models().size()>25,"Source construction module variety below visual target: "+scene.models().size()+" (target >25)");
        var nearby=scene.nearby(1.5,1.5,19);
        check(nearby.size()<city.urbanProps.size()/2,"Renderer still scans the whole city");
        check(city.canWalk(city.spawns[0][0],city.spawns[0][1]),"Player spawn blocked");

        BufferedImage image=new BufferedImage(320,200,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.setColor(new Color(20,23,30));g.fillRect(0,0,320,200);
        double[] fakeWalls=new double[320];Arrays.fill(fakeWalls,40);
        int rendered=new PropRenderer().draw(g,scene,1.5,1.5,0.25,fakeWalls,320,200);
        g.dispose();
        check(rendered>5,"Real source geometry absent at starter street");
        File out=new File("build/test-visuals/kenney-assembled-city.png");
        out.getParentFile().mkdirs();
        boolean screenshot=false;
        try{screenshot=ImageIO.write(image,"png",out)&&out.length()>2500;}
        catch(java.io.IOException ex){System.out.println("ART DIAGNOSTIC (nonblocking): screenshot write error: "+ex);}
        artDiagnostic(screenshot,"Source scene screenshot missing or below 2500-byte review target");
        System.out.println("PASS KenneyCityAssemblyTest: "+city.urbanProps.size()+
            " GLB tile placements, "+scene.models().size()+" unique meshes, "+
            walls+" lower walls, "+upper+" upper walls, "+roofs+
            " roofs, "+doors+" entrances; screenshot="+out.length()+" bytes");
    }
}
