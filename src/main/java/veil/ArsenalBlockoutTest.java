package veil;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Validates the original blockout exports with the game's actual static GLB reader. */
public final class ArsenalBlockoutTest {
    public static void main(String[] args) throws Exception {
        Path folder=Path.of("assets/game/arsenal-blockouts");
        List<String> rows=Files.readAllLines(folder.resolve("catalog.tsv"),StandardCharsets.UTF_8);
        Set<String> ids=new HashSet<>();
        int triangles=0;
        for(String line:rows.subList(1,rows.size())) {
            String[] fields=line.split("\t",-1);
            if(fields.length!=6 || !ids.add(fields[0]))throw new AssertionError("Invalid catalog row: "+line);
            byte[] bytes=Files.readAllBytes(folder.resolve(fields[0]+"-static.glb"));
            GlbMesh mesh=GlbMesh.read(bytes);
            if(mesh.triangleCount()<50)throw new AssertionError("Missing blockout geometry: "+fields[0]);
            float[] b=mesh.bounds();
            for(float n:b)if(!Float.isFinite(n))throw new AssertionError("Nonfinite bounds: "+fields[0]);
            for(int axis=0;axis<3;axis++)if(b[axis+3]<=b[axis])throw new AssertionError("Flat volume: "+fields[0]);
            for(GlbMesh.Triangle t:mesh.triangles()) {
                float[] xyz={t.ax(),t.ay(),t.az(),t.bx(),t.by(),t.bz(),t.cx(),t.cy(),t.cz()};
                for(float n:xyz)if(!Float.isFinite(n)||Math.abs(n)>3)throw new AssertionError("Invalid local position: "+fields[0]);
            }
            triangles+=mesh.triangleCount();
        }
        if(ids.size()!=32)throw new AssertionError("Expected 32 distinct blockouts, got "+ids.size());
        try(var files=Files.list(folder)) {
            long count=files.filter(p->p.getFileName().toString().endsWith("-static.glb")).count();
            if(count!=32)throw new AssertionError("Uncataloged static blockouts: "+count);
        }
        System.out.println("PASS ArsenalBlockoutTest: 32 original blockouts, "+triangles+" imported triangles; local bounds finite and under 3m");
    }
}
