package veil;
// Upstream independent SDK regression adapted from The Beckenburg Project.
import dev.mrcalzon.water.*;
import dev.mrcalzon.water.WaterModule.*;
import java.util.*;
import java.io.*;
/** Independent analytic/reference-ray checks for the animated module. */
public final class WaterEffectsTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static WaterEffects effects(double amplitude){var t=new Texture(2,2,new int[]{0x202020,0xdddddd,0x777777,0xaaaaaa});return new WaterEffects(new WaterEffects.Layer(t,.3,.2,.015,.001,128),new WaterEffects.Layer(t,.2,.3,-.015,-.02,192),.27,.35,amplitude,1,.8,.045,.28,.65,5,.5,0x303b4c);}
 public static void main(String[] args)throws Exception{
  var e=effects(.065);var random=new Random(1928);int rays=0;
  for(int k=0;k<200;k++){
   double x=random.nextDouble()*20-10,z=random.nextDouble()*20-10,time=random.nextDouble()*120,dx=random.nextDouble()*2-1,dz=random.nextDouble()*2-1,dy=-.015-random.nextDouble();var c=new Camera(x,1,z,0,0,1,1,1);
   double hit=e.intersect(c,dx,dy,dz,0,time,180);if(!Double.isFinite(hit))continue;rays++;
   check(Math.abs(1+dy*hit-e.surface(x+dx*hit,z+dz*hit,time).offset())<1e-7,"Ray must land on actual displaced triangle");
   // An independent dense sweep detects a skipped earlier wave crossing.
   for(double t=.065;t<hit-.0002;t+=.0005)check(1+dy*t-e.surface(x+dx*t,z+dz*t,time).offset()>-1e-5,"Skipped nearer wave at grazing angle");
  }
  check(rays>190,"Ray coverage");var c=new Camera(0,2,0,0,0,1,1,1);check(Math.abs(effects(0).intersect(c,0,-.5,1,0,2,180)-4)<1e-8,"Zero waves preserves flat analytic plane");
  check(Double.isNaN(e.intersect(c,0,.5,1,0,2,180)),"Upward ray must not draw underwater plane");
  check(Math.abs(e.vertex(3,4,0)-e.vertex(3,4,1))>.001,"Waves actually animate");
  var bed=new GridBed(0,0,1,2,2,new double[]{0,1,3,2});check(Math.abs(bed.bottomY(.75,.25)-1)<1e-8&&Math.abs(bed.bottomY(.25,.75)-2)<1e-8,"Bed follows rendered triangle diagonal, not bilinear approximation");check(Double.isNaN(bed.bottomY(-1,0)),"Bed cannot extrapolate outside footprint");check(bed.triangles().size()==2,"Single grid cell exports two triangles");
  var hull=HullSections.read(WaterEffectsTest.class.getResourceAsStream("/org/beckenburg/sites/hollow-creek-water-hull.tsv"));
  for(double y=-2.015;y<=-1.885;y+=.001){check(hull.excludes(23.9,y,-5),"Boat center stays dry for full wave cycle");check(!hull.excludes(23,y,-5)&&!hull.excludes(24.8,y,-5),"Hull cutout cannot swallow outside water");}
  check(!hull.excludes(23.9,-1.5,-5),"Profile does not create an infinitely tall exclusion");
  try{GridBed.read(new ByteArrayInputStream(new byte[40]));throw new AssertionError("Bad bed accepted");}catch(IOException expected){}
  System.out.println("PASS water effects: "+rays+" independent nearest-wave rays; zero-wave plane; animation; exact optical bed triangles; full-wave actual hull exclusion; bounded readers");
 }
}
