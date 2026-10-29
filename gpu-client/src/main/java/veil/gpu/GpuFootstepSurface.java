package veil.gpu;

import veil.CityLayout;
import veil.ContinentLayout;
import veil.World;

/** A ground material follows the same world/continent cells used by gameplay. */
public final class GpuFootstepSurface {
    public enum Material { ASPHALT, STONE, GRASS, LEAVES, GRAVEL, SAND, MUD, SNOW }
    private GpuFootstepSurface(){}

    public static Material classify(World world,double x,double y,
            double snow,double rain){
        if(world==null||!Double.isFinite(x)||!Double.isFinite(y))
            return Material.STONE;
        boolean city=world.detailedCity(x,y);
        boolean road=city&&world.city.road((int)Math.floor(x/CityLayout.CELL),
            (int)Math.floor(y/CityLayout.CELL));
        ContinentLayout.Biome biome=city?ContinentLayout.Biome.PLAINS:
            world.continent().biome(world.parcelX(x),world.parcelY(y));
        return select(city,road,biome,snow,rain);
    }

    static Material select(boolean city,boolean road,ContinentLayout.Biome biome,
            double snow,double rain){
        if(Double.isFinite(snow)&&snow>.18)return Material.SNOW;
        if(city)return road?Material.ASPHALT:Material.STONE;
        if(biome==null)return Material.STONE;
        return switch(biome){
            case BEACH -> Material.SAND;
            case MARSH -> Material.MUD;
            case FOREST -> Material.LEAVES;
            case PLAINS -> Double.isFinite(rain)&&rain>.70?
                Material.MUD:Material.GRASS;
            case HILLS,MOUNTAINS -> Material.GRAVEL;
            case WATER -> Material.MUD; // shoreline movement only
        };
    }

    public static void main(String[] ignored){
        check(select(true,true,ContinentLayout.Biome.PLAINS,0,0),Material.ASPHALT);
        check(select(true,false,ContinentLayout.Biome.PLAINS,0,0),Material.STONE);
        check(select(false,false,ContinentLayout.Biome.FOREST,0,0),Material.LEAVES);
        check(select(false,false,ContinentLayout.Biome.BEACH,0,0),Material.SAND);
        check(select(false,false,ContinentLayout.Biome.MARSH,0,0),Material.MUD);
        check(select(false,false,ContinentLayout.Biome.PLAINS,0,.9),Material.MUD);
        check(select(false,false,ContinentLayout.Biome.HILLS,0,0),Material.GRAVEL);
        check(select(true,true,ContinentLayout.Biome.PLAINS,.5,0),Material.SNOW);
        System.out.println("PASS GPU_FOOTSTEP_SURFACE city,road,biome,rain,snow");
    }
    private static void check(Material actual,Material expected){
        if(actual!=expected)throw new AssertionError(actual+" != "+expected);
    }
}
