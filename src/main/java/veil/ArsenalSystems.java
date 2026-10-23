package veil;

import java.io.*;
import java.util.*;
import static veil.ArsenalCatalog.*;

/** Shared solo/LAN host authority for the working arsenal and fabrication economy. */
public final class ArsenalSystems {
    private final ArenaSystems host;
    private final Map<Integer,State> active=new HashMap<>();
    private final Map<String,State> accounts=new TreeMap<>();
    public final List<Loot> loot=new ArrayList<>();
    private final List<Projectile> projectiles=new ArrayList<>();
    private final List<Effect> effects=new ArrayList<>();
    private final List<Field> fields=new ArrayList<>();
    private final Map<Object,Long> chilled=new IdentityHashMap<>();
    private int nextEffect;
    public static final class State {
        long owned=1,researched; int selected,faction=-1; boolean factionLocked;
        final int[] clip=new int[32],reserve=new int[32],materials=new int[9];
        long readyAt,reloadEnd,chargeAt=-1,lastSequence,spinUntil; int reloadWeapon=-1,charging=-1,action,damage,storedScrap;
        boolean chargeAlt; double heat; long actionAt;
        State(){clip[0]=get(0).capacity();reserve[0]=48;}
    }
    public record View(long owned,int selected,int faction,boolean factionLocked,int[] clip,int[] reserve,int[] materials,
                       int action,long actionAt,long reloadRemaining,long chargeRemaining,double heat,int benchTier,long researched) {
        public View {clip=clip.clone();reserve=reserve.clone();materials=materials.clone();}
        public int[] clip(){return clip.clone();} public int[] reserve(){return reserve.clone();} public int[] materials(){return materials.clone();}
    }
    public static final class Loot {
        public final double x,y; public final int type,value; public boolean taken;
        Loot(double x,double y,int type,int value){this.x=x;this.y=y;this.type=type;this.value=value;}
    }
    public record Result(boolean ok,String reason){}
    public record Shot(boolean fired,String reason,int damage,int ammo){}
    public record Effect(int id,String kind,double x,double y,double tx,double ty,double size,int color,long until){}
    private static final class Projectile {
        int owner,weapon,damage,bounces; double x,y,z=1.1,vx,vy,vz,radius;long expires;Kind kind;boolean alt,returning,stuck;
        final Set<Object> hit=Collections.newSetFromMap(new IdentityHashMap<>());
    }
    private static final class Field {int owner,damage;double x,y,radius;long expires,next;Kind kind;}
    public ArsenalSystems(ArenaSystems host){
        this.host=host;Random random=new Random(host.world.seed^0x415253454e414cL);
        for(int i=0;i<112;i++){
            double x=0,y=0;
            for(int tries=0;tries<10000;tries++){x=1.5+random.nextInt(World.SIZE-3);y=1.5+random.nextInt(World.SIZE-3);if(host.world.canWalk(x,y))break;}
            loot.add(new Loot(x,y,i<32?0:i<88?1:2,i<32?i%8:i<88?(i-32)%4:0));
        }
        // Visible initial scavenging trail. These consume the same finite loot entries.
        double[][] trail={{2.5,1.5},{3.5,1.5},{1.5,2.5},{1.5,3.5}};
        for(int i=0;i<4;i++)if(host.world.canWalk(trail[i][0],trail[i][1]))loot.add(new Loot(trail[i][0],trail[i][1],1,i));
    }
    public void join(int id){synchronized(host){active.put(id,new State());}}
    public void bind(int id,String account){synchronized(host){
        State s=accounts.get(account);if(s==null){s=active.get(id);accounts.put(account,s);}else host.player(id).scrap=s.storedScrap;
        active.put(id,s);cancel(s);s.lastSequence=0;alignFaction(id,s);
    }}
    public void leave(int id){synchronized(host){State s=active.remove(id);ArenaSystems.Player p=host.player(id);if(s!=null){if(p!=null)s.storedScrap=p.scrap;cancel(s);}}}
    private State state(int id){return active.get(id);}
    private static boolean owns(State s,int w){return s!=null&&w>=0&&w<32&&(s.owned&(1L<<w))!=0;}
    private boolean alive(int id){ArenaSystems.Player p=host.player(id);return p!=null&&!p.dead&&host.vehicles.carDrivenBy(id)<0;}
    private void cancel(State s){s.reloadWeapon=-1;s.reloadEnd=0;s.charging=-1;s.chargeAt=-1;s.spinUntil=0;s.action=0;}
    public Result select(int id,int w){synchronized(host){State s=state(id);if(!alive(id)||!owns(s,w))return new Result(false,"NOT_OWNED");if(s.selected!=w){cancel(s);s.selected=w;}return new Result(true,"OK");}}
    public void release(int id){synchronized(host){State s=state(id);if(s!=null){s.charging=-1;s.chargeAt=-1;s.spinUntil=0;if(s.action==2)s.action=0;}}}
    private void alignFaction(int id,State state){
        ArenaSystems.Player player=host.player(id);
        state.faction=player==null?-1:ArsenalCatalog.factionIndex(
            host.factions.faction(player.accountKey));
    }
    /** The player's already-registered faction is the only manufacturer. */
    public Result faction(int id,int requested){synchronized(host){
        State state=state(id);
        if(!alive(id)||state==null||requested<0||requested>2)
            return new Result(false,"INVALID_FACTION");
        alignFaction(id,state);
        return state.faction==requested?new Result(true,"FACTION_BOUND"):
            new Result(false,"FACTION_ALREADY_ENROLLED");
    }}
    private World.Device bench(int id){
        ArenaSystems.Player p=host.player(id);if(p==null)return null;World.Device nearest=null;double best=2.6;
        for(World.Device d:host.world.devices)if(d.kind==ArenaSystems.FIELD_BENCH&&d.hp>0&&(d.ownerId==id||p.accountKey.equals(d.ownerKey))){
            double dist=Math.hypot(d.x-p.x,d.y-p.y);if(dist<best&&host.world.lineClear(p.x,p.y,d.x,d.y)){nearest=d;best=dist;}
        }return nearest;
    }
    public View view(int id){synchronized(host){State s=state(id);ArenaSystems.Player p=host.player(id);if(s==null||p==null)return null;
        alignFaction(id,s);
        int[] m=s.materials.clone();m[0]=p.scrap;s.storedScrap=p.scrap;World.Device b=bench(id);
        return new View(s.owned,s.selected,s.faction,s.factionLocked,s.clip,s.reserve,m,s.action,s.actionAt,
            Math.max(0,s.reloadEnd-host.time()),s.chargeAt<0?0:Math.max(0,mode(s.selected,s.chargeAlt).charge()-(host.time()-s.chargeAt)),s.heat,b==null?0:b.tier,s.researched);
    }}
    private Result pay(int id,int[] cost,boolean debit){
        State s=state(id);ArenaSystems.Player p=host.player(id);
        for(int i=0;i<9;i++)if((i==0?p.scrap:s.materials[i])<cost[i])return new Result(false,"NEED_"+MATERIALS[i].toUpperCase().replace(' ','_'));
        if(debit)for(int i=0;i<9;i++){if(i==0)p.scrap-=cost[i];else s.materials[i]-=cost[i];}
        return new Result(true,"OK");
    }
    public Result craft(int id,int recipe){synchronized(host){
        if(!alive(id))return new Result(false,"UNAVAILABLE");State s=state(id);alignFaction(id,s);World.Device b=bench(id);if(b==null)return new Result(false,"OWN_BENCH_REQUIRED");
        int[] cost;int component=-1,w=-1;
        if(recipe>=32&&recipe<=36){component=recipe-28;cost=COMPONENT_COSTS[recipe-32];if(recipe==36&&b.tier<3)return new Result(false,"BENCH_TIER_3");}
        else if(recipe==40){w=s.selected;if(s.reserve[w]>=get(w).capacity()*8)return new Result(false,"AMMO_FULL");cost=new int[]{2,0,0,0,0,0,0,0,0};if(get(w).faction()==1){cost[1]=1;cost[2]=1;}if(get(w).tier()==3)cost[3]=2;}
        else if(recipe>=0&&recipe<32){
            w=recipe;Weapon d=get(w);if(owns(s,w))return new Result(false,"ALREADY_OWNED");
            if(d.tier()>b.tier)return new Result(false,"BENCH_TIER_"+d.tier());
            if(d.faction()>=0&&s.faction!=d.faction())return new Result(false,"FACTION_RECIPE");
            if(d.prerequisite()>=0&&(s.researched&(1L<<d.prerequisite()))==0&&!get(d.prerequisite()).mapSpawn())return new Result(false,"RESEARCH_"+get(d.prerequisite()).name().toUpperCase().replace(' ','_'));
            if(d.prerequisite()>=0&&get(d.prerequisite()).mapSpawn()&&!owns(s,d.prerequisite()))return new Result(false,"BASE_WEAPON_REQUIRED");cost=d.cost();
        }else return new Result(false,"UNKNOWN_RECIPE");
        if(component>=0&&s.materials[component]>=999)return new Result(false,"STACK_FULL");
        Result paid=pay(id,cost,false);if(!paid.ok())return paid;pay(id,cost,true);
        if(component>=0)s.materials[component]++;
        else if(recipe==40)s.reserve[w]=Math.min(get(w).capacity()*8,s.reserve[w]+get(w).capacity()*2);
        else{s.owned|=1L<<w;s.researched|=1L<<w;s.clip[w]=get(w).capacity();s.reserve[w]=get(w).capacity();if(get(w).faction()>=0)s.factionLocked=true;}
        return new Result(true,"CRAFTED");
    }}
    public Result upgrade(int id){synchronized(host){
        if(!alive(id))return new Result(false,"UNAVAILABLE");World.Device b=bench(id);if(b==null)return new Result(false,"OWN_BENCH_REQUIRED");if(b.tier>=3)return new Result(false,"MAX_TIER");
        int[] cost=b.tier==1?new int[]{20,10,0,8,0,0,0,0,0}:new int[]{0,0,0,0,3,3,2,0,0};Result r=pay(id,cost,false);if(!r.ok())return r;pay(id,cost,true);b.tier++;return new Result(true,"BENCH_TIER_"+b.tier);
    }}
    public Result pickup(int id,int index){synchronized(host){
        if(!alive(id)||index<0||index>=loot.size())return new Result(false,"INVALID_LOOT");Loot l=loot.get(index);ArenaSystems.Player p=host.player(id);State s=state(id);
        if(l.taken||Math.hypot(l.x-p.x,l.y-p.y)>.65||!host.clear(p.x,p.y,l.x,l.y))return new Result(false,"OUT_OF_REACH");
        if(l.type==0){if(!get(l.value).mapSpawn())throw new IllegalStateException("Crafted weapon in map loot");if(owns(s,l.value)){if(s.reserve[l.value]>=get(l.value).capacity()*8)return new Result(false,"AMMO_FULL");s.reserve[l.value]=Math.min(get(l.value).capacity()*8,s.reserve[l.value]+get(l.value).capacity());}else{s.owned|=1L<<l.value;s.clip[l.value]=get(l.value).capacity();s.reserve[l.value]=get(l.value).capacity();}}
        else if(l.type==1){int value=l.value==0?p.scrap:s.materials[l.value];if(value>=999)return new Result(false,"STACK_FULL");if(l.value==0)p.scrap=Math.min(999,p.scrap+5);else s.materials[l.value]=Math.min(999,value+4);}
        else{int w=s.selected;if(s.reserve[w]>=get(w).capacity()*8)return new Result(false,"AMMO_FULL");s.reserve[w]=Math.min(get(w).capacity()*8,s.reserve[w]+get(w).capacity()*2);}
        l.taken=true;return new Result(true,"COLLECTED");
    }}
    public Result reload(int id){synchronized(host){
        State s=state(id);if(!alive(id))return new Result(false,"UNAVAILABLE");int w=s.selected;
        if(s.reloadWeapon>=0)return new Result(false,"RELOADING");if(s.clip[w]>=get(w).capacity()||s.reserve[w]==0)return new Result(false,"NO_RELOAD");
        s.charging=-1;s.chargeAt=-1;s.reloadWeapon=w;s.reloadEnd=host.time()+get(w).reloadMs();s.action=3;s.actionAt=host.time();return new Result(true,"RELOADING");
    }}
    void legacyAmmo(int id,int kind){State s=state(id);if(s==null)return;for(int w=0;w<8;w++)if(owns(s,w)&&((kind==3)==(w>=4&&w<=6)))s.reserve[w]=Math.min(get(w).capacity()*8,s.reserve[w]+get(w).capacity());}
    private static Mode mode(int w,boolean alt){return alt?get(w).alternate():get(w).primary();}
    public ArenaSystems.ShotReport compatibilityFire(int id,int oldWeapon,long sequence){synchronized(host){
        int w=oldWeapon==0?0:oldWeapon==1?6:-1;State s=state(id);if(w<0||!owns(s,w))return new ArenaSystems.ShotReport(sequence,false,"WEAPON",0,0,0);
        if(s.selected!=w){Result r=select(id,w);if(!r.ok())return new ArenaSystems.ShotReport(sequence,false,r.reason(),0,0,s.clip[w]);}
        Map<Integer,Integer> before=new HashMap<>();for(var p:host.roster())before.put(p.id,p.hp);int[] npcs=host.world.npcs.stream().mapToInt(n->n.hp).toArray();Shot shot=fire(id,false,sequence);
        int target=0;String reason=shot.fired()?"MISS":shot.reason();
        for(var p:host.roster())if(p.hp<before.getOrDefault(p.id,p.hp)){target=p.id;reason="PLAYER";break;}
        if(reason.equals("MISS"))for(int i=0;i<npcs.length;i++)if(host.world.npcs.get(i).hp<npcs[i]){target=i;reason="NPC";break;}
        return new ArenaSystems.ShotReport(sequence,shot.fired(),reason,target,shot.damage(),shot.ammo());
    }}
    private static int cost(int w,boolean alt,Mode m){
        if(m.kind()==Kind.MISSILE)return m.pellets();if(alt&&w==18)return 3;if(alt&&w==4)return 2;if(alt&&w==5)return 3;if(alt&&(w==1||w==12||w==10))return 2;if(alt&&w==13)return get(w).capacity();return w==27&&!alt?4:1;
    }
    public Shot fire(int id,boolean alt,long sequence){synchronized(host){
        State s=state(id);if(s==null||sequence<=s.lastSequence||sequence<=0)return new Shot(false,"DUPLICATE",0,0);s.lastSequence=sequence;
        int w=s.selected;int remaining=s.clip[w];if(!alive(id))return new Shot(false,"UNAVAILABLE",0,remaining);long now=host.time();
        if(s.reloadWeapon>=0)return new Shot(false,"RELOADING",0,remaining);
        Mode m=mode(w,alt);int price=cost(w,alt,m);
        if(m.kind()==Kind.DETONATE||m.kind()==Kind.RECALL||m.kind()==Kind.STICKY){
            List<Projectile> owned=new ArrayList<>();for(Projectile q:projectiles)if(q.owner==id&&q.weapon==w&&!(m.kind()==Kind.RECALL&&q.returning))owned.add(q);
            if(!owned.isEmpty()&&alt){s.damage=0;for(Projectile q:owned){if(m.kind()==Kind.RECALL){q.returning=true;q.hit.clear();q.damage=m.damage();}else{q.damage=m.damage();q.radius=m.radius();explode(q);projectiles.remove(q);}}s.readyAt=now+m.cooldown();s.action=4;s.actionAt=now;return new Shot(true,"ACTIVATED",s.damage,remaining);}
            if(m.kind()==Kind.DETONATE||m.kind()==Kind.RECALL)return new Shot(false,"NO_ACTIVE_PROJECTILE",0,remaining);
        }
        if(now<s.readyAt)return new Shot(false,"COOLDOWN",0,remaining);
        if(remaining<price)return new Shot(false,"NO_AMMO",0,remaining);
        if(s.heat>=1)return new Shot(false,"COOLING",0,remaining);
        boolean heldWarm=(get(w).mechanism().equals("rotor")||m.kind()==Kind.BEAM)&&s.spinUntil>now;
        if(m.charge()>0&&!heldWarm){
            if(s.charging!=w||s.chargeAlt!=alt){s.charging=w;s.chargeAlt=alt;s.chargeAt=now;s.action=2;s.actionAt=now;}
            if(now-s.chargeAt<m.charge())return new Shot(false,"CHARGING",0,remaining);
        }
        s.charging=-1;s.chargeAt=-1;s.spinUntil=now+300;s.clip[w]-=price;s.readyAt=now+m.cooldown();s.action=alt?4:1;s.actionAt=now;s.damage=0;
        if(w==0)host.player(id).pistolClip=s.clip[w];
        s.heat=Math.min(1.2,s.heat+(get(w).mechanism().equals("rotor")?.065:m.kind()==Kind.BEAM?.10:.025));
        if(s.heat>=1)s.readyAt=Math.max(s.readyAt,now+2200);
        ArenaSystems.Player p=host.player(id);p.protectedUntil=0;p.shotAt=now;host.refreshVisibility(p);
        switch(m.kind()){
            case HIT,PELLET,BEAM,RAIL,FLAK,SCRAP,CRYO -> {
                int pellets=m.pellets();double spread=m.kind()==Kind.PELLET||m.kind()==Kind.FLAK||m.kind()==Kind.SCRAP?.23:pellets>1?.06:0;
                for(int i=0;i<pellets;i++){
                    double a=p.angle+(pellets==1?0:(i/(double)(pellets-1)-.5)*spread*2);
                    ray(id,w,m,a,m.kind()==Kind.RAIL?2:1);
                }
            }
            case CHAIN -> chain(id,w,m);
            case PULSE -> area(id,p.x,p.y,m.radius(),m.damage(),Kind.PULSE);
            default -> {
                int n=m.kind()==Kind.MISSILE||m.kind()==Kind.MORTAR||m.kind()==Kind.ROCKET?m.pellets():1;
                if(projectiles.size()+n>192){s.clip[w]+=price;return new Shot(false,"PROJECTILE_LIMIT",0,s.clip[w]);}
                for(int j=0;j<n;j++){
                    Projectile q=new Projectile();q.owner=id;q.weapon=w;q.damage=m.damage();q.radius=m.radius();q.kind=m.kind();q.alt=alt;q.bounces=m.bounces();
                    double a=p.angle+(n==1?0:(j-(n-1)/2.0)*.08);q.x=p.x;q.y=p.y;q.vx=Math.cos(a)*m.speed();q.vy=Math.sin(a)*m.speed();
                    q.expires=now+(m.kind()==Kind.GRENADE?1800:m.kind()==Kind.MORTAR?2400:4500);
                    if(m.kind()==Kind.GRENADE||m.kind()==Kind.MORTAR||m.kind()==Kind.SMOKE||m.kind()==Kind.STICKY||m.kind()==Kind.BIO&&alt)q.vz=m.kind()==Kind.MORTAR?4.5:2.5;
                    projectiles.add(q);
                }
            }
        }
        effect("MUZZLE",p.x,p.y,p.x+Math.cos(p.angle)*.4,p.y+Math.sin(p.angle)*.4,.13,w,120);
        return new Shot(true,"FIRED",s.damage,s.clip[w]);
    }}
    private record Target(Object key,double x,double y,double radius){}
    private boolean visible(double x,double y,Target t){
        if(!(t.key instanceof World.Device target))return host.clear(x,y,t.x,t.y);
        if(!host.world.lineClear(x,y,t.x,t.y))return false;
        double dx=t.x-x,dy=t.y-y,len=dx*dx+dy*dy;
        for(World.Device d:host.world.devices)if(d!=target&&d.hp>0&&d.kind==ArenaSystems.BARRICADE){double f=ArenaSystems.clamp(((d.x-x)*dx+(d.y-y)*dy)/(len+1e-9),0,1);if(Math.hypot(x+dx*f-d.x,y+dy*f-d.y)<.36)return false;}return true;
    }
    private List<Target> targets(int owner,boolean self){
        List<Target> out=new ArrayList<>();for(ArenaSystems.Player p:host.roster())if(!p.dead&&(self||p.id!=owner))out.add(new Target(p,p.x,p.y,.33));
        for(World.Npc n:host.world.npcs)if(n.hp>0)out.add(new Target(n,n.x,n.y,.33));
        ArenaSystems.Player p=host.player(owner);
        for(World.Device d:host.world.devices)if(d.hp>0&&p!=null&&d.ownerId!=owner&&!p.accountKey.equals(d.ownerKey))out.add(new Target(d,d.x,d.y,.38));return out;
    }
    private void hit(int owner,Target t,int amount,Kind kind){
        if(amount<=0)return;State s=state(owner);ArenaSystems.Player p=host.player(owner);if(p==null||p.dead)return;int actual=0;
        if(t.key instanceof ArenaSystems.Player v){int old=v.hp;host.damage(v,amount,1);actual=old-v.hp;if(v.dead&&old>0&&v.id!=owner){p.frags++;p.cash+=25;}}
        else if(t.key instanceof World.Npc n){int old=n.hp;n.hp=Math.max(0,n.hp-amount);actual=old-n.hp;if(old>0&&n.hp==0){p.frags++;p.cash+=25;}}
        else if(t.key instanceof World.Device d){int old=d.hp;host.damageDevice(owner,host.world.devices.indexOf(d),Math.min(250,amount),System.currentTimeMillis());actual=old-d.hp;}
        if(s!=null)s.damage+=actual;if(kind==Kind.CRYO&&actual>0)chilled.put(t.key,host.time()+1800);
        if(actual>0)effect("HIT",t.x,t.y,t.x,t.y,.15,0,180);
    }
    private void ray(int owner,int w,Mode m,double angle,int penetrate){
        ArenaSystems.Player p=host.player(owner);double range=m.kind()==Kind.CRYO?5:m.kind()==Kind.PELLET||m.kind()==Kind.FLAK||m.kind()==Kind.SCRAP?8:m.kind()==Kind.BEAM&&w==13?7:30;
        double end=range;for(double r=.08;r<=range;r+=.08)if(!host.clear(p.x,p.y,p.x+Math.cos(angle)*r,p.y+Math.sin(angle)*r)){end=r-.08;break;}
        List<Target> aim=new ArrayList<>();for(Target t:targets(owner,false)){
            double dx=t.x-p.x,dy=t.y-p.y,along=dx*Math.cos(angle)+dy*Math.sin(angle),side=Math.abs(-dx*Math.sin(angle)+dy*Math.cos(angle));
            if(along>0&&along-t.radius<=end&&side<t.radius&&visible(p.x,p.y,t))aim.add(t);
        }aim.sort(Comparator.comparingDouble(t->Math.hypot(t.x-p.x,t.y-p.y)));
        for(int i=0;i<Math.min(penetrate,aim.size());i++)hit(owner,aim.get(i),m.damage(),m.kind());
        if(!aim.isEmpty())end=Math.min(end,Math.hypot(aim.get(Math.min(aim.size(),penetrate)-1).x-p.x,aim.get(Math.min(aim.size(),penetrate)-1).y-p.y));
        effect(m.kind()==Kind.BEAM?"BEAM":"TRACE",p.x,p.y,p.x+Math.cos(angle)*end,p.y+Math.sin(angle)*end,.06,w,140);
    }
    private void chain(int owner,int w,Mode m){
        ArenaSystems.Player p=host.player(owner);List<Target> available=targets(owner,false);Target first=null;double best=12;
        for(Target t:available){double d=Math.hypot(t.x-p.x,t.y-p.y);double a=Math.atan2(Math.sin(Math.atan2(t.y-p.y,t.x-p.x)-p.angle),Math.cos(Math.atan2(t.y-p.y,t.x-p.x)-p.angle));if(d<best&&Math.abs(a)<.25&&host.clear(p.x,p.y,t.x,t.y)){first=t;best=d;}}
        if(first==null)return;double x=p.x,y=p.y;int damage=m.damage()*m.pellets();Target target=first;
        for(int j=0;j<4&&target!=null;j++){
            hit(owner,target,damage,Kind.CHAIN);effect("ARC",x,y,target.x,target.y,.08,w,200);available.remove(target);x=target.x;y=target.y;damage=(int)(damage*.7);target=null;best=8;
            for(Target t:available){double d=Math.hypot(t.x-x,t.y-y);if(d<best&&host.clear(x,y,t.x,t.y)){best=d;target=t;}}
        }
    }
    private void area(int owner,double x,double y,double radius,int damage,Kind kind){
        for(Target t:targets(owner,true)){double d=Math.hypot(t.x-x,t.y-y);if(d<radius&&visible(x,y,t))hit(owner,t,Math.max(1,(int)(damage*(1-.7*d/radius))),kind);}
        effect(kind==Kind.SMOKE?"SMOKE":"BLAST",x,y,x,y,radius,kind==Kind.BIO?28:kind==Kind.CRYO?29:18,500);
    }
    private void explode(Projectile q){
        if(q.kind==Kind.SMOKE||q.kind==Kind.BIO&&q.alt||q.kind==Kind.VORTEX){
            if(fields.size()<32){Field f=new Field();f.owner=q.owner;f.x=q.x;f.y=q.y;f.radius=q.radius;f.damage=q.damage;f.kind=q.kind;f.expires=host.time()+(q.kind==Kind.SMOKE?6000:q.kind==Kind.BIO?4000:2000);fields.add(f);}return;
        }
        if(q.radius>0)area(q.owner,q.x,q.y,q.radius,q.damage,q.kind);else effect("IMPACT",q.x,q.y,q.x,q.y,.2,q.weapon,250);
    }
    private void effect(String kind,double x,double y,double tx,double ty,double size,int w,long ms){
        if(effects.size()>=192)effects.remove(0);int color=w>=9&&w<=17?0x73D5F4:w>=28?0xDD83E9:0xF5B55C;
        effects.add(new Effect(++nextEffect,kind,x,y,tx,ty,size,color,host.time()+ms));
    }
    public List<Effect> effects(){synchronized(host){List<Effect> out=new ArrayList<>(effects);
        for(Projectile q:projectiles)out.add(new Effect(-q.hashCode(),"PROJECTILE",q.x,q.y,q.x,q.y,.10,0xEBBB6E,host.time()+150));
        for(Field f:fields)out.add(new Effect(-f.hashCode(),f.kind.name(),f.x,f.y,f.x,f.y,f.radius,f.kind==Kind.BIO?0x70BD65:0x9D82C7,host.time()+150));return List.copyOf(out);
    }}
    public double speed(Object actor){synchronized(host){return chilled.getOrDefault(actor,0L)>host.time()?.75:1;}}
    public void advance(long ms){synchronized(host){
        long now=host.time();effects.removeIf(e->e.until()<now);chilled.entrySet().removeIf(e->e.getValue()<=now);
        for(var entry:active.entrySet()){
            State s=entry.getValue();ArenaSystems.Player p=host.player(entry.getKey());if(p==null||p.dead){cancel(s);continue;}
            s.heat=Math.max(0,s.heat-ms*.00013);if(s.reloadWeapon>=0&&now>=s.reloadEnd){int w=s.reloadWeapon,n=Math.min(get(w).capacity()-s.clip[w],s.reserve[w]);s.clip[w]+=n;s.reserve[w]-=n;s.reloadWeapon=-1;s.reloadEnd=0;s.action=0;}
            if(s.action!=2&&s.action!=3&&now-s.actionAt>400)s.action=0;
        }
        // Substeps keep fast projectiles from tunneling through thin walls at a 100ms LAN tick.
        int steps=(int)Math.ceil(ms/20.0);double dt=ms/1000.0/steps;
        for(int step=0;step<steps;step++){
            for(Iterator<Projectile> it=projectiles.iterator();it.hasNext();){Projectile q=it.next();
                ArenaSystems.Player owner=host.player(q.owner);if(owner==null||owner.dead){it.remove();continue;}
                if(now>=q.expires){explode(q);it.remove();continue;}if(q.stuck)continue;
                if(q.returning){double a=Math.atan2(owner.y-q.y,owner.x-q.x);q.vx=Math.cos(a)*16;q.vy=Math.sin(a)*16;if(Math.hypot(owner.x-q.x,owner.y-q.y)<.4){it.remove();continue;}}
                else if(q.kind==Kind.MISSILE||q.kind==Kind.ORB){Target target=null;double best=q.kind==Kind.ORB?5:12;for(Target t:targets(q.owner,false)){double d=Math.hypot(t.x-q.x,t.y-q.y);if(d<best&&host.clear(q.x,q.y,t.x,t.y)){target=t;best=d;}}if(target!=null){double a=Math.atan2(q.vy,q.vx),wish=Math.atan2(target.y-q.y,target.x-q.x),turn=Math.atan2(Math.sin(wish-a),Math.cos(wish-a)),v=Math.hypot(q.vx,q.vy);a+=ArenaSystems.clamp(turn,-dt*1.5,dt*1.5);q.vx=Math.cos(a)*v;q.vy=Math.sin(a)*v;}}
                double nx=q.x+q.vx*dt,ny=q.y+q.vy*dt;
                boolean wall=!host.clear(q.x,q.y,nx,ny)||host.world.solid(nx,ny);
                if(wall){
                    if(q.kind==Kind.STICKY){q.stuck=true;continue;}
                    if(q.bounces>0){boolean bx=host.world.solid(nx,q.y)||!host.clear(q.x,q.y,nx,q.y),by=host.world.solid(q.x,ny)||!host.clear(q.x,q.y,q.x,ny);if(bx||!by)q.vx=-q.vx;if(by)q.vy=-q.vy;q.bounces--;if(q.kind==Kind.BOUNCE)q.damage=Math.min(80,q.damage+15);effect("BOUNCE",q.x,q.y,q.x,q.y,.15,q.weapon,180);continue;}
                    for(Target t:targets(q.owner,false))if(t.key instanceof World.Device&&Math.hypot(t.x-q.x,t.y-q.y)<.62&&visible(q.x,q.y,t))hit(q.owner,t,q.damage,q.kind);
                    explode(q);it.remove();continue;
                }
                q.x=nx;q.y=ny;
                if(q.vz!=0||q.kind==Kind.GRENADE||q.kind==Kind.MORTAR||q.kind==Kind.SMOKE||q.kind==Kind.STICKY||q.kind==Kind.BIO&&q.alt){q.z+=q.vz*dt;q.vz-=6*dt;if(q.z<=0){q.z=0;if(q.kind==Kind.GRENADE&&q.bounces>0){q.vz=Math.abs(q.vz)*.4;q.bounces--;}else if(q.kind==Kind.STICKY){q.stuck=true;}else{explode(q);it.remove();continue;}}}
                boolean impact=false;
                if(q.z<1.9)for(Target t:targets(q.owner,false))if(!q.hit.contains(t.key)&&Math.hypot(t.x-q.x,t.y-q.y)<t.radius+.10&&host.clear(q.x,q.y,t.x,t.y)){
                    q.hit.add(t.key);hit(q.owner,t,q.damage,q.kind);if(q.kind!=Kind.DISC){impact=true;break;}
                }
                if(impact){explode(q);it.remove();}
            }
        }
        for(Iterator<Field> it=fields.iterator();it.hasNext();){Field f=it.next();if(now>=f.expires||host.player(f.owner)==null){it.remove();continue;}if(now>=f.next){f.next=now+500;
            if(f.kind!=Kind.SMOKE)for(Target t:targets(f.owner,false))if(Math.hypot(t.x-f.x,t.y-f.y)<f.radius&&host.clear(f.x,f.y,t.x,t.y))hit(f.owner,t,f.damage,f.kind);
            if(f.kind==Kind.VORTEX)for(Projectile q:projectiles)if(Math.hypot(q.x-f.x,q.y-f.y)<f.radius&&host.clear(q.x,q.y,f.x,f.y)){q.vx+=(f.x-q.x)*.5;q.vy+=(f.y-q.y)*.5;}
        }}
    }}
    /** Save only verified account inventories; transient animations/projectiles never survive a restart. */
    public byte[] save()throws IOException{synchronized(host){
        for(var e:active.entrySet()){ArenaSystems.Player p=host.player(e.getKey());if(p!=null)e.getValue().storedScrap=p.scrap;}
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){
            out.writeInt(1);out.writeInt(loot.size());for(Loot l:loot)out.writeBoolean(l.taken);out.writeInt(accounts.size());
            for(var e:accounts.entrySet()){State s=e.getValue();out.writeUTF(e.getKey());out.writeLong(s.owned);out.writeLong(s.researched);out.writeInt(s.selected);out.writeInt(s.faction);out.writeBoolean(s.factionLocked);out.writeInt(s.storedScrap);for(int n:s.materials)out.writeInt(n);for(int n:s.clip)out.writeInt(n);for(int n:s.reserve)out.writeInt(n);}
        }return bytes.toByteArray();
    }}
    public void restore(byte[] bytes,boolean apply)throws IOException{synchronized(host){
        if(bytes.length==0)return;if(bytes.length>1_000_000)throw new IOException("Arsenal save too large");
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){
            if(in.readInt()!=1||in.readInt()!=loot.size())throw new IOException("Arsenal save catalog mismatch");boolean[] taken=new boolean[loot.size()];for(int i=0;i<taken.length;i++)taken[i]=in.readBoolean();
            int count=in.readInt();if(count<0||count>2000)throw new IOException("Arsenal account cap");Map<String,State> loaded=new TreeMap<>();
            for(int i=0;i<count;i++){String key=in.readUTF();State s=new State();s.owned=in.readLong();s.researched=in.readLong();s.selected=in.readInt();s.faction=in.readInt();s.factionLocked=in.readBoolean();s.storedScrap=in.readInt();
                if(!key.matches("acct_[a-f0-9]{64}")||loaded.containsKey(key)||(s.owned&~0xffffffffL)!=0||(s.researched&~0xffffffffL)!=0||!owns(s,s.selected)||s.faction< -1||s.faction>2||s.factionLocked&&s.faction<0||s.storedScrap<0||s.storedScrap>1_000_000_000)throw new IOException("Invalid arsenal account");
                for(int j=0;j<9;j++){s.materials[j]=in.readInt();if(s.materials[j]<0||s.materials[j]>999)throw new IOException("Invalid materials");}
                for(int j=0;j<32;j++){s.clip[j]=in.readInt();if(s.clip[j]<0||s.clip[j]>get(j).capacity())throw new IOException("Invalid clip");}
                for(int j=0;j<32;j++){s.reserve[j]=in.readInt();if(s.reserve[j]<0||s.reserve[j]>get(j).capacity()*8)throw new IOException("Invalid ammo");}loaded.put(key,s);
            }if(in.available()!=0)throw new IOException("Arsenal save trailing bytes");if(apply){if(!active.isEmpty())throw new IOException("Cannot restore live inventories");accounts.clear();accounts.putAll(loaded);for(int i=0;i<taken.length;i++)loot.get(i).taken=taken[i];}
        }
    }}
}
