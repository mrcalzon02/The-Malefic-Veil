package veil;

import dev.mrcalzon.water.*;
import dev.mrcalzon.water.WaterModule.*;
import java.util.*;

/**
 * Read-only ocean adapter. The independent MIT water engine owns the waves
 * and optical settings; Malefic's real seeded coastline owns where water is
 * allowed. Outside the actual continent, or on beach/city/land, no sea is
 * invented. No actor physics or collision rules are changed by this renderer.
 */
public final class VeilOcean {
    public static final double SEA_LEVEL=0;
    private final World world;
    private final ContinentVista vista;
    private final Bathymetry bed;
    private final WaterSettings settings;
    private final WaterModule module;
    private final Map<String,WaterEffects> weatherEffects=new HashMap<>();

    public VeilOcean(World world,ContinentVista vista){
        this.world=Objects.requireNonNull(world);
        this.vista=Objects.requireNonNull(vista);
        var ground=vista.cityPlates();
        double left=ground.worldX(0),right=ground.worldX(ContinentLayout.SIZE);
        double near=ground.worldZ(0),far=ground.worldZ(ContinentLayout.SIZE);
        var polygon=new Polygon(List.of(new Point(left,near),new Point(right,near),
            new Point(right,far),new Point(left,far)));
        bed=(x,z)->{
            int px=world.parcelX(x),pz=world.parcelY(z);
            if(!world.continent().inBounds(px,pz)||
                world.continent().biome(px,pz)!=ContinentLayout.Biome.WATER)
                return SEA_LEVEL+.1; // dry land never qualifies as water
            double floor=Math.min(-.15,vista.sample(x,z).elevation());
            // Optical bathymetry shallows only at REAL source-biome land
            // borders. The terrain/collision mesh is never modified. This
            // lets the independent shore-runup and foam system visibly break
            // at the continent's actual coast instead of a fake global band.
            double edge=Double.POSITIVE_INFINITY;
            double x0=vista.cityPlates().worldX(px);
            double z0=vista.cityPlates().worldZ(pz);
            double size=World.PARCEL_METERS;
            var land=world.continent();
            if(land.inBounds(px-1,pz)&&land.biome(px-1,pz)!=ContinentLayout.Biome.WATER)
                edge=Math.min(edge,x-x0);
            if(land.inBounds(px+1,pz)&&land.biome(px+1,pz)!=ContinentLayout.Biome.WATER)
                edge=Math.min(edge,x0+size-x);
            if(land.inBounds(px,pz-1)&&land.biome(px,pz-1)!=ContinentLayout.Biome.WATER)
                edge=Math.min(edge,z-z0);
            if(land.inBounds(px,pz+1)&&land.biome(px,pz+1)!=ContinentLayout.Biome.WATER)
                edge=Math.min(edge,z0+size-z);
            return Double.isFinite(edge)?
                Math.min(-.12,Math.max(floor,-Math.max(.12,edge*.55))):floor;
        };
        settings=new WaterSettings().with("wave.height",.16)
            .with("shore.height",.055).with("foam.depth",.7)
            .with("colour.shallow","345966").with("colour.deep","182d45")
            .with("colour.reflection","536677");
        var effect=settings.effects(WaterTextures::builtin,bed,SEA_LEVEL);
        module=new WaterModule(List.of(new Body("continental-ocean",polygon,
            SEA_LEVEL,bed,WaterTextures.builtin("water://ripple-a"),
            settings.style(),List.of(),effect)));
    }
    /** Source-owned parcel footprint for native chunk meshing. No floating
     * corner sampling can incorrectly reject entire coastline sections. */
    public record SeaParcel(double west,double north,double size){}
    public List<SeaParcel> sourceSeaParcels(double west,double north,
                                            double east,double south){
        int x0=Math.max(0,world.parcelX(west));
        int x1=Math.min(ContinentLayout.SIZE-1,world.parcelX(Math.nextDown(east)));
        int z0=Math.max(0,world.parcelY(north));
        int z1=Math.min(ContinentLayout.SIZE-1,world.parcelY(Math.nextDown(south)));
        if(x0>x1||z0>z1)return List.of();
        var found=new ArrayList<SeaParcel>();
        for(int z=z0;z<=z1;z++)for(int x=x0;x<=x1;x++)
            if(world.continent().biome(x,z)==ContinentLayout.Biome.WATER)
                found.add(new SeaParcel(world.worldX(x),world.worldY(z),
                    World.PARCEL_METERS));
        return found;
    }
    public WaterModule module(){return module;}
    public boolean wet(double x,double z){return module.bodies().get(0).wet(x,z);}
    public double bottom(double x,double z){return bed.bottomY(x,z);}
    /** Preset changes originate from the host. The shared wave formula and
     * independent scrolling texture layers remain exactly the SDK's. */
    public synchronized WaterEffects effects(String preset){
        String state=preset==null?"CLEAR":preset;
        return weatherEffects.computeIfAbsent(state,kind->{
            double height=switch(kind){
                case "THUNDERSTORM","HEAVY_RAIN","BLIZZARD" -> .29;
                case "RAIN","SLEET","HAIL","DUST_STORM","ASHFALL" -> .22;
                case "DRIZZLE","SNOW" -> .18;
                default -> .16;
            };
            return settings.with("wave.height",height)
                .effects(WaterTextures::builtin,bed,SEA_LEVEL);
        });
    }
    public double height(double x,double z,double seconds,String preset){
        if(!wet(x,z))return Double.NaN;
        return effects(preset).surface(x,z,seconds).offset();
    }
    /** Alpha-tinted vertex color sampled from the water library's two real
     * scrolling PSX texture layers, not a static biome/terrain fill. */
    public int color(double x,double z,double seconds,String preset){
        if(!wet(x,z))return 0;
        var e=effects(preset);
        var style=module.bodies().get(0).style();
        double depth=SEA_LEVEL-bottom(x,z);
        double absorption=1-Math.exp(-style.absorption()*depth);
        int a=style.shallow(),b=style.deep();
        int rr=(int)(((a>>>16)&255)*(1-absorption)+((b>>>16)&255)*absorption);
        int gg=(int)(((a>>>8)&255)*(1-absorption)+((b>>>8)&255)*absorption);
        int bb=(int)((a&255)*(1-absorption)+(b&255)*absorption);
        int c1=e.first().sample(x,z,seconds),c2=e.second().sample(x,z,seconds);
        double variation=(((c1>>>16)&255)*(1-e.blend())+
            ((c2>>>16)&255)*e.blend()-128)/128.0*e.detail();
        double foam=Math.max(0,1-depth/e.foamDepth())*
            e.foamPattern(x,z,seconds,depth)*e.foamOpacity();
        rr=(int)Math.max(0,Math.min(255,rr*(1+variation)*(1-foam)+202*foam));
        gg=(int)Math.max(0,Math.min(255,gg*(1+variation)*(1-foam)+215*foam));
        bb=(int)Math.max(0,Math.min(255,bb*(1+variation)*(1-foam)+218*foam));
        int alpha=(int)Math.round(255*(style.minimumOpacity()+
            (style.maximumOpacity()-style.minimumOpacity())*absorption));
        return (alpha<<24)|(rr<<16)|(gg<<8)|bb;
    }
}
