package veil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;

/** Real triangle, palette, and opacity acceptance test for authored game PSX models. */
public final class GameplayAssetTest {
    private static int checks;
    private static void check(boolean ok,String msg){checks++;if(!ok)throw new AssertionError(msg);}
    private static BufferedImage render(GameplayMeshes pack,double alpha){
        BufferedImage img=new BufferedImage(320,200,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=img.createGraphics();
        g.setColor(new Color(18,20,28));g.fillRect(0,0,320,200);
        double[] depth=new double[320];Arrays.fill(depth,40);
        java.util.List<UrbanScene.ModelInstance> models=java.util.List.of(
            new UrbanScene.ModelInstance(GameplayMeshes.HERO[0],3.5,1.5,-Math.PI/2,1,alpha),
            new UrbanScene.ModelInstance(GameplayMeshes.PICKUP[0],4.5,2.2,0,1,1,.95),
            new UrbanScene.ModelInstance(GameplayMeshes.DEVICE[0],5.3,.3,0,1));
        int visible=new PropRenderer().draw(g,pack.meshes(),models,1.5,1.5,0,
            depth,320,200);
        g.dispose();
        check(visible==(alpha==0?2:3),
            "visible instances must exclude a fully transparent character");
        return img;
    }
    public static void main(String[] ignored)throws Exception{
        GameplayMeshes lib=new GameplayMeshes();
        // Preserve every legacy gameplay model while accepting actual
        // resource nodes and nine new static airframes in the same atlas.
        java.util.Set<String> required=new java.util.HashSet<>();
        Collections.addAll(required,GameplayMeshes.HERO);
        Collections.addAll(required,GameplayMeshes.WEAPON);
        Collections.addAll(required,GameplayMeshes.PICKUP);
        Collections.addAll(required,GameplayMeshes.DEVICE);
        required.add("enemy-walker");
        required.addAll(java.util.List.of(
            "resource-tree-broadleaf","resource-tree-conifer","resource-stone",
            "resource-ore","resource-fiber","resource-edibles",
            "resource-herbs","resource-water"));
        for(var aircraft:AirPowerMeshes.specs())required.add(aircraft.id());
        check(required.size()==38,"Complete legacy + 8 resources + 9 aircraft + 3 field structures");
        check(lib.meshes().keySet().containsAll(required),
            "No prior gameplay mesh may disappear during aircraft integration");
        check(lib.meshes().size()>=required.size(),
            "Static airframes and harvested-resource meshes must be registered");
        int triangles=0;
        for(var entry:lib.meshes().entrySet()){
            GlbMesh mesh=entry.getValue();int count=mesh.triangleCount();
            check(count>=12,entry.getKey()+" should be actual triangulated geometry");
            float[] bounds=mesh.bounds();
            check(bounds[4]>bounds[1]&&bounds[3]>bounds[0],"nonzero 3D bounds: "+entry.getKey());
            triangles+=count;
        }
        check(triangles>700,"models are too simple to be meaningful 3D geometry");
        BufferedImage opaque=render(lib,1),translucent=render(lib,.25),empty=render(lib,0);
        int changed=0,lessVisible=0;
        for(int y=0;y<200;y++)for(int x=0;x<320;x++){
            int full=opaque.getRGB(x,y),quarter=translucent.getRGB(x,y),none=empty.getRGB(x,y);
            if(full!=none){
                changed++;
                if(full!=quarter&&quarter!=none)lessVisible++;
            }
        }
        check(changed>40,"mesh geometry never rasterized");
        check(lessVisible>20,"25%-opaque character did not alpha blend into the scene");
        BufferedImage sheet=new BufferedImage(960,1400,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=sheet.createGraphics();
        String[] gallery=new String[21];
        System.arraycopy(GameplayMeshes.HERO,0,gallery,0,4);
        gallery[4]="enemy-walker";
        System.arraycopy(GameplayMeshes.WEAPON,0,gallery,5,2);
        System.arraycopy(GameplayMeshes.PICKUP,0,gallery,7,7);
        System.arraycopy(GameplayMeshes.DEVICE,0,gallery,14,7);
        for(int row=0;row<7;row++)for(int col=0;col<3;col++){
            int index=row*3+col;
            String id=gallery[index];
            BufferedImage panel=new BufferedImage(320,200,BufferedImage.TYPE_INT_RGB);
            Graphics2D pg=panel.createGraphics();
            pg.setColor(new Color(19,20,28));pg.fillRect(0,0,320,200);
            double[] depth=new double[320];Arrays.fill(depth,40);
            if(index!=5&&index!=6){
                boolean pickup=index>=7&&index<14;
                double distance=pickup?3.0:index>=14?3.2:2.0;
                double rise=pickup?.96:0;
                new PropRenderer().draw(pg,lib.meshes(),
                    java.util.List.of(new UrbanScene.ModelInstance(id,1.5+distance,1.5,
                        -Math.PI/2,pickup?1.5:1,1,rise)),
                    1.5,1.5,0,depth,320,200);
            }
            else new WeaponViewRenderer().draw(pg,lib.meshes().get(id),320,200,0);
            int visiblePixels=0;
            int bg=new Color(19,20,28).getRGB();
            for(int yy=34;yy<200;yy++)for(int xx=5;xx<315;xx++)
                if(panel.getRGB(xx,yy)!=bg)visiblePixels++;
            check(visiblePixels>=8,id+" is visually absent or clipped below camera");
            pg.setFont(new Font(Font.MONOSPACED,Font.BOLD,13));
            pg.setColor(Color.WHITE);pg.drawString(id,10,22);
            pg.dispose();g.drawImage(panel,col*320,row*200,null);
        }
        g.dispose();
        File image=new File("build/test-visuals/gameplay-models.png");
        image.getParentFile().mkdirs();
        check(ImageIO.write(sheet,"png",image),"cannot write PSX model QA image");
        check(image.length()>4000,"QA image blank");
        System.out.println("PASS GameplayAssetTest: "+lib.meshes().size()+
            " authored 3D models, "+triangles+" triangles, "+checks+
            " assertions, "+lessVisible+" alpha-blended pixels");
    }
}
