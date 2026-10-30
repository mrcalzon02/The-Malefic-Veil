package veil.gpu;

import veil.MaleficVeil;
import java.util.*;

/**
 * Render-thread-only, host-snapshot-driven vehicle mix. No throttle/key guesses.
 * Deliberately mixes the nearest/audibly strongest vehicle into two voices
 * instead of allocating a permanent OpenAL source per car.
 */
public final class GpuVehicleAudioDynamics {
    public enum Event { NONE, IGNITION, SHUTDOWN, ACCELERATE, BRAKE, BODY_DAMAGE }
    public record Mix(float idleGain,float runningGain,float pitch,float pan,
                      Event event,float eventGain,float eventPan,
                      double sourceX,double sourceY,
                      double eventX,double eventY) {}
    private record Prior(double x,double y,double speed,double fuel,int hp,int driver) {}
    private Map<Integer,Prior> previous=Map.of();
    private boolean initialized;
    private float elapsedSeconds,lastCueSeconds=-100f;
    private int dominant=-1;

    public Mix accept(List<MaleficVeil.GpuVehicleAudio> vehicles,
            double px,double py,double yaw,float dt){
        Objects.requireNonNull(vehicles);
        elapsedSeconds+=Math.max(0,Math.min(.1f,Float.isFinite(dt)?dt:0));
        Map<Integer,Prior> next=new HashMap<>();
        MaleficVeil.GpuVehicleAudio closest=null;
        double best=-1;
        float bodyGain=0,bodyPan=0;
        double bodyX=Double.NaN,bodyY=Double.NaN;
        for(var car:vehicles){
            if(car==null||car.id()<0||!Double.isFinite(car.x())||
                !Double.isFinite(car.y())||!Double.isFinite(car.speed())||
                !Double.isFinite(car.fuel()))continue;
            next.put(car.id(),new Prior(car.x(),car.y(),car.speed(),
                car.fuel(),car.hp(),car.driverId()));
            Prior priorCar=previous.get(car.id());
            double damageDistance=Math.hypot(car.x()-px,car.y()-py);
            if(initialized&&priorCar!=null&&priorCar.hp()>car.hp()&&
                    damageDistance<36&&Double.isFinite(damageDistance)){
                float gain=(float)(1/(1+Math.pow(damageDistance/10,2)));
                if(gain>bodyGain){
                    bodyGain=gain;
                    bodyX=car.x();bodyY=car.y();
                    bodyPan=(float)Math.max(-1,Math.min(1,
                        Math.sin(Math.atan2(car.y()-py,car.x()-px)-yaw)*
                        Math.min(1,damageDistance/3)));
                }
            }
            if(car.hp()<=0||car.driverId()<0||car.fuel()<=0)continue;
            double d=Math.hypot(car.x()-px,car.y()-py);
            if(d>40||!Double.isFinite(d))continue;
            double strength=1/(1+Math.pow(d/10,2));
            if(strength>best){best=strength;closest=car;}
        }
        Event cue=bodyGain>0?Event.BODY_DAMAGE:Event.NONE;
        float cueGain=bodyGain;
        float cuePan=bodyPan;
        double cueX=bodyX,cueY=bodyY;
        // Host-driven shutdown is audible only when its previous state was
        // actually witnessed. Missing/stale LAN snapshots are not events.
        if(cue==Event.NONE&&initialized&&closest==null&&dominant>=0){
            Prior before=previous.get(dominant);
            Prior after=next.get(dominant);
            if(before!=null&&after!=null&&before.driver()>=0&&
                    (after.driver()<0||after.fuel()<=0||after.hp()<=0)){
                cue=Event.SHUTDOWN;
                cueX=after.x();cueY=after.y();
                cueGain=(float)Math.max(0,Math.min(1,
                    1/(1+Math.pow(Math.hypot(after.x()-px,after.y()-py)/10,2))));
            }
        }
        if(closest==null){
            previous=Map.copyOf(next);
            dominant=-1;
            initialized=true;
            if(cue!=Event.NONE&&elapsedSeconds-lastCueSeconds>.075f)
                lastCueSeconds=elapsedSeconds;
            else cue=Event.NONE;
            return new Mix(0,0,1,0,cue,cueGain,cuePan,
                Double.NaN,Double.NaN,cueX,cueY);
        }

        double d=Math.hypot(closest.x()-px,closest.y()-py);
        float falloff=(float)(1/(1+Math.pow(d/10,2)));
        float pan=(float)Math.max(-1,Math.min(1,
            Math.sin(Math.atan2(closest.y()-py,closest.x()-px)-yaw)*
            Math.min(1,d/3)));
        double speed=Math.min(9,Math.abs(closest.speed()));
        float idle=falloff*(float)(.42+.15*Math.min(1,speed/4));
        float moving=falloff*(float)Math.min(.92,Math.max(0,(speed-.12)/7));
        float pitch=(float)Math.max(.80,Math.min(1.88,
            .85+speed*.115));
        if(closest.speed()<-.05)pitch*=.89f; // reverse gear timbre
        Prior prior=previous.get(closest.id());
        if(cue==Event.NONE&&initialized&&prior!=null&&elapsedSeconds-lastCueSeconds>=.075f){
            if(prior.driver()<0&&closest.driverId()>=0&&
                    closest.fuel()>0){
                cue=Event.IGNITION;
            }else if(prior.driver()>=0&&closest.driverId()>=0&&
                    prior.hp()>0&&Math.hypot(closest.x()-prior.x(),
                        closest.y()-prior.y())<3.5){
                double delta=Math.abs(closest.speed())-Math.abs(prior.speed());
                if(delta>=.44)cue=Event.ACCELERATE;
                else if(delta<=-.55&&Math.abs(prior.speed())>1.1)
                    cue=Event.BRAKE;
            }
        }
        if(cue!=Event.NONE&&elapsedSeconds-lastCueSeconds>=.075f){
            if(cueGain==0){
                cueGain=falloff;cuePan=pan;
                cueX=closest.x();cueY=closest.y();
            }
            lastCueSeconds=elapsedSeconds;
        }else cue=Event.NONE;
        dominant=closest.id();
        previous=Map.copyOf(next);
        initialized=true;
        return new Mix(idle,moving,pitch,pan,cue,cueGain,cuePan,
            closest.x(),closest.y(),cueX,cueY);
    }
    public void reset(){
        previous=Map.of();initialized=false;dominant=-1;
        elapsedSeconds=0;lastCueSeconds=-100;
    }
    private static MaleficVeil.GpuVehicleAudio car(double speed,int driver,
            double fuel,int hp){
        return new MaleficVeil.GpuVehicleAudio(3,2,0,speed,fuel,hp,driver);
    }
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args){
        var gate=new GpuVehicleAudioDynamics();
        var idle=car(0,1,100,200);
        var first=gate.accept(List.of(idle),0,0,0,.1f);
        check(first.idleGain()>0&&first.runningGain()==0&&first.event()==Event.NONE,
            "Joining an occupied car must not invent an ignition");
        var accelerate=gate.accept(List.of(car(1.6,1,99,200)),0,0,0,.4f);
        check(accelerate.event()==Event.ACCELERATE&&accelerate.runningGain()>0,
            "Authoritative speed gain must rev");
        var brake=gate.accept(List.of(car(.2,1,98,200)),0,0,0,.4f);
        check(brake.event()==Event.BRAKE&&brake.pitch()<accelerate.pitch(),
            "Vehicle slowdown must brake and lower pitch");
        var collision=gate.accept(List.of(car(0,1,98,185)),0,0,0,.4f);
        check(collision.event()==Event.BODY_DAMAGE,"Host HP loss must confirm vehicle body damage");
        var shutdown=gate.accept(List.of(car(0,-1,98,185)),0,0,0,.4f);
        check(shutdown.event()==Event.SHUTDOWN&&shutdown.idleGain()==0,
            "Driver leaving must silence engine");
        var ignition=gate.accept(List.of(car(0,1,98,185)),0,0,0,.4f);
        check(ignition.event()==Event.IGNITION,"Real reentry should start motor");
        var reverse=gate.accept(List.of(car(-2,1,98,185)),0,0,0,.4f);
        check(reverse.sourceX()==2&&reverse.sourceY()==0,
            "Mix source must track the chosen authoritative car");
        check(reverse.runningGain()>0&&reverse.pitch()<1.15,
            "Reverse should have a distinct lower engine pitch");
        gate.reset();
        check(gate.accept(List.of(car(3,1,99,200)),0,0,0,.4f).event()==Event.NONE,
            "A late join should not replay movement events");
        check(gate.accept(List.of(),0,0,0,.4f).event()==Event.NONE,
            "Missing LAN car packet must not fake shutdown");
        check(gate.accept(List.of(car(1,-1,99,200)),0,0,0,.4f).idleGain()==0,
            "Unoccupied car must remain silent");
        check(gate.accept(List.of(car(1,1,0,200)),0,0,0,.4f).idleGain()==0,
            "Empty fuel tank must remain silent");
        var parkedGate=new GpuVehicleAudioDynamics();
        parkedGate.accept(List.of(car(0,-1,100,200)),0,0,0,.1f);
        var parkedHit=parkedGate.accept(List.of(car(0,-1,100,182)),0,0,0,.1f);
        check(parkedHit.event()==Event.BODY_DAMAGE&&parkedHit.idleGain()==0&&
              parkedHit.eventX()==2,
            "A parked vehicle must have positional body damage without engine");
        var ruinedGate=new GpuVehicleAudioDynamics();
        ruinedGate.accept(List.of(car(3,1,100,20)),0,0,0,.1f);
        var destroyed=ruinedGate.accept(List.of(car(0,-1,100,0)),0,0,0,.1f);
        check(destroyed.event()==Event.BODY_DAMAGE&&destroyed.idleGain()==0,
            "Destroyed car must not sound like ordinary engine shutdown");
        System.out.println("PASS GPU_VEHICLE_DYNAMICS engine,pitch,reverse,brake,damage,join");
    }
}
