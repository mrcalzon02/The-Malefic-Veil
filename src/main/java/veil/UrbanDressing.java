package veil;

import java.util.*;

/**
 * Real Kenney Retro Urban Kit environmental placement shared by client and host.
 * Urban trees and parked, abandoned road vehicles become authoritative obstacles.
 * These are static derelicts; active Vehicles remain a separate server-owned system.
 */
public final class UrbanDressing {
    private UrbanDressing(){}
    private static long hash(long seed,int x,int y,int salt){
        return World.mix(seed ^ ((long)x*0x9e3779b97f4a7c15L) ^
            ((long)y*0x632be59bd9b4e019L) ^ ((long)salt*0x91e10da5L));
    }
    private static boolean clear(CityLayout layout,double x,double y,double r,
                                 double[][] spawns,List<World.UrbanProp> placed){
        // Check the full trunk/hulk footprint, not just its center.
        for(double dx:new double[]{-r,0,r})for(double dy:new double[]{-r,0,r})
            if(layout.tile((int)Math.floor(x+dx),(int)Math.floor(y+dy))!=0)return false;
        for(double[] s:spawns)if(Math.hypot(x-s[0],y-s[1])<5+r)return false;
        for(World.UrbanProp p:placed)
            if(p.radius()>0&&Math.hypot(x-p.x(),y-p.y())<r+p.radius()+.5)return false;
        return true;
    }
    public static List<World.UrbanProp> generate(long seed,CityLayout city,double[][] spawns){
        Objects.requireNonNull(city);
        Objects.requireNonNull(spawns);
        List<World.UrbanProp> out=new ArrayList<>();
        for(CityLayout.Plot plot:city.plots()){
            CityLayout.Category category=plot.template().category();
            int variant=plot.template().variant();
            long n=hash(seed,plot.x(),plot.y(),911);
            int x0=plot.x()*CityLayout.CELL,x1=plot.right()*CityLayout.CELL;
            int y0=plot.y()*CityLayout.CELL,y1=plot.bottom()*CityLayout.CELL;
            int width=x1-x0,height=y1-y0;
            // Trees form meaningful park clusters, with thinner growth on
            // abandoned/damaged lots. They are not planted in active streets.
            // OPEN variant 2 is legally VACANT construction land: never plant
            // collidable trees there or remove the only building site.
            boolean park=category==CityLayout.Category.OPEN&&(variant==3||variant==7||
                variant==5);
            boolean neglected=category==CityLayout.Category.HAZARD&&(n&3)==0;
            if(park||neglected){
                int count=park?3+Math.min(7,width*height/65):2;
                for(int i=0;i<count;i++){
                    long roll=hash(seed,plot.x()+i,plot.y(),931+i);
                    double x=x0+1.5+Math.floorMod(roll,Math.max(1,width-3));
                    double y=y0+1.5+Math.floorMod(roll>>>24,Math.max(1,height-3));
                    boolean shrub=(roll&3)==0;
                    double radius=shrub?.25:.52;
                    if(!clear(city,x,y,radius,spawns,out))continue;
                    String type=shrub?"tree-shrub":switch((int)(roll>>>5)&3){
                        case 0 -> "tree-small";case 1 -> "tree-pine-small";
                        case 2 -> "tree-park-large";default -> "tree-large";
                    };
                    double scale=shrub?1.0:((roll&8)==0?1.0:1.25);
                    out.add(new World.UrbanProp(type,x,y,((roll>>>9)&3)*Math.PI/2,
                        scale,radius,0));
                }
            }
            // Actual kit contains green/grey TRUCK models, not a car model.
            // Place those honestly as derelict vehicles; server-owned playable
            // cars are handled independently by Vehicles and its network state.
            boolean depot=category==CityLayout.Category.RESOURCE&&
                (variant==0||variant==3||variant==4||variant==6||variant==7);
            boolean carPark=category==CityLayout.Category.OPEN&&variant==1;
            if(depot||carPark){
                int count=carPark?Math.min(5,1+width/4):2;
                for(int i=0;i<count;i++){
                    long pick=hash(seed,plot.x(),plot.y()+i,1199+i);
                    double x=x0+2.5+(i%3)*2.2;
                    double y=y0+2.5+(i/3)*3.0;
                    if(x>=x1-1||y>=y1-1||!clear(city,x,y,.82,spawns,out))continue;
                    String truck=switch((int)(pick&3)){
                        case 0 -> "truck-grey";case 1 -> "truck-green";
                        case 2 -> "truck-grey-cargo";default -> "truck-green-cargo";
                    };
                    out.add(new World.UrbanProp(truck,x,y,(pick&4)==0?0:Math.PI/2,
                        1.05,.82,0));
                }
            }
        }
        return List.copyOf(out);
    }
}
