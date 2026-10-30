package veil.gpu;

import java.util.*;
import veil.World;

/**
 * Per-source LOS cache for the four faction/utility loops and the single
 * dominant vehicle. It uses the WORLD's real collision path, not invented
 * room/door flags, and never performs world ray checks for silent lanes.
 */
public final class GpuSpatialOcclusionGate {
    @FunctionalInterface public interface Sight {
        boolean clear(double listenerX,double listenerY,double sourceX,double sourceY);
    }
    private record Cached(double px,double py,double sx,double sy,
                          float checkedAt,float attenuation) {}
    private static final int MOTOR=GpuIndustryAudioPlanner.Channel.values().length;
    private final Cached[] cache=new Cached[MOTOR+1];
    private float seconds;
    private int rays;
    public int rays(){return rays;}
    public void reset(){Arrays.fill(cache,null);seconds=0;rays=0;}

    private void advance(float dt){
        if(Float.isFinite(dt))seconds+=Math.max(0f,Math.min(.1f,dt));
    }
    private float attenuation(int id,double px,double py,double sx,double sy,
            Sight world){
        if(!Double.isFinite(sx)||!Double.isFinite(sy)||world==null)return 1f;
        Cached last=cache[id];
        if(last!=null&&seconds-last.checkedAt()<.3f&&
           Math.hypot(last.px()-px,last.py()-py)<1.15&&
           Math.hypot(last.sx()-sx,last.sy()-sy)<1.15)
            return last.attenuation();
        boolean visible=world.clear(px,py,sx,sy);
        rays++;
        float factor=visible?1f:.38f;
        cache[id]=new Cached(px,py,sx,sy,seconds,factor);
        return factor;
    }
    public EnumMap<GpuIndustryAudioPlanner.Channel,GpuIndustryAudioPlanner.Mix>
            industry(EnumMap<GpuIndustryAudioPlanner.Channel,GpuIndustryAudioPlanner.Mix> raw,
            double x,double y,Sight world,float dt){
        Objects.requireNonNull(raw);
        advance(dt);
        EnumMap<GpuIndustryAudioPlanner.Channel,GpuIndustryAudioPlanner.Mix> mixed=
            new EnumMap<>(GpuIndustryAudioPlanner.Channel.class);
        for(var lane:GpuIndustryAudioPlanner.Channel.values()){
            var src=raw.get(lane);
            if(src==null||src.gain()<=0||!Double.isFinite(src.sourceX())||
                !Double.isFinite(src.sourceY())){
                if(src!=null)mixed.put(lane,src);
                continue;
            }
            float factor=attenuation(lane.ordinal(),x,y,
                src.sourceX(),src.sourceY(),world);
            mixed.put(lane,new GpuIndustryAudioPlanner.Mix(
                src.gain()*factor,src.pan(),src.sourceX(),src.sourceY()));
        }
        return mixed;
    }
    public GpuVehicleAudioDynamics.Mix vehicle(GpuVehicleAudioDynamics.Mix raw,
            double x,double y,Sight world,float dt){
        Objects.requireNonNull(raw);
        advance(dt);
        float factor=raw.idleGain()>0||raw.runningGain()>0?
            attenuation(MOTOR,x,y,raw.sourceX(),raw.sourceY(),world):1f;
        float eventFactor=raw.event()!=GpuVehicleAudioDynamics.Event.NONE&&
            Float.isFinite(raw.eventGain())&&raw.eventGain()>0&&
            Double.isFinite(raw.eventX())&&Double.isFinite(raw.eventY())&&
            world!=null?(world.clear(x,y,raw.eventX(),raw.eventY())?1f:.38f):1f;
        if(eventFactor!=1f)rays++;
        return new GpuVehicleAudioDynamics.Mix(
            raw.idleGain()*factor,raw.runningGain()*factor,
            raw.pitch(),raw.pan(),raw.event(),raw.eventGain()*eventFactor,
            raw.eventPan(),raw.sourceX(),raw.sourceY(),raw.eventX(),raw.eventY());
    }
    public static Sight from(World world){
        Objects.requireNonNull(world);
        return world::lineClear;
    }
    private static void check(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    public static void main(String[] args){
        var lane=GpuIndustryAudioPlanner.Channel.UNRAVELED;
        var sources=new EnumMap<GpuIndustryAudioPlanner.Channel,
            GpuIndustryAudioPlanner.Mix>(GpuIndustryAudioPlanner.Channel.class);
        sources.put(lane,new GpuIndustryAudioPlanner.Mix(.8f,.25f,5,0));
        final boolean[] open={false};
        final int[] queries={0};
        Sight sight=(ax,ay,bx,by)->{queries[0]++;return open[0];};
        var gate=new GpuSpatialOcclusionGate();
        var first=gate.industry(sources,0,0,sight,.05f);
        check(first.get(lane).gain()>.30f&&first.get(lane).gain()<.31f,
            "Solid wall should dampen live industry, not mute it");
        open[0]=true;
        gate.industry(sources,0,0,sight,.05f);
        check(queries[0]==1,"Same emitter must use cached line of sight");
        for(int i=0;i<4;i++)gate.industry(sources,0,0,sight,.1f);
        var visible=gate.industry(sources,0,0,sight,.05f);
        check(queries[0]>=2&&Math.abs(visible.get(lane).gain()-.8f)<1e-5,
            "Visibility should update on the next bounded probe");
        gate.industry(sources,2,0,sight,.01f);
        check(queries[0]>=3,"Moving listener must invalidate visibility");
        var dormant=new EnumMap<GpuIndustryAudioPlanner.Channel,
            GpuIndustryAudioPlanner.Mix>(GpuIndustryAudioPlanner.Channel.class);
        dormant.put(lane,new GpuIndustryAudioPlanner.Mix(0,0));
        int before=queries[0];
        gate.industry(dormant,0,0,sight,.1f);
        check(queries[0]==before,"Silent industry must not trace rays");
        var moving=new GpuVehicleAudioDynamics.Mix(.5f,.3f,1.2f,.2f,
            GpuVehicleAudioDynamics.Event.ACCELERATE,.6f,.1f,5,0,5,0);
        open[0]=false;
        gate.reset();
        var car=gate.vehicle(moving,0,0,sight,.1f);
        check(car.idleGain()<.20f&&car.runningGain()<.12f&&
            car.eventGain()<.24f&&car.pitch()==1.2f&&
            car.event()==GpuVehicleAudioDynamics.Event.ACCELERATE,
            "Vehicle occlusion must preserve host event identity, pitch and pan");
        var missing=new GpuVehicleAudioDynamics.Mix(0,0,1,0,
            GpuVehicleAudioDynamics.Event.NONE,0,0,
            Double.NaN,Double.NaN,Double.NaN,Double.NaN);
        gate.reset();
        check(gate.vehicle(missing,0,0,sight,.1f).idleGain()==0&&gate.rays()==0,
            "Parked/unavailable cars must not allocate sound or LOS queries");
        System.out.println("PASS GPU_SPATIAL_OCCLUSION cached industry + motors, no ghost sources");
    }
}
