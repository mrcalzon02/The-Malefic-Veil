package veil;

import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** Verifies exported pilot tracks through the actual runtime evaluator. */
public final class FieldPilotAnimationTest {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    @SuppressWarnings("unchecked")
    public static void main(String[] args)throws Exception{
        Path folder=Path.of(args.length==0?"build/candidates/field-pilots-r9":args[0]);
        var ids=args.length>1?List.of(args[1]):List.of("sentry-mki","censer-of-dread","regeneration-heart");
        for(String id:ids){
            byte[] bytes=Files.readAllBytes(folder.resolve(id).resolve(id+".glb"));
            AnimatedGlb rig=new AnimatedGlb(bytes,id);
            check(rig.clips().size()==9,"Missing state clips: "+id);
            ByteBuffer file=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            int length=file.getInt(12);byte[] json=Arrays.copyOfRange(bytes,20,20+length);
            var doc=(Map<String,Object>)new GlbMesh.Parser(new String(json,java.nio.charset.StandardCharsets.UTF_8).trim()).parse();
            check(rig.hasFieldCoordinateContract(),"Missing runtime coordinate contract: "+id);
            for(var mesh:(List<Map<String,Object>>)doc.get("meshes"))
                for(var primitive:(List<Map<String,Object>>)mesh.get("primitives")){
                    var attributes=(Map<String,Object>)primitive.get("attributes");
                    check(!attributes.containsKey("NORMAL")&&!attributes.containsKey("TANGENT"),"Runtime normal/tangent attributes returned: "+id);
                    for(var morph:(List<Map<String,Object>>)primitive.getOrDefault("targets",List.of()))
                        check(!morph.containsKey("NORMAL")&&!morph.containsKey("TANGENT"),"Morph normal/tangent attributes returned: "+id);
                }
            for(var material:(List<Map<String,Object>>)doc.get("materials"))
                check(!material.containsKey("normalTexture"),"Runtime normal map returned: "+id);
            var nodes=(List<Map<String,Object>>)doc.get("nodes");
            for(var clip:(List<Map<String,Object>>)doc.get("animations")){
                var channels=(List<Map<String,Object>>)clip.get("channels");check(!channels.isEmpty(),"Empty clip: "+id+" "+clip.get("name"));
                Set<String> targets=new HashSet<>();
                for(var channel:channels){var target=(Map<String,Object>)channel.get("target");
                    String key=target.get("node")+":"+target.get("path");check(targets.add(key),"Duplicate target: "+id+" "+clip.get("name")+" "+key);
                    var node=nodes.get(((Number)target.get("node")).intValue());
                    check(!"DeployRoot".equals(node.get("name"))||!"scale".equals(target.get("path")),"Whole-object shrinking returned: "+id);
                }
            }
            GlbMesh rest=rig.pose("ACTIVE",0,0,0),operating=rig.pose("OPERATING",.20,0,0),packed=rig.pose("PACKED",0,0,0);
            check(rest.triangleCount()==operating.triangleCount(),"Pose topology changed: "+id);
            check(!rest.triangles().equals(operating.triangles()),"Operation frozen: "+id);
            check(rest.surfaces().stream().allMatch(s->s.texture()!=null),"Missing baked surface: "+id);
            float[] p=packed.bounds();
            System.out.printf(java.util.Locale.ROOT,"%s triangles=%d packed=%.3fx%.3fx%.3f minY=%.3f%n",id,rest.triangleCount(),p[3]-p[0],p[4]-p[1],p[5]-p[2],p[1]);
            float minimumGround=Float.POSITIVE_INFINITY;
            for(int sample=0;sample<=30;sample++){
                GlbMesh mesh=rig.pose("DEPLOYING",sample/20.,0,0);
                check(mesh.triangleCount()==rest.triangleCount(),"Deployment topology changed: "+id);
                for(float coordinate:mesh.bounds())check(Float.isFinite(coordinate),"Nonfinite deployed bounds: "+id);
                minimumGround=Math.min(minimumGround,mesh.bounds()[1]);
            }
            System.out.printf(java.util.Locale.ROOT,"%s deployment minimum Y=%.4f%n",id,minimumGround);
            check(minimumGround>=-.001,"Deployment penetrates the ground: "+id);
            if(id.equals("regeneration-heart")){
                // The organ must actually deform, beyond moving appendages: contraction changes its upper bounds.
                float[] a=rest.bounds(),b=operating.bounds();
                check(Math.abs(a[4]-b[4])>.001,"Cardiac morph deformation missing");
            }
            if(id.equals("sentry-mki"))
                check(!rest.triangles().equals(rig.pose("ACTIVE",0,1,.2).triangles()),"Host aim ignored");
            int blueprint=switch(id){case "sentry-mki"->2;case "field-medic"->3;case "censer-of-dread"->12;case "regeneration-heart"->17;default->throw new IllegalArgumentException(id);};
            var bp=ThrownDeployables.blueprint(blueprint);
            Path fixture=Path.of("build/audit-field-loading",id);Path staged=fixture.resolve("assets/game/field-deployables-approved");
            Files.createDirectories(staged);Files.write(staged.resolve(bp.mesh()+".glb"),bytes);
            var loader=new FieldDeployableAssets(fixture); // Isolated test fixture; never promotes into game assets.
            var dying=new ThrownDeployables.View(1,blueprint,0,0,0,0,0,0,false,123456,-1,400,0,0,0,false);
            var collapse=loader.pose(dying);
            check(collapse.triangles().equals(rig.pose("DESTROYED",.4,0,0).triangles()),"Runtime collapse clock differs from host destruction age: "+id);
            check(!collapse.triangles().equals(rest.triangles()),"Destroyed pose frozen at ACTIVE: "+id);
        }
        System.out.println("FieldPilotAnimationTest OK: exported TRS/morph evaluation, nonempty unique-channel clips, no root scale, finite deployment, texture coverage and host aim");
    }
}
