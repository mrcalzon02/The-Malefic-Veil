package veil.gpu;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import javax.swing.SwingUtilities;
import veil.*;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;

/**
 * One native OpenGL window from initial logo to authoritative gameplay.
 * The actual city renderer runs behind all launcher controls, not a wallpaper.
 * Solo/host/join/create/load/class selection never uses Swing modal dialogs.
 */
public final class GpuFrontEnd extends ApplicationAdapter {
    public enum Screen { HOME,WORLDS,CREATE,LOAD,JOIN,FACTION,CLASS,VIDEO,AUDIO,CONTROLS,CREDITS,CONNECTING }
    private record Entry(String label,Runnable action,Runnable previous,Runnable next,String textField){}
    private static final int W=640,H=400,START_Y=307,ROW=24;
    private final boolean smoke,flowSmoke;
    private GpuKenneyPreview backdrop;
    private GpuKenneyPreview gameRenderer;
    private PsxHorrorMenu skin;
    private SpriteBatch ui;
    private GpuClientSettings settings;
    private GpuAudioBus audio;
    private Screen screen=Screen.HOME;
    private Screen backFromOptions=Screen.HOME;
    private final List<Entry> entries=new ArrayList<>();
    private final List<WorldCreationConfig> saves=new ArrayList<>();
    private int selected,loadOffset,frames;
    private String mode="solo",worldName="New Settlement",seedText="";
    private String enemy="normal",loot="normal",address="127.0.0.1",portText="7777";
    private int ai=6,capacity=12;
    private boolean hardcore;
    private WorldCreationConfig chosenWorld;
    private CharacterRoster.Faction faction=CharacterRoster.Faction.THE_UNRAVELED;
    private volatile MaleficVeil ready;
    private MaleficVeil readyGame;
    private volatile Throwable failed;
    private String message="Select a mode. One game window; no external setup dialogs.";
    public GpuFrontEnd(){this(false,false);}
    public GpuFrontEnd(boolean smoke){this(smoke,false);}
    public GpuFrontEnd(boolean smoke,boolean flowSmoke){
        this.smoke=smoke;this.flowSmoke=flowSmoke;
    }
    public Screen screen(){return screen;}

