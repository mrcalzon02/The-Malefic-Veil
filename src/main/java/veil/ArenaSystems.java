package veil;

import java.util.*;

/**
 * Host-owned arena state. All mutations are serialized by synchronized methods.
 * Millisecond game clock is explicitly advanced by host tick, so tests are deterministic.
 */
public final class ArenaSystems {
    public final World world;
    private final Map<Integer,Player> players=new LinkedHashMap<>();
    private long clock;
    public static final int TURRET=0, HEALTH_STATION=1, REVIVE_STATION=2, BARRICADE=3;
    public static final class Player {
        public final int id;
        public double x,y,angle,visibility;
        public int hp=100,sanity=100,cash=150,scrap=4,medkits=2;
        public int pistolReserve=48,shellReserve=18,pistolClip=12,shellClip=6;
        public int classId,frags,flags;
        public long lastHurt=-100000,shotAt=-100000,shadeUntil,abilityAt=-100000,respawnAt=-100000,moveAt;
        public boolean dead;
        Player(int id,double x,double y,long now){
            this.id=id;this.x=x;this.y=y;moveAt=now;
        }
    }
    public ArenaSystems(long seed){world=new World(seed);}
    public synchronized Player join(int id){
        if(players.containsKey(id))throw new IllegalArgumentException("duplicate peer");
        Player p=new Player(id,world.spawns[0][0],world.spawns[0][1],clock);
        players.put(id,p);refreshVisibility(p);return p;
    }
    public synchronized void leave(int id){players.remove(id);}
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
        if(p==null||p.dead||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(angle)||!free(x,y))return false;
        long elapsed=Math.max(0,clock-p.moveAt);
        double max=(flags&1)!=0?1.20:(flags&2)!=0?4.30:2.85;
        double permitted=.33+Math.min(elapsed,500)*max/1000;
        if(Math.hypot(x-p.x,y-p.y)>permitted)return false;
        p.x=x;p.y=y;p.angle=angle;p.flags=flags&3;p.moveAt=clock;
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
        if(p==null||p.dead)return;
        p.hp=Math.max(0,p.hp-Math.max(0,hp));
        p.sanity=Math.max(0,p.sanity-Math.max(0,sanity));
        p.lastHurt=clock;
        if(p.hp==0){p.dead=true;p.respawnAt=clock;}
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
    public synchronized boolean fire(int id,int weapon){
        Player p=player(id);
        if(p==null||p.dead||weapon<0||weapon>1)return false;
        long cooldown=weapon==0?210:850;
        if(clock-p.shotAt<cooldown)return false;
        if(weapon==0&&p.pistolClip<=0||weapon==1&&p.shellClip<=0)return false;
        if(weapon==0)p.pistolClip--;else p.shellClip--;
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
        int amount=weapon==0?25:Math.max(18,70-(int)(nearest*8));
        if(target instanceof Player victim){
            boolean wasAlive=!victim.dead;damage(victim,amount,weapon==1?6:2);
            if(wasAlive&&victim.dead){p.frags++;p.cash+=25;}
        } else if(target instanceof World.Npc npc){
            npc.hp-=amount;
            if(npc.hp<=0){p.frags++;p.cash+=25;}
        }
        return true;
    }
    public synchronized boolean build(int id,int kind){
        Player p=player(id);
        if(p==null||p.dead||kind<0||kind>3||world.devices.size()>=100)return false;
        int cash=kind==0?(p.classId==0?60:100):kind==1?75:kind==2?120:45;
        int scrap=kind==0?3:kind==1?1:kind==2?2:1;
        if(p.cash<cash||p.scrap<scrap)return false;
        double x=p.x+Math.cos(p.angle)*1.1,y=p.y+Math.sin(p.angle)*1.1;
        if(!free(x,y)||!clear(p.x,p.y,x,y))return false;
        for(World.Device d:world.devices)
            if(d.hp>0&&Math.hypot(d.x-x,d.y-y)<1.2)return false;
        p.cash-=cash;p.scrap-=scrap;
        world.devices.add(new World.Device(x,y,kind));return true;
    }
    public synchronized boolean respawn(int id,int spawn){
        Player p=player(id);
        if(p==null||!p.dead||spawn<0||spawn>=world.spawns.length||clock-p.respawnAt<1200)return false;
        p.x=world.spawns[spawn][0];p.y=world.spawns[spawn][1];
        p.hp=100;p.sanity=75;p.dead=false;p.moveAt=clock;
        p.pistolClip=12;p.shellClip=6;
        p.pistolReserve=Math.max(24,p.pistolReserve);p.shellReserve=Math.max(12,p.shellReserve);
        refreshVisibility(p);return true;
    }
    /** Drives health regeneration, devices and host-owned hostile AI. */
    public synchronized void advance(long ms){
        if(ms<=0||ms>1000)throw new IllegalArgumentException("bounded fixed tick required");
        clock+=ms;
        for(Player p:players.values()){
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
                    if(p.dead||Math.hypot(p.x-d.x,p.y-d.y)>=nearest||!clear(d.x,d.y,p.x,p.y))continue;
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
