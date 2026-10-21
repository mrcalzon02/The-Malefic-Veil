package veil;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Regression proves nine committed glTF models genuinely decode as mesh triangles. */
public final class AirPowerMeshesTest {
    private static void check(boolean valid,String message){
        if(!valid)throw new AssertionError(message);
    }
    public static void main(String[] args)throws Exception{
        var registered=AirPowerMeshes.specs();
        check(registered.size()==9,"The 3x3 aerial faction roster must be complete");
        Set<String> ids=new HashSet<>();
        for(var spec:registered)
            check(ids.add(spec.id()),"No duplicate aircraft identifiers");
        var loaded=AirPowerMeshes.load(Path.of("."));
        check(loaded.size()==9,"Nine actual GLB objects must be available locally");
        for(var faction:CharacterRoster.Faction.values()){
            int count=0;
            float priorSpan=0;
            for(var role:AirPowerMeshes.Role.values()){
                var descriptor=registered.stream().filter(
                    s->s.faction()==faction&&s.role()==role).findFirst().orElseThrow();
                GlbMesh mesh=loaded.get(descriptor.id());
                check(mesh!=null&&mesh.triangleCount()==descriptor.triangles(),
                    "Model did not decode from real GLB "+descriptor.id());
                check(mesh.triangleCount()>500&&mesh.triangleCount()<4000,
                    "Aircraft must be polygonal authored geometry within interim limits");
                check(mesh.surfaces().size()==mesh.triangleCount(),
                    "Material surfaces must match physical triangle faces");
                var materialColors=new HashSet<Integer>();
                for(var face:mesh.triangles())materialColors.add(face.rgb());
                check(materialColors.size()>=4,"Not a single-color cardboard aircraft");
                float[] bounds=mesh.bounds();
                float span=bounds[3]-bounds[0];
                check(span>priorSpan,"Faction's heavy aircraft must be physically wider than medium/light");
                priorSpan=span;count++;
            }
            check(count==3,"Each faction must have one light, medium and heavy aircraft");
        }
        var gnat=loaded.get("begotten_gnat");
        var mantis=loaded.get("begotten_mantis");
        var whale=loaded.get("begotten_behemoth");
        check(gnat.triangleCount()>1300,"Bat harness, mangy flesh and dual guns require geometric detail");
        check(mantis.triangleCount()>1400,"Four clawed legs and vulture/lion head modeled");
        check(whale.triangleCount()>3000,"Whale maw, paired dragon wings and skin scaffolding modeled");
        check(AirPowerMeshes.spec("penitent_cathedral").role()==AirPowerMeshes.Role.HEAVY,
            "Legacy ID preserved for the non-cathedral industrial abattoir design");
        System.out.println("PASS AirPowerMeshesTest: nine GLBs, SHA-256, actual triangles, materials, role silhouettes");
    }
}
