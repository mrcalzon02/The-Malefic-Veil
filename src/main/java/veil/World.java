package veil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Reproducible tile/collision authority shared by the renderer and LAN shot tests. */
public final class World {
    public static final int SIZE = 132;
    public final long seed;
    private final byte[][] walls = new byte[SIZE][SIZE];
    public final CityLayout city;
    public final List<Npc> npcs = new ArrayList<>();
    public final List<Pickup> pickups = new ArrayList<>();
    public final List<Device> devices = new ArrayList<>();
    /** Same deterministic prop placements used by the client renderer and host collision. */
    public final List<UrbanProp> urbanProps = new ArrayList<>();
    public record UrbanProp(String model,double x,double y,double heading,double scale,double radius) {
        public boolean solid(){return radius>0;}
    }
    public final double[][] spawns = {{1.5,1.5},{61.5,61.5},{121.5,121.5}};
    public static final class Npc {
        public double x, y, cooldown;
        public int hp = 80;
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
        public int ownerId=-1;
        public double cooldown;
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
        for(int i=0;i<45;i++) {
            double[] p=randomOpen(r,i%4==0?null:CityLayout.Category.HAZARD);
            npcs.add(new Npc(p[0],p[1]));
        }
    }
    private void generateUrbanProps() {
        // Deliberate recognizable urban furniture on the initial thoroughfare.
        urbanProps.add(new UrbanProp("detail-barrier-type-a",5.2,1.5,0,1.7,.43));
        urbanProps.add(new UrbanProp("detail-bench",4.3,2.5,0,1.8,.58));
        urbanProps.add(new UrbanProp("detail-dumpster-closed",9.1,2.2,0,1.6,.59));
        urbanProps.add(new UrbanProp("detail-light-single",7.8,2.2,0,3.0,.15));
        urbanProps.add(new UrbanProp("road-asphalt-straight",4.7,1.5,Math.PI/2,1.0,0));
        String[] models={"road-asphalt-straight","detail-barrier-type-a","detail-bench",
            "detail-dumpster-closed","detail-light-single"};
        for(int bx=0;bx<SIZE/12;bx++)for(int by=0;by<SIZE/12;by++) {
            long hash=mix(seed^((long)bx*42137)^((long)by*99497));
            if((hash&3)==0)continue;
            int x=bx*12,y=by*12;
            if(x>SIZE-10||y>SIZE-10)continue;
            String model=models[(int)Math.floorMod(hash>>>5,models.length)];
            double px=x+5.5,py=y+1.45;
            if(solid(px,py))continue;
            double scale=model.contains("light")?2.8:model.contains("road")?1.0:1.5;
            double rad=switch(model){
                case "detail-barrier-type-a" -> .38;
                case "detail-bench" -> .5;
                case "detail-dumpster-closed" -> .55;
                case "detail-light-single" -> .14;
                default -> 0;
            };
            urbanProps.add(new UrbanProp(model,px,py,0,scale,rad));
        }
    }
    public static long mix(long a) {
        a=(a^(a>>>30))*0xbf58476d1ce4e5b9L;
        a=(a^(a>>>27))*0x94d049bb133111ebL;
        return a^(a>>>31);
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
        if(x<0||y<0||x>=SIZE||y>=SIZE) return 1;
        return walls[x][y];
    }
    public boolean solid(double x,double y){return tile((int)Math.floor(x),(int)Math.floor(y))!=0;}
    public boolean canWalk(double x,double y) {
        double r=.19;
        if(solid(x-r,y-r)||solid(x+r,y-r)||solid(x-r,y+r)||solid(x+r,y+r))return false;
        for(UrbanProp p:urbanProps)
            if(p.solid()&&Math.hypot(x-p.x,y-p.y)<p.radius+r)return false;
        return true;
    }
    public boolean lineClear(double x,double y,double tx,double ty) {
        double dx=tx-x,dy=ty-y,len=Math.hypot(dx,dy);
        if(len<.01) return true;
        for(double d=.08;d<len-.08;d+=.08)
            if(solid(x+dx*d/len,y+dy*d/len))return false;
        for(UrbanProp p:urbanProps) {
            if(!p.solid())continue;
            double t=Math.max(0,Math.min(1,((p.x-x)*dx+(p.y-y)*dy)/(len*len)));
            if(t>.05&&t<.95&&Math.hypot(x+dx*t-p.x,y+dy*t-p.y)<p.radius)return false;
        }
        return true;
    }
}
