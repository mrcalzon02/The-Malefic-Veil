package veil;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Host-authoritative LAN match protocol v2: visibility, hits, inventory, pickups,
 * healing, sanity, structures and NPCs are simulated by one ArenaSystems instance.
 */
public final class LanServer implements AutoCloseable {
    private final ServerSocket listener;
    private final ExecutorService workers=Executors.newCachedThreadPool();
    private final ScheduledExecutorService ticks=Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService saves=Executors.newSingleThreadScheduledExecutor();
    private final Semaphore connectionSlots=new Semaphore(32);
    private final HostModeration moderation;
    private final Path dataDirectory;
    private final long startup=System.currentTimeMillis();
    private final ConcurrentMap<Integer,Peer> connections=new ConcurrentHashMap<>();
    public final ArenaSystems match;
    public final ContinentSectors continentSectors;
    public final WorldHarvesting harvesting;
    public final FactionAiDirector ai;
    private final VeilWeather weather;
    private volatile boolean running=true;
    private int nextId;
    private static final class Peer {
        int id;
        Socket socket;
        PrintWriter out;
        boolean authenticated;
        int commands;
        long commandWindow=System.nanoTime();
        double lastResourceX=Double.NaN,lastResourceY=Double.NaN;
        synchronized void send(String message){out.println(message);}
    }
    public LanServer(int port,long seed)throws IOException {this(port,seed,null,false);}
    public LanServer(int port,long seed,Path saveDir)throws IOException {
        this(port,seed,saveDir,Boolean.getBoolean("veil.hardcore"));
    }
    /** Persisted world rule: hardmode cannot change across restarts without migration. */
    public LanServer(int port,long seed,Path saveDir,boolean hardcore)throws IOException {
        this(port,seed,saveDir,hardcore,HostAiSettings.fromProperties());
    }
    /** Explicit host AI settings, validated before binding the listening socket. */
    public LanServer(int port,long seed,Path saveDir,boolean hardcore,
                     HostAiSettings.Rules aiSettings)throws IOException {
        dataDirectory=saveDir;
        boolean active=HostRules.bind(saveDir,hardcore);
        moderation=saveDir==null?null:new HostModeration(saveDir);
        match=new ArenaSystems(seed,active);
        ContinentLayout continent=match.world.continent();
        continentSectors=new ContinentSectors(continent,new ContinentPopulation(continent));
        harvesting=new WorldHarvesting(match.world);
        // One atmospheric simulation per connected world, NOT per player or
        // separate city/wilderness scene. Dedicated hosts do not load GLBs/audio.
        weather=new VeilWeather(seed,Path.of("."),false);
        if(saveDir!=null){
            DurableRealm.load(match,saveDir);
            AccountDirectory.load(match,saveDir);
            harvesting.load(saveDir);
        }
        ai=new FactionAiDirector(match,harvesting,Objects.requireNonNull(aiSettings));
        if(saveDir!=null){ai.load(saveDir);HostAiSettings.save(saveDir,aiSettings);}
        listener=saveDir==null?new ServerSocket(port):
            new ServerSocket(port,32,InetAddress.getByName(System.getProperty("veil.bind","127.0.0.1")));
        if(saveDir!=null){
            saves.scheduleWithFixedDelay(()->{
                try{saveAll();}
                catch(Exception e){System.err.println("AUTOSAVE FAILED: "+e);audit("autosave_failure "+e.getClass().getSimpleName());}
            },60,60,TimeUnit.SECONDS);
        }
        ticks.scheduleAtFixedRate(()->{
            try {
                match.advance(100);
                harvesting.advance(100);
                ai.advance(100);
                synchronized(weather){
                    weather.tick(.1,match.world.spawns[0][0],
                        match.world.spawns[0][1],0,1);
                }
                if(match.time()%2000==0)broadcast(weatherPacket(),-1);
                for(Peer p:connections.values()){
                    ArenaSystems.Player self=match.player(p.id);
                    if(self!=null){
                        p.send(selfPacket(self));
                        if(!Double.isFinite(p.lastResourceX)||
                           Math.hypot(self.x-p.lastResourceX,self.y-p.lastResourceY)>12||
                           match.time()%5000==0)syncResources(p,self);
                        if(self.dead){
                            List<ArenaSystems.SpawnRisk> risks=match.spawnRisks(p.id);
                            p.send("SPAWNS|"+risks.get(0).score()+"|"+risks.get(1).score()+"|"+risks.get(2).score());
                            p.send("RESPAWN_TIMER|"+match.respawnRemainingMs(p.id));
                        }
                    }
                }
                for(ArenaSystems.Player p:match.roster())broadcast(peerPacket(p),p.id);
                if(match.time()%2000==0)
                    for(var bot:ai.views())broadcast(aiPacket(bot),-1);
                for(Vehicles.State car:match.vehicles.states())broadcast(carPacket(car),-1);
                if(match.time()%500==0){
                    for(Vehicles.State car:match.vehicles.states())
                        broadcast(cargoTruckPacket(car),-1);
                    for(CargoHaulage.Site yard:match.cargo.sites())
                        broadcast(cargoYardPacket(yard),-1);
                    broadcast(cargoDropIdsPacket(),-1);
                    for(CargoHaulage.Drop drop:match.cargo.drops())
                        broadcast(cargoDropPacket(drop),-1);
                }
                if(match.time()%500==0)for(int i=0;i<match.world.devices.size();i++){
                    World.Device d=match.world.devices.get(i);
                    broadcast("DEVICE|"+i+"|"+d.kind+"|"+d.x+"|"+d.y+"|"+d.hp,-1);
                }
                for(int i=0;i<match.world.npcs.size();i++){
                    World.Npc n=match.world.npcs.get(i);
                    broadcast("NPC|"+i+"|"+n.x+"|"+n.y+"|"+n.hp,-1);
                }
            }catch(RuntimeException e){System.err.println("Arena tick failed: "+e);}
        },100,100,TimeUnit.MILLISECONDS);
        workers.execute(()->{
            while(running)try{
                Socket s=listener.accept();
                if(!connectionSlots.tryAcquire()){s.close();continue;}
                workers.execute(()->{
                    try{serve(s);}finally{connectionSlots.release();}
                });
            }catch(IOException e){if(running)System.err.println("Accept failed: "+e);}
        });
    }
    private synchronized int id(){return ++nextId;}
    private void broadcast(String message,int except){
        for(Peer q:connections.values())if(q.id!=except)q.send(message);
    }
    /** Nearby harvestable geometry is ordinary shared-world entity interest. */
    private void syncResources(Peer peer,ArenaSystems.Player actor){
        peer.send("RESOURCE_RESET");
        for(var node:harvesting.nearby(actor.x,actor.y,55)){
            peer.send("RESOURCE_NODE|"+node.id()+"|"+harvesting.nodeX(node)+"|"+
                harvesting.nodeY(node)+"|"+node.type()+"|"+
                node.remaining()+"|"+node.capacity());
        }
        peer.lastResourceX=actor.x;peer.lastResourceY=actor.y;
    }
    private String weatherPacket(){
        synchronized(weather){
            return "WEATHER|"+weather.current()+"|"+weather.timeOfDay();
        }
    }
    /** Dedicated-host console authority; clients cannot forge weather packets. */
    public String weatherStatus(){
        synchronized(weather){
            return weather.current()+" at daylight="+String.format(Locale.ROOT,"%.4f",weather.timeOfDay());
        }
    }
    public String setWeatherPreset(String name){
        synchronized(weather){
            // Validate before mutating auto scheduling: unknown operator input
            // must not silently disable the active climate simulation.
            weather.setPreset(name,8);
            weather.autoWeather(false,240,.55,65);
        }
        broadcast(weatherPacket(),-1);
        audit("weather_preset "+name);
        return weatherStatus();
    }
    public String resumeAutoWeather(){
        synchronized(weather){weather.autoWeather(true,240,.55,65);}
        audit("weather_auto");
        return "Automatic weather enabled; "+weatherStatus();
    }
    private static String aiPacket(FactionAiDirector.View bot){
        return "AI_ACTOR|"+bot.id()+"|"+bot.faction()+"|"+bot.level()+
            "|"+bot.group()+"|"+bot.activity()+"|"+bot.buildings();
    }
    public String aiStatus(){
        StringJoiner j=new StringJoiner("; ",ai.settings().description()+" ", "");
        for(var bot:ai.views())j.add(bot.id()+":"+bot.faction().name().replace("THE_","")+
            ":"+bot.activity()+":lv"+bot.level()+":base"+bot.buildings());
        return j.toString();
    }
    private static String peerPacket(ArenaSystems.Player p){
        return "PEER|"+p.id+"|"+p.x+"|"+p.y+"|"+p.angle+"|"+p.hp+"|"+p.visibility+"|"+p.classId;
    }
    private static String carPacket(Vehicles.State car){
        return "CAR|"+car.id()+"|"+car.x()+"|"+car.y()+"|"+car.heading()+"|"+
            car.speed()+"|"+car.fuel()+"|"+car.hp()+"|"+car.driverId();
    }
    private String cargoTruckPacket(Vehicles.State car){
        Vehicles.Load load=match.vehicles.cargo(car.id());
        return "TRUCK_CARGO|"+car.id()+"|"+
            (load==null||load.resourceType()==null?"NONE":load.resourceType());
    }
    private static String cargoYardPacket(CargoHaulage.Site yard){
        return "CARGO_YARD|"+yard.id()+"|"+yard.x()+"|"+yard.y()+"|"+
            yard.resource()+"|"+yard.stock();
    }
    private String cargoDropIdsPacket(){
        List<CargoHaulage.Drop> drops=match.cargo.drops();
        if(drops.isEmpty())return "CARGO_DROP_IDS|-";
        StringJoiner names=new StringJoiner(",");
        for(CargoHaulage.Drop drop:drops)names.add(Integer.toString(drop.id()));
        return "CARGO_DROP_IDS|"+names;
    }
    private static String cargoDropPacket(CargoHaulage.Drop drop){
        return "CARGO_DROP|"+drop.id()+"|"+drop.x()+"|"+drop.y()+"|"+drop.resource();
    }
    private String progressionPacket(ArenaSystems.Player p){
        var v=p.progression.view();
        return "RPG|"+v.level()+"|"+v.experience()+"|"+v.nextLevelExperience()+
            "|"+v.aliveMs()+"|"+v.bonus()+
            "|"+v.skills().get(CharacterProgression.Skill.MARKSMANSHIP)+
            "|"+v.skills().get(CharacterProgression.Skill.ATHLETICS)+
            "|"+v.skills().get(CharacterProgression.Skill.ENGINEERING)+
            "|"+v.skills().get(CharacterProgression.Skill.SCAVENGING)+
            "|"+v.skills().get(CharacterProgression.Skill.SURVIVAL)+
            "|"+Math.max(0,p.protectedUntil-match.time())+
            "|"+p.survival.status().hunger()+"|"+p.survival.status().hydration();
    }
    private static String inventoryPacket(ArenaSystems.Player p){
        StringJoiner content=new StringJoiner(",");
        for(var item:p.survival.inventory().entrySet())
            content.add(item.getKey().name()+"="+item.getValue());
        return "INVENTORY|"+(content.length()==0?"-":content.toString());
    }
    private String factionPacket(String account){
        CharacterRoster.Faction faction=match.factions.faction(account);
        FactionGroups.Crew crew=match.factions.crewFor(account);
        FactionGroups.Role role=crew==null?null:crew.members().get(account);
        return "FACTION_STATE|"+(faction==null?"NONE":faction.name())+
            "|"+(crew==null?"NONE":crew.id())+"|"+(role==null?"NONE":role.name());
    }
    private void factionStateTo(ArenaSystems.Player actor,Peer peer){
        peer.send(factionPacket(actor.accountKey));
    }
    private static String selfPacket(ArenaSystems.Player p){
        return "SELF|"+p.hp+"|"+p.sanity+"|"+p.cash+"|"+p.scrap+"|"+p.medkits+
            "|"+p.pistolReserve+"|"+p.shellReserve+"|"+p.pistolClip+"|"+p.shellClip+
            "|"+(p.dead?1:0)+"|"+p.frags+"|"+p.x+"|"+p.y+"|"+p.angle+"|"+p.visibility;
    }
    private void sendWorld(Peer peer){
        for(ArenaSystems.Player other:match.roster())if(other.id!=peer.id)peer.send(peerPacket(other));
        for(var bot:ai.views())peer.send(aiPacket(bot));
        for(int i=0;i<match.world.pickups.size();i++)
            if(match.world.pickups.get(i).taken)peer.send("PICKUP|"+i);
        for(int i=0;i<match.world.npcs.size();i++){
            World.Npc n=match.world.npcs.get(i);
            peer.send("NPC_RANK|"+i+"|"+CharacterProgression.monsterLevel(
                match.world.seed,n)+"|"+n.kind);
        }
        for(Vehicles.State car:match.vehicles.states()){
            peer.send(carPacket(car));peer.send(cargoTruckPacket(car));
        }
        for(CargoHaulage.Site yard:match.cargo.sites())peer.send(cargoYardPacket(yard));
        peer.send(cargoDropIdsPacket());
        for(CargoHaulage.Drop drop:match.cargo.drops())peer.send(cargoDropPacket(drop));
        for(int i=0;i<match.world.devices.size();i++){
            World.Device d=match.world.devices.get(i);
            peer.send("DEVICE|"+i+"|"+d.kind+"|"+d.x+"|"+d.y+"|"+d.hp);
        }
    }
    private void serve(Socket socket){
        Peer peer=new Peer();peer.id=id();peer.socket=socket;
        try(socket;
            BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true)){
            socket.setSoTimeout(30000);
            peer.out=out;
            ArenaSystems.Player actor=match.join(peer.id);
            match.beginLoading(peer.id);
            peer.send("WELCOME|"+peer.id+"|"+match.world.seed+"|3");
            peer.send("SERVER_MODE|"+(match.hardcore?"HARDCORE":"NORMAL"));
            peer.send(weatherPacket());
            connections.put(peer.id,peer);
            sendWorld(peer);peer.send(selfPacket(actor));syncResources(peer,actor);
            peer.send(progressionPacket(actor));peer.send(inventoryPacket(actor));
            factionStateTo(actor,peer);
            broadcast(peerPacket(actor),peer.id);
            String line;
            while((line=in.readLine())!=null){
                if(line.length()>256)break;
                long nowNanos=System.nanoTime();
                if(nowNanos-peer.commandWindow>1_000_000_000L){
                    peer.commandWindow=nowNanos;peer.commands=0;
                }
                if(++peer.commands>120){audit("rate_limit id="+peer.id);break;}
                String[] s=line.split("\\|");
                try{
                    boolean changed=false;
                    if(moderation!=null&&!peer.authenticated&&!"AUTH".equals(s[0])){
                        peer.send("AUTH_DENIED|REQUIRED");return;
                    }
                    // A persistent player may not evade hard faction alliances by
                    // omitting selection and fighting as an "unaligned" account.
                    if(moderation!=null&&peer.authenticated&&
                       match.factions.faction(match.player(peer.id).accountKey)==null&&
                       Set.of("FIRE","ABILITY","PICKUP","MEDKIT","RELOAD","BUILD",
                           "RIDE","DRIVE","CARGO_LOAD","CARGO_RECOVER","CRAFT_HAND",
                           "HARVEST").contains(s[0])){
                        peer.send("FACTION_RESULT|REQUIRED");continue;
                    }
                    switch(s[0]){
                        case "AUTH" -> {
                            if(s.length!=2)continue;
                            String account;
                            try{account=PlayerIdentity.account(s[1]);}
                            catch(IllegalArgumentException invalid){peer.send("AUTH_DENIED|INVALID");break;}
                            if(moderation!=null&&!moderation.permitted(account)){
                                peer.send("AUTH_DENIED|ACCESS");audit("access_denied account="+account);
                                return;
                            }
                            if(match.assignAccount(peer.id,account)){
                                peer.authenticated=true;
                                peer.send("AUTH_OK|"+account);
                                factionStateTo(match.player(peer.id),peer);
                                audit("login id="+peer.id+" account="+account);
                            }else if(moderation==null){
                                // Local multi-client smoke tests can share a machine's stored key.
                                // The second session stays an unowned guest; never duplicate an account.
                                peer.send("AUTH_GUEST|DUPLICATE");
                            }else{peer.send("AUTH_DENIED|DUPLICATE");return;}
                        }
                        case "FACTION_JOIN" -> {
                            if(s.length!=2||!peer.authenticated)continue;
                            CharacterRoster.Faction selected;
                            try{selected=CharacterRoster.Faction.valueOf(s[1]);}
                            catch(IllegalArgumentException invalid){
                                peer.send("FACTION_RESULT|INVALID");continue;
                            }
                            boolean ok=match.enrollFaction(peer.id,selected);
                            peer.send("FACTION_RESULT|"+(ok?"OK":"LOCKED"));
                            factionStateTo(match.player(peer.id),peer);
                            if(ok&&dataDirectory!=null)saveAll();
                        }
                        case "CREW_CREATE" -> {
                            if(s.length!=2||!peer.authenticated)continue;
                            ArenaSystems.Player requester=match.player(peer.id);
                            FactionGroups.Crew made=match.factions.create(requester.accountKey,s[1]);
                            peer.send("CREW_RESULT|CREATE|"+(made==null?"REJECTED":made.id()));
                            factionStateTo(requester,peer);
                            if(made!=null&&dataDirectory!=null)saveAll();
                        }
                        case "CREW_INVITE" -> {
                            if(s.length!=2||!peer.authenticated)continue;
                            int recipientId=Integer.parseInt(s[1]);
                            ArenaSystems.Player issuer=match.player(peer.id);
                            ArenaSystems.Player recipient=match.player(recipientId);
                            boolean ok=recipient!=null&&match.factions.invite(
                                issuer.accountKey,recipient.accountKey,System.currentTimeMillis());
                            peer.send("CREW_RESULT|INVITE|"+(ok?"OK":"REJECTED"));
                            Peer invited=connections.get(recipientId);
                            if(ok&&invited!=null)
                                invited.send("CREW_INVITE|"+match.factions.crewId(issuer.accountKey));
                        }
                        case "CREW_ACCEPT" -> {
                            if(s.length!=1||!peer.authenticated)continue;
                            ArenaSystems.Player requester=match.player(peer.id);
                            boolean ok=match.factions.accept(requester.accountKey,System.currentTimeMillis());
                            peer.send("CREW_RESULT|ACCEPT|"+(ok?"OK":"REJECTED"));
                            factionStateTo(requester,peer);
                            if(ok&&dataDirectory!=null)saveAll();
                        }
                        case "CREW_LEAVE" -> {
                            if(s.length!=1||!peer.authenticated)continue;
                            ArenaSystems.Player requester=match.player(peer.id);
                            boolean ok=match.factions.remove(requester.accountKey,requester.accountKey);
                            peer.send("CREW_RESULT|LEAVE|"+(ok?"OK":"REJECTED"));
                            factionStateTo(requester,peer);
                            if(ok&&dataDirectory!=null)saveAll();
                        }
                        case "CREW_PROMOTE" -> {
                            if(s.length!=2||!peer.authenticated)continue;
                            ArenaSystems.Player requester=match.player(peer.id);
                            ArenaSystems.Player recipient=match.player(Integer.parseInt(s[1]));
                            boolean ok=recipient!=null&&match.factions.promote(
                                requester.accountKey,recipient.accountKey);
                            peer.send("CREW_RESULT|PROMOTE|"+(ok?"OK":"REJECTED"));
                            factionStateTo(requester,peer);
                            Peer promoted=recipient==null?null:connections.get(recipient.id);
                            if(ok&&promoted!=null)factionStateTo(recipient,promoted);
                            if(ok&&dataDirectory!=null)saveAll();
                        }
                        case "CREW_KICK" -> {
                            if(s.length!=2||!peer.authenticated)continue;
                            ArenaSystems.Player requester=match.player(peer.id);
                            ArenaSystems.Player recipient=match.player(Integer.parseInt(s[1]));
                            boolean ok=recipient!=null&&match.factions.remove(
                                requester.accountKey,recipient.accountKey);
                            peer.send("CREW_RESULT|KICK|"+(ok?"OK":"REJECTED"));
                            Peer removed=recipient==null?null:connections.get(recipient.id);
                            if(ok&&removed!=null)factionStateTo(recipient,removed);
                            if(ok&&dataDirectory!=null)saveAll();
                        }
                        case "CREW_TRANSFER" -> {
                            if(s.length!=2||!peer.authenticated)continue;
                            ArenaSystems.Player requester=match.player(peer.id);
                            ArenaSystems.Player recipient=match.player(Integer.parseInt(s[1]));
                            boolean ok=recipient!=null&&match.factions.transfer(
                                requester.accountKey,recipient.accountKey);
                            peer.send("CREW_RESULT|TRANSFER|"+(ok?"OK":"REJECTED"));
                            factionStateTo(requester,peer);
                            if(ok&&dataDirectory!=null)saveAll();
                        }
                        case "CREW_DISBAND" -> {
                            if(s.length!=1||!peer.authenticated)continue;
                            ArenaSystems.Player requester=match.player(peer.id);
                            boolean ok=match.factions.disband(requester.accountKey);
                            peer.send("CREW_RESULT|DISBAND|"+(ok?"OK":"REJECTED"));
                            factionStateTo(requester,peer);
                            if(ok&&dataDirectory!=null)saveAll();
                        }
                        case "SCENE_READY" -> {
                            if(s.length!=1)continue;
                            boolean admitted=match.sceneReady(peer.id);
                            peer.send("SCENE_READY_RESULT|"+(admitted?"OK":"IGNORED"));
                        }
                        case "HARVEST" -> {
                            if(s.length!=2)continue;
                            ArenaSystems.Player p=match.player(peer.id);
                            int nodeId=Integer.parseInt(s[1]);
                            var outcome=harvesting.harvest(p,nodeId);
                            peer.send("HARVEST_RESULT|"+nodeId+"|"+outcome.reason());
                            if(outcome.accepted()){
                                peer.send(inventoryPacket(p));
                                p.progression.reward(CharacterProgression.Skill.SCAVENGING,
                                    8,match.time(),3000);
                                peer.send(progressionPacket(p));
                                var node=outcome.node();
                                for(Peer observer:connections.values()){
                                    ArenaSystems.Player other=match.player(observer.id);
                                    if(other!=null&&Math.hypot(other.x-harvesting.nodeX(node),
                                        other.y-harvesting.nodeY(node))<65)
                                        observer.send("RESOURCE_NODE|"+node.id()+"|"+
                                            harvesting.nodeX(node)+"|"+harvesting.nodeY(node)+"|"+
                                            node.type()+"|"+node.remaining()+"|"+node.capacity());
                                }
                            }
                        }
                        case "STATE" -> {
                            if(s.length!=5)continue;
                            changed=match.move(peer.id,Double.parseDouble(s[1]),
                                Double.parseDouble(s[2]),Double.parseDouble(s[3]),Integer.parseInt(s[4]));
                        }
                        case "CHOOSE" -> {
                            if(s.length!=2)continue;
                            match.choose(peer.id,Integer.parseInt(s[1]));changed=true;
                        }
                        case "FIRE" -> {
                            if(s.length!=3)continue;
                            long sequence=Long.parseLong(s[1]);
                            ArenaSystems.ShotReport shot=match.fireReport(peer.id,Integer.parseInt(s[2]),sequence);
                            peer.send("SHOT|"+shot.sequence()+"|"+shot.fired()+"|"+shot.status()+"|"+
                                shot.targetId()+"|"+shot.damage()+"|"+shot.ammo());
                            changed=shot.fired();
                        }
                        case "PICKUP" -> {
                            if(s.length!=2)continue;
                            int index=Integer.parseInt(s[1]);
                            changed=match.interactPickup(peer.id,index);
                            if(changed)broadcast("PICKUP|"+index,-1);
                        }
                        case "MEDKIT" -> changed=match.useMedkit(peer.id);
                        case "RELOAD" -> {
                            if(s.length!=2)continue;
                            changed=match.reload(peer.id,Integer.parseInt(s[1]));
                        }
                        case "ABILITY" -> changed=match.ability(peer.id);
                        case "CRAFT_HAND" -> {
                            if(s.length!=2||s[1].length()>48)continue;
                            boolean crafted=match.craftHand(peer.id,s[1]);
                            peer.send("CRAFT_RESULT|"+s[1]+"|"+(crafted?"OK":"DENIED"));
                            changed=crafted;
                            if(crafted){
                                ArenaSystems.Player maker=match.player(peer.id);
                                peer.send(inventoryPacket(maker));
                                peer.send(progressionPacket(maker));
                            }
                        }
                        case "BUILD" -> {
                            if(s.length!=2)continue;
                            int kind=Integer.parseInt(s[1]);
                            ArenaSystems.BuildOutcome outcome=match.buildOutcome(peer.id,kind);
                            ArenaSystems.BuildReview review=outcome.review();
                            changed=outcome.built();
                            peer.send("BUILD_RESULT|"+kind+"|"+(changed?"OK":review.reason())+"|"+
                                review.cashCost()+"|"+review.suppliesCost());
                            if(changed){
                                int idx=outcome.deviceIndex();
                                World.Device d=match.world.devices.get(idx);
                                broadcast("DEVICE|"+idx+"|"+d.kind+"|"+d.x+"|"+d.y+"|"+d.hp,-1);
                            }
                        }
                        case "CARGO_LOAD" -> {
                            if(s.length!=3)continue;
                            int car=Integer.parseInt(s[1]),yard=Integer.parseInt(s[2]);
                            changed=match.loadTruck(peer.id,car,yard);
                            peer.send("CARGO_RESULT|LOAD|"+(changed?"OK":"REJECTED"));
                            if(changed){
                                broadcast(cargoTruckPacket(match.vehicles.state(car)),-1);
                                broadcast(cargoYardPacket(match.cargo.sites().get(yard)),-1);
                            }
                        }
                        case "CARGO_RECOVER" -> {
                            if(s.length!=3)continue;
                            int car=Integer.parseInt(s[1]),drop=Integer.parseInt(s[2]);
                            changed=match.recoverTruckLoad(peer.id,car,drop);
                            peer.send("CARGO_RESULT|RECOVER|"+(changed?"OK":"REJECTED"));
                            if(changed){
                                broadcast(cargoTruckPacket(match.vehicles.state(car)),-1);
                                broadcast("CARGO_DROP_TAKEN|"+drop,-1);
                            }
                        }
                        case "RIDE" -> {
                            if(s.length!=2)continue;
                            int index=Integer.parseInt(s[1]);
                            changed=match.enterVehicle(peer.id,index);
                            peer.send("RIDE_RESULT|"+(changed?"OK":"REJECTED"));
                        }
                        case "EXIT" -> {
                            if(s.length!=1)continue;
                            changed=match.exitVehicle(peer.id);
                            peer.send("RIDE_RESULT|"+(changed?"EXITED":"REJECTED"));
                        }
                        case "DRIVE" -> {
                            if(s.length!=3)continue;
                            changed=match.driveVehicle(peer.id,
                                Double.parseDouble(s[1]),Double.parseDouble(s[2]));
                        }
                        case "SECTOR" -> {
                            // Explicitly a macro-blueprint query: no player teleport or game mutation.
                            if(s.length!=3)continue;
                            int sx=Integer.parseInt(s[1]),sy=Integer.parseInt(s[2]);
                            if(sx<0||sy<0||sx>=ContinentSectors.COUNT||sy>=ContinentSectors.COUNT){
                                peer.send("SECTOR_DENIED|BOUNDS");continue;
                            }
                            ContinentSectors.Sector sector=continentSectors.sector(new ContinentSectors.Key(sx,sy));
                            peer.send("SECTOR_BEGIN|"+sx+"|"+sy+"|"+sector.parcels().size()+"|"+
                                sector.playerSpawns().size()+"|"+sector.npcSpawns().size());
                            for(ContinentSectors.Parcel cell:sector.parcels())
                                peer.send("PARCEL|"+cell.x()+"|"+cell.y()+"|"+cell.biome()+"|"+
                                    cell.elevation()+"|"+(int)cell.city()+"|"+(cell.road()?1:0)+"|"+
                                    (cell.worldOwned()?1:0)+"|"+(cell.buildable()?1:0));
                            for(var spawn:sector.playerSpawns())
                                peer.send("CONTINENT_SPAWN|"+spawn.id()+"|"+spawn.x()+"|"+spawn.y()+"|"+
                                    spawn.kind()+"|"+(int)spawn.city());
                            for(var npc:sector.npcSpawns())
                                peer.send("CONTINENT_ENCOUNTER|"+npc.id()+"|"+npc.x()+"|"+npc.y()+"|"+
                                    npc.kind()+"|"+npc.groupSize());
                            peer.send("SECTOR_END|"+sx+"|"+sy);
                        }
                        case "PING" -> {
                            if(s.length!=2)continue;
                            long nonce=Long.parseLong(s[1]);
                            if(nonce>0)peer.send("PONG|"+nonce);
                        }
                        case "RESPAWN" -> {
                            if(s.length!=2)continue;
                            int index=Integer.parseInt(s[1]);
                            changed=match.respawn(peer.id,index);
                            peer.send("RESPAWN_RESULT|"+index+"|"+(changed?"OK":
                                match.respawnRemainingMs(peer.id)>0?"WAIT":"REJECTED"));
                        }
                        default -> { }
                    }
                    if(changed){
                        ArenaSystems.Player updated=match.player(peer.id);
                        if(updated!=null){
                            peer.send(selfPacket(updated));
                            broadcast(peerPacket(updated),peer.id);
                        }
                    }
                }catch(NumberFormatException ignored){/* malformed client command rejected */}
            }
        }catch(IOException ignored){
        }finally{
            if(peer.authenticated) audit("logout id="+peer.id);
            connections.remove(peer.id);
            match.leave(peer.id);
            broadcast("LEFT|"+peer.id,-1);
        }
    }
    private void audit(String event){
        if(moderation!=null)try{moderation.audit(event);}
        catch(IOException ex){System.err.println("Audit write failed: "+ex);}
    }
    public int port(){return listener.getLocalPort();}
    public String status(){
        return "mode="+(match.hardcore?"HARDCORE":"NORMAL")+
            " uptime="+((System.currentTimeMillis()-startup)/1000)+"s"+
            " peers="+connections.size()+" structures="+match.world.devices.size()+
            " cars="+match.vehicles.states().size()+" npcs="+match.world.npcs.size()+
            " persistent="+(dataDirectory!=null);
    }
    public List<String> connectedPlayers(){
        List<String> out=new ArrayList<>();
        for(Peer p:connections.values()){
            ArenaSystems.Player actor=match.player(p.id);
            if(actor!=null)out.add("id="+p.id+" account="+actor.accountKey+" hp="+actor.hp);
        }
        Collections.sort(out);return out;
    }
    public boolean kick(int id){
        Peer p=connections.get(id);
        if(p==null)return false;
        audit("kick id="+id);
        try{p.socket.close();}catch(IOException ignored){}
        return true;
    }
    public boolean ban(String account)throws IOException{
        if(moderation==null)return false;
        boolean result=moderation.ban(account);
        if(result){
            audit("ban account="+account);
            for(Peer p:connections.values()){
                ArenaSystems.Player player=match.player(p.id);
                if(player!=null&&account.equals(player.accountKey))kick(p.id);
            }
        }
        return result;
    }
    public boolean unban(String account)throws IOException{
        if(moderation==null)return false;
        boolean result=moderation.unban(account);
        if(result)audit("unban account="+account);
        return result;
    }
    private synchronized void saveAll()throws IOException{
        if(dataDirectory==null)throw new IOException("In-memory LAN match has no save directory");
        DurableRealm.save(match,dataDirectory);
        AccountDirectory.save(match,dataDirectory);
        harvesting.save(dataDirectory);
        ai.save(dataDirectory);
    }
    public void saveNow()throws IOException{
        saveAll();audit("manual_save");
    }
    @Override public synchronized void close()throws IOException{
        if(!running)return;
        running=false;
        listener.close();
        for(Peer p:connections.values())try{p.socket.close();}catch(IOException ignored){}
        ticks.shutdownNow();saves.shutdownNow();workers.shutdownNow();
        synchronized(weather){weather.close();}
        if(dataDirectory!=null)saveNow();
    }
    public static void main(String[] args)throws Exception{
        boolean hardcore=Boolean.getBoolean("veil.hardcore");
        List<String> positional=new ArrayList<>(),aiOptions=new ArrayList<>();
        for(String arg:args){
            if("--hardcore".equals(arg))hardcore=true;
            else if(arg.startsWith("--ai-"))aiOptions.add(arg);
            else if(arg.startsWith("--"))throw new IllegalArgumentException("Unknown host option "+arg);
            else positional.add(arg);
        }
        int port=positional.size()>0?Integer.parseInt(positional.get(0)):7777;
        long seed=positional.size()>1?Long.parseLong(positional.get(1)):20261020L;
        Path root=positional.size()>2?Path.of(positional.get(2)):Path.of("server-data");
        if(positional.size()>3)throw new IllegalArgumentException("Expected port seed save-path [--hardcore]");
        HostAiSettings.Rules hostAI=aiOptions.isEmpty()?
            HostAiSettings.fromProperties():HostAiSettings.fromOptions(aiOptions);
        LanServer host=new LanServer(port,seed,root,hardcore,hostAI);
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            try{host.close();}catch(IOException e){System.err.println("Shutdown save failed: "+e);}
        },"Veil-server-shutdown"));
        System.out.println("Malefic Veil dedicated host "+host.port()+" seed "+seed+
            " mode="+(hardcore?"HARDCORE":"NORMAL")+
            " bind="+host.listener.getInetAddress().getHostAddress()+" storage="+root+
            " "+host.ai.settings().description());
        System.out.println("Host AI: --ai-count=6 --ai-mode=SINGLE_FACTION --ai-team=3 --ai-level=5");
        System.out.println("Or --ai-count=5 --ai-mode=BALANCED; group size is always 3.");
        System.out.println("Commands: status, players, ai, weather [PRESET|auto], save, kick ID,"+
            " ban ACCOUNT, unban ACCOUNT, allow ACCOUNT, deny ACCOUNT, allowlist on/off, shutdown");
        BufferedReader console=new BufferedReader(new InputStreamReader(System.in));
        String line;
        while((line=console.readLine())!=null){
            try{
                String[] input=line.trim().split("\\s+");
                if(input.length==0)continue;
                String result=switch(input[0].toLowerCase(Locale.ROOT)){
                    case "status" -> host.status();
                    case "players" -> String.join("\\n",host.connectedPlayers());
                    case "ai" -> host.aiStatus();
                    case "weather" -> {
                        if(input.length==1)yield host.weatherStatus();
                        if(input.length!=2)yield "Use weather [CLEAR|RAIN|SNOW|...|auto]";
                        if(input[1].equalsIgnoreCase("auto"))yield host.resumeAutoWeather();
                        yield host.setWeatherPreset(input[1].toUpperCase(Locale.ROOT));
                    }
                    case "save" -> {host.saveNow();yield "Saved and verified";}
                    case "kick" -> input.length==2&&host.kick(Integer.parseInt(input[1]))?"Kicked":"Unknown player";
                    case "ban" -> input.length==2&&host.ban(input[1])?"Banned":"Invalid/duplicate account";
                    case "unban" -> input.length==2&&host.unban(input[1])?"Unbanned":"Unknown account";
                    case "allow" -> input.length==2&&host.moderation.allow(input[1])?"Allowed":"Invalid/duplicate account";
                    case "deny" -> input.length==2&&host.moderation.deny(input[1])?"Removed":"Unknown account";
                    case "allowlist" -> {
                        if(input.length==2&&(input[1].equals("on")||input[1].equals("off"))){
                            host.moderation.setAllowlist(input[1].equals("on"));
                        }
                        yield "Allowlist "+(host.moderation.allowlistEnabled()?"ON":"OFF");
                    }
                    case "shutdown" -> {host.close();yield "Stopped"; }
                    default -> "Unknown command";
                };
                System.out.println(result);
                if(!host.running)break;
            }catch(Exception e){System.err.println("Admin command rejected: "+e.getMessage());}
        }
        if(host.running)new CountDownLatch(1).await(); // no console under systemd; shutdown hook owns save
    }
}
