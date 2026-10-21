package veil;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/** Pure Java 17 regression tests for continental topology (no GPU). */
public final class ContinentLayoutTest {
    private static void check(boolean yes,String detail){if(!yes)throw new AssertionError(detail);}
    private static void checkConnected(ContinentLayout m){
        boolean[][] seen=new boolean[ContinentLayout.SIZE][ContinentLayout.SIZE];
        ArrayDeque<int[]> q=new ArrayDeque<>();
        var a=m.cities().get(0);q.add(new int[]{a.x(),a.y()});seen[a.x()][a.y()]=true;
        int[] dx={1,-1,0,0},dy={0,0,1,-1};
        while(!q.isEmpty()){
            int[] p=q.removeFirst();
            for(int i=0;i<4;i++){
                int x=p[0]+dx[i],y=p[1]+dy[i];
                if(m.inBounds(x,y)&&!seen[x][y]&&m.road(x,y)){
                    seen[x][y]=true;q.addLast(new int[]{x,y});
                }
            }
        }
        for(var city:m.cities())check(seen[city.x()][city.y()],"city disconnected "+city.id());
    }
    public static void main(String[] args){
        long[] seeds={0,1,42,20261020,-1,Long.MAX_VALUE};
        Set<String> footprints=new HashSet<>();
        for(long seed:seeds){
            ContinentLayout map=ContinentLayout.generate(seed),same=ContinentLayout.generate(seed);
            check(map.cities().size()==7,"missing cities");
            check(map.cities().get(2).shape()==ContinentLayout.SiteShape.SQUARE,"C must be rectangular");
            check(map.routes().size()>=6,"intercity road spanning tree missing");
            check(map.cities().equals(same.cities())&&map.lots().equals(same.lots())&&
                map.routes().equals(same.routes()),"non-deterministic blueprint");
            int land=0,water=0,buildable=0;
            EnumSet<ContinentLayout.Biome> observed=EnumSet.noneOf(ContinentLayout.Biome.class);
            boolean[][] occupied=new boolean[ContinentLayout.SIZE][ContinentLayout.SIZE];
            for(int x=0;x<ContinentLayout.SIZE;x++)for(int y=0;y<ContinentLayout.SIZE;y++){
                observed.add(map.biome(x,y));
                check(map.biome(x,y)==same.biome(x,y)&&map.cityAt(x,y)==same.cityAt(x,y)&&
                    map.road(x,y)==same.road(x,y)&&map.worldStructureAt(x,y)==same.worldStructureAt(x,y),"map changed across same seed");
                if(map.land(x,y))land++;else water++;
                if(map.buildableGround(x,y))buildable++;
                check(!map.road(x,y)||map.land(x,y),"road drowned");
                check(!map.road(x,y)||map.worldStructureAt(x,y),"road not protected");
                if(map.cityAt(x,y)!='\0')check(map.land(x,y),"city off shore");
            }
            check(map.biome(0,0)==ContinentLayout.Biome.WATER,"not surrounded by water");
            check(land>15000&&water>6000,"bad coastline balance");
            check(buildable>1000,"players have nowhere to build");
            check(observed.containsAll(EnumSet.allOf(ContinentLayout.Biome.class)),"missing terrain classes "+observed);
            for(var lot:map.lots())for(int x=lot.x();x<lot.x()+lot.width();x++)
                for(int y=lot.y();y<lot.y()+lot.height();y++){
                    check(!occupied[x][y],"overlapping lots");occupied[x][y]=true;
                    check(map.cityAt(x,y)==lot.city(),"plot outside city");
                    check(!map.road(x,y),"lot overlaps road");
                    if(lot.developed())check(map.worldStructureAt(x,y),"world lot not immutable");
                }
            for(int x=0;x<ContinentLayout.SIZE;x++)for(int y=0;y<ContinentLayout.SIZE;y++)
                if(map.cityAt(x,y)!='\0'&&!map.road(x,y))check(occupied[x][y],"untiled city cell");
            for(var city:map.cities()){
                EnumSet<ContinentLayout.Zone> zones=EnumSet.noneOf(ContinentLayout.Zone.class);
                for(var lot:map.lots())if(lot.city()==city.id()&&lot.width()==2&&lot.height()==2)
                    zones.add(lot.zone());
                check(zones.size()==4,"2x2 lot categories missing in "+city.id());
            }
            checkConnected(map);
            footprints.add(map.cities().toString());
            System.out.println("PASS continent seed "+seed+" land="+land+" routes="+map.routes().size()+" lots="+map.lots().size());
        }
        check(footprints.size()==seeds.length,"city sites do not vary with seed");
        // Broad procedural stress must catch seed-specific route and city-site fragmentation.
        for(int i=0;i<100;i++){
            long candidate=(i*0x9e3779b97f4a7c15L)^0x4c8fbd23;
            ContinentLayout stress=ContinentLayout.generate(candidate);
            check(stress.cities().size()==7&&stress.routes().size()>=6,
                "continent topology collapsed for seed "+candidate);
            checkConnected(stress);
            int usable=0;
            for(int x=0;x<ContinentLayout.SIZE;x++)
                for(int y=0;y<ContinentLayout.SIZE;y++)
                    if(stress.buildableGround(x,y))usable++;
            check(usable>1000,"no buildable wilderness for seed "+candidate);
        }
        System.out.println("CONTINENT GENERATION GATE PASS (6 exact invariants + 100 stress seeds)");
    }
}
