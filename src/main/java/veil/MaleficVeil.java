package veil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** PSX-resolution Java2D raycast arena. No external jars, assets or native libraries. */
public final class MaleficVeil extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
    private static final int RW=480,RH=300;
    private static final double FOV=1.18;
    private final BufferedImage buffer=new BufferedImage(RW,RH,BufferedImage.TYPE_INT_RGB);
    private final double[] depth=new double[RW];
    private final Set<Integer> held=new HashSet<>();
    private final World world;
    private final UrbanScene urban;
    private final ContinentVista vista;
    private final int[][] groundAlbedo=new int[World.SIZE][World.SIZE];
    private final PropRenderer urbanRenderer=new PropRenderer();
    private final PerformanceSettings performance=new PerformanceSettings();
    private final GameplayMeshes actorModels=new GameplayMeshes();
    private final WeaponViewRenderer weaponRenderer=new WeaponViewRenderer();
    private final ArsenalMeshes arsenalMeshes=new ArsenalMeshes();
    private long arsenalSequence;
    private int fieldChoice;
    private record FieldBurstVisual(double x,double y,int faction,long untilTick){}
    private final java.util.List<FieldBurstVisual> fieldBurstVisuals=new java.util.ArrayList<>();
    private boolean arsenalMenuVisible,primaryHeld,secondaryHeld;
    private int arsenalMenuChoice;
    private final NetLink net;
    private final Vehicles localVehicles;
    private final CargoHaulage localCargo;
    private final ArenaSystems localHost;
    private final WorldHarvesting localHarvesting;
    private final VeilWeather weather;
    private NetLink.WeatherSnapshot appliedWeather;
    private final int archetype;
    private final javax.swing.Timer timer;
    private double px,py,angle=0.2,abilitySeconds=0,abilityCooldown=0,fireCooldown=0,attackFlash=0;
    private int health=100,sanity=100,cash=150,scrap=4,medkits=2,pistolReserve=48,shellReserve=18,pistolClip=12,shellClip=6;
    private int gun=0,spawn=0,frags=0,previousMouse=-1,previousMouseY=-1;
    private double cameraPitch=0;
    private boolean mapVisible=false,continentVisible=false,help=true,dead=false;
    private boolean inventoryVisible=false,skillsVisible=false;
    private boolean aircraftShowroom=false;
    private int aircraftShowroomIndex=0;
    private double aircraftShowroomYaw=-Math.PI/2+.25;
    private boolean respawnPending=false;
    private long respawnRequestAt=0;
    private long ticks=0;
    private final String[] names={"WARDEN","MEDIC","SHADE","OCCULTIST"};
    private String notice="THE VEIL HAS THINNED";
    private int noticeTicks=120;
    private final JFrame window;
    /** OpenGL presents this authoritative, immutable-at-publication simulation view. */
    public record GpuFrame(double x,double z,double yaw,double pitch,double hour,
        int health,int sanity,int cash,int scrap,int medkits,
        int pistolClip,int pistolReserve,int shellClip,int shellReserve,
        int weapon,int frags,boolean dead,boolean connected,boolean inventory,
        boolean skills,boolean map,boolean continent,String notice,
        java.util.Map<String,Integer> inventoryItems,
        java.util.List<String> skillStats,
        java.util.List<GpuCraftRecipe> crafts,
        java.util.List<UrbanScene.ModelInstance> dynamic,
        ArsenalSystems.View arsenalState,boolean arsenalMenu,int arsenalChoice) {}
    public record GpuCraftRecipe(String id,String station,String description,
                                 boolean affordable) {}
    private volatile GpuFrame gpuFrame;
    private LanServer gpuHostedServer;
    private boolean gpuPresentation;
    /** The same World used by LAN messages, combat and CityLayout collision. */
    public World gpuWorld(){return world;}
    /** Authoritative actor, pickup, item, resource and weather model identities. */
    public java.util.Map<String,GlbMesh> gpuDynamicMeshes(){
        java.util.Map<String,GlbMesh> combined=new java.util.LinkedHashMap<>(actorModels.meshes());
        for(int i=0;i<32;i++)arsenalMeshes.worldId(i);
        combined.putAll(arsenalMeshes.worldModels());
        return java.util.Collections.unmodifiableMap(combined);
    }
    /** Only immutable snapshots cross the Swing simulation / OpenGL threads. */
    public GpuFrame gpuFrame(){return gpuFrame;}
    /** Synthetic GPU events use original gameplay input handlers on Swing EDT. */
    public void gpuKey(int key,boolean pressed){
        SwingUtilities.invokeLater(()->{
            KeyEvent event=new KeyEvent(this,pressed?KeyEvent.KEY_PRESSED:KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(),0,key,KeyEvent.CHAR_UNDEFINED);
            if(pressed)keyPressed(event);else keyReleased(event);
        });
    }
    public void gpuLook(double deltaX,double deltaY){
        if(!Double.isFinite(deltaX)||!Double.isFinite(deltaY))return;
        SwingUtilities.invokeLater(()->{
            if(dead)return;
            angle+=Math.max(-120,Math.min(120,deltaX))*.004;
            cameraPitch=Math.max(-1.52,Math.min(1.52,
                cameraPitch-Math.max(-120,Math.min(120,deltaY))*.004));
        });
    }
    public void gpuFire(){SwingUtilities.invokeLater(()->{if(!dead)shoot(false);});}
    public void gpuSceneReady(){SwingUtilities.invokeLater(()->{
        if(net!=null)net.sceneReady();
        DesktopDiagnostics.note("GPU_PLAY_SESSION_READY");
    });}
    /** Read-only recipe preview. The host always validates and consumes resources. */
    private java.util.List<GpuCraftRecipe> gpuCraftingSnapshot(){
        var result=new java.util.ArrayList<GpuCraftRecipe>();
        java.util.Map<String,Integer> inventory=carried();
        boolean fire=world.devices.stream().anyMatch(d->
            d.hp>0&&d.kind==ArenaSystems.FIELD_FIRE&&
            Math.hypot(d.x-px,d.y-py)<=2.2);
        boolean bench=world.devices.stream().anyMatch(d->
            d.hp>0&&d.kind==ArenaSystems.FIELD_BENCH&&
            Math.hypot(d.x-px,d.y-py)<=2.2);
        for(var recipe:SurvivalCrafting.recipes().values()){
            boolean nearby=recipe.station()==SurvivalCrafting.Station.HAND||
                (recipe.station()==SurvivalCrafting.Station.FIRE&&fire)||
                (recipe.station()==SurvivalCrafting.Station.WORKBENCH&&bench);
            boolean enough=nearby;
            var parts=new java.util.ArrayList<String>();
            for(var item:recipe.requires().entrySet()){
                int count=inventory.getOrDefault(item.getKey().name(),0);
                parts.add(item.getKey().name()+" "+count+"/"+item.getValue());
                if(count<item.getValue())enough=false;
            }
            java.util.Collections.sort(parts);
            result.add(new GpuCraftRecipe(recipe.id(),recipe.station().name(),
                String.join("  ",parts)+(nearby?"":recipe.station()==
                    SurvivalCrafting.Station.WORKBENCH?"  NEED WORKBENCH":
                    "  NEED CAMPFIRE"),enough));
        }
        for(ThrownDeployables.Blueprint bp:ThrownDeployables.catalog()){
            var selected=gpuCraftFaction();
            if(selected==CraftingEraRegistry.Faction.ALL||
               !bp.faction().name().equals(selected.name()))continue;
            boolean near=bp.station()==SurvivalCrafting.Station.HAND||
                bp.station()==SurvivalCrafting.Station.FIRE&&fire||
                bp.station()==SurvivalCrafting.Station.WORKBENCH&&bench;
            boolean enough=near;var parts=new java.util.ArrayList<String>();
            for(var ingredient:bp.ingredients().entrySet()){
                int count=inventory.getOrDefault(ingredient.getKey().name(),0);
                parts.add(ingredient.getKey()+" "+count+"/"+ingredient.getValue());
                if(count<ingredient.getValue())enough=false;
            }
            java.util.Collections.sort(parts);
            result.add(new GpuCraftRecipe("field:"+bp.id(),bp.station().name(),
                String.join(" ",parts)+(near?"":" NEED "+bp.station()),enough));
        }
        result.sort(java.util.Comparator.comparing(GpuCraftRecipe::id));
        return java.util.List.copyOf(result);
    }
    /** GPU UI never manipulates inventory directly; sends existing transactions. */
    public void gpuCraft(String id){
        if(id!=null&&id.startsWith("field:")){
            ThrownDeployables.Blueprint bp=ThrownDeployables.catalog().stream()
                .filter(b->id.equals("field:"+b.id())).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Unknown throwable recipe"));
            SwingUtilities.invokeLater(()->{
                if(net!=null)net.fieldCraft(bp.role().ordinal());
                else if(localHost!=null)toast(localHost.thrown.craft(1,bp.role().ordinal()).reason());
            });
            return;
        }
        var recipe=SurvivalCrafting.recipes().get(id);
        if(recipe==null)throw new IllegalArgumentException("Unknown field recipe");
        SwingUtilities.invokeLater(()->{
            if(net!=null){
                if(recipe.station()==SurvivalCrafting.Station.FIRE)net.craftFire(id);
                else if(recipe.station()==SurvivalCrafting.Station.WORKBENCH)
                    net.craftWorkbench(id);
                else net.craftHand(id);
            }else if(localHost!=null){
                boolean accepted=switch(recipe.station()){
                    case HAND -> localHost.craftHand(1,id);
                    case FIRE -> localHost.craftStation(1,id);
                    case WORKBENCH -> localHost.craftWorkbench(1,id);
                };
                toast(accepted?"CRAFTED "+id:"CRAFT DENIED");
            }
        });
    }
    /** The in-world menu reads the real host or subscribed authoritative
     * stock ledger; no locally invented inventory or free material grant. */
    public java.util.Map<String,Integer> gpuCarriedItems(){return carried();}
    public java.util.Map<String,Integer> gpuCampStock(){
        if(net!=null)return java.util.Map.copyOf(net.campStock);
        java.util.Map<String,Integer> stock=new java.util.TreeMap<>();
        if(localHost!=null)localHost.nearbyCampStock(1).forEach(
            (item,amount)->stock.put(item.name(),amount));
        return stock;
    }
    public void gpuRequestCampStock(){if(net!=null)net.requestCampStock();}
    public void gpuTransferCampStock(boolean take,String item,int quantity){
        SwingUtilities.invokeLater(()->{
            if(net!=null)net.transferCampStock(take,item,quantity);
            else if(localHost!=null){
                boolean ok;
                try{ok=localHost.transferCampStock(1,take,
                        SurvivalCrafting.Item.valueOf(item),quantity);}
                catch(IllegalArgumentException invalid){ok=false;}
                toast(ok?"STOCK "+(take?"TAKEN":"DEPOSITED"):"CAMP STOCK DENIED");
            }
        });
    }
    /** Authoritative first-tier work orders; all three progress through
     * independent faction-specific operating conditions, not one factory. */
    public void gpuStartFactionBatch(){
        gpuStartFactionBatch(null);
    }
    public void gpuStartFactionBatch(String requested){
        SwingUtilities.invokeLater(()->{
            if(net!=null){
                if(requested==null)net.startFactionBatch();
                else net.startFactionBatch(requested);
            }else if(localHost!=null)
                toast(localHost.startFactionBatch(1,requested)?
                    "WORKSITE PRODUCTION STARTED":"WORKSITE NEEDS SUPPLY / FACTION / REACH");
        });
    }
    public void gpuInspectFactionBatch(){
        SwingUtilities.invokeLater(()->{
            if(net!=null)net.inspectFactionBatch();
            else if(localHost!=null)
                toast(localHost.factionBatchStatus(1).replace('_',' '));
        });
    }
    /** Listed entries have a real physical blueprint and host-paid build path;
     * the UI never offers registry-only late-game machinery as functional. */
    public java.util.List<String> gpuSupportedFacilities(){
        CraftingEraRegistry.Faction selected=gpuCraftFaction();
        java.util.List<String> result=new java.util.ArrayList<>();
        for(int kind=ArenaSystems.CLAIM_HEART;
                kind<=ArenaSystems.BEGOTTEN_SPORE_BASIN;kind++){
            String id=ArenaSystems.factionFacility(kind);
            var facility=CraftingEraRegistry.facility(id);
            if(facility!=null&&(facility.faction()==CraftingEraRegistry.Faction.ALL||
               facility.faction()==selected))
                result.add(id);
        }
        return java.util.List.copyOf(result);
    }
    public java.util.List<String> gpuSupportedProcesses(){
        CraftingEraRegistry.Faction selected=gpuCraftFaction();
        java.util.List<String> result=new java.util.ArrayList<>();
        for(var recipe:CraftingEraRegistry.processes().values()){
            if(ArenaSystems.kindForFacility(recipe.facility())<0||
               recipe.faction()!=CraftingEraRegistry.Faction.ALL&&
               recipe.faction()!=selected)continue;
            try{
                SurvivalCrafting.Item.valueOf(recipe.output());
                for(String ingredient:recipe.inputs().keySet())
                    SurvivalCrafting.Item.valueOf(ingredient);
            }catch(IllegalArgumentException unknown){continue;}
            result.add(recipe.id());
        }
        return java.util.List.copyOf(result);
    }
    public void gpuPlaceFacility(String facilityId){
        int kind=ArenaSystems.kindForFacility(facilityId);
        if(kind<0){toast("FACILITY IS NOT YET CONSTRUCTIBLE");return;}
        var facility=CraftingEraRegistry.facility(facilityId);
        if(facility==null||facility.faction()!=CraftingEraRegistry.Faction.ALL&&
           facility.faction()!=gpuCraftFaction()){
            toast("WRONG FACTION CONSTRUCTION");return;
        }
        SwingUtilities.invokeLater(()->{
            if(net!=null)net.build(kind);
            else if(localHost!=null){
                var result=localHost.buildOutcome(1,kind);
                toast(result.built()?"FACILITY PLACED":
                    "BUILD "+result.review().reason().replace('_',' '));
            }
        });
    }
    public void gpuPlaceFactionFactory(){
        CraftingEraRegistry.Faction faction=gpuCraftFaction();
        int kind=switch(faction){
            case UNRAVELED -> ArenaSystems.UNRAVELED_SCRAP_OUTPOST;
            case PENITENT -> ArenaSystems.PENITENT_VOTIVE_ALTAR;
            case BEGOTTEN -> ArenaSystems.BEGOTTEN_BROOD_NEST;
            default -> -1;
        };
        if(kind<0){toast("CHOOSE A FACTION BEFORE FOUNDING INDUSTRY");return;}
        SwingUtilities.invokeLater(()->{
            if(net!=null)net.build(kind);
            else if(localHost!=null)
                toast(localHost.build(1,kind)?
                    "FACTION FACILITY CONSTRUCTED":"FACTION SITE REQUIREMENTS NOT MET");
        });
    }
    public void gpuPlaceStorage(){
        SwingUtilities.invokeLater(()->{
            if(net!=null)net.build(ArenaSystems.STORAGE_CRATE);
            else if(localHost!=null)
                toast(localHost.build(1,ArenaSystems.STORAGE_CRATE)?
                    "PERMANENT CRATE CONSTRUCTED":"CRATE PLACEMENT DENIED");
        });
    }
    /** Native in-game supply action. Item selection uses the replicated
     * inventory, but consumption and need recovery belong to the host. */
    public void gpuUseSupplies(boolean water){
        String chosen=null;
        for(String name:water?
            new String[]{"CLEAN_WATER"}:
            new String[]{"EDIBLES","RATIONS","SMOKED_RATIONS"}){
            if(carried().getOrDefault(name,0)>0){chosen=name;break;}
        }
        if(chosen==null){
            toast(water?"NO CLEAN WATER CARRIED":"NO EDIBLE FOOD CARRIED");
            return;
        }
        String item=chosen;
        SwingUtilities.invokeLater(()->{
            if(net!=null)net.useSupply(item);
            else if(localHost!=null){
                boolean ok=localHost.consumeSurvival(1,SurvivalCrafting.Item.valueOf(item));
                toast(ok?"USED "+item:"CANNOT USE "+item);
            }
        });
    }
    public String gpuFieldStatus(){
        String faction=net!=null?net.faction():localHost==null?"NONE":
            String.valueOf(localHost.factions.faction(localHost.player(1).accountKey));
        ThrownDeployables.Role role=ThrownDeployables.Role.values()[fieldChoice];
        String manufacturer=faction.startsWith("THE_")?faction.substring(4):"UNENROLLED";
        int available=carried().getOrDefault("DEPLOY_"+manufacturer+"_"+role.name(),0);
        return "FIELD "+role.name()+" x"+available;
    }
    private void throwField(){
        if(dead)return;
        if(net!=null)net.fieldThrow(fieldChoice);
        else if(localHost!=null){
            ArenaSystems.Player p=localHost.player(1);
            p.x=px;p.y=py;p.angle=angle;
            toast(localHost.thrown.throwKit(1,fieldChoice).reason());
        }
    }
    private void cycleField(){
        fieldChoice=(fieldChoice+1)%ThrownDeployables.Role.values().length;
        toast("THROWABLE "+ThrownDeployables.Role.values()[fieldChoice]+" / Y THROW");
    }
    /** Geometry views come from the transient registry, not World.Device. */
    private java.util.List<UrbanScene.ModelInstance> fieldVisuals(){
        var result=new java.util.ArrayList<UrbanScene.ModelInstance>();
        if(net!=null){
            for(NetLink.FieldEvent f:net.activeFields.values()){
                ThrownDeployables.Blueprint bp=ThrownDeployables.blueprint(f.blueprint());
                if(bp!=null&&f.hp()>0)
                    result.add(new UrbanScene.ModelInstance(bp.mesh(),f.x(),f.y(),f.id()*.1,
                        f.flying()?.35:1,1,f.height()));
            }
        }else if(localHost!=null){
            for(ThrownDeployables.View f:localHost.thrown.views()){
                ThrownDeployables.Blueprint bp=ThrownDeployables.blueprint(f.blueprint());
                if(bp!=null&&f.hp()>0)
                    result.add(new UrbanScene.ModelInstance(bp.mesh(),f.x(),f.y(),f.heading(),
                        f.flying()?.35:1,1,f.z()));
            }
        }
        fieldBurstVisuals.removeIf(f->f.untilTick()<ticks);
        for(FieldBurstVisual f:fieldBurstVisuals)
            result.add(new UrbanScene.ModelInstance("thrown-harmless-burst",f.x(),f.y(),
                ticks*.06,Math.max(.05,(f.untilTick()-ticks)/25.0),.75));
        return result;
    }
    /** A crafted kit is consumed by the host only after real placement validation. */
    public void gpuPlaceWorkbench(){
        SwingUtilities.invokeLater(()->{
            if(net!=null)net.build(ArenaSystems.FIELD_BENCH);
            else toast("WORKBENCH PLACEMENT REQUIRES HOSTED WORLD");
        });
    }
    /** Existing NetLink social protocol remains server authoritative. */
    /** Only server-confirmed faction state determines which industrial
     * recipes appear as belonging to the player's production economy. */
    public CraftingEraRegistry.Faction gpuCraftFaction(){
        String hostFaction=net==null?"NONE":net.faction();
        return switch(hostFaction){
            case "THE_UNRAVELED" -> CraftingEraRegistry.Faction.UNRAVELED;
            case "THE_PENITENT" -> CraftingEraRegistry.Faction.PENITENT;
            case "THE_BEGOTTEN" -> CraftingEraRegistry.Faction.BEGOTTEN;
            default -> CraftingEraRegistry.Faction.ALL;
        };
    }
    public String gpuCrewStatus(){
        return net==null?"OFFLINE - NO CREW":
            "FACTION "+net.faction()+"  CREW "+net.crew()+"  ROLE "+net.crewRole();
    }
    public void gpuCrewAction(String action,String argument){
        if(net==null)throw new IllegalStateException("No connected multiplayer host");
        String input=argument==null?"":argument.strip();
        if(input.length()>32)throw new IllegalArgumentException("Crew input exceeds 32 characters");
        int player=0;
        if(java.util.Set.of("INVITE","PROMOTE","REMOVE","TRANSFER").contains(action)){
            try{player=Integer.parseInt(input);}
            catch(NumberFormatException bad){throw new IllegalArgumentException("Player ID must be numeric");}
            if(player<1)throw new IllegalArgumentException("Player ID must be positive");
        }
        if(action.equals("CREATE")&&!input.matches("[A-Za-z0-9][A-Za-z0-9 _-]{2,31}"))
            throw new IllegalArgumentException("Crew name must be 3-32 letters/numbers");
        final int target=player;
        SwingUtilities.invokeLater(()->{
            switch(action){
                case "CREATE" -> net.createCrew(input);
                case "INVITE" -> net.inviteCrew(target);
                case "ACCEPT" -> net.acceptCrew();
                case "PROMOTE" -> net.promoteCrew(target);
                case "REMOVE" -> net.kickCrew(target);
                case "LEAVE" -> net.leaveCrew();
                case "TRANSFER" -> net.transferCrew(target);
                case "DISBAND" -> net.disbandCrew();
                default -> throw new IllegalArgumentException("Unknown crew action");
            }
        });
    }
    /** Close the real host and flush persistent world saves before JVM exit. */
    public void gpuShutdown(){
        SwingUtilities.invokeLater(()->{
            timer.stop();
            try{
                if(net!=null)net.close();
                if(gpuHostedServer!=null)gpuHostedServer.close();
                weather.close();
                DesktopDiagnostics.note("GPU_SESSION_STOPPED");
            }catch(IOException failure){
                DesktopDiagnostics.note("GPU_SHUTDOWN_SAVE_ERROR "+failure);
            }
        });
    }
    /** Optional compact gateway to the existing validated transaction dialogs. */
    public void gpuManagement(){
        SwingUtilities.invokeLater(()->{
            String[] choices={"Drop item","Give item","Vehicle inspection",
                "Industrial recipes","Crafting","Crew","Cancel"};
            Object action=JOptionPane.showInputDialog(null,"Management",
                "The Malefic Veil",JOptionPane.PLAIN_MESSAGE,null,choices,choices[0]);
            if(action==null)return;
            switch(action.toString()){
                case "Drop item"->inventoryAction(false);
                case "Give item"->inventoryAction(true);
                case "Vehicle inspection"->vehicleInspection();
                case "Industrial recipes"->productionCatalog();
                case "Crafting"->handCraftDialog();
                case "Crew"->crewDialog();
                default->{}
            }
        });
    }
    private void publishGpuFrame(){
        if(!gpuPresentation)return;
        java.util.List<String> stats=new java.util.ArrayList<>();
        if(net!=null){
            NetLink.Rpg rpg=net.rpg();
            if(rpg!=null){
                stats.add("LEVEL "+rpg.level()+"  XP "+rpg.experience());
                stats.add("NEXT "+rpg.next());
                stats.add("MARKSMAN "+rpg.marksmanship());
                stats.add("ATHLETICS "+rpg.athletics());
                stats.add("ENGINEER "+rpg.engineering());
                stats.add("SCAVENGING "+rpg.scavenging());
                stats.add("SURVIVAL "+rpg.survival());
                stats.add("HUNGER "+rpg.hunger()+" WATER "+rpg.hydration());
                if(rpg.hunger()==0||rpg.hydration()==0)
                    stats.add((rpg.hunger()==0?"STARVING ":"")+
                        (rpg.hydration()==0?"DEHYDRATED ":"")+"- HEALTH LOSS");
                stats.add("LIFE "+rpg.aliveMs()/60000+"m BOOST "+
                    String.format(java.util.Locale.ROOT,"%.1fx",rpg.streakBonus()));
                stats.add(net.hardcore()?"HARDCORE RESET ON DEATH":"STREAK RESET ON DEATH");
            }else stats.add("WAITING FOR HOST STATISTICS");
        }else stats.add("SOLO LOCAL SIMULATION");
        java.util.List<UrbanScene.ModelInstance> actors=new java.util.ArrayList<>();
        for(var loot:(localHost!=null?localHost.arsenal.loot:arsenalLootVisuals()))
            if(!loot.taken&&loot.type==0)
                actors.add(new UrbanScene.ModelInstance(arsenalMeshes.worldId(loot.value),
                    loot.x,loot.y,ticks*.018,1.0,1.0,.85));
        for(World.Npc npc:world.npcs)if(npc.hp>0)
            actors.add(new UrbanScene.ModelInstance("enemy-walker",npc.x,npc.y,
                Math.atan2(py-npc.y,px-npc.x)-Math.PI/2,1));
        for(World.Pickup pickup:world.pickups)
            if(!pickup.taken&&pickup.kind>=0&&pickup.kind<GameplayMeshes.PICKUP.length)
                actors.add(new UrbanScene.ModelInstance(GameplayMeshes.PICKUP[pickup.kind],
                    pickup.x,pickup.y,ticks*.018,1.15,1,.86+.10*Math.sin(ticks*.11+pickup.kind)));
        actors.addAll(fieldVisuals());
        for(World.Device device:world.devices)
            if(device.hp>0&&device.kind>=0&&device.kind<GameplayMeshes.DEVICE.length)
                actors.add(new UrbanScene.ModelInstance(GameplayMeshes.DEVICE[device.kind],
                    device.x,device.y,0,1));
        if(net!=null){
            for(var entry:net.others.entrySet()){
                var other=entry.getValue();
                if(other.hp>0)actors.add(new UrbanScene.ModelInstance(
                    actorMesh(entry.getKey(),other.classId),other.x,other.y,
                    other.angle-Math.PI/2,1,other.opacity));
            }
            for(var node:net.resourceNodes.values())
                if(node.remaining()>0&&Math.hypot(node.x()-px,node.y()-py)<180)
                    actors.add(new UrbanScene.ModelInstance(modelForResource(node.type(),node.id()),
                        node.x(),node.y(),0,node.type().equals("TIMBER")?.39:1.05));
            for(var car:net.cars.values())
                if(car.hp()>0&&car.driverId()!=net.id)
                    actors.add(new UrbanScene.ModelInstance(
                        (car.id()%2==0?"truck-green":"truck-grey")+
                        (net.truckCargo.containsKey(car.id())?"-cargo":""),
                        car.x(),car.y(),car.heading(),1.9));
        }else{
            for(var node:localHarvesting.nearby(px,py,21))
                if(node.remaining()>0)
                    actors.add(new UrbanScene.ModelInstance(
                        modelForResource(node.type().name(),node.id()),
                        localHarvesting.nodeX(node),localHarvesting.nodeY(node),0,
                        node.type()==WildernessResources.Type.TIMBER?.39:1.05));
            for(var car:localVehicles.states())if(car.hp()>0&&car.driverId()!=1)
                actors.add(new UrbanScene.ModelInstance(localVehicles.cargo(car.id()).model(),
                    car.x(),car.y(),car.heading(),1.9));
        }
        ArsenalSystems.View equipped=arsenalView();
        int selected=equipped==null?0:equipped.selected();
        int[] rounds=equipped==null?new int[32]:equipped.clip();
        int[] reserves=equipped==null?new int[32]:equipped.reserve();
        gpuFrame=new GpuFrame(px,py,angle,cameraPitch,weather.timeOfDay()*24,
            health,sanity,cash,scrap,medkits,rounds[0],reserves[0],
            rounds[6],reserves[6],selected,frags,dead,net==null||net.connected(),
            inventoryVisible,skillsVisible,mapVisible,continentVisible,
            noticeTicks>0?notice:"",java.util.Map.copyOf(carried()),
            java.util.List.copyOf(stats),gpuCraftingSnapshot(),
            java.util.List.copyOf(actors),equipped,arsenalMenuVisible,arsenalMenuChoice);
    }

    private java.util.List<ArsenalSystems.Loot> arsenalLootVisuals(){
        // Client and host use the same deterministic loot schedule.
        return arsenalVisualHost.arsenal.loot;
    }
    private ArenaSystems arsenalVisualHost;
    private MaleficVeil(JFrame window,long seed,int archetype,NetLink net) {
        this.window=window;this.net=net;this.archetype=archetype;
        localHost=net==null?new ArenaSystems(seed):null;
        // Never simulate a different solo map than the authoritative host map.
        world=localHost==null?new World(seed):localHost.world;
        vista=new ContinentVista(world);
        localVehicles=localHost==null?new Vehicles(world):localHost.vehicles;
        localCargo=localHost==null?new CargoHaulage(world,localVehicles):localHost.cargo;
        if(localHost!=null){
            localHost.join(1);
            // A local solo world also needs a stable owner identity. Anonymous
            // guest keys may not own/operate wilderness camp freight.
            localHost.assignAccount(1,"acct_"+PlayerIdentity.sha256("solo:"+seed));
        }
        arsenalVisualHost=localHost==null?new ArenaSystems(seed):localHost;
        localHarvesting=net==null?new WorldHarvesting(world):null;
        try{weather=new VeilWeather(seed,java.nio.file.Path.of("."));}
        catch(java.io.IOException ex){throw new IllegalStateException("Pinned shared weather library missing",ex);}
        if(net!=null)weather.followHost();
        try{urban=new UrbanScene(world,vista.foundations());}
        catch(IOException e){throw new IllegalStateException("Curated urban meshes unavailable",e);}
        // Build the floor material lookup from the SAME Kenney source placements
        // used for real geometry. This fills near-plane gaps during steep looks.
        for(World.UrbanProp item:world.urbanProps){
            if(item.heightOffset()!=0||!(item.model().startsWith("road-")||
                item.model().equals("grass")))continue;
            int gx=(int)Math.floor(item.x()),gz=(int)Math.floor(item.y());
            if(gx<0||gz<0||gx>=World.SIZE||gz>=World.SIZE)continue;
            GlbMesh source=urban.models().get(item.model());
            if(source!=null&&!source.triangles().isEmpty())
                groundAlbedo[gx][gz]=source.triangles().get(0).rgb()&0xffffff;
        }
        px=world.spawns[0][0];py=world.spawns[0][1];
        if(net==null){
            world.npcs.add(new World.Npc(7.5,1.5));
            world.npcs.add(new World.Npc(1.5,8.5));
            world.pickups.add(new World.Pickup(4.5,1.5,3));
            world.pickups.add(new World.Pickup(1.5,4.5,4));
        }else net.choose(archetype);
        urbanRenderer.setBudgets(performance);
        setBackground(Color.BLACK);setPreferredSize(new Dimension(1024,640));
        setFocusable(true);
        addKeyListener(this);addMouseListener(this);addMouseMotionListener(this);
        timer=new javax.swing.Timer(33,this);timer.start();
    }
    /** CI runs this real gameplay simulation with OpenGL without showing dialogs. */
    public static MaleficVeil gpuSmokeSession(long seed){
        if(!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Gameplay must initialize on Swing EDT");
        MaleficVeil game=new MaleficVeil(null,seed,0,null);
        game.gpuPresentation=true;
        game.publishGpuFrame();
        return game;
    }
    /**
     * In-window frontend path. No modal dialogs or duplicate game simulation.
     * World rules are still validated/saved through WorldCreationConfig;
     * host-side authority remains with LanServer and NetLink.
     */
    public record GpuLaunch(WorldCreationConfig world,String joinHost,int joinPort,
                            CharacterRoster.Faction faction,FactionClasses.Role role) {
        public GpuLaunch {
            if(faction==null||role==null||role.faction()!=faction||!role.playable())
                throw new IllegalArgumentException("Choose a playable faction class");
            if(world==null){
                if(joinHost==null||joinHost.isBlank()||joinHost.length()>253||
                    joinPort<1||joinPort>65535)
                    throw new IllegalArgumentException("Invalid multiplayer host");
            }else if(joinHost!=null)
                throw new IllegalArgumentException("Cannot host and join simultaneously");
        }
    }
    public static MaleficVeil createGpuSession(GpuLaunch requested)throws IOException{
        if(!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Initialize gameplay on Swing EDT");
        java.util.Objects.requireNonNull(requested);
        NetLink link=null;
        LanServer host=null;
        try{
            long worldSeed;
            if(requested.world()!=null){
                WorldCreationConfig options=requested.world();
                // Loading an already saved config must use the same document
                // that was previously authenticated and persisted.
                if(!options.equals(WorldCreationConfig.load(options.directory())))
                    throw new IOException("Host world rules disagree with saved world");
                HostAiSettings.Rules bots=new HostAiSettings.Rules(
                    options.aiPopulation(),HostAiSettings.Distribution.BALANCED,1,
                    1,0,0,0);
                int port=options.hostType().equals("multi")?7777:0;
                host=new LanServer(port,options.seed(),options.directory(),
                    options.hardcore(),bots);
                link=new NetLink("127.0.0.1",host.port());
                worldSeed=options.seed();
            }else{
                link=new NetLink(requested.joinHost(),requested.joinPort());
                worldSeed=link.seed;
            }
            link.selectFaction(requested.faction());
            MaleficVeil game=new MaleficVeil(null,worldSeed,
                requested.role().existingAbilityId(),link);
            game.gpuPresentation=true;
            game.gpuHostedServer=host;
            game.publishGpuFrame();
            DesktopDiagnostics.note("GPU_FRONTEND_WORLD_READY seed="+worldSeed+
                " mode="+(requested.world()==null?"join":requested.world().hostType()));
            return game;
        }catch(IOException|RuntimeException failure){
            if(link!=null)try{link.close();}catch(Exception ignored){}
            if(host!=null)try{host.close();}catch(Exception ignored){}
            throw failure;
        }
    }
    /** Compact host-only setup; network clients cannot change population rules. */
    private static HostAiSettings.Rules hostAiDialog(){
        HostAiSettings.Rules initial=HostAiSettings.fromProperties();
        JTextField count=new JTextField(Integer.toString(initial.count()),5);
        JTextField level=new JTextField(initial.level()==initial.ceiling()?
            Integer.toString(initial.level()):initial.level()+"-"+initial.ceiling(),7);
        JTextField split=new JTextField(initial.unraveled()+","+
            initial.penitent()+","+initial.begotten(),9);
        JComboBox<HostAiSettings.Distribution> distribution=new JComboBox<>(
            HostAiSettings.Distribution.values());
        distribution.setSelectedItem(initial.distribution());
        JComboBox<String> faction=new JComboBox<>(
            new String[]{"1 — UNRAVELED","2 — PENITENT","3 — BEGOTTEN"});
        faction.setSelectedIndex(initial.faction()-1);
        JPanel choices=new JPanel(new GridLayout(5,2,8,3));
        choices.add(new JLabel("AI characters (0–24)"));choices.add(count);
        choices.add(new JLabel("Distribution"));choices.add(distribution);
        choices.add(new JLabel("All on faction"));choices.add(faction);
        choices.add(new JLabel("Starting level (5 or 5-12)"));choices.add(level);
        choices.add(new JLabel("Custom 1,2,3 counts"));choices.add(split);
        if(JOptionPane.showConfirmDialog(null,choices,
            "Host AI Factions — max 3 per crew",JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return null;
        try{
            var mode=(HostAiSettings.Distribution)distribution.getSelectedItem();
            java.util.List<String> options=new java.util.ArrayList<>();
            options.add("--ai-count="+count.getText().trim());
            options.add("--ai-level="+level.getText().trim());
            options.add("--ai-mode="+mode);
            options.add("--ai-team="+(faction.getSelectedIndex()+1));
            if(mode==HostAiSettings.Distribution.CUSTOM)
                options.add("--ai-split="+split.getText().trim());
            return HostAiSettings.fromOptions(options);
        }catch(IllegalArgumentException exception){
            JOptionPane.showMessageDialog(null,exception.getMessage(),
                "Invalid AI Host Settings",JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
    /** New/continue world entry, separate from joining an existing host. */
    private static WorldCreationConfig worldSelection(boolean multiplayer){
        String[] actions={"CREATE WORLD","LOAD WORLD","CANCEL"};
        int action=JOptionPane.showOptionDialog(null,
            multiplayer?"HOST A WORLD":"SINGLE PLAYER WORLDS",
            "The Malefic Veil",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,
            null,actions,actions[0]);
        if(action==2||action<0)return null;
        if(action==1){
            try{
                var worlds=WorldCreationConfig.list().stream()
                    .filter(w->w.hostType().equals(multiplayer?"multi":"solo")).toList();
                if(worlds.isEmpty()){
                    JOptionPane.showMessageDialog(null,"No saved worlds for this mode.");
                    return null;
                }
                String[] labels=worlds.stream().map(w->w.worldName()+
                    " | "+(w.hardcore()?"Hardcore":"Normal")+" | "+w.seed())
                    .toArray(String[]::new);
                Object selection=JOptionPane.showInputDialog(null,"Load world",
                    "Saved Worlds",JOptionPane.PLAIN_MESSAGE,null,labels,labels[0]);
                if(selection==null)return null;
                return worlds.get(java.util.Arrays.asList(labels).indexOf(selection));
            }catch(IOException failure){
                JOptionPane.showMessageDialog(null,failure.getMessage(),"World load error",
                    JOptionPane.ERROR_MESSAGE);return null;
            }
        }
        JTextField name=new JTextField("New Settlement",18);
        JTextField seedField=new JTextField("",18);
        JCheckBox hardcore=new JCheckBox("Hardcore character reset");
        JComboBox<String> enemy=new JComboBox<>(new String[]{"normal","low","high"});
        JComboBox<String> abundance=new JComboBox<>(new String[]{"normal","scarce","abundant"});
        JSpinner population=new JSpinner(new SpinnerNumberModel(6,0,24,1));
        JSpinner capacity=new JSpinner(new SpinnerNumberModel(multiplayer?12:1,1,32,1));
        capacity.setEnabled(multiplayer);
        JPanel panel=new JPanel(new GridLayout(0,2,6,4));
        panel.add(new JLabel("World name"));panel.add(name);
        panel.add(new JLabel("Seed (blank=random)"));panel.add(seedField);
        panel.add(new JLabel("Game mode"));panel.add(hardcore);
        panel.add(new JLabel("Enemy density (reserved)"));panel.add(enemy);
        panel.add(new JLabel("Resource abundance (reserved)"));panel.add(abundance);
        panel.add(new JLabel("AI characters"));panel.add(population);
        panel.add(new JLabel("Maximum players"));panel.add(capacity);
        if(JOptionPane.showConfirmDialog(null,panel,"Create World",
            JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=
            JOptionPane.OK_OPTION)return null;
        try{
            long seed=seedField.getText().isBlank()?
                new java.security.SecureRandom().nextLong():
                Long.parseLong(seedField.getText().trim());
            var config=new WorldCreationConfig(name.getText().trim(),seed,
                hardcore.isSelected(),multiplayer?"multi":"solo",
                (String)enemy.getSelectedItem(),(String)abundance.getSelectedItem(),
                multiplayer?(Integer)capacity.getValue():1,(Integer)population.getValue());
            config.saveNew();
            return config;
        }catch(IOException|IllegalArgumentException failure){
            JOptionPane.showMessageDialog(null,failure.getMessage(),"World creation error",
                JOptionPane.ERROR_MESSAGE);return null;
        }
    }
    public static void main(String[] args) {
        DesktopDiagnostics.install("desktop");
        DesktopDiagnostics.note("MENU_OPEN");
        SwingUtilities.invokeLater(()->{
            String[] modes={"SINGLE PLAYER","HOST MULTIPLAYER","JOIN MULTIPLAYER",
                "LEVEL EDITOR (DEV)"};
            int mode=JOptionPane.showOptionDialog(null,"THE MALEFIC VEIL — PROTOTYPE\nSelect play mode",
                "The Malefic Veil",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,modes,modes[0]);
            if(mode<0)return;
            DesktopDiagnostics.note("PLAY_MODE_SELECTED mode="+mode);
            if(mode==3){
                WorldCreationConfig editWorld=worldSelection(false);
                if(editWorld==null)return;
                try{
                    Class.forName("veil.gpu.GpuDesktop").getMethod("launchEditor",long.class)
                        .invoke(null,editWorld.seed());
                    DesktopDiagnostics.note("PORTABLE_LEVEL_EDITOR_LAUNCHED seed="+editWorld.seed());
                }catch(Exception problem){
                    DesktopDiagnostics.note("LEVEL_EDITOR_LAUNCH_FAILED "+problem);
                    JOptionPane.showMessageDialog(null,problem.getMessage(),
                        "Level editor launch failed",JOptionPane.ERROR_MESSAGE);
                }
                return;
            }
            WorldCreationConfig config=mode==2?null:worldSelection(mode==1);
            if(mode!=2&&config==null)return;
            CharacterRoster.Faction faction=(CharacterRoster.Faction)JOptionPane.showInputDialog(
                null,"Choose faction","Faction Selection",JOptionPane.PLAIN_MESSAGE,
                null,CharacterRoster.Faction.values(),CharacterRoster.Faction.THE_UNRAVELED);
            if(faction==null)return;
            java.util.List<FactionClasses.Role> choices=FactionClasses.faction(faction);
            String[] labels=choices.stream().map(role->role.name()+" — "+role.purpose()+
                (role.playable()?"":" [COMING SOON]")).toArray(String[]::new);
            Object selected=JOptionPane.showInputDialog(null,"Choose "+faction.display+" class",
                "Character Selection",JOptionPane.PLAIN_MESSAGE,null,labels,labels[0]);
            if(selected==null)return;
            int chosen=java.util.Arrays.asList(labels).indexOf(selected.toString());
            if(chosen<0)return;
            FactionClasses.Role role=choices.get(chosen);
            if(!role.playable()){
                JOptionPane.showMessageDialog(null,"This faction class awaits its gameplay and character model integration.",
                    "Class Not Yet Playable",JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int character=role.existingAbilityId();
            long seed=config==null?20261020L:config.seed();
            NetLink link=null;
            LanServer hostedServer=null;
            try {
                DesktopDiagnostics.note("SESSION_CONNECT_BEGIN");
                if(mode!=2){
                    HostAiSettings.Rules bots=new HostAiSettings.Rules(
                        config.aiPopulation(),HostAiSettings.Distribution.BALANCED,1,
                        1,0,0,0);
                    // Every solo and hosted world uses the same host-side
                    // transactions, durable realm snapshots, and faction rules.
                    int port=mode==0?0:7777;
                    hostedServer=new LanServer(port,seed,config.directory(),
                        config.hardcore(),bots);
                    link=new NetLink("127.0.0.1",hostedServer.port());
                } else {
                    String host=JOptionPane.showInputDialog(null,
                        "Host IP / hostname and port (default 7777)","127.0.0.1:7777");
                    if(host==null||host.isBlank())return;
                    String address=host.trim();int port=7777;
                    if(address.contains(":")){
                        int separator=address.lastIndexOf(':');
                        port=Integer.parseInt(address.substring(separator+1));
                        address=address.substring(0,separator);
                    }
                    link=new NetLink(address,port);
                }
            } catch(IOException|IllegalArgumentException ex) {
                DesktopDiagnostics.note("SESSION_CONNECT_FAILURE "+
                    ex.getClass().getSimpleName()+": "+ex.getMessage());
                JOptionPane.showMessageDialog(null,"LAN connection failed: "+ex.getMessage(),"Network error",JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(link!=null)seed=link.seed;
            DesktopDiagnostics.note("WORLD_SCENE_CONSTRUCTION_BEGIN");
            if(link!=null)link.selectFaction(faction);
            // GPU is the real desktop default. Software is a deliberately
            // selected fallback only, never an invisible renderer downgrade.
            if(!"software".equals(System.getProperty("veil.renderer"))){
                MaleficVeil game=null;
                try{
                    game=new MaleficVeil(null,seed,character,link);
                    game.gpuPresentation=true;
                    game.gpuHostedServer=hostedServer;
                    game.publishGpuFrame();
                    Class<?> desktop=Class.forName("veil.gpu.GpuDesktop");
                    desktop.getMethod("launchGame",MaleficVeil.class).invoke(null,game);
                    DesktopDiagnostics.note("GPU_WORLD_SCENE_LAUNCH_REQUESTED");
                }catch(Exception problem){
                    if(game!=null)game.gpuShutdown();
                    DesktopDiagnostics.note("GPU_WORLD_SCENE_FAILURE "+problem);
                    JOptionPane.showMessageDialog(null,
                        "OpenGL gameplay cannot launch: "+problem.getMessage()+
                        "\nThe software renderer is only available with -Dveil.renderer=software.",
                        "GPU startup failure",JOptionPane.ERROR_MESSAGE);
                }
            }else{
                JFrame frame=new JFrame("THE MALEFIC VEIL — SOFTWARE");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                MaleficVeil game=new MaleficVeil(frame,seed,character,link);
                frame.setContentPane(game);frame.setJMenuBar(game.managementMenu());
                frame.pack();frame.setLocationRelativeTo(null);frame.setVisible(true);
                game.requestFocusInWindow();
                DesktopDiagnostics.note("SOFTWARE_WORLD_SCENE_VISIBLE");
                if(link!=null)link.sceneReady();
            }
            DesktopDiagnostics.note("PLAY_SESSION_READY");
        });
    }
    private boolean down(int key){return held.contains(key);}
    private void toast(String msg){notice=msg;noticeTicks=85;}
    private double visibility(){
        double v=.36;
        int tileX=(int)px,tileY=(int)py;
        boolean avenue=world.detailedCity(px,py)?world.city.road(tileX/CityLayout.CELL,tileY/CityLayout.CELL):world.continent().road(world.parcelX(px),world.parcelY(py));
        v+=avenue?.25:-.14;
        if(down(KeyEvent.VK_SHIFT))v+=.29;
        if(down(KeyEvent.VK_C))v-=.28;
        if(attackFlash>0)v+=.38;
        if(archetype==2&&abilitySeconds>0)v-=.72;
        return Math.max(.02,Math.min(1,v));
    }
    /** The same movement and world collision work on either side of city limits. */
    private void slide(double dx,double dy){
        if(world.canWalk(px+dx,py))px+=dx;
        if(world.canWalk(px,py+dy))py+=dy;
    }
    @Override public void actionPerformed(ActionEvent unused){
        ticks++;
        if(localHarvesting!=null)localHarvesting.advance(33);
        // Clients render the HOST weather clock; solo games run their own
        // independent authoritative weather timeline. No client-side F9 cheat.
        if(net!=null){
            NetLink.WeatherSnapshot snapshot=net.weather();
            if(snapshot!=null&&snapshot!=appliedWeather){
                weather.applyHostWeather(snapshot.preset(),snapshot.dayFraction());
                appliedWeather=snapshot;
            }
        }
        if(ticks%30==1)weather.registerAround(world,px,py);
        weather.tick(.033,px,py,angle,VeilWeather.exposure(world,px,py));
        if(net!=null){
            String result;
            while((result=net.harvestResults.poll())!=null)
                toast(result.endsWith("|OK")?"HARVESTED "+result.substring(0,result.indexOf('|')):
                    "GATHER REJECTED: "+result.substring(result.indexOf('|')+1));
        }
        double step=.033;
        abilityCooldown=Math.max(0,abilityCooldown-step);
        abilitySeconds=Math.max(0,abilitySeconds-step);
        fireCooldown=Math.max(0,fireCooldown-step);
        attackFlash=Math.max(0,attackFlash-step);
        if(noticeTicks>0)noticeTicks--;
        if(net!=null) {
            String cargoMessage;
            while((cargoMessage=net.cargoResults.poll())!=null)
                toast(cargoMessage.endsWith("|OK")?
                    "TRUCK "+cargoMessage.substring(0,cargoMessage.indexOf('|'))+" COMPLETE":
                    "CARGO "+cargoMessage.substring(0,cargoMessage.indexOf('|'))+" REJECTED");
            String factoryMessage;
            while((factoryMessage=net.factoryResults.poll())!=null)
                toast("FACTORY "+factoryMessage.replace('_',' '));
            String storageMessage;
            while((storageMessage=net.campStockResults.poll())!=null)
                toast(storageMessage.equals("OK")?"CAMP CRATE UPDATED":
                    "STORAGE REJECTED: "+storageMessage);
            String factionMessage;
            while((factionMessage=net.factionResults.poll())!=null)
                toast(factionMessage.replace('|',' ').replace('_',' '));
            String transferNotice;
            while((transferNotice=net.itemResults.poll())!=null)
                toast(transferNotice.replace('|',' ').replace('_',' '));
            net.looseItems.values().removeIf(item->
                System.currentTimeMillis()>=item.expiry());
            String craftMessage;
            String arsenalMessage;
            while((arsenalMessage=net.arsenalResults.poll())!=null)
                if(!arsenalMessage.equals("FIRED"))
                    toast(arsenalMessage.replace('_',' '));
            Integer takenArsenal;
            while((takenArsenal=net.arsenalPicked.poll())!=null)
                if(takenArsenal>=0&&takenArsenal<arsenalVisualHost.arsenal.loot.size())
                    arsenalVisualHost.arsenal.loot.get(takenArsenal).taken=true;
            for(var tier:net.benchTiers.entrySet())
                if(tier.getKey()>=0&&tier.getKey()<world.devices.size())
                    world.devices.get(tier.getKey()).tier=tier.getValue();
            String usedSupply;
            while((usedSupply=net.consumeResults.poll())!=null)
                toast(usedSupply.endsWith("|OK")?
                    "CONSUMED "+usedSupply.substring(0,usedSupply.indexOf('|')).replace('_',' '):
                    "SUPPLY USE DENIED: "+usedSupply.substring(0,usedSupply.indexOf('|')).replace('_',' '));
            while((craftMessage=net.craftResults.poll())!=null)
                toast(craftMessage.endsWith("|OK")?
                    "CRAFTED "+craftMessage.substring(0,craftMessage.indexOf('|')):
                    "CRAFT DENIED: MISSING RESOURCES OR WORK TIME");
            if(respawnPending&&(!net.connected()||
                System.nanoTime()-respawnRequestAt>5_000_000_000L)){
                respawnPending=false;
                toast(net.connected()?"RESPAWN TIMED OUT — RETRY":"DISCONNECTED — RECONNECT REQUIRED");
            }
            NetLink.Self s=net.self();
            if(s!=null){
                if(s.dead()&&!dead){respawnPending=false;toast("YOU HAVE FALLEN — SELECT A RESPAWN POINT");}
                if(dead&&!s.dead()){respawnPending=false;px=s.x();py=s.y();toast("RETURNED FROM THE VEIL");}
                dead=s.dead();
                health=s.health();sanity=s.sanity();cash=s.cash();scrap=s.supplies();
                medkits=s.medkits();pistolReserve=s.pistolReserve();shellReserve=s.shellReserve();
                pistolClip=s.pistolClip();shellClip=s.shellClip();frags=s.frags();
                if(net.drivenCar()>=0||Math.hypot(px-s.x(),py-s.y())>1.0){
                    px=s.x();py=s.y();
                    if(net.drivenCar()>=0)angle=s.angle();
                }
            }
            String rideReply;
            while((rideReply=net.rideResults.poll())!=null)
                toast(rideReply.equals("OK")?"ENGINE ENGAGED":rideReply.equals("EXITED")?
                    "EXITED VEHICLE":"VEHICLE ACTION REJECTED");
            String respawnReply;
            while((respawnReply=net.respawnResults.poll())!=null)
                if(!respawnReply.equals("OK")){respawnPending=false;toast(respawnReply.equals("WAIT")?"RESPAWN COUNTDOWN ACTIVE":"RESPAWN LOCATION UNAVAILABLE");}
            NetLink.ShotEvent result;
            while((result=net.shots.poll())!=null){
                if(!result.fired())toast("SHOT REJECTED: "+result.outcome());
                else if(result.damage()>0)toast("CONFIRMED "+result.outcome()+" HIT "+result.damage());
            }
            NetLink.BuildEvent construction;
            while((construction=net.buildResults.poll())!=null){
                toast(construction.reason().equals("OK")?"CONSTRUCTION ACCEPTED":
                    construction.reason().equals("CITY_PROTECTED")?
                    "CITY IS IMMUTABLE — BUILD IN WILDERNESS":
                    "BUILD REJECTED: "+construction.reason());
            }
            String fieldResult;
            while((fieldResult=net.fieldResults.poll())!=null)toast("THROWN FIELD "+fieldResult);
            NetLink.FieldBurst fieldBurst;
            while((fieldBurst=net.fieldBursts.poll())!=null)
                fieldBurstVisuals.add(new FieldBurstVisual(fieldBurst.x(),fieldBurst.y(),
                    fieldBurst.faction(),ticks+25));
            Integer pickupIndex;
            while((pickupIndex=net.pickedUp.poll())!=null){
                if(pickupIndex>=0&&pickupIndex<world.pickups.size())world.pickups.get(pickupIndex).taken=true;
            }
            NetLink.DeviceEvent built;
            while((built=net.structures.poll())!=null){
                if(built.index()==world.devices.size()){
                    World.Device device=new World.Device(built.x(),built.y(),built.kind());
                    device.hp=built.hp();world.devices.add(device);
                }else if(built.index()>=0&&built.index()<world.devices.size()){
                    World.Device device=world.devices.get(built.index());
                    if(device.kind==built.kind()&&device.x==built.x()&&device.y==built.y())
                        device.hp=built.hp();
                }
            }
            NetLink.NpcEvent npc;
            while((npc=net.npcs.poll())!=null){
                if(npc.index()>=0&&npc.index()<world.npcs.size()){
                    World.Npc n=world.npcs.get(npc.index());
                    n.x=npc.x();n.y=npc.y();n.hp=npc.hp();
                }
            }
            if(ticks%3==0){
                if(net.drivenCar()>=0)net.drive((down(KeyEvent.VK_W)?1:0)-(down(KeyEvent.VK_S)?1:0),
                    (down(KeyEvent.VK_D)?1:0)-(down(KeyEvent.VK_A)?1:0));
                else net.state(px,py,angle,(down(KeyEvent.VK_C)?1:0)|(down(KeyEvent.VK_SHIFT)?2:0));
            }
            if(ticks%90==0)net.ping();
        }
        if(net==null){
            ArenaSystems.Player p=localHost.player(1);
            p.x=px;p.y=py;p.angle=angle;
            localHost.advance(33);
            for(ThrownDeployables.Burst burst:localHost.thrown.drainBursts())
                fieldBurstVisuals.add(new FieldBurstVisual(burst.x(),burst.y(),burst.faction(),ticks+25));
            localHost.thrown.drainRemoved();
        }
        if(!dead&&ticks%2==0&&(primaryHeld||secondaryHeld))shoot(secondaryHeld);
        if(!dead){
            int carId=net==null?localVehicles.carDrivenBy(1):net.drivenCar();
            if(carId>=0){
                if(net==null){
                    localVehicles.input(1,(down(KeyEvent.VK_W)?1:0)-(down(KeyEvent.VK_S)?1:0),
                        (down(KeyEvent.VK_D)?1:0)-(down(KeyEvent.VK_A)?1:0));
                    Vehicles.State car=localVehicles.state(carId);
                    if(car!=null){px=car.x();py=car.y();angle=car.heading();}
                }
            }else{
            if(down(KeyEvent.VK_LEFT))angle-=step*2.1;
            if(down(KeyEvent.VK_RIGHT))angle+=step*2.1;
            double forward=(down(KeyEvent.VK_W)?1:0)-(down(KeyEvent.VK_S)?1:0);
            double sideways=(down(KeyEvent.VK_D)?1:0)-(down(KeyEvent.VK_A)?1:0);
            double m=Math.hypot(forward,sideways);
            if(m>0){
                forward/=m;sideways/=m;
                double speed=down(KeyEvent.VK_C)?1.1:down(KeyEvent.VK_SHIFT)?4.2:2.8;
                slide((Math.cos(angle)*forward-Math.sin(angle)*sideways)*speed*step,
                    (Math.sin(angle)*forward+Math.cos(angle)*sideways)*speed*step);
            }
            }
            if(net==null){
            simulateNpc(step);
            for(World.Device d:world.devices) {
                if(d.hp<=0)continue;
                d.cooldown=Math.max(0,d.cooldown-step);
                if(d.kind==1&&Math.hypot(d.x-px,d.y-py)<1.8&&ticks%20==0){
                    health=Math.min(100,health+2);sanity=Math.min(100,sanity+1);
                }
                if(d.kind==0&&d.cooldown==0){
                    World.Npc target=null;double best=7;
                    for(World.Npc n:world.npcs){
                        double dist=Math.hypot(n.x-d.x,n.y-d.y);
                        if(n.hp>0&&dist<best&&world.lineClear(d.x,d.y,n.x,n.y)){target=n;best=dist;}
                    }
                    if(target!=null){target.hp-=18;d.cooldown=.45;}
                }
            }
            if(sanity<25&&ticks%33==0)health=Math.max(1,health-1);
            }
        }
        publishGpuFrame();
        if(!gpuPresentation&&(performance.frameCap>=30 || ticks%2==0))repaint();
    }
    private void simulateNpc(double dt){
        for(World.Npc n:world.npcs) {
            if(n.hp<=0)continue;
            double dx=px-n.x,dy=py-n.y,d=Math.hypot(dx,dy);
            if(d<11&&visibility()>.13&&world.lineClear(n.x,n.y,px,py)&&d>.7){
                double speed=.72*dt;
                double nx=n.x+dx/d*speed,ny=n.y+dy/d*speed;
                if(world.canWalk(nx,n.y))n.x=nx;
                if(world.canWalk(n.x,ny))n.y=ny;
            } else if(ticks%8==0){
                double dir=(World.mix(world.seed+ticks/8+n.hashCode())&1023)*Math.PI/512;
                double nx=n.x+Math.cos(dir)*.09,ny=n.y+Math.sin(dir)*.09;
                if(world.canWalk(nx,ny)){n.x=nx;n.y=ny;}
            }
            n.cooldown=Math.max(0,n.cooldown-dt);
            if(d<1.05&&n.cooldown<=0){
                hurt(8,9);n.cooldown=1.15;
            }else if(d<3&&ticks%45==0)sanity=Math.max(0,sanity-1);
        }
    }
    private void hurt(int h,int s){
        health=Math.max(0,health-h);sanity=Math.max(0,sanity-s);
        if(health==0){dead=true;toast("YOU HAVE FALLEN — 1/2/3 CHOOSE RESPAWN");}
    }
    private ArsenalSystems.View arsenalView(){
        return net==null?localHost.arsenal.view(1):net.arsenal();
    }
    private void shoot(){shoot(false);}
    private void shoot(boolean alternate){
        if(dead||arsenalMenuVisible)return;
        if(net!=null){net.arsenalFire(alternate);return;}
        ArenaSystems.Player player=localHost.player(1);
        player.x=px;player.y=py;player.angle=angle;
        ArsenalSystems.Shot shot=localHost.arsenal.fire(1,alternate,++arsenalSequence);
        if(shot.fired()){
            // Compatibility HUD and real-input regression reflect the same
            // host ammo accounting, never a detached two-gun local counter.
            ArsenalSystems.View confirmed=localHost.arsenal.view(1);
            pistolClip=confirmed.clip()[0];pistolReserve=confirmed.reserve()[0];
            attackFlash=.18;
            if(shot.damage()>0)toast("HIT "+shot.damage());
        }else if(!shot.reason().equals("COOLDOWN"))
            toast(shot.reason().replace('_',' '));
    }
    private void legacyShoot(){
        if(dead||fireCooldown>0)return;
        int clip=gun==0?pistolClip:shellClip;
        if(clip<=0){toast("EMPTY — R TO RELOAD");return;}
        if(gun==0)pistolClip--;else shellClip--;
        fireCooldown=gun==0?.22:.88;
        attackFlash=.18;
        if(net!=null){net.fire(gun);return;}
        World.Npc hit=null;double best=gun==0?13:4.5;
        double spread=gun==0?.065:.20;
        for(World.Npc n:world.npcs){
            if(n.hp<=0)continue;
            double dx=n.x-px,dy=n.y-py,dist=Math.hypot(dx,dy);
            if(dist>best||dist<.01||!world.lineClear(px,py,n.x,n.y))continue;
            double da=Math.atan2(Math.sin(Math.atan2(dy,dx)-angle),Math.cos(Math.atan2(dy,dx)-angle));
            if(Math.abs(da)<Math.atan2(.3,dist)+spread&&dist<best){best=dist;hit=n;}
        }
        if(hit!=null){
            hit.hp-=gun==0?27:Math.max(25,85-(int)(best*8));
            if(hit.hp<=0){cash+=25;frags++;toast("HOSTILE DOWN  +$25");}
        }
    }
    private void reload(){
        if(dead)return;
        if(net!=null){net.arsenalReload();return;}
        var result=localHost.arsenal.reload(1);
        toast(result.reason().replace('_',' '));
    }
    private void legacyReload(){
        if(dead)return;
        if(net!=null){net.reload(gun);return;}
        if(gun==0){
            int used=Math.min(12-pistolClip,pistolReserve);
            pistolReserve-=used;pistolClip+=used;
        }else{
            int used=Math.min(6-shellClip,shellReserve);
            shellReserve-=used;shellClip+=used;
        }
    }
    private void deploy(int kind){
        if(dead)return;
        // This build UI still runs inside the fixed city-only sector.
        // Tell the player why construction is unavailable without pretending
        // a vacant urban plot is wilderness.
        if(net!=null){net.build(kind);return;}
        if(world.detailedCity(px,py)&&world.city.worldStructureAt(px+Math.cos(angle)*1.1,
                                       py+Math.sin(angle)*1.1,.55)){
            toast("CITY IS IMMUTABLE — BUILD IN WILDERNESS");return;
        }
        int price=kind==0?(archetype==0?60:100):kind==1?75:kind==2?120:45;
        int cost=kind==0?3:kind==1?1:kind==2?2:1;
        if(cash<price||scrap<cost){toast("NEED $"+price+" AND "+cost+" SUPPLIES");return;}
        double x=px+Math.cos(angle)*1.1,y=py+Math.sin(angle)*1.1;
        if(world.worldStructureAt(x,y,.55)){toast("WORLD STRUCTURE PROTECTED");return;}
        if(!world.canWalk(x,y)){toast("BUILD SPACE BLOCKED");return;}
        for(World.Device d:world.devices)if(Math.hypot(x-d.x,y-d.y)<1.2){toast("TOO CLOSE TO ANOTHER DEVICE");return;}
        cash-=price;scrap-=cost;
        world.devices.add(new World.Device(x,y,kind));
        toast(kind==0?"TURRET DEPLOYED":kind==1?"HEAL STATION DEPLOYED":"REVIVE STATION DEPLOYED");
    }
    private void ability(){
        if(dead||abilityCooldown>0)return;
        abilityCooldown=18;
        if(net!=null){net.ability();return;}
        switch(archetype){
            case 0 -> {cash+=35;scrap++;toast("SALVAGE PROTOCOL +$35 +1 SUPPLY");}
            case 1 -> {health=Math.min(100,health+55);sanity=Math.min(100,sanity+20);toast("FIELD RECOVERY");}
            case 2 -> {abilitySeconds=7;toast("SHADE ACTIVE — LOW VISIBILITY");}
            case 3 -> {
                sanity=Math.min(100,sanity+45);
                for(World.Npc n:world.npcs)if(n.hp>0&&Math.hypot(n.x-px,n.y-py)<4)n.hp-=38;
                toast("PSYCHIC REPULSE");
            }
        }
    }
    private void respawnAt(int index){
        if(!dead)return;
        spawn=index;
        if(net!=null){
            if(net.respawnRemainingMs()>0){toast("RESPAWN COUNTDOWN ACTIVE");return;}
            if(net.spawnScores()[index]>=1000){toast("SPAWN POINT BLOCKED");return;}
            if(respawnPending)return;
            if(!net.connected()){toast("DISCONNECTED — RECONNECT REQUIRED");return;}
            respawnPending=true;respawnRequestAt=System.nanoTime();
            net.respawn(index);toast("RESPAWN REQUESTED");return;
        }
        spawn=index;px=world.spawns[index][0];py=world.spawns[index][1];
        health=100;sanity=75;dead=false;pistolClip=12;shellClip=6;
        pistolReserve=Math.max(24,pistolReserve);shellReserve=Math.max(12,shellReserve);
        if(net!=null)net.respawn(index);
        toast("RESPAWN: SECTOR "+(index+1));
    }
    private static String modelForResource(String type,int id){
        return switch(type){
            case "TIMBER" -> id%2==0?"resource-tree-broadleaf":"resource-tree-conifer";
            case "FIELD_STONE" -> "resource-stone";
            case "RAW_ORE" -> "resource-ore";
            case "FIBER" -> "resource-fiber";
            case "EDIBLES" -> "resource-edibles";
            case "MEDICINAL_HERBS" -> "resource-herbs";
            case "DIRTY_WATER" -> "resource-water";
            default -> throw new IllegalArgumentException("Unknown harvestable type "+type);
        };
    }
    private java.util.List<InteractionFocus.Candidate> resourceCandidates(){
        java.util.List<InteractionFocus.Candidate> list=new java.util.ArrayList<>();
        if(net!=null){
            for(NetLink.ResourceNode n:net.resourceNodes.values())
                if(n.remaining()>0&&Math.hypot(n.x()-px,n.y()-py)<3)
                    list.add(new InteractionFocus.Candidate(InteractionFocus.Kind.RESOURCE,
                        n.id(),n.type().replace('_',' '),n.x(),n.y(),.45,2.1));
        }else{
            for(var node:localHarvesting.nearby(px,py,3))if(node.remaining()>0)
                list.add(new InteractionFocus.Candidate(InteractionFocus.Kind.RESOURCE,
                    node.id(),node.type().name().replace('_',' '),
                    localHarvesting.nodeX(node),localHarvesting.nodeY(node),.45,2.1));
        }
        return list;
    }
    private InteractionFocus.Target focusTarget(){
        if(dead)return null;
        java.util.List<InteractionFocus.Candidate> objects=new java.util.ArrayList<>();
        for(int i=0;i<world.pickups.size();i++){
            var p=world.pickups.get(i);
            if(!p.taken&&Math.hypot(p.x-px,p.y-py)<1.2)
                objects.add(new InteractionFocus.Candidate(InteractionFocus.Kind.PICKUP,
                    i,InteractionFocus.pickupName(p.kind),p.x,p.y,.30,.75));
        }
        if(net!=null){
            for(var car:net.cars.values())if(car.driverId()<0&&car.hp()>0)
                objects.add(new InteractionFocus.Candidate(InteractionFocus.Kind.VEHICLE,
                    car.id(),"VEHICLE",car.x(),car.y(),.55,2.1));
            for(var yard:net.cargoYards.values())if(yard.stock()>0)
                objects.add(new InteractionFocus.Candidate(InteractionFocus.Kind.CARGO_YARD,
                    yard.id(),"CARGO",yard.x()+.5,yard.y()+.5,.55,2.5));
        }else{
            for(var car:localVehicles.states())if(car.driverId()<0&&car.hp()>0)
                objects.add(new InteractionFocus.Candidate(InteractionFocus.Kind.VEHICLE,
                    car.id(),"VEHICLE",car.x(),car.y(),.55,2.1));
            for(var yard:localCargo.sites())if(yard.stock()>0)
                objects.add(new InteractionFocus.Candidate(InteractionFocus.Kind.CARGO_YARD,
                    yard.id(),"CARGO",yard.x()+.5,yard.y()+.5,.55,2.5));
        }
        objects.addAll(resourceCandidates());
        return InteractionFocus.pick(px,py,angle,objects,
            (x,y,a,b)->world.lineClear(x,y,a,b));
    }
    /** Camera can freely look above/below eye level without changing world collision. */
    private int horizon(){
        double focal=RW/(2*Math.tan(FOV/2));
        return (int)Math.max(-RH*4,Math.min(RH*5,
            Math.round(RH/2.0+Math.tan(cameraPitch)*focal)));
    }
    /** Perspective ground texels use genuine source-kit surface colors and the
     * same continent biome projection. No eye-level flat plane is drawn. */
    private void drawPerspectiveGround(Graphics2D g){
        double focal=RW/(2*Math.tan(FOV/2));
        int start=Math.max(0,horizon());
        for(int row=start;row<RH;row+=3){
            double downward=Math.atan((row+1.5-RH*.5)/focal)-cameraPitch;
            if(downward<=0)continue;
            double distance=Math.min(170,1.65/Math.tan(downward));
            for(int col=0;col<RW;col+=3){
                double offset=Math.atan(((col+1.5)/RW-.5)*2*Math.tan(FOV/2));
                double d=distance/Math.cos(offset),ray=angle+offset;
                double wx=px+Math.cos(ray)*d,wz=py+Math.sin(ray)*d;
                int sx=(int)Math.floor(wx),sz=(int)Math.floor(wz);
                int rgb=0x1d1c1d;
                if(sx>=0&&sz>=0&&sx<World.SIZE&&sz<World.SIZE){
                    int authored=groundAlbedo[sx][sz];
                    if(authored!=0)rgb=authored;
                }else{
                    var type=world.continent().biome(world.parcelX(wx),world.parcelY(wz));
                    rgb=switch(type){
                        case FOREST -> 0x455a3b; case PLAINS -> 0x68754a;
                        case HILLS -> 0x655d4c; case MOUNTAINS -> 0x696a6d;
                        case MARSH -> 0x3c5556; case BEACH -> 0x8a8063;
                        case WATER -> 0x344e70;
                    };
                }
                g.setColor(new Color(weather.tint(wx,wz,rgb)));
                g.fillRect(col,row,3,3);
            }
        }
    }
    /** Walkable streamed continent scene, using imported GLB trees and real 3D node meshes. */
    private void renderContinentalGround(Graphics2D g){
        int horizon=horizon();
        g.setColor(new Color(38,37,62));g.fillRect(0,0,RW,Math.max(0,Math.min(RH,horizon)));
        weather.paintSky(g,RW,RH);
        urbanRenderer.setCamera(1.65,cameraPitch);
        ContinentLayout terrain=world.continent();
        double tangent=Math.tan(FOV/2);
        for(int row=Math.max(0,horizon);row<RH;row++){
            double distance=Math.min(150,1.65*RW/(2*Math.tan(FOV/2)) /
                Math.max(1,row-horizon));
            for(int col=0;col<RW;col++){
                double ray=angle+Math.atan(((col+.5)/RW-.5)*2*tangent);
                int x=(int)Math.floor(px+Math.cos(ray)*distance);
                int y=(int)Math.floor(py+Math.sin(ray)*distance);
                var biome=terrain.biome(world.parcelX(x),world.parcelY(y));
                int base=switch(biome){
                    case FOREST -> 0x455A3B;
                    case PLAINS -> 0x68754A;
                    case HILLS -> 0x655D4C;
                    case MOUNTAINS -> 0x696A6D;
                    case MARSH -> 0x3C5556;
                    case BEACH -> 0x8A8063;
                    case WATER -> 0x344E70;
                };
                int dim=(((x*7+y*13)&3)-1)*4;
                int rr=Math.max(0,((base>>>16)&255)+dim);
                int gg=Math.max(0,((base>>>8)&255)+dim);
                int bb=Math.max(0,(base&255)+dim);
                int ground=weather.tint(x+.5,y+.5,
                    (Math.min(255,rr)<<16)|(Math.min(255,gg)<<8)|Math.min(255,bb));
                g.setColor(new Color(ground));
                g.fillRect(col,row,1,1);
            }
        }
        java.util.List<UrbanScene.ModelInstance> displays=new java.util.ArrayList<>();
        if(net!=null){
            for(var node:net.resourceNodes.values())
                if(node.remaining()>0&&Math.hypot(node.x()-px,node.y()-py)<21)
                    displays.add(new UrbanScene.ModelInstance(
                        modelForResource(node.type(),node.id()),node.x(),node.y(),0,
                        node.type().equals("TIMBER")?.39:1.05));
        }else{
            for(var node:localHarvesting.nearby(px,py,21))
                if(node.remaining()>0)
                    displays.add(new UrbanScene.ModelInstance(
                        modelForResource(node.type().name(),node.id()),
                        localHarvesting.nodeX(node),localHarvesting.nodeY(node),0,
                        node.type()==WildernessResources.Type.TIMBER?.39:1.05));
        }
        if(net!=null){
            for(var entry:net.others.entrySet()){
                var remote=entry.getValue();
                if(remote.hp>0&&Math.hypot(remote.x-px,remote.y-py)<21)
                    displays.add(new UrbanScene.ModelInstance(
                        actorMesh(entry.getKey(),remote.classId),
                        remote.x,remote.y,remote.angle-Math.PI/2,1.0,remote.opacity));
            }
        }
        for(var npc:world.npcs)if(npc.hp>0&&Math.hypot(npc.x-px,npc.y-py)<21)
            displays.add(new UrbanScene.ModelInstance("enemy-walker",npc.x,npc.y,
                Math.atan2(py-npc.y,px-npc.x)-Math.PI/2,1.0));
        java.util.Arrays.fill(depth,65);
        urbanRenderer.draw(g,urban,px,py,angle,depth,RW,RH);
        displays.addAll(fieldVisuals());
        urbanRenderer.draw(g,actorModels.meshes(),displays,px,py,angle,depth,RW,RH);
        renderWeatherEffects(g);
        g.setColor(new Color(204,186,157));
        g.fillRect(RW/2-4,RH/2,9,1);g.fillRect(RW/2,RH/2-4,1,9);
        InteractionFocus.draw(g,RW,RH,focusTarget());
        g.setColor(new Color(229,214,178));g.setFont(new Font(Font.MONOSPACED,Font.BOLD,8));
        g.drawString("CONTINENT  "+(int)px+","+(int)py+"   E INTERACT",5,10);
    }
    /** Temporary roster-consistent silhouettes until dedicated faction NPC art lands. */
    private String actorMesh(int actorId,int classId){
        if(net!=null){
            var ai=net.aiActors.get(actorId);
            if(ai!=null){
                return switch(ai.faction()){
                    case "THE_BEGOTTEN" -> "enemy-walker";
                    case "THE_PENITENT" -> GameplayMeshes.HERO[2];
                    default -> GameplayMeshes.HERO[0];
                };
            }
        }
        return GameplayMeshes.HERO[Math.max(0,Math.min(3,classId))];
    }
    private void interactTarget(){
        var t=focusTarget();
        if(t==null){toast("NO OBJECT IN SIGHT");return;}
        switch(t.kind()){
            case PICKUP -> {
                if(net!=null){net.state(px,py,angle,0);net.pickup(t.id());}
                else{
                    World.Pickup item=world.pickups.get(t.id());
                    if(item.taken)return;
                    item.taken=true;
                    switch(item.kind){
                        case 0 -> {health=Math.min(100,health+30);toast("+30 HEALTH");}
                        case 1 -> {sanity=Math.min(100,sanity+30);toast("+30 SANITY");}
                        case 2 -> {pistolReserve+=24;toast("+24 ROUNDS");}
                        case 3 -> {shellReserve+=10;toast("+10 SHELLS");}
                        case 4 -> {cash+=50;toast("+$50");}
                        case 5 -> {scrap+=2;toast("+2 SUPPLIES");}
                        case 6 -> {medkits++;toast("+1 MEDKIT");}
                    }
                }
            }
            case VEHICLE -> toggleCar();
            case CARGO_YARD -> loadNearestCargo();
            case CARGO_DROP -> recoverNearestCargo();
            case RESOURCE -> {
                if(net!=null)net.harvest(t.id());
                else {
                    ArenaSystems.Player actor=localHost.player(1);
                    actor.x=px;actor.y=py;actor.angle=angle;
                    var result=localHarvesting.harvest(actor,t.id());
                    toast(result.accepted()?"+1 "+result.item().name():result.reason());
                }
            }
        }
    }
    /** Actual shared-module weather: near-field 3D lightning, rain, snow and fog. */
    private void renderWeatherEffects(Graphics2D g){
        java.util.List<UrbanScene.ModelInstance> lightning=new java.util.ArrayList<>();
        weather.emitLightning(lightning);
        if(!lightning.isEmpty())
            urbanRenderer.draw(g,actorModels.meshes(),lightning,px,py,angle,depth,RW,RH);
        weather.paintOverlay(g,RW,RH);
    }
    private void renderScene(Graphics2D g){
        if(!world.detailedCity(px,py)){renderContinentalGround(g);return;}
        int horizon=horizon();
        g.setColor(new Color(14,14,27));
        g.fillRect(0,0,RW,Math.max(0,Math.min(RH,horizon)));
        weather.paintSky(g,RW,RH);
        drawPerspectiveGround(g);
        urbanRenderer.setCamera(1.65,cameraPitch);
        for(int i=0;i<RW;i++){
            double offset=Math.atan(((i+.5)/RW-.5)*2*Math.tan(FOV/2));
            double ray=angle+offset,rx=Math.cos(ray),ry=Math.sin(ray);
            double distance=.04,hitX=px,hitY=py;
            while(distance<110){
                hitX=px+rx*distance;hitY=py+ry*distance;
                if(world.solid(hitX,hitY))break;
                distance+=distance<13?.055:distance<42?.14:.30;
            }
            depth[i]=distance;
            double corrected=Math.max(.14,distance*Math.cos(offset));
            int h=(int)Math.min(1500,RH*.7/corrected);
            int top=horizon-h/2;
            int tx=(int)Math.floor(hitX),ty=(int)Math.floor(hitY);
            int shade=Math.max(13,165-(int)(corrected*6));
            boolean concrete=(tx/12+ty/12)%3==0;
            int r=(int)(shade*(concrete?.75:.53));
            int gg=(int)(shade*(concrete?.76:.50));
            int b=(int)(shade*(concrete?.76:.54));
            g.setColor(new Color(r,gg,b));g.drawLine(i,Math.max(0,top),i,Math.min(RH,top+h));
            if((tx+ty)%5==0){g.setColor(new Color(r/2,gg/2,b/2));g.drawLine(i,Math.max(0,top+h/2),i,Math.min(RH,top+h/2));}
        }
        urbanRenderer.draw(g,urban,px,py,angle,depth,RW,RH);
        java.util.List<UrbanScene.ModelInstance> cars=new java.util.ArrayList<>();
        if(net!=null){
            for(NetLink.Car car:net.cars.values())if(car.hp()>0&&car.driverId()!=net.id)
                cars.add(new UrbanScene.ModelInstance(
                    (car.id()%2==0?"truck-green":"truck-grey")+
                        (net.truckCargo.containsKey(car.id())?"-cargo":""),
                    car.x(),car.y(),car.heading(),1.9));
        }else{
            for(Vehicles.State car:localVehicles.states())if(car.hp()>0&&car.driverId()!=1)
                cars.add(new UrbanScene.ModelInstance(localVehicles.cargo(car.id()).model(),
                    car.x(),car.y(),car.heading(),1.9));
        }
        urbanRenderer.draw(g,urban.models(),cars,px,py,angle,depth,RW,RH);
        // Dynamic licensed Kenney pallets: only stocked yards show resources;
        // a wrecked truck drops one recoverable ground item, not an invisible
        // inventory entry. The network is authoritative for both lists.
        java.util.List<UrbanScene.ModelInstance> cargoMeshes=new java.util.ArrayList<>();
        if(net!=null){
            for(NetLink.CargoYard yard:net.cargoYards.values())if(yard.stock()>0)
                cargoMeshes.add(new UrbanScene.ModelInstance(
                    yard.type().equals("METAL_STOCK")?"pallet":"pallet-small",
                    yard.x()+.5,yard.y()+.5,0,1.2));
            for(NetLink.CargoDrop drop:net.cargoDrops.values())
                cargoMeshes.add(new UrbanScene.ModelInstance(
                    drop.type().equals("METAL_STOCK")?"pallet":"pallet-small",
                    drop.x(),drop.y(),0,1.0));
        }else{
            for(CargoHaulage.Site yard:localCargo.sites())if(yard.stock()>0)
                cargoMeshes.add(new UrbanScene.ModelInstance(
                    yard.groundMesh(),yard.x()+.5,yard.y()+.5,0,1.2));
            for(CargoHaulage.Drop drop:localCargo.drops())
                cargoMeshes.add(new UrbanScene.ModelInstance(
                    drop.resource().equals("METAL_STOCK")?"pallet":"pallet-small",
                    drop.x(),drop.y(),0,1.0));
        }
        urbanRenderer.draw(g,urban.models(),cargoMeshes,px,py,angle,depth,RW,RH);
        java.util.List<UrbanScene.ModelInstance> gameplay=new java.util.ArrayList<>();
        for(World.Pickup p:world.pickups)if(!p.taken&&p.kind>=0&&p.kind<GameplayMeshes.PICKUP.length)
            gameplay.add(new UrbanScene.ModelInstance(GameplayMeshes.PICKUP[p.kind],p.x,p.y,
                ticks*.018+p.kind,1.15,1.0,.86+.10*Math.sin(ticks*.11+p.kind)));
        gameplay.addAll(fieldVisuals());
        for(World.Device d:world.devices)if(d.hp>0&&d.kind>=0&&d.kind<GameplayMeshes.DEVICE.length)
            gameplay.add(new UrbanScene.ModelInstance(GameplayMeshes.DEVICE[d.kind],d.x,d.y,0,1.0));
        for(World.Npc n:world.npcs)if(n.hp>0)
            gameplay.add(new UrbanScene.ModelInstance("enemy-walker",n.x,n.y,
                Math.atan2(py-n.y,px-n.x)-Math.PI/2,1.0));
        if(net!=null)for(java.util.Map.Entry<Integer,NetLink.Remote> entry:net.others.entrySet()){
            NetLink.Remote p=entry.getValue();
            if(p.hp>0&&net.cars.values().stream().noneMatch(car->car.driverId()==entry.getKey()))
                gameplay.add(new UrbanScene.ModelInstance(
                    actorMesh(entry.getKey(),p.classId),
                    p.x,p.y,p.angle-Math.PI/2,1.0,p.opacity));
        }
        // Material nodes outside City A remain visible through the ordinary
        // city camera when standing near a real border crossing.
        if(net!=null){
            for(var node:net.resourceNodes.values())
                if(node.remaining()>0&&Math.hypot(node.x()-px,node.y()-py)<19)
                    gameplay.add(new UrbanScene.ModelInstance(modelForResource(
                        node.type(),node.id()),node.x(),node.y(),0,
                        node.type().equals("TIMBER")?.39:1.05));
        }else{
            for(var node:localHarvesting.nearby(px,py,19))if(node.remaining()>0)
                gameplay.add(new UrbanScene.ModelInstance(modelForResource(
                    node.type().name(),node.id()),localHarvesting.nodeX(node),
                    localHarvesting.nodeY(node),0,
                    node.type()==WildernessResources.Type.TIMBER?.39:1.05));
        }
        urbanRenderer.draw(g,actorModels.meshes(),gameplay,px,py,angle,depth,RW,RH);
        java.util.List<UrbanScene.ModelInstance> arsenalLoot=new java.util.ArrayList<>();
        for(var loot:arsenalVisualHost.arsenal.loot)
            if(!loot.taken&&loot.type==0)
                arsenalLoot.add(new UrbanScene.ModelInstance(
                    arsenalMeshes.worldId(loot.value),loot.x,loot.y,ticks*.018,1.0));
        urbanRenderer.draw(g,arsenalMeshes.worldModels(),arsenalLoot,px,py,angle,depth,RW,RH);
        ArsenalEffects.draw(g,net==null?localHost.arsenal.effects():net.arsenalEffects(),
            world,px,py,angle,RW,RH);
        renderWeatherEffects(g);
        drawMonsterThreats(g);
        g.setColor(new Color(187,176,149));
        g.fillRect(RW/2-5,RH/2,11,1);g.fillRect(RW/2,RH/2-5,1,11);
        int bob=(int)(Math.sin(ticks*.15)*(down(KeyEvent.VK_W)?2:0));
        ArsenalSystems.View current=arsenalView();
        int selected=current==null?0:current.selected();
        long elapsed=current==null?0:
            (net==null?localHost.time():System.nanoTime()/1_000_000)-current.actionAt();
        arsenalMeshes.draw(g,selected,current,elapsed,RW,RH,bob);
        InteractionFocus.draw(g,RW,RH,focusTarget());
        if(attackFlash>0){
            g.setColor(new Color(255,207,85));
            g.fillOval(RW/2-12,RH-64+bob,24,22);
        }
        if(sanity<35){
            int a=(int)((35-sanity)*1.9);
            g.setColor(new Color(65,30,91,Math.min(100,a)));
            g.fillRect(0,0,RW,RH);
        }
    }
    /** Server-ranked creature danger at a glance; never trusts client-authored levels. */
    private void drawMonsterThreats(Graphics2D g){
        int playerLevel=net!=null&&net.rpg()!=null?net.rpg().level():1;
        int drawn=0;
        Font original=g.getFont();
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,8));
        for(int index=0;index<world.npcs.size()&&drawn<7;index++){
            World.Npc monster=world.npcs.get(index);
            if(monster.hp<=0)continue;
            double dx=monster.x-px,dy=monster.y-py,d=Math.hypot(dx,dy);
            if(d<=.35||d>=15)continue;
            double relative=Math.atan2(Math.sin(Math.atan2(dy,dx)-angle),
                Math.cos(Math.atan2(dy,dx)-angle));
            if(Math.abs(relative)>=FOV*.48)continue;
            int column=(int)(RW/2+Math.tan(relative)/Math.tan(FOV/2)*RW/2);
            if(column<0||column>=RW||d>=depth[column]-.1)continue;
            NetLink.NpcRank rank=net==null?null:net.npcRanks.get(index);
            int enemyLevel=rank==null?CharacterProgression.monsterLevel(world.seed,monster):
                rank.level();
            CharacterProgression.ThreatMark threat=CharacterProgression.threat(playerLevel,enemyLevel);
            int barWidth=33,center=Math.max(17,Math.min(RW-18,column));
            int top=Math.max(10,Math.min(RH-26,RH/2-(int)(62/d)-14));
            g.setColor(new Color(10,12,17,205));
            g.fillRect(center-36,top-13,72,23);
            g.setColor(Color.decode(threat.colorHex()));
            String archetype=monster.kind.replace("lost-","").toUpperCase();
            String label=threat.icon()+" LV"+enemyLevel+" "+archetype;
            g.drawString(label,center-Math.min(35,label.length()*2),top-4);
            g.setColor(new Color(55,54,61));g.fillRect(center-16,top,barWidth,3);
            g.setColor(Color.decode(threat.colorHex()));
            g.fillRect(center-16,top,(int)Math.ceil(barWidth*Math.max(0,monster.hp)/80.0),3);
            drawn++;
        }
        g.setFont(original);
    }
    private void sprite(Graphics2D g,double x,double y,int rgb,int scale,boolean humanoid){
        sprite(g,x,y,rgb,scale,humanoid,1.0);
    }
    private void sprite(Graphics2D g,double x,double y,int rgb,int scale,boolean humanoid,double opacity){
        opacity=ArenaSystems.opacity(opacity);
        if(opacity==0)return;
        double dx=x-px,dy=y-py,d=Math.hypot(dx,dy);
        if(d<.15||d>27)return;
        double theta=Math.atan2(Math.sin(Math.atan2(dy,dx)-angle),Math.cos(Math.atan2(dy,dx)-angle));
        if(Math.abs(theta)>FOV*.68)return;
        double sx=RW/2+Math.tan(theta)/Math.tan(FOV/2)*RW/2;
        int col=(int)Math.round(sx);
        if(col<0||col>=RW||d>=depth[col])return;
        int size=(int)Math.min(RH*2,scale/d);
        int width=humanoid?Math.max(3,size/3):Math.max(2,size/2);
        int top=humanoid?RH/2-size/2:RH/2+Math.max(1,32/(int)Math.max(1,d))-size/2;
        int left=col-width/2;
        Composite old=g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)opacity));
        g.setColor(new Color(rgb));
        g.fillRect(left,top,width,size);
        if(humanoid){
            g.setColor(new Color(15,16,25));
            g.fillRect(left+width/5,top+size/4,Math.max(1,width/5),Math.max(2,size/9));
            g.fillRect(left+width*3/5,top+size/4,Math.max(1,width/5),Math.max(2,size/9));
        }
        g.setComposite(old);
    }
    /** A disposed/reparented Swing view must not keep an event timer running. */
    @Override public void removeNotify(){
        timer.stop();
        weather.close();
        super.removeNotify();
    }
    @Override public void addNotify(){
        super.addNotify();
        if(timer!=null)timer.start();
    }
    @Override protected void paintComponent(Graphics raw){
        super.paintComponent(raw);
        Graphics2D g=(Graphics2D)raw.create();
        Graphics2D low=buffer.createGraphics();
        if(aircraftShowroom)
            AircraftShowroom.render(low,actorModels.meshes(),
                aircraftShowroomIndex,aircraftShowroomYaw);
        else renderScene(low);
        low.dispose();
        if(aircraftShowroom){
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(buffer,0,0,getWidth(),getHeight(),null);
            g.dispose();
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(buffer,0,0,getWidth(),getHeight(),null);
        int h=getHeight(),w=getWidth();
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,15));
        g.setColor(new Color(10,11,16,225));g.fillRect(0,h-70,w,70);
        g.setColor(new Color(215,215,207));
        g.drawString("HP "+health+"   SAN "+sanity+"   CASH $"+cash+"   SUP "+scrap+"   MED "+medkits+"   KILLS "+frags,105,h-44);
        ArsenalSystems.View arsenal=arsenalView();int equippedIndex=arsenal==null?0:arsenal.selected();
        g.drawString(ArsenalCatalog.get(equippedIndex).name()+" "+
            (arsenal==null?0:arsenal.clip()[equippedIndex])+"/"+
            (arsenal==null?0:arsenal.reserve()[equippedIndex])+
            "     "+names[archetype]+"  F "+(abilityCooldown==0?"READY":String.format("%.0fs",abilityCooldown)),105,h-17);
        double gem=net!=null&&net.self()!=null?net.self().visibility():visibility();
        drawGem(g,gem,31,h-35);
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,15));
        g.setColor(new Color(236,234,228));
        g.drawString("THE MALEFIC VEIL",15,22);
        g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        g.drawString("SEED "+world.seed+"  "+(net==null?"SOLO":"LAN PLAYERS "+(net.others.size()+1)),w-218,22);
        drawArsenalMenu(g,w,h);
        if(net!=null){
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
            String diagnostic=net.connected()?
                "HOST RTT "+(net.roundTripMs()<0?"--":net.roundTripMs()+"ms"):"HOST DISCONNECTED";
            g.setColor(net.connected()?new Color(181,196,177):new Color(247,107,105));
            g.drawString(diagnostic,w-218,39);
        }
        if(net!=null&&net.rpg()!=null){
            NetLink.Rpg progress=net.rpg();
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));
            g.setColor(net.hardcore()?new Color(237,99,102):new Color(171,221,179));
            g.drawString((net.hardcore()?"HARDCORE":"NORMAL")+"  LV "+progress.level()+
                "  STREAK "+String.format("%.1fx",progress.streakBonus())+
                "  "+net.faction().replace("THE_","").replace('_',' '),15,40);
            if(progress.protectionMs()>0){
                g.setColor(new Color(247,210,119));
                g.drawString("PROTECTED "+String.format("%.1fs",progress.protectionMs()/1000.0),15,58);
            }
        }
        if(noticeTicks>0){g.setFont(new Font(Font.MONOSPACED,Font.BOLD,17));g.setColor(new Color(234,202,132));g.drawString(notice,15,52);}
        if(inventoryVisible||skillsVisible)drawCharacterPanel(g,w,h);
        if(continentVisible)drawContinentMap(g,w,h);
        else if(mapVisible)drawMap(g,w,h);
        int activeCar=net==null?localVehicles.carDrivenBy(1):net.drivenCar();
        if(activeCar>=0){
            double speed=0,fuel=0;
            if(net==null){Vehicles.State car=localVehicles.state(activeCar);
                if(car!=null){speed=car.speed();fuel=car.fuel();}}
            else{NetLink.Car car=net.cars.get(activeCar);
                if(car!=null){speed=car.speed();fuel=car.fuel();}}
            g.setColor(new Color(229,211,152));
            String load=net==null?localVehicles.cargo(activeCar).resourceType():
                net.truckCargo.get(activeCar);
            int hp=net==null?localVehicles.state(activeCar).hp():
                net.cars.get(activeCar).hp();
            g.drawString(String.format("TRUCK %d  SPEED %.1f  FUEL %.0f%%  HULL %d/200",
                activeCar,speed,fuel,hp),15,70);
            g.drawString("CARGO "+(load==null?"EMPTY":load)+
                "   Q exit   Vehicles > Inspection",15,88);
        }
        if(help&&!dead){g.setColor(new Color(9,10,17,220));g.fillRect(12,64,440,100);
            g.setColor(Color.WHITE);g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
            g.drawString("WASD move / Mouse look (360 + vertical) / Shift sprint / C crouch",23,87);
            g.drawString("CLICK / RMB alternate  1-8 select  [/] cycle  R reload",23,107);
            g.drawString("E INTERACT / HARVEST  X MEDKIT  Q VEHICLE",23,127);
            g.drawString("I inventory K arsenal J field craft F8 aircraft F9 weather",23,147);}
        if(net!=null&&!net.connected()){
            g.setColor(new Color(0,0,0,220));g.fillRect(0,h/3,w,h/3);
            g.setColor(new Color(255,126,126));g.setFont(new Font(Font.MONOSPACED,Font.BOLD,23));
            g.drawString("HOST CONNECTION LOST",Math.max(15,w/2-170),h/2);
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,14));
            g.drawString("This match is no longer synchronized. Close to reconnect.",
                Math.max(15,w/2-235),h/2+27);
        }
        if(dead){
            int panelWidth=Math.min(590,w-30),left=(w-panelWidth)/2,top=Math.max(46,(h-282)/2);
            g.setColor(new Color(10,12,19,238));g.fillRoundRect(left,top,panelWidth,282,14,14);
            g.setColor(new Color(238,114,128));g.setFont(new Font(Font.MONOSPACED,Font.BOLD,24));
            g.drawString("YOU HAVE FALLEN",left+20,top+37);
            long wait=net==null?0:net.respawnRemainingMs();
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,14));
            g.setColor(wait>0?new Color(240,191,103):new Color(127,224,161));
            g.drawString(respawnPending?"DEPLOYING — AWAITING SERVER":wait>0?"RESPAWN IN "+String.format("%.1f",wait/1000.0)+"s":"RESPAWN READY",
                left+20,top+64);
            int[] risks=net==null?new int[]{0,0,0}:net.spawnScores();
            String[] labels={"NORTH","CENTER","SOUTH"};
            for(int i=0;i<3;i++){
                int score=risks[i],rowTop=top+79+i*52;
                boolean blocked=score>=1000,selected=spawn==i;
                g.setColor(selected?new Color(54,65,80):new Color(28,32,41));
                g.fillRoundRect(left+16,rowTop,panelWidth-32,46,8,8);
                g.setColor(blocked?new Color(238,114,128):
                    score>=55?new Color(240,154,111):score>=20?new Color(244,211,134):
                    new Color(133,218,164));
                g.setFont(new Font(Font.MONOSPACED,Font.BOLD,15));
                g.drawString("["+ (i+1) +"]  "+labels[i],left+30,rowTop+29);
                String status=blocked?"BLOCKED":score>=55?"DANGER":score>=20?"CAUTION":"SAFE";
                g.drawString(status,left+panelWidth-155,rowTop+29);
            }
            g.setColor(Color.WHITE);g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
            g.drawString("1–3 SELECT  /  CLICK ROW  /  ENTER DEPLOY",left+20,top+258);
            if(net!=null&&!net.connected())g.drawString("CONNECTION LOST — RECONNECT REQUIRED",left+20,top+275);
        }
        g.dispose();
    }
    private void drawGem(Graphics2D g,double v,int cx,int cy){
        int red=(int)(35+v*205),green=(int)(75+(1-v)*165);
        Polygon jewel=new Polygon(new int[]{cx,cx+22,cx,cx-22},new int[]{cy-26,cy,cy+26,cy},4);
        g.setColor(new Color(red,green,110));g.fillPolygon(jewel);
        g.setColor(Color.WHITE);g.drawPolygon(jewel);
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,10));
        g.setColor(Color.BLACK);g.drawString(""+(int)(v*100),cx-9,cy+4);
    }
    private void drawMap(Graphics2D g,int w,int h){
        int tile=3,left=w-World.SIZE*tile-16,top=35;
        g.setColor(new Color(8,9,13,225));g.fillRect(left-6,top-6,World.SIZE*tile+12,World.SIZE*tile+12);
        for(int x=0;x<World.SIZE;x++)for(int y=0;y<World.SIZE;y++) {
            CityLayout.Category category=world.city.categoryAt(x,y);
            Color tint=category==null?new Color(43,45,54):switch(category){
                case HAZARD -> new Color(184,77,73);
                case RESOURCE -> new Color(95,152,81);
                case SAFE -> new Color(75,132,172);
                case OPEN -> new Color(169,144,104);
            };
            g.setColor(world.tile(x,y)==0?tint:tint.darker().darker());
            g.fillRect(left+x*tile,top+y*tile,tile,tile);
        }
        for(double[] s:world.spawns){g.setColor(Color.YELLOW);g.fillRect(left+(int)(s[0]*tile)-2,top+(int)(s[1]*tile)-2,5,5);}
        g.setColor(Color.CYAN);g.fillOval(left+(int)(px*tile)-3,top+(int)(py*tile)-3,7,7);
    }
    /** Continental overview is a generation preview; actual traversal is still city-sector only. */
    private void drawContinentMap(Graphics2D g,int w,int h){
        ContinentLayout map=world.continent();
        int tile=2,left=w-ContinentLayout.SIZE*tile-16,top=35;
        g.setColor(new Color(8,9,13,225));
        g.fillRect(left-6,top-6,ContinentLayout.SIZE*tile+12,ContinentLayout.SIZE*tile+12);
        for(int x=0;x<ContinentLayout.SIZE;x++)for(int y=0;y<ContinentLayout.SIZE;y++){
            Color tint=switch(map.biome(x,y)){
                case WATER -> new Color(37,67,107);
                case BEACH -> new Color(170,149,108);
                case PLAINS -> new Color(99,145,78);
                case FOREST -> new Color(38,91,60);
                case HILLS -> new Color(121,121,91);
                case MOUNTAINS -> new Color(131,136,141);
                case MARSH -> new Color(81,107,91);
            };
            if(map.cityAt(x,y)!='\0')tint=map.worldStructureAt(x,y)?new Color(108,104,109):new Color(135,128,98);
            if(map.road(x,y))tint=new Color(192,186,165);
            g.setColor(tint);g.fillRect(left+x*tile,top+y*tile,tile,tile);
        }
        for(ContinentLayout.City city:map.cities()){
            g.setColor(Color.BLACK);g.fillOval(left+city.x()*tile-8,top+city.y()*tile-8,16,16);
            g.setColor(Color.WHITE);g.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));
            g.drawString(String.valueOf(city.id()),left+city.x()*tile-4,top+city.y()*tile+5);
        }
    }
    private void selectArsenal(int id){
        if(net!=null)net.arsenalSelect(id);
        else toast(localHost.arsenal.select(1,id).reason().replace('_',' '));
    }
    private void cycleArsenal(int delta){
        var view=arsenalView();if(view==null)return;
        for(int offset=1;offset<=32;offset++){
            int index=Math.floorMod(view.selected()+delta*offset,32);
            if((view.owned()&(1L<<index))!=0){selectArsenal(index);return;}
        }
    }
    private void arsenalCraft(int id){
        if(net!=null)net.arsenalCraft(id);
        else toast(localHost.arsenal.craft(1,id).reason().replace('_',' '));
    }
    private void arsenalUpgrade(){
        if(net!=null)net.arsenalUpgrade();
        else toast(localHost.arsenal.upgrade(1).reason().replace('_',' '));
    }
    private void drawArsenalMenu(Graphics2D g,int w,int h){
        if(!arsenalMenuVisible)return;
        var state=arsenalView();
        int x=Math.max(16,(w-650)/2),y=Math.max(35,(h-450)/2);
        g.setColor(new Color(8,10,16,243));g.fillRoundRect(x,y,650,450,12,12);
        g.setColor(new Color(224,218,201));g.setFont(new Font(Font.MONOSPACED,Font.BOLD,17));
        g.drawString("ARSENAL / FIELD FABRICATION",x+15,y+29);
        int owner=state==null?-1:state.faction();
        String name=owner<0?"UNENROLLED":ArsenalCatalog.FACTIONS[owner];
        g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        g.drawString(name+" / Bench "+(state==null?0:state.benchTier())+
            " / UP-DOWN SELECT  ENTER FABRICATE  E EQUIP  U UPGRADE  ESC CLOSE",
            x+15,y+52);
        int start=(arsenalMenuChoice/10)*10;
        for(int i=start;i<Math.min(start+10,32);i++){
            var item=ArsenalCatalog.get(i);
            boolean owned=state!=null&&(state.owned()&(1L<<i))!=0;
            boolean foreign=item.faction()>=0&&item.faction()!=owner;
            int row=y+83+(i-start)*31;
            if(i==arsenalMenuChoice){
                g.setColor(new Color(50,57,71));g.fillRect(x+12,row-18,625,26);
            }
            g.setColor(foreign?new Color(156,110,108):
                owned?new Color(118,209,179):new Color(224,218,201));
            g.drawString(String.format(java.util.Locale.ROOT,"%02d %-24s %s T%d %s",
                i+1,item.name(),item.mapSpawn()?"SCAVENGE":"FABRICATE",item.tier(),
                foreign?"FOREIGN":owned?"OWNED":"AVAILABLE"),x+21,row);
        }
        var item=ArsenalCatalog.get(arsenalMenuChoice);
        g.setColor(new Color(219,206,177));
        g.drawString(item.name()+" / "+(item.faction()<0?"SHARED":ArsenalCatalog.FACTIONS[item.faction()])+
            " / Primary "+item.primary().kind()+" / Alternate "+item.alternate().kind(),
            x+15,y+405);
        g.drawString("Cost: "+java.util.Arrays.toString(item.cost())+"  J: PLACE BENCH",x+15,y+426);
    }
    @Override public void keyPressed(KeyEvent e){
        int k=e.getKeyCode();
        boolean fresh=held.add(k);
        if(!fresh)return;
        // Offline visual inspection is not an aircraft unlock, spawn,
        // teleport, or exemption from host gameplay. No player input packet
        // or actor position change is sent while inspecting GLB geometry.
        if(k==KeyEvent.VK_F8){
            aircraftShowroom=!aircraftShowroom;
            held.remove(k);
            repaint();
            return;
        }
        if(aircraftShowroom){
            switch(k){
                case KeyEvent.VK_RIGHT -> aircraftShowroomIndex=
                    (aircraftShowroomIndex+1)%AircraftShowroom.count();
                case KeyEvent.VK_LEFT -> aircraftShowroomIndex=
                    (aircraftShowroomIndex+AircraftShowroom.count()-1)%
                    AircraftShowroom.count();
                case KeyEvent.VK_A -> aircraftShowroomYaw-=.19;
                case KeyEvent.VK_D -> aircraftShowroomYaw+=.19;
                case KeyEvent.VK_ESCAPE -> aircraftShowroom=false;
                default -> { }
            }
            held.remove(k);
            repaint();
            return;
        }
        if(arsenalMenuVisible){
            if(k==KeyEvent.VK_K||k==KeyEvent.VK_ESCAPE)arsenalMenuVisible=false;
            else if(k==KeyEvent.VK_UP)arsenalMenuChoice=Math.floorMod(arsenalMenuChoice-1,32);
            else if(k==KeyEvent.VK_DOWN)arsenalMenuChoice=(arsenalMenuChoice+1)%32;
            else if(k==KeyEvent.VK_ENTER)arsenalCraft(arsenalMenuChoice);
            else if(k==KeyEvent.VK_E)selectArsenal(arsenalMenuChoice);
            else if(k==KeyEvent.VK_U)arsenalUpgrade();
            else if(k==KeyEvent.VK_J)gpuPlaceWorkbench();
            return;
        }
        if(dead){
            if(k>=KeyEvent.VK_1&&k<=KeyEvent.VK_3)spawn=k-KeyEvent.VK_1;
            if(k==KeyEvent.VK_ENTER)respawnAt(spawn);
            return;
        }
        switch(k){
            case KeyEvent.VK_1,KeyEvent.VK_2,KeyEvent.VK_3,KeyEvent.VK_4,
                 KeyEvent.VK_5,KeyEvent.VK_6,KeyEvent.VK_7,KeyEvent.VK_8 ->
                selectArsenal(k-KeyEvent.VK_1);
            case KeyEvent.VK_OPEN_BRACKET -> cycleArsenal(-1);
            case KeyEvent.VK_CLOSE_BRACKET -> cycleArsenal(1);
            case KeyEvent.VK_R -> reload();
            case KeyEvent.VK_L -> loadNearestCargo();
            case KeyEvent.VK_PERIOD -> unloadNearestCargo();
            case KeyEvent.VK_G -> recoverNearestCargo();
            case KeyEvent.VK_F -> ability();
            case KeyEvent.VK_T -> deploy(0);
            case KeyEvent.VK_H -> deploy(1);
            case KeyEvent.VK_V -> deploy(2);
            case KeyEvent.VK_E -> interactTarget();
            case KeyEvent.VK_X -> {
                if(net!=null)net.useMedkit();
                else if(medkits>0&&health<100){medkits--;health=Math.min(100,health+55);toast("MEDKIT USED");}
            }
            case KeyEvent.VK_B -> deploy(3);
            case KeyEvent.VK_Z -> cycleField();
            case KeyEvent.VK_Y -> throwField();
            case KeyEvent.VK_Q -> toggleCar();
            case KeyEvent.VK_SPACE -> shoot();
            case KeyEvent.VK_M -> {mapVisible=!mapVisible;continentVisible=false;}
            case KeyEvent.VK_N -> {continentVisible=!continentVisible;mapVisible=false;}
            case KeyEvent.VK_I -> {inventoryVisible=!inventoryVisible;skillsVisible=false;}
            case KeyEvent.VK_K -> {arsenalMenuVisible=!arsenalMenuVisible;inventoryVisible=false;}
            case KeyEvent.VK_O -> crewDialog();
            case KeyEvent.VK_J -> handCraftDialog();
            case KeyEvent.VK_P -> scavengeNearby();
            case KeyEvent.VK_U -> collectNearby();
            case KeyEvent.VK_F9 -> toast(net==null?"WEATHER: "+weather.cycle():"HOST WEATHER: "+weather.current());
            case KeyEvent.VK_F10 -> performanceDialog();
            case KeyEvent.VK_TAB,KeyEvent.VK_ESCAPE -> help=!help;
        }
    }
    private void drawCharacterPanel(Graphics2D g,int width,int height){
        int x=Math.max(12,width-302),y=58,h=260;
        g.setColor(new Color(8,11,17,236));g.fillRoundRect(x,y,290,h,9,9);
        g.setColor(new Color(228,228,218));
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,15));
        g.drawString(inventoryVisible?"INVENTORY / CARRIED":"CHARACTER / ABILITIES",x+13,y+26);
        g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        int row=y+49;
        if(net==null){
            if(inventoryVisible){
                var items=localHost.player(1).survival.inventory();
                if(items.isEmpty())g.drawString("EMPTY — GATHER WITH E",x+13,row);
                for(var entry:items.entrySet()){
                    if(row>=y+h-32)break;
                    g.drawString(entry.getKey().name().replace('_',' ')+" x"+
                        entry.getValue(),x+13,row);
                    row+=21;
                }
            }else g.drawString("Solo character active",x+13,row);
        }else if(inventoryVisible){
            var items=new java.util.TreeMap<>(net.survivalInventory);
            if(items.isEmpty())g.drawString("EMPTY — LOST ON DEATH",x+13,row);
            for(var entry:items.entrySet()){
                if(row>=y+h-32)break;
                g.drawString(entry.getKey().replace('_',' ')+"  x"+entry.getValue(),x+13,row);
                row+=21;
            }
            g.drawString("Equipped gear is also lost on death",x+13,y+h-15);
        }else{
            NetLink.Rpg progress=net.rpg();
            if(progress!=null){
                String[] values={"LEVEL  "+progress.level()+"  XP "+progress.experience(),
                    "NEXT XP  "+progress.next(),
                    "MARKSMANSHIP   "+progress.marksmanship(),
                    "ATHLETICS      "+progress.athletics(),
                    "ENGINEERING    "+progress.engineering(),
                    "SCAVENGING     "+progress.scavenging(),
                    "SURVIVAL       "+progress.survival(),
                    "HUNGER "+progress.hunger()+"  HYDRATION "+progress.hydration(),
                    "LIFE "+progress.aliveMs()/60000+"m  BOOST "+String.format("%.1fx",progress.streakBonus())};
                for(String value:values){g.drawString(value,x+13,row);row+=21;}
                g.drawString(net.hardcore()?"DEATH RESETS ALL SKILLS":"DEATH RESETS LIFE BOOST",
                    x+13,y+h-15);
            }
        }
    }
    /** Lists only presently affordable hand recipes; the host still validates every debit. */
    private void handCraftDialog(){
        if(net==null&&localHost==null){toast("CRAFTING REQUIRES INVENTORY");return;}
        java.util.Map<String,Integer> possessed;
        if(net==null){
            possessed=new java.util.HashMap<>();
            for(var item:localHost.player(1).survival.inventory().entrySet())
                possessed.put(item.getKey().name(),item.getValue());
        }else possessed=net.survivalInventory;
        java.util.List<String> recipes=new java.util.ArrayList<>();
        boolean nearFire=net!=null&&world.devices.stream().anyMatch(d->
            d.hp>0&&d.kind==ArenaSystems.FIELD_FIRE&&
            Math.hypot(d.x-px,d.y-py)<=2.2);
        for(var recipe:SurvivalCrafting.recipes().values()){
            if(recipe.station()!=SurvivalCrafting.Station.HAND&&
               !(recipe.station()==SurvivalCrafting.Station.FIRE&&nearFire))continue;
            boolean affordable=true;
            for(var part:recipe.requires().entrySet())
                if(possessed.getOrDefault(part.getKey().name(),0)<part.getValue())
                    affordable=false;
            if(affordable)recipes.add(recipe.id());
        }
        java.util.Collections.sort(recipes);
        if(recipes.isEmpty()){
            toast("NO AFFORDABLE HAND RECIPES — GATHER WILDERNESS MATERIALS");
            return;
        }
        String chosen=(String)JOptionPane.showInputDialog(window,
            "Choose a hand recipe (server consumes all ingredients)",
            "Field Crafting",JOptionPane.PLAIN_MESSAGE,null,
            recipes.toArray(String[]::new),recipes.get(0));
        if(chosen!=null){
            if(net!=null){
                if(SurvivalCrafting.recipes().get(chosen).station()==SurvivalCrafting.Station.FIRE)
                    net.craftFire(chosen);
                else net.craftHand(chosen);
            }
            else toast(localHost.player(1).survival.craft(chosen,SurvivalCrafting.Station.HAND)?
                "CRAFTED "+chosen:"CRAFT DENIED");
        }
        requestFocusInWindow();
    }
    /** One compact real host management prompt, not a screen of inert controls. */
    private void crewDialog(){
        if(net==null){toast("CREWS REQUIRE A MULTIPLAYER ACCOUNT");return;}
        String[] choices={"CREATE","INVITE PLAYER ID","ACCEPT INVITE",
            "PROMOTE OFFICER","REMOVE MEMBER","LEAVE CREW",
            "TRANSFER LEADERSHIP","DISBAND CREW","CANCEL"};
        String choice=(String)JOptionPane.showInputDialog(window,
            "FACTION "+net.faction()+" / CREW "+net.crew()+" / "+net.crewRole(),
            "Crew Management",JOptionPane.PLAIN_MESSAGE,null,choices,choices[0]);
        if(choice==null)return;
        try{
            switch(choice){
                case "CREATE" -> {
                    String name=JOptionPane.showInputDialog(window,"New crew name (3-32 characters)");
                    if(name!=null)net.createCrew(name);
                }
                case "INVITE PLAYER ID" -> {
                    String id=JOptionPane.showInputDialog(window,"Same-faction player ID");
                    if(id!=null)net.inviteCrew(Integer.parseInt(id));
                }
                case "PROMOTE OFFICER" -> {
                    String id=JOptionPane.showInputDialog(window,"Current member player ID");
                    if(id!=null)net.promoteCrew(Integer.parseInt(id));
                }
                case "REMOVE MEMBER" -> {
                    String id=JOptionPane.showInputDialog(window,"Member player ID to remove");
                    if(id!=null)net.kickCrew(Integer.parseInt(id));
                }
                case "ACCEPT INVITE" -> net.acceptCrew();
                case "LEAVE CREW" -> net.leaveCrew();
                case "TRANSFER LEADERSHIP" -> {
                    String id=JOptionPane.showInputDialog(window,"Successor player ID");
                    if(id!=null)net.transferCrew(Integer.parseInt(id));
                }
                case "DISBAND CREW" -> {
                    if(JOptionPane.showConfirmDialog(window,"Disband this crew?","Crew",
                        JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)net.disbandCrew();
                }
                default -> {}
            }
        }catch(NumberFormatException invalid){toast("INVALID PLAYER ID");}
        requestFocusInWindow();
    }
    /** Compact graphics panel; local display preferences do not modify network simulation. */
    private void performanceDialog(){
        JSlider distance=new JSlider(8,180,performance.propDistance);
        JSlider detail=new JSlider(4,90,performance.highDetailDistance);
        JSlider models=new JSlider(24,700,performance.meshBudget);
        JSlider cap=new JSlider(15,30,performance.frameCap);
        JPanel panel=new JPanel(new GridLayout(4,2,8,3));
        panel.add(new JLabel("View distance"));panel.add(distance);
        panel.add(new JLabel("High detail range"));panel.add(detail);
        panel.add(new JLabel("Visible model budget"));panel.add(models);
        panel.add(new JLabel("Target FPS"));panel.add(cap);
        if(JOptionPane.showConfirmDialog(window,panel,"Graphics / LOD",JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        performance.apply(distance.getValue(),detail.getValue(),models.getValue(),cap.getValue());
        urbanRenderer.setBudgets(performance);
        // Simulation remains at 30 ticks/s regardless of the frame rendering cap.
        toast("GRAPHICS SETTINGS SAVED");
    }
    private java.util.Map<String,Integer> carried(){
        if(net!=null)return new java.util.TreeMap<>(net.survivalInventory);
        java.util.Map<String,Integer> map=new java.util.TreeMap<>();
        for(var e:localHost.player(1).survival.inventory().entrySet())
            if(e.getValue()>0)map.put(e.getKey().name(),e.getValue());
        return map;
    }
    private void inventoryAction(boolean trading){
        if(net==null){toast("ITEM HANDOFF REQUIRES MULTIPLAYER HOST");return;}
        var carried=carried();
        if(carried.isEmpty()){toast("INVENTORY EMPTY");return;}
        int recipient=-1;double distance=3;
        if(trading){
            for(var e:net.others.entrySet()){
                var other=e.getValue();double dx=other.x-px,dy=other.y-py;
                double range=Math.hypot(dx,dy);
                if(range>.01&&range<distance&&
                    dx*Math.cos(angle)+dy*Math.sin(angle)>=range*Math.cos(Math.PI/6)&&
                    world.lineClear(px,py,other.x,other.y)){
                    recipient=e.getKey();distance=range;
                }
            }
            if(recipient<0){toast("LOOK AT A PLAYER WITHIN 3 METRES");return;}
        }
        var keys=carried.keySet().toArray(String[]::new);
        String item=(String)JOptionPane.showInputDialog(window,"Item",
            trading?"Give to player "+recipient:"Drop item",
            JOptionPane.PLAIN_MESSAGE,null,keys,keys[0]);
        if(item==null)return;
        String quantity=JOptionPane.showInputDialog(window,
            "Quantity (1-"+carried.get(item)+")","1");
        if(quantity==null)return;
        try{
            int n=Integer.parseInt(quantity.trim());
            if(n<1||n>carried.get(item)){toast("INVALID QUANTITY");return;}
            if(trading)net.tradeItem(recipient,item,n);
            else net.dropItem(item,n);
        }catch(NumberFormatException invalid){toast("INVALID QUANTITY");}
    }
    private void scavengeNearby(){
        if(net==null){toast("SCAVENGING REQUIRES LAN HOST");return;}
        NetLink.SalvageNode best=null;double distance=3;
        for(var node:net.salvageNodes.values()){
            double d=Math.hypot(px-node.x(),py-node.y());
            if(!node.depleted()&&d<distance&&world.lineClear(px,py,node.x(),node.y())){
                best=node;distance=d;
            }
        }
        if(best==null){toast("NO CONTAINER IN REACH");return;}
        net.scavenge(best.id());
    }
    private void collectNearby(){
        if(net==null){toast("COLLECTION REQUIRES LAN HOST");return;}
        NetLink.LooseItem best=null;double distance=2.5;
        for(var item:net.looseItems.values()){
            double d=Math.hypot(px-item.x(),py-item.y());
            if(System.currentTimeMillis()<item.expiry()&&d<distance&&
                world.lineClear(px,py,item.x(),item.y())){
                best=item;distance=d;
            }
        }
        if(best==null){toast("NO DROPPED ITEM IN REACH");return;}
        net.takeGround(best.id());
    }
    /** One compact navigation surface; unavailable transactions are not fake buttons. */
    private JMenuBar managementMenu(){
        JMenuBar bar=new JMenuBar();
        JMenu player=new JMenu("Player");
        menuAction(player,"Inventory (I)",()->{inventoryVisible=true;skillsVisible=false;repaint();});
        menuAction(player,"Skills (K)",()->{skillsVisible=true;inventoryVisible=false;repaint();});
        menuAction(player,"Field crafting (J)",this::handCraftDialog);
        menuAction(player,"Drop item",()->inventoryAction(false));
        menuAction(player,"Give to aimed player",()->inventoryAction(true));
        menuAction(player,"Scavenge container (P)",this::scavengeNearby);
        menuAction(player,"Collect floor item (U)",this::collectNearby);
        JMenu worldMenu=new JMenu("World");
        menuAction(worldMenu,"City map (M)",()->{mapVisible=true;continentVisible=false;repaint();});
        menuAction(worldMenu,"Continent (N)",()->{continentVisible=true;mapVisible=false;repaint();});
        menuAction(worldMenu,"Crew management (O)",this::crewDialog);
        if(net!=null){
            menuAction(worldMenu,"Place camp (tent kit)",()->deploy(ArenaSystems.CAMP_STAKE));
            menuAction(worldMenu,"Place fire (campfire kit)",()->deploy(ArenaSystems.FIELD_FIRE));
        }
        JMenu vehicleMenu=new JMenu("Vehicles");
        menuAction(vehicleMenu,"Enter / exit (Q)",this::toggleCar);
        menuAction(vehicleMenu,"Vehicle inspection",this::vehicleInspection);
        menuAction(vehicleMenu,"Load cargo (L)",this::loadNearestCargo);
        menuAction(vehicleMenu,"Unload at camp (.)",this::unloadNearestCargo);
        menuAction(vehicleMenu,"Recover cargo (G)",this::recoverNearestCargo);
        JMenu production=new JMenu("Production");
        menuAction(production,"Industrial recipe catalog",this::productionCatalog);
        JMenu settings=new JMenu("Settings");
        menuAction(settings,"Graphics / performance (F10)",this::performanceDialog);
        menuAction(settings,"Controls",()->JOptionPane.showMessageDialog(window,
            "WASD move / drive    Q enter / exit    E interact\n"+
            "I inventory    J craft    K skills    O crew\n"+
            "M city map    N continent    F10 graphics\n"+
            "L load truck    G recover cargo    Esc help",
            "Controls",JOptionPane.INFORMATION_MESSAGE));
        for(JMenu menu:new JMenu[]{player,worldMenu,vehicleMenu,production,settings})
            bar.add(menu);
        return bar;
    }
    private void menuAction(JMenu menu,String title,Runnable action){
        JMenuItem item=new JMenuItem(title);
        item.addActionListener(e->{action.run();requestFocusInWindow();});
        menu.add(item);
    }
    /** Displays authoritative or local simulation state; no unimplemented repairs. */
    private void vehicleInspection(){
        int active=net==null?localVehicles.carDrivenBy(1):net.drivenCar();
        if(active<0){
            double nearest=3.5;
            if(net==null){
                for(var state:localVehicles.states()){
                    double d=Math.hypot(px-state.x(),py-state.y());
                    if(d<nearest){active=state.id();nearest=d;}
                }
            }else for(var state:net.cars.values()){
                double d=Math.hypot(px-state.x(),py-state.y());
                if(d<nearest){active=state.id();nearest=d;}
            }
        }
        if(active<0){toast("NO VEHICLE IN REACH");return;}
        double speed,fuel;int hp,driver;String cargo;
        if(net==null){
            var state=localVehicles.state(active);
            if(state==null)return;
            speed=state.speed();fuel=state.fuel();hp=state.hp();
            driver=state.driverId();
            cargo=localVehicles.cargo(active).resourceType();
        }else{
            var state=net.cars.get(active);
            if(state==null)return;
            speed=state.speed();fuel=state.fuel();hp=state.hp();
            driver=state.driverId();cargo=net.truckCargo.get(active);
        }
        JOptionPane.showMessageDialog(window,
            "Vehicle #"+active+"\nHull: "+hp+" / 200"+
            "\nFuel: "+String.format(java.util.Locale.ROOT,"%.1f",fuel)+"%"+
            "\nSpeed: "+String.format(java.util.Locale.ROOT,"%.1f",speed)+
            "\nDriver: "+(driver<0?"NONE":driver)+
            "\nCargo: "+(cargo==null?"EMPTY":cargo)+
            "\nRepair and component replacement: not yet available.",
            "Vehicle status",JOptionPane.INFORMATION_MESSAGE);
    }
    private void productionCatalog(){
        String[] choices=CraftingEraRegistry.processes().keySet().stream()
            .sorted().toArray(String[]::new);
        String id=(String)JOptionPane.showInputDialog(window,
            "Industrial process definitions (work orders require built facilities)",
            "Production catalog",JOptionPane.PLAIN_MESSAGE,null,choices,choices[0]);
        if(id==null)return;
        var recipe=CraftingEraRegistry.process(id);
        JOptionPane.showMessageDialog(window,
            "Facility: "+recipe.facility()+"    Era: "+recipe.era()+
            "\nFaction: "+recipe.faction()+"    Utility: "+recipe.utilityDemand()+
            "\nInputs: "+recipe.inputs()+"\nOutput: "+recipe.quantity()+" "+recipe.output()+
            "\nDuration: "+recipe.seconds()+" seconds"+
            "\nWorkstation controls require an installed production facility.",
            id,JOptionPane.INFORMATION_MESSAGE);
    }
    private void toggleCar(){
        if(net!=null){
            if(net.drivenCar()>=0){net.exitCar();return;}
            int chosen=-1;double best=2.0;
            for(NetLink.Car car:net.cars.values()){
                double dist=Math.hypot(px-car.x(),py-car.y());
                if(car.hp()>0&&car.driverId()<0&&dist<best){best=dist;chosen=car.id();}
            }
            if(chosen<0)toast("NO VEHICLE IN REACH");else net.ride(chosen);
        }else{
            if(localVehicles.carDrivenBy(1)>=0){
                double[] exited=localVehicles.exit(1);
                if(exited==null)toast("STOP AND CLEAR THE DOOR");
                else{px=exited[0];py=exited[1];toast("EXITED VEHICLE");}
                return;
            }
            int chosen=-1;double best=2.0;
            for(Vehicles.State car:localVehicles.states()){
                double dist=Math.hypot(px-car.x(),py-car.y());
                if(car.hp()>0&&car.driverId()<0&&dist<best){best=dist;chosen=car.id();}
            }
            if(chosen<0||!localVehicles.enter(1,px,py,chosen))toast("NO VEHICLE IN REACH");
            else toast("ENGINE ENGAGED");
        }
    }
    /** Pick up a heavy load only on foot next to both the yard and stopped truck. */
    private void loadNearestCargo(){
        if(net!=null){
            if(net.drivenCar()>=0){toast("EXIT TRUCK TO LOAD");return;}
            for(NetLink.CargoYard yard:net.cargoYards.values()){
                if(yard.stock()<=0||Math.hypot(px-yard.x()-.5,py-yard.y()-.5)>2.5)continue;
                for(NetLink.Car car:net.cars.values()){
                    if(car.driverId()!=-1||car.hp()<=80||Math.abs(car.speed())>.10||
                        net.truckCargo.containsKey(car.id())||
                        Math.hypot(car.x()-yard.x()-.5,car.y()-yard.y()-.5)>4.2)continue;
                    net.loadCargo(car.id(),yard.id());toast("REQUESTING CARGO LOAD");return;
                }
            }
        }else{
            if(localVehicles.carDrivenBy(1)>=0){toast("EXIT TRUCK TO LOAD");return;}
            for(CargoHaulage.Site yard:localCargo.sites()){
                if(yard.stock()==0||Math.hypot(px-yard.x()-.5,py-yard.y()-.5)>2.5)continue;
                for(Vehicles.State car:localVehicles.states())
                    if(localCargo.load(1,px,py,car.id(),yard.id())){
                        toast("LOADED "+yard.resource().replace('_',' '));return;
                    }
            }
        }
        toast("NO LOADING BAY / PARKED TRUCK IN REACH");
    }
    /** On-foot field transfer requires a loaded truck and an owned camp. */
    private void unloadNearestCargo(){
        if(net!=null){
            if(net.drivenCar()>=0){toast("EXIT TRUCK TO UNLOAD");return;}
            for(NetLink.Car car:net.cars.values())
                if(car.hp()>CargoHaulage.CARGO_DAMAGE_THRESHOLD&&
                   car.driverId()<0&&Math.abs(car.speed())<=.10&&
                   net.truckCargo.containsKey(car.id())&&
                   Math.hypot(px-car.x(),py-car.y())<=4.5){
                    net.unloadCargo(car.id());
                    toast("REQUESTING CAMP UNLOAD");return;
                }
        }else if(localHost!=null){
            if(localVehicles.carDrivenBy(1)>=0){toast("EXIT TRUCK TO UNLOAD");return;}
            for(Vehicles.State car:localVehicles.states())
                if(localHost.unloadTruck(1,car.id())){
                    toast("CARGO STAGED AT CAMP");return;
                }
        }
        toast("NEED LOADED TRUCK AND OWN WILDERNESS CAMP");
    }
    /** Ground cargo is reloaded onto an intact nearby parked truck, never hand-carried. */
    private void recoverNearestCargo(){
        if(net!=null){
            if(net.drivenCar()>=0){toast("EXIT TRUCK TO RECOVER");return;}
            for(NetLink.CargoDrop d:net.cargoDrops.values()){
                if(Math.hypot(px-d.x(),py-d.y())>2.5)continue;
                for(NetLink.Car car:net.cars.values()){
                    if(car.hp()<=80||car.driverId()>=0||Math.abs(car.speed())>.10||
                        net.truckCargo.containsKey(car.id()))continue;
                    if(Math.hypot(d.x()-car.x(),d.y()-car.y())<=4.2){
                        net.recoverCargo(car.id(),d.id());toast("REQUESTING CARGO RECOVERY");return;
                    }
                }
            }
        }else{
            if(localVehicles.carDrivenBy(1)>=0){toast("EXIT TRUCK TO RECOVER");return;}
            for(CargoHaulage.Drop d:localCargo.drops())if(Math.hypot(px-d.x(),py-d.y())<2.5)
                for(Vehicles.State car:localVehicles.states())
                    if(localCargo.recover(1,px,py,car.id(),d.id())){
                        toast("RECOVERED "+d.resource().replace('_',' '));return;
                    }
        }
        toast("NO DAMAGED CARGO / PARKED TRUCK IN REACH");
    }
    @Override public void keyReleased(KeyEvent e){held.remove(e.getKeyCode());}
    @Override public void keyTyped(KeyEvent e){}
    @Override public void mousePressed(MouseEvent e){
        requestFocusInWindow();
        if(e.getButton()==MouseEvent.BUTTON1)primaryHeld=true;
        if(e.getButton()==MouseEvent.BUTTON3)secondaryHeld=true;
        if(e.getButton()!=MouseEvent.BUTTON1&&e.getButton()!=MouseEvent.BUTTON3)return;
        if(dead){
            int top=Math.max(46,(getHeight()-282)/2);
            int left=(getWidth()-Math.min(590,getWidth()-30))/2;
            int right=getWidth()-left;
            for(int i=0;i<3;i++){
                int rowTop=top+79+i*52;
                if(e.getX()>=left+16&&e.getX()<right-16&&
                   e.getY()>=rowTop&&e.getY()<rowTop+46)spawn=i;
            }
            return;
        }
        shoot(e.getButton()==MouseEvent.BUTTON3);
    }
    @Override public void mouseReleased(MouseEvent e){
        if(e.getButton()==MouseEvent.BUTTON1)primaryHeld=false;
        if(e.getButton()==MouseEvent.BUTTON3)secondaryHeld=false;
        if(!primaryHeld&&!secondaryHeld){
            if(net!=null)net.arsenalRelease();
            else localHost.arsenal.release(1);
        }
    }
    @Override public void mouseClicked(MouseEvent e){}
    @Override public void mouseEntered(MouseEvent e){previousMouse=-1;previousMouseY=-1;}
    @Override public void mouseExited(MouseEvent e){previousMouse=-1;previousMouseY=-1;}
    @Override public void mouseDragged(MouseEvent e){mouseMoved(e);}
    @Override public void mouseMoved(MouseEvent e){
        if(previousMouse>=0&&previousMouseY>=0&&!dead){
            angle+=Math.max(-60,Math.min(60,e.getX()-previousMouse))*.004;
            cameraPitch=Math.max(-1.52,Math.min(1.52,
                cameraPitch-Math.max(-60,Math.min(60,e.getY()-previousMouseY))*.004));
        }
        previousMouse=e.getX();previousMouseY=e.getY();
    }
}
