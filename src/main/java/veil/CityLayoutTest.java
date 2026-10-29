package veil;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Deterministic plot library, road graph, tiling and collision connectivity acceptance. */
public final class CityLayoutTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static boolean allFloorsConnected(CityLayout map){
        int n=World.SIZE;
        boolean[][] seen=new boolean[n][n];
        ArrayDeque<Integer> queue=new ArrayDeque<>();
        queue.add(n+1);seen[1][1]=true;
        int[] dx={1,-1,0,0},dy={0,0,1,-1};
        while(!queue.isEmpty()){
            int p=queue.removeFirst(),x=p/n,y=p%n;
            for(int i=0;i<4;i++){
                int nx=x+dx[i],ny=y+dy[i];
                if(nx<1||ny<1||nx>=n-1||ny>=n-1||seen[nx][ny]||map.tile(nx,ny)!=0)continue;
                seen[nx][ny]=true;queue.addLast(nx*n+ny);
            }
        }
        for(int x=1;x<n-1;x++)for(int y=1;y<n-1;y++)
            if(map.tile(x,y)==0&&!seen[x][y])return false;
        return true;
    }
    private static void roadsConnected(CityLayout map){
        Set<Integer> encountered=new HashSet<>();
        ArrayDeque<Integer> queue=new ArrayDeque<>();
        CityLayout.Road first=map.roadTiles().get(0);
        queue.add(first.x()*CityLayout.GRID+first.y());
        while(!queue.isEmpty()){
            int p=queue.removeFirst(),x=p/CityLayout.GRID,y=p%CityLayout.GRID;
            if(!encountered.add(p))continue;
            int[][] neighbors={{x-1,y},{x+1,y},{x,y-1},{x,y+1}};
            for(int[] q:neighbors)if(map.road(q[0],q[1]))queue.add(q[0]*CityLayout.GRID+q[1]);
        }
        check(encountered.size()==map.roadTiles().size(),"disconnected road graph");
    }
    public static void main(String[] unused){
        check(CityLayout.templateCount()>=100,"plot library must offer at least 100 authored definitions");
        for(CityLayout.Category type:CityLayout.Category.values())
            check(CityLayout.templates(new CityLayout.Footprint(2,2),type).size()>=3,
                "missing 2x2 category variants: "+type);
        long[] seeds={0,1,-1,42,567,20261020L,Long.MIN_VALUE,Long.MAX_VALUE};
        Set<String> maps=new HashSet<>();
        for(long seed:seeds){
            CityLayout map=CityLayout.generate(seed),same=CityLayout.generate(seed);
            check(map.plots().equals(same.plots())&&map.roadTiles().equals(same.roadTiles()),"nondeterministic placements");
            boolean[][] occupied=new boolean[CityLayout.GRID][CityLayout.GRID];
            int standard=0;
            Set<CityLayout.Category> types=new HashSet<>();
            for(CityLayout.Road road:map.roadTiles()){
                check(!occupied[road.x()][road.y()],"road duplicated");
                occupied[road.x()][road.y()]=true;
            }
            for(CityLayout.Plot lot:map.plots()){
                CityLayout.Template part=lot.template();
                if(part.footprint().equals(new CityLayout.Footprint(2,2))){standard++;types.add(part.category());}
                for(int x=lot.x();x<lot.right();x++)for(int y=lot.y();y<lot.bottom();y++){
                    check(!occupied[x][y],"overlap at "+x+","+y);
                    occupied[x][y]=true;
                    check(map.categoryAt(x*CityLayout.CELL,y*CityLayout.CELL)==part.category(),"incorrect plot tagging");
                }
            }
            for(int x=0;x<CityLayout.GRID;x++)for(int y=0;y<CityLayout.GRID;y++)
                check(occupied[x][y],"unassigned map cell");
            check(standard>=4,"not enough standard 2x2 lots");
            check(types.size()==4,"2x2 layouts lack all four playable categories");
            for(int x=0;x<World.SIZE;x++)for(int y=0;y<World.SIZE;y++)
                check(map.tile(x,y)==same.tile(x,y),"non-deterministic walls");
            // The entire 132x132 city, including visually vacant/ruined/open
            // parcels, is world-owned. Walkable never means constructible.
            for(int x=1;x<World.SIZE-1;x++)for(int y=1;y<World.SIZE-1;y++)
                check(map.worldStructureAt(x+.5,y+.5,.20),
                    "city parcel incorrectly buildable at "+x+","+y+" seed "+seed);
            for(CityLayout.Plot lot:map.plots())
                if(lot.template().category()==CityLayout.Category.OPEN&&
                   lot.template().variant()==2){
                    double cx=(lot.x()+lot.right())*CityLayout.CELL/2.0;
                    double cy=(lot.y()+lot.bottom())*CityLayout.CELL/2.0;
                    check(map.worldStructureAt(cx,cy,1.5),
                        "empty lot cannot be player construction land");
                }
            check(allFloorsConnected(map),"disconnected walkable area");
            roadsConnected(map);
            for(int i=0;i<World.SIZE;i++){
                check(map.tile(i,0)==1&&map.tile(0,i)==1,"map border not closed");
                check(map.tile(i,World.SIZE-1)==1&&map.tile(World.SIZE-1,i)==1,"map border not closed");
            }
            maps.add(map.roadTiles().toString());
            System.out.println("PASS plot seed "+seed+": "+map.roadTiles().size()+" road tiles, "+map.plots().size()+" plots, "+standard+" standard lots");
        }
        check(maps.size()>=5,"road layouts insufficiently varied");
        System.out.println("PASS CITY LAYOUT: "+CityLayout.templateCount()+" variants; seeded tiling, graph, and connectivity");
    }
}
