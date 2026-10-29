package veil;

import java.util.*;

/**
 * Immutable macro-only spawn plans for discoverable road vehicles. Not active
 * arena cars: streamed sectors, fine collision and spawn tombstones are gated.
 */
public final class ContinentVehicleSites {
    public enum Kind { CITY_PARKING, HIGHWAY_SHOULDER }
    public enum Condition { WORKING, DISABLED, WRECKED }
    public record Site(int id,int x,int y,int roadX,int roadY,char city,
                       Kind kind,int sourceCar,Condition condition,
                       int hp,int fuel,int cargoSlots,String markings) {}
    private record Candidate(int x,int y,int rx,int ry,char city,Kind kind) {}
    private static final int[] DX={1,-1,0,0},DY={0,0,1,-1};
    private final ContinentLayout map;
    private final List<ContinentPopulation.PlayerSpawn> players;
    private final List<ContinentPopulation.NpcSpawn> npcs;
    private final List<Site> sites=new ArrayList<>();
    public ContinentVehicleSites(ContinentLayout layout,
            List<ContinentPopulation.PlayerSpawn> playerSites,
            List<ContinentPopulation.NpcSpawn> npcSites) {
        map=Objects.requireNonNull(layout);
        players=List.copyOf(playerSites);
        npcs=List.copyOf(npcSites);
        generate();
    }
    public List<Site> sites(){return List.copyOf(sites);}
    private long roll(int x,int y,long salt){
        return World.mix(map.seed()^salt^((long)x*0x9e3779b97f4a7c15L)^
                ((long)y*0x632be59bd9b4e019L));
    }
    /** Park off the roadway with cardinal access to a road of the same region. */
    private Candidate candidate(int x,int y,char city,Kind kind){
        for(int i=0;i<4;i++){
            int rx=x+DX[i],ry=y+DY[i];
            if(!map.road(rx,ry))continue;
            if(kind==Kind.CITY_PARKING&&map.cityAt(rx,ry)!=city)continue;
            if(kind==Kind.HIGHWAY_SHOULDER&&map.cityAt(rx,ry)!=0)continue;
            return new Candidate(x,y,rx,ry,city,kind);
        }
        return null;
    }
    private boolean clear(Candidate at,int separation){
        if(!map.land(at.x(),at.y())||map.road(at.x(),at.y()))return false;
        for(var p:players)if(Math.abs(p.x()-at.x())+Math.abs(p.y()-at.y())<6)return false;
        for(var n:npcs)if(Math.abs(n.x()-at.x())+Math.abs(n.y()-at.y())<3)return false;
        for(Site s:sites)
            if(Math.abs(s.x()-at.x())+Math.abs(s.y()-at.y())<separation)return false;
        return true;
    }
    private void select(List<Candidate> pool,int limit,int separation,long salt){
        pool.sort((a,b)->{
            int c=Long.compareUnsigned(roll(a.x(),a.y(),salt),roll(b.x(),b.y(),salt));
            if(c!=0)return c;
            c=Integer.compare(a.x(),b.x());
            return c!=0?c:Integer.compare(a.y(),b.y());
        });
        int count=0;
        for(Candidate p:pool){
            if(count>=limit)break;
            if(!clear(p,separation))continue;
            long r=roll(p.x(),p.y(),0x56454849434c4553L);
            int pick=(int)Math.floorMod(r,24);
            int model=pick==0?6:pick<4?8:pick<7?4:pick<11?1:
                pick<15?2:pick<19?3:pick<22?5:7;
            int conditionRoll=(int)Math.floorMod(r>>>11,10);
            Condition condition=model==6||conditionRoll>=8?Condition.WRECKED:
                conditionRoll<2?Condition.WORKING:Condition.DISABLED;
            int hp=condition==Condition.WRECKED?0:
                condition==Condition.WORKING?125+(int)Math.floorMod(r>>>17,76):
                25+(int)Math.floorMod(r>>>17,75);
            int fuel=condition==Condition.WORKING?8+(int)Math.floorMod(r>>>29,53):0;
            int slots=model==8?10:model==1||model==4?6:
                model==3||model==7?2:4;
            int emblem=(int)Math.floorMod(r>>>38,16);
            String markings=emblem<10?"NEUTRAL":emblem<12?"UNRAVELED":
                emblem<14?"PENITENT":"BEGOTTEN";
            sites.add(new Site(sites.size(),p.x(),p.y(),p.rx(),p.ry(),p.city(),
                p.kind(),model,condition,hp,fuel,slots,markings));
            count++;
        }
    }
    private void generate(){
        for(ContinentLayout.City city:map.cities()){
            List<Candidate> options=new ArrayList<>();
            for(ContinentLayout.Lot lot:map.lots()){
                if(lot.city()!=city.id()||lot.developed()||
                   lot.zone()!=ContinentLayout.Zone.OPEN)continue;
                for(int x=lot.x();x<lot.x()+lot.width();x++)
                    for(int y=lot.y();y<lot.y()+lot.height();y++){
                        Candidate p=candidate(x,y,city.id(),Kind.CITY_PARKING);
                        if(p!=null)options.add(p);
                    }
            }
            select(options,6,5,0x434954595041524bL+city.id());
        }
        List<Candidate> shoulders=new ArrayList<>();
        for(int x=2;x<ContinentLayout.SIZE-2;x++)
            for(int y=2;y<ContinentLayout.SIZE-2;y++){
                if(map.cityAt(x,y)!=0||!map.buildableGround(x,y))continue;
                if(map.biome(x,y)!=ContinentLayout.Biome.PLAINS&&
                   map.biome(x,y)!=ContinentLayout.Biome.FOREST)continue;
                Candidate p=candidate(x,y,'\0',Kind.HIGHWAY_SHOULDER);
                if(p!=null)shoulders.add(p);
            }
        select(shoulders,35,6,0x53484f554c444552L);
    }
}
