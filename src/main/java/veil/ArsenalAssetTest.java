package veil;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Runtime mesh import, actual rigid pose changes and rendered evidence for all 32. */
public final class ArsenalAssetTest {
    public static void main(String[] args)throws Exception{
        ArsenalMeshes meshes=new ArsenalMeshes();int count=0;
        for(int page=0;page<2;page++){
            BufferedImage sheet=new BufferedImage(900,2880,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();g.setColor(new Color(17,23,32));g.fillRect(0,0,900,2880);
            for(int row=0;row<16;row++){
                int w=page*16+row;var rest=meshes.posed(w,0,0);var fired=meshes.posed(w,1,.4);var reload=meshes.posed(w,3,.5);
                if(rest.triangleCount()<50||rest.triangles().equals(fired.triangles())||rest.triangles().equals(reload.triangles()))throw new AssertionError("Missing geometry / animation: "+w);
                for(float b:reload.bounds())if(!Float.isFinite(b))throw new AssertionError("Bad pose bounds");
                for(int col=0;col<3;col++){
                    int action=col==0?0:col==1?1:3;long age=col==2?ArsenalCatalog.get(w).reloadMs()/2:col==1?(long)(Math.min(400,ArsenalCatalog.get(w).primary().cooldown())*.4):0;
                    Graphics2D panel=(Graphics2D)g.create(col*300,row*180,300,180);
                    var view=new ArsenalSystems.View(1L<<w,w,0,false,new int[32],new int[32],new int[9],action,0,0,0,0,1,0);
                    meshes.draw(panel,w,view,age,300,180,0);panel.setColor(Color.WHITE);panel.setFont(new Font(Font.MONOSPACED,Font.PLAIN,10));panel.drawString(ArsenalCatalog.get(w).name()+" / "+(col==0?"idle":col==1?"fire":"reload"),5,14);panel.dispose();
                }count++;
            }g.dispose();Path path=Path.of("build/test-visuals/arsenal-runtime-"+(page+1)+".png");Files.createDirectories(path.getParent());ImageIO.write(sheet,"png",path.toFile());
        }System.out.println("PASS ArsenalAssetTest: "+count+" textured runtime meshes; fire/reload poses and 96 rendered views");
    }
}
