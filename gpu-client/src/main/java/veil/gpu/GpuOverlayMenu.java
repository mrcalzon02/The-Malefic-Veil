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
    public enum Screen { GAME,PAUSE,VIDEO,AUDIO,CONTROLS,CRAFT,INDUSTRY,PRODUCTION_OVERVIEW,FACTORY_STATUS,UTILITIES,CONVEYOR,CONVEYOR_STATUS,SITES,WORK_ORDERS,QUEUE_ORDERS,STORAGE,BOOK,CREW,FACTION,CREW_INPUT,CONFIRM_DISBAND }
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
    private boolean conveyorReverse,repeatOrders;
    /** One / ten / full stack; host still validates actual item and crate capacity. */
    private int stockTransferSize=1;
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
    public void openConveyor(){
        pageOffset=0;conveyorReverse=false;
        gameplay.gpuRequestCampStock();show(Screen.CONVEYOR);
    }
    public void openWorkOrders(){pageOffset=0;show(Screen.WORK_ORDERS);}
    public void openFactoryInspection(){
        gameplay.gpuRequestFactoryInspection();
        show(Screen.FACTORY_STATUS);
    }
    public void openConveyorInspection(){
        gameplay.gpuRequestConveyorInspection();
        show(Screen.CONVEYOR_STATUS);
    }
    public void openProductionOverview(){
        pageOffset=0;
        gameplay.gpuRequestProductionOverview(pageOffset);
        show(Screen.PRODUCTION_OVERVIEW);
    }
    public void openStorage(){
        pageOffset=0;
        gameplay.gpuRequestCampStock();
        show(Screen.STORAGE);
    }
    public void openBook(){book.reset();show(Screen.BOOK);}
    public void openCrew(){show(Screen.CREW);}
    /** Existing game simulation stays live for clients; only local input pauses. */
    private void show(Screen target){
        if(audio!=null&&screen!=target)audio.panel();
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
        if(audio!=null){
            String signal=feedback.toUpperCase(Locale.ROOT);
            if(signal.contains("FAIL")||signal.contains("NEED ")||
               signal.contains("CANNOT")||signal.contains("FIRST PAGE")||
               signal.contains("END OF")||signal.contains("AWAITING")||
               signal.contains("INVALID"))audio.reject();
            else if(signal.startsWith("OPTIONS SAVED")||
                    signal.startsWith("SAVED "))audio.confirm();
            // A request sent to the host is not confirmation of success.
            // The published host notice drives definitive positive feedback.
        }
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
            case "window"->{
                settings.windowSize=Math.floorMod(
                    settings.windowSize+direction,GpuClientSettings.WINDOWS.length);
                settings.fullscreen=false;
                int[] size=GpuClientSettings.WINDOWS[settings.windowSize];
                Gdx.graphics.setWindowedMode(size[0],size[1]);
            }
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
                slider("WINDOW RESOLUTION "+
                    GpuClientSettings.WINDOWS[settings.windowSize][0]+"x"+
                    GpuClientSettings.WINDOWS[settings.windowSize][1],
                    ()->changeVideo("window",-1),()->changeVideo("window",1));
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
                add("INSPECT WORKSITE PROGRESS",this::openFactoryInspection);
                add("SETTLEMENT PRODUCTION OVERVIEW",this::openProductionOverview);
                add("POWER UTILITIES",()->show(Screen.UTILITIES));
                add("CONFIGURE CONVEYOR",this::openConveyor);
                add("INSPECT CONVEYOR LOGISTICS",this::openConveyorInspection);
                add("BUILD SHARED / E3 FACILITIES",this::openSites);
                add("SELECT INDUSTRIAL RECIPE",this::openWorkOrders);
                add("DEPLOY CRAFTED CRATE",()->{
                    gameplay.gpuPlaceStorage();show(Screen.GAME);
                });
                add("CAMP CRATE INVENTORY",this::openStorage);
                // The full recipe index remains in the Crafting menu;
                // keep this crowded industry panel within the framebuffer.
                add("BACK TO CRAFTING",this::openCraft);
            }
            case PRODUCTION_OVERVIEW->{
                add("REFRESH PRODUCTION REPORT",()->{
                    gameplay.gpuRequestProductionOverview(pageOffset);
                });
                add("PREVIOUS FOUR FACILITIES",()->{
                    if(pageOffset==0){toast("FIRST PAGE");return;}
                    pageOffset--;
                    gameplay.gpuRequestProductionOverview(pageOffset);
                    build();
                });
                add("NEXT FOUR FACILITIES",()->{
                    var report=gameplay.gpuProductionOverview(pageOffset);
                    if(report==null){toast("AWAITING HOST REPORT");return;}
                    if((pageOffset+1)*veil.ArenaSystems.OVERVIEW_PAGE_SIZE>=report.total()){
                        toast("END OF OWNED FACILITIES");return;
                    }
                    pageOffset++;
                    gameplay.gpuRequestProductionOverview(pageOffset);
                    build();
                });
                add("RETURN TO INDUSTRY",()->show(Screen.INDUSTRY));
            }
            case FACTORY_STATUS->{
                add("REFRESH AUTHORIZED WORKSITE",gameplay::gpuRequestFactoryInspection);
                add("RETURN TO INDUSTRY",()->show(Screen.INDUSTRY));
            }
            case CONVEYOR_STATUS->{
                add("REFRESH OWNED CONVEYOR",gameplay::gpuRequestConveyorInspection);
                add("PROGRAM THIS CONVEYOR",this::openConveyor);
                add("RETURN TO INDUSTRY",()->show(Screen.INDUSTRY));
            }
            case UTILITIES->{
                add("STOKE STEAM BOILER",gameplay::gpuFuelSteamBoiler);
                add("CHARGE ELECTRIC GENERATOR",gameplay::gpuChargeGenerator);
                add("BACK TO INDUSTRY",()->show(Screen.INDUSTRY));
            }
            case CONVEYOR->{
                add("DISABLE AUTOMATIC ROUTE",()->{
                    gameplay.gpuProgramConveyor("NONE");show(Screen.GAME);
                });
                add("DIRECTION: "+(conveyorReverse?"EAST > WEST":"WEST > EAST"),
                    ()->{conveyorReverse=!conveyorReverse;build();});
                add("AUTO: FEED QUEUED E5 INPUTS",()->{
                    gameplay.gpuProgramConveyor("AUTO",conveyorReverse);show(Screen.GAME);
                });
                var materials=gameplay.gpuConveyorStockItems();
                int end=Math.min(materials.size(),pageOffset+4);
                for(int i=pageOffset;i<end;i++){
                    String item=materials.get(i);
                    add((conveyorReverse?"EAST > WEST  ":"WEST > EAST  ")+item,()->{
                        gameplay.gpuProgramConveyor(item,conveyorReverse);show(Screen.GAME);
                    });
                }
                if(pageOffset>0)add("PREVIOUS MATERIALS",()->{
                    pageOffset=Math.max(0,pageOffset-4);show(Screen.CONVEYOR);
                });
                if(end<materials.size())add("MORE MATERIALS",()->{
                    pageOffset+=4;show(Screen.CONVEYOR);
                });
                add("INSPECT CONVEYOR STATUS",this::openConveyorInspection);
                add("BACK TO INDUSTRY",()->show(Screen.INDUSTRY));
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
                add("PROGRAM NEXT E5 JOB",()->{
                    pageOffset=0;show(Screen.QUEUE_ORDERS);
                });
                add("BACK TO INDUSTRY",()->show(Screen.INDUSTRY));
            }
            case QUEUE_ORDERS->{
                add("MODE: "+(repeatOrders?"REPEAT EACH PAID BATCH":"SINGLE NEXT BATCH"),
                    ()->{repeatOrders=!repeatOrders;build();});
                var ids=gameplay.gpuAutomationRecipes();
                int end=Math.min(ids.size(),pageOffset+4);
                for(int i=pageOffset;i<end;i++){
                    String id=ids.get(i);
                    add((repeatOrders?"REPEAT ":"QUEUE ")+id.toUpperCase(java.util.Locale.ROOT),()->{
                        gameplay.gpuQueueAutomation(repeatOrders?"REPEAT:"+id:id);
                        show(Screen.GAME);
                    });
                }
                if(pageOffset>0)add("PREVIOUS E5 JOBS",()->{
                    pageOffset=Math.max(0,pageOffset-4);show(Screen.QUEUE_ORDERS);
                });
                if(end<ids.size())add("MORE E5 JOBS",()->{
                    pageOffset+=4;show(Screen.QUEUE_ORDERS);
                });
                add("CANCEL QUEUED / REPEATING JOB",()->{
                    gameplay.gpuQueueAutomation("NONE");show(Screen.GAME);
                });
                add("BACK TO WORK ORDERS",()->show(Screen.WORK_ORDERS));
            }
            case STORAGE->{
                add("TRANSFER SIZE: "+(stockTransferSize==128?"MAX 128":Integer.toString(stockTransferSize)),
                    ()->{
                        stockTransferSize=stockTransferSize==1?10:
                            stockTransferSize==10?128:1;
                        build();
                    });
                var carried=new java.util.TreeMap<>(gameplay.gpuCarriedItems());
                var stored=new java.util.TreeMap<>(gameplay.gpuCampStock());
                java.util.List<String[]> options=new java.util.ArrayList<>();
                carried.forEach((item,n)->{if(n>0)options.add(new String[]{"STORE",item,Integer.toString(n)});});
                stored.forEach((item,n)->{if(n>0)options.add(new String[]{"TAKE",item,Integer.toString(n)});});
                int end=Math.min(options.size(),pageOffset+7);
                for(int i=pageOffset;i<end;i++){
                    var option=options.get(i);
                    boolean take=option[0].equals("TAKE");
                    int available=Integer.parseInt(option[2]);
                    int amount=Math.min(available,stockTransferSize);
                    add("    "+option[0]+" "+option[1]+" x"+amount+"/"+available,()->{
                        // Physical item movement remains one atomic host
                        // transaction; never synthesize stock in the menu.
                        gameplay.gpuTransferCampStock(take,option[1],amount);
                        toast("HOST "+option[0]+" x"+amount+" REQUEST");
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
        int wasSelected=selected;
        if(Gdx.input.isKeyJustPressed(Input.Keys.UP))
            selected=Math.floorMod(selected-1,Math.max(1,items.size()));
        if(Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
            selected=Math.floorMod(selected+1,Math.max(1,items.size()));
        if(wasSelected!=selected&&audio!=null)audio.navigate();
        if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER))trigger(0);
        if(screen!=Screen.CREW_INPUT){
            if(Gdx.input.isKeyJustPressed(Input.Keys.LEFT))trigger(-1);
            if(Gdx.input.isKeyJustPressed(Input.Keys.RIGHT))trigger(1);
        }
        if(Gdx.input.justTouched()){
            float px=GpuScreenLayout.pointerX(Gdx.input.getX(),
                Gdx.graphics.getWidth(),GpuScreenLayout.width(
                    Gdx.graphics.getWidth(),Gdx.graphics.getHeight()));
            float py=GpuScreenLayout.pointerY(Gdx.input.getY(),
                Gdx.graphics.getHeight());
            if(px>=39&&px<=403)
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
        skin.frame(sprites,16,30,408,345);
        skin.band(sprites,33,334,375,30);
        skin.font().draw(sprites,"THE MALEFIC VEIL / "+screen,45,354);
        if(screen==Screen.CREW||screen==Screen.CREW_INPUT){
            String status=gameplay.gpuCrewStatus();
            if(status.length()>43)status=status.substring(0,42)+"~";
            skin.font().draw(sprites,status,43,331);
        }else if(screen==Screen.CRAFT){
            skin.font().draw(sprites,"HOST VALIDATES INGREDIENTS / LOCATION",43,331);
         }else if(screen==Screen.INDUSTRY){
            skin.font().draw(sprites,"SEPARATE FACTION SYSTEMS / LIVE HOST ACTIONS",43,331);
        }else if(screen==Screen.FACTORY_STATUS){
            skin.font().draw(sprites,
                "AUTHORITATIVE NEAREST OWNED WORKSITE",43,331);
        }else if(screen==Screen.PRODUCTION_OVERVIEW){
            var report=gameplay.gpuProductionOverview(pageOffset);
            String line=report==null?"AWAITING HOST PRODUCTION REPORT":
                "OWNED INDUSTRY: "+report.total()+" SITES  PAGE "+(pageOffset+1);
            skin.font().draw(sprites,line,43,331);
        }else if(screen==Screen.CONVEYOR){
            skin.font().draw(sprites,
                "OWNED CRATES / DIRECTIONAL ROUTE / PAID POWER",43,331);
        }else if(screen==Screen.CONVEYOR_STATUS){
            skin.font().draw(sprites,
                "HOST-VERIFIED NEAREST OWNED CONVEYOR",43,331);
        }else if(screen==Screen.SITES||screen==Screen.WORK_ORDERS){
            skin.font().draw(sprites,
                "HOST CHECKS REAL COSTS / LOCATION / DEPENDENCIES",43,331);
        }else if(screen==Screen.STORAGE){
            skin.font().draw(sprites,"OWN CRATE / HOST VALIDATES BULK TRANSFERS",43,331);
        }
        for(int i=0;i<items.size();i++){
            float row=297-i*24;
            skin.choice(sprites,items.get(i).label(),39,row,364,22,selected==i);
            if(screen==Screen.STORAGE){
                // Match host transfer rows exactly; artwork never changes the transaction.
                List<String> materialIds=new ArrayList<>();
                new TreeMap<>(gameplay.gpuCarriedItems()).forEach((id,count)->{
                    if(count>0)materialIds.add(id);
                });
                new TreeMap<>(gameplay.gpuCampStock()).forEach((id,count)->{
                    if(count>0)materialIds.add(id);
                });
                // Row zero is the quantity selector, not an inventory item.
                // Remaining row icons must follow the paginated item index.
                int source=pageOffset+i-1;
                if(i>=1&&i<=7&&source<materialIds.size())
                    book.drawIcon(sprites,materialIds.get(source),42,row+1,19);
            }
            if(screen==Screen.CRAFT){
                List<MaleficVeil.GpuCraftRecipe> recipes=crafts();
                int source=pageOffset+i;
                if(source<recipes.size()&&i<MAX_RECIPES_PER_PAGE){
                    String id=recipes.get(source).id();
                    var index=veil.CraftingRecipeIndex.recipe(id.startsWith("field:")?
                        "throw:"+id.substring(6):"hand:"+id);
                    if(index!=null)book.drawIcon(sprites,index.output(),42,row+1,19);
                }
            }
        }
        if(screen==Screen.FACTORY_STATUS){
            var report=gameplay.gpuFactoryInspection();
            skin.panel(sprites,39,95,365,147);
            if(report==null){
                skin.font().draw(sprites,"AWAITING HOST INSPECTION",47,220);
            }else{
                String[] lines={
                    "SITE: "+report.facility().replace('-',' ').toUpperCase(Locale.ROOT),
                    "PROCESS: "+report.recipe().replace('-',' ').toUpperCase(Locale.ROOT),
                    "STATE: "+report.state().replace('_',' '),
                    "DETAIL: "+report.detail().replace('_',' '),
                    "PROGRESS: "+report.elapsed()+"/"+report.duration()+" SEC   DONE: "+report.completed(),
                    "NEXT: "+report.next().replace('-',' ').toUpperCase(Locale.ROOT)
                };
                for(int i=0;i<lines.length;i++){
                    String line=lines[i];
                    if(line.length()>45)line=line.substring(0,44)+"~";
                    skin.font().draw(sprites,line,47,222-i*22);
                }
            }
        }
        if(screen==Screen.PRODUCTION_OVERVIEW){
            var report=gameplay.gpuProductionOverview(pageOffset);
            skin.panel(sprites,39,38,365,177);
            if(report==null){
                skin.font().draw(sprites,"AWAITING AUTHORIZED HOST SNAPSHOT",47,196);
            }else if(report.entries().isEmpty()){
                skin.font().draw(sprites,
                    report.total()==0?"NO OWNED PRODUCTION FACILITIES":"NO SITES ON THIS PAGE",
                    47,196);
            }else{
                for(int i=0;i<report.entries().size();i++){
                    var entry=report.entries().get(i);
                    String[] lines={
                        "#"+entry.index()+" "+entry.state().replace('_',' ')+
                            " / "+entry.facility().replace('_',' '),
                        "JOB: "+entry.recipe().replace('-',' ').toUpperCase(Locale.ROOT)+
                            "  DONE: "+entry.completed(),
                        "NEED: "+entry.detail().replace('_',' ')+
                            "  NEXT: "+entry.next().replace('-',' ').toUpperCase(Locale.ROOT),
                        "FIX: "+entry.remedy().replace('_',' ')
                    };
                    for(int j=0;j<lines.length;j++){
                        String line=lines[j];
                        if(line.length()>44)line=line.substring(0,43)+"~";
                        skin.font().draw(sprites,line,47,205-i*42-j*10);
                    }
                }
            }
        }
        if(screen==Screen.CONVEYOR_STATUS){
            var report=gameplay.gpuConveyorInspection();
            skin.panel(sprites,39,95,365,147);
            if(report==null){
                skin.font().draw(sprites,"AWAITING HOST CONVEYOR INSPECTION",47,220);
            }else{
                String[] lines={
                    "ROUTE: "+report.mode().replace('_',' '),
                    "DIRECTION: "+report.direction().replace('_',' '),
                    "STATE: "+report.state().replace('_',' '),
                    "DETAIL: "+report.detail().replace('_',' '),
                    "TRANSFER: "+report.elapsed()+"/"+report.duration()+" MS   LOAD: "+report.load(),
                    "SOURCE: "+report.source()+"   RECEIVER FREE: "+report.space()
                };
                for(int i=0;i<lines.length;i++){
                    String line=lines[i];
                    if(line.length()>45)line=line.substring(0,44)+"~";
                    skin.font().draw(sprites,line,47,222-i*22);
                }
            }
        }
        if(screen==Screen.CREW_INPUT){
            skin.panel(sprites,39,147,365,34);
            skin.font().draw(sprites,crewAction+" > "+crewText+"_",48,169);
        }
        skin.band(sprites,31,39,379,24);
        skin.font().draw(sprites,notice.isBlank()?"ESC BACK  ARROWS / ENTER  MOUSE":notice,43,55);
    }
}
