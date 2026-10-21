package veil;

import java.util.*;

/** Macro-only population and construction geometry gates before streaming integration. */
public final class ContinentPopulationTest {
    private static void check(boolean yes,String msg){if(!yes)throw new AssertionError(msg);}
    public static void main(String[] args) {
        for(long seed:new long[]{1,12,42,20261020L}){
            ContinentLayout layout=ContinentLayout.generate(seed);
            ContinentPopulation a=new ContinentPopulation(layout);
            ContinentPopulation b=new ContinentPopulation(layout);
            check(a.playerSpawns().equals(b.playerSpawns()),"player sites must be deterministic seed "+seed);
            check(a.npcSpawns().equals(b.npcSpawns()),"npc sites must be deterministic seed "+seed);
            check(a.playerSpawns().size()>=25,"seven cities plus wilderness sites "+seed);
            check(!a.npcSpawns().isEmpty(),"population must include NPC sites "+seed);
            for(ContinentLayout.City city:layout.cities()){
                long count=a.playerSpawns().stream().filter(site->site.city()==city.id()).count();
                check(count==3,"each city has three selectable player spawns "+city.id());
            }
            for(ContinentPopulation.PlayerSpawn spawn:a.playerSpawns()){
                check(layout.land(spawn.x(),spawn.y()),"player may not spawn at sea");
                if(spawn.kind()==ContinentPopulation.SpawnKind.CITY)
                    check(layout.road(spawn.x(),spawn.y()),"city player site must attach to road");
            }
            for(ContinentPopulation.NpcSpawn npc:a.npcSpawns()){
                check(layout.land(npc.x(),npc.y()),"NPC site must be on continent");
                check(npc.groupSize()>=1&&npc.groupSize()<=3,"NPC group has bounded size");
            }
            boolean validWilderness=false;
            for(int x=5;x<ContinentLayout.SIZE-6&&!validWilderness;x++)
                for(int y=5;y<ContinentLayout.SIZE-6&&!validWilderness;y++){
                    var review=a.reviewConstruction(x,y,2,2,1,false);
                    if(review.allowed()){
                        validWilderness=true;
                        for(int dx=-1;dx<=2;dx++)for(int dy=-1;dy<=2;dy++)
                            check(layout.cityAt(x+dx,y+dy)=='\\0',
                                "accepted construction intersects protected city buffer");
                    }
                }
            check(validWilderness,"2x2 macro building permitted on eligible wilderness land");
            int protectedCityCells=0,emptyLots=0;
            for(int x=0;x<ContinentLayout.SIZE;x++)
                for(int y=0;y<ContinentLayout.SIZE;y++){
                    if(layout.cityAt(x,y)=='\\0')continue;
                    protectedCityCells++;
                    check(layout.cityProtected(x,y)&&layout.worldStructureAt(x,y)&&
                        !layout.buildableGround(x,y),"all city territory is protected");
                    var denied=a.reviewConstruction(x,y,1,1,0,false);
                    check(!denied.allowed()&&denied.reason().equals("CITY_PROTECTED"),
                        "empty, road and occupied city parcels must reject building");
                    if(!layout.road(x,y))emptyLots++;
                }
            check(protectedCityCells>100&&emptyLots>50,"city protection must include open parcels");
            check(!a.reviewConstruction(0,0,1,1,0,false).allowed(),"ocean forbidden");
            check(!a.reviewConstruction(-1,0,1,1,0,false).allowed(),"out of bounds forbidden");
            check(!a.reviewConstruction(5,5,99,2,0,false).allowed(),"giant invalid footprints rejected");
        }
        System.out.println("PASS ContinentPopulationTest: four seeds, all city cells immutable, wilderness building permitted");
    }
}
