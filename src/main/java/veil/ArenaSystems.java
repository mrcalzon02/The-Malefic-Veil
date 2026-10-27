package veil;

import java.util.*;

/**
 * Host-owned arena state. All mutations are serialized by synchronized methods.
 * Millisecond game clock is explicitly advanced by host tick, so tests are deterministic.
 */
public final class ArenaSystems {
    public final World world;
    public final Vehicles vehicles;
    public final CargoHaulage cargo;
    public final ArsenalSystems arsenal;
    public final ThrownDeployables thrown;
    public final boolean hardcore;
    public final FactionGroups factions=new FactionGroups();
    public final RaidProtection raidProtection=new RaidProtection(Long.getLong("veil.offlineGraceMinutes",30L));
    private final Map<Integer,Player> players=new LinkedHashMap<>();
    private final Map<String,Profile> accountProfiles=new LinkedHashMap<>();
    private final Map<String,CharacterProgression.Snapshot> accountSkills=new LinkedHashMap<>();
    private final Map<String,SurvivalCrafting.Status> accountSurvival=new LinkedHashMap<>();
    private final java.security.SecureRandom respawnRandom=new java.security.SecureRandom();
    /** Persistent host-owned account snapshot, excluding transient socket/vehicle ownership. */
    public record Profile(int hp,int sanity,int cash,int scrap,int medkits,int pistolReserve,
        int shellReserve,int pistolClip,int shellClip,int classId,int frags,boolean dead,
        double x,double y,double angle){}
    private static Profile savePlayer(Player p){
        return new Profile(p.hp,p.sanity,p.cash,p.scrap,p.medkits,p.pistolReserve,
            p.shellReserve,p.pistolClip,p.shellClip,p.classId,p.frags,p.dead,p.x,p.y,p.angle);
    }
    public static boolean validProfile(Profile p){
        return p!=null&&p.hp>=0&&p.hp<=100&&p.sanity>=0&&p.sanity<=100&&
            p.cash>=0&&p.cash<=1_000_000_000&&p.scrap>=0&&p.scrap<=1_000_000_000&&
            p.medkits>=0&&p.medkits<=100_000&&p.pistolReserve>=0&&p.pistolReserve<=1_000_000&&
            p.shellReserve>=0&&p.shellReserve<=1_000_000&&p.pistolClip>=0&&p.pistolClip<=12&&
            p.shellClip>=0&&p.shellClip<=6&&p.classId>=0&&p.classId<4&&
            p.frags>=0&&p.frags<=1_000_000_000&&Double.isFinite(p.x)&&
            Double.isFinite(p.y)&&Double.isFinite(p.angle);
    }
    private void applyProfile(Player p,Profile profile){
        if(!validProfile(profile))throw new IllegalStateException("Invalid profile");
        p.hp=profile.hp();p.sanity=profile.sanity();p.cash=profile.cash();
        p.scrap=profile.scrap();p.medkits=profile.medkits();
        p.pistolReserve=profile.pistolReserve();p.shellReserve=profile.shellReserve();
        p.pistolClip=profile.pistolClip();p.shellClip=profile.shellClip();
        p.classId=profile.classId();p.frags=profile.frags();
        p.dead=profile.dead();p.respawnAt=clock-1200;
        if(free(profile.x(),profile.y())){
            p.x=profile.x();p.y=profile.y();p.angle=profile.angle();
        }else{
            p.x=world.spawns[0][0];p.y=world.spawns[0][1];
        }
        p.moveAt=clock;refreshVisibility(p);
    }
    /** Bound, validated physical inventory and bodily needs for authenticated
     * accounts. Transient guests are never serialized into persistent slots. */
    public synchronized Map<String,SurvivalCrafting.Status> survivalSnapshot(){
        Map<String,SurvivalCrafting.Status> out=new TreeMap<>(accountSurvival);
        for(Player p:players.values())if(p.accountKey.startsWith("acct_"))
            out.put(p.accountKey,p.survival.status());
        return Collections.unmodifiableMap(out);
    }
    public synchronized void restoreSurvival(Map<String,SurvivalCrafting.Status> data){
        if(!players.isEmpty())throw new IllegalStateException("Live survival restore forbidden");
        Map<String,SurvivalCrafting.Status> checked=new TreeMap<>();
        for(var entry:data.entrySet()){
            if(!entry.getKey().matches("acct_[a-f0-9]{64}"))
                throw new IllegalArgumentException("Invalid survival account owner");
            SurvivalCrafting validation=new SurvivalCrafting();
            validation.restoreStatus(entry.getValue());
            checked.put(entry.getKey(),validation.status());
        }
        accountSurvival.clear();accountSurvival.putAll(checked);
    }
    public synchronized Map<String,CharacterProgression.Snapshot> skillsSnapshot(){
        Map<String,CharacterProgression.Snapshot> out=new TreeMap<>(accountSkills);
        for(Player p:players.values())if(p.accountKey.startsWith("acct_"))
            out.put(p.accountKey,p.progression.snapshot());
        return Collections.unmodifiableMap(out);
    }
    public synchronized void restoreSkills(Map<String,CharacterProgression.Snapshot> data){
        if(!players.isEmpty())throw new IllegalStateException("Live skill restore forbidden");
        Map<String,CharacterProgression.Snapshot> checked=new TreeMap<>();
        for(var entry:data.entrySet()){
            if(!entry.getKey().matches("acct_[a-f0-9]{64}"))
                throw new IllegalArgumentException("Invalid account skill owner");
            CharacterProgression validation=new CharacterProgression();
            validation.restore(entry.getValue());
            checked.put(entry.getKey(),validation.snapshot());
        }
        accountSkills.clear();accountSkills.putAll(checked);
    }
    public synchronized Map<String,Profile> profilesSnapshot(){
        Map<String,Profile> result=new TreeMap<>(accountProfiles);
        for(Player p:players.values())if(p.accountKey.startsWith("acct_"))
            result.put(p.accountKey,savePlayer(p));
        return Collections.unmodifiableMap(result);
    }
    /** Called only during startup before sockets are accepted. */
    public synchronized void restoreProfiles(Map<String,Profile> persisted){
        if(!players.isEmpty())throw new IllegalStateException("Cannot overwrite live accounts");
        Map<String,Profile> validated=new LinkedHashMap<>();
        for(var entry:persisted.entrySet()){
            if(!entry.getKey().matches("acct_[a-f0-9]{64}")||!validProfile(entry.getValue()))
                throw new IllegalArgumentException("Corrupt account profile");
            validated.put(entry.getKey(),entry.getValue());
        }
        accountProfiles.clear();accountProfiles.putAll(validated);
    }

