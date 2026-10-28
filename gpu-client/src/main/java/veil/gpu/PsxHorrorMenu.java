package veil.gpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.*;

/**
 * Real PSX Horror UI Free skin from heyheythere's CC BY 4.0 PNG assets.
 * These are 9-sliced source sprites rather than drawn approximation panels.
 * No Godot runtime is needed; the source PNGs are loaded from the licensed ZIP.
 */
public final class PsxHorrorMenu implements AutoCloseable {
    // Original licensed bytes are verified before first GPU texture upload.
    private static final int CANVAS_W=640,CANVAS_H=400;
    private static final int REFERENCE_W=320,REFERENCE_H=200;
    private static final int OFFSET_X=(CANVAS_W-REFERENCE_W)/2;
    private static final int OFFSET_Y=(CANVAS_H-REFERENCE_H)/2;
    private int sourcedDraws;
    public int sourcedDraws(){return sourcedDraws;}
    public interface Settings {
        void changeView(int delta);
        void changeLights(int delta);
        void changeTimeSpeed(int direction);
    }
    public enum Screen { MAIN, GRAPHICS, CREDITS, RUNNING }
    private Screen screen=Screen.MAIN;
    private int selection;
    private final Map<String,Texture> textures=new LinkedHashMap<>();
    private final Map<String,NinePatch> patches=new HashMap<>();
    private BitmapFont packFont;

