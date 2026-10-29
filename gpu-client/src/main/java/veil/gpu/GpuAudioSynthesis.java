package veil.gpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Deterministic, license-independent emergency sound bank. These cues are
 * audible fallbacks; the sourced recordings take priority where authorized.
 * Every arsenal index has a different acoustic profile, not one shared shot.
 */
public final class GpuAudioSynthesis {
    private GpuAudioSynthesis() {}
    private static final int RATE = 22050;
    private static final double TAU = Math.PI * 2.0;
    public enum Cue { CLICK, NAVIGATE, CONFIRM, REJECT, FOOTSTEP_A, FOOTSTEP_B, EMPTY, RELOAD }

    private static byte[] pcm(double[] samples) {
        ByteBuffer out = ByteBuffer.allocate(44 + samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        out.put(new byte[]{'R','I','F','F'}).putInt(36 + samples.length * 2);
        out.put(new byte[]{'W','A','V','E','f','m','t',' '}).putInt(16);
        out.putShort((short)1).putShort((short)1);
        out.putInt(RATE).putInt(RATE * 2).putShort((short)2).putShort((short)16);
        out.put(new byte[]{'d','a','t','a'}).putInt(samples.length * 2);
        for (double sample : samples) {
            double bounded = Math.max(-1.0, Math.min(1.0, sample));
            out.putShort((short)Math.round(bounded * 23000.0));
        }
        return out.array();
    }

    public static byte[] cue(Cue cue) {
        double seconds = switch(cue) {
            case CLICK, NAVIGATE -> .065;
            case CONFIRM -> .19;
            case REJECT -> .22;
            case FOOTSTEP_A, FOOTSTEP_B -> .14;
            case EMPTY -> .12;
            case RELOAD -> .32;
        };
        int count = (int)(RATE * seconds);
        double[] samples = new double[count];
        Random random = new Random(331 + cue.ordinal() * 79L);
        double phase = 0;
        for (int i = 0; i < count; i++) {
            double t = i / (double)count;
            double frequency = switch(cue) {
                case CLICK -> 780 - 220 * t;
                case NAVIGATE -> 540 - 150 * t;
                case CONFIRM -> t < .46 ? 580 : 820;
                case REJECT -> 460 - 310 * t;
                case FOOTSTEP_A -> 100 - 55 * t;
                case FOOTSTEP_B -> 140 - 85 * t;
                case EMPTY -> 940 - 450 * t;
                case RELOAD -> 285 - 65 * t;
            };
            phase += TAU * frequency / RATE;
            double noise = random.nextDouble() * 2 - 1;
            double signal = switch(cue) {
                case CLICK, NAVIGATE -> Math.sin(phase) * .38 + noise * .12;
                case CONFIRM -> Math.sin(phase) * .46 + Math.sin(phase * .5) * .2;
                case REJECT -> Math.sin(phase) * .43 + noise * .11;
                case FOOTSTEP_A, FOOTSTEP_B -> noise * .35 + Math.sin(phase) * .28;
                case EMPTY -> Math.sin(phase) * .25 + noise * .16;
                case RELOAD -> Math.sin(phase) * .23 + noise * (t > .58 ? .28 : .08);
            };
            samples[i] = signal * Math.pow(1 - t, cue == Cue.CONFIRM ? 1.2 : 2.5);
        }
        return pcm(samples);
    }

    /** Index is the authoritative ArsenalCatalog index, exactly 0 through 31. */
    public static int family(int index) {
        if(index < 0 || index >= 32) throw new IllegalArgumentException("Unknown arsenal index " + index);
        if(index <= 8) return 0;   // ballistic, mechanical and rotary
        if(index <= 13) return 1;  // laser, plasma, rail, heavy beam
        if(index <= 17) return 2;  // electrical arc and plasma discharge
        if(index <= 24) return 3;  // rockets, grenades, mortars and shrapnel
        if(index <= 27) return 4;  // spinning discs, rebounds, scrap projectiles
        if(index == 28) return 5;  // organic miasma
        if(index == 29) return 6;  // cryogenic pressure
        return 7;                  // gravitational / singularity weapons
    }

    public static byte[] weapon(int index) {return weapon(index,false);}
    /** Alternate firing has its own signature, not a replay of primary SFX. */
    public static byte[] weapon(int index, boolean alternate) {
        int family = family(index);
        double duration = .14 + .026 * (index % 6) + (index == 31 ? .23 : 0)
            + (alternate ? .10 + (index%3)*.02 : 0);
        int count = (int)(RATE * duration);
        double[] samples = new double[count];
        Random random = new Random(15121L + index * 997L + (alternate?9876543L:0));
        double phase = 0;
        double lowNoise = 0;
        for(int i = 0; i < count; i++) {
            double t = i / (double)count;
            double r = random.nextDouble() * 2 - 1;
            lowNoise = lowNoise * .77 + r * .23;
            double base = (90 + index * 23 + (index % 3) * 38) * (alternate ? 1.63 : 1.0);
            double f = switch(family) {
                case 0 -> base * (1.35 - .75*t);
                case 1 -> base * (2.7 - 1.45*t);
                case 2 -> base * (4.3 - 2.2*t);
                case 3 -> base * (.85 - .37*t);
                case 4 -> base * (2.5 - .9*t);
                case 5 -> base * (.8 + .65*t);
                case 6 -> base * (3.3 + .8*t);
                default -> base * (.65 - .42*t);
            };
            phase += TAU * f / RATE;
            double harmonic = Math.sin(phase) + .42 * Math.sin(phase * (index % 4 + 2));
            double attack = Math.min(1, t * (family == 1 || family == 7 ? 45 : 190));
            double release = Math.pow(1 - t, family == 1 || family == 7 ? 1.3 : 2.1);
            double signal = switch(family) {
                case 0 -> r*.52 + harmonic*.32 + lowNoise*.26;
                case 1 -> harmonic*.52 + Math.sin(phase*.24)*.17 + r*.10;
                case 2 -> harmonic*.33 + r*(Math.sin(TAU*51*t)>0 ? .34 : .09);
                case 3 -> lowNoise*.66 + r*.25 + harmonic*.15;
                case 4 -> harmonic*.49 + r*.21 + Math.sin(phase*1.8)*.18;
                case 5 -> lowNoise*.63 + Math.sin(phase*.29)*.27;
                case 6 -> harmonic*.37 + r*.32 + Math.sin(phase*.41)*.21;
                default -> Math.sin(phase*.16)*.35 + lowNoise*.42 + harmonic*.22;
            };
            if(index == 7 || index == 8 || index == 17 || index == 27)
                signal *= .69 + .31 * Math.sin(TAU * (8 + index%5) * t);
            if(index == 4 || index == 5 || index == 6)
                signal += r * (t < .11 ? .21 : .02);
            samples[i] = attack * release * signal * (alternate ? .57 : .68);
        }
        return pcm(samples);
    }

    /** Seamless 8-second low city/wind/industrial bed, no fake weather alerts. */
    public static byte[] cityAmbience() {
        int count = RATE * 8;
        double[] samples = new double[count];
        for (int i = 0; i < count; i++) {
            double loop = i / (double)count;
            // Integral cycle counts make the WAV loop boundary continuous.
            double wind = Math.sin(TAU * 7 * loop) * .16
                + Math.sin(TAU * 13 * loop) * .09
                + Math.sin(TAU * 29 * loop) * .045;
            double machinery = Math.sin(TAU * 37 * loop) * .10
                + Math.sin(TAU * 81 * loop) * .04;
            double swell = .65 + .35 * Math.sin(TAU * loop);
            samples[i] = (wind * swell + machinery) * .48;
        }
        return pcm(samples);
    }

    /** A keyed mechanical, capacitor or organic handling signature. */
    public static byte[] handling(int index) {
        int family=family(index);
        int count=(int)(RATE * (.22 + (index%5)*.045));
        double[] out=new double[count];
        Random random=new Random(85531L + index*761L);
        double phase=0;
        double base=210+index*19;
        for(int i=0;i<count;i++){
            double t=i/(double)count;
            phase+=TAU*(base+(family==1?600:family==7?-120:0)*t)/RATE;
            double clicks=Math.exp(-Math.pow((t-.18)/.045,2))+
                .83*Math.exp(-Math.pow((t-.73)/.055,2));
            double r=random.nextDouble()*2-1;
            out[i]=(r*.33+Math.sin(phase)*.27)*
                Math.min(1,clicks)*.75;
        }
        return pcm(out);
    }

    public enum WeatherBed { RAIN, WIND, SNOW }

    /**
     * Loop-periodic procedural beds. Separate mixer volumes follow the shared
     * host's rain/snow/preset values; no second weather randomizer is created.
     * Noise is circularly interpolated, so the sample at the loop boundary
     * has no abrupt transition. These are provisional, wholly original beds.
     */
    public static byte[] weatherBed(WeatherBed kind) {
        int count = RATE * 4;
        int points = 2048;
        double[] noise = new double[points];
        Random random = new Random(2217L + kind.ordinal() * 157L);
        for(int i = 0; i < points; i++)noise[i] = random.nextDouble()*2-1;
        double[] samples = new double[count];
        for(int i = 0; i < count; i++) {
            double t = i / (double)count;
            double k = t * points;
            int start=(int)k;
            double blend=k-start;
            // Smoothstep interpolation suppresses zippering from white noise.
            blend=blend*blend*(3-2*blend);
            double hiss=noise[start]*(1-blend)+noise[(start+1)%points]*blend;
            double low=noise[(start/32)%points]*(1-blend)+
                noise[((start/32)+1)%points]*blend;
            double periodicWind=Math.sin(TAU*5*t)*.09 + Math.sin(TAU*11*t)*.05;
            samples[i]=switch(kind) {
                case RAIN -> hiss*.25 + low*.05;
                case WIND -> hiss*.10 + low*.18 + periodicWind;
                case SNOW -> hiss*.06 + low*.07 + Math.sin(TAU*3*t)*.04;
            };
        }
        return pcm(samples);
    }

    public static byte[] thunder() {
        int count=(int)(RATE*1.9);
        double[] samples=new double[count];
        Random random=new Random(72615L);
        double phase=0,rumble=0;
        for(int i=0;i<count;i++){
            double t=i/(double)count;
            double white=random.nextDouble()*2-1;
            rumble=rumble*.94 + white*.06;
            phase+=TAU*(72-22*t)/RATE;
            double clap=Math.exp(-t*21)*white*.44;
            double tail=(rumble*.7+Math.sin(phase)*.23)*
                Math.pow(1-t,1.5);
            samples[i]=clap+tail;
        }
        return pcm(samples);
    }

    /** One-shot collision sounds, not random gunfire substituted for an impact. */
    public enum Impact { SOLID, FLESH, RICOCHET, ELECTRIC, EXPLOSION }
    public static byte[] impact(Impact kind) {
        int count=(int)(RATE*(kind==Impact.EXPLOSION?1.35:
            kind==Impact.ELECTRIC?.36:.23));
        double[] result=new double[count];
        Random random=new Random(61017L+kind.ordinal()*1129L);
        double phase=0,low=0;
        for(int i=0;i<count;i++){
            double t=i/(double)count;
            double noise=random.nextDouble()*2-1;
            low=.92*low+.08*noise;
            double hz=switch(kind){
                case SOLID -> 130-65*t;
                case FLESH -> 92-51*t;
                case RICOCHET -> 1340-520*t;
                case ELECTRIC -> 780+430*Math.sin(TAU*12*t);
                case EXPLOSION -> 76-52*t;
            };
            phase+=TAU*hz/RATE;
            double wave=Math.sin(phase);
            double value=switch(kind){
                case SOLID -> low*.67+noise*.33+wave*.31;
                case FLESH -> low*.88+wave*.28;
                case RICOCHET -> wave*.68+noise*.33;
                case ELECTRIC -> wave*.56+noise*(Math.sin(TAU*39*t)>.2?.40:.08);
                case EXPLOSION -> low*.94+noise*.40+wave*.28;
            };
            double envelope=(kind==Impact.EXPLOSION?
                Math.pow(1-t,1.35):Math.pow(1-t,2.1));
            result[i]=value*envelope*.62;
        }
        return pcm(result);
    }

    /** Loop-periodic fire crackle for an actual nearby placed campfire. */
    public static byte[] campfire() {
        final int size=RATE*4;
        double[] values=new double[size];
        Random random=new Random(774423L);
        for(int i=0;i<size;i++){
            double t=i/(double)size;
            // All harmonics have integer cycles in the four-second loop.
            double crackle=Math.max(0,Math.sin(TAU*29*t))*
                Math.max(0,Math.sin(TAU*73*t))*.09;
            double bed=.10*Math.sin(TAU*17*t)+.06*Math.sin(TAU*41*t);
            values[i]=(bed+crackle)*(.7+.3*Math.cos(TAU*2*t));
        }
        return pcm(values);
    }

    /** Original nonmelodic combat tension pulse; no borrowed soundtrack. */
    public static byte[] combatPulse() {
        final int size=RATE*8;
        double[] values=new double[size];
        for(int i=0;i<size;i++){
            double t=i/(double)size;
            // Eight bars of equally spaced, strictly periodic low pulses.
            double beat=t*32;
            double envelope=Math.exp(-13*(beat-Math.floor(beat)));
            double low=Math.sin(TAU*72*i/RATE)*envelope*.21;
            double drone=Math.sin(TAU*48*i/RATE)*.035+
                Math.sin(TAU*96*i/RATE)*.022;
            values[i]=(low+drone)*.92;
        }
        return pcm(values);
    }

    /** Device-free deterministic integrity check for CI / headless builds. */
    public static void main(String[] args) {
        for(Impact kind:Impact.values()) {
            byte[] cue=impact(kind);
            if(cue.length<44+RATE/7)throw new IllegalStateException("Empty combat FX "+kind);
        }
        if(campfire().length!=44+RATE*4*2||combatPulse().length!=44+RATE*8*2)
            throw new IllegalStateException("Broken periodic environment/combat loop");
        Set<Integer> checksums = new HashSet<>();
        for(int i = 0; i < 32; i++) {
            byte[] wav = weapon(i);
            if(wav.length < 44 || wav[0] != 'R' || wav[8] != 'W')
                throw new IllegalStateException("Invalid weapon WAV " + i);
            if(!checksums.add(Arrays.hashCode(wav)))
                throw new IllegalStateException("Duplicate weapon tone " + i);
            byte[] alt=weapon(i,true);
            if(!checksums.add(Arrays.hashCode(alt)))
                throw new IllegalStateException("Duplicate alternate fire tone "+i);
            byte[] mech=handling(i);
            if(!checksums.add(Arrays.hashCode(mech)))
                throw new IllegalStateException("Duplicate handling tone "+i);
        }
        for(Cue cue : Cue.values()) {
            if(cue(cue).length <= 44) throw new IllegalStateException("Empty feedback " + cue);
        }
        for(WeatherBed bed:WeatherBed.values()) {
            if(weatherBed(bed).length!=44+RATE*4*2)
                throw new IllegalStateException("Invalid looped weather layer "+bed);
        }
        if(thunder().length <= RATE*2)
            throw new IllegalStateException("Missing procedural thunder transient");
        if(cityAmbience().length <= RATE * 8) throw new IllegalStateException("Missing city ambience");
        System.out.println("PASS GPU_AUDIO_BANK 32 distinctive weapon WAVs " + Cue.values().length
            + " feedback cues city ambience");
    }
}
