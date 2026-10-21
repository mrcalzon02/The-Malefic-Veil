package veil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;

/** Verify Kenney GLBs form actual roads, plot floors, facades and roofs. */
public final class KenneyCityAssemblyTest {
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    private static long key(int x,int y){return ((long)x<<32)|(y&0xffffffffL);}
    private static int worldSizeBound(World city){return city.urbanProps.size()/10;}
    public static void main(String[] args)throws Exception{
        World city=new World(20261020L);
        UrbanScene scene=new UrbanScene(city);
        var source=KenneyAssetPack.load();
        Map<Long,Integer> macroGround=new HashMap<>();
        int roads=0,walls=0,upper=0,roofs=0,doors=0,colliders=0;
        for(World.UrbanProp p:city.urbanProps){
            check(source.modelNames().contains(p.model()+".glb"),"Non-source geometry: "+p.model());
            if(p.radius()>0)colliders++;
            if(p.model().startsWith("road-asphalt-")&&p.scale()==1.0)roads++;
            if(p.model().startsWith("wall-")&&p.scale()==1.0){
                if(p.heightOffset()==1.0)upper++;
                if(p.heightOffset()==0.0)walls++;
                if(p.model().endsWith("-open"))doors++;
            }
            if((p.model().startsWith("roof-")||p.model().contains("-roof"))&&p.heightOffset()>1.5)roofs++;
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
        check(roads>200,"Road network does not use real one-unit modular road pieces");
        check(walls>200&&upper>200,"Building shell not assembled from wall tiles");
        check(roofs>80,"Building roofs not represented by modular roofs");
        check(doors>5,"Entrances not represented by open wall modules");
        check(colliders>=4,"Kenney scene lost source-backed collision obstacles");
        check(colliders<=worldSizeBound(city),"Unbounded collision-object density");
        check(scene.models().size()>25,"Too few distinct source construction modules");
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
        check(ImageIO.write(image,"png",out)&&out.length()>2500,"Source scene screenshot failed");
        System.out.println("PASS KenneyCityAssemblyTest: "+city.urbanProps.size()+
            " GLB tile placements, "+scene.models().size()+" unique meshes, "+
            walls+" lower walls, "+upper+" upper walls, "+roofs+
            " roofs, "+doors+" entrances; screenshot="+out.length()+" bytes");
    }
}
