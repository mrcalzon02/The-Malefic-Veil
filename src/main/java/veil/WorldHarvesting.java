package veil;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Persistent harvesting within the SAME World and player coordinates as
 * cities, roads, vehicles, NPCs and combat. No session switch, dimension or
 * teleport. Source nodes are macro-parcel authored but located in world metres.
 */
public final class WorldHarvesting {
    public record Harvest(boolean accepted,String reason,WildernessResources.Node node,
                          SurvivalCrafting.Item item) {}
    public final WildernessResources resources;
    private final World world;
    private final Map<Integer,Long> lastHarvest=new HashMap<>();
    private long clock;
    public WorldHarvesting(World world){
        this.world=Objects.requireNonNull(world);
        resources=new WildernessResources(world.continent());
    }
    public double nodeX(WildernessResources.Node node){
        return world.worldX(node.x()+.5);
    }
    public double nodeY(WildernessResources.Node node){
        return world.worldY(node.y()+.5);
    }
    public synchronized List<WildernessResources.Node> nearby(double x,double y,double radius){
        if(!Double.isFinite(x)||!Double.isFinite(y)||radius<0||radius>96)
            throw new IllegalArgumentException("Invalid physical interest radius");
        List<WildernessResources.Node> result=new ArrayList<>();
        for(var n:resources.nodes()){
            double nx=nodeX(n),ny=nodeY(n);
            if(!world.detailedCity(nx,ny)&&Math.hypot(nx-x,ny-y)<=radius)
                result.add(n);
        }
        return List.copyOf(result);
    }
    private InteractionFocus.Target focus(ArenaSystems.Player p){
        List<InteractionFocus.Candidate> candidates=new ArrayList<>();
        for(var node:nearby(p.x,p.y,3)){
            if(node.remaining()<=0)continue;
            double nx=nodeX(node),ny=nodeY(node);
            candidates.add(new InteractionFocus.Candidate(InteractionFocus.Kind.RESOURCE,
                node.id(),node.type().name().replace('_',' '),nx,ny,.45,2.10));
        }
        return InteractionFocus.pick(p.x,p.y,p.angle,candidates,world::lineClear);
    }
    public synchronized InteractionFocus.Target focusFor(ArenaSystems.Player player){
        return player==null||player.dead?null:focus(player);
    }
    /** Host-owned actor pose, inventory and depletion; requests supply ONLY node ID. */
    public synchronized Harvest harvest(ArenaSystems.Player player,int nodeId){
        if(player==null||player.dead)return new Harvest(false,"DEAD",null,null);
        if(nodeId<0)return new Harvest(false,"UNKNOWN_RESOURCE",null,null);
        if(clock-lastHarvest.getOrDefault(player.id,-10_000L)<800)
            return new Harvest(false,"COOLDOWN",null,null);
        InteractionFocus.Target target=focus(player);
        if(target==null||target.id()!=nodeId)
            return new Harvest(false,"NO_TARGET",null,null);
        WildernessResources.Node node=resources.node(nodeId);
        if(node==null||node.remaining()<1)
            return new Harvest(false,"EXHAUSTED",node,null);
        SurvivalCrafting.Item item=SurvivalCrafting.Item.valueOf(node.type().name());
        if(!player.survival.canAcquire(item,1,false))
            return new Harvest(false,"INVENTORY_FULL",node,item);
        WildernessResources.Type source=resources.harvest(nodeId,
            world.parcelX(player.x),world.parcelY(player.y));
        if(source!=node.type())
            return new Harvest(false,"DEPLETED",resources.node(nodeId),item);
        if(!player.survival.acquire(item,1,false))
            throw new IllegalStateException("Host inventory changed during atomic harvest");
        lastHarvest.put(player.id,clock);
        return new Harvest(true,"OK",resources.node(nodeId),item);
    }
    public synchronized void advance(long millis){
        if(millis<0||millis>1000)throw new IllegalArgumentException("Bounded world tick");
        clock+=millis;resources.advance(millis);
    }
    /** Retains the previous depletion journal schema and source seed. */
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
            throw new IOException("Invalid resource journal size");
        try{
            String[] head=lines.get(0).split("\\|");
            if(head.length!=3||!head[0].equals("WILD_1"))
                throw new IllegalArgumentException("Unsupported resource journal");
            List<WildernessResources.Depletion> state=new ArrayList<>();
            for(int i=1;i<lines.size();i++){
                String[] row=lines.get(i).split("\\|");
                if(row.length!=3)throw new IllegalArgumentException("Invalid resource row");
                state.add(new WildernessResources.Depletion(Integer.parseInt(row[0]),
                    Integer.parseInt(row[1]),Long.parseLong(row[2])));
            }
            resources.restore(new WildernessResources.Snapshot(Long.parseLong(head[1]),
                Long.parseLong(head[2]),state));
            clock=Long.parseLong(head[2]);
        }catch(IllegalArgumentException err){
            throw new IOException("Corrupt resource journal",err);
        }
    }
}
