package veil;

import java.nio.file.*;
import java.util.*;

/**
 * Regression for the single continuous continental world: no portal action,
 * teleport session, dimension flag or alternate player transform required.
 */
public final class WorldHarvestingTest {
    private static void check(boolean yes,String reason){
        if(!yes)throw new AssertionError(reason);
    }
    public static void main(String[] ignored)throws Exception{
        long seed=20261021L;
        ArenaSystems arena=new ArenaSystems(seed);
        var player=arena.join(1);
        var other=arena.join(2);
        World world=arena.world;
        WorldHarvesting system=new WorldHarvesting(world);
        check(world.detailedCity(player.x,player.y),"spawn belongs to City A");
        check(world.canWalk(1.5,1.5),"existing city street remains accessible");
        check(!world.detailedCity(-.5,1.5),"same coordinate world continues past old city border");
        check(world.parcelX(world.worldX(33.5))==33,"macro node X maps to world metres");
        check(world.parcelY(world.worldY(44.5))==44,"macro node Y maps to world metres");
        WildernessResources.Node chosen=null;
        for(var candidate:system.resources.nodes()){
            double nx=system.nodeX(candidate),ny=system.nodeY(candidate);
            if(world.detailedCity(nx,ny))continue;
            for(int side=0;side<4;side++){
                double angle=side*Math.PI/2;
                double x=nx-Math.cos(angle)*1.25;
                double y=ny-Math.sin(angle)*1.25;
                if(!world.canWalk(x,y)||!world.lineClear(x,y,nx,ny))continue;
                player.x=x;player.y=y;player.angle=angle;
                other.x=x;other.y=y;other.angle=angle;
                var focus=system.focusFor(player);
                if(focus!=null&&focus.id()==candidate.id()){
                    chosen=candidate;break;
                }
            }
            if(chosen!=null)break;
        }
        check(chosen!=null,"actual traversable resources are aim-accessible on the same map");
        int nodeId=chosen.id();
        int before=system.resources.node(nodeId).remaining();
        check(!system.harvest(player,nodeId+100000).accepted(),
            "malicious client cannot invent a resource identifier");
        var gained=system.harvest(player,nodeId);
        check(gained.accepted(),"valid world-ray harvesting accepted");
        check(gained.node().remaining()==before-1,"node depletion is authoritative");
        check(player.survival.count(gained.item())==1,
            "same character inventory receives harvested material");
        check(system.resources.node(nodeId).remaining()==before-1,
            "all players observe the same resource node");
        check(!system.harvest(player,nodeId).accepted(),"harvest cooldown on same character");
        system.advance(900);
        var next=system.harvest(other,nodeId);
        check(next.accepted(),"second player shares location and can harvest same node");
        check(system.resources.node(nodeId).remaining()==before-2,
            "second player harvest shares first player's resource budget");
        check(!system.harvest(player,nodeId+100000).accepted(),
            "other player cannot forge remote node");
        Path temp=Files.createTempDirectory("veil-unified");
        try{
            system.save(temp);
            WorldHarvesting loaded=new WorldHarvesting(world);
            loaded.load(temp);
            check(loaded.resources.snapshot().depletion().equals(
                system.resources.snapshot().depletion()),
                "same-world resource depletion survives host reboot");
        }finally{
            try(var files=Files.list(temp)){
                files.forEach(p->{try{Files.deleteIfExists(p);}catch(Exception e){
                    throw new IllegalStateException(e);
                }});
            }
            Files.deleteIfExists(temp);
        }
        System.out.println("PASS WorldHarvestingTest: one world, one actor pose, shared harvest/cargo, restart");
    }
}
