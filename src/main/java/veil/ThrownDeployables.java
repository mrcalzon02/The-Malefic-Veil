package veil;

import java.util.*;

/**
 * Thrown field equipment, deliberately NOT settlement construction or World.Device.
 * Host owns the craft/debit, launch, flight, combat, decay and despawn clock.
 */
public final class ThrownDeployables {
    public static final long LIFETIME_MS=3_600_000L;
    public static final int MAX_ACTIVE=84, MAX_OWNED=12;
    public static final long IMPACT_MS=150, DEPLOY_MS=1500;
    public static final long ACTIVATION_MS=IMPACT_MS+DEPLOY_MS;
    public static final long DESTROY_MS=800;
    public static final double SHIELD_RADIUS=2.4, MIASMA_RADIUS=3.0;
    public enum State { PACKED, THROWN, IMPACT, DEPLOYING, ACTIVE, OPERATING, DAMAGED, EXPIRING, DESTROYED }
    public enum Role { WALL, BARRICADE, TURRET, HEALTH, AMMO, MIASMA, SHIELD }
    public enum Faction { UNRAVELED, PENITENT, BEGOTTEN }
    public record Blueprint(int index,Role role,Faction faction,String id,String name,
                            SurvivalCrafting.Item item,SurvivalCrafting.Station station,
                            Map<SurvivalCrafting.Item,Integer> ingredients,int hp,double radius) {
        public Blueprint {ingredients=Map.copyOf(ingredients);}
        public String mesh(){return "thrown-"+id;}
    }
    public record View(int id,int blueprint,double x,double y,double z,double heading,
                       int hp,long remainingMs,boolean flying,long ageMs,long operationAgeMs,
                       long damageAgeMs,double aimYaw,double aimPitch,int ammoStock,boolean tracking) {
        public State state(){
            if(hp<=0)return State.DESTROYED;
            if(flying)return State.THROWN;
            if(ageMs<IMPACT_MS)return State.IMPACT;
            if(ageMs<IMPACT_MS+DEPLOY_MS)return State.DEPLOYING;
            if(remainingMs<5000)return State.EXPIRING;
            if(damageAgeMs>=0&&damageAgeMs<450)return State.DAMAGED;
            if(operationAgeMs>=0&&operationAgeMs<650)return State.OPERATING;
            return State.ACTIVE;
        }
    }
    public record Burst(double x,double y,int faction) {}
    public record Result(boolean ok,String reason) {}
    private static final List<Blueprint> CATALOG;
    static {
        List<Blueprint> entries=new ArrayList<>();
        for(Faction faction:Faction.values())for(Role role:Role.values()){
            String id=faction.name().toLowerCase(Locale.ROOT)+"-"+role.name().toLowerCase(Locale.ROOT);
            SurvivalCrafting.Item item=SurvivalCrafting.Item.valueOf(
                "DEPLOY_"+faction.name()+"_"+role.name());
            Map<SurvivalCrafting.Item,Integer> cost=new EnumMap<>(SurvivalCrafting.Item.class);
            SurvivalCrafting.Station station;
            switch(faction){
                case UNRAVELED -> {
                    station=SurvivalCrafting.Station.WORKBENCH;
                    cost.put(SurvivalCrafting.Item.MACHINE_PARTS,1);
                    cost.put(SurvivalCrafting.Item.METAL_STOCK,role==Role.SHIELD||role==Role.TURRET?2:1);
                    cost.put(SurvivalCrafting.Item.CHARCOAL,role==Role.MIASMA?3:1);
                }
                case PENITENT -> {
                    station=SurvivalCrafting.Station.FIRE;
                    cost.put(SurvivalCrafting.Item.CLOTH_ROLL,role==Role.WALL?2:1);
                    cost.put(SurvivalCrafting.Item.HERBAL_BUNDLE,role==Role.HEALTH||role==Role.SHIELD?3:2);
                    cost.put(SurvivalCrafting.Item.FIRED_BRICKS,role==Role.WALL||role==Role.BARRICADE?3:1);
                }
                case BEGOTTEN -> {
                    station=SurvivalCrafting.Station.HAND; // Cultivate/assemble living seed packs.
                    cost.put(SurvivalCrafting.Item.COMPOST,role==Role.MIASMA||role==Role.SHIELD?4:3);
                    cost.put(SurvivalCrafting.Item.EDIBLES,role==Role.HEALTH||role==Role.AMMO?4:2);
                    cost.put(SurvivalCrafting.Item.MEDICINAL_HERBS,role==Role.MIASMA?4:2);
                }
                default -> throw new IllegalStateException("Unknown faction");
            }
            int hp=switch(role){
                case WALL -> 260;case BARRICADE -> 160;case TURRET -> 110;
                case HEALTH,AMMO,MIASMA -> 90;case SHIELD -> 170;
            };
            hp+=switch(faction){case UNRAVELED -> 30;case PENITENT -> 0;case BEGOTTEN -> 20;};
            double radius=role==Role.WALL?.70:role==Role.BARRICADE?.50:.33;
            entries.add(new Blueprint(entries.size(),role,faction,id,
                faction.name()+" "+role.name(),item,station,cost,hp,radius));
        }
        CATALOG=List.copyOf(entries);
    }
    public static List<Blueprint> catalog(){return CATALOG;}
    public static Blueprint blueprint(int index){return index>=0&&index<CATALOG.size()?CATALOG.get(index):null;}
    public static Faction faction(CharacterRoster.Faction character){
        if(character==null)return null;
        return switch(character){
            case THE_UNRAVELED -> Faction.UNRAVELED;
            case THE_PENITENT -> Faction.PENITENT;
            case THE_BEGOTTEN -> Faction.BEGOTTEN;
        };
    }
    private static final class Entity {
        int id,hp,ownerId,ammoStock;
        String ownerKey;
        Blueprint blueprint;
        double x,y,z,fromX,fromY,toX,toY,heading;
        long launchedAt,landedAt=-1,expiresAt,nextPulse,operatedAt=-1,damagedAt=-1,destroyedAt=-1;
        double aimYaw,aimPitch;
        boolean flying=true,tracking;
        ArenaSystems.Player aimedPlayer;World.Npc aimedNpc;
    }
    private final ArenaSystems host;
    private final LinkedHashMap<Integer,Entity> entities=new LinkedHashMap<>();
    private final ArrayList<Burst> bursts=new ArrayList<>();
    private final ArrayList<Integer> removed=new ArrayList<>();
    private int nextId=1;
    public ThrownDeployables(ArenaSystems host){this.host=Objects.requireNonNull(host);}
    private Blueprint selected(int playerId,int role){
        ArenaSystems.Player p=host.player(playerId);
        if(p==null||p.dead||role<0||role>=Role.values().length)return null;
        Faction f=faction(host.factions.faction(p.accountKey));
        return f==null?null:CATALOG.get(f.ordinal()*Role.values().length+role);
    }
    private boolean station(ArenaSystems.Player p,SurvivalCrafting.Station station){
        if(station==SurvivalCrafting.Station.HAND)return true;
        int kind=station==SurvivalCrafting.Station.FIRE?
            ArenaSystems.FIELD_FIRE:ArenaSystems.FIELD_BENCH;
        for(World.Device d:host.world.devices)
            if(d.hp>0&&d.kind==kind&&
               (p.accountKey.equals(d.ownerKey)||host.factions.allied(p.accountKey,d.ownerKey))&&
               Math.hypot(p.x-d.x,p.y-d.y)<2.2&&host.world.lineClear(p.x,p.y,d.x,d.y))
                return true;
        return false;
    }
    public Result craft(int playerId,int role){
        synchronized(host){
            ArenaSystems.Player p=host.player(playerId);
            Blueprint b=selected(playerId,role);
            if(b==null)return new Result(false,"FACTION_REQUIRED");
            if(host.vehicles.carDrivenBy(playerId)>=0)return new Result(false,"IN_VEHICLE");
            if(host.time()<p.craftReadyAt)return new Result(false,"CRAFT_COOLDOWN");
            if(!station(p,b.station()))return new Result(false,"NEED_"+b.station());
            if(!p.survival.craftEquipment(b.ingredients(),b.item(),1))
                return new Result(false,"MATERIALS_OR_CAPACITY");
            p.craftReadyAt=host.time()+4000;
            p.protectedUntil=0;
            p.progression.reward(CharacterProgression.Skill.ENGINEERING,12,host.time(),3500);
            return new Result(true,"CRAFTED_"+b.id());
        }
    }
    private boolean validLanding(double x,double y){
        if(!host.world.canWalk(x,y))return false;
        for(Entity e:entities.values())
            if(e.hp>0&&!e.flying&&Math.hypot(x-e.x,y-e.y)<e.blueprint.radius()+.42)
                return false;
        for(World.Device d:host.world.devices)
            if(d.hp>0&&Math.hypot(x-d.x,y-d.y)<.65)return false;
        return true;
    }
    /** Predict host-authorized impact; no world-structure build/plot test is involved. */
    public Result throwKit(int playerId,int role){
        synchronized(host){
            ArenaSystems.Player p=host.player(playerId);
            Blueprint b=selected(playerId,role);
            if(b==null)return new Result(false,"FACTION_REQUIRED");
            if(host.vehicles.carDrivenBy(playerId)>=0)return new Result(false,"IN_VEHICLE");
            if(entities.size()>=MAX_ACTIVE)return new Result(false,"FIELD_CAP");
            long owned=entities.values().stream().filter(e->e.ownerKey.equals(p.accountKey)).count();
            if(owned>=MAX_OWNED)return new Result(false,"PERSONAL_FIELD_CAP");
            if(p.survival.count(b.item())==0)return new Result(false,"NO_CRAFTED_KIT");
            if(!Double.isFinite(p.x)||!Double.isFinite(p.y)||!Double.isFinite(p.angle))
                return new Result(false,"INVALID_THROW");
            double last=0;
            for(double step=.35;step<=3.6;step+=.15){
                double x=p.x+Math.cos(p.angle)*step,y=p.y+Math.sin(p.angle)*step;
                if(!validLanding(x,y)||!host.world.lineClear(p.x,p.y,x,y))break;
                last=step;
            }
            if(last<1.0)return new Result(false,"NO_LANDING_SPACE");
            if(!p.survival.debit(b.item(),1))return new Result(false,"NO_CRAFTED_KIT");
            Entity e=new Entity();e.id=nextId++;e.blueprint=b;
            e.ownerId=playerId;e.ownerKey=p.accountKey;
            e.fromX=p.x;e.fromY=p.y;e.x=p.x;e.y=p.y;e.z=.9;
            e.toX=p.x+Math.cos(p.angle)*last;e.toY=p.y+Math.sin(p.angle)*last;
            e.heading=p.angle+(b.role()==Role.HEALTH||b.role()==Role.AMMO?-Math.PI/2:Math.PI/2);e.hp=b.hp();e.launchedAt=host.time();
            if(b.role()==Role.AMMO)e.ammoStock=switch(b.faction()){
                case UNRAVELED -> 300;case PENITENT -> 240;case BEGOTTEN -> 180;};
            entities.put(e.id,e);p.protectedUntil=0;
            return new Result(true,"THROWN_"+b.id());
        }
    }
    public List<View> views(){
        synchronized(host){
            List<View> result=new ArrayList<>();
            for(Entity e:entities.values())
                result.add(new View(e.id,e.blueprint.index(),e.x,e.y,e.z,e.heading,
                    e.hp,e.flying?LIFETIME_MS:Math.max(0,e.expiresAt-host.time()),e.flying,
                    host.time()-(e.flying?e.launchedAt:e.landedAt),
                    e.operatedAt<0?-1:host.time()-e.operatedAt,
                    e.destroyedAt>=0?host.time()-e.destroyedAt:e.damagedAt<0?-1:host.time()-e.damagedAt,e.aimYaw,e.aimPitch,e.ammoStock,e.tracking));
            return List.copyOf(result);
        }
    }
    public List<Burst> drainBursts(){
        synchronized(host){List<Burst> result=List.copyOf(bursts);bursts.clear();return result;}
    }
    public List<Integer> drainRemoved(){
        synchronized(host){List<Integer> result=List.copyOf(removed);removed.clear();return result;}
    }
    private boolean active(Entity e){
        return e.hp>0&&!e.flying&&host.time()-e.landedAt>=ACTIVATION_MS;
    }
    /** Cover and field effects activate only after the visible impact/unfold sequence. */
    private static boolean coverContains(Entity e,double x,double y,double padding){
        double dx=x-e.x,dy=y-e.y,c=Math.cos(e.heading),s=Math.sin(e.heading);
        return Math.abs(dx*c+dy*s)<1.10+padding&&Math.abs(-dx*s+dy*c)<.46+padding;
    }
    public boolean occupied(double x,double y){
        synchronized(host){
            for(Entity e:entities.values())
                if(active(e)&&(e.blueprint.role()==Role.WALL||
                    e.blueprint.role()==Role.BARRICADE)&&
                    coverContains(e,x,y,.19))return true;
            return false;
        }
    }
    public boolean blocksLine(double x,double y,double tx,double ty){
        synchronized(host){
            double dx=tx-x,dy=ty-y,len=dx*dx+dy*dy;
            if(len<.0001)return false;
            for(Entity e:entities.values()){
                if(!active(e)||
                   (e.blueprint.role()!=Role.WALL&&e.blueprint.role()!=Role.BARRICADE))continue;
                double c=Math.cos(e.heading),s=Math.sin(e.heading);
                double[] origin={(x-e.x)*c+(y-e.y)*s,-(x-e.x)*s+(y-e.y)*c};
                double[] direction={dx*c+dy*s,-dx*s+dy*c},half={1.10,.46};
                double enter=.04,exit=.96;
                for(int axis=0;axis<2;axis++){
                    if(Math.abs(direction[axis])<1e-10){if(Math.abs(origin[axis])>half[axis]){enter=1;break;}}
                    else{double a=(-half[axis]-origin[axis])/direction[axis],b=(half[axis]-origin[axis])/direction[axis];
                        enter=Math.max(enter,Math.min(a,b));exit=Math.min(exit,Math.max(a,b));}
                }
                if(enter<exit)return true;
            }
            return false;
        }
    }
    private boolean ally(Entity e,ArenaSystems.Player p){
        return e.ownerKey.equals(p.accountKey)||host.factions.allied(e.ownerKey,p.accountKey);
    }
    public double moveFactor(ArenaSystems.Player p){
        synchronized(host){
            double factor=1;
            for(Entity e:entities.values())
                if(active(e)&&!ally(e,p)&&
                   e.blueprint.role()==Role.BARRICADE&&Math.hypot(e.x-p.x,e.y-p.y)<2.0)
                    factor=Math.min(factor,switch(e.blueprint.faction()){
                        case UNRAVELED -> .65;case PENITENT -> .75;case BEGOTTEN -> .48;});
            return factor;
        }
    }
    public double visibilityFactor(ArenaSystems.Player p){
        synchronized(host){
            for(Entity e:entities.values())
                if(active(e)&&!ally(e,p)&&e.blueprint.role()==Role.MIASMA&&
                   e.blueprint.faction()==Faction.UNRAVELED&&Math.hypot(e.x-p.x,e.y-p.y)<3)
                    return .45;
            return 1;
        }
    }
    public int absorb(ArenaSystems.Player p,int incoming){
        synchronized(host){
            if(incoming<=0)return 0;
            for(Entity e:entities.values()){
                if(!active(e)||e.blueprint.role()!=Role.SHIELD||!ally(e,p)||
                   Math.hypot(e.x-p.x,e.y-p.y)>SHIELD_RADIUS)continue;
                int saved=Math.min(e.hp,Math.max(1,(int)Math.ceil(incoming*
                    switch(e.blueprint.faction()){case UNRAVELED->.80;case PENITENT->.65;case BEGOTTEN->.55;})));
                e.hp-=saved;e.damagedAt=host.time();if(e.hp<=0)destroy(e);return saved;
            }
            return 0;
        }
    }
    /** Combat can shoot these entities directly, bypassing offline settlement raid rules. */
    public boolean damage(int attackerId,int entityId,int damage){
        synchronized(host){
            ArenaSystems.Player attacker=host.player(attackerId);
            Entity e=entities.get(entityId);
            if(attacker==null||attacker.dead||e==null||e.hp<=0||damage<1||damage>500||
               e.ownerKey.equals(attacker.accountKey)||
               host.factions.allied(e.ownerKey,attacker.accountKey))return false;
            if(active(e)&&(e.blueprint.role()==Role.WALL||e.blueprint.role()==Role.BARRICADE))e.operatedAt=host.time();
            e.hp=Math.max(0,e.hp-damage);e.damagedAt=host.time();if(e.hp<=0)destroy(e);return true;
        }
    }
    private void destroy(Entity e){
        if(e.destroyedAt>=0)return;
        e.hp=0;e.destroyedAt=host.time();e.tracking=false;e.aimedPlayer=null;e.aimedNpc=null;
        bursts.add(new Burst(e.x,e.y,e.blueprint.faction().ordinal()));
    }
    private void updateTurretAim(Entity e){
        Blueprint b=e.blueprint;
            ArenaSystems.Player victim=null;World.Npc npc=null;double nearest=7.5;
            for(ArenaSystems.Player p:host.roster()){
                if(p.dead||ally(e,p))continue;
                double range=Math.hypot(p.x-e.x,p.y-e.y);
                if(range<nearest&&host.clear(e.x,e.y,p.x,p.y)){nearest=range;victim=p;npc=null;}
            }
            if(b.faction()!=Faction.BEGOTTEN)for(World.Npc n:host.world.npcs){
                if(n.hp<=0)continue;
                double range=Math.hypot(n.x-e.x,n.y-e.y);
                if(range<nearest&&host.clear(e.x,e.y,n.x,n.y)){nearest=range;victim=null;npc=n;}
            }
        e.aimedPlayer=victim;e.aimedNpc=npc;e.tracking=victim!=null||npc!=null;
        if(e.tracking){
            double tx=victim!=null?victim.x:npc.x,ty=victim!=null?victim.y:npc.y;
            e.aimYaw=e.heading-Math.atan2(ty-e.y,tx-e.x)-Math.PI/2;e.aimPitch=Math.atan2(.3,nearest);
        }
    }
    private void tickEffect(Entity e){
        Blueprint b=e.blueprint;
        if(b.role()==Role.MIASMA)e.operatedAt=host.time();
        for(ArenaSystems.Player p:host.roster()){
            if(p.dead||Math.hypot(p.x-e.x,p.y-e.y)>
               (b.role()==Role.MIASMA?3.0:b.role()==Role.SHIELD?2.4:2.2))continue;
            boolean friendly=ally(e,p);
            switch(b.role()){
                case HEALTH -> {
                    if(friendly){if(p.hp<100||p.sanity<100)e.operatedAt=host.time();p.hp=Math.min(100,p.hp+(b.faction()==Faction.BEGOTTEN?5:
                        b.faction()==Faction.PENITENT?2:4));
                        p.sanity=Math.min(100,p.sanity+(b.faction()==Faction.PENITENT?4:1));}
                }
                case AMMO -> {
                    if(friendly&&e.ammoStock>0&&host.time()%3000<1000){
                        int amount=Math.min(e.ammoStock,b.faction()==Faction.UNRAVELED?9:
                            b.faction()==Faction.PENITENT?7:6);
                        int supplied=host.arsenal.fieldRefill(p.id,b.faction().ordinal(),amount);
                        e.ammoStock-=supplied;
                        if(supplied>0)e.operatedAt=host.time();
                        if(supplied>0&&b.faction()==Faction.PENITENT)
                            p.sanity=Math.min(100,p.sanity+2);
                        if(supplied>0&&b.faction()==Faction.BEGOTTEN)
                            p.hp=Math.min(100,p.hp+2);
                    }
                }
                case MIASMA -> {
                    e.operatedAt=host.time();
                    if(!friendly){
                        if(b.faction()==Faction.PENITENT)host.damage(p,0,3);
                        else if(b.faction()==Faction.BEGOTTEN)host.damage(p,3,1);
                    }
                }
                case BARRICADE -> {
                    if(!friendly&&b.faction()==Faction.PENITENT)host.damage(p,0,1);
                    else if(!friendly&&b.faction()==Faction.BEGOTTEN)host.damage(p,2,0);
                }
                case WALL -> {
                    if(friendly&&b.faction()==Faction.PENITENT)
                        p.sanity=Math.min(100,p.sanity+1);
                    if(!friendly&&b.faction()==Faction.BEGOTTEN&&
                       Math.hypot(p.x-e.x,p.y-e.y)<.9)host.damage(p,2,0);
                }
                case SHIELD -> {
                    if(friendly&&b.faction()==Faction.PENITENT)p.sanity=Math.min(100,p.sanity+2);
                }
                case TURRET -> {}
            }
        }
        if(b.role()==Role.TURRET){
            updateTurretAim(e);ArenaSystems.Player victim=e.aimedPlayer;World.Npc npc=e.aimedNpc;
            int hit=b.faction()==Faction.UNRAVELED?11:b.faction()==Faction.PENITENT?7:9;
            if(e.tracking)e.operatedAt=host.time();
            if(victim!=null){
                host.damage(victim,hit,b.faction()==Faction.PENITENT?6:0);
                if(b.faction()==Faction.BEGOTTEN)
                    host.damage(victim,2,1); // Living weapon delivers a second toxic pulse.
            }
            if(npc!=null)npc.hp=Math.max(0,npc.hp-hit);
        }
        if(b.role()==Role.SHIELD&&b.faction()==Faction.BEGOTTEN)
            e.hp=Math.min(b.hp(),e.hp+1);
    }
    /** Called after host clock increments; a harmless burst is the ONLY death/expiry explosion. */
    public void advance(long ms){
        synchronized(host){
            long now=host.time();
            Iterator<Entity> it=entities.values().iterator();
            while(it.hasNext()){
                Entity e=it.next();
                if(e.destroyedAt<0&&(e.hp<=0||!e.flying&&now>=e.expiresAt))destroy(e);
                if(e.destroyedAt>=0){
                    if(now-e.destroyedAt>=DESTROY_MS){removed.add(e.id);it.remove();}
                    continue;
                }
                if(e.flying){
                    double t=Math.min(1,(now-e.launchedAt)/700.0);
                    e.x=e.fromX+(e.toX-e.fromX)*t;e.y=e.fromY+(e.toY-e.fromY)*t;
                    e.z=.08+.85*Math.sin(t*Math.PI);
                    if(t>=1){e.flying=false;e.z=0;e.landedAt=now;e.expiresAt=now+LIFETIME_MS;e.nextPulse=now+ACTIVATION_MS;}
                }else{
                    if(active(e)&&e.blueprint.role()==Role.TURRET)updateTurretAim(e);
                    if(active(e)&&now>=e.nextPulse){
                        tickEffect(e);e.nextPulse=now+1000;
                    }
                    // Structural integrity naturally deteriorates throughout the hour.
                    int timeHp=Math.max(1,(int)Math.ceil(e.blueprint.hp()*
                        ((e.expiresAt-now)/(double)LIFETIME_MS)));
                    e.hp=Math.min(e.hp,timeHp);
                }
            }
        }
    }
}
