package veil.gpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Conservative output gain trim for OUR generated one-shot PCM only. Master
 * source music and independently sourced recordings retain their bytes/hash.
 * This is repeatable PCM gain staging, not a claim of human loudness mastering.
 */
public final class GpuAudioMastering {
    public enum Group {
        WEAPON(3300), HANDLING(1700), IMPACT(2600), STEP(1000),
        UI(1700), FIELD(1400), VEHICLE(2200), THUNDER(1600);
        final double wantedRms;
        Group(double rms){wantedRms=rms;}
    }
    private GpuAudioMastering(){}
    public static Group classify(String name){
        if(name==null)throw new IllegalArgumentException("No sound name");
        if(name.startsWith("weapon-handling"))return Group.HANDLING;
        if(name.startsWith("weapon"))return Group.WEAPON;
        if(name.startsWith("impact"))return Group.IMPACT;
        if(name.startsWith("step-"))return Group.STEP;
        if(name.startsWith("field-"))return Group.FIELD;
        if(name.startsWith("vehicle-")||name.startsWith("engine-"))return Group.VEHICLE;
        if(name.equals("thunder"))return Group.THUNDER;
        return Group.UI;
    }
    public static byte[] trim(String name,byte[] wav){
        Group group=classify(name);
        if(wav==null||wav.length<256||(wav.length&1)!=0)
            throw new IllegalArgumentException("Expected mono WAV");
        ByteBuffer in=ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        if(in.getInt(0)!=0x46464952||in.getInt(8)!=0x45564157||
           in.getInt(36)!=0x61746164||in.getShort(22)!=1||
           in.getInt(24)!=22050||in.getShort(34)!=16||
           in.getInt(40)!=wav.length-44)
            throw new IllegalArgumentException("Unsupported PCM sound");
        int total=(wav.length-44)/2,peak=0;
        double energy=0;
        for(int i=0;i<total;i++){
            int sample=in.getShort(44+i*2);
            peak=Math.max(peak,Math.abs(sample));
            energy+=(double)sample*sample;
        }
        if(peak==0)return wav.clone();
        double rms=Math.sqrt(energy/total);
        double factor=Math.min(1.50,Math.max(.67,group.wantedRms/Math.max(1,rms)));
        factor=Math.min(factor,22000.0/peak);
        byte[] mastered=Arrays.copyOf(wav,wav.length);
        ByteBuffer output=ByteBuffer.wrap(mastered).order(ByteOrder.LITTLE_ENDIAN);
        for(int i=0;i<total;i++){
            int sample=(int)Math.round(in.getShort(44+i*2)*factor);
            output.putShort(44+i*2,(short)Math.max(-22000,Math.min(22000,sample)));
        }
        return mastered;
    }
    public static void main(String[] args){
        String[] labels={"weapon-1","weapon-handling-2","impact-solid",
            "step-grass","ui-confirm","field-0-launch","vehicle-ignition","thunder"};
        byte[][] waves={
            GpuAudioSynthesis.weapon(0),GpuAudioSynthesis.handling(1),
            GpuAudioSynthesis.impact(GpuAudioSynthesis.Impact.SOLID),
            GpuAudioSynthesis.footstep(GpuFootstepSurface.Material.GRASS,0),
            GpuAudioSynthesis.cue(GpuAudioSynthesis.Cue.CONFIRM),
            GpuAudioSynthesis.deployable(0,GpuAudioSynthesis.DeployablePhase.LAUNCH),
            GpuAudioSynthesis.vehicleEvent(GpuAudioSynthesis.VehicleEvent.IGNITION),
            GpuAudioSynthesis.thunder()
        };
        for(int j=0;j<waves.length;j++){
            byte[] original=waves[j].clone();
            byte[] mastered=trim(labels[j],waves[j]);
            if(!Arrays.equals(original,waves[j])||mastered.length!=original.length||
               !Arrays.equals(Arrays.copyOf(original,44),Arrays.copyOf(mastered,44)))
                throw new AssertionError("Mastering mutated input or RIFF header "+labels[j]);
            if(!Arrays.equals(mastered,trim(labels[j],waves[j])))
                throw new AssertionError("Nondeterministic PCM mastering "+labels[j]);
            ByteBuffer samples=ByteBuffer.wrap(mastered).order(ByteOrder.LITTLE_ENDIAN);
            for(int i=44;i<mastered.length;i+=2)
                if(Math.abs(samples.getShort(i))>22000)
                    throw new AssertionError("Clip after mastering "+labels[j]);
        }
        if(classify("weapon-handling-1")!=Group.HANDLING)
            throw new AssertionError("More specific handling group lost");
        System.out.println("PASS GPU_AUDIO_MASTERING deterministic, capped, non-mutating, 8 categories");
    }
}