    private long clock;
    private final Map<Integer,Long> lastShotSequence=new HashMap<>();
    private final Map<Integer,Integer> shieldedShot=new HashMap<>();
    public static final int TURRET=0, HEALTH_STATION=1, REVIVE_STATION=2, BARRICADE=3, CAMP_STAKE=4, FIELD_FIRE=5, FIELD_BENCH=6, STORAGE_CRATE=7,
        UNRAVELED_SCRAP_OUTPOST=8,PENITENT_VOTIVE_ALTAR=9,BEGOTTEN_BROOD_NEST=10,
        CLAIM_HEART=11,HAND_WORKSHOP=12,DRY_STORE=13,COOKING_HEARTH=14,
        RAIN_CATCHER=15,CARGO_LAYDOWN=16,FORAGER_GARDEN=17,
        COMPOST_YARD=18,HERB_PLOT=19,CHARCOAL_KILN=20,
        STONE_FURNACE=21,FORGE=22,FREIGHT_BAY=23,MATERIALS_RECYCLER=24,
        UNRAVELED_MACHINIST_BAY=25,PENITENT_RELIQUARY=26,
        BEGOTTEN_SPORE_BASIN=27,
        STEAM_BOILER=28, MECHANICAL_SHAFT=29, METAL_PRESS=30,
        UNRAVELED_PRESSURE_SHOP=31, PENITENT_CANTOR_DAIS=32,
        BEGOTTEN_CHITIN_VAT=33,
        GENERATOR=34,ELECTRIC_BUS=35,PRECISION_LATHE=36,WATER_WORKS=37,
        UNRAVELED_SENSOR_LAB=38,PENITENT_CHOIR_RELAY=39,BEGOTTEN_MEMBRANE_LAB=40,
        CONVEYOR_LINE=41,AUTOMATIC_SORTER=42,INDUSTRIAL_WORKSHOP=43,
        ASSEMBLY_LINE=44,COMMAND_HALL=45,RESEARCH_ARCHIVE=46,
        SIGNAL_RELAY=47,SECURITY_GRID=48,FUEL_REFINERY=49,
        TERROR_COLLECTOR=50,NUTRIENT_DIGESTOR=51,
        UNRAVELED_DEFENSE_WORKS=52,PENITENT_WARD_SANCTUM=53,
        BEGOTTEN_ORGAN_LATTICE=54;
    public static final class Player {
        public final int id;
        public final CharacterProgression progression=new CharacterProgression();
        public final SurvivalCrafting survival=new SurvivalCrafting();
        public boolean ready;
        public long joinedAt,loadingDeadline,reloadAt=-100000,craftReadyAt;
        public double distanceTowardAthletics;
        public double x,y,angle,visibility;
        public int hp=100,sanity=100,cash=150,scrap=4,medkits=2;
        public int pistolReserve=48,shellReserve=18,pistolClip=12,shellClip=6;
        public int classId,frags,flags;
        public String accountKey;
        public long lastHurt=-100000,shotAt=-100000,shadeUntil,abilityAt=-100000,respawnAt=-100000,moveAt;
        public boolean dead;
        public long protectedUntil,lastSpawnAt=-100000;
        public int lastSpawnIndex=-1;
        public final int[] earlySpawnDeaths=new int[3];
        public double spawnX,spawnY;
        Player(int id,double x,double y,long now){
            this.id=id;this.x=x;this.y=y;moveAt=now;
            accountKey="guest_"+id;
            joinedAt=now;
            loadingDeadline=now; // protection is armed by the actual network handshake
        }
    }
    public ArenaSystems(long seed){this(seed,false);}
    public ArenaSystems(long seed,boolean hardcore){
        this.hardcore=hardcore;
        world=new World(seed);vehicles=new Vehicles(world);
        cargo=new CargoHaulage(world,vehicles);
        arsenal=new ArsenalSystems(this);
        thrown=new ThrownDeployables(this);
    }
    public synchronized Player join(int id){
        if(players.containsKey(id))throw new IllegalArgumentException("duplicate peer");
        Player p=new Player(id,world.spawns[0][0],world.spawns[0][1],clock);
        players.put(id,p);
        arsenal.join(id);
        // Disposable guest peers have no durable property claim; only authenticated accounts do.
        refreshVisibility(p);return p;
    }
    public synchronized void leave(int id){
        arsenal.leave(id);
        vehicles.disconnect(id);
        Player departing=players.remove(id);
        if(departing!=null){
            if(departing.accountKey.startsWith("acct_"))
                accountProfiles.put(departing.accountKey,savePlayer(departing));
            if(departing.accountKey.startsWith("acct_"))
                accountSkills.put(departing.accountKey,departing.progression.snapshot());
            if(departing.accountKey.startsWith("acct_"))
                accountSurvival.put(departing.accountKey,departing.survival.status());
            raidProtection.logout(departing.accountKey,System.currentTimeMillis());
        }
        lastShotSequence.remove(id);
    }
    /** Only the host may assign a verified session identity, never raw client user IDs. */
    public synchronized boolean assignAccount(int id,String key){
        Player p=players.get(id);
        if(p==null||key==null||!key.matches("acct_[a-f0-9]{64}"))return false;
        for(Player other:players.values())
            if(other.id!=id&&other.accountKey.equals(key))return false;
        if(p.accountKey.equals(key))return true;
        String prior=p.accountKey;
        raidProtection.logout(prior,System.currentTimeMillis());
        p.accountKey=key;
        arsenal.bind(id,key);
        raidProtection.login(key,System.currentTimeMillis());
        for(World.Device d:world.devices)if(d.ownerId==id&&prior.equals(d.ownerKey))
            d.ownerKey=key;
        Profile saved=accountProfiles.get(key);
        if(saved!=null)applyProfile(p,saved);
        CharacterProgression.Snapshot skills=accountSkills.get(key);
        if(skills!=null)p.progression.restore(skills);
        SurvivalCrafting.Status supplies=accountSurvival.get(key);
        if(supplies!=null)p.survival.restoreStatus(supplies);
        return true;
    }
    /** Network server calls this when accepting a real, authenticated loading session. */
    public synchronized boolean beginLoading(int id){
        Player p=players.get(id);
        if(p==null||p.dead)return false;
        p.ready=false;p.joinedAt=clock;
        p.loadingDeadline=clock+15_000;
        p.protectedUntil=clock+10_000;
        return true;
    }
    /** One-shot readiness proof after scene/model loading; cannot be spammed. */
    public synchronized boolean sceneReady(int id){
        Player p=player(id);
        if(p==null||p.dead||p.ready||clock>=p.loadingDeadline)return false;
        p.ready=true;
        p.protectedUntil=Math.min(p.loadingDeadline,clock+6000);
        return true;
    }
    public synchronized CharacterProgression.View progression(int id){
        Player p=player(id);
        return p==null?null:p.progression.view();
    }
    /** Explicit once-only faction enrollment, separate from private crew membership. */
    public synchronized boolean enrollFaction(int id,CharacterRoster.Faction faction){
        Player player=players.get(id);
        if(player==null||player.dead||!player.accountKey.startsWith("acct_"))return false;
        return factions.enroll(player.accountKey,faction);
    }
    public synchronized boolean allied(int firstId,int secondId){
        Player a=players.get(firstId),b=players.get(secondId);
        return a!=null&&b!=null&&factions.allied(a.accountKey,b.accountKey);
    }
    private void breakGrace(Player p){p.protectedUntil=0;}
    public synchronized Collection<Player> roster(){return List.copyOf(players.values());}
    public synchronized Player player(int id){return players.get(id);}
    public synchronized long time(){return clock;}
    public synchronized void choose(int id,int classId){
        Player p=player(id);
        if(p==null||classId<0||classId>=4)return;
        CharacterRoster.Faction affiliation=factions.faction(p.accountKey);
        if(affiliation!=null&&affiliation!=(classId==3?
            CharacterRoster.Faction.THE_PENITENT:CharacterRoster.Faction.THE_UNRAVELED))return;
        p.classId=classId;
    }
    public static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
    public static double opacity(double visibility){return clamp(visibility,0,1);}
    public static double transparencyPercent(double visibility){return (1-opacity(visibility))*100.0;}
    private boolean free(double x,double y){
        if(!world.canWalk(x,y)||thrown.occupied(x,y))return false;
        for(Vehicles.State car:vehicles.states())
            if(car.hp()>0&&Math.hypot(car.x()-x,car.y()-y)<.90)return false;
        for(World.Device d:world.devices)
            if(d.hp>0&&(d.kind==BARRICADE||d.kind==FIELD_BENCH)&&
                Math.hypot(d.x-x,d.y-y)<(d.kind==FIELD_BENCH?.63:.52))
                return false;
        return true;
    }
    boolean clear(double x,double y,double tx,double ty){
        if(!world.lineClear(x,y,tx,ty))return false;
        double len=Math.hypot(tx-x,ty-y);
        for(World.Device d:world.devices){
            if(d.hp<=0||(d.kind!=BARRICADE&&d.kind!=FIELD_BENCH))continue;
            double t=clamp(((d.x-x)*(tx-x)+(d.y-y)*(ty-y))/(len*len+1e-9),0,1);
            if(Math.hypot(x+(tx-x)*t-d.x,y+(ty-y)*t-d.y)<
                (d.kind==FIELD_BENCH?.43:.36))return false;
        }
        return !thrown.blocksLine(x,y,tx,ty);
    }
    /** Crouch/sprint are intents; host caps actual movement to reject teleport attempts. */
    public synchronized boolean move(int id,double x,double y,double angle,int flags){
        Player p=player(id);
        if(p==null||p.dead||vehicles.carDrivenBy(id)>=0||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(angle)||!free(x,y))return false;
        long elapsed=Math.max(0,clock-p.moveAt);
        double max=((flags&1)!=0?1.20:(flags&2)!=0?4.30:2.85)*p.progression.movementMultiplier()*arsenal.speed(p);
        double permitted=.33+Math.min(elapsed,500)*max*thrown.moveFactor(p)/1000;
        double distance=Math.hypot(x-p.x,y-p.y);
        if(distance>permitted)return false;
        int steps=Math.max(1,(int)Math.ceil(distance/.18));
        for(int step=1;step<=steps;step++){
            double q=step/(double)steps;
            if(!free(p.x+(x-p.x)*q,p.y+(y-p.y)*q))return false;
        }
        double travelled=Math.hypot(x-p.x,y-p.y);
        p.x=x;p.y=y;p.angle=angle;p.flags=flags&3;p.moveAt=clock;
        // Athletics XP needs actual distance walked, not idle movement packet spam.
        p.distanceTowardAthletics=Math.min(30,p.distanceTowardAthletics+travelled);
        if(p.distanceTowardAthletics>=25&&
           p.progression.reward(CharacterProgression.Skill.ATHLETICS,6,clock,20_000)>0)
            p.distanceTowardAthletics-=25;
        // Spawn/login grace permits a brief non-offensive escape to cover.
        refreshVisibility(p);return true;
    }
    public synchronized void refreshVisibility(Player p){
        boolean avenue=(int)p.x%12<3||(int)p.y%12<3;
        double v=.36+(avenue?.25:-.14);
        if((p.flags&1)!=0)v-=.28;
        if((p.flags&2)!=0)v+=.29;
        if(clock-p.shotAt<250)v+=.38;
        if(p.classId==2&&p.shadeUntil>clock)v-=.72;
        p.visibility=opacity(v)*thrown.visibilityFactor(p);
    }
    public synchronized void damage(Player p,int hp,int sanity) {
        if(p==null||p.dead||clock<p.protectedUntil)return;
        int effectiveHp=Math.max(0,hp);
        effectiveHp-=thrown.absorb(p,effectiveHp);
        applyHostDamage(p,effectiveHp,sanity);
    }
    /** Direct environmental injury bypasses tactical projectile shields but
     * still respects the host's nonoffensive arrival grace and normal death. */
    private void applyDeprivationDamage(Player p,int hp){
        if(p==null||p.dead||clock<p.protectedUntil||hp<=0)return;
        applyHostDamage(p,hp,0);
    }
    /** Every lethal cause funnels through the same inventory, arsenal and
     * normal/hardcore XP loss. Never create an alternate survival death path. */
    private void applyHostDamage(Player p,int hp,int sanity){
        p.hp=Math.max(0,p.hp-Math.max(0,hp));
        p.sanity=Math.max(0,p.sanity-Math.max(0,sanity));
        p.lastHurt=clock;
        if(p.hp==0){
            p.dead=true;p.respawnAt=clock;p.protectedUntil=0;
            if(hardcore){
                p.progression.resetCharacter();
                p.frags=0;
                // Character's affiliation and account authentication survive;
                // earned proficiency, achievements and personal score do not.
            }else p.progression.died();
            // No graveyard has been commissioned: legacy carried resources are lost.
            p.cash=0;p.scrap=0;p.medkits=0;
            p.pistolClip=0;p.shellClip=0;p.pistolReserve=0;p.shellReserve=0;
            p.survival.clearOnDeath();
            arsenal.clearOnDeath(p.id);
            if(p.lastSpawnIndex>=0&&clock-p.lastSpawnAt<5000)
                p.earlySpawnDeaths[p.lastSpawnIndex]=Math.min(10,p.earlySpawnDeaths[p.lastSpawnIndex]+1);
        }
    }
    public synchronized boolean pickup(int id,int itemIndex){
        Player p=player(id);
        if(p==null||p.dead||itemIndex<0||itemIndex>=world.pickups.size())return false;
        World.Pickup item=world.pickups.get(itemIndex);
        if(item.taken||Math.hypot(item.x-p.x,item.y-p.y)>.63)return false;
        item.taken=true;
        breakGrace(p);
        p.progression.reward(CharacterProgression.Skill.SCAVENGING,8,clock,3000);
        switch(item.kind){
            case 0 -> p.hp=Math.min(100,p.hp+30);
            case 1 -> p.sanity=Math.min(100,p.sanity+30);
            case 2 -> p.pistolReserve+=24;
            case 3 -> p.shellReserve+=10;
            case 4 -> p.cash+=50;
            case 5 -> p.scrap+=2;
            case 6 -> p.medkits++;
            default -> throw new IllegalStateException("Invalid pickup kind");
        }
        return true;
    }
    /** The city pickup packet must match the object under the actual host-owned aim. */
    public synchronized boolean interactPickup(int playerId,int index){
        Player p=players.get(playerId);
        if(p==null||p.dead||index<0||index>=world.pickups.size())return false;
        List<InteractionFocus.Candidate> candidates=new ArrayList<>();
        for(int i=0;i<world.pickups.size();i++){
            World.Pickup item=world.pickups.get(i);
            if(!item.taken&&Math.hypot(item.x-p.x,item.y-p.y)<1.2)
                candidates.add(new InteractionFocus.Candidate(InteractionFocus.Kind.PICKUP,
                    i,InteractionFocus.pickupName(item.kind),item.x,item.y,.30,.75));
        }
        InteractionFocus.Target target=InteractionFocus.pick(p.x,p.y,p.angle,candidates,
            (x,y,a,b)->world.lineClear(x,y,a,b));
        return target!=null&&target.id()==index&&pickup(playerId,index);
    }
    public synchronized boolean useMedkit(int id){
        Player p=player(id);
        if(p==null||p.dead||p.medkits<=0||p.hp>=100)return false;
        breakGrace(p);p.medkits--;p.hp=Math.min(100,p.hp+55);return true;
    }
    public synchronized boolean reload(int id,int weapon){
        Player p=player(id);if(p==null||p.dead)return false;
        if(clock<p.reloadAt)return false;
        if(weapon==0){
            int rounds=Math.min(12-p.pistolClip,p.pistolReserve);
            if(rounds<=0)return false;
            p.pistolReserve-=rounds;p.pistolClip+=rounds;
            p.reloadAt=clock+Math.round(1700*p.progression.reloadMultiplier());
            breakGrace(p);return true;
        }
        if(weapon==1){
            int rounds=Math.min(6-p.shellClip,p.shellReserve);
            if(rounds<=0)return false;
            p.shellReserve-=rounds;p.shellClip+=rounds;
            p.reloadAt=clock+Math.round(2400*p.progression.reloadMultiplier());
            breakGrace(p);return true;
        }
        return false;
    }
    public synchronized boolean ability(int id){
        Player p=player(id);
        if(p==null||p.dead||clock-p.abilityAt<18000)return false;
        p.abilityAt=clock;
        breakGrace(p);
        switch(p.classId){
            case 0 -> {p.cash+=35;p.scrap++;}
            case 1 -> {p.hp=Math.min(100,p.hp+55);p.sanity=Math.min(100,p.sanity+20);}
            case 2 -> p.shadeUntil=clock+7000;
            case 3 -> {
                p.sanity=Math.min(100,p.sanity+45);
                for(World.Npc n:world.npcs)if(n.hp>0&&Math.hypot(n.x-p.x,n.y-p.y)<4)n.hp-=38;
            }
            default -> throw new IllegalStateException("class outside roster");
        }
        refreshVisibility(p);return true;
    }
    /** Correlates each client shot with an independently checked server damage result. */
    public record ShotReport(long sequence,boolean fired,String status,int targetId,int damage,int ammo){}
    private static int ammo(Player p,int weapon){
        if(p==null)return 0;
        return weapon==0?p.pistolClip:weapon==1?p.shellClip:0;
    }
    public synchronized ShotReport fireReport(int id,int weapon,long sequence){
        Player shooter=player(id);
        int remaining=ammo(shooter,weapon);
        if(shooter==null||sequence<=0)return new ShotReport(sequence,false,"INVALID",0,0,remaining);
        Long last=lastShotSequence.get(id);
        if(last!=null&&sequence<=last)return new ShotReport(sequence,false,"DUPLICATE",0,0,remaining);
        lastShotSequence.put(id,sequence);
        if(shooter.dead)return new ShotReport(sequence,false,"DEAD",0,0,remaining);
        if(weapon<0||weapon>1)return new ShotReport(sequence,false,"WEAPON",0,0,remaining);
        if(clock-shooter.shotAt<(weapon==0?210:850))
            return new ShotReport(sequence,false,"COOLDOWN",0,0,remaining);
        if(remaining==0)return new ShotReport(sequence,false,"NO_AMMO",0,0,remaining);
        Map<Integer,Integer> before=new HashMap<>();
        for(Player other:players.values())if(other.id!=id)before.put(other.id,other.hp);
        int[] deviceHealth=new int[world.devices.size()];
        for(int i=0;i<deviceHealth.length;i++)deviceHealth[i]=world.devices.get(i).hp;
        int[] vehicleHealth=new int[vehicles.states().size()];
        for(Vehicles.State car:vehicles.states())vehicleHealth[car.id()]=car.hp();
        int[] npcHealth=new int[world.npcs.size()];
        for(int i=0;i<npcHealth.length;i++)npcHealth[i]=world.npcs.get(i).hp;
        if(!fire(id,weapon))return new ShotReport(sequence,false,"REJECTED",0,0,ammo(shooter,weapon));
        for(Player other:players.values()){
            Integer previous=before.get(other.id);
            if(previous!=null&&other.hp<previous)
                return new ShotReport(sequence,true,"PLAYER",other.id,previous-other.hp,ammo(shooter,weapon));
        }
        for(int i=0;i<npcHealth.length;i++){
            if(world.npcs.get(i).hp<npcHealth[i])
                return new ShotReport(sequence,true,"NPC",i,npcHealth[i]-world.npcs.get(i).hp,ammo(shooter,weapon));
        }
        for(Vehicles.State car:vehicles.states()){
            if(car.hp()<vehicleHealth[car.id()])
                return new ShotReport(sequence,true,"VEHICLE",car.id(),
                    vehicleHealth[car.id()]-car.hp(),ammo(shooter,weapon));
        }
        for(int i=0;i<deviceHealth.length;i++){
            if(world.devices.get(i).hp<deviceHealth[i])
                return new ShotReport(sequence,true,"DEVICE",i,
                    deviceHealth[i]-world.devices.get(i).hp,ammo(shooter,weapon));
        }
        Integer shield=shieldedShot.remove(id);
        if(shield!=null)return new ShotReport(sequence,true,"SHIELDED",shield,0,ammo(shooter,weapon));
        return new ShotReport(sequence,true,"MISS",0,0,ammo(shooter,weapon));
    }
    public synchronized boolean fire(int id,int weapon){
        Player p=player(id);
        if(p==null||p.dead||vehicles.carDrivenBy(id)>=0||weapon<0||weapon>1)return false;
        shieldedShot.remove(id);
        long cooldown=weapon==0?210:850;
        if(clock-p.shotAt<cooldown)return false;
        if(weapon==0&&p.pistolClip<=0||weapon==1&&p.shellClip<=0)return false;
        if(weapon==0)p.pistolClip--;else p.shellClip--;
        breakGrace(p); // offensive action immediately ends all invulnerability.
        p.shotAt=clock;refreshVisibility(p);
        double max=weapon==0?12:4.5;
        Object target=null;
        double nearest=max;
        for(Player other:players.values()){
            if(other.id==p.id||other.dead)continue;
            double range=Math.hypot(other.x-p.x,other.y-p.y);
            if(range>=nearest||range<.01||!clear(p.x,p.y,other.x,other.y))continue;
            double diff=Math.atan2(Math.sin(Math.atan2(other.y-p.y,other.x-p.x)-p.angle),
                                    Math.cos(Math.atan2(other.y-p.y,other.x-p.x)-p.angle));
            if(Math.abs(diff)<=Math.atan2(.4,range)){nearest=range;target=other;}
        }
        for(World.Npc n:world.npcs){
            // World monsters and Begotten are permanently allied.
            if(n.hp<=0||factions.faction(p.accountKey)==
                CharacterRoster.Faction.THE_BEGOTTEN)continue;
            double range=Math.hypot(n.x-p.x,n.y-p.y);
            if(range>=nearest||range<.01||!clear(p.x,p.y,n.x,n.y))continue;
            double diff=Math.atan2(Math.sin(Math.atan2(n.y-p.y,n.x-p.x)-p.angle),
                                    Math.cos(Math.atan2(n.y-p.y,n.x-p.x)-p.angle));
            if(Math.abs(diff)<=Math.atan2(.4,range)){nearest=range;target=n;}
        }
        for(int i=0;i<world.devices.size();i++){
            World.Device d=world.devices.get(i);
            if(d.hp<=0||p.id==d.ownerId||p.accountKey.equals(d.ownerKey)||
               factions.allied(p.accountKey,d.ownerKey))continue;
            double range=Math.hypot(d.x-p.x,d.y-p.y);
            if(range>=nearest||range<.01||!world.lineClear(p.x,p.y,d.x,d.y))continue;
            double diff=Math.atan2(Math.sin(Math.atan2(d.y-p.y,d.x-p.x)-p.angle),
                Math.cos(Math.atan2(d.y-p.y,d.x-p.x)-p.angle));
            if(Math.abs(diff)<=Math.atan2(.43,range)){nearest=range;target=d;}
        }
        for(Vehicles.State car:vehicles.states()){
            if(car.hp()<=0)continue;
            double range=Math.hypot(car.x()-p.x,car.y()-p.y);
            if(range>=nearest||range<.2||!clear(p.x,p.y,car.x(),car.y()))continue;
            double diff=Math.atan2(Math.sin(Math.atan2(car.y()-p.y,car.x()-p.x)-p.angle),
                                   Math.cos(Math.atan2(car.y()-p.y,car.x()-p.x)-p.angle));
            if(Math.abs(diff)<=Math.atan2(.65,range)){nearest=range;target=car;}
        }
        int amount=(int)Math.round((weapon==0?25:Math.max(18,70-(int)(nearest*8)))*
            p.progression.damageMultiplier());
        if(target instanceof Player victim){
            if(factions.allied(p.accountKey,victim.accountKey)){
                shieldedShot.put(id,victim.id);return true; // ally absorbs shot; no shoot-through
            }
            boolean wasAlive=!victim.dead;damage(victim,amount,weapon==1?6:2);
            if(wasAlive&&victim.dead){p.frags++;p.cash+=25;
                p.progression.reward(CharacterProgression.Skill.MARKSMANSHIP,80,clock,1000);
            }
        } else if(target instanceof World.Device device){
            int index=world.devices.indexOf(device);
            if(!damageDevice(id,index,amount,System.currentTimeMillis()) &&
               raidProtection.protectedOffline(device.ownerKey,System.currentTimeMillis()))
                shieldedShot.put(id,index);
        } else if(target instanceof World.Npc npc){
            npc.hp-=amount;
            if(npc.hp<=0){p.frags++;p.cash+=25;
                p.progression.reward(CharacterProgression.Skill.MARKSMANSHIP,
                    25L+CharacterProgression.monsterLevel(world.seed,npc)*5L,clock,800);
            }
        } else if(target instanceof Vehicles.State car){
            if(vehicles.damageCar(car.id(),amount))cargo.advance(0);
        }
        return true;
    }
    /** The three first industrial facilities are different manufacturing
     * civilizations, and must retain their own legal recipe/cost/tech spine. */
    public static String factionFacility(int kind){
        return switch(kind){
            case UNRAVELED_SCRAP_OUTPOST -> "unraveled-scrap-outpost";
            case PENITENT_VOTIVE_ALTAR -> "penitent-votive-altar";
            case BEGOTTEN_BROOD_NEST -> "begotten-brood-nest";
            case CLAIM_HEART -> "claim-heart";
            case HAND_WORKSHOP -> "hand-workshop";
            case DRY_STORE -> "dry-store";
            case COOKING_HEARTH -> "cooking-hearth";
            case RAIN_CATCHER -> "rain-catcher";
            case CARGO_LAYDOWN -> "cargo-laydown";
            case FORAGER_GARDEN -> "forager-garden";
            case COMPOST_YARD -> "compost-yard";
            case HERB_PLOT -> "herb-plot";
            case CHARCOAL_KILN -> "charcoal-kiln";
            case STONE_FURNACE -> "stone-furnace";
            case FORGE -> "forge";
            case FREIGHT_BAY -> "freight-bay";
            case MATERIALS_RECYCLER -> "materials-recycler";
            case UNRAVELED_MACHINIST_BAY -> "unraveled-machinist-bay";
            case PENITENT_RELIQUARY -> "penitent-reliquary";
            case BEGOTTEN_SPORE_BASIN -> "begotten-spore-basin";
            case STEAM_BOILER -> "steam-boiler";
            case MECHANICAL_SHAFT -> "mechanical-shaft";
            case METAL_PRESS -> "metal-press";
            case UNRAVELED_PRESSURE_SHOP -> "unraveled-pressure-shop";
            case PENITENT_CANTOR_DAIS -> "penitent-cantor-dais";
            case BEGOTTEN_CHITIN_VAT -> "begotten-chitin-vat";
            case GENERATOR -> "generator";
            case ELECTRIC_BUS -> "electric-bus";
            case PRECISION_LATHE -> "precision-lathe";
            case WATER_WORKS -> "water-works";
            case UNRAVELED_SENSOR_LAB -> "unraveled-sensor-lab";
            case PENITENT_CHOIR_RELAY -> "penitent-choir-relay";
            case BEGOTTEN_MEMBRANE_LAB -> "begotten-membrane-lab";
            case CONVEYOR_LINE -> "conveyor-line";
            case AUTOMATIC_SORTER -> "automatic-sorter";
            case INDUSTRIAL_WORKSHOP -> "industrial-workshop";
            case ASSEMBLY_LINE -> "assembly-line";
            case COMMAND_HALL -> "command-hall";
            case RESEARCH_ARCHIVE -> "research-archive";
            case SIGNAL_RELAY -> "signal-relay";
            case SECURITY_GRID -> "security-grid";
            case FUEL_REFINERY -> "fuel-refinery";
            case TERROR_COLLECTOR -> "terror-collector";
            case NUTRIENT_DIGESTOR -> "nutrient-digestor";
            case UNRAVELED_DEFENSE_WORKS -> "unraveled-defense-works";
            case PENITENT_WARD_SANCTUM -> "penitent-ward-sanctum";
            case BEGOTTEN_ORGAN_LATTICE -> "begotten-organ-lattice";
            default -> null;
        };
    }
    public static int kindForFacility(String facility){
        for(int kind=UNRAVELED_SCRAP_OUTPOST;kind<=BEGOTTEN_ORGAN_LATTICE;kind++)
            if(java.util.Objects.equals(factionFacility(kind),facility))return kind;
        return -1;
    }
    private static CharacterRoster.Faction facilityFaction(int kind){
        return switch(kind){
            case UNRAVELED_SCRAP_OUTPOST -> CharacterRoster.Faction.THE_UNRAVELED;
            case PENITENT_VOTIVE_ALTAR -> CharacterRoster.Faction.THE_PENITENT;
            case BEGOTTEN_BROOD_NEST,BEGOTTEN_SPORE_BASIN,BEGOTTEN_CHITIN_VAT,BEGOTTEN_MEMBRANE_LAB,
                NUTRIENT_DIGESTOR,BEGOTTEN_ORGAN_LATTICE ->
                CharacterRoster.Faction.THE_BEGOTTEN;
            case UNRAVELED_MACHINIST_BAY ->
                CharacterRoster.Faction.THE_UNRAVELED;
            case PENITENT_RELIQUARY,PENITENT_CANTOR_DAIS,PENITENT_CHOIR_RELAY,
                TERROR_COLLECTOR,PENITENT_WARD_SANCTUM ->
                CharacterRoster.Faction.THE_PENITENT;
            case UNRAVELED_PRESSURE_SHOP,UNRAVELED_SENSOR_LAB,UNRAVELED_DEFENSE_WORKS ->
                CharacterRoster.Faction.THE_UNRAVELED;
            default -> null;
        };
    }
    public static String firstProcess(int kind){
        return switch(kind){
            case UNRAVELED_SCRAP_OUTPOST -> "unraveled-recover-scrap";
            case PENITENT_VOTIVE_ALTAR -> "penitent-weave-ritual";
            case BEGOTTEN_BROOD_NEST -> "begotten-mix-slurry";
            case COOKING_HEARTH -> "split-firewood";
            case FORAGER_GARDEN -> "grow-field-crops";
            case COMPOST_YARD -> "compost-ferment";
            case HERB_PLOT -> "grow-medicinals";
            case CHARCOAL_KILN -> "charcoal-burn";
            case STONE_FURNACE -> "firebrick-batch";
            case FORGE -> "iron-bloom";
            case MATERIALS_RECYCLER -> "sort-mineral-feed";
            case UNRAVELED_MACHINIST_BAY -> "unraveled-make-bearings";
            case PENITENT_RELIQUARY -> "penitent-engrave-reliquary";
            case BEGOTTEN_SPORE_BASIN -> "begotten-culture-spores";
            case METAL_PRESS -> "driveshaft";
            case UNRAVELED_PRESSURE_SHOP -> "unraveled-pressure-vessel";
            case PENITENT_CANTOR_DAIS -> "penitent-cast-censer";
            case BEGOTTEN_CHITIN_VAT -> "begotten-harden-chitin";
            case PRECISION_LATHE -> "copper-wire";
            case UNRAVELED_SENSOR_LAB -> "unraveled-calibrate-sensors";
            case PENITENT_CHOIR_RELAY -> "penitent-score-hymn";
            case BEGOTTEN_MEMBRANE_LAB -> "begotten-grow-membrane";
            case INDUSTRIAL_WORKSHOP -> "precision-feedlines";
            case ASSEMBLY_LINE -> "production-controller";
            case RESEARCH_ARCHIVE -> "chorus-resonance";
            case SECURITY_GRID -> "secure-ammo-cache";
            case TERROR_COLLECTOR -> "bound-liturgy";
            case NUTRIENT_DIGESTOR -> "biomass-feed";
            case UNRAVELED_DEFENSE_WORKS -> "unraveled-defense-scanner";
            case PENITENT_WARD_SANCTUM -> "penitent-sanctify-shell";
            case BEGOTTEN_ORGAN_LATTICE -> "begotten-pulse-organ";
            default -> null;
        };
    }
    private static Map<SurvivalCrafting.Item,Integer> processInputs(String processId){
        var recipe=CraftingEraRegistry.process(processId);
        EnumMap<SurvivalCrafting.Item,Integer> materials=
            new EnumMap<>(SurvivalCrafting.Item.class);
        for(var item:recipe.inputs().entrySet())
            materials.put(SurvivalCrafting.Item.valueOf(item.getKey()),item.getValue());
        return materials;
    }
    /** A building is not considered a dependency merely because its catalog
     * descriptor exists. E2 and support structures require real nearby,
     * living, same-owner installed structures. Older E1 installs are retained
     * for backwards compatibility with realms created before this gate. */
    private boolean hasRealFacilityDependencies(Player player,int kind,double x,double y){
        if(kind<CLAIM_HEART)return true;
        var spec=CraftingEraRegistry.facility(factionFacility(kind));
        if(spec==null)return false;
        for(String required:spec.requires()){
            boolean installed=false;
            for(World.Device candidate:world.devices){
                if(candidate.hp<=0||!player.accountKey.equals(candidate.ownerKey)||
                   Math.hypot(candidate.x-x,candidate.y-y)>12)continue;
                if(required.equals(factionFacility(candidate.kind))){
                    installed=true;break;
                }
            }
            if(!installed)return false;
        }
        return true;
    }
    private static Map<SurvivalCrafting.Item,Integer> facilityMaterialCost(int kind){
        String id=factionFacility(kind);
        if(id==null)return Map.of();
        var spec=CraftingEraRegistry.facility(id);
        EnumMap<SurvivalCrafting.Item,Integer> need=
            new EnumMap<>(SurvivalCrafting.Item.class);
        for(var entry:spec.materials().entrySet())
            need.put(SurvivalCrafting.Item.valueOf(entry.getKey()),entry.getValue());
        return need;
    }
    private World.Device fundedFactoryCrate(Player p,double x,double y,int kind){
        Map<SurvivalCrafting.Item,Integer> need=facilityMaterialCost(kind);
        for(World.Device d:world.devices){
            if(d.hp<=0||d.kind!=STORAGE_CRATE||
               !p.accountKey.equals(d.ownerKey)||
               Math.hypot(d.x-x,d.y-y)>5.5)continue;
            boolean complete=true;
            for(var material:need.entrySet())
                if(d.storage.count(material.getKey())<material.getValue()){
                    complete=false;break;
                }
            if(complete)return d;
        }
        return null;
    }
    public record BuildReview(boolean allowed,String reason,int cashCost,int suppliesCost,double x,double y){}
    /** Returns a concrete rejection without mutating inventories. */
    public synchronized BuildReview reviewBuild(int id,int kind){
        Player p=player(id);
        if(p==null||p.dead)return new BuildReview(false,"DEAD_OR_MISSING",0,0,0,0);
        if(vehicles.carDrivenBy(id)>=0)return new BuildReview(false,"IN_VEHICLE",0,0,0,0);
        if(kind<0||kind>BEGOTTEN_ORGAN_LATTICE)return new BuildReview(false,"UNKNOWN_STRUCTURE",0,0,0,0);
        int price=kind>=FIELD_FIRE?0:
            kind==CAMP_STAKE?35:kind==0?(p.classId==0?60:100):kind==1?75:kind==2?120:45;
        int supplies=kind>=FIELD_FIRE?0:
            kind==0?3:kind==1?1:kind==2?2:1;
        double x=p.x+Math.cos(p.angle)*1.1,y=p.y+Math.sin(p.angle)*1.1;
        // The current 132x132 arena is an ENTIRE fixed city sector. Player
        // structures are wilderness-only; reject here before any resource debit
        // or spawn/route heuristics. Continental wilderness activation is gated.
        if(world.detailedCity(x,y)&&world.city.worldStructureAt(x,y,.55))
            return new BuildReview(false,"CITY_PROTECTED",price,supplies,x,y);
        if(world.devices.size()>=100)return new BuildReview(false,"BUILD_LIMIT",price,supplies,x,y);
        if(p.cash<price||p.scrap<supplies)return new BuildReview(false,"RESOURCES",price,supplies,x,y);
        if(kind==CAMP_STAKE){
            if(p.survival.count(SurvivalCrafting.Item.TENT_KIT)<1)
                return new BuildReview(false,"REQUIRES_CRAFTED_TENT",price,supplies,x,y);
            for(World.Device existing:world.devices)
                if(existing.kind==CAMP_STAKE&&existing.hp>0&&
                   p.accountKey.equals(existing.ownerKey))
                    return new BuildReview(false,"ALREADY_OWNS_CAMP",price,supplies,x,y);
        }
        if(factionFacility(kind)!=null){
            if(world.detailedCity(x,y))
                return new BuildReview(false,"CITY_PROTECTED",price,supplies,x,y);
            CharacterRoster.Faction needed=facilityFaction(kind);
            if(needed!=null&&factions.faction(p.accountKey)!=needed)
                return new BuildReview(false,"WRONG_FACTION_FACILITY",price,supplies,x,y);
            if(world.devices.stream().anyMatch(d->d.hp>0&&d.kind==kind&&
               p.accountKey.equals(d.ownerKey)))
                return new BuildReview(false,"ALREADY_OWNS_FACTION_SITE",price,supplies,x,y);
            boolean ownsCamp=world.devices.stream().anyMatch(d->
                d.hp>0&&d.kind==CAMP_STAKE&&p.accountKey.equals(d.ownerKey)&&
                Math.hypot(x-d.x,y-d.y)<=8);
            if(!ownsCamp)return new BuildReview(false,"REQUIRES_OWN_CAMP",
                price,supplies,x,y);
            if(!hasRealFacilityDependencies(p,kind,x,y))
                return new BuildReview(false,"MISSING_PHYSICAL_PREREQUISITE",
                    price,supplies,x,y);
            if(fundedFactoryCrate(p,x,y,kind)==null)
                return new BuildReview(false,"NEEDS_STOCKED_OWN_CRATE",
                    price,supplies,x,y);
        }
        if(kind==STORAGE_CRATE&&world.detailedCity(x,y))
            return new BuildReview(false,"CITY_PROTECTED",price,supplies,x,y);
        if(kind==STORAGE_CRATE&&p.survival.count(SurvivalCrafting.Item.CRATE_KIT)<1)
            return new BuildReview(false,"REQUIRES_CRAFTED_CRATE",price,supplies,x,y);
        if(kind==FIELD_BENCH&&world.detailedCity(x,y))
            return new BuildReview(false,"CITY_PROTECTED",price,supplies,x,y);
        if(kind==FIELD_BENCH&&p.survival.count(SurvivalCrafting.Item.WORKBENCH)<1)
            return new BuildReview(false,"REQUIRES_CRAFTED_WORKBENCH",price,supplies,x,y);
        if(kind==FIELD_FIRE||kind==FIELD_BENCH||kind==STORAGE_CRATE){
            if(kind==FIELD_FIRE&&p.survival.count(SurvivalCrafting.Item.CAMPFIRE)<1)
                return new BuildReview(false,"REQUIRES_CAMPFIRE_KIT",price,supplies,x,y);
            boolean hasCamp=false;
            for(World.Device d:world.devices){
                if(d.hp<=0||!p.accountKey.equals(d.ownerKey))continue;
                if(d.kind==kind&&kind!=STORAGE_CRATE)
                    return new BuildReview(false,
                        kind==FIELD_FIRE?"ALREADY_OWNS_FIRE":"ALREADY_OWNS_BENCH",
                        price,supplies,x,y);
                if(d.kind==CAMP_STAKE&&Math.hypot(d.x-x,d.y-y)<=6)
                    hasCamp=true;
            }
            if(!hasCamp)
                return new BuildReview(false,"REQUIRES_OWN_CAMP",price,supplies,x,y);
            if(kind==STORAGE_CRATE&&world.devices.stream().filter(d->
                d.hp>0&&d.kind==STORAGE_CRATE&&p.accountKey.equals(d.ownerKey)).count()>=3)
                return new BuildReview(false,"CRATE_LIMIT",price,supplies,x,y);
        }
        // Dedicated spawn-clearance feedback takes priority over overlapping world roads.
        for(double[] spawn:world.spawns)
            if(Math.hypot(x-spawn[0],y-spawn[1])<2.0)
                return new BuildReview(false,"SPAWN_CLEARANCE",price,supplies,x,y);
        if(world.worldStructureAt(x,y,.55))
            return new BuildReview(false,"WORLD_STRUCTURE",price,supplies,x,y);
        if(!free(x,y)||!clear(p.x,p.y,x,y))
            return new BuildReview(false,"OBSTRUCTED",price,supplies,x,y);
        for(World.Device d:world.devices)
            if(d.hp>0&&Math.hypot(d.x-x,d.y-y)<1.2)
                return new BuildReview(false,"DEVICE_CLEARANCE",price,supplies,x,y);
        if(kind==BARRICADE&&!routesRemainOpen(x,y))
            return new BuildReview(false,"ROUTE_BLOCKED",price,supplies,x,y);
        return new BuildReview(true,"OK",price,supplies,x,y);
    }
    /**
     * A proposed barricade cannot disconnect any of the three established spawn sectors.
     * Pathfinding reads actual colliders and existing barricades, not a decorative map.
     */
    private boolean routesRemainOpen(double blockX,double blockY){
        final int n=World.SIZE;
        boolean[][] seen=new boolean[n][n];
        ArrayDeque<int[]> pending=new ArrayDeque<>();
        double[] first=world.spawns[0];
        int sx=(int)first[0],sy=(int)first[1];
        pending.add(new int[]{sx,sy});seen[sx][sy]=true;
        int[][] dirs={{1,0},{-1,0},{0,1},{0,-1}};
        while(!pending.isEmpty()){
            int[] pos=pending.removeFirst();
            for(int[] dir:dirs){
                int xx=pos[0]+dir[0],yy=pos[1]+dir[1];
                if(xx<1||yy<1||xx>=n-1||yy>=n-1||seen[xx][yy])continue;
                double cx=xx+.5,cy=yy+.5;
                if(Math.hypot(cx-blockX,cy-blockY)<.72||!free(cx,cy))continue;
                seen[xx][yy]=true;pending.addLast(new int[]{xx,yy});
            }
        }
        for(double[] sector:world.spawns)
            if(!seen[(int)sector[0]][(int)sector[1]])return false;
        return true;
    }
    public record BuildOutcome(boolean built,BuildReview review,int deviceIndex){}
    /** Atomic authoritative validation, inventory debit and structure creation. */
    public synchronized BuildOutcome buildOutcome(int id,int kind){
        BuildReview review=reviewBuild(id,kind);
        if(!review.allowed())return new BuildOutcome(false,review,-1);
        Player p=player(id);
        if(kind==CAMP_STAKE&&!p.survival.debit(SurvivalCrafting.Item.TENT_KIT,1))
            throw new IllegalStateException("Camp inventory changed after authoritative preflight");
        if(kind==FIELD_FIRE&&!p.survival.debit(SurvivalCrafting.Item.CAMPFIRE,1))
            throw new IllegalStateException("Fire kit changed after authoritative preflight");
        if(kind==FIELD_BENCH&&!p.survival.debit(SurvivalCrafting.Item.WORKBENCH,1))
            throw new IllegalStateException("Workbench kit changed after authoritative preflight");
        if(kind==STORAGE_CRATE&&!p.survival.debit(SurvivalCrafting.Item.CRATE_KIT,1))
            throw new IllegalStateException("Crate kit changed after authoritative preflight");
        if(factionFacility(kind)!=null){
            World.Device funded=fundedFactoryCrate(p,review.x(),review.y(),kind);
            if(funded==null||!funded.storage.consume(facilityMaterialCost(kind)))
                throw new IllegalStateException("Verified factory funding changed");
        }
        p.cash-=review.cashCost();p.scrap-=review.suppliesCost();
        World.Device device=new World.Device(review.x(),review.y(),kind);
        device.ownerId=p.id;
        device.ownerKey=p.accountKey;
        world.devices.add(device);
        return new BuildOutcome(true,review,world.devices.size()-1);
    }
    public synchronized boolean build(int id,int kind){return buildOutcome(id,kind).built();}
    /** Host-validated material exchange with an actual owned wilderness crate. */
    public synchronized boolean transferCampStock(int id,boolean taking,
                               SurvivalCrafting.Item item,int quantity){
        Player p=players.get(id);
        if(p==null||p.dead||item==null||quantity<1||quantity>CampStockpile.MAX_STACK||
           vehicles.carDrivenBy(id)>=0||p.accountKey==null||
           !p.accountKey.startsWith("acct_"))return false;
        for(World.Device d:world.devices)
            if(d.hp>0&&d.kind==STORAGE_CRATE&&
               p.accountKey.equals(d.ownerKey)&&
               Math.hypot(p.x-d.x,p.y-d.y)<=2.6&&
               world.lineClear(p.x,p.y,d.x,d.y)){
                boolean ok=taking?d.storage.withdraw(p.survival,item,quantity):
                                  d.storage.deposit(p.survival,item,quantity);
                if(ok)breakGrace(p);
                return ok;
            }
        return false;
    }
    public synchronized Map<SurvivalCrafting.Item,Integer> nearbyCampStock(int id){
        Player p=players.get(id);
        if(p==null||p.dead||vehicles.carDrivenBy(id)>=0)return Map.of();
        for(World.Device d:world.devices)
            if(d.hp>0&&d.kind==STORAGE_CRATE&&
               p.accountKey.equals(d.ownerKey)&&
               Math.hypot(p.x-d.x,p.y-d.y)<=2.6&&
               world.lineClear(p.x,p.y,d.x,d.y))
                return d.storage.snapshot();
        return Map.of();
    }
    /** Begin one real faction-only recipe using existing physical crate stock.
     * The order and input debit are both in the world ledger, ensuring a
     * crash cannot independently reissue previously spent materials. */
    public synchronized boolean startFactionBatch(int playerId){
        return startFactionBatch(playerId,null);
    }
    /** Select a concrete registered recipe and matching PHYSICAL facility;
     * the host, not the menu, determines ownership, faction, cost and range. */
    public synchronized boolean startFactionBatch(int playerId,String requestedRecipe){
        Player p=players.get(playerId);
        if(p==null||p.dead||vehicles.carDrivenBy(playerId)>=0||
           !p.accountKey.startsWith("acct_"))return false;
        if(requestedRecipe!=null&&(requestedRecipe.length()>80||
           !requestedRecipe.matches("[a-z0-9-]+")))return false;
        for(World.Device device:world.devices){
            int kind=device.kind;
            String processId=requestedRecipe==null?firstProcess(kind):requestedRecipe;
            var process=CraftingEraRegistry.process(processId);
            if(device.hp<=0||process==null||
               !process.facility().equals(factionFacility(kind))||
               device.productionActive||
               !p.accountKey.equals(device.ownerKey)||
               (facilityFaction(kind)!=null&&
                factions.faction(p.accountKey)!=facilityFaction(kind))||
               (process.faction()!=CraftingEraRegistry.Faction.ALL&&
                factions.faction(p.accountKey)!=switch(process.faction()){
                    case UNRAVELED -> CharacterRoster.Faction.THE_UNRAVELED;
                    case PENITENT -> CharacterRoster.Faction.THE_PENITENT;
                    case BEGOTTEN -> CharacterRoster.Faction.THE_BEGOTTEN;
                    default -> null;
                })||
               Math.hypot(p.x-device.x,p.y-device.y)>2.6||
               !world.lineClear(p.x,p.y,device.x,device.y))continue;
            Map<SurvivalCrafting.Item,Integer> inputs=processInputs(processId);
            for(World.Device crate:world.devices){
                if(crate.hp<=0||crate.kind!=STORAGE_CRATE||
                   !p.accountKey.equals(crate.ownerKey)||
                   Math.hypot(crate.x-device.x,crate.y-device.y)>5.5)continue;
                if(process.utilityDemand()>0){
                    boolean powered=process.era().ordinal()>=CraftingEraRegistry.Era.ELECTRIC.ordinal()?
                        ElectricUtility.available(world.devices,device,
                            process.utilityDemand(),Map.of())!=null:
                        SteamUtility.available(world.devices,device,
                            process.utilityDemand(),Map.of())!=null;
                    if(!powered)continue;
                }
                if(!crate.storage.consume(inputs))continue;
                device.productionMs=0;
                device.productionRecipeId=processId;
                device.productionActive=true;
                breakGrace(p);
                p.progression.reward(CharacterProgression.Skill.ENGINEERING,12,clock,3500);
                return true;
            }
        }
        return false;
    }
    /** A one-slot next E5 work order. Enqueueing never spends ingredients.
     * Only a matched, owned, permanent AUTOMATION-era physical machine can
     * accept a job, and no client can replace an occupied slot. */
    public synchronized boolean programNextAutomation(int playerId,String requested){
        Player p=players.get(playerId);
        if(p==null||p.dead||vehicles.carDrivenBy(playerId)>=0||
           p.accountKey==null||!p.accountKey.startsWith("acct_")||
           requested==null||requested.length()>80)return false;
        boolean cancel=requested.equals("NONE");
        var job=cancel?null:CraftingEraRegistry.process(requested);
        if(!cancel&&(job==null||job.era()!=CraftingEraRegistry.Era.AUTOMATION||
            !requested.matches("[a-z0-9-]+")||job.utilityDemand()<1))
            return false;
        for(World.Device device:world.devices){
            if(device.hp<=0||!p.accountKey.equals(device.ownerKey)||
               Math.hypot(p.x-device.x,p.y-device.y)>2.6||
               !world.lineClear(p.x,p.y,device.x,device.y))continue;
            String station=factionFacility(device.kind);
            var spec=CraftingEraRegistry.facility(station);
            if(spec==null||spec.era()!=CraftingEraRegistry.Era.AUTOMATION||
               (facilityFaction(device.kind)!=null&&
                factions.faction(p.accountKey)!=facilityFaction(device.kind)))continue;
            if(cancel){
                if(device.queuedRecipeId.isEmpty())continue;
                device.queuedRecipeId="";
            }else{
                if(!device.queuedRecipeId.isEmpty()||!job.facility().equals(station)||
                   (job.faction()!=CraftingEraRegistry.Faction.ALL&&
                    factions.faction(p.accountKey)!=switch(job.faction()){
                        case UNRAVELED -> CharacterRoster.Faction.THE_UNRAVELED;
                        case PENITENT -> CharacterRoster.Faction.THE_PENITENT;
                        case BEGOTTEN -> CharacterRoster.Faction.THE_BEGOTTEN;
                        default -> null;
                    }))continue;
                device.queuedRecipeId=requested;
            }
            breakGrace(p);
            return true;
        }
        return false;
    }
    /** Queued E5 work requires power LEFT after already-operating factories,
     * conveyors and sorters. Reservation is per host tick and per generator.
     * Never debit a queued recipe merely because an overloaded grid exists. */
    private void advanceQueuedAutomation(Map<World.Device,Integer> allocated){
        for(World.Device d:world.devices){
            if(d.hp<=0||d.productionActive||d.queuedRecipeId.isEmpty())continue;
            var recipe=CraftingEraRegistry.process(d.queuedRecipeId);
            if(recipe==null||recipe.era()!=CraftingEraRegistry.Era.AUTOMATION||
               !recipe.facility().equals(factionFacility(d.kind))||
               d.ownerKey==null)continue;
            if((d.kind==PENITENT_WARD_SANCTUM||d.kind==TERROR_COLLECTOR)&&
               liturgicalWitnesses(d)==0)continue;
            World.Device generator=ElectricUtility.available(world.devices,d,
                recipe.utilityDemand(),allocated);
            if(generator==null)continue;
            var inputs=processInputs(recipe.id());
            for(World.Device crate:world.devices){
                if(crate.hp<=0||crate.kind!=STORAGE_CRATE||
                   !d.ownerKey.equals(crate.ownerKey)||
                   Math.hypot(crate.x-d.x,crate.y-d.y)>5.5||
                   !crate.storage.consume(inputs))continue;
                d.productionActive=true;
                d.productionMs=0;
                d.productionRecipeId=recipe.id();
                d.queuedRecipeId="";
                allocated.merge(generator,recipe.utilityDemand(),Integer::sum);
                break;
            }
        }
    }
    /** Owner programs one named stock item on a real reachable conveyor.
     * The route remains physically directional, west source to east receiver. */
    public synchronized boolean programConveyor(int id,String itemName){
        Player p=players.get(id);
        if(p==null||p.dead||vehicles.carDrivenBy(id)>=0||
           p.accountKey==null||!p.accountKey.startsWith("acct_")||
           itemName==null||itemName.length()>80)return false;
        SurvivalCrafting.Item item=null;
        if(!itemName.equals("NONE"))try{
            item=SurvivalCrafting.Item.valueOf(itemName);
        }catch(IllegalArgumentException invalid){return false;}
        for(World.Device conveyor:world.devices){
            if(conveyor.kind!=CONVEYOR_LINE||conveyor.hp<=0||
               !p.accountKey.equals(conveyor.ownerKey)||
               Math.hypot(p.x-conveyor.x,p.y-conveyor.y)>2.6||
               !world.lineClear(p.x,p.y,conveyor.x,conveyor.y))continue;
            if(item!=null&&ConveyorLogistics.between(conveyor,world.devices)==null)
                return false;
            conveyor.routeItem=item==null?"":item.name();
            conveyor.routeMs=0;
            breakGrace(p);return true;
        }
        return false;
    }
    /** Supply is physically routed once per host tick, not generated from a
     * recipe descriptor. Competing powered machines share boiler capacity. */
    private void advanceFactoryUtilities(long ms){
        Map<World.Device,Integer> demand=new IdentityHashMap<>();
        Map<World.Device,Integer> electricDraw=new IdentityHashMap<>();
        for(World.Device d:world.devices){
            if(d.hp<=0||!d.productionActive)continue;
            var recipe=CraftingEraRegistry.process(d.productionRecipeId);
            if(recipe==null)continue;
            long needMs=recipe.seconds()*1000L;
            // A Penitent cantor dais does not spend pressurized steam on a
            // ritual when no allied witness is present to perform it.
            if((d.kind==PENITENT_CANTOR_DAIS||d.kind==PENITENT_CHOIR_RELAY||
                d.kind==PENITENT_WARD_SANCTUM||d.kind==TERROR_COLLECTOR)&&
               liturgicalWitnesses(d)==0&&d.productionMs<needMs)continue;
            if(recipe.era().ordinal()>=CraftingEraRegistry.Era.ELECTRIC.ordinal()&&
               recipe.utilityDemand()>0&&d.productionMs<needMs){
                World.Device generator=ElectricUtility.available(world.devices,d,
                    recipe.utilityDemand(),electricDraw);
                if(generator==null)continue;
                electricDraw.merge(generator,recipe.utilityDemand(),Integer::sum);
                advanceFactionBatch(d,ms);
            }else if(recipe.utilityDemand()>0&&d.productionMs<needMs){
                World.Device boiler=SteamUtility.available(world.devices,d,
                    recipe.utilityDemand(),demand);
                if(boiler==null)continue;
                demand.merge(boiler,recipe.utilityDemand(),Integer::sum);
                advanceFactionBatch(d,ms);
            }else advanceFactionBatch(d,ms);
        }
        for(World.Device boiler:demand.keySet())
            boiler.steamMs=Math.max(0,boiler.steamMs-ms);
        // Each stationary conveyor is its own paid logistics mechanism,
        // sharing exactly the same grid budget as adjacent E5 fabrication.
        for(World.Device conveyor:world.devices){
            if(conveyor.kind!=CONVEYOR_LINE||conveyor.hp<=0||
               conveyor.routeItem.isEmpty())continue;
            SurvivalCrafting.Item item=SurvivalCrafting.Item.valueOf(conveyor.routeItem);
            var route=ConveyorLogistics.between(conveyor,world.devices);
            if(!ConveyorLogistics.ready(route,item))continue;
            boolean sorted=ConveyorLogistics.sorted(conveyor,world.devices);
            int beltLoad=sorted?ConveyorLogistics.SORTED_DEMAND:ConveyorLogistics.DEMAND;
            World.Device power=ElectricUtility.available(world.devices,conveyor,
                beltLoad,electricDraw);
            if(power==null)continue;
            electricDraw.merge(power,beltLoad,Integer::sum);
            conveyor.routeMs=Math.min(ConveyorLogistics.TICK_MS,conveyor.routeMs+ms);
            if(conveyor.routeMs>=ConveyorLogistics.TICK_MS){
                if(ConveyorLogistics.moveOne(route,item)){
                    // Additional throughput is paid by a real powered sorter,
                    // and never overfills the receiving warehouse.
                    if(sorted)ConveyorLogistics.moveOne(route,item);
                    conveyor.routeMs=0;
                }
            }
        }
        // Launch unpaid E5 jobs only after active factory AND conveyor loads
        // are known. A copy reserves capacity for multiple new starts without
        // charging a factory for a tick in which it did not yet manufacture.
        advanceQueuedAutomation(new IdentityHashMap<>(electricDraw));
        for(World.Device generator:electricDraw.keySet())
            generator.electricMs=Math.max(0,generator.electricMs-ms);
    }
    /** Refuel a real boiler at the player's own camp; fuel is consumed from
     * an owned nearby fixed crate and is never a temporary field deployable. */
    public synchronized boolean fireSteamBoiler(int playerId){
        Player p=players.get(playerId);
        if(p==null||p.dead||vehicles.carDrivenBy(playerId)>=0||
           p.accountKey==null||!p.accountKey.startsWith("acct_"))return false;
        for(World.Device boiler:world.devices){
            if(boiler.hp<=0||boiler.kind!=STEAM_BOILER||
               !p.accountKey.equals(boiler.ownerKey)||
               Math.hypot(p.x-boiler.x,p.y-boiler.y)>2.6||
               !world.lineClear(p.x,p.y,boiler.x,boiler.y)||
               boiler.steamMs>SteamUtility.MAX_CHARGE_MS/2)continue;
            for(World.Device crate:world.devices){
                if(crate.hp<=0||crate.kind!=STORAGE_CRATE||
                   !p.accountKey.equals(crate.ownerKey)||
                   Math.hypot(crate.x-boiler.x,crate.y-boiler.y)>5.5)continue;
                if(!crate.storage.consume(SteamUtility.FUEL))continue;
                boiler.steamMs=Math.min(SteamUtility.MAX_CHARGE_MS,
                    boiler.steamMs+SteamUtility.MAX_CHARGE_MS);
                breakGrace(p);
                return true;
            }
        }
        return false;
    }
    /** Steam-driven generator consumes real stored fuel and pressurized steam. */
    public synchronized boolean chargeGenerator(int playerId){
        Player p=players.get(playerId);
        if(p==null||p.dead||vehicles.carDrivenBy(playerId)>=0||
           p.accountKey==null||!p.accountKey.startsWith("acct_"))return false;
        for(World.Device generator:world.devices){
            if(generator.kind!=GENERATOR||generator.hp<=0||
               !p.accountKey.equals(generator.ownerKey)||
               Math.hypot(p.x-generator.x,p.y-generator.y)>2.6||
               !world.lineClear(p.x,p.y,generator.x,generator.y)||
               generator.electricMs>ElectricUtility.MAX_CHARGE_MS/2)continue;
            World.Device boiler=ElectricUtility.turbineSource(world.devices,generator);
            if(boiler==null)continue;
            for(World.Device crate:world.devices){
                if(crate.hp<=0||crate.kind!=STORAGE_CRATE||
                   !p.accountKey.equals(crate.ownerKey)||
                   Math.hypot(crate.x-generator.x,crate.y-generator.y)>5.5)continue;
                if(!crate.storage.consume(ElectricUtility.FUEL))continue;
                boiler.steamMs-=ElectricUtility.STEAM_STARTUP_MS;
                generator.electricMs=Math.min(ElectricUtility.MAX_CHARGE_MS,
                    generator.electricMs+ElectricUtility.MAX_CHARGE_MS);
                breakGrace(p);return true;
            }
        }
        return false;
    }
    /** Penitent rites are participant-operated spiritual manufacturing,
     * not unattended motors that consume heat in an empty building. */
    private int liturgicalWitnesses(World.Device device){
        int count=0;
        for(Player p:players.values()){
            if(!p.dead&&factions.faction(p.accountKey)==
               CharacterRoster.Faction.THE_PENITENT&&
               factions.allied(p.accountKey,device.ownerKey)&&
               Math.hypot(p.x-device.x,p.y-device.y)<=3.5&&
               world.lineClear(p.x,p.y,device.x,device.y))count++;
        }
        return Math.min(3,count);
    }
    /** Distinct manufacturing logic runs after actual steam distribution. */
    private void advanceFactionBatch(World.Device device,long ms){
        if(device.hp<=0||!device.productionActive)return;
        String id=device.productionRecipeId;
        var process=CraftingEraRegistry.process(id);
        if(process==null||!process.facility().equals(factionFacility(device.kind)))
            throw new IllegalStateException("Invalid faction work order on world device");
        long required=process.seconds()*1000L;
        long gain=switch(device.kind){
            case PENITENT_VOTIVE_ALTAR,PENITENT_RELIQUARY,PENITENT_CANTOR_DAIS,PENITENT_CHOIR_RELAY,
                 PENITENT_WARD_SANCTUM,TERROR_COLLECTOR ->
                ms*liturgicalWitnesses(device); // no rite without a present choir
            // All other salvage and cultivation equipment has a physical
            // stocked facility and independently registered batch ingredients.
            // Purely Begotten and Unraveled sites run unattended, while rites
            // remain witness-dependent even at the higher tier.
            default -> ms;
        };
        device.productionMs=Math.min(required,device.productionMs+gain);
        if(device.productionMs<required)return;
        SurvivalCrafting.Item item=SurvivalCrafting.Item.valueOf(process.output());
        for(World.Device crate:world.devices){
            if(crate.hp<=0||crate.kind!=STORAGE_CRATE||
               !device.ownerKey.equals(crate.ownerKey)||
               Math.hypot(device.x-crate.x,device.y-crate.y)>5.5)continue;
            if(!crate.storage.delivered(item,process.quantity()))continue;
            device.productionActive=false;
            device.productionRecipeId="";
            device.productionMs=0;
            device.productionCompleted++;
            return;
        }
        // Completed production waits for physical storage capacity: no
        // material appears from thin air or off-screen remote transactions.
    }
    /** No client-predicted industrial progress or hidden remote machine access. */
    public synchronized String factionBatchStatus(int id){
        Player p=players.get(id);
        if(p==null||p.dead)return "NO_LIVE_OPERATOR";
        for(World.Device d:world.devices){
            if(d.hp<=0||firstProcess(d.kind)==null||
               !p.accountKey.equals(d.ownerKey)||
               Math.hypot(p.x-d.x,p.y-d.y)>3.0)continue;
            if(!d.productionActive)continue;
            var process=CraftingEraRegistry.process(d.productionRecipeId);
            long duration=process.seconds();
            return "RUNNING_"+d.productionRecipeId+"_"+
                (d.productionMs/1000)+"_OF_"+duration+"_SECONDS"+
                (d.queuedRecipeId.isEmpty()?"":"_NEXT_"+d.queuedRecipeId);
        }
        for(World.Device d:world.devices)
            if(d.hp>0&&firstProcess(d.kind)!=null&&
               p.accountKey.equals(d.ownerKey)&&
               Math.hypot(p.x-d.x,p.y-d.y)<=3.0)
                return "IDLE_"+factionFacility(d.kind)+"_COMPLETED_"+d.productionCompleted+
                    (d.queuedRecipeId.isEmpty()?"":"_WAITING_NEXT_"+d.queuedRecipeId);
        return "NO_OWN_WORKSITE_IN_REACH";
    }
    /** All base PvP damage passes through this gate, including future explosives. */
    public synchronized boolean damageDevice(int attackerId,int deviceIndex,int amount,long utcMillis){
        Player attacker=players.get(attackerId);
        if(attacker==null||attacker.dead||deviceIndex<0||deviceIndex>=world.devices.size()||
           amount<=0||amount>250)return false;
        World.Device device=world.devices.get(deviceIndex);
        if(device.hp<=0||attacker.accountKey.equals(device.ownerKey)||
           factions.allied(attacker.accountKey,device.ownerKey))return false;
        if(device.ownerKey!=null&&device.ownerKey.startsWith("acct_")&&
           raidProtection.protectedOffline(device.ownerKey,utcMillis))return false;
        device.hp=Math.max(0,device.hp-amount);
        if(device.hp==0&&device.kind==STORAGE_CRATE)device.storage.destroy();
        if((device.kind==BEGOTTEN_BROOD_NEST||
            device.kind==BEGOTTEN_SPORE_BASIN||
            device.kind==BEGOTTEN_CHITIN_VAT||
            device.kind==BEGOTTEN_MEMBRANE_LAB||
            device.kind==NUTRIENT_DIGESTOR||
            device.kind==BEGOTTEN_ORGAN_LATTICE)&&device.productionActive)
            device.productionMs=Math.max(0,device.productionMs-amount*1000L);
        if((device.kind==PENITENT_VOTIVE_ALTAR||
            device.kind==PENITENT_RELIQUARY||
            device.kind==PENITENT_CANTOR_DAIS||
            device.kind==PENITENT_CHOIR_RELAY||
            device.kind==TERROR_COLLECTOR||
            device.kind==PENITENT_WARD_SANCTUM)&&device.productionActive)
            device.productionMs=0; // sacred rite must begin its chant again
        if(device.hp==0){
            if(device.kind==CONVEYOR_LINE){device.routeItem="";device.routeMs=0;}
            if(device.kind==STEAM_BOILER)device.steamMs=0;
            if(device.kind==GENERATOR)device.electricMs=0;
            device.productionActive=false;
            device.productionRecipeId="";
            device.queuedRecipeId="";
            device.productionMs=0;
        }
        return true;
    }
    /** Scores current threats for each selectable spawn without secretly denying chosen risk. */
    public record SpawnRisk(int index,int score,String label){}
    public synchronized List<SpawnRisk> spawnRisks(int id){
        Player player=player(id);
        List<SpawnRisk> ranks=new ArrayList<>();
        for(int i=0;i<world.spawns.length;i++){
            double[] point=world.spawns[i];
            int risk=player==null?0:player.earlySpawnDeaths[i]*14;
            if(!free(point[0],point[1]))risk=1000;
            for(Player enemy:players.values()){
                if(enemy.id==id||enemy.dead||
                   factions.allied(player.accountKey,enemy.accountKey))continue;
                double distance=Math.hypot(enemy.x-point[0],enemy.y-point[1]);
                if(distance<10)risk+=Math.max(0,(int)(65-distance*5));
                if(distance<15&&clear(enemy.x,enemy.y,point[0],point[1]))risk+=30;
            }
            for(World.Device device:world.devices){
                if(device.hp<=0||device.kind!=TURRET||device.ownerId==id||
                   factions.allied(player==null?null:player.accountKey,device.ownerKey))continue;
                double distance=Math.hypot(device.x-point[0],device.y-point[1]);
                if(distance<7&&clear(device.x,device.y,point[0],point[1]))
                    risk+=Math.max(0,95-(int)(distance*8));
            }
            for(World.Npc npc:world.npcs){
                if(npc.hp<=0)continue;
                double distance=Math.hypot(npc.x-point[0],npc.y-point[1]);
                if(distance<4)risk+=Math.max(0,35-(int)(distance*7));
            }
            String label=risk>=1000?"BLOCKED":risk>=55?"DANGER":risk>=20?"CAUTION":"SAFE";
            ranks.add(new SpawnRisk(i,risk,label));
        }
        return List.copyOf(ranks);
    }
    /** Remaining mandatory death lockout, measured only by the host simulation clock. */
    public synchronized long respawnRemainingMs(int id){
        Player p=player(id);
        if(p==null||!p.dead)return 0;
        return Math.max(0,1200-(clock-p.respawnAt));
    }
    public synchronized boolean respawn(int id,int spawn){
        Player p=player(id);
        if(p==null||!p.dead||spawn<0||spawn>=world.spawns.length||clock-p.respawnAt<1200)return false;
        if(!free(world.spawns[spawn][0],world.spawns[spawn][1]))return false;
        p.x=world.spawns[spawn][0];p.y=world.spawns[spawn][1];
        p.lastSpawnIndex=spawn;p.lastSpawnAt=clock;
        p.spawnX=p.x;p.spawnY=p.y;
        p.protectedUntil=clock+5000+respawnRandom.nextInt(3001);
        p.ready=true;
        p.hp=100;p.sanity=75;p.dead=false;p.moveAt=clock;
        // No free weapons/ammo are magically returned after death.
        refreshVisibility(p);return true;
    }
    /** Seat changes are authoritative and never accept client coordinates. */
    public synchronized boolean enterVehicle(int id,int carIndex){
        Player p=players.get(id);
        if(p==null||p.dead||!vehicles.enter(id,p.x,p.y,carIndex))return false;
        Vehicles.State car=vehicles.state(carIndex);
        p.x=car.x();p.y=car.y();p.angle=car.heading();
        p.moveAt=clock;breakGrace(p);return true;
    }
    public synchronized boolean exitVehicle(int id){
        Player p=players.get(id);
        if(p==null)return false;
        double[] at=vehicles.exit(id);
        if(at==null)return false;
        p.x=at[0];p.y=at[1];p.moveAt=clock;refreshVisibility(p);
        return true;
    }
    public synchronized boolean driveVehicle(int id,double throttle,double steering){
        Player p=players.get(id);
        return p!=null&&!p.dead&&vehicles.input(id,throttle,steering);
    }
    /** Players must leave the cab and physically visit a fixed resource yard. */
    public synchronized boolean loadTruck(int playerId,int carId,int yardId){
        Player player=players.get(playerId);
        return player!=null&&!player.dead&&
            cargo.load(playerId,player.x,player.y,carId,yardId);
    }
    public synchronized boolean recoverTruckLoad(int playerId,int carId,int droppedId){
        Player player=players.get(playerId);
        return player!=null&&!player.dead&&
            cargo.recover(playerId,player.x,player.y,carId,droppedId);
    }
    /** Only a live authenticated owner beside their wilderness camp
     * may unload into carried staging stock. No city or remote transfer. */
    public synchronized boolean unloadTruck(int playerId,int carId){
        Player p=players.get(playerId);
        if(p==null||p.dead||p.accountKey==null||
           !p.accountKey.matches("acct_[a-f0-9]{64}")||
           vehicles.carDrivenBy(playerId)>=0)return false;
        for(World.Device camp:world.devices){
            if(camp.hp<=0||camp.kind!=CAMP_STAKE||
               !p.accountKey.equals(camp.ownerKey))continue;
            // Unload to fixed camp storage before temporary carried staging.
            for(World.Device d:world.devices)
                if(d.hp>0&&d.kind==STORAGE_CRATE&&
                   p.accountKey.equals(d.ownerKey)&&
                   Math.hypot(d.x-camp.x,d.y-camp.y)<=6&&
                   cargo.unloadToStorage(playerId,p.x,p.y,carId,
                       camp.x,camp.y,d.x,d.y,d.storage)){
                    breakGrace(p);
                    p.progression.reward(CharacterProgression.Skill.SCAVENGING,
                        18,clock,3000);
                    return true;
                }
            if(cargo.unload(playerId,p.x,p.y,carId,camp.x,camp.y,p.survival)){
                breakGrace(p);
                p.progression.reward(CharacterProgression.Skill.SCAVENGING,18,clock,3000);
                return true;
            }
        }
        return false;
    }
    /** Damage originates at host-verified attack events, never client HP reports. */
    public synchronized boolean damageTruck(int attackerId,int carId,int amount){
        Player attacker=players.get(attackerId);
        Vehicles.State car=vehicles.state(carId);
        if(attacker==null||attacker.dead||car==null||amount<1||amount>100||
           vehicles.carDrivenBy(attackerId)>=0||
           Math.hypot(attacker.x-car.x(),attacker.y-car.y())>8||
           !world.lineClear(attacker.x,attacker.y,car.x(),car.y()))return false;
        boolean hit=vehicles.damageCar(carId,amount);
        if(hit)cargo.advance(0);
        return hit;
    }
    /** A FIRE recipe requires an adjacent, living and allied physical workstation. */
    public synchronized boolean craftStation(int id,String recipe){
        Player p=players.get(id);
        if(p==null||p.dead||recipe==null||clock<p.craftReadyAt||
           vehicles.carDrivenBy(id)>=0)return false;
        SurvivalCrafting.Recipe record=SurvivalCrafting.recipes().get(recipe);
        if(record==null||record.station()!=SurvivalCrafting.Station.FIRE)return false;
        boolean nearFire=false;
        for(World.Device d:world.devices){
            if(d.hp<=0||d.kind!=FIELD_FIRE||
               !(p.accountKey.equals(d.ownerKey)||factions.allied(p.accountKey,d.ownerKey)))
                continue;
            if(Math.hypot(p.x-d.x,p.y-d.y)<=2.2&&clear(p.x,p.y,d.x,d.y)){
                nearFire=true;break;
            }
        }
        if(!nearFire||!p.survival.craft(recipe,SurvivalCrafting.Station.FIRE))return false;
        p.craftReadyAt=clock+Math.round(4000*p.progression.craftingMultiplier());
        breakGrace(p);
        p.progression.reward(CharacterProgression.Skill.ENGINEERING,9,clock,3500);
        return true;
    }
    /** Actual owned/allied workbench with unobstructed reach, inventory
     * debit, cooldown and XP, identical transaction discipline as fire. */
    public synchronized boolean craftWorkbench(int id,String recipe){
        Player p=players.get(id);
        if(p==null||p.dead||recipe==null||clock<p.craftReadyAt||
            vehicles.carDrivenBy(id)>=0)return false;
        SurvivalCrafting.Recipe record=SurvivalCrafting.recipes().get(recipe);
        if(record==null||record.station()!=SurvivalCrafting.Station.WORKBENCH)return false;
        boolean reachable=false;
        for(World.Device d:world.devices){
            if(d.hp<=0||d.kind!=FIELD_BENCH||
               !(p.accountKey.equals(d.ownerKey)||factions.allied(p.accountKey,d.ownerKey)))
                continue;
            if(Math.hypot(p.x-d.x,p.y-d.y)<=2.2&&
                world.lineClear(p.x,p.y,d.x,d.y)){
                reachable=true;break;
            }
        }
        if(!reachable||!p.survival.craft(recipe,SurvivalCrafting.Station.WORKBENCH))
            return false;
        p.craftReadyAt=clock+Math.round(4000*p.progression.craftingMultiplier());
        breakGrace(p);
        p.progression.reward(CharacterProgression.Skill.ENGINEERING,12,clock,3500);
        return true;
    }
    /** Food and clean water are physically carried items, consumed only by
     * the authoritative actor. Using supplies cancels nonoffensive spawn grace. */
    public synchronized boolean consumeSurvival(int id,SurvivalCrafting.Item item){
        Player p=players.get(id);
        if(p==null||p.dead||item==null||!p.survival.consume(item))return false;
        breakGrace(p);
        return true;
    }
    /** HAND recipes only: server verifies item costs, cooldown and authenticated player inventory. */
    public synchronized boolean craftHand(int id,String recipe){
        Player p=players.get(id);
        if(p==null||p.dead||recipe==null||clock<p.craftReadyAt||
           vehicles.carDrivenBy(id)>=0)return false;
        SurvivalCrafting.Recipe record=SurvivalCrafting.recipes().get(recipe);
        if(record==null||record.station()!=SurvivalCrafting.Station.HAND||
           !p.survival.craft(recipe,SurvivalCrafting.Station.HAND))return false;
        p.craftReadyAt=clock+Math.round(4000*p.progression.craftingMultiplier());
        breakGrace(p);
        p.progression.reward(CharacterProgression.Skill.ENGINEERING,9,clock,3500);
        return true;
    }
    /** Drives health regeneration, devices and host-owned hostile AI. */
    public synchronized void advance(long ms){
        if(ms<=0||ms>1000)throw new IllegalArgumentException("bounded fixed tick required");
        clock+=ms;
        thrown.advance(ms);
        arsenal.advance(ms);
        vehicles.advance(ms);
        cargo.advance(ms);
        // Server-owned Lost spawning: fixed ruin ownership, local caps,
        // lifetime/death tracking and no appearance within player sight.
        List<double[]> observers=new ArrayList<>();
        for(Player p:players.values())if(!p.dead)observers.add(new double[]{p.x,p.y});
        world.lostSites.advance(clock,observers);
        for(Player p:players.values()){
            int carId=vehicles.carDrivenBy(p.id);
            if(carId>=0){
                Vehicles.State car=vehicles.state(carId);
                p.x=car.x();p.y=car.y();p.angle=car.heading();p.moveAt=clock;
                if(p.dead||car.hp()<=0)vehicles.disconnect(p.id);
            }
            if(p.dead)continue;
            p.progression.aliveTick(ms);
            // Genuine alive-time survival XP, not client-claimed AFK "challenges".
            if(p.progression.view().aliveMs()%300_000L<ms)
                p.progression.reward(CharacterProgression.Skill.SURVIVAL,
                    20,clock,300_000);
            p.survival.advance(ms);
            SurvivalCrafting.Status vitals=p.survival.status();
            boolean starving=vitals.hunger()==0;
            boolean dehydrated=vitals.hydration()==0;
            // Empty need meters must matter. Every five seconds of simulated
            // host time, hunger removes 2 HP and thirst 3 HP. Both stack.
            // Tactical shield bubbles cannot absorb biological deprivation.
            if((starving||dehydrated)&&clock%5000<ms)
                applyDeprivationDamage(p,(starving?2:0)+(dehydrated?3:0));
            // Never passively heal the damage away while either need is empty.
            // Food/water consumption resumes normal recovery after combat rest.
            if(!p.dead&&!starving&&!dehydrated&&clock-p.lastHurt>=5000&&
               clock%1000<ms)
                p.hp=Math.min(100,p.hp+2);
            refreshVisibility(p);
        }
        advanceFactoryUtilities(ms);
        for(World.Device d:world.devices){
            if(d.hp<=0)continue;
            d.cooldown=Math.max(0,d.cooldown-ms/1000.0);
            if(d.kind==HEALTH_STATION&&clock%500<ms)for(Player p:players.values()){
                if(!p.dead&&Math.hypot(p.x-d.x,p.y-d.y)<2){
                    p.hp=Math.min(100,p.hp+3);p.sanity=Math.min(100,p.sanity+2);
                }
            }
            if(d.kind==REVIVE_STATION)for(Player p:players.values()){
                if(p.dead&&clock-p.respawnAt>=3000&&Math.hypot(p.x-d.x,p.y-d.y)<2){
                    p.hp=35;p.sanity=40;p.dead=false;p.moveAt=clock;refreshVisibility(p);
                }
            }
            if(d.kind==TURRET&&d.cooldown<=0){
                Player opponent=null;double nearest=6;
                for(Player p:players.values()){
                    if(p.dead||p.id==d.ownerId||p.accountKey.equals(d.ownerKey)||
                       factions.allied(p.accountKey,d.ownerKey)||
                        Math.hypot(p.x-d.x,p.y-d.y)>=nearest||!clear(d.x,d.y,p.x,p.y))continue;
                    // Prototype turret is a public hazard; future build owner/team should be recorded.
                    opponent=p;nearest=Math.hypot(p.x-d.x,p.y-d.y);
                }
                World.Npc npc=null;
                if(factions.faction(d.ownerKey)!=CharacterRoster.Faction.THE_BEGOTTEN)
                    for(World.Npc n:world.npcs){
                        double range=Math.hypot(n.x-d.x,n.y-d.y);
                        if(n.hp>0&&range<nearest&&clear(d.x,d.y,n.x,n.y)){
                            npc=n;nearest=range;opponent=null;
                        }
                    }
                if(npc!=null){npc.hp-=18;d.cooldown=.45;}
                else if(opponent!=null){damage(opponent,12,2);d.cooldown=.45;}
            }
        }
        for(World.Npc n:world.npcs){
            if(n.hp<=0)continue;
            n.cooldown=Math.max(0,n.cooldown-ms/1000.0);
            Player nearest=null;double best=10;
            for(Player p:players.values()){
                double dist=Math.hypot(p.x-n.x,p.y-n.y);
                if(!p.dead&&factions.faction(p.accountKey)!=CharacterRoster.Faction.THE_BEGOTTEN&&
                   p.visibility>.10&&dist<best&&clear(n.x,n.y,p.x,p.y)){
                    nearest=p;best=dist;
                }
            }
            // Loaded, moving trucks are audible beyond visual aggro range.
            // The host owns vehicle state, so clients cannot suppress noise.
            // Hearing does not require line of sight; a hostile follows the
            // truck's current position and still obeys world collision.
            for(Vehicles.State truck:vehicles.states()){
                if(truck.driverId()<0||Math.abs(truck.speed())<.35||
                   vehicles.cargo(truck.id()).resourceType()==null)continue;
                Player driver=players.get(truck.driverId());
                if(driver==null||driver.dead||
                   factions.faction(driver.accountKey)==CharacterRoster.Faction.THE_BEGOTTEN)
                    continue;
                double distance=Math.hypot(truck.x()-n.x,truck.y()-n.y);
                if(distance<=26&&(nearest==null||distance<best)){
                    nearest=driver;best=distance;
                }
            }
            if(nearest==null)continue;
            if(best>.85){
                double nx=n.x+(nearest.x-n.x)/best*.55*arsenal.speed(n)*ms/1000;
                double ny=n.y+(nearest.y-n.y)/best*.55*arsenal.speed(n)*ms/1000;
                if(free(nx,n.y))n.x=nx;if(free(n.x,ny))n.y=ny;
            } else if(n.cooldown<=0){
                damage(nearest,8,8);n.cooldown=1.2;
            }
        }
    }
}
