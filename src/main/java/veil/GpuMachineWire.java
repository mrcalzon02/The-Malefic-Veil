package veil;

import java.util.*;

/**
 * Bounded, audio-only snapshot for nearby ACTIVE host production equipment.
 * Readers never infer running state from decorative meshes or the legacy
 * DEVICE packet. All actual factory rules remain inside ArenaSystems.
 */
public final class GpuMachineWire {
    public static final int MAX=24;
    public record Operating(int index,int kind,double x,double y) {}
    private GpuMachineWire(){}

    public static List<Operating> nearby(List<World.Device> devices,
            double playerX,double playerY) {
        Objects.requireNonNull(devices);
        List<Operating> active=new ArrayList<>();
        for(int i=0;i<devices.size();i++){
            World.Device d=devices.get(i);
            if(d.hp<=0||!d.productionActive||!d.factoryHold.isEmpty()||
                d.kind<0||d.kind>=GameplayMeshes.DEVICE.length)continue;
            double distance=Math.hypot(d.x-playerX,d.y-playerY);
            if(Double.isFinite(distance)&&distance<=32)
                active.add(new Operating(i,d.kind,d.x,d.y));
        }
        active.sort(Comparator.comparingDouble(v->
            Math.hypot(v.x()-playerX,v.y()-playerY)));
        if(active.size()>MAX)active.subList(MAX,active.size()).clear();
        return List.copyOf(active);
    }
    public static String encode(List<Operating> list){
        Objects.requireNonNull(list);
        if(list.size()>MAX)throw new IllegalArgumentException("Too many machines");
        if(list.isEmpty())return "MACHINE_AUDIO|NONE";
        StringJoiner entries=new StringJoiner(";");
        for(var value:list){
            validate(value);
            entries.add(value.index()+","+value.kind()+","+value.x()+","+value.y());
        }
        return "MACHINE_AUDIO|"+entries;
    }
    public static List<Operating> decode(String line) {
        if(line==null||line.length()>4096||!line.startsWith("MACHINE_AUDIO|"))
            throw new IllegalArgumentException("Invalid machine packet");
        String body=line.substring("MACHINE_AUDIO|".length());
        if(body.equals("NONE"))return List.of();
        String[] entries=body.split(";",-1);
        if(entries.length<1||entries.length>MAX)
            throw new IllegalArgumentException("Machine packet count");
        List<Operating> found=new ArrayList<>();
        Set<Integer> seen=new HashSet<>();
        for(String entry:entries){
            String[] parts=entry.split(",",-1);
            if(parts.length!=4)throw new IllegalArgumentException("Machine fields");
            try{
                Operating value=new Operating(Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]));
                validate(value);
                if(!seen.add(value.index()))
                    throw new IllegalArgumentException("Duplicate machine");
                found.add(value);
            }catch(NumberFormatException bad){
                throw new IllegalArgumentException("Invalid machine number",bad);
            }
        }
        return List.copyOf(found);
    }
    private static void validate(Operating value) {
        if(value==null||value.index()<0||value.kind()<0||
            value.kind()>=GameplayMeshes.DEVICE.length||
            !Double.isFinite(value.x())||!Double.isFinite(value.y())||
            Math.abs(value.x())>1e7||Math.abs(value.y())>1e7)
            throw new IllegalArgumentException("Untrusted machine state");
    }
    public static void main(String[] args){
        World.Device ready=new World.Device(2,1,ArenaSystems.UNRAVELED_MACHINIST_BAY);
        ready.productionActive=true;
        World.Device held=new World.Device(3,1,ArenaSystems.PENITENT_RELIQUARY);
        held.productionActive=true;held.factoryHold="WAIT_WITNESS";
        World.Device idle=new World.Device(4,1,ArenaSystems.BEGOTTEN_SPORE_BASIN);
        World.Device distant=new World.Device(70,1,ArenaSystems.GENERATOR);
        distant.productionActive=true;
        List<Operating> nearby=nearby(List.of(ready,held,idle,distant),0,0);
        if(nearby.size()!=1||nearby.get(0).kind()!=ready.kind)
            throw new AssertionError("Idle/blocked/distant machine sounded "+nearby);
        if(!decode(encode(nearby)).equals(nearby))
            throw new AssertionError("Machine packet roundtrip failed");
        if(!decode(encode(List.of())).isEmpty())
            throw new AssertionError("Empty machines must clear the client");
        for(String unsafe:List.of("MACHINE_AUDIO|5,25,NaN,1",
                "MACHINE_AUDIO|0,25,1,2;0,26,2,3",
                "MACHINE_AUDIO|1,25,Infinity,4",
                "MACHINE_AUDIO|1,999,0,0")){
            try{decode(unsafe);throw new AssertionError("Accepted "+unsafe);}
            catch(IllegalArgumentException expected){}
        }
        System.out.println("PASS GPU_MACHINE_WIRE active-only/nearby/bounded/roundtrip");
    }
}
