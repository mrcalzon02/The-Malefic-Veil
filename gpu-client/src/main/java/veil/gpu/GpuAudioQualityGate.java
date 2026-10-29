package veil.gpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Offline verification of synthesized WAV resources, no audio device needed. */
public final class GpuAudioQualityGate {
    private GpuAudioQualityGate(){}
    private static int tested;
    private static void validate(String name,byte[] wav,boolean loop){
        if(wav==null||wav.length<256||(wav.length&1)!=0)
            throw new AssertionError("Missing or short WAV: "+name);
        ByteBuffer b=ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        if(b.getInt(0)!=0x46464952||b.getInt(8)!=0x45564157||
           b.getInt(12)!=0x20746d66||b.getInt(36)!=0x61746164)
            throw new AssertionError("Invalid RIFF structure: "+name);
        if(b.getInt(4)!=wav.length-8||b.getInt(40)!=wav.length-44||
           b.getShort(20)!=1||b.getShort(22)!=1||
           b.getInt(24)!=22050||b.getShort(34)!=16)
            throw new AssertionError("Incorrect PCM metadata: "+name);
        int largest=0;
        double power=0;
        int count=(wav.length-44)/2;
        for(int i=0;i<count;i++){
            int sample=b.getShort(44+i*2);
            largest=Math.max(largest,Math.abs(sample));
            power+=(double)sample*sample;
        }
        double rms=Math.sqrt(power/count);
        if(largest<75||largest>23000||rms<15)
            throw new AssertionError("Quiet/clipped WAV "+name+" peak="+largest+
                " rms="+rms);
        if(loop){
            // A streaming Music loop must not introduce an abrupt PCM jump.
            int first=b.getShort(44),last=b.getShort(wav.length-2);
            if(Math.abs(first-last)>450)
                throw new AssertionError("Loop seam "+name+
                    " first="+first+" last="+last);
        }
        tested++;
    }

    public static void main(String[] args){
        for(int w=0;w<32;w++){
            validate("weapon-primary-"+w,GpuAudioSynthesis.weapon(w),false);
            validate("weapon-alternate-"+w,GpuAudioSynthesis.weapon(w,true),false);
            validate("weapon-reload-"+w,GpuAudioSynthesis.handling(w),false);
        }
        for(var material:GpuFootstepSurface.Material.values())
            for(int variation=0;variation<2;variation++)
                validate("step-"+material+"-"+variation,
                    GpuAudioSynthesis.footstep(material,variation),false);
        for(var impact:GpuAudioSynthesis.Impact.values())
            validate("impact-"+impact,GpuAudioSynthesis.impact(impact),false);
        for(var weather:GpuAudioSynthesis.WeatherBed.values())
            validate("weather-"+weather,GpuAudioSynthesis.weatherBed(weather),true);
        validate("city",GpuAudioSynthesis.cityAmbience(),true);
        validate("campfire",GpuAudioSynthesis.campfire(),true);
        validate("combat",GpuAudioSynthesis.combatPulse(),true);
        validate("thunder",GpuAudioSynthesis.thunder(),false);
        for(var cue:GpuAudioSynthesis.Cue.values())
            validate("ui-"+cue,GpuAudioSynthesis.cue(cue),false);
        if(tested!=132)throw new AssertionError("Expected 132 WAV audits, got "+tested);
        System.out.println("PASS GPU_AUDIO_QUALITY samples="+tested+
            " pcm=mono16/22050 complete=true loops=6 no-digital-clipping");
    }
}
