package veil.gpu;

import veil.ContinentLayout;
import veil.World;

/** One location and one host-time classification; no parallel world RNG. */
public final class GpuBiomeAudioPlanner {
    public enum Bed { CITY, COAST, FOREST, MARSH, HIGHLANDS, PLAINS, NIGHT }
    public record Selection(Bed region,float nightMix) {}
    private GpuBiomeAudioPlanner(){}

    public static Selection at(World world,double x,double y,double hour) {
        if(world==null||!Double.isFinite(x)||!Double.isFinite(y))
            return new Selection(Bed.CITY,0);
        if(world.detailedCity(x,y))return choose(true,null,hour);
        return choose(false,world.continent().biome(
            world.parcelX(x),world.parcelY(y)),hour);
    }
    static Selection choose(boolean city,ContinentLayout.Biome biome,double hour){
        Bed bed=city?Bed.CITY:biome==null?Bed.PLAINS:switch(biome){
            case WATER,BEACH -> Bed.COAST;
            case FOREST -> Bed.FOREST;
            case MARSH -> Bed.MARSH;
            case HILLS,MOUNTAINS -> Bed.HIGHLANDS;
            case PLAINS -> Bed.PLAINS;
        };
        double h=Double.isFinite(hour)?((hour%24)+24)%24:12;
        float night=(float)(h>=21||h<4?.85:
            h>=19?(.85*(h-19)/2):
            h<6?(.85*(6-h)/2):0);
        return new Selection(bed,Math.max(0,Math.min(1,night)));
    }
    private static void test(Selection got,Bed region,boolean night){
        if(got.region()!=region||(night?got.nightMix()<=0:got.nightMix()!=0))
            throw new AssertionError("Unexpected soundscape "+got);
    }
    public static void main(String[] args){
        test(choose(true,null,12),Bed.CITY,false);
        test(choose(false,ContinentLayout.Biome.BEACH,12),Bed.COAST,false);
        test(choose(false,ContinentLayout.Biome.WATER,1),Bed.COAST,true);
        test(choose(false,ContinentLayout.Biome.FOREST,2),Bed.FOREST,true);
        test(choose(false,ContinentLayout.Biome.MARSH,17),Bed.MARSH,false);
        test(choose(false,ContinentLayout.Biome.HILLS,21),Bed.HIGHLANDS,true);
        test(choose(false,ContinentLayout.Biome.PLAINS,10),Bed.PLAINS,false);
        if(choose(false,ContinentLayout.Biome.PLAINS,6).nightMix()!=0)
            throw new AssertionError("Daylight soundscape not settled");
        System.out.println("PASS GPU_BIOME_AUDIO city,coast,forest,marsh,hills,plains,night");
    }
}
