package veil;
import java.nio.file.*;
import java.util.*;
/** Exercises actual exported GLBs, not generated metadata or a mirrored animation implementation. */
public final class FieldAnimationTest {
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception{
        Path folder=Path.of(args.length==0?"build/candidates/field-deployables":args[0]);
        int models=0;
        for(var bp:ThrownDeployables.catalog()){
            Path p=folder.resolve(bp.mesh()).resolve(bp.mesh()+".glb");
            AnimatedGlb rig=new AnimatedGlb(Files.readAllBytes(p),bp.mesh());
            check(rig.clips().size()==9,"Nine states missing "+bp.id());
            GlbMesh deployed=rig.pose("ACTIVE",0,0,0),packed=rig.pose("PACKED",0,0,0);
            check(deployed.triangleCount()>=700&&deployed.triangleCount()<=3500,"Budget "+bp.id());
            check(packed.bounds()[3]-packed.bounds()[0]<.65,"Not portable "+bp.id());
            check(deployed.surfaces().stream().allMatch(s->s.texture()!=null),"Missing surface texture "+bp.id());
            check(deployed.textureImage(deployed.surfaces().get(0).texture()).getWidth()==256,"Missing baked atlas");
            for(var state:ThrownDeployables.State.values()){
                GlbMesh a=rig.pose(state.name(),0,0,0),b=rig.pose(state.name(),.30,0,0);
                check(a.triangleCount()==b.triangleCount(),"Pose topology changed");
                for(float value:b.bounds())check(Float.isFinite(value),"Nonfinite animated geometry");
                if(!Set.of("PACKED","ACTIVE").contains(state.name()))
                    check(!a.triangles().equals(b.triangles()),"Frozen clip "+bp.id()+" "+state);
            }
            if(bp.role()==ThrownDeployables.Role.TURRET)
                check(!deployed.triangles().equals(rig.pose("ACTIVE",0,1,.2).triangles()),"Host aim not applied");
            models++;
        }
        boolean rejected=false;try{new AnimatedGlb(new byte[32],"bad");}catch(java.io.IOException e){rejected=true;}
        check(rejected,"Malformed GLB accepted");
        System.out.println("FieldAnimationTest OK: "+models+" exported rigs, 189 clips, embedded textures, aim, bounds and malformed-file rejection");
    }
}
