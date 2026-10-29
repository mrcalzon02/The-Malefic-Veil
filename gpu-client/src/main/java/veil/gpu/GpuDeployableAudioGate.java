package veil.gpu;

import veil.MaleficVeil;
import java.util.*;

/**
 * Edge-triggered thrown-equipment audio from authoritative field snapshots.
 * Not a second field simulation, and no conflation with permanent buildings.
 */
public final class GpuDeployableAudioGate {
    public enum Phase { LAUNCH, LAND, EXPIRE }
    public record Event(int faction,Phase phase,double x,double y) {}
    private record State(int faction,boolean flying,long remaining) {}
    private Map<Integer,State> last=Map.of();
    private boolean ready;
    private long latestBurstId;

    public List<Event> accept(List<MaleficVeil.GpuFieldAudio> published){
        return accept(published,List.of());
    }
    public List<Event> accept(List<MaleficVeil.GpuFieldAudio> published,
            List<MaleficVeil.GpuFieldBurstAudio> bursts){
        Objects.requireNonNull(published);
        Objects.requireNonNull(bursts);
        Map<Integer,State> next=new HashMap<>();
        List<Event> out=new ArrayList<>();
        for(var field:published){
            if(field.hp()<=0||field.blueprint()<0||field.blueprint()>=21)continue;
            int faction=field.blueprint()/7; // authoritative catalog order
            State state=new State(faction,field.flying(),field.remainingMs());
            next.put(field.id(),state);
            if(!ready||out.size()>=6)continue;
            State old=last.get(field.id());
            if(old==null&&field.flying())
                out.add(new Event(faction,Phase.LAUNCH,field.x(),field.y()));
            else if(old!=null&&old.flying()&&!field.flying())
                out.add(new Event(faction,Phase.LAND,field.x(),field.y()));
        }
        // Only real host-emitted harmless bursts can play a removal cue.
        // A vanished network field alone is NOT an explosion.
        long newest=latestBurstId;
        for(var burst:bursts){
            newest=Math.max(newest,burst.id());
            if(ready&&burst.id()>latestBurstId&&burst.faction()>=0&&
                    burst.faction()<3&&out.size()<6)
                out.add(new Event(burst.faction(),Phase.EXPIRE,burst.x(),burst.y()));
        }
        latestBurstId=newest;
        last=Map.copyOf(next);
        ready=true;
        return List.copyOf(out);
    }
    public void reset(){last=Map.of();latestBurstId=0;ready=false;}
    private static MaleficVeil.GpuFieldAudio f(int id,int faction,boolean flight){
        return new MaleficVeil.GpuFieldAudio(id,faction*7,2,0,flight,
            flight?3_600_000L:3_599_000L,120);
    }
    private static void expect(List<Event> actual,Phase... expected){
        if(actual.size()!=expected.length)throw new AssertionError("Unexpected "+actual);
        for(int i=0;i<expected.length;i++)
            if(actual.get(i).phase()!=expected[i])
                throw new AssertionError("Expected "+expected[i]+" got "+actual);
    }
    public static void main(String[] args){
        GpuDeployableAudioGate gate=new GpuDeployableAudioGate();
        expect(gate.accept(List.of(f(1,0,true))));
        expect(gate.accept(List.of(f(1,0,true),f(2,1,true))),Phase.LAUNCH);
        expect(gate.accept(List.of(f(1,0,false),f(2,1,false),f(3,2,true))),
            Phase.LAND,Phase.LAND,Phase.LAUNCH);
        expect(gate.accept(List.of(f(1,0,false),f(2,1,false),f(3,2,true))));
        expect(gate.accept(List.of())); // no fake explosion on stream loss
        var burst=new MaleficVeil.GpuFieldBurstAudio(1,2,0,2);
        expect(gate.accept(List.of(),List.of(burst)),Phase.EXPIRE);
        expect(gate.accept(List.of(),List.of(burst)));
        gate.reset();
        expect(gate.accept(List.of(f(4,2,false)))); // joining history silence
        System.out.println("PASS GPU_DEPLOYABLE_AUDIO faction launch/land, actual harmless burst, dedup");
    }
}
