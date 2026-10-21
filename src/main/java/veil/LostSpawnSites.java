package veil;

import java.util.*;

/**
 * The Lost: fixed city hazard structures own NPC lifetimes and respawns.
 * A killed or removed NPC frees only its own site slot; one global spawn cap
 * prevents nearby sites endlessly stacking enemies at an occupied street.
 *
 * All changes run on the authoritative ArenaSystems clock, never client timers.
 */
public final class LostSpawnSites {
    public record SiteView(int id,String building,int x,int y,int capacity,
                           int alive,long nextSpawnAt,String appearance) {}
    private static final class Site {
        int id,x,y,capacity;
        String building,appearance;
        long nextAt;
        int lastLiving;
        final List<World.Npc> owned=new ArrayList<>();
    }
    private final World world;
    private final List<Site> sites=new ArrayList<>();
    private int generation;
    private static final long COOLDOWN_MS=45_000;
    private static final int MAX_GLOBAL=45,MAX_NEAR=5;
    private static final double AREA_RADIUS=12;
    public LostSpawnSites(World world){
        this.world=Objects.requireNonNull(world);
        List<CityLayout.Plot> hazard=new ArrayList<>();
        for(CityLayout.Plot plot:world.city.plots())
            if(plot.template().category()==CityLayout.Category.HAZARD)hazard.add(plot);
        hazard.sort((a,b)->Long.compareUnsigned(
            keyed(world.seed,a,37),keyed(world.seed,b,37)));
        for(CityLayout.Plot plot:hazard){
            if(sites.size()>=36)break;
            int variant=plot.template().variant();
            { // every built HAZARD plot can be a Lost origin, not only select variants
                int left=plot.x()*CityLayout.CELL+2,top=plot.y()*CityLayout.CELL+2;
                int right=plot.right()*CityLayout.CELL-2,bottom=plot.bottom()*CityLayout.CELL-2;
                boolean created=false;
                for(int y=top;y<bottom&&!created;y++)for(int x=left;x<right&&!created;x++){
                    double px=x+.5,py=y+.5;
                    if(!world.canWalk(px,py))continue;
                    boolean protectedSpawn=false;
                    for(double[] spawn:world.spawns)if(Math.hypot(px-spawn[0],py-spawn[1])<8)
                        protectedSpawn=true;
                    if(protectedSpawn)continue;
                    Site site=new Site();
                    site.id=sites.size();site.x=x;site.y=y;
                    site.capacity=1; // assigned after site count is known
                    site.building=plot.template().id();
                    site.appearance=lostModel(world.seed,plot.x(),plot.y(),variant);
                    sites.add(site);created=true;
                }
            }
        }
        if(sites.isEmpty())throw new IllegalStateException("No hazard sites in generated city");
        // Compatibility: version-1 realms and the LAN protocol have precisely
        // 45 stable NPC indexes. Spread dormant slots among genuine buildings,
        // never fabricate extra city sites or let one building grow unbounded.
        int maxPerSite=(int)Math.ceil(MAX_GLOBAL/(double)sites.size());
        for(Site site:sites)site.capacity=Math.max(1,maxPerSite);
        while(world.npcs.size()<MAX_GLOBAL){
            boolean allocated=false;
            for(Site site:sites){
                if(world.npcs.size()>=MAX_GLOBAL)break;
                if(site.owned.size()>=site.capacity)continue;
                reserve(site);allocated=true;
            }
            if(!allocated)throw new IllegalStateException("Lost NPC slot assignment stuck");
        }
        advance(0,List.of());
    }
    private void reserve(Site site){
        World.Npc npc=new World.Npc(site.x+.5,site.y+.5);
        npc.hp=0;npc.sourceSite=site.id;npc.kind=site.appearance;
        site.owned.add(npc);world.npcs.add(npc);
    }
    private static long keyed(long seed,CityLayout.Plot plot,int salt){
        return World.mix(seed ^ ((long)plot.x()<<32) ^ plot.y() ^ salt*7919L);
    }
    private static String lostModel(long seed,int x,int y,int variant){
        String[] choices={"lost-drifter","lost-stalker","lost-wailer","lost-brute"};
        return choices[(int)Math.floorMod(World.mix(seed^(x*911382323L)^(y*972663749L)^variant),4)];
    }
    private int aliveNear(double x,double y,double radius){
        int count=0;
        for(World.Npc npc:world.npcs)
            if(npc.hp>0&&Math.hypot(npc.x-x,npc.y-y)<radius)count++;
        return count;
    }
    private int aliveTotal(){
        int count=0;for(World.Npc n:world.npcs)if(n.hp>0)count++;return count;
    }
    /** Dead creatures remain in their stable network/snapshot slots, never duplicated. */
    public void advance(long now,List<double[]> playerPositions){
        if(now<0)throw new IllegalArgumentException("Invalid host tick");
        Objects.requireNonNull(playerPositions);
        for(Site site:sites){
            int living=0;
            for(World.Npc npc:site.owned)if(npc.hp>0)living++;
            // Detect death transitions even after an older spawn cooldown expired.
            if(living<site.lastLiving)site.nextAt=Math.max(site.nextAt,now+COOLDOWN_MS);
            site.lastLiving=living;
            if(living>=site.capacity||now<site.nextAt||aliveTotal()>=MAX_GLOBAL||
                aliveNear(site.x+.5,site.y+.5,AREA_RADIUS)>=MAX_NEAR)continue;
            boolean watched=false;
            for(double[] p:playerPositions)if(Math.hypot(p[0]-site.x,p[1]-site.y)<10)
                watched=true;
            if(watched)continue; // no pop-in near living players
            World.Npc npc=null;
            for(World.Npc previous:site.owned)if(previous.hp<=0){npc=previous;break;}
            if(npc==null)continue;
            npc.hp=80;
            npc.x=site.x+.5;npc.y=site.y+.5;
            npc.cooldown=0;
            site.lastLiving++;
            site.nextAt=now+COOLDOWN_MS;generation++;
        }
    }
    public int generation(){return generation;}
    public int totalAlive(){return aliveTotal();}
    public List<SiteView> sites(){
        List<SiteView> out=new ArrayList<>();
        for(Site site:sites){
            int count=0;for(World.Npc npc:site.owned)if(npc.hp>0)count++;
            out.add(new SiteView(site.id,site.building,site.x,site.y,
                site.capacity,count,site.nextAt,site.appearance));
        }
        return List.copyOf(out);
    }
    public int siteFor(World.Npc npc){return npc==null?-1:npc.sourceSite;}
}
