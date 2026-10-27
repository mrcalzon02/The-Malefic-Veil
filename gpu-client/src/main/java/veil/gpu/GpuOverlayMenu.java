package veil.gpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import veil.MaleficVeil;

import java.io.IOException;
import java.util.*;

/**
 * Compact, source-skinned menus inside the actual world framebuffer. No
 * native Swing dialogs. Crew and crafting commands use existing host RPCs,
 * never client-side item minting or unilateral faction changes.
 */
public final class GpuOverlayMenu {
    public enum Screen { GAME,PAUSE,VIDEO,AUDIO,CONTROLS,CRAFT,INDUSTRY,SITES,WORK_ORDERS,STORAGE,BOOK,CREW,FACTION,CREW_INPUT,CONFIRM_DISBAND }
    private static final int WIDTH=640,HEIGHT=400,MAX_RECIPES_PER_PAGE=4;
    private record Item(String label,Runnable action,Runnable prev,Runnable next){}
    private final PsxHorrorMenu skin;
    private final GpuCraftingBook book;
    private final MaleficVeil gameplay;
    private final GpuClientSettings settings;
    private final Runnable goMain,apply;
    private final GpuAudioBus audio;
    private final List<Item> items=new ArrayList<>();
    private final InputAdapter inputProcessor=new InputAdapter(){
        @Override public boolean keyTyped(char character){
            if(screen==Screen.BOOK)return book.type(character);
            if(screen!=Screen.CREW_INPUT)return false;
            if(character=='\b'||character==127){
                if(!crewText.isEmpty())crewText=crewText.substring(0,crewText.length()-1);
                return true;
            }
            if(character>=32&&character<=126&&crewText.length()<32){
                crewText+=character;
                return true;
            }
            return false;
        }
    };
    private Screen screen=Screen.GAME;
    private int selected,pageOffset;
    private String notice="",crewAction="",crewText="";
    public GpuOverlayMenu(PsxHorrorMenu skin,MaleficVeil game,
            GpuClientSettings settings,GpuAudioBus audio,
            Runnable goMain,Runnable apply){
        this.skin=Objects.requireNonNull(skin);
        this.gameplay=Objects.requireNonNull(game);
        this.book=new GpuCraftingBook(skin,this.gameplay::gpuCraftFaction);
        this.settings=Objects.requireNonNull(settings);
        this.audio=audio;this.goMain=goMain;this.apply=apply;
    }
    public InputAdapter inputProcessor(){return inputProcessor;}
    public Screen screen(){return screen;}
    public boolean active(){return screen==Screen.GAME;}
    public void openCraft(){pageOffset=0;show(Screen.CRAFT);}
    public void openSites(){pageOffset=0;show(Screen.SITES);}
    public void openWorkOrders(){pageOffset=0;show(Screen.WORK_ORDERS);}
    public void openStorage(){
        pageOffset=0;
        gameplay.gpuRequestCampStock();
        show(Screen.STORAGE);
    }
    public void openBook(){book.reset();show(Screen.BOOK);}
    public void openCrew(){show(Screen.CREW);}
    /** Existing game simulation stays live for clients; only local input pauses. */
    private void show(Screen target){
        screen=target;selected=0;
        Gdx.input.setCursorCatched(false);
        build();
    }
    /** CI-only visible-state proof; production uses ESC/F11. */
    void auditPause(boolean paused){show(paused?Screen.PAUSE:Screen.GAME);}
    void auditPanel(String name){
        switch(name){
            case "craft"->openCraft();
            case "book"->{
                openBook();book.inspectRecipe("hand:camp-ration");
            }
            case "book-industrial"->{
                openBook();book.inspectRecipe("process:tracking-electronics");
            }
            case "book-item"->{
                openBook();book.inspectItem("TIMBER");
            }
            case "book-penitent"->{
                openBook();book.auditFactionRecipe("process:penitent-sanctify-shell",
                    veil.CraftingEraRegistry.Faction.PENITENT);
            }
            case "book-begotten"->{
                openBook();book.auditFactionRecipe("process:begotten-pulse-organ",
                    veil.CraftingEraRegistry.Faction.BEGOTTEN);
            }
            case "crew"->openCrew();
            case "crew-input"->beginCrewInput("CREATE");
            default->throw new IllegalArgumentException("Unknown menu proof: "+name);
        }
    }
    private void add(String label,Runnable action){items.add(new Item(label,action,null,null));}
    private void slider(String label,Runnable before,Runnable after){
        items.add(new Item(label,after,before,after));
    }
    private void toast(String feedback){
        notice=feedback.length()>48?feedback.substring(0,47)+"~":feedback;
    }
    private void quickPanel(int key){
        gameplay.gpuKey(key,true);gameplay.gpuKey(key,false);
        show(Screen.GAME);
    }
    private void save(){
        try{settings.save();toast("OPTIONS SAVED");show(Screen.PAUSE);}
        catch(IOException failure){toast("SAVE FAILED: "+failure.getMessage());}
    }
    private void adjust(String key,int diff){
        int value=switch(key){
            case "master"->settings.master;case "music"->settings.music;
            case "effects"->settings.effects;case "ambience"->settings.ambience;
            case "ui"->settings.ui;case "voice"->settings.voice;
            default->throw new IllegalArgumentException(key);
        };
        value=Math.max(0,Math.min(100,value+diff));
        switch(key){
            case "master"->settings.master=value;case "music"->settings.music=value;
            case "effects"->settings.effects=value;case "ambience"->settings.ambience=value;
            case "ui"->settings.ui=value;case "voice"->settings.voice=value;
        }
        if(audio!=null)audio.apply(settings);
        build();
    }
    private void changeVideo(String key,int direction){
        switch(key){
            case "distance"->settings.distance=Math.max(45,
                Math.min(240,settings.distance+direction*10));
            case "fov"->settings.fieldOfView=Math.max(55,
                Math.min(100,settings.fieldOfView+direction*5));
            case "lights"->settings.lights=Math.max(0,Math.min(8,settings.lights+direction));
            case "vsync"->{settings.vsync=!settings.vsync;Gdx.graphics.setVSync(settings.vsync);}
            case "fullscreen"->{
                settings.fullscreen=!settings.fullscreen;
                if(settings.fullscreen)Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                else{
                    int[] size=GpuClientSettings.WINDOWS[settings.windowSize];
                    Gdx.graphics.setWindowedMode(size[0],size[1]);
                }
            }
            default->throw new IllegalArgumentException(key);
        }
        apply.run();build();
    }
    private List<MaleficVeil.GpuCraftRecipe> crafts(){
        MaleficVeil.GpuFrame frame=gameplay.gpuFrame();
        return frame==null?List.of():frame.crafts();
    }
    private void beginCrewInput(String action){
        crewAction=action;crewText="";show(Screen.CREW_INPUT);
    }
    private void submitCrew(){
        try{
            gameplay.gpuCrewAction(crewAction,crewText);
            show(Screen.CREW);
            toast("REQUESTED HOST "+crewAction);
        }catch(IllegalArgumentException|IllegalStateException failure){
            toast(failure.getMessage());
        }
    }
    private void crewImmediate(String action){
        try{
            gameplay.gpuCrewAction(action,"");
            show(Screen.CREW);
            toast("REQUESTED HOST "+action);
        }catch(IllegalArgumentException|IllegalStateException failure){
            toast(failure.getMessage());
        }
    }
    private void build(){
        items.clear();
        switch(screen){
            case GAME->{}
            case PAUSE->{
                add("RESUME GAME",()->show(Screen.GAME));
                add("INVENTORY",()->quickPanel(java.awt.event.KeyEvent.VK_I));
                add("CHARACTER / SKILLS",()->quickPanel(java.awt.event.KeyEvent.VK_K));
                add("CITY / CONTINENT MAP",()->quickPanel(java.awt.event.KeyEvent.VK_M));
                add("EAT CARRIED FOOD",()->{gameplay.gpuUseSupplies(false);toast("HOST FOOD REQUEST");});
                add("DRINK CLEAN WATER",()->{gameplay.gpuUseSupplies(true);toast("HOST WATER REQUEST");});
                add("FIELD CRAFTING",this::openCraft);
                add("PERMANENT CAMP CRATE",this::openStorage);
                add("RECIPE / INGREDIENT INDEX",this::openBook);
                add("FACTION / CREW",this::openCrew);
                add("VIDEO / GRAPHICS",()->show(Screen.VIDEO));
                add("SOUND / CONTROLS",()->show(Screen.AUDIO));
                add("RETURN TO MAIN MENU",goMain);
            }
            case VIDEO->{
                slider("VIEW DISTANCE "+settings.distance+" M",
                    ()->changeVideo("distance",-1),()->changeVideo("distance",1));
                slider("FIELD OF VIEW "+settings.fieldOfView+" DEG",
                    ()->changeVideo("fov",-1),()->changeVideo("fov",1));
                slider("LOCAL LIGHTS "+settings.lights,
                    ()->changeVideo("lights",-1),()->changeVideo("lights",1));
                slider("VSYNC "+(settings.vsync?"ON":"OFF"),
                    ()->changeVideo("vsync",0),()->changeVideo("vsync",0));
                slider("FULLSCREEN "+(settings.fullscreen?"YES":"NO"),
                    ()->changeVideo("fullscreen",0),()->changeVideo("fullscreen",0));
                add("CONTROLS",()->show(Screen.CONTROLS));
                add("SAVE / BACK",this::save);
            }
            case AUDIO->{
                slider("MASTER "+settings.master+"%",()->adjust("master",-5),()->adjust("master",5));
                slider("MUSIC "+settings.music+"%",()->adjust("music",-5),()->adjust("music",5));
                slider("EFFECTS "+settings.effects+"%",()->adjust("effects",-5),()->adjust("effects",5));
                slider("AMBIENCE "+settings.ambience+"%",()->adjust("ambience",-5),()->adjust("ambience",5));
                slider("UI "+settings.ui+"%",()->adjust("ui",-5),()->adjust("ui",5));
                slider("VOICE "+settings.voice+"%",()->adjust("voice",-5),()->adjust("voice",5));
                add("CONTROLS",()->show(Screen.CONTROLS));
                add("SAVE / BACK",this::save);
            }
            case CONTROLS->{
                slider("MOUSE LOOK "+settings.sensitivity+"%",
                    ()->{settings.sensitivity=Math.max(20,settings.sensitivity-10);build();},
                    ()->{settings.sensitivity=Math.min(200,settings.sensitivity+10);build();});
                slider("INVERT Y "+(settings.invertY?"ON":"OFF"),
                    ()->{settings.invertY=!settings.invertY;build();},
                    ()->{settings.invertY=!settings.invertY;build();});
                add("SAVE / BACK",this::save);
            }
            case CRAFT->{
                List<MaleficVeil.GpuCraftRecipe> available=crafts();
                int end=Math.min(available.size(),pageOffset+MAX_RECIPES_PER_PAGE);
                for(int i=pageOffset;i<end;i++){
                    var recipe=available.get(i);
                    add((recipe.affordable()?"CRAFT ":"NEED ")+recipe.id().toUpperCase(Locale.ROOT),
                        ()->{
                            if(!recipe.affordable()){toast(recipe.description());return;}
                            gameplay.gpuCraft(recipe.id());
                            toast("HOST CRAFT REQUEST: "+recipe.id());
                        });
                }
                if(pageOffset>0)add("PREVIOUS RECIPES",()->{
                    pageOffset=Math.max(0,pageOffset-MAX_RECIPES_PER_PAGE);show(Screen.CRAFT);
                });
                if(end<available.size())add("MORE RECIPES",()->{
                    pageOffset+=MAX_RECIPES_PER_PAGE;show(Screen.CRAFT);
                });
                add("FULL RECIPE INDEX",this::openBook);
                add("FACTION INDUSTRY",()->show(Screen.INDUSTRY));
                add("DEPLOY CRAFTED WORKBENCH",()->{
                    gameplay.gpuPlaceWorkbench();show(Screen.GAME);
                });
                add("BACK TO PAUSE",()->show(Screen.PAUSE));
            }
            case INDUSTRY->{
                add("CONSTRUCT FACTION WORKSITE",()->{
                    gameplay.gpuPlaceFactionFactory();show(Screen.GAME);
                });
                add("START FACTION WORK ORDER",()->{
                    gameplay.gpuStartFactionBatch();show(Screen.GAME);
                });
                add("INSPECT WORKSITE PROGRESS",gameplay::gpuInspectFactionBatch);
                add("BUILD SHARED / E2 FACILITIES",this::openSites);
                add("SELECT INDUSTRIAL RECIPE",this::openWorkOrders);
                add("DEPLOY CRAFTED CRATE",()->{
                    gameplay.gpuPlaceStorage();show(Screen.GAME);
                });
                add("CAMP CRATE INVENTORY",this::openStorage);
                add("FULL INDUSTRIAL RECIPE INDEX",this::openBook);
                add("BACK TO CRAFTING",this::openCraft);
            }
            case SITES->{
                var ids=gameplay.gpuSupportedFacilities();
                int end=Math.min(ids.size(),pageOffset+5);
                for(int i=pageOffset;i<end;i++){
                    String id=ids.get(i);
                    add("BUILD "+id.toUpperCase(java.util.Locale.ROOT),()->{
                        gameplay.gpuPlaceFacility(id);
                        show(Screen.GAME);
                    });
                }
                if(pageOffset>0)add("PREVIOUS FACILITIES",()->{
                    pageOffset=Math.max(0,pageOffset-5);show(Screen.SITES);
                });
                if(end<ids.size())add("MORE FACILITIES",()->{
                    pageOffset+=5;show(Screen.SITES);
                });
                add("BACK TO INDUSTRY",()->show(Screen.INDUSTRY));
            }
            case WORK_ORDERS->{
                var ids=gameplay.gpuSupportedProcesses();
                int end=Math.min(ids.size(),pageOffset+5);
                for(int i=pageOffset;i<end;i++){
                    String id=ids.get(i);
                    add("RUN "+id.toUpperCase(java.util.Locale.ROOT),()->{
                        gameplay.gpuStartFactionBatch(id);
                        show(Screen.GAME);
                    });
                }
                if(pageOffset>0)add("PREVIOUS WORK ORDERS",()->{
                    pageOffset=Math.max(0,pageOffset-5);show(Screen.WORK_ORDERS);
                });
                if(end<ids.size())add("MORE WORK ORDERS",()->{
                    pageOffset+=5;show(Screen.WORK_ORDERS);
                });
                add("BACK TO INDUSTRY",()->show(Screen.INDUSTRY));
            }
            case STORAGE->{
                var carried=new java.util.TreeMap<>(gameplay.gpuCarriedItems());
                var stored=new java.util.TreeMap<>(gameplay.gpuCampStock());
                java.util.List<String[]> options=new java.util.ArrayList<>();
                carried.forEach((item,n)->{if(n>0)options.add(new String[]{"STORE",item,Integer.toString(n)});});
                stored.forEach((item,n)->{if(n>0)options.add(new String[]{"TAKE",item,Integer.toString(n)});});
                int end=Math.min(options.size(),pageOffset+7);
                for(int i=pageOffset;i<end;i++){
                    var option=options.get(i);
                    boolean take=option[0].equals("TAKE");
                    add(option[0]+" "+option[1]+" x"+option[2],()->{
                        gameplay.gpuTransferCampStock(take,option[1],1);
                        toast("HOST "+option[0]+" REQUEST");
                    });
                }
                if(pageOffset>0)add("PREVIOUS MATERIALS",()->{
                    pageOffset=Math.max(0,pageOffset-7);show(Screen.STORAGE);
                });
                if(end<options.size())add("MORE MATERIALS",()->{
                    pageOffset+=7;show(Screen.STORAGE);
                });
                add("REFRESH CAMP STOCK",()->{gameplay.gpuRequestCampStock();build();});
                add("BACK TO PAUSE",()->show(Screen.PAUSE));
            }
            case BOOK->{} // Owns its list and detail navigation; never crafts items.
            case CREW->{
                if(gameplay.gpuCraftFaction()==veil.CraftingEraRegistry.Faction.ALL)
                    add("CHOOSE PERMANENT FACTION",()->show(Screen.FACTION));
                if(gameplay.gpuHasNetworkCrew()){
                add("CREATE CREW",()->beginCrewInput("CREATE"));
                add("INVITE PLAYER ID",()->beginCrewInput("INVITE"));
                add("ACCEPT INVITATION",()->crewImmediate("ACCEPT"));
                add("PROMOTE OFFICER",()->beginCrewInput("PROMOTE"));
                add("REMOVE MEMBER",()->beginCrewInput("REMOVE"));
                add("TRANSFER LEADERSHIP",()->beginCrewInput("TRANSFER"));
                add("LEAVE CREW",()->crewImmediate("LEAVE"));
                add("DISBAND CREW",()->show(Screen.CONFIRM_DISBAND));
                }
                add("BACK",()->show(Screen.PAUSE));
            }
            case FACTION->{
                if(gameplay.gpuCraftFaction()==veil.CraftingEraRegistry.Faction.ALL){
                    add("JOIN UNRAVELED",()->{
                        gameplay.gpuEnrollFaction(veil.CharacterRoster.Faction.THE_UNRAVELED);
                        show(Screen.GAME);
                    });
                    add("JOIN PENITENT",()->{
                        gameplay.gpuEnrollFaction(veil.CharacterRoster.Faction.THE_PENITENT);
                        show(Screen.GAME);
                    });
                    add("JOIN BEGOTTEN",()->{
                        gameplay.gpuEnrollFaction(veil.CharacterRoster.Faction.THE_BEGOTTEN);
                        show(Screen.GAME);
                    });
                }
                add("BACK TO CREW / FACTION",()->show(Screen.CREW));
            }
            case CREW_INPUT->{
                add("SUBMIT "+crewAction,this::submitCrew);
                add("CANCEL",()->show(Screen.CREW));
            }
            case CONFIRM_DISBAND->{
                add("NO / KEEP CREW",()->show(Screen.CREW));
                add("YES / DISBAND CREW",()->crewImmediate("DISBAND"));
            }
        }
        selected=Math.min(selected,Math.max(0,items.size()-1));
    }
    private void trigger(int change){
        if(items.isEmpty())return;
        if(audio!=null)audio.click();
        Item item=items.get(selected);
        if(change<0&&item.prev()!=null)item.prev().run();
        else if(change>0&&item.next()!=null)item.next().run();
        else item.action().run();
    }
    /** Return true only when world movement/shooting input is permitted. */
    public boolean input(){
        if(screen==Screen.BOOK){
            if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)||
               Gdx.input.isKeyJustPressed(Input.Keys.F11)){
                show(Screen.PAUSE);
            }else book.input();
            Gdx.input.setCursorCatched(false);
            return false;
        }
        if(screen==Screen.GAME&&Gdx.input.isKeyJustPressed(Input.Keys.F2))
            openBook();
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)||
            Gdx.input.isKeyJustPressed(Input.Keys.F11)){
            if(screen==Screen.GAME)show(Screen.PAUSE);
            else if(screen==Screen.PAUSE)show(Screen.GAME);
            else if(screen==Screen.CREW_INPUT||screen==Screen.CONFIRM_DISBAND||
                    screen==Screen.FACTION)show(Screen.CREW);
            else show(Screen.PAUSE);
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.F10))
            show(screen==Screen.VIDEO?Screen.GAME:Screen.VIDEO);
        if(screen==Screen.GAME)return true;
        if(screen==Screen.STORAGE)build();
        if(Gdx.input.isKeyJustPressed(Input.Keys.UP))
            selected=Math.floorMod(selected-1,Math.max(1,items.size()));
        if(Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
            selected=Math.floorMod(selected+1,Math.max(1,items.size()));
        if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER))trigger(0);
        if(screen!=Screen.CREW_INPUT){
            if(Gdx.input.isKeyJustPressed(Input.Keys.LEFT))trigger(-1);
            if(Gdx.input.isKeyJustPressed(Input.Keys.RIGHT))trigger(1);
        }
        if(Gdx.input.justTouched()){
            float px=Gdx.input.getX()*WIDTH/Gdx.graphics.getWidth();
            float py=HEIGHT-Gdx.input.getY()*HEIGHT/Gdx.graphics.getHeight();
            if(px>=144&&px<=504)
                for(int i=0;i<items.size();i++){
                    float bottom=297-i*24;
                    if(py>=bottom&&py<bottom+22){selected=i;trigger(0);break;}
                }
        }
        Gdx.input.setCursorCatched(false);
        return false;
    }
    /** Draw using the identical licensed source panel/button sprites as launcher. */
    public void dispose(){book.close();}
    public void draw(SpriteBatch sprites){
        if(screen==Screen.GAME)return;
        if(screen==Screen.BOOK){
            book.draw(sprites,gameplay.gpuFrame());
            return;
        }
        skin.frame(sprites,120,30,408,345);
        skin.band(sprites,137,334,375,30);
        skin.font().draw(sprites,"THE MALEFIC VEIL / "+screen,149,354);
        if(screen==Screen.CREW||screen==Screen.CREW_INPUT){
            String status=gameplay.gpuCrewStatus();
            if(status.length()>43)status=status.substring(0,42)+"~";
            skin.font().draw(sprites,status,147,331);
        }else if(screen==Screen.CRAFT){
            skin.font().draw(sprites,"HOST VALIDATES INGREDIENTS / LOCATION",147,331);
         }else if(screen==Screen.INDUSTRY){
            skin.font().draw(sprites,"SEPARATE FACTION SYSTEMS / LIVE HOST ACTIONS",147,331);
        }else if(screen==Screen.SITES||screen==Screen.WORK_ORDERS){
            skin.font().draw(sprites,
                "HOST CHECKS REAL COSTS / LOCATION / DEPENDENCIES",147,331);
        }else if(screen==Screen.STORAGE){
            skin.font().draw(sprites,"STAND BY YOUR OWN CRATE / ONE UNIT PER ACTION",147,331);
        }
        for(int i=0;i<items.size();i++){
            float row=297-i*24;
            skin.choice(sprites,items.get(i).label(),143,row,364,22,selected==i);
            if(screen==Screen.CRAFT){
                List<MaleficVeil.GpuCraftRecipe> recipes=crafts();
                int source=pageOffset+i;
                if(source<recipes.size()&&i<MAX_RECIPES_PER_PAGE){
                    String id=recipes.get(source).id();
                    var index=veil.CraftingRecipeIndex.recipe(id.startsWith("field:")?
                        "throw:"+id.substring(6):"hand:"+id);
                    if(index!=null)book.drawIcon(sprites,index.output(),146,row+1,19);
                }
            }
        }
        if(screen==Screen.CREW_INPUT){
            skin.panel(sprites,143,147,365,34);
            skin.font().draw(sprites,crewAction+" > "+crewText+"_",152,169);
        }
        skin.band(sprites,135,39,379,24);
        skin.font().draw(sprites,notice.isBlank()?"ESC BACK  ARROWS / ENTER  MOUSE":notice,147,55);
    }
}
