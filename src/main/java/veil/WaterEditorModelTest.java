package veil;
// Upstream independent SDK regression adapted from The Beckenburg Project.
import dev.mrcalzon.water.*;
import dev.mrcalzon.water.WaterModule.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Authoring/runtime round trips, multi-edit transactions and actual seam pixels. */
public final class WaterEditorModelTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static WaterSettings flat(){var v=new HashMap<String,String>();v.put("wave.height","0");v.put("shore.height","0");v.put("foam.opacity","0");v.put("fresnel","0");v.put("refraction","0");v.put("detail","0");v.put("alpha.minimum",".5");v.put("alpha.maximum",".5");v.put("colour.shallow","0000ff");v.put("colour.deep","0000ff");return new WaterSettings(v);}
 static int pixel(List<Body> bodies){int[] p=new int[9];Arrays.fill(p,0xff0000);double[] d=new double[9];Arrays.fill(d,Double.POSITIVE_INFINITY);new WaterModule(bodies).composite(new Camera(0,2,0,0,-Math.PI/4,2,3,3),p,d,0);return p[4];}
 static Body body(WaterPlacement p){return p.body(WaterTextures::builtin,w->(x,z)->w.y()-3,List.of());}
 public static void main(String[] args)throws Exception{
  var settings=flat();var left=new WaterPlacement("left",-2,0,2,4,8,0,settings);var right=new WaterPlacement("right",2,0,2,4,8,0,settings);
  check(pixel(List.of(body(left)))==0x800080,"Single editor plane must automatically render its configured water");check(pixel(List.of(body(left),body(right)))==0x800080,"Adjacent matching planes must not double opacity at their shared edge");
  var overlap=new WaterPlacement("overlap",0,0,2,8,8,.7,settings);check(pixel(List.of(body(left),body(right),body(overlap)))==0x800080,"Overlapping compatible modules must form one seamless surface");
  var raised=new WaterPlacement("raised",0,.5,2,8,8,0,settings);check(pixel(List.of(body(left),body(raised)))==0x4000c0,"Genuinely distinct elevations retain independent transparent layering");
  var doc=new WaterDocument("synthetic-map",List.of(left,right));var model=new WaterEditorModel(doc);model.select(List.of("left","right"));model.edit("$x",1);check(model.document().placements().get(0).x()==1&&model.document().placements().get(1).x()==5,"Multi-move preserves relative placement");check(model.undo()&&model.document().equals(doc)&&model.redo(),"Bounded history restores complete immutable documents");
  String before=model.document().revision();try{model.edit("alpha.minimum",.9);throw new AssertionError("Invalid alpha accepted");}catch(IllegalArgumentException expected){}check(model.document().revision().equals(before),"Invalid multi-edit cannot partially change the scene");
  model.validator(d->{if(d.placements().stream().anyMatch(p->p.settings().text("layer1.texture").equals("water://missing")))throw new IllegalArgumentException("Missing texture");});
  try{model.edit("layer1.texture","water://missing");throw new AssertionError("Provider failure accepted");}catch(IllegalArgumentException expected){}check(model.document().revision().equals(before),"Missing asset validation rolls back before history/preview mutation");
  model.duplicateSelected(4,0);check(model.document().placements().size()==4&&model.selected().size()==2,"Multi-copy creates unique independently positioned module records");model.edit("wave.height",.2);check(model.selected().stream().allMatch(p->p.settings().number("wave.height")==.2),"Parameter edits reach every selected module");model.matchSettings();
  var encoded=model.document().encode();var decoded=WaterDocument.read(new ByteArrayInputStream(encoded.getBytes(StandardCharsets.UTF_8)));check(decoded.equals(model.document())&&decoded.revision().equals(model.document().revision()),"Save/load preserves exact settings, textures, travel directions and dimensions");
  var path=Path.of("build/water-editor-check/project-test.water");decoded.save(path);check(WaterDocument.read(Files.newInputStream(path)).equals(decoded),"Actual atomic UTF-8 project export/import");
  for(String bad:List.of(encoded+"unexpected=1\n",encoded.replace("water.0.x=1.0\n",""),encoded.replace("water.0.width=4.0","water.0.width=NaN"))){try{WaterDocument.read(new ByteArrayInputStream(bad.getBytes(StandardCharsets.UTF_8)));throw new AssertionError("Bad module document accepted");}catch(IOException expected){}}
  var moving=new WaterSettings();var a=body(new WaterPlacement("a",-2,0,2,4,8,0,moving));var b=body(new WaterPlacement("b",2,0,2,4,8,.5,moving));for(double t=0;t<8;t+=.1)check(a.effects().surface(0,2,t).offset()==b.effects().surface(0,2,t).offset()&&a.effects().first().sample(0,2,t)==b.effects().first().sample(0,2,t),"Shared world geometry/UV phase remains seamless under placement rotation");
  check(Math.abs(moving.effects(WaterTextures::builtin,(x,z)->-3,0).first().scrollX()-.015)<1e-10,"Editor direction/speed conversion preserves the approved texture motion");
  System.out.println("PASS portable module documents, real automatic surface pixels, seamless adjacent/overlap joins, layered elevations, multi-edit/copy/undo, validation rollback, atomic save/load and unchanged texture travel");
 }
}
