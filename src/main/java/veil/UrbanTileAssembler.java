package veil;

import java.util.*;

/**
 * Assemble the CITY ITSELF from the licensed Kenney Retro Urban Kit's tile grammar.
 * The 6x6 CityLayout cell is a macroplot unit: actual roadway pieces, pavement,
 * floors, walls, doors and roofs are instantiated from source GLB modules.
 * The authoritative CityLayout tile map remains the collider/LOS source.
 */
public final class UrbanTileAssembler {
    private UrbanTileAssembler(){}
    private static final int CELL=CityLayout.CELL;
    private static void add(List<World.UrbanProp> out,String id,double x,double y,
                            double rotation,double scale,double radius){
        out.add(new World.UrbanProp(id,x,y,rotation,scale,radius));
    }
    private static void add(List<World.UrbanProp> out,String id,double x,double y,
                            double rotation,double scale,double radius,double heightOffset){
        out.add(new World.UrbanProp(id,x,y,rotation,scale,radius,heightOffset));
    }
    private static long roll(long seed,int x,int y,int salt){
        return World.mix(seed ^ ((long)x*0x9e3779b97f4a7c15L) ^
            ((long)y*0x632be59bd9b4e019L) ^ ((long)salt*0x1234f017L));
    }
    private static String from(long n,String... ids){
        return ids[(int)Math.floorMod(n,ids.length)];
    }
    public static List<World.UrbanProp> assemble(long seed,CityLayout city,double[][] spawns){
        Objects.requireNonNull(city);
        List<World.UrbanProp> assembled=new ArrayList<>();
        // The connected road graph is represented by the actual asphalt LEGO tiles,
        // not by paintings scattered arbitrarily over independent geometry.
        for(CityLayout.Road road:city.roadTiles()){
            int x=road.x(),y=road.y(),bits=road.connections();
            double cx=x*CELL+CELL/2.0,cy=y*CELL+CELL/2.0;
            if(bits==5||bits==10){
                boolean vertical=bits==5;
                for(int side=-1;side<=1;side+=2)
                    add(assembled,"road-asphalt-straight",
                        cx+(vertical?side*1.5:0),cy+(vertical?0:side*1.5),
                        vertical?0:Math.PI/2,3,0);
            }else if(Integer.bitCount(bits)==2){
                double heading=switch(bits){
                    case 3 -> 0; case 6 -> Math.PI/2;
                    case 12 -> Math.PI; case 9 -> Math.PI*1.5;
                    default -> 0;
                };
                add(assembled,"road-asphalt-corner",cx,cy,heading,3,0);
            }else{
                // T junction/crossroads/dead-end: seamless center-tile material,
                // exposed corners and pavement distinguish road connection counts.
                add(assembled,"road-asphalt-center",cx,cy,0,CELL,0);
            }
            // Road details are placed on the existing paved grid only.
            if((roll(seed,x,y,44)&15)==0)
                add(assembled,"road-asphalt-damaged",cx,cy,0,1.3,0);
        }
        for(CityLayout.Plot plot:city.plots()){
            CityLayout.Category category=plot.template().category();
            int variant=plot.template().variant();
            boolean vacant=category==CityLayout.Category.OPEN&&variant==2;
            for(int px=plot.x();px<plot.right();px++)
                for(int py=plot.y();py<plot.bottom();py++){
                    double cx=px*CELL+CELL/2.0,cy=py*CELL+CELL/2.0;
                    long n=roll(seed,px,py,91);
                    String ground=switch(category){
                        case HAZARD -> from(n,"road-dirt-center","road-dirt-pavement","road-dirt-damaged");
                        case RESOURCE -> from(n,"road-asphalt-pavement","road-dirt-center");
                        case SAFE -> from(n,"road-asphalt-pavement","road-asphalt-center");
                        case OPEN -> from(n,"grass","road-asphalt-pavement","road-dirt-center");
                    };
                    add(assembled,ground,cx,cy,0,CELL,0);
                    if(!vacant&&category!=CityLayout.Category.OPEN){
                        // Metal source roof modules form traversable-lot overhead surfaces.
                        // Height is a render offset, never an extra gameplay collider.
                        String roof=from(n>>>7,"roof-metal-type-a","roof-metal-type-b",
                            "wall-a-roof","wall-b-roof");
                        add(assembled,roof,cx,cy,((n>>>9)&1)==0?0:Math.PI/2,5.2,0,2.0);
                    }
                }
            // Kit doorway modules frame the openings already carved by CityLayout.
            // No doors are invented in blocked squares: the exact same walkable gap
            // is used by the renderer and the authoritative tile collider.
            if(category!=CityLayout.Category.OPEN){
                int x0=plot.x()*CELL,y0=plot.y()*CELL;
                int x1=plot.right()*CELL,y1=plot.bottom()*CELL;
                int midX=(x0+x1)/2,midY=(y0+y1)/2;
                int[][] entries={{midX,y0},{midX,y1-1},{x0,midY},{x1-1,midY}};
                for(int side=0;side<4;side++){
                    int ex=entries[side][0],ey=entries[side][1];
                    if(city.tile(ex,ey)!=0)continue;
                    // The open frame is an aperture, not a collision override.
                    String frame=((roll(seed,ex,ey,71)&1)==0)?"wall-a-open":"wall-b-open";
                    add(assembled,frame,ex+.5,ey+.5,side*Math.PI/2,1,0);
                }
            }
            // Lot dressing respects the existing authored plot categorization, rather
            // than randomly scattering contradictory geometry across city roads.
            double ix=(plot.x()+plot.right())*CELL/2.0,
                iy=(plot.y()+plot.bottom())*CELL/2.0;
            long n=roll(seed,plot.x(),plot.y(),187);
            String detail=switch(category){
                case HAZARD -> from(n,"scaffolding-structure","detail-bricks-type-a",
                    "detail-bricks-type-b","wall-broken-type-a","detail-cables-type-a",
                    "detail-barrier-strong-damaged","planks");
                case RESOURCE -> from(n,"pallet","pallet-small","truck-flat",
                    "detail-dumpster-closed","detail-dumpster-open",
                    "scaffolding-floor","detail-beam","planks");
                case SAFE -> from(n,"detail-bench","detail-light-single",
                    "detail-light-double","detail-awning-small",
                    "detail-awning-wide","wall-steps-type-a","detail-barrier-type-a");
                case OPEN -> from(n,"tree-small","tree-pine-small","tree-shrub",
                    "tree-large","tree-park-large","detail-bench",
                    "detail-light-traffic");
            };
            // No new obstruction without an agreed game collider: authoring the
            // visual kit is separate from changing legal movement and spawn routes.
            if(!vacant && city.tile((int)ix,(int)iy)==0 &&
               clearSpawns(ix,iy,spawns,3.0)){
                double scale=detail.contains("light")?2.5:
                    detail.contains("tree")?2.0:detail.contains("truck")?2.0:
                    detail.contains("scaffolding")?1.8:1.6;
                add(assembled,detail,ix,iy,(n&3)*Math.PI/2,scale,0);
            }
        }
        // The old invisible tile wall map is still authoritative for collision.
        // Each EXPOSED wall tile now receives an actual Kenney modular wall model;
        // stacking two 1-m modules keeps eye-level sightlines aligned.
        for(int x=1;x<World.SIZE-1;x++)for(int y=1;y<World.SIZE-1;y++){
            if(city.tile(x,y)==0)continue;
            if(city.tile(x-1,y)!=0&&city.tile(x+1,y)!=0&&
               city.tile(x,y-1)!=0&&city.tile(x,y+1)!=0)continue;
            long hash=roll(seed,x,y,251);
            boolean useB=(hash&1)!=0;
            String wall;
            if((hash&31)==0)
                wall=useB?"wall-b-corner":"wall-a-corner";
            else if((hash&15)==1)
                wall=useB?"wall-b-window":"wall-a-window";
            else if((hash&7)==2)
                wall=useB?"wall-b":"wall-a-painted";
            else
                wall=useB?"wall-b":"wall-a";
            double angle=(hash&3)*Math.PI/2;
            add(assembled,wall,x+.5,y+.5,angle,1,0);
            // Height stacking uses existing facade GLBs, not extruded proxy cubes.
            add(assembled,wall,x+.5,y+.5,angle,1,0,1.0);
        }
        // Reserve the existing spawn-near source-grounded proof props and obstacles.
        // Their explicit physical radii intentionally participate in canWalk/lineClear.
        add(assembled,"detail-barrier-type-a",5.2,1.5,0,1.7,.43);
        add(assembled,"detail-bench",4.3,2.5,0,1.8,.58);
        add(assembled,"detail-dumpster-closed",9.1,2.2,0,1.6,.59);
        add(assembled,"detail-light-single",7.8,2.2,0,3.0,.15);
        return List.copyOf(assembled);
    }
    private static boolean clearSpawns(double x,double y,double[][] spawns,double range){
        for(double[] point:spawns)if(Math.hypot(x-point[0],y-point[1])<range)return false;
        return true;
    }
}
