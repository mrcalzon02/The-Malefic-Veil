package veil.gpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import veil.MaleficVeil;

import java.io.IOException;
import java.util.*;

/**
 * PSX source-skinned in-world pause/options/menu interface. Never opens Swing
 * or creates a second window. Controls talk to the actual authoritative game.
 */
public final class GpuOverlayMenu {
    public enum Screen { GAME,PAUSE,VIDEO,AUDIO,CONTROLS }
    private static final int WIDTH=640,HEIGHT=400;
    private record Item(String label,Runnable action,Runnable prev,Runnable next){}
    private final PsxHorrorMenu skin;
    private final MaleficVeil gameplay;
    private final GpuClientSettings settings;
    private final Runnable goMain;
    private final Runnable apply;
    private final GpuAudioBus audio;
    private final List<Item> items=new ArrayList<>();
    private Screen screen=Screen.GAME;
    private int selected;
    private String notice="";
    public GpuOverlayMenu(PsxHorrorMenu skin,MaleficVeil game,
            GpuClientSettings settings,GpuAudioBus audio,
            Runnable goMain,Runnable apply){
        this.skin=Objects.requireNonNull(skin);
        this.gameplay=Objects.requireNonNull(game);
        this.settings=Objects.requireNonNull(settings);
        this.audio=audio;this.goMain=goMain;this.apply=apply;
    }
    public Screen screen(){return screen;}
    public boolean active(){return screen==Screen.GAME;}
    private void show(Screen target){screen=target;selected=0;build();}
    private void add(String label,Runnable action){items.add(new Item(label,action,null,null));}
    private void slider(String label,Runnable prev,Runnable next){
        items.add(new Item(label,next,prev,next));
    }
    private void change(int key){
        gameplay.gpuKey(key,true);
        gameplay.gpuKey(key,false);
        show(Screen.GAME);
    }
    private void save(){
        try{settings.save();notice="OPTIONS SAVED";show(Screen.PAUSE);}
        catch(IOException error){notice="COULD NOT SAVE: "+error.getMessage();}
    }
    private void volume(String group,int delta){
        int next;
        switch(group){
            case "master"->{next=settings.master+delta;settings.master=Math.max(0,Math.min(100,next));}
            case "music"->{next=settings.music+delta;settings.music=Math.max(0,Math.min(100,next));}
            case "effects"->{next=settings.effects+delta;settings.effects=Math.max(0,Math.min(100,next));}
            case "ambience"->{next=settings.ambience+delta;settings.ambience=Math.max(0,Math.min(100,next));}
            case "ui"->{next=settings.ui+delta;settings.ui=Math.max(0,Math.min(100,next));}
            case "voice"->{next=settings.voice+delta;settings.voice=Math.max(0,Math.min(100,next));}
            default->throw new IllegalArgumentException(group);
        }
        if(audio!=null)audio.apply(settings);
        build();
    }
    private void build(){
        items.clear();
        switch(screen){
            case GAME->{}
            case PAUSE->{
                add("RESUME GAME",()->show(Screen.GAME));
                add("INVENTORY",()->change(java.awt.event.KeyEvent.VK_I));
                add("CHARACTER / SKILLS",()->change(java.awt.event.KeyEvent.VK_K));
                add("CITY MAP",()->change(java.awt.event.KeyEvent.VK_M));
                add("CONTINENT MAP",()->change(java.awt.event.KeyEvent.VK_N));
                add("VIDEO / GRAPHICS",()->show(Screen.VIDEO));
                add("SOUND / VOLUME",()->show(Screen.AUDIO));
                add("CONTROLS",()->show(Screen.CONTROLS));
                add("RETURN TO MAIN MENU",goMain);
            }
            case VIDEO->{
                slider("DRAW DISTANCE "+settings.distance+" M",
                    ()->{settings.distance=Math.max(45,settings.distance-10);apply.run();build();},
                    ()->{settings.distance=Math.min(240,settings.distance+10);apply.run();build();});
                slider("FOV "+settings.fieldOfView+" DEG",
                    ()->{settings.fieldOfView=Math.max(55,settings.fieldOfView-5);apply.run();build();},
                    ()->{settings.fieldOfView=Math.min(100,settings.fieldOfView+5);apply.run();build();});
                slider("LOCAL LIGHTS "+settings.lights,
                    ()->{settings.lights=Math.max(0,settings.lights-1);apply.run();build();},
                    ()->{settings.lights=Math.min(8,settings.lights+1);apply.run();build();});
                slider("VSYNC "+(settings.vsync?"ON":"OFF"),
                    this::vsync,this::vsync);
                slider("FULLSCREEN "+(settings.fullscreen?"YES":"NO"),
                    this::fullscreen,this::fullscreen);
                add("SAVE / BACK",this::save);
            }
            case AUDIO->{
                slider("MASTER "+settings.master+"%",()->volume("master",-5),()->volume("master",5));
                slider("MUSIC "+settings.music+"%",()->volume("music",-5),()->volume("music",5));
                slider("EFFECTS "+settings.effects+"%",()->volume("effects",-5),()->volume("effects",5));
                slider("AMBIENCE "+settings.ambience+"%",()->volume("ambience",-5),()->volume("ambience",5));
                slider("UI "+settings.ui+"%",()->volume("ui",-5),()->volume("ui",5));
                slider("VOICE "+settings.voice+"%",()->volume("voice",-5),()->volume("voice",5));
                add("SAVE / BACK",this::save);
            }
            case CONTROLS->{
                slider("LOOK SENSITIVITY "+settings.sensitivity+"%",
                    ()->{settings.sensitivity=Math.max(20,settings.sensitivity-10);build();},
                    ()->{settings.sensitivity=Math.min(200,settings.sensitivity+10);build();});
                slider("INVERT MOUSE Y "+(settings.invertY?"ON":"OFF"),
                    ()->{settings.invertY=!settings.invertY;build();},
                    ()->{settings.invertY=!settings.invertY;build();});
                add("SAVE / BACK",this::save);
            }
        }
    }
    private void vsync(){settings.vsync=!settings.vsync;Gdx.graphics.setVSync(settings.vsync);build();}
    private void fullscreen(){
        settings.fullscreen=!settings.fullscreen;
        if(settings.fullscreen)Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        else{
            int[] size=GpuClientSettings.WINDOWS[settings.windowSize];
            Gdx.graphics.setWindowedMode(size[0],size[1]);
        }
        build();
    }
    private void trigger(int change){
        if(items.isEmpty())return;
        if(audio!=null)audio.click();
        Item item=items.get(selected);
        if(change<0&&item.prev()!=null)item.prev().run();
        else if(change>0&&item.next()!=null)item.next().run();
        else item.action().run();
    }
    /** Return true ONLY when gameplay controls may run. */
    public boolean input(){
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)||
            Gdx.input.isKeyJustPressed(Input.Keys.F11)){
            if(screen==Screen.GAME)show(Screen.PAUSE);
            else if(screen==Screen.PAUSE)show(Screen.GAME);
            else show(Screen.PAUSE);
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.F10))
            show(screen==Screen.VIDEO?Screen.GAME:Screen.VIDEO);
        if(screen==Screen.GAME)return true;
        if(Gdx.input.isKeyJustPressed(Input.Keys.UP))
            selected=Math.floorMod(selected-1,items.size());
        if(Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
            selected=Math.floorMod(selected+1,items.size());
        if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER))trigger(0);
        if(Gdx.input.isKeyJustPressed(Input.Keys.LEFT))trigger(-1);
        if(Gdx.input.isKeyJustPressed(Input.Keys.RIGHT))trigger(1);
        if(Gdx.input.justTouched()){
            float px=Gdx.input.getX()*WIDTH/Gdx.graphics.getWidth();
            float py=HEIGHT-Gdx.input.getY()*HEIGHT/Gdx.graphics.getHeight();
            if(px>=144&&px<=504)for(int i=0;i<items.size();i++){
                float bottom=297-i*24;
                if(py>=bottom&&py<bottom+22){selected=i;trigger(0);break;}
            }
        }
        Gdx.input.setCursorCatched(false);
        return false;
    }
    /** One source-skinned menu over the exact paused, live 3D world. */
    public void draw(SpriteBatch sprites){
        if(screen==Screen.GAME)return;
        skin.frame(sprites,120,30,408,345);
        skin.band(sprites,137,334,375,30);
        skin.font().draw(sprites,"THE MALEFIC VEIL / "+screen,149,354);
        for(int i=0;i<items.size();i++)
            skin.choice(sprites,items.get(i).label(),143,297-i*24,364,22,selected==i);
        skin.band(sprites,135,39,379,24);
        skin.font().draw(sprites,notice.isBlank()?
            "ESC RESUME   ARROWS / ENTER   MOUSE":
            notice,147,55);
    }
}
