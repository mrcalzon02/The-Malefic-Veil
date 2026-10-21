package veil.gpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
    private static final String ZIP="/psx-ui/psx-horror-ui-free-assets.zip";
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
        try(InputStream file=PsxHorrorMenu.class.getResourceAsStream(ZIP)){
            if(file==null)throw new IOException("Missing CC BY 4.0 PSX Horror UI Free resource "+ZIP);
            archive=Files.createTempFile("malefic-psx-ui-", ".zip");
            Files.copy(file,archive,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            try(ZipFile zip=new ZipFile(archive.toFile())){
                Enumeration<? extends ZipEntry> entries=zip.entries();
                while(entries.hasMoreElements()){
                    ZipEntry entry=entries.nextElement();
                    String name=entry.getName();
                    if(!name.startsWith(base)||!name.endsWith(".png"))continue;
                    String key=name.substring(base.length(),name.length()-4);
                    if(!required.contains(key))continue;
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
        if(!textures.keySet().containsAll(required))
            throw new IllegalStateException("Incomplete PSX UI kit: "+required+" vs "+textures.keySet());
        patch("window",5,18,5,5);
        patch("panel",5,5,5,5);
        patch("menu_band",4,16,6,7);
        patch("button_normal",4,4,7,8);
        patch("button_hover",4,4,7,8);
        patch("slider",3,3,2,2);
        patch("slider_fill",3,3,2,2);
    }
    private void patch(String id,int left,int right,int top,int bottom){
        patches.put(id,new NinePatch(new TextureRegion(textures.get(id)),
            left,right,top,bottom));
    }
    public Screen screen(){return screen;}
    public void showMain(){screen=Screen.MAIN;selection=0;}
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
            float px=Gdx.input.getX()*320f/Gdx.graphics.getWidth();
            float py=200-Gdx.input.getY()*200f/Gdx.graphics.getHeight();
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
        int y=128-index*23;
        patches.get("menu_band").draw(batch,54,y,212,20);
        if(index==selection)patches.get("button_hover").draw(batch,58,y+1,204,18);
        else patches.get("button_normal").draw(batch,58,y+1,204,18);
        drawText(font,batch,(index==selection?"> ":"  ")+label,68,y+13);
    }
    public void draw(SpriteBatch batch,BitmapFont font,float viewRange,int maxLights,int timeSpeed){
        if(screen==Screen.RUNNING)return;
        patches.get("window").draw(batch,34,18,252,170);
        drawText(font,batch,"THE MALEFIC VEIL",47,176);
        String subtitle=switch(screen){
            case MAIN -> "THE WORLD HAS FORGOTTEN ITSELF";
            case GRAPHICS -> "GRAPHICS / ENVIRONMENT";
            case CREDITS -> "ART / ATTRIBUTION";
            case RUNNING -> "";
        };
        drawText(font,batch,subtitle,47,157);
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
            patches.get("panel").draw(batch,50,60,220,88);
            drawText(font,batch,"PSX Horror UI Free by heyheythere",57,130);
            drawText(font,batch,"CC BY 4.0",57,116);
            drawText(font,batch,"heyheythere.itch.io/psx-horror-ui-free",57,103);
            drawText(font,batch,"KENNEY RETRO URBAN KIT - CC0",57,88);
            button(batch,font,0,"RETURN");
        }
        drawText(font,batch,"ARROWS SELECT   ENTER APPLY   ESC BACK",47,30);
    }
    @Override public void close(){
        for(Texture texture:textures.values())texture.dispose();
        textures.clear();patches.clear();
    }
}