    @Override public void create(){
        DesktopDiagnostics.install("desktop");
        DesktopDiagnostics.note("UNIFIED_GPU_MAIN_MENU");
        settings=GpuClientSettings.load();
        ui=new SpriteBatch();
        skin=new PsxHorrorMenu(true);
        backdrop=GpuKenneyPreview.menuBackdrop(20261020);
        backdrop.clientSettings(settings);
        backdrop.create();
        audio=new GpuAudioBus(settings);
        audio.open();
        applyVideo();
        Gdx.input.setInputProcessor(new InputAdapter(){
            @Override public boolean keyTyped(char character){
                if(screen!=Screen.CREATE&&screen!=Screen.JOIN)return false;
                if(selected<0||selected>=entries.size())return false;
                String field=entries.get(selected).textField();
                if(field==null)return false;
                String val=getText(field);
                if(character=='\b'||character==127){
                    if(!val.isEmpty())setText(field,val.substring(0,val.length()-1));
                    return true;
                }
                if(character>=32&&character<=126&&val.length()<50){
                    setText(field,val+character);
                    return true;
                }
                return false;
            }
        });
        rebuild();
    }
    private String getText(String key){return switch(key){
        case "world"->worldName;case "seed"->seedText;
        case "address"->address;case "port"->portText;
        default->throw new IllegalArgumentException("Unknown input");
    };}
    private void setText(String key,String value){
        switch(key){
            case "world"->worldName=value;case "seed"->seedText=value;
            case "address"->address=value;case "port"->portText=value;
            default->throw new IllegalArgumentException("Unknown input");
        }
        rebuild(false);
    }
    private void gotoScreen(Screen next){screen=next;selected=0;rebuild();}
    private void note(String value){message=value;System.out.println("GPU_MENU "+value);}
    private void item(String label,Runnable action){
        entries.add(new Entry(label,action,null,null,null));
    }
    private void edit(String label,String key){
        entries.add(new Entry(label,()->note("Type directly; Backspace deletes."),
            null,null,key));
    }
    private void adjustable(String label,Runnable backwards,Runnable forwards){
        entries.add(new Entry(label,forwards,backwards,forwards,null));
    }
    private void rebuild(){rebuild(true);}
    private void rebuild(boolean reset){
        int old=selected;entries.clear();
        switch(screen){
            case HOME -> {
                item("SINGLE PLAYER",()->worldMode("solo"));
                item("HOST LOCAL / LAN MULTIPLAYER",()->worldMode("multi"));
                item("JOIN MULTIPLAYER HOST",()->{
                    mode="join";chosenWorld=null;gotoScreen(Screen.JOIN);
                });
                item("LEVEL EDITOR (DEV)",()->worldMode("editor"));
                item("OPTIONS",()->{backFromOptions=Screen.HOME;gotoScreen(Screen.VIDEO);});
                item("CREDITS",()->gotoScreen(Screen.CREDITS));
                item("QUIT GAME",()->Gdx.app.exit());
            }
            case WORLDS -> {
                item("CREATE NEW WORLD",()->gotoScreen(Screen.CREATE));
                item("LOAD / CONTINUE WORLD",this::showSaves);
                item("BACK",()->gotoScreen(Screen.HOME));
            }
            case CREATE -> {
                edit("WORLD NAME: "+worldName+"_", "world");
                edit("SEED (BLANK=RANDOM): "+seedText+"_", "seed");
                adjustable("MODE: "+(hardcore?"HARDCORE":"NORMAL")+
                    " < >",()->{hardcore=!hardcore;rebuild(false);},
                    ()->{hardcore=!hardcore;rebuild(false);});
                adjustable("ENEMY DENSITY: "+enemy+" < >",()->cycleEnemy(-1),()->cycleEnemy(1));
                adjustable("RESOURCE LOOT: "+loot+" < >",()->cycleLoot(-1),()->cycleLoot(1));
                adjustable("AI POPULATION: "+ai+" < >",()->stepAi(-1),()->stepAi(1));
                if(mode.equals("multi"))
                    adjustable("MAX PLAYERS: "+capacity+" < >",()->stepCap(-1),()->stepCap(1));
                item("CREATE AND CONTINUE",this::createWorld);
                item("BACK",()->gotoScreen(Screen.WORLDS));
            }
            case LOAD -> {
                int stop=Math.min(saves.size(),loadOffset+9);
                for(int i=loadOffset;i<stop;i++){
                    WorldCreationConfig found=saves.get(i);
                    item(found.worldName()+"  "+(found.hardcore()?"HC ":"")+
                        found.seed(),()->{chosenWorld=found;afterWorld();});
                }
                if(loadOffset>0)item("PREVIOUS WORLDS",()->{loadOffset=Math.max(0,loadOffset-9);rebuild();});
                if(loadOffset+9<saves.size())
                    item("MORE WORLDS",()->{loadOffset+=9;rebuild();});
                item("BACK",()->gotoScreen(Screen.WORLDS));
            }
            case JOIN -> {
                edit("HOST / IP: "+address+"_", "address");
                edit("PORT: "+portText+"_", "port");
                item("SELECT FACTION",()->{
                    try{
                        if(address.isBlank()||address.length()>253)throw new IllegalArgumentException("Host required");
                        int port=Integer.parseInt(portText);
                        if(port<1||port>65535)throw new IllegalArgumentException("Port must be 1-65535");
                        chosenWorld=null;gotoScreen(Screen.FACTION);
                    }catch(NumberFormatException invalid){note("Invalid network port");}
                    catch(IllegalArgumentException invalid){note(invalid.getMessage());}
                });
                item("BACK",()->gotoScreen(Screen.HOME));
            }
            case FACTION -> {
                for(CharacterRoster.Faction f:CharacterRoster.Faction.values())
                    item(f.display.toUpperCase(Locale.ROOT),()->{
                        faction=f;gotoScreen(Screen.CLASS);
                    });
                item("BACK",()->gotoScreen(mode.equals("join")?Screen.JOIN:Screen.WORLDS));
            }
            case CLASS -> {
                for(FactionClasses.Role role:FactionClasses.faction(faction)){
                    item(role.name().toUpperCase(Locale.ROOT)+
                        (role.playable()?"":" [UNAVAILABLE]"),()->{
                        if(!role.playable()){note("Class unavailable: "+role.purpose());return;}
                        startGame(role);
                    });
                }
                item("CHANGE FACTION",()->gotoScreen(Screen.FACTION));
            }
            case VIDEO -> {
                adjustable("VIEW DISTANCE: "+settings.distance+" M",
                    ()->changeView(-10),()->changeView(10));
                adjustable("FIELD OF VIEW: "+settings.fieldOfView+" DEG",
                    ()->changeFov(-5),()->changeFov(5));
                adjustable("LOCAL LIGHTS: "+settings.lights,
                    ()->changeLights(-1),()->changeLights(1));
                adjustable("WINDOW SIZE: "+GpuClientSettings.WINDOWS[settings.windowSize][0]+
                    "X"+GpuClientSettings.WINDOWS[settings.windowSize][1],
                    ()->changeWindow(-1),()->changeWindow(1));
                adjustable("FULLSCREEN: "+(settings.fullscreen?"YES":"NO"),
                    this::toggleFullscreen,this::toggleFullscreen);
                adjustable("VSYNC: "+(settings.vsync?"ON":"OFF"),
                    this::toggleVsync,this::toggleVsync);
                item("AUDIO OPTIONS",()->gotoScreen(Screen.AUDIO));
                item("CONTROL OPTIONS",()->gotoScreen(Screen.CONTROLS));
                item("SAVE & RETURN",this::saveSettings);
            }
            case AUDIO -> {
                adjustable("MASTER VOLUME: "+settings.master+"%",
                    ()->sound("master",-5),()->sound("master",5));
                adjustable("MUSIC VOLUME: "+settings.music+"%",
                    ()->sound("music",-5),()->sound("music",5));
                adjustable("SFX VOLUME: "+settings.effects+"%",
                    ()->sound("effects",-5),()->sound("effects",5));
                adjustable("AMBIENCE VOLUME: "+settings.ambience+"%",
                    ()->sound("ambience",-5),()->sound("ambience",5));
                adjustable("UI VOLUME: "+settings.ui+"%",
                    ()->sound("ui",-5),()->sound("ui",5));
                adjustable("VOICE VOLUME: "+settings.voice+"%",
                    ()->sound("voice",-5),()->sound("voice",5));
                item("VIDEO OPTIONS",()->gotoScreen(Screen.VIDEO));
                item("SAVE & RETURN",this::saveSettings);
            }
            case CONTROLS -> {
                adjustable("MOUSE SENSITIVITY: "+settings.sensitivity+"%",
                    ()->{settings.sensitivity=Math.max(20,settings.sensitivity-10);rebuild(false);},
                    ()->{settings.sensitivity=Math.min(200,settings.sensitivity+10);rebuild(false);});
                adjustable("INVERT VERTICAL LOOK: "+(settings.invertY?"YES":"NO"),
                    ()->{settings.invertY=!settings.invertY;rebuild(false);},
                    ()->{settings.invertY=!settings.invertY;rebuild(false);});
                item("VIDEO OPTIONS",()->gotoScreen(Screen.VIDEO));
                item("SAVE & RETURN",this::saveSettings);
            }
            case CREDITS -> {
                item("PSX HORROR UI FREE / HEYHEYTHERE",()->note("Art: heyheythere (CC BY 4.0)"));
                item("KENNEY RETRO URBAN / CC0",()->note("Art: Kenney (CC0)"));
                item("MUSIC: COLD UNRESOLVED DRONE",()->note("User-provided audio (Drive source)"));
                item("BACK",()->gotoScreen(Screen.HOME));
            }
            case CONNECTING -> item("CONNECTING... PLEASE KEEP GAME OPEN",()->{});
        }
        selected=reset?0:Math.min(Math.max(0,old),entries.size()-1);
    }
    private void worldMode(String next){
        mode=next;chosenWorld=null;gotoScreen(Screen.WORLDS);
    }
    private void showSaves(){
        try{
            saves.clear();
            String kind=mode.equals("multi")?"multi":"solo";
            saves.addAll(WorldCreationConfig.list().stream()
                .filter(w->w.hostType().equals(kind)).toList());
            if(saves.isEmpty()){note("No existing "+kind+" worlds; create a new one.");return;}
            loadOffset=0;gotoScreen(Screen.LOAD);
        }catch(IOException invalid){note("Cannot list saved worlds: "+invalid.getMessage());}
    }
    private void createWorld(){
        try{
            long seed=seedText.isBlank()?new SecureRandom().nextLong():
                Long.parseLong(seedText.trim());
            String type=mode.equals("multi")?"multi":"solo";
            WorldCreationConfig config=new WorldCreationConfig(worldName.trim(),seed,
                hardcore,type,enemy,loot,type.equals("multi")?capacity:1,ai);
            config.saveNew();
            chosenWorld=config;
            note("Created persistent world: "+config.worldName());
            afterWorld();
        }catch(IOException|IllegalArgumentException invalid){
            note("Cannot create world: "+invalid.getMessage());
        }
    }
    private void afterWorld(){
        if(mode.equals("editor")){
            beginEditor(chosenWorld.seed());return;
        }
        gotoScreen(Screen.FACTION);
    }
    private void startGame(FactionClasses.Role role){
        if(screen==Screen.CONNECTING)return;
        try{
            final int port=mode.equals("join")?Integer.parseInt(portText):0;
            MaleficVeil.GpuLaunch request=new MaleficVeil.GpuLaunch(
                mode.equals("join")?null:chosenWorld,
                mode.equals("join")?address:null,port,faction,role);
            gotoScreen(Screen.CONNECTING);
            note("Connecting to authoritative world...");
            Thread worker=new Thread(()->{
                try{
                    final MaleficVeil[] result={null};
                    SwingUtilities.invokeAndWait(()->{
                        try{result[0]=MaleficVeil.createGpuSession(request);}
                        catch(IOException failure){throw new RuntimeException(failure);}
                    });
                    ready=result[0];
                }catch(Exception failure){failed=failure;}
            },"veil-session-bootstrap");
            worker.setDaemon(true);worker.start();
        }catch(RuntimeException invalid){note("Cannot start game: "+invalid.getMessage());}
    }
    private void beginEditor(long seed){
        switchView(GpuKenneyPreview.editableWorld(seed,false));
    }
    private void switchView(GpuKenneyPreview view){
        if(audio!=null)audio.inGame();
        if(backdrop!=null){backdrop.dispose();backdrop=null;}
        if(skin!=null){skin.close();skin=null;}
        if(ui!=null){ui.dispose();ui=null;}
        gameRenderer=view;
        gameRenderer.clientSettings(settings);
        gameRenderer.audioBus(audio);
        gameRenderer.create();
        Gdx.input.setInputProcessor(null);
        Gdx.input.setCursorCatched(false);
    }
    private void changeView(int step){
        settings.distance=Math.max(45,Math.min(240,settings.distance+step));
        backdrop.clientSettings(settings);rebuild(false);
    }
    private void changeFov(int step){
        settings.fieldOfView=Math.max(55,Math.min(100,settings.fieldOfView+step));
        backdrop.clientSettings(settings);rebuild(false);
    }
    private void changeLights(int step){
        settings.lights=Math.max(0,Math.min(8,settings.lights+step));
        backdrop.clientSettings(settings);rebuild(false);
    }
    private void changeWindow(int step){
        settings.windowSize=Math.floorMod(settings.windowSize+step,
            GpuClientSettings.WINDOWS.length);
        settings.fullscreen=false;applyVideo();rebuild(false);
    }
    private void toggleFullscreen(){settings.fullscreen=!settings.fullscreen;applyVideo();rebuild(false);}
    private void toggleVsync(){settings.vsync=!settings.vsync;applyVideo();rebuild(false);}
    private void applyVideo(){
        Gdx.graphics.setVSync(settings.vsync);
        if(settings.fullscreen)Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        else{
            int[] size=GpuClientSettings.WINDOWS[settings.windowSize];
            Gdx.graphics.setWindowedMode(size[0],size[1]);
        }
    }
    private void cycleEnemy(int direction){
        List<String> options=List.of("low","normal","high");
        enemy=options.get(Math.floorMod(options.indexOf(enemy)+direction,options.size()));
        rebuild(false);
    }
    private void cycleLoot(int direction){
        List<String> options=List.of("scarce","normal","abundant");
        loot=options.get(Math.floorMod(options.indexOf(loot)+direction,options.size()));
        rebuild(false);
    }
    private void stepAi(int direction){ai=Math.max(0,Math.min(24,ai+direction));rebuild(false);}
    private void stepCap(int direction){capacity=Math.max(2,Math.min(32,capacity+direction));rebuild(false);}
    private void sound(String key,int direction){
        int value=switch(key){
            case "master"->settings.master;case "music"->settings.music;
            case "effects"->settings.effects;case "ambience"->settings.ambience;
            case "ui"->settings.ui;case "voice"->settings.voice;
            default->throw new IllegalArgumentException(key);
        };
        value=Math.max(0,Math.min(100,value+direction));
        switch(key){
            case "master"->settings.master=value;case "music"->settings.music=value;
            case "effects"->settings.effects=value;case "ambience"->settings.ambience=value;
            case "ui"->settings.ui=value;case "voice"->settings.voice=value;
        }
        if(audio!=null)audio.apply(settings);
        rebuild(false);
    }
    private void saveSettings(){
        try{
            settings.save();
            note("Client options saved locally.");
            gotoScreen(backFromOptions);
        }catch(IOException|IllegalArgumentException invalid){
            note("Cannot save options: "+invalid.getMessage());
        }
    }
    private void back(){
        switch(screen){
            case HOME -> {}
            case WORLDS,JOIN,CREDITS -> gotoScreen(Screen.HOME);
            case CREATE,LOAD -> gotoScreen(Screen.WORLDS);
            case FACTION -> gotoScreen(mode.equals("join")?Screen.JOIN:Screen.WORLDS);
            case CLASS -> gotoScreen(Screen.FACTION);
            case AUDIO,CONTROLS -> gotoScreen(Screen.VIDEO);
            case VIDEO -> saveSettings();
            case CONNECTING -> note("World connection in progress.");
        }
    }
    private void activate(int direction){
        if(entries.isEmpty())return;
        if(audio!=null)audio.click();
        Entry entry=entries.get(selected);
        if(direction<0&&entry.previous()!=null)entry.previous().run();
        else if(direction>0&&entry.next()!=null)entry.next().run();
        else entry.action().run();
    }
    private void input(){
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)){back();return;}
        if(Gdx.input.isKeyJustPressed(Input.Keys.UP))
            selected=Math.floorMod(selected-1,Math.max(1,entries.size()));
        if(Gdx.input.isKeyJustPressed(Input.Keys.DOWN)||
            Gdx.input.isKeyJustPressed(Input.Keys.TAB))
            selected=Math.floorMod(selected+1,Math.max(1,entries.size()));
        if(Gdx.input.isKeyJustPressed(Input.Keys.LEFT))activate(-1);
        if(Gdx.input.isKeyJustPressed(Input.Keys.RIGHT))activate(1);
        if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER))activate(0);
        if(Gdx.input.justTouched()){
            float px=Gdx.input.getX()*W/(float)Gdx.graphics.getWidth();
            float py=H-Gdx.input.getY()*H/(float)Gdx.graphics.getHeight();
            for(int i=0;i<entries.size();i++){
                int baseline=START_Y-i*ROW;
                if(px>=37&&px<426&&py>=baseline&&py<baseline+22){
                    selected=i;activate(0);break;
                }
            }
        }
    }
    private void draw(){
        ui.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0,W,H));
        ui.begin();
        skin.frame(ui,16,21,424,360);
        skin.band(ui,27,332,400,35);
        skin.font().draw(ui,"THE MALEFIC VEIL",38,355);
        skin.font().draw(ui,switch(screen){
            case HOME -> "THE CITY IS WATCHING";
            case WORLDS -> mode.equals("multi")?"HOST A WORLD":"WORLD SELECTION";
            case CREATE -> "WORLD RULES / PERSISTENT";
            case LOAD -> "SAVED WORLDS";
            case JOIN -> "JOIN LAN GAME";
            case FACTION -> "FACTION";
            case CLASS -> faction.display.toUpperCase(Locale.ROOT)+" / CLASS";
            case VIDEO -> "OPTIONS / VIDEO";
            case AUDIO -> "OPTIONS / SOUND";
            case CONTROLS -> "OPTIONS / CONTROLS";
            case CREDITS -> "CREDITS";
            case CONNECTING -> "WORLD LOADING";
        },38,335);
        for(int i=0;i<entries.size();i++){
            String label=entries.get(i).label();
            if(label.length()>49)label=label.substring(0,48)+"~";
            skin.choice(ui,label,35,START_Y-i*ROW,390,22,selected==i);
        }
        skin.band(ui,17,22,422,29);
        String foot=message.length()>57?message.substring(0,55)+"~":message;
        skin.font().draw(ui,foot,32,43);
        skin.font().draw(ui,"ARROWS/TAB   ENTER   ESC BACK   CLICK/TYPE",32,29);
        if(screen==Screen.CREDITS){
            skin.font().draw(ui,"HEYHEYTHERE  /  CC BY 4.0",454,80);
        }
        ui.end();
    }
    @Override public void render(){
        if(gameRenderer!=null){
            gameRenderer.render();
            if(flowSmoke&&++frames==110){
                MaleficVeil.GpuFrame frame=readyGame.gpuFrame();
                if(frame==null||!frame.connected())
                    throw new IllegalStateException("In-window world launch not connected");
                Pixmap pixels=Pixmap.createFromFrameBuffer(0,0,
                    Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
                var out=Gdx.files.absolute(new java.io.File(
                    "build/test-visuals/gpu-frontend-live-world.png").getAbsolutePath());
                out.parent().mkdirs();PixmapIO.writePNG(out,pixels);pixels.dispose();
                System.out.println("PASS GPU_UNIFIED_FRONTEND_FULL_FLOW seed="+
                    readyGame.gpuWorld().seed+" world="+readyGame.gpuFrame().connected()+
                    " faction="+faction+" role=WARDEN source=authoritative-lan");
                Gdx.app.exit();
            }
            return;
        }
        if(ready!=null){
            MaleficVeil session=ready;ready=null;
            readyGame=session;
            switchView(new GpuKenneyPreview(session,false));
            System.out.println("PASS GPU_UNIFIED_FRONTEND_SESSION seed="+session.gpuWorld().seed);
            gameRenderer.render();return;
        }
        if(failed!=null){
            Throwable why=failed;failed=null;
            gotoScreen(Screen.CLASS);
            note("Cannot join/start world: "+why.getCause());
        }
        input();
        backdrop.render();
        draw();
        if(flowSmoke){
            frames++;
            if(frames==18){
                worldMode("solo");
                gotoScreen(Screen.CREATE);
                worldName="UIFlow"+Long.toUnsignedString(System.nanoTime(),36);
                seedText="20261020";
                createWorld();
                if(screen!=Screen.FACTION)throw new IllegalStateException(
                    "World create did not advance to faction selection");
                faction=CharacterRoster.Faction.THE_UNRAVELED;
                gotoScreen(Screen.CLASS);
                startGame(FactionClasses.faction(faction).stream().filter(
                    FactionClasses.Role::playable).findFirst().orElseThrow());
            }
        }
        if(smoke&&++frames==65){
            if(skin.sourcedDraws()<150)throw new IllegalStateException("PSX scene menu not drawn");
            Pixmap pix=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
            var dst=Gdx.files.absolute(new java.io.File(
                "build/test-visuals/gpu-unified-front-end.png").getAbsolutePath());
            dst.parent().mkdirs();PixmapIO.writePNG(dst,pix);pix.dispose();
            System.out.println("PASS GPU_UNIFIED_FRONTEND_MENU camera=animated-city ui="+
                skin.sourcedDraws()+" state="+screen+" resolution="+Gdx.graphics.getWidth()+
                "x"+Gdx.graphics.getHeight());
            Gdx.app.exit();
        }
    }
    @Override public void dispose(){
        if(gameRenderer!=null)gameRenderer.dispose();
        if(backdrop!=null)backdrop.dispose();
        if(skin!=null)skin.close();
        if(ui!=null)ui.dispose();
        if(audio!=null)audio.close();
    }
}
