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
            // Keep one visually vacant city-lot variant for readability;
            // it is still fixed CITY land and may never be player-built.
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
            // City scavenging props are physical source-kit containers. Never
            // place loot containers in SAFE plots (including spawn buildings).
            // The loot ledger derives immutable IDs from these seeded positions.
            if(category!=CityLayout.Category.SAFE){
                int count=category==CityLayout.Category.RESOURCE?3:
                    category==CityLayout.Category.HAZARD?2:1;
                for(int i=0;i<count;i++){
                    long pick=hash(seed,plot.x()+i,plot.y(),2219+i);
                    if(category==CityLayout.Category.OPEN&&(pick&3)==0)continue;
                    double x=x0+1.5+Math.floorMod(pick,Math.max(1,width-3));
                    double y=y0+1.5+Math.floorMod(pick>>>24,Math.max(1,height-3));
                    if(!clear(city,x,y,.70,spawns,out))continue;
                    out.add(new World.UrbanProp("detail-dumpster-closed",x,y,
                        ((pick>>>9)&3)*Math.PI/2,2.4,.70,0));
                }
            }
            // Keep Kenney trucks in depots and cargo yards. Parked civilian cars
            // instead use GGBot's UV-authored OBJ+PNG variants (when packaged).
            boolean depot=category==CityLayout.Category.RESOURCE&&
                (variant==0||variant==3||variant==4||variant==6||variant==7);
            boolean carPark=category==CityLayout.Category.OPEN&&variant==1;
            boolean wreckLot=category==CityLayout.Category.HAZARD&&(n&3)==1;
            boolean carsAvailable=GGBotCarPack.available();
            if(depot||carPark||wreckLot&&carsAvailable){
                List<String> civilians=List.of(),services=List.of(),wrecks=List.of();
                if(carsAvailable){
                    try{
                        civilians=GGBotCarPack.required().civilianModels();
                        services=GGBotCarPack.required().serviceModels();
                        wrecks=GGBotCarPack.required().wreckModels();
                    }catch(java.io.IOException e){
                        throw new IllegalStateException("Car asset changed during city generation",e);
                    }
                }
                int count=depot?2:carPark?Math.min(4,1+width/5):1;
                for(int i=0;i<count;i++){
                    long pick=hash(seed,plot.x(),plot.y()+i,1199+i);
                    double x=x0+2.5+(i%2)*3.2;
                    double y=y0+2.5+(i/2)*3.4;
                    boolean ggbot=carsAvailable&&!depot;
                    double radius=ggbot?1.1:.82;
                    if(x>=x1-1.6||y>=y1-1.6||
                       !clear(city,x,y,radius,spawns,out))continue;
                    String model;
                    if(ggbot){
                        List<String> palette=wreckLot?wrecks:
                            ((pick>>>9)&15)==0?services:civilians;
                        if(palette.isEmpty())continue;
                        model=palette.get((int)Math.floorMod(pick,palette.size()));
                    }else{
                        model=switch((int)(pick&3)){
                            case 0 -> "truck-grey";case 1 -> "truck-green";
                            case 2 -> "truck-grey-cargo";default -> "truck-green-cargo";
                        };
                    }
                    out.add(new World.UrbanProp(model,x,y,(pick&4)==0?0:Math.PI/2,
                        ggbot?.52:1.05,radius,0));
                }
            }
        }
        return List.copyOf(out);
    }
}
