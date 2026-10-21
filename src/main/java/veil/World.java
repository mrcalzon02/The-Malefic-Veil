package veil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Reproducible tile/collision authority shared by the renderer and LAN shot tests. */
public final class World {
    public static final int SIZE = 132;
    public final long seed;
    private final byte[][] walls = new byte[SIZE][SIZE];
    public final List<Npc> npcs = new ArrayList<>();
    public final List<Pickup> pickups = new ArrayList<>();
    public final List<Device> devices = new ArrayList<>();
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
        public double cooldown;
        Device(double x,double y,int kind){this.x=x;this.y=y;this.kind=kind;}
    }
    public World(long seed) {
        this.seed=seed;
        for(int x=0;x<SIZE;x++) for(int y=0;y<SIZE;y++) {
            int a=x%12,b=y%12;
            boolean road=a<3||b<3;
            int lotX=x/12,lotY=y/12;
            long block=mix(seed ^ (lotX*7919L) ^ (lotY*104729L));
            boolean courtyard=(block&7)==0;
            boolean perimeter=a==3||a==11||b==3||b==11;
            boolean door=(a==7&&(b==3||b==11))||(b==7&&(a==3||a==11));
            boolean pillar=a==6&&b==6&&((block>>>8)&3)==0;
            walls[x][y]=(byte)(road||courtyard||door ? 0 : perimeter||pillar ? 1 : 0);
            if(x==0||y==0||x==SIZE-1||y==SIZE-1) walls[x][y]=1;
        }
        for(double[] s:spawns) {
            int sx=(int)s[0],sy=(int)s[1];
            for(int x=Math.max(1,sx-1);x<=Math.min(SIZE-2,sx+1);x++)
                for(int y=Math.max(1,sy-1);y<=Math.min(SIZE-2,sy+1);y++) walls[x][y]=0;
        }
        Random r=new Random(seed^0x4D414C45464943L);
        for(int i=0;i<170;i++) {
            double[] p=randomOpen(r);
            pickups.add(new Pickup(p[0],p[1],i%7));
        }
        for(int i=0;i<45;i++) {
            double[] p=randomOpen(r);
            npcs.add(new Npc(p[0],p[1]));
        }
    }
    public static long mix(long a) {
        a=(a^(a>>>30))*0xbf58476d1ce4e5b9L;
        a=(a^(a>>>27))*0x94d049bb133111ebL;
        return a^(a>>>31);
    }
    private double[] randomOpen(Random r) {
        for(int i=0;i<10000;i++) {
            double x=1.5+r.nextInt(SIZE-3), y=1.5+r.nextInt(SIZE-3);
            if(!solid(x,y)) return new double[]{x,y};
        }
        throw new IllegalStateException("No traversable city tiles");
    }
    public int tile(int x,int y) {
        if(x<0||y<0||x>=SIZE||y>=SIZE) return 1;
        return walls[x][y];
    }
    public boolean solid(double x,double y){return tile((int)Math.floor(x),(int)Math.floor(y))!=0;}
    public boolean canWalk(double x,double y) {
        double r=.19;
        return !solid(x-r,y-r)&&!solid(x+r,y-r)&&!solid(x-r,y+r)&&!solid(x+r,y+r);
    }
    public boolean lineClear(double x,double y,double tx,double ty) {
        double dx=tx-x,dy=ty-y,len=Math.hypot(dx,dy);
        if(len<.01) return true;
        for(double d=.08;d<len-.08;d+=.08)
            if(solid(x+dx*d/len,y+dy*d/len))return false;
        return true;
    }
}
