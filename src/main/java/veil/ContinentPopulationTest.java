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
            boolean validBuild=false;
            for(int x=5;x<ContinentLayout.SIZE-6&&!validBuild;x++)
                for(int y=5;y<ContinentLayout.SIZE-6&&!validBuild;y++)
                    validBuild=a.reviewConstruction(x,y,2,2,1,false).allowed();
            check(validBuild,"at least one legal 2x2 protected macro construction footprint");
            for(ContinentLayout.City city:layout.cities()){
                var denied=a.reviewConstruction(city.x(),city.y(),1,1,0,false);
                check(!denied.allowed(),"generated city center road cannot be overwritten");
            }
            check(!a.reviewConstruction(0,0,1,1,0,false).allowed(),"ocean forbidden");
            check(!a.reviewConstruction(-1,0,1,1,0,false).allowed(),"out of bounds forbidden");
            check(!a.reviewConstruction(5,5,99,2,0,false).allowed(),"giant invalid footprints rejected");
        }
        System.out.println("PASS ContinentPopulationTest: 4 seeds, city and wilderness spawns, NPCs and build masks");
    }
}
