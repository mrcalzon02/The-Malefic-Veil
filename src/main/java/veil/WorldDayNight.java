package veil;

/** Deterministic, inspectable world lighting clock shared by desktop renderers.
 * Pure Java: the GPU backend reads it; the dedicated host never loads OpenGL. */
public final class WorldDayNight {
    public static final long DAY_MS=24L*60L*1000L; // 24 in-game hours over 24 real minutes
    public record Conditions(double hour, double daylight, double dusk,
        double ambient, double fogDensity, double lampStrength,
        float skyRed,float skyGreen,float skyBlue,
        float hazeRed,float hazeGreen,float hazeBlue,
        float sunRed,float sunGreen,float sunBlue) {}
    private WorldDayNight() {}
    private static double clamp(double value){return Math.max(0,Math.min(1,value));}
    private static double smooth(double edgeA,double edgeB,double value){
        double u=clamp((value-edgeA)/(edgeB-edgeA));
        return u*u*(3-2*u);
    }
    private static float mix(float a,float b,double fraction){
        return (float)(a+(b-a)*fraction);
    }
    public static Conditions atHour(double hour){
        if(!Double.isFinite(hour))throw new IllegalArgumentException("Invalid time");
        hour=((hour%24)+24)%24;
        double rise=smooth(5.0,7.4,hour);
        double set=smooth(17.1,19.5,hour);
        double day=clamp(rise*(1-set));
        double dusk=clamp(1-Math.abs(hour-6.4)/1.8)*0.7+
                    clamp(1-Math.abs(hour-18.3)/1.8)*0.7;
        dusk*=1-day*0.35;
        double ambient=0.11+0.70*day+0.10*dusk;
        float skyR=mix(.018f,.27f,day),skyG=mix(.020f,.43f,day),
            skyB=mix(.055f,.58f,day);
        skyR=mix(skyR,.48f,dusk*.6);skyG=mix(skyG,.17f,dusk*.6);
        float hazeR=mix(.045f,.60f,day),hazeG=mix(.050f,.70f,day),
            hazeB=mix(.083f,.68f,day);
        hazeR=mix(hazeR,.63f,dusk*.72);
        hazeG=mix(hazeG,.24f,dusk*.72);
        float sr=mix(.28f,1.0f,day),sg=mix(.32f,.91f,day),sb=mix(.54f,.74f,day);
        sr=mix(sr,1.0f,dusk*.8);sg=mix(sg,.55f,dusk*.8);
        return new Conditions(hour,day,dusk,ambient,
            .12+(.14*(1-day)),clamp(1-day*.90),
            skyR,skyG,skyB,hazeR,hazeG,hazeB,sr,sg,sb);
    }
    public static Conditions atElapsedMs(long worldElapsedMs){
        return atHour((Math.floorMod(worldElapsedMs,DAY_MS)*24.0)/DAY_MS);
    }
    public static String clock(double hour){
        int minutes=(int)Math.floor((((hour%24)+24)%24)*60);
        return String.format(java.util.Locale.ROOT,"%02d:%02d",minutes/60,minutes%60);
    }
}
