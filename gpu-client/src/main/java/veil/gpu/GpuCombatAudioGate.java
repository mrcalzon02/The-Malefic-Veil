package veil.gpu;

import veil.ArsenalSystems;
import java.util.*;

/**
 * Converts existing host-owned combat FX into audible one-shot events.
 * Negative IDs are transient render-only projectiles/fields. Positive IDs
 * monotonically identify authoritative effects. Never replay joining history.
 */
public final class GpuCombatAudioGate {
    public enum Kind { IMPACT, HIT, BOUNCE, ARC, BLAST }
    public record Event(Kind kind,double x,double y,double radius,int color) {}
    private boolean initialized;
    private int largestId;

    public List<Event> accept(List<ArsenalSystems.Effect> effects) {
        Objects.requireNonNull(effects);
        int latest=largestId;
        for(var effect:effects)
            if(effect.id()>latest)latest=effect.id();
        if(!initialized){
            initialized=true;largestId=latest;
            return List.of();
        }
        List<ArsenalSystems.Effect> unseen=new ArrayList<>();
        for(var effect:effects)
            if(effect.id()>largestId)unseen.add(effect);
        largestId=latest;
        unseen.sort(Comparator.comparingInt(ArsenalSystems.Effect::id));
        List<Event> out=new ArrayList<>();
        for(var effect:unseen){
            Kind kind=switch(effect.kind()){
                case "HIT" -> Kind.HIT;
                case "IMPACT" -> Kind.IMPACT;
                case "BOUNCE" -> Kind.BOUNCE;
                case "ARC" -> Kind.ARC;
                case "BLAST" -> Kind.BLAST;
                default -> null; // MUZZLE, TRACE, projectiles and fields are not impacts
            };
            if(kind!=null&&out.size()<8)
                out.add(new Event(kind,effect.x(),effect.y(),
                    Math.max(0,effect.size()),effect.color()));
        }
        return List.copyOf(out);
    }
    public void reset(){initialized=false;largestId=0;}

    private static ArsenalSystems.Effect effect(int id,String kind){
        return new ArsenalSystems.Effect(id,kind,3,4,3,4,1,0xFF8800,100);
    }
    private static void expect(List<Event> events,Kind... kinds){
        if(events.size()!=kinds.length)
            throw new AssertionError("Expected "+kinds.length+" got "+events.size());
        for(int i=0;i<kinds.length;i++)
            if(events.get(i).kind()!=kinds[i])
                throw new AssertionError("Expected "+kinds[i]+" got "+events.get(i));
    }
    public static void main(String[] args){
        GpuCombatAudioGate gate=new GpuCombatAudioGate();
        expect(gate.accept(List.of(effect(4,"HIT"),effect(-7,"PROJECTILE"))));
        expect(gate.accept(List.of(effect(4,"HIT"))));
        expect(gate.accept(List.of(effect(8,"MUZZLE"),effect(6,"BLAST"),effect(7,"ARC"))),
            Kind.BLAST,Kind.ARC);
        expect(gate.accept(List.of(effect(6,"BLAST"),effect(7,"ARC"))));
        expect(gate.accept(List.of(effect(10,"BOUNCE"),effect(9,"IMPACT"))),
            Kind.IMPACT,Kind.BOUNCE);
        gate.reset();
        expect(gate.accept(List.of(effect(200,"BLAST"))));
        expect(gate.accept(List.of(effect(201,"HIT"))),Kind.HIT);
        List<ArsenalSystems.Effect> burst=new ArrayList<>();
        for(int i=202;i<225;i++)burst.add(effect(i,"BLAST"));
        if(gate.accept(burst).size()!=8)throw new AssertionError("Per-frame burst cap");
        System.out.println("PASS GPU_HOST_COMBAT_AUDIO hit,impact,arc,bounce,blast,dedup,cap");
    }
}
