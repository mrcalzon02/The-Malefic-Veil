package veil;
// Upstream independent SDK regression adapted from The Beckenburg Project.
import dev.mrcalzon.water.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class WaterShapesSnapTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static WaterPlacement p(String id,double x,double y,double z,double width,double length,WaterPlacement.Shape shape){return new WaterPlacement(id,x,y,z,width,length,0,new WaterSettings(),shape);}
 static double minX(WaterPlacement p){return p.polygon().points().stream().mapToDouble(v->v.x()).min().orElseThrow();}
 public static void main(String[] args)throws Exception{
  var square=p("square",0,0,0,4,2,WaterPlacement.Shape.SQUARE);check(square.polygon().contains(1.9,.9)&&!square.polygon().contains(2.01,0),"Square/rectangle has real bounded water footprint");
  var circle=p("circle",0,0,0,4,4,WaterPlacement.Shape.CIRCLE);check(circle.polygon().contains(1.9,0)&&!circle.polygon().contains(1.5,1.5),"Circle clips actual water corners");check(circle.resized(4,6).width()==6&&circle.resized(4,6).depth()==6,"Circle diameter stays circular from either size handle");
  var oval=p("oval",0,0,0,6,2,WaterPlacement.Shape.OBLONG);check(oval.polygon().contains(2.9,0)&&oval.polygon().contains(0,.9)&&!oval.polygon().contains(2.9,.9),"Oblong is a real ellipse");
  var oct=p("oct",0,0,0,4,4,WaterPlacement.Shape.OCTAGON);check(oct.polygon().contains(1.99,0)&&!oct.polygon().contains(1.9,1.9)&&Math.abs(minX(oct)+2)<1e-8,"Octagon uses declared bounds and clipped corners");
  var pill=p("pill",0,0,0,8,2,WaterPlacement.Shape.PILL);check(pill.polygon().contains(3.8,0)&&pill.polygon().contains(0,.9)&&!pill.polygon().contains(3.8,.8),"Pill has semicircular caps and straight sides");var tall=p("tall",0,0,0,2,8,WaterPlacement.Shape.PILL);check(tall.polygon().contains(0,3.8)&&!tall.polygon().contains(.8,3.8),"Pill supports either long axis");check(WaterSnap.rotated(pill,Math.PI/2).polygon().contains(0,3.8),"Footprints follow real rotation");
  var target=p("target",0,0,0,4,4,WaterPlacement.Shape.SQUARE);var moving=p("moving",4.2,.001,1,4,2,WaterPlacement.Shape.SQUARE);var match=WaterSnap.find(moving,List.of(target),.4,false);check(match!=null,"Nearby facing edges detected");var snapped=WaterSnap.apply(moving,match,moving);check(Math.abs(minX(snapped)-2)<1e-8&&Math.abs(snapped.z()-1)<1e-8&&snapped.y()==0,"Partial-edge join snaps flush without arbitrary midpoint movement");
  check(WaterSnap.find(moving.moved(10,0,0),List.of(target),.4,true)==null&&WaterSnap.find(moving.moved(0,.5,0),List.of(target),.4,true)==null,"Snap tolerance and storey separation respected");
  for(var shape:List.of(WaterPlacement.Shape.CIRCLE,WaterPlacement.Shape.OBLONG,WaterPlacement.Shape.OCTAGON,WaterPlacement.Shape.PILL)){double width=shape==WaterPlacement.Shape.PILL?8:2;var rim=p("rim",2+width/2+.1,0,0,width,2,shape);var m=WaterSnap.find(rim,List.of(target),.4,false);check(m!=null&&Math.abs(minX(WaterSnap.apply(rim,m,rim))-2)<1e-7,"Actual curved/polygon rim snaps with rotation disabled: "+shape);}
  var tilted=WaterSnap.rotated(moving,-.02);var angled=WaterSnap.find(tilted,List.of(target),.4,true);check(angled!=null&&Math.abs(WaterSnap.apply(tilted,angled,tilted).yaw())<1e-7,"Optional small-angle alignment makes edges parallel");
  var initial=new WaterDocument("shape-test",List.of(target,moving));var model=new WaterEditorModel(initial);model.select(List.of("moving"));model.beginGesture();for(int i=0;i<20;i++)model.edit("$x",4.2+i*.002);check(model.snapSelected(.4,false)!=null,"Controller applies edge snap");model.endGesture(false);check(model.undo()&&model.document().equals(initial),"Complete drag plus snap is one undo operation");check(model.redo(),"Gesture redo");var before=model.document();model.beginGesture();model.edit("$x",12);model.endGesture(true);check(model.document().equals(before),"Cancelled drag preserves exact scene");
  model.edit("$shape",WaterPlacement.Shape.PILL);model.duplicateSelected(3,0);check(model.selected().get(0).shape()==WaterPlacement.Shape.PILL,"Shape survives multi-copy");var decoded=WaterDocument.read(new ByteArrayInputStream(model.document().encode().getBytes(StandardCharsets.UTF_8)));check(decoded.equals(model.document()),"Shapes persist through actual module export/import");
  System.out.println("PASS all5 real water footprints, size/rotation, exact straight/curved edge joins, optional angle/height snapping, bounded proximity and single-gesture undo/redo/cancel");
 }
}
