package veil;

import java.util.*;

/**
 * Seed-stable continental spawn/encounter and parcel-build authority.
 * All locations are 224x224 MACRO cells, not client city-sector coordinates.
 * Runtime materialization occurs only when streamed sectors are implemented.
 */
public final class ContinentPopulation {
    public enum SpawnKind { CITY, ROAD_OUTPOST, WILDERNESS }
    public enum EncounterKind { CITY_SCAVENGER, ROAD_AMBUSH, FOREST_HOSTILE, MARSH_HOSTILE, MOUNTAIN_HOSTILE }
    public record PlayerSpawn(int id,int x,int y,char city,SpawnKind kind) {}
    public record NpcSpawn(int id,int x,int y,EncounterKind kind,int groupSize) {}
    public record BuildCheck(boolean allowed,String reason) {}
    private final ContinentLayout world;
    private final List<PlayerSpawn> players=new ArrayList<>();
    private final List<NpcSpawn> npcs=new ArrayList<>();
    public ContinentPopulation(ContinentLayout world){
        this.world=Objects.requireNonNull(world);
        createPlayerSites();createNpcs();
    }
    public List<PlayerSpawn> playerSpawns(){return List.copyOf(players);}
    public List<NpcSpawn> npcSpawns(){return List.copyOf(npcs);}
    private long roll(int x,int y,int salt){
        return World.mix(world.seed()^((long)x*0x9e3779b97f4a7c15L)^
            ((long)y*0x632be59bd9b4e019L)^(salt*7919L));
    }
    private boolean farEnough(int x,int y,int min){
        for(PlayerSpawn site:players)
            if(Math.hypot(site.x()-x,site.y()-y)<min)return false;
        return true;
    }
    private void createPlayerSites(){
        for(ContinentLayout.City city:world.cities()){
            List<int[]> options=new ArrayList<>();
            int radius=Math.max(city.radiusX(),city.radiusY());
            for(int x=Math.max(1,city.x()-radius);x<Math.min(ContinentLayout.SIZE-1,city.x()+radius);x++)
                for(int y=Math.max(1,city.y()-radius);y<Math.min(ContinentLayout.SIZE-1,city.y()+radius);y++)
                    if(world.cityAt(x,y)==city.id()&&world.road(x,y)&&world.land(x,y))
                        options.add(new int[]{x,y});
            options.sort((a,b)->{
                long ra=roll(a[0],a[1],1),rb=roll(b[0],b[1],1);
                int r=Long.compareUnsigned(ra,rb);
                return r!=0?r:Integer.compare(a[0]*ContinentLayout.SIZE+a[1],
                    b[0]*ContinentLayout.SIZE+b[1]);
            });
            int added=0;
            for(int[] site:options){
                if(!farEnough(site[0],site[1],5))continue;
                players.add(new PlayerSpawn(players.size(),site[0],site[1],city.id(),SpawnKind.CITY));
                if(++added==3)break;
            }
            if(added<3)throw new IllegalStateException("Insufficient city spawn routes: "+city.id());
        }
        // Scattered wilderness rally points on buildable ground near, not over, highways.
        List<int[]> wild=new ArrayList<>();
        for(int x=4;x<ContinentLayout.SIZE-4;x++)for(int y=4;y<ContinentLayout.SIZE-4;y++){
            if(!world.buildableGround(x,y)||world.cityAt(x,y)!=0)continue;
            if(world.biome(x,y)!=ContinentLayout.Biome.PLAINS&&
               world.biome(x,y)!=ContinentLayout.Biome.FOREST)continue;
            if(Math.floorMod(roll(x,y,2),200)!=0)continue;
            int minRoad=99;
            for(int dx=-6;dx<=6;dx++)for(int dy=-6;dy<=6;dy++)
                if(world.road(x+dx,y+dy))minRoad=Math.min(minRoad,Math.abs(dx)+Math.abs(dy));
            if(minRoad<2||minRoad>8)continue;
            wild.add(new int[]{x,y});
        }
        wild.sort((a,b)->Long.compareUnsigned(roll(a[0],a[1],3),roll(b[0],b[1],3)));
        for(int[] site:wild){
            if(!farEnough(site[0],site[1],12))continue;
            players.add(new PlayerSpawn(players.size(),site[0],site[1],'\0',SpawnKind.WILDERNESS));
            if(players.size()>=35)break;
        }
        if(players.size()<25)throw new IllegalStateException("Too few continental player rally points");
    }
    private void createNpcs(){
        for(int x=3;x<ContinentLayout.SIZE-3;x++)for(int y=3;y<ContinentLayout.SIZE-3;y++){
            if(!world.land(x,y))continue;
            boolean road=world.road(x,y);char city=world.cityAt(x,y);
            int frequency=city!=0&&road?31:road?53:world.biome(x,y)==ContinentLayout.Biome.FOREST?88:123;
            if(Math.floorMod(roll(x,y,11),frequency)!=0)continue;
            if(world.worldStructureAt(x,y)&&!road)continue;
            EncounterKind type;
            if(city!=0){if(!road)continue;type=EncounterKind.CITY_SCAVENGER;}
            else if(road)type=EncounterKind.ROAD_AMBUSH;
            else type=switch(world.biome(x,y)){
                case FOREST -> EncounterKind.FOREST_HOSTILE;
                case MARSH -> EncounterKind.MARSH_HOSTILE;
                case HILLS,MOUNTAINS -> EncounterKind.MOUNTAIN_HOSTILE;
                default -> EncounterKind.FOREST_HOSTILE;
            };
            boolean protectedSpawn=false;
            for(PlayerSpawn site:players)
                if(Math.abs(x-site.x())+Math.abs(y-site.y())<6){protectedSpawn=true;break;}
            if(protectedSpawn)continue;
            npcs.add(new NpcSpawn(npcs.size(),x,y,type,1+(int)Math.floorMod(roll(x,y,17),3)));
            if(npcs.size()>=650)return;
        }
    }
    /** Full rotated macro rectangular footprints; rejects any generated world geometry. */
    public BuildCheck reviewConstruction(int x,int y,int width,int height,int clearance,boolean rotated){
        if(width<1||height<1||width>16||height>16||clearance<0||clearance>6)
            return new BuildCheck(false,"INVALID_FOOTPRINT");
        int w=rotated?height:width,h=rotated?width:height;
        if(x<0||y<0||x+w>ContinentLayout.SIZE||y+h>ContinentLayout.SIZE)
            return new BuildCheck(false,"OUT_OF_BOUNDS");
        for(int px=x-clearance;px<x+w+clearance;px++)
            for(int py=y-clearance;py<y+h+clearance;py++){
                if(!world.land(px,py))return new BuildCheck(false,"WATER");
                if(world.worldStructureAt(px,py))return new BuildCheck(false,"WORLD_STRUCTURE");
                if(!world.buildableGround(px,py))return new BuildCheck(false,"TERRAIN");
                for(PlayerSpawn site:players)
                    if(Math.abs(px-site.x())<=2&&Math.abs(py-site.y())<=2)
                        return new BuildCheck(false,"SPAWN_CLEARANCE");
            }
        return new BuildCheck(true,"OK");
    }
}
