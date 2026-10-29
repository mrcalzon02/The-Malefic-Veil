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
            // Every world tile has exactly one licensed Kenney road surface.
            // Pavement is placed on *external* edges, never across connected
            // road exits: route continuity is derived from the road graph.
            for(int sx=0;sx<CELL;sx++)for(int sy=0;sy<CELL;sy++){
                int tx=x*CELL+sx,ty=y*CELL+sy;
                boolean outerWest=sx==0&&(bits&8)==0;
                boolean outerEast=sx==CELL-1&&(bits&2)==0;
                boolean outerNorth=sy==0&&(bits&1)==0;
                boolean outerSouth=sy==CELL-1&&(bits&4)==0;
                boolean curb=outerWest||outerEast||outerNorth||outerSouth;
                long n=roll(seed,tx,ty,44);
                String ground=curb?"road-asphalt-pavement":
                    (n&31)==0?"road-asphalt-damaged":"road-asphalt-center";
                add(assembled,ground,tx+.5,ty+.5,0,1,0);
            }
            // Lit crossings are actual source fixtures along the sidewalk,
            // never invented emissive cubes. Fixtures are visual while the
            // original road/plot map remains the sole collision authority.
            long street=roll(seed,x,y,238);
            if((street&3)==0){
                int sx=(street&8)==0?0:CELL-1;
                int sy=(street&16)==0?0:CELL-1;
                double lx=x*CELL+sx+.5,ly=y*CELL+sy+.5;
                if(clearSpawns(lx,ly,spawns,4))
                    add(assembled,(street&32)==0?"detail-light-single":"detail-light-double",
                        lx,ly,((street>>>6)&3)*Math.PI/2,1.35,0);
            }
            if(Integer.bitCount(bits)>=3&&(street&7)==5){
                double ax=x*CELL+.5,ay=y*CELL+CELL-1.5;
                if(clearSpawns(ax,ay,spawns,4))
                    add(assembled,"detail-barrier-type-a",ax,ay,Math.PI/2,0.9,0);
            }
        }
        for(CityLayout.Plot plot:city.plots()){
            CityLayout.Category category=plot.template().category();
            int variant=plot.template().variant();
            boolean vacant=category==CityLayout.Category.OPEN&&variant==2;
            for(int mx=plot.x();mx<plot.right();mx++)
                for(int my=plot.y();my<plot.bottom();my++){
                    for(int sx=0;sx<CELL;sx++)for(int sy=0;sy<CELL;sy++){
                        int tx=mx*CELL+sx,ty=my*CELL+sy;
                        long n=roll(seed,tx,ty,91);
                        // Plot archetypes own their surface palette: the whole
                        // block reads as a court, park, yard or building lot.
                        // Rare scars interrupt that base; never random checkerboard.
                        String ground=switch(category){
                            case HAZARD -> (n&31)==0?"road-dirt-damaged":"road-dirt-center";
                            case RESOURCE -> (variant==3||variant==4||variant==7)
                                ?"road-asphalt-pavement":"road-dirt-center";
                            case SAFE -> (n&63)==0?"road-asphalt-damaged":"road-asphalt-pavement";
                            case OPEN -> switch(variant){
                                case 0,1,4,6 -> "road-asphalt-pavement";
                                case 3,7 -> "grass";
                                case 2,5 -> (n&15)==0?"grass":"road-dirt-center";
                                default -> "grass";
                            };
                        };
                        add(assembled,ground,tx+.5,ty+.5,0,1,0);
                        // A 6x6 macrocell represents building footprint, NOT
                        // 36 dollhouse-sized roof sections. One licensed mesh
                        // covers a six-metre span at the second-storey roofline.
                        boolean roofIntact=category==CityLayout.Category.SAFE||
                            category==CityLayout.Category.RESOURCE&&(n&7)!=0||
                            category==CityLayout.Category.HAZARD&&(n&3)==0;
                        if(sx==0&&sy==0&&!vacant&&
                           category!=CityLayout.Category.OPEN&&roofIntact&&
                           city.tile(tx+CELL/2,ty+CELL/2)==0){
                            String roof=from(n>>>7,"roof-metal-type-a","roof-metal-type-b",
                                "wall-b-roof");
                            add(assembled,roof,tx+CELL/2.0,ty+CELL/2.0,
                                ((n>>>9)&1)==0?0:Math.PI/2,CELL,0,6.0);
                        }
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
                    // Source wall-open modules are metre-sized; an entrance
                    // must be large enough for an adult, not a doll.
                    add(assembled,frame,ex+.5,ey+.5,side*Math.PI/2,3.0,0);
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
            // Dressed plots get a small reproducible composition, not just
            // one prop in the middle of an otherwise empty 12x12 parcel.
            int area=(plot.right()-plot.x())*(plot.bottom()-plot.y());
            int desired=Math.max(1,Math.min(5,area+1));
            int minX=plot.x()*CELL+1,maxX=plot.right()*CELL-2;
            int minY=plot.y()*CELL+1,maxY=plot.bottom()*CELL-2;
            for(int piece=0;piece<desired&&!vacant;piece++){
                long local=roll(seed,plot.x(),plot.y(),187+piece*19);
                int dx=Math.max(1,maxX-minX+1),dy=Math.max(1,maxY-minY+1);
                double ox=minX+Math.floorMod(local,dx)+.5;
                double oy=minY+Math.floorMod(local>>>17,dy)+.5;
                if(city.tile((int)ox,(int)oy)!=0||!clearSpawns(ox,oy,spawns,3.0))continue;
                String model=piece==0?detail:switch(category){
                    case HAZARD -> from(local,"detail-bricks-type-a","planks",
                        "detail-cables-type-b","detail-barrier-strong-damaged");
                    case RESOURCE -> from(local,"pallet","pallet-small",
                        "detail-beam","detail-dumpster-closed","scaffolding-poles");
                    case SAFE -> from(local,"detail-bench","detail-light-single",
                        "detail-barrier-type-b","detail-awning-small");
                    case OPEN -> from(local,"tree-small","tree-shrub",
                        "detail-bench","tree-pine-small");
                };
                double scale=model.contains("light")?3.0:
                    model.contains("tree")?2.1:
                    model.contains("truck")?1.5:
                    model.contains("detail-bench")?3.2:
                    model.contains("dumpster")?2.4:
                    model.contains("barrier")?2.2:
                    model.contains("scaffolding")?2.0:1.6;
                add(assembled,model,ox,oy,(local&3)*Math.PI/2,scale,0);
            }
        }
        // One collision tile remains one metre. The licensed thin wall-type-b
        // façade is 1 m wide, 0.5 m tall and 0.07 m deep at native scale.
        // A single 6x section is therefore six metres wide, one THREE-METRE
        // storey high and only 0.42 m deep. The former per-metre wall cubes
        // made the player Godzilla-sized and violated walkable visual space.
        // Host collider/LOS stays immutable; openings in that authoritative
        // grid are NOT filled by stretching a façade over a doorway.
        for(int x=1;x<World.SIZE-CELL;x++)for(int y=1;y<World.SIZE-CELL;y++){
            if(city.tile(x,y)==0)continue;
            boolean north=city.tile(x,y-1)==0;
            boolean south=city.tile(x,y+1)==0;
            boolean west=city.tile(x-1,y)==0;
            boolean east=city.tile(x+1,y)==0;
            if(!north&&!south&&!west&&!east)continue;
            if((north||south)&&x%CELL==0){
                boolean uninterrupted=true;
                for(int offset=1;offset<CELL;offset++)
                    if(city.tile(x+offset,y)==0){uninterrupted=false;break;}
                if(uninterrupted){
                    add(assembled,"wall-type-b",x+CELL/2.0,y+.5,0,CELL,0);
                    add(assembled,"wall-type-b",x+CELL/2.0,y+.5,0,CELL,0,3.0);
                }
            }
            if((west||east)&&y%CELL==0){
                boolean uninterrupted=true;
                for(int offset=1;offset<CELL;offset++)
                    if(city.tile(x,y+offset)==0){uninterrupted=false;break;}
                if(uninterrupted){
                    add(assembled,"wall-type-b",x+.5,y+CELL/2.0,
                        Math.PI/2,CELL,0);
                    add(assembled,"wall-type-b",x+.5,y+CELL/2.0,
                        Math.PI/2,CELL,0,3.0);
                }
            }
        }
        // Distinct source-backed obstacles remain physical and co-located;
        // increase their visuals to adult-height street furniture.
        add(assembled,"detail-barrier-type-a",5.2,1.5,0,2.4,.60);
        add(assembled,"detail-bench",4.3,2.5,0,3.2,.74);
        add(assembled,"detail-dumpster-closed",9.1,2.2,0,2.4,.78);
        add(assembled,"detail-light-single",7.8,2.2,0,3.0,.20);
        return List.copyOf(assembled);
    }
    private static boolean clearSpawns(double x,double y,double[][] spawns,double range){
        for(double[] point:spawns)if(Math.hypot(x-point[0],y-point[1])<range)return false;
        return true;
    }
}
