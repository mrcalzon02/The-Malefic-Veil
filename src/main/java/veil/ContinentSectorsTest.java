package veil;

import java.util.*;

/** Boundary, deterministic sector indexing, spawn uniqueness and interest-region checks. */
public final class ContinentSectorsTest {
    private static void check(boolean value,String reason){
        if(!value)throw new AssertionError(reason);
    }
    public static void main(String[] args){
        for(long seed:new long[]{1,42,2468,20261020}){
            ContinentLayout layout=ContinentLayout.generate(seed);
            ContinentPopulation population=new ContinentPopulation(layout);
            ContinentSectors sectors=new ContinentSectors(layout,population);
            Set<Integer> playerIds=new HashSet<>(),npcIds=new HashSet<>();
            long parcels=0;
            for(int sy=0;sy<ContinentSectors.COUNT;sy++)
                for(int sx=0;sx<ContinentSectors.COUNT;sx++){
                    var key=new ContinentSectors.Key(sx,sy);
                    var sector=sectors.sector(key);
                    check(sector==sectors.sector(key),"Cached sector identity");
                    for(var parcel:sector.parcels()){
                        parcels++;
                        check(ContinentSectors.keyFor(parcel.x(),parcel.y()).equals(key),"Parcel partition");
                        check(parcel.road()==layout.road(parcel.x(),parcel.y()),"Road mapping");
                        check(parcel.worldOwned()==layout.worldStructureAt(parcel.x(),parcel.y()),"Protected terrain");
                        check(parcel.buildable()==layout.buildableGround(parcel.x(),parcel.y()),"Buildable mask");
                    }
                    for(var spawn:sector.playerSpawns()){
                        check(playerIds.add(spawn.id()),"Duplicate player spawn marker");
                        check(ContinentSectors.keyFor(spawn.x(),spawn.y()).equals(key),"Player spawn sector");
                    }
                    for(var spawn:sector.npcSpawns()){
                        check(npcIds.add(spawn.id()),"Duplicate NPC marker");
                        check(ContinentSectors.keyFor(spawn.x(),spawn.y()).equals(key),"NPC spawn sector");
                    }
                }
            check(parcels==ContinentLayout.SIZE*ContinentLayout.SIZE,"Full map cover");
            check(playerIds.size()==population.playerSpawns().size(),"All player sites indexed");
            check(npcIds.size()==population.npcSpawns().size(),"All NPC sites indexed");
            check(sectors.interest(0,0,1).size()==4,"Coastal corner bounded interest");
            check(sectors.interest(ContinentLayout.SIZE/2,ContinentLayout.SIZE/2,1).size()==9,
                "Center adjacent subscription");
            check(sectors.interest(0,0,0).equals(List.of(new ContinentSectors.Key(0,0))),
                "Single-sector interest");
            try{sectors.interest(0,0,9);throw new AssertionError("Unbounded radius");}
            catch(IllegalArgumentException expected){}
            try{ContinentSectors.keyFor(-1,0);throw new AssertionError("Negative coordinate");}
            catch(IllegalArgumentException expected){}
        }
        System.out.println("PASS ContinentSectorsTest: full partition, world masks, spawn indexing, bounds");
    }
}