    public PsxHorrorMenu(boolean startImmediately){
        if(startImmediately)screen=Screen.RUNNING;
        String base="psx-horror-ui-free/addons/psx_horror_ui/themes/fog/";
        Set<String> required=Set.of("window","panel","menu_band","button_normal",
            "button_hover","button_pressed","slider","slider_fill","divider",
            "dialogue","save_slot_normal");
        // The source ZIP contains STORE-method entries with data descriptors.
        // ZipInputStream rejects that producer layout; ZipFile reads its
        // central directory correctly. A short-lived 135 KB temp copy avoids
        // extracting assets into game data folders or redistributing content.
        Path archive=null;
        byte[] fontDefinition=null,fontAtlas=null;
        try{
            archive=PsxHorrorUiSource.verifiedCopy();
            try(ZipFile zip=new ZipFile(archive.toFile())){
                Enumeration<? extends ZipEntry> entries=zip.entries();
                while(entries.hasMoreElements()){
                    ZipEntry entry=entries.nextElement();
                    String name=entry.getName();
                    if(name.endsWith("/font/psx_horror_ui_font.fnt")){
                        try(InputStream raw=zip.getInputStream(entry)){fontDefinition=raw.readAllBytes();}
                        continue;
                    }
                    if(name.endsWith("/font/psx_horror_ui_font.png")){
                        try(InputStream raw=zip.getInputStream(entry)){fontAtlas=raw.readAllBytes();}
                        continue;
                    }
                    if(!name.startsWith(base)||!name.endsWith(".png"))continue;
                    String key=name.substring(base.length(),name.length()-4);
                    // Load all 53 original fog sprites, including inventory,
                    // health, maps, cursor, dialogs and save-slot art.
                    if(entry.getSize()<0||entry.getSize()>256*1024)
                        throw new IOException("Invalid UI sprite size for "+name);
                    byte[] bytes;
                    try(InputStream raw=zip.getInputStream(entry)){bytes=raw.readAllBytes();}
                    Pixmap source=new Pixmap(bytes,0,bytes.length);
                    Texture texture=new Texture(source);
                    source.dispose();
                    texture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
                    textures.put(key,texture);
                }
            }
        }catch(IOException cause){
            close();throw new IllegalStateException("PSX UI art missing or corrupt",cause);
        }finally{
            if(archive!=null)try{Files.deleteIfExists(archive);}catch(IOException ignored){}
        }
        if(fontDefinition==null||fontAtlas==null)
            throw new IllegalStateException("The PSX UI source bitmap font is missing");
        Path glyphDir=null;
        try{
            glyphDir=Files.createTempDirectory("malefic-psx-glyphs-");
            Path descriptor=glyphDir.resolve("psx_horror_ui_font.fnt");
            Path atlas=glyphDir.resolve("psx_horror_ui_font.png");
            Files.write(descriptor,fontDefinition);Files.write(atlas,fontAtlas);
            packFont=new BitmapFont(new FileHandle(descriptor.toFile()),
                new FileHandle(atlas.toFile()),false);
            packFont.getRegion().getTexture().setFilter(
                Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        }catch(IOException problem){
            close();throw new IllegalStateException("Cannot load PSX bitmap typeface",problem);
        }finally{
            if(glyphDir!=null){
                try{
                    Files.deleteIfExists(glyphDir.resolve("psx_horror_ui_font.fnt"));
                    Files.deleteIfExists(glyphDir.resolve("psx_horror_ui_font.png"));
                    Files.deleteIfExists(glyphDir);
                }catch(IOException ignored){}
            }
        }
        if(!textures.keySet().containsAll(required)||textures.size()!=53)
            throw new IllegalStateException("Incomplete original PSX UI fog theme: "+
                textures.size()+" sprites, required="+required);
        patch("window",5,18,5,5);
        patch("panel",5,5,5,5);
        patch("menu_band",4,16,6,7);
        patch("button_normal",4,4,7,8);
        patch("button_hover",4,4,7,8);
        patch("slider",3,3,2,2);
        patch("slider_fill",3,3,2,2);
        patch("progress_bg",4,4,3,3);
        patch("progress_fill",3,3,3,3);
        patch("tooltip",4,4,4,4);
        patch("line_edit",4,4,4,4);
        patch("slot",6,6,6,6);
        patch("slot_selected",6,6,6,6);
        patch("slot_equipped",6,6,6,6);
        // Approved original Malefic Veil art: small indexed PNG atlases.
        // Use nine-slicing so labels are always live text, never baked pixels.
        // Keep the licensed PSX windows, progress, tooltip and bitmap font.
        Texture originalButtons=new Texture(Gdx.files.classpath(
            "veil-original-art/pack01/button-states-preview.png"));
        Texture originalMenuIcons=new Texture(Gdx.files.classpath(
            "veil-original-art/pack01/menu-icons-preview.png"));
        Texture originalFolders=new Texture(Gdx.files.classpath(
            "veil-original-art/pack01/folder-icons-preview.png"));
        if(originalButtons.getWidth()!=240||originalButtons.getHeight()!=24||
           originalMenuIcons.getWidth()!=256||originalMenuIcons.getHeight()!=32||
           originalFolders.getWidth()!=256||originalFolders.getHeight()!=32)
            throw new IllegalStateException("Original Veil UI atlas dimensions changed");
        for(Texture atlas:new Texture[]{originalButtons,originalMenuIcons,originalFolders})
            atlas.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        textures.put("veil_buttons",originalButtons);
        textures.put("veil_menu_icons",originalMenuIcons);
        textures.put("veil_folders",originalFolders);
        patches.put("veil_button_normal",new NinePatch(
            new TextureRegion(originalButtons,0,0,80,24),8,8,7,7));
        patches.put("veil_button_hover",new NinePatch(
            new TextureRegion(originalButtons,80,0,80,24),8,8,7,7));
        patches.put("veil_button_pressed",new NinePatch(
            new TextureRegion(originalButtons,160,0,80,24),8,8,7,7));
        System.out.println("VEIL_ORIGINAL_UI_ART_READY buttons=3 menuIcons=8 folders=8");
        System.out.println("PSX_HORROR_UI_READY source=heyheythere sha256="+
            PsxHorrorUiSource.SHA256+" sprites="+textures.size()+
            " font=original fog-theme=yes");
    }
    private void patch(String id,int left,int right,int top,int bottom){
        patches.put(id,new NinePatch(new TextureRegion(textures.get(id)),
            left,right,top,bottom));
    }
    public BitmapFont font(){return packFont;}
    private void source(NinePatch sprite,SpriteBatch batch,float x,float y,float w,float h){
        sprite.draw(batch,x,y,w,h);sourcedDraws++;
    }
    private void raw(String key,SpriteBatch batch,float x,float y,float w,float h){
        Texture t=textures.get(key);
        if(t==null)throw new IllegalStateException("Original heyheythere sprite missing: "+key);
        batch.draw(t,x,y,w,h);sourcedDraws++;
    }
    /** The same original nine-sliced window/panel assets as the main menu. */
    public void frame(SpriteBatch batch,float x,float y,float width,float height){
        source(patches.get("window"),batch,x,y,width,height);
    }
    public void panel(SpriteBatch batch,float x,float y,float width,float height){
        source(patches.get("panel"),batch,x,y,width,height);
    }
    public void tooltip(SpriteBatch batch,float x,float y,float width,float height){
        source(patches.get("tooltip"),batch,x,y,width,height);
    }
    /** The actual source progress sprites, not hand-drawn status rectangles. */
    public void progress(SpriteBatch batch,float x,float y,float width,float fraction){
        if(!Float.isFinite(fraction))throw new IllegalArgumentException("Invalid status fraction");
        float bounded=Math.max(0,Math.min(1,fraction));
        source(patches.get("progress_bg"),batch,x,y,width,11);
        if(bounded>0)source(patches.get("progress_fill"),batch,x+2,y+2,
            Math.max(6,(width-4)*bounded),7);
    }
    public void slot(SpriteBatch batch,float x,float y,boolean selected){
        slot(batch,x,y,38,selected);
    }
    public void slot(SpriteBatch batch,float x,float y,float size,boolean selected){
        source(patches.get(selected?"slot_selected":"slot"),batch,x,y,size,size);
    }
    public void marker(SpriteBatch batch,float x,float y){
        raw("mark_you",batch,x,y,9,9);
    }
    public void separator(SpriteBatch batch,float x,float y,float width){
        raw("divider",batch,x,y,width,5);
    }
    public void band(SpriteBatch batch,float x,float y,float width,float height){
        source(patches.get("menu_band"),batch,x,y,width,height);
    }
    /** Standardized in-game menu row; original licensed normal/hover skin. */
    public void choice(SpriteBatch batch,String text,float x,float y,float width,
                       float height,boolean highlighted){
        source(patches.get(highlighted?"veil_button_hover":"veil_button_normal"),
            batch,x,y,width,height);
        packFont.draw(batch,text,x+10,y+height-5);
    }
    /** Tile atlas graphics only. Real in-engine controls still own hitboxes. */
    public void originalMenuIcon(SpriteBatch batch,int index,float x,float y,float size){
        if(index<0||index>=8)throw new IllegalArgumentException("Menu icon");
        batch.draw(new TextureRegion(textures.get("veil_menu_icons"),
            index*32,0,32,32),x,y,size,size);
        sourcedDraws++;
    }
    public void originalFolderIcon(SpriteBatch batch,int index,float x,float y,float size){
        if(index<0||index>=8)throw new IllegalArgumentException("Folder icon");
        batch.draw(new TextureRegion(textures.get("veil_folders"),
            index*32,0,32,32),x,y,size,size);
        sourcedDraws++;
    }
    public Screen screen(){return screen;}
    public void showMain(){screen=Screen.MAIN;selection=0;}
    public void showRunning(){screen=Screen.RUNNING;selection=0;}
    public boolean running(){return screen==Screen.RUNNING;}
    private int size(){return switch(screen){
        case MAIN -> 4; case GRAPHICS -> 4; case CREDITS -> 1; case RUNNING -> 0;
    };}
    private void back(){
        if(screen==Screen.RUNNING){screen=Screen.MAIN;selection=0;}
        else if(screen!=Screen.MAIN){screen=Screen.MAIN;selection=0;}
    }
    private void activate(Settings settings,int direction){
        if(screen==Screen.MAIN){
            if(selection==0){screen=Screen.RUNNING;}
            else if(selection==1){screen=Screen.GRAPHICS;selection=0;}
            else if(selection==2){screen=Screen.CREDITS;selection=0;}
            else if(selection==3)Gdx.app.exit();
        }else if(screen==Screen.GRAPHICS){
            if(selection==0)settings.changeView(direction==0?4:direction*4);
            else if(selection==1)settings.changeLights(direction==0?1:direction);
            else if(selection==2)settings.changeTimeSpeed(direction==0?1:direction);
            else back();
        }else if(screen==Screen.CREDITS)back();
    }
    /** Returns whether world controls should be processed. */
    public boolean input(Settings settings){
        if(Gdx.input.isKeyJustPressed(Input.Keys.F10)){
            screen=screen==Screen.GRAPHICS?Screen.RUNNING:Screen.GRAPHICS;
            selection=0;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)){back();return false;}
        if(screen==Screen.RUNNING)return true;
        if(Gdx.input.isKeyJustPressed(Input.Keys.UP))
            selection=(selection+size()-1)%size();
        if(Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
            selection=(selection+1)%size();
        if(screen==Screen.GRAPHICS&&Gdx.input.isKeyJustPressed(Input.Keys.LEFT))activate(settings,-1);
        if(screen==Screen.GRAPHICS&&Gdx.input.isKeyJustPressed(Input.Keys.RIGHT))activate(settings,1);
        if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)){
            activate(settings,0);
            return false;
        }
        if(Gdx.input.justTouched()){
            // UI renders at the center of the 640x400 framebuffer, not at
            // the old 320x200 origin. Match pointer hitboxes to actual art.
            float px=Gdx.input.getX()*CANVAS_W/Gdx.graphics.getWidth()-OFFSET_X;
            float py=CANVAS_H-Gdx.input.getY()*CANVAS_H/Gdx.graphics.getHeight()-OFFSET_Y;
            if(px>=54&&px<=269){
                for(int n=0;n<size();n++){
                    float top=128-n*23;
                    if(py>=top&&py<top+20){selection=n;activate(settings,0);break;}
                }
            }
        }
        return screen==Screen.RUNNING;
    }
    private void drawText(BitmapFont font,SpriteBatch batch,String value,float x,float y){
        font.draw(batch,value,x,y);
    }
    private void button(SpriteBatch batch,BitmapFont font,int index,String label){
        int y=OFFSET_Y+128-index*23;
        band(batch,OFFSET_X+54,y,212,20);
        source(patches.get(index==selection?"veil_button_hover":"veil_button_normal"),
            batch,OFFSET_X+58,y+1,204,18);
        drawText(font,batch,(index==selection?"> ":"  ")+label,OFFSET_X+68,y+13);
    }
    public void draw(SpriteBatch batch,BitmapFont font,float viewRange,int maxLights,int timeSpeed){
        if(screen==Screen.RUNNING)return;
        frame(batch,OFFSET_X+34,OFFSET_Y+18,252,170);
        drawText(font,batch,"THE MALEFIC VEIL",OFFSET_X+47,OFFSET_Y+176);
        String subtitle=switch(screen){
            case MAIN -> "THE WORLD HAS FORGOTTEN ITSELF";
            case GRAPHICS -> "GRAPHICS / ENVIRONMENT";
            case CREDITS -> "ART / ATTRIBUTION";
            case RUNNING -> "";
        };
        drawText(font,batch,subtitle,OFFSET_X+47,OFFSET_Y+157);
        if(screen==Screen.MAIN){
            button(batch,font,0,"ENTER THE VEIL");
            button(batch,font,1,"GRAPHICS");
            button(batch,font,2,"CREDITS");
            button(batch,font,3,"QUIT");
        }else if(screen==Screen.GRAPHICS){
            button(batch,font,0,"VIEW RANGE: "+(int)viewRange+"   -/+");
            button(batch,font,1,"LOCAL LIGHTS: "+maxLights+"   -/+");
            button(batch,font,2,"TIME SPEED: "+timeSpeed+"X   -/+");
            button(batch,font,3,"RETURN");
        }else if(screen==Screen.CREDITS){
            panel(batch,OFFSET_X+50,OFFSET_Y+60,220,88);
            drawText(font,batch,"PSX Horror UI Free by heyheythere",OFFSET_X+57,OFFSET_Y+130);
            drawText(font,batch,"CC BY 4.0",OFFSET_X+57,OFFSET_Y+116);
            drawText(font,batch,"heyheythere.itch.io/psx-horror-ui-free",OFFSET_X+57,OFFSET_Y+103);
            drawText(font,batch,"KENNEY RETRO URBAN KIT - CC0",OFFSET_X+57,OFFSET_Y+88);
            button(batch,font,0,"RETURN");
        }
        drawText(font,batch,"ARROWS SELECT   ENTER APPLY   ESC BACK",
            OFFSET_X+47,OFFSET_Y+30);
    }
    @Override public void close(){
        for(Texture texture:textures.values())texture.dispose();
        textures.clear();patches.clear();
        if(packFont!=null){packFont.dispose();packFont=null;}
    }
}
