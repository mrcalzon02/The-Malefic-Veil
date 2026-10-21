package veil;

import java.util.*;
import java.io.*;
import java.nio.file.*;

/**
 * Live, host-owned wilderness expedition. A separate playable continental
 * coordinate space (macro parcels, not city tiles) shares the player's REAL
 * survival inventory. Clients never specify pickup coordinates or rewards.
 */
public final class WildernessExpedition {
    public record Position(double x,double y,double angle,long lastMove) {}
    public record Harvest(boolean accepted,String reason,WildernessResources.Node node,
                          SurvivalCrafting.Item item) {}
    private final ContinentLayout map;
    public final WildernessResources resources;
    private final Map<Integer,Position> positions=new HashMap<>();
    private final Map<Integer,Long> lastHarvest=new HashMap<>();
    private long clock;
    private final double[] start;
    public WildernessExpedition(ContinentLayout map){
        this.map=Objects.requireNonNull(map);
        resources=new WildernessResources(map);
        double[] found=null;
        // Begin in genuine reachable wilderness, within arm's reach of a
        // resource; never teleport into a city plot, sea or world structure.
        for(var source:resources.nodes()){
            if(source.type()!=WildernessResources.Type.FIBER&&
               source.type()!=WildernessResources.Type.TIMBER)continue;
            for(int dx=-1;dx<=1&&found==null;dx++)
                for(int dy=-1;dy<=1&&found==null;dy++){
                    double x=source.x()+.5+dx*.9,y=source.y()+.5+dy*.9;
                    if(Math.hypot(dx,dy)<.5||!walkable(x,y))continue;
                    double angle=Math.atan2(source.y()+.5-y,source.x()+.5-x);
                    found=new double[]{x,y,angle};
                }
            if(found!=null)break;
        }
        if(found==null)throw new IllegalStateException("No playable gathering ground");
        start=found;
    }
    public synchronized Position enter(int playerId,ArenaSystems.Player player){
        if(player==null||player.dead)return null;
        return positions.computeIfAbsent(playerId,k->
            new Position(start[0],start[1],start[2],clock));
    }
    public synchronized void leave(int playerId){
        positions.remove(playerId);lastHarvest.remove(playerId);
    }
    public synchronized boolean active(int playerId){return positions.containsKey(playerId);}
    public synchronized Position position(int playerId){return positions.get(playerId);}
    public boolean walkable(double x,double y){
        if(!Double.isFinite(x)||!Double.isFinite(y))return false;
        int tx=(int)Math.floor(x),ty=(int)Math.floor(y);
        if(!map.inBounds(tx,ty)||!map.land(tx,ty)||map.cityProtected(tx,ty)||
           map.worldStructureAt(tx,ty))return false;
        // Ground players may traverse roads; only aerial travel is miasma-limited.
        return true;
    }
    private boolean sight(double ax,double ay,double bx,double by){
        for(int i=1;i<=12;i++){
            double x=ax+(bx-ax)*i/12.,y=ay+(by-ay)*i/12.;
            if(!walkable(x,y))return false;
        }
        return true;
    }
    public synchronized boolean move(int playerId,ArenaSystems.Player player,
                         double x,double y,double angle,int flags){
        Position old=positions.get(playerId);
        if(old==null||player==null||player.dead||!Double.isFinite(angle)||
           !walkable(x,y))return false;
        double distance=Math.hypot(x-old.x(),y-old.y());
        long elapsed=Math.min(500,Math.max(0,clock-old.lastMove()));
        double speed=(flags&1)!=0?1.20:(flags&2)!=0?4.30:2.85;
        double permitted=.33+speed*elapsed/1000.;
        if(distance>permitted)return false;
        for(int i=1;i<=Math.max(1,(int)Math.ceil(distance/.12));i++){
            double fraction=i/(double)Math.max(1,(int)Math.ceil(distance/.12));
            if(!walkable(old.x()+(x-old.x())*fraction,
                         old.y()+(y-old.y())*fraction))return false;
        }
        positions.put(playerId,new Position(x,y,angle,clock));
        return true;
    }
    private InteractionFocus.Target focused(Position player){
        List<InteractionFocus.Candidate> near=new ArrayList<>();
        for(var n:resources.nodes()){
            if(n.remaining()<=0||Math.hypot(n.x()+.5-player.x(),n.y()+.5-player.y())>2.6)
                continue;
            near.add(new InteractionFocus.Candidate(InteractionFocus.Kind.RESOURCE,
                n.id(),n.type().name().replace('_',' '),
                n.x()+.5,n.y()+.5,.38,2.10));
        }
        return InteractionFocus.pick(player.x(),player.y(),player.angle(),
            near,this::sight);
    }
    public synchronized InteractionFocus.Target focus(int playerId){
        Position p=positions.get(playerId);
        return p==null?null:focused(p);
    }
    public synchronized Harvest harvest(int playerId,ArenaSystems.Player player,int nodeId){
        Position p=positions.get(playerId);
        if(p==null||player==null||player.dead)return new Harvest(false,"NOT_IN_WILDERNESS",null,null);
        if(clock-lastHarvest.getOrDefault(playerId,-10_000L)<800)
            return new Harvest(false,"COOLDOWN",null,null);
        InteractionFocus.Target focus=focused(p);
        if(focus==null||focus.id()!=nodeId)
            return new Harvest(false,"NO_TARGET",null,null);
        WildernessResources.Node node=resources.node(nodeId);
        if(node==null||node.remaining()<1)
            return new Harvest(false,"EXHAUSTED",node,null);
        SurvivalCrafting.Item item=SurvivalCrafting.Item.valueOf(node.type().name());
        if(!player.survival.canAcquire(item,1,false))
            return new Harvest(false,"INVENTORY_FULL",node,item);
        WildernessResources.Type result=resources.harvest(nodeId,(int)Math.floor(p.x()),
            (int)Math.floor(p.y()));
        if(result!=node.type())
            return new Harvest(false,"DEPLETED",resources.node(nodeId),item);
        if(!player.survival.acquire(item,1,false))
            throw new IllegalStateException("Harvest inventory changed after host preflight");
        lastHarvest.put(playerId,clock);
        return new Harvest(true,"OK",resources.node(nodeId),item);
    }
    public synchronized List<WildernessResources.Node> nearby(double x,double y,double radius){
        if(!Double.isFinite(x)||!Double.isFinite(y)||radius<0||radius>36)
            throw new IllegalArgumentException("Invalid streaming bounds");
        List<WildernessResources.Node> nodes=new ArrayList<>();
        for(var n:resources.nodes())if(Math.hypot(n.x()+.5-x,n.y()+.5-y)<=radius)
            nodes.add(n);
        return nodes;
    }
    public synchronized void advance(long millis){
        if(millis<0||millis>1000)throw new IllegalArgumentException("Bounded wilderness tick");
        clock+=millis;
        resources.advance(millis);
    }
    /** Independent compact journal, persisted alongside the match. */
    public synchronized void save(Path root)throws IOException{
        Files.createDirectories(root);
        Path to=root.resolve("wilderness-resources.txt");
        var state=resources.snapshot();
        List<String> lines=new ArrayList<>();
        lines.add("WILD_1|"+state.seed()+"|"+state.clock());
        for(var n:state.depletion())
            lines.add(n.id()+"|"+n.remaining()+"|"+n.replenishesAt());
        Path temp=Files.createTempFile(root,".wilderness-", ".tmp");
        try{
            Files.write(temp,lines);
            try{Files.move(temp,to,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){
                Files.move(temp,to,StandardCopyOption.REPLACE_EXISTING);
            }
        }finally{Files.deleteIfExists(temp);}
    }
    public synchronized void load(Path root)throws IOException{
        Path path=root.resolve("wilderness-resources.txt");
        if(!Files.exists(path))return;
        List<String> lines=Files.readAllLines(path);
        if(lines.isEmpty()||lines.size()>resources.nodes().size()+1)
            throw new IOException("Invalid wilderness save size");
        try{
            String[] header=lines.get(0).split("\\|");
            if(header.length!=3||!header[0].equals("WILD_1"))
                throw new IllegalArgumentException("Unsupported wilderness schema");
            List<WildernessResources.Depletion> state=new ArrayList<>();
            for(int i=1;i<lines.size();i++){
                String[] s=lines.get(i).split("\\|");
                if(s.length!=3)throw new IllegalArgumentException("Malformed depletion entry");
                state.add(new WildernessResources.Depletion(Integer.parseInt(s[0]),
                    Integer.parseInt(s[1]),Long.parseLong(s[2])));
            }
            resources.restore(new WildernessResources.Snapshot(Long.parseLong(header[1]),
                Long.parseLong(header[2]),state));
        }catch(IllegalArgumentException exception){
            throw new IOException("Corrupt wilderness depletion save",exception);
        }
    }
}
