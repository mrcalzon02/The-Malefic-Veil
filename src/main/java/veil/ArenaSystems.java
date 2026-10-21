package veil;

import java.util.*;

/**
 * Host-owned arena state. All mutations are serialized by synchronized methods.
 * Millisecond game clock is explicitly advanced by host tick, so tests are deterministic.
 */
public final class ArenaSystems {
    public final World world;
    public final Vehicles vehicles;
    public final RaidProtection raidProtection=new RaidProtection(Long.getLong("veil.offlineGraceMinutes",30L));
    private final Map<Integer,Player> players=new LinkedHashMap<>();
    private final Map<String,Profile> accountProfiles=new LinkedHashMap<>();
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
    public static final int TURRET=0, HEALTH_STATION=1, REVIVE_STATION=2, BARRICADE=3;
    public static final class Player {
        public final int id;
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
        }
    }
    public ArenaSystems(long seed){world=new World(seed);vehicles=new Vehicles(world);}
    public synchronized Player join(int id){
        if(players.containsKey(id))throw new IllegalArgumentException("duplicate peer");
        Player p=new Player(id,world.spawns[0][0],world.spawns[0][1],clock);
        players.put(id,p);
        // Disposable guest peers have no durable property claim; only authenticated accounts do.
        refreshVisibility(p);return p;
    }
    public synchronized void leave(int id){
        vehicles.disconnect(id);
        Player departing=players.remove(id);
        if(departing!=null){
            if(departing.accountKey.startsWith("acct_"))
                accountProfiles.put(departing.accountKey,savePlayer(departing));
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
        raidProtection.login(key,System.currentTimeMillis());
        for(World.Device d:world.devices)if(d.ownerId==id&&prior.equals(d.ownerKey))
            d.ownerKey=key;
        Profile saved=accountProfiles.get(key);
        if(saved!=null)applyProfile(p,saved);
        return true;
    }
    public synchronized Collection<Player> roster(){return List.copyOf(players.values());}
    public synchronized Player player(int id){return players.get(id);}
    public synchronized long time(){return clock;}
    public synchronized void choose(int id,int classId){
        Player p=player(id);if(p!=null&&classId>=0&&classId<4)p.classId=classId;
    }
    public static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
    public static double opacity(double visibility){return clamp(visibility,0,1);}
    public static double transparencyPercent(double visibility){return (1-opacity(visibility))*100.0;}
    private boolean free(double x,double y){
        if(!world.canWalk(x,y))return false;
        for(Vehicles.State car:vehicles.states())
            if(car.hp()>0&&Math.hypot(car.x()-x,car.y()-y)<.90)return false;
        for(World.Device d:world.devices)
            if(d.hp>0&&d.kind==BARRICADE&&Math.hypot(d.x-x,d.y-y)<.52)return false;
        return true;
    }
    private boolean clear(double x,double y,double tx,double ty){
        if(!world.lineClear(x,y,tx,ty))return false;
        double len=Math.hypot(tx-x,ty-y);
        for(World.Device d:world.devices){
            if(d.hp<=0||d.kind!=BARRICADE)continue;
            double t=clamp(((d.x-x)*(tx-x)+(d.y-y)*(ty-y))/(len*len+1e-9),0,1);
            if(Math.hypot(x+(tx-x)*t-d.x,y+(ty-y)*t-d.y)<.36)return false;
        }
        return true;
    }
    /** Crouch/sprint are intents; host caps actual movement to reject teleport attempts. */
    public synchronized boolean move(int id,double x,double y,double angle,int flags){
        Player p=player(id);
        if(p==null||p.dead||vehicles.carDrivenBy(id)>=0||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(angle)||!free(x,y))return false;
        long elapsed=Math.max(0,clock-p.moveAt);
        double max=(flags&1)!=0?1.20:(flags&2)!=0?4.30:2.85;
        double permitted=.33+Math.min(elapsed,500)*max/1000;
        if(Math.hypot(x-p.x,y-p.y)>permitted)return false;
        p.x=x;p.y=y;p.angle=angle;p.flags=flags&3;p.moveAt=clock;
        if(clock<p.protectedUntil&&Math.hypot(x-p.spawnX,y-p.spawnY)>.85)p.protectedUntil=0;
        refreshVisibility(p);return true;
    }
    public synchronized void refreshVisibility(Player p){
        boolean avenue=(int)p.x%12<3||(int)p.y%12<3;
        double v=.36+(avenue?.25:-.14);
        if((p.flags&1)!=0)v-=.28;
        if((p.flags&2)!=0)v+=.29;
        if(clock-p.shotAt<250)v+=.38;
        if(p.classId==2&&p.shadeUntil>clock)v-=.72;
        p.visibility=opacity(v);
    }
    public synchronized void damage(Player p,int hp,int sanity) {
        if(p==null||p.dead||clock<p.protectedUntil)return;
        p.hp=Math.max(0,p.hp-Math.max(0,hp));
        p.sanity=Math.max(0,p.sanity-Math.max(0,sanity));
        p.lastHurt=clock;
        if(p.hp==0){
            p.dead=true;p.respawnAt=clock;p.protectedUntil=0;
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
    public synchronized boolean useMedkit(int id){
        Player p=player(id);
        if(p==null||p.dead||p.medkits<=0||p.hp>=100)return false;
        p.medkits--;p.hp=Math.min(100,p.hp+55);return true;
    }
    public synchronized boolean reload(int id,int weapon){
        Player p=player(id);if(p==null||p.dead)return false;
        if(weapon==0){
            int rounds=Math.min(12-p.pistolClip,p.pistolReserve);
            if(rounds<=0)return false;
            p.pistolReserve-=rounds;p.pistolClip+=rounds;return true;
        }
        if(weapon==1){
            int rounds=Math.min(6-p.shellClip,p.shellReserve);
            if(rounds<=0)return false;
            p.shellReserve-=rounds;p.shellClip+=rounds;return true;
        }
        return false;
    }
    public synchronized boolean ability(int id){
        Player p=player(id);
        if(p==null||p.dead||clock-p.abilityAt<18000)return false;
        p.abilityAt=clock;
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
        p.protectedUntil=0; // offensive action immediately ends spawn invulnerability.
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
            if(n.hp<=0)continue;
            double range=Math.hypot(n.x-p.x,n.y-p.y);
            if(range>=nearest||range<.01||!clear(p.x,p.y,n.x,n.y))continue;
            double diff=Math.atan2(Math.sin(Math.atan2(n.y-p.y,n.x-p.x)-p.angle),
                                    Math.cos(Math.atan2(n.y-p.y,n.x-p.x)-p.angle));
            if(Math.abs(diff)<=Math.atan2(.4,range)){nearest=range;target=n;}
        }
        for(int i=0;i<world.devices.size();i++){
            World.Device d=world.devices.get(i);
            if(d.hp<=0||p.id==d.ownerId||p.accountKey.equals(d.ownerKey))continue;
            double range=Math.hypot(d.x-p.x,d.y-p.y);
            if(range>=nearest||range<.01||!world.lineClear(p.x,p.y,d.x,d.y))continue;
            double diff=Math.atan2(Math.sin(Math.atan2(d.y-p.y,d.x-p.x)-p.angle),
                Math.cos(Math.atan2(d.y-p.y,d.x-p.x)-p.angle));
            if(Math.abs(diff)<=Math.atan2(.43,range)){nearest=range;target=d;}
        }
        int amount=weapon==0?25:Math.max(18,70-(int)(nearest*8));
        if(target instanceof Player victim){
            boolean wasAlive=!victim.dead;damage(victim,amount,weapon==1?6:2);
            if(wasAlive&&victim.dead){p.frags++;p.cash+=25;}
        } else if(target instanceof World.Device device){
            int index=world.devices.indexOf(device);
            if(!damageDevice(id,index,amount,System.currentTimeMillis()) &&
               raidProtection.protectedOffline(device.ownerKey,System.currentTimeMillis()))
                shieldedShot.put(id,index);
        } else if(target instanceof World.Npc npc){
            npc.hp-=amount;
            if(npc.hp<=0){p.frags++;p.cash+=25;}
        }
        return true;
    }
    public record BuildReview(boolean allowed,String reason,int cashCost,int suppliesCost,double x,double y){}
    /** Returns a concrete rejection without mutating inventories. */
    public synchronized BuildReview reviewBuild(int id,int kind){
        Player p=player(id);
        if(p==null||p.dead)return new BuildReview(false,"DEAD_OR_MISSING",0,0,0,0);
        if(vehicles.carDrivenBy(id)>=0)return new BuildReview(false,"IN_VEHICLE",0,0,0,0);
        if(kind<0||kind>3)return new BuildReview(false,"UNKNOWN_STRUCTURE",0,0,0,0);
        int price=kind==0?(p.classId==0?60:100):kind==1?75:kind==2?120:45;
        int supplies=kind==0?3:kind==1?1:kind==2?2:1;
        double x=p.x+Math.cos(p.angle)*1.1,y=p.y+Math.sin(p.angle)*1.1;
        if(world.devices.size()>=100)return new BuildReview(false,"BUILD_LIMIT",price,supplies,x,y);
        if(p.cash<price||p.scrap<supplies)return new BuildReview(false,"RESOURCES",price,supplies,x,y);
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
        p.cash-=review.cashCost();p.scrap-=review.suppliesCost();
        World.Device device=new World.Device(review.x(),review.y(),kind);
        device.ownerId=p.id;
        device.ownerKey=p.accountKey;
        world.devices.add(device);
        return new BuildOutcome(true,review,world.devices.size()-1);
    }
    public synchronized boolean build(int id,int kind){return buildOutcome(id,kind).built();}
    /** All base PvP damage passes through this gate, including future explosives. */
    public synchronized boolean damageDevice(int attackerId,int deviceIndex,int amount,long utcMillis){
        Player attacker=players.get(attackerId);
        if(attacker==null||attacker.dead||deviceIndex<0||deviceIndex>=world.devices.size()||
           amount<=0||amount>250)return false;
        World.Device device=world.devices.get(deviceIndex);
        if(device.hp<=0||attacker.accountKey.equals(device.ownerKey))return false;
        if(device.ownerKey!=null&&device.ownerKey.startsWith("acct_")&&
           raidProtection.protectedOffline(device.ownerKey,utcMillis))return false;
        device.hp=Math.max(0,device.hp-amount);
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
                if(enemy.id==id||enemy.dead)continue;
                double distance=Math.hypot(enemy.x-point[0],enemy.y-point[1]);
                if(distance<10)risk+=Math.max(0,(int)(65-distance*5));
                if(distance<15&&clear(enemy.x,enemy.y,point[0],point[1]))risk+=30;
            }
            for(World.Device device:world.devices){
                if(device.hp<=0||device.kind!=TURRET||device.ownerId==id)continue;
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
    public synchronized boolean respawn(int id,int spawn){
        Player p=player(id);
        if(p==null||!p.dead||spawn<0||spawn>=world.spawns.length||clock-p.respawnAt<1200)return false;
        if(!free(world.spawns[spawn][0],world.spawns[spawn][1]))return false;
        p.x=world.spawns[spawn][0];p.y=world.spawns[spawn][1];
        p.lastSpawnIndex=spawn;p.lastSpawnAt=clock;
        p.spawnX=p.x;p.spawnY=p.y;p.protectedUntil=clock+1200;
        p.hp=100;p.sanity=75;p.dead=false;p.moveAt=clock;
        p.pistolClip=12;p.shellClip=6;
        p.pistolReserve=Math.max(24,p.pistolReserve);p.shellReserve=Math.max(12,p.shellReserve);
        refreshVisibility(p);return true;
    }
    /** Seat changes are authoritative and never accept client coordinates. */
    public synchronized boolean enterVehicle(int id,int carIndex){
        Player p=players.get(id);
        if(p==null||p.dead||!vehicles.enter(id,p.x,p.y,carIndex))return false;
        Vehicles.State car=vehicles.state(carIndex);
        p.x=car.x();p.y=car.y();p.angle=car.heading();
        p.moveAt=clock;p.protectedUntil=0;return true;
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
    /** Drives health regeneration, devices and host-owned hostile AI. */
    public synchronized void advance(long ms){
        if(ms<=0||ms>1000)throw new IllegalArgumentException("bounded fixed tick required");
        clock+=ms;
        vehicles.advance(ms);
        for(Player p:players.values()){
            int carId=vehicles.carDrivenBy(p.id);
            if(carId>=0){
                Vehicles.State car=vehicles.state(carId);
                p.x=car.x();p.y=car.y();p.angle=car.heading();p.moveAt=clock;
                if(p.dead||car.hp()<=0)vehicles.disconnect(p.id);
            }
            if(p.dead)continue;
            if(clock-p.lastHurt>=5000&&clock%1000<ms)
                p.hp=Math.min(100,p.hp+2);
            refreshVisibility(p);
        }
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
                        Math.hypot(p.x-d.x,p.y-d.y)>=nearest||!clear(d.x,d.y,p.x,p.y))continue;
                    // Prototype turret is a public hazard; future build owner/team should be recorded.
                    opponent=p;nearest=Math.hypot(p.x-d.x,p.y-d.y);
                }
                World.Npc npc=null;
                for(World.Npc n:world.npcs){
                    double range=Math.hypot(n.x-d.x,n.y-d.y);
                    if(n.hp>0&&range<nearest&&clear(d.x,d.y,n.x,n.y)){npc=n;nearest=range;opponent=null;}
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
                if(!p.dead&&p.visibility>.10&&dist<best&&clear(n.x,n.y,p.x,p.y)){
                    nearest=p;best=dist;
                }
            }
            if(nearest==null)continue;
            if(best>.85){
                double nx=n.x+(nearest.x-n.x)/best*.55*ms/1000;
                double ny=n.y+(nearest.y-n.y)/best*.55*ms/1000;
                if(free(nx,n.y))n.x=nx;if(free(n.x,ny))n.y=ny;
            } else if(n.cooldown<=0){
                damage(nearest,8,8);n.cooldown=1.2;
            }
        }
    }
}
