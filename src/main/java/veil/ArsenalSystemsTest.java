package veil;
import java.util.*;
import java.lang.reflect.*;
import java.nio.file.*;
import static veil.ArsenalCatalog.*;

/** Deterministic gameplay checks, including denials and actual damage rather than catalog counts. */
public final class ArsenalSystemsTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    @SuppressWarnings("unchecked") static ArsenalSystems.State state(ArenaSystems h)throws Exception{
        Field f=ArsenalSystems.class.getDeclaredField("active");f.setAccessible(true);return ((Map<Integer,ArsenalSystems.State>)f.get(h.arsenal)).get(1);
    }
    static ArenaSystems fixture(){ArenaSystems h=new ArenaSystems(20261020);h.world.npcs.clear();h.join(1);h.player(1).angle=0;return h;}
    static void tick(ArenaSystems h,long ms){while(ms>0){long d=Math.min(ms,100);h.advance(d);ms-=d;}}
    public static void main(String[] args)throws Exception{
        ArenaSystems h=fixture();
        Set<Integer> pool=new TreeSet<>();for(var l:h.arsenal.loot)if(l.type==0){pool.add(l.value);check(l.value<8,"advanced weapon leaked to map");}
        check(pool.size()==8,"all eight basic weapons present");
        check(!h.arsenal.select(1,9).ok(),"unowned weapon selection");
        check(!h.arsenal.craft(1,9).ok(),"craft without bench");
        var s=state(h);s.clip[0]=1;int ammo=s.reserve[0];
        check(h.arsenal.reload(1).ok(),"reload starts");check(s.clip[0]==1&&s.reserve[0]==ammo,"reload not instantaneous");
        check(!h.arsenal.fire(1,false,1).fired(),"fire during reload");tick(h,1500);check(s.clip[0]==get(0).capacity(),"reload completes");
        s.owned|=2;s.clip[0]=0;h.arsenal.reload(1);h.arsenal.select(1,1);tick(h,1600);check(s.clip[0]==0,"switch cancels reload without minting ammo");
        // Each primary and secondary traverses real host firing rules; projectile modes advance.
        for(int w=0;w<32;w++)for(boolean alt:new boolean[]{false,true}){
            ArenaSystems a=fixture();ArsenalSystems.State st=state(a);st.owned=0xffffffffL;Arrays.fill(st.clip,200);for(int j=0;j<32;j++)st.clip[j]=get(j).capacity();Arrays.fill(st.reserve,100);
            a.arsenal.select(1,w);a.world.npcs.add(new World.Npc(4,1.5));
            if(alt&&(w==16||w==25)){a.world.npcs.clear();a.arsenal.fire(1,false,1);tick(a,100);}
            long seq=10;var shot=a.arsenal.fire(1,alt,seq++);if(shot.reason().equals("CHARGING")){tick(a,mode(w,alt).charge()+100);shot=a.arsenal.fire(1,alt,seq++);}
            check(shot.fired(),get(w).name()+" mode "+alt+" failed: "+shot.reason());
            check(!a.arsenal.fire(1,alt,seq-1).fired(),"duplicate replay");
            tick(a,5000);check(a.arsenal.effects().stream().noneMatch(e->e.kind().equals("PROJECTILE")),"finite projectile lifetime");
            if(!alt&&w<3)check(a.world.npcs.get(0).hp<80,"real ballistic hit "+w);
        }
        ArenaSystems chain=fixture();var cs=state(chain);cs.owned|=1L<<14;cs.clip[14]=10;chain.arsenal.select(1,14);
        chain.player(1).angle=Math.PI/2;chain.world.npcs.add(new World.Npc(1.5,4));chain.world.npcs.add(new World.Npc(1.5,5));chain.world.npcs.add(new World.Npc(1.5,6));
        check(chain.arsenal.fire(1,false,1).fired(),"chain fired");check(chain.world.npcs.stream().allMatch(n->n.hp<80),"lightning hits distinct nearby enemies");
        // Craft atomically at own bench. The fixture places only a test bench; normal placement uses reviewBuild.
        double bx=0,by=0;search:for(int x=3;x<World.SIZE-3;x++)for(int y=3;y<World.SIZE-3;y++)if(h.world.canWalk(x+.5,y+.5)&&!h.world.worldStructureAt(x+.5,y+.5,.55)){bx=x+.5;by=y+.5;break search;}
        h.player(1).x=bx;h.player(1).y=by;World.Device bench=new World.Device(bx,by,ArenaSystems.FIELD_BENCH);bench.ownerId=1;h.world.devices.add(bench);
        s.owned=1;s.selected=0;s.faction=0;s.factionLocked=false;
        check(!h.arsenal.craft(1,9).ok(),"wrong faction denied");check(h.arsenal.faction(1,1).ok(),"choose faction separately from class");
        int scrap=h.player(1).scrap;check(!h.arsenal.craft(1,9).ok()&&h.player(1).scrap==scrap,"resource failure atomic");
        h.player(1).scrap=500;Arrays.fill(s.materials,200);
        check(h.arsenal.craft(1,9).ok(),"tier-one energy recipe");check(!h.arsenal.faction(1,2).ok(),"faction locks on first specialist fabrication");
        check(!h.arsenal.craft(1,12).ok(),"higher bench tier denied");check(h.arsenal.upgrade(1).ok(),"bench tier two upgrade");check(h.arsenal.craft(1,12).ok(),"research prerequisite and tier two");
        long mask=s.owned;check(!h.arsenal.craft(1,12).ok()&&s.owned==mask,"duplicate craft does not debit");
        bench.hp=0;check(!h.arsenal.craft(1,32).ok(),"destroyed bench unavailable");bench.hp=100;
        String key="acct_"+"a".repeat(64);h.assignAccount(1,key);Path dir=Files.createTempDirectory("veil-arsenal-");
        try{DurableRealm.save(h,dir);ArenaSystems restored=fixtureWithoutJoin();check(DurableRealm.load(restored,dir),"inventory snapshot loads");restored.join(1);restored.assignAccount(1,key);var v=restored.arsenal.view(1);check(v.owned()==s.owned&&v.factionLocked(),"crafted ownership and faction survive restart");check(v.materials()[1]==s.materials[1],"materials survive restart");}
        finally{try(var files=Files.list(dir)){for(Path file:files.toList())Files.delete(file);}Files.delete(dir);}
        System.out.println("PASS ArsenalSystemsTest: "+checks+" assertions, 64 firing modes, timed reloads, crafting authority and persistence");
    }
    static ArenaSystems fixtureWithoutJoin(){ArenaSystems h=new ArenaSystems(20261020);h.world.npcs.clear();return h;}
    static Mode mode(int w,boolean a){return a?get(w).alternate():get(w).primary();}
}
