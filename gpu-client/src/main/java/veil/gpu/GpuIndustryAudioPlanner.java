package veil.gpu;

import veil.MaleficVeil;
import java.util.*;

/**
 * Mix plan for actual host-confirmed operating facilities.
 * Each faction owns its own acoustic civilization; shared steam and electrical
 * utilities get a distinct fourth signature. This does not create a sound
 * for a merely visible but inactive industrial structure.
 */
public final class GpuIndustryAudioPlanner {
    public enum Channel { UNRAVELED, PENITENT, BEGOTTEN, UTILITY, ENGINE_IDLE, ENGINE_MOVING }
    public record Mix(float gain,float pan,double sourceX,double sourceY) {
        public Mix(float gain,float pan){this(gain,pan,Double.NaN,Double.NaN);}
        public Mix {
            if(!Float.isFinite(gain)||!Float.isFinite(pan)||gain<0||gain>1||
                pan< -1||pan>1)throw new IllegalArgumentException("Invalid ambient mix");
        }
    }
    private GpuIndustryAudioPlanner(){}

    /** The explicit three factions are not inferred from arbitrary model names. */
    public static int faction(int kind) {
        return switch(kind){
            case 8,25,31,38,52,55 -> 0; // Unraveled
            case 9,26,32,39,50,53 -> 1; // Penitent
            case 10,27,33,40,51,54 -> 2; // Begotten
            default -> -1;                // shared infrastructure
        };
    }
    public static EnumMap<Channel,Mix> plan(double x,double y,double yaw,
            List<MaleficVeil.GpuMachineAudio> machines) {
        Objects.requireNonNull(machines);
        EnumMap<Channel,Mix> result=new EnumMap<>(Channel.class);
        for(Channel channel:Channel.values())result.put(channel,new Mix(0,0));
        // Find the loudest ACTUAL operating source in each independently mixed
        // class. This prevents hundreds of workstations stacking to clipping.
        for(var machine:machines) {
            if(!machine.operating())continue;
            int family=faction(machine.kind());
            Channel channel=family==0?Channel.UNRAVELED:
                family==1?Channel.PENITENT:family==2?Channel.BEGOTTEN:
                Channel.UTILITY;
            put(result,channel,x,y,yaw,machine.x(),machine.y(),30,.74);
        }
        // Vehicles have a separate speed-driven Sound mixer that can adjust
        // pitch, rather than fixed-frequency streaming factory loops.
        return result;
    }
    private static void put(EnumMap<Channel,Mix> target,Channel channel,
            double px,double py,double yaw,double sx,double sy,
            double range,double strength) {
        if(!Double.isFinite(sx)||!Double.isFinite(sy))return;
        double dx=sx-px,dy=sy-py,distance=Math.hypot(dx,dy);
        if(distance>range)return;
        float gain=(float)Math.min(1,strength/
            (1+Math.pow(distance/(range*.30),2)));
        if(gain<=target.get(channel).gain())return;
        float pan=(float)Math.max(-1,Math.min(1,
            Math.sin(Math.atan2(dy,dx)-yaw)*Math.min(1,distance/3)));
        target.put(channel,new Mix(gain,pan,sx,sy));
    }
    private static void need(boolean trueValue,String why) {
        if(!trueValue)throw new AssertionError(why);
    }
    public static void main(String[] args) {
        var off=List.of(new MaleficVeil.GpuMachineAudio(25,2,0,false));
        var on=List.of(new MaleficVeil.GpuMachineAudio(25,2,0,true),
            new MaleficVeil.GpuMachineAudio(26,3,0,true),
            new MaleficVeil.GpuMachineAudio(27,4,0,true),
            new MaleficVeil.GpuMachineAudio(34,5,0,true));
        var empty=plan(0,0,0,off);
        for(Channel lane:Channel.values())need(empty.get(lane).gain()==0,
            "Idle infrastructure must remain silent: "+lane);
        var loud=plan(0,0,0,on);
        for(Channel lane:List.of(Channel.UNRAVELED,Channel.PENITENT,
                Channel.BEGOTTEN,Channel.UTILITY))
            need(loud.get(lane).gain()>0,"Missing distinct faction/utility "+lane);
        need(loud.get(Channel.ENGINE_IDLE).gain()==0,"Engine without vehicle");
        need(loud.get(Channel.UNRAVELED).sourceX()==2&&
            loud.get(Channel.PENITENT).sourceX()==3&&
            loud.get(Channel.BEGOTTEN).sourceX()==4,
            "Each faction's actual source coordinates must survive planning");
        need(loud.get(Channel.ENGINE_MOVING).gain()==0,
            "Vehicle audio belongs exclusively to speed-controlled mixer");
        need(faction(8)==0&&faction(9)==1&&faction(10)==2&&faction(34)==-1,
            "Faction mapping collapse");
        System.out.println("PASS GPU_INDUSTRY_AUDIO three factories, utility, silent idle");
    }
}
