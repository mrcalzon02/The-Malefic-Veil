package veil.gpu;

import veil.World;

/**
 * Conservative, two-dimensional acoustic wall-proximity probe. This is NOT a
 * roof test or a second room simulation: it only reacts to real collision
 * solids around a listener inside the detailed-city world.
 */
public final class GpuAcousticSpace {
    @FunctionalInterface interface Solid {boolean at(double x,double y);}
    public record Result(float enclosure,float outdoorGain,float earlyReflection) {
        public Result {
            if(!Float.isFinite(enclosure)||!Float.isFinite(outdoorGain)||
               !Float.isFinite(earlyReflection)||enclosure<0||enclosure>1||
               outdoorGain<.35f||outdoorGain>1||
               earlyReflection<0||earlyReflection>.27f)
                throw new IllegalArgumentException("Invalid acoustic result");
        }
    }
    private static final Result OUTDOORS=new Result(0,1,0);
    private static final double[][] DIRECTIONS={
        {1,0},{-1,0},{0,1},{0,-1},
        {.7071067811865476,.7071067811865476},
        {-.7071067811865476,.7071067811865476},
        {.7071067811865476,-.7071067811865476},
        {-.7071067811865476,-.7071067811865476}};
    private GpuAcousticSpace(){}
    public static Result inspect(World world,double x,double y){
        if(world==null||!Double.isFinite(x)||!Double.isFinite(y)||
           !world.detailedCity(x,y))return OUTDOORS;
        // These probes are intentionally cheap: 16 existing collision-cell
        // reads, NO potentially expensive lineClear/UrbanProp sweeps.
        return sample(true,x,y,world::solid);
    }
    static Result sample(boolean detailedCity,double x,double y,Solid geometry){
        if(!detailedCity||geometry==null)return OUTDOORS;
        int near=0,far=0;
        for(double[] axis:DIRECTIONS){
            if(geometry.at(x+axis[0]*1.65,y+axis[1]*1.65))near++;
            if(geometry.at(x+axis[0]*3.25,y+axis[1]*3.25))far++;
        }
        float enclose=(float)((near*.65+far*.35)/DIRECTIONS.length);
        float external=1f-.55f*enclose;
        float echo=.26f*enclose;
        return new Result(enclose,external,echo);
    }
    /** Sum-cap shared weather and regional fauna, without muting combat/UI. */
    static float environmentalBudget(float[] weather,float[] regions){
        float sum=0;
        if(weather!=null)for(float v:weather)sum+=Math.max(0,v);
        if(regions!=null)for(float v:regions)sum+=Math.max(0,v)*.38f;
        return sum<=.78f?1f:.78f/sum;
    }
    private static void require(boolean pass,String why){
        if(!pass)throw new AssertionError(why);
    }
    public static void main(String[] args){
        Result outdoors=sample(false,0,0,(x,y)->true);
        require(outdoors.enclosure()==0&&outdoors.outdoorGain()==1&&
            outdoors.earlyReflection()==0,"Wilderness must not invent walls");
        Result empty=sample(true,0,0,(x,y)->false);
        require(empty.enclosure()==0&&empty.outdoorGain()==1,"Open street");
        Result corridor=sample(true,0,0,(x,y)->Math.abs(y)>1.2&&
            Math.abs(x)<.5);
        require(corridor.enclosure()>0&&corridor.enclosure()<.7,
            "Two opposing surfaces should be partial enclosure");
        Result boxed=sample(true,0,0,(x,y)->true);
        require(boxed.enclosure()==1&&boxed.outdoorGain()>=.4f&&
            boxed.earlyReflection()>.2f,"Dense walls should dampen outside");
        require(environmentalBudget(new float[]{.5f,.34f,.20f},
            new float[]{.65f})<1,"Weather + fauna gain must be bounded");
        require(environmentalBudget(new float[]{.1f},
            new float[]{.2f})==1,"Quiet world should not be amplified");
        System.out.println("PASS GPU_ACOUSTIC_SPACE wall sampling, outdoor, reflections, ambient budget");
    }
}
