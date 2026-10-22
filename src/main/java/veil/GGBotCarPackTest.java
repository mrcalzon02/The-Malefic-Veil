package veil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Full asset gate: known original archive, Blender/MTL/PNG agreement,
 * all 39 original-UV car skins, real world placement, software rasterization,
 * and retention of operational Kenney cargo trucks.
 */
public final class GGBotCarPackTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        GGBotCarPack library=GGBotCarPack.required();
        check(library.names().size()==39,"39 approved source car appearances required");
        for(String name:java.util.List.of("ggbot-01-car","ggbot-02-car2",
                "ggbot-05-car5_police","ggbot-05-car5_taxi",
                "ggbot-06-car6","ggbot-08-car8_mail"))
            check(library.names().contains(name),"Missing intended variant "+name);
        int faceTotal=0;
        for(String id:library.names()){
            GlbMesh mesh=library.model(id);
            check(mesh.triangleCount()>=300,"Source geometry absent from "+id);
            check(mesh.surfaces().size()==mesh.triangleCount(),"UV surface face count mismatch "+id);
            for(GlbMesh.Surface face:mesh.surfaces())
                check(id.equals(face.texture())&&Float.isFinite(face.au())&&
                    Float.isFinite(face.av()),"Wrong material slot or broken UV "+id);
            BufferedImage atlas=mesh.textureImage(id);
            check(atlas!=null&&atlas.getWidth()==128&&atlas.getHeight()==128,
                "Atlas missing or not original 128px size "+id);
            faceTotal+=mesh.triangleCount();
        }
        int cityCars=0,truckCount=0;
        for(long seed:new long[]{20261020L,11L,93L}){
            World world=new World(seed);
            UrbanScene city=new UrbanScene(world);
            int foundCars=0,foundTrucks=0;
            for(World.UrbanProp prop:world.urbanProps){
                if(prop.model().startsWith("ggbot-")){
                    foundCars++;
                    check(city.models().containsKey(prop.model()),
                        "World generated an invisible civilian car");
                    check(prop.radius()>0,"Car lacks host collision footprint");
                }
                if(prop.model().startsWith("truck-"))foundTrucks++;
            }
            check(foundCars>=1,"No GGBot cars in generated city "+seed);
            check(foundTrucks>=1,"Kenney depot trucks were overwritten "+seed);
            check(world.canWalk(world.spawns[0][0],world.spawns[0][1]),
                "Cars block the player spawn");
            cityCars+=foundCars;truckCount+=foundTrucks;
            Vehicles vehicles=new Vehicles(world);
            for(Vehicles.State car:vehicles.states()){
                Vehicles.Load load=vehicles.cargo(car.id());
                check(load.model().startsWith("truck-"),
                    "GGBot cosmetic cars replaced a functional cargo truck");
            }
        }

        GlbMesh mesh=library.model("ggbot-05-car5_taxi");
        BufferedImage output=new BufferedImage(320,200,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=output.createGraphics();
        g.setColor(Color.BLACK);g.fillRect(0,0,320,200);
        double[] walls=new double[320];Arrays.fill(walls,100);
        var placed=java.util.List.of(new UrbanScene.ModelInstance(
            "ggbot-05-car5_taxi",4,0,0,.52));
        int rendered=new PropRenderer().draw(g,Map.of("ggbot-05-car5_taxi",mesh),
            placed,0,0,0,walls,320,200);
        g.dispose();
        Set<Integer> colors=new HashSet<>();
        int pixels=0;
        for(int y=0;y<200;y++)for(int x=0;x<320;x++){
            int rgb=output.getRGB(x,y)&0xffffff;
            if(rgb!=0){pixels++;colors.add(rgb);}
        }
        check(rendered==1&&pixels>30&&colors.size()>10,
            "Original UV-atlas car rendering failed: pixels="+pixels+
                ", color count="+colors.size());
        System.out.println("PASS GGBotCarPackTest appearances="+library.names().size()+
            " totalFaces="+faceTotal+" seededCarInstances="+cityCars+
            " KenneyTrucks="+truckCount+" coloredPixels="+pixels+
            " distinctColors="+colors.size());
    }
}
