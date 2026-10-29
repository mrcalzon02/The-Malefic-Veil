package veil;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Actual in-game static-airframe inspection stage. Uses the very same GLB
 * instances, mesh catalog, and PropRenderer as the playable first-person view.
 * It provides model evidence, NOT a functional aircraft spawn or flight mode.
 */
public final class AircraftShowroom {
    public static final int WIDTH=320,HEIGHT=200;
    private static final int TOTAL=9;
    private static final PropRenderer RENDERER=new PropRenderer();
    private AircraftShowroom(){}

    public static int count(){return TOTAL;}
    public static String id(int index){
        return AirPowerMeshes.specs().get(Math.floorMod(index,TOTAL)).id();
    }
    public static String caption(int index){
        var spec=AirPowerMeshes.specs().get(Math.floorMod(index,TOTAL));
        return switch(spec.id()){
            case "unraveled_needlewing" -> "SCRAP RECON / PUSHER PROP";
            case "unraveled_harrier" -> "MILITARY / CIVILIAN HYBRID";
            case "unraveled_leviathan" -> "ARMORED SALVAGE FREIGHTER";
            case "penitent_shrike" -> "TRACTOR CAB / CHIPPER JET";
            case "penitent_censer" -> "FLYING THRESHER / HARVESTER";
            case "penitent_cathedral" -> "FLYING INDUSTRIAL ABATTOIR";
            case "begotten_gnat" -> "MANGY BAT / GUN-LEGS";
            case "begotten_mantis" -> "VULTURE-LION / FOUR CLAWS";
            case "begotten_behemoth" -> "SKINLESS WHALE / FOUR WINGS";
            default -> throw new IllegalStateException("Unregistered airframe");
        };
    }
    /**
     * Strict PSX pixel view with in-engine triangle projection, real imported
     * material RGB and correct model scaling from its actual GLB AABB.
     */
    public static void render(Graphics2D g,Map<String,GlbMesh> gameMeshes,
                              int selection,double rotation){
        Objects.requireNonNull(g);
        Objects.requireNonNull(gameMeshes);
        int index=Math.floorMod(selection,TOTAL);
        String modelId=id(index);
        GlbMesh mesh=gameMeshes.get(modelId);
        if(mesh==null)throw new IllegalStateException("Unloaded aircraft: "+modelId);
        float[] b=mesh.bounds();
        double span=b[3]-b[0],length=b[5]-b[2],height=b[4]-b[1];
        double scale=Math.min(.92,Math.min(7.0/span,7.8/length));
        // Centre mesh at reference viewing height regardless of wingspan.
        double altitude=1.55-(b[1]+b[4])*.5*scale;
        g.setColor(new Color(16,23,30));
        g.fillRect(0,0,WIDTH,HEIGHT);
        g.setColor(new Color(49,57,62));
        g.fillRect(0,79,WIDTH,121);
        // Stage floor and orientation lines are a real-time drawn environment,
        // not a pasted image of concept art.
        g.setColor(new Color(67,76,77));
        for(int y=94;y<200;y+=10)g.drawLine(0,y,WIDTH,y);
        g.setColor(new Color(89,93,85));
        for(int off=-3;off<=3;off++){
            g.drawLine(WIDTH/2+off*21,84,WIDTH/2+off*64,199);
        }
        g.setColor(new Color(171,137,78));
        g.drawLine(0,80,319,80);
        var placed=List.of(new UrbanScene.ModelInstance(
            modelId,7.65,0,rotation,scale,1,altitude));
        double[] wall=new double[WIDTH];
        Arrays.fill(wall,40.);
        // Raised observational camera looks down onto the *actual* mesh,
        // exposing wings, engine tops, nacelles and organic body volume.
        double cameraPitch=.48;
        double cameraEye=altitude+7.65*Math.tan(cameraPitch);
        int drawn=RENDERER.draw(g,gameMeshes,placed,0,0,0,wall,
            WIDTH,HEIGHT,cameraEye,cameraPitch);
        if(drawn!=1)throw new IllegalStateException("Airframe not within real renderer view: "+modelId);
        g.setColor(new Color(10,15,22,222));
        g.fillRect(0,0,WIDTH,34);
        g.fillRect(0,176,WIDTH,24);
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,10));
        g.setColor(new Color(225,219,198));
        var spec=AirPowerMeshes.specs().get(index);
        String faction=switch(spec.faction()){
            case THE_UNRAVELED -> "UNRAVELED";
            case THE_PENITENT -> "PENITENT";
            case THE_BEGOTTEN -> "BEGOTTEN";
        };
        g.drawString((index+1)+"/9  "+faction+"   "+spec.role(),7,12);
        g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,9));
        g.setColor(new Color(192,193,185));
        g.drawString(modelId.toUpperCase(Locale.ROOT),7,26);
        g.setColor(new Color(218,184,120));
        g.drawString(caption(index),7,187);
        g.setColor(new Color(176,182,185));
        g.drawString("F8 EXIT   LEFT/RIGHT MODEL  A/D TURN",7,197);
    }
}
