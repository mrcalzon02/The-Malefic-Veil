package veil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Reproducible tile/collision authority shared by the renderer and LAN shot tests. */
public final class World {
    public static final int SIZE = 132;
    public static final int PARCEL_METERS = CityLayout.CELL; // One actual world, one metre-scale coordinate system.
    public final long seed;
    private final byte[][] walls = new byte[SIZE][SIZE];
    public final CityLayout city;
    private volatile ContinentLayout continent;
    public final List<Npc> npcs = new ArrayList<>();
    public final LostSpawnSites lostSites;
    public final List<Pickup> pickups = new ArrayList<>();
    public final List<Device> devices = new ArrayList<>();
    /** Same deterministic prop placements used by the client renderer and host collision. */
    public final List<UrbanProp> urbanProps = new ArrayList<>();
    /** Only extra physical obstacles, never every decorative Kenney tile. */
    private final List<UrbanProp> physicalProps = new ArrayList<>();
    public record UrbanProp(String model,double x,double y,double heading,double scale,double radius,
                            double heightOffset) {
        public UrbanProp(String model,double x,double y,double heading,double scale,double radius){
            this(model,x,y,heading,scale,radius,0);
        }
        public boolean solid(){return radius>0;}
    }
    public final double[][] spawns = {{1.5,1.5},{61.5,61.5},{121.5,121.5}};
    public static final class Npc {
        public double x, y, cooldown;
        public int hp = 80;
        public int sourceSite=-1;
        public String kind="lost-drifter";
        Npc(double x, double y) {this.x=x; this.y=y;}
    }
    public static final class Pickup {
        public final double x,y;
        public final int kind; // health, sanity, pistol, shotgun, cash, material, medkit
        public boolean taken;
        Pickup(double x,double y,int kind){this.x=x;this.y=y;this.kind=kind;}
    }
    public static final class Device {
        public final double x,y;
        public final int kind; // turret, heal, revive
        public int hp=100;
        public int tier=1; // host-upgradable arsenal fabrication station only
        public int ownerId=-1;
        public String ownerKey;
        public double cooldown;
        public final CampStockpile storage=new CampStockpile();
        // Durable active batch, not a shared factory inventory. Each faction
        // below has its own distinct processor and operating conditions.
        public boolean productionActive;
        public String productionRecipeId="";
        public long productionMs;
        public int productionCompleted;
        /** Charged seconds of genuine pressurized boiler operation. Only a
         * STEAM_BOILER may hold it; the host persists its remaining reserve. */
        public long steamMs;
        Device(double x,double y,int kind){this.x=x;this.y=y;this.kind=kind;}
    }
    public World(long seed) {
        this.seed=seed;
        city=CityLayout.generate(seed);
        city.copyInto(walls);
        for(double[] s:spawns) {
            int sx=(int)s[0],sy=(int)s[1];
            for(int x=Math.max(1,sx-1);x<=Math.min(SIZE-2,sx+1);x++)
                for(int y=Math.max(1,sy-1);y<=Math.min(SIZE-2,sy+1);y++) walls[x][y]=0;
        }
        generateUrbanProps();
        Random r=new Random(seed^0x4D414C45464943L);
        for(int i=0;i<170;i++) {
            int kind=i%7;
            CityLayout.Category preferred=kind==0||kind==6?CityLayout.Category.SAFE:
                kind==2||kind==3||kind==4||kind==5?CityLayout.Category.RESOURCE:null;
            double[] p=randomOpen(r,preferred);
            pickups.add(new Pickup(p[0],p[1],kind));
        }
        // Every initial hostile Lost NPC now belongs to a concrete ruin site.
        // Site-local caps, persistent slot identity and cooldowns govern respawns.
        lostSites=new LostSpawnSites(this);
    }
    private void generateUrbanProps() {
        // Kenney's original modular assets are the physical city construction grammar.
        // The CityLayout map continues to own all solid collision and LOS rules.
        urbanProps.addAll(UrbanTileAssembler.assemble(seed,city,spawns));
        // Trees, shrubs and derelict source-kit trucks have real, shared collision footprints.
        urbanProps.addAll(UrbanDressing.generate(seed,city,spawns));
        for(UrbanProp p:urbanProps)if(p.solid())physicalProps.add(p);
    }
    public static long mix(long a) {
        a=(a^(a>>>30))*0xbf58476d1ce4e5b9L;
        a=(a^(a>>>27))*0x94d049bb133111ebL;
        return a^(a>>>31);
    }
    /**
     * The existing 132 m authored arena is the detailed City A district of
     * the SAME procedural continent. No teleport, dimension, or second player
     * coordinate space. The macro continent uses six world metres per parcel.
     * Global (0,0) remains the legacy arena origin for save compatibility.
     */
    public int worldOffsetX(){
        return SIZE/2-continent().cities().get(0).x()*PARCEL_METERS;
    }
    public int worldOffsetY(){
        return SIZE/2-continent().cities().get(0).y()*PARCEL_METERS;
    }
    public int parcelX(double worldX){
        return (int)Math.floor((worldX-worldOffsetX())/PARCEL_METERS);
    }
    public int parcelY(double worldY){
        return (int)Math.floor((worldY-worldOffsetY())/PARCEL_METERS);
    }
    public double worldX(double parcelX){return worldOffsetX()+parcelX*PARCEL_METERS;}
    public double worldY(double parcelY){return worldOffsetY()+parcelY*PARCEL_METERS;}
    public boolean detailedCity(double x,double y){
        return x>=0&&y>=0&&x<SIZE&&y<SIZE;
    }
    private boolean openCityGate(int x,int y){
        // The northwest street/spawn is already walkable; open its two edge
        // crossings so walkers can leave City A on foot with no transition.
        return x>=0&&y>=0&&((x==0&&y>=1&&y<=2)||
            (y==0&&x>=1&&x<=2));
    }
    public boolean continentalWalkable(double x,double y){
        int px=parcelX(x),py=parcelY(y);
        ContinentLayout map=continent();
        return map.inBounds(px,py)&&map.land(px,py)&&
            (!map.worldStructureAt(px,py)||map.road(px,py)||
             map.cityProtected(px,py)&&!map.road(px,py));
    }
    /** Seed-identical continental plan, part of the actual shared world. */
    public ContinentLayout continent() {
        ContinentLayout map=continent;
        if(map==null)synchronized(this){if(continent==null)continent=ContinentLayout.generate(seed);map=continent;}
        return map;
    }
    /** Generated structures are not overwriteable by player-built devices or buildings. */
    public boolean worldStructureAt(double x,double y,double radius) {
        if(!Double.isFinite(x)||!Double.isFinite(y)||radius<0)return true;
        if(detailedCity(x,y)){
            if(city.worldStructureAt(x,y,radius))return true;
            for(UrbanProp p:physicalProps)
                if(Math.hypot(x-p.x,y-p.y)<radius+p.radius)return true;
            return false;
        }
        int cx=parcelX(x),cy=parcelY(y);
        return continent().worldStructureAt(cx,cy);
    }
    private double[] randomOpen(Random r,CityLayout.Category preferred) {
        for(int i=0;i<10000;i++) {
            double x=1.5+r.nextInt(SIZE-3), y=1.5+r.nextInt(SIZE-3);
            if(canWalk(x,y)&&(preferred==null||city.categoryAt((int)x,(int)y)==preferred))
                return new double[]{x,y};
        }
        if(preferred!=null)return randomOpen(r,null);
        throw new IllegalStateException("No traversable city tiles");
    }
    public int tile(int x,int y) {
        if(x>=0&&y>=0&&x<SIZE&&y<SIZE)
            return openCityGate(x,y)?0:walls[x][y];
        return continentalWalkable(x+.5,y+.5)?0:1;
    }
    public boolean solid(double x,double y){return tile((int)Math.floor(x),(int)Math.floor(y))!=0;}
    public boolean canWalk(double x,double y) {
        double r=.19;
        if(solid(x-r,y-r)||solid(x+r,y-r)||solid(x-r,y+r)||solid(x+r,y+r))return false;
        if(detailedCity(x,y))for(UrbanProp p:physicalProps)
            if(Math.hypot(x-p.x,y-p.y)<p.radius+r)return false;
        return true;
    }
    public boolean lineClear(double x,double y,double tx,double ty) {
        double dx=tx-x,dy=ty-y,len=Math.hypot(dx,dy);
        if(len<.01) return true;
        for(double d=.08;d<len-.08;d+=.08)
            if(solid(x+dx*d/len,y+dy*d/len))return false;
        for(UrbanProp p:physicalProps) {
            double t=Math.max(0,Math.min(1,((p.x-x)*dx+(p.y-y)*dy)/(len*len)));
            if(t>.05&&t<.95&&Math.hypot(x+dx*t-p.x,y+dy*t-p.y)<p.radius)return false;
        }
        return true;
    }
}
