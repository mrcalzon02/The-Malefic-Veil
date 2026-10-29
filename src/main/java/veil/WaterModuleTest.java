package veil;
// Upstream independent SDK regression adapted from The Beckenburg Project.
import dev.mrcalzon.water.WaterModule;
import dev.mrcalzon.water.WaterModule.*;
import java.util.*;
/** Ray/opaque-depth/shore/hull/layer regressions independent of the game adapter. */
public final class WaterModuleTest {
 private static final Polygon AREA=new Polygon(List.of(new Point(-5,-5),new Point(5,-5),new Point(5,5),new Point(-5,5)));
 private static final Camera CAMERA=new Camera(0,2,0,0,-Math.PI/4,2,3,3);
 private static Texture solid(int rgb){return new Texture(2,2,new int[]{rgb,rgb,rgb,rgb});}
 private static Style style(int rgb){return new Style(rgb,rgb,1,.5,.5,2,.1);}
 private static Body body(String id,double y,int rgb,List<DryVolume> dry){return new Body(id,AREA,y,(x,z)->-3,solid(rgb),style(rgb),dry);}
 private static int[] red(){int[] p=new int[9];Arrays.fill(p,0xff0000);return p;}
 private static double[] far(){double[] p=new double[9];Arrays.fill(p,Double.POSITIVE_INFINITY);return p;}
 private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
 public static void main(String[] args){
  var water=new WaterModule(List.of(body("lake",0,0x0000ff,List.of())));int[] pixels=red();double[] depths=far();
  water.composite(CAMERA,pixels,depths,0);check(pixels[4]==0x800080,"Half-transparent surface must retain real opaque RGB");check(Math.abs(depths[4]-Math.sqrt(8))<1e-8,"Water writes actual camera-forward surface depth");
  pixels=red();depths=far();depths[4]=1;water.composite(CAMERA,pixels,depths,0);check(pixels[4]==0xff0000&&depths[4]==1,"Opaque foreground must occlude water");
  Polygon interior=new Polygon(List.of(new Point(-.5,1.5),new Point(.5,1.5),new Point(.5,2.5),new Point(-.5,2.5)));
  var dry=new DryVolume("actual hull",interior,-.3,.8);check(dry.excludes(.5,0,2),"Hull boundary is dry too");check(!dry.excludes(0,1,2),"Exclusion is a volume, not an infinitely tall hole");
  var boatWater=new WaterModule(List.of(body("lake",0,0x0000ff,List.of(dry))));pixels=red();depths=far();var stats=boatWater.composite(CAMERA,pixels,depths,0);check(pixels[4]==0xff0000&&Double.isInfinite(depths[4])&&stats.excludedHullSamples()>0,"Boat cockpit must retain its dry opaque interior");
  var layered=new WaterModule(List.of(body("near",.5,0x00ff00,List.of()),body("far",0,0x0000ff,List.of())));pixels=red();depths=far();layered.composite(CAMERA,pixels,depths,0);check(pixels[4]==0x408040,"Independent transparent layers sort back-to-front");
  var dryShore=new Body("shore",AREA,0,(x,z)->.1,solid(0x0000ff),style(0x0000ff),List.of());pixels=red();new WaterModule(List.of(dryShore)).composite(CAMERA,pixels,far(),0);check(pixels[4]==0xff0000&&!dryShore.wet(0,2),"No water above a bed higher than its surface");
  var depthStyle=new Style(0x0000ff,0x0000ff,1,0,.95,2,0);var shallow=new Body("shallow",AREA,0,(x,z)->-.1,solid(0x0000ff),depthStyle,List.of());var deep=new Body("deep",AREA,0,(x,z)->-3,solid(0x0000ff),depthStyle,List.of());int[] a=red(),b=red();new WaterModule(List.of(shallow)).composite(CAMERA,a,far(),0);new WaterModule(List.of(deep)).composite(CAMERA,b,far(),0);check((b[4]&255)>(a[4]&255),"Deeper water absorbs more underlying light");
  int[] raw={1,2,3,4};var texture=new Texture(2,2,raw);raw[0]=99;check(texture.sample(0,0)==1&&texture.sample(-1,-1)==1,"Texture is immutable and repeats across negative world coordinates");
  System.out.println("PASS independent water module: opaque occlusion, real depth, transparent layering, shore dryness, volumetric hull exclusion, depth absorption and texture ownership");
 }
}
